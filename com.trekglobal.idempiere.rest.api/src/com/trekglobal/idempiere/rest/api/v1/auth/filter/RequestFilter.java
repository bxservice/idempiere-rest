/**********************************************************************
* This file is part of iDempiere ERP Open Source                      *
* http://www.idempiere.org                                            *
*                                                                     *
* Copyright (C) Contributors                                          *
*                                                                     *
* This program is free software; you can redistribute it and/or       *
* modify it under the terms of the GNU General Public License         *
* as published by the Free Software Foundation; either version 2      *
* of the License, or (at your option) any later version.              *
*                                                                     *
* This program is distributed in the hope that it will be useful,     *
* but WITHOUT ANY WARRANTY; without even the implied warranty of      *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
* GNU General Public License for more details.                        *
*                                                                     *
* You should have received a copy of the GNU General Public License   *
* along with this program; if not, write to the Free Software         *
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
* MA 02110-1301, USA.                                                 *
*                                                                     *
* Contributors:                                                       *
* - Trek Global Corporation                                           *
* - Heng Sin Low                                                      *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.v1.auth.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.annotation.Priority;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.adempiere.util.ServerContext;
import org.compiere.model.MClient;
import org.compiere.model.MRole;
import org.compiere.model.MSession;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Login;
import org.compiere.util.Util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.trekglobal.idempiere.rest.api.json.ResponseUtils;
import com.trekglobal.idempiere.rest.api.json.RestUtils;
import com.trekglobal.idempiere.rest.api.model.MAuthToken;
import com.trekglobal.idempiere.rest.api.model.MOIDCService;
import com.trekglobal.idempiere.rest.api.model.MRefreshToken;
import com.trekglobal.idempiere.rest.api.model.MRestResourceAccess;
import com.trekglobal.idempiere.rest.api.v1.jwt.LoginClaims;
import com.trekglobal.idempiere.rest.api.v1.jwt.TokenUtils;

@Provider
@Priority(Priorities.AUTHORIZATION)
/**
 * Validate JWT token and set environment context(client,org,user,role and warehouse)
 * @author hengsin
 *
 */
public class RequestFilter implements ContainerRequestFilter {
	// optional health monitoring key (AD_SysConfig.Name=REST_HEALTH_MONITORING_KEY)
	private static final String REST_HEALTH_MONITORING_KEY = "REST_HEALTH_MONITORING_KEY";
	public static final String LOGIN_NAME = "#LoginName";
	public static final String LOGIN_CLIENTS = "#LoginClients";

	public RequestFilter() {
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		Properties ctx = new Properties();
		ServerContext.setCurrentInstance(ctx);
		
		if (   HttpMethod.OPTIONS.equals(requestContext.getMethod())
			|| (   HttpMethod.POST.equals(requestContext.getMethod())
				&& requestContext.getUriInfo().getPath().endsWith("v1/auth/tokens")
				)
			|| (   HttpMethod.GET.equals(requestContext.getMethod())
					&& requestContext.getUriInfo().getPath().endsWith("v1/auth/jwk")
					)
			|| (   HttpMethod.POST.equals(requestContext.getMethod())
					&& requestContext.getUriInfo().getPath().endsWith("v1/auth/refresh")
					)
			|| (   HttpMethod.POST.equals(requestContext.getMethod())
					&& requestContext.getUriInfo().getPath().endsWith("v1/auth/logout")
					)
			) {
			return;
		}
		
		if (HttpMethod.GET.equals(requestContext.getMethod())
			 && requestContext.getUriInfo().getPath().endsWith("v1/health"))
		{
			String healthMonitoringKey = MSysConfig.getValue(REST_HEALTH_MONITORING_KEY, "");
			if (!Util.isEmpty(healthMonitoringKey)) {
				String healthMonitoringValue = requestContext.getUriInfo().getQueryParameters().getFirst("key");
				if (Util.isEmpty(healthMonitoringValue) || !healthMonitoringValue.equals(healthMonitoringKey)) {
					requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());					
				}
			}
			return;
		}
		
		if (PresignedURL.isPresignedURL(requestContext)) {
			PresignedURL.validateSignature(requestContext);
			return;
		}
		
		String authHeaderVal = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

		// consume JWT i.e. execute signature validation
		if (authHeaderVal != null && authHeaderVal.startsWith("Bearer")) {
			try {
				//validate Bearer token exists
				String[] authHeaderValues = authHeaderVal.split(" ");
				if (authHeaderValues.length < 2) {
					requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
					return;
				}
				validate(authHeaderValues[1], requestContext);
				if (Util.isEmpty(Env.getContext(Env.getCtx(), Env.AD_USER_ID)) ||
					Util.isEmpty(Env.getContext(Env.getCtx(), Env.AD_ROLE_ID))) {
					if (!requestContext.getUriInfo().getPath().startsWith("v1/auth/")) {
						requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
					}
				}
				//check resource access by role (if enable)
				if (MRestResourceAccess.isResourceAccessByRole()) {
					if (!requestContext.getUriInfo().getPath().startsWith("v1/auth/")) {
						if (!MRestResourceAccess.hasAccess(requestContext.getUriInfo().getPath(true), requestContext.getMethod())) {
							requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
						}
					}
				}
			} catch (JWTVerificationException ex) {
				ex.printStackTrace();				
				requestContext.abortWith(ResponseUtils.getResponseError(Status.UNAUTHORIZED, ex.getLocalizedMessage(), "", ""));
			} catch (Exception ex) {
				ex.printStackTrace();
				requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
			}
		} else {
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
		}
	}

	private void validate(String token, ContainerRequestContext requestContext) throws IllegalArgumentException, UnsupportedEncodingException {
		
		if(MAuthToken.isBlocked(token)) {
			throw new JWTVerificationException("Token is blocked");
		}
		if(MRefreshToken.isRevoked(token)) {
			throw new JWTVerificationException("Token is revoked");
		}
		
		MOIDCService service = MOIDCService.findMatchingOIDCService(token);
		if (service != null) {
			service.validateAccessToken(token, requestContext);
			return;
		}
		
		Algorithm algorithm = Algorithm.HMAC512(TokenUtils.getTokenSecret());
		JWTVerifier verifier = JWT.require(algorithm)
		        .withIssuer(TokenUtils.getTokenIssuer())
		        .build(); //Reusable verifier instance
		DecodedJWT jwt = verifier.verify(token);
		String userName = jwt.getSubject();
		ServerContext.setCurrentInstance(new Properties());
		Env.setContext(Env.getCtx(), LOGIN_NAME, userName);
		Claim claim = jwt.getClaim(LoginClaims.Clients.name());
		if (!claim.isNull() && !claim.isMissing()) {
			String clients = claim.asString();
			Env.setContext(Env.getCtx(), LOGIN_CLIENTS, clients);
		}
		claim = jwt.getClaim(LoginClaims.AD_Client_ID.name());
		int AD_Client_ID = 0;
		if (!claim.isNull() && !claim.isMissing()) {
			AD_Client_ID = claim.asInt();
			MClient client = MClient.get(AD_Client_ID);
			if (client == null)
				throw new JWTVerificationException("Invalid client claim");
			if (!client.isActive())
				throw new JWTVerificationException("Client is inactive");
			Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, AD_Client_ID);				
		}
		claim = jwt.getClaim(LoginClaims.AD_User_ID.name());
		if (!claim.isNull() && !claim.isMissing()) {
			MUser user = MUser.get(claim.asInt());
			if (user == null)
				throw new JWTVerificationException("Invalid user claim");
			if (!user.isActive())
				throw new JWTVerificationException("User is inactive");
			Env.setContext(Env.getCtx(), Env.AD_USER_ID, claim.asInt());
		}
		claim = jwt.getClaim(LoginClaims.AD_Role_ID.name());
		int AD_Role_ID = 0;
		if (!claim.isNull() && !claim.isMissing()) {
			AD_Role_ID = claim.asInt();
			MRole role = MRole.get(Env.getCtx(), AD_Role_ID);
			if (role == null)
				throw new JWTVerificationException("Invalid role claim");
			if (!role.isActive())
				throw new JWTVerificationException("Role is inactive");
			Env.setContext(Env.getCtx(), Env.AD_ROLE_ID, AD_Role_ID);				
		}
		claim = jwt.getClaim(LoginClaims.AD_Org_ID.name());
		int AD_Org_ID = 0;
		if (!claim.isNull() && !claim.isMissing()) {
			AD_Org_ID = claim.asInt();
			Env.setContext(Env.getCtx(), Env.AD_ORG_ID, AD_Org_ID);				
		}
		claim = jwt.getClaim(LoginClaims.M_Warehouse_ID.name());
		if (!claim.isNull() && !claim.isMissing()) {
			Env.setContext(Env.getCtx(), Env.M_WAREHOUSE_ID, claim.asInt());				
		}
		claim = jwt.getClaim(LoginClaims.AD_Language.name());
		if (!claim.isNull() && !claim.isMissing()) {
			String AD_Language = claim.asString();
			Env.setContext(Env.getCtx(), Env.LANGUAGE, AD_Language);
		}
		claim = jwt.getClaim(LoginClaims.AD_Session_ID.name());
		int AD_Session_ID = 0;
		if (!claim.isNull() && !claim.isMissing()) {
			AD_Session_ID = claim.asInt();
			Env.setContext(Env.getCtx(), Env.AD_SESSION_ID, AD_Session_ID);
			MSession session = MSession.get(Env.getCtx());
			if (session == null)
				throw new JWTVerificationException("Invalid session claim");
			if (session.isProcessed()) {
				// is possible that the session was finished in a reboot instead of a logout
				// if there is a REST_AuthToken or a REST_RefreshToken, then the user has not logged out
				MAuthToken authToken = MAuthToken.get(Env.getCtx(), token);
				if (authToken != null  || MRefreshToken.existsAuthToken(token)) {
					DB.executeUpdateEx("UPDATE AD_Session SET Processed='N', UpdatedBy=CreatedBy, Updated=getDate() WHERE AD_Session_ID=?", new Object[] {AD_Session_ID}, null);
					session.load(session.get_TrxName());
				} else {
					requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
				}
			}
			// validate login on the token to check if session is still valid
			String errorMessage = new Login(Env.getCtx()).validateLogin(new KeyNamePair(AD_Org_ID, ""));
			if (!Util.isEmpty(errorMessage))
				throw new JWTVerificationException(errorMessage);
		}
		RestUtils.setSessionContextVariables(Env.getCtx());
	}

}

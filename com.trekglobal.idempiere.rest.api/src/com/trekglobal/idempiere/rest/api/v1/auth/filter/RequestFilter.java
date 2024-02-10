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
import java.sql.Timestamp;
import java.util.Properties;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.adempiere.util.ServerContext;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MClientInfo;
import org.compiere.model.MRole;
import org.compiere.model.MSession;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.trekglobal.idempiere.rest.api.model.MAuthToken;
import com.trekglobal.idempiere.rest.api.model.MOIDCService;
import com.trekglobal.idempiere.rest.api.v1.jwt.LoginClaims;
import com.trekglobal.idempiere.rest.api.v1.jwt.TokenUtils;

@Provider
/**
 * Validate JWT token and set environment context(client,org,user,role and warehouse)
 * @author hengsin
 *
 */
public class RequestFilter implements ContainerRequestFilter {
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
		
		String authHeaderVal = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

		// consume JWT i.e. execute signature validation
		if (authHeaderVal != null && authHeaderVal.startsWith("Bearer")) {
			try {
				validate(authHeaderVal.split(" ")[1], requestContext);
				if (Util.isEmpty(Env.getContext(Env.getCtx(), Env.AD_USER_ID)) ||
					Util.isEmpty(Env.getContext(Env.getCtx(), Env.AD_ROLE_ID))) {
					if (!requestContext.getUriInfo().getPath().startsWith("v1/auth/")) {
						requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
					}
				}
			} catch (JWTVerificationException ex) {
				ex.printStackTrace();
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
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
			Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, AD_Client_ID);				
		}
		claim = jwt.getClaim(LoginClaims.AD_User_ID.name());
		if (!claim.isNull() && !claim.isMissing()) {
			Env.setContext(Env.getCtx(), Env.AD_USER_ID, claim.asInt());
		}
		claim = jwt.getClaim(LoginClaims.AD_Role_ID.name());
		int AD_Role_ID = 0;
		if (!claim.isNull() && !claim.isMissing()) {
			AD_Role_ID = claim.asInt();
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
			Env.setContext(Env.getCtx(), "#AD_Session_ID", AD_Session_ID);
			MSession session = MSession.get(Env.getCtx());
			if (session.isProcessed())
				requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
		}
		
		if (AD_Role_ID > 0) {
			if (MRole.getDefault(Env.getCtx(), false).isShowAcct())
				Env.setContext(Env.getCtx(), "#ShowAcct", "Y");
			else
				Env.setContext(Env.getCtx(), "#ShowAcct", "N");
		}
		
		Env.setContext(Env.getCtx(), "#Date", new Timestamp(System.currentTimeMillis()));
		
		/** Define AcctSchema , Currency, HasAlias **/
		if (AD_Client_ID > 0) {
			if (MClientInfo.get(Env.getCtx(), AD_Client_ID).getC_AcctSchema1_ID() > 0) {
				MAcctSchema primary = MAcctSchema.get(Env.getCtx(), MClientInfo.get(Env.getCtx(), AD_Client_ID).getC_AcctSchema1_ID());
				Env.setContext(Env.getCtx(), "$C_AcctSchema_ID", primary.getC_AcctSchema_ID());
				Env.setContext(Env.getCtx(), "$C_Currency_ID", primary.getC_Currency_ID());
				Env.setContext(Env.getCtx(), "$HasAlias", primary.isHasAlias());
			}
			
			MAcctSchema[] ass = MAcctSchema.getClientAcctSchema(Env.getCtx(), AD_Client_ID);
			if(ass != null && ass.length > 1) {
				for(MAcctSchema as : ass) {
					if (as.getAD_OrgOnly_ID() != 0) {
						if (as.isSkipOrg(AD_Org_ID)) {
							continue;
						} else  {
							Env.setContext(Env.getCtx(), "$C_AcctSchema_ID", as.getC_AcctSchema_ID());
							Env.setContext(Env.getCtx(), "$C_Currency_ID", as.getC_Currency_ID());
							Env.setContext(Env.getCtx(), "$HasAlias", as.isHasAlias());
							break;
						}
					}
				}
			}
		}
	}
}

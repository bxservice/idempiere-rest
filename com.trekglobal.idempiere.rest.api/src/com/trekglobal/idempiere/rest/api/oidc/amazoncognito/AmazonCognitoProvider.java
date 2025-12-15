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
* - Elaine Tan	                                                      *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.oidc.amazoncognito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;

import org.compiere.model.MOrg;
import org.compiere.model.MRole;
import org.compiere.model.MSession;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.osgi.service.component.annotations.Component;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.model.MOIDCService;
import com.trekglobal.idempiere.rest.api.oidc.AbstractOIDCProvider;
import com.trekglobal.idempiere.rest.api.oidc.AuthenticatedUser;
import com.trekglobal.idempiere.rest.api.oidc.IOIDCProvider;

/**
 * @author etantg
 */
@Component(immediate = true, service = IOIDCProvider.class, property = "name=Amazon Cognito")
public class AmazonCognitoProvider extends AbstractOIDCProvider {
	
	public AmazonCognitoProvider() {
	}

	@Override
	public AuthenticatedUser getAuthenticatedUser(DecodedJWT decodedJwt, ContainerRequestContext requestContext,
			MOIDCService oidcService) {
		int AD_Client_ID = oidcService.getAD_Client_ID();
		int AD_Org_ID = -1;
		int AD_User_ID = -1;
		int AD_Role_ID = -1;
        
        String orgHeader = requestContext.getHeaderString(MOIDCService.ORG_HEADER);
        String roleHeader = requestContext.getHeaderString(MOIDCService.ROLE_HEADER);
        
        DecodedJWT decodedIdToken = oidcService.getDecodedIdToken(requestContext);
		if (oidcService.isAuthorization_OIDC()) {			
			//cognito groups mapping to iDempiere role (AD_Role.Name) without space
			List<String> roleNames = new ArrayList<>();
			Claim rolesClaim = decodedJwt.getClaim("cognito:groups");
			if (rolesClaim.isNull() || rolesClaim.isMissing()) {
				if (decodedIdToken != null)
					rolesClaim = decodedIdToken.getClaim("cognito:groups");
				if (rolesClaim.isNull() || rolesClaim.isMissing())
					throw new JWTVerificationException("Missing roles claim");
			}
			String[] roles = rolesClaim.asArray(String.class);
			for(String role : roles) {
				String roleName = role;
				Query query = new Query(Env.getCtx(), MRole.Table_Name, "AD_Client_ID=? AND REPLACE(Name,' ','')=?", null);
				MRole mRole = query.setOnlyActiveRecords(true).setParameters(AD_Client_ID, roleName).firstOnly();
				if (mRole != null) {
					roleNames.add(mRole.getName());
				}
			}
			
			//cognito roles mapping to iDempiere role
			String roleName = null;
			if (roleNames.size() == 1) {
				roleName = roleNames.get(0);
				if (!Util.isEmpty(roleHeader) && !roleHeader.equals(roleName)) {
					throw new JWTVerificationException("Invalid %s header".formatted(MOIDCService.ROLE_HEADER));
				}
			} else {
				if (!Util.isEmpty(roleHeader)) {
					final String finalRoleHeader = roleHeader;
					Optional<String> optional = roleNames.stream().filter(e -> finalRoleHeader.equals(e)).findFirst();
					if (optional.isPresent())
						roleName = optional.get();
					else
						throw new JWTVerificationException("Invalid %s header".formatted(MOIDCService.ROLE_HEADER));
				}
			}
			
			//validate roleName claim
			Query query = new Query(Env.getCtx(), MRole.Table_Name, "AD_Client_ID=? AND Name=?", null);
			if (roleName != null) {
				MRole mRole = query.setOnlyActiveRecords(true).setParameters(AD_Client_ID, roleName).first();
				if (mRole != null) {
					AD_Role_ID = mRole.get_ID();
				}
			} else {
				MRole mRole = null;
				for (String r : roleNames) {
					mRole = query.setOnlyActiveRecords(true).setParameters(AD_Client_ID, r).first();
					if (mRole != null) {
						AD_Role_ID = mRole.get_ID();
						break;
					}
				}
			}
			
			if (AD_Role_ID == -1) {
				throw new JWTVerificationException("Invalid Role claim");
			}
		} else {
			//not resolving authorization details from access token, try http header
			if (!Util.isEmpty(orgHeader)) {
				if ("*".equals(orgHeader)) {
					AD_Org_ID = 0;
				} else {
					Query query = new Query(Env.getCtx(), MOrg.Table_Name, "AD_Client_ID=? AND Value=?", null);
					MOrg org = query.setOnlyActiveRecords(true).setParameters(AD_Client_ID, orgHeader).first();
					if (org != null) {
						AD_Org_ID = org.getAD_Org_ID();
					}
					if (AD_Org_ID == -1)
						throw new JWTVerificationException("Invalid %s header".formatted(MOIDCService.ORG_HEADER));
				}
			}
			if (!Util.isEmpty(roleHeader)) {
				Query query = new Query(Env.getCtx(), MRole.Table_Name, "AD_Client_ID=? AND Name=?", null);
				MRole mRole = query.setOnlyActiveRecords(true).setParameters(AD_Client_ID, roleHeader).first();
				if (mRole != null) {
					AD_Role_ID = mRole.getAD_Role_ID();
				} else {
					throw new JWTVerificationException("Invalid %s header".formatted(MOIDCService.ROLE_HEADER));
				}
			}
		}
		
		//resolve user using ad_user.email or ad_user.name
		boolean useEmail = MSysConfig.getBooleanValue(MSysConfig.USE_EMAIL_FOR_LOGIN, false);
		String userId = decodedJwt.getClaim(useEmail ? "email" : "username").asString();
		if (Util.isEmpty(userId) && decodedIdToken != null)
			userId = decodedIdToken.getClaim(useEmail ? "email" : "cognito:username").asString();
		if (Util.isEmpty(userId)) {
			String userInfo = getUserInfo(decodedJwt, requestContext, oidcService);
			if (!Util.isEmpty(userInfo)) {
				JsonObject json = new Gson().fromJson(userInfo, JsonObject.class);
				userId = json.get(useEmail ? "email" : "username").getAsString();
			}
		}
		if (Util.isEmpty(userId)) {
			throw new JWTVerificationException("Missing user claim");
		}
		Query query = new Query(Env.getCtx(), MUser.Table_Name, "AD_Client_ID IN (0,?) AND %s=?".formatted((useEmail ? "Email" : "Name")), null);
		MUser user = query.setOnlyActiveRecords(true).setParameters(AD_Client_ID, userId).setOrderBy("AD_Client_ID DESC").first();
		if (user == null) {
			throw new JWTVerificationException("Invalid user claim");
		}
		AD_User_ID = user.get_ID();
		
		boolean orgAccessValidated = false;
		if (AD_Role_ID == -1) {
			//get role from id token or if no id token but user has single role only 
			if (!Util.isEmpty(roleHeader)) {
				query = new Query(Env.getCtx(), MRole.Table_Name, "AD_Client_ID=? AND Name=?", null);
				MRole mRole = query.setOnlyActiveRecords(true).setParameters(AD_Client_ID, roleHeader).first();
				if (mRole != null) {
					AD_Role_ID = mRole.getAD_Role_ID();
				} else {
					throw new JWTVerificationException("Invalid %s header".formatted(MOIDCService.ROLE_HEADER));
				}
			} else {
				AD_Role_ID = getSingleRoleIDOnly(AD_Client_ID, user);
			}
		}
		
		//get org from id token or if role has access to single org only
		if (AD_Role_ID >= 0) {
			if (AD_Org_ID == -1) {
				if (!Util.isEmpty(orgHeader)) {
					if ("*".equals(orgHeader)) {
						AD_Org_ID = 0;
					} else {
						query = new Query(Env.getCtx(), MOrg.Table_Name, "AD_Client_ID=? AND Value=?", null);
						MOrg org = query.setOnlyActiveRecords(true).setParameters(AD_Client_ID, orgHeader).first();
						if (org != null) {
							AD_Org_ID = org.getAD_Org_ID();
						} else {
							throw new JWTVerificationException("Invalid %s header".formatted(MOIDCService.ORG_HEADER));
						}
					}
				} else {
					AD_Org_ID = getSingleOrgIdOnly(AD_Client_ID, AD_Role_ID, user);
					if (AD_Org_ID >= 0)
						orgAccessValidated = true;
					else
						throw new JWTVerificationException("Missing Organization Claim or Header");
				}
			}
		} else {
			throw new JWTVerificationException("Missing Role Claim or Header");
		}
		
		if (AD_Role_ID >= 0 && AD_Org_ID >= 0 && !orgAccessValidated){		
			//validate org access
			Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, AD_Client_ID);
			MRole mRole = new MRole(Env.getCtx(), AD_Role_ID, null);
			if (mRole.isUseUserOrgAccess()) {
				mRole.setAD_User_ID(AD_User_ID);
			}
			if (!mRole.isOrgAccess(AD_Org_ID, true)) {
				throw new JWTVerificationException("Invalid user and organization combination");
			}
		}
		
		MSession session = MSession.get(Env.getCtx());
		if (session == null){
			session = MSession.create(Env.getCtx());
			session.setWebSession("idempiere-rest-oidc");
			session.saveEx();
		}
		
		return new AuthenticatedUser(AD_Client_ID, AD_Org_ID, AD_Role_ID, AD_User_ID, session.getAD_Session_ID());
	}
	
	private String getUserInfo(DecodedJWT decodedJwt, ContainerRequestContext requestContext, 
			MOIDCService oidcService) {		
		String scope = decodedJwt.getClaim("scope").asString();
		if (Util.isEmpty(scope) || !scope.contains("openid"))
			return null;
		
		String endpoint = null;
		String wellKnownUrl = oidcService.getOIDC_ConfigurationURL();
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wellKnownUrl))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse the JSON response
            JsonObject json = new Gson().fromJson(response.body(), JsonObject.class);

            // Extract the UserInfo endpoint 
            endpoint = json.get("userinfo_endpoint").getAsString();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        
		try {	        
            String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

            // Create the URL object
            URL url = new URL(endpoint);

            // Open a connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set the request method and headers
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", authorizationHeader);

            // Get the response code
            int responseCode = connection.getResponseCode();

            // If the response code is 200, read and print the response
             if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Print the response (contains user attributes)
                return response.toString();
            } else {
            	// Failed to get user information
            }
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
		
		return null;
	}
}

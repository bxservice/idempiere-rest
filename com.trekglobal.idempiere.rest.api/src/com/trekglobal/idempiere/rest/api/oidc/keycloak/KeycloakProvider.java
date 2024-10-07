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
package com.trekglobal.idempiere.rest.api.oidc.keycloak;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.container.ContainerRequestContext;

import org.compiere.model.MOrg;
import org.compiere.model.MRole;
import org.compiere.model.MSession;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.osgi.service.component.annotations.Component;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.model.MOIDCService;
import com.trekglobal.idempiere.rest.api.oidc.AuthenticatedUser;
import com.trekglobal.idempiere.rest.api.oidc.IOIDCProvider;

/**
 * @author hengsin
 */
@Component(immediate = true, service = IOIDCProvider.class, property = "name=Keycloak")
public class KeycloakProvider implements IOIDCProvider {
	/**
	 * Default constructor
	 */
	public KeycloakProvider() {
	}

	@Override
	public AuthenticatedUser getAuthenticatedUser(DecodedJWT decodedJwt, ContainerRequestContext requestContext, MOIDCService oidcService) {		
		int AD_Client_ID = oidcService.getAD_Client_ID();
		int AD_Org_ID = -1;
		int AD_User_ID = -1;
		int AD_Role_ID = -1;
		
		String orgHeader = requestContext.getHeaderString(MOIDCService.ORG_HEADER);
		String roleHeader = requestContext.getHeaderString(MOIDCService.ROLE_HEADER);
		String headerOrgValue = null;
		
		if (oidcService.isAuthorization_OIDC()) {
			//resource_access.${keycloak_client_id}.roles
			List<String> roleNames = new ArrayList<>();
			Claim azpClaim = decodedJwt.getClaim("azp");
			String keycloakClientId = azpClaim.asString();
			Claim resourceAccessClaim = decodedJwt.getClaim("resource_access");
			if (resourceAccessClaim.isNull() || resourceAccessClaim.isMissing()) {
				throw new JWTVerificationException("Missing roles claim");
			}
			String resourceAccessText = resourceAccessClaim.toString();
			Gson gson = new Gson();
			JsonObject resourceAccessJson = gson.fromJson(resourceAccessText, JsonObject.class);
			JsonObject resourceClientJson = resourceAccessJson.getAsJsonObject(keycloakClientId);
			JsonElement rolesElement = resourceClientJson.get("roles");
			JsonArray rolesArray = rolesElement.getAsJsonArray();
			rolesArray.forEach(e -> roleNames.add(e.getAsString()));
			if (roleNames.size() == 0) {
				throw new JWTVerificationException("Missing roles claim");
			}
			
			//keycloak groups mapping to iDempiere org (AD_Org.Value)
			Claim groupsClaim = decodedJwt.getClaim("groups");
			if (groupsClaim.isNull() || groupsClaim.isMissing()) {
				throw new JWTVerificationException("Missing organization claim");
			}
			String[] groupNames = groupsClaim.asArray(String.class);
			List<MOrg> orgList = new ArrayList<>();
			for(String groupName : groupNames) {				
				if (groupName.startsWith("/")) 
					groupName = groupName.substring(1);
				Query query = new Query(Env.getCtx(), MOrg.Table_Name, "AD_Client_ID=? AND Value=?", null);
				MOrg org = query.setOnlyActiveRecords(true).setParameters(AD_Client_ID, groupName).first();
				if (org != null) {
					orgList.add(org);
				}				
			}
			if (orgList.size() == 0) {
				throw new JWTVerificationException("Missing organization claim");
			}
			
			if (orgList.size() == 1) {
				if (Util.isEmpty(orgHeader) || orgHeader.equals(orgList.get(0).getValue())) {
					AD_Org_ID = orgList.get(0).get_ID();
				} else {
					throw new JWTVerificationException("Invalid %s header".formatted(MOIDCService.ORG_HEADER));
				}
			} else {			
				if (!Util.isEmpty(orgHeader)) {
					for(int i = 0; i < orgList.size(); i++) {
						if (orgHeader.equals(orgList.get(i).getValue())) {
							AD_Org_ID = orgList.get(i).get_ID();
							break;
						}
					}
					if (AD_Org_ID == -1) {
						throw new JWTVerificationException("Invalid %s header".formatted(MOIDCService.ORG_HEADER));
					}
				} 
			}
			
			//keycloaks roles mapping to iDempiere role
			String roleName = null;
			if (roleNames.size() == 1) {
				roleName = roleNames.get(0);
				if (!Util.isEmpty(roleHeader) && !roleHeader.equals(roleName)) {
					throw new JWTVerificationException("Invalid %s header".formatted(MOIDCService.ROLE_HEADER));
				}
			} else {			
				if (!Util.isEmpty(roleHeader)) {
					Optional<String> optional = roleNames.stream().filter(e -> roleHeader.equals(e)).findFirst();
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
				Query query = new Query(Env.getCtx(), MOrg.Table_Name, "AD_Client_ID=? AND Value=?", null);
				MOrg org = query.setOnlyActiveRecords(true).setParameters(AD_Client_ID, orgHeader).first();
				if (org != null) {
					AD_Org_ID = org.getAD_Org_ID();
				}
				if (AD_Org_ID == -1)
					throw new JWTVerificationException("Invalid %s header".formatted(MOIDCService.ORG_HEADER));
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
		String userId = decodedJwt.getClaim(useEmail ? "email" : "name").asString();
		if (Util.isEmpty(userId)) {
			throw new JWTVerificationException("Missing user claim");
		}				
		Query query = new Query(Env.getCtx(), MUser.Table_Name, "AD_Client_ID=? AND %s=?".formatted((useEmail ? "Email" : "Name")), null);
		MUser user = query.setOnlyActiveRecords(true).setParameters(AD_Client_ID, userId).first();		
		if (user == null) {
			throw new JWTVerificationException("Invalid user claim");
		}
		AD_User_ID = user.get_ID();
		
		boolean orgAccessValidated = false;
		if (AD_Role_ID == -1) {
			//get role from http header or if no http header but user has single role only 
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
		
		//get org from http header or if role has access to single org only
		if (AD_Role_ID >= 0) {
			if (AD_Org_ID == -1) {
				if (!Util.isEmpty(headerOrgValue)) {
					query = new Query(Env.getCtx(), MOrg.Table_Name, "AD_Client_ID=? AND Value=?", null);
					MOrg org = query.setOnlyActiveRecords(true).setParameters(AD_Client_ID, headerOrgValue).first();
					if (org != null) {
						AD_Org_ID = org.getAD_Org_ID();
					} else {
						throw new JWTVerificationException("Invalid %s header".formatted(MOIDCService.ORG_HEADER));
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
			if (!mRole.isOrgAccess(AD_Org_ID, false)) {
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

	/**
	 * @param AD_Client_ID
	 * @param AD_Role_ID
	 * @param user
	 * @return AD_Role_ID if user+AD_Role_ID has only 1 org, otherwise -1
	 */
	private int getSingleOrgIdOnly(int AD_Client_ID, int AD_Role_ID, MUser user) {
		Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, AD_Client_ID);
		MRole role = new MRole(Env.getCtx(), AD_Role_ID, null);
		role.setAD_User_ID(user.get_ID());
		MOrg[] orgs = MOrg.getOfClient(AD_Client_ID);
		int AD_Org_ID = -1;
		for(MOrg org : orgs) {
			if (role.isOrgAccess(org.getAD_Org_ID(), false)) {
				if (AD_Org_ID >= 0)
					return -1;
				AD_Org_ID = org.getAD_Org_ID();
			}
		}
		return AD_Org_ID;
	}

	/**
	 * @param AD_Client_ID
	 * @param user
	 * @return AD_Role_ID if user has only 1 role, otherwise -1
	 */
	private int getSingleRoleIDOnly(int AD_Client_ID, MUser user) {
		StringBuilder sql = new StringBuilder("SELECT AD_Role_ID FROM AD_User_Roles WHERE IsActive='Y' AND AD_Client_ID=? AND AD_User_ID=?");
		try (PreparedStatement stmt = DB.prepareStatement(sql.toString(), (String)null)) {
			stmt.setInt(1, AD_Client_ID);
			stmt.setInt(2, user.get_ID());
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				int AD_Role_ID = rs.getInt(1);
				if (!rs.next())
					return AD_Role_ID;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}

}

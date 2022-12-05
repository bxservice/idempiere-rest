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
package com.trekglobal.idempiere.rest.api.v1.auth.impl;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.DatatypeConverter;

import org.adempiere.util.LogAuthFailure;
import org.compiere.model.I_AD_Preference;
import org.compiere.model.MClient;
import org.compiere.model.MOrg;
import org.compiere.model.MPreference;
import org.compiere.model.MRole;
import org.compiere.model.MSession;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Language;
import org.compiere.util.Login;
import org.compiere.util.Msg;
import org.compiere.util.Util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.v1.auth.AuthService;
import com.trekglobal.idempiere.rest.api.v1.auth.LoginCredential;
import com.trekglobal.idempiere.rest.api.v1.auth.LoginParameters;
import com.trekglobal.idempiere.rest.api.v1.auth.filter.RequestFilter;
import com.trekglobal.idempiere.rest.api.v1.jwt.LoginClaims;
import com.trekglobal.idempiere.rest.api.v1.jwt.TokenUtils;

/**
 * @author hengsin
 *
 */
public class AuthServiceImpl implements AuthService {

	private static LogAuthFailure logAuthFailure = new LogAuthFailure();

	public static final String ROLE_TYPES_WEBSERVICE = "NULL,WS";  //webservice+null

	private @Context HttpServletRequest request = null;

	/**
	 * default constructor
	 */
	public AuthServiceImpl() {
	}

	/* (non-Javadoc)
	 * @see org.idempiere.rest.api.v1.AuthService#login(org.idempiere.rest.api.v1.LoginCredential)
	 */
	@Override
	public Response authenticate(LoginCredential credential) {
		Login login = new Login(Env.getCtx());
		KeyNamePair[] clients = login.getClients(credential.getUserName(), credential.getPassword(), ROLE_TYPES_WEBSERVICE);
		if (clients == null || clients.length == 0) {
        	String loginErrMsg = login.getLoginErrMsg();
        	return unauthorized(loginErrMsg, credential.getUserName());
		} else {
			JsonArray clientNodes = new JsonArray();
			StringBuilder clientsSB = new StringBuilder();
			for(KeyNamePair client : clients) {
				JsonObject node = new JsonObject();
				node.addProperty("id", client.getKey());
				node.addProperty("name", client.getName());
				clientNodes.add(node);
				if (clientsSB.length() > 0)
					clientsSB.append(",");
				clientsSB.append(client.getKey());
			}
			if (credential.getParameters() != null) {
				LoginParameters parameters = credential.getParameters();
				String userName = credential.getUserName();
				Env.setContext(Env.getCtx(), RequestFilter.LOGIN_NAME, userName);
				return processLoginParameters(parameters, userName, clientsSB.toString());
			}
			JsonObject responseNode = new JsonObject();
			responseNode.add("clients", clientNodes);
			Builder builder = JWT.create()
					.withSubject(credential.getUserName())
					.withClaim(LoginClaims.Clients.name(), clientsSB.toString());
			Timestamp expiresAt = TokenUtils.getTokenExpiresAt();
			builder.withIssuer(TokenUtils.getTokenIssuer()).withExpiresAt(expiresAt).withKeyId(TokenUtils.getTokenKeyId());
			try {
				String token = builder.sign(Algorithm.HMAC512(TokenUtils.getTokenSecret()));
				responseNode.addProperty("token", token);
			} catch (IllegalArgumentException | JWTCreationException | UnsupportedEncodingException e) {
				e.printStackTrace();
				return Response.status(Status.BAD_REQUEST).build();
			}
			return Response.ok(responseNode.toString()).build();
		}
	}

	/**
	 * @param loginErrMsg
	 * @return
	 */
	private Response unauthorized(String loginErrMsg, String userName) {
    	if (Util.isEmpty(loginErrMsg))
    		loginErrMsg = Msg.getMsg(Env.getCtx(),"FailedLogin", true);
    	String x_Forward_IP = request.getHeader("X-Forwarded-For");
        if (x_Forward_IP == null) {
        	 x_Forward_IP = request.getRemoteAddr();
        }
    	logAuthFailure.log(x_Forward_IP, "/api", userName, loginErrMsg);

		return Response.status(Status.UNAUTHORIZED)
				.entity(new ErrorBuilder().status(Status.UNAUTHORIZED).title("Authenticate error").append(loginErrMsg).build().toString())
				.build();
	}

	/* (non-Javadoc)
	 * @see org.idempiere.rest.api.v1.AuthService#getRoles(int)
	 */
	@Override
	public Response getRoles(int clientId) {
		try {
			String userName = Env.getContext(Env.getCtx(), RequestFilter.LOGIN_NAME);
			MClient client = MClient.get(Env.getCtx(), clientId);
			String clients = Env.getContext(Env.getCtx(), RequestFilter.LOGIN_CLIENTS);
			boolean isValidClient = isValidClient(clientId, clients);
			if (!isValidClient)
				return unauthorized("Invalid client", userName);
			KeyNamePair knp = new KeyNamePair(client.getAD_Client_ID(), client.getName());
			Login login = new Login(Env.getCtx());
			KeyNamePair[] roles = login.getRoles(userName, knp, ROLE_TYPES_WEBSERVICE);
			JsonArray array = new JsonArray();
			if (roles != null) {
				for(KeyNamePair role : roles) {
					JsonObject node = new JsonObject();
					node.addProperty("id", role.getKey());
					node.addProperty("name", role.getName());
					array.add(node);
				}
			}
			JsonObject json = new JsonObject();
			json.add("roles", array);
			return Response.ok(json.toString()).build();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	/* (non-Javadoc)
	 * @see org.idempiere.rest.api.v1.AuthService#getOrganizations(int, int)
	 */
	@Override
	public Response getOrganizations(int clientId, int roleId) {
		try {
			String userName = Env.getContext(Env.getCtx(), RequestFilter.LOGIN_NAME);
			MClient client = MClient.get(Env.getCtx(), clientId);
			String clients = Env.getContext(Env.getCtx(), RequestFilter.LOGIN_CLIENTS);
			boolean isValidClient = isValidClient(clientId, clients);
			if (!isValidClient)
				return unauthorized("Invalid client", userName);
			KeyNamePair clientKeyNamePair = new KeyNamePair(client.getAD_Client_ID(), client.getName());
			Login login = new Login(Env.getCtx());
			KeyNamePair[] roles = login.getRoles(userName, clientKeyNamePair, ROLE_TYPES_WEBSERVICE);
			boolean isValidRole = isValidRole(roleId, roles);
			if (!isValidRole)
				return unauthorized("Invalid role", userName);
			Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, client.getAD_Client_ID());
			MUser user = MUser.get(Env.getCtx(), userName);
			Env.setContext(Env.getCtx(), Env.AD_USER_ID, user.getAD_User_ID());
			MRole role = MRole.get(Env.getCtx(), roleId);
			KeyNamePair knp = new KeyNamePair(role.getAD_Role_ID(), role.getName());
			KeyNamePair[] orgs = login.getOrgs(knp);
			JsonArray array = new JsonArray();
			if (orgs != null) {
				for(KeyNamePair org : orgs) {
					JsonObject node = new JsonObject();
					node.addProperty("id", org.getKey());
					node.addProperty("name", org.getName());
					array.add(node);
				}
			}
			JsonObject json = new JsonObject();
			json.add("organizations", array);
			return Response.ok(json.toString()).build();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	/* (non-Javadoc)
	 * @see org.idempiere.rest.api.v1.AuthService#getWarehouses(int, int, int)
	 */
	@Override
	public Response getWarehouses(int clientId, int roleId, int organizationId) {
		try {
			String userName = Env.getContext(Env.getCtx(), RequestFilter.LOGIN_NAME);
			MClient client = MClient.get(Env.getCtx(), clientId);
			String clients = Env.getContext(Env.getCtx(), RequestFilter.LOGIN_CLIENTS);
			boolean isValidClient = isValidClient(clientId, clients);
			if (!isValidClient)
				return unauthorized("Invalid client", userName);
			KeyNamePair clientKeyNamePair = new KeyNamePair(client.getAD_Client_ID(), client.getName());
			Login login = new Login(Env.getCtx());
			KeyNamePair[] roles = login.getRoles(userName, clientKeyNamePair, ROLE_TYPES_WEBSERVICE);
			boolean isValidRole = isValidRole(roleId, roles);
			if (!isValidRole)
				return unauthorized("Invalid role", userName);
			boolean isValidOrg = isValidOrg(organizationId, roleId, login);
			if (!isValidOrg)
				return unauthorized("Invalid organization", userName);
			Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, client.getAD_Client_ID());
			MUser user = MUser.get(Env.getCtx(), userName);
			Env.setContext(Env.getCtx(), Env.AD_USER_ID, user.getAD_User_ID());
			MRole role = MRole.get(Env.getCtx(), roleId);
			Env.setContext(Env.getCtx(), Env.AD_ROLE_ID, role.getAD_Role_ID());
			MOrg org = MOrg.get(organizationId);
			KeyNamePair knp = new KeyNamePair(org.getAD_Org_ID(), org.getName());
			KeyNamePair[] warehouses = login.getWarehouses(knp);
			JsonArray array = new JsonArray();
			if (warehouses != null) {
				for(KeyNamePair warehouse : warehouses) {
					JsonObject node = new JsonObject();
					node.addProperty("id", warehouse.getKey());
					node.addProperty("name", warehouse.getName());
					array.add(node);
				}
			}
			JsonObject json = new JsonObject();
			json.add("warehouses", array);
			return Response.ok(json.toString()).build();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	/* (non-Javadoc)
	 * @see org.idempiere.rest.api.v1.AuthService#getWarehouses(int, int, int)
	 */
	@Override
	public Response getClientLanguage(int clientId) {
		try {
			MClient client = MClient.get(Env.getCtx(), clientId);
			JsonObject node = new JsonObject();
			node.addProperty("AD_Language", client.getAD_Language());
			return Response.ok(node.toString()).build();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	/* (non-Javadoc)
	 * @see org.idempiere.rest.api.v1.AuthService#updateLoginParameters(org.idempiere.rest.api.v1.LoginParameters)
	 */
	@Override
	public Response changeLoginParameters(LoginParameters parameters) {
		String userName = Env.getContext(Env.getCtx(), RequestFilter.LOGIN_NAME);
		String clients = Env.getContext(Env.getCtx(), RequestFilter.LOGIN_CLIENTS);
		return processLoginParameters(parameters, userName, clients);
	}

	/**
	 * @param parameters
	 * @param userName
	 * @param clients
	 * @return
	 */
	private Response processLoginParameters(LoginParameters parameters, String userName, String clients) {
		JsonObject responseNode = new JsonObject();
		Builder builder = JWT.create().withSubject(userName);
		String defaultLanguage = Language.getBaseAD_Language();
		int clientId = parameters.getClientId();
		boolean isValidClient = isValidClient(clientId, clients);

		if (isValidClient) {
			builder.withClaim(LoginClaims.AD_Client_ID.name(), clientId);
			Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, clientId);
			MUser user = MUser.get(Env.getCtx(), userName);
			builder.withClaim(LoginClaims.AD_User_ID.name(), user.getAD_User_ID());
			responseNode.addProperty("userId", user.getAD_User_ID());
			defaultLanguage = getPreferenceUserLanguage(user.getAD_User_ID());

			int roleId = parameters.getRoleId();
			int orgId = parameters.getOrganizationId();
			int warehouseId = parameters.getWarehouseId();
			String errorMessage = validateLoginParameters(userName, clientId, roleId, orgId, warehouseId);

			if (Util.isEmpty(errorMessage)) {
				builder.withClaim(LoginClaims.AD_Role_ID.name(), roleId);
				builder.withClaim(LoginClaims.AD_Org_ID.name(), orgId);
				if (orgId > 0 && warehouseId > 0)
					builder.withClaim(LoginClaims.M_Warehouse_ID.name(), warehouseId);
			} else {
				return unauthorized(errorMessage, userName);
			}
		} else {
			return unauthorized("Invalid clientId", userName);
		}
		if (parameters.getLanguage() != null) {
			for (String langAllowed : Env.getLoginLanguages()) {
				if (parameters.getLanguage().equals(langAllowed)) {
					defaultLanguage = parameters.getLanguage();
					break;
				}
			}
		}

		builder.withClaim(LoginClaims.AD_Language.name(), defaultLanguage);
		responseNode.addProperty("language", defaultLanguage);

		// Create AD_Session here and set the session in the token as another parameter
		MSession session = MSession.get(Env.getCtx());
		if (session == null){
			session = MSession.create(Env.getCtx());
			session.setWebSession("idempiere-rest");
			session.saveEx();
		}
		builder.withClaim(LoginClaims.AD_Session_ID.name(), session.getAD_Session_ID());

		Timestamp expiresAt = TokenUtils.getTokenExpiresAt();
		builder.withIssuer(TokenUtils.getTokenIssuer()).withExpiresAt(expiresAt).withKeyId(TokenUtils.getTokenKeyId());
		try {
			String token = builder.sign(Algorithm.HMAC512(TokenUtils.getTokenSecret()));
			responseNode.addProperty("token", token);
		} catch (IllegalArgumentException | JWTCreationException | UnsupportedEncodingException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).build();
		}
		return Response.ok(responseNode.toString()).build();
	}

	private boolean isValidClient(int clientID, String clients) {
		if (clientID >= 0 && !Util.isEmpty(clients)) {
			for (String allowedClient : clients.split(",")) {
				if (clientID == Integer.valueOf(allowedClient)) {
					return true;
				}
			}
		}
		return false;
	}

	private String validateLoginParameters(String userName, int clientId, int roleId, int orgId, int warehouseId) {
		MClient client = MClient.get(Env.getCtx(), clientId);
		KeyNamePair clientKeyNamePair = new KeyNamePair(client.getAD_Client_ID(), client.getName());
		Login login = new Login(Env.getCtx());
		KeyNamePair[] roles = login.getRoles(userName, clientKeyNamePair, ROLE_TYPES_WEBSERVICE);
		boolean isValidRole = isValidRole(roleId, roles);

		if (isValidRole) {
			boolean isValidOrg = isValidOrg(orgId, roleId, login);
			if (isValidOrg) {
				if (orgId > 0 && warehouseId > 0) {
					boolean warehouseValid = isValidWarehouse(orgId, warehouseId, login);
					if (!warehouseValid)
						return "Invalid warehouseId";
				}
			} else {
				return "Invalid organizationId";
			}
		} else {
			return "Invalid roleId";
		}

		return "";
	}

	private boolean isValidRole(int roleId, KeyNamePair[] roles) {
		if (roleId >= 0 && roles != null) {
			for (KeyNamePair roleAllowed : roles) {
				if (roleId == roleAllowed.getKey()) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isValidOrg(int orgId, int roleId, Login login) {
		if (orgId >= 0) {
			String userName = Env.getContext(Env.getCtx(), RequestFilter.LOGIN_NAME);
			MUser user = MUser.get(Env.getCtx(), userName);
			Env.setContext(Env.getCtx(), Env.AD_USER_ID, user.getAD_User_ID());
			MRole role = MRole.get(Env.getCtx(), roleId);
			KeyNamePair rolesKeyNamePair = new KeyNamePair(role.getAD_Role_ID(), role.getName());
			KeyNamePair[] orgs = login.getOrgs(rolesKeyNamePair);
			if (orgs != null) {
				for (KeyNamePair orgAllowed : orgs) {
					if (orgId == orgAllowed.getKey()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isValidWarehouse(int orgId, int warehouseId, Login login) {
		MOrg org = MOrg.get(orgId);
		KeyNamePair orgKeyNamePair = new KeyNamePair(org.getAD_Org_ID(), org.getName());
		KeyNamePair[] warehouses = login.getWarehouses(orgKeyNamePair);
		if (warehouses != null) {
			for (KeyNamePair allowedWarehouse : warehouses) {
				if (warehouseId == allowedWarehouse.getKey()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the user preference language
	 * if non exist - returns the client language
	 * */
	private String getPreferenceUserLanguage(int AD_User_ID) {
		Query query = new Query(Env.getCtx(),
    			MTable.get(Env.getCtx(), I_AD_Preference.Table_ID),
    			" Attribute=? AND AD_User_ID=? AND PreferenceFor = 'W'",
    			null);

    	MPreference preference = query.setOnlyActiveRecords(true)
    			.setParameters("Language", AD_User_ID)
    			.first();
   
    	return preference != null ? Language.getAD_Language(preference.getValue()) : MClient.get(Env.getCtx()).getAD_Language();
	}

	/* (non-Javadoc)
	 * @see org.idempiere.rest.api.v1.AuthService#getJWK()
	 */
	@Override
	public Response getJWK() {
		JsonObject jwks = new JsonObject();
		if (MSysConfig.getBooleanValue("IDEMPIERE_REST_EXPOSE_JWK", false)) {
			JsonArray keys = new JsonArray();
			JsonObject key = new JsonObject();
			try {
				key.addProperty("alg", Algorithm.HMAC512(TokenUtils.getTokenSecret()).getName());
			} catch (IllegalArgumentException | UnsupportedEncodingException e) {
				e.printStackTrace();
				return Response.status(Status.BAD_REQUEST).build();
			}
			key.addProperty("k", DatatypeConverter.printBase64Binary(TokenUtils.getTokenSecret().getBytes()));
			key.addProperty("kid", TokenUtils.getTokenKeyId());
			key.addProperty("kty", "oct");
			keys.add(key);

			jwks.add("keys", keys);
		}

		return Response.ok(jwks.toString()).build();
	}

}

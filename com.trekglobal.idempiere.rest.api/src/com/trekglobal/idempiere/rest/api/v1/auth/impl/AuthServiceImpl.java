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
        	if (Util.isEmpty(loginErrMsg))
        		loginErrMsg = Msg.getMsg(Env.getCtx(),"FailedLogin", true);
        	String x_Forward_IP = request.getHeader("X-Forwarded-For");
            if (x_Forward_IP == null) {
            	 x_Forward_IP = request.getRemoteAddr();
            }
        	logAuthFailure.log(x_Forward_IP, "/api", credential.getUserName(), loginErrMsg);

			return Response.status(Status.UNAUTHORIZED).build();
		} else {
			JsonObject responseNode = new JsonObject();
			JsonArray clientNodes = new JsonArray(); 
			responseNode.add("clients", clientNodes);
			for(KeyNamePair client : clients) {
				JsonObject node = new JsonObject();
				node.addProperty("id", client.getKey());
				node.addProperty("name", client.getName());
				clientNodes.add(node);
			}
			Builder builder = JWT.create().withSubject(credential.getUserName());
			if (credential.getParameters() != null) {
				LoginParameters parameters = credential.getParameters();
				if (parameters.getClientId() >= 0) {
					builder.withClaim(LoginClaims.AD_Client_ID.name(), parameters.getClientId());
					if (parameters.getRoleId() > 0) {
						builder.withClaim(LoginClaims.AD_Role_ID.name(), parameters.getRoleId());
						if (parameters.getOrganizationId() >= 0) {
							builder.withClaim(LoginClaims.AD_Org_ID.name(), parameters.getOrganizationId());
						}
						if (parameters.getOrganizationId() > 0 && parameters.getWarehouseId() > 0) {
							builder.withClaim(LoginClaims.M_Warehouse_ID.name(), parameters.getOrganizationId());							
						}
					}
					Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, parameters.getClientId());
					MUser user = MUser.get(Env.getCtx(), credential.getUserName());
					builder.withClaim(LoginClaims.AD_User_ID.name(), user.getAD_User_ID());
				}
				if (parameters.getLanguage() != null) {
					builder.withClaim(LoginClaims.AD_Language.name(), parameters.getLanguage());
				}
			}
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

	/* (non-Javadoc)
	 * @see org.idempiere.rest.api.v1.AuthService#getRoles(int)
	 */
	@Override
	public Response getRoles(int clientId) {		
		try {
			String userName = Env.getContext(Env.getCtx(), RequestFilter.LOGIN_NAME);
			MClient client = MClient.get(Env.getCtx(), clientId);
			KeyNamePair knp = new KeyNamePair(client.getAD_Client_ID(), client.getName());
			Login login = new Login(Env.getCtx());
			KeyNamePair[] roles = login.getRoles(userName, knp);
			JsonArray array = new JsonArray();
			for(KeyNamePair role : roles) {
				JsonObject node = new JsonObject();
				node.addProperty("id", role.getKey());
				node.addProperty("name", role.getName());
				array.add(node);
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
			Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, client.getAD_Client_ID());
			MUser user = MUser.get(Env.getCtx(), userName);
			Env.setContext(Env.getCtx(), Env.AD_USER_ID, user.getAD_User_ID());
			MRole role = MRole.get(Env.getCtx(), roleId);
			KeyNamePair knp = new KeyNamePair(role.getAD_Role_ID(), role.getName());
			Login login = new Login(Env.getCtx());
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
			Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, client.getAD_Client_ID());
			MUser user = MUser.get(Env.getCtx(), userName);
			Env.setContext(Env.getCtx(), Env.AD_USER_ID, user.getAD_User_ID());
			MRole role = MRole.get(Env.getCtx(), roleId);
			Env.setContext(Env.getCtx(), Env.AD_ROLE_ID, role.getAD_Role_ID());
			MOrg org = new MOrg(Env.getCtx(), organizationId, null);
			KeyNamePair knp = new KeyNamePair(org.getAD_Org_ID(), org.getName());
			Login login = new Login(Env.getCtx());
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
		JsonObject responseNode = new JsonObject();
		String userName = Env.getContext(Env.getCtx(), RequestFilter.LOGIN_NAME);
		Builder builder = JWT.create().withSubject(userName);
		String defaultLanguage = Language.getBaseAD_Language();
		if (parameters.getClientId() >= 0) {
			builder.withClaim(LoginClaims.AD_Client_ID.name(), parameters.getClientId());
			if (parameters.getRoleId() >= 0) {
				builder.withClaim(LoginClaims.AD_Role_ID.name(), parameters.getRoleId());
				if (parameters.getOrganizationId() >= 0) {
					builder.withClaim(LoginClaims.AD_Org_ID.name(), parameters.getOrganizationId());
				}
				if (parameters.getOrganizationId() > 0 && parameters.getWarehouseId() > 0) {
					builder.withClaim(LoginClaims.M_Warehouse_ID.name(), parameters.getWarehouseId());							
				}
			}
			Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, parameters.getClientId());
			MUser user = MUser.get(Env.getCtx(), userName);
			builder.withClaim(LoginClaims.AD_User_ID.name(), user.getAD_User_ID());
			responseNode.addProperty("userId", user.getAD_User_ID());
			defaultLanguage = getPreferenceUserLanguage(user.getAD_User_ID());
		}
		if (parameters.getLanguage() != null) {
			defaultLanguage = parameters.getLanguage();
		}

		builder.withClaim(LoginClaims.AD_Language.name(), defaultLanguage);
		responseNode.addProperty("language", defaultLanguage);

		// Create AD_Session here and set the session in the token as another parameter
		MSession session = MSession.get(Env.getCtx(), false);
		if (session == null) {
			session = MSession.get(Env.getCtx(), true);
		}
		session.setWebSession("idempiere-rest");
		session.saveEx();
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

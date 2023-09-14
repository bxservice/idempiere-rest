/***********************************************************************
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
 **********************************************************************/

package com.trekglobal.idempiere.rest.api.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSession;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Login;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.idempiere.cache.ImmutablePOCache;
import org.idempiere.cache.ImmutablePOSupport;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.algorithms.Algorithm;
import com.trekglobal.idempiere.rest.api.v1.jwt.LoginClaims;
import com.trekglobal.idempiere.rest.api.v1.jwt.TokenUtils;

/**
 * 
 * @author matheus.marcelino
 *
 */
public class MAuthToken extends X_REST_AuthToken implements ImmutablePOSupport {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3420277395055535123L;

	/** Context Help Message Cache				*/
	private static ImmutablePOCache<String, MAuthToken> s_authtoken_cache = new ImmutablePOCache<String, MAuthToken>(Table_Name, 40);
	
	public MAuthToken(Properties ctx, int REST_AuthToken_ID, String trxName) {
		super(ctx, REST_AuthToken_ID, trxName);
	}

	public MAuthToken(Properties ctx, String REST_AuthToken_UUID, String trxName) {
		super(ctx, REST_AuthToken_UUID, trxName);
	}

	public MAuthToken(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * 
	 * @param copy
	 */
	public MAuthToken(MAuthToken copy) {
		this(Env.getCtx(), copy);
	}

	/**
	 * 
	 * @param ctx
	 * @param copy
	 */
	public MAuthToken(Properties ctx, MAuthToken copy) {
		this(ctx, copy, (String) null);
	}

	/**
	 * 
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MAuthToken(Properties ctx, MAuthToken copy, String trxName) {
		this(ctx, 0, trxName);
		copyPO(copy);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		Properties tempCtx = new Properties();
		Env.setContext(tempCtx, Env.AD_CLIENT_ID, getAD_Client_ID());

		Login login = new Login(tempCtx);
		KeyNamePair knpr = new KeyNamePair(getAD_Role_ID(), "dummy");
		KeyNamePair[] orgs = login.getOrgs(knpr);
		boolean validOrg = false;
		for (KeyNamePair org : orgs) {
			if (getAD_Org_ID() == org.getKey()) {
				validOrg = true;
				break;
			}
		}
		if (! validOrg) {
			log.saveError("Error", Msg.getMsg(getCtx(), "RoleInconsistent"));
			return false;
		}

		if (getM_Warehouse_ID() > 0) {
			KeyNamePair knpo = new KeyNamePair(getAD_Org_ID(), "dummy");
			KeyNamePair[] whs = login.getWarehouses(knpo);
			boolean validWh = false;
			for (KeyNamePair wh : whs) {
				if (getM_Warehouse_ID() == wh.getKey()) {
					validWh = true;
					break;
				}
			}
			if (! validWh) {
				log.saveError("Error", Msg.getMsg(getCtx(), "WarehouseOrgConflict"));
				return false;
			}
		}

		if (newRecord) {
			MUser u = MUser.get(getAD_User_ID());
			Builder builder = JWT.create().withSubject(u.getName());
			int clientId = getAD_Client_ID();

			builder.withClaim(LoginClaims.AD_Client_ID.name(), clientId);
			builder.withClaim(LoginClaims.AD_User_ID.name(), u.getAD_User_ID());
			builder.withClaim(LoginClaims.AD_Role_ID.name(), getAD_Role_ID());
			builder.withClaim(LoginClaims.AD_Org_ID.name(), getAD_Org_ID());
			if (getAD_Org_ID() > 0 && getM_Warehouse_ID() > 0)
				builder.withClaim(LoginClaims.M_Warehouse_ID.name(), getM_Warehouse_ID());

			builder.withClaim(LoginClaims.AD_Language.name(), getAD_Language());

			// Create AD_Session here and set the session in the token as another parameter
			Env.setContext(tempCtx, Env.AD_CLIENT_ID, getAD_Client_ID());
			Env.setContext(tempCtx, Env.LANGUAGE, getAD_Language());
			Env.setContext(tempCtx, Env.AD_ORG_ID, getAD_Org_ID());
			Env.setContext(tempCtx, Env.M_WAREHOUSE_ID, getM_Warehouse_ID());
			Env.setContext(tempCtx, Env.AD_USER_ID, getAD_User_ID());
			Env.setContext(tempCtx, "#SalesRep_ID", getAD_User_ID());
			Env.setContext(tempCtx, Env.AD_ROLE_ID, getAD_Role_ID());
			Timestamp ts = new Timestamp(System.currentTimeMillis());
			SimpleDateFormat dateFormat4Timestamp = new SimpleDateFormat("yyyy-MM-dd");
			Env.setContext(tempCtx, "#Date", dateFormat4Timestamp.format(ts)+" 00:00:00" );    //  JDBC format

			MSession session = MSession.get (tempCtx);
			if (session == null){
				log.fine("No Session found");
				session = MSession.create(tempCtx);
			}
			setAD_Session_ID(session.getAD_Session_ID());
			session.setWebSession("idempiere-rest");
			session.saveEx();

			builder.withClaim(LoginClaims.AD_Session_ID.name(), session.getAD_Session_ID());

			builder = builder.withIssuer(TokenUtils.getTokenIssuer());
			if (getExpireInMinutes() > 0) {
				Timestamp expiresAt = new Timestamp(System.currentTimeMillis());
				expiresAt = TimeUtil.addMinutess(expiresAt, getExpireInMinutes());
				builder = builder.withExpiresAt(expiresAt);
				setExpiresAt(expiresAt);
			}

			builder = builder.withKeyId(TokenUtils.getTokenKeyId());
			try {
				String token = builder.sign(Algorithm.HMAC512(TokenUtils.getTokenSecret()));
				setToken(token);
				setProcessed(true);
			} catch (Exception e) {
				throw new AdempiereException(e.getMessage());
			}
		}

		return super.beforeSave(newRecord);
	}
	
	/**
	 * Get the auth for the token (immutable)
	 * @param ctx
	 * @param token
	 * @return an immutable instance of auth token record (if any)
	 */
	public static MAuthToken get(Properties ctx, String token) {
		MAuthToken retValue = null;
		if (s_authtoken_cache.containsKey(token)) {
			retValue = s_authtoken_cache.get(ctx, token.toString(), e -> new MAuthToken(ctx, e));
			return retValue;
		}
		retValue = new Query(ctx, MAuthToken.Table_Name, "Token=?", null)
				.setParameters(token)
				.first();
		s_authtoken_cache.put(token, retValue, e -> new MAuthToken(ctx, e));
		return retValue;
	}

	public static boolean isBlocked(String token) {
		MAuthToken auth = get(Env.getCtx(), token);
		return (auth != null && (auth.isExpired() || !auth.isActive()));
	}
	
	@Override
	public PO markImmutable() {
		if (is_Immutable())
			return this;

		super.makeImmutable();
		return this;
	}

}

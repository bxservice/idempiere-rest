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
 * - Carlos Ruiz - globalqss - bx-service                              *
 **********************************************************************/

package com.trekglobal.idempiere.rest.api.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.cache.ImmutablePOCache;
import org.idempiere.cache.ImmutablePOSupport;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.trekglobal.idempiere.rest.api.v1.jwt.LoginClaims;
import com.trekglobal.idempiere.rest.api.v1.jwt.TokenUtils;

/**
 * Model class for REST_RefreshToken - temporary table to persist refresh tokens vs auth tokens
 * @author Carlos Ruiz
 */
public class MRefreshToken extends X_REST_RefreshToken implements ImmutablePOSupport {
	/**
	 * 
	 */
	private static final long serialVersionUID = 728068698206813303L;

	private static ImmutablePOCache<String, MRefreshToken> s_refreshtoken_cache_from_authtoken = new ImmutablePOCache<String, MRefreshToken>(Table_Name, 40);
	
	/**
	 * UUID constructor
	 * @param ctx
	 * @param REST_RefreshToken_UUID
	 * @param trxName
	 */
	public MRefreshToken(Properties ctx, String REST_RefreshToken_UUID, String trxName) {
		super(ctx, REST_RefreshToken_UUID, trxName);
	}

	public MRefreshToken(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Copy constructor
	 * @param copy
	 */
	public MRefreshToken(MRefreshToken copy) {
		this(Env.getCtx(), copy);
	}

	/**
	 * Copy constructor with context
	 * @param ctx
	 * @param copy
	 */
	public MRefreshToken(Properties ctx, MRefreshToken copy) {
		this(ctx, copy, (String) null);
	}

	/**
	 * Copy constructor with context and transaction
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MRefreshToken(Properties ctx, MRefreshToken copy, String trxName) {
		this(ctx, PO.UUID_NEW_RECORD, trxName);
		copyPO(copy);
	}

	/**
	 * Get a refresh token based on the RefreshToken
	 * @param refreshToken
	 * @return
	 */
	public static MRefreshToken get(String refreshToken) {
		return new Query(Env.getCtx(), Table_Name, "RefreshToken=?", null).setParameters(refreshToken).first();
	}

	/**
	 * Get a refresh token based on the auth token -> Token
	 * @param authToken
	 * @return
	 */
	public static MRefreshToken getFromToken(String authToken) {
		Properties ctx = Env.getCtx();
		MRefreshToken retValue = null;
		if (s_refreshtoken_cache_from_authtoken.containsKey(authToken)) {
			retValue = s_refreshtoken_cache_from_authtoken.get(ctx, authToken.toString(), e -> new MRefreshToken(ctx, e));
			return retValue;
		}
		retValue = new Query(Env.getCtx(), Table_Name, "Token=?", null).setParameters(authToken).first();
		s_refreshtoken_cache_from_authtoken.put(authToken, retValue, e -> new MRefreshToken(ctx, e));
		return retValue;
	}

	/**
	 * Get a refresh token based on the ParentToken
	 * @param parentToken
	 * @return
	 */
	public static MRefreshToken getFromParent(String parentToken) {
		return new Query(Env.getCtx(), Table_Name, "ParentToken=?", null).setParameters(parentToken).first();
	}

	/**
	 * Get a token that is valid for refresh (not revoked, not expired)
	 * @param refreshToken
	 * @return
	 */
	public static MRefreshToken getValidForRefresh(String refreshToken) {
		return new Query(Env.getCtx(), Table_Name, "RefreshToken=? AND RevokedAt IS NULL AND (ExpiresAt IS NULL OR ExpiresAt>=getDate()) AND (AbsoluteExpiresAt IS NULL OR AbsoluteExpiresAt>=getDate())", null)
				.setOnlyActiveRecords(true)
				.setParameters(refreshToken)
				.first();
	}

	/**
	 * Set Token.  Set client, org and user (CreatedBy) from token
	 *
	 * @param Token Token
	 */
	@Override
	public void setToken(String Token) {
		// get the clientId, orgId and UserId from the token
		try {
			Algorithm algorithm = Algorithm.HMAC512(TokenUtils.getTokenSecret());
			JWTVerifier verifier = JWT.require(algorithm)
					.withIssuer(TokenUtils.getTokenIssuer())
					.acceptExpiresAt(Instant.MAX.getEpochSecond()) // do not validate expiration of token
					.build();
			DecodedJWT jwt = verifier.verify(Token);
			int clientId = -1;
			int userId = -1;
			int orgId = -1;
			Claim claim = jwt.getClaim(LoginClaims.AD_Client_ID.name());
			if (!claim.isNull() && !claim.isMissing()) {
				clientId = claim.asInt();
			} else {
				throw new AdempiereException("Invalid token - no clientId");
			}
			setAD_Client_ID(clientId);
			claim = jwt.getClaim(LoginClaims.AD_User_ID.name());
			if (!claim.isNull() && !claim.isMissing()) {
				userId = claim.asInt();
			} else {
				throw new AdempiereException("Invalid token - no userId");
			}
			set_ValueNoCheck(COLUMNNAME_CreatedBy, userId);
			claim = jwt.getClaim(LoginClaims.AD_Org_ID.name());
			if (!claim.isNull() && !claim.isMissing()) {
				orgId = claim.asInt();
			} else {
				throw new AdempiereException("Invalid token - no orgId");
			}
			setAD_Org_ID(orgId);
		} catch (Exception e) {
			throw new AdempiereException("Invalid token -> " + e.getMessage(), e);
		}
		super.setToken(Token);
	}

	/**
	 * Verify if an auth token exists in the refresh token table
	 * @param authToken
	 * @return
	 */
	public static boolean existsAuthToken(String authToken) {
		return new Query(Env.getCtx(), Table_Name, "Token=?", null).setParameters(authToken).match();
	}

	/**
	 * Verify if the refresh token is already a parent
	 * @param refreshToken
	 * @return
	 */
	public static boolean isParent(String refreshToken) {
		return new Query(Env.getCtx(), Table_Name, "ParentToken=?", null).setParameters(refreshToken).match();
	}

	/**
	 * Revoke chain of tokens on logout
	 * @param authToken
	 */
	public static void logout(String authToken) {
		MRefreshToken rtc = MRefreshToken.getFromToken(authToken);
		MRefreshToken rt = new MRefreshToken(rtc);
		Timestamp now = new Timestamp(System.currentTimeMillis());
		rt.revoke(now, REST_REVOKECAUSE_Logout);
		rt.saveEx();
		String refreshToken = rt.getRefreshToken();
		// revoke chain of parents
		MRefreshToken childrt = getFromParent(refreshToken);
		while (childrt != null) {
			if (childrt.getRevokedAt() == null) {
				childrt.revoke(now, REST_REVOKECAUSE_Logout);
				childrt.saveEx();
			}
			childrt = getFromParent(childrt.getRefreshToken());
		}
		// revoke chain of children
		MRefreshToken parentrt = get(rt.getParentToken());
		while (parentrt != null) {
			if (parentrt.getRevokedAt() == null) {
				parentrt.revoke(now, REST_REVOKECAUSE_Logout);
				parentrt.saveEx();
			}
			parentrt = get(parentrt.getParentToken());
		}
	}

	/**
	 * Revoke chain of tokens on breach detected
	 * @param refreshToken
	 */
	public static void breachDetected(String refreshToken) {
		// Attempt to refresh a token that it was already refreshed, the whole token chain must be revoked
		MRefreshToken rt = MRefreshToken.get(refreshToken);
		Timestamp now = new Timestamp(System.currentTimeMillis());
		rt.revoke(now, REST_REVOKECAUSE_Breach);
		rt.saveEx();
		// revoke chain of parents
		MRefreshToken childrt = getFromParent(refreshToken);
		while (childrt != null) {
			childrt.revoke(now, REST_REVOKECAUSE_BreachChain);
			childrt.saveEx();
			childrt = getFromParent(childrt.getRefreshToken());
		}
		// revoke chain of children
		MRefreshToken parentrt = get(rt.getParentToken());
		while (parentrt != null) {
			parentrt.revoke(now, REST_REVOKECAUSE_BreachChain);
			parentrt.saveEx();
			parentrt = get(parentrt.getParentToken());
		}
	}

	/**
	 * Expire tokens based on a where clause
	 * @param where
	 * @param revokeCause
	 * @param params
	 * @return
	 */
	public static int expireTokens(String where, String revokeCause, ArrayList<Object> params) {
		StringBuilder whereClause = new StringBuilder("(ExpiresAt IS NULL OR ExpiresAt>=?) AND (AbsoluteExpiresAt IS NULL OR AbsoluteExpiresAt>=?) AND RevokedAt IS NULL");
		if (!Util.isEmpty(where))
			whereClause.append(" AND ").append(where);
		Timestamp now = new Timestamp(System.currentTimeMillis());
		params.add(0, now);
		params.add(0, now);
		List<MRefreshToken> rts = new Query(Env.getCtx(), Table_Name, whereClause.toString(), null)
				.setParameters(params)
				.list();
		int cnt = 0;
		for (MRefreshToken rt : rts) {
			rt.revoke(now, revokeCause);
			rt.saveEx();
			cnt++;
		}
		return cnt;
	}

	/**
	 * Expire this token
	 * @param revokedAt
	 * @param revokeCause
	 */
	public void revoke(Timestamp revokedAt, String revokeCause) {
		setRevokedAt(revokedAt);
		setREST_RevokeCause(revokeCause);
		setIsActive(false);
	}

	/**
	 * Verify if a token is revoked or expired
	 * @param token
	 * @return
	 */
	public static boolean isRevoked(String token) {
		MRefreshToken rt = getFromToken(token);
		return (   rt != null
				&& (rt.getRevokedAt() != null
				    || (   rt.getAbsoluteExpiresAt() != null
				        && rt.getAbsoluteExpiresAt().before(new Timestamp(System.currentTimeMillis())))));
	}

	@Override
	public PO markImmutable() {
		if (is_Immutable())
			return this;

		super.makeImmutable();
		return this;
	}

}

package com.trekglobal.idempiere.rest.api.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSession;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
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

	private static final long serialVersionUID = 5401044865717234931L;

	/** Context Help Message Cache				*/
	private static ImmutablePOCache<String, MAuthToken> s_authtoken_cache = new ImmutablePOCache<String, MAuthToken>(Table_Name, 40);
	
	public MAuthToken(Properties ctx, int REST_AuthToken_ID, String trxName) {
		super(ctx, REST_AuthToken_ID, trxName);
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

		if (newRecord) {
			MUser u = (MUser) getAD_User();
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
			MSession session = MSession.get(Env.getCtx());
			if (session == null) {
				session = MSession.create(Env.getCtx());
				session.setWebSession("idempiere-rest");
				session.saveEx();
			}
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

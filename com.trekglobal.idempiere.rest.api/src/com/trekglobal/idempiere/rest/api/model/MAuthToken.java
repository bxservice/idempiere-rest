package com.trekglobal.idempiere.rest.api.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSession;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;

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
public class MAuthToken extends X_REST_AuthToken {

	private static final long serialVersionUID = -894000837154529326L;
	
	/** Blocked Auth Token Cache */
	private static CCache<String,String>  s_blocked_authtoken_cache = new CCache<String, String>(null, 
			"REST_Blocked_Tokens_List", 40, 0, true);
	
	public MAuthToken(Properties ctx, int REST_AuthToken_ID, String trxName) {
		super(ctx, REST_AuthToken_ID, trxName);
	}

	public MAuthToken(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
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
		} else if (is_ValueChanged(COLUMNNAME_IsActive)) {
			manageBlockedList(getToken(), !isActive());
		}

		return super.beforeSave(newRecord);
	}
	
	private static synchronized void manageBlockedList(String token, boolean block) {
		if(block) {
			s_blocked_authtoken_cache.put(token, token);
		} else {
			s_blocked_authtoken_cache.remove(token);
		}
	}
	
	public static boolean isBlocked(String token) {
		return s_blocked_authtoken_cache.containsKey(token);
	}
	
	public static synchronized void loadBlockedTokens() {
		List<MAuthToken> blockedTokens = new Query(Env.getCtx(), MAuthToken.Table_Name, "IsExpired = 'N' AND IsActive = 'N'", null).list();
		blockedTokens.forEach(x -> MAuthToken.manageBlockedList(x.getToken(), true));
	}
}

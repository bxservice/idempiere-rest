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
 *                                                                     *
 * Contributors:                                                       *
 * - Carlos Ruiz - globalqss - bxservice                               *
 **********************************************************************/

/**
 *
 * @author Carlos Ruiz - globalqss - bxservice
 *
 */
package com.trekglobal.idempiere.rest.api.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;

import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.trekglobal.idempiere.rest.api.model.MRefreshToken;
import com.trekglobal.idempiere.rest.api.v1.jwt.LoginClaims;
import com.trekglobal.idempiere.rest.api.v1.jwt.TokenUtils;

@org.adempiere.base.annotation.Process
public class ExpireRefreshTokens extends SvrProcess {

	/* All Tokens */
	private Boolean p_REST_AllTokens = null;
	/* User/Contact */
	private int p_AD_User_ID = 0;

	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			switch (name) {
			case "REST_AllTokens":
				p_REST_AllTokens = para.getParameterAsBoolean();
				break;
			case "AD_User_ID":
				p_AD_User_ID = para.getParameterAsInt();
				break;
			default:
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
			}
		}
	}

	@Override
	protected String doIt() throws Exception {
		int cnt = 0;

		if (getAD_Client_ID() == 0 && p_REST_AllTokens) {
			cnt = DB.executeUpdateEx("DELETE FROM REST_RefreshToken", get_TrxName());
			return "@Deleted@ " + cnt;
		}

		MRefreshToken.deleteExpired(get_TrxName());

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement("SELECT Token FROM REST_RefreshToken", get_TrxName());
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String token = rs.getString(1);

				// get the clientId and UserId from the token
				try {
					Algorithm algorithm = Algorithm.HMAC512(TokenUtils.getTokenSecret());
					JWTVerifier verifier = JWT.require(algorithm)
							.withIssuer(TokenUtils.getTokenIssuer())
							.acceptExpiresAt(Instant.MAX.getEpochSecond()) // do not validate expiration of token
							.build();
					DecodedJWT jwt = verifier.verify(token);
					Claim claim = jwt.getClaim(LoginClaims.AD_Client_ID.name());
					int clientId = -1;
					int userId = -1;
					if (!claim.isNull() && !claim.isMissing()) {
						clientId = claim.asInt();
					} else {
						// invalid refresh token - delete it
						cnt = cnt + deleteToken(token);
						continue;
					}
					claim = jwt.getClaim(LoginClaims.AD_User_ID.name());
					if (!claim.isNull() && !claim.isMissing()) {
						userId = claim.asInt();
					} else {
						// invalid refresh token - delete it
						cnt = cnt + deleteToken(token);
						continue;
					}

					if (clientId == getAD_Client_ID()) {
						if (p_AD_User_ID <= 0 || userId == p_AD_User_ID) {
							cnt = cnt + deleteToken(token);
						}
					}
				} catch (Exception e) {
					// invalid refresh token - delete it
					cnt = cnt + deleteToken(token);
					continue;
				}
			}
		} finally {
			DB.close(rs, pstmt);
		}

		return "@Deleted@ " + cnt;
	}

	private int deleteToken(String token) {
		int cnt = DB.executeUpdateEx("DELETE FROM REST_RefreshToken WHERE Token=?", new Object[] {token}, get_TrxName());
		return cnt;
	}

}

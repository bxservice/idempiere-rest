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
package com.trekglobal.idempiere.rest.api.v1.jwt;

import java.sql.Timestamp;

import org.adempiere.base.Service;
import org.compiere.model.MSysConfig;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;

/**
 * 
 * @author hengsin
 *
 */
public class TokenUtils {

	private TokenUtils() {
	}

	/**
	 * 
	 * @return token secret
	 */
	public static String getTokenSecret() {
		ITokenSecretProvider provider = Service.locator().locate(ITokenSecretProvider.class).getService();
		if (provider != null) {
			return provider.getSecret();
		}
		
		if(MSysConfig.getBooleanValue("REST_USE_SYSCONFIG_SECRET", true))
			return SysConfigTokenSecretProvider.instance.getSecret();
		
		return DefaultTokenSecretProvider.instance.getSecret();
	}

	/**
	 *
	 * @return token key id
	 */
	public static String getTokenKeyId() {
		ITokenSecretProvider provider = Service.locator().locate(ITokenSecretProvider.class).getService();
		if (provider != null) {
			return provider.getKeyId();
		}
		return DefaultTokenSecretProvider.instance.getKeyId();
	}

	/**
	 * 
	 * @return issuer of token
	 */
	public static String getTokenIssuer() {
		ITokenSecretProvider provider = Service.locator().locate(ITokenSecretProvider.class).getService();
		if (provider != null) {
			return provider.getIssuer();
		}
		return DefaultTokenSecretProvider.instance.getIssuer();
	}

	/**
	 * Get the expiration of auth token based on SysConfig REST_TOKEN_EXPIRE_IN_MINUTES, defaults to 1 hour
	 * @return token expire time stamp
	 */
	public static Timestamp getTokenExpiresAt() {
		int expMinutes = MSysConfig.getIntValue("REST_TOKEN_EXPIRE_IN_MINUTES", 60, Env.getAD_Client_ID(Env.getCtx()));
		if (expMinutes == 0)
			return null;
		return TimeUtil.addMinutess(new Timestamp(System.currentTimeMillis()), expMinutes);
	}

	/**
	 * Get the absolute expiration of auth token based (session timeout) on SysConfig REST_TOKEN_ABSOLUTE_EXPIRE_IN_MINUTES, defaults to 1 week
	 * @return token absolute expire time stamp
	 */
	public static Timestamp getTokenAbsoluteExpiresAt() {
		int expMinutes = MSysConfig.getIntValue("REST_TOKEN_ABSOLUTE_EXPIRE_IN_MINUTES", 10080, Env.getAD_Client_ID(Env.getCtx()));
		if (expMinutes == 0)
			return null;
		return TimeUtil.addMinutess(new Timestamp(System.currentTimeMillis()), expMinutes);
	}

	/**
	 * Get the expiration of refresh token (inactivity timeout) based on SysConfig REST_REFRESH_TOKEN_EXPIRE_IN_MINUTES, defaults to 1 day
	 * @return refresh token expire time stamp
	 */
	public static Timestamp getRefreshTokenExpiresAt() {
		int expMinutes = MSysConfig.getIntValue("REST_REFRESH_TOKEN_EXPIRE_IN_MINUTES", 1440, Env.getAD_Client_ID(Env.getCtx()));
		if (expMinutes == 0)
			return null;
		return TimeUtil.addMinutess(new Timestamp(System.currentTimeMillis()), expMinutes);
	}

}

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
	 * 
	 * @return token expire time stamp
	 */
	public static Timestamp getTokenExpiresAt() {
		Timestamp expiresAt = new Timestamp(System.currentTimeMillis());
		expiresAt = TimeUtil.addMinutess(expiresAt, 480);
		return expiresAt;
	}
}

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

import java.util.UUID;

import org.compiere.model.MSysConfig;
import org.compiere.util.CacheMgt;
import org.compiere.util.Env;

/**
 * 
 * @author matheus.marcelino
 *
 */
public class SysConfigTokenSecretProvider implements ITokenSecretProvider {

	public static final String REST_TOKEN_SECRET = "REST_TOKEN_SECRET";

	private SysConfigTokenSecretProvider() {
		String secret = MSysConfig.getValue(REST_TOKEN_SECRET);
		if (secret == null) {
			try {
				MSysConfig.setCrossTenantSafe();
				MSysConfig sysConfig = new MSysConfig(Env.getCtx(), 0, null);
				sysConfig.set_ValueNoCheck(MSysConfig.COLUMNNAME_AD_Client_ID, 0);
				sysConfig.set_ValueNoCheck(MSysConfig.COLUMNNAME_AD_Org_ID, 0);
				sysConfig.setName(REST_TOKEN_SECRET);
				sysConfig.setValue(UUID.randomUUID().toString());
				sysConfig.saveEx();
				CacheMgt.get().reset(MSysConfig.Table_Name);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				MSysConfig.clearCrossTenantSafe();
			}
		}
	}

	@Override
	public String getSecret() {
		return MSysConfig.getValue(REST_TOKEN_SECRET);
	}

	public final static SysConfigTokenSecretProvider instance = new SysConfigTokenSecretProvider();
}

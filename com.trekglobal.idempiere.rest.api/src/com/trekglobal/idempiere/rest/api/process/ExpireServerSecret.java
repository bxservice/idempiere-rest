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

package com.trekglobal.idempiere.rest.api.process;

import java.util.UUID;

import org.compiere.model.MSysConfig;
import org.compiere.process.SvrProcess;
import org.compiere.util.CacheMgt;
import org.compiere.util.DB;

import com.trekglobal.idempiere.rest.api.model.MAuthToken;
import com.trekglobal.idempiere.rest.api.v1.jwt.SysConfigTokenSecretProvider;

/**
 * 
 * @author matheus.marcelino
 *
 */
@org.adempiere.base.annotation.Process
public class ExpireServerSecret extends SvrProcess {

	@Override
	protected void prepare() {
	}

	@Override
	protected String doIt() throws Exception {

		DB.executeUpdateEx("UPDATE AD_SysConfig SET Value = ? WHERE AD_Client_ID = ? AND AD_Org_ID = ? AND Name = ?",
				new Object[] { UUID.randomUUID().toString(), 0, 0, SysConfigTokenSecretProvider.REST_TOKEN_SECRET },
				get_TrxName());
		CacheMgt.get().reset(MSysConfig.Table_Name);
		
		DB.executeUpdateEx("UPDATE REST_AuthToken SET IsActive = 'N', IsExpired = 'Y'", get_TrxName());
		CacheMgt.get().reset(MAuthToken.Table_Name);

		return "The server secret has expired, tokens generated before the execution of this process are expired too";
	}

}

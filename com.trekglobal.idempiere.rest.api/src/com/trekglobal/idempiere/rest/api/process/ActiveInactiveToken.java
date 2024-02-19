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

import org.compiere.model.MSession;
import org.compiere.process.SvrProcess;

import com.trekglobal.idempiere.rest.api.model.MAuthToken;

/**
 * 
 * @author matheus.marcelino
 *
 */
@org.adempiere.base.annotation.Process
public class ActiveInactiveToken extends SvrProcess {

	@Override
	protected void prepare() {
	}

	@Override
	protected String doIt() throws Exception {
		
		MAuthToken token = new MAuthToken(getCtx(), getRecord_ID(), get_TrxName());
		token.setIsActive(!token.isActive());		
		token.saveEx();

		MSession sessionToken = new MSession(getCtx(), token.getAD_Session_ID(), get_TrxName());
		sessionToken.logout();

		return null;
	}

}

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
package com.trekglobal.idempiere.rest.api.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MRestViewAccess extends X_REST_View_Access {

	private static final long serialVersionUID = -4657714402555157701L;

	public MRestViewAccess(Properties ctx, int REST_View_Access_ID, String trxName) {
		super(ctx, REST_View_Access_ID, trxName);
	}

	public MRestViewAccess(Properties ctx, int REST_View_Access_ID, String trxName, String... virtualColumns) {
		super(ctx, REST_View_Access_ID, trxName, virtualColumns);
	}

	public MRestViewAccess(Properties ctx, String REST_View_Access_UU, String trxName) {
		super(ctx, REST_View_Access_UU, trxName);
	}

	public MRestViewAccess(Properties ctx, String REST_View_Access_UU, String trxName, String... virtualColumns) {
		super(ctx, REST_View_Access_UU, trxName, virtualColumns);
	}

	public MRestViewAccess(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}

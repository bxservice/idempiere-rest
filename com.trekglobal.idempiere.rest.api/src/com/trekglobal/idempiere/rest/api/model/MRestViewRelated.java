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

import org.compiere.model.PO;
import org.idempiere.cache.ImmutablePOSupport;

public class MRestViewRelated extends X_REST_ViewRelated implements ImmutablePOSupport {

	private static final long serialVersionUID = 1649842210282842151L;

	public MRestViewRelated(Properties ctx, int REST_ViewRelated_ID, String trxName) {
		super(ctx, REST_ViewRelated_ID, trxName);
	}

	public MRestViewRelated(Properties ctx, int REST_ViewRelated_ID, String trxName, String... virtualColumns) {
		super(ctx, REST_ViewRelated_ID, trxName, virtualColumns);
	}

	public MRestViewRelated(Properties ctx, String REST_ViewRelated_UU, String trxName) {
		super(ctx, REST_ViewRelated_UU, trxName);
	}

	public MRestViewRelated(Properties ctx, String REST_ViewRelated_UU, String trxName, String... virtualColumns) {
		super(ctx, REST_ViewRelated_UU, trxName, virtualColumns);
	}

	public MRestViewRelated(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	public PO markImmutable() {
		if (is_Immutable())
			return this;
		
		makeImmutable();
		return this;
	}

}

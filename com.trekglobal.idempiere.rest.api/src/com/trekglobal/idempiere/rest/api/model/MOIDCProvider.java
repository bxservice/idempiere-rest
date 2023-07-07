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

import org.adempiere.base.IServiceHolder;
import org.adempiere.base.Service;
import org.adempiere.base.ServiceQuery;

import com.trekglobal.idempiere.rest.api.oidc.IOIDCProvider;

/**
 * @author hengsin
 */
public class MOIDCProvider extends X_REST_OIDCProvider {

	private static final long serialVersionUID = -1900888949266545659L;

	/**
	 * @param ctx
	 * @param REST_OIDCProvider_ID
	 * @param trxName
	 */
	public MOIDCProvider(Properties ctx, int REST_OIDCProvider_ID, String trxName) {
		super(ctx, REST_OIDCProvider_ID, trxName);
	}

	/**
	 * @param ctx
	 * @param REST_OIDCProvider_ID
	 * @param trxName
	 * @param virtualColumns
	 */
	public MOIDCProvider(Properties ctx, int REST_OIDCProvider_ID, String trxName, String... virtualColumns) {
		super(ctx, REST_OIDCProvider_ID, trxName, virtualColumns);
	}

	/**
	 * @param ctx
	 * @param REST_OIDCProvider_UU
	 * @param trxName
	 */
	public MOIDCProvider(Properties ctx, String REST_OIDCProvider_UU, String trxName) {
		super(ctx, REST_OIDCProvider_UU, trxName);
	}

	/**
	 * @param ctx
	 * @param REST_OIDCProvider_UU
	 * @param trxName
	 * @param virtualColumns
	 */
	public MOIDCProvider(Properties ctx, String REST_OIDCProvider_UU, String trxName, String... virtualColumns) {
		super(ctx, REST_OIDCProvider_UU, trxName, virtualColumns);
	}

	/**
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MOIDCProvider(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * @return IOIDCProvider instance
	 */
	public IOIDCProvider getProvider() {
		String providerName = getName();
		ServiceQuery query = new ServiceQuery();
		query.put("name", providerName);
		IServiceHolder<IOIDCProvider> holder = Service.locator().locate(IOIDCProvider.class, query);
		return holder.getService();
	}
}

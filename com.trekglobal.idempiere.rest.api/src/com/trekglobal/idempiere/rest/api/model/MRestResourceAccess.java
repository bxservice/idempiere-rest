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

import org.compiere.model.MRole;
import org.compiere.model.MSysConfig;

public class MRestResourceAccess extends X_REST_Resource_Access {

	private static final long serialVersionUID = -6737316139847537038L;
	private static final String REST_RESOURCE_ACCESS_CONTROL = "REST_RESOURCE_ACCESS_CONTROL";

	public MRestResourceAccess(Properties ctx, int REST_Resource_Access_ID, String trxName) {
		super(ctx, REST_Resource_Access_ID, trxName);
	}

	public MRestResourceAccess(Properties ctx, int REST_Resource_Access_ID, String trxName, String... virtualColumns) {
		super(ctx, REST_Resource_Access_ID, trxName, virtualColumns);
	}

	public MRestResourceAccess(Properties ctx, String REST_Resource_Access_UU, String trxName) {
		super(ctx, REST_Resource_Access_UU, trxName);
	}

	public MRestResourceAccess(Properties ctx, String REST_Resource_Access_UU, String trxName,
			String... virtualColumns) {
		super(ctx, REST_Resource_Access_UU, trxName, virtualColumns);
	}

	public MRestResourceAccess(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Is manage resource access permission by role
	 * @return true if resource access permission is manage by role
	 */
	public static boolean isResourceAccessByRole() {
		return MSysConfig.getBooleanValue(REST_RESOURCE_ACCESS_CONTROL, true);
	}

	/**
	 * Is current role has access to path
	 * @param path
	 * @param method HTTP method of GET, POST, PUT or DELETE
	 * @return true if current role has access to path
	 */
	public static boolean hasAccess(String path, String method) {
		MRole role = MRole.getDefault();
		if (role == null)
			return false;
		MRestResource[] resources = MRestResource.getMatchResources(path);
		if (resources == null || resources.length == 0)
			return true;
		for(MRestResource resource : resources) {
			if (resource.hasAccess(role, method)) {
				return true;
			}
		}
		return false;
	}
}

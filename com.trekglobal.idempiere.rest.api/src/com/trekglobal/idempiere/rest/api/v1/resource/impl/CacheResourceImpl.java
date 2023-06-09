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
package com.trekglobal.idempiere.rest.api.v1.resource.impl;

import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.MRole;
import org.compiere.model.MUser;
import org.compiere.util.CacheInfo;
import org.compiere.util.CacheMgt;
import org.compiere.util.Env;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.v1.resource.CacheResource;

/**
 * @author hengsin
 *
 */
public class CacheResourceImpl implements CacheResource {

	public final static int PROCESS_CACHE_RESET = 205;

	/**
	 * default constructor
	 */
	public CacheResourceImpl() {
	}

	@Override
	public Response getCaches(String tableName, String name) {
		MUser user = MUser.get(Env.getCtx());
		if (!user.isAdministrator())
			return forbidden("Access denied", "Access denied for get caches request");

		List<CacheInfo> cacheInfos = CacheInfo.getCacheInfos(true);
		JsonArray caches = new JsonArray();
		for(CacheInfo cacheInfo : cacheInfos) {
			if (cacheInfo.getName().endsWith("|CCacheListener"))
				continue;
			
			if (tableName != null && !tableName.equals(cacheInfo.getTableName()))
				continue;
			
			if (name != null && !name.equals(cacheInfo.getName()))
				continue;
			
			JsonObject cache = new JsonObject();
			cache.addProperty("name", cacheInfo.getName());
			cache.addProperty("tableName", cacheInfo.getTableName());
			cache.addProperty("size", cacheInfo.getSize());
			cache.addProperty("expireMinutes", cacheInfo.getExpireMinutes());
			cache.addProperty("maxSize", cacheInfo.getMaxSize());
			cache.addProperty("distributed", cacheInfo.isDistributed());
			if (cacheInfo.getNodeId() != null)
				cache.addProperty("nodeId", cacheInfo.getNodeId());
			caches.add(cache);
		}
		JsonObject json = new JsonObject();
		json.add("caches", caches);
		return Response.ok(json.toString()).build();
	}

	@Override
	public Response resetCache(String tableName, int recordId) {
		Boolean hasAccess = MRole.getDefault().getProcessAccess(PROCESS_CACHE_RESET);
		if (hasAccess == null || !hasAccess)
			return forbidden("Access denied", "Access denied for cache reset request");

		int count = 0;
		if (tableName == null) {
			count = CacheMgt.get().reset();
		} else {
			if (recordId > 0) {
				count = CacheMgt.get().reset(tableName, recordId);
			} else {
				count = CacheMgt.get().reset(tableName);
			}
		}
		return Response.ok("{\"entriesReset\": " + count + "}").build();
	}

	private Response forbidden(String title, String detail) {
		return Response.status(Status.FORBIDDEN)
				.entity(new ErrorBuilder().status(Status.FORBIDDEN).title(title).append(detail).build().toString())
				.build();
	}

}

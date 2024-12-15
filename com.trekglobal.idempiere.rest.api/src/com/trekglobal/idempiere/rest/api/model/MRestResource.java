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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.compiere.model.MRole;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;

public class MRestResource extends X_REST_Resource implements ImmutablePOSupport {

	private static final long serialVersionUID = -3227579665231376856L;
	private static final ImmutableIntPOCache<Integer, MRestResource> s_cache = new ImmutableIntPOCache<Integer, MRestResource>(Table_Name, 100, 0);
	private static final CCache<Integer, String> s_patternCache = new CCache<Integer, String>(null, "REST_Resource_PathPatternCache", 100, false);

	public MRestResource(Properties ctx, int REST_Resource_ID, String trxName) {
		super(ctx, REST_Resource_ID, trxName);
	}

	public MRestResource(Properties ctx, int REST_Resource_ID, String trxName, String... virtualColumns) {
		super(ctx, REST_Resource_ID, trxName, virtualColumns);
	}

	public MRestResource(Properties ctx, String REST_Resource_UU, String trxName) {
		super(ctx, REST_Resource_UU, trxName);
	}

	public MRestResource(Properties ctx, String REST_Resource_UU, String trxName, String... virtualColumns) {
		super(ctx, REST_Resource_UU, trxName, virtualColumns);
	}

	public MRestResource(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Get resources that match the pass in path
	 * @param path
	 * @return matching resources
	 */
	public static MRestResource[] getMatchResources(String path) {
		if (s_cache.isEmpty()) {
			loadRestResources();
		}
		List<MRestResource> matchList = new ArrayList<MRestResource>();
		for(int id : s_patternCache.keySet()) {			
			String pathExpression = s_patternCache.get(id);
			if (Pattern.matches(pathExpression, path)) {
				MRestResource restResource = s_cache.get(id);
				if (restResource != null) {
					matchList.add(restResource);
				} else {
					MRestResource resource = new MRestResource(Env.getCtx(), id, null);
					if (resource.get_ID() == id) {
						if (resource.getREST_ResourcePath().equals(pathExpression)) {
							s_cache.put(id, resource);
							matchList.add(resource);
							restResource = resource;
						}
					}
					if (restResource == null)
						s_patternCache.remove(id);
				}
			}
		}
		return matchList.toArray(new MRestResource[0]);
	}

	/**
	 * Load REST_Resource records into cache
	 */
	private static void loadRestResources() {
		Query query = new Query(Env.getCtx(), MRestResource.Table_Name, null, null);
		List<MRestResource> resources = query.setOnlyActiveRecords(true).list();
		for(MRestResource resource : resources) {
			s_cache.put(resource.get_ID(), resource);
			s_patternCache.put(resource.get_ID(), resource.getREST_ResourcePath());
		}
		//avoid repeated loading call if REST_Resource is empty
		if (s_cache.isEmpty()) {
			s_cache.put(0, new MRestResource(Env.getCtx(), 0, null));
		}
	}

	/**
	 * Is role has access to this resource
	 * @param role
	 * @param method HTTP method of GET, POST, PUT or DELETE
	 * @return true if role has access to this resource
	 */
	public boolean hasAccess(MRole role, String method) {
		Query query = new Query(Env.getCtx(), MRestResourceAccess.Table_Name, "REST_Resource_ID=? AND AD_Role_ID=? AND REST_HttpMethods LIKE ?", null);
		return query.setOnlyActiveRecords(true).setParameters(getREST_Resource_ID(), role.getAD_Role_ID(), "%"+method+"%").count() == 1;
	}

	@Override
	public PO markImmutable() {
		if (this.is_Immutable())
			return this;
		
		this.makeImmutable();
		return this;
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (!Util.isEmpty(getREST_ResourcePath(), true)) {
			try {
				Pattern.compile(getREST_ResourcePath());
			} catch (PatternSyntaxException e) {
				CLogger.getCLogger(getClass()).saveError("Error", "Invalid regular expression syntax. " + e.getLocalizedMessage(), e);
				return false;
			}
		}
		return true;
	}
}

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
package com.trekglobal.idempiere.rest.api.v1.resource;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * 
 * @author hengsin
 *
 */
@Path("v1/caches")
public interface CacheResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * get cache infos
	 * @param tableName
	 * @param name
	 * @return cache info array
	 */
	public Response getCaches(@QueryParam("table_name") String tableName, @QueryParam("name") String name);
	
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * clear/reset cache
	 * @param tableName optional table name filter
	 * @param recordId optional record id filter (must use together with tableName)
	 * @return entries reset
	 */
	public Response resetCache(@QueryParam("table_name") String tableName, @DefaultValue("0") @QueryParam("record_id") int recordId);
}

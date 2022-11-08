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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.trekglobal.idempiere.rest.api.json.QueryOperators;

/**
 * 
 * @author hengsin
 *
 */
@Path("v1/infos")
public interface InfoResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get infowindows available
	 * @param filter optional where clause
	 * @return json array of infowindows
	 */
	public Response getInfoWindows(@QueryParam(QueryOperators.FILTER) String filter);
	
	@Path("{slug}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get records
	 * @param infoSlug infowindow name slug
	 * @param searchValue optional free text search value
	 * @param parameters optional query parameters (in json {property:value} format)
	 * @param whereClause optional whereClause
	 * @param orderBy optional order by clause
	 * @param pageNo
	 * @return json array of records
	 */
	public Response getInfoWindowRecords(@PathParam("slug") String infoSlug, @QueryParam("$parameters") String parameters, @QueryParam("$where_clause") String whereClause, 
			@QueryParam("$order_by") String orderBy, @DefaultValue("0") @QueryParam("$page_no") int pageNo);
	

	@Path("{slug}/columns")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get columns definition
	 * @param infoSlug infowindow name slug
	 * @return json array of records
	 */
	public Response getInfoWindowColumns(@PathParam("slug") String infoSlug);
	
	@Path("{slug}/processes")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get processes definition
	 * @param infoSlug infowindow name slug
	 * @return json array of records
	 */
	public Response getInfoWindowProcesses(@PathParam("slug") String infoSlug);
	
	@Path("{slug}/relateds")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get related infowindow definition
	 * @param infoSlug infowindow name slug
	 * @return json array of records
	 */
	public Response getInfoWindowRelateds(@PathParam("slug") String infoSlug);
}

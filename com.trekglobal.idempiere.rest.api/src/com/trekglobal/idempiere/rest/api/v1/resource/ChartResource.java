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
* - BX Service GmbH                                                   *
* - Carlos Ruiz                                                       *
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
 * @author Carlos Ruiz
 *
 */
@Path("v1/charts")
public interface ChartResource {

	@Path("{chartId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get the resulting image of the chart by id
	 * @param chartId
	 * @return image file, or JSON representation of chart image when the "json" parameter is present
	 */
	public Response getChartImage(@PathParam("chartId") String id, @QueryParam("width") @DefaultValue("-1") int width, @QueryParam("height") @DefaultValue("-1") int height,
			@QueryParam(QueryOperators.AS_JSON) String asJson);

	@Path("{chartId}/data")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get the resulting data of the chart by id
	 * @param chartId
	 * @return data represented as JSON
	 */
	public Response getChartData(@PathParam("chartId") String id);
	
	@Path("data")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get the resulting data of the chart(s) using the filter 
	 * @param filter
	 * @return data represented as JSON
	 */
	public Response getMultipleChartData(@QueryParam(QueryOperators.FILTER) String filter);


}

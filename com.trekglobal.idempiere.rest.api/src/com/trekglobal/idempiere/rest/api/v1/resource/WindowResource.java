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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
@Path("v1/windows")
public interface WindowResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get windows available
	 * @param filter optional where clause
	 * @return json array of windows
	 */
	public Response getWindows(@QueryParam(QueryOperators.FILTER) String filter, @QueryParam(QueryOperators.EXPAND) String details,
			@QueryParam(QueryOperators.SELECT) String select);
	
	@Path("{windowSlug}/tabs")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get tabs for a window
	 * @param windowSlug slug of window name
	 * @return window details and list of tabs
	 */
	public Response getTabs(@PathParam("windowSlug") String windowSlug);
	
	@Path("{windowSlug}/tabs/{tabSlug}/fields")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get fields for a tab
	 * @param windowSlug slug of window name
	 * @param tabSlug slug of tab name
	 * @param filter optional where clause
	 * @return json array of tab field 
	 */
	public Response getTabFields(@PathParam("windowSlug") String windowSlug, @PathParam("tabSlug") String tabSlug, @QueryParam(QueryOperators.FILTER) String filter);
	
	@Path("{windowSlug}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get list of records for header tab
	 * @param windowSlug slug of window name
	 * @param filter optional where clause
	 * @param sortColumn optional sort column name. use ! prefix for descending sort
	 * @param pageNo
	 * @return json array of records
	 */
	public Response getWindowRecords(@PathParam("windowSlug") String windowSlug, @QueryParam(QueryOperators.FILTER) String filter, @QueryParam("$sort_column") String sortColumn, @QueryParam("$page_no") int pageNo);
	
	@Path("{windowSlug}/{recordId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get record of header tab by id
	 * @param windowSlug slug of window name
	 * @param recordId
	 * @param details optional comma separated list of child tabs to retrieve
	 * @return json representation of record
	 */
	public Response getWindowRecord(@PathParam("windowSlug") String windowSlug, @PathParam("recordId") int recordId, @QueryParam(QueryOperators.EXPAND) String details);
	
	@Path("{windowSlug}/tabs/{tabSlug}/{recordId}/{childTabSlug}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get records of child tab
	 * @param windowSlug slug of window name
	 * @param tabSlug slug of parent tab name
	 * @param recordId id of parent record
	 * @param childTabSlug slug of child tab name
	 * @param filter optional where clause for child tab
	 * @param sortColumn optional sort column for child tab (! prefix for descending sort)
	 * @param pageNo
	 * @return json array of child tab records
	 */
	public Response getChildTabRecords(@PathParam("windowSlug") String windowSlug, @PathParam("tabSlug") String tabSlug, @PathParam("recordId") int recordId, 
			@PathParam("childTabSlug") String childTabSlug, @QueryParam(QueryOperators.FILTER) String filter, @QueryParam("$sort_column") String sortColumn, @QueryParam("$page_no") int pageNo);

	@Path("{windowSlug}/tabs/{tabSlug}/{recordId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get record by id
	 * @param windowSlug slug of window name
	 * @param tabSlug slug of tab name
	 * @param recordId
	 * @param details optional comma separated list of child tabs to retrieve
	 * @return json representation of record
	 */
	public Response getTabRecord(@PathParam("windowSlug") String windowSlug, @PathParam("tabSlug") String tabSlug, @PathParam("recordId") int recordId, @QueryParam(QueryOperators.EXPAND) String details);
	
	@Path("{windowSlug}/{recordId}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * update record of header tab.
	 * predefine property:
	 *   doc-action (document action to execute)
	 * @param windowSlug slug of window name
	 * @param recordId
	 * @param jsonText json representation of data to process
	 * @return json representation of updated record
	 */
	public Response updateWindowRecord(@PathParam("windowSlug") String windowSlug, @PathParam("recordId") int recordId, String jsonText);
	
	@Path("{windowSlug}")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Create new record for header tab.
	 * predefine property:
	 *   doc-action (document action to execute)
	 * @param windowSlug slug of window name
	 * @param jsonText json representation of data to process
	 * @return json representation of created record
	 */
	public Response createWindowRecord(@PathParam("windowSlug") String windowSlug, String jsonText);
	
	@Path("{windowSlug}/{recordId}")
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * delete record of header tab
	 * @param windowSlug slug of window name
	 * @param recordId
	 * @return http response
	 */
	public Response deleteWindowRecord(@PathParam("windowSlug") String windowSlug, @PathParam("recordId") int recordId);
	
	@Path("{windowSlug}/tabs/{tabSlug}/{recordId}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * update record of a tab.
	 * predefine property:
	 *   doc-action (document action to execute)
	 * @param windowSlug slug of window name
	 * @param tabSlug slug of tab name
	 * @param recordId
	 * @param jsonText json representation of data to process
	 * @return json representation of updated record
	 */
	public Response updateTabRecord(@PathParam("windowSlug") String windowSlug, @PathParam("tabSlug") String tabSlug, @PathParam("recordId") int recordId, String jsonText);
	
	@Path("{windowSlug}/tabs/{tabSlug}/{recordId}/{childTabSlug}")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Create child tab record
	 * @param windowSlug slug of window name
	 * @param tabSlug slug of parent tab name
	 * @param recordId id of parent record
	 * @param childTabSlug slug of child tab name
	 * @param jsonText json representation of created record
	 * @return json representation of created record
	 */
	public Response createChildTabRecord(@PathParam("windowSlug") String windowSlug, @PathParam("tabSlug") String tabSlug, @PathParam("recordId") int recordId, 
			@PathParam("childTabSlug") String childTabSlug, String jsonText);
	
	@Path("{windowSlug}/tabs/{tabSlug}/{recordId}")
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * delete tab record
	 * @param windowSlug slug of window name
	 * @param tabSlug slug of tab name
	 * @param recordId
	 * @return http response
	 */
	public Response deleteTabRecord(@PathParam("windowSlug") String windowSlug, @PathParam("tabSlug") String tabSlug, @PathParam("recordId") int recordId);
	
	@Path("{windowSlug}/{recordId}/print")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Print record of header tab by id
	 * @param windowSlug slug of window name
	 * @param recordId
	 * @param reportType print output type
	 * @return json representation of record
	 */
	public Response printWindowRecord(@PathParam("windowSlug") String windowSlug, @PathParam("recordId") int recordId, @QueryParam("$report_type") String reportType);
	
	@Path("{windowSlug}/tabs/{tabSlug}/{recordId}/print")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Print tab record by id
	 * @param windowSlug slug of window name
	 * @param tabSlug slug of tab name
	 * @param recordId
	 * @param reportType print output type
	 * @return json representation of record
	 */
	public Response printTabRecord(@PathParam("windowSlug") String windowSlug, @PathParam("tabSlug") String tabSlug, @PathParam("recordId") int recordId, @QueryParam("$report_type") String reportType);
}


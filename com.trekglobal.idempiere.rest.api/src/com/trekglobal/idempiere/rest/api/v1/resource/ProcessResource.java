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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * 
 * @author hengsin
 *
 */
@Path("v1/processes")
public interface ProcessResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get available processes
	 * @param filter
	 * @return json array of process
	 */
	public Response getProcesses(@QueryParam("filter") String filter);
	
	@Path("{processSlug}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get details of a process by slug
	 * @param processSlug slug of process value/search key
	 * @return json representation of process and process param
	 */
	public Response getProcess(@PathParam("processSlug") String processSlug);
	
	@Path("{processSlug}")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * run process
	 * predefine property:
	 *   record-id 
	 *   table-id (ad_table_id)
	 *   model-name (tablename)
	 *   report-type (HTML, CSV, PDF or XLS)
	 *   is-summary (true/false)
	 *   print-format-id (ad_printformat_id)
	 * note: use one of table-id or model-name.
	 * @param processSlug
	 * @param jsonText process parameters and process info setting
	 * @return json representation of process info
	 */
	public Response runProcess(@PathParam("processSlug") String processSlug, String jsonText);
	
	@Path("jobs")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * get list of active jobs
	 * @return json array of active jobs 
	 */
	public Response getJobs();
	
	@Path("jobs/{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * get job details by id(ad_pinstance_id)
	 * @param id
	 * @return job details
	 */
	public Response getJob(@PathParam("id") int id);
	
	@Path("jobs/{processSlug}")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * run process as background job.
	 * predefine property:
	 *   notification-type (E, N or B)
	 *   + the list for runProcess
	 * @param processSlug
	 * @param jsonText process parameters and job settings
	 * @return job details
	 */
	public Response runJob(@PathParam("processSlug") String processSlug, String jsonText);
}

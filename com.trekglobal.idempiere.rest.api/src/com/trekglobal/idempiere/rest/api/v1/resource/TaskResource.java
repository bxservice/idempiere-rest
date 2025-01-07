/***********************************************************************
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
 * - Carlos Ruiz - globalqss - bxservice                               *
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

import com.trekglobal.idempiere.rest.api.json.QueryOperators;

/**
 *
 * @author Carlos Ruiz - globalqss - bxservice
 *
 */
@Path("v1/tasks")
public interface TaskResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get available tasks
	 * @param filter
	 * @return json array of task
	 */
	public Response getTasks(@QueryParam(QueryOperators.FILTER) String filter);

	@Path("{taskSlug}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get details of a task by slug
	 * @param taskSlug slug of task name
	 * @return json representation of task
	 */
	public Response getTask(@PathParam("taskSlug") String taskSlug);

	@Path("{taskSlug}")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * run task
	 * @param taskSlug
	 * @param jsonText task parameters and task info setting
	 * @return json representation of task info
	 */
	public Response runTask(@PathParam("taskSlug") String taskSlug, String jsonText);

}

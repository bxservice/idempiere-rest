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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * 
 * @author hengsin
 *
 */
@Path("v1/servers")
public interface ServerResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServers();
	
	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServer(@PathParam("id") String id);
	
	@GET
	@Path("{id}/logs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServerLogs(@PathParam("id") String id);
	
	@POST
	@Path("{id}/state")
	@Produces(MediaType.APPLICATION_JSON)
	public Response changeServerState(@PathParam("id") String id);
	
	@POST
	@Path("{id}/run")
	@Produces(MediaType.APPLICATION_JSON)
	public Response runServer(@PathParam("id") String id);
	
	@POST
	@Path("reload")
	@Produces(MediaType.APPLICATION_JSON)
	public Response reloadServers();
	
	@GET
	@Path("schedulers/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getScheduler(@PathParam("id") int id);
	
	@POST
	@Path("schedulers/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response addScheduler(@PathParam("id") int id);
	
	@DELETE
	@Path("schedulers/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response removeScheduler(@PathParam("id") int id);
}

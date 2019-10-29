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
@Path("v1/nodes")
public interface NodeResource {

	public static final String LOCAL_ID="local";
	
	public static final String CURRENT_FILE_NAME = "current";
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNodes();
	
	@Path("{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNodeInfo(@PathParam("id") String id);

	@Path("{id}/logs")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNodeLogs(@PathParam("id") String id);
	
	@Path("{id}/logs/{fileName}")
	@GET
	@Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN})
	public Response getNodeLogFile(@PathParam("id") String id, @PathParam("fileName") String fileName);
	
	@Path("{id}/logs")
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteNodeLogs(@PathParam("id") String id);
	
	@Path("{id}/logs/rotate")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response rotateNodeLogs(@PathParam("id") String id);
}

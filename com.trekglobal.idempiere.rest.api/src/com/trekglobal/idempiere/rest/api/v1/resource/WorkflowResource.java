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
* - Diego Ruiz                                                        *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.v1.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("v1/workflow")
public interface WorkflowResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get workflow nodes
	 * @return json representation of all the suspended, non-processed nodes from the current user
	 */
	public Response getNodes();
	
	@Path("{userid}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get workflow nodes by id/uuid
	 * @return json representation of all the suspended, non-processed nodes from the corresponding user
	 */
	public Response getNodes(@PathParam("userid") String id);
	
	@Path("approve/{id}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * approves node
	 * @param json body containing nodeId and a msg optionally
	 * @return json representation updated node
	 */
	public Response approve(@PathParam("id") String nodeId, String jsonText);
	
	@Path("reject/{id}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * rejects node
	 * @param json body containing nodeId and a msg optionally
	 * @return json representation updated node
	 */
	public Response reject(@PathParam("id") String nodeId, String jsonText);
	
	@Path("forward/{id}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * forward the current node to a different user
	 * @param json body containing nodeId, an userToID and a msg optionally
	 * @return json representation updated node
	 */
	public Response forward(@PathParam("id") String nodeId, String jsonText);
	
	@Path("setuserchoice/{id}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * same logic as the approve and reject options but it can be used for other column types such as: Yes-No and Lists.
	 * @param json body containing nodeId and a msg optionally
	 * @return json representation updated node
	 */
	public Response setUserChoice(@PathParam("id") String nodeId, String jsonText);
	
	@Path("acknowledge/{id}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * a user gets notified when specific workflow nodes run, 
	 * it does not require any action from the user but the user receives a message after a specific action is performed.
	 * This is used when the node is from type Window/form
	 * @param json body containing nodeId and a msg optionally
	 * @return json representation updated node
	 */
	public Response acknowledge(@PathParam("id") String nodeId, String jsonText);
}

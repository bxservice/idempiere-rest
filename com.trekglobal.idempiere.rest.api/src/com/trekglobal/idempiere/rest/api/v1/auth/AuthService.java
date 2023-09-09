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
package com.trekglobal.idempiere.rest.api.v1.auth;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("v1")
/**
 * authentication service
 * @author hengsin
 *
 */
public interface AuthService {

	@Path("auth/tokens")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get auth token with username and password
	 * @return new auth token and list of clients available
	 */
	public Response authenticate(LoginCredential credential);
	
	@Path("auth/roles")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get roles available
	 * @param clientId
	 * @return list of roles
	 */
	public Response getRoles(@QueryParam(value = "client") int clientId);
	
	@Path("auth/organizations")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get list of organizations available
	 * @param clientId
	 * @param roleId
	 * @return list of organization
	 */
	public Response getOrganizations(@QueryParam(value = "client") int clientId, 
			@QueryParam(value = "role") int roleId);
	
	@Path("auth/warehouses")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get list of warehouses available
	 * @param clientId
	 * @param roleId
	 * @param organizationId
	 * @return list of warehouse
	 */
	public Response getWarehouses(@QueryParam(value = "client") int clientId, 
			@QueryParam(value = "role") int roleId, @QueryParam(value = "organization") int organizationId);
	
	@Path("auth/language")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get default client language
	 * @param clientId
	 * @return client language
	 */
	public Response getClientLanguage(@QueryParam(value = "client") int clientId);
	
	@Path("auth/tokens")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Set/Modify login role, organization and warehouse
	 * @param token from /users/login
	 * @param loginRole
	 * @return new auth token
	 */
	public Response changeLoginParameters(LoginParameters loginRole);

	@Path("auth/jwk")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get JWK
	 * @return jwk keys
	 */
	public Response getJWK();

	@Path("auth/refresh")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get auth token with a refresh token
	 * @return new auth token
	 */
	public Response tokenRefresh(RefreshParameters refresh);

}

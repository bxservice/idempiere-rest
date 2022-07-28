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
**********************************************************************/
package com.trekglobal.idempiere.rest.api.v1.resource.impl;

import javax.ws.rs.core.Response;

import org.compiere.util.DB;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.v1.resource.HealthResource;

/**
 * 
 * @author Anozi Mada
 *
 */
public class HealthResourceImpl implements HealthResource {
	
	public HealthResourceImpl() {
	}

	@Override
	public Response health() {
		JsonObject databaseCheck = new JsonObject();
		databaseCheck.addProperty("name", "Database connection health check");
		databaseCheck.addProperty("status", DB.isConnected() ? "UP" : "DOWN");
		
		JsonArray checks = new JsonArray();
		checks.add(databaseCheck);
		
		JsonObject json = new JsonObject();
		json.addProperty("status", "UP");
		json.add("checks", checks);
		
		return Response.ok(json.toString()).header("Cache-Control", "no-cache").build();
	}
}

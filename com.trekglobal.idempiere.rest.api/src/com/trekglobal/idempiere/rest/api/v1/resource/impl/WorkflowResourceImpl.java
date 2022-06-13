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
* - Diego Ruiz /Carlos Ruiz                                           *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.v1.resource.impl;

import javax.ws.rs.core.Response;

import com.trekglobal.idempiere.rest.api.v1.resource.WorkflowResource;

public class WorkflowResourceImpl implements WorkflowResource {

	@Override
	public Response getNodes() {
		System.out.println("return all nodes from current user");
		return null;
	}

	@Override
	public Response getNodes(String id) {
		System.out.println("return all nodes from user id = " + id);
		return null;
	}

	@Override
	public Response approve(String nodeId, String jsonText) {
		System.out.println("approve node: "+ nodeId);
		System.out.println(jsonText);
		return null;
	}
	
	@Override
	public Response reject(String nodeId, String jsonText) {
		System.out.println("Reject node: "+ nodeId);
		System.out.println(jsonText);
		return null;
	}

	@Override
	public Response forward(String nodeId, String jsonText) {
		System.out.println("forward node: "+ nodeId);
		System.out.println(jsonText);
		return null;
	}

	@Override
	public Response setUserChoice(String nodeId, String jsonText) {
		System.out.println("setuserchoice node: "+ nodeId);
		System.out.println(jsonText);
		return null;
	}

	@Override
	public Response aknowledge(String nodeId, String jsonText) {
		System.out.println("aknowledge node: "+ nodeId);
		System.out.println(jsonText);
		return null;
	}
}

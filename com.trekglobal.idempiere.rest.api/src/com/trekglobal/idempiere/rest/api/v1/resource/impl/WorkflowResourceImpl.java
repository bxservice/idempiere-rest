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

import java.util.List;

import javax.ws.rs.core.Response;

import org.compiere.model.I_AD_WF_Activity;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.compiere.wf.MWFActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.v1.resource.WorkflowResource;

public class WorkflowResourceImpl implements WorkflowResource {

	@Override
	public Response getNodes() {
		return getNodes(null);
	}

	@Override
	public Response getNodes(String userid) {

		List<MWFActivity> activities = getUserUnprocessedSuspendedActivities(userid);

		JsonArray array = new JsonArray();
		for (MWFActivity activity : activities) {
			array.add(getActivityJsonObject(activity));
		}
		JsonObject json = new JsonObject();
		json.addProperty("row-count", array.size());
		json.add("nodes", array);
		return Response.ok(json.toString()).build();
	}
	
	private List<MWFActivity> getUserUnprocessedSuspendedActivities(String userid) {
		final String whereClause = 
				"AD_WF_Activity.Processed=? AND AD_WF_Activity.WFState=? AND ("
				//	Owner of Activity
				+ " AD_WF_Activity.AD_User_ID=?"	//	#1
				//	Invoker (if no invoker = all)
				+ " OR EXISTS (SELECT * FROM AD_WF_Responsible r WHERE AD_WF_Activity.AD_WF_Responsible_ID=r.AD_WF_Responsible_ID"
				+ " AND r.ResponsibleType='H' AND COALESCE(r.AD_User_ID,0)=0 AND COALESCE(r.AD_Role_ID,0)=0 AND (AD_WF_Activity.AD_User_ID=? OR AD_WF_Activity.AD_User_ID IS NULL))"	//	#2
				//  Responsible User
				+ " OR EXISTS (SELECT * FROM AD_WF_Responsible r WHERE AD_WF_Activity.AD_WF_Responsible_ID=r.AD_WF_Responsible_ID"
				+ " AND r.ResponsibleType='H' AND r.AD_User_ID=?)"		//	#3
				//	Responsible Role
				+ " OR EXISTS (SELECT * FROM AD_WF_Responsible r INNER JOIN AD_User_Roles ur ON (r.AD_Role_ID=ur.AD_Role_ID)"
				+ " WHERE AD_WF_Activity.AD_WF_Responsible_ID=r.AD_WF_Responsible_ID AND r.ResponsibleType='R' AND ur.AD_User_ID=? AND ur.isActive = 'Y')"	//	#4
				///* Manual Responsible */ 
				+ " OR EXISTS (SELECT * FROM AD_WF_ActivityApprover r "
				+ " WHERE AD_WF_Activity.AD_WF_Activity_ID=r.AD_WF_Activity_ID AND r.AD_User_ID=? AND r.isActive = 'Y')" 
				+ ")";
		
		int AD_User_ID = getAD_User_ID(userid);
		
		return new Query(Env.getCtx(), I_AD_WF_Activity.Table_Name, whereClause, null)
				.setParameters(false, MWFActivity.WFSTATE_Suspended, AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID)
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.list();
	}
	
	//TODO handle when the UUID is sent instead of the ID
	private int getAD_User_ID(String userId) {
		return userId != null ? Integer.parseInt(userId) : Env.getAD_User_ID(Env.getCtx()); 
	}
	
	private JsonObject getActivityJsonObject(MWFActivity activity) {
		JsonObject json = new JsonObject();
		json.addProperty("id", activity.getAD_WF_Activity_ID());
		if (!Util.isEmpty(activity.getAD_WF_Activity_UU())) {
			json.addProperty("uid", activity.getAD_WF_Activity_UU());
		}
		json.addProperty("model-name", activity.get_TableName().toLowerCase());
		json.addProperty("node-name", activity.getNodeName());
		if (!Util.isEmpty(activity.getNodeDescription())) {
			json.addProperty("node-description", activity.getNodeDescription());
		}
		
		return json;
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
	public Response acknowledge(String nodeId, String jsonText) {
		System.out.println("aknowledge node: "+ nodeId);
		System.out.println(jsonText);
		return null;
	}
}

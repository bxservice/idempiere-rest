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
import java.util.logging.Level;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.MBPartner;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.compiere.wf.MWFActivity;
import org.compiere.wf.MWFProcess;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.RestUtils;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.v1.resource.WorkflowResource;

public class WorkflowResourceImpl implements WorkflowResource {

	private static final CLogger log = CLogger.getCLogger(WorkflowResourceImpl.class);

	@Override
	public Response getNodes() {
		return getNodes(null);
	}

	@Override
	public Response getNodes(String userId) {

		int AD_User_ID = getAD_User_ID(userId);
		if (AD_User_ID <= 0)
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("User not found").append("No user found matching id ").append(userId).build().toString())
					.build();

		List<MWFActivity> activities = getUserPendingActivities(AD_User_ID);

		JsonArray array = new JsonArray();
		for (MWFActivity activity : activities) {
			array.add(getActivityJsonObject(activity));
		}
		JsonObject json = new JsonObject();
		json.addProperty("row-count", array.size());
		json.add("nodes", array);
		return Response.ok(json.toString()).build();
	}

	private int getAD_User_ID(String userId) {
		if (Util.isEmpty(userId))
			return Env.getAD_User_ID(Env.getCtx());

		MUser user = (MUser) RestUtils.getPO(MUser.Table_Name, userId, true, false);
		return user == null ? -1 : user.getAD_User_ID();
	}

	private List<MWFActivity> getUserPendingActivities(int AD_User_ID) {
		final String whereClause = getWhereUserPendingActivities(); // MWFActivity.getWhereUserPendingActivities();
		return new Query(Env.getCtx(), MWFActivity.Table_Name, whereClause, null)
				.setParameters(AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID, Env.getAD_Client_ID(Env.getCtx()))
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.setOrderBy("AD_WF_Activity.Priority DESC, AD_WF_Activity.Created")
				.list();
	}

	private JsonObject getActivityJsonObject(MWFActivity activity) {
		JsonObject json = new JsonObject();
		json.addProperty("id", activity.getAD_WF_Activity_ID());
		if (!Util.isEmpty(activity.getAD_WF_Activity_UU())) {
			json.addProperty("uid", activity.getAD_WF_Activity_UU());
		}
		json.addProperty("model-name", activity.get_TableName().toLowerCase());
		json.addProperty("node-name", activity.getNodeName());
		json.addProperty("priority", activity.getPriority());
		String summary = activity.getSummary();
		if (!Util.isEmpty(summary, true))
			json.addProperty("summary", summary);
		String nodeDesc = activity.getNodeDescription();
		if (!Util.isEmpty(nodeDesc, true))
			json.addProperty("node-description", nodeDesc);
		String nodeHelp = activity.getNodeHelp();
		if (!Util.isEmpty(nodeHelp, true))
			json.addProperty("node-help", nodeHelp);
		String history = activity.getHistoryHTML();
		if (!Util.isEmpty(history, true))
			json.addProperty("history-records", history);
		int tableId = activity.getAD_Table_ID();
		if (tableId > 0)
			json.addProperty("ad_table_id", tableId);
		int recordId = activity.getRecord_ID();
		if (recordId > 0)
			json.addProperty("record_id", recordId);

		return json;
	}

	@Override
	public Response approve(String nodeId, String jsonText) {
		log.info("approve node: "+ nodeId);
		log.info(jsonText);
		return actionActivity(nodeId, jsonText, "Y", true, false);
	}

	@Override
	public Response reject(String nodeId, String jsonText) {
		log.info("Reject node: "+ nodeId);
		log.info(jsonText);
		return actionActivity(nodeId, jsonText, "N", true, false);
	}

	@Override
	public Response setUserChoice(String nodeId, String jsonText) {
		log.info("setuserchoice node: "+ nodeId);
		log.info(jsonText);
		Gson gson = new GsonBuilder().create();
		JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
		String value = getValueProperty(jsonObject);
		return actionActivity(nodeId, jsonText, value, false, false);
	}

	@Override
	public Response acknowledge(String nodeId, String jsonText) {
		log.info("acknowledge node: "+ nodeId);
		log.info(jsonText);
		return actionActivity(nodeId, jsonText, null, false, true);
	}

	/**
	 * This method actions an Activity for approve, reject, setUserChoice or Acknowledge
	 * @param nodeId - AD_WF_Activity_ID
	 * @param jsonText - optional text containing the message
	 * @param value - optional value for setUserChoice (for approve must be Y, for reject must be N)
	 * @param isApproval - is this an approval node, true for approve and reject, false for the others
	 * @param isConfirmation - is this a confirmation node, true for acknowledge
	 * @return
	 */
	private Response actionActivity(String nodeId, String jsonText, String value, boolean isApproval, boolean isConfirmation) {
		Trx trx = null;
		MWFActivity activity = null;
		try {
			trx = Trx.get(Trx.createTrxName("RWFS"), true);
			trx.setDisplayName(getClass().getName()+"_setUserChoice");
			activity = (MWFActivity) RestUtils.getPO(MWFActivity.Table_Name, nodeId, true, true);
			if (activity == null)
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Activity not found").build().toString())
						.build();
			int currentUserId = Env.getAD_User_ID(Env.getCtx());
			if (!isValidActionUser(currentUserId, activity))
				return Response.status(Status.BAD_REQUEST)
						.entity(new ErrorBuilder().status(Status.BAD_REQUEST)
								.title("The current User cannot action this Activity")
								.build().toString())
						.build();

			activity.set_TrxName(trx.getTrxName());
			if (isApproval && !activity.getNode().isUserApproval())
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Not an approval node").build().toString())
						.build();
			if (isConfirmation && !activity.getNode().isUserManual())
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Not an acknowledgment node").build().toString())
						.build();
			if ( (!isApproval && !isConfirmation) && !activity.getNode().isUserChoice())
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Not a user choice node").build().toString())
						.build();

			String textMsg = null;
			if (!Util.isEmpty(jsonText, true)) {
				Gson gson = new GsonBuilder().create();
				JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
				textMsg = getMessageProperty(jsonObject);
			}
			if (isConfirmation)
				activity.setUserConfirmation(currentUserId, textMsg);
			else
				activity.setUserChoice(currentUserId, value, activity.getNode().getColumn().getAD_Reference_ID(), textMsg);
			MWFProcess wfpr = new MWFProcess(activity.getCtx(), activity.getAD_WF_Process_ID(), activity.get_TrxName());
			wfpr.checkCloseActivities(activity.get_TrxName());
			trx.commit();
		} catch (Exception ex) {
			if (trx != null)
				trx.rollback();
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Approve error").append("Approve error with exception: ").append(ex.getMessage()).build().toString())
					.build();
		} finally {
			if (trx != null)
				trx.close();
		}
		JsonObject json = new JsonObject();
		json.addProperty("msg", Msg.getMsg(Env.getCtx(), "Updated"));
		String summary = activity.getSummary();
		if (!Util.isEmpty(summary, true))
			json.addProperty("summary", summary);
		String history = activity.getHistoryHTML();
		if (!Util.isEmpty(history, true))
			json.addProperty("history-records", history);
		return Response.ok(json.toString()).build();
	}

	@Override
	public Response forward(String nodeId, String jsonText) {
		Gson gson = new GsonBuilder().create();
		JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
		MUser user = getForwardToUser(jsonObject);
		if (user == null)
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST)
							.title("userTo property is mandatory.")
							.append("userTo property is not set or it has an invalid value").build().toString())
					.build();
		// User validation: AD_Reference_ID=286 - AD_User - Internal - Employee or SalesRep
		// EXISTS (SELECT * FROM C_BPartner bp
		//    WHERE AD_User.C_BPartner_ID=bp.C_BPartner_ID AND (bp.IsEmployee='Y' OR bp.IsSalesRep='Y'))
		MBPartner bp = null;
		if (user.getC_BPartner_ID() > 0)
			bp = MBPartner.get(Env.getCtx(), user.getC_BPartner_ID());
		if (bp == null || ! ( bp.isEmployee() || bp.isSalesRep() ))
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST)
							.title("Invalid user - not Internal")
							.build().toString())
					.build();

		Trx trx = null;
		MWFActivity activity = null;
		try {
			trx = Trx.get(Trx.createTrxName("RWFF"), true);
			trx.setDisplayName(getClass().getName()+"_forward");
			activity = (MWFActivity) RestUtils.getPO(MWFActivity.Table_Name, nodeId, true, true);
			if (activity == null)
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Activity not found").build().toString())
						.build();
			int currentUserId = Env.getAD_User_ID(Env.getCtx());
			if (!isValidActionUser(currentUserId, activity))
				return Response.status(Status.BAD_REQUEST)
						.entity(new ErrorBuilder().status(Status.BAD_REQUEST)
								.title("The current User cannot action this Activity")
								.build().toString())
						.build();

			activity.set_TrxName(trx.getTrxName());
			String textMsg = null;
			if (!Util.isEmpty(jsonText, true))
				textMsg = getMessageProperty(jsonObject);
			if (!activity.forwardTo(user.getAD_User_ID(), textMsg)) {
				trx.rollback();
				return Response.status(Status.NOT_MODIFIED)
						.entity(new ErrorBuilder().status(Status.NOT_MODIFIED).title(Msg.getMsg(Env.getCtx(), "CannotForward")).build().toString())
						.build();
			}
			trx.commit();
		} catch (Exception ex) {
			if (trx != null)
				trx.rollback();
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Forward error").append("Forward error with exception: ").append(ex.getMessage()).build().toString())
					.build();
		} finally {
			if (trx != null)
				trx.close();
		}
		JsonObject json = new JsonObject();
		json.addProperty("msg", Msg.getMsg(Env.getCtx(), "Updated"));
		String summary = activity.getSummary();
		if (!Util.isEmpty(summary, true))
			json.addProperty("summary", summary);
		String history = activity.getHistoryHTML();
		if (!Util.isEmpty(history, true))
			json.addProperty("history-records", history);
		return Response.ok(json.toString()).build();
	}

	private MUser getForwardToUser(JsonObject jsonObject) {
		JsonElement jsonElement = jsonObject.get("userTo");
		if (jsonElement == null || !jsonElement.isJsonPrimitive() || Util.isEmpty(jsonElement.getAsString(), true))
			return null;

		String userId = jsonElement.getAsString();
		return  (MUser) RestUtils.getPO(MUser.Table_Name, userId, true, false);
	}

	private String getValueProperty(JsonObject jsonObject) {
		return getStringProperty(jsonObject, "value");
	}

	private String getMessageProperty(JsonObject jsonObject) {
		return getStringProperty(jsonObject, "message");
	}

	private String getStringProperty(JsonObject jsonObject, String memberName) {
		JsonElement jsonElement = jsonObject.get(memberName);
		if (jsonElement == null || !jsonElement.isJsonPrimitive() || Util.isEmpty(jsonElement.getAsString(), true))
			return "";

		return jsonElement.getAsString();
	}

	/**
	 * Validate if a user can action an Activity
	 * @param AD_User_ID
	 * @param activity
	 * @return
	 */
	private boolean isValidActionUser(int AD_User_ID, MWFActivity activity) {
		String whereClause = getWhereUserPendingActivities(); // MWFActivity.getWhereUserPendingActivities();
		whereClause += " AND AD_WF_Activity.AD_WF_Activity_ID=?";
		int cnt = new Query(Env.getCtx(), MWFActivity.Table_Name, whereClause, null)
				.setParameters(AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID, AD_User_ID, Env.getAD_Client_ID(Env.getCtx()), activity.getAD_WF_Activity_ID())
				.setClient_ID()
				.setOnlyActiveRecords(true)
				.count();
		return (cnt == 1);
	}

	/**
	 * Where to get the pending activities related to a User (unprocessed and suspended)
	 * The where returned requires the AD_User_ID parameter 5 times, and then AD_Client_ID
	 * @return Where Clause
	 */
	private static String getWhereUserPendingActivities() {
		final String where =
			"AD_WF_Activity.Processed='N' AND AD_WF_Activity.WFState='OS' AND ("
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
			+ ") AND AD_WF_Activity.AD_Client_ID=?";	//	#5
		return where;
	}

}

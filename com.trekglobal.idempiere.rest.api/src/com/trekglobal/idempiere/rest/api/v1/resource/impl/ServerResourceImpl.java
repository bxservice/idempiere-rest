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
package com.trekglobal.idempiere.rest.api.v1.resource.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.AdempiereProcessor;
import org.compiere.model.AdempiereProcessorLog;
import org.compiere.model.MRole;
import org.compiere.model.MScheduler;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.server.AdempiereServerMgr;
import org.compiere.server.IServerManager;
import org.compiere.server.ServerInstance;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.idempiere.server.cluster.ClusterServerMgr;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.util.ClusterUtil;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.v1.resource.ServerResource;

/**
 * @author hengsin
 *
 */
public class ServerResourceImpl implements ServerResource {

	/**
	 * default constructor 
	 */
	public ServerResourceImpl() {
	}

	@Override
	public Response getServers() {
		MUser user = MUser.get(Env.getCtx());
		if (!user.isAdministrator())
			return accessDenied("", "get servers");

		IServerManager serverMgr = getServerManager();
		ServerInstance[] instances = serverMgr.getServerInstances();
		JsonArray servers = new JsonArray();
		for(ServerInstance instance : instances) {
			JsonObject server = new JsonObject();
			server.addProperty("id", instance.getServerId());
			server.addProperty("name", instance.getModel().getName());
			if (instance.getClusterMember() != null) {
				server.addProperty("nodeId", instance.getClusterMember().getId());
				server.addProperty("hostName", instance.getClusterMember().getAddress().getCanonicalHostName());
				server.addProperty("port", instance.getClusterMember().getPort());
			}
			server.addProperty("started", instance.isStarted());
			servers.add(server);
		}
		JsonObject json = new JsonObject();
		json.add("servers", servers);
		return Response.ok(json.toString()).build();
	}

	@Override
	public Response getServer(String id) {
		MUser user = MUser.get(Env.getCtx());
		if (!user.isAdministrator())
			return accessDenied(id, "get server");

		IServerManager serverMgr = getServerManager();
		ServerInstance instance = serverMgr.getServerInstance(id);
		if (instance == null) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid server Id").append("No match found for server id: ").append(id).build().toString())
					.build(); 
		}
		
		JsonObject server = new JsonObject();
		server.addProperty("id", instance.getServerId());
		server.addProperty("name", instance.getModel().getName());
		if (instance.getClusterMember() != null) {
			server.addProperty("nodeId", instance.getClusterMember().getId());
			server.addProperty("hostName", instance.getClusterMember().getAddress().getCanonicalHostName());
			server.addProperty("port", instance.getClusterMember().getPort());
		}
		if (!Util.isEmpty(instance.getModel().getDescription()))
			server.addProperty("description", instance.getModel().getDescription());
		if (instance.getModel().getDateLastRun() != null)
			server.addProperty("lastRun", DisplayType.getDateFormat(DisplayType.DateTime).format(instance.getModel().getDateLastRun()));
		server.addProperty("info", instance.getServerInfo());
		if (instance.getModel().getDateNextRun(false) != null)
			server.addProperty("nextRun", DisplayType.getDateFormat(DisplayType.DateTime).format(instance.getModel().getDateNextRun(false)));
		server.addProperty("statistics", instance.getStatistics());
		server.addProperty("started", instance.isStarted());
		server.addProperty("sleeping", instance.isSleeping());
		server.addProperty("interruptd", instance.isInterrupted());
		
		return Response.ok(server.toString()).build();
	}

	@Override
	public Response getServerLogs(String id) {
		MUser user = MUser.get(Env.getCtx());
		if (!user.isAdministrator())
			return accessDenied(id, "get server logs");

		IServerManager serverMgr = getServerManager();
		ServerInstance instance = serverMgr.getServerInstance(id);
		if (instance == null) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid server Id").append("No match found for server id: ").append(id).build().toString())
					.build(); 
		}
		AdempiereProcessorLog[] instanceLogs = instance.getModel().getLogs();
		JsonArray logs = new JsonArray();
		for(AdempiereProcessorLog instanceLog : instanceLogs) {
			JsonObject log = new JsonObject();
			log.addProperty("created", DisplayType.getDateFormat(DisplayType.DateTime).format(instanceLog.getCreated()));
			if (!Util.isEmpty(instanceLog.getSummary()))
				log.addProperty("summary", instanceLog.getSummary());
			if (!Util.isEmpty(instanceLog.getDescription()))
				log.addProperty("description", instanceLog.getDescription());
			if (!Util.isEmpty(instanceLog.getReference()))
				log.addProperty("reference", instanceLog.getReference());
			if (!Util.isEmpty(instanceLog.getTextMsg()))
				log.addProperty("textMessage", instanceLog.getTextMsg());
			log.addProperty("error", instanceLog.isError());
			logs.add(log);
		}
		
		JsonObject json = new JsonObject();
		json.add("logs", logs);
		return Response.ok(json.toString()).build();
	}

	@Override
	public Response changeServerState(String id) {
		IServerManager serverMgr = getServerManager();
		ServerInstance instance = serverMgr.getServerInstance(id);
		if (instance == null) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid server Id").append("No match found for server id: ").append(id).build().toString())
					.build(); 
		}
		
		MUser user = MUser.get(Env.getCtx());
		if (!user.isAdministrator()) {
			AdempiereProcessor model = instance.getModel();
			if (model instanceof PO) {
				PO po  = (PO) model;
				MTable table = MTable.get(Env.getCtx(), po.get_Table_ID());
				if (!hasAccess(table, true)) {
					return accessDenied(id, "change server state");
				}
				if (po.getAD_Client_ID() != Env.getAD_Client_ID(Env.getCtx())) {
					return accessDenied(id, "change server state");
				}
				MRole role = MRole.getDefault();
				if (!role.isOrgAccess(po.getAD_Org_ID(), true)) {
					return accessDenied(id, "change server state");
				}
			} else {
				return accessDenied(id, "change server state");
			}
		}
		
		String error = null;
		if (instance.isStarted()) {
			error = serverMgr.stop(id);
		} else {
			error = serverMgr.start(id);
		}
		
		if (!Util.isEmpty(error, true)) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Server error").append("Server error with exception: ").append(error).build().toString())
					.build();
		}
		
		return getServer(id);
	}

	private Response accessDenied(String id, String request) {
		return Response.status(Status.FORBIDDEN)
				.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for " + request + " request: " + id).build().toString())
				.build();
	}

	@Override
	public Response runServer(String id) {
		IServerManager serverMgr = getServerManager();
		ServerInstance instance = serverMgr.getServerInstance(id);
		if (instance == null) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid server Id").append("No match found for server id: ").append(id).build().toString())
					.build(); 
		}
		
		MUser user = MUser.get(Env.getCtx());
		if (!user.isAdministrator()) {
			AdempiereProcessor model = instance.getModel();
			if (model instanceof PO) {
				PO po  = (PO) model;
				MTable table = MTable.get(Env.getCtx(), po.get_Table_ID());
				if (!hasAccess(table, true)) {
					return accessDenied(id, "run now");
				}
				if (po.getAD_Client_ID() != Env.getAD_Client_ID(Env.getCtx())) {
					return accessDenied(id, "run now");
				}
				MRole role = MRole.getDefault();
				if (!role.isOrgAccess(po.getAD_Org_ID(), true)) {
					return accessDenied(id, "run now");
				}
			} else {
				return accessDenied(id, "run now");
			}
		}
		
		String error = serverMgr.runNow(id);
		if (!Util.isEmpty(error, true)) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Server error").append("Server error with exception: ").append(error).build().toString())
					.build();
		}
		
		return getServer(id);
	}

	private IServerManager getServerManager() {
		if (ClusterUtil.getClusterService() != null) {
			return ClusterServerMgr.getInstance();
		} else {
			return AdempiereServerMgr.get();
		}
	}

	@Override
	public Response reloadServers() {
		MUser user = MUser.get(Env.getCtx());
		if (!user.isAdministrator())
			return accessDenied("", "reload servers");

		IServerManager serverMgr = getServerManager();
		String error = serverMgr.reload();
		if (!Util.isEmpty(error, true)) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Server error").append("Server error with exception: ").append(error).build().toString())
					.build();
		}
		
		return getServers();
	}

	@Override
	public Response getScheduler(int id) {
		MUser user = MUser.get(Env.getCtx());
		if (!user.isAdministrator())
			return accessDenied(String.valueOf(id), "get scheduler");

		if (id <= 0) {
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Invalid scheduler id").append("Invalid scheduler id: ").append(id).build().toString())
					.build();
		}
		MScheduler scheduler = new MScheduler(Env.getCtx(), id, null);
		if (scheduler.getAD_Scheduler_ID() != id) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid scheduler Id").append("No match found for scheduler id: ").append(id).build().toString())
					.build();
		}
		
		IServerManager serverMgr = getServerManager();
		ServerInstance instance = serverMgr.getServerInstance(scheduler.getServerID());
		JsonObject json = toSchedulerJson(scheduler, instance);
		return Response.ok(json.toString()).build();
	}

	private JsonObject toSchedulerJson(MScheduler scheduler, ServerInstance instance) {
		String state = null;
		if (instance == null) {
			state = Msg.getMsg(Env.getCtx(), "SchedulerNotSchedule");				 
		} else if (instance.isStarted()) {
			state = Msg.getMsg(Env.getCtx(), "SchedulerStarted");
		} else {
			state = Msg.getMsg(Env.getCtx(), "SchedulerStopped");
		}
		
		IPOSerializer serializer = IPOSerializer.getPOSerializer(MScheduler.Table_Name, scheduler.getClass());
		JsonObject json = serializer.toJson(scheduler);
		json.addProperty("server-id", scheduler.getServerID());
		json.addProperty("scheduler-state", state);
		if (instance != null && instance.getClusterMember() != null) {
			json.addProperty("node-id", instance.getClusterMember().getId());
			json.addProperty("node-host-name", instance.getClusterMember().getAddress().getCanonicalHostName());
			json.addProperty("node-port", instance.getClusterMember().getPort());
		}
		return json;
	}

	@Override
	public Response addScheduler(int id) {
		if (id <= 0) {
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Invalid scheduler id").append("Invalid scheduler id: ").append(id).build().toString())
					.build();
		}
		MScheduler scheduler = new MScheduler(Env.getCtx(), id, null);
		if (scheduler.getAD_Scheduler_ID() != id) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid scheduler Id").append("No match found for scheduler id: ").append(id).build().toString())
					.build();
		}
		
		MUser user = MUser.get(Env.getCtx());
		if (!user.isAdministrator()) {
			MTable table = MTable.get(Env.getCtx(), MScheduler.Table_ID);
			if (!hasAccess(table, true)) {
				return accessDenied(scheduler.getServerID(), "add scheduler");
			}
			if (scheduler.getAD_Client_ID() != Env.getAD_Client_ID(Env.getCtx())) {
				return accessDenied(scheduler.getServerID(), "add scheduler");
			}
			MRole role = MRole.getDefault();
			if (!role.isOrgAccess(scheduler.getAD_Org_ID(), true)) {
				return accessDenied(scheduler.getServerID(), "add scheduler");
			}
		}
		
		IServerManager serverMgr = getServerManager();
		ServerInstance instance = serverMgr.getServerInstance(scheduler.getServerID());
		if (instance != null) {
			JsonObject json = toSchedulerJson(scheduler, instance);
			return Response.status(Status.CONFLICT).entity(json.toString()).build();
		} else {
			String error = serverMgr.addScheduler(scheduler);
			if (!Util.isEmpty(error, true)) {
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Server error").append("Server error with exception: ").append(error).build().toString())
						.build();
			}
			
			instance = serverMgr.getServerInstance(scheduler.getServerID());
			JsonObject json = toSchedulerJson(scheduler, instance);
			return Response.status(Status.CREATED).entity(json.toString()).build();
		}
	}

	@Override
	public Response removeScheduler(int id) {
		if (id <= 0) {
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Invalid scheduler id").append("Invalid scheduler id: ").append(id).build().toString())
					.build();
		}
		MScheduler scheduler = new MScheduler(Env.getCtx(), id, null);
		if (scheduler.getAD_Scheduler_ID() != id) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid scheduler Id").append("No match found for scheduler id: ").append(id).build().toString())
					.build();
		}
		
		MUser user = MUser.get(Env.getCtx());
		if (!user.isAdministrator()) {
			MTable table = MTable.get(Env.getCtx(), MScheduler.Table_ID);
			if (!hasAccess(table, true)) {
				return accessDenied(scheduler.getServerID(), "remove scheduler");
			}
			if (scheduler.getAD_Client_ID() != Env.getAD_Client_ID(Env.getCtx())) {
				return accessDenied(scheduler.getServerID(), "remove scheduler");
			}
			MRole role = MRole.getDefault();
			if (!role.isOrgAccess(scheduler.getAD_Org_ID(), true)) {
				return accessDenied(scheduler.getServerID(), "remove scheduler");
			}
		}
		
		IServerManager serverMgr = getServerManager();
		String error = serverMgr.removeScheduler(scheduler);
		if (!Util.isEmpty(error, true)) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Server error").append("Server error with exception: ").append(error).build().toString())
					.build();
		}
		ServerInstance instance = serverMgr.getServerInstance(scheduler.getServerID());
		JsonObject json = toSchedulerJson(scheduler, instance);
		return Response.ok(json.toString()).build();
	}
	
	private boolean hasAccess(MTable table, boolean rw) {
		MRole role = MRole.getDefault();
		if (role == null)
			return false;
		
		StringBuilder builder = new StringBuilder("SELECT DISTINCT a.AD_Window_ID FROM AD_Window a JOIN AD_Tab b ON a.AD_Window_ID=b.AD_Window_ID ");
		builder.append("WHERE a.IsActive='Y' AND b.IsActive='Y' AND b.AD_Table_ID=?");
		try (PreparedStatement stmt = DB.prepareStatement(builder.toString(), null)) {
			stmt.setInt(1, table.getAD_Table_ID());			
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				int windowId = rs.getInt(1);
				Boolean b = role.getWindowAccess(windowId);
				if (b != null) {
					if (!rw || b.booleanValue())
						return true;
				}
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			return false;
		}
		return false;
	}
}

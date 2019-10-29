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

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.AdempiereProcessorLog;
import org.compiere.model.MScheduler;
import org.compiere.server.AdempiereServerMgr;
import org.compiere.server.IServerManager;
import org.compiere.server.ServerInstance;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.idempiere.server.cluster.ClusterServerMgr;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
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
		return Response.ok(servers.toString()).build();
	}

	@Override
	public Response getServer(String id) {
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
		return Response.ok(logs.toString()).build();
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

	@Override
	public Response runServer(String id) {
		IServerManager serverMgr = getServerManager();
		ServerInstance instance = serverMgr.getServerInstance(id);
		if (instance == null) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid server Id").append("No match found for server id: ").append(id).build().toString())
					.build(); 
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
}

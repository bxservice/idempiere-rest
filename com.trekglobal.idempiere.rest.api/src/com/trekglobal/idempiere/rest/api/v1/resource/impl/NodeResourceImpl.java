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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.logging.Level;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.server.LogFileInfo;
import org.compiere.server.SystemInfo;
import org.compiere.util.CLogger;
import org.compiere.util.TimeUtil;
import org.idempiere.distributed.IClusterMember;
import org.idempiere.distributed.IClusterService;
import org.idempiere.server.cluster.callable.DeleteLogsCallable;
import org.idempiere.server.cluster.callable.RotateLogCallable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.v1.resource.NodeResource;

/**
 * @author hengsin
 *
 */
public class NodeResourceImpl implements NodeResource {

	private CLogger log = CLogger.getCLogger(getClass());
	
	/**
	 * default constructor 
	 */
	public NodeResourceImpl() {
	}

	@Override
	public Response getNodes() {
		JsonArray nodes = new JsonArray();
		IClusterService service = ClusterUtil.getClusterService();
		if (service == null) {
			JsonObject localNode = new JsonObject();
			localNode.addProperty("id", LOCAL_ID);
			try {
				localNode.addProperty("name", InetAddress.getLocalHost().getCanonicalHostName());
			} catch (UnknownHostException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
			nodes.add(localNode);
			return Response.ok(localNode.toString()).build();
		}
		
		Collection<IClusterMember> members = service.getMembers();
		for(IClusterMember member : members) {
			JsonObject node = new JsonObject();
			node.addProperty("id", member.getId());
			node.addProperty("hostName", member.getAddress().getCanonicalHostName());
			node.addProperty("port", member.getPort());
			nodes.add(node);
		}
		return Response.ok(nodes.toString()).build();
	}

	@Override
	public Response getNodeInfo(String id) {
		SystemInfo info = getSystemInfo(id);
		
		if (info == null) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid Node Id").append("No match found for node id: ").append(id).build().toString())
					.build(); 
		}
		
		JsonObject json = new JsonObject();
		json.addProperty("id", id);
		InetAddress address = null;
		try {
			address = info.getAddress() != null ? info.getAddress() : InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
		}
		if (address != null)
			json.addProperty("hostName", address.getCanonicalHostName());
		json.addProperty("home", info.getIDempiereHome());
		json.addProperty("os", info.getOperatingSystem());
		json.addProperty("jvm", info.getJavaVM());
		json.addProperty("databaseDescription", info.getDatabaseDescription());
		json.addProperty("databaseConnectionURL", info.getDatabaseConnectionURL());
		json.addProperty("databaseStatus", info.getDatabaseStatus());
		json.addProperty("availableProcessors", info.getAvailableProcessors());
		json.addProperty("averageSystemLoad", info.getAverageSystemLoad());
		json.addProperty("memoryUsage", info.getMemoryUsage());
		json.addProperty("heapMemoryUsage", info.getHeapMemoryUsage());
		json.addProperty("runtime", info.getRuntimeName());
		json.addProperty("runtimeUptime", TimeUtil.formatElapsed(info.getRuntimeUpTime()));
		json.addProperty("threadCount", info.getThreadCount());
		json.addProperty("peakThreadCount", info.getPeakThreadCount());
		json.addProperty("daemonThreadCount", info.getDaemonThreadCount());
		json.addProperty("totalStartedThreadCount", info.getTotalStartedThreadCount());
		json.addProperty("logLevel", info.getLogLevel().getName());
		json.addProperty("currentLogFile", info.getCurrentLogFile());
		json.addProperty("sessionCount", info.getSessionCount());
		json.addProperty("garbageCollectionCount", info.getGarbageCollectionCount());
		json.addProperty("garbageCollectionTime", info.getGarbageCollectionTime());
		
		return Response.ok(json.toString()).build();
	}

	private SystemInfo getSystemInfo(String id) {
		SystemInfo info = null;
		if (id.equals(LOCAL_ID)) {
			info = SystemInfo.getLocalSystemInfo();
		} else {
			IClusterService service = ClusterUtil.getClusterService();
			if (service != null && service.getLocalMember().getId().equals(id))
				info = SystemInfo.getLocalSystemInfo();
			else
				info = SystemInfo.getClusterNodeInfo(id);
		}
		return info;
	}

	@Override
	public Response getNodeLogs(String id) {
		SystemInfo info = getSystemInfo(id);
		
		if (info == null) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid Node Id").append("No match found for node id: ").append(id).build().toString())
					.build(); 
		}
		
		LogFileInfo[] logInfos = info.getLogFileInfos();
		JsonArray logs = new JsonArray();
		for(LogFileInfo logInfo : logInfos) {
			JsonObject log = new JsonObject();
			log.addProperty("fileName", logInfo.getFileName());
			log.addProperty("fileSize", logInfo.getFileSize());
			logs.add(log);
		}
		return Response.ok(logs.toString()).build();
	}

	@Override
	public Response getNodeLogFile(String id, String fileName) {
		FileResourceImpl fileResource = new FileResourceImpl();
		return fileResource.getFile(fileName, id);
	}

	@Override
	public Response deleteNodeLogs(String id) {
		DeleteLogsCallable callable = new DeleteLogsCallable();
		Boolean result = null;
		try {
			if (id.equals(LOCAL_ID)) {
				result = callable.call();
			} else {
				IClusterService service = ClusterUtil.getClusterService();
				if (service != null && service.getLocalMember().getId().equals(id)) {
					result = callable.call();
				} else if (service == null) {
					return Response.status(Status.NOT_FOUND)
							.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid Node Id").append("No match found for node id: ").append(id).build().toString())
							.build();
				} else {
					IClusterMember member = ClusterUtil.getClusterMember(id);
					if (member == null) {
						return Response.status(Status.NOT_FOUND)
								.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid Node Id").append("No match found for node id: ").append(id).build().toString())
								.build();
					}
					result = service.execute(callable, member).get();
				}
					
			}
		} catch (Exception ex) {
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Server error").append("Server error with exception: ").append(ex.getMessage()).build().toString())
					.build();
		}
		
		if (result != null && result)
			return Response.ok("Logs deleted").build();
		else
			return Response.status(Status.NOT_MODIFIED).entity("Failed to delete logs").build();
	}

	@Override
	public Response rotateNodeLogs(String id) {
		RotateLogCallable callable = new RotateLogCallable();
		Boolean result = null;
		try {
			if (id.equals(LOCAL_ID)) {
				result = callable.call();
			} else {
				IClusterService service = ClusterUtil.getClusterService();
				if (service != null && service.getLocalMember().getId().equals(id)) {
					result = callable.call();
				} else if (service == null) {
					return Response.status(Status.NOT_FOUND)
							.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid Node Id").append("No match found for node id: ").append(id).build().toString())
							.build();
				} else {
					IClusterMember member = ClusterUtil.getClusterMember(id);
					if (member == null) {
						return Response.status(Status.NOT_FOUND)
								.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid Node Id").append("No match found for node id: ").append(id).build().toString())
								.build();
					}
					result = service.execute(callable, member).get();
				}
					
			}
		} catch (Exception ex) {
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Server error").append("Server error with exception: ").append(ex.getMessage()).build().toString())
					.build();
		}
		
		if (result != null && result)
			return Response.ok("Logs rotated").build();
		else
			return Response.status(Status.NOT_MODIFIED).entity("Failed to rotate logs").build();
	}

}

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

import java.util.List;
import java.util.logging.Level;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.MTask;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.json.TypeConverterUtils;
import com.trekglobal.idempiere.rest.api.json.filter.ConvertedQuery;
import com.trekglobal.idempiere.rest.api.json.filter.IQueryConverter;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.v1.resource.TaskResource;

/**
 * 
 * @author hengsin
 *
 */
public class TaskResourceImpl implements TaskResource {

	private final static CLogger log = CLogger.getCLogger(TaskResourceImpl.class);

	public TaskResourceImpl() {
	}

	@Override
	public Response getTasks(String filter) {
		IQueryConverter converter = IQueryConverter.getQueryConverter("DEFAULT");
		try {
			ConvertedQuery convertedStatement = converter.convertStatement(MTask.Table_Name, filter);
			if (log.isLoggable(Level.INFO)) log.info("Where Clause: " + convertedStatement.getWhereClause());

			JsonArray taskArray = new JsonArray();
			Query query = new Query(Env.getCtx(), MTask.Table_Name, convertedStatement.getWhereClause(), null);
			query.setApplyAccessFilter(true).setOnlyActiveRecords(true).setOrderBy(MTask.COLUMNNAME_Name);
			query.setParameters(convertedStatement.getParameters());

			List<MTask> tasks = query.list();
			MRole role = MRole.getDefault();
			IPOSerializer serializer = IPOSerializer.getPOSerializer(MTask.Table_Name, MTable.getClass(MTask.Table_Name));
			for (MTask task : tasks) {
				if (role.getTaskAccess(task.getAD_Task_ID()) == null)
					continue;

				JsonObject jsonObject = serializer.toJson(task,
						new String[] { MTask.COLUMNNAME_AD_Task_ID, MTask.COLUMNNAME_AD_Task_UU, MTask.COLUMNNAME_Name,
								MTask.COLUMNNAME_Description, MTask.COLUMNNAME_Help, MTask.COLUMNNAME_EntityType, },
						null);
				jsonObject.addProperty("slug", TypeConverterUtils.slugify(task.getName()));
				taskArray.add(jsonObject);

			}
			JsonObject json = new JsonObject();
			json.add("tasks", taskArray);
			return Response.ok(json.toString()).build();
		} catch (Exception ex) {
			Status status = Status.INTERNAL_SERVER_ERROR;
			if (ex instanceof IDempiereRestException)
				status = ((IDempiereRestException) ex).getErrorResponseStatus();

			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(status)
					.entity(new ErrorBuilder().status(status)
							.title("GET Error")
							.append("Get task with exception: ")
							.append(ex.getMessage())
							.build().toString())
					.build();
		}
	}

	@Override
	public Response getTask(String taskSlug) {
		Query query = new Query(Env.getCtx(), MTask.Table_Name, "Slugify(Name)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		MTask task = query.setParameters(taskSlug).first();
		if (task == null) {
			query.setApplyAccessFilter(false);
			task = query.setParameters(taskSlug).first();
			if (task != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for task: ").append(taskSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid task name").append("No match found for task name: ").append(taskSlug).build().toString())
						.build();
			}
		}
		MRole role = MRole.getDefault();
		if (role.getTaskAccess(task.getAD_Task_ID()) == null)
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for task: ").append(taskSlug).build().toString())
					.build();

		IPOSerializer serializer = IPOSerializer.getPOSerializer(MTask.Table_Name, MTable.getClass(MTask.Table_Name));
		JsonObject jsonObject = serializer.toJson(task,
				new String[] { MTask.COLUMNNAME_AD_Task_ID, MTask.COLUMNNAME_AD_Task_UU, MTask.COLUMNNAME_Name,
						MTask.COLUMNNAME_Description, MTask.COLUMNNAME_Help, MTask.COLUMNNAME_EntityType, },
				null);
		jsonObject.addProperty("slug", TypeConverterUtils.slugify(task.getName()));

		return Response.ok(jsonObject.toString()).build();
	}

	@Override
	public Response runTask(String taskSlug, String jsonText) {
		Query query = new Query(Env.getCtx(), MTask.Table_Name, "Slugify(Name)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		MTask task = query.setParameters(taskSlug).first();
		if (task == null) {
			query.setApplyAccessFilter(false);
			task = query.setParameters(taskSlug).first();
			if (task != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for task: ").append(taskSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid task name").append("No match found for task name: ").append(taskSlug).build().toString())
						.build();
			}
		}
		MRole role = MRole.getDefault();
		if (role.getTaskAccess(task.getAD_Task_ID()) == null || role.getTaskAccess(task.getAD_Task_ID()).booleanValue()==false)
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for task: ").append(taskSlug).build().toString())
					.build();

		task.execute();
		String stdOut = task.getExecutedTask().getOut().toString();
		String stdErr = task.getExecutedTask().getErr().toString();
		int exitValue = task.getExecutedTask().getExitValue();

		JsonObject json = new JsonObject();
		json.addProperty("exitValue", exitValue);
		json.addProperty("stdOut", stdOut);
		json.addProperty("stdErr", stdErr);
		return Response.ok(json.toString()).build();
	}

}

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.adempiere.util.ServerContext;
import org.compiere.Adempiere;
import org.compiere.model.MAttachment;
import org.compiere.model.MClient;
import org.compiere.model.MNote;
import org.compiere.model.MPInstance;
import org.compiere.model.MPInstanceLog;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoUtil;
import org.compiere.process.ServerProcessCtl;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.json.Process;
import com.trekglobal.idempiere.rest.api.json.RestUtils;
import com.trekglobal.idempiere.rest.api.json.TypeConverterUtils;
import com.trekglobal.idempiere.rest.api.json.filter.ConvertedQuery;
import com.trekglobal.idempiere.rest.api.json.filter.IQueryConverter;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.v1.resource.ProcessResource;

/**
 * 
 * @author hengsin
 *
 */
public class ProcessResourceImpl implements ProcessResource {

	private final static CLogger log = CLogger.getCLogger(ProcessResourceImpl.class);

	public ProcessResourceImpl() {
	}

	@Override
	public Response getProcesses(String filter) {
		IQueryConverter converter = IQueryConverter.getQueryConverter("DEFAULT");
		try {
			ConvertedQuery convertedStatement = converter.convertStatement(MProcess.Table_Name, filter);
			if (log.isLoggable(Level.INFO)) log.info("Where Clause: " + convertedStatement.getWhereClause());

			JsonArray processArray = new JsonArray();
			StringBuilder where = new StringBuilder("AD_Form_ID IS NULL");
			if (!Util.isEmpty(filter, true)) {
				where.append(" AND (").append(convertedStatement.getWhereClause()).append(")");
			}
			Query query = new Query(Env.getCtx(), MProcess.Table_Name, where.toString(), null);
			query.setApplyAccessFilter(true).setOnlyActiveRecords(true).setOrderBy("Value");
			query.setParameters(convertedStatement.getParameters());

			List<MProcess> processes = query.list();
			MRole role = MRole.getDefault();
			IPOSerializer serializer = IPOSerializer.getPOSerializer(MProcess.Table_Name, MTable.getClass(MProcess.Table_Name));
			for(MProcess process : processes) {
				if (role.getProcessAccess(process.getAD_Process_ID()) == null)
					continue;

				JsonObject jsonObject = serializer.toJson(process, new String[] {"AD_Process_ID", "AD_Process_UU", "Value", "Name", "Description", "Help", "EntityType", "IsReport", "AD_ReportView_ID", "AD_PrintFormat_ID", "AD_Workflow_ID", "JasperReport"}, null);
				jsonObject.addProperty("slug", TypeConverterUtils.slugify(process.getValue()));
				processArray.add(jsonObject);

			}
			JsonObject json = new JsonObject();
			json.add("processes", processArray);
			return Response.ok(json.toString()).build();
		} catch (Exception ex) {
			Status status = Status.INTERNAL_SERVER_ERROR;
			if (ex instanceof IDempiereRestException)
				status = ((IDempiereRestException) ex).getErrorResponseStatus();
			
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(status)
					.entity(new ErrorBuilder().status(status)
							.title("GET Error")
							.append("Get process with exception: ")
							.append(ex.getMessage())
							.build().toString())
					.build();
		}
	}

	@Override
	public Response getProcess(String processSlug) {
		Query query = new Query(Env.getCtx(), MProcess.Table_Name, "Slugify(Value)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		MProcess process = query.setParameters(processSlug).first();
		if (process == null) {
			query.setApplyAccessFilter(false);
			process = query.setParameters(processSlug).first();
			if (process != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for process: ").append(processSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid process name").append("No match found for process name: ").append(processSlug).build().toString())
						.build();
			}
		}
		MRole role = MRole.getDefault();
		if (role.getProcessAccess(process.getAD_Process_ID()) == null)
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for process: ").append(processSlug).build().toString())
					.build();
		
		IPOSerializer serializer = IPOSerializer.getPOSerializer(MProcess.Table_Name, MTable.getClass(MProcess.Table_Name));
		JsonObject jsonObject = serializer.toJson(process, new String[] {"AD_Process_ID", "AD_Process_UU", "Value", "Name", "Description", "Help", "EntityType", "IsReport", "AD_ReportView_ID", "AD_PrintFormat_ID", "AD_Workflow_ID", "JasperReport"}, null);
		jsonObject.addProperty("slug", TypeConverterUtils.slugify(process.getValue()));
	
		JsonArray parameterArray = new JsonArray();
		MProcessPara[] parameters = process.getParameters();
		serializer = IPOSerializer.getPOSerializer(MProcessPara.Table_Name, MTable.getClass(MProcessPara.Table_Name));
		for(MProcessPara parameter : parameters) {
			JsonObject parameterJsonObject = serializer.toJson(parameter, null, new String[] {"AD_Client_ID", "AD_Org_ID", "AD_Process_ID", 
					"FieldLength", "IsCentrallyMaintained", "IsEncrypted", "AD_Element_ID", "ColumnName", "IsActive",
					"Created", "CreatedBy", "Updated", "UpdatedBy"});
			String propertyName = parameter.getColumnName();
			parameterJsonObject.addProperty("parameterName", propertyName);
			parameterArray.add(parameterJsonObject);
		}
		jsonObject.add("parameters", parameterArray);
		
		return Response.ok(jsonObject.toString()).build();
	}

	@Override
	public Response runProcess(String processSlug, String jsonText) {
		Query query = new Query(Env.getCtx(), MProcess.Table_Name, "Slugify(Value)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		MProcess process = query.setParameters(processSlug).first();
		if (process == null) {
			query.setApplyAccessFilter(false);
			process = query.setParameters(processSlug).first();
			if (process != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for process: ").append(processSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid process name").append("No match found for process name: ").append(processSlug).build().toString())
						.build();
			}
		}
		MRole role = MRole.getDefault();
		if (role.getProcessAccess(process.getAD_Process_ID()) == null || role.getProcessAccess(process.getAD_Process_ID()).booleanValue()==false)
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for process: ").append(processSlug).build().toString())
					.build();
		
		Gson gson = new GsonBuilder().create();
		JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
		MPInstance pInstance = Process.createPInstance(process, jsonObject, false);
		
		ProcessInfo processInfo = Process.createProcessInfo(process, pInstance, jsonObject);

		if(processInfo.isProcessRunning(pInstance.getParameters())) {
			return Response.status(Status.CONFLICT)
					.entity(new ErrorBuilder().status(Status.CONFLICT).title(Msg.getMsg(Env.getCtx(), "ProcessAlreadyRunning")).append(processSlug).build().toString())
					.build();
		}
		
		ServerProcessCtl.process(processInfo, null);
		
		JsonObject processInfoJson = Process.toJsonObject(processInfo, processSlug);
		
		return Response.ok(processInfoJson.toString()).build();
	}

	@Override
	public Response getJobs() {
		List<MPInstance> instances = new Query(Env.getCtx(), MPInstance.Table_Name, "IsProcessing='Y' AND IsRunAsJob='Y' ", null)
							.setOnlyActiveRecords(true)
							.setClient_ID()
							.setOrderBy("AD_PInstance_ID")
							.list();
		JsonArray instanceArray = new JsonArray();
		for(MPInstance instance : instances) {
			JsonObject jsonObject = toJsonObject(instance);
			instanceArray.add(jsonObject);
		}
		JsonObject json = new JsonObject();
		json.add("jobs", instanceArray);
		return Response.ok(json.toString()).build();
	}
	
	@Override
	public Response getJob(int id) {
		MPInstance instance = new Query(Env.getCtx(), MPInstance.Table_Name, "IsRunAsJob='Y' AND AD_PInstance_ID=?", null)
							.setClient_ID()
							.setParameters(id)
							.first();
		
		if (instance != null) {
			JsonObject jsonObject = toJsonObject(instance);
			return Response.ok(jsonObject.toString()).build();
		} else {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Job not found").append("No job found matching id ").append(id).build().toString())
					.build();
		}		
	}

	@Override
	public Response runJob(String processSlug, String jsonText) {		
		Query query = new Query(Env.getCtx(), MProcess.Table_Name, "Slugify(Value)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		MProcess process = query.setParameters(processSlug).first();
		if (process == null) {
			query.setApplyAccessFilter(false);
			process = query.setParameters(processSlug).first();
			if (process != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for process: ").append(processSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid process name").append("No match found for process name: ").append(processSlug).build().toString())
						.build();
			}
		}
		MRole role = MRole.getDefault();
		if (role.getProcessAccess(process.getAD_Process_ID()) == null || role.getProcessAccess(process.getAD_Process_ID()).booleanValue()==false)
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for process: ").append(processSlug).build().toString())
					.build();
		
		Gson gson = new GsonBuilder().create();
		JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
		MPInstance pInstance = Process.createPInstance(process, jsonObject, true);
		ProcessInfo processInfo = Process.createProcessInfo(process, pInstance, jsonObject);

		if(processInfo.isProcessRunning(pInstance.getParameters())) {
			return Response.status(Status.CONFLICT)
					.entity(new ErrorBuilder().status(Status.CONFLICT).title(Msg.getMsg(Env.getCtx(), "ProcessAlreadyRunning")).append(processSlug).build().toString())
					.build();
		}
		
		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		MPInstance.publishChangedEvent(AD_User_ID);
		Adempiere.getThreadPoolExecutor().schedule(new BackgroundJobRunnable(Env.getCtx(), processInfo), 1000, TimeUnit.MILLISECONDS);
		
		JsonObject instanceJson = toJsonObject(pInstance);
		return Response.ok(instanceJson.toString()).build();
	}
	
	private JsonObject toJsonObject(MPInstance instance) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("id", instance.getAD_PInstance_ID());
		jsonObject.addProperty("client", MClient.get(Env.getCtx(), instance.getAD_Client_ID()).getName());
		jsonObject.addProperty("user", MUser.get(Env.getCtx(), instance.getCreatedBy()).getName());
		jsonObject.addProperty("process", TypeConverterUtils.slugify(MProcess.get(Env.getCtx(), instance.getAD_Process_ID()).getValue()));
		jsonObject.addProperty("since", DisplayType.getDateFormat(DisplayType.DateTime).format(instance.getCreated()));
		jsonObject.addProperty("processing", instance.isProcessing());
		return jsonObject;
	}
	
	private class BackgroundJobRunnable implements Runnable
	{
		private Properties m_ctx;
		private ProcessInfo m_pi;
		
		private BackgroundJobRunnable(Properties ctx, ProcessInfo pi) 
		{
			super();
			
			m_ctx = new Properties();
			Env.setContext(m_ctx, Env.AD_CLIENT_ID, ctx.getProperty(Env.AD_CLIENT_ID));
			Env.setContext(m_ctx, Env.AD_ORG_ID, ctx.getProperty(Env.AD_ORG_ID));
			Env.setContext(m_ctx, Env.AD_ROLE_ID, ctx.getProperty(Env.AD_ROLE_ID));
			Env.setContext(m_ctx, Env.M_WAREHOUSE_ID, ctx.getProperty(Env.M_WAREHOUSE_ID));
			Env.setContext(m_ctx, Env.LANGUAGE, ctx.getProperty(Env.LANGUAGE));
			Env.setContext(m_ctx, Env.AD_USER_ID, ctx.getProperty(Env.AD_USER_ID));
			Env.setContext(m_ctx, Env.AD_SESSION_ID, ctx.getProperty(Env.AD_SESSION_ID));
			Env.setContext(m_ctx, Env.DATE, ctx.getProperty(Env.DATE));
			RestUtils.setSessionContextVariables(m_ctx);

			m_pi = pi;
		}
		
		@Override
		public void run() {
			try {
				ServerContext.setCurrentInstance(m_ctx);
				doRun();
			} finally {
				ServerContext.dispose();
			}
		}
		
		private void doRun()
		{			
			m_pi.setIsBatch(true);
			m_pi.setPrintPreview(true);
			
			MPInstance instance = new MPInstance(m_ctx, m_pi.getAD_PInstance_ID(), null);
			String notificationType = instance.getNotificationType();
			boolean sendEmail = notificationType.equals(MPInstance.NOTIFICATIONTYPE_EMail) || notificationType.equals(MPInstance.NOTIFICATIONTYPE_EMailPlusNotice);
			boolean createNotice = notificationType.equals(MPInstance.NOTIFICATIONTYPE_Notice) || notificationType.equals(MPInstance.NOTIFICATIONTYPE_EMailPlusNotice);
			
			int AD_Client_ID = Env.getAD_Client_ID(m_ctx);
			int AD_User_ID = Env.getAD_User_ID(m_ctx);
			
			try {
				MProcess process = new MProcess(m_ctx, m_pi.getAD_Process_ID(), null);	
				if (process.isReport() && process.getJasperReport() != null) {
					if (!Util.isEmpty(process.getJasperReport())) 
					{
						m_pi.setExport(true);
						if ("HTML".equals(m_pi.getReportType())) 
							m_pi.setExportFileExtension("html");
						else if ("CSV".equals(m_pi.getReportType()))
							m_pi.setExportFileExtension("csv");
						else if ("XLS".equals(m_pi.getReportType()))
							m_pi.setExportFileExtension("xls");
						else
							m_pi.setExportFileExtension("pdf");
					}
				}
				
				List<File> files = new ArrayList<>();
				ServerProcessCtl.process(m_pi, null);
				ProcessInfoUtil.setLogFromDB(m_pi);
				if (!m_pi.isError())
				{					
					boolean isReport = (process.isReport() || process.getAD_ReportView_ID() > 0 || process.getJasperReport() != null || process.getAD_PrintFormat_ID() > 0);
					if (isReport && m_pi.getPDFReport() != null)
					{
						files.add(m_pi.getPDFReport());
					}
					
					if (m_pi.isExport() && m_pi.getExportFile() != null)
						files.add(m_pi.getExportFile());										
				}
				
				if (sendEmail)
				{
					MClient client = MClient.get(m_ctx, AD_Client_ID);
					client.sendEMailAttachments(AD_User_ID, process.get_Translation("Name", Env.getAD_Language(Env.getCtx())), m_pi.getSummary() + " " + m_pi.getLogInfo(), files);
				}
				
				if (createNotice)
				{
					MNote note = new MNote(m_ctx, "BackgroundJob", AD_User_ID, null);
					note.setTextMsg(process.get_Translation("Name", Env.getAD_Language(Env.getCtx())) + "\n" + m_pi.getSummary());
					note.setRecord(MPInstance.Table_ID, m_pi.getAD_PInstance_ID());
					note.saveEx();
					
					MAttachment attachment = null;
					if (files.size() > 0)
					{
						attachment = note.createAttachment();
						for (File downloadFile : files)
							attachment.addEntry(downloadFile);						
					}
					String log = m_pi.getLogInfo(true);
					if (log != null && log.trim().length() > 0) {
						if (attachment == null)
							attachment = note.createAttachment();
						attachment.addEntry("ProcessLog.html", log.getBytes("UTF-8"));
					}
					if (attachment != null)
						attachment.saveEx();
					MPInstanceLog il = instance.addLog(null, 0, null, Msg.parseTranslation(m_ctx, "@Created@ @AD_Note_ID@ " + note.getAD_Note_ID()),
							MNote.Table_ID, note.getAD_Note_ID());
					il.saveEx();
				}
			} catch (Exception e) {
				CLogger.getCLogger(getClass()).log(Level.SEVERE, e.getLocalizedMessage());				
			} finally {
				instance.setIsProcessing(false);
				instance.saveEx();
				
				MPInstance.publishChangedEvent(AD_User_ID);
			}
		}
	}
}

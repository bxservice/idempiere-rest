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
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.adempiere.util.ServerContext;
import org.compiere.Adempiere;
import org.compiere.model.GridField;
import org.compiere.model.GridFieldVO;
import org.compiere.model.MAttachment;
import org.compiere.model.MClient;
import org.compiere.model.MNote;
import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.print.MPrintFormat;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoLog;
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.json.TypeConverterUtils;
import com.trekglobal.idempiere.rest.api.v1.resource.ProcessResource;

/**
 * 
 * @author hengsin
 *
 */
public class ProcessResourceImpl implements ProcessResource {

	public ProcessResourceImpl() {
	}

	@Override
	public Response getProcesses(String filter) {
		JsonArray processArray = new JsonArray();
		StringBuilder where = new StringBuilder("AD_Form_ID IS NULL");
		if (!Util.isEmpty(filter, true)) {
			where.append(" AND (").append(filter).append(")");
		}
		Query query = new Query(Env.getCtx(), MProcess.Table_Name, where.toString(), null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true).setOrderBy("Value");
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
		return Response.ok(processArray.toString()).build();
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
				return Response.status(Status.FORBIDDEN).build();
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		}
		MRole role = MRole.getDefault();
		if (role.getProcessAccess(process.getAD_Process_ID()) == null)
			return Response.status(Status.FORBIDDEN).build();
		
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
			String propertyName = TypeConverterUtils.toPropertyName(parameter.getColumnName());
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
				return Response.status(Status.FORBIDDEN).build();
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		}
		MRole role = MRole.getDefault();
		if (role.getProcessAccess(process.getAD_Process_ID()) == null)
			return Response.status(Status.FORBIDDEN).build();
		
		Gson gson = new GsonBuilder().create();
		JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
		MPInstance pInstance = createPInstance(process, jsonObject, false);
		
		ProcessInfo processInfo = createProcessInfo(process, pInstance, jsonObject);
		
		ServerProcessCtl.process(processInfo, null);
		
		JsonObject processInfoJson = new JsonObject();
		processInfoJson.addProperty("AD_PInstance_ID", processInfo.getAD_PInstance_ID());
		processInfoJson.addProperty("process", processSlug);
		processInfoJson.addProperty("summary", processInfo.getSummary());
		processInfoJson.addProperty("isError", processInfo.isError());
		if (processInfo.getPDFReport() != null) {
			processInfoJson.addProperty("reportFile", processInfo.getPDFReport().getName());
			processInfoJson.addProperty("reportFileLength", processInfo.getPDFReport().length());
		}
		if (processInfo.getExportFile() != null) {
			processInfoJson.addProperty("exportFile", processInfo.getExportFile().getName());
			processInfoJson.addProperty("exportFileLength", processInfo.getExportFile().length());
		}
		
		ProcessInfoUtil.setLogFromDB(processInfo);
		ProcessInfoLog[] logs = processInfo.getLogs();
		if (logs != null && logs.length > 0) {
			JsonArray logArray = new JsonArray();
			SimpleDateFormat dateFormat = DisplayType.getDateFormat(DisplayType.Date);
			for(ProcessInfoLog log : logs) {
				StringBuilder sb = new StringBuilder();
				if (log.getP_Date() != null)
					sb.append(dateFormat.format(log.getP_Date()))
					  .append(" \t");
				//
				if (log.getP_Number() != null)
					sb.append(log.getP_Number())
					  .append(" \t");
				//
				if (log.getP_Msg() != null)
					sb.append(Msg.parseTranslation(Env.getCtx(), log.getP_Msg()));
				
				JsonPrimitive logStr = new JsonPrimitive(sb.toString());
				logArray.add(logStr);
			}
			processInfoJson.add("logs", logArray);
		}
		
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
		return Response.ok(instanceArray.toString()).build();
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
			return Response.status(Status.NOT_FOUND).build();
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
				return Response.status(Status.FORBIDDEN).build();
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		}
		MRole role = MRole.getDefault();
		if (role.getProcessAccess(process.getAD_Process_ID()) == null)
			return Response.status(Status.FORBIDDEN).build();
		
		Gson gson = new GsonBuilder().create();
		JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
		MPInstance pInstance = createPInstance(process, jsonObject, true);
		ProcessInfo processInfo = createProcessInfo(process, pInstance, jsonObject);
		
		int AD_User_ID = Env.getAD_User_ID(Env.getCtx());
		MPInstance.publishChangedEvent(AD_User_ID);
		Adempiere.getThreadPoolExecutor().schedule(new BackgroundJobRunnable(Env.getCtx(), processInfo), 1000, TimeUnit.MILLISECONDS);
		
		JsonObject instanceJson = toJsonObject(pInstance);
		return Response.ok(instanceJson.toString()).build();
	}
	
	private ProcessInfo createProcessInfo(MProcess process, MPInstance pInstance, JsonObject jsonObject) {
		ProcessInfo processInfo = new ProcessInfo(process.getName(), process.getAD_Process_ID());
		processInfo.setAD_PInstance_ID(pInstance.getAD_PInstance_ID());
		JsonElement recordIdElement = jsonObject.get("record-id");
		if (recordIdElement != null && recordIdElement.isJsonPrimitive()) {
			int Record_ID = recordIdElement.getAsInt();
			if (Record_ID > 0) {
				processInfo.setRecord_ID(Record_ID);
				JsonElement tableIdElement = jsonObject.get("table-id");
				int AD_Table_ID = 0;
				if (tableIdElement != null && tableIdElement.isJsonPrimitive()) {
					AD_Table_ID = tableIdElement.getAsInt();
				}
				if (AD_Table_ID==0) {
					JsonElement tableNameElement = jsonObject.get("model-name");
					if (tableNameElement != null && tableNameElement.isJsonPrimitive()) {
						MTable table = MTable.get(Env.getCtx(), tableNameElement.getAsString());
						if (table != null)
							AD_Table_ID = table.getAD_Table_ID();
					}
				}
				if (AD_Table_ID > 0)
					processInfo.setTable_ID(AD_Table_ID);
			}
		}
		if (process.isReport()) {
			JsonElement reportTypeElement = jsonObject.get("report-type");
			if (reportTypeElement != null && reportTypeElement.isJsonPrimitive()) {
				processInfo.setReportType(reportTypeElement.getAsString());
			}
			JsonElement isSummaryElement = jsonObject.get("is-summary");
			if (isSummaryElement != null && isSummaryElement.isJsonPrimitive()) {
				processInfo.setIsSummary(isSummaryElement.getAsBoolean());
			}
		}
		
		processInfo.setAD_User_ID(Env.getAD_User_ID(Env.getCtx()));
		processInfo.setAD_Client_ID(Env.getAD_Client_ID(Env.getCtx()));
		processInfo.setAD_PInstance_ID(pInstance.getAD_PInstance_ID());
		processInfo.setAD_Process_UU(process.getAD_Process_UU());
		processInfo.setIsBatch(true);
		processInfo.setPrintPreview(true);
		processInfo.setExport(true);
		JsonElement printFormatIdElement = jsonObject.get("print-format-id");
		if (printFormatIdElement != null && printFormatIdElement.isJsonPrimitive()) {
			int AD_PrintFormat_ID = printFormatIdElement.getAsInt();
			if (AD_PrintFormat_ID > 0) 
			{
				MPrintFormat format = new MPrintFormat(Env.getCtx(), AD_PrintFormat_ID, null);
				processInfo.setSerializableObject(format);
			}
		}
		
		if (!Util.isEmpty(process.getJasperReport())) 
		{
			if ("HTML".equals(processInfo.getReportType())) 
				processInfo.setExportFileExtension("html");
			else if ("CSV".equals(processInfo.getReportType()))
				processInfo.setExportFileExtension("csv");
			else if ("XLS".equals(processInfo.getReportType()))
				processInfo.setExportFileExtension("xls");
			else
				processInfo.setExportFileExtension("pdf");
		}
		return processInfo;
	}

	private MPInstance createPInstance(MProcess process, JsonObject jsonObject, boolean runAsJob) {
		MPInstance pInstance = new MPInstance(process, 0);
		MPInstancePara[] iParams = pInstance.getParameters();
		for(MPInstancePara iParam : iParams) {
			MProcessPara processPara = process.getParameter(iParam.getParameterName());
			String columnName = processPara.getColumnName();
			String propertyName = TypeConverterUtils.toPropertyName(columnName);
			GridFieldVO gridFieldVO = GridFieldVO.createParameter(Env.getCtx(), 0, 0, 0, 0, columnName, processPara.getName(), processPara.getAD_Reference_ID(), 
					processPara.getAD_Reference_Value_ID(), processPara.isMandatory(), false, "");
			GridField gridField = new GridField(gridFieldVO);
			JsonElement element = jsonObject.get(propertyName);
			if (element != null) {			
				Object value = TypeConverterUtils.fromJsonValue(gridField, element);
				if (value != null) {
					if (value instanceof BigDecimal)
						iParam.setP_Number((BigDecimal)value);
					else if (value instanceof Number)
						iParam.setP_Number(((Number)value).intValue());
					else if (value instanceof Timestamp)
						iParam.setP_Date((Timestamp) value);
					else
						iParam.setP_String(value.toString());
				}
			}
			if (processPara.isRange()) {
				String toPropertyName = propertyName + "_to";
				element = jsonObject.get(toPropertyName);
				if (element != null) {			
					Object value = TypeConverterUtils.fromJsonValue(gridField, element);
					if (value != null) {
						if (value instanceof BigDecimal)
							iParam.setP_Number_To((BigDecimal)value);
						else if (value instanceof Number)
							iParam.setP_Number_To(((Number)value).intValue());
						else if (value instanceof Timestamp)
							iParam.setP_Date_To((Timestamp) value);
						else
							iParam.setP_String_To(value.toString());
					}
				}
			}
			if (iParam.is_Changed())
				iParam.saveEx();
		}
		
		if (runAsJob) {
			pInstance.setIsRunAsJob(true);
			pInstance.setIsProcessing(true);
			JsonElement notificationTypeElement= jsonObject.get("notification-type");
			if (notificationTypeElement != null && notificationTypeElement.isJsonPrimitive()) {
				String notificationType = notificationTypeElement.getAsString();
				if (notificationType.equals(MPInstance.NOTIFICATIONTYPE_EMail) || notificationType.equals(MPInstance.NOTIFICATIONTYPE_Notice) || notificationType.equals(MPInstance.NOTIFICATIONTYPE_EMailPlusNotice))
					pInstance.setNotificationType(notificationType);
				else
					pInstance.setNotificationType(MPInstance.NOTIFICATIONTYPE_EMailPlusNotice);
			} else {
				pInstance.setNotificationType(MPInstance.NOTIFICATIONTYPE_EMailPlusNotice);
			}
		}
		
		return pInstance;
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
			Env.setContext(m_ctx, "#AD_Client_ID", ctx.getProperty("#AD_Client_ID"));
			Env.setContext(m_ctx, "#AD_Org_ID", ctx.getProperty("#AD_Org_ID"));
			Env.setContext(m_ctx, "#AD_Role_ID", ctx.getProperty("#AD_Role_ID"));
			Env.setContext(m_ctx, "#M_Warehouse_ID", ctx.getProperty("#M_Warehouse_ID"));
			Env.setContext(m_ctx, "#AD_Language", ctx.getProperty("#AD_Language"));
			Env.setContext(m_ctx, "#AD_User_ID", ctx.getProperty("#AD_User_ID"));
			Env.setContext(m_ctx, "#Date", ctx.getProperty("#Date"));
			
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

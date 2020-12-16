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
package com.trekglobal.idempiere.rest.api.json;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.compiere.model.GridField;
import org.compiere.model.GridFieldVO;
import org.compiere.model.MPInstance;
import org.compiere.model.MPInstancePara;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.print.MPrintFormat;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoLog;
import org.compiere.process.ProcessInfoUtil;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.idempiere.distributed.IClusterService;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.trekglobal.idempiere.rest.api.util.ClusterUtil;

/**
 * 
 * @author hengsin
 *
 */
public class Process {

	private Process() {
	}
	
	/**
	 * 
	 * @param process
	 * @param jsonObject process instance configs
	 * @param runAsJob
	 * @return MPInstance
	 */
	public static MPInstance createPInstance(MProcess process, JsonObject jsonObject, boolean runAsJob) {
		MPInstance pInstance = new MPInstance(process, 0);
		MPInstancePara[] iParams = pInstance.getParameters();
		for(MPInstancePara iParam : iParams) {
			MProcessPara processPara = process.getParameter(iParam.getParameterName());
			String columnName = processPara.getColumnName();
			String propertyName = TypeConverterUtils.toPropertyName(columnName);
			GridFieldVO gridFieldVO = GridFieldVO.createParameter(Env.getCtx(), 0, 0, 0, 0, columnName, processPara.getName(), processPara.getAD_Reference_ID(), 
					processPara.getAD_Reference_Value_ID(), processPara.isMandatory(), false, "");
			gridFieldVO.AD_Column_ID = processPara.getAD_Process_Para_ID();
			GridField gridField = new GridField(gridFieldVO);
			JsonElement element = jsonObject.get(propertyName);
			if (element == null)
				element = jsonObject.get(columnName);
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
	
	/**
	 * 
	 * @param process
	 * @param pInstance
	 * @param jsonObject process info configs
	 * @return ProcessInfo
	 */
	public static ProcessInfo createProcessInfo(MProcess process, MPInstance pInstance, JsonObject jsonObject) {
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
	
	/**
	 * 
	 * @param processInfo
	 * @param processSlug
	 * @return JsonObject
	 */
	public static JsonObject toJsonObject(ProcessInfo processInfo, String processSlug) {
		JsonObject processInfoJson = new JsonObject();
		processInfoJson.addProperty("AD_PInstance_ID", processInfo.getAD_PInstance_ID());
		processInfoJson.addProperty("process", processSlug);
		processInfoJson.addProperty("summary", processInfo.getSummary());
		processInfoJson.addProperty("isError", processInfo.isError());
		if (processInfo.getPDFReport() != null) {
			processInfoJson.addProperty("reportFile", processInfo.getPDFReport().getAbsolutePath());
			processInfoJson.addProperty("reportFileLength", processInfo.getPDFReport().length());
		}
		if (processInfo.getExportFile() != null) {
			processInfoJson.addProperty("exportFile", processInfo.getExportFile().getAbsolutePath());
			processInfoJson.addProperty("exportFileLength", processInfo.getExportFile().length());
		}
		IClusterService service = ClusterUtil.getClusterService();
		if (service != null && service.getLocalMember() != null) {
			processInfoJson.addProperty("nodeId", service.getLocalMember().getId());
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
		
		return processInfoJson;
	}

}

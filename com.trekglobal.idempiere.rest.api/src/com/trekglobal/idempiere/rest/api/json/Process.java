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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
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
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
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
		
		//Save Process Instance
		pInstance.saveEx();

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
			File file = processInfo.getPDFReport();
			String property = "reportFile";
			addFile(processInfoJson, file, property, "pdf");
		}
		if (processInfo.getExportFile() != null) {
			File file = processInfo.getExportFile();
			String extension = processInfo.getExportFileExtension();
			String property = "exportFile";
			addFile(processInfoJson, file, property, extension);
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

	private static void addFile(JsonObject processInfoJson, File file, String property, String extension) {
		JsonElement jsonElement = null;
		JsonParser parser = new JsonParser();
		try {
			if ("json".equals(extension)) {
				FileReader reader = new FileReader(file.getAbsolutePath());
				JsonReader jsonReader = new JsonReader(reader);
				jsonElement = parser.parse(jsonReader);
			} else {
				// treat as binary
				ByteArrayOutputStream ba= loadFile(file);
				String base64String = StringUtils.newStringUtf8(Base64.encodeBase64(ba.toByteArray()));
				jsonElement = new JsonPrimitive(base64String);
			}
		} catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
			throw new AdempiereException("Could not create JSON element from file " + file.getAbsolutePath(), e);
		}
		if (jsonElement != null)
			processInfoJson.add(property, jsonElement);
		processInfoJson.addProperty(property + "Name", file.getName());
		processInfoJson.addProperty(property + "Length", file.length());
	}

	private static ByteArrayOutputStream loadFile(File file) throws FileNotFoundException {
		FileInputStream fis = new FileInputStream(file);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		try {
			for (int readNum; (readNum = fis.read(buf)) != -1;) {
				bos.write(buf, 0, readNum); //no doubt here is 0
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return bos;
	}

}

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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.DataStatusEvent;
import org.compiere.model.DataStatusListener;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.GridWindow;
import org.compiere.model.MField;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MQuery;
import org.compiere.model.MRole;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ServerProcessCtl;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.compiere.wf.MWorkflow;
import org.idempiere.db.util.SQLFragment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.IGridTabSerializer;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.json.Process;
import com.trekglobal.idempiere.rest.api.json.ResponseUtils;
import com.trekglobal.idempiere.rest.api.json.RestUtils;
import com.trekglobal.idempiere.rest.api.json.TypeConverterUtils;
import com.trekglobal.idempiere.rest.api.json.filter.ConvertedQuery;
import com.trekglobal.idempiere.rest.api.json.filter.IQueryConverter;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.util.GridTabPaging;
import com.trekglobal.idempiere.rest.api.util.Paging;
import com.trekglobal.idempiere.rest.api.util.ThreadLocalTrx;
import com.trekglobal.idempiere.rest.api.v1.resource.WindowResource;

/**
 * 
 * @author hengsin
 *
 */
public class WindowResourceImpl implements WindowResource {

	private static final int DEFAULT_PAGE_SIZE = 100;
	private static final CLogger log = CLogger.getCLogger(WindowResourceImpl.class);
	
	/** Default select values **/
	private static final String[] WINDOW_SELECT_COLUMNS = new String[] {"AD_Window_ID", "AD_Window_UU", "Name", "Description", "Help", "WindowType", "EntityType"};
	private static final String[] TAB_SELECT_COLUMNS = new String[] {"AD_Tab_ID", "AD_Tab_UU", "Name", "Description", "Help", "EntityType", "SeqNo", "TabLevel"};
	private static final String[] FIELD_SELECT_COLUMNS = new String[] {"AD_Field_ID", "AD_Field_UU", "Name", "Description", "Help", "EntityType", "AD_Reference_ID", "AD_Column_ID", "MandatoryLogic"};
	
	public WindowResourceImpl() {
	}

	@Override
	public Response getWindows(String filter, String details, String select) {
		IQueryConverter converter = IQueryConverter.getQueryConverter("DEFAULT");
		try {
			JsonArray windowArray = new JsonArray();
			ConvertedQuery convertedStatement = converter.convertStatement(MWindow.Table_Name, filter);
			if (log.isLoggable(Level.INFO)) log.info("Where Clause: " + convertedStatement.getWhereClause());

			Query query = new Query(Env.getCtx(), MWindow.Table_Name, convertedStatement.getWhereClause(), null);
			query.setApplyAccessFilter(true).setOnlyActiveRecords(true).setOrderBy("Name");
			query.setParameters(convertedStatement.getParameters());

			List<MWindow> windows = query.list();
			MRole role = MRole.getDefault();
			IPOSerializer serializer = IPOSerializer.getPOSerializer(MWindow.Table_Name, MTable.getClass(MWindow.Table_Name));

			HashMap<String, ArrayList<String>> includes = RestUtils.getIncludes(MWindow.Table_Name, select, details);
			String[] mainSelect = includes != null && includes.get(MWindow.Table_Name) != null ? 
					includes.get(MWindow.Table_Name).toArray(new String[includes.get(MWindow.Table_Name).size()]) : 
					WINDOW_SELECT_COLUMNS;

			for(MWindow window : windows) {
				if (role.getWindowAccess(window.getAD_Window_ID()) == null)
					continue;
				
				JsonObject jsonObject = serializer.toJson(window, mainSelect, null);
				jsonObject.addProperty("slug", TypeConverterUtils.slugify(window.getName()));
				if (!Util.isEmpty(details, true)) {
					boolean addTabs = details.contains(MTab.Table_Name);
					boolean addFields = details.contains(MField.Table_Name);
					if (addTabs) {
						jsonObject.add("tabs", getWindowTabs(window, includes, addFields));
					}
				}
				windowArray.add(jsonObject);
			}
			JsonObject json = new JsonObject();
			json.add("windows", windowArray);
			return Response.ok(json.toString()).build();
		} catch (Exception ex) {
			Status status = Status.INTERNAL_SERVER_ERROR;
			if (ex instanceof IDempiereRestException)
				status = ((IDempiereRestException) ex).getErrorResponseStatus();
			
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(status)
					.entity(new ErrorBuilder().status(status)
							.title("GET Error")
							.append("Get windows with exception: ")
							.append(ex.getMessage())
							.build().toString())
					.build();
		}
	}

	@Override
	public Response getTabs(String windowSlug) {
		MRole role = MRole.getDefault();
		Query query = new Query(Env.getCtx(), MWindow.Table_Name, "slugify(name)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		query.setParameters(windowSlug);
		MWindow window = query.first();
		if (window == null) {
			query.setApplyAccessFilter(false);
			window = query.first();
			if (window != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid window name").append("No match found for window name: ").append(windowSlug).build().toString())
						.build();
			}
		}
		
		if (role.getWindowAccess(window.getAD_Window_ID()) == null) {
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
					.build();
		}
		IPOSerializer serializer = IPOSerializer.getPOSerializer(MWindow.Table_Name, MTable.getClass(MWindow.Table_Name));
		JsonObject windowJsonObject = serializer.toJson(window, WINDOW_SELECT_COLUMNS, null);
		windowJsonObject.addProperty("slug", TypeConverterUtils.slugify(window.getName()));
		windowJsonObject.add("tabs", getWindowTabs(window, null, false));
		
		return Response.ok(windowJsonObject.toString()).build();
	}
	
	private JsonArray getWindowTabs(MWindow window, HashMap<String, ArrayList<String>> includes, boolean includeFields) {
		MTab[] tabs = window.getTabs(false, null);
		JsonArray tabArray = new JsonArray();
		IPOSerializer tabSerializer = IPOSerializer.getPOSerializer(MTab.Table_Name, MTable.getClass(MTab.Table_Name));
		
		String[] tabSelect = TAB_SELECT_COLUMNS;
		String[] fieldSelect = FIELD_SELECT_COLUMNS;
		if (includes != null) {
			if (includes.get(MTab.Table_Name) != null)
				tabSelect = includes.get(MTab.Table_Name).toArray(new String[includes.get(MTab.Table_Name).size()]);
			
			if (includeFields && includes.get(MField.Table_Name) != null)
				fieldSelect =  includes.get(MField.Table_Name).toArray(new String[includes.get(MField.Table_Name).size()]);
		}
	
		
		for(MTab tab : tabs) {
			JsonObject tabJsonObject = tabSerializer.toJson(tab, tabSelect, null);
			tabJsonObject.addProperty("slug", TypeConverterUtils.slugify(tab.getName()));
			
			if (includeFields) {
				MField[] fields = tab.getFields(false, null);
				JsonArray fieldArray = new JsonArray();
				IPOSerializer serializer = IPOSerializer.getPOSerializer(MField.Table_Name, MTable.getClass(MField.Table_Name));
				for(MField field : fields) {
					if (!field.isDisplayed())
						continue;
					JsonObject jsonObject = serializer.toJson(field, fieldSelect, null);
					fieldArray.add(jsonObject);
				}
				tabJsonObject.add("fields", fieldArray);
			}

			tabArray.add(tabJsonObject);
		}
		return tabArray;
	}

	@Override
	public Response getTabFields(String windowSlug, String tabSlug, String filter) {
		MRole role = MRole.getDefault();
		Query query = new Query(Env.getCtx(), MWindow.Table_Name, "slugify(name)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		query.setParameters(windowSlug);
		MWindow window = query.first();
		if (window == null) {
			query.setApplyAccessFilter(false);
			window = query.first();
			if (window != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid window name").append("No match found for window name: ").append(windowSlug).build().toString())
						.build();
			}
		}
		
		if (role.getWindowAccess(window.getAD_Window_ID()) == null) {
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
					.build();
		}
		
		MTab[] tabs = window.getTabs(false, null);
		int tabId=0;
		for(MTab tab : tabs) {
			if (TypeConverterUtils.slugify(tab.getName()).equals(tabSlug)) {
				tabId = tab.getAD_Tab_ID();
				break;
			}
		}
		if (tabId==0) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid tab name").append("No match found for tab name: ").append(tabSlug).build().toString())
					.build();
		}
		
		IQueryConverter converter = IQueryConverter.getQueryConverter("DEFAULT");
		try {
			StringBuilder whereClause = new StringBuilder("AD_Tab_ID=?");
			ConvertedQuery convertedStatement = converter.convertStatement(MField.Table_Name, filter);
			if (log.isLoggable(Level.INFO)) log.info("Where Clause: " + convertedStatement.getWhereClause());
			
			if (!Util.isEmpty(filter, true)) {
				whereClause.append(" AND (").append(convertedStatement.getWhereClause()).append(")");
			}
			query = new Query(Env.getCtx(), MField.Table_Name, whereClause.toString(), null);
			List<Object> prmCopy = new ArrayList<>(convertedStatement.getParameters());
			prmCopy.add(0, tabId);

			List<MField> fields = query.setParameters(prmCopy).list();
			JsonArray fieldArray = new JsonArray();
			IPOSerializer serializer = IPOSerializer.getPOSerializer(MField.Table_Name, MTable.getClass(MField.Table_Name));
			for(MField field : fields ) {
				JsonObject jsonObject = serializer.toJson(field, FIELD_SELECT_COLUMNS, null);
				fieldArray.add(jsonObject);
			}

			JsonObject json = new JsonObject();
			json.add("fields", fieldArray);
			return Response.ok(json.toString()).build();
		}  catch (Exception ex) {
			Status status = Status.INTERNAL_SERVER_ERROR;
			if (ex instanceof IDempiereRestException)
				status = ((IDempiereRestException) ex).getErrorResponseStatus();
			
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(status)
					.entity(new ErrorBuilder().status(status)
							.title("GET Error")
							.append("Get TabFields with exception: ")
							.append(ex.getMessage())
							.build().toString())
					.build();
		}
	}
	
	@Override
	public Response getWindowRecords(String windowSlug, String filter, String sortColumn, int pageNo) {
		try {
			return doGetWindowRecords(windowSlug, filter, sortColumn, pageNo);
		} catch (Exception ex) {
			return ResponseUtils.getResponseErrorFromException(ex, "Internal Server Error");
		}	
	}

	private Response doGetWindowRecords(String windowSlug, String filter, String sortColumn, int pageNo) {
		MRole role = MRole.getDefault();
		Query query = new Query(Env.getCtx(), MWindow.Table_Name, "slugify(name)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		query.setParameters(windowSlug);
		MWindow window = query.first();
		if (window == null) {
			query.setApplyAccessFilter(false);
			window = query.first();
			if (window != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid window name").append("No match found for window name: ").append(windowSlug).build().toString())
						.build();
			}
		}
		
		if (role.getWindowAccess(window.getAD_Window_ID()) == null) {
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
					.build();
		}
		
		GridWindow gridWindow = GridWindow.get(Env.getCtx(), 1, window.getAD_Window_ID());
		for(int i = 0; i < gridWindow.getTabCount(); i++) {
			GridTab gridTab = gridWindow.getTab(i);
			if (gridTab.getTabLevel()==0) {
				QueryResult queryResult = query(gridTab, filter, sortColumn, pageNo);
				JsonObject json = new JsonObject();
				json.addProperty("page-count", queryResult.pageCount);
				json.addProperty("page-size", queryResult.pageSize);
				json.addProperty("page-number", queryResult.pageNo);
				json.addProperty("row-count", queryResult.rowCount);
				json.add("window-records", queryResult.jsonArray);
				return Response.ok(json.toString())
						.header("X-Page-Count", queryResult.pageCount)
						.header("X-Page-Size", queryResult.pageSize)
						.header("X-Page-Number", queryResult.pageNo)
						.header("X-Row-Count", queryResult.rowCount)
						.build();
			}
		}
		
		return Response.status(Status.NOT_FOUND)
				.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid window name").append("No match found for window name: ").append(windowSlug).build().toString())
				.build();
	}
	
	@Override
	public Response getWindowRecord(String windowSlug, int recordId, String details) {
		return getTabRecord(windowSlug, null, recordId, details);
	}
	
	@Override
	public Response getChildTabRecords(String windowSlug, String tabSlug, int recordId, String childTabSlug,
			String filter, String sortColumn, int pageNo) {
		try {
			return doGetChildTabRecords(windowSlug, tabSlug, recordId, childTabSlug, filter, sortColumn, pageNo);
		} catch (Exception ex) {
			return ResponseUtils.getResponseErrorFromException(ex, "Internal Server Error");
		}
	}

	private Response doGetChildTabRecords(String windowSlug, String tabSlug, int recordId, String childTabSlug,
			String filter, String sortColumn, int pageNo) {
		MRole role = MRole.getDefault();
		Query query = new Query(Env.getCtx(), MWindow.Table_Name, "slugify(name)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		query.setParameters(windowSlug);
		MWindow window = query.first();
		if (window == null) {
			query.setApplyAccessFilter(false);
			window = query.first();
			if (window != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid window name").append("No match found for window name: ").append(windowSlug).build().toString())
						.build();
			}
		}
		
		if (role.getWindowAccess(window.getAD_Window_ID()) == null) {
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
					.build();
		}
		
		GridTab parentTab = null;
		GridWindow gridWindow = GridWindow.get(Env.getCtx(), 1, window.getAD_Window_ID());
		for(int i = 0; i < gridWindow.getTabCount(); i++) {
			GridTab gridTab = gridWindow.getTab(i);
			if (TypeConverterUtils.slugify(gridTab.getName()).equals(tabSlug)) {
				parentTab = gridTab;
				load(gridWindow, gridTab, recordId);
				break;
			}
		}
		
		if (parentTab == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid tab name").append("No match found for tab name: ").append(tabSlug).build().toString())
					.build();
		}
		
		if (parentTab.getRowCount() == 0) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found matching id ").append(recordId).build().toString())
					.build();
		}
		
		for(int i = 0; i < gridWindow.getTabCount(); i++) {
			GridTab gridTab = gridWindow.getTab(i);
			if (TypeConverterUtils.slugify(gridTab.getName()).equals(childTabSlug)) {
				if (gridTab.getTabLevel() != (parentTab.getTabLevel()+1)) {
					return Response.status(Status.BAD_REQUEST).entity(childTabSlug + " is not child tab of " + tabSlug).build();
				}
				if (!gridWindow.isTabInitialized(i))
					gridWindow.initTab(i);
				QueryResult queryResult = query(gridTab, filter, sortColumn, pageNo);
				JsonObject json = new JsonObject();
				json.addProperty("page-count", queryResult.pageCount);
				json.addProperty("page-size", queryResult.pageSize);
				json.addProperty("page-number", queryResult.pageNo);
				json.addProperty("row-count", queryResult.rowCount);
				json.add("childtab-records", queryResult.jsonArray);
				return Response.ok(json.toString())
						.header("X-Page-Count", queryResult.pageCount)
						.header("X-Page-Size", queryResult.pageSize)
						.header("X-Page-Number", queryResult.pageNo)
						.header("X-Row-Count", queryResult.rowCount)
						.build();
			}
		}
				
		return Response.status(Status.NOT_FOUND)
				.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid tab name").append("No match found for tab name: ").append(childTabSlug).build().toString())
				.build();
	}
	
	@Override
	public Response getTabRecord(String windowSlug, String tabSlug, int recordId, String details) {
		MRole role = MRole.getDefault();
		Query query = new Query(Env.getCtx(), MWindow.Table_Name, "slugify(name)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		query.setParameters(windowSlug);
		MWindow window = query.first();
		if (window == null) {
			query.setApplyAccessFilter(false);
			window = query.first();
			if (window != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid window name").append("No match found for window name: ").append(windowSlug).build().toString())
						.build();
			}
		}
		
		if (role.getWindowAccess(window.getAD_Window_ID()) == null) {
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
					.build();
		}
		
		JsonObject jsonObject = loadTabRecord(window, tabSlug, recordId, details);
		
		if (jsonObject != null)
			return Response.ok(jsonObject.toString()).build();
		else
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found matching id ").append(recordId).build().toString())
					.build();
	}
	
	@Override
	public Response updateWindowRecord(String windowSlug, int recordId, String jsonText) {
		return updateTabRecord(windowSlug, null, recordId, jsonText);
	}

	@Override
	public Response createWindowRecord(String windowSlug, String jsonText) {
		MRole role = MRole.getDefault();
		Query query = new Query(Env.getCtx(), MWindow.Table_Name, "slugify(name)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		query.setParameters(windowSlug);
		MWindow window = query.first();
		if (window == null) {
			query.setApplyAccessFilter(false);
			window = query.first();
			if (window != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid window name").append("No match found for window name: ").append(windowSlug).build().toString())
						.build();
			}
		}
		
		Boolean windowAccess = role.getWindowAccess(window.getAD_Window_ID());
		if ( windowAccess == null || windowAccess.booleanValue()==false) {
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
					.build();
		}
		
		Env.setContext(Env.getCtx(), 1, "IsSOTrx", window.isSOTrx());
		Gson gson = new GsonBuilder().create();
		JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
		GridWindow gridWindow = GridWindow.get(Env.getCtx(), 1, window.getAD_Window_ID());
		
		String threadLocalTrxName = ThreadLocalTrx.getTrxName();
		Trx trx = threadLocalTrxName != null ? Trx.get(threadLocalTrxName, false) : Trx.get(Trx.createTrxName(), true);
		try {
			if (threadLocalTrxName == null)
				trx.start();
			return createTabRecord(gridWindow, null, null, jsonObject, trx, (threadLocalTrxName != null));
		} catch (Exception ex) {
			trx.rollback();
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Server error").append("Server error with exception: ").append(ex.getMessage()).build().toString())
					.build();
		} finally {
			if (threadLocalTrxName == null)
				trx.close();
		}		
	}

	@Override
	public Response deleteWindowRecord(String windowSlug, int recordId) {
		return deleteTabRecord(windowSlug, null, recordId);
	}

	@Override
	public Response updateTabRecord(String windowSlug, String tabSlug, int recordId, String jsonText) {
		MRole role = MRole.getDefault();
		Query query = new Query(Env.getCtx(), MWindow.Table_Name, "slugify(name)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		query.setParameters(windowSlug);
		MWindow window = query.first();
		if (window == null) {
			query.setApplyAccessFilter(false);
			window = query.first();
			if (window != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid window name").append("No match found for window name: ").append(windowSlug).build().toString())
						.build();
			}
		}
		
		Boolean windowAccess = role.getWindowAccess(window.getAD_Window_ID());
		if ( windowAccess == null || windowAccess.booleanValue()==false) {
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
					.build();
		}
		
		Env.setContext(Env.getCtx(), 1, "IsSOTrx", window.isSOTrx());
		Gson gson = new GsonBuilder().create();
		JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);		
		GridWindow gridWindow = GridWindow.get(Env.getCtx(), 1, window.getAD_Window_ID());
		String threadLocalTrxName = ThreadLocalTrx.getTrxName();
		Trx trx = threadLocalTrxName != null ? Trx.get(threadLocalTrxName, false) : Trx.get(Trx.createTrxName(), true);
		try {
			if (threadLocalTrxName == null)
				trx.start();
			return updateTabRecord(gridWindow, tabSlug, recordId, jsonObject, trx, (threadLocalTrxName != null));
		} catch (Exception ex) {
			trx.rollback();
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Server error").append("Server error with exception: ").append(ex.getMessage()).build().toString())
					.build();
		} finally {
			if (threadLocalTrxName == null)
				trx.close();
		}		
	}

	@Override
	public Response createChildTabRecord(String windowSlug, String tabSlug, int recordId, String childTabSlug,
			String jsonText) {
		MRole role = MRole.getDefault();
		Query query = new Query(Env.getCtx(), MWindow.Table_Name, "slugify(name)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		query.setParameters(windowSlug);
		MWindow window = query.first();
		if (window == null) {
			query.setApplyAccessFilter(false);
			window = query.first();
			if (window != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid window name").append("No match found for window name: ").append(windowSlug).build().toString())
						.build();
			}
		}
		
		Boolean windowAccess = role.getWindowAccess(window.getAD_Window_ID());
		if ( windowAccess == null || windowAccess.booleanValue()==false) {
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
					.build();
		}
		
		Env.setContext(Env.getCtx(), 1, "IsSOTrx", window.isSOTrx());
		Gson gson = new GsonBuilder().create();
		JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
		GridTab parentTab = null;
		GridWindow gridWindow = GridWindow.get(Env.getCtx(), 1, window.getAD_Window_ID());
		for(int i = 0; i < gridWindow.getTabCount(); i++) {
			GridTab gridTab = gridWindow.getTab(i);
			if (TypeConverterUtils.slugify(gridTab.getName()).equals(tabSlug)) {
				parentTab = gridTab;
				load(gridWindow, gridTab, recordId);
				break;
			}
		}
		
		if (parentTab == null) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid tab name").append("No match found for tab name: ").append(tabSlug).build().toString())
					.build();
		}
		
		if (parentTab.getRowCount() == 0) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found matching id ").append(recordId).build().toString())
					.build();
		}
		
		String threadLocalTrxName = ThreadLocalTrx.getTrxName();
		Trx trx = threadLocalTrxName != null ? Trx.get(threadLocalTrxName, false) : Trx.get(Trx.createTrxName(), true);
		try {
			if (threadLocalTrxName == null)
				trx.start();
			return createTabRecord(gridWindow, parentTab, childTabSlug, jsonObject, trx, (threadLocalTrxName != null));
		} catch (Exception ex) {
			trx.rollback();
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Server error").append("Server error with exception: ").append(ex.getMessage()).build().toString())
					.build();
		} finally {
			if (threadLocalTrxName == null)
				trx.close();
		}
	}
	
	@Override
	public Response deleteTabRecord(String windowSlug, String tabSlug, int recordId) {
		MRole role = MRole.getDefault();
		Query query = new Query(Env.getCtx(), MWindow.Table_Name, "slugify(name)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		query.setParameters(windowSlug);
		MWindow window = query.first();
		if (window == null) {
			query.setApplyAccessFilter(false);
			window = query.first();
			if (window != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid window name").append("No match found for window name: ").append(windowSlug).build().toString())
						.build();
			}
		}
		
		Boolean windowAccess = role.getWindowAccess(window.getAD_Window_ID());
		if ( windowAccess == null || windowAccess.booleanValue()==false) {
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
					.build();
		}
		
		GridWindow gridWindow = GridWindow.get(Env.getCtx(), 1, window.getAD_Window_ID());
		for(int i = 0; i < gridWindow.getTabCount(); i++) {
			GridTab gridTab = gridWindow.getTab(i);
			if ((gridTab.getTabLevel()==0 && Util.isEmpty(tabSlug, true)) || 
				(TypeConverterUtils.slugify(gridTab.getName()).equals(tabSlug))) {
				if (gridTab.isReadOnly() || !gridTab.isDeleteRecord()) {
					return Response.status(Status.FORBIDDEN)
							.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Delete not allow").append("Delete not allow for tab: ").append(TypeConverterUtils.slugify(gridTab.getName())).build().toString())
							.build();
				}
				load(gridWindow, gridTab, recordId);
				if (gridTab.getRowCount() < 1) {
					return Response.status(Status.NOT_FOUND)
							.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found matching id: ").append(recordId).build().toString())
							.build();
				} else if (gridTab.getRowCount() > 1) {
					return Response.status(Status.BAD_REQUEST)
							.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("More than 1 match").append("More than 1 matching record for id: ").append(recordId).build().toString())
							.build();
				}
				ErrorDataStatusListener edsl = new ErrorDataStatusListener();
				gridTab.getTableModel().addDataStatusListener(edsl);
				try {
					String threadLocalTrxName = ThreadLocalTrx.getTrxName();
					if (threadLocalTrxName != null)
						gridTab.getTableModel().setImportingMode(true, threadLocalTrxName);
					if (gridTab.dataDelete()) {
						return Response.status(Status.OK).build();
					} else {
						String error = edsl.getError();
						return Response.status(Status.INTERNAL_SERVER_ERROR)
								.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Delete error")
										.append(!Util.isEmpty(error) ? "Server error with exception: " : "")
										.append(!Util.isEmpty(error) ? error : "")
										.build().toString())
								.build();
					}
				} finally {
					gridTab.getTableModel().removeDataStatusListener(edsl);
				}
			}
		}
		
		return Response.status(Status.NOT_FOUND)
				.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid tab name").append("No match found for tab name: ").append(tabSlug).build().toString())
				.build();
	}
	
	private QueryResult query(GridTab gridTab, String filter, String sortColumn, int pageNo) {
		IGridTabSerializer serializer = IGridTabSerializer.getGridTabSerializer(gridTab.getAD_Tab_UU());
		if (!Util.isEmpty(filter, true)) {
			IQueryConverter converter = IQueryConverter.getQueryConverter("DEFAULT");
			ConvertedQuery convertedStatement = converter.convertStatement(gridTab.getTableName(), filter);
			if (log.isLoggable(Level.INFO))
				log.info("Where Clause: " + convertedStatement.getWhereClause());
			MQuery gridTabQuery = new MQuery(gridTab.getTableName());
			var queryParam = convertedStatement.getParameters();
			// change boolean to Y/N
			List<Object> paramList = new ArrayList<>();
			for(Object param : queryParam) {
				if(param instanceof Boolean b) {
					paramList.add(b ? "Y" : "N");
				} else {
					paramList.add(param);
				}
			}
			gridTabQuery.addRestriction(
					new SQLFragment(convertedStatement.getWhereClause(), paramList));
			gridTab.setQuery(gridTabQuery);
			gridTab.query(false);
		} else {
			gridTab.query(false);
		}
		if (!Util.isEmpty(sortColumn, true)) {
			boolean ascending = true;
			if (sortColumn.startsWith("!")) {
				sortColumn = sortColumn.substring(1);
				ascending = false;
			}
			GridField[] fields = gridTab.getTableModel().getFields();
			for(int i = 0; i < fields.length; i++) {
				if (fields[i].getColumnName().equals(sortColumn)) {
					gridTab.getTableModel().sort(i, ascending);
					break;
				}
			}
		}
		JsonArray jsonArray = new JsonArray();
		Paging paging = new Paging(gridTab.getRowCount(), DEFAULT_PAGE_SIZE);
		GridTabPaging gridTabPaging = new GridTabPaging(gridTab, paging);
		if (pageNo > 0)
			paging.setActivePage(pageNo);
		int pageRowCount = gridTabPaging.getSize();
		for(int j = 0; j < pageRowCount; j++) {
			gridTabPaging.setCurrentRow(j);
			JsonObject jsonObject = serializer.toJson(gridTab);
			jsonObject.addProperty("slug", TypeConverterUtils.slugify(gridTab.getName()));
			jsonArray.add(jsonObject);
		}
		QueryResult queryResult = new QueryResult();
		queryResult.jsonArray = jsonArray;
		queryResult.rowCount = gridTab.getRowCount();
		queryResult.pageNo = paging.getActivePage();
		queryResult.pageCount = paging.getPageCount();
		queryResult.pageSize = paging.getPageSize();
		return queryResult;
	}
	
	private class QueryResult {
		JsonArray jsonArray;
		int rowCount;
		int pageNo;
		int pageCount;
		int pageSize;
	}
	
	private JsonObject loadTabRecord(MWindow window, String tabSlug, int recordId, String details) {
		JsonObject jsonObject = null;
		List<String> detailList = new ArrayList<>();
		if (!Util.isEmpty(details, true)) {
			String[] detailArray = details.split("[,]");
			detailList = Arrays.asList(detailArray);
		}
		GridWindow gridWindow = GridWindow.get(Env.getCtx(), 1, window.getAD_Window_ID());
		GridTab headerTab = null;
		for(int i = 0; i < gridWindow.getTabCount(); i++) {
			GridTab gridTab = gridWindow.getTab(i);
			if (gridTab.getTabLevel()==0 && Util.isEmpty(tabSlug, true)) {
				if (!gridWindow.isTabInitialized(i))
					gridWindow.initTab(i);
				IGridTabSerializer serializer = load(gridWindow, gridTab, recordId);
				if (gridTab.getRowCount()==1) {
					jsonObject = serializer.toJson(gridTab);
					headerTab = gridTab;
				} else {
					break;
				}
			} else if (!Util.isEmpty(tabSlug, true) && headerTab == null) {
				String slug = TypeConverterUtils.slugify(gridTab.getName());
				if (slug.equals(tabSlug)) {
					if (!gridWindow.isTabInitialized(i))
						gridWindow.initTab(i);
					IGridTabSerializer serializer = load(gridWindow, gridTab, recordId);
					if (gridTab.getRowCount()==1) {
						jsonObject = serializer.toJson(gridTab);
						headerTab = gridTab;
					} else {
						break;
					}
				}
			} else if (headerTab != null && gridTab.getTabLevel()==(headerTab.getTabLevel()+1)) {
				String slug = TypeConverterUtils.slugify(gridTab.getName());
				if (detailList.contains(slug)) {
					if (!gridWindow.isTabInitialized(i))
						gridWindow.initTab(i);
					JsonArray jsonArray = new JsonArray();
					gridTab.query(false);						
					if (gridTab.getRowCount() > 0) {
						IGridTabSerializer serializer = IGridTabSerializer.getGridTabSerializer(gridTab.getAD_Tab_UU());
						for(int j = 0; j < gridTab.getRowCount(); j++) {
							if (j > 0)
								gridTab.setCurrentRow(j);
							JsonObject childJsonObject = serializer.toJson(gridTab);
							jsonArray.add(childJsonObject);
						}
					}											
					jsonObject.add(slug, jsonArray);
				}
			} else if (headerTab != null && gridTab.getTabLevel() < headerTab.getTabLevel()) {
				break;
			} else if (headerTab != null && gridTab.getTabLevel() == headerTab.getTabLevel() && headerTab.getTabLevel()>0) {
				break;
			}
		}
		return jsonObject;
	}

	private IGridTabSerializer load(GridWindow gridWindow, GridTab gridTab, int recordId) {
		IGridTabSerializer serializer = IGridTabSerializer.getGridTabSerializer(gridTab.getAD_Tab_UU());
		if (gridTab.getTabLevel() == 0) {
			MQuery gridTabQuery = new MQuery(gridTab.getTableName());
			gridTabQuery.addRestriction(gridTab.getTableModel().getKeyColumnName(), "=", recordId);
			gridTab.setQuery(gridTabQuery);
			gridTab.query(false);
		} else {
			List<GridTab> parents = new ArrayList<>();
			GridTab parent = gridTab.getParentTab();
			while(parent != null) {
				parents.add(parent);
				parent = parent.getParentTab();
			}
			List<Integer> parentIds = new ArrayList<>();
			GridTab currentChild = gridTab;
			int currentChildId = recordId;
			for(GridTab p : parents) {
				int index = gridWindow.getTabIndex(currentChild);
				if (!gridWindow.isTabInitialized(index))
					gridWindow.initTab(index);
				String linkColumn = currentChild.getLinkColumnName();
				String keyColumn = currentChild.getTableModel().getKeyColumnName();
				int parentId = DB.getSQLValueEx(null, "SELECT " + linkColumn + " FROM " + currentChild.getTableName() + " WHERE " + keyColumn + "=?", currentChildId);
				parentIds.add(parentId);
				currentChild = p;
				currentChildId = parentId;
			}
			for(int i = parents.size()-1; i >= 0; i--) {
				GridTab p = parents.get(i);
				int index = gridWindow.getTabIndex(p);
				if (!gridWindow.isTabInitialized(index))
					gridWindow.initTab(index);
				int id = parentIds.get(i);
				MQuery query = new MQuery();
				query.addRestriction(p.getTableModel().getKeyColumnName(), "=", id);
				p.setQuery(query);
				p.query(false);
			}
			
			if (!gridTab.isCurrent())
				gridTab.query(false);
			MQuery gridTabQuery = new MQuery(gridTab.getTableName());
			gridTabQuery.addRestriction(gridTab.getTableModel().getKeyColumnName(), "=", recordId);
			gridTab.setQuery(gridTabQuery);
			gridTab.query(false);
		}
		return serializer;
	}
	
	private Response updateTabRecord(GridWindow gridWindow, String tabSlug, int recordId, JsonObject jsonObject, Trx trx, boolean threadLocalTrx)
			throws SQLException {
		Response errorResponse = null;
		GridTab headerTab = null;
		Map<String, JsonArray> childMap = new LinkedHashMap<String, JsonArray>();
		for(int i = 0; i < gridWindow.getTabCount(); i++) {
			GridTab gridTab = gridWindow.getTab(i);
			if (headerTab == null && 
				((gridTab.getTabLevel()==0 && Util.isEmpty(tabSlug, true)) || 
				 (TypeConverterUtils.slugify(gridTab.getName()).equals(tabSlug)))) {
				if (gridTab.isReadOnly()) {
					errorResponse = Response.status(Status.FORBIDDEN)
							.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Tab is readonly").append("Tab is readonly: ").append(tabSlug).build().toString())
							.build();
					break;
				}
				gridTab.getTableModel().setImportingMode(true, trx.getTrxName());
				IGridTabSerializer serializer = load(gridWindow, gridTab, recordId);
				if (gridTab.getRowCount() < 1) {
					errorResponse = Response.status(Status.NOT_FOUND)
							.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found matching id: ").append(recordId).build().toString())
							.build();
					break;
				} else if (gridTab.getRowCount() > 1) {
					errorResponse = Response.status(Status.BAD_REQUEST)
							.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("More than 1 match").append("More than 1 matching record for id: ").append(recordId).build().toString())
							.build();
					break;
				}				
				serializer.fromJson(jsonObject, gridTab);
				if (gridTab.needSave(true, true)) {
					ErrorDataStatusListener edsl = new ErrorDataStatusListener();
					gridTab.getTableModel().addDataStatusListener(edsl);
					try {
						if (!gridTab.dataSave(false))  {
							String error = edsl.getError();
							errorResponse = Response.status(Status.INTERNAL_SERVER_ERROR)
												.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Save error")
														.append(!Util.isEmpty(error) ? "Save error with exception: " : "")
														.append(!Util.isEmpty(error) ? error : "")
														.build().toString())
												.build();
							break;
						} else {
							gridTab.dataRefresh();
						}
					} finally {
						gridTab.getTableModel().removeDataStatusListener(edsl);
					}
				}
				headerTab = gridTab;
			} else if (headerTab != null && gridTab.getTabLevel()==(headerTab.getTabLevel()+1) && !gridTab.isReadOnly()) {
				String slug = TypeConverterUtils.slugify(gridTab.getName());
				JsonElement tabSlugElement = jsonObject.get(slug);					
				if (tabSlugElement != null && tabSlugElement.isJsonArray()) {
					if (!gridWindow.isTabInitialized(i))
						gridWindow.initTab(i);
					gridTab.getTableModel().setImportingMode(true, trx.getTrxName());
					IGridTabSerializer serializer = IGridTabSerializer.getGridTabSerializer(gridTab.getAD_Tab_UU());
					JsonArray childJsonArray = tabSlugElement.getAsJsonArray();
					JsonArray updatedArray = new JsonArray();
					final Boolean[] error = new Boolean[] {Boolean.FALSE};
					ErrorDataStatusListener edsl = new ErrorDataStatusListener();
					gridTab.getTableModel().addDataStatusListener(edsl);
					try {
						childJsonArray.forEach(e -> {
							if (e.isJsonObject() && !error[0].booleanValue()) {
								JsonObject childJsonObject = e.getAsJsonObject();
								if (!optLoad(gridTab, childJsonObject)) {
									if (!gridTab.dataNew(false)) {
										error[0] = Boolean.TRUE;
										return;
									}
									gridTab.setValue(gridTab.getLinkColumnName(), recordId);
								}
								serializer.fromJson(childJsonObject, gridTab);
								if (gridTab.needSave(true, true)) {
									if (!gridTab.dataSave(false))  {
										error[0] = Boolean.TRUE;
										return;
									} else {
										gridTab.dataRefresh(false);
									}
								}
								childJsonObject = serializer.toJson(gridTab);
								updatedArray.add(childJsonObject);
							}
						});
					} finally {
						gridTab.getTableModel().removeDataStatusListener(edsl);
					}
					if (error[0].booleanValue()) {
						String saveError = edsl.getError();
						errorResponse = Response.status(Status.INTERNAL_SERVER_ERROR)
											.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Save error")
													.append(!Util.isEmpty(saveError) ? "Save error with exception: " : "")
													.append(!Util.isEmpty(saveError) ? saveError : "")
													.build().toString())
											.build();
						break;
					}
					childMap.put(slug, updatedArray);
				}
			} else if (headerTab != null && gridTab.getTabLevel() < headerTab.getTabLevel()) {
				break;
			} else if (headerTab != null && gridTab.getTabLevel() == headerTab.getTabLevel() && headerTab.getTabLevel()>0) {
				break;
			}
		}
		
		if (errorResponse != null) {
			trx.rollback();
			return errorResponse;
		}
		
		String error = runDocAction(headerTab, jsonObject, trx.getTrxName());
		if (Util.isEmpty(error, true)) {
			if (!threadLocalTrx)
				trx.commit(true);
		} else {
			trx.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Can't perform document action").append("Encounter exception during execution of document action: ").append(error).build().toString())
					.build();
		}
		
		JsonObject updatedJsonObject = null;
		if (headerTab != null) {
			IGridTabSerializer serializer = IGridTabSerializer.getGridTabSerializer(headerTab.getAD_Tab_UU());
			updatedJsonObject = serializer.toJson(headerTab);
			if (childMap.size() > 0) {
				for(String slug : childMap.keySet()) {
					updatedJsonObject.add(slug, childMap.get(slug));
				}
			}
		}
		ResponseBuilder responseBuilder = Response.status(Status.OK);
		if (updatedJsonObject != null)
			return responseBuilder.entity(updatedJsonObject.toString()).build();
		else
			return responseBuilder.build();
	}

	private boolean optLoad(GridTab gridTab, JsonObject jsonObject) {
		JsonElement idElement = jsonObject.get("id");											
		if (idElement != null && idElement.isJsonPrimitive()) {
			if (!gridTab.isCurrent())
				gridTab.query(false);
			MQuery query = new MQuery();
			query.addRestriction(gridTab.getTableModel().getKeyColumnName(), "=", idElement.getAsInt());
			gridTab.setQuery(query);
			gridTab.query(false);
			return gridTab.getRowCount()==1;
		} else {
			JsonElement uidElement = jsonObject.get("uid");
			if (uidElement != null && uidElement.isJsonPrimitive()) {
				if (!gridTab.isCurrent())
					gridTab.query(false);
				MQuery query = new MQuery();
				String uidColumnName = PO.getUUIDColumnName(gridTab.getTableName());
				query.addRestriction(uidColumnName, "=", uidElement.getAsString());
				gridTab.setQuery(query);
				gridTab.query(false);
				return gridTab.getRowCount()==1;
			}
		}
		return false;
	}
	
	private Response createTabRecord(GridWindow gridWindow, GridTab parentTab, String tabSlug, JsonObject jsonObject, Trx trx, boolean threadLocalTrx) throws SQLException {
		GridTab headerTab = null;
		Map<String, JsonArray> childMap = new LinkedHashMap<String, JsonArray>();
		for(int i = 0; i < gridWindow.getTabCount(); i++) {
			GridTab gridTab = gridWindow.getTab(i);
			if (headerTab == null && 
				 ((gridTab.getTabLevel()==0 && Util.isEmpty(tabSlug, true)) || 
				  (TypeConverterUtils.slugify(gridTab.getName()).equals(tabSlug)))) {
				if (gridTab.isReadOnly() || !gridTab.isInsertRecord()) {
					return Response.status(Status.FORBIDDEN)
							.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Create not allow").append("Create not allow for tab: ").append(TypeConverterUtils.slugify(gridTab.getName())).build().toString())
							.build();
				}
				if (gridTab.getTabLevel() > 0 && parentTab == null) {
					return Response.status(Status.BAD_REQUEST)
							.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("No parent tab record").append("Can't create child tab record without parent tab record: ").append(tabSlug).build().toString())
							.build();
				}
				if (gridTab.getTabLevel() > 0 && parentTab != null && gridTab.getTabLevel() != (parentTab.getTabLevel()+1) ) {
					return Response.status(Status.BAD_REQUEST)
							.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Wrong parent tab").append(tabSlug)
									.append(" is not child tab of ").append(TypeConverterUtils.slugify(parentTab.getName())).build().toString())
							.build();
				}
				if (!gridWindow.isTabInitialized(i))
					gridWindow.initTab(i);
				gridTab.getTableModel().setImportingMode(true, trx.getTrxName());
				if (!gridTab.isCurrent()) {
					if (gridTab.getTabLevel() > 0) {
						gridTab.query(false);
					} else {
						MQuery query = new MQuery("");
			    		query.addRestriction(new SQLFragment("1=2"));
						query.setRecordCount(0);
						gridTab.setQuery(query);
						gridTab.query(false);
					}
				}
				IGridTabSerializer serializer = IGridTabSerializer.getGridTabSerializer(gridTab.getAD_Tab_UU());
				ErrorDataStatusListener edsl = new ErrorDataStatusListener();
				gridTab.getTableModel().addDataStatusListener(edsl);
				try {
					if (!gridTab.dataNew(false)) {
						String error = edsl.getError();
						return Response.status(Status.INTERNAL_SERVER_ERROR)
								.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Save error")
										.append(!Util.isEmpty(error) ? "Save error with exception: " : "")
										.append(!Util.isEmpty(error) ? error : "")
										.build().toString())
								.build();
					}
					serializer.fromJson(jsonObject, gridTab);
					if (!gridTab.dataSave(false))  {
						trx.rollback();
						String error = edsl.getError();
						return Response.status(Status.INTERNAL_SERVER_ERROR)
								.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Save error")
										.append(!Util.isEmpty(error) ? "Save error with exception: " : "")
										.append(!Util.isEmpty(error) ? error : "")
										.build().toString())
								.build();
					} else {
						gridTab.dataRefresh(false);
					}
				} finally {
					gridTab.removeDataStatusListener(edsl);
				}
				
				headerTab = gridTab;
			} else if (headerTab != null && gridTab.getTabLevel()==(headerTab.getTabLevel()+1) && !gridTab.isReadOnly()) {
				String tSlug = TypeConverterUtils.slugify(gridTab.getName());
				JsonElement tabSlugElement = jsonObject.get(tSlug);					
				if (tabSlugElement != null && tabSlugElement.isJsonArray()) {
					if (!gridWindow.isTabInitialized(i))
						gridWindow.initTab(i);
					gridTab.getTableModel().setImportingMode(true, trx.getTrxName());
					IGridTabSerializer serializer = IGridTabSerializer.getGridTabSerializer(gridTab.getAD_Tab_UU());
					JsonArray childJsonArray = tabSlugElement.getAsJsonArray();
					JsonArray updatedArray = new JsonArray();
					final Boolean[] error = new Boolean[] {Boolean.FALSE};
					final GridTab finalHeaderTab = headerTab;
					ErrorDataStatusListener edsl = new ErrorDataStatusListener();
					gridTab.getTableModel().addDataStatusListener(edsl);
					try {
						childJsonArray.forEach(e -> {
							if (e.isJsonObject() && !error[0].booleanValue()) {
								JsonObject childJsonObject = e.getAsJsonObject();
								if (!gridTab.isCurrent())
									gridTab.query(false);
								if (!gridTab.dataNew(false)) {
									error[0] = Boolean.TRUE;
									return;
								}
								gridTab.setValue(gridTab.getLinkColumnName(), finalHeaderTab.getKeyID(0));								
								serializer.fromJson(childJsonObject, gridTab);
								if (!gridTab.dataSave(false))  {
									error[0] = Boolean.TRUE;
									return;
								} else {
									gridTab.dataRefresh(false);
								}
								childJsonObject = serializer.toJson(gridTab);
								updatedArray.add(childJsonObject);
							}
						});
						if (error[0].booleanValue()) {
							trx.rollback();
							String msg = edsl.getError();
							return Response.status(Status.INTERNAL_SERVER_ERROR)
									.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Save error")
											.append(!Util.isEmpty(msg) ? "Save error with exception: " : "")
											.append(!Util.isEmpty(msg) ? msg : "")
											.build().toString())
									.build();
						}
						childMap.put(tSlug, updatedArray);
					} finally {
						gridTab.removeDataStatusListener(edsl);
					}
				}
			} else if (headerTab != null && gridTab.getTabLevel() < headerTab.getTabLevel()) {
				break;
			} else if (headerTab != null && gridTab.getTabLevel() == headerTab.getTabLevel() && headerTab.getTabLevel()>0) {
				break;
			}
		}
		
		String error = runDocAction(headerTab, jsonObject, trx.getTrxName());
		if (Util.isEmpty(error, true)) {
			if (!threadLocalTrx)
				trx.commit(true);
		} else {
			trx.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Can't perform document action").append("Encounter exception during execution of document action: ").append(error).build().toString())
					.build();
		}
		
		JsonObject updatedJsonObject = null;
		if (headerTab != null) {
			IGridTabSerializer serializer = IGridTabSerializer.getGridTabSerializer(headerTab.getAD_Tab_UU());
			updatedJsonObject = serializer.toJson(headerTab);
			if (childMap.size() > 0) {
				for(String slug : childMap.keySet()) {
					updatedJsonObject.add(slug, childMap.get(slug));
				}
			}
		}
		ResponseBuilder responseBuilder = Response.status(Status.CREATED);
		if (updatedJsonObject != null)
			return responseBuilder.entity(updatedJsonObject.toString()).build();
		else
			return responseBuilder.build();
	}
	
	private String runDocAction(GridTab gridTab, JsonObject jsonObject, String trxName) {
		if (gridTab == null)
			return null;
		
		PO po = gridTab.getTableModel().getPO(gridTab.getCurrentRow());
		if (po instanceof DocAction) {
			JsonElement docActionElement = jsonObject.get("doc-action");
			if (docActionElement != null) {
				String docAction = null;
				if (docActionElement.isJsonPrimitive()) {
					docAction = docActionElement.getAsString();
				} else if (docActionElement.isJsonObject()) {
					JsonObject docActionJsonObject = docActionElement.getAsJsonObject();
					docActionElement = docActionJsonObject.get("id");
					if (docActionElement != null && docActionElement.isJsonPrimitive()) {
						docAction = docActionElement.getAsString();
					}
				}
				if (!Util.isEmpty(docAction, true) && !DocAction.ACTION_None.equals(docAction)) {
					po.set_TrxName(trxName);
					ProcessInfo processInfo = MWorkflow.runDocumentActionWorkflow(po, docAction);
					if (processInfo.isError()) {
						return processInfo.getSummary();
					} else {
						try {
							po.saveEx();
						} catch (Exception ex) {
							log.log(Level.SEVERE, ex.getMessage(), ex);
							return ex.getMessage();
						}
					}
					gridTab.dataRefresh();
				}
			}
		}
		return null;
	}
	
	private class ErrorDataStatusListener implements DataStatusListener {
		private String error = null;
		
		@Override
		public void dataStatusChanged(DataStatusEvent e) {
			if (e.isError()) {
				String msg = e.getAD_Message();
				if (!Util.isEmpty(msg, true)) {
					error = Msg.getMsg(Env.getCtx(), msg);
				}
			}
		}
		
		public String getError() {
			return error;
		}
		
	}

	@Override
	public Response printWindowRecord(String windowSlug, int recordId, String reportType) {
		return printTabRecord(windowSlug, null, recordId, reportType);
	}

	@Override
	public Response printTabRecord(String windowSlug, String tabSlug, int recordId, String reportType) {
		MRole role = MRole.getDefault();
		Query query = new Query(Env.getCtx(), MWindow.Table_Name, "slugify(name)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		query.setParameters(windowSlug);
		MWindow window = query.first();
		if (window == null) {
			query.setApplyAccessFilter(false);
			window = query.first();
			if (window != null) {
				return ResponseUtils.getResponseError(Status.FORBIDDEN, 
						"Access denied", 
						"Access denied for window: ", 
						windowSlug);
			} else {
				return ResponseUtils.getResponseError(Status.NOT_FOUND, 
						"Invalid window name", 
						"No match found for window name: ", 
						windowSlug);
			}
		}
		
		if (role.getWindowAccess(window.getAD_Window_ID()) == null) {
			return ResponseUtils.getResponseError(Status.FORBIDDEN, 
					"Access denied", 
					"Access denied for window: ", 
					windowSlug);
		}
		
		GridWindow gridWindow = GridWindow.get(Env.getCtx(), 1, window.getAD_Window_ID());
		GridTab headerTab = null;
		for(int i = 0; i < gridWindow.getTabCount(); i++) {
			GridTab gridTab = gridWindow.getTab(i);
			if (gridTab.getTabLevel()==0 && Util.isEmpty(tabSlug, true)) {
				headerTab = gridTab;
				break;
			} else if (!Util.isEmpty(tabSlug, true)) {
				String slug = TypeConverterUtils.slugify(gridTab.getName());
				if (slug.equals(tabSlug)) {
					headerTab = gridTab;
					break;
				}
			}
		}
		
		if (headerTab == null)
			return ResponseUtils.getResponseError(Status.NOT_FOUND, 
					"Invalid tab name", 
					"No match found for tab name: ", 
					tabSlug);
		
		int AD_Process_ID = headerTab.getAD_Process_ID();
		if (AD_Process_ID == 0)
			return ResponseUtils.getResponseError(Status.NOT_FOUND, 
					"No print process", 
					"No print process have been defined for ", 
					tabSlug==null?"window":"tab");
		
		JsonObject jsonObject = loadTabRecord(window, tabSlug, recordId, null);
		
		if (jsonObject == null)
			return ResponseUtils.getResponseError(Status.NOT_FOUND, 
					"Record not found", 
					"No record found matching id ", 
					String.valueOf(recordId));
					
		MProcess process = MProcess.get(Env.getCtx(), AD_Process_ID);
		MPInstance pinstance = Process.createPInstance(process, new JsonObject(), false);
		JsonObject processConfig = new JsonObject();
		processConfig.addProperty("record-id", recordId);
		processConfig.addProperty("table-id", headerTab.getAD_Table_ID());
		if (!Util.isEmpty(reportType, true)) {
			processConfig.addProperty("report-type", reportType);
		}
		ProcessInfo processInfo = Process.createProcessInfo(process, pinstance, processConfig);
		ServerProcessCtl.process(processInfo, null);
		
		JsonObject processInfoJson = Process.toJsonObject(processInfo, TypeConverterUtils.slugify(process.getValue()));
		
		return Response.ok(processInfoJson.toString()).build();
	}
}

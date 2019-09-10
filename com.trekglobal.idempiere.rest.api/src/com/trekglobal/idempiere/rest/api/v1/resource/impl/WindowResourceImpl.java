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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.GridWindow;
import org.compiere.model.MField;
import org.compiere.model.MQuery;
import org.compiere.model.MRole;
import org.compiere.model.MTab;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.compiere.wf.MWorkflow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IGridTabSerializer;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.json.TypeConverterUtils;
import com.trekglobal.idempiere.rest.api.v1.resource.WindowResource;

/**
 * 
 * @author hengsin
 *
 */
public class WindowResourceImpl implements WindowResource {

	private static final int DEFAULT_PAGE_SIZE = 100;
	
	public WindowResourceImpl() {
	}

	@Override
	public Response getWindows(String filter) {
		JsonArray windowArray = new JsonArray();
		Query query = new Query(Env.getCtx(), MWindow.Table_Name, filter != null ? filter : "", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true).setOrderBy("Name");
		List<MWindow> windows = query.list();
		MRole role = MRole.getDefault();
		IPOSerializer serializer = IPOSerializer.getPOSerializer(MWindow.Table_Name, MTable.getClass(MWindow.Table_Name));
		for(MWindow window : windows) {
			if (role.getWindowAccess(window.getAD_Window_ID()) == null)
				continue;
				
			JsonObject jsonObject = serializer.toJson(window, new String[] {"AD_Window_ID", "AD_Window_UU", "Name", "Description", "Help", "WindowType", "EntityType"}, null);
			jsonObject.addProperty("slug", TypeConverterUtils.slugify(window.getName()));
			windowArray.add(jsonObject);
			
		}
		return Response.ok(windowArray.toString()).build();
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
				return Response.status(Status.FORBIDDEN).build();
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		}
		
		if (role.getWindowAccess(window.getAD_Window_ID()) == null) {
			return Response.status(Status.FORBIDDEN).build();
		}
		IPOSerializer serializer = IPOSerializer.getPOSerializer(MWindow.Table_Name, MTable.getClass(MWindow.Table_Name));
		JsonObject windowJsonObject = serializer.toJson(window, new String[] {"AD_Window_ID", "AD_Window_UU", "Name", "Description", "Help", "WindowType", "EntityType"}, null);
		windowJsonObject.addProperty("slug", TypeConverterUtils.slugify(window.getName()));
		MTab[] tabs = window.getTabs(false, null);
		JsonArray tabArray = new JsonArray();
		IPOSerializer tabSerializer = IPOSerializer.getPOSerializer(MTab.Table_Name, MTable.getClass(MTab.Table_Name));
		for(MTab tab : tabs) {
			JsonObject tabJsonObject = tabSerializer.toJson(tab, new String[] {"AD_Tab_ID", "AD_Tab_UU", "Name", "Description", "Help", "EntityType", "SeqNo", "TabLevel"}, null);
			tabJsonObject.addProperty("slug", TypeConverterUtils.slugify(tab.getName()));
			tabArray.add(tabJsonObject);
		}
		windowJsonObject.add("tabs", tabArray);
		
		return Response.ok(windowJsonObject.toString()).build();
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
				return Response.status(Status.FORBIDDEN).build();
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		}
		
		if (role.getWindowAccess(window.getAD_Window_ID()) == null) {
			return Response.status(Status.FORBIDDEN).build();
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
			return Response.status(Status.BAD_REQUEST).entity("Invalid Tab Slug: " + tabSlug).build();
		}
		
		StringBuilder whereClause = new StringBuilder("AD_Tab_ID=?");
		if (!Util.isEmpty(filter, true)) {
			whereClause.append(" AND (").append(filter).append(")");
		}
		query = new Query(Env.getCtx(), MField.Table_Name, whereClause.toString(), null);
		List<MField> fields = query.setParameters(tabId).list();
		JsonArray fieldArray = new JsonArray();
		IPOSerializer serializer = IPOSerializer.getPOSerializer(MField.Table_Name, MTable.getClass(MField.Table_Name));
		for(MField field : fields ) {
			JsonObject jsonObject = serializer.toJson(field, new String[] {"AD_Field_ID", "AD_Field_UU", "Name", "Description", "Help", "EntityType", "AD_Reference_ID", "AD_Column_ID"}, null);
			fieldArray.add(jsonObject);
		}
		
		return Response.ok(fieldArray.toString()).build();
	}
	
	@Override
	public Response getWindowRecords(String windowSlug, String filter, String sortColumn, int pageNo) {
		MRole role = MRole.getDefault();
		Query query = new Query(Env.getCtx(), MWindow.Table_Name, "slugify(name)=?", null);
		query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
		query.setParameters(windowSlug);
		MWindow window = query.first();
		if (window == null) {
			query.setApplyAccessFilter(false);
			window = query.first();
			if (window != null) {
				return Response.status(Status.FORBIDDEN).build();
			} else {
				return Response.status(Status.BAD_REQUEST).entity("Invalid window slug: " + windowSlug).build();
			}
		}
		
		if (role.getWindowAccess(window.getAD_Window_ID()) == null) {
			return Response.status(Status.FORBIDDEN).build();
		}
		
		GridWindow gridWindow = GridWindow.get(Env.getCtx(), 1, window.getAD_Window_ID());
		for(int i = 0; i < gridWindow.getTabCount(); i++) {
			GridTab gridTab = gridWindow.getTab(i);
			if (gridTab.getTabLevel()==0) {
				QueryResult queryResult = query(gridTab, filter, sortColumn, pageNo);
				return Response.ok(queryResult.jsonArray.toString())
						.header("X-Page-Count", queryResult.pageCount)
						.header("X-Page-Size", queryResult.pageSize)
						.header("X-Page-Number", queryResult.pageNo)
						.header("X-Row-Count", queryResult.rowCount)
						.build();
			}
		}
		
		return Response.status(Status.BAD_REQUEST).entity("Invalid window slug: " + windowSlug).build();
	}
	
	@Override
	public Response getWindowRecord(String windowSlug, int recordId, String details) {
		return getTabRecord(windowSlug, null, recordId, details);
	}
	
	@Override
	public Response getChildTabRecords(String windowSlug, String tabSlug, int recordId, String childTabSlug,
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
				return Response.status(Status.FORBIDDEN).build();
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		}
		
		if (role.getWindowAccess(window.getAD_Window_ID()) == null) {
			return Response.status(Status.FORBIDDEN).build();
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
			return Response.status(Status.BAD_REQUEST).entity("Invalid tab slug: " + tabSlug).build();
		}
		
		if (parentTab.getRowCount() == 0) {
			return Response.status(Status.NOT_FOUND).build();
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
				return Response.ok(queryResult.jsonArray.toString())
						.header("X-Page-Count", queryResult.pageCount)
						.header("X-Page-Size", queryResult.pageSize)
						.header("X-Page-Number", queryResult.pageNo)
						.header("X-Row-Count", queryResult.rowCount)
						.build();
			}
		}
				
		return Response.status(Status.BAD_REQUEST).entity("Invalid tab slug: " + childTabSlug).build();
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
				return Response.status(Status.FORBIDDEN).build();
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		}
		
		if (role.getWindowAccess(window.getAD_Window_ID()) == null) {
			return Response.status(Status.FORBIDDEN).build();
		}
		
		JsonObject jsonObject = loadTabRecord(window, tabSlug, recordId, details);
		
		if (jsonObject != null)
			return Response.ok(jsonObject.toString()).build();
		else
			return Response.status(Status.NOT_FOUND).build();
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
				return Response.status(Status.FORBIDDEN).build();
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		}
		
		Boolean windowAccess = role.getWindowAccess(window.getAD_Window_ID());
		if ( windowAccess == null || windowAccess.booleanValue()==false) {
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Gson gson = new GsonBuilder().create();
		JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
		GridWindow gridWindow = GridWindow.get(Env.getCtx(), 1, window.getAD_Window_ID());
		Trx trx = Trx.get(Trx.createTrxName(), true);
		try {
			trx.start();
			return createTabRecord(gridWindow, null, null, jsonObject, trx);
		} catch (Exception ex) {
			trx.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
		} finally {
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
				return Response.status(Status.FORBIDDEN).build();
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		}
		
		Boolean windowAccess = role.getWindowAccess(window.getAD_Window_ID());
		if ( windowAccess == null || windowAccess.booleanValue()==false) {
			return Response.status(Status.FORBIDDEN).build();
		}
		
		Gson gson = new GsonBuilder().create();
		JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);		
		GridWindow gridWindow = GridWindow.get(Env.getCtx(), 1, window.getAD_Window_ID());
		Trx trx = Trx.get(Trx.createTrxName(), true);
		try {
			trx.start();
			return updateTabRecord(gridWindow, tabSlug, recordId, jsonObject, trx);
		} catch (Exception ex) {
			trx.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
		} finally {
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
				return Response.status(Status.FORBIDDEN).build();
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		}
		
		Boolean windowAccess = role.getWindowAccess(window.getAD_Window_ID());
		if ( windowAccess == null || windowAccess.booleanValue()==false) {
			return Response.status(Status.FORBIDDEN).build();
		}
		
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
			return Response.status(Status.BAD_REQUEST).entity("Invalid tab slug: " + tabSlug).build();
		}
		
		if (parentTab.getRowCount() == 0) {
			return Response.status(Status.NOT_FOUND).build();
		}
		
		Trx trx = Trx.get(Trx.createTrxName(), true);
		try {
			trx.start();
			return createTabRecord(gridWindow, parentTab, childTabSlug, jsonObject, trx);
		} catch (Exception ex) {
			trx.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
		} finally {
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
				return Response.status(Status.FORBIDDEN).build();
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		}
		
		Boolean windowAccess = role.getWindowAccess(window.getAD_Window_ID());
		if ( windowAccess == null || windowAccess.booleanValue()==false) {
			return Response.status(Status.FORBIDDEN).build();
		}
		
		GridWindow gridWindow = GridWindow.get(Env.getCtx(), 1, window.getAD_Window_ID());
		for(int i = 0; i < gridWindow.getTabCount(); i++) {
			GridTab gridTab = gridWindow.getTab(i);
			if ((gridTab.getTabLevel()==0 && Util.isEmpty(tabSlug, true)) || 
				(TypeConverterUtils.slugify(gridTab.getName()).equals(tabSlug))) {
				if (gridTab.isReadOnly() || !gridTab.isDeleteRecord()) {
					return Response.status(Status.FORBIDDEN).build();
				}
				load(gridWindow, gridTab, recordId);
				if (gridTab.getRowCount() < 1) {
					return Response.status(Status.NOT_FOUND).build();
				} else if (gridTab.getRowCount() > 1) {
					return Response.status(Status.BAD_REQUEST).entity("More than one match for id " + recordId).build();
				}
				if (gridTab.dataDelete())
					return Response.status(Status.OK).build();
				else
					return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
		
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}
	
	private QueryResult query(GridTab gridTab, String filter, String sortColumn, int pageNo) {
		IGridTabSerializer serializer = IGridTabSerializer.getGridTabSerializer(gridTab.getAD_Tab_UU());
		if (!Util.isEmpty(filter, true)) {
			MQuery gridTabQuery = new MQuery(gridTab.getTableName());
			gridTabQuery.addRestriction(filter);
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
				int parentId = DB.getSQLValue(null, "SELECT " + linkColumn + " FROM " + currentChild.getTableName() + " WHERE " + keyColumn + "=?", currentChildId);
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
	
	private Response updateTabRecord(GridWindow gridWindow, String tabSlug, int recordId, JsonObject jsonObject, Trx trx)
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
					errorResponse = Response.status(Status.FORBIDDEN).build();
					break;
				}
				IGridTabSerializer serializer = load(gridWindow, gridTab, recordId);
				if (gridTab.getRowCount() < 1) {
					errorResponse = Response.status(Status.NOT_FOUND).build();
					break;
				} else if (gridTab.getRowCount() > 1) {
					errorResponse = Response.status(Status.BAD_REQUEST).entity("More than one match for id " + recordId).build();
					break;
				}
				gridTab.getTableModel().setImportingMode(true, trx.getTrxName());
				serializer.fromJson(jsonObject, gridTab);
				if (gridTab.needSave(true, true)) {
					if (!gridTab.dataSave(false))  {
						errorResponse = Response.status(Status.INTERNAL_SERVER_ERROR).build();
						break;
					} else {
						gridTab.dataRefresh();
					}
				}
				headerTab = gridTab;
			} else if (headerTab != null && gridTab.getTabLevel()==(headerTab.getTabLevel()+1) && !gridTab.isReadOnly()) {
				String slug = TypeConverterUtils.slugify(gridTab.getName());
				JsonElement tabSlugElement = jsonObject.get(slug);					
				if (tabSlugElement != null && tabSlugElement.isJsonArray()) {
					if (!gridWindow.isTabInitialized(i))
						gridWindow.initTab(i);
					IGridTabSerializer serializer = IGridTabSerializer.getGridTabSerializer(gridTab.getAD_Tab_UU());
					JsonArray childJsonArray = tabSlugElement.getAsJsonArray();
					JsonArray updatedArray = new JsonArray();
					final Boolean[] error = new Boolean[] {Boolean.FALSE};
					childJsonArray.forEach(e -> {
						if (e.isJsonObject() && !error[0].booleanValue()) {
							JsonObject childJsonObject = e.getAsJsonObject();
							if (!optLoad(gridTab, childJsonObject)) {
								gridTab.dataNew(false);
								gridTab.setValue(gridTab.getLinkColumnName(), recordId);
							}
							gridTab.getTableModel().setImportingMode(true, trx.getTrxName());
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
					if (error[0].booleanValue()) {
						errorResponse = Response.status(Status.INTERNAL_SERVER_ERROR).build();
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
			trx.commit(true);
		} else {
			trx.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build();
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
	
	private Response createTabRecord(GridWindow gridWindow, GridTab parentTab, String tabSlug, JsonObject jsonObject, Trx trx) throws SQLException {
		GridTab headerTab = null;
		Map<String, JsonArray> childMap = new LinkedHashMap<String, JsonArray>();
		for(int i = 0; i < gridWindow.getTabCount(); i++) {
			GridTab gridTab = gridWindow.getTab(i);
			if (headerTab == null && 
				 ((gridTab.getTabLevel()==0 && Util.isEmpty(tabSlug, true)) || 
				  (TypeConverterUtils.slugify(gridTab.getName()).equals(tabSlug)))) {
				if (gridTab.isReadOnly() || !gridTab.isInsertRecord()) {
					return Response.status(Status.FORBIDDEN).build();
				}
				if (gridTab.getTabLevel() > 0 && parentTab == null) {
					return Response.status(Status.BAD_REQUEST).build();
				}
				if (gridTab.getTabLevel() > 0 && parentTab != null && gridTab.getTabLevel() != (parentTab.getTabLevel()+1) ) {
					return Response.status(Status.BAD_REQUEST).entity(tabSlug + " is not child tab of " + TypeConverterUtils.slugify(parentTab.getName())).build();
				}
				if (!gridWindow.isTabInitialized(i))
					gridWindow.initTab(i);
				if (!gridTab.isCurrent())
					gridTab.query(false);
				IGridTabSerializer serializer = IGridTabSerializer.getGridTabSerializer(gridTab.getAD_Tab_UU());
				gridTab.getTableModel().setImportingMode(true, trx.getTrxName());
				gridTab.dataNew(false);
				serializer.fromJson(jsonObject, gridTab);
				if (!gridTab.dataSave(false))  {
					trx.rollback();
					return Response.status(Status.INTERNAL_SERVER_ERROR).build();
				} else {
					gridTab.dataRefresh(false);
				}
				
				headerTab = gridTab;
			} else if (headerTab != null && gridTab.getTabLevel()==(headerTab.getTabLevel()+1) && !gridTab.isReadOnly()) {
				String tSlug = TypeConverterUtils.slugify(gridTab.getName());
				JsonElement tabSlugElement = jsonObject.get(tSlug);					
				if (tabSlugElement != null && tabSlugElement.isJsonArray()) {
					if (!gridWindow.isTabInitialized(i))
						gridWindow.initTab(i);
					IGridTabSerializer serializer = IGridTabSerializer.getGridTabSerializer(gridTab.getAD_Tab_UU());
					JsonArray childJsonArray = tabSlugElement.getAsJsonArray();
					JsonArray updatedArray = new JsonArray();
					final Boolean[] error = new Boolean[] {Boolean.FALSE};
					final GridTab finalHeaderTab = headerTab;
					childJsonArray.forEach(e -> {
						if (e.isJsonObject() && !error[0].booleanValue()) {
							JsonObject childJsonObject = e.getAsJsonObject();
							gridTab.dataNew(false);
							gridTab.setValue(gridTab.getLinkColumnName(), finalHeaderTab.getKeyID(0));								
							gridTab.getTableModel().setImportingMode(true, trx.getTrxName());
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
						return Response.status(Status.INTERNAL_SERVER_ERROR).build();
					}
					childMap.put(tSlug, updatedArray);
				}
			} else if (headerTab != null && gridTab.getTabLevel() < headerTab.getTabLevel()) {
				break;
			} else if (headerTab != null && gridTab.getTabLevel() == headerTab.getTabLevel() && headerTab.getTabLevel()>0) {
				break;
			}
		}
		
		String error = runDocAction(headerTab, jsonObject, trx.getTrxName());
		if (Util.isEmpty(error, true)) {
			trx.commit(true);
		} else {
			trx.rollback();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(error).build();
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
			JsonElement docActionElement = jsonObject.get("docAction");
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
					if (processInfo.isError())
						return processInfo.getSummary();
					else
						po.saveEx();
					gridTab.dataRefresh();
				}
			}
		}
		return null;
	}
}

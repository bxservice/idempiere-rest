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
* - BX Service GmbH                                                   *
* - Diego Ruiz                                                        *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.json.expand;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.MTree_Base;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.json.ModelHelper;
import com.trekglobal.idempiere.rest.api.json.POParser;
import com.trekglobal.idempiere.rest.api.json.RestUtils;
import com.trekglobal.idempiere.rest.api.model.MRestView;
import com.trekglobal.idempiere.rest.api.model.MRestViewColumn;
import com.trekglobal.idempiere.rest.api.model.MRestViewRelated;

public class ExpandParser {
	
	private PO po;
	private String expandParameter = null;
	private String showlabel = null;
	private String masterTableName = null;
	private Map<String, String> tableNameSQLStatementMap = new HashMap<>();
	private Map<String, JsonElement> tableNameChildArrayMap = new HashMap<>();
	private MRestView view;
	private final static CLogger log = CLogger.getCLogger(ExpandParser.class);
	
	public ExpandParser(PO po, String expandParameter) {
		this(po, null, expandParameter);
	}
	
	public ExpandParser(PO po, MRestView view, String expandParameter) {
		this(po, view, expandParameter, null);
	}
	
	public ExpandParser(PO po, MRestView view, String expandParameter, String showlabel) {
		this.po = po;
		this.view = view;
		this.expandParameter = expandParameter;
		this.showlabel = showlabel;
		masterTableName = po.get_TableName();
		expandRelatedResources();
	}

	private void expandRelatedResources() {
		if (Util.isEmpty(expandParameter, true))
			return;
		
		HashMap<String, List<String>>  detailTablesWithOperators = ExpandUtils.getTableNamesOperatorsMap(expandParameter);
		for (Map.Entry<String,List<String>> entry : detailTablesWithOperators.entrySet()) {
			expandRelatedResource(entry.getKey(), entry.getValue());
		}
	}
	
	private void expandRelatedResource(String relatedResource, List<String> operators) {
		if (isExpandMaster(relatedResource))
			expandMaster(relatedResource, operators);
		else
			expandDetail(relatedResource, operators);
	}
	
	private boolean isExpandMaster(String relatedResource) {
		String columnName = relatedResource;
		if (view != null) {
			//find matching column or property name from view definition
			MRestViewColumn[] columns = view.getColumns();
			boolean found = false;
			for (MRestViewColumn column : columns) {
				String viewColumn = MColumn.getColumnName(Env.getCtx(), column.getAD_Column_ID());
				if (column.getName().equals(columnName)) {
					columnName = viewColumn;
					found = true;
					break;
				}
			}
			if (!found)
				return false;
		}
		int columnIndex = po.get_ColumnIndex(columnName);
		if (columnIndex < 0)
			return false;
		
		//check expand tree node
		if ("Node_ID".equalsIgnoreCase(columnName)) {
			MTable masterTable = MTable.get(Env.getCtx(), masterTableName);
			if (masterTable.getColumn("AD_Tree_ID") != null && masterTable.getColumn("AD_Table_ID") != null) 
				return true;
		}
		
		MColumn column = MColumn.get(Env.getCtx(), masterTableName, columnName);
		if (column == null || Util.isEmpty(column.getReferenceTableName())) {
			throw new IDempiereRestException("Expand error", "Column " + relatedResource + " cannot be expanded", Status.BAD_REQUEST);
		}
		
		return true;
	}
	
	private void expandMaster(String columnName, List<String> operators) {
		checkMasterExpandOperators(operators);
		MRestView referenceView = null;
		MRestViewColumn restViewColumn = null;
		if (view != null) {
			//find matching view column and reference view (if set)
			MRestViewColumn[] columns = view.getColumns();
			for (MRestViewColumn column : columns) {
				String viewColumn = MColumn.getColumnName(Env.getCtx(), column.getAD_Column_ID());
				if (column.getName().equals(columnName)) {
					columnName = viewColumn;
					if (column.getREST_ReferenceView_ID() > 0) {
						referenceView = new MRestView(Env.getCtx(), column.getREST_ReferenceView_ID(), null);
					}
					restViewColumn = column;
					break;
				}
			}
		}
		MColumn column = MColumn.get(Env.getCtx(), masterTableName, columnName);
		String tableName = referenceView != null ? MTable.getTableName(Env.getCtx(), referenceView.getAD_Table_ID()) : column.getReferenceTableName();
		//handle ad_treenode and ad_tree
		if ("Node_ID".equalsIgnoreCase(columnName) && po.get_ValueAsInt("AD_Tree_ID") > 0) {
			tableName = MTree_Base.get(po.get_ValueAsInt("AD_Tree_ID")).getSourceTableName(true);
		}
		String foreignTableID = po.get_ValueAsString(columnName);
		if (Util.isEmpty(foreignTableID) && po.isPartial() && !po.isColumnLoaded(columnName)) {
			// the foreign key was not included, reload the PO
			log.warning("For performance reasons, it is recommended to include foreign keys in the $select clause when expanding master records. Reloaded PO to get value for column: " + columnName);
			po.load(po.get_TrxName());
			foreignTableID = po.get_ValueAsString(columnName);
		}
		
		Query query = RestUtils.getQuery(tableName, foreignTableID, true, false);

		PO po = query.first();
		addSQLStatementToMap(tableName, columnName, query.getSQL());

		POParser poParser = new POParser(tableName, foreignTableID, po);
		if (poParser.isValidPO()) {
			IPOSerializer serializer = IPOSerializer.getPOSerializer(tableName, po.getClass());
			String select = ExpandUtils.getSelectClause(operators);
			String[] includes = RestUtils.getSelectedColumns(referenceView, tableName, select); 
			if (includes != null && includes.length > 0)
				query.selectColumns(includes);
			JsonObject json = serializer.toJson(po, referenceView, includes, null);
			if (showlabel != null)
				ExpandUtils.addAssignedLabelsToJson(po, showlabel, json);
			tableNameChildArrayMap.put(restViewColumn != null ? restViewColumn.getName() : column.getColumnName(), json);
		}
	}
	
	private void expandDetail(String detailEntity, List<String> operators) {
		String[] includes;

		MRestViewRelated restViewRelated = null;
		MRestView detailView = null;
		if (view != null) {
			//find detail view definition
			MRestViewRelated[] relateds = view.getRelatedViews();
			for (MRestViewRelated related : relateds) {
				if (related.getName().equals(detailEntity)) {
					detailView = new MRestView(Env.getCtx(), related.getREST_RelatedRestView_ID(), null);
					detailEntity = MTable.getTableName(Env.getCtx(), detailView.getAD_Table_ID());
					restViewRelated = related;
					break;
				}
			}
			if (detailView == null)
				return;
		}
		String[] tableNameKeyColumnName = getTableNameAndKeyColumnName(detailEntity);
		String tableName = tableNameKeyColumnName[0];
		String parentKeyColumn = tableNameKeyColumnName[1];
		String childKeyColumn = parentKeyColumn;
		if (parentKeyColumn.contains(":")) {
			String[] pcKeys = parentKeyColumn.split("[:]");
			parentKeyColumn = pcKeys[0];
			childKeyColumn = pcKeys[1];
		}

		String select = ExpandUtils.getSelectClause(operators);
		includes = RestUtils.getSelectedColumns(detailView, tableName, select);
		List<PO> childPOs = getChildPOs(operators, tableName, parentKeyColumn, childKeyColumn, includes);
		if (childPOs != null && childPOs.size() > 0) {
			JsonArray childArray = new JsonArray();
			IPOSerializer serializer = IPOSerializer.getPOSerializer(tableName, MTable.getClass(tableName));

			for (PO child : childPOs) {
				JsonObject childJsonObject = serializer.toJson(child, detailView, includes, new String[] {childKeyColumn, "model-name"});
				expandChildDetails(child, ExpandUtils.getExpandClause(operators), childJsonObject, detailView);
				//expand tree
				if (po.get_Table_ID() == child.get_Table_ID()) {
					ExpandParser expandParser = new ExpandParser(child, view, expandParameter);
					ExpandUtils.addDetailDataToJson(expandParser.getTableNameChildArrayMap(), childJsonObject);
				}
				if (showlabel != null)
					ExpandUtils.addAssignedLabelsToJson(child, showlabel, childJsonObject);
				childArray.add(childJsonObject);
			}
			tableNameChildArrayMap.put(restViewRelated != null ? restViewRelated.getName() : detailEntity, childArray);
		}
	}
	
	private void checkMasterExpandOperators(List<String> operators) {
		
		if (!Util.isEmpty(ExpandUtils.getFilterClause(operators)) ||
				!Util.isEmpty(ExpandUtils.getOrderByClause(operators)) || 
				!Util.isEmpty(ExpandUtils.getExpandClause(operators)) ||
				ExpandUtils.getTopClause(operators) > 0 ||
				ExpandUtils.getSkipClause(operators) > 0 ||
				!Util.isEmpty(ExpandUtils.getLabelClause(operators)))
			throw new IDempiereRestException("Expand error", "Expanding a master only support the $select query operator", Status.BAD_REQUEST);

	}
	
	private String[] getTableNameAndKeyColumnName(String detailEntity) {
		String[] tableNameKeyColumnName = new String[2];
		tableNameKeyColumnName[0] = getTableName(detailEntity);
		tableNameKeyColumnName[1] = getKeyColumnName(detailEntity);
		String keyColumn = tableNameKeyColumnName[1];
		if (keyColumn.contains(":"))
			keyColumn = keyColumn.split("[:]")[0];
		
		if (usesDifferentFK(detailEntity) && 
				!isValidTableAndKeyColumn(tableNameKeyColumnName[0], keyColumn))
			throw new IDempiereRestException("Expand error", 
					"Column: " +  tableNameKeyColumnName[1] + " is not a valid FK for table: " + masterTableName
					, Status.BAD_REQUEST);

		return tableNameKeyColumnName;
	}
	
	private String getKeyColumnName(String detailEntity) {
		return usesDifferentFK(detailEntity) ? detailEntity.substring(detailEntity.indexOf(".") + 1)
				: RestUtils.getLinkKeyColumnName(masterTableName, detailEntity);
	}
	
	private String getTableName(String detailEntity) {
		return usesDifferentFK(detailEntity) ? detailEntity.substring(0, detailEntity.indexOf(".")) : detailEntity;
	}
	
	private boolean usesDifferentFK(String detailEntity) {
		return detailEntity.contains(".");
	}
	
	private boolean isValidTableAndKeyColumn(String tableName, String keyColumnName) {
		MTable table = RestUtils.getTableAndCheckAccess(tableName);
		MColumn column = table.getColumn(keyColumnName);

		return column != null && 
				(ExpandUtils.isRecordIDTableIDFK(keyColumnName) || masterTableName.equalsIgnoreCase(column.getReferenceTableName())
				 || MColumn.get(Env.getCtx(), masterTableName, keyColumnName) != null);
	}	

	private List<PO> getChildPOs(List<String> operators, String tableName, String parentKeyColumn, String childKeyColumn, String[] includes) {
		ModelHelper modelHelper = getModelHelper(operators, tableName, parentKeyColumn, childKeyColumn);
		List<PO> poList = modelHelper.getPOsFromRequest(includes);
		addSQLStatementToMap(tableName, childKeyColumn, modelHelper.getSQLStatement());

		return poList;
	}
	
	private ModelHelper getModelHelper(List<String> operators, String tableName, String parentKeyColumn, String childKeyColumn) {
		MColumn column = MColumn.get(Env.getCtx(), tableName, childKeyColumn);
		if (column == null)
			throw new IDempiereRestException("Invalid column for expand: " + childKeyColumn, Status.BAD_REQUEST);
		Serializable parentId = getParentId(column, childKeyColumn, parentKeyColumn);
		
		String filter = getFilterClause(operators, childKeyColumn, parentId);
		String orderBy = ExpandUtils.getOrderByClause(operators);
		int top = ExpandUtils.getTopClause(operators);
		int skip = ExpandUtils.getSkipClause(operators);
		String label = ExpandUtils.getLabelClause(operators);

		return new ModelHelper(tableName, filter, orderBy, top, skip, null, null, label);
	}
	
	private Serializable getParentId(MColumn column, String childKeyColumn, String parentKeyColumn) {
	    if (masterTableName.equalsIgnoreCase(column.getReferenceTableName()) || ExpandUtils.isRecordIDTableIDFK(childKeyColumn)) {
	        return getIdForTable(MTable.get(po.get_Table_ID()), po, null);
	    }
	    
	    MTable referenceTable = MTable.get(Env.getCtx(), column.getReferenceTableName());
	    if (referenceTable != null) {
	        return getIdForTable(referenceTable, po, parentKeyColumn);
	    }
	    
	    return po.get_ID(); // fallback to normal ID
	}
	
	private Serializable getIdForTable(MTable table, PO po, String keyColumn) {
	    if (table.isUUIDKeyTable()) {
	        return keyColumn != null ? po.get_ValueAsString(keyColumn) : po.get_UUID();
	    }
	    return keyColumn != null ? po.get_ValueAsInt(keyColumn) : po.get_ID();
	}
	
	public String getFilterClause(List<String> operators, String keyColumnName, Serializable keyColumnValue) {
		StringBuilder filterClause = new StringBuilder(keyColumnName + " eq ");
		if (keyColumnValue instanceof String keyColumnValueString)
			filterClause.append(DB.TO_STRING(keyColumnValueString));
		else
			filterClause.append(keyColumnValue);
		
		if (   ExpandUtils.isRecordIDTableIDFK(keyColumnName)
			|| ExpandUtils.isRecordUUTableUUFK(keyColumnName))
			filterClause.append(" AND ").append(ExpandUtils.TABLE_ID_COLUMN).append(" eq ").append(po.get_Table_ID());

		String requestFilterClause = ExpandUtils.getFilterClause(operators);
		if (!Util.isEmpty(requestFilterClause)) 
			filterClause.append(" AND ").append(requestFilterClause);
		
		return filterClause.toString();
	}
	
	private void expandChildDetails(PO childPO, String expandClause, JsonObject childJson, MRestView detailView) {
		if (Util.isEmpty(expandClause))
			return;

		ExpandParser expandParser = new ExpandParser(childPO, detailView, expandClause);
		ExpandUtils.addDetailDataToJson(expandParser.getTableNameChildArrayMap(), childJson);
		addChildDetailSQLToMap(expandParser.getTableNameSQLStatementMap());
	}
	
	private void addChildDetailSQLToMap(Map<String, String> sqlStatementMap) {
		for (Map.Entry<String,String> entry : sqlStatementMap.entrySet()) {
			String tableName = entry.getKey();
			String sqlStatement = entry.getValue();
			addSQLStatementToMap(tableName, null, sqlStatement);
		}
	}
	
	private void addSQLStatementToMap(String tableName, String columnName, String sqlStatement) {
		String key = columnName != null ? tableName + "[" + columnName + "]" : tableName;
		if (tableNameSQLStatementMap.get(key) == null)
			tableNameSQLStatementMap.put(key, sqlStatement);
	}

	public Map<String, String> getTableNameSQLStatementMap() {
		return tableNameSQLStatementMap;
	}
	
	public Map<String, JsonElement> getTableNameChildArrayMap() {
		return tableNameChildArrayMap;
	}
}

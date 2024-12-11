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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
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

public class ExpandParser {
	
	private PO po;
	private String expandParameter = null;
	private String masterTableName = null;
	private Map<String, String> tableNameSQLStatementMap = new HashMap<>();
	private Map<String, JsonElement> tableNameChildArrayMap = new HashMap<>();
	
	public ExpandParser(PO po, String expandParameter) {
		this.po = po;
		this.expandParameter = expandParameter;
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
		int columnIndex = po.get_ColumnIndex(relatedResource);
		if (columnIndex < 0)
			return false;
		
		MColumn column = MColumn.get(Env.getCtx(), masterTableName, relatedResource);
		if (column == null || Util.isEmpty(column.getReferenceTableName())) {
			throw new IDempiereRestException("Expand error", "Column " + relatedResource + " cannot be expanded", Status.BAD_REQUEST);
		}
		
		return true;
	}
	
	private void expandMaster(String columnName, List<String> operators) {
		checkMasterExpandOperators(operators);
		
		MColumn column = MColumn.get(Env.getCtx(), masterTableName, columnName);
		String tableName = column.getReferenceTableName();
		String foreignTableID = po.get_ValueAsString(columnName);
		
		Query query = RestUtils.getQuery(tableName, foreignTableID, true, false);

		String select = ExpandUtils.getSelectClause(operators);
		String[] includes = RestUtils.getSelectedColumns(tableName, select);
		if (includes != null && includes.length > 0)
			query.selectColumns(includes);
		
		PO po = query.first();
		addSQLStatementToMap(tableName, columnName, query.getSQL());

		POParser poParser = new POParser(tableName, foreignTableID, po);
		if (poParser.isValidPO()) {
			IPOSerializer serializer = IPOSerializer.getPOSerializer(tableName, po.getClass());
			JsonObject json = serializer.toJson(po, includes, null);
			tableNameChildArrayMap.put(column.getColumnName(), json);
		}
	}
	
	private void expandDetail(String detailEntity, List<String> operators) {
		String[] includes;

		String[] tableNameKeyColumnName = getTableNameAndKeyColumnName(detailEntity);
		String tableName = tableNameKeyColumnName[0];
		String keyColumn = tableNameKeyColumnName[1];		

		String select = ExpandUtils.getSelectClause(operators);
		includes = RestUtils.getSelectedColumns(tableName, select);
		List<PO> childPOs = getChildPOs(operators, tableName, keyColumn, includes);
		if (childPOs != null && childPOs.size() > 0) {
			JsonArray childArray = new JsonArray();
			IPOSerializer serializer = IPOSerializer.getPOSerializer(tableName, MTable.getClass(tableName));

			for (PO child : childPOs) {
				JsonObject childJsonObject = serializer.toJson(child, includes, new String[] {keyColumn, "model-name"});
				expandChildDetails(child, ExpandUtils.getExpandClause(operators), childJsonObject);
				childArray.add(childJsonObject);
			}
			tableNameChildArrayMap.put(detailEntity, childArray);
		}
	}
	
	private void checkMasterExpandOperators(List<String> operators) {
		
		if (!Util.isEmpty(ExpandUtils.getFilterClause(operators)) ||
				!Util.isEmpty(ExpandUtils.getOrderByClause(operators)) || 
				!Util.isEmpty(ExpandUtils.getExpandClause(operators)) ||
				ExpandUtils.getTopClause(operators) > 0 ||
				ExpandUtils.getSkipClause(operators) > 0)
			throw new IDempiereRestException("Expand error", "Expanding a master only support the $select query operator", Status.BAD_REQUEST);

	}
	
	private String[] getTableNameAndKeyColumnName(String detailEntity) {
		String[] tableNameKeyColumnName = new String[2];
		tableNameKeyColumnName[0] = getTableName(detailEntity);
		tableNameKeyColumnName[1] = getKeyColumnName(detailEntity);
		
		if (usesDifferentFK(detailEntity) && 
				!isValidTableAndKeyColumn(tableNameKeyColumnName[0], tableNameKeyColumnName[1]))
			throw new IDempiereRestException("Expand error", 
					"Column: " +  tableNameKeyColumnName[1] + " is not a valid FK for table: " + masterTableName
					, Status.BAD_REQUEST);

		return tableNameKeyColumnName;
	}
	
	private String getKeyColumnName(String detailEntity) {
		return usesDifferentFK(detailEntity) ? detailEntity.substring(detailEntity.indexOf(".") + 1)
				: RestUtils.getKeyColumnName(masterTableName);
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

	private List<PO> getChildPOs(List<String> operators, String tableName, String keyColumnName, String[] includes) {
		ModelHelper modelHelper = getModelHelper(operators, tableName, keyColumnName);
		List<PO> poList = modelHelper.getPOsFromRequest(includes);
		addSQLStatementToMap(tableName, keyColumnName, modelHelper.getSQLStatement());

		return poList;
	}
	
	private ModelHelper getModelHelper(List<String> operators, String tableName, String keyColumnName) {
		MColumn column = MColumn.get(Env.getCtx(), tableName, keyColumnName);
		if (column == null)
			throw new IDempiereRestException("Invalid column for expand: " + keyColumnName, Status.BAD_REQUEST);

		int parentId = masterTableName.equalsIgnoreCase(column.getReferenceTableName()) ? po.get_ID() : po.get_ValueAsInt(keyColumnName); 
		
		String filter = getFilterClause(operators, keyColumnName, parentId);
		String orderBy = ExpandUtils.getOrderByClause(operators);
		int top = ExpandUtils.getTopClause(operators);
		int skip = ExpandUtils.getSkipClause(operators);

		return new ModelHelper(tableName, filter, orderBy, top, skip);
	}
	
	public String getFilterClause(List<String> operators, String keyColumnName, int keyColumnValue) {
		StringBuilder filterClause = new StringBuilder(keyColumnName + " eq " + keyColumnValue);
		
		if (ExpandUtils.isRecordIDTableIDFK(keyColumnName))
			filterClause.append(" AND ").append(ExpandUtils.TABLE_ID_COLUMN).append(" eq ").append(po.get_Table_ID());
		
		String requestFilterClause = ExpandUtils.getFilterClause(operators);
		if (!Util.isEmpty(requestFilterClause)) 
			filterClause.append(" AND ").append(requestFilterClause);
		
		return filterClause.toString();
	}
	
	private void expandChildDetails(PO childPO, String expandClause, JsonObject childJson) {
		if (Util.isEmpty(expandClause))
			return;

		ExpandParser expandParser = new ExpandParser(childPO, expandClause);
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
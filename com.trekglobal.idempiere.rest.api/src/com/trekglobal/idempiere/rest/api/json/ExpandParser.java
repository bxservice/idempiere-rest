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
package com.trekglobal.idempiere.rest.api.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.Util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ExpandParser {
	
	private static final String RECORD_ID_COLUMN = "Record_ID";
	private static final String TABLE_ID_COLUMN  = "AD_Table_ID";
	
	private PO po;
	private String expandParameter = null;
	private String masterTableName = null;
	private Map<String, String> tableNameSQLStatementMap = new HashMap<>();
	private Map<String, JsonArray> tableNameChildArrayMap = new HashMap<>();
	
	public ExpandParser(PO po, String expandParameter) {
		this.po = po;
		this.expandParameter = expandParameter;
		masterTableName = po.get_TableName();
		expandDetails();
	}

	private void expandDetails() {
		if (Util.isEmpty(expandParameter, true))
			return;
		
		HashMap<String, List<String>>  detailTablesWithOperators = getTableNamesOperatorsMap();
		for (Map.Entry<String,List<String>> entry : detailTablesWithOperators.entrySet()) {
			expandDetail(entry.getKey(), entry.getValue());
		}
	}
	
	private void expandDetail(String detailEntity, List<String> operators) {
		String[] includes;

		String[] tableNameKeyColumnName = getTableNameAndKeyColumnName(detailEntity);
		String tableName = tableNameKeyColumnName[0];
		String keyColumn = tableNameKeyColumnName[1];		

		List<PO> childPOs = getChildPOs(operators, tableName, keyColumn);
		if (childPOs != null && childPOs.size() > 0) {
			JsonArray childArray = new JsonArray();
			IPOSerializer serializer = IPOSerializer.getPOSerializer(tableName, MTable.getClass(tableName));

			String select = getSelectClause(operators);
			includes = RestUtils.getSelectedColumns(tableName, select); 

			for (PO child : childPOs) {
				JsonObject childJsonObject = serializer.toJson(child, includes, new String[] {keyColumn, "model-name"});
				childArray.add(childJsonObject);
			}
			tableNameChildArrayMap.put(tableName, childArray);
		}
	}
	
	private String[] getTableNameAndKeyColumnName(String detailEntity) {
		String[] tableNameKeyColumnName = new String[2];
		tableNameKeyColumnName[0] = getTableName(detailEntity);
		tableNameKeyColumnName[1] = getKeyColumnName(detailEntity);
		
		if (usesDifferentFK(detailEntity) && 
				!isValidTableAndKeyColumn(tableNameKeyColumnName[0], tableNameKeyColumnName[1]))
			throw new IDempiereRestException("Expand error", 
					"Column: " +  tableNameKeyColumnName[1] + " is not a valid FK for table: " + tableNameKeyColumnName[0]
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
		MTable table = RestUtils.getTable(tableName);
		MColumn column = table.getColumn(keyColumnName);

		return column != null && 
				(isRecordIDTableIDFK(keyColumnName) || masterTableName.equalsIgnoreCase(column.getReferenceTableName()));
	}
	
	private boolean isRecordIDTableIDFK(String keyColumnName) {
		return RECORD_ID_COLUMN.equalsIgnoreCase(keyColumnName);
	}

	private HashMap<String, List<String>> getTableNamesOperatorsMap() {
		HashMap<String, List<String>> tableNamesOperatorsMap = new HashMap<String, List<String>>();
		
		if (!hasQueryOperators())
			fillMapWithNoOperators(tableNamesOperatorsMap);
		else
			fillMapWithQueryOperators(tableNamesOperatorsMap);

		return tableNamesOperatorsMap;
	}
	
	private boolean hasQueryOperators() {
		return expandParameter.contains("(");
	}
	
	private void fillMapWithNoOperators(HashMap<String, List<String>> tableNamesOperatorsMap) {
		List<String> detailTables = Arrays.asList(expandParameter.split("[,]"));
		for (String tableName : detailTables) 
			tableNamesOperatorsMap.put(tableName, Collections.emptyList());
	}
	
	private void fillMapWithQueryOperators(HashMap<String, List<String>> tableNamesOperatorsMap) {
		String commasOutsideParenthesisRegexp = ",(?![^()]*\\))";
		List<String> detailTables = Arrays.asList(expandParameter.split(commasOutsideParenthesisRegexp));
		
		for (String detailTable : detailTables)  {
			String tableName =  getDetailEntity(detailTable);
			List<String> operators = getExpandOperators(detailTable);
			tableNamesOperatorsMap.put(tableName, operators);
		}
	}
	
	private String getDetailEntity(String parameter) {
		String detailEntity = parameter;
		if (parameter.contains("("))
			detailEntity = parameter.substring(0, parameter.indexOf("("));

		return detailEntity;
	}
	
	private List<String> getExpandOperators(String parameter) {
		List<String> operators = new ArrayList<String>();

		if (parameter.contains("(")) {
			String queryOperators = parameter.substring(parameter.indexOf("(")+1, parameter.indexOf(")"));
			for (String operator : queryOperators.split("[;]")) {
				operators.add(operator.trim());
			}
		}

		return operators;
	}
	
	private String getSelectClause(List<String> operators) {
		for (String operator : operators) {
			if (operator.startsWith(QueryOperators.SELECT)) {
				return getStringOperatorValue(operator);
			}
		}
		
		return "";
	}
	
	private List<PO> getChildPOs(List<String> operators, String tableName, String keyColumnName) {
		ModelHelper modelHelper = getModelHelper(operators, tableName, keyColumnName);
		List<PO> poList = modelHelper.getPOsFromRequest(); 
		if (tableNameSQLStatementMap.get(tableName) == null)
			tableNameSQLStatementMap.put(tableName, modelHelper.getSQLStatement());

		return poList;
	}
	
	private ModelHelper getModelHelper(List<String> operators, String tableName, String keyColumnName) {
		String filter = getFilterClause(operators, keyColumnName, po.get_ID());
		String orderBy = getOrderByClause(operators);
		int top = getTopClause(operators);
		int skip = getSkipClause(operators);

		return new ModelHelper(tableName, filter, orderBy, top, skip);
	}
	
	private String getFilterClause(List<String> operators, String keyColumnName, int keyColumnValue) {
		StringBuilder filterClause = new StringBuilder(keyColumnName + " eq " + keyColumnValue);
		
		if (isRecordIDTableIDFK(keyColumnName))
			filterClause.append(" AND " + TABLE_ID_COLUMN + " eq " + po.get_Table_ID());
		
		for (String operator : operators) {
			if (operator.startsWith(QueryOperators.FILTER)) {
				filterClause.append(filterClause + " AND " + getStringOperatorValue(operator));
			}
		}
		
		return filterClause.toString();
	}
	
	private String getOrderByClause(List<String> operators) {
		for (String operator : operators) {
			if (operator.startsWith(QueryOperators.ORDERBY)) {
				return getStringOperatorValue(operator);
			}
		}
		
		return "";
	}
	
	private int getTopClause(List<String> operators) {
		for (String operator : operators) {
			if (operator.startsWith(QueryOperators.TOP)) {
				return getIntegerOperatorValue(operator);
			}
		}
		
		return 0;
	}
	
	private int getSkipClause(List<String> operators) {
		for (String operator : operators) {
			if (operator.startsWith(QueryOperators.SKIP)) {
				return getIntegerOperatorValue(operator);
			}
		}
		
		return 0;
	}
	
	private String getStringOperatorValue(String operator) {
		return operator.substring(operator.indexOf("=") +1);		
	}
	
	private int getIntegerOperatorValue(String operator) {
		String operatorValue = getStringOperatorValue(operator);
		try {
			return Integer.parseInt(operatorValue);
		} catch(NumberFormatException ex) {
			throw new IDempiereRestException("Expand error", "failed to parse Skip or Top operator. Only integers allowed", Status.BAD_REQUEST);
		}
	}

	public Map<String, String> getTableNameSQLStatementMap() {
		return tableNameSQLStatementMap;
	}
	
	public Map<String, JsonArray> getTableNameChildArrayMap() {
		return tableNameChildArrayMap;
	}
}

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

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ExpandParser {
	
	private PO po;
	private JsonObject jsonObject;
	private String expandParameter = null;
	
	public ExpandParser(PO po, JsonObject jsonObject, String expandParameter) {
		this.po = po;
		this.jsonObject = jsonObject;
		this.expandParameter = expandParameter;
	}

	public void expandDetailsIntoJsonObject() {
		if (Util.isEmpty(expandParameter, true))
			return;
		
		HashMap<String, List<String>>  detailTablesWithOperators = getTableNamesOperatorsMap();
		String keyColumn = RestUtils.getKeyColumnName(po.get_TableName());
		String[] includes;

		for (Map.Entry<String,List<String>> entry : detailTablesWithOperators.entrySet()) {
			String tableName = entry.getKey();
			List<String> operators = entry.getValue();
			
			MTable table = RestUtils.getTable(tableName);
		
			Query query = new Query(Env.getCtx(), table, keyColumn + "=?", null);
			query.setApplyAccessFilter(true, false)
				 .setOnlyActiveRecords(true);

			List<PO> childPOs = query.setParameters(po.get_ID()).list();
			if (childPOs != null && childPOs.size() > 0) {
				JsonArray childArray = new JsonArray();
				IPOSerializer serializer = IPOSerializer.getPOSerializer(tableName, MTable.getClass(tableName));

				String select = getSelectClause(operators);
				includes = RestUtils.getSelectedColumns(tableName, select); 

				for(PO child : childPOs) {
					JsonObject childJsonObject = serializer.toJson(child, includes, new String[] {keyColumn, "model-name"});
					childArray.add(childJsonObject);
				}
				jsonObject.add(tableName, childArray);
			}
		}
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
			String tableName =  getTableName(detailTable);
			List<String> operators = getExpandOperators(detailTable);
			tableNamesOperatorsMap.put(tableName, operators);
		}
	}
	
	private String getTableName(String parameter) {
		String tableName = parameter;
		if (parameter.contains("("))
			tableName = parameter.substring(0, parameter.indexOf("("));

		return tableName;
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
			if (operator.startsWith("$select=")) {
				return operator.substring(operator.indexOf("=") +1);
			}
		}
		
		return "";
	}
}

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
		
		HashMap<String, List<String>>  tableNames = getTableNamesOperatorsMap(expandParameter);
		String keyColumn = po.get_TableName() + "_ID";
		String[] includes;
		for (Map.Entry<String,List<String>> entry : tableNames.entrySet()) {
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
	
	private HashMap<String, List<String>> getTableNamesOperatorsMap(String expandParameter) {
		HashMap<String, List<String>> tableNamesOperatorsMap = new HashMap<String, List<String>>();
		List<String> detailTables = new ArrayList<String>();
		if (!expandParameter.contains("(")) {
			detailTables = Arrays.asList(expandParameter.split("[,]"));
			for (String tableName : detailTables) 
				tableNamesOperatorsMap.put(tableName, Collections.emptyList());
		} else {
			detailTables = Arrays.asList(expandParameter.split(",(?![^()]*\\))")); //Split commas outside of ( )
			for (String detailTable : detailTables)  {
				String tableName =  getTableName(detailTable);
				String queryOperators =  getExpandOperators(detailTable);

				//Separate operators by ;
				List<String> operators = new ArrayList<String>();
				operators.add(queryOperators);//More operators
				tableNamesOperatorsMap.put(tableName, operators);
			}

		}
		
		return tableNamesOperatorsMap;
	}
	
	private String getTableName(String parameter) {
		String tableName = parameter;
		if (parameter.contains("("))
			tableName = parameter.substring(0, parameter.indexOf("("));
			
		return tableName;
	}
	
	private String getExpandOperators(String parameter) {
		String queryOperators = "";
		if (parameter.contains("("))
			queryOperators = parameter.substring(parameter.indexOf("(")+1, parameter.indexOf(")"));
			
		return queryOperators;
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

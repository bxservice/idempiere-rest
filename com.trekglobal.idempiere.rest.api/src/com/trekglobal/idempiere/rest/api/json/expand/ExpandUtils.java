package com.trekglobal.idempiere.rest.api.json.expand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.compiere.model.MLabel;
import org.compiere.model.MLabelAssignment;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.json.QueryOperators;
import com.trekglobal.idempiere.rest.api.json.RestUtils;

public class ExpandUtils {
	
	private static final String RECORD_ID_COLUMN = "Record_ID";
	public static final String TABLE_ID_COLUMN  = "AD_Table_ID";
	
	public static boolean isRecordIDTableIDFK(String keyColumnName) {
		return RECORD_ID_COLUMN.equalsIgnoreCase(keyColumnName);
	}
	
	public static String getSelectClause(List<String> operators) {
		for (String operator : operators) {
			if (operator.startsWith(QueryOperators.SELECT)) {
				return getStringOperatorValue(operator);
			}
		}
		
		return "";
	}
	
	public static String getFilterClause(List<String> operators) {
		for (String operator : operators) {
			if (operator.startsWith(QueryOperators.FILTER)) {
				return getStringOperatorValue(operator);
			}
		}
		
		return "";
	}
	
	public static String getLabelClause(List<String> operators) {
		for (String operator : operators) {
			if (operator.startsWith(QueryOperators.LABEL)) {
				return getStringOperatorValue(operator);
			}
		}
		
		return "";
	}
	
	public static String getOrderByClause(List<String> operators) {
		for (String operator : operators) {
			if (operator.startsWith(QueryOperators.ORDERBY)) {
				return getStringOperatorValue(operator);
			}
		}
		
		return "";
	}
	
	public static int getTopClause(List<String> operators) {
		for (String operator : operators) {
			if (operator.startsWith(QueryOperators.TOP)) {
				return getIntegerOperatorValue(operator);
			}
		}
		
		return 0;
	}
	
	public static int getSkipClause(List<String> operators) {
		for (String operator : operators) {
			if (operator.startsWith(QueryOperators.SKIP)) {
				return getIntegerOperatorValue(operator);
			}
		}
		
		return 0;
	}
	
	public static String getExpandClause(List<String> operators) {
		for (String operator : operators) {
			if (operator.startsWith(QueryOperators.EXPAND)) {
				return getStringOperatorValue(operator);
			}
		}
		
		return "";
	}
	
	private static String getStringOperatorValue(String operator) {
		return operator.substring(operator.indexOf("=") +1);		
	}
	
	private static int getIntegerOperatorValue(String operator) {
		String operatorValue = getStringOperatorValue(operator);
		try {
			return Integer.parseInt(operatorValue);
		} catch(NumberFormatException ex) {
			throw new IDempiereRestException("Expand error", "failed to parse Skip or Top operator. Only integers allowed", Status.BAD_REQUEST);
		}
	}
	
	public static HashMap<String, List<String>> getTableNamesOperatorsMap(String expandParameter) {
		return !hasQueryOperators(expandParameter) ? 
				fillMapWithNoOperators(expandParameter) : 
				fillMapWithQueryOperators(expandParameter);
	}
	
	private static boolean hasQueryOperators(String expandParameter) {
		return expandParameter.contains("(");
	}
	
	private static HashMap<String, List<String>> fillMapWithNoOperators(String expandParameter) {
		HashMap<String, List<String>> tableNamesOperatorsMap = new HashMap<String, List<String>>();

		List<String> detailTables = Arrays.asList(expandParameter.split("[,]"));
		for (String tableName : detailTables) 
			tableNamesOperatorsMap.put(tableName.trim(), Collections.emptyList());
		
		return tableNamesOperatorsMap;
	}
	
	private static HashMap<String, List<String>> fillMapWithQueryOperators(String expandParameter) {
		HashMap<String, List<String>> tableNamesOperatorsMap = new HashMap<String, List<String>>();

		List<String> detailTables = splitStringByChar(expandParameter, ',');

		for (String detailTable : detailTables)  {
			String tableName = getDetailEntity(detailTable);
			List<String> operators = getExpandOperators(detailTable);
			tableNamesOperatorsMap.put(tableName, operators);
		}
		
		return tableNamesOperatorsMap;
	}
	
	private static List<String> splitStringByChar(String expandParameter, char splittingChar) {
	    List<String> splitted = new ArrayList<String>();
	    int nextingLevel = 0;
	    StringBuilder result = new StringBuilder();
	    for (char c : expandParameter.toCharArray()) {
	        if (c == splittingChar && nextingLevel == 0) {
	            splitted.add(result.toString());
	            result.setLength(0);// clean buffer
	        } else {
	            if (c == '(')
	                nextingLevel++;
	            if (c == ')')
	                nextingLevel--;
	            result.append(c);
	        }
	    }

	    splitted.add(result.toString());
	    return splitted;
	}
	   
	
	private static String getDetailEntity(String parameter) {
		String detailEntity = parameter;
		if (parameter.contains("("))
			detailEntity = parameter.substring(0, parameter.indexOf("("));

		return detailEntity.trim();
	}
	
	private static List<String> getExpandOperators(String parameter) {
		List<String> operators = new ArrayList<String>();

		if (parameter.contains("(")) {
			String queryOperators = parameter.substring(parameter.indexOf("(")+1, parameter.lastIndexOf(")"));
			String separator = MSysConfig.getValue("REST_EXPAND_SEPARATOR", ";");
			for (String operator : splitStringByChar(queryOperators, separator.charAt(0))) {
				operators.add(operator.trim());
			}
		}

		return operators;
	}
	
	public static void addDetailSQLCommandToJson(Map<String, String> tableSQLMap, JsonObject json) {
		for (Map.Entry<String,String> entry : tableSQLMap.entrySet()) {
			String tableName = entry.getKey();
			String sqlStatement = entry.getValue();
			json.addProperty("sql-command-" + tableName, DB.getDatabase().convertStatement(sqlStatement));
		}
	}
	
	public static void addDetailDataToJson(Map<String, JsonElement> tableNameDataMap, JsonObject json) {
		for (Map.Entry<String,JsonElement> entry : tableNameDataMap.entrySet()) {
			String tableName = entry.getKey();
			JsonElement childArray = entry.getValue();
			json.add(tableName, childArray);
		}
	}
	
	public static void addAssignedLabelsToJson(PO po, String showlabel, JsonObject json) {
		if (MLabelAssignment.hasAnyAssignment(po.get_Table_ID(), po.get_ID(), po.get_UUID())) {
			StringBuilder whereClause = new StringBuilder();
			whereClause.append(MLabel.COLUMNNAME_AD_Label_ID + " IN (");
			whereClause.append("SELECT "+ MLabelAssignment.COLUMNNAME_AD_Label_ID);
			whereClause.append(" FROM " + MLabelAssignment.Table_Name);
			whereClause.append(" WHERE " + MLabelAssignment.COLUMNNAME_AD_Table_ID + "=?");
			whereClause.append(" AND " + MLabelAssignment.COLUMNNAME_Record_UU + "=?)");
			Query query = new Query(Env.getCtx(), MLabel.Table_Name, whereClause.toString(), null)
					.setParameters(po.get_Table_ID(), po.get_UUID())
					.setOrderBy(MLabel.COLUMNNAME_Name)
					.setApplyAccessFilter(true)
					.setOnlyActiveRecords(true);
			String selectClause = null;
			if (!Util.isEmpty(showlabel, true))
				selectClause = showlabel;
  			String[] includes = RestUtils.getSelectedColumns(MLabel.Table_Name, selectClause); 
			if (includes != null && includes.length > 0)
				query.selectColumns(includes);
			List<MLabel> labelList = query.list();
			IPOSerializer serializer = IPOSerializer.getPOSerializer(MLabel.Table_Name, MTable.getClass(MLabel.Table_Name));
			JsonArray labelArray = new JsonArray();
			for (MLabel label : labelList) {
				JsonObject jsonObject = serializer.toJson(label, includes, null);
				if (includes != null && includes.length > 0) {
					if (includes.length == 1) {
						// showlabel={column name} returns the values of the assigned label {column name}
						JsonElement jsonElement = jsonObject.get(includes[0]);
						labelArray.add(jsonElement);
					} else {
						// showlabel={columnname list} returns a list of assigned label objects, including the {columnname list} only
						JsonObject jsonObjectIncludes = new JsonObject();
						for (String include : includes) {
							JsonElement jsonElement = jsonObject.get(include);
							jsonObjectIncludes.add(include, jsonElement);
						}
						labelArray.add(jsonObjectIncludes);
					}
				} else {
					// showlabel returns a list of assigned label objects, including all columns
					labelArray.add(jsonObject);
				}
			}
			json.add("assigned-labels", labelArray);
		}
	}
}

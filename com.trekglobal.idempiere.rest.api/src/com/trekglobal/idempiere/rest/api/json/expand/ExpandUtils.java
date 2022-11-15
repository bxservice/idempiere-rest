package com.trekglobal.idempiere.rest.api.json.expand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.compiere.util.DB;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.QueryOperators;

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
			tableNamesOperatorsMap.put(tableName, Collections.emptyList());
		
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

		return detailEntity;
	}
	
	private static List<String> getExpandOperators(String parameter) {
		List<String> operators = new ArrayList<String>();

		if (parameter.contains("(")) {
			String queryOperators = parameter.substring(parameter.indexOf("(")+1, parameter.lastIndexOf(")"));
			for (String operator : splitStringByChar(queryOperators, ';')) {
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
}

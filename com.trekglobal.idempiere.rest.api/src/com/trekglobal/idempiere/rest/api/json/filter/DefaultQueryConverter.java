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
package com.trekglobal.idempiere.rest.api.json.filter;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.util.CCache;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.osgi.service.component.annotations.Component;

import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.model.MRestView;

/**
 * Default Query converter that uses oData notation
 * */
@Component(name = "com.trekglobal.idempiere.rest.api.json.DefaultQueryConverter", service = IQueryConverterFactory.class, 
property = {"service.ranking:Integer=0"}, immediate = true)
public class DefaultQueryConverter implements IQueryConverter, IQueryConverterFactory {

	private CCache<String, ConvertedQuery> convertCache = new CCache<String, ConvertedQuery>(null, "JSON_DB_Convert_Cache", 1000, 60, false);
	private ConvertedQuery convertedQuery;
	private MTable table;
	private MRestView view;

	@Override
	public synchronized ConvertedQuery convertStatement(String tableName, String queryStatement) {
		return convertStatement(null, tableName, queryStatement);				
	}
	
	@Override
	public synchronized ConvertedQuery convertStatement(MRestView view, String tableName, String queryStatement) {
		ConvertedQuery cache = convertCache.get(queryStatement);
		if (cache != null) {
			return cache;
		}

		convertedQuery = new ConvertedQuery();
		if (!Util.isEmpty(queryStatement, true)) {
			table = MTable.get(Env.getCtx(), tableName);
			this.view = view;
			convertStatement(queryStatement);

			convertCache.put(queryStatement, convertedQuery);
		}

		return convertedQuery;
	}

	private void convertStatement(String originalFilter) {
		List<String> literals = split(originalFilter);
		convertLiterals(literals);
	}
	
	private void convertLiterals(List<String> literals) {

		String literal;
		String sqlStatement;
		for (int i=0; i< literals.size(); i++) {
			literal = literals.get(i);
			sqlStatement = "";
			
			if (ODataUtils.AND.equalsIgnoreCase(literal) || ODataUtils.OR.equalsIgnoreCase(literal)) {
				sqlStatement = " " + literal.toUpperCase() + " ";
			} else if (ODataUtils.NOT.equalsIgnoreCase(literal)) {
				String nextOperator = literals.get(++i);

				if (ODataUtils.isMethodCall(nextOperator)) {
					sqlStatement = convertMethodWithParamsLiteral(nextOperator, true);
				} else {
					throw new IDempiereRestException("Operator NOT is only compatible with certain functions. Not with " + nextOperator, Status.BAD_REQUEST);
				}

			} else {
				if (literal.startsWith("(") && literal.endsWith(")")) {
					literal = literal.substring(1, literal.length()-1);
					List<String> subliterals = split(literal);
					convertedQuery.appendWhereClause("(");
					convertLiterals(subliterals);
					convertedQuery.appendWhereClause(")");
				} else if (ODataUtils.isMethodCall(literal) && ODataUtils.isMethodWithParameters(ODataUtils.getMethodCall(literal))) {
					sqlStatement = convertMethodWithParamsLiteral(literal, false);
				} else {
					String left = literal;
					String operator = literals.get(++i);
					String right = literals.get(++i);

					sqlStatement = convertBinaryOperator(left, operator, right);
				}
			}
			convertedQuery.appendWhereClause(sqlStatement);
		}
	}
	
	private String convertMethodWithParamsLiteral(String literal, boolean isNot) {

		String methodName = ODataUtils.getMethodCall(literal);

		String leftParameter = ODataUtils.getFirstParameter(methodName, literal);
		String columnName = leftParameter;

		if (leftParameter.contains("(")) {
			//Another method, f.i contains(tolower(name),'admin')
			String innerMethodName = ODataUtils.getMethodCall(leftParameter);
			columnName = ODataUtils.getFirstParameter(innerMethodName, leftParameter);
			leftParameter = ODataUtils.getSQLFunction(innerMethodName, columnName, false);
		}

		String value = ODataUtils.getSecondParameter(methodName, literal);
		return leftParameter + convertMethodCall(methodName, columnName, value, isNot);
	}

	private String convertBinaryOperator(String left, String operator, String right) {
		String leftParameter = null;
		String strOperator = ODataUtils.getOperator(operator);
		String rightParameter = null;

		if (strOperator == null) {
			throw new IDempiereRestException("Unsupported operator: " + operator, Status.BAD_REQUEST);
		}
		MColumn column = null;
		if (left.contains("(")) {
			//Another method, f.i contains(tolower(name),'admin')
			String innerMethodName = ODataUtils.getMethodCall(left);
			String columnName = ODataUtils.getFirstParameter(innerMethodName, left);
			MColumn tableColumn = getTableColumn(table, columnName.trim());
			String viewColumnName = null;
			if (view != null) {
				viewColumnName = view.toColumnName(columnName.trim());
				if (viewColumnName == null && tableColumn != null)
					throw new IDempiereRestException("Invalid column for filter: " + columnName.trim(), Status.BAD_REQUEST);
			}
			column = viewColumnName != null && tableColumn == null ? getTableColumn(table, viewColumnName) : tableColumn;
			if (column == null || column.isSecure() || column.isEncrypted()) {
				throw new IDempiereRestException("Invalid column for filter: " + columnName.trim(), Status.BAD_REQUEST);
			}
			leftParameter = ODataUtils.getSQLFunction(innerMethodName, columnName, false);
		} else {
			MColumn tableColumn = getTableColumn(table, left.trim());
			String viewColumnName = null;
			if (view != null) {
				viewColumnName = view.toColumnName(left.trim());
				if (viewColumnName == null && tableColumn != null)
					throw new IDempiereRestException("Invalid column for filter: " + left.trim(), Status.BAD_REQUEST);
			}
			column = viewColumnName != null && tableColumn == null ? getTableColumn(table, viewColumnName) : tableColumn;
			if (column == null || column.isSecure() || column.isEncrypted()) {
				throw new IDempiereRestException("Invalid column for filter: " + left.trim(), Status.BAD_REQUEST);
			}
			leftParameter = column.getColumnName();
		}
		
		if ("null".equals(right)) {
			switch (operator) {
			case ODataUtils.EQUALS:
				strOperator = " IS ";
				rightParameter = "NULL";
				break;
			case ODataUtils.NOT_EQUALS:
				strOperator = " IS NOT ";
				rightParameter = "NULL";
				break;
			default: 
				throw new IDempiereRestException("Operator " + operator + " is not compatible with NULL comparision", Status.BAD_REQUEST);
			}
		} else if(ODataUtils.IN.equals(operator)) {
			if (right.startsWith("(") && right.endsWith(")")) {
				StringBuilder rightParameterBuilder = new StringBuilder("(");
				String values = right.substring(1, right.length() - 1).trim();

				String[] valueArray = values.split(",");
				for (String value : valueArray) {
					convertedQuery.addParameter(column, value.trim());
					rightParameterBuilder.append("?,");
				}

				// Remove last comma and close parentheses
				rightParameterBuilder.setLength(rightParameterBuilder.length() - 1);
				rightParameterBuilder.append(")");

				rightParameter = rightParameterBuilder.toString();
			} else {
				throw new IDempiereRestException("Wrong right parameter for IN operator", Status.BAD_REQUEST);
			}
		} else if (right.startsWith("'") && right.endsWith("'")) {
			convertedQuery.addParameter(column, right);
			rightParameter = " ?";
		} else {
			// Get Right Value
			if (right.contains("(")) {
				//Another method, f.i tolower(name)
				String innerMethodName = ODataUtils.getMethodCall(right);
				String innerValue = ODataUtils.getFirstParameter(innerMethodName, right);
				MColumn tableColumn = getTableColumn(table, innerValue.trim());
				String viewColumnName = null;
				if (view != null) {
					viewColumnName = view.toColumnName(innerValue.trim());
					if (viewColumnName == null && tableColumn != null)
						throw new IDempiereRestException("Invalid column for filter: " + innerValue.trim(), Status.BAD_REQUEST);
				}
				MColumn columnRight = viewColumnName != null && tableColumn == null ? getTableColumn(table, viewColumnName) : tableColumn;
				if (columnRight != null) {
					if(columnRight.isSecure() || columnRight.isEncrypted()) {
						throw new IDempiereRestException("Invalid column for filter: " + innerValue.trim(), Status.BAD_REQUEST);
					}
					
					rightParameter = ODataUtils.getSQLFunction(innerMethodName, columnRight.getColumnName(), false);
				} else {
					convertedQuery.addParameter(column, innerValue);
					rightParameter = ODataUtils.getSQLFunction(innerMethodName, "?", false);
				}
			} else {
				// Check Right is Column
				MColumn tableColumnRight = getTableColumn(table, right.trim());
				String viewRight = null;
				if (view != null) {
					viewRight = view.toColumnName(right.trim());
					if (viewRight == null && tableColumnRight != null)
						throw new IDempiereRestException("Invalid column for filter: " + tableColumnRight.getColumnName(), Status.BAD_REQUEST);
				}
				MColumn columnRight = viewRight != null && tableColumnRight == null ? getTableColumn(table, viewRight) : tableColumnRight;
				if (columnRight != null) {
					if(columnRight.isSecure() || columnRight.isEncrypted()) {
						throw new IDempiereRestException("Invalid column for filter: " + right.trim(), Status.BAD_REQUEST);
					}
					rightParameter = columnRight.getColumnName();
				} else {
					convertedQuery.addParameter(column, right);
					rightParameter = " ?";
				}
			}
		}

		return leftParameter + strOperator + rightParameter;
	}
	
	private String convertMethodCall(String methodCall, String columnName, String value, boolean isNot) {
		String rightParameter = "?";
		String innerMethodName = null;
		MColumn tableColumn = getTableColumn(table, columnName);
		String viewColumnName = null;
		if (view != null) {
			viewColumnName = view.toColumnName(columnName);
			if (viewColumnName == null && tableColumn != null) 
				throw new IDempiereRestException("Invalid column for filter: " + columnName, Status.BAD_REQUEST);
		}
		MColumn column = viewColumnName != null && tableColumn == null ? getTableColumn(table, viewColumnName) : tableColumn;
		if (column == null || column.isSecure() || column.isEncrypted()) {
			throw new IDempiereRestException("Invalid column for filter: " + columnName, Status.BAD_REQUEST);
		}
		
		// Check Right is Column
		if (value.contains("(")) {
			//Another method, f.i contains(tolower(name),'admin')
			innerMethodName = ODataUtils.getMethodCall(value);
			value = ODataUtils.getFirstParameter(innerMethodName, value);
			rightParameter = ODataUtils.getSQLFunction(innerMethodName, "?", false);
		}
		
		tableColumn = getTableColumn(table, value.trim());
		viewColumnName = null;
		if (view != null) {
			viewColumnName = view.toColumnName(value.trim());
			if (viewColumnName == null && tableColumn != null)
				throw new IDempiereRestException("Invalid column for filter: " + value.trim(), Status.BAD_REQUEST);
		}
		MColumn columnRight = viewColumnName != null && tableColumn == null ? getTableColumn(table, viewColumnName) : tableColumn;
		if (columnRight != null) {
			if (columnRight.isSecure() || columnRight.isEncrypted()) {
				throw new IDempiereRestException("Invalid column for filter: " + value.trim(), Status.BAD_REQUEST);
			}
			if(innerMethodName != null)
				rightParameter = ODataUtils.getSQLFunction(innerMethodName, columnRight.getColumnName(), false);
			else
				rightParameter = columnRight.getColumnName();
		} else {
			value = ConvertedQuery.extractFromStringValue(value);

			switch (methodCall) {
			case ODataUtils.CONTAINS:
				convertedQuery.addParameter(column, "'%"+ value + "%'");
				break;
			case ODataUtils.STARTSWITH:
				convertedQuery.addParameter(column, "'" + value + "%'");
				break;
			case ODataUtils.ENDSWITH:
				convertedQuery.addParameter(column, "'%"+ value + "'");
				break;
			default: 
				throw new IDempiereRestException("Method call " + methodCall + " not implemented", Status.NOT_IMPLEMENTED);
			}
		}
		
		return ODataUtils.getSQLMethodOperator(methodCall, isNot) +  rightParameter;
	}

	public static List<String> split(String expression){
		List<String> operatorList = new LinkedList<String>();
		int depth=0;
		boolean singleQuotes=false;
		StringBuilder sb = new StringBuilder();
		for(int i=0; i< expression.length(); i++){
			char c = expression.charAt(i);
			if(c=='('){
				depth++;
			}else if(c==')'){
				depth--;
			}else if(c=='\''){
				singleQuotes=!singleQuotes;
			}else if(c==' ' && depth==0 && !singleQuotes){
				operatorList.add(sb.toString());
				sb = new StringBuilder();
				continue;
			}
			sb.append(c);
		}
		operatorList.add(sb.toString());

		//Purge the list removing null and empty strings
		operatorList.removeIf(item -> item == null || "".equals(item));
		return operatorList;
	}

	@Override
	public IQueryConverter getQueryConverter(String converterName) {
		if ("DEFAULT".equals(converterName))
			return this;

		return null;
	}
	
	private MColumn getTableColumn(MTable table, String columnName) {
		if (columnName.equals("id")) {
			String keyColumn = table.getKeyColumns() != null && table.getKeyColumns().length == 1 ? table.getKeyColumns()[0] : null;
			if (keyColumn != null)
				columnName = keyColumn;
		} else if (columnName.equals("uid"))
			columnName = PO.getUUIDColumnName(table.getTableName());
		return table.getColumn(columnName);
	}

}

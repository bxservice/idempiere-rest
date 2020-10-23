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

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.util.CCache;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.osgi.service.component.annotations.Component;

/**
 * Default Query converter that uses oData notation
 * */
@Component(name = "com.trekglobal.idempiere.rest.api.json.DefaultQueryConverter", service = IQueryConverterFactory.class, 
property = {"service.ranking:Integer=0"}, immediate = true)
public class DefaultQueryConverter implements IQueryConverter, IQueryConverterFactory {

	private CCache<String, ConvertedQuery> convertCache = new CCache<String, ConvertedQuery>(null, "JSON_DB_Convert_Cache", 1000, 60, false);
	private ConvertedQuery convertedQuery;
	private MTable table;
	private Status status = Status.OK;

	@Override
	public ConvertedQuery convertStatement(String tableName, String queryStatement) {
		ConvertedQuery cache = convertCache.get(queryStatement);
		if (cache != null) {
			return cache;
		}

		convertedQuery = new ConvertedQuery();
		if (!Util.isEmpty(queryStatement, true)) {
			table = MTable.get(Env.getCtx(), tableName);
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
					sqlStatement = convertMethodWithParamsLiteral(nextOperator, null, true);
				} else {
					status = Status.BAD_REQUEST;
					throw new AdempiereException("Operator NOT is only compatible with certain functions. Not with " + nextOperator);
				}

			} else {
				if (literal.startsWith("(") && literal.endsWith(")")) {
					literal = literal.substring(1, literal.length()-1);
					List<String> subliterals = split(literal);
					convertedQuery.appendWhereClause("(");
					convertLiterals(subliterals);
					convertedQuery.appendWhereClause(")");
				} else if (ODataUtils.isMethodCall(literal)) {
					String methodName = ODataUtils.getMethodCall(literal);

					if (ODataUtils.isMethodWithParameters(methodName)) {
						sqlStatement = convertMethodWithParamsLiteral(literal, methodName, false);
					} else {
						//It contains a binary operator - f.i tolower(name) eq 'garden'
						String columnName = ODataUtils.getFirstParameter(methodName, literal);
						String operator = literals.get(++i);
						String right = literals.get(++i);

						sqlStatement = convertMethodBinaryOperator(methodName, columnName, operator, right);
					}
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
	
	private String convertMethodWithParamsLiteral(String literal, String methodName, boolean isNot) {

		if (methodName == null)
			methodName = ODataUtils.getMethodCall(literal);

		String leftParameter = ODataUtils.getFirstParameter(methodName, literal);
		String columnName = leftParameter;

		if (leftParameter.contains("(")) {
			//Another method, f.i contains(tolower(name),'admin')
			String innerMethodName = ODataUtils.getMethodCall(leftParameter);
			columnName = ODataUtils.getFirstParameter(innerMethodName, leftParameter);
			leftParameter = ODataUtils.getSQLFunction(innerMethodName, columnName, false);
		}

		String value = ODataUtils.getSecondParameter(methodName, literal);
		return convertMethodCall(methodName, leftParameter, columnName, value, isNot);
	}

	private String convertBinaryOperator(String left, String operator, String right) {
		String strOperator = ODataUtils.getOperator(operator);

		if (strOperator == null) {
			status = Status.BAD_REQUEST;
			throw new AdempiereException("Unsupported operator: " + operator);
		}

		MColumn column = table.getColumn(left.trim());
		if (column == null || column.isSecure() || column.isEncrypted()) {
			status = Status.BAD_REQUEST;
			throw new AdempiereException("Invalid column for filter: " + left.trim());
		}

		if ("null".equals(right)) {
			switch (operator) {
			case ODataUtils.EQUALS:
				strOperator = " IS NULL";
				break;
			case ODataUtils.NOT_EQUALS:
				strOperator = " IS NOT NULL";
				break;
			default: 
				status = Status.BAD_REQUEST;
				throw new AdempiereException("Operator " + operator + " is not compatible with NULL comparision");
			}
		} else {
			convertedQuery.addParameter(column, right);
			strOperator = strOperator + " ?";
		}

		return column.getColumnName() + strOperator;
	}
	
	private String convertMethodBinaryOperator(String methodCall, String columnName, String operator, String right) {
		String strOperator = ODataUtils.getOperator(operator);

		if (strOperator == null) {
			status = Status.BAD_REQUEST;
			throw new AdempiereException("Unsupported operator:: " + operator);
		}

		MColumn column = table.getColumn(columnName);
		if (column == null || column.isSecure() || column.isEncrypted()) {
			status = Status.BAD_REQUEST;
			throw new AdempiereException("Invalid column for filter: " + columnName.trim());
		}

		convertedQuery.addParameter(column, right);
		return ODataUtils.getSQLFunction(methodCall, columnName, false) + " = ?";
	}

	private String convertMethodCall(String methodCall, String leftParameter, String columnName, String value, boolean isNot) {

		MColumn column = table.getColumn(columnName);
		if (column == null || column.isSecure() || column.isEncrypted()) {
			status = Status.BAD_REQUEST;
			throw new AdempiereException("Invalid column for filter: " + columnName);
		}

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
			status = Status.NOT_IMPLEMENTED;
			throw new AdempiereException("Method call " + methodCall + " not implemented");
		}

		return leftParameter + ODataUtils.getSQLFunction(methodCall, columnName, isNot);
	}

	public static List<String> split(String expression){
		List<String> operatorList = new LinkedList<String>();
		int depth=0;
		StringBuilder sb = new StringBuilder();
		for(int i=0; i< expression.length(); i++){
			char c = expression.charAt(i);
			if(c=='('){
				depth++;
			}else if(c==')'){
				depth--;
			}else if(c==' ' && depth==0){
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

	@Override
	public Status getResponseStatus() {
		return status;
	}

}

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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;

public class ODataUtils {
	
	/** Logical operators **/
	public static final String EQUALS = "eq";
	public static final String IN = "in";
	public static final String NOT_EQUALS = "neq";
	public static final String GREATER_THAN = "gt";
	public static final String GREATER_THAN_OR_EQUAL = "ge";
	public static final String LESS_THAN = "lt";
	public static final String LESS_THAN_OR_EQUAL = "le";
	public static final String AND = "and";
	public static final String OR = "or";
	public static final String NOT = "not";

	/** Methods Operators **/
	public static final String CONTAINS = "contains";
	public static final String STARTSWITH = "startswith";
	public static final String ENDSWITH = "endswith";
	public static final String LOWER = "tolower";
	public static final String UPPER = "toupper";
	
	public static String getOperator(String operator) {
		return OPERATORS.get(operator);
	}
	
	/***
	 * Returns true if it is a supported function
	 * **/
	public static boolean isMethodCall(String operator) {
		if (!operator.trim().startsWith("(") && operator.trim().endsWith(")")) {
			String methodName = getMethodCallName(operator);
			if (SUPPORTED_METHODS.contains(methodName) || SUPPORTED_PARAM_METHODS.contains(methodName))
				return true;
			else 
				throw new AdempiereException("Method call '" + methodName + "' not implemented");
		}
		return false;
	}
	
	public static String getMethodCall(String operator) {
		return isMethodCall(operator) ? getMethodCallName(operator) : null;
	}
	
	/**
	 * Returns true if the given method, the second literal is a parameter
	 * f.i: constains(columname,value)
	 * **/
	public static boolean isMethodWithParameters(String methodName) {
		return SUPPORTED_PARAM_METHODS.contains(methodName);
	}

	private static String getMethodCallName(String expression) {
		return expression.indexOf("(") > 0 ? expression.substring(0, expression.indexOf("(")) : "";
	}
	
	public static String getFirstParameter(String methodName, String expression) {
		return isMethodWithParameters(methodName) ? 
				expression.substring(expression.indexOf("(")+1 , expression.indexOf(",")) : 
				expression.substring(expression.indexOf("(")+1 , expression.indexOf(")"));
	}
	
	public static String getSecondParameter(String methodName, String expression) {
		return isMethodWithParameters(methodName) ? expression.substring(expression.indexOf(",")+1, expression.lastIndexOf(")")) : null;
	}

		
	public static String getSQLMethodOperator(String methodName, boolean isNot) {
		String sql = isNot ? " NOT " : "";
		switch (methodName) {
		case ODataUtils.CONTAINS:
		case ODataUtils.STARTSWITH:
		case ODataUtils.ENDSWITH:
			return sql + " LIKE ";
		default:
			throw new AdempiereException("Method call " + methodName + " not implemented");
		}
	}
	
	public static String getSQLFunction(String methodName, String columnName, boolean isNot) {
		String sql = isNot ? " NOT " : "";
		switch (methodName) {
		case ODataUtils.CONTAINS:
		case ODataUtils.STARTSWITH:
		case ODataUtils.ENDSWITH:
			return sql + " LIKE ?";
		case ODataUtils.LOWER:
			return "lower(" + columnName + ")";
		case ODataUtils.UPPER:
			return "upper(" + columnName + ")";
		default:
			throw new AdempiereException("Method call " + methodName + " not implemented");
		}
	}

	private static final List<String> SUPPORTED_PARAM_METHODS = 
			Collections.unmodifiableList(Arrays.asList(CONTAINS, 
					STARTSWITH, 
					ENDSWITH));
	
	private static final List<String> SUPPORTED_METHODS = 
			Collections.unmodifiableList(Arrays.asList(LOWER, 
					UPPER));

	private static final Map<String, String> OPERATORS = new HashMap<>() {
		private static final long serialVersionUID = 8733161114590577691L;
		{
			put(AND, "AND");
			put(EQUALS, "=");
			put(IN, " IN ");
			put(GREATER_THAN_OR_EQUAL, ">=");
			put(GREATER_THAN, ">");
			put(LESS_THAN_OR_EQUAL, "<=");
			put(LESS_THAN, "<");
			put(NOT_EQUALS, "<>");
			put(OR, "OR");
		}};
}

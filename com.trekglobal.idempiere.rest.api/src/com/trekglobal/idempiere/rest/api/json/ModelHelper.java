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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response.Status;

import org.compiere.model.MColumn;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.MValRule;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.trekglobal.idempiere.rest.api.json.filter.ConvertedQuery;
import com.trekglobal.idempiere.rest.api.json.filter.IQueryConverter;
import com.trekglobal.idempiere.rest.api.model.MRestView;

public class ModelHelper {
	
	private final static CLogger log = CLogger.getCLogger(ModelHelper.class);
	private static final int DEFAULT_QUERY_TIMEOUT = 60 * 2;
	private static final int MAX_RECORDS_SIZE = MSysConfig.getIntValue("REST_MAX_RECORDS_SIZE", 100);
	private static final String CONTEXT_VARIABLES_SEPARATOR = ",";
	private static final String CONTEXT_NAMEVALUE_SEPARATOR = ":";
	
	private static final AtomicInteger windowNoAtomic = new AtomicInteger();
	
	private String tableName;
	private String filter;
	private String orderBy;
	private int top;
	private int skip;
	private String validationRuleID;
	private String context;
	
	private int rowCount = 0;
	private int pageCount = 0;
	private String sqlStatement;
	private MRestView view;
	
	public ModelHelper(String tableName, String filter, String orderBy, 
			int top, int skip, String validationRuleID, String context) {
		this.tableName=tableName;
		this.filter=filter;
		this.orderBy=orderBy;
		this.top=top;
		this.skip=skip;
		this.validationRuleID=validationRuleID;
		this.context=context;		
	}
	
	public ModelHelper(String tableName, String filter, String orderBy, int top, int skip) {
		this(tableName, filter, orderBy, top, skip, null, null);
	}
	
	public void setView(MRestView view) {
		this.view = view;
		//validate view and tableName agree, should never happens
		if (view != null && !(MTable.getTableName(Env.getCtx(), view.getAD_Table_ID()).equalsIgnoreCase(tableName))) {
			throw new IllegalArgumentException("Rest view belongs to a different table from what this ModelHelper is using");
		}
	}
	
	public List<PO> getPOsFromRequest() {
		return getPOsFromRequest(null);
	}
	
	public List<PO> getPOsFromRequest(String[] includeColumns) {

		String whereClause = getRequestWhereClause();
		IQueryConverter converter = IQueryConverter.getQueryConverter("DEFAULT");
		MTable table = RestUtils.getTableAndCheckAccess(tableName);

		ConvertedQuery convertedStatement = converter.convertStatement(view, tableName, whereClause);
		String convertedWhereClause = getFullWhereClause(convertedStatement);

		Query query = RestUtils.getQuery(tableName, convertedWhereClause,  new ArrayList<Object>(convertedStatement.getParameters()));
		addOrderByWhenValid(table, orderBy, query);

		query.setQueryTimeout(DEFAULT_QUERY_TIMEOUT);
		rowCount = query.count();
		pageCount = 1;
		if (MAX_RECORDS_SIZE > 0 && (top > MAX_RECORDS_SIZE || top <= 0))
			top = MAX_RECORDS_SIZE;

		if (top > 0 && rowCount > top) {
			pageCount = (int)Math.ceil(rowCount / (double)top);
		} 
		query.setPageSize(top);
		query.setRecordstoSkip(skip);

		if (includeColumns != null && includeColumns.length > 0)
			query.selectColumns(includeColumns);
		
		sqlStatement= query.getSQL();
		return query.list();
	}
	
	private String getRequestWhereClause() {
		return !Util.isEmpty(filter, true) ? filter : "";
	}
	
	private String getFullWhereClause(ConvertedQuery convertedStatement) {
		String convertedWhereClause = convertedStatement.getWhereClause();
		if (log.isLoggable(Level.INFO)) 
			log.info("Where Clause: " + convertedWhereClause);

		if (validationRuleID != null) {
			MValRule validationRule = getValidationRule(validationRuleID);
			if (validationRule == null ||validationRule.getAD_Val_Rule_ID() == 0) {
				throw new IDempiereRestException("Invalid validation rule", "No match found for validation with ID: " + validationRuleID, Status.NOT_FOUND);
			}

			if (validationRule.getCode() != null) {
				if (!Util.isEmpty(convertedWhereClause))
					convertedWhereClause =  convertedWhereClause + " AND ";
				convertedWhereClause = convertedWhereClause + "(" + validationRule.getCode() + ")";

				if (!Util.isEmpty(context)) {
					convertedWhereClause = parseContext(convertedWhereClause, context);
				}
			}
		}
		
		//add optional where clause from view definition
		if (view != null && !Util.isEmpty(view.getWhereClause(), true)) {
			String viewWhereClause = view.getWhereClause();
			int atIdx = viewWhereClause.indexOf("@");
			if (atIdx >= 0 && viewWhereClause.indexOf("@", atIdx+1) > atIdx) {
				viewWhereClause = Env.parseContext(Env.getCtx(), -1, viewWhereClause, false);
			}
			if (!Util.isEmpty(convertedWhereClause))
				convertedWhereClause =  convertedWhereClause + " AND ";			
			convertedWhereClause = convertedWhereClause + "(" + viewWhereClause + ")";
		}
		
		return convertedWhereClause;
	} 
	
	private MValRule getValidationRule(String validationRuleID) {
		return (MValRule) RestUtils.getPO(MValRule.Table_Name, validationRuleID, false, false);
	}
	
	private String parseContext(String whereClause, String context) {
		String parsedWhereClause = whereClause;
		int windowNo = windowNoAtomic.getAndIncrement();

		for (String contextNameValue : context.split(CONTEXT_VARIABLES_SEPARATOR)) {
			String[] namevaluePair = contextNameValue.split(CONTEXT_NAMEVALUE_SEPARATOR);
			String contextName = namevaluePair[0];
			String contextValue = namevaluePair[1];
			
			if (!isValidContextValue(contextValue)) 
				continue;
			Env.setContext(Env.getCtx(), windowNo, contextName, contextValue);
		}
		
		parsedWhereClause = Env.parseContext(Env.getCtx(), windowNo, parsedWhereClause, false, true);
		Env.clearWinContext(windowNo);

		return parsedWhereClause;
	}
	
	/**
	 * Validates the context value to avoid
	 * potential SQL injection
	 * @param value
	 * @return
	 */
	private boolean isValidContextValue(String value) {
		// At the moment accept context values just composed by letters, numbers, space and dash (for UUID)
		// this is mainly to avoid the usage of strange characters (like semicolon or quotes) opening the door for SQL injection
		final String sanitize = "^[A-Za-z0-9\\s\\-]+$";
		return Pattern.matches(sanitize, value);
	}
	
	private boolean addOrderByWhenValid(MTable table, String orderBy, Query query) {
		if (!Util.isEmpty(orderBy, true)) {
			String[] columnNames = orderBy.split("[,]");
			List<String> virtualColumns = new ArrayList<String>();
			for (String columnName : columnNames) {
				columnName = columnName.trim();

				if (columnName.contains(" ")) {
					String[] names = columnName.split(" ");
					columnName = names[0];
					String orderPreference = names[1];
					if (names.length > 2 || (!"asc".equals(orderPreference.toLowerCase()) && !"desc".equals(orderPreference.toLowerCase()))) {
						log.log(Level.WARNING, "Invalid order by clause.");
						return false;
					}
				}
				MColumn column = table.getColumn(columnName);
				if (column == null) {
					log.log(Level.WARNING, "Column: " + columnName + " is not a valid column to be ordered by");
					return false;
				}
				if (!Util.isEmpty(column.getColumnSQL())) {
					virtualColumns.add(columnName);
				}
			}
			query.setOrderBy(orderBy);
			if (virtualColumns.size() > 0)
				query.setVirtualColumns(virtualColumns.toArray(new String[virtualColumns.size()]));
			return true;
		}

		return false;
	}

	public int getRowCount() {
		return rowCount;
	}

	public int getPageCount() {
		return pageCount;
	}
	
	public String getSQLStatement() {
		return sqlStatement;
	}
	
	public int getTop() {
		return top;
	}
	
	public int getSkip() {
		return skip;
	}
}

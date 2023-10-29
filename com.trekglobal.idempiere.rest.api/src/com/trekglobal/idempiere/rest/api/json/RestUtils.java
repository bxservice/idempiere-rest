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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import javax.ws.rs.core.Response.Status;

import org.compiere.model.MColumn;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Language;
import org.compiere.util.Util;

public class RestUtils {

	private final static CLogger log = CLogger.getCLogger(RestUtils.class);
	private final static String UUID_REGEX="[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";

	/**
	 * @param value
	 * @return true if value is a UUID identifier
	 */
	private static boolean isUUID(String value) {
		return value == null ? false : value.matches(UUID_REGEX);
	}
	
	public static Query getQuery(String tableName, String recordID, boolean fullyQualified, boolean RW) {
		boolean isUUID = isUUID(recordID);
		
		String keyColumn = getKeyColumn(tableName, isUUID);
		
		Query query = new Query(Env.getCtx(), tableName, keyColumn + "=?", null);
		
		if (fullyQualified || RW)
			query.setApplyAccessFilter(fullyQualified, RW);
		
		if (isUUID)
			query.setParameters(recordID);
		else
			query.setParameters(Util.isEmpty(recordID) ? null : getIntegerValue(recordID));
		
		return query;
	}

	public static PO getPO(String tableName, String recordID, boolean fullyQualified, boolean RW) {
		return getQuery(tableName, recordID, fullyQualified, RW).first();
	}
	
	private static String getKeyColumn(String tableName, boolean isUUID) {
		return isUUID ? PO.getUUIDColumnName(tableName) : getKeyColumnName(tableName);
	}
	
	private static int getIntegerValue(String id) {
		try {
			return Integer.parseInt(id);
		} catch(NumberFormatException ex) {
			throw new IDempiereRestException("Request Error", "Wrong ID "+ id + " is not an UU value nor a valid integer ID", Status.BAD_REQUEST);
		}
	} 
	
	public static HashMap<String, ArrayList<String>> getIncludes(String tableName, String select, String details) {
		
		if (Util.isEmpty(select, true) || Util.isEmpty(tableName, true))
			return null;

		HashMap<String, ArrayList<String>> tableSelect = new HashMap<>();

		boolean hasDetail = !Util.isEmpty(details, true); 
		MTable mTable = MTable.get(Env.getCtx(), tableName);
		String[] columnNames = select.split("[,]");
		for(String columnName : columnNames) {
			MTable table = mTable;
			if (hasDetail && columnName.contains("/")) { //Detail select
				String selectTableName = columnName.substring(0, columnName.indexOf("/")).trim();
				if (details.toLowerCase().contains(selectTableName.toLowerCase())) {
					table = MTable.get(Env.getCtx(), selectTableName);
					columnName = columnName.substring(columnName.indexOf("/")+1, columnName.length());
				} else {
					throw new IDempiereRestException(selectTableName + " does not make part of the request body.", Status.BAD_REQUEST);
				}
			}
			if (table.getColumnIndex(columnName.trim()) < 0)
				throw new IDempiereRestException(columnName + " is not a valid column of table " + table.getTableName(), Status.BAD_REQUEST);

			MColumn mColumn = table.getColumn(columnName.trim());
			if (MRole.getDefault().isColumnAccess(table.getAD_Table_ID(), mColumn.getAD_Column_ID(), true)) {
				if (tableSelect.get(table.getTableName()) == null)
					tableSelect.put(table.getTableName(), new ArrayList<String>());
				
				tableSelect.get(table.getTableName()).add(columnName.trim());
			}
		}

		return tableSelect;
	}
	
	public static String[] getSelectedColumns(String tableName, String selectClause) {
		List<String> selectedColumns = new ArrayList<String>();
		if (Util.isEmpty(selectClause, true) || Util.isEmpty(tableName, true))
			return new String[0];
		
		MTable mTable = MTable.get(Env.getCtx(), tableName);
		String[] columnNames = selectClause.split("[,]");
		for(String columnName : columnNames) {
			MTable table = mTable;
			if (table.getColumnIndex(columnName.trim()) < 0)
				throw new IDempiereRestException(columnName + " is not a valid column of table " + table.getTableName(), Status.BAD_REQUEST);

			MColumn mColumn = table.getColumn(columnName.trim());
			if (MRole.getDefault().isColumnAccess(table.getAD_Table_ID(), mColumn.getAD_Column_ID(), true)) {
				selectedColumns.add(columnName.trim());
			}
		}

		return selectedColumns.toArray(new String[selectedColumns.size()]);
	}
	
	/**
	 * Get the query (translating table _v to _vt when conditions are met)
	 * @param tableName
	 * @param whereClause
	 * @param params
	 * @return Query
	 */
	public static Query getQuery(String tableName, String whereClause, List<Object> params, String includeInactive) {
		MTable table = getQueryTable(tableName);

		if (   table != null
			&& table.isView()
			&& tableName.toLowerCase().endsWith("_v")
			&& table.getTableName().toLowerCase().endsWith("_vt")) {
			if (!Util.isEmpty(whereClause))
				whereClause = whereClause + " AND ";

			whereClause = whereClause + "AD_Language=?";
			params.add(Env.getAD_Language(Env.getCtx()));
		}
		
		if(includeInactive!=null && includeInactive.equals("only")) {
			if (!Util.isEmpty(whereClause))
				whereClause = whereClause + " AND ";
			
			whereClause += "isActive=?";
			params.add("N");
		}

		Query query = new Query(Env.getCtx(), table, whereClause, null);

		query.setApplyAccessFilter(true, false);
		
		if(includeInactive==null)
			query.setOnlyActiveRecords(true);
		
		query.setParameters(params);

		return query;
	}

	/**
	 * Get the table associated with the query
	 * if it is a view ending with _v and exists a _vt with columns, then return the _vt table instead
	 * @param tableName
	 * @return MTable
	 */
	private static MTable getQueryTable(String tableName) {
		MTable table = MTable.get(Env.getCtx(), tableName);

		boolean isBaseLanguage = Language.isBaseLanguage(Env.getAD_Language(Env.getCtx()));
		if (   !isBaseLanguage
			&& table != null
			&& table.isView()
			&& tableName.toLowerCase().endsWith("_v")) {
		    MTable trl_view = MTable.get(Env.getCtx(), tableName + "t");
		    if (trl_view != null && trl_view.get_ColumnCount() > 0)
		        return trl_view;
		}

		return table;
	}
	
	public static MTable getTableAndCheckAccess(String tableName) {
		return getTableAndCheckAccess(tableName, false);
	}
	
	public static MTable getTableAndCheckAccess(String tableName, boolean isReadWrite) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID()==0) {
			throw new IDempiereRestException("Invalid table name", "No match found for table name: " + tableName, Status.NOT_FOUND);
		}
		
		if (!hasAccess(table, isReadWrite)) {
			throw new IDempiereRestException("Access denied", "Access denied for table: " + tableName, Status.FORBIDDEN);
		}
		
		return table;

	}
	
	public static boolean hasAccess(MTable table, boolean isReadWrite) {
		MRole role = MRole.getDefault();
		if (role == null)
			return false;
		
		StringBuilder builder = new StringBuilder("SELECT DISTINCT a.AD_Window_ID FROM AD_Window a JOIN AD_Tab b ON a.AD_Window_ID=b.AD_Window_ID ");
		builder.append("WHERE a.IsActive='Y' AND b.IsActive='Y' AND b.AD_Table_ID=?");
		try (PreparedStatement stmt = DB.prepareStatement(builder.toString(), null)) {
			stmt.setInt(1, table.getAD_Table_ID());			
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				int windowId = rs.getInt(1);
				Boolean hasReadWriteAccess = role.getWindowAccess(windowId);
				if (hasReadWriteAccess != null) {
					if (!isReadWrite || hasReadWriteAccess.booleanValue())
						return true;
				}
			}
		} catch (SQLException ex) {
			log.log(Level.SEVERE, ex.getMessage(), ex);
			throw new RuntimeException(ex.getMessage());
		}
		
		//If no window or no access to the window - check if the role has read/write access to the table
		return role.isTableAccess(table.getAD_Table_ID(), false);
	}
	
	public static String getKeyColumnName(String tableName) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		String[] keyColumns = table.getKeyColumns();
		
		if (keyColumns.length <= 0 || keyColumns.length > 1)
			throw new IDempiereRestException("Wrong detail", "Cannot expand to the detail table because it has none or more than one primary key: " + tableName, Status.INTERNAL_SERVER_ERROR);

		return keyColumns[0];
	}
}

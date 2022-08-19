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
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.compiere.model.MColumn;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

public class RestUtils {

	private final static String UUID_REGEX="[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";

	/**
	 * @param value
	 * @return true if value is a UUID identifier
	 */
	private static boolean isUUID(String value) {
		return value == null ? false : value.matches(UUID_REGEX);
	}
	
	public static PO getPO(String tableName, String recordID, boolean fullyQualified, boolean RW) {
		boolean isUUID = isUUID(recordID);
		
		String keyColumn = getKeyColumn(tableName, isUUID);
		
		Query query = new Query(Env.getCtx(), tableName, keyColumn + "=?", null);
		
		if (fullyQualified || RW)
			query.setApplyAccessFilter(fullyQualified, RW);
		
		if (isUUID)
			query.setParameters(recordID);
		else
			query.setParameters(Integer.parseInt(recordID));
		
		return query.first();
	}
	
	private static String getKeyColumn(String tableName, boolean isUUID) {
		return isUUID ? PO.getUUIDColumnName(tableName) : tableName + "_ID";
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
	
	public static Query getQuery(String tableName, String whereClause, List<Object> params) {
		MTable table = getTable(tableName);

		if (table != null && table.isView() && tableName.toLowerCase().endsWith("_vt")) {
			if (!Util.isEmpty(whereClause))
				whereClause = whereClause + " AND ";

			whereClause = whereClause + "AD_Language=?";
			params.add(Env.getAD_Language(Env.getCtx()));
		}

		Query query = new Query(Env.getCtx(), table, whereClause, null);

		query.setApplyAccessFilter(true, false)
			.setOnlyActiveRecords(true)
			.setParameters(params);

		return query;
	}

	private static MTable getTable(String tableName) {
		MTable table = MTable.get(Env.getCtx(), tableName);

		if (table != null && table.isView() && tableName.toLowerCase().endsWith("_v")) {
			boolean hasVT = DB.isTableOrViewExists(tableName+"t");
			if (hasVT) {
				table = MTable.get(Env.getCtx(), tableName+ "t");
			}
		} 

		return table;
	}
}

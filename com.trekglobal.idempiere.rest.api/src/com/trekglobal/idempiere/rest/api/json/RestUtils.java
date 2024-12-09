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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.ws.rs.core.Response.Status;

import org.compiere.model.MAcctSchema;
import org.compiere.model.MAcctSchemaElement;
import org.compiere.model.MClientInfo;
import org.compiere.model.MColumn;
import org.compiere.model.MCountry;
import org.compiere.model.MRole;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.MUserPreference;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Language;
import org.compiere.util.Util;

import com.trekglobal.idempiere.rest.api.model.MRestView;

public class RestUtils {

	private final static CLogger log = CLogger.getCLogger(RestUtils.class);
	private final static String UUID_REGEX="[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
	private final static String EXPORT_UU_LOOKUP_SYSCONFIG_NAME = "REST_TABLES_EXPORT_LOOKUP_UU";

	/**
	 * @param value
	 * @return true if value is a UUID identifier
	 */
	public static boolean isUUID(String value) {
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
	public static Query getQuery(String tableName, String whereClause, List<Object> params) {
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

		Query query = new Query(Env.getCtx(), table, whereClause, null)
				.setApplyAccessFilter(true, false)
				.setParameters(params);

		if (! whereClause.toLowerCase().matches(".*\\bisactive\\b.*"))
			query.setOnlyActiveRecords(true);

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
	
	public static boolean hasRoleUpdateAccess(int AD_Client_ID, int AD_Org_ID, int AD_Table_ID, int Record_ID, boolean isNew) {
		return MRole.getDefault(Env.getCtx(), false).canUpdate(AD_Client_ID, AD_Org_ID, AD_Table_ID, Record_ID, isNew);
	}
	
	/**
	 * Check if the role has access to this column
	 * @param AD_Table_ID
	 * @param AD_Column_ID
	 * @param readOnly
	 * @return true if user has access
	 */
	public static boolean hasRoleColumnAccess(int AD_Table_ID, int AD_Column_ID, boolean readOnly) {
		return MRole.getDefault(Env.getCtx(), false).isColumnAccess(AD_Table_ID, AD_Column_ID, readOnly);
	}

	/**
	 * Get view definition and perform access right check
	 * @param name
	 * @param isReadWrite
	 * @return Rest view. Throw IDempiereRestException if current role doesn't has access
	 */
	public static MRestView getViewAndCheckAccess(String name, boolean isReadWrite) {
		MRestView view = MRestView.get(name);
		if (view == null || view.get_ID()==0) {
			return null;
		}
		
		checkViewAccess(view, isReadWrite);
		
		return view;
	}

	/**
	 * Check view access. Throw IDempiereRestException if current role doesn't has access
	 * @param view
	 * @param isReadWrite
	 */
	public static void checkViewAccess(MRestView view, boolean isReadWrite) {
		if (!hasViewAccess(view, isReadWrite)) {
			throw new IDempiereRestException("Access denied", "Access denied for view: " + view.getName(), Status.FORBIDDEN);
		}
	}
	
	/**
	 * Is current role has access to view
	 * @param view
	 * @param isReadWrite
	 * @return true if current role has access to view
	 */
	public static boolean hasViewAccess(MRestView view, boolean isReadWrite) {
		MRole role = MRole.getDefault();
		if (role == null)
			return false;
		return view.hasAccess(role, isReadWrite);
	}
	
	public static String getKeyColumnName(String tableName) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		String[] keyColumns = table.getKeyColumns();
		
		if (keyColumns.length <= 0 || keyColumns.length > 1)
			throw new IDempiereRestException("Wrong detail", "Cannot expand to the detail table because it has none or more than one primary key: " + tableName, Status.INTERNAL_SERVER_ERROR);

		return keyColumns[0];
	}
	
	/**
	 * Defines whether the UUID value of a lookup record is returned in the response  
	 * @param tableName
	 * @return true if the lookup table should return the UUID in the REST response
	 */
	public static boolean isReturnUULookup(String tableName) {
		String exportedUUTables = MSysConfig.getValue(EXPORT_UU_LOOKUP_SYSCONFIG_NAME, Env.getAD_Client_ID(Env.getCtx()));
		return !Util.isEmpty(exportedUUTables) && 
				(exportedUUTables.equals("ALL") || isStringInCommaSeparatedList(exportedUUTables, tableName));
	}

	public static boolean isStringInCommaSeparatedList(String commaSeparatedString, String stringToCompare) {
		String[] tableArray = commaSeparatedString.split(",");
		for (String tableName : tableArray) {
			if (tableName.trim().equalsIgnoreCase(stringToCompare.trim())) {
				return true;
			}
		}

		return false;
	}

	private static CCache<Integer, Properties> ctxSessionCache = new CCache<Integer, Properties>("REST_SessionCtxCache", 100, MSysConfig.getIntValue("REST_TOKEN_EXPIRE_IN_MINUTES", 60, Env.getAD_Client_ID(Env.getCtx())));

	/**
	 * Set the session context variables
	 */
	public static void setSessionContextVariables(Properties ctx) {
		int sessionId = Env.getContextAsInt(ctx, Env.AD_SESSION_ID);
		if (sessionId > 0) {
			if (ctxSessionCache.containsKey(sessionId)) {
				// key found in cache, just set the properties found in cache and return
				Properties savedCtx = ctxSessionCache.get(sessionId);
				setCtxFromSavedCtx(ctx, savedCtx);
				return;
			}
		}

		// Context session not found in cache
		if (Util.isEmpty(Env.getContext(ctx, Env.DATE)))
			Env.setContext(ctx, Env.DATE, new Timestamp(System.currentTimeMillis()));

		boolean roleSet = ! Util.isEmpty(Env.getContext(ctx, Env.AD_ROLE_ID));
		if (roleSet) {
			MRole role = MRole.getDefault();
			Env.setContext(ctx, Env.SHOW_ACCOUNTING, role.isShowAcct());
			Env.setPredefinedVariables(ctx, -1, role.getPredefinedContextVariables());
			if (role.isShowAcct())
				Env.setContext(ctx, Env.SHOW_ACCOUNTING, Ini.getProperty(Ini.P_SHOW_ACCT));
			else
				Env.setContext(ctx, Env.SHOW_ACCOUNTING, "N");
			Env.setContext(ctx, Env.SHOW_ADVANCED, MRole.getDefault().isAccessAdvanced());
		}

		int clientId = Env.getAD_Client_ID(ctx);
		int orgId = Env.getAD_Org_ID(ctx);
		if (clientId > 0) {
			MClientInfo clientInfo = MClientInfo.get(ctx, clientId);
			/** Define AcctSchema , Currency, HasAlias **/
			if (clientInfo.getC_AcctSchema1_ID() > 0) {
				MAcctSchema primary = MAcctSchema.get(ctx, clientInfo.getC_AcctSchema1_ID());
				Env.setContext(ctx, Env.C_ACCTSCHEMA_ID, primary.getC_AcctSchema_ID());
				Env.setContext(ctx, Env.C_CURRENCY_ID, primary.getC_Currency_ID());
				Env.setContext(ctx, Env.HAS_ALIAS, primary.isHasAlias());
				MAcctSchemaElement[] els = MAcctSchemaElement.getAcctSchemaElements(primary);
				for (MAcctSchemaElement el : els)
					Env.setContext(ctx, "$Element_" + el.getElementType(), "Y");
			}
			MAcctSchema[] ass = MAcctSchema.getClientAcctSchema(ctx, clientId);
			if (ass != null && ass.length > 1) {
				for (MAcctSchema as : ass) {
					if (as.getAD_OrgOnly_ID() != 0) {
						if (as.isSkipOrg(orgId)) {
							continue;
						} else  {
							Env.setContext(ctx, Env.C_ACCTSCHEMA_ID, as.getC_AcctSchema_ID());
							Env.setContext(ctx, Env.C_CURRENCY_ID, as.getC_Currency_ID());
							Env.setContext(ctx, Env.HAS_ALIAS, as.isHasAlias());
							MAcctSchemaElement[] els = MAcctSchemaElement.getAcctSchemaElements(as);
							for (MAcctSchemaElement el : els)
								Env.setContext(ctx, "$Element_" + el.getElementType(), "Y");
							break;
						}
					}
				}
			}
		}

		Env.setContext(ctx, Env.SHOW_TRANSLATION, Ini.getProperty(Ini.P_SHOW_TRL));
		Env.setContext(ctx, Env.DEVELOPER_MODE, Util.isDeveloperMode() ? "Y" : "N");

		if (Env.getAD_User_ID(ctx) > 0) {
			MUserPreference userPreference = MUserPreference.getUserPreference(Env.getAD_User_ID(ctx), Env.getAD_Client_ID(ctx));
			userPreference.fillPreferences();
		}

		Env.setContext(ctx, Env.C_COUNTRY_ID, MCountry.getDefault().getC_Country_ID());

		// TODO: Preferences?  // can have impact on performance, driven by SysConfig?
		// TODO: Defaults?     // can have impact on performance, driven by SysConfig?
		// TODO: ModelValidationEngine.get().afterLoadPreferences(m_ctx);    // is this necessary?

		if (sessionId > 0) {
			Properties saveCtx = new Properties();
			saveCtx.putAll(ctx);
			ctxSessionCache.put(sessionId, saveCtx);
		}
	}

	/**
	 * Set a context from a saved/cached context
	 * @param ctx
	 * @param savedCtx
	 */
	private static void setCtxFromSavedCtx(Properties ctx, Properties savedCtx) {
		savedCtx.forEach((key, value) -> {
			if (value instanceof String)
				Env.setContext(ctx, key.toString(), value.toString());
			else
				ctx.put(key, value);
		});
	}

	/**
	 * Remove a session from cache - on logout
	 * @param sessionId
	 */
	public static void removeSavedCtx(int sessionId) {
		ctxSessionCache.remove(sessionId);
	}

}

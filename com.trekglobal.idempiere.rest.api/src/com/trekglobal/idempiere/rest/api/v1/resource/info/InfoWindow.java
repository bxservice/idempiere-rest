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
* - Trek Global Corporation                                           *
* - Heng Sin Low                                                      *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.v1.resource.info;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.adempiere.model.IInfoColumn;
import org.adempiere.model.MInfoProcess;
import org.compiere.minigrid.ColumnInfo;
import org.compiere.minigrid.IDColumn;
import org.compiere.minigrid.UUIDColumn;
import org.compiere.model.AccessSqlParser;
import org.compiere.model.AccessSqlParser.TableInfo;
import org.compiere.model.GridField;
import org.compiere.model.GridFieldVO;
import org.compiere.model.MColumn;
import org.compiere.model.MInfoColumn;
import org.compiere.model.MInfoWindow;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.X_AD_InfoColumn;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.compiere.util.ValueNamePair;
import org.idempiere.db.util.SQLFragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.RestUtils;
import com.trekglobal.idempiere.rest.api.json.TypeConverterUtils;

/**
 * 
 * @author hengsin
 *
 */
public class InfoWindow {

	private static final CLogger log = CLogger.getCLogger(InfoWindow.class);
	
	private MInfoWindow infoWindowModel;
	private TableInfo[] tableInfos;
	private String p_whereClause;
	private String p_orderBy;
	private MInfoColumn[] infoColumns;
	private ArrayList<GridField> gridFields;
	private MInfoColumn keyColumnOfView;
	
	private List<ColumnInfo> columnInfos;
	private String p_keyColumn;
	private String m_sqlMain;
	private String m_sqlOrder;
	private MInfoProcess[] infoProcessList;
	private String tableName;
	private Map<String, JsonElement> queryParameters;
	private boolean useAnd;
	
	/**
	 * 
	 * @param iw
	 * @param whereClause
	 * @param orderBy
	 * @param and
	 */
	public InfoWindow(MInfoWindow iw, String orderBy, boolean and) {
		infoWindowModel = iw; 
		p_orderBy = orderBy;
		useAnd = and;
		loadInfoDefinition();
		prepareQuery();
	}

	private void loadInfoDefinition() {
		tableName = MTable.getTableName(Env.getCtx(), infoWindowModel.getAD_Table_ID());
		p_keyColumn = RestUtils.getKeyColumnName(tableName);
		
		AccessSqlParser sqlParser = new AccessSqlParser("SELECT * FROM " + infoWindowModel.getFromClause());
		tableInfos = sqlParser.getTableInfo(0);
		if (tableInfos[0].getSynonym() != null && tableInfos[0].getSynonym().trim().length() > 0) {
			String alias = tableInfos[0].getSynonym().trim();
			if (p_whereClause != null && p_whereClause.trim().length() > 0) {
				p_whereClause = p_whereClause.replace(tableName+".", alias+".");
			}					
		}
		
		infoColumns = infoWindowModel.getInfoColumns(tableInfos);
	
		gridFields = new ArrayList<GridField>();
		
		for(MInfoColumn infoColumn : infoColumns) {
			if (infoColumn.isKey())
				keyColumnOfView = infoColumn;
			GridField gridField = toGridField(infoColumn);
			gridFields.add(gridField);
		}
		
		StringBuilder builder = new StringBuilder(p_whereClause != null ? p_whereClause.trim() : "");
		String infoWhereClause = infoWindowModel.getWhereClause();
		if (infoWhereClause != null && infoWhereClause.indexOf("@") >= 0) {
			infoWhereClause = Env.parseContext(Env.getCtx(), 0, infoWhereClause, true, false);
			if (infoWhereClause.length() == 0)
				log.log(Level.SEVERE, "Cannot parse context= " + infoWindowModel.getWhereClause());
		}
		if (infoWhereClause != null && infoWhereClause.trim().length() > 0) {								
			if (builder.length() > 0) {
				builder.append(" AND ");
			}
			builder.append(infoWhereClause);
			p_whereClause = builder.toString();
		}
		
		infoProcessList = infoWindowModel.getInfoProcess(false);		
	}

	private GridField toGridField(MInfoColumn infoColumn) {
		String columnName = infoColumn.getColumnName();
		boolean isMandatory = infoColumn.isMandatory() && infoColumn.isQueryCriteria();
		GridFieldVO vo = GridFieldVO.createParameter(Env.getCtx(), 0, 0, infoWindowModel.getAD_InfoWindow_ID(), 0,
				columnName, infoColumn.get_Translation("Name"), infoColumn.getAD_Reference_ID(), 
				infoColumn.getAD_Reference_Value_ID(), isMandatory, false, infoColumn.get_Translation("Placeholder"));
		if (infoColumn.getAD_Val_Rule_ID() > 0) {
			vo.ValidationCode = infoColumn.getAD_Val_Rule().getCode();
			if (vo.lookupInfo != null) {
				vo.lookupInfo.ValidationCode = vo.ValidationCode;
				vo.lookupInfo.IsValidated = false;
			}
		}
		if (infoColumn.getDisplayLogic() != null)					
			vo.DisplayLogic =  infoColumn.getDisplayLogic();
		if (infoColumn.isQueryCriteria() && infoColumn.getDefaultValue() != null)
			vo.DefaultValue = infoColumn.getDefaultValue();
		String desc = infoColumn.get_Translation("Description");
		vo.Description = desc != null ? desc : "";
		String help = infoColumn.get_Translation("Help");
		vo.Help = help != null ? help : "";
		vo.AD_FieldStyle_ID = infoColumn.getAD_FieldStyle_ID();
		GridField gridField = new GridField(vo);
		return gridField;
	}
	
	private void prepareQuery() {		
		List<ColumnInfo> list = new ArrayList<ColumnInfo>();
		String keyTableAlias = tableInfos[0].getSynonym() != null && tableInfos[0].getSynonym().trim().length() > 0 
				? tableInfos[0].getSynonym()
				: tableInfos[0].getTableName();
					
		String keySelectClause = keyTableAlias+"."+p_keyColumn;
		ColumnInfo columnInfo;
		columnInfo = new ColumnInfo(" ", keySelectClause, IDColumn.class, true, false, null, p_keyColumn);
		if (p_keyColumn.endsWith("_UU"))
			columnInfo = new ColumnInfo(" ", keySelectClause, UUIDColumn.class, true, false, null, p_keyColumn);
		else
			columnInfo = new ColumnInfo(" ", keySelectClause, IDColumn.class, true, false, null, p_keyColumn);
		columnInfo.setGridField(findGridField(p_keyColumn));
		list.add(columnInfo);
		
		int i = 0;
		for(MInfoColumn infoColumn : infoColumns) 
		{						
			if (infoColumn.isDisplayed(Env.getCtx(), 0)) 
			{
				if (infoColumn.getAD_Reference_ID() == DisplayType.ID && infoColumn.getSelectClause().equalsIgnoreCase(keySelectClause))
					continue;
				
				GridField gridField = gridFields.get(i);
				columnInfo = toColumnInfo(infoColumn, gridField);
				list.add(columnInfo);
				
			}		
			i++;
		}
				
		columnInfos = list;
		
		validateOrderByParameter(); 
		
		prepareQuery(list.toArray(new ColumnInfo[0]), infoWindowModel.getFromClause(), p_whereClause, 
				p_orderBy != null ? p_orderBy : infoWindowModel.getOrderByClause());
	}
	
	private ColumnInfo toColumnInfo(MInfoColumn infoColumn, GridField gridField) {
		ColumnInfo columnInfo = null;
		String colSQL = infoColumn.getSelectClause();
		if (! colSQL.toUpperCase().contains(" AS "))
			colSQL += " AS " + infoColumn.getColumnName();
		if (infoColumn.getAD_Reference_ID() == DisplayType.ID) 
		{
			columnInfo = new ColumnInfo(infoColumn.get_Translation("Name"), colSQL, DisplayType.getClass(infoColumn.getAD_Reference_ID(), true), infoColumn.isReadOnly());
		}
		else if (DisplayType.isLookup(infoColumn.getAD_Reference_ID()))
		{
			if (DisplayType.isList(infoColumn.getAD_Reference_ID()))
			{
				columnInfo = new ColumnInfo(infoColumn.get_Translation("Name"), colSQL, ValueNamePair.class, (String)null, infoColumn.isReadOnly());
			}
			else
			{
				columnInfo = createLookupColumnInfo(tableInfos, gridField, infoColumn);
			}					
		}
		else  
		{
			columnInfo = new ColumnInfo(infoColumn.get_Translation("Name"), colSQL, DisplayType.getClass(infoColumn.getAD_Reference_ID(), true), infoColumn.isReadOnly());
		}
		columnInfo.setColDescription(infoColumn.get_Translation("Description"));
		columnInfo.setAD_Reference_ID(infoColumn.getAD_Reference_ID());
		columnInfo.setGridField(gridField);
		columnInfo.setColumnName(infoColumn.getColumnName());
		
		return columnInfo;
	}
	
	private ColumnInfo createLookupColumnInfo(TableInfo[] tableInfos,
			GridField gridField, MInfoColumn infoColumn) {
		String columnName = gridField.getColumnName();
		String validationCode = "";
		MLookupInfo lookupInfo = MLookupFactory.getLookupInfo(Env.getCtx(), 0, 0, infoColumn.getAD_Reference_ID(), Env.getLanguage(Env.getCtx()), columnName, infoColumn.getAD_Reference_Value_ID(), false, validationCode);
		String displayColumn = lookupInfo.DisplayColumn;
		
		int index = infoColumn.getSelectClause().indexOf(".");
		if (index == infoColumn.getSelectClause().lastIndexOf("."))
		{
			String synonym = infoColumn.getSelectClause().substring(0, index);
			for(TableInfo tableInfo : tableInfos)
			{
				if (tableInfo.getSynonym() != null && tableInfo.getSynonym().equals(synonym)) 
				{
					if (tableInfo.getTableName().equalsIgnoreCase(lookupInfo.TableName))
					{
						displayColumn = displayColumn.replace(lookupInfo.TableName+".", tableInfo.getSynonym()+".");
						ColumnInfo columnInfo = new ColumnInfo(infoColumn.get_Translation("Name"), displayColumn, KeyNamePair.class, infoColumn.getSelectClause(), infoColumn.isReadOnly());
						return columnInfo;
					}
					break;
				}
			}
		}
		
		String colSQL = infoColumn.getSelectClause();
		if (! colSQL.toUpperCase().contains(" AS "))
			colSQL += " AS " + infoColumn.getColumnName();
        Class<?> colClass = columnName.endsWith("_ID") ? KeyNamePair.class : String.class;
		ColumnInfo columnInfo = new ColumnInfo(infoColumn.get_Translation("Name"), colSQL, colClass, (String)null, infoColumn.isReadOnly());
		return columnInfo;
	}
	
	private void prepareQuery(ColumnInfo[] layout, String from, String where,
			String orderBy) {
		StringBuilder builder = new StringBuilder ("SELECT ");
		
		// add columns & sql
        for (int columnIndex = 0; columnIndex < layout.length; columnIndex++)
        {
            //  create sql
            if (columnIndex > 0)
            {
                builder.append(", ");
            }
            builder.append(layout[columnIndex].getColSQL());

            //  adding ID column
            if (layout[columnIndex].isKeyPairCol())
            {
                builder.append(",").append(layout[columnIndex].getKeyPairColSQL());
            }
        }
        
        builder.append( " FROM ").append(from);
        builder.append(" WHERE ");
        if (where != null)
        	builder.append(where);

        //
		String sql = builder.toString();
		m_sqlMain = sql;
		//
		m_sqlOrder = "";
		if (orderBy != null && orderBy.trim().length() > 0)
			m_sqlOrder = " ORDER BY " + orderBy;
		if (m_sqlMain.indexOf("@") >= 0) {
			sql = Env.parseContext(Env.getCtx(), 0, m_sqlMain, true);
			if (sql == null || sql.length() == 0) {
				log.severe("Failed to parsed sql. sql=" + m_sqlMain);
			} else {
				m_sqlMain = sql;
			}
		}
		
		addViewIDToQuery();
		addKeyViewToQuery();
		
		if (m_sqlMain.length() > 0 &&  infoWindowModel.isDistinct()) {
			m_sqlMain = m_sqlMain.substring("SELECT ".length());
			m_sqlMain = "SELECT DISTINCT " + m_sqlMain;			
		}	
		
		if (m_sqlOrder != null && m_sqlOrder.indexOf("@") >= 0) {
			sql = Env.parseContext(Env.getCtx(), 0, m_sqlOrder, true, false);
			if (sql == null || sql.length() == 0) {
				log.severe("Failed to parsed sql. sql=" + m_sqlOrder);
			} else {
				m_sqlOrder = sql;
			}
		}
	}
	
	/**
	 * add all ViewID in each MInfoProcess to query
	 * if main query have subquery in SELECT, it will beak or incorrect
	 */
	private void addViewIDToQuery () {
		m_sqlMain = addMoreColumnToQuery (m_sqlMain, infoProcessList);
	}
	
	/**
	 * if {@link #keyColumnOfView} not null and not display, add query to query it's value
	 */
	private void addKeyViewToQuery () {
		if (isNeedAppendKeyViewData()){
			m_sqlMain = addMoreColumnToQuery (m_sqlMain, new IInfoColumn [] {keyColumnOfView});
		}
	}
	
	private boolean isNeedAppendKeyViewData() {
		return (keyColumnOfView != null && !keyColumnOfView.isDisplayed(Env.getCtx(), 0));
	}
		
	/**
	 * because data of infoColumn have isDisplay = false not load, 
	 * just display column is load to show in List.
	 * Some function receive data from hidden column as viewID in infoProcess
	 * or parentLink of infoRelateWindow.
	 * 
	 * this function just add column name of hidden infoWindow to end of query
	 * @param sqlMain main sql to append column
	 * @param listInfoColumn list of PO contain infoColumnID, this infoColumnID will add to query
	 * @return sql after append column
	 */
	private String addMoreColumnToQuery (String sqlMain, IInfoColumn [] listInfoColumn) {
		if (sqlMain == null || sqlMain.length() == 0 || listInfoColumn == null || listInfoColumn.length == 0){
			return sqlMain;
		}
				
		int fromIndex = sqlMain.indexOf("FROM");
		// split Select and from clause
		String selectClause = sqlMain.substring(0, fromIndex);
		String fromClause = sqlMain.substring(fromIndex);
		
		// get alias of main table
		StringBuilder sqlBuilder = new StringBuilder(selectClause);
		StringBuilder sqlColumn = new StringBuilder();
		
		// add View_ID column to select clause
		for (IInfoColumn infoProcess : listInfoColumn) {
			// this process hasn't viewID column, next other infoProcess
			if (infoProcess.getInfoColumnID() <= 0)
				continue;

			MInfoColumn infocol = (MInfoColumn) infoProcess.getAD_InfoColumn();
			
			if (! infocol.isDisplayed()) {
				sqlColumn.append(", ").append(infocol.getSelectClause()).append(" AS ").append(infocol.getColumnName()).append(" ");
				// add column to SELECT clause of main sql, if query is include this viewID column, not need add
				if (!sqlBuilder.toString().contains(sqlColumn)){
					sqlBuilder.append(sqlColumn);
					GridField gridField = findGridField(infocol.getColumnName());
					if (gridField == null)
						gridField = toGridField(infocol);
					ColumnInfo columnInfo = toColumnInfo(infocol, gridField);
					columnInfos.add(columnInfo);
				}
				
				sqlColumn.delete(0, sqlColumn.length());
			}
		}
		
		sqlBuilder.append(fromClause);
		// update main sql 
		return sqlBuilder.toString();
		
	}
	
	private SQLFragment buildQuerySQL(int start, int end) {
		String dataSql;
		SQLFragment dynFilter = getSQLFilter();
		String dynWhere = dynFilter.sqlClause();
        StringBuilder sql = new StringBuilder (m_sqlMain);
        if (dynWhere.length() > 0)
            sql.append(dynWhere);   //  includes first AND
        
        if (sql.toString().trim().endsWith("WHERE")) {
        	int index = sql.lastIndexOf(" WHERE");
        	sql.delete(index, sql.length());
        }
        
        if (!Util.isEmpty(m_sqlOrder, true)) {
			sql.append(m_sqlOrder);
		}
        
        dataSql = Msg.parseTranslation(Env.getCtx(), sql.toString());    //  Variables
        String alias = tableName;
        if (tableInfos[0].getSynonym() != null && tableInfos[0].getSynonym().trim().length() > 0) {
        	alias = tableInfos[0].getSynonym().trim();
        }
        dataSql = MRole.getDefault().addAccessSQL(dataSql, alias,
            MRole.SQL_FULLYQUALIFIED, MRole.SQL_RO);
        if (end > start && DB.getDatabase().isPagingSupported())
        {
        	dataSql = DB.getDatabase().addPagingSQL(dataSql, start, end);
        }
                
        return new SQLFragment(dataSql, dynFilter.parameters());
	}
	
	private SQLFragment getSQLFilter() {
		StringBuilder builder = new StringBuilder();
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table.getColumnIndex("IsActive") >=0 ) {
			if (p_whereClause != null && p_whereClause.trim().length() > 0) {
				builder.append(" AND ");
			}
			builder.append(tableInfos[0].getSynonym()).append(".IsActive='Y'");
		}
		int count = 0;
		List<Object> statementParameters = new ArrayList<Object>();
		for(Entry<String, JsonElement> entries : queryParameters.entrySet()) {
			String name = entries.getKey();
			for(GridField gridField : gridFields) {
				if (gridField.getColumnName().equalsIgnoreCase(name)) {
					Object value = TypeConverterUtils.fromJsonValue(gridField, entries.getValue());
					MInfoColumn mInfoColumn = findInfoColumn(gridField);
					if (mInfoColumn == null || mInfoColumn.getSelectClause().equals("0")) {
						break;
					}
					String columnName = mInfoColumn.getSelectClause();
					int asIndex = columnName.toUpperCase().lastIndexOf(" AS ");
					if (asIndex > 0) {
						columnName = columnName.substring(0, asIndex);
					}
					
					count++;
					if (count == 1) {
						if (builder.length() > 0) {
							builder.append(" AND ");
							if (!useAnd) builder.append(" ( ");
						} else if (p_whereClause != null && p_whereClause.trim().length() > 0) {
							builder.append(" AND ");
							if (!useAnd) builder.append(" ( ");
						} else if (!useAnd) {
							builder.append(" ( ");
						}
					} else {
						builder.append(useAnd ? " AND " : " OR ");
					}
					
					if (mInfoColumn.getAD_Reference_ID() == DisplayType.ChosenMultipleSelectionList)
					{
						String pString = value.toString();
						String column = columnName;
						if (column.indexOf(".") > 0)
							column = column.substring(column.indexOf(".")+1);
						int cnt = DB.getSQLValueEx(null, "SELECT Count(*) From AD_Column WHERE IsActive='Y' AND AD_Client_ID=0 AND Upper(ColumnName)=? AND AD_Reference_ID=?", column.toUpperCase(), DisplayType.ChosenMultipleSelectionList);
						if (cnt > 0) {
							SQLFragment filter = DB.intersectFilterForCSV(columnName, pString);
							builder.append(filter.sqlClause());
							statementParameters.addAll(filter.parameters());
						} else {
							SQLFragment filter = DB.inFilterForCSV(columnName, pString);
							builder.append(filter.sqlClause());
							statementParameters.addAll(filter.parameters());
						}
					} 
					else if (mInfoColumn.getAD_Reference_ID() == DisplayType.ChosenMultipleSelectionTable || mInfoColumn.getAD_Reference_ID() == DisplayType.ChosenMultipleSelectionSearch)
					{
						String pString = value.toString();
						if (columnName.endsWith("_ID"))
						{						
							SQLFragment filter = DB.inFilterForCSV(columnName, pString);
							builder.append(filter.sqlClause());
							statementParameters.addAll(filter.parameters());
						}
						else
						{
							SQLFragment filter = DB.intersectFilterForCSV(columnName, pString);
							builder.append(filter.sqlClause());
							statementParameters.addAll(filter.parameters());
						}
					}
					else
					{
						String columnClause = null;
						if (mInfoColumn.getQueryFunction() != null && mInfoColumn.getQueryFunction().trim().length() > 0) {
							String function = mInfoColumn.getQueryFunction();
							if (function.indexOf("@") >= 0) {
								String s = Env.parseContext(Env.getCtx(), 0, function, true, false);
								if (s.length() == 0) {
									log.log(Level.SEVERE, "Failed to parse query function. " + function);
								} else {
									function = s;
								}
							}
							if (function.indexOf("?") >= 0) {
								columnClause = function.replaceFirst("[?]", columnName);
							} else {
								columnClause = function+"("+columnName+")";
							}
						} else {
							columnClause = columnName;
						}
						builder.append(columnClause)
							   .append(" ")
							   .append(mInfoColumn.getQueryOperator());
						if (columnClause.toUpperCase().startsWith("UPPER(")) {
							builder.append(" UPPER(?)");
						} else {
							builder.append(" ?");
						}
						statementParameters.add(toParameterValue(value, mInfoColumn.getQueryOperator()));
					}
					break;
				}
			}
		}	
		if (count > 0 && !useAnd) {
			builder.append(" ) ");
		}
		String sql = builder.toString();
		if (sql.indexOf("@") >= 0) {
			String preParse = sql;
			List<Object> parameters = new ArrayList<Object>();
			sql = Env.parseContext(Env.getCtx(), 0, sql, true, true);
			sql = Env.parseContextForSql(Env.getCtx(), 0, sql, true, true, parameters);
			if (parameters.size() > 0) {
				if (statementParameters.size() > 0) {
					statementParameters = Env.mergeParameters(preParse, sql, statementParameters.toArray(), parameters.toArray());
				} else {
					statementParameters = parameters;
				}
			}
		}
		
		return new SQLFragment(sql, statementParameters);
	}
	
	/**
	 * Validates the ORDER BY parameter against info columns and transforms column names to their SQL select clauses.
	 * <p>
	 * This method checks if p_orderBy contains valid column names that exist in the infoColumns array.
	 * It supports multiple formats:
	 * <ul>
	 * <li>Single column: "name"</li>
	 * <li>Single column with sort direction: "name desc"</li>
	 * <li>Multiple columns: "name,value"</li>
	 * <li>Multiple columns with sort directions: "name, value desc"</li>
	 * </ul>
	 * </p>
	 * <p>
	 * If validation succeeds, p_orderBy is modified to contain the SQL select clauses instead of column names.
	 * If any column name is invalid, p_orderBy is set to null and the method returns false.
	 * </p>
	 * 
	 * @return true if p_orderBy is valid and was successfully transformed, false if invalid or empty
	 */
	private boolean validateOrderByParameter() {
		if (!Util.isEmpty(p_orderBy, true)) {
			String orderByClause = p_orderBy.trim();
			
			// Split by comma to handle multiple columns (e.g., "name, value desc")
			String[] orderByParts = orderByClause.split(",");
			StringBuilder validatedOrderBy = new StringBuilder();
			
			for (String part : orderByParts) {
				part = part.trim();
				if (part.isEmpty()) {
					continue;
				}
				
				// Extract column name and sort direction (ASC/DESC)
				String columnName = part;
				String sortDirection = "";
				int spaceIndex = part.indexOf(' ');
				if (spaceIndex > 0) {
					columnName = part.substring(0, spaceIndex).trim();
					sortDirection = " " + part.substring(spaceIndex).trim();
				}

				// Check if the column name matches any info column
				boolean found = false;
				for (MInfoColumn infoColumn : infoColumns) {
					
					// Only consider displayed columns for ORDER BY - same as in UI
					if (!infoColumn.isDisplayed()) {
						continue;
					}
					
					if (infoColumn.getColumnName().equalsIgnoreCase(columnName)) {
						String selectClause = infoColumn.getSelectClause();
						// Remove any AS clause from select clause for ORDER BY
						int asIndex = selectClause.toUpperCase().lastIndexOf(" AS ");
						if (asIndex > 0) {
							selectClause = selectClause.substring(0, asIndex).trim();
						}
						
						// Append to the validated ORDER BY clause
						if (validatedOrderBy.length() > 0) {
							validatedOrderBy.append(", ");
						}
						validatedOrderBy.append(selectClause).append(sortDirection);
						found = true;
						break;
					}
				}
				
				// If any column is invalid, return false
				if (!found) {
					p_orderBy = null;
					log.log(Level.WARNING, "Invalid order by clause.");
					return false;
				}
			}
			
			// Set the validated ORDER BY clause
			p_orderBy = validatedOrderBy.length() > 0 ? validatedOrderBy.toString() : null;
			return p_orderBy != null;
		}
		
		p_orderBy = null;
		return false;
	}

	private MInfoColumn findInfoColumn(GridField gridField) {
		for(int i = 0; i < gridFields.size(); i++) {
			if (gridFields.get(i) == gridField) {
				return infoColumns[i];
			}
		}
		return null;
	}
	
	private GridField findGridField(String columnName) {
		for(int i = 0; i < gridFields.size(); i++) {
			if (gridFields.get(i).getColumnName().equalsIgnoreCase(columnName)) {
				return gridFields.get(i);
			}
		}
		return null;
	}

	/**
	 * 
	 * @param paraMap
	 */
	public void setQueryParameters(Map<String, JsonElement> paraMap) {
		queryParameters = paraMap;
	}

	/**
	 * 
	 * @param pageSize
	 * @param pageNo page number to retrieve (one base index)
	 * @param defaultQueryTimeout
	 * @return JsonArray
	 */
	public QueryResponse executeQuery(int pageSize, int pageNo, int defaultQueryTimeout) {
		QueryResponse response = new QueryResponse();
		if (pageNo <= 0)
			pageNo = 1;
		int pagesToSkip = pageNo - 1;
		int start = (pageSize*pagesToSkip) + 1;
		int end = (pageSize * (pagesToSkip+1)) + 1;
		SQLFragment sql = buildQuerySQL(start, end);
		JsonArray array = new JsonArray();
		try (PreparedStatement pstmt = DB.prepareStatement(sql.sqlClause(), null)) {
			for(int i = 0; i < sql.parameters().size(); i++) {
				Object value = sql.parameters().get(i);
				setParameter(pstmt, i+1, value);
			}
			pstmt.setQueryTimeout(defaultQueryTimeout);			
			ResultSet rs = pstmt.executeQuery();
			int count = 0;
			while (rs.next()) {
				count++;
				if (count > pageSize) {
					response.setHasNextPage(true);
					break;
				}
				JsonObject json = new JsonObject();
				for(ColumnInfo columnInfo : columnInfos) {
					MColumn column = null;
					GridField field = columnInfo.getGridField();					
					if (field == null) {
						MTable table = MTable.get(Env.getCtx(), tableName);
						if (table == null)
							continue;
						column = table.getColumn(columnInfo.getColumnName());
						if (column == null)
							continue;
					}
					Object value = rs.getObject(columnInfo.getColumnName());
					if (value != null) {
						String propertyName = columnInfo.getColumnName();
						Object jsonValue = field != null ? TypeConverterUtils.toJsonValue(field, value) : TypeConverterUtils.toJsonValue(column, value); 
						if (jsonValue != null) {
							if (jsonValue instanceof Number)
								json.addProperty(propertyName, (Number)jsonValue);
							else if (jsonValue instanceof Boolean)
								json.addProperty(propertyName, (Boolean)jsonValue);
							else if (jsonValue instanceof String)
								json.addProperty(propertyName, (String)jsonValue);
							else if (jsonValue instanceof JsonElement)
								json.add(propertyName, (JsonElement) jsonValue);
							else
								json.addProperty(propertyName, jsonValue.toString());
						}
					}
				}
				array.add(json);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		response.setRecords(array);
		return response;
	}
	
	/**
	 * set parameter for statement. 
	 * not need check null for value
	 * @param pstmt
	 * @param parameterIndex
	 * @param value
	 * @throws SQLException
	 */
	private void setParameter (PreparedStatement pstmt, int parameterIndex, Object value) throws SQLException {
		if (value instanceof Boolean) {					
			pstmt.setString(parameterIndex, ((Boolean) value).booleanValue() ? "Y" : "N");
		} else if (value instanceof String valueStr) {
			pstmt.setString(parameterIndex, valueStr);
		} else {
			pstmt.setObject(parameterIndex, value);
		}
	}
	
	private Object toParameterValue(Object value, String queryOperator) {
		if (value instanceof String) {
			StringBuilder valueStr = new StringBuilder(value.toString());
			if (queryOperator.equals(X_AD_InfoColumn.QUERYOPERATOR_Like)) {
				if (!valueStr.toString().endsWith("%"))
					valueStr.append("%");
			} else if (queryOperator.equals(X_AD_InfoColumn.QUERYOPERATOR_FullLike)) {
				if (!valueStr.toString().startsWith("%"))
					valueStr.insert(0, "%");
				if (!valueStr.toString().endsWith("%"))
					valueStr.append("%");
			}
			return valueStr.toString();
		}
		return value;
	}
}

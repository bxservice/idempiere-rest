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
package com.trekglobal.idempiere.rest.api.model;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MColumn;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOCache;
import org.idempiere.cache.ImmutablePOSupport;

import com.trekglobal.idempiere.rest.api.json.TypeConverterUtils;

public class MRestView extends X_REST_View implements ImmutablePOSupport {

	private static final long serialVersionUID = 7362614368992553892L;

	private final static ImmutablePOCache<String, MRestView> s_cache = new ImmutablePOCache<String, MRestView>(MRestView.Table_Name, 20);
	private final static ImmutableIntPOCache<Integer, MRestView> s_idCache = new ImmutableIntPOCache<Integer, MRestView>(MRestView.Table_Name, 20);
	
	private MRestViewColumn[] columns = null;
	private MRestViewRelated[] relateds = null;
	
	public MRestView(Properties ctx, int REST_View_ID, String trxName) {
		super(ctx, REST_View_ID, trxName);
	}

	public MRestView(Properties ctx, int REST_View_ID, String trxName, String... virtualColumns) {
		super(ctx, REST_View_ID, trxName, virtualColumns);
	}

	public MRestView(Properties ctx, String REST_View_UU, String trxName) {
		super(ctx, REST_View_UU, trxName);
	}

	public MRestView(Properties ctx, String REST_View_UU, String trxName, String... virtualColumns) {
		super(ctx, REST_View_UU, trxName, virtualColumns);
	}

	public MRestView(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	public MRestView(MRestView copy) {
		this(Env.getCtx(), copy);
	}
	
	public MRestView(Properties ctx, MRestView copy) {
		this(ctx, copy, (String) null);
	}
	
	public MRestView(Properties ctx, MRestView copy, String trxName) {
		//-1 to avoid infinite loop
		this(ctx, -1, trxName);
		copyPO(copy);
	}
	
	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (newRecord && success) {
			copyColumns();
		} else if (success) {
			if (is_ValueChanged(COLUMNNAME_AD_Table_ID)) {
				Query query = new Query(Env.getCtx(), MRestViewColumn.Table_Name, "REST_View_ID=?", null);
				List<MRestViewColumn> columnList = query.setParameters(getREST_View_ID()).list();
				for(MRestViewColumn column : columnList) {
					column.deleteEx(true, get_TrxName());
				}
				columns = null;
				copyColumns();				
				query = new Query(Env.getCtx(), MRestViewRelated.Table_Name, "REST_View_ID=?", null);
				List<MRestViewRelated> relatedList = query.setParameters(getREST_View_ID()).list();
				for(MRestViewRelated related : relatedList) {
					related.deleteEx(true, get_TrxName());
				}
				relateds = null;
			}
		}
		return success;
	}

	/**
	 * Copy all columns from table
	 */
	private void copyColumns() {
		//auto populate view columns
		MTable table = MTable.get(getAD_Table_ID());
		MColumn[] columns = table.getColumns(false);
		String keyColumn = table.getKeyColumns() != null && table.getKeyColumns().length == 1 ? table.getKeyColumns()[0] : "";
		String uidColumn = PO.getUUIDColumnName(table.getTableName());
		for(MColumn column : columns) {
			if (column.getColumnName().equals(keyColumn))
				continue;
			else if (column.getColumnName().equals(uidColumn))
				continue;
			MRestViewColumn restViewColumn = new MRestViewColumn(Env.getCtx(), 0, get_TrxName());
			restViewColumn.setREST_View_ID(getREST_View_ID());
			restViewColumn.setAD_Column_ID(column.getAD_Column_ID());
			restViewColumn.setName(TypeConverterUtils.toPropertyName(column.getColumnName()));
			restViewColumn.saveEx();
		}
	}

	/**
	 * Get view by name
	 * @param name
	 * @return MRestView
	 */
	public static MRestView get(String name) {
		MRestView view = s_cache.get(Env.getCtx(), name, e -> new MRestView(Env.getCtx(), e));
		if (view != null)
			return view;
		
		Query query = new Query(Env.getCtx(), Table_Name, "Name=?", null);
		view = query.setParameters(name).first();
		if (view != null) {
			s_cache.put (name, view, e -> new MRestView(Env.getCtx(), e));
			if (!s_idCache.containsKey(view.get_ID()))
				s_idCache.put(view.get_ID(), view, e -> new MRestView(Env.getCtx(), e));
		}
		return view;
	}
	
	/**
	 * Get view by id
	 * @param name
	 * @return MRestView
	 */
	public static MRestView get(int id) {
		MRestView view = s_idCache.get(Env.getCtx(), id, e -> new MRestView(Env.getCtx(), e));
		if (view != null)
			return view;
		
		Query query = new Query(Env.getCtx(), Table_Name, "REST_View_ID=?", null);
		view = query.setParameters(id).first();
		if (view != null) {
			s_idCache.put (id, view, e -> new MRestView(Env.getCtx(), e));
			if (!s_cache.containsKey(view.getName()))
				s_cache.put(view.getName(), view, e -> new MRestView(Env.getCtx(), e));
		}
		return view;
	}
	
	/**
	 * Get view columns
	 * @return view columns
	 */
	public MRestViewColumn[] getColumns() {
		return getColumns(false);
	}
	
	/**
	 * Get view columns
	 * @param refresh
	 * @return view columns
	 */
	public MRestViewColumn[] getColumns(boolean refresh) {
		if (columns != null && !refresh)
			return columns;
		
		Query query = new Query(Env.getCtx(), MRestViewColumn.Table_Name, "REST_View_ID=?", null);
		List<MRestViewColumn> columnList = query.setOnlyActiveRecords(true)
			.setOrderBy("SeqNo, REST_ViewColumn_ID")
			.setParameters(getREST_View_ID()).list();
		columns = columnList.toArray(new MRestViewColumn[0]);
		return columns;
	}
	
	/**
	 * Get related detail views
	 * @return related views
	 */
	public MRestViewRelated[] getRelatedViews() {
		return getRelatedViews(false);
	}
	
	/**
	 * Get related detail views
	 * @param refresh
	 * @return related views
	 */
	public MRestViewRelated[] getRelatedViews(boolean refresh) {
		if (relateds != null && !refresh)
			return relateds;
		
		Query query = new Query(Env.getCtx(), MRestViewRelated.Table_Name, "REST_View_ID=?", null);
		List<MRestViewRelated> relatedList = query.setOnlyActiveRecords(true)
			.setParameters(getREST_View_ID()).list();
		relateds = relatedList.toArray(new MRestViewRelated[0]);
		return relateds;
	}

	/**
	 * Is role has access to this view
	 * @param role
	 * @param isReadWrite
	 * @return true if has access
	 */
	public boolean hasAccess(MRole role, boolean isReadWrite) {
		Query query = new Query(Env.getCtx(), MRestViewAccess.Table_Name, "REST_View_ID=? AND AD_Role_ID=?"+(isReadWrite ? " AND IsReadonly='N'" : ""), null);		
		return query.setOnlyActiveRecords(true).setParameters(getREST_View_ID(), role.getAD_Role_ID()).count() == 1;
	}

	/**
	 * Convert view property name to table column name
	 * @param name
	 * @return table column name or null
	 */
	public String toColumnName(String name) {
		MRestViewColumn[] columns = getColumns();
		for(MRestViewColumn column : columns) {
			String columnName = MColumn.getColumnName(Env.getCtx(), column.getAD_Column_ID());
			if (column.getName().equals(name) || columnName.equalsIgnoreCase(name)) {
				return name;
			}
		}
		return null;
	}
	
	/**
	 * Convert list of view property name to table column names
	 * @param includes
	 * @param keepNotViewPropertyName true to keep input name in the return list when there's no matching view property name
	 * @return list of names
	 */
	public String[] toColumnNames(String[] includes, boolean keepNotViewPropertyName) {
		List<String> list = new ArrayList<String>();
		for(String include : includes) {
			String columnName = toColumnName(include);
			if (columnName != null)
				list.add(columnName);
			else if (keepNotViewPropertyName)
				list.add(include);
		}
		return list.toArray(new String[0]);
	}

	@Override
	public PO markImmutable() {
		if (is_Immutable())
			return this;
		
		makeImmutable();
		if (columns != null) {
			Arrays.stream(columns).forEach(e -> e.markImmutable());
		}
		if (relateds != null) {
			Arrays.stream(relateds).forEach(e -> e.markImmutable());
		}
		return this;
	}
}

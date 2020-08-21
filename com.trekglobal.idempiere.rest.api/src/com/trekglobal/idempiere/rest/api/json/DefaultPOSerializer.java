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
package com.trekglobal.idempiere.rest.api.json;

import java.util.Set;

import org.compiere.model.GridField;
import org.compiere.model.GridFieldVO;
import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.osgi.service.component.annotations.Component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 
 * Default serializer implementation for PO
 * @author hengsin
 *
 */
@Component(name = "com.trekglobal.idempiere.rest.api.json.DefaultPOSerializer", service = IPOSerializerFactory.class, 
	property = {"service.ranking:Integer=0"}, immediate = true)
public class DefaultPOSerializer implements IPOSerializer, IPOSerializerFactory {

	/**
	 * default constructor
	 */
	public DefaultPOSerializer() {
	}

	@Override
	public JsonObject toJson(PO po, String[] includes, String[] excludes) {
		JsonObject json = new JsonObject();
		String[] keyColumns = po.get_KeyColumns();
		String keyColumn = null;
		if (keyColumns != null && keyColumns.length == 1) {
			json.addProperty("id", po.get_ID());
			keyColumn = keyColumns[0];
		}
		String uidColumn = po.getUUIDColumnName();
		if (po.get_ColumnIndex(uidColumn) >= 0) {
			String uid = po.get_ValueAsString(uidColumn);
			if (!Util.isEmpty(uid, true)) {
				json.addProperty("uid", uid);
			}
		}		
		POInfo poInfo = POInfo.getPOInfo(Env.getCtx(), po.get_Table_ID());
		for(int i=0;i < poInfo.getColumnCount(); i++) {
			String columnName = poInfo.getColumnName(i);
			if (keyColumn != null && keyColumn.equalsIgnoreCase(columnName))
				continue;
			if (uidColumn != null && uidColumn.equalsIgnoreCase(columnName))
				continue;
			if (!include(columnName, includes))
				continue;
			if (exclude(columnName, excludes))
				continue;
			MColumn column = MColumn.get(Env.getCtx(), poInfo.getAD_Column_ID(columnName));
			if (column.isSecure() || column.isEncrypted())
				continue;
			
			Object value = po.get_Value(i);
			if (value != null) {
				String propertyName = TypeConverterUtils.toPropertyName(columnName);
				Object jsonValue = TypeConverterUtils.toJsonValue(column, value);
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
		if (!exclude("model-name", excludes))
			json.addProperty("model-name", poInfo.getTableName().toLowerCase());
		return json;
	}

	@Override
	public PO fromJson(JsonObject json, MTable table) {
		PO po = table.getPO(0, null);
		POInfo poInfo = POInfo.getPOInfo(Env.getCtx(), table.getAD_Table_ID());
		Set<String> jsonFields = json.keySet();
		for(int i = 0; i < poInfo.getColumnCount(); i++) {
			String columnName = poInfo.getColumnName(i);
			MColumn column = table.getColumn(columnName);
			String propertyName = TypeConverterUtils.toPropertyName(columnName);
			if (!jsonFields.contains(propertyName)) {
				setDefaultValue(po, column);
				continue;
			}
			if (column.isSecure() || column.isEncrypted() || column.isVirtualColumn())
				continue;
			
			JsonElement field = json.get(propertyName);
			if (field == null) {
				setDefaultValue(po, column);
				continue;
			}
			Object value = TypeConverterUtils.fromJsonValue(column, field);
			if (value != null)
				po.set_ValueOfColumn(column.getAD_Column_ID(), value);
		}
		
		return po;
	}

	@Override
	public PO fromJson(JsonObject json, PO po) {
		MTable table = MTable.get(Env.getCtx(), po.get_Table_ID());
		POInfo poInfo = POInfo.getPOInfo(Env.getCtx(), table.getAD_Table_ID());
		Set<String> jsonFields = json.keySet();
		for(int i = 0; i < poInfo.getColumnCount(); i++) {
			String columnName = poInfo.getColumnName(i);
			MColumn column = table.getColumn(columnName);
			String propertyName = TypeConverterUtils.toPropertyName(columnName);
			if (!jsonFields.contains(propertyName))
				continue;
			JsonElement field = json.get(propertyName);
			if (field == null)
				continue;
			if (!column.isUpdateable())
				continue;
			if (column.isSecure() || column.isEncrypted())
				continue;
			if (po.get_ColumnIndex("processed") >= 0) {
				if (po.get_ValueAsBoolean("processed")) {
					if (!column.isAlwaysUpdateable())
						continue;
				}
			}
			if (po.get_ColumnIndex("posted") >= 0) {
				if (po.get_ValueAsBoolean("posted")) {
					if (!column.isAlwaysUpdateable())
						continue;
				}
			}
			Object value = TypeConverterUtils.fromJsonValue(column, field);
			if (value != null)
				po.set_ValueOfColumn(column.getAD_Column_ID(), value);
		}
		
		return po;
	}
	
	private boolean exclude(String columnName, String[] excludes) {
		if (excludes == null || excludes.length == 0)
			return false;
		for(String exclude : excludes) {
			if (exclude.equals(columnName))
				return true;
		}
		return false;
	}

	private boolean include(String columnName, String[] includes) {
		if (includes == null || includes.length == 0)
			return true;
		for(String include : includes) {
			if (include.equalsIgnoreCase(columnName))
				return true;
		}
		return false;
	}
	
	private void setDefaultValue(PO po, MColumn column) {
		if (!Util.isEmpty(column.getDefaultValue(), true)) {
			GridFieldVO vo = GridFieldVO.createParameter(Env.getCtx(), 0, 0, 0, column.getAD_Column_ID(), column.getColumnName(), column.getName(), 
						DisplayType.isLookup(column.getAD_Reference_ID()) 
						? (DisplayType.isText(column.getAD_Reference_ID()) || DisplayType.List==column.getAD_Reference_ID() ? DisplayType.String : DisplayType.ID) 
						: column.getAD_Reference_ID(), 0, false, false, "");
			GridField gridField = new GridField(vo);
			Object defaultValue = gridField.getDefault();
			if (defaultValue != null) {
				po.set_ValueOfColumn(column.getAD_Column_ID(), defaultValue);
			}
		}		
	}

	@Override
	public IPOSerializer getPOSerializer(String tableName, Class<?> modelClass) {
		if ("*".equals(tableName)) {
			return this;
		}
		return null;
	}
}

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
import java.util.UUID;

import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MColumn;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.osgi.service.component.annotations.Component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 
 * Default serializer implementation for Grid Tab
 * @author hengsin
 *
 */
@Component(name = "com.trekglobal.idempiere.rest.api.json.DefaultGridTabSerializer", service = IGridTabSerializerFactory.class, 
	property = {"service.ranking:Integer=0"}, immediate = true)
public class DefaultGridTabSerializer implements IGridTabSerializer, IGridTabSerializerFactory {

	/**
	 * default constructor
	 */
	public DefaultGridTabSerializer() {
	}

	@Override
	public JsonObject toJson(GridTab gridTab, String[] includes, String[] excludes) {
		JsonObject json = new JsonObject();
		String keyColumn = gridTab.getKeyColumnName();
		if (!Util.isEmpty(keyColumn, true)) {
			Object value = gridTab.getValue(keyColumn);
			if (value != null) {
				if (value instanceof Number) {
					json.addProperty("id", ((Number)value).intValue());
				} else {
					json.addProperty("id", value.toString());
				}
			}
		}
		UUID uid = gridTab.getTableModel().getUUID(gridTab.getCurrentRow());
		if (uid != null) {
			json.addProperty("uid", uid.toString());
		}
		GridField[] fields = gridTab.getFields();
		for(int i = 0; i < fields.length; i++) {
			if (i == gridTab.getKeyColumnIndex())
				continue;
			GridField gridField = fields[i];
			if (gridField.isUUID())
				continue;
			if (!gridField.isDisplayed(true))
				continue;
			
			Object value = gridField.getValue();
			if (value == null)
				continue;
			
			
			String columnName = gridField.getColumnName();
			if (!include(columnName, includes))
				continue;
			if (exclude(columnName, excludes))
				continue;
			
			MColumn column = MColumn.get(Env.getCtx(), gridField.getAD_Column_ID());
			if (gridField.isEncrypted() || column.isSecure())
				continue;
			
			String propertyName = column.getColumnName();
			Object jsonValue = TypeConverterUtils.toJsonValue(gridField, value);
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
		
		return json;
	}
	
	@Override
	public void fromJson(JsonObject json, GridTab gridTab) {
		Set<String> jsonFields = json.keySet();
		for(int i = 0; i < gridTab.getFieldCount(); i++) {
			GridField gridField = gridTab.getField(i);
			String columnName = gridField.getColumnName();
			MColumn column = MColumn.get(Env.getCtx(), gridField.getAD_Column_ID());
			String propertyName = TypeConverterUtils.toPropertyName(columnName);
			if (!jsonFields.contains(propertyName) && !jsonFields.contains(columnName))
				continue;
			JsonElement jsonField = json.get(propertyName);
			if (jsonField == null)
				jsonField = json.get(columnName);
			if (jsonField == null) {
				if (gridTab.isNew()) {
					gridField.setValue(gridField.getDefault(), true);
				}
				continue;
			}
			if (jsonField.isJsonArray())
				continue;
			if (!gridField.isUpdateable() && !gridTab.isNew())
				continue;
			if (gridField.isVirtualColumn() || gridField.isEncrypted() || column.isSecure())
				continue;
			if (gridTab.getField("processed") != null) {
				if (gridTab.getValueAsBoolean("processed")) {
					if (!gridField.isAlwaysUpdateable())
						continue;
				}
			}
			if (gridTab.getField("posted") != null) {
				if (gridTab.getValueAsBoolean("posted")) {
					if (!gridField.isAlwaysUpdateable())
						continue;
				}
			}
			
			Object value = TypeConverterUtils.fromJsonValue(gridField, jsonField);
			Object oldValue = gridField.getValue();
			if (value == null && oldValue == null)
				continue;
			else if (value != null && value.equals(oldValue))
				continue;
			
			gridTab.setValue(gridField, value);
			gridTab.processFieldChange(gridField);
		}
		
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
			if (include.equals(columnName))
				return true;
		}
		return false;
	}

	@Override
	public IGridTabSerializer getGridTabSerializer(String gridTabUID) {
		if ("*".equals(gridTabUID)) {
			return this;
		} else {
			return null;
		}
	}
}

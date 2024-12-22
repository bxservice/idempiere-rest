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
import org.compiere.model.Lookup;
import org.compiere.model.MColumn;
import org.compiere.model.MLocation;
import org.compiere.model.MLocationLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.model.MValRule;
import org.compiere.model.POInfo;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.trekglobal.idempiere.rest.api.model.MRestView;
import com.trekglobal.idempiere.rest.api.model.MRestViewColumn;

/**
 * json type converter for C_Location
 * @author muriloht (devCoffee, muriloht@devcoffee.com.br)
 *
 */
public class LocationTypeConverter implements ITypeConverter<Object> {

	/**
	 * default constructor
	 */
	public LocationTypeConverter() {
		
	}

	@Override
	public Object toJsonValue(MColumn column, Object value) {
		return toJsonValue(column, value, null);
	}
	
	@Override
	public Object toJsonValue(MColumn column, Object value, MRestView referenceView) {
		String label = Msg.getElement(Env.getCtx(), column.getColumnName());
		Lookup lookup = new MLocationLookup(Env.getCtx(), 0);
		return toJsonValue(column.getAD_Reference_ID(), label, lookup, column.getReferenceTableName(), value, referenceView);
	}

	@Override
	public Object toJsonValue(GridField field, Object value) {
		return toJsonValue(field.getDisplayType(), field.getHeader(), field.getLookup(), getReferenceTableNameFromField(field), value, null);
	}
	
	private String getReferenceTableNameFromField(GridField field) {
		String refTableName = null;
		if (field.getVO().isProcess) {
			MProcessPara pp = field.getAD_Column_ID() > 0 ? MProcessPara.get(field.getAD_Column_ID()) : null;
			if (pp != null)
				refTableName = pp.getReferenceTableName();
		} else {
			MColumn column = field.getAD_Column_ID() > 0 ? MColumn.get(Env.getCtx(), field.getAD_Column_ID()) : null;
			if (column != null)
				refTableName = column.getReferenceTableName();
		}
		return refTableName;
	}
	
	private Object toJsonValue(int displayType, String label, Lookup lookup, String refTableName, Object value, MRestView referenceView) {
		if (lookup != null) {
			MLocation loc = MLocation.get((Integer)value);
			JsonObject ref = new JsonObject();
			if (referenceView == null)
				ref.addProperty("propertyLabel", label);
			if (value instanceof Number)
				ref.addProperty("id", ((Number)value).intValue());
			else
				ref.addProperty("id", value.toString());
			String display = lookup.getDisplay(value);
			if (!Util.isEmpty(display, true)) {
				ref.addProperty("identifier", display);
			}							
			if (!Util.isEmpty(refTableName) && referenceView == null)
				ref.addProperty("model-name", refTableName.toLowerCase());
			
			MColumn[] columns = MTable.get(MLocation.Table_ID).getColumns(false);
			String columnName = null;
			Object columnValue = null;
			
			MRestViewColumn[] viewColumns = referenceView != null ? referenceView.getColumns() : null;
			int count = viewColumns != null ? viewColumns.length : columns.length;
			for(int i = 0; i < count; i++) {
				MColumn column = viewColumns != null ? MColumn.get(viewColumns[i].getAD_Column_ID()) : columns[i];
				if(column.isKey())continue;
				
				columnName = column.getColumnName();
				
				if (columnName.endsWith("_ID")) {
					if((columnValue = loc.get_Value(columnName))!=null) {
						JsonObject refChild = new JsonObject();
						if (viewColumns == null)
							refChild.addProperty("propertyLabel",Msg.getElement(Env.getCtx(), columnName));
						if (value instanceof Number)
							refChild.addProperty("id", (Integer)columnValue);
						else
							refChild.addProperty("id", value.toString());
						String displayValue = getColumnLookup(column).getDisplay(columnValue);
						if(displayValue!=null)
							refChild.addProperty("identifier",displayValue);
						if (viewColumns != null && viewColumns[i].getREST_ReferenceView_ID() > 0)
							refChild.addProperty("view-name", MRestView.get(viewColumns[i].getREST_ReferenceView_ID()).getName());
						else
							refChild.addProperty("model-name", MTable.get(Env.getCtx(), columnName.replace("_ID", "")).getTableName().toLowerCase());
						ref.add(viewColumns != null ? viewColumns[i].getName() : columnName, refChild);
					}
				} else {
					if((columnValue = loc.get_Value(columnName))!=null) {
						ref.addProperty(viewColumns != null ? viewColumns[i].getName() : columnName, columnValue.toString());
					}
				}
			}
			return ref;
		} else {
			return null;
		}
	}
		
	@Override
	public Object fromJsonValue(GridField field, JsonElement value) {
		return fromJson(value);
	}

	@Override
	public Object fromJsonValue(MColumn column, JsonElement value) {
		return fromJsonValue(column, value, null);
	}
	
	@Override
	public Object fromJsonValue(MColumn column, JsonElement value, MRestView referenceView) {
		return fromJson(value, referenceView);
	}
	
	public Object fromJson(JsonElement element) {
		return fromJson(element, null);
	}
	
	public Object fromJson(JsonElement element, MRestView referenceView) {
		if (element != null && element.isJsonObject()) {
		
			JsonObject json = element.getAsJsonObject();
			JsonElement idField = json.get("id");
			int C_Location_ID = 0;
			if (idField != null) {
				JsonPrimitive primitive = (JsonPrimitive) idField;
				if (primitive.isNumber())
					C_Location_ID = primitive.getAsInt();
				else
					C_Location_ID = 0;
			}

			MLocation po = new MLocation(Env.getCtx(), C_Location_ID, null);

			MTable table = MTable.get(Env.getCtx(), MLocation.Table_ID);
			POInfo poInfo = POInfo.getPOInfo(Env.getCtx(), table.getAD_Table_ID());
			DefaultPOSerializer.validateJsonFields(json, po);
			Set<String> jsonFields = json.keySet();
			MRestViewColumn[] viewColumns = referenceView != null ? referenceView.getColumns() : null;
			int count = viewColumns != null ? viewColumns.length : poInfo.getColumnCount(); 
			for(int i = 0; i < count; i++) {
				MRestViewColumn viewColumn = viewColumns != null ? viewColumns[i] : null;
				String columnName = viewColumn != null ? MColumn.get(viewColumn.getAD_Column_ID()).getColumnName() : poInfo.getColumnName(i);
				String propertyName = viewColumn != null ? viewColumn.getName() : TypeConverterUtils.toPropertyName(columnName);
				if (!jsonFields.contains(propertyName) && !jsonFields.contains(columnName))
					continue;
				JsonElement field = json.get(propertyName);
				if (field == null)
					field = json.get(columnName);
				if (field == null)
					continue;
				MColumn column = table.getColumn(columnName);
				Object value = TypeConverterUtils.fromJsonValue(column, field, viewColumn != null && viewColumn.getREST_ReferenceView_ID() > 0
						? MRestView.get(viewColumn.getREST_ReferenceView_ID()) : null);
				if (! DefaultPOSerializer.isValueUpdated(po.get_ValueOfColumn(column.getAD_Column_ID()), value))
					continue;
				if (! DefaultPOSerializer.isUpdatable(column, false, po))
					continue;
				if (   value != null
					&& value instanceof Integer) {
					if (((Integer)value).intValue() < 0 && DisplayType.isID(column.getAD_Reference_ID())) {
						element = null;
					} else if (((Integer)value).intValue() == 0 && DisplayType.isLookup(column.getAD_Reference_ID())) {
						if (! MTable.isZeroIDTable(column.getReferenceTableName()))
							element = null;
					}
				}
				po.set_ValueOfColumn(column.getAD_Column_ID(), value);
			}
			po.saveEx();
			return po.get_ID();
		}

		if (element instanceof JsonPrimitive)
		{
			JsonPrimitive primitive = (JsonPrimitive) element;

			if (primitive.isNumber())
				return primitive.getAsInt();
		}
		
		return null;
	}
	
	private Lookup getColumnLookup(MColumn column) {
		int WindowNo = 0;
		Lookup lookup = null;
		try {
			String validationCode = null;
			if (column.getAD_Val_Rule_ID() > 0) {
				MValRule rule = MValRule.get(Env.getCtx(), column.getAD_Val_Rule_ID());
				validationCode = rule.getCode();
			}

			lookup = MLookupFactory.get(Env.getCtx(), WindowNo, column.getAD_Column_ID(), column.getAD_Reference_ID(),
					Env.getLanguage(Env.getCtx()), column.getColumnName(), column.getAD_Reference_Value_ID(),
					column.isParent(), validationCode);

		} catch (Exception e) {
			lookup = null; 
		}
		return lookup;
	}
	
}
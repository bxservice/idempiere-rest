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

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.GridField;
import org.compiere.model.Lookup;
import org.compiere.model.MAccountLookup;
import org.compiere.model.MColumn;
import org.compiere.model.MLocationLookup;
import org.compiere.model.MLocatorLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MPAttributeLookup;
import org.compiere.model.MPaymentLookup;
import org.compiere.model.MProcessPara;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.MValRule;
import org.compiere.model.PO;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * json type converter for AD lookup type
 * @author hengsin
 *
 */
public class LookupTypeConverter implements ITypeConverter<Object> {

	/**
	 * default constructor
	 */
	public LookupTypeConverter() {
	}

	@Override
	public Object toJsonValue(MColumn column, Object value) {
		String label = Msg.getElement(Env.getCtx(), column.getColumnName());
		return toJsonValue(column.getAD_Reference_ID(), label, getColumnLookup(column), column.getReferenceTableName(), value);
	}

	@Override
	public Object toJsonValue(GridField field, Object value) {
		return toJsonValue(field.getDisplayType(), field.getHeader(), field.getLookup(), getReferenceTableNameFromField(field), value);
	}

	@Override
	public Object fromJsonValue(MColumn column, JsonElement value) {
		return fromJsonValue(column.getAD_Reference_ID(), column.getReferenceTableName(), value);
	}

	@Override
	public Object fromJsonValue(GridField field, JsonElement value) {
		return fromJsonValue(field.getDisplayType(), getReferenceTableNameFromField(field), value);
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

	private Object toJsonValue(int displayType, String label, Lookup lookup, String refTableName, Object value) {
		if (lookup != null) {
			JsonObject ref = new JsonObject();
			ref.addProperty("propertyLabel", label);
			if (value instanceof Number)
				ref.addProperty("id", ((Number)value).intValue());
			else
				ref.addProperty("id", value.toString());
			String display = lookup.getDisplay(value);
			if (!Util.isEmpty(display, true)) {
				ref.addProperty("identifier", display);
			}							
			if (!Util.isEmpty(refTableName))
				ref.addProperty("model-name", refTableName.toLowerCase());
			return ref;
		} else {
			return null;
		}
	}
	
	/**
	 *  Get Lookup
	 *  @param column
	 *  @return Lookup
	 */
	private Lookup getColumnLookup (MColumn column)
	{
		int WindowNo = 0;
		//  List, Table, TableDir
		Lookup lookup = null;
		int displayType = column.getAD_Reference_ID();
		try
		{
			String validationCode = null;
			if (column.getAD_Val_Rule_ID() > 0)
			{
				MValRule rule = MValRule.get(Env.getCtx(), column.getAD_Val_Rule_ID());
				validationCode = rule.getCode();
			}
			if (displayType == DisplayType.Account) 
			{
				lookup = new MAccountLookup(Env.getCtx(), 0);
			}
			else if (displayType == DisplayType.Location)
			{
				lookup = new MLocationLookup(Env.getCtx(), 0);
			}
			else if (displayType == DisplayType.Locator)
			{
				lookup = new MLocatorLookup(Env.getCtx(), 0);
			} 
			else if (displayType == DisplayType.PAttribute)
			{
				lookup = new MPAttributeLookup(Env.getCtx(), 0);
			}
			else if (displayType == DisplayType.Payment)
			{
				lookup = new MPaymentLookup(Env.getCtx(), 0, validationCode);
			}
			else
			{
				lookup = MLookupFactory.get (Env.getCtx(), WindowNo,
					column.getAD_Column_ID(), column.getAD_Reference_ID(),
					Env.getLanguage(Env.getCtx()), column.getColumnName(),
					column.getAD_Reference_Value_ID(),
					column.isParent(), validationCode);
			}
		}
		catch (Exception e)
		{
			lookup = null;          //  cannot create Lookup
		}
		return lookup;
	}
	
	private Object fromJsonValue(int displayType, String refTableName, JsonElement value) {
		if (value != null && value.isJsonObject()) {
			JsonObject ref = value.getAsJsonObject();
			JsonElement idField = ref.get("id");
			if (idField != null) {
				JsonPrimitive primitive = (JsonPrimitive) idField;
				if (primitive.isNumber())
					return primitive.getAsInt();
				else
					return primitive.getAsString();
			}
			JsonElement identifier = ref.get("identifier");
			if (identifier != null && !Util.isEmpty(refTableName)) {
				int id = findId(refTableName, identifier);
				if (id >= 0)
					return id;
			}
			
			JsonElement uidField = ref.get("uid");
			if (uidField != null && !Util.isEmpty(refTableName)) {
				String uidColumn = PO.getUUIDColumnName(refTableName);
				String keyColumn = refTableName + "_ID";
				int id = DB.getSQLValue(null, "SELECT " + keyColumn + " FROM " + refTableName + " WHERE " + uidColumn + "=?", uidField.getAsString());
				if (id > 0)
					return id;
			}
			throw new AdempiereException("Could not convert value " + value + " for " + refTableName);
		} else if (value != null && value.isJsonPrimitive()) {
			JsonPrimitive primitive = (JsonPrimitive) value;
			if (primitive.isNumber())
				return primitive.getAsInt();
			else
				return primitive.getAsString();
		} else if(DisplayType.isText(displayType) || DisplayType.isList(displayType)) {
			return value != null && !(value instanceof JsonNull) ? value.getAsString() : null;
		} else if (value != null) {
			throw new AdempiereException("Could not convert value " + value + " for " + refTableName);
		} else {
			return null;
		}
	}

	private int findId(String tableName, JsonElement identifier) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		String[] identifiers = table.getIdentifierColumns();
		if (identifiers != null && identifiers.length > 0) {
			StringBuilder builder = new StringBuilder();
			builder.append("SELECT ")
			   .append(tableName)
			   .append("_ID FROM ")
			   .append(tableName)
			   .append(" WHERE ")
			   .append(identifiers[0])
			   .append("=?");
			String sql = MRole.getDefault().addAccessSQL(builder.toString(), tableName, true, false);
			return DB.getSQLValue(null, sql, identifier.getAsString());
		}
		return -1;
	}
}

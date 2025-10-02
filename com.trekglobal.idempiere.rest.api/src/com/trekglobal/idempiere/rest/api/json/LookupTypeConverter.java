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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.trekglobal.idempiere.rest.api.model.MRestView;
import com.trekglobal.idempiere.rest.api.util.ThreadLocalTrx;

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
		return toJsonValue(column, value, null);
	}
	
	@Override
	public Object toJsonValue(MColumn column, Object value, MRestView referenceView) {
		String label = Msg.getElement(Env.getCtx(), column.getColumnName());
		return toJsonValue(column.getAD_Reference_ID(), label, getColumnLookup(column), column.getReferenceTableName(), value, referenceView);
	}

	@Override
	public Object toJsonValue(GridField field, Object value) {
		return toJsonValue(field.getDisplayType(), field.getHeader(), field.getLookup(), getReferenceTableNameFromField(field), value, null);
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

	private Object toJsonValue(int displayType, String label, Lookup lookup, String refTableName, Object value, MRestView referenceView) {
		if (lookup != null && value != null) {
			JsonObject ref = new JsonObject();
			if (referenceView == null)
				ref.addProperty("propertyLabel", label);
			if (displayType == DisplayType.ChosenMultipleSelectionSearch || displayType == DisplayType.ChosenMultipleSelectionTable) {
				return toJsonValueForChosenMultipleSelectionTable(lookup, refTableName, value, referenceView, ref);
			}
			addRecordIdProperty(lookup, refTableName, value, referenceView, ref);
			return ref;
		} else {
			return null;
		}
	}

	private Object toJsonValueForChosenMultipleSelectionTable(Lookup lookup, String refTableName, Object value,
			MRestView referenceView, JsonObject ref) {
		JsonArray array = new JsonArray();
		String[] values = value.toString().split(",");
		if (values.length > 0) {
			for(String v : values) {
				JsonObject item = new JsonObject();
				addRecordIdProperty(lookup, refTableName, v, referenceView, item);
				array.add(item);
			}
		}
		ref.add("selections", array);
		return ref;
	}

	private void addRecordIdProperty(Lookup lookup, String refTableName, Object value, MRestView referenceView,
			JsonObject ref) {
		if (value instanceof Number)
			ref.addProperty("id", ((Number)value).intValue());
		else
			ref.addProperty("id", value.toString());
		String display = lookup.getDisplay(value);
		if (!Util.isEmpty(display, true)) {
			ref.addProperty("identifier", display);
		}							
		if (!Util.isEmpty(refTableName)) {
			if (referenceView != null)
				ref.addProperty("view-name", referenceView.getName());
			else
				ref.addProperty("model-name", refTableName.toLowerCase());
			if (RestUtils.isReturnUULookup(refTableName)) {
				String uidColumn = PO.getUUIDColumnName(refTableName);
				String keyColumn = RestUtils.getKeyColumnName(refTableName);
				String uuid = DB.getSQLValueString(ThreadLocalTrx.getTrxName(), "SELECT " + uidColumn + " FROM " + refTableName + " WHERE " + keyColumn + "=?", value);
				if (!Util.isEmpty(uuid))
					ref.addProperty("uuid", uuid);
			}
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
			if (displayType == DisplayType.ChosenMultipleSelectionSearch || displayType == DisplayType.ChosenMultipleSelectionTable) {
				return fromJsonValueForChosenMultipleSelectionTable(refTableName, value.getAsJsonObject());
			}
			JsonObject ref = value.getAsJsonObject();
			Object id = findRecordId(ref, refTableName);
			if (id != null)
				return id;
			else
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

	private Object fromJsonValueForChosenMultipleSelectionTable(String refTableName, JsonObject value) {
		JsonElement selections = value.get("selections");
		if (selections != null && selections.isJsonArray()) {
			JsonArray array = selections.getAsJsonArray();
			StringBuilder sb = new StringBuilder();
			for (JsonElement el : array) {
				if (el.isJsonObject()) {
					JsonObject ref = el.getAsJsonObject();
					Object id = findRecordId(ref, refTableName);
					if (id != null) {
						if (sb.length() > 0)
							sb.append(",");
						sb.append(id.toString());
					} else {
						throw new AdempiereException("Could not convert value " + value + " for " + refTableName);
					}
				} else if (el.isJsonPrimitive()) {
					JsonPrimitive primitive = (JsonPrimitive) el;
					if (primitive.isNumber()) {
						if (sb.length() > 0)
							sb.append(",");
						sb.append(primitive.getAsInt());
					} else {
						if (sb.length() > 0)
							sb.append(",");
						sb.append(primitive.getAsString());
					}
				} else {
					throw new AdempiereException("Could not convert value " + value + " for " + refTableName);
				}
			}
			return sb.toString();
		}
		throw new AdempiereException("Could not convert value " + value + " for " + refTableName);
	}

	private Object findRecordId(JsonObject ref, String refTableName) {
		JsonElement idField = ref.get("id");
		if (idField != null) {
			JsonPrimitive primitive = (JsonPrimitive) idField;
			if (primitive.isNumber())
				return primitive.getAsInt();
			else
				return primitive.getAsString();
		}
		JsonElement identifier = ref.get("identifier");
		if (identifier != null && !Util.isEmpty(refTableName) && !identifier.isJsonNull()) {
			int id = findId(refTableName, identifier);
			if (id >= 0)
				return id;
		}
		
		JsonElement uidField = ref.get("uid");
		if (uidField != null && !Util.isEmpty(refTableName) && !uidField.isJsonNull()) {
			String uidColumn = PO.getUUIDColumnName(refTableName);
			String keyColumn = RestUtils.getKeyColumnName(refTableName);
			int id = DB.getSQLValue(ThreadLocalTrx.getTrxName(), "SELECT " + keyColumn + " FROM " + refTableName + " WHERE " + uidColumn + "=?", uidField.getAsString());
			if (id > 0)
				return id;
		}
		JsonElement columnName = ref.get("lookupColumn");
		if (columnName != null) {
			uidField = ref;
			JsonElement searchValue = ref.get("lookupValue");

			int id = findIdbyColumn(refTableName, columnName.getAsString(), searchValue);
			if (id >= 0)
				return id;
		}
		return null;
	}

	/**
	 * Find ID using the first column defined as identifier
	 * @param tableName
	 * @param identifier
	 * @return
	 */
	private int findId(String tableName, JsonElement identifier) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		String[] identifiers = table.getIdentifierColumns();
		if (identifiers != null && identifiers.length > 0) {
			MColumn column = table.getColumn(identifiers[0]);
			return getFirstIdOnly(table, column, identifier);
		}
		return -1;
	}

	/**
	 * Find ID using columnName
	 * @param tableName
	 * @param columnName
	 * @param searchValue
	 * @return
	 */
	private int findIdbyColumn(String tableName, String columnName, JsonElement searchValue) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		MColumn column = table.getColumn(columnName);
		if (column == null)
			throw new AdempiereException("Column not found -> " + tableName + "." + columnName);
		return getFirstIdOnly(table, column, searchValue);
	}

	/**
	 * Get the first ID found, error if it finds more than one ID with same parameters
	 * @param table
	 * @param columnName
	 * @param identifier
	 * @return
	 */
	private int getFirstIdOnly(MTable table, MColumn column, JsonElement identifier) {
		int id = -1;
		String tableName = table.getTableName();
		StringBuilder builder = new StringBuilder()
		   .append("SELECT ").append(tableName).append("_ID FROM ").append(tableName)
		   .append(" WHERE ").append(column.getColumnName()).append("=?");
		String sql = MRole.getDefault().addAccessSQL(builder.toString(), tableName, true, false);

		try (PreparedStatement stmt = DB.prepareStatement(sql, null)) {
			Object param;
			if (DisplayType.isID(column.getAD_Reference_ID()))
				param = identifier.getAsInt();
			else if (DisplayType.isNumeric(column.getAD_Reference_ID()))
				param = identifier.getAsBigDecimal();
			else if (DisplayType.isDate(column.getAD_Reference_ID())) {
				Date date;
				SimpleDateFormat dateTimeFormat = DisplayType.getTimestampFormat_Default();
				SimpleDateFormat dateFormat = DisplayType.getDateFormat_JDBC();
				try {
					if (column.getAD_Reference_ID() == DisplayType.Date) {
						date = dateFormat.parse(identifier.getAsString());
					} else {
						date = dateTimeFormat.parse(identifier.getAsString());
					}
				} catch (ParseException e) {
					try {
						date = dateFormat.parse(identifier.getAsString());
					} catch (ParseException e1) {
						throw new AdempiereException("Date wrongly formatted -> " + identifier.getAsString());
					}
				}
				param = new Timestamp(date.getTime());
			} else
				param = identifier.getAsString();
			stmt.setObject(1, param);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				id = rs.getInt(1);
				if (rs.next()) {
					throw new AdempiereException("More than one ID found for " + tableName + "." + column.getColumnName() + " = " + identifier.getAsString());
				}
			}
		} catch (SQLException ex) {
			throw new AdempiereException("Error getting the first ID -> " + sql, ex);
		}

		return id;
	}

}

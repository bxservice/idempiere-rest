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

import static org.compiere.util.DisplayType.Account;
import static org.compiere.util.DisplayType.Binary;
import static org.compiere.util.DisplayType.Location;
import static org.compiere.util.DisplayType.Locator;
import static org.compiere.util.DisplayType.PAttribute;
import static org.compiere.util.DisplayType.Payment;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response.Status;

import org.adempiere.base.Service;
import org.adempiere.base.ServiceQuery;
import org.compiere.model.GridField;
import org.compiere.model.MColumn;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

/**
 * @author hengsin
 *
 */
public class TypeConverterUtils {

	private static final Pattern NONLATIN = Pattern.compile("[^\\w_-]");  
	private static final Pattern SEPARATORS = Pattern.compile("[\\s\\p{Punct}&&[^-]&&[^_]]");
	
	/**
	 * private constructor
	 */
	private TypeConverterUtils() {
	}

	/**
	 * Convert table's column name to json property name
	 * @param columnName
	 * @return propertyName
	 */
	public static String toPropertyName(String columnName) {
		String propertyName = columnName;
		if (!propertyName.contains("_")) {
			String initial = propertyName.substring(0, 1).toLowerCase();
			propertyName = initial + propertyName.substring(1);
		}
		return propertyName;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	/**
	 * Convert db column value to json value
	 * @param column
	 * @param value
	 * @return Object
	 */
	public static Object toJsonValue(MColumn column, Object value) {		
		ITypeConverter typeConverter = getTypeConverter(column.getAD_Reference_ID(), value);
		
		if (typeConverter != null) {
			return typeConverter.toJsonValue(column, value);
		} else if (value != null && DisplayType.isText(column.getAD_Reference_ID())) {
			return value.toString();
		} else if (value != null && column.getAD_Reference_ID() == DisplayType.ID && value instanceof Number) {
			return ((Number)value).intValue();
		} else {
			return null;
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	/**
	 * Convert db column value to json value
	 * @param field
	 * @param value
	 * @return Object
	 */
	public static Object toJsonValue(GridField field, Object value) {		
		ITypeConverter typeConverter = getTypeConverter(field.getDisplayType(), value);
		
		if (typeConverter != null) {
			return typeConverter.toJsonValue(field, value);
		} else if (value != null && DisplayType.isText(field.getDisplayType())) {
			return value.toString();
		} else if (value != null && field.getDisplayType() == DisplayType.ID && value instanceof Number) {
			return ((Number)value).intValue();
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("rawtypes")
	/**
	 * Convert json value to db column value
	 * @param column
	 * @param value
	 * @return Object
	 */
	public static Object fromJsonValue(MColumn column, JsonElement value) {		
		ITypeConverter typeConverter = getTypeConverter(column.getAD_Reference_ID(), value);
		
		if (typeConverter != null) {
			return typeConverter.fromJsonValue(column, value);
		} else if (value != null && !(value instanceof JsonNull) && DisplayType.isText(column.getAD_Reference_ID())) {
			return value.getAsString();
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("rawtypes")
	/**
	 * Convert json value to db column value
	 * @param gridField
	 * @param value
	 * @return Object
	 */
	public static Object fromJsonValue(GridField gridField, JsonElement value) {		
		ITypeConverter typeConverter = getTypeConverter(gridField.getDisplayType(), value);
		
		if (typeConverter != null) {
			return typeConverter.fromJsonValue(gridField, value);
		} else if (value != null && !(value instanceof JsonNull) && DisplayType.isText(gridField.getDisplayType())) {
			return value.getAsString();
		} else {
			return null;
		}
	}
	
	/**
	 * convert arbitrary text to slug
	 * @param input
	 * @return slug
	 */
	public static String slugify(String input) {  
		String noseparators = SEPARATORS.matcher(input).replaceAll("-");
	    String normalized = Normalizer.normalize(noseparators, Form.NFD);
	    String slug = NONLATIN.matcher(normalized).replaceAll("");
	    return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}","-").replaceAll("^-|-$","");
	}
	
	@SuppressWarnings({ "rawtypes" })
	private static ITypeConverter getTypeConverter(int displayType, Object value) {
		ITypeConverter typeConverter = null;
		ServiceQuery query = new ServiceQuery();
		query.put("displayType", Integer.toString(displayType));
		typeConverter = Service.locator().locate(ITypeConverter.class, query).getService();
		if (typeConverter == null) {
			if (DisplayType.isNumeric(displayType) && value instanceof Number) {
				typeConverter = new NumericTypeConverter();
			} else if (DisplayType.isDate(displayType) && value instanceof Date) {
				typeConverter = new DateTypeConverter();
			} else if (DisplayType.YesNo == displayType) {
				typeConverter = new YesNoTypeConverter();
			} else if (displayType == Location
					|| displayType == Locator
					|| displayType == Account
					|| displayType == PAttribute
					|| displayType == Payment
					|| DisplayType.isLookup(displayType)) {
				return new LookupTypeConverter();
			} else if (displayType == Binary) {
				return new BinaryTypeConverter();
			}
		}
		return typeConverter;
	}
	
	@SuppressWarnings({ "rawtypes" })
	private static ITypeConverter getTypeConverter(int displayType, JsonElement value) {
		ITypeConverter typeConverter = null;
		ServiceQuery query = new ServiceQuery();
		query.put("displayType", Integer.toString(displayType));
		typeConverter = Service.locator().locate(ITypeConverter.class, query).getService();
		if (typeConverter == null) {
			if (DisplayType.isNumeric(displayType) && (isNumber(value) || isString(value))) {
				typeConverter = new NumericTypeConverter();
			} else if (DisplayType.isDate(displayType) && isString(value)) {
				typeConverter = new DateTypeConverter();
			} else if (DisplayType.YesNo == displayType && (isBoolean(value) || isString(value))) {
				typeConverter = new YesNoTypeConverter();
			} else if (displayType == Location
					|| displayType == Locator
					|| displayType == Account
					|| displayType == PAttribute
					|| displayType == Payment
					|| DisplayType.isLookup(displayType)) {
				return new LookupTypeConverter();
			} else if (displayType == Binary) {
				return new BinaryTypeConverter();
			}
		}
		return typeConverter;
	}

	private static boolean isBoolean(JsonElement value) {
		if (value instanceof JsonPrimitive) {
			JsonPrimitive primitive = (JsonPrimitive) value;
			return primitive.isBoolean();
		}
		return false;
	}

	private static boolean isString(JsonElement value) {
		if (value instanceof JsonPrimitive) {
			JsonPrimitive primitive = (JsonPrimitive) value;
			return primitive.isString();
		}
		return false;
	}

	private static boolean isNumber(JsonElement value) {
		if (value instanceof JsonPrimitive) {
			JsonPrimitive primitive = (JsonPrimitive) value;
			return primitive.isNumber();
		}
		return false;
	}		  	
	
	private final static String UUID_REGEX="[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
	
	/**
	 * @param value
	 * @return true if value is a UUID identifier
	 */
	public static boolean isUUID(String value) {
		return value == null ? false : value.matches(UUID_REGEX);
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
}

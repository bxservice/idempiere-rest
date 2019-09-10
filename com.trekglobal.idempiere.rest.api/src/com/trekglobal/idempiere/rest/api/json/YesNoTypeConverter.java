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

import org.compiere.model.GridField;
import org.compiere.model.MColumn;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * 
 * json type converter for AD yes/no type
 * @author hengsin
 *
 */
public class YesNoTypeConverter implements ITypeConverter<Object> {

	/**
	 * default constructor
	 */
	public YesNoTypeConverter() {
	}

	@Override
	public Object toJsonValue(MColumn column, Object value) {
		return toJsonValue(column.getAD_Reference_ID(), value);
	}

	@Override
	public Object toJsonValue(GridField field, Object value) {
		return toJsonValue(field.getDisplayType(), value);
	}
	
	@Override
	public Object fromJsonValue(MColumn column, JsonElement value) {
		return fromJsonValue(column.getAD_Reference_ID(), value);
	}

	@Override
	public Object fromJsonValue(GridField field, JsonElement value) {
		return fromJsonValue(field.getDisplayType(), value);
	}
	
	private Object toJsonValue(int displayType, Object value) {
		if (value instanceof Boolean) {
			return (Boolean)value;
		} else {
			return ("y".equalsIgnoreCase(value.toString()) || "true".equalsIgnoreCase(value.toString()));
		}
	}
	
	private Object fromJsonValue(int displayType, JsonElement value) {
		JsonPrimitive primitive = (JsonPrimitive) value;
		if (primitive.isBoolean()) {
			return primitive.getAsBoolean();
		} else {
			return ("y".equalsIgnoreCase(value.toString()) || "true".equalsIgnoreCase(value.toString()));
		}
	}
}

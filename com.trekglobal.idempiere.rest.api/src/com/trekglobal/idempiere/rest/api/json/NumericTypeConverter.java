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
import org.compiere.util.DisplayType;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * 
 * json type converter for AD numeric type
 * @author hengsin
 *
 */
public class NumericTypeConverter implements ITypeConverter<Number> {

	/**
	 * 
	 */
	public NumericTypeConverter() {
	}

	@Override
	public Object toJsonValue(MColumn column, Number value) {
		return toJsonValue(column.getAD_Reference_ID(), value);
	}

	@Override
	public Object toJsonValue(GridField field, Number value) {
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
	
	private Object toJsonValue(int displayType, Number value) {
		if (!(DisplayType.isNumeric(displayType) || displayType == Button))
			return null;
		
		if (displayType == DisplayType.Integer) {
			return value.intValue();
		} else {
			return value;
		}
	}
	
	private Object fromJsonValue(int displayType, JsonElement value) {
		if (!(DisplayType.isNumeric(displayType) || displayType == Button))
			return null;
		
		JsonPrimitive primitive = (JsonPrimitive) value;
		if (displayType == DisplayType.Integer) {
			if (primitive.isString())
				return Integer.parseInt(primitive.getAsString());
			else
				return primitive.getAsInt();
		} else {
			return primitive.getAsBigDecimal();
		}
	}
}

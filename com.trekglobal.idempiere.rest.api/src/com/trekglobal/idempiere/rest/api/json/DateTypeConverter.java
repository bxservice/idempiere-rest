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

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.core.Response.Status;

import org.compiere.model.GridField;
import org.compiere.model.MColumn;
import org.compiere.util.DisplayType;

import com.google.gson.JsonElement;

/**
 * 
 * Type converter for DisplayType.Date and DisplayType.DateTime
 * @author hengsin
 *
 */
public class DateTypeConverter implements ITypeConverter<Date> {
	public static final String ISO8601_DATE_PATTERN = "yyyy-MM-dd";
	public static final String ISO8601_TIME_PATTERN = "HH:mm:ss'Z'";
	public static final String ISO8601_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	/**
	 * 
	 */
	public DateTypeConverter() {
	}

	public Object toJsonValue(int displayType, Date value) {
		String pattern = getPattern(displayType);
		
		if (DisplayType.isDate(displayType) && pattern != null && value != null) {
			String formatted = new SimpleDateFormat(pattern).format(value);
			return formatted;
		} else {
			return null;
		}
	}

	@Override
	public Object toJsonValue(MColumn column, Date value) {
		return toJsonValue(column.getAD_Reference_ID(), value);
	}

	@Override
	public Object toJsonValue(GridField field, Date value) {
		return toJsonValue(field.getDisplayType(), value);
	}

	private Timestamp fromJsonValue(int displayType, JsonElement value) {
		String pattern = getPattern(displayType);
		
		if (DisplayType.isDate(displayType) && pattern != null && value != null) {
			Date parsed = null;
			try {
				parsed = new SimpleDateFormat(pattern).parse(value.getAsString());
			} catch (ParseException e) {
				throw new IDempiereRestException("Invalid date", "The " + DisplayType.getDescription(displayType) + " pattern should be: " + pattern + ". Exception: " + e.getLocalizedMessage(), Status.BAD_REQUEST);
			}
			return new Timestamp(parsed.getTime());
		} else {
			return null;
		}
	}
	
	@Override
	public Object fromJsonValue(MColumn column, JsonElement value) {
		return fromJsonValue(column.getAD_Reference_ID(), value);
	}

	@Override
	public Object fromJsonValue(GridField field, JsonElement value) {
		return fromJsonValue(field.getDisplayType(), value);
	}
	
	/**
	 * Returns an ISO-8601 format pattern according to the display type.
	 * @param displayType Display Type
	 * @return formatting pattern
	 */
	private String getPattern(int displayType) {
		if (displayType == DisplayType.Date)
			return ISO8601_DATE_PATTERN;
		else if (displayType == DisplayType.Time)
			return ISO8601_TIME_PATTERN;
		else if (displayType == DisplayType.DateTime)
			return ISO8601_DATETIME_PATTERN;
		else
			return null;
	}
}

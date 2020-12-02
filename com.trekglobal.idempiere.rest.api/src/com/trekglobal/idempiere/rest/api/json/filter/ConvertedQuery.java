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
 * - BX Service GmbH                                                   *
 * - Diego Ruiz                                                        *
 **********************************************************************/
package com.trekglobal.idempiere.rest.api.json.filter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.util.DisplayType;

import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;

public class ConvertedQuery {
	
	private StringBuilder whereClause;
	private List<Object> parameters;
	
	public ConvertedQuery() {
		whereClause = new StringBuilder("");
		parameters = new ArrayList<>();
	}
	
	public String getWhereClause() {
		return whereClause.toString();
	}
	
	public void appendWhereClause(String append) {
		whereClause.append(append);
	}
	
	public List<Object> getParameters() {
		return parameters;
	}
	
	public void addParameter(Object parameter) {
		parameters.add(parameter);
	}
	
	public void addParameter(int pos, Object parameter) {
		parameters.add(pos, parameter);
	}
	
	/**
	 * Converts the parameter to the appropriate DataType
	 * */
	public void addParameter(MColumn column, String parameter) {
		if (column == null)
			return;
		
		if (column.isSecure() || column.isEncrypted())
			throw new AdempiereException("Cannot query the column: " + column);
			
		int displayType = column.getAD_Reference_ID();
		try {
			
			if (DisplayType.isID(displayType) && 
					// Two special columns: Defined as Table but DB Type is String					
					!column.getColumnName().equals("EntityType") && 
					!column.getColumnName().equals("AD_Language"))
				addParameter(Integer.valueOf(parameter));
			else if (displayType == DisplayType.Integer)   //   Integer
				addParameter(Integer.valueOf(parameter));
			else if (DisplayType.isNumeric(displayType))   //	Number
				addParameter(new BigDecimal(parameter));
			else if (DisplayType.isDate(displayType)) //	Timestamps
			{
				// Try Timestamp format - then date format
				java.util.Date date = null;
				SimpleDateFormat dateTimeFormat = DisplayType.getTimestampFormat_Default();
				SimpleDateFormat dateFormat = DisplayType.getDateFormat_JDBC();
				
				//If the value comes with ' remove them
				if (parameter.contains("'"))
					parameter = extractFromStringValue(parameter);
				try {
					if (displayType == DisplayType.Date) {
						date = dateFormat.parse(parameter);
					} else {
						date = dateTimeFormat.parse(parameter);
					}
				} catch (java.text.ParseException e) {
					date = dateFormat.parse(parameter);
				}
				addParameter(new Timestamp (date.getTime()));
			}
			else if (displayType == DisplayType.YesNo) //	Boolean
				addParameter(Boolean.valueOf("Y".equals(parameter) || "true".equals(parameter)));
			else if (displayType == DisplayType.String || displayType == DisplayType.List  || 
					parameter.startsWith("'") && parameter.endsWith("'")) {
				if (parameter.startsWith("'") && parameter.endsWith("'"))
					addParameter(extractFromStringValue(parameter));
				else 
					throw new IDempiereRestException("String values must be put between single quotes. ColumnName: " + column.getName(), Status.BAD_REQUEST);
			}
			else 
				addParameter(parameter);
		} catch (Exception e) {
			throw new IDempiereRestException("Error convertig parameter with value: " + parameter + " - " + e.getMessage(), Status.BAD_REQUEST);
		}
	}
	
	
	/**
	 * Remove the ' from the call Strings 
	 * */
    public static String extractFromStringValue(String value) {
        return value.substring(1, value.length() - 1);
    }
}

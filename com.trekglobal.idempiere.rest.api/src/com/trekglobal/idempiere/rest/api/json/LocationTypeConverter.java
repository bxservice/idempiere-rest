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
import org.compiere.model.Lookup;
import org.compiere.model.MColumn;
import org.compiere.model.MLocation;
import org.compiere.model.MLocationLookup;
import org.compiere.model.MLookupFactory;
import org.compiere.model.MProcessPara;
import org.compiere.model.MTable;
import org.compiere.model.MValRule;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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
		String label = Msg.getElement(Env.getCtx(), column.getColumnName());
		Lookup lookup = new MLocationLookup(Env.getCtx(), 0);
		return toJsonValue(column.getAD_Reference_ID(), label, lookup, column.getReferenceTableName(), value);
	}

	@Override
	public Object toJsonValue(GridField field, Object value) {
		return toJsonValue(field.getDisplayType(), field.getHeader(), field.getLookup(), getReferenceTableNameFromField(field), value);
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
			MLocation loc = MLocation.get((Integer)value);
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
			
			MColumn[] columns = MTable.get(MLocation.Table_ID).getColumns(false);
			String columnName = null;
			Object columnValue = null;
			
			for(MColumn column : columns) {
				if(column.isKey())continue;
				
				columnName = column.getColumnName();
				
				if (columnName.endsWith("_ID")) {
					if((columnValue = loc.get_Value(columnName))!=null) {
						JsonObject refChild = new JsonObject();
						refChild.addProperty("propertyLabel",Msg.getElement(Env.getCtx(), columnName));
						if (value instanceof Number)
							refChild.addProperty("id", (Integer)columnValue);
						else
							refChild.addProperty("id", value.toString());
						String displayValue = getColumnLookup(column).getDisplay(columnValue);
						if(displayValue!=null)
							refChild.addProperty("identifier",displayValue);
						refChild.addProperty("model-name", MTable.get(Env.getCtx(), columnName.replace("_ID", "")).getTableName());
						ref.add(columnName, refChild);
					}
				} else {
					if((columnValue = loc.get_Value(columnName))!=null) {
						ref.addProperty(columnName, columnValue.toString());
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
		return fromJson(value);
	}
	
	public Object fromJson(JsonElement value) {
		if (value != null && value.isJsonObject()) {
		
			JsonObject ref = value.getAsJsonObject();
			JsonElement idField = ref.get("id");
			int C_Location_ID = 0;
			if (idField != null) {
				JsonPrimitive primitive = (JsonPrimitive) idField;
				if (primitive.isNumber())
					C_Location_ID = primitive.getAsInt();
				else
					C_Location_ID = 0;
			}

			JsonElement address1Field = ref.get(MLocation.COLUMNNAME_Address1.toLowerCase());
			String address1 = null;
			if (address1Field != null) {
				JsonPrimitive primitive = (JsonPrimitive) address1Field;
				address1 = primitive.getAsString();
			}
			
			JsonElement address2Field = ref.get(MLocation.COLUMNNAME_Address2.toLowerCase());
			String address2 = null;
			if (address2Field != null) {
				JsonPrimitive primitive = (JsonPrimitive) address2Field;
				address2 = primitive.getAsString();
			}
			
			JsonElement address3Field = ref.get(MLocation.COLUMNNAME_Address3.toLowerCase());
			String address3 = null;
			if (address3Field != null) {
				JsonPrimitive primitive = (JsonPrimitive) address3Field;
				address3 = primitive.getAsString();
			}
			
			JsonElement address4Field = ref.get(MLocation.COLUMNNAME_Address3.toLowerCase());
			String address4 = null;
			if (address3Field != null) {
				JsonPrimitive primitive = (JsonPrimitive) address4Field;
				address4 = primitive.getAsString();
			}
			
			JsonElement address5Field = ref.get(MLocation.COLUMNNAME_Address5.toLowerCase());
			String address5 = null;
			if (address5Field != null) {
				JsonPrimitive primitive = (JsonPrimitive) address5Field;
				address5 = primitive.getAsString();
			}
			
			JsonElement postalField = ref.get(MLocation.COLUMNNAME_Postal.toLowerCase());
			String postal = null;
			if (postalField != null) {
				JsonPrimitive primitive = (JsonPrimitive) postalField;
				postal = primitive.getAsString();
			}
			
			JsonElement postalAddField = ref.get(MLocation.COLUMNNAME_Postal_Add.toLowerCase());
			String postalAdd = null;
			if (postalAddField != null) {
				JsonPrimitive primitive = (JsonPrimitive) postalAddField;
				postalAdd = primitive.getAsString();
			}
			
			JsonElement countryField = ref.get(MLocation.COLUMNNAME_C_Country_ID.toLowerCase());
			int C_Country_ID = 0;
			if (countryField != null) {
				JsonPrimitive primitive = (JsonPrimitive) countryField;
				if (primitive.isNumber())
					C_Country_ID = primitive.getAsInt();
			}
			
			JsonElement regionField = ref.get(MLocation.COLUMNNAME_C_Region_ID.toLowerCase());
			int C_Region_ID = 0;
			if (countryField != null) {
				JsonPrimitive primitive = (JsonPrimitive) regionField;
				if (primitive.isNumber())
					C_Region_ID = primitive.getAsInt();
			}
			
			JsonElement regionNameField = ref.get(MLocation.COLUMNNAME_RegionName.toLowerCase());
			String regionName = null;
			if (regionNameField != null) {
				JsonPrimitive primitive = (JsonPrimitive) regionNameField;
				regionName = primitive.getAsString();
			}
			
			JsonElement cityIDField = ref.get(MLocation.COLUMNNAME_C_City_ID.toLowerCase());
			int C_City_ID = 0;
			if (cityIDField != null) {
				JsonPrimitive primitive = (JsonPrimitive) cityIDField;
				if (primitive.isNumber())
					C_City_ID = primitive.getAsInt();
			}
			
			JsonElement cityField = ref.get(MLocation.COLUMNNAME_City.toLowerCase());
			String city = null;
			if (cityField != null) {
				JsonPrimitive primitive = (JsonPrimitive) cityField;
				city = primitive.getAsString();
			}
			
			JsonElement commentsField = ref.get(MLocation.COLUMNNAME_Comments.toLowerCase());
			String comments = null;
			if (commentsField != null) {
				JsonPrimitive primitive = (JsonPrimitive) commentsField;
				comments = primitive.getAsString();
			}
			
			JsonElement isActiveField = ref.get(MLocation.COLUMNNAME_IsActive.toLowerCase());
			boolean isActive = false;
			if (isActiveField != null) {
				JsonPrimitive primitive = (JsonPrimitive) isActiveField;
				isActive = primitive.getAsBoolean();
			}
			
			MLocation loc = new MLocation(Env.getCtx(), C_Location_ID, null);
			
			if (address1 != null)
				loc.setAddress1(address1);
			if (address2 != null)
				loc.setAddress2(address2);
			if (address3 != null)
				loc.setAddress3(address3);
			if (address4 != null)
				loc.setAddress4(address4);
			if (address5 != null)
				loc.setAddress5(address5);
			if (postal != null)
				loc.setPostal(postal);
			if (postalAdd != null)
				loc.setPostal_Add(postalAdd);
			
			if (C_Country_ID > 0)
				loc.setC_Country_ID(C_Country_ID);
			
			if (C_Region_ID > 0)
				loc.setC_Region_ID(C_Region_ID);
			
			if (regionName != null)
				loc.setRegionName(regionName);
			
			if (C_City_ID > 0)
				loc.setC_City_ID(C_City_ID);
			
			if (city != null)
				loc.setCity(city);
			
			if (comments != null)
				loc.setComments(comments);
			
			loc.setIsActive(isActive);
			loc.saveEx();
			
			return loc.get_ID();
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
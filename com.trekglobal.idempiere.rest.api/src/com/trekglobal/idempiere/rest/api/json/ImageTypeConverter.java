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

import java.util.Base64;

import org.compiere.model.GridField;
import org.compiere.model.MColumn;
import org.compiere.model.MImage;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * json type converter for AD_Image
 * @author muriloht
 *
 */
public class ImageTypeConverter implements ITypeConverter<Object> {

	/**
	 * default constructor
	 */
	public ImageTypeConverter() {
	}

	@Override
	public Object toJsonValue(MColumn column, Object value) {
		String label = Msg.getElement(Env.getCtx(), column.getColumnName());
		return toJsonValue(label, value);
	}

	@Override
	public Object toJsonValue(GridField field, Object value) {
		return toJsonValue(field.getHeader(), value);
	}

	private Object toJsonValue(String label, Object value) {
		if (value != null)
		{
			MImage img = MImage.get((Integer)value);
			
			JsonObject ref = new JsonObject();
			ref.addProperty("propertyLabel", label);
			if (value instanceof Number)
				ref.addProperty("id", ((Number)value).intValue());
			
			String data = Base64.getEncoder().encodeToString(img.getBinaryData());
			if (!Util.isEmpty(data, true)) {
				ref.addProperty("data", data);
			}							
			ref.addProperty("model-name", MImage.Table_Name.toLowerCase());
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
			int AD_Image_ID = 0;
			if (idField != null) {
				JsonPrimitive primitive = (JsonPrimitive) idField;
				if (primitive.isNumber())
					AD_Image_ID = primitive.getAsInt();
				else
					AD_Image_ID = 0;
			}

			JsonElement nameField = ref.get("file_name");
			String fileName = null;
			if (nameField != null) {
				JsonPrimitive primitive = (JsonPrimitive) nameField;
				fileName = primitive.getAsString();
			}

			JsonElement url = ref.get("url");
			String fileURL = null;
			if (url != null) {
				JsonPrimitive primitive = (JsonPrimitive) url;
				fileURL = primitive.getAsString();
			}

			MImage image = new MImage(Env.getCtx(), AD_Image_ID, null);
			JsonElement data = ref.get("data");
			if (data != null) {
				if (fileName != null)
					image.setName(fileName);
				
				if (fileURL != null)
					image.setImageURL(fileURL);
				
				byte[] imageBytes = Base64.getDecoder().decode(data.getAsString());
				image.setBinaryData(imageBytes);
				image.saveEx();

				return image.get_ID();
			}			
		} 
		
		return null;
	}
}

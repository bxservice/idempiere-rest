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

import javax.xml.bind.DatatypeConverter;

import org.compiere.model.GridField;
import org.compiere.model.MColumn;

import com.google.gson.JsonElement;

/**
 * type converter for DisplayType.Binary
 * @author hengsin
 *
 */
public class BinaryTypeConverter implements ITypeConverter<byte[]> {

	public BinaryTypeConverter() {
	}

	@Override
	public Object toJsonValue(MColumn column, byte[] value) {
		if (value != null) {
			return DatatypeConverter.printBase64Binary(value);
		}
		return null;
	}

	@Override
	public Object toJsonValue(GridField field, byte[] value) {
		if (value != null) {
			return DatatypeConverter.printBase64Binary(value);
		}
		return null;
	}

	@Override
	public Object fromJsonValue(MColumn column, JsonElement value) {
		if (value != null && value.isJsonPrimitive()) {
			String base64Value = value.getAsString();
			byte[] data = DatatypeConverter.parseBase64Binary(base64Value);
			return data;
		}
		return null;
	}

	@Override
	public Object fromJsonValue(GridField field, JsonElement value) {
		if (value != null && value.isJsonPrimitive()) {
			String base64Value = value.getAsString();
			byte[] data = DatatypeConverter.parseBase64Binary(base64Value);
			return data;
		}
		return null;
	}

}

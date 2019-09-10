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

/**
 * 
 * Interface for type converter between AD type and Json type
 * @author hengsin
 *
 */
public interface ITypeConverter<T> {

	/**
	 * Convert AD type to json type
	 * @param column
	 * @param value
	 * @return Object
	 */
	public Object toJsonValue(MColumn column, T value);
	
	/**
	 * Convert AD type to json type
	 * @param field
	 * @param value
	 * @return Object
	 */
	public Object toJsonValue(GridField field, T value);

	/**
	 * Convert json type to AD type
	 * @param column
	 * @param value
	 * @return Object
	 */
	public Object fromJsonValue(MColumn column, JsonElement value);
	
	/**
	 * Convert json type to AD type
	 * @param field
	 * @param value
	 * @return Object
	 */
	public Object fromJsonValue(GridField field, JsonElement value);
}

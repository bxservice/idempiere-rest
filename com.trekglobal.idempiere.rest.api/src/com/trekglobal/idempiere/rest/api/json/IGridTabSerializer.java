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

import java.util.List;

import org.adempiere.base.Service;
import org.compiere.model.GridTab;

import com.google.gson.JsonObject;

/**
 * Serialize/Deserialize interface for GridTab
 * @author hengsin
 *
 */
public interface IGridTabSerializer {
	/**
	 * Convert current row to json
	 * @param gridTab
	 * @return JsonObject
	 */
	public default JsonObject toJson(GridTab gridTab) {
		return toJson(gridTab, null, null);
	}
	
	/**
	 * Convert current row to json
	 * @param gridTab
	 * @param includes columns to include
	 * @param excludes columns to exclude
	 * @return JsonObject
	 */
	public JsonObject toJson(GridTab gridTab, String[] includes, String[] excludes);
	
	/**
	 * Copy values from JsonObject to GridTab
	 * @param json
	 * @param gridTab 
	 */
	public void fromJson(JsonObject json, GridTab gridTab);
	
	/**
	 * Get GridTab serializer
	 * @param gridTabUID uuid of ad_tab
	 * @return IGridTabSerializer
	 */
	public static IGridTabSerializer getGridTabSerializer(String gridTabUID) {
		IGridTabSerializer serializer = null;
		List<IGridTabSerializerFactory> factories = Service.locator().list(IGridTabSerializerFactory.class).getServices();
		for (IGridTabSerializerFactory  factory : factories) {
			serializer = factory.getGridTabSerializer(gridTabUID);
			if (serializer != null) {
				break;
			}
		}
		if (serializer == null) {
			for (IGridTabSerializerFactory  factory : factories) {
				serializer = factory.getGridTabSerializer("*");
				if (serializer != null) {
					break;
				}
			}
		}
			
		
		return serializer;
	}
}

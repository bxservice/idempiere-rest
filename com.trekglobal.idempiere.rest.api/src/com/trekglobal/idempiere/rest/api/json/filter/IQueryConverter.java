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

import java.util.List;

import org.adempiere.base.Service;

public interface IQueryConverter {

    /*************************************************************************
	 *  Convert an individual REST style statements to a database WHERE statement syntax
	 *
	 *  @param queryStatement
	 *  @return converted object including where clause and Array of parameters
	 *  @throws Exception
	 */
	public ConvertedQuery convertStatement(String tableName, String queryStatement);
	
	/**
	 * Get Query Converter based on the convention being used
	 * @param converterName
	 * @return IQueryConverter
	 */
	public static IQueryConverter getQueryConverter(String converterName) {
		IQueryConverter serializer = null;
		List<IQueryConverterFactory> factories = Service.locator().list(IQueryConverterFactory.class).getServices();
		for (IQueryConverterFactory  factory : factories) {
			serializer = factory.getQueryConverter(converterName);
			if (serializer != null) {
				break;
			}
		}
		if (serializer == null) {
			for (IQueryConverterFactory  factory : factories) {
				serializer = factory.getQueryConverter("DEFAULT");
				if (serializer != null) {
					break;
				}
			}
		}

		return serializer;
	}
}

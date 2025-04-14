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
package com.trekglobal.idempiere.rest.api.json;

public interface QueryOperators {

	//Supported OData query operators
	public static final String SELECT  = "$select";
	public static final String EXPAND  = "$expand";
	public static final String FILTER  = "$filter";
	public static final String ORDERBY = "$orderby";
	public static final String TOP     = "$top";
	public static final String SKIP    = "$skip";
	
	//Custom iDempiere query operators
	public static final String SHOW_SQL    = "showsql";
	public static final String VALRULE     = "$valrule";
	public static final String CONTEXT     = "$context";
	public static final String REPORTTYPE  = "$report_type";
	public static final String INCLUDE_MSG = "with_messages";
	public static final String AS_JSON     = "json";
	public static final String LABEL	   = "label";
	public static final String SHOW_LABEL  = "showlabel";

}

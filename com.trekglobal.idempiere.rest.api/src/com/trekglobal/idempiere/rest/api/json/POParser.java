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

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.PO;

public class POParser {
	
	//TODO: Replace with Msg calls to make it translatable
	private static final String ACCESS_DENIED_TITLE = "Access denied";
	private static final String ACCESS_DENIED_MESSAGE = "Access denied for record with id: ";
	private static final String NOT_FOUND_TITLE = "Record not found";
    private static final String NOT_FOUND_MESSAGE = "No record found matching id: ";

	private PO po;
	private String tableName;
	private String recordID;
	private Response responseError;
	
	public POParser(String tableName, String recordID, boolean fullyQualifiedWhere, boolean isReadWrite) {
		this.tableName = tableName;
		this.recordID = recordID;
		if (isValidTable(isReadWrite))
			po = RestUtils.getPO(tableName, recordID, fullyQualifiedWhere, isReadWrite);
	}
	
	public POParser(String tableName, String recordID, PO po) {
		this.tableName = tableName;
		this.recordID = recordID;
		if (isValidTable(false))
			this.po = po;
	}
	
	private boolean isValidTable(boolean isReadWrite) {
		try {
			//Call to check if user has access to the table
			RestUtils.getTableAndCheckAccess(tableName, isReadWrite);
		} catch (Exception ex) {
			responseError = ResponseUtils.getResponseErrorFromException(ex, "", "");
			return false;
		}
		return true;
	}
	
	public boolean isValidPO() {
		return po != null;
	}
	
	public PO getPO() {
		return po;
	}
	
	public Response getResponseError() {
		if (responseError != null)
			return responseError;

		po = RestUtils.getPO(tableName, recordID, false, false);

		if (po != null) {
			return ResponseUtils.getResponseError(Status.FORBIDDEN, ACCESS_DENIED_TITLE, ACCESS_DENIED_MESSAGE, recordID);
		} else {
			return ResponseUtils.getResponseError(Status.NOT_FOUND, NOT_FOUND_TITLE, NOT_FOUND_MESSAGE, recordID);
		}
	}
}

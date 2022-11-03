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

import javax.ws.rs.core.Response.Status;

import org.adempiere.exceptions.AdempiereException;

public class IDempiereRestException extends AdempiereException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3555412217944639699L;
	
	private Status status;
	private String title;
	
	/**
	 * @param message
	 * @param error -> Response status error
	 */
	public IDempiereRestException(String message, Status error) {
		super(message);
		setErrorResponseStatus(error);
	}
	
	/**
	 * @param title
	 * @param message
	 * @param error -> Response status error
	 */
	public IDempiereRestException(String title, String message, Status error) {
		super(message);
		setErrorResponseStatus(error);
		setTitle(title);
	}
	
	public void setErrorResponseStatus(Status status) {
		this.status = status;
	}
	
	public Status getErrorResponseStatus() {
		return status;
	}
	
	public void setTitle(String title) {
		this.title= title;
	}
	
	public String getTitle() {
		return title;
	}
}

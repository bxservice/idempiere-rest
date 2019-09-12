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
package com.trekglobal.idempiere.rest.api.v1.resource.impl;

import javax.ws.rs.core.Response.Status;

import org.compiere.util.Util;

import com.google.gson.JsonObject;

/**
 * Builder for error response json object 
 * @author hengsin
 *
 */
public class ErrorBuilder {

	private Status status=null;
	private String title=null;
	private StringBuilder detail = new StringBuilder();
	private String type = null;
	
	public ErrorBuilder() {
	}

	/**
	 * 
	 * @param status http status code
	 * @return ErrorBuilder
	 */
	public ErrorBuilder status(Status status) {
		this.status = status;
		return this;
	}
	
	/**
	 * 
	 * @param title error summary
	 * @return ErrorBuilder
	 */
	public ErrorBuilder title(String title) {
		this.title = title;
		return this;
	}
	
	/**
	 * error type/code
	 * @param type
	 * @return ErrorBuilder
	 */
	public ErrorBuilder type(String type) {
		this.type = type;
		return this;
	}
	
	/**
	 * 
	 * @param detail extra details
	 * @return ErrorBuilder
	 */
	public ErrorBuilder append(String detail) {
		this.detail.append(detail);
		return this;
	}
	
	/**
	 * 
	 * @return error response json object
	 */
	public JsonObject build() {
		JsonObject jso = new JsonObject();
		if (!Util.isEmpty(type, true)) {
			jso.addProperty("type", type);
		}
		if (!Util.isEmpty("title", true)) {
			jso.addProperty("title", title);
		}
		if (status != null) {
			jso.addProperty("status", status.getStatusCode());
		}		
		if (detail.length() > 0) {
			jso.addProperty("detail", detail.toString());
		}
		return jso;
	}
}

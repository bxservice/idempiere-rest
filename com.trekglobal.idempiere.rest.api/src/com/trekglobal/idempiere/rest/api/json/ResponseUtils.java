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

import java.util.logging.Level;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.util.CLogger;

import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;

public class ResponseUtils {

	private final static CLogger log = CLogger.getCLogger(ResponseUtils.class);

	public static Response getResponseErrorFromException(Exception ex, String title) {
		return getResponseErrorFromException(ex, title, "");
	}
	
	private static IDempiereRestException findRestException(Throwable ex) {
		if (ex == null)
			return null;
		if (ex instanceof IDempiereRestException)
			return (IDempiereRestException) ex;
		
		Throwable cause = ex.getCause();
		while (cause != null) {
			if (cause instanceof IDempiereRestException)
				return (IDempiereRestException) cause;
			cause = cause.getCause();
		}
		
		return null;
	}
	
	public static Response getResponseErrorFromException(Exception ex, String title, String detailText) {
		Status status = Status.INTERNAL_SERVER_ERROR;
		IDempiereRestException restException = findRestException(ex);
		if (restException != null) {
			status = restException.getErrorResponseStatus();
			title = restException.getTitle();			
		}

		// Handle formatting issues with error message from model validation events
		String msg = ex.getMessage();
		if (msg != null) {
			if (msg.endsWith("<br>")) {
				msg = msg.substring(0, msg.length() - 4);
			}
			if (status == Status.BAD_REQUEST) {
				if (msg.startsWith("Error: ") && msg.indexOf(":", 8) > 0) {
					msg = msg.substring(7);
				}
			}
		}
		log.log(Level.SEVERE, msg, ex);
		return getResponseError(status, title, detailText, msg);
	}
	
	public static Response getResponseError(Status status, String title, String text1, String text2) {
		return Response.status(status)
				.entity(new ErrorBuilder()
						.status(status)
						.title(title)
						.append(text1)
						.append(text2)
						.build()
						.toString())
				.build();
	}
	
}

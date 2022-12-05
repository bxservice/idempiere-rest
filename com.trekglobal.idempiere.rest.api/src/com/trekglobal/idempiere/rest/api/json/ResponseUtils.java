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

	public static Response getResponseErrorFromException(Exception ex, String title, String detailText) {
		Status status = Status.INTERNAL_SERVER_ERROR;
		if (ex instanceof IDempiereRestException) {
			status = ((IDempiereRestException) ex).getErrorResponseStatus();
			title = ((IDempiereRestException) ex).getTitle();
		}

		log.log(Level.SEVERE, ex.getMessage(), ex);
		return getResponseError(status, title, detailText, ex.getMessage());
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

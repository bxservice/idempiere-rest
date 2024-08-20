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
package com.trekglobal.idempiere.rest.api.v1.resource.impl;

import java.util.List;
import java.util.logging.Level;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.MStatusLine;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.json.ResponseUtils;
import com.trekglobal.idempiere.rest.api.json.filter.ConvertedQuery;
import com.trekglobal.idempiere.rest.api.json.filter.IQueryConverter;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.v1.resource.StatusLineResource;

/**
 * 
 * @author Diego Ruiz
 *
 */
public class StatusLineResourceImpl implements StatusLineResource {

	private final static CLogger log = CLogger.getCLogger(StatusLineResourceImpl.class);

	public StatusLineResourceImpl() {
	}

	@Override
	public Response getStatusLines(String filter) {
		IQueryConverter converter = IQueryConverter.getQueryConverter("DEFAULT");
		try {
			ConvertedQuery convertedStatement = converter.convertStatement(MStatusLine.Table_Name, filter);
			if (log.isLoggable(Level.INFO)) 
				log.info("Where Clause: " + convertedStatement.getWhereClause());

			JsonArray statusLineArray = new JsonArray();

			Query query = new Query(Env.getCtx(), MStatusLine.Table_Name, convertedStatement.getWhereClause(), null);
			query.setApplyAccessFilter(true)
			.setOnlyActiveRecords(true)
			.setOrderBy("Name");
			query.setParameters(convertedStatement.getParameters());

			List<MStatusLine> statusLines = query.list();
			IPOSerializer serializer = IPOSerializer.getPOSerializer(MStatusLine.Table_Name, MTable.getClass(MStatusLine.Table_Name));
			for(MStatusLine statusLine : statusLines) {
				JsonObject jsonObject = serializer.toJson(statusLine, new String[] {"AD_StatusLine_ID", "AD_StatusLine_UU", "Name", "EntityType"}, null);
				statusLineArray.add(jsonObject);
			}

			JsonObject json = new JsonObject();
			json.add("statusLines", statusLineArray);
			return Response.ok(json.toString()).build();

		} catch (Exception ex) {
			Status status = Status.INTERNAL_SERVER_ERROR;
			if (ex instanceof IDempiereRestException)
				status = ((IDempiereRestException) ex).getErrorResponseStatus();

			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(status)
					.entity(new ErrorBuilder().status(status)
							.title("GET Error")
							.append("Get status lines with exception: ")
							.append(ex.getMessage())
							.build().toString())
					.build();
		}
	}

	@Override
	public Response getStatusLineValue(int statusLineId) {
		try {
			MStatusLine statusLine = new MStatusLine(Env.getCtx(), statusLineId, null);
			if (statusLine.getSQLStatement() != null) {
				JsonObject json = getJsonStatusLine(statusLine);
				return Response.ok(json.toString()).build();
			} else {
				return ResponseUtils.getResponseError(Status.NOT_FOUND, "Status Line not found", 
						"No valid status line with the given id = ", String.valueOf(statusLineId));
			}
		} catch (Exception ex) {
			Status status = Status.INTERNAL_SERVER_ERROR;
			if (ex instanceof IDempiereRestException)
				status = ((IDempiereRestException) ex).getErrorResponseStatus();

			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(status)
					.entity(new ErrorBuilder().status(status)
							.title("GET Error")
							.append("Get status line with exception: ")
							.append(ex.getMessage())
							.build().toString())
					.build();
		}
	}
	
	private JsonObject getJsonStatusLine(MStatusLine statusLine) {
		String line = statusLine.parseLine(0);
		JsonObject json = new JsonObject();
		json.addProperty("name", statusLine.getName());
		json.addProperty("message", line);
		return json;
	}
}


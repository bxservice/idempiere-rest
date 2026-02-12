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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.adempiere.model.MInfoProcess;
import org.adempiere.model.MInfoRelated;
import org.compiere.model.MInfoColumn;
import org.compiere.model.MInfoWindow;
import org.compiere.model.MProcess;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.json.TypeConverterUtils;
import com.trekglobal.idempiere.rest.api.json.filter.ConvertedQuery;
import com.trekglobal.idempiere.rest.api.json.filter.IQueryConverter;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.v1.resource.InfoResource;
import com.trekglobal.idempiere.rest.api.v1.resource.info.InfoWindow;
import com.trekglobal.idempiere.rest.api.v1.resource.info.QueryResponse;

/**
 * @author hengsin
 *
 */
public class InfoResourceImpl implements InfoResource {

	private final static CLogger log = CLogger.getCLogger(InfoResourceImpl.class);
	
	private static final int DEFAULT_QUERY_TIMEOUT = 60 * 2;
	private static final int DEFAULT_PAGE_SIZE = 100;

	/**
	 * default constructor 
	 */
	public InfoResourceImpl() {
	}

	@Override
	public Response getInfoWindows(String filter) {
		IQueryConverter converter = IQueryConverter.getQueryConverter("DEFAULT");
		try {
			ConvertedQuery convertedStatement = converter.convertStatement(MInfoWindow.Table_Name, filter);
			if (log.isLoggable(Level.INFO)) log.info("Where Clause: " + convertedStatement.getWhereClause());

			Query query = new Query(Env.getCtx(), MInfoWindow.Table_Name, convertedStatement.getWhereClause(), null);
			query.setOnlyActiveRecords(true).setApplyAccessFilter(true);
			query.setQueryTimeout(DEFAULT_QUERY_TIMEOUT);
			query.setParameters(convertedStatement.getParameters());

			List<MInfoWindow> infoWindows = query.setOrderBy("AD_InfoWindow.Name").list();
			JsonArray array = new JsonArray();
			IPOSerializer serializer = IPOSerializer.getPOSerializer(MInfoWindow.Table_Name, MInfoWindow.class);
			for(MInfoWindow infoWindow : infoWindows) {
				if (hasAccess(infoWindow)) {
					JsonObject json = serializer.toJson(infoWindow);
					json.addProperty("slug", TypeConverterUtils.slugify(infoWindow.getName()));
					array.add(json);
				}
			}
			JsonObject json = new JsonObject();
			json.add("infowindows", array);
			return Response.ok(json.toString()).build();
		} catch (Exception ex) {
			Status status = Status.INTERNAL_SERVER_ERROR;
			if (ex instanceof IDempiereRestException)
				status = ((IDempiereRestException) ex).getErrorResponseStatus();
			
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(status)
					.entity(new ErrorBuilder().status(status)
							.title("GET Error")
							.append("Get InfoWindows with exception: ")
							.append(ex.getMessage())
							.build().toString())
					.build();
		}
	}

	private boolean hasAccess(MInfoWindow infoWindow) {
		MRole role = MRole.getDefault();
		if (role.getInfoAccess(infoWindow.getAD_InfoWindow_ID()) != null)
			return true;
		else
			return false;
	}

	@Override
	public Response getInfoWindowRecords(String infoSlug, String parameters, String orderBy, int pageNo) {
		Query query = new Query(Env.getCtx(), MInfoWindow.Table_Name, "slugify(name)=?", null);
		query.setOnlyActiveRecords(true).setApplyAccessFilter(true);
		MInfoWindow infoWindowModel = query.setParameters(infoSlug).first();
		if (infoWindowModel == null) {
			query.setApplyAccessFilter(false);
			infoWindowModel = query.setParameters(infoSlug).first();
			if (infoWindowModel != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for info window: ").append(infoSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid info window name").append("No match found for info window name: ").append(infoSlug).build().toString())
						.build();
			}
		}
		
		if (!hasAccess(infoWindowModel)) {
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for info window: ").append(infoSlug).build().toString())
					.build();
		}
		
		Map<String, JsonElement> paraMap = new HashMap<String, JsonElement>();
		if (!Util.isEmpty(parameters)) {
			Gson gson = new GsonBuilder().create();
			JsonObject jso = gson.fromJson(parameters, JsonObject.class);
			for(String key : jso.keySet()) {
				JsonElement element = jso.get(key);
				if (element != null) {
					paraMap.put(key, element);
				}
			}
		}
		
		InfoWindow infoWindow = new InfoWindow(infoWindowModel, null, orderBy, true);
		infoWindow.setQueryParameters(paraMap);
		QueryResponse queryResponse = infoWindow.executeQuery(DEFAULT_PAGE_SIZE, pageNo, DEFAULT_QUERY_TIMEOUT);
		JsonArray array = queryResponse.getRecords();
		JsonObject json = new JsonObject();
		json.addProperty("row-count", array.size());
		json.add("infowindow-records", array);
		ResponseBuilder response = Response.ok(json.toString());
		if (array.size() > 0) {
			pageNo = queryResponse.getPageNo();
			response.header("X-Array-Count", array.size());
			response.header("X-Page", pageNo);
			response.header("X-Per-Page", DEFAULT_PAGE_SIZE);
			if (queryResponse.isHasNextPage()) {
				response.header("X-Next-Page", pageNo+1);
			}
			if (pageNo > 1)
				response.header("X-Prev-Page: 1", pageNo-1);						
		}
		
		return response.build();
	}

	@Override
	public Response getInfoWindowColumns(String infoSlug) {
		Query query = new Query(Env.getCtx(), MInfoWindow.Table_Name, "slugify(name)=?", null);
		query.setOnlyActiveRecords(true).setApplyAccessFilter(true);
		MInfoWindow infoWindowModel = query.setParameters(infoSlug).first();
		if (infoWindowModel == null) {
			query.setApplyAccessFilter(false);
			infoWindowModel = query.setParameters(infoSlug).first();
			if (infoWindowModel != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for info window: ").append(infoSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid info window name").append("No match found for info window name: ").append(infoSlug).build().toString())
						.build();
			}
		}
		
		if (!hasAccess(infoWindowModel)) {
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for info window: ").append(infoSlug).build().toString())
					.build();
		}
		
		MInfoColumn[] infoColumns = infoWindowModel.getInfoColumns();
		JsonArray array = new JsonArray();
		IPOSerializer serializer = IPOSerializer.getPOSerializer(MInfoColumn.Table_Name, MInfoColumn.class);
		for(MInfoColumn infoColumn : infoColumns) {
			JsonObject json = serializer.toJson(infoColumn);
			array.add(json);
		}
		JsonObject json = new JsonObject();
		json.add("infowindowcolumns", array);
		return Response.ok(json.toString()).build();
	}

	@Override
	public Response getInfoWindowProcesses(String infoSlug) {
		Query query = new Query(Env.getCtx(), MInfoWindow.Table_Name, "slugify(name)=?", null);
		query.setOnlyActiveRecords(true).setApplyAccessFilter(true);
		MInfoWindow infoWindowModel = query.setParameters(infoSlug).first();
		if (infoWindowModel == null) {
			query.setApplyAccessFilter(false);
			infoWindowModel = query.setParameters(infoSlug).first();
			if (infoWindowModel != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for info window: ").append(infoSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid info window name").append("No match found for info window name: ").append(infoSlug).build().toString())
						.build();
			}
		}
		
		if (!hasAccess(infoWindowModel)) {
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for info window: ").append(infoSlug).build().toString())
					.build();
		}
		
		MInfoProcess[] infoProcesses = infoWindowModel.getInfoProcess(false);
		JsonArray array = new JsonArray();
		IPOSerializer serializer = IPOSerializer.getPOSerializer(MInfoProcess.Table_Name, MInfoProcess.class);
		for(MInfoProcess infoProcess : infoProcesses) {
			JsonObject json = serializer.toJson(infoProcess);
			MProcess process = MProcess.get(infoProcess.getAD_Process_ID());
			String slug = TypeConverterUtils.slugify(process != null ? process.getValue() : "");
			json.addProperty("slug", slug);
			array.add(json);
		}
		JsonObject json = new JsonObject();
		json.add("infowindowprocesses", array);
		return Response.ok(json.toString()).build();
	}

	@Override
	public Response getInfoWindowRelateds(String infoSlug) {
		Query query = new Query(Env.getCtx(), MInfoWindow.Table_Name, "slugify(name)=?", null);
		query.setOnlyActiveRecords(true).setApplyAccessFilter(true);
		MInfoWindow infoWindowModel = query.setParameters(infoSlug).first();
		if (infoWindowModel == null) {
			query.setApplyAccessFilter(false);
			infoWindowModel = query.setParameters(infoSlug).first();
			if (infoWindowModel != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for info window: ").append(infoSlug).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid info window name").append("No match found for info window name: ").append(infoSlug).build().toString())
						.build();
			}
		}
		
		if (!hasAccess(infoWindowModel)) {
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for info window: ").append(infoSlug).build().toString())
					.build();
		}
		
		MInfoRelated[] infoRelateds = infoWindowModel.getInfoRelated(false);
		JsonArray array = new JsonArray();
		IPOSerializer serializer = IPOSerializer.getPOSerializer(MInfoRelated.Table_Name, MInfoRelated.class);
		for(MInfoRelated infoRelated : infoRelateds) {
			JsonObject json = serializer.toJson(infoRelated);
			MInfoWindow relatedWindow = MInfoWindow.get(infoRelated.getRelatedInfo_ID(), null);
			String slug = TypeConverterUtils.slugify(relatedWindow != null ? relatedWindow.getName() : "");
			json.addProperty("slug", slug);
			array.add(json);
		}
		JsonObject json = new JsonObject();
		json.add("infowindowrelateds", array);
		return Response.ok(json.toString()).build();
	}

}

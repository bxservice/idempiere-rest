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

import java.util.List;
import java.util.logging.Level;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.MColumn;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.ResponseUtils;
import com.trekglobal.idempiere.rest.api.json.filter.ConvertedQuery;
import com.trekglobal.idempiere.rest.api.json.filter.IQueryConverter;
import com.trekglobal.idempiere.rest.api.model.MRestView;
import com.trekglobal.idempiere.rest.api.model.MRestViewColumn;
import com.trekglobal.idempiere.rest.api.model.MRestViewRelated;
import com.trekglobal.idempiere.rest.api.v1.resource.ViewResource;

public class ViewResourceImpl implements ViewResource {

	private final static CLogger log = CLogger.getCLogger(ViewResourceImpl.class);
	
	public ViewResourceImpl() {
	}

	private ModelResourceImpl restView() {
		return new ModelResourceImpl().restView();
	}
	
	@Override
	public Response getPO(String tableName, String id, String details, String select, String showsql) {
		return restView().getPO(tableName, id, details, select, showsql);
	}

	@Override
	public Response getPOProperty(String tableName, String id, String propertyName, String showsql) {
		return restView().getPOProperty(tableName, id, propertyName, showsql);
	}

	@Override
	public Response getPOs(String tableName, String details, String filter, String order, String select, int top,
			int skip, String validationRuleID, String context, String showsql) {
		return restView().getPOs(tableName, details, filter, order, select, top, skip, validationRuleID, context, showsql);
	}

	@Override
	public Response getModels(String filter) {
		IQueryConverter converter = IQueryConverter.getQueryConverter("DEFAULT");
		try {
			ConvertedQuery convertedStatement = converter.convertStatement(MRestView.Table_Name, filter);

			if (log.isLoggable(Level.INFO)) log.info("Where Clause: " + convertedStatement.getWhereClause());

			Query query = new Query(Env.getCtx(), MRestView.Table_Name, convertedStatement.getWhereClause(), null);
			query.setOnlyActiveRecords(true).setApplyAccessFilter(true);
			query.setParameters(convertedStatement.getParameters());

			List<MRestView> views = query.setOrderBy("REST_View.Name").list();
			JsonArray array = new JsonArray();
			for(MRestView view : views) {
				JsonObject json = new JsonObject();
				json.addProperty("id", view.getREST_View_ID());
				if (!Util.isEmpty(view.getREST_View_UU())) {
					json.addProperty("uid", view.getREST_View_UU());
				}
				json.addProperty("name", view.getName());
				MTable table = MTable.get(view.getAD_Table_ID());
				json.addProperty("tableName", table.getTableName().toLowerCase());
				
				if (!Util.isEmpty(view.getWhereClause())) {
					json.addProperty("whereClause", view.getWhereClause());
				}
				MRestViewColumn[] columns = view.getColumns();
				if (columns.length > 0) {
					JsonArray columnArray = new JsonArray();
					for(MRestViewColumn column : columns) {
						JsonObject columnJson = new JsonObject();
						columnJson.addProperty("id", column.get_ID());
						if (!Util.isEmpty(column.getREST_ViewColumn_UU())) {
							columnJson.addProperty("uid", column.getREST_ViewColumn_UU());
						}
						columnJson.addProperty("name", column.getName());
						MColumn tableColumn = MColumn.get(column.getAD_Column_ID());								
						columnJson.addProperty("columnName", tableColumn.getColumnName());
						String helpText = null;
						if (!Util.isEmpty(tableColumn.get_Translation(MColumn.COLUMNNAME_Help)))
							helpText = tableColumn.get_Translation(MColumn.COLUMNNAME_Help);
						else
							helpText = tableColumn.get_Translation(MColumn.COLUMNNAME_Description);
						if (!Util.isEmpty(helpText))
							columnJson.addProperty("help", helpText);
						if (column.getREST_ReferenceView_ID() > 0) {
							MRestView referenceView = MRestView.get(column.getREST_ReferenceView_ID());
							JsonObject refJson = new JsonObject();
							refJson.addProperty("id", referenceView.get_ID());
							if (!Util.isEmpty(referenceView.getREST_View_UU()))
								refJson.addProperty("uid", referenceView.getREST_View_UU());
							refJson.addProperty("name", referenceView.getName());
							columnJson.add("referenceView", refJson);
						}
						columnArray.add(columnJson);
					}
					json.add("columns", columnArray);
				}
				MRestViewRelated[] relateds = view.getRelatedViews();
				if (relateds.length > 0) {
					JsonArray relatedArray = new JsonArray();
					for(MRestViewRelated related : relateds) {
						JsonObject relatedJson = new JsonObject();
						relatedJson.addProperty("id", related.get_ID());
						if (!Util.isEmpty(related.getREST_ViewRelated_UU())) {
							relatedJson.addProperty("uid", related.getREST_ViewRelated_UU());
						}
						relatedJson.addProperty("name", related.getName());
						MRestView referenceView = MRestView.get(related.getREST_RelatedRestView_ID());
						JsonObject refJson = new JsonObject();
						refJson.addProperty("id", referenceView.get_ID());
						if (!Util.isEmpty(referenceView.getREST_View_UU()))
							refJson.addProperty("uid", referenceView.getREST_View_UU());
						refJson.addProperty("name", referenceView.getName());
						relatedJson.add("view", refJson);
						relatedJson.addProperty("autoExpand", related.isRestAutoExpand());
						relatedArray.add(relatedJson);
					}
					json.add("relatedViews", relatedArray);
				}
				array.add(json);
			}
			JsonObject json = new JsonObject();
			json.add("views", array);
			return Response.ok(json.toString()).build();			
		} catch (Exception ex) {
			return ResponseUtils.getResponseErrorFromException(ex, "GET Error");
		}	
	}

	@Override
	public Response create(String tableName, String jsonText) {
		return restView().create(tableName, jsonText);
	}

	@Override
	public Response update(String tableName, String id, String jsonText) {
		return restView().update(tableName, id, jsonText);
	}

	@Override
	public Response delete(String tableName, String id) {
		return restView().delete(tableName, id);
	}

	@Override
	public Response getAttachments(String tableName, String id) {
		return restView().getAttachments(tableName, id);
	}

	@Override
	public Response getAttachmentsAsZip(String tableName, String id, String asJson) {
		return restView().getAttachmentsAsZip(tableName, id, asJson);
	}

	@Override
	public Response createAttachmentsFromZip(String tableName, String id, String jsonText) {
		return restView().createAttachmentsFromZip(tableName, id, jsonText);
	}

	@Override
	public Response getAttachmentEntry(String tableName, String id, String fileName, String asJson) {
		return restView().getAttachmentEntry(tableName, id, fileName, asJson);
	}

	@Override
	public Response addAttachmentEntry(String tableName, String id, String jsonText) {
		return restView().addAttachmentEntry(tableName, id, jsonText);
	}

	@Override
	public Response deleteAttachments(String tableName, String id) {
		return restView().deleteAttachments(tableName, id);
	}

	@Override
	public Response deleteAttachmentEntry(String tableName, String id, String fileName) {
		return restView().deleteAttachmentEntry(tableName, id, fileName);
	}

	@Override
	public Response printModelRecord(String tableName, String id, String reportType) {
		return restView().printModelRecord(tableName, id, reportType);
	}

	@Override
	public Response getModelYAML(String tableName) {
		MRestView view = MRestView.get(tableName);
		
		StringBuilder header = new StringBuilder();
		header.append("openapi: 3.0.0\n");
		header.append("info:\n");
		header.append(" ".repeat(2)).append("title: views/").append(tableName).append("\n");
		header.append(" ".repeat(2)).append("version: 1.0.0\n");
		header.append("components:\n");
		header.append(" ".repeat(2)).append("schemas:\n");
		
		StringBuilder body = new StringBuilder();		
		buildYAMLForView(view, body);
		
		if (body.indexOf("#/components/schemas/Image") > 0) {
			YAMLSchema.addImageReference(header);
		}
		if (body.indexOf("#/components/schemas/Location") > 0) {
			YAMLSchema.addLocationReference(header, 4);			
		}
		
		addRelatedViews(view, header);
		
		body.append("paths:\n");
		body.append(" ".repeat(2)).append("/:\n");
		body.append(" ".repeat(4)).append("get:\n");
		body.append(" ".repeat(6)).append("responses:\n");
		body.append(" ".repeat(8)).append("'200':\n");
		body.append(" ".repeat(10)).append("description: dummy request\n");
		body.append(" ".repeat(10)).append("content:\n");
		body.append(" ".repeat(12)).append("application/json: {}\n");
						
		return Response.status(Status.OK).entity(header.append(body.toString()).toString()).build();
	}

	private void addRelatedViews(MRestView view, StringBuilder header) {
		MRestViewRelated[] relatedViews = view.getRelatedViews();
		for (MRestViewRelated relatedView : relatedViews) {
			if (relatedView.isRestAutoExpand()) {
				MRestView childView = MRestView.get(relatedView.getREST_RelatedRestView_ID()); 
				buildYAMLForView(childView, header);
				addRelatedViews(childView, header);
			}
		}
	}
	
	private void buildYAMLForView(MRestView view, StringBuilder body) {
		MTable table = MTable.get(view.getAD_Table_ID());
		
		body.append(" ".repeat(4)).append(view.getName()).append(":\n");
		body.append(" ".repeat(6)).append("type: object\n");
		body.append(" ".repeat(6)).append("properties:\n");
		if (table.getKeyColumns() != null && table.getKeyColumns().length == 1 && table.getKeyColumns()[0].endsWith("_ID")) {
			body.append(" ".repeat(8)).append("id:\n");
			body.append(" ".repeat(10)).append("type: integer\n");
			body.append(" ".repeat(10)).append("description: record id\n");
		}
		body.append(" ".repeat(8)).append("uid:\n");
		body.append(" ".repeat(10)).append("type: string\n");
		body.append(" ".repeat(10)).append("description: record uuid\n");
				
		YAMLSchema.addViewProperties(view, body, 8);
	}	
}

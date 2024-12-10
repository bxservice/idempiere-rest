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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.DatatypeConverter;

import org.adempiere.base.event.EventManager;
import org.adempiere.base.event.EventProperty;
import org.adempiere.base.event.IEventManager;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MTable;
import org.compiere.model.MWindow;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.compiere.wf.MWorkflow;
import org.osgi.service.event.Event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.json.ModelHelper;
import com.trekglobal.idempiere.rest.api.json.POParser;
import com.trekglobal.idempiere.rest.api.json.ResponseUtils;
import com.trekglobal.idempiere.rest.api.json.RestUtils;
import com.trekglobal.idempiere.rest.api.json.TypeConverterUtils;
import com.trekglobal.idempiere.rest.api.json.expand.ExpandParser;
import com.trekglobal.idempiere.rest.api.json.expand.ExpandUtils;
import com.trekglobal.idempiere.rest.api.json.filter.ConvertedQuery;
import com.trekglobal.idempiere.rest.api.json.filter.IQueryConverter;
import com.trekglobal.idempiere.rest.api.model.MRestView;
import com.trekglobal.idempiere.rest.api.model.MRestViewRelated;
import com.trekglobal.idempiere.rest.api.v1.resource.ModelResource;
import com.trekglobal.idempiere.rest.api.v1.resource.WindowResource;
import com.trekglobal.idempiere.rest.api.v1.resource.file.FileStreamingOutput;

/**
 * @author hengsin
 *
 */
public class ModelResourceImpl implements ModelResource {

	private final static CLogger log = CLogger.getCLogger(ModelResourceImpl.class);
	
	public static final String PO_BEFORE_REST_SAVE = "idempiere-rest/po/beforeSave";
	public static final String PO_AFTER_REST_SAVE = "idempiere-rest/po/afterSave";

	/**
	 * default constructor
	 */
	public ModelResourceImpl() {
	}

	@Override
	public Response getPO(String tableName, String id, String details, String select, String showsql) {
		return getPO(tableName, id, details, select, null, showsql);
	}
	
	/**
	 * 
	 * @param tableName table or rest view definition name
	 * @param id id or uuid
	 * @param details child/link entity
	 * @param multiProperty comma separated columns
	 * @param singleProperty single column
	 * @return
	 */
	private Response getPO(String tableName, String id, String details, String multiProperty, String singleProperty, String showsql) {
		try {
			MRestView view = RestUtils.getViewAndCheckAccess(tableName, false);
			if (view != null)
				tableName = MTable.getTableName(Env.getCtx(), view.getAD_Table_ID());
			
			Query query = RestUtils.getQuery(tableName, id, true, false);
			PO po = query.first();

			POParser poParser = new POParser(tableName, id, po);
			if (poParser.isValidPO()) {
				IPOSerializer serializer = IPOSerializer.getPOSerializer(tableName, po.getClass());
				String[] includes = null;
				if (!Util.isEmpty(multiProperty, true)) {
					if (view != null)
						multiProperty = toColumnNames(view, multiProperty);
					includes = RestUtils.getSelectedColumns(tableName, multiProperty);
				} else if (!Util.isEmpty(singleProperty, true)) {
					if (view != null) {
						String original = singleProperty;
						singleProperty = toColumnNames(view, singleProperty);
						if (Util.isEmpty(singleProperty, true))
							return ResponseUtils.getResponseError(Status.NOT_FOUND, "Invalid property name", "No match found for table name: ", original);
					}
					if (po.get_Value(singleProperty) == null) {
						return ResponseUtils.getResponseError(Status.NOT_FOUND, "Invalid property name", "No match found for table name: ", singleProperty);
					}
					includes = new String[] {singleProperty};
				}
				JsonObject json;
				boolean showData = (showsql == null || !"nodata".equals(showsql));
				if (showData)
					json = serializer.toJson(po, view, includes, null);
				else
					json = new JsonObject();

				if (showsql != null) {
					json.addProperty("sql-command", DB.getDatabase().convertStatement(query.getSQL()));
				}
				if (!Util.isEmpty(details, true)) {
					expandDetailsInJsonObject(po, view, json, json, details, showsql != null, showData);
				} else if (view != null) {
					//add auto expand detail view definition
					MRestViewRelated[] relateds = view.getRelatedViews();
					if (relateds != null && relateds.length > 0) {
						StringBuilder expands = new StringBuilder();
						for (MRestViewRelated related : relateds) {
							if (related.isRestAutoExpand()) {
								if (expands.length() > 0)
									expands.append(",");
								expands.append(related.getName());
							}
						}
						if (expands.length() > 0) {
							expandDetailsInJsonObject(po, view, json, json, expands.toString(), showsql != null, showData);
						}
					}
				}

				return Response.ok(json.toString()).build();
			} else {
				return poParser.getResponseError();
			}
		} catch(Exception ex) {
			return ResponseUtils.getResponseErrorFromException(ex, "GET Error");
		}
	}
	
	/**
	 * Convert view property name to table column name (if needed)
	 * @param view
	 * @param propertyNames comma separated list of view property or table column name
	 * @return converted names
	 */
	private String toColumnNames(MRestView view, String propertyNames) {
		String[] columns = propertyNames.split("[,]");
		columns = view.toColumnNames(columns, true);
		return String.join(",", columns);
	}

	private void expandDetailsInJsonObject(PO po, MRestView view, JsonObject masterJsonObject, JsonObject detailJsonObject, String expandParameter, boolean showSql, boolean showData) {
		ExpandParser expandParser = new ExpandParser(po, view, expandParameter);
		if (showSql)
			ExpandUtils.addDetailSQLCommandToJson(expandParser.getTableNameSQLStatementMap(), masterJsonObject);
		
		if (showData)
			ExpandUtils.addDetailDataToJson(expandParser.getTableNameChildArrayMap(), detailJsonObject);			
	}
	
	@Override
	public Response getPOProperty(String tableName, String id, String propertyName, String showsql) {
		return getPO(tableName, id, null, null, propertyName, showsql);
	}

	@Override
	public Response getModels(String filter) {
		IQueryConverter converter = IQueryConverter.getQueryConverter("DEFAULT");
		try {
			ConvertedQuery convertedStatement = converter.convertStatement(MTable.Table_Name, filter);

			if (log.isLoggable(Level.INFO)) log.info("Where Clause: " + convertedStatement.getWhereClause());

			Query query = new Query(Env.getCtx(), MTable.Table_Name, convertedStatement.getWhereClause(), null);
			query.setOnlyActiveRecords(true).setApplyAccessFilter(true);
			query.setParameters(convertedStatement.getParameters());

			List<MTable> tables = query.setOrderBy("AD_Table.TableName").list();
			JsonArray array = new JsonArray();
			for(MTable table : tables) {
				if (RestUtils.hasAccess(table, false)) {
					JsonObject json = new JsonObject();
					json.addProperty("id", table.getAD_Table_ID());
					if (!Util.isEmpty(table.getAD_Table_UU())) {
						json.addProperty("uid", table.getAD_Table_UU());
					}
					json.addProperty("model-name", table.getTableName().toLowerCase());
					json.addProperty("name", table.getName());
					if (!Util.isEmpty(table.getDescription())) {
						json.addProperty("description", table.getDescription());
					}
					array.add(json);
				}
			}
			JsonObject json = new JsonObject();
			json.add("models", array);
			return Response.ok(json.toString()).build();			
		} catch (Exception ex) {
			return ResponseUtils.getResponseErrorFromException(ex, "GET Error");
		}

	}

	@Override
	public Response getPOs(String tableName, String details, String filter, String order, String select, int top, int skip,
			String validationRuleID, String context, String showsql) {
		try {
			MRestView view = RestUtils.getViewAndCheckAccess(tableName, false);
			if (view != null)
				tableName = MTable.getTableName(Env.getCtx(), view.getAD_Table_ID());
			
			ModelHelper modelHelper = new ModelHelper(tableName, filter, order, top, skip, validationRuleID, context);
			if (view != null)
				modelHelper.setView(view);
			List<PO> list = modelHelper.getPOsFromRequest();
			
			JsonArray array = new JsonArray();
			if (list != null) {
				IPOSerializer serializer = IPOSerializer.getPOSerializer(tableName, MTable.getClass(tableName));
				String[] includes = RestUtils.getSelectedColumns(tableName, select);
				if (view != null)
					includes = view.toColumnNames(includes, true);

				boolean showData = (showsql == null || !"nodata".equals(showsql));
				JsonObject json = new JsonObject();
				json.addProperty("page-count", modelHelper.getPageCount());
				json.addProperty("records-size", modelHelper.getTop());
				json.addProperty("skip-records", modelHelper.getSkip());
				json.addProperty("row-count", modelHelper.getRowCount());
				json.addProperty("array-count", array.size());
				if (showsql != null) {
					json.addProperty("sql-command", DB.getDatabase().convertStatement(modelHelper.getSQLStatement()));
				}
				
				for (PO po : list) {
					JsonObject detailJson = serializer.toJson(po, view, includes, null);
					if (!Util.isEmpty(details, true))
						expandDetailsInJsonObject(po, view, json, detailJson, details, showsql != null, showData);
					array.add(detailJson);
				}
				
				if (showData)
					json.add("records", array);
				
				return Response.ok(json.toString())
						.header("X-Page-Count", modelHelper.getPageCount())
						.header("X-Records-Size", modelHelper.getTop())
						.header("X-Skip-Records", modelHelper.getSkip())
						.header("X-Row-Count", modelHelper.getRowCount())
						.header("X-Array-Count", array.size())
						.build();
			} else {
				JsonObject json = new JsonObject();
				json.add("records", array);
				return Response.ok(json.toString()).build();
			}
		} catch (Exception ex) {
			return ResponseUtils.getResponseErrorFromException(ex, "GET Error");
		}
	}
	
	@Override
	public Response create(String tableName, String jsonText) {
		Trx trx = Trx.get(Trx.createTrxName(), true);
		try {
			MRestView view = RestUtils.getViewAndCheckAccess(tableName, false);
			if (view != null)
				tableName = MTable.getTableName(Env.getCtx(), view.getAD_Table_ID());
			
			MTable table = RestUtils.getTableAndCheckAccess(tableName, true);

			trx.start();
			Gson gson = new GsonBuilder().create();
			JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
			IPOSerializer serializer = IPOSerializer.getPOSerializer(tableName, MTable.getClass(tableName));
			PO po = serializer.fromJson(jsonObject, table, view);
			if (!RestUtils.hasRoleUpdateAccess(po.getAD_Client_ID(), po.getAD_Org_ID(), po.get_Table_ID(), 0, true))
				return ResponseUtils.getResponseError(Status.FORBIDDEN, "Update error", "Role does not have access","");

			po.set_TrxName(trx.getTrxName());
			fireRestSaveEvent(po, PO_BEFORE_REST_SAVE, true);
			try {
				if (! po.validForeignKeys()) {
					String msg = CLogger.retrieveErrorString("Foreign key validation error");
					throw new AdempiereException(msg);
				}
				po.saveEx();
				fireRestSaveEvent(po, PO_AFTER_REST_SAVE, true);
			} catch (Exception ex) {
				trx.rollback();
				return ResponseUtils.getResponseErrorFromException(ex, "Save error");
			}
			Map<String, JsonArray> detailMap = new LinkedHashMap<>();
			Set<String> fields = jsonObject.keySet();
			for(String field : fields) {
				String strError = createChild(field, jsonObject, po, view, detailMap, trx);
				if(strError != null)
					return ResponseUtils.getResponseError(Status.INTERNAL_SERVER_ERROR, "Save error", "Save error with exception: ", strError);
			}

			StringBuilder processMsg = new StringBuilder();
			String processError = runDocAction(po, jsonObject, processMsg);
			if (!Util.isEmpty(processError, true)) {
				trx.rollback();
				log.warning("Encounter exception during execution of document action in REST: " + processError);
				return ResponseUtils.getResponseError(Status.INTERNAL_SERVER_ERROR, Msg.getMsg(po.getCtx(), "FailedProcessingDocument"), processError, "");
			}
			trx.commit(true);
			po.load(trx.getTrxName());
			jsonObject = serializer.toJson(po, view);
			if (processMsg.length() > 0)
				jsonObject.addProperty("doc-processmsg", processMsg.toString());
			if (detailMap.size() > 0) {
				for(String childTableName : detailMap.keySet()) {
					JsonArray childArray = detailMap.get(childTableName);
					jsonObject.add(childTableName, childArray);
				}
			}
			return Response.status(Status.CREATED).entity(jsonObject.toString()).build();
		} catch (Exception ex) {
			trx.rollback();
			return ResponseUtils.getResponseErrorFromException(ex, "Server error");
		} finally {
			trx.close();
		}
	}

	/**
	 * Recursive Method to Create Children
	 * @param field
	 * @param jsonObject
	 * @param po
	 * @param view 
	 * @param detailMap
	 * @param trx
	 * @return
	 */
	private String createChild(String field, JsonObject jsonObject, PO po, MRestView view, Map<String, JsonArray> detailMap, Trx trx) {
		JsonElement fieldElement = jsonObject.get(field);
		if (fieldElement != null && fieldElement.isJsonArray()) {
			String childTableName = field;
			MRestView childView = null;
			if (view != null) {
				//find child view definition
				MRestViewRelated[] relateds = view.getRelatedViews();
				for(MRestViewRelated related : relateds) {
					MRestView relatedView = new MRestView(Env.getCtx(), related.getREST_RelatedRestView_ID(), null);
					RestUtils.checkViewAccess(relatedView, true);
					String tableName = MTable.getTableName(Env.getCtx(), relatedView.getAD_Table_ID());
					if (related.getName().equals(field)) {						
						childTableName = tableName;
						childView = relatedView;
						break;
					} else if (tableName.equals(field)) {
						childView = relatedView;
						break;
					}
				}
				if (childView == null)
					return null;
			}
			MTable childTable = MTable.get(Env.getCtx(), childTableName);
			if (childTable != null && childTable.getAD_Table_ID() > 0) {
				IPOSerializer childSerializer = IPOSerializer.getPOSerializer(field, MTable.getClass(field));
				JsonArray fieldArray = fieldElement.getAsJsonArray();
				JsonArray savedArray = new JsonArray();
				try {
					MRestView finalChildView = childView;
					fieldArray.forEach(e -> {
						if (e.isJsonObject()) {
							JsonObject childJsonObject = e.getAsJsonObject();
							PO childPO = childSerializer.fromJson(childJsonObject, childTable, finalChildView);
							if (!RestUtils.hasRoleUpdateAccess(childPO.getAD_Client_ID(), childPO.getAD_Org_ID(), childPO.get_Table_ID(), 0, true))
								throw new AdempiereException("AccessCannotUpdate");
							
							childPO.set_TrxName(trx.getTrxName());
							childPO.set_ValueOfColumn(RestUtils.getKeyColumnName(po.get_TableName()), po.get_ID());
							fireRestSaveEvent(childPO, PO_BEFORE_REST_SAVE, true);
							if (! childPO.validForeignKeys()) {
								String msg = CLogger.retrieveErrorString("Foreign key validation error");
								throw new AdempiereException(msg);
							}
							childPO.saveEx();
							fireRestSaveEvent(childPO, PO_AFTER_REST_SAVE, true);
							childJsonObject = childSerializer.toJson(childPO, finalChildView);
							JsonObject newChildJsonObject = e.getAsJsonObject();
							Map<String, JsonArray> childDetailMap = new LinkedHashMap<>();
							Set<String> fields = newChildJsonObject.keySet();
							for(String childField : fields) {
								String strError = createChild(childField, newChildJsonObject, childPO, finalChildView, childDetailMap, trx);
								if(strError != null)
									throw new AdempiereException(strError);
							}
							if (childDetailMap.size() > 0) {
								for(String tableName : childDetailMap.keySet()) {
									JsonArray childArray = childDetailMap.get(tableName);
									childJsonObject.add(tableName, childArray);
								}
							}
							savedArray.add(childJsonObject);
							StringBuilder processMsg = new StringBuilder();
							String processError = runDocAction(childPO, newChildJsonObject, processMsg);
							if(processError != null)
								throw new AdempiereException(processError + " - " + processMsg != null ? processMsg.toString() : "");
						}
					});
					if (savedArray.size() > 0)
						detailMap.put(field, savedArray);
				} catch (Exception ex) {
					trx.rollback();
					log.log(Level.SEVERE, ex.getMessage(), ex);
					return ex.getMessage();
				}
			}
		}
		return null;
	}

	@Override
	public Response update(String name, String id, String jsonText) {
		MRestView view = RestUtils.getViewAndCheckAccess(name, false);
		if (view != null)
			name = MTable.getTableName(Env.getCtx(), view.getAD_Table_ID());
		
		String tableName = name;
		POParser poParser = new POParser(tableName, id, true, true);
		if (!poParser.isValidPO()) {
			return poParser.getResponseError();
		}
		
		PO po = poParser.getPO();
		if (!RestUtils.hasRoleUpdateAccess(po.getAD_Client_ID(), po.getAD_Org_ID(), po.get_Table_ID(), po.get_ID(), false))
			return ResponseUtils.getResponseError(Status.FORBIDDEN, "Update error", "Role does not have access","");

		Trx trx = Trx.get(Trx.createTrxName(), true);
		try {

			trx.start();
			Gson gson = new GsonBuilder().create();
			JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
			IPOSerializer serializer = IPOSerializer.getPOSerializer(tableName, MTable.getClass(tableName));
			po = serializer.fromJson(jsonObject, po, view);
			po.set_TrxName(trx.getTrxName());
			fireRestSaveEvent(po, PO_BEFORE_REST_SAVE, false);
			try {
				if (! po.validForeignKeys()) {
					String msg = CLogger.retrieveErrorString("Foreign key validation error");
					throw new AdempiereException(msg);
				}
				po.saveEx();
				fireRestSaveEvent(po, PO_AFTER_REST_SAVE, false);
			} catch (Exception ex) {
				trx.rollback();
				return ResponseUtils.getResponseErrorFromException(ex, "Save error");
			}
			
			Map<String, JsonArray> detailMap = new LinkedHashMap<>();
			Set<String> fields = jsonObject.keySet();
			final int parentId = po.get_ID();
			for(String field : fields) {
				JsonElement fieldElement = jsonObject.get(field);
				if (fieldElement != null && fieldElement.isJsonArray()) {					
					MRestView childView = null;
					if (view != null) {
						//find child view definition
						MRestViewRelated[] relateds = view.getRelatedViews();
						for(MRestViewRelated related : relateds) {
							MRestView relatedView = new MRestView(Env.getCtx(), related.getREST_RelatedRestView_ID(), null);
							RestUtils.checkViewAccess(relatedView, true);
							String tName = MTable.getTableName(Env.getCtx(), relatedView.getAD_Table_ID());
							if (related.getName().equals(field)) {						
								childView = relatedView;
								break;
							} else if (tName.equals(field)) {
								childView = relatedView;
								break;
							}
						}
						if (childView == null)
							continue;
					}
					String childTableName = childView != null ? MTable.getTableName(Env.getCtx(), childView.getAD_Table_ID()) : field;
					MTable childTable = MTable.get(Env.getCtx(), childTableName);
					if (childTable != null && childTable.getAD_Table_ID() > 0) {									
						IPOSerializer childSerializer = IPOSerializer.getPOSerializer(childTableName, MTable.getClass(childTableName));
						JsonArray fieldArray = fieldElement.getAsJsonArray();
						JsonArray savedArray = new JsonArray();
						MRestView finalChildView = childView;
						try {
							fieldArray.forEach(e -> {
								if (e.isJsonObject()) {
									JsonObject childJsonObject = e.getAsJsonObject();
									PO childPO = loadPO(childTableName, childJsonObject);
									
									if (childPO == null) {
										childPO = childSerializer.fromJson(childJsonObject, childTable, finalChildView);
										childPO.set_ValueOfColumn(RestUtils.getKeyColumnName(tableName), parentId);
									} else {
										childPO = childSerializer.fromJson(childJsonObject, childPO, finalChildView);
									}
									childPO.set_TrxName(trx.getTrxName());
									fireRestSaveEvent(childPO, PO_BEFORE_REST_SAVE, false);
									if (! childPO.validForeignKeys()) {
										String msg = CLogger.retrieveErrorString("Foreign key validation error");
										throw new AdempiereException(msg);
									}
									childPO.saveEx();
									fireRestSaveEvent(childPO, PO_AFTER_REST_SAVE, false);
									childJsonObject = serializer.toJson(childPO, finalChildView);
									savedArray.add(childJsonObject);
								}
							});
							if (savedArray.size() > 0)
								detailMap.put(field, savedArray);
						} catch (Exception ex) {
							trx.rollback();
							return ResponseUtils.getResponseErrorFromException(ex, "Save error");
						}
					}
				}
			}

			StringBuilder processMsg = new StringBuilder();
			String error = runDocAction(po, jsonObject, processMsg);
			if (Util.isEmpty(error, true)) {
				trx.commit(true);
			} else {
				trx.rollback();
				log.warning("Encounter exception during execution of document action in REST: " + error);
				return ResponseUtils.getResponseError(Status.INTERNAL_SERVER_ERROR, Msg.getMsg(po.getCtx(), "FailedProcessingDocument"), error, "");
			}
			
			po.load(trx.getTrxName());
			jsonObject = serializer.toJson(po, view);
			if (processMsg.length() > 0)
				jsonObject.addProperty("doc-processmsg", processMsg.toString());
			if (detailMap.size() > 0) {
				for(String field : detailMap.keySet()) {
					JsonArray child = detailMap.get(field);
					jsonObject.add(field, child);
				}
			}
			return Response.status(Status.OK).entity(jsonObject.toString()).build();
		} catch (Exception ex) {
			trx.rollback();
			return ResponseUtils.getResponseErrorFromException(ex, "Update error");
		} finally {
			trx.close();
		}
	}

	/**
	 * Fire the PO_BEFORE_REST_SAVE/PO_AFTER_REST_SAVE event, to catch and manipulate the object before the model beforeSave/afterSave
	 * @param po
	 */
	private void fireRestSaveEvent(PO po, String topic, boolean isNew) {
		Event event = EventManager.newEvent(topic,
				new EventProperty(EventManager.EVENT_DATA, po), new EventProperty("tableName", po.get_TableName()),
				new EventProperty("isNew", isNew));
		EventManager.getInstance().sendEvent(event);
		@SuppressWarnings("unchecked")
		List<String> errors = (List<String>) event.getProperty(IEventManager.EVENT_ERROR_MESSAGES);
		if (errors != null && !errors.isEmpty())
			throw new AdempiereException(errors.get(0));
	}

	@Override
	public Response delete(String tableName, String id) {
		MRestView view = RestUtils.getViewAndCheckAccess(tableName, false);
		if (view != null)
			tableName = MTable.getTableName(Env.getCtx(), view.getAD_Table_ID());
		
		POParser poParser = new POParser(tableName, id, true, true);
		if (poParser.isValidPO()) {
			PO po = poParser.getPO();
			if (!RestUtils.hasRoleUpdateAccess(po.getAD_Client_ID(), po.getAD_Org_ID(), po.get_Table_ID(), 0, true)) {
				return ResponseUtils.getResponseError(Status.FORBIDDEN, "Delete error", "AccessCannotDelete","");
			}

			try {
				po.deleteEx(true);
				JsonObject json = new JsonObject();
				json.addProperty("msg", Msg.getMsg(Env.getCtx(), "Deleted"));
				return Response.ok(json.toString()).build();
			} catch (Exception ex) {
				return ResponseUtils.getResponseErrorFromException(ex, "Delete error");
			}
		} else {
			return poParser.getResponseError();
		}
	}

	@Override
	public Response getAttachments(String tableName, String id) {
		MRestView view = RestUtils.getViewAndCheckAccess(tableName, false);
		if (view != null)
			tableName = MTable.getTableName(Env.getCtx(), view.getAD_Table_ID());
		
		JsonArray array = new JsonArray();
		POParser poParser = new POParser(tableName, id, true, false);
		if (poParser.isValidPO()) {
			PO po = poParser.getPO();
			MAttachment attachment = po.getAttachment();
			if (attachment != null) {
				for(MAttachmentEntry entry : attachment.getEntries()) {
					JsonObject entryJsonObject = new JsonObject();
					entryJsonObject.addProperty("name", entry.getName());
					if (!Util.isEmpty(entry.getContentType(),  true))
						entryJsonObject.addProperty("contentType", entry.getContentType());
					array.add(entryJsonObject);
				}
			}
			JsonObject json = new JsonObject();
			json.add("attachments", array);
			return Response.ok(json.toString()).build();
		} else {
			return poParser.getResponseError();
		}
	}

	@Override
	public Response getAttachmentsAsZip(String tableName, String id, String asJson) {
		MRestView view = RestUtils.getViewAndCheckAccess(tableName, false);
		if (view != null)
			tableName = MTable.getTableName(Env.getCtx(), view.getAD_Table_ID());
		
		POParser poParser = new POParser(tableName, id, true, false);
		if (poParser.isValidPO()) {
			PO po = poParser.getPO();
			MAttachment attachment = po.getAttachment();
			if (attachment != null) {
				try {
					File zipFile = attachment.saveAsZip();
					if (zipFile != null) {
						if (asJson == null) {
							FileStreamingOutput fso = new FileStreamingOutput(zipFile);
							return Response.ok(fso).build();
						} else {
							JsonObject json = new JsonObject();
							byte[] binaryData = Files.readAllBytes(zipFile.toPath());
							String data = Base64.getEncoder().encodeToString(binaryData);
							json.addProperty("data", data);
							return Response.ok(json.toString()).build();
						}
					}
				} catch (IOException ex) {
					return ResponseUtils.getResponseErrorFromException(ex, "IO error");
				}
			}
			return Response.status(Status.NO_CONTENT).build();
		} else {
			return poParser.getResponseError();
		}
	}

	@Override
	public Response createAttachmentsFromZip(String tableName, String id, String jsonText) {
		Gson gson = new GsonBuilder().create();
		JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
		
		boolean overwrite = false;
		JsonElement jsonElement = jsonObject.get("overwrite");
		if (jsonElement != null && jsonElement.isJsonPrimitive())
			overwrite = jsonElement.getAsBoolean();
		
		jsonElement = jsonObject.get("data");
		if (jsonElement == null || !jsonElement.isJsonPrimitive())
			return ResponseUtils.getResponseError(Status.BAD_REQUEST, "data property is mandatory", "", "");

		String base64Content = jsonElement.getAsString();
		if (Util.isEmpty(base64Content, true))
			return ResponseUtils.getResponseError(Status.BAD_REQUEST, "data property is mandatory", "", "");
		
		MRestView view = RestUtils.getViewAndCheckAccess(tableName, false);
		if (view != null)
			tableName = MTable.getTableName(Env.getCtx(), view.getAD_Table_ID());
		
		POParser poParser = new POParser(tableName, id, true, false);
		if (poParser.isValidPO()) {
			PO po = poParser.getPO();
			byte[] data = DatatypeConverter.parseBase64Binary(base64Content);
			if (data == null || data.length == 0)
				return ResponseUtils.getResponseError(Status.BAD_REQUEST, "Can't parse data", "Can't parse data in Json content, not base64 encoded", "");

			MAttachment attachment = po.getAttachment();
			if (attachment == null)
				attachment = po.createAttachment();
			
			try (ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(data))) {
	            ZipEntry entry;
	            while ((entry = stream.getNextEntry()) != null) {
	            	String name = entry.getName();
	            	for(int i = 0; i < attachment.getEntryCount(); i++) {
	    				MAttachmentEntry e = attachment.getEntry(i);
	    				if (e.getName().equals(name)) {
	    					if (overwrite) {
	    						attachment.deleteEntry(i);
	    						break;
	    					} else {
	    						return ResponseUtils.getResponseError(Status.CONFLICT, "Duplicate file name", "Duplicate file name: ", name);
	    					}
	    				}
	    			}
	            	
	            	byte[] buffer = new byte[2048];
	            	ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    int len;
                    while ((len = stream.read(buffer)) > 0) {
                        bos.write(buffer, 0, len);
                    }
                    attachment.addEntry(name, bos.toByteArray());
	            }
	            attachment.saveEx();
	        } catch (Exception ex) {
				return ResponseUtils.getResponseErrorFromException(ex, "Create attachment error");
			}
															
			return Response.status(Status.CREATED).build();
		} else {
			return poParser.getResponseError();
		}
	}

	@Override
	public Response getAttachmentEntry(String tableName, String id, String fileName, String asJson) {
	
		MRestView view = RestUtils.getViewAndCheckAccess(tableName, false);
		if (view != null)
			tableName = MTable.getTableName(Env.getCtx(), view.getAD_Table_ID());
		
		POParser poParser = new POParser(tableName, id, true, false);
		if (poParser.isValidPO()) {
			PO po = poParser.getPO();
			MAttachment attachment = po.getAttachment();
			if (attachment != null) {
				for(MAttachmentEntry entry : attachment.getEntries()) {
					if (entry.getName().equals(fileName)) {
						try {
							Path tempPath = Files.createTempDirectory(tableName);
							File tempFolder = tempPath.toFile();
							File zipFile = new File(tempFolder, fileName);
							zipFile = entry.getFile(zipFile);
							if (asJson == null) {
								FileStreamingOutput fso = new FileStreamingOutput(zipFile);
								return Response.ok(fso).build();
							} else {
								JsonObject json = new JsonObject();
								byte[] binaryData = Files.readAllBytes(zipFile.toPath());
								String data = Base64.getEncoder().encodeToString(binaryData);
								json.addProperty("data", data);
								return Response.ok(json.toString()).build();
							}
						} catch (IOException ex) {
							return ResponseUtils.getResponseErrorFromException(ex, "IO error");
						}
					}
				}
			}
			return Response.status(Status.NO_CONTENT).build();
		} else {
			return poParser.getResponseError();
		}
	}

	@Override
	public Response addAttachmentEntry(String tableName, String id, String jsonText) {
		Gson gson = new GsonBuilder().create();
		JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
		
		JsonElement jsonElement = jsonObject.get("name");
		if (jsonElement == null || !jsonElement.isJsonPrimitive())
			return ResponseUtils.getResponseError(Status.BAD_REQUEST, "name property is mandatory", "", "");

		String fileName = jsonElement.getAsString();
		if (Util.isEmpty(fileName, true))
			return ResponseUtils.getResponseError(Status.BAD_REQUEST, "name property is mandatory", "", "");
		
		jsonElement = jsonObject.get("data");
		if (jsonElement == null || !jsonElement.isJsonPrimitive())
			return ResponseUtils.getResponseError(Status.BAD_REQUEST, "data property is mandatory", "", "");

		String base64Content = jsonElement.getAsString();
		if (Util.isEmpty(base64Content, true))
			return ResponseUtils.getResponseError(Status.BAD_REQUEST, "data property is mandatory", "", "");
		
		boolean overwrite = false;
		jsonElement = jsonObject.get("overwrite");
		if (jsonElement != null && jsonElement.isJsonPrimitive())
			overwrite = jsonElement.getAsBoolean();
		
		MRestView view = RestUtils.getViewAndCheckAccess(tableName, false);
		if (view != null)
			tableName = MTable.getTableName(Env.getCtx(), view.getAD_Table_ID());
		
		POParser poParser = new POParser(tableName, id, true, false);
		if (poParser.isValidPO()) {
			PO po = poParser.getPO();
			byte[] data = DatatypeConverter.parseBase64Binary(base64Content);
			if (data == null || data.length == 0)
				return ResponseUtils.getResponseError(Status.BAD_REQUEST, "Can't parse data", "Can't parse data in Json content, not base64 encoded", "");

			MAttachment attachment = po.getAttachment();
			if (attachment == null)
				attachment = po.createAttachment();
			
			for(int i = 0; i < attachment.getEntryCount(); i++) {
				MAttachmentEntry entry = attachment.getEntry(i);
				if (entry.getName().equals(fileName)) {
					if (overwrite) {
						attachment.deleteEntry(i);
						break;
					} else {
						return ResponseUtils.getResponseError(Status.CONFLICT, "Duplicate file name", "Duplicate file name: ", fileName);
					}
				}
			}		
			
			try {
				attachment.addEntry(fileName, data);
				attachment.saveEx();
			} catch (Exception ex) {
				return ResponseUtils.getResponseErrorFromException(ex, "Save error");
			}
			return Response.status(Status.CREATED).build();
		} else {
			return poParser.getResponseError();
		}
	}

	@Override
	public Response deleteAttachments(String tableName, String id) {
		MRestView view = RestUtils.getViewAndCheckAccess(tableName, false);
		if (view != null)
			tableName = MTable.getTableName(Env.getCtx(), view.getAD_Table_ID());
		
		POParser poParser = new POParser(tableName, id, true, false);
		if (poParser.isValidPO()) {
			PO po = poParser.getPO();
			MAttachment attachment = po.getAttachment();
			if (attachment != null) {
				try {
					attachment.deleteEx(true);
				} catch (Exception ex) {
					return ResponseUtils.getResponseErrorFromException(ex, "Delete error");
				}
				JsonObject json = new JsonObject();
				json.addProperty("msg", Msg.getMsg(Env.getCtx(), "Deleted"));
				return Response.ok(json.toString()).build();
			} else {
				return ResponseUtils.getResponseError(Status.NOT_FOUND, "No attachments", "No attachment is found for record with id ", id);
			}
		} else {
			return poParser.getResponseError();
		}
	}

	@Override
	public Response deleteAttachmentEntry(String tableName, String id, String fileName) {
		MRestView view = RestUtils.getViewAndCheckAccess(tableName, false);
		if (view != null)
			tableName = MTable.getTableName(Env.getCtx(), view.getAD_Table_ID());
		
		POParser poParser = new POParser(tableName, id, true, false);
		if (poParser.isValidPO()) {
			PO po = poParser.getPO();
			MAttachment attachment = po.getAttachment();
			if (attachment != null) {
				int i = 0;
				for(MAttachmentEntry entry : attachment.getEntries()) {
					if (entry.getName().equals(fileName)) {
						if (attachment.deleteEntry(i)) {
							try {
								attachment.saveEx();
							} catch (Exception ex) {
								return ResponseUtils.getResponseErrorFromException(ex, "Delete error");
							}
							JsonObject json = new JsonObject();
							json.addProperty("msg", Msg.getMsg(Env.getCtx(), "Deleted"));
							return Response.ok(json.toString()).build();
						} else {
							return ResponseUtils.getResponseError(Status.INTERNAL_SERVER_ERROR, "Fail to remove attachment entry", "", "");
						}
					}
					i++;
				}
				return ResponseUtils.getResponseError(Status.NOT_FOUND, "No matching attachment entry", "No attachment entry is found for name ", fileName);
			} else {
				return ResponseUtils.getResponseError(Status.NOT_FOUND, "No attachments", "No attachment is found for record with id ", id);
			}
		} else {
			return poParser.getResponseError();
		}
	}
	
	@Override
	public Response printModelRecord(String tableName, String id, String reportType) {
		MRestView view = RestUtils.getViewAndCheckAccess(tableName, false);
		if (view != null)
			tableName = MTable.getTableName(Env.getCtx(), view.getAD_Table_ID());
		
		POParser poParser = new POParser(tableName, id, true, true);
		if (poParser.isValidPO()) {
			PO po = poParser.getPO();
			try {
				MTable table = RestUtils.getTableAndCheckAccess(tableName, true);
				int windowId = Env.getZoomWindowID(table.get_ID(), po.get_ID());
				if (windowId == 0)
					return ResponseUtils.getResponseError(Status.NOT_FOUND, "Window not found", "No valid window found for table name: ", tableName);
				
				MWindow window = MWindow.get(Env.getCtx(), windowId);
				String windowSlug = TypeConverterUtils.slugify(window.getName());
				WindowResource windowResource = new WindowResourceImpl();
				return windowResource.printWindowRecord(windowSlug, po.get_ID(), reportType);
			} catch (Exception ex) {
				return ResponseUtils.getResponseErrorFromException(ex, "Print model error");
			}
		} else {
			return poParser.getResponseError();
		}
	}
	
	private PO loadPO(String tableName, JsonObject jsonObject) {
		PO po = null;
		String idColumn = RestUtils.getKeyColumnName(tableName);
		String uidColumn = PO.getUUIDColumnName(tableName);
		JsonElement idElement = jsonObject.get("id");											
		if (idElement != null && idElement.isJsonPrimitive()) {
			Query query = new Query(Env.getCtx(), tableName, idColumn + "=?", null);
			query.setApplyAccessFilter(true, false);
			po = query.setParameters(idElement.getAsInt()).first();
		}
		else {
			JsonElement uidElement = jsonObject.get("uid");
			if (uidElement != null && uidElement.isJsonPrimitive()) {
				Query query = new Query(Env.getCtx(), tableName, uidColumn + "=?", null);
				query.setApplyAccessFilter(true, false);
				po = query.setParameters(uidElement.getAsString()).first();
			}
		}
		return po;
	}

	private String runDocAction(PO po, JsonObject jsonObject, StringBuilder processMsg) {
		if (po instanceof DocAction) {
			JsonElement docActionElement = jsonObject.get("doc-action");
			if (docActionElement != null) {
				String docAction = null;
				if (docActionElement.isJsonPrimitive()) {
					docAction = docActionElement.getAsString();
				} else if (docActionElement.isJsonObject()) {
					JsonObject docActionJsonObject = docActionElement.getAsJsonObject();
					docActionElement = docActionJsonObject.get("id");
					if (docActionElement != null && docActionElement.isJsonPrimitive()) {
						docAction = docActionElement.getAsString();
					}
				}
				if (!Util.isEmpty(docAction, true) && !DocAction.ACTION_None.equals(docAction)) {
					ProcessInfo processInfo = MWorkflow.runDocumentActionWorkflow(po, docAction);
					if (processInfo.isError()) {
						return Msg.parseTranslation(po.getCtx(), processInfo.getSummary());
					} else {
						try {
							po.saveEx();
						} catch (Exception ex) {
							log.log(Level.SEVERE, ex.getMessage(), ex);
							return ex.getMessage();
						}
					}
					String pMsg = Msg.parseTranslation(po.getCtx(), ((DocAction)po).getProcessMsg());
					processMsg.append(pMsg);
				}
			}
		}
		return null;
	}

}

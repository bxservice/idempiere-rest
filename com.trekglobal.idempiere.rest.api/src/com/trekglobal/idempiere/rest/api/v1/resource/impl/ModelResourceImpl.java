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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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

import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MColumn;
import org.compiere.model.MRole;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.compiere.wf.MWorkflow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.json.TypeConverterUtils;
import com.trekglobal.idempiere.rest.api.json.filter.ConvertedQuery;
import com.trekglobal.idempiere.rest.api.json.filter.IQueryConverter;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.v1.resource.ModelResource;
import com.trekglobal.idempiere.rest.api.v1.resource.file.FileStreamingOutput;

/**
 * @author hengsin
 *
 */
public class ModelResourceImpl implements ModelResource {

	private static final int DEFAULT_QUERY_TIMEOUT = 60 * 2;
	private static final int MAX_RECORDS_SIZE = MSysConfig.getIntValue("REST_MAX_RECORDS_SIZE", 100);
	private final static CLogger log = CLogger.getCLogger(ModelResourceImpl.class);

	/**
	 * default constructor
	 */
	public ModelResourceImpl() {
	}

	@Override
	public Response getPO(String tableName, String id, String details, String select) {
		return getPO(tableName, id, details, select, null);
	}
	
	/**
	 * 
	 * @param tableName
	 * @param id id or uuid
	 * @param details child/link entity
	 * @param multiProperty comma separated columns
	 * @param singleProperty single column
	 * @return
	 */
	private Response getPO(String tableName, String id, String details, String multiProperty, String singleProperty) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID()==0)
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid table name").append("No match found for table name: ").append(tableName).build().toString())
					.build();
		
		if (!hasAccess(table, false)) 
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for table: ").append(tableName).build().toString())
					.build();
		
		boolean isUUID = TypeConverterUtils.isUUID(id);
		String keyColumn = isUUID ? PO.getUUIDColumnName(tableName) : tableName + "_ID";
		Query query = new Query(Env.getCtx(), table, keyColumn + "=?", null);
		query.setApplyAccessFilter(true, false);
		PO po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
		if (po != null) {
			IPOSerializer serializer = IPOSerializer.getPOSerializer(tableName, po.getClass());
			String[] includes = null;
			if (!Util.isEmpty(multiProperty, true)) {
				includes = getIncludes(po, multiProperty);
			} else if (!Util.isEmpty(singleProperty, true)) {
				if (po.get_Value(singleProperty) == null) {
					return Response.status(Status.NOT_FOUND)
							.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid property name").append("No match found for table name: ").append(singleProperty).build().toString())
							.build();
				}
				includes = new String[] {singleProperty};
			}
			JsonObject json = serializer.toJson(po, includes, null);
			if (!Util.isEmpty(details, true))
				loadDetails(po, json, details);
			return Response.ok(json.toString()).build();
		} else {
			query.setApplyAccessFilter(false);
			po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
			if (po != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for record with id ").append(id).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found matching id ").append(id).build().toString())
						.build();
			}
		}
	}
	
	@Override
	public Response getPOProperty(String tableName, String id, String propertyName) {
		return getPO(tableName, id, null, null, propertyName);
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
				if (hasAccess(table, false)) {
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
			return Response.ok(array.toString()).build();			
		} catch (Exception ex) {
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(converter.getErrorResponseStatus())
					.entity(new ErrorBuilder().status(converter.getErrorResponseStatus())
							.title("GET Error")
							.append("Get models with exception: ")
							.append(ex.getMessage())
							.build().toString())
					.build();
		}

	}

	@Override
	public Response getPOs(String tableName, String filter, String order, String select, int top, int skip) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID()==0)
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid table name").append("No match found for table name: ").append(tableName).build().toString())
					.build();
		
		if (!hasAccess(table, false)) 
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for table: ").append(tableName).build().toString())
					.build();
		
		String whereClause = "";
		if (!Util.isEmpty(filter, true) ) {
			whereClause = filter;
		}

		IQueryConverter converter = IQueryConverter.getQueryConverter("DEFAULT");
		try {
			ConvertedQuery convertedStatement = converter.convertStatement(tableName, whereClause);
			if (log.isLoggable(Level.INFO)) log.info("Where Clause: " + convertedStatement.getWhereClause());

			Query query = new Query(Env.getCtx(), table, convertedStatement.getWhereClause(), null);
			query.setApplyAccessFilter(true, false)
			.setOnlyActiveRecords(true)
			.setParameters(convertedStatement.getParameters());

			if (isValidOrderBy(table, order)) {
				query.setOrderBy(order);
			}
			query.setQueryTimeout(DEFAULT_QUERY_TIMEOUT);
			int rowCount = query.count();
			int pageCount = 1;
			if (top > MAX_RECORDS_SIZE || top <= 0)
				top = MAX_RECORDS_SIZE;

			if (rowCount > top) {
				pageCount = (int)Math.ceil(rowCount / (double)top);
			} 
			query.setPageSize(top);
			query.setRecordstoSkip(skip);

			List<PO> list = query.list();
			JsonArray array = new JsonArray();
			if (list != null) {
				IPOSerializer serializer = IPOSerializer.getPOSerializer(tableName, MTable.getClass(tableName));
				String[] includes = null;
				for(PO po : list) {
					if (!Util.isEmpty(select, true) && includes == null) {
						includes = getIncludes(po, select);
					}
					JsonObject json = serializer.toJson(po, includes, null);
					array.add(json);
				}
				return Response.ok(array.toString())
						.header("X-Page-Count", pageCount)
						.header("X-Records-Size", top)
						.header("X-Skip-Records", skip)
						.header("X-Row-Count", rowCount)
						.build();
			} else {
				return Response.ok(array.toString()).build();
			}
		} catch (Exception ex) {
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(converter.getErrorResponseStatus())
					.entity(new ErrorBuilder().status(converter.getErrorResponseStatus())
							.title("GET Error")
							.append("Get POs with exception: ")
							.append(ex.getMessage())
							.build().toString())
					.build();
		}
	}

	@Override
	public Response create(String tableName, String jsonText) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID()==0)
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid table name").append("No match found for table name: ").append(tableName).build().toString())
					.build();
		
		if (!hasAccess(table, true)) 
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for table: ").append(tableName).build().toString())
					.build();
		
		Trx trx = Trx.get(Trx.createTrxName(), true);
		try {
			trx.start();
			Gson gson = new GsonBuilder().create();
			JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
			IPOSerializer serializer = IPOSerializer.getPOSerializer(tableName, MTable.getClass(tableName));
			PO po = serializer.fromJson(jsonObject, table);
			po.set_TrxName(trx.getTrxName());
			try {
				po.saveEx();
			} catch (Exception ex) {
				trx.rollback();
				log.log(Level.SEVERE, ex.getMessage(), ex);
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Save error").append("Save error with exception: ").append(ex.getMessage()).build().toString())
						.build();
			}
			
			Map<String, JsonArray> detailMap = new LinkedHashMap<>();
			Set<String> fields = jsonObject.keySet();
			for(String field : fields) {
				JsonElement fieldElement = jsonObject.get(field);
				if (fieldElement != null && fieldElement.isJsonArray()) {
					MTable childTable = MTable.get(Env.getCtx(), field);
					if (childTable != null && childTable.getAD_Table_ID() > 0) {
						IPOSerializer childSerializer = IPOSerializer.getPOSerializer(field, MTable.getClass(field));
						JsonArray fieldArray = fieldElement.getAsJsonArray();
						JsonArray savedArray = new JsonArray();
						try {
							fieldArray.forEach(e -> {
								if (e.isJsonObject()) {
									JsonObject childJsonObject = e.getAsJsonObject();
									PO childPO = childSerializer.fromJson(childJsonObject, childTable);
									childPO.set_TrxName(trx.getTrxName());
									childPO.set_ValueOfColumn(tableName+"_ID", po.get_ID());
									childPO.saveEx();
									childJsonObject = serializer.toJson(childPO);
									savedArray.add(childJsonObject);
								}
							});
							if (savedArray.size() > 0)
								detailMap.put(field, savedArray);
						} catch (Exception ex) {
							trx.rollback();
							log.log(Level.SEVERE, ex.getMessage(), ex);
							return Response.status(Status.INTERNAL_SERVER_ERROR)
									.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Save error").append("Save error with exception: ").append(ex.getMessage()).build().toString())
									.build();
						}
					}
				}
			}
			
			String error = runDocAction(po, jsonObject);
			if (Util.isEmpty(error, true)) {
				trx.commit(true);
			} else {
				trx.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Can't perform document action").append("Encounter exception during execution of document action: ").append(error).build().toString())
						.build();
			}
			
			jsonObject = serializer.toJson(po);
			if (detailMap.size() > 0) {
				for(String childTableName : detailMap.keySet()) {
					JsonArray childArray = detailMap.get(childTableName);
					jsonObject.add(childTableName, childArray);
				}
			}
			return Response.status(Status.CREATED).entity(jsonObject.toString()).build();
		} catch (Exception ex) {
			trx.rollback();
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Server error").append("Server error with exception: ").append(ex.getMessage()).build().toString())
					.build();
		} finally {
			trx.close();
		}
	}

	@Override
	public Response update(String tableName, String id, String jsonText) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID()==0)
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid table name").append("No match found for table name: ").append(tableName).build().toString())
					.build();
		
		if (!hasAccess(table, true)) 
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for table: ").append(tableName).build().toString())
					.build();
		
		boolean isUUID = TypeConverterUtils.isUUID(id);
		String keyColumn = isUUID ? PO.getUUIDColumnName(tableName) : tableName + "_ID";
		Query query = new Query(Env.getCtx(), table, keyColumn + "=?", null);
		query.setApplyAccessFilter(true, true);
		PO po = isUUID ? query.setParameters(id).first()
					 : query.setParameters(Integer.parseInt(id)).first();
		if (po == null) {
			query.setApplyAccessFilter(false);
			po = isUUID ? query.setParameters(id).first()
					 	: query.setParameters(Integer.parseInt(id)).first();
			if (po != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for record with id ").append(id).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found matching id ").append(id).build().toString())
						.build();
			}
		}
		
		Trx trx = Trx.get(Trx.createTrxName(), true);
		try {
			trx.start();
			Gson gson = new GsonBuilder().create();
			JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
			IPOSerializer serializer = IPOSerializer.getPOSerializer(tableName, MTable.getClass(tableName));
			po = serializer.fromJson(jsonObject, po);
			po.set_TrxName(trx.getTrxName());
			try {
				po.saveEx();
			} catch (Exception ex) {
				trx.rollback();
				log.log(Level.SEVERE, ex.getMessage(), ex);
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Save error").append("Save error with exception: ").append(ex.getMessage()).build().toString())
						.build();
			}
			
			Map<String, JsonArray> detailMap = new LinkedHashMap<>();
			Set<String> fields = jsonObject.keySet();
			final int parentId = po.get_ID();
			for(String field : fields) {
				JsonElement fieldElement = jsonObject.get(field);
				if (fieldElement != null && fieldElement.isJsonArray()) {
					MTable childTable = MTable.get(Env.getCtx(), field);
					if (childTable != null && childTable.getAD_Table_ID() > 0) {									
						IPOSerializer childSerializer = IPOSerializer.getPOSerializer(field, MTable.getClass(field));
						JsonArray fieldArray = fieldElement.getAsJsonArray();
						JsonArray savedArray = new JsonArray();
						try {
							fieldArray.forEach(e -> {
								if (e.isJsonObject()) {
									JsonObject childJsonObject = e.getAsJsonObject();
									PO childPO = loadPO(field, childJsonObject);
									
									if (childPO == null) {
										childPO = childSerializer.fromJson(childJsonObject, childTable);
										childPO.set_ValueOfColumn(tableName+"_ID", parentId);
									} else {
										childPO = childSerializer.fromJson(childJsonObject, childPO);
									}
									childPO.set_TrxName(trx.getTrxName());
									childPO.saveEx();
									childJsonObject = serializer.toJson(childPO);
									savedArray.add(childJsonObject);
								}
							});
							if (savedArray.size() > 0)
								detailMap.put(field, savedArray);
						} catch (Exception ex) {
							trx.rollback();
							log.log(Level.SEVERE, ex.getMessage(), ex);
							return Response.status(Status.INTERNAL_SERVER_ERROR)
									.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Save error").append("Save error with exception: ").append(ex.getMessage()).build().toString())
									.build();
						}
					}
				}
			}
			
			String error = runDocAction(po, jsonObject);
			if (Util.isEmpty(error, true)) {
				trx.commit(true);
			} else {
				trx.rollback();
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Can't perform document action").append("Encounter exception during execution of document action: ").append(error).build().toString())
						.build();
			}
			
			jsonObject = serializer.toJson(po);
			if (detailMap.size() > 0) {
				for(String field : detailMap.keySet()) {
					JsonArray child = detailMap.get(field);
					jsonObject.add(field, child);
				}
			}
			return Response.status(Status.OK).entity(jsonObject.toString()).build();
		} catch (Exception ex) {
			trx.rollback();
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Server error").append("Server error with exception: ").append(ex.getMessage()).build().toString())
					.build();
		} finally {
			trx.close();
		}
	}

	@Override
	public Response delete(String tableName, String id) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID()==0)
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid table name").append("No match found for table name: ").append(tableName).build().toString())
					.build();
		
		if (!hasAccess(table, true)) 
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for table: ").append(tableName).build().toString())
					.build();
		
		boolean uuid = TypeConverterUtils.isUUID(id);
		String keyColumn = uuid ? PO.getUUIDColumnName(tableName) : tableName + "_ID";
		Query query = new Query(Env.getCtx(), table, keyColumn + "=?", null);
		query.setApplyAccessFilter(true, true);
		PO po = uuid ? query.setParameters(id).first()
					 : query.setParameters(Integer.parseInt(id)).first();
		if (po != null) {
			try {
				po.deleteEx(true);
				return Response.ok().build();
			} catch (Exception ex) {
				log.log(Level.SEVERE, ex.getMessage(), ex);
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Delete error").append("Delete error with exception: ").append(ex.getMessage()).build().toString())
						.build();
			}
		} else {
			query.setApplyAccessFilter(false);
			po = uuid ? query.setParameters(id).first()
					  : query.setParameters(Integer.parseInt(id)).first(); 
			if (po != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for record with id ").append(id).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found matching id ").append(id).build().toString())
						.build();
			}
		}
	}

	@Override
	public Response getAttachments(String tableName, String id) {
		JsonArray array = new JsonArray();
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID()==0)
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid table name").append("No match found for table name: ").append(tableName).build().toString())
					.build();
		
		if (!hasAccess(table, false)) 
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for table: ").append(tableName).build().toString())
					.build();
		
		boolean isUUID = TypeConverterUtils.isUUID(id);
		String keyColumn = isUUID ? PO.getUUIDColumnName(tableName) : tableName + "_ID";
		Query query = new Query(Env.getCtx(), table, keyColumn + "=?", null);
		query.setApplyAccessFilter(true, false);
		PO po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
		if (po != null) {
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
			return Response.ok(array.toString()).build();
		} else {
			query.setApplyAccessFilter(false);
			po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
			if (po != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for record with id ").append(id).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found matching id ").append(id).build().toString())
						.build();
			}
		}
	}

	@Override
	public Response getAttachmentsAsZip(String tableName, String id) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID()==0)
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid table name").append("No match found for table name: ").append(tableName).build().toString())
					.build();
		
		if (!hasAccess(table, false)) 
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for table: ").append(tableName).build().toString())
					.build();
		
		boolean isUUID = TypeConverterUtils.isUUID(id);
		String keyColumn = isUUID ? PO.getUUIDColumnName(tableName) : tableName + "_ID";
		Query query = new Query(Env.getCtx(), table, keyColumn + "=?", null);
		query.setApplyAccessFilter(true, false);
		PO po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
		if (po != null) {
			MAttachment attachment = po.getAttachment();
			if (attachment != null) {
				File zipFile = attachment.saveAsZip();
				if (zipFile != null) {
					FileStreamingOutput fso = new FileStreamingOutput(zipFile);
					return Response.ok(fso).build();
				}
			}
			return Response.status(Status.NO_CONTENT).build();
		} else {
			query.setApplyAccessFilter(false);
			po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
			if (po != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for record with id ").append(id).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found matching id ").append(id).build().toString())
						.build();
			}
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
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("data property is mandatory").build().toString())
					.build();
		String base64Content = jsonElement.getAsString();
		if (Util.isEmpty(base64Content, true))
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("data property is mandatory").build().toString())
					.build();
		
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID()==0)
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid table name").append("No match found for table name: ").append(tableName).build().toString())
					.build();
		
		if (!hasAccess(table, true)) 
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for table: ").append(tableName).build().toString())
					.build();
		
		boolean isUUID = TypeConverterUtils.isUUID(id);
		String keyColumn = isUUID ? PO.getUUIDColumnName(tableName) : tableName + "_ID";
		Query query = new Query(Env.getCtx(), table, keyColumn + "=?", null);
		query.setApplyAccessFilter(true, false);
		PO po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
		if (po != null) {
			byte[] data = DatatypeConverter.parseBase64Binary(base64Content);
			if (data == null || data.length == 0)
				return Response.status(Status.BAD_REQUEST)
						.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Can't parse data").append("Can't parse data in Json content, not base64 encoded").build().toString())
						.build();
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
	    						return Response.status(Status.CONFLICT)
	    								.entity(new ErrorBuilder().status(Status.CONFLICT).title("Duplicate file name").append("Duplicate file name: ").append(name).build().toString())
	    								.build();
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
	        	log.log(Level.SEVERE, ex.getMessage(), ex);
	        	return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Server error").append("Server error with exception: ").append(ex.getMessage()).build().toString())
						.build();
			}
															
			return Response.status(Status.CREATED).build();
		} else {
			query.setApplyAccessFilter(false);
			po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
			if (po != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for record with id ").append(id).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found matching id ").append(id).build().toString())
						.build();
			}
		}
	}

	@Override
	public Response getAttachmentEntry(String tableName, String id, String fileName) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID()==0)
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid table name").append("No match found for table name: ").append(tableName).build().toString())
					.build();
		
		if (!hasAccess(table, false)) 
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for table: ").append(tableName).build().toString())
					.build();
		
		boolean isUUID = TypeConverterUtils.isUUID(id);
		String keyColumn = isUUID ? PO.getUUIDColumnName(tableName) : tableName + "_ID";
		Query query = new Query(Env.getCtx(), table, keyColumn + "=?", null);
		query.setApplyAccessFilter(true, false);
		PO po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
		if (po != null) {
			MAttachment attachment = po.getAttachment();
			if (attachment != null) {
				for(MAttachmentEntry entry : attachment.getEntries()) {
					if (entry.getName().equals(fileName)) {
						try {
							Path tempPath = Files.createTempDirectory(tableName);
							File tempFolder = tempPath.toFile();
							File zipFile = new File(tempFolder, fileName);
							zipFile = entry.getFile(zipFile);
							FileStreamingOutput fso = new FileStreamingOutput(zipFile);
							return Response.ok(fso).build();
						} catch (IOException ex) {
							log.log(Level.SEVERE, ex.getMessage(), ex);
							return Response.status(Status.INTERNAL_SERVER_ERROR)
									.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("IO error").append("IO error with exception: ").append(ex.getMessage()).build().toString())
									.build();
						}
					}
				}
			}
			return Response.status(Status.NO_CONTENT).build();
		} else {
			query.setApplyAccessFilter(false);
			po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
			if (po != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for record with id ").append(id).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found matching id ").append(id).build().toString())
						.build();
			}
		}
	}

	@Override
	public Response addAttachmentEntry(String tableName, String id, String jsonText) {
		Gson gson = new GsonBuilder().create();
		JsonObject jsonObject = gson.fromJson(jsonText, JsonObject.class);
		
		JsonElement jsonElement = jsonObject.get("name");
		if (jsonElement == null || !jsonElement.isJsonPrimitive())
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("name property is mandatory").build().toString())
					.build();
		String fileName = jsonElement.getAsString();
		if (Util.isEmpty(fileName, true))
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("name property is mandatory").build().toString())
					.build();
		
		jsonElement = jsonObject.get("data");
		if (jsonElement == null || !jsonElement.isJsonPrimitive())
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("data property is mandatory").build().toString())
					.build();
		String base64Content = jsonElement.getAsString();
		if (Util.isEmpty(base64Content, true))
			return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("data property is mandatory").build().toString())
					.build();
		
		boolean overwrite = false;
		jsonElement = jsonObject.get("overwrite");
		if (jsonElement != null && jsonElement.isJsonPrimitive())
			overwrite = jsonElement.getAsBoolean();
		
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID()==0)
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid table name").append("No match found for table name: ").append(tableName).build().toString())
					.build();
		
		if (!hasAccess(table, true)) 
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for table: ").append(tableName).build().toString())
					.build();
		
		boolean isUUID = TypeConverterUtils.isUUID(id);
		String keyColumn = isUUID ? PO.getUUIDColumnName(tableName) : tableName + "_ID";
		Query query = new Query(Env.getCtx(), table, keyColumn + "=?", null);
		query.setApplyAccessFilter(true, false);
		PO po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
		if (po != null) {
			byte[] data = DatatypeConverter.parseBase64Binary(base64Content);
			if (data == null || data.length == 0)
				return Response.status(Status.BAD_REQUEST)
						.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Can't parse data").append("Can't parse data in Json content, not base64 encoded").build().toString())
						.build();
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
						return Response.status(Status.CONFLICT)
								.entity(new ErrorBuilder().status(Status.CONFLICT).title("Duplicate file name").append("Duplicate file name: ").append(fileName).build().toString())
								.build();
					}
				}
			}		
			
			try {
				attachment.addEntry(fileName, data);
				attachment.saveEx();
			} catch (Exception ex) {
				log.log(Level.SEVERE, ex.getMessage(), ex);
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Save error").append("Save error with exception: ").append(ex.getMessage()).build().toString())
						.build();
			}
			return Response.status(Status.CREATED).build();
		} else {
			query.setApplyAccessFilter(false);
			po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
			if (po != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for record with id ").append(id).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found matching id ").append(id).build().toString())
						.build();
			}
		}
	}

	@Override
	public Response deleteAttachments(String tableName, String id) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID()==0)
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid table name").append("No match found for table name: ").append(tableName).build().toString())
					.build();
		
		if (!hasAccess(table, true)) 
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for table: ").append(tableName).build().toString())
					.build();
		
		boolean isUUID = TypeConverterUtils.isUUID(id);
		String keyColumn = isUUID ? PO.getUUIDColumnName(tableName) : tableName + "_ID";
		Query query = new Query(Env.getCtx(), table, keyColumn + "=?", null);
		query.setApplyAccessFilter(true, false);
		PO po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
		if (po != null) {
			MAttachment attachment = po.getAttachment();
			if (attachment != null) {
				try {
					attachment.deleteEx(true);
				} catch (Exception ex) {
					log.log(Level.SEVERE, ex.getMessage(), ex);
					return Response.status(Status.INTERNAL_SERVER_ERROR)
							.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Delete error").append("Delete error with exception: ").append(ex.getMessage()).build().toString())
							.build();
				}
				return Response.ok().build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("No attachments").append("No attachment is found for record with id ").append(id).build().toString())
						.build();
			}
		} else {
			query.setApplyAccessFilter(false);
			po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
			if (po != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for record with id ").append(id).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found matching id ").append(id).build().toString())
						.build();
			}
		}
	}

	@Override
	public Response deleteAttachmentEntry(String tableName, String id, String fileName) {
		MTable table = MTable.get(Env.getCtx(), tableName);
		if (table == null || table.getAD_Table_ID()==0)
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid table name").append("No match found for table name: ").append(tableName).build().toString())
					.build();
		
		if (!hasAccess(table, true)) 
			return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for table: ").append(tableName).build().toString())
					.build();
		
		boolean isUUID = TypeConverterUtils.isUUID(id);
		String keyColumn = isUUID ? PO.getUUIDColumnName(tableName) : tableName + "_ID";
		Query query = new Query(Env.getCtx(), table, keyColumn + "=?", null);
		query.setApplyAccessFilter(true, false);
		PO po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
		if (po != null) {
			MAttachment attachment = po.getAttachment();
			if (attachment != null) {
				int i = 0;
				for(MAttachmentEntry entry : attachment.getEntries()) {
					if (entry.getName().equals(fileName)) {
						if (attachment.deleteEntry(i)) {
							try {
								attachment.saveEx();
							} catch (Exception ex) {
								log.log(Level.SEVERE, ex.getMessage(), ex);
								return Response.status(Status.INTERNAL_SERVER_ERROR)
										.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Save error").append("Save error with exception: ").append(ex.getMessage()).build().toString())
										.build();
							}
							return Response.ok().build();							
						} else {
							return Response.status(Status.INTERNAL_SERVER_ERROR)
									.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Fail to remove attachment entry").build().toString())
									.build();
						}
					}
					i++;
				}
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("No matching attachment entry").append("No attachment entry is found for name: ").append(fileName).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("No attachments").append("No attachment is found for record with id ").append(id).build().toString())
						.build();
			}
		} else {
			query.setApplyAccessFilter(false);
			po = isUUID ? query.setParameters(id).first()
					   : query.setParameters(Integer.parseInt(id)).first();
			if (po != null) {
				return Response.status(Status.FORBIDDEN)
						.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for record with id ").append(id).build().toString())
						.build();
			} else {
				return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found matching id ").append(id).build().toString())
						.build();
			}
		}
	}

	private void loadDetails(PO po, JsonObject jsonObject, String details) {
		if (Util.isEmpty(details, true))
			return;
		
		String[] tableNames = details.split("[,]");
		String keyColumn = po.get_TableName() + "_ID";
		for(String tableName : tableNames) {
			MTable table = MTable.get(Env.getCtx(), tableName);
			if (table == null)
				continue;
			
			if (!hasAccess(table, false))
				continue;
			
			Query query = new Query(Env.getCtx(), table, keyColumn + "=?", null);
			query.setApplyAccessFilter(true, false)
				 .setOnlyActiveRecords(true);
			List<PO> childPOs = query.setParameters(po.get_ID()).list();
			if (childPOs != null && childPOs.size() > 0) {
				JsonArray childArray = new JsonArray();
				IPOSerializer serializer = IPOSerializer.getPOSerializer(tableName, MTable.getClass(tableName));
				for(PO child : childPOs) {							
					JsonObject childJsonObject = serializer.toJson(child, null, new String[] {keyColumn, "model-name"});
					childArray.add(childJsonObject);
				}
				jsonObject.add(tableName, childArray);
			}
		}
	}
	
	private String[] getIncludes(PO po, String select) {

		if (Util.isEmpty(select, true))
			return null;

		ArrayList<String> includes = new ArrayList<String>();

		MTable mTable = MTable.get(po.get_Table_ID());
		String[] columnNames = select.split("[,]");
		for(String columnName : columnNames) {
			if (po.get_ColumnIndex(columnName.trim()) < 0)
				continue;

			MColumn mColumn = mTable.getColumn(columnName.trim());
			if (MRole.getDefault().isColumnAccess(mTable.getAD_Table_ID(), mColumn.getAD_Column_ID(), true))
				includes.add(columnName.trim());
		}

		return includes.toArray(new String[includes.size()]);
	}

	private boolean hasAccess(MTable table, boolean rw) {
		MRole role = MRole.getDefault();
		if (role == null)
			return false;
		
		StringBuilder builder = new StringBuilder("SELECT DISTINCT a.AD_Window_ID FROM AD_Window a JOIN AD_Tab b ON a.AD_Window_ID=b.AD_Window_ID ");
		builder.append("WHERE a.IsActive='Y' AND b.IsActive='Y' AND b.AD_Table_ID=?");
		try (PreparedStatement stmt = DB.prepareStatement(builder.toString(), null)) {
			stmt.setInt(1, table.getAD_Table_ID());			
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				int windowId = rs.getInt(1);
				Boolean b = role.getWindowAccess(windowId);
				if (b != null) {
					if (!rw || b.booleanValue())
						return true;
				}
			}
		} catch (SQLException ex) {
			log.log(Level.SEVERE, ex.getMessage(), ex);
			throw new RuntimeException(ex.getMessage());
		}
		
		//If no window or no access to the window - check if the role has read/write access to the table
		return role.isTableAccess(table.getAD_Table_ID(), false);
	}
	
	private PO loadPO(String tableName, JsonObject jsonObject) {
		PO po = null;
		String idColumn = tableName + "_ID";
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

	private String runDocAction(PO po, JsonObject jsonObject) {
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
						return processInfo.getSummary();
					} else {
						try {
							po.saveEx();
						} catch (Exception ex) {
							log.log(Level.SEVERE, ex.getMessage(), ex);
							return ex.getMessage();
						}
					}
				}
			}
		}
		return null;
	}
	
	private boolean isValidOrderBy(MTable table, String orderBy) {
		if (!Util.isEmpty(orderBy, true)) {
			String[] columnNames = orderBy.split("[,]");
			for (String columnName : columnNames) {
				columnName = columnName.trim();

				if (columnName.contains(" ")) {
					String[] names = columnName.split(" ");
					columnName = names[0];
					String orderPreference = names[1];
					if (names.length > 2 || (!"asc".equals(orderPreference.toLowerCase()) && !"desc".equals(orderPreference.toLowerCase()))) {
						log.log(Level.WARNING, "Invalid order by clause.");
						return false;
					}
				}
				if (table.getColumnIndex(columnName) < 0) {
					log.log(Level.WARNING, "Column: " + columnName + " is not a valid column to be ordered by");
					return false;
				}
			}

			return true;
		}

		return false;
	}
}

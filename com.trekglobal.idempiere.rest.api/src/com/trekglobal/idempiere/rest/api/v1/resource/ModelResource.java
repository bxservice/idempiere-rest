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
package com.trekglobal.idempiere.rest.api.v1.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author hengsin
 *
 */
@Path("v1/models")
public interface ModelResource {

	@Path("{tableName}/{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get record by id/uuid
	 * @param tableName
	 * @param id id/uuid
	 * @param details optional comma separated list of child PO to retrieve
	 * @param select optional comma separated list of columns to retrieve
	 * @return json representation of record
	 */
	public Response getPO(@PathParam("tableName") String tableName, @PathParam("id") String id, @QueryParam("$expand") String details, 
			@QueryParam("$select") String select, @QueryParam("showsql") String showsql);

	@Path("{tableName}/{id}/{property}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get record id/uuid property by Name 
	 * @param tableName
	 * @param id id/uuid
	 * @param propertyName columnName to be retrieved
	 * @return json representation of record
	 */
	public Response getPOProperty(@PathParam("tableName") String tableName, @PathParam("id") String id, @PathParam("property") String propertyName, @QueryParam("showsql") String showsql);

	@Path("{tableName}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get records
	 * @param tableName
	 * @param filter optional where clause
	 * @param order optional order by clause
	 * @param pageNo
	 * @return json array of records
	 */
	public Response getPOs(@PathParam("tableName") String tableName, @QueryParam("$expand") String details, @QueryParam("$filter") String filter, @QueryParam("$orderby") String order, 
			@QueryParam("$select") String select, @QueryParam("$top") int top, @DefaultValue("0") @QueryParam("$skip") int skip,
			@QueryParam("$valrule") String validationRuleID, @QueryParam("$context") String context, @QueryParam("showsql") String showsql);
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get models available
	 * @param filter optional where clause
	 * @return json array of model
	 */
	public Response getModels(@QueryParam("$filter") String filter);
	
	@Path("{tableName}")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * create new record
	 * predefine property:
	 *   doc-action (document action to execute)
	 * @param tableName
	 * @param jsonText json representation of data to process
	 * @return json representation of created record
	 */
	public Response create(@PathParam("tableName") String tableName, String jsonText);
	
	@Path("{tableName}/{id}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * update record
	 * predefine property:
	 *   doc-action (document action to execute)
	 * @param tableName
	 * @param id id/uuid
	 * @param jsonText json representation of data to process
	 * @return json representation updated record
	 */
	public Response update(@PathParam("tableName") String tableName, @PathParam("id") String id, String jsonText);
	
	@Path("{tableName}/{id}")
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * delete record by id/uuid
	 * @param tableName
	 * @param id id/uuid
	 * @return http response
	 */
	public Response delete(@PathParam("tableName") String tableName, @PathParam("id") String id);
	
	@Path("{tableName}/{id}/attachments")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Get attachments
	 * @param tableName
	 * @param id record id/uuid
	 * @return json array of attachment item
	 */
	public Response getAttachments(@PathParam("tableName") String tableName, @PathParam("id") String id);
	
	@Path("{tableName}/{id}/attachments/zip")
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	/**
	 * Get all attachment as single zip file
	 * @param tableName
	 * @param id record id/uuid
	 * @return zip file binary stream
	 */
	public Response getAttachmentsAsZip(@PathParam("tableName") String tableName, @PathParam("id") String id);
	
	@Path("{tableName}/{id}/attachments/zip")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * add multiple attachments from zip file 
	 * @param tableName
	 * @param id record id/uuid
	 * @param jsonText json with base64 encoded zip file content
	 * @return http response
	 */
	public Response createAttachmentsFromZip(@PathParam("tableName") String tableName, @PathParam("id") String id, String jsonText);
	
	@Path("{tableName}/{id}/attachments/{fileName}")
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	/**
	 * Get content of an attachment item
	 * @param tableName
	 * @param id record id/uuid
	 * @param fileName name of an attachment item
	 * @return binary stream of an attachment item
	 */
	public Response getAttachmentEntry(@PathParam("tableName") String tableName, @PathParam("id") String id, @PathParam("fileName") String fileName);
	
	@Path("{tableName}/{id}/attachments")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * add/update attachment
	 * @param tableName
	 * @param id id/uuid
	 * @param jsonText json with base64 encoded attachment data
	 * @return http response
	 */
	public Response addAttachmentEntry(@PathParam("tableName") String tableName, @PathParam("id") String id, String jsonText);
	
	@Path("{tableName}/{id}/attachments")
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * delete attachments
	 * @param tableName
	 * @param id id/uuid
	 * @return http response
	 */
	public Response deleteAttachments(@PathParam("tableName") String tableName, @PathParam("id") String id);
	
	@Path("{tableName}/{id}/attachments/{fileName}")
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * delete attachment entry by name
	 * @param tableName
	 * @param id id/uuid
	 * @param fileName
	 * @return http response
	 */
	public Response deleteAttachmentEntry(@PathParam("tableName") String tableName, @PathParam("id") String id, @PathParam("fileName") String fileName);
	
	@Path("{tableName}/{id}/print")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	/**
	 * Print model record
	 * @param tableName
	 * @param id id/uuid
	 * @param reportType print output type
	 * @return json representation of record
	 */
	public Response printModelRecord(@PathParam("tableName") String tableName, @PathParam("id") String id, @QueryParam("$report_type") String reportType);
}

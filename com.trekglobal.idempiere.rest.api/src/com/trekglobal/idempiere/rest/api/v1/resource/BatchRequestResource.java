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

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.internal.PropertiesDelegate;

@Path("v1/batch")
public interface BatchRequestResource {

	/**
	 * Process a batch of requests.
	 * <pre>
	 * [
	 *	  {
	 *	    "method": "POST",
	 *	    "path": "v1/model/C_Order",
	 *	    "responseAlias": "order",
	 * 	    "body": {
	 *	      "DocumentNo": "ORD001",
	 *	      "C_BPartner_ID": 1000000
	 *	    }
	 *	  },
	 *	  {
	 *	    "method": "PUT",
	 *	    "path": "v1/model/C_Order/1000012",
	 *	    "responseAlias": "orderUpdate",
	 *	    "body": {
	 *	      "DocStatus": "CO"
	 *	    }
	 *	  },
	 *	  {
	 *	    "method": "DELETE",
	 *	    "path": "v1/model/C_Order/1000013",
	 *	    "responseAlias": "orderDelete"
	 *	  }
	 * ]
	 * </pre>
	 * Every sub-request must set a {@code responseAlias}, unique within the batch. A later sub-request's
	 * body may reference an earlier one's response instead of a literal value, using
	 * {@code alias$.jsonPathExpr} - a bare standard JSONPath (RFC 9535, e.g.
	 * {@code order$.Lines[0].C_OrderLine_ID}) evaluated against the response cached under that
	 * {@code responseAlias}. No wrapping punctuation is needed: an identifier immediately followed by
	 * JSONPath's own root marker '$' is already an unambiguous shape. A record's own primary key is always
	 * at {@code $.id} in its response (e.g. {@code bpartner$.id}).
	 * <p>
	 * Separately, {@code @#GlobalVar@} (session/context variable, e.g. {@code @#AD_Org_ID@}) resolves a
	 * value from the caller's session rather than a prior sub-request - wrapped in '@' because that's
	 * iDempiere's own pre-existing {@code Evaluator.VARIABLE_START_END_MARKER} convention, not something
	 * invented for batch requests specifically.
	 * <pre>
	 * [
	 *   { "method": "POST", "path": "v1/models/c_bpartner", "responseAlias": "bpartner",
	 *     "body": { "Value": "CUST-1001", "Name": "Acme" } },
	 *   { "method": "POST", "path": "v1/models/c_bpartner_location", "responseAlias": "bpartnerLocation",
	 *     "body": { "C_BPartner_ID": "bpartner$.id", "Name": "Main", "IsShipTo": "Y" } }
	 * ]
	 * </pre>
	 * @param requests the list of batch requests to process
	 * @param uriInfo the URI information for the request
	 * @param headers the HTTP headers for the request
	 * @param transaction if true, all requests are processed in a single transaction (default: true)
	 * @return a response containing the results of processing the batch requests
	 */ 
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	Response processBatch(List<BatchRequest> requests, @Context UriInfo uriInfo, @Context HttpHeaders headers, 
			@Context PropertiesDelegate propertiesDelegate, 
			@DefaultValue("true") @QueryParam("transaction") boolean transaction);

	class BatchRequest {
	    private String method;
	    private String path;
	    private Object body;
	    private String responseAlias;

	    // Getters and Setters
	    public String getMethod() { return method; }
	    public void setMethod(String method) { this.method = method; }
	    public String getPath() { return path; }
	    public void setPath(String path) { this.path = path; }
	    public Object getBody() { return body; }
	    public void setBody(Object body) { this.body = body; }
	    /**
	     * Mandatory, batch-unique alias this sub-request's response is cached under, so a later
	     * sub-request's body can reference it via {@code responseAlias$.jsonPathExpr}.
	     */
	    public String getResponseAlias() { return responseAlias; }
	    public void setResponseAlias(String responseAlias) { this.responseAlias = responseAlias; }
	}

	class BatchResponse {
	    private String status;
	    private int statusCode;
	    private Object body;

	    public BatchResponse(String status, int statusCode, Object body) {
	        this.status = status;
	        this.statusCode = statusCode;
	        this.body = body;
	    }

	    // Getters and Setters
	    public String getStatus() { return status; }
	    public void setStatus(String status) { this.status = status; }
	    public int getStatusCode() { return statusCode; }
	    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
	    public Object getBody() { return body; }
	    public void setBody(Object body) { this.body = body; }
	} 

}


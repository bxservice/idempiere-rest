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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
	 * 	    "body": {
	 *	      "DocumentNo": "ORD001",
	 *	      "C_BPartner_ID": 1000000
	 *	    }
	 *	  },
	 *	  {
	 *	    "method": "PUT",
	 *	    "path": "v1/model/C_Order/1000012",
	 *	    "body": {
	 *	      "DocStatus": "CO"
	 *	    }
	 *	  },
	 *	  {
	 *	    "method": "DELETE",
	 *	    "path": "v1/model/C_Order/1000013"
	 *	  }
	 * ]
	 * </pre>
	 * @param requests the list of batch requests to process
	 * @param uriInfo the URI information for the request
	 * @param headers the HTTP headers for the request
	 * @return a response containing the results of processing the batch requests
	 */ 
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	Response processBatch(List<BatchRequest> requests, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context PropertiesDelegate propertiesDelegate);

	class BatchRequest {
	    private String method;
	    private String path;
	    private Object body;

	    // Getters and Setters
	    public String getMethod() { return method; }
	    public void setMethod(String method) { this.method = method; }
	    public String getPath() { return path; }
	    public void setPath(String path) { this.path = path; }
	    public Object getBody() { return body; }
	    public void setBody(Object body) { this.body = body; }
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


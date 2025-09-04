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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.compiere.util.Trx;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.util.ThreadLocalTrx;
import com.trekglobal.idempiere.rest.api.v1.resource.BatchRequestResource;

public class BatchRequestResourseImpl implements BatchRequestResource {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Context
    private SecurityContext securityContext;
    
    @Context
    private ApplicationHandler applicationHandler;
    
    @Override
	public Response processBatch(List<BatchRequest> requests, UriInfo uriInfo, HttpHeaders headers, 
			PropertiesDelegate propertiesDelegate, boolean transaction) {
        List<BatchRequestResource.BatchResponse> results = new ArrayList<>();
        URI baseUri = uriInfo.getBaseUri();
        
        boolean badRequest = false;
        try (ThreadLocalTrx trx = new ThreadLocalTrx("BatchRequest")) {
			// Process each request in the batch
	        for (int i = 0; i < requests.size(); i++) {
	            BatchRequest req = requests.get(i);
	            try {
	            	String method = req.getMethod().toUpperCase();
	            	if (!HttpMethod.POST.equals(method) && !HttpMethod.PUT.equals(method) && !HttpMethod.DELETE.equals(method)) {
	            		BatchResponse batchResp = new BatchResponse(Status.BAD_REQUEST.getReasonPhrase(), Status.BAD_REQUEST.getStatusCode(), "Unsupported Method: "+method);
		                results.add(batchResp);
		                Trx threadLocalTrx = Trx.get(ThreadLocalTrx.getTrxName(), false);
	                    if (threadLocalTrx != null && threadLocalTrx.isActive()) {
	                        threadLocalTrx.rollback();
	                    }
	                    if (transaction) {
		                    badRequest = true;
		                    break; // Stop processing further requests on error
	                    } else {
	                    	continue; // Proceed to next request
	                    }
	            	}
	                
	                URI requestUri = URI.create(baseUri.toString().replaceAll("v1/batch/?$", "") + req.getPath());
	
	                ContainerRequest containerRequest = new ContainerRequest(
	                        baseUri,
	                        requestUri,
	                        method,
	                        securityContext,
	                        propertiesDelegate,
	                        null
	                );
	
	                if (req.getBody() != null) {
	                    byte[] bodyBytes = objectMapper.writeValueAsBytes(req.getBody());
	                    containerRequest.setEntityStream(new ByteArrayInputStream(bodyBytes));
	                }
	                containerRequest.headers(headers.getRequestHeaders());
	
	                Future<ContainerResponse> responseFuture = applicationHandler.apply(containerRequest);
	                ContainerResponse containerResponse = responseFuture.get();
	                int statusCode = containerResponse.getStatus();
	                String entity = containerResponse.getEntity() != null ? containerResponse.getEntity().toString() : null;
	                JsonObject bodyObject = null;
	                Map<?, ?> bodyAsMap = null;
	                if (entity != null && !entity.isEmpty()) {
	                	try {
	                		Gson gson = new Gson();
	                		bodyObject = gson.fromJson(entity, JsonObject.class);
	                		bodyAsMap = gson.fromJson(bodyObject, Map.class);
	                	} catch (Exception e) {}
	                }
	                BatchResponse batchResp = new BatchResponse(containerResponse.getStatusInfo().getReasonPhrase(), statusCode, bodyAsMap != null ? bodyAsMap : entity);
	                results.add(batchResp);
	                if (statusCode != Status.OK.getStatusCode() && statusCode != Status.CREATED.getStatusCode() && statusCode != Status.ACCEPTED.getStatusCode()) {
	                    // If the response is not OK, Created or Accepted, rollback the transaction
	                    Trx threadLocalTrx = Trx.get(ThreadLocalTrx.getTrxName(), false);
	                    if (threadLocalTrx != null && threadLocalTrx.isActive()) {
	                        threadLocalTrx.rollback();
	                    }
	                    if (transaction) {
	                    	badRequest = true;
	                    	break; // Stop processing further requests on error
	                    } else {
	                    	if (threadLocalTrx != null)
	                    		threadLocalTrx.start(); // Start a new transaction for next request
	                    }
	                } else {
	                	if (!transaction) {
	                		Trx threadLocalTrx = Trx.get(ThreadLocalTrx.getTrxName(), false);
	                		if (threadLocalTrx != null && threadLocalTrx.isActive()) {
	                			threadLocalTrx.commit(true);
	                			threadLocalTrx.start(); // Start a new transaction for next request
	                		}
	                	}
	                }
	            } catch (Exception e) {
	                results.add(new BatchResponse(Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage()));
	                Trx threadLocalTrx = Trx.get(ThreadLocalTrx.getTrxName(), false);
	                if (threadLocalTrx != null && threadLocalTrx.isActive()) {
	                    threadLocalTrx.rollback();
	                }
	                if (transaction) {
		                badRequest = true;
		                break; // Stop processing further requests on error
	                } else {
	                	if (threadLocalTrx != null)
	                		threadLocalTrx.start(); // Start a new transaction for next request
	                }
	            }
	        }
        }

        return badRequest ? Response.status(Status.BAD_REQUEST).entity(results).build() : Response.ok(results).build();
    }
}

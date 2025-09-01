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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.compiere.Adempiere;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Util;

import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.v1.resource.HealthResource;

public class HealthResourceImpl implements HealthResource {

	// System or environment property to override REST health check cache duration in milliseconds
	private static final String REST_HEALTH_CACHE_DURATION_PROPERTY = "REST_HEALTH_CACHE_DURATION";

	private static final String DISCONNECTED_STATUS_TEXT = "disconnected";

	private static final String CONNECTED_STATUS_TEXT = "connected";

	private static final String DOWN_STATUS_TEXT = "DOWN";

	private static final String UP_STATUS_TEXT = "UP";

	private static final CLogger log = CLogger.getCLogger(HealthResourceImpl.class);
	
    // cached health status
    private static class HealthStatus {
        final int httpStatusCode;
        final String jsonResponse;

        HealthStatus(int httpStatusCode, String jsonResponse) {
            this.httpStatusCode = httpStatusCode;
            this.jsonResponse = jsonResponse;
        }
    }

    // Cache duration in milliseconds
    private static long CACHE_DURATION_MS = 20000; //20 seconds
    static {
    	String duration = System.getProperty(REST_HEALTH_CACHE_DURATION_PROPERTY);
		if (Util.isEmpty(duration, true))
			duration = System.getenv(REST_HEALTH_CACHE_DURATION_PROPERTY);
		
		if (!Util.isEmpty(duration, true)) {
			try {
				long d = Long.parseLong(duration);
				if (d >= 0) {
					// set cache duration
					CACHE_DURATION_MS = d;
				} else {
					log.warning("Ignoring invalid negative REST_HEALTH_CACHE_DURATION=" + duration);
				}
			} catch (NumberFormatException e) {
				log.warning("Ignoring invalid non-numeric REST_HEALTH_CACHE_DURATION=" + duration);
			}
		}
    }

    private static final AtomicLong dbLastCheckTimestamp = new AtomicLong(0);
    private static final AtomicLong lastCheckTimestamp = new AtomicLong(0);
    private static final AtomicReference<HealthStatus> dbCachedStatus = new AtomicReference<>();
    private static final AtomicReference<HealthStatus> cachedStatus = new AtomicReference<>();

    @Context
	private ResourceContext resourceContext;
    
    @Override
    public Response getHealthStatus(boolean dbstatus) {
    	AtomicLong timeStamp = dbstatus ? dbLastCheckTimestamp : lastCheckTimestamp;
    	AtomicReference<HealthStatus> statusRef = dbstatus ? dbCachedStatus : cachedStatus;
    	
        long now = System.currentTimeMillis();
        long lastCheck = timeStamp.get();

        // Check if the cached result is still valid
        if (now - lastCheck < CACHE_DURATION_MS && statusRef.get() != null) {
            HealthStatus status = statusRef.get();
            if (log.isLoggable(Level.FINE))
            	log.fine("Returning cached health status: " + status.jsonResponse);
            return Response.status(status.httpStatusCode).entity(status.jsonResponse).build();
        }

        // The cache is stale. We need to try and update it.
        if (timeStamp.compareAndSet(lastCheck, now)) {
        	ModelResourceImpl modelResource = resourceContext.getResource(ModelResourceImpl.class);
        	if (dbstatus) {
	            try {
	            	// use thread to enforce 2 second timeout
	            	Future<Boolean> future = Adempiere.getThreadPoolExecutor().submit(() -> {
	            		return DB.isConnected(); //depends on HikariCP connection timeout settings, this can be longer than 2 minutes
	            	});            	
	            	boolean isConnected = false;
	            	try {
	            		isConnected = future.get(2, TimeUnit.SECONDS);
	            	} catch (Exception e) {}
	            	
	                if (isConnected) {
	                	if (modelResource != null)
	                		updateCache(Response.Status.OK.getStatusCode(), UP_STATUS_TEXT, CONNECTED_STATUS_TEXT);
	                	else
	                		updateCache(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), DOWN_STATUS_TEXT, CONNECTED_STATUS_TEXT);
	                } else {
	                    updateCache(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), DOWN_STATUS_TEXT, DISCONNECTED_STATUS_TEXT);
	                }
	            } catch (Exception e) {
	                updateCache(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), DOWN_STATUS_TEXT, DISCONNECTED_STATUS_TEXT);
	            }
        	} else {
    			if (modelResource != null) {
    				updateCache(Response.Status.OK.getStatusCode(), UP_STATUS_TEXT, null);
    			} else {
    				updateCache(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), DOWN_STATUS_TEXT, null);
    			}
        	}
        }
        
        HealthStatus status = statusRef.get();
        return Response.status(status.httpStatusCode).entity(status.jsonResponse).build();
    }
    
    private void updateCache(int code, String status, String dbStatus) {
    	if (log.isLoggable(Level.FINE)) {
    		if (dbStatus != null)
    			log.fine("Updating health status cache: " + status + ", database: " + dbStatus);
    		else
    			log.fine("Updating health status cache: " + status);
    	}
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", status);
        if (dbStatus != null) {
        	responseJson.addProperty("database", dbStatus);
        	dbCachedStatus.set(new HealthStatus(code, responseJson.toString()));
        } else {
        	cachedStatus.set(new HealthStatus(code, responseJson.toString()));
        }
    }
}
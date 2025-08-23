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

import javax.ws.rs.core.Response;

import org.compiere.Adempiere;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.v1.resource.HealthResource;

public class HealthResourceImpl implements HealthResource {

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
    private static final long CACHE_DURATION_MS = 60000; //60 seconds

    private static final AtomicLong lastCheckTimestamp = new AtomicLong(0);
    private static final AtomicReference<HealthStatus> cachedStatus = new AtomicReference<>();

    @Override
    public Response getHealthStatus() {
        long now = System.currentTimeMillis();
        long lastCheck = lastCheckTimestamp.get();

        // Check if the cached result is still valid
        if (now - lastCheck < CACHE_DURATION_MS && cachedStatus.get() != null) {
            HealthStatus status = cachedStatus.get();
            if (log.isLoggable(Level.FINE))
            	log.fine("Returning cached health status: " + status.jsonResponse);
            return Response.status(status.httpStatusCode).entity(status.jsonResponse).build();
        }

        // The cache is stale. We need to try and update it.
        if (lastCheckTimestamp.compareAndSet(lastCheck, now)) {
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
                    updateCache(200, "UP", "connected");
                } else {
                    updateCache(503, "DOWN", "disconnected");
                }
            } catch (Exception e) {
                updateCache(503, "DOWN", "disconnected");
            }
        }
        
        HealthStatus status = cachedStatus.get();
        return Response.status(status.httpStatusCode).entity(status.jsonResponse).build();
    }
    
    private void updateCache(int code, String status, String dbStatus) {
    	if (log.isLoggable(Level.FINE))
    		log.fine("Updating health status cache: " + status + ", database: " + dbStatus);
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", status);
        responseJson.addProperty("database", dbStatus);
        cachedStatus.set(new HealthStatus(code, responseJson.toString()));
    }
}
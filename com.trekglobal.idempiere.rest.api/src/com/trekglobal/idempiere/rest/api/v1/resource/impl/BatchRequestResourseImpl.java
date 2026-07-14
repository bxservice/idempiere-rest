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
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.Future;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.compiere.model.MTable;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.RestUtils;
import com.trekglobal.idempiere.rest.api.util.ThreadLocalTrx;
import com.trekglobal.idempiere.rest.api.v1.resource.BatchRequestResource;

public class BatchRequestResourseImpl implements BatchRequestResource {

    // USE_BIG_DECIMAL_FOR_FLOATS avoids losing precision on Amount/Quantity columns when a
    // sub-response's numeric value is later spliced into a subsequent sub-request via @Table.Column@.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);

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
        // Responses of successful sub-requests, cached by table name and by the sub-request's optional "as" alias,
        // so a later sub-request's body can reference a value from one via "@Table.Column@" / "@bind.Column@".
        // Case-insensitive: table names (and the batch sub-request paths that produce them) are resolved
        // case-insensitively everywhere else in this API (MTable.get), so @bind.Column@ lookups must match.
        Map<String, Object> referenceCache = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        // Canonical table name behind each cache key above (a table name maps to itself; an alias maps to its table),
        // used to resolve the primary-key fallback to the response's "id" property.
        Map<String, String> referenceTableNames = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        // Copy the calling session's context now, before any sub-request runs: Env.getCtx() is a live
        // view that ResponseFilter disposes after every sub-request, so a reference to it (rather than
        // a copy) would go stale mid-batch.
        Properties sessionCtx = new Properties();
        sessionCtx.putAll(Env.getCtx());
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
	                if (!Util.isEmpty(req.getAs(), true)) {
	                	if (MTable.get(sessionCtx, req.getAs()) != null) {
	                		throw new IDempiereRestException("Invalid batch alias",
	                				"'as' value '" + req.getAs() + "' collides with an existing table name and cannot be used as an alias.", Status.BAD_REQUEST);
	                	}
	                	if (referenceCache.containsKey(req.getAs())) {
	                		throw new IDempiereRestException("Invalid batch alias",
	                				"'as' value '" + req.getAs() + "' was already used earlier in this batch.", Status.BAD_REQUEST);
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
	                    resolveReferences(req.getBody(), referenceCache, referenceTableNames, sessionCtx);
	                    byte[] bodyBytes = objectMapper.writeValueAsBytes(req.getBody());
	                    containerRequest.setEntityStream(new ByteArrayInputStream(bodyBytes));
	                }
	                containerRequest.headers(headers.getRequestHeaders());
	
	                Future<ContainerResponse> responseFuture = applicationHandler.apply(containerRequest);
	                ContainerResponse containerResponse = responseFuture.get();
	                int statusCode = containerResponse.getStatus();
	                String entity = containerResponse.getEntity() != null ? containerResponse.getEntity().toString() : null;
	                Map<?, ?> bodyAsMap = null;
	                if (entity != null && !entity.isEmpty()) {
	                	// parse with Jackson (not Gson) so Amount/Quantity columns round-trip as BigDecimal,
	                	// not a lossy double, when referenced by a later sub-request via @Table.Column@.
	                	try {
	                		bodyAsMap = objectMapper.readValue(entity, Map.class);
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
	                	if (bodyAsMap != null) {
	                		String tableName = canonicalTableName(extractTableNameFromPath(req.getPath()), sessionCtx);
	                		if (tableName != null) {
	                			referenceCache.put(tableName, bodyAsMap);
	                			referenceTableNames.put(tableName, tableName);
	                			if (!Util.isEmpty(req.getAs(), true)) {
	                				referenceCache.put(req.getAs(), bodyAsMap);
	                				referenceTableNames.put(req.getAs(), tableName);
	                			}
	                		}
	                	}
	                	if (!transaction) {
	                		Trx threadLocalTrx = Trx.get(ThreadLocalTrx.getTrxName(), false);
	                		if (threadLocalTrx != null && threadLocalTrx.isActive()) {
	                			threadLocalTrx.commit(true);
	                			threadLocalTrx.start(); // Start a new transaction for next request
	                		}
	                	}
	                }
	            } catch (IDempiereRestException e) {
	                Status refStatus = e.getErrorResponseStatus() != null ? e.getErrorResponseStatus() : Status.BAD_REQUEST;
	                results.add(new BatchResponse(refStatus.getReasonPhrase(), refStatus.getStatusCode(), e.getMessage()));
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

    /**
     * Recursively walk a sub-request body (as deserialized by Jackson: nested {@link Map}/{@link List}/scalars)
     * and replace any string value shaped like a "@...@" reference token with the value it references, in place.
     * @param node the body, or a nested object/array within it
     * @param referenceCache successful sub-request responses, keyed by table name and by "as" alias
     * @param referenceTableNames canonical table name behind each referenceCache key
     */
    @SuppressWarnings("unchecked")
    private void resolveReferences(Object node, Map<String, Object> referenceCache, Map<String, String> referenceTableNames, Properties sessionCtx) {
        if (node instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) node).entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String str && isReferenceToken(str)) {
                    entry.setValue(resolveReference(str, referenceCache, referenceTableNames, sessionCtx));
                } else {
                    resolveReferences(value, referenceCache, referenceTableNames, sessionCtx);
                }
            }
        } else if (node instanceof List) {
            List<Object> list = (List<Object>) node;
            for (int i = 0; i < list.size(); i++) {
                Object value = list.get(i);
                if (value instanceof String str && isReferenceToken(str)) {
                    list.set(i, resolveReference(str, referenceCache, referenceTableNames, sessionCtx));
                } else {
                    resolveReferences(value, referenceCache, referenceTableNames, sessionCtx);
                }
            }
        }
    }

    /**
     * A reference token is wrapped on both sides with '@', matching iDempiere's own
     * {@code Evaluator.VARIABLE_START_END_MARKER} convention (e.g. {@code @#AD_Client_ID@}) - not just a
     * value that happens to start with '@'.
     */
    private boolean isReferenceToken(String value) {
        return value.length() > 1 && value.charAt(0) == '@' && value.charAt(value.length() - 1) == '@';
    }

    /**
     * Resolve a single "@...@" token against the sub-requests processed so far in this batch.
     * Grammar: {@code @Table.Column@} / {@code @bind.Column@} (a value from an earlier sub-request's response),
     * or {@code @#GlobalVar@} / {@code @$GlobalVar@} / {@code @+GlobalVar@} (a session/context variable,
     * per {@link Env#isGlobalVariable(String)} - resolved via {@link Env#parseContext} so it follows the
     * same convention as the rest of iDempiere).
     */
    @SuppressWarnings("unchecked")
    private Object resolveReference(String token, Map<String, Object> referenceCache, Map<String, String> referenceTableNames, Properties sessionCtx) {
        String varName = token.substring(1, token.length() - 1);
        if (varName.isEmpty())
            throw new IDempiereRestException("Unresolved batch reference",
                    "Empty reference: " + token, Status.BAD_REQUEST);

        if (Env.isGlobalVariable(varName)) {
            String ctxValue = Env.parseContext(sessionCtx, 0, token, false, false, false, false);
            if (Util.isEmpty(ctxValue))
                throw new IDempiereRestException("Unresolved batch reference",
                        "No value found for context variable: " + token, Status.BAD_REQUEST);
            return ctxValue;
        }

        int dot = varName.indexOf('.');
        if (dot < 0)
            throw new IDempiereRestException("Unresolved batch reference",
                    "Invalid reference syntax: " + token, Status.BAD_REQUEST);

        String bind = varName.substring(0, dot);
        String colName = varName.substring(dot + 1);
        if (colName.indexOf('.') >= 0)
            throw new IDempiereRestException("Unresolved batch reference",
                    "Multi-level reference not supported: " + token, Status.BAD_REQUEST);

        Object cached = referenceCache.get(bind);
        if (!(cached instanceof Map))
            throw new IDempiereRestException("Unresolved batch reference",
                    "No prior successful sub-request found for '" + bind + "'. Referenced by: " + token, Status.BAD_REQUEST);

        Map<String, Object> responseMap = (Map<String, Object>) cached;
        Object value = getIgnoreCase(responseMap, colName);
        if (value == null) {
            String tableName = referenceTableNames.get(bind);
            // nullForMultipleKeys=true: this is a best-effort PK-name check, not a hard requirement -
            // tables with no/composite keys just don't get the "id" fallback.
            String keyColumn = tableName != null ? RestUtils.getKeyColumnName(tableName, true) : null;
            if (keyColumn != null && keyColumn.equalsIgnoreCase(colName))
                value = responseMap.get("id");
        }
        if (value == null)
            throw new IDempiereRestException("Unresolved batch reference",
                    "Column '" + colName + "' not found in response for '" + bind + "'. Referenced by: " + token, Status.BAD_REQUEST);

        return value;
    }

    private Object getIgnoreCase(Map<String, Object> map, String key) {
        Object direct = map.get(key);
        if (direct != null)
            return direct;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key))
                return entry.getValue();
        }
        return null;
    }

    /**
     * Extract the raw {tableName} path segment from a "v1/models/{tableName}[/{id}]" batch sub-request path.
     * Only "models" (plural) is ever actually routed - {@code ModelResource}'s {@code @Path} is "v1/models".
     * (This class's own Javadoc example uses the singular "v1/model/...", but that's a pre-existing typo in
     * the upstream docstring, not a real route - not matched here.)
     */
    private String extractTableNameFromPath(String path) {
        if (path == null)
            return null;
        String[] segments = path.split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            if ("models".equalsIgnoreCase(segments[i]))
                return segments[i + 1];
        }
        return null;
    }

    private String canonicalTableName(String rawTableName, Properties sessionCtx) {
        if (rawTableName == null)
            return null;
        MTable table = MTable.get(sessionCtx, rawTableName);
        return table != null ? table.getTableName() : null;
    }
}

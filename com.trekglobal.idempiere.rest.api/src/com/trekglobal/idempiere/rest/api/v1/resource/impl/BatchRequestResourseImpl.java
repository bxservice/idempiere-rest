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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.util.ThreadLocalTrx;
import com.trekglobal.idempiere.rest.api.v1.resource.BatchRequestResource;

public class BatchRequestResourseImpl implements BatchRequestResource {

    // USE_BIG_DECIMAL_FOR_FLOATS avoids losing precision on Amount/Quantity columns when a
    // sub-response's numeric value is later spliced into a subsequent sub-request via a JSONPath reference.
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
        // Copy the calling session's context now, before any sub-request runs: Env.getCtx() is a live
        // view that ResponseFilter disposes after every sub-request, so a reference to it (rather than
        // a copy) would go stale mid-batch.
        Properties sessionCtx = new Properties();
        sessionCtx.putAll(Env.getCtx());
        // Resolves reference values (see BatchReferenceResolver) against sub-request responses
        // accumulated as this batch runs.
        BatchReferenceResolver referenceResolver = new BatchReferenceResolver(sessionCtx);
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
	                if (!Util.isEmpty(req.getResponseAlias(), true) && referenceResolver.isAliasTaken(req.getResponseAlias())) {
	                	throw new IDempiereRestException("Invalid batch alias",
	                			"'responseAlias' value '" + req.getResponseAlias() + "' was already used earlier in this batch.", Status.BAD_REQUEST);
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
	                    referenceResolver.resolveReferences(req.getBody());
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
	                	// not a lossy double, when referenced by a later sub-request via a JSONPath reference.
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
	                		referenceResolver.cacheResponse(req.getResponseAlias(), bodyAsMap);
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
     * Resolves reference values against sub-request responses accumulated as one batch runs.
     * One instance is scoped to a single {@code processBatch} call.
     * <p>
     * Two independent grammars, both wrapped in '@' - matching iDempiere's own pre-existing
     * {@code Evaluator.VARIABLE_START_END_MARKER} convention (used for context vars in message texts,
     * SQL, print formats, etc.) rather than inventing a marker of our own - so an ordinary literal that
     * happens to contain '$' (a price, a bracketed SKU) is never mistaken for a reference just because
     * of its shape. Distinguished by what's inside the wrapper - see {@link #resolveReference}:
     * <ul>
     * <li>{@code @alias$.jsonPathExpr@} - a standard JSONPath (RFC 9535) against a prior sub-request's
     * response, cached under its optional, batch-unique {@code responseAlias}. JSONPath itself requires
     * every expression to start with its root marker '$', so the alias and the path are unambiguous
     * once split on it (see {@link #isJsonPathReferenceToken}); the {@code JsonPath} library itself is
     * the sole authority on whether what follows is valid syntax.</li>
     * <li>{@code @#GlobalVar@} / {@code @$GlobalVar@} / {@code @+GlobalVar@} - a session/context variable.</li>
     * </ul>
     */
    private static class BatchReferenceResolver {

        // Responses of successful sub-requests, cached by the sub-request's optional, batch-unique
        // responseAlias, so a later sub-request's body can reference a value from one via "@alias$.jsonPathExpr@".
        private final Map<String, Object> referenceCache = new HashMap<>();
        private final Properties sessionCtx;

        BatchReferenceResolver(Properties sessionCtx) {
            this.sessionCtx = sessionCtx;
        }

        boolean isAliasTaken(String alias) {
            return referenceCache.containsKey(alias);
        }

        /**
         * Cache a successful sub-request's response under its response alias, if it was given one.
         * @param alias the sub-request's "responseAlias" value, or null/empty if none was given
         * @param bodyAsMap the sub-request's response body
         */
        void cacheResponse(String alias, Object bodyAsMap) {
            if (!Util.isEmpty(alias, true))
                referenceCache.put(alias, bodyAsMap);
        }

        /**
         * Recursively walk a sub-request body (as deserialized by Jackson: nested {@link Map}/{@link List}/scalars)
         * and replace any string value shaped like a reference with the value it references, in place.
         * @param node the body, or a nested object/array within it
         */
        @SuppressWarnings("unchecked")
        void resolveReferences(Object node) {
            if (node instanceof Map) {
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) node).entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String str && isReference(str)) {
                        entry.setValue(resolveReference(str));
                    } else {
                        resolveReferences(value);
                    }
                }
            } else if (node instanceof List) {
                List<Object> list = (List<Object>) node;
                for (int i = 0; i < list.size(); i++) {
                    Object value = list.get(i);
                    if (value instanceof String str && isReference(str)) {
                        list.set(i, resolveReference(str));
                    } else {
                        resolveReferences(value);
                    }
                }
            }
        }

        private boolean isReference(String value) {
            return isGlobalVariableToken(value) || isJsonPathReferenceToken(value);
        }

        /**
         * Wrapped on both sides with '@', matching iDempiere's own {@code Evaluator.VARIABLE_START_END_MARKER}
         * convention (e.g. {@code @#AD_Client_ID@}) - not just any value that happens to start and end with
         * '@'. The inner content must also look like a real global variable ({@link Env#isGlobalVariable}'s
         * '#'/'$'/'+' prefixes), so an unrelated literal like {@code @example@} is left untouched instead of
         * being rejected as an unresolved reference.
         */
        private boolean isGlobalVariableToken(String value) {
            return value.length() > 2 && value.charAt(0) == '@' && value.charAt(value.length() - 1) == '@'
                    && Env.isGlobalVariable(value.substring(1, value.length() - 1));
        }

        /**
         * A wrapped {@code @alias$.jsonPathExpr@} / {@code @alias$[jsonPathExpr]@} value: same '@...@'
         * wrapping as {@link #isGlobalVariableToken}, so ordinary literal data is never mistaken for a
         * reference just because it happens to contain '$'. Inside the wrapper, an identifier immediately
         * followed by JSONPath's own root marker '$' (and not itself a global variable, which would have
         * that marker right after the opening '@') is what marks this grammar - the {@code JsonPath}
         * library itself is the authority on whether what follows is valid syntax.
         */
        private boolean isJsonPathReferenceToken(String value) {
            if (value.length() <= 2 || value.charAt(0) != '@' || value.charAt(value.length() - 1) != '@')
                return false;
            String inner = value.substring(1, value.length() - 1);
            int dollar = inner.indexOf('$');
            return dollar > 0 && dollar + 1 < inner.length();
        }

        /**
         * Resolve a single reference against the sub-requests processed so far in this batch, or against
         * the session/context - see the class Javadoc for the two grammars.
         */
        private Object resolveReference(String token) {
            if (isGlobalVariableToken(token))
                return resolveGlobalVariable(token);
            return resolveJsonPath(token);
        }

        private String resolveGlobalVariable(String token) {
            String ctxValue = Env.parseContext(sessionCtx, 0, token, false, false, false, false);
            if (Util.isEmpty(ctxValue))
                throw new IDempiereRestException("Unresolved batch reference",
                        "No value found for context variable: " + token, Status.BAD_REQUEST);
            return ctxValue;
        }

        private Object resolveJsonPath(String token) {
            String inner = token.substring(1, token.length() - 1);
            int dollar = inner.indexOf('$');
            String alias = inner.substring(0, dollar);
            String jsonPath = inner.substring(dollar);

            Object cached = referenceCache.get(alias);
            if (!(cached instanceof Map))
                throw new IDempiereRestException("Unresolved batch reference",
                        "No prior successful sub-request found for responseAlias '" + alias + "'. Referenced by: " + token, Status.BAD_REQUEST);

            try {
                return JsonPath.read(cached, jsonPath);
            } catch (InvalidPathException e) {
                throw new IDempiereRestException("Unresolved batch reference",
                        "JSONPath '" + jsonPath + "' not found in response for responseAlias '" + alias + "'. Referenced by: " + token, Status.BAD_REQUEST);
            }
        }
    }
}

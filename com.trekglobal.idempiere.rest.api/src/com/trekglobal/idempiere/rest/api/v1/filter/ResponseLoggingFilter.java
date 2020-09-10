package com.trekglobal.idempiere.rest.api.v1.filter;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.compiere.util.Util;

/**
 * Log Response Information
 * @author Igor Pojzl
 *
 */
@Provider
public class ResponseLoggingFilter implements ContainerResponseFilter {

	private Logger logger = LogManager.getLogger();
	
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		//Add Request Order
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();
		String request_ID = requestContext.getHeaderString("X-Request-ID");
		if(!headers.containsKey("X-Request-ID")) {			
			if(Util.isEmpty(request_ID))
				request_ID = UUID.randomUUID().toString();
			headers.putSingle("X-Request-ID", request_ID);			
		} else {
			request_ID = (String) headers.getFirst("X-Request-ID");
		}
		
        // Log Inforation as Map to for JSON parse
		Map<String, Object> logMap = new TreeMap<>();
		logMap.put("id", request_ID);
		logMap.put("uri", requestContext.getUriInfo().getAbsolutePath());
		logMap.put("method", requestContext.getMethod());
		logMap.put("status", responseContext.getStatus());
		logMap.put("duration", (System.currentTimeMillis() - ((Long) requestContext.getProperty(RequestLoggingFilter.REQUEST_START_TIME))) + "ms");
		logger.info(logMap);
	}
}
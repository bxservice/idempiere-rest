package com.trekglobal.idempiere.rest.api.v1.filter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;

/**
 * Request Logging Filter
 * Add Start time for log Processing Time
 * @author Igor Pojzl
 *
 */
@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class RequestLoggingFilter implements ContainerRequestFilter {

	public static final String REQUEST_START_TIME = "RequestStartTime";
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		requestContext.setProperty(REQUEST_START_TIME, System.currentTimeMillis());
		
		 if (requestContext.hasEntity()) {
	           String entity = getEntityBody(requestContext);
	           requestContext.setProperty(ResponseLoggingFilter.REQUEST_BODY_PROPERTY, entity);
        }
	}
	
	private String getEntityBody(ContainerRequestContext requestContext) {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    InputStream in = requestContext.getEntityStream();

	    final StringBuilder body = new StringBuilder();
	    try {
	        IOUtils.copy(in, out);

	        byte[] requestEntity = out.toByteArray();
	        body.append(new String(requestEntity));
	        requestContext.setEntityStream(new ByteArrayInputStream(requestEntity));

	    } catch (IOException e) {
	    }
	    return body.toString();
	}

}

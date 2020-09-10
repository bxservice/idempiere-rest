package com.trekglobal.idempiere.rest.api.v1.filter;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

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
	}

}
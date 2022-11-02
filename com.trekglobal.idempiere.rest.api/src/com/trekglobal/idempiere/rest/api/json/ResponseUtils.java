package com.trekglobal.idempiere.rest.api.json;

import java.util.logging.Level;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.util.CLogger;

import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;

public class ResponseUtils {

	private final static CLogger log = CLogger.getCLogger(ResponseUtils.class);

	public static Response getResponseErrorFromException(Exception ex, String title, String detailText) {
		Status status = Status.INTERNAL_SERVER_ERROR;
		if (ex instanceof IDempiereRestException) {
			status = ((IDempiereRestException) ex).getErrorResponseStatus();
			title = ((IDempiereRestException) ex).getTitle();
		}

		log.log(Level.SEVERE, ex.getMessage(), ex);
		return getResponseError(status, title, detailText, ex.getMessage());
	}
	
	public static Response getResponseError(Status status, String title, String text1, String text2) {
		return Response.status(status)
				.entity(new ErrorBuilder()
						.status(status)
						.title(title)
						.append(text1)
						.append(text2)
						.build()
						.toString())
				.build();
	}
	
}

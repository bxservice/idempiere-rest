package com.trekglobal.idempiere.rest.api.json;

import javax.ws.rs.core.Response.Status;

import org.adempiere.exceptions.AdempiereException;

public class IDempiereRestException extends AdempiereException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3555412217944639699L;
	
	private Status status;
	
	/**
	 * @param message
	 * @param error -> Response status error
	 */
	public IDempiereRestException(String message, Status error) {
		super(message);
		setErrorResponseStatus(error);
	}
	
	public void setErrorResponseStatus(Status status) {
		this.status = status;
	}
	
	public Status getErrorResponseStatus() {
		return status;
	}

}

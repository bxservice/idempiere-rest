package com.trekglobal.idempiere.rest.api.json;

import javax.ws.rs.core.Response.Status;

import org.adempiere.exceptions.AdempiereException;

public class IDempiereRestException extends AdempiereException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3555412217944639699L;
	
	private Status status;
	private String title;
	
	/**
	 * @param message
	 * @param error -> Response status error
	 */
	public IDempiereRestException(String message, Status error) {
		super(message);
		setErrorResponseStatus(error);
	}
	
	/**
	 * @param title
	 * @param message
	 * @param error -> Response status error
	 */
	public IDempiereRestException(String title, String message, Status error) {
		super(message);
		setErrorResponseStatus(error);
		setTitle(title);
	}
	
	public void setErrorResponseStatus(Status status) {
		this.status = status;
	}
	
	public Status getErrorResponseStatus() {
		return status;
	}
	
	public void setTitle(String title) {
		this.title= title;
	}
	
	public String getTitle() {
		return title;
	}
}

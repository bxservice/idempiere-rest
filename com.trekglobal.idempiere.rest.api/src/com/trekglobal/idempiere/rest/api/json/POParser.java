package com.trekglobal.idempiere.rest.api.json;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.PO;

public class POParser {
	
	//TODO: Replace with Msg calls to make it translatable
	private static final String ACCESS_DENIED_TITLE = "Access denied";
	private static final String ACCESS_DENIED_MESSAGE = "Access denied for record with id: ";
	private static final String NOT_FOUND_TITLE = "Record not found";
    private static final String NOT_FOUND_MESSAGE = "No record found matching id: ";

	private PO po;
	private String tableName;
	private String recordID;
	private Response responseError;
	
	public POParser(String tableName, String recordID, boolean fullyQualifiedWhere, boolean isReadWrite) {
		this.tableName = tableName;
		this.recordID = recordID;
		if (isValidTable(isReadWrite))
			po = RestUtils.getPO(tableName, recordID, fullyQualifiedWhere, isReadWrite);
	}
	
	private boolean isValidTable(boolean isReadWrite) {
		try {
			//Call to check if user has access to the table
			RestUtils.getTable(tableName, isReadWrite);
		} catch (Exception ex) {
			responseError = ResponseUtils.getResponseErrorFromException(ex, "", "");
			return false;
		}
		return true;
	}
	
	public POParser(String tableName, String recordID, PO po) {
		this.tableName = tableName;
		this.recordID = recordID;
		this.po = po;
	}
	
	public boolean isValidPO() {
		return po != null;
	}
	
	public PO getPO() {
		return po;
	}
	
	public Response getResponseError() {
		if (responseError != null)
			return responseError;

		po = RestUtils.getPO(tableName, recordID, false, false);

		if (po != null) {
			return ResponseUtils.getResponseError(Status.FORBIDDEN, ACCESS_DENIED_TITLE, ACCESS_DENIED_MESSAGE, recordID);
		} else {
			return ResponseUtils.getResponseError(Status.NOT_FOUND, NOT_FOUND_TITLE, NOT_FOUND_MESSAGE, recordID);
		}
	}
}

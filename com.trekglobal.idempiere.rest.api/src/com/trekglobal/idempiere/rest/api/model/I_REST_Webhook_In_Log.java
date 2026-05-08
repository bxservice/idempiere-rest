/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package com.trekglobal.idempiere.rest.api.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for REST_Webhook_In_Log
 *  @author iDempiere (generated) 
 *  @version Release 14
 */
@SuppressWarnings("all")
public interface I_REST_Webhook_In_Log 
{

    /** TableName=REST_Webhook_In_Log */
    public static final String Table_Name = "REST_Webhook_In_Log";

    /** AD_Table_ID=200444 */
    public static final int Table_ID = 200444;

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 7 - System - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(7);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Tenant.
	  * Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within tenant
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within tenant
	  */
	public int getAD_Org_ID();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name ErrorMessage */
    public static final String COLUMNNAME_ErrorMessage = "ErrorMessage";

	/** Set Error Message.
	  * Error details from the last failed attempt (connection errors, timeouts, etc.)
	  */
	public void setErrorMessage (String ErrorMessage);

	/** Get Error Message.
	  * Error details from the last failed attempt (connection errors, timeouts, etc.)
	  */
	public String getErrorMessage();

    /** Column name HttpStatus */
    public static final String COLUMNNAME_HttpStatus = "HttpStatus";

	/** Set HTTP Status.
	  * HTTP response status code from the last delivery attempt
	  */
	public void setHttpStatus (int HttpStatus);

	/** Get HTTP Status.
	  * HTTP response status code from the last delivery attempt
	  */
	public int getHttpStatus();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name IsError */
    public static final String COLUMNNAME_IsError = "IsError";

	/** Set Error.
	  * An Error occurred in the execution
	  */
	public void setIsError (boolean IsError);

	/** Get Error.
	  * An Error occurred in the execution
	  */
	public boolean isError();

    /** Column name Payload */
    public static final String COLUMNNAME_Payload = "Payload";

	/** Set Payload.
	  * Full JSON payload sent in the webhook body. Standard Webhooks envelope with type, timestamp, and data fields.
	  */
	public void setPayload (String Payload);

	/** Get Payload.
	  * Full JSON payload sent in the webhook body. Standard Webhooks envelope with type, timestamp, and data fields.
	  */
	public String getPayload();

    /** Column name Processed */
    public static final String COLUMNNAME_Processed = "Processed";

	/** Set Processed.
	  * The document has been processed
	  */
	public void setProcessed (boolean Processed);

	/** Get Processed.
	  * The document has been processed
	  */
	public boolean isProcessed();

    /** Column name ProcessedAt */
    public static final String COLUMNNAME_ProcessedAt = "ProcessedAt";

	/** Set Processed At.
	  * Timestamp when processing completed
	  */
	public void setProcessedAt (Timestamp ProcessedAt);

	/** Get Processed At.
	  * Timestamp when processing completed
	  */
	public Timestamp getProcessedAt();

    /** Column name REST_Webhook_In_ID */
    public static final String COLUMNNAME_REST_Webhook_In_ID = "REST_Webhook_In_ID";

	/** Set REST Webhook In.
	  * Inbound webhook endpoint configuration
	  */
	public void setREST_Webhook_In_ID (int REST_Webhook_In_ID);

	/** Get REST Webhook In.
	  * Inbound webhook endpoint configuration
	  */
	public int getREST_Webhook_In_ID();

	@Deprecated(since="13") // use better methods with cache
	public com.trekglobal.idempiere.rest.api.model.I_REST_Webhook_In getREST_Webhook_In() throws RuntimeException;

    /** Column name REST_Webhook_In_Log_ID */
    public static final String COLUMNNAME_REST_Webhook_In_Log_ID = "REST_Webhook_In_Log_ID";

	/** Set REST Webhook In Log.
	  * Inbound webhook delivery log
	  */
	public void setREST_Webhook_In_Log_ID (int REST_Webhook_In_Log_ID);

	/** Get REST Webhook In Log.
	  * Inbound webhook delivery log
	  */
	public int getREST_Webhook_In_Log_ID();

    /** Column name REST_Webhook_In_Log_UU */
    public static final String COLUMNNAME_REST_Webhook_In_Log_UU = "REST_Webhook_In_Log_UU";

	/** Set REST Webhook In Log UU	  */
	public void setREST_Webhook_In_Log_UU (String REST_Webhook_In_Log_UU);

	/** Get REST Webhook In Log UU	  */
	public String getREST_Webhook_In_Log_UU();

    /** Column name RemoteAddr */
    public static final String COLUMNNAME_RemoteAddr = "RemoteAddr";

	/** Set Remote Address.
	  * IP address of the caller
	  */
	public void setRemoteAddr (String RemoteAddr);

	/** Get Remote Address.
	  * IP address of the caller
	  */
	public String getRemoteAddr();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();

    /** Column name WebhookMessageId */
    public static final String COLUMNNAME_WebhookMessageId = "WebhookMessageId";

	/** Set Webhook Message ID.
	  * Unique message identifier (msg_XXX format). Remains constant across retries. Used as idempotency key by receivers.
	  */
	public void setWebhookMessageId (String WebhookMessageId);

	/** Get Webhook Message ID.
	  * Unique message identifier (msg_XXX format). Remains constant across retries. Used as idempotency key by receivers.
	  */
	public String getWebhookMessageId();
}

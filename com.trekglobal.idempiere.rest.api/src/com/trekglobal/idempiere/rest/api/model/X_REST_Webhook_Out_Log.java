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
/** Generated Model - DO NOT CHANGE */
package com.trekglobal.idempiere.rest.api.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for REST_Webhook_Out_Log
 *  @author iDempiere (generated)
 *  @version Release 14 - $Id$ */
@org.adempiere.base.Model(table="REST_Webhook_Out_Log")
public class X_REST_Webhook_Out_Log extends PO implements I_REST_Webhook_Out_Log, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260413L;

    /** Standard Constructor */
    public X_REST_Webhook_Out_Log (Properties ctx, int REST_Webhook_Out_Log_ID, String trxName)
    {
      super (ctx, REST_Webhook_Out_Log_ID, trxName);
      /** if (REST_Webhook_Out_Log_ID == 0)
        {
			setAttempts (0);
// 0
			setDeliveryStatus (null);
// P
			setPayload (null);
			setREST_Webhook_Out_ID (0);
			setREST_Webhook_Out_Log_ID (0);
			setRecord_ID (0);
			setWebhookMessageId (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_Webhook_Out_Log (Properties ctx, int REST_Webhook_Out_Log_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_Webhook_Out_Log_ID, trxName, virtualColumns);
      /** if (REST_Webhook_Out_Log_ID == 0)
        {
			setAttempts (0);
// 0
			setDeliveryStatus (null);
// P
			setPayload (null);
			setREST_Webhook_Out_ID (0);
			setREST_Webhook_Out_Log_ID (0);
			setRecord_ID (0);
			setWebhookMessageId (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_Webhook_Out_Log (Properties ctx, String REST_Webhook_Out_Log_UU, String trxName)
    {
      super (ctx, REST_Webhook_Out_Log_UU, trxName);
      /** if (REST_Webhook_Out_Log_UU == null)
        {
			setAttempts (0);
// 0
			setDeliveryStatus (null);
// P
			setPayload (null);
			setREST_Webhook_Out_ID (0);
			setREST_Webhook_Out_Log_ID (0);
			setRecord_ID (0);
			setWebhookMessageId (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_Webhook_Out_Log (Properties ctx, String REST_Webhook_Out_Log_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_Webhook_Out_Log_UU, trxName, virtualColumns);
      /** if (REST_Webhook_Out_Log_UU == null)
        {
			setAttempts (0);
// 0
			setDeliveryStatus (null);
// P
			setPayload (null);
			setREST_Webhook_Out_ID (0);
			setREST_Webhook_Out_Log_ID (0);
			setRecord_ID (0);
			setWebhookMessageId (null);
        } */
    }

    /** Load Constructor */
    public X_REST_Webhook_Out_Log (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 7 - System - Client - Org
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_REST_Webhook_Out_Log[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Table getAD_Table() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Table)MTable.get(getCtx(), org.compiere.model.I_AD_Table.Table_ID)
			.getPO(getAD_Table_ID(), get_TrxName());
	}

	/** Set Table.
		@param AD_Table_ID Database Table information
	*/
	public void setAD_Table_ID (int AD_Table_ID)
	{
		if (AD_Table_ID < 1)
			set_Value (COLUMNNAME_AD_Table_ID, null);
		else
			set_Value (COLUMNNAME_AD_Table_ID, Integer.valueOf(AD_Table_ID));
	}

	/** Get Table.
		@return Database Table information
	  */
	public int getAD_Table_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Table_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Attempts.
		@param Attempts Number of delivery attempts made. Incremented on each try.
	*/
	public void setAttempts (int Attempts)
	{
		set_Value (COLUMNNAME_Attempts, Integer.valueOf(Attempts));
	}

	/** Get Attempts.
		@return Number of delivery attempts made. Incremented on each try.
	  */
	public int getAttempts()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Attempts);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** DeliveryStatus AD_Reference_ID=200285 */
	public static final int DELIVERYSTATUS_AD_Reference_ID=200285;
	/** Abandoned = A */
	public static final String DELIVERYSTATUS_Abandoned = "A";
	/** Failed = F */
	public static final String DELIVERYSTATUS_Failed = "F";
	/** Pending = P */
	public static final String DELIVERYSTATUS_Pending = "P";
	/** Success = S */
	public static final String DELIVERYSTATUS_Success = "S";
	/** Set Delivery Status.
		@param DeliveryStatus Current delivery status: Pending, Success, Failed, or Abandoned
	*/
	public void setDeliveryStatus (String DeliveryStatus)
	{

		set_Value (COLUMNNAME_DeliveryStatus, DeliveryStatus);
	}

	/** Get Delivery Status.
		@return Current delivery status: Pending, Success, Failed, or Abandoned
	  */
	public String getDeliveryStatus()
	{
		return (String)get_Value(COLUMNNAME_DeliveryStatus);
	}

	/** Set Error Message.
		@param ErrorMessage Error details from the last failed attempt (connection errors, timeouts, etc.)
	*/
	public void setErrorMessage (String ErrorMessage)
	{
		set_Value (COLUMNNAME_ErrorMessage, ErrorMessage);
	}

	/** Get Error Message.
		@return Error details from the last failed attempt (connection errors, timeouts, etc.)
	  */
	public String getErrorMessage()
	{
		return (String)get_Value(COLUMNNAME_ErrorMessage);
	}

	/** EventType AD_Reference_ID=53237 */
	public static final int EVENTTYPE_AD_Reference_ID=53237;
	/** Document After Reactivate = DAAC */
	public static final String EVENTTYPE_DocumentAfterReactivate = "DAAC";
	/** Document After Close = DACL */
	public static final String EVENTTYPE_DocumentAfterClose = "DACL";
	/** Document After Complete = DACO */
	public static final String EVENTTYPE_DocumentAfterComplete = "DACO";
	/** Document After Post = DAPO */
	public static final String EVENTTYPE_DocumentAfterPost = "DAPO";
	/** Document After Prepare = DAPR */
	public static final String EVENTTYPE_DocumentAfterPrepare = "DAPR";
	/** Document After Reverse Accrual = DARA */
	public static final String EVENTTYPE_DocumentAfterReverseAccrual = "DARA";
	/** Document After Reverse Correct = DARC */
	public static final String EVENTTYPE_DocumentAfterReverseCorrect = "DARC";
	/** Document After Void = DAVO */
	public static final String EVENTTYPE_DocumentAfterVoid = "DAVO";
	/** Document Before Reactivate = DBAC */
	public static final String EVENTTYPE_DocumentBeforeReactivate = "DBAC";
	/** Document Before Close = DBCL */
	public static final String EVENTTYPE_DocumentBeforeClose = "DBCL";
	/** Document Before Complete = DBCO */
	public static final String EVENTTYPE_DocumentBeforeComplete = "DBCO";
	/** Document Before Post = DBPO */
	public static final String EVENTTYPE_DocumentBeforePost = "DBPO";
	/** Document Before Prepare = DBPR */
	public static final String EVENTTYPE_DocumentBeforePrepare = "DBPR";
	/** Document Before Reverse Accrual = DBRA */
	public static final String EVENTTYPE_DocumentBeforeReverseAccrual = "DBRA";
	/** Document Before Reverse Correct = DBRC */
	public static final String EVENTTYPE_DocumentBeforeReverseCorrect = "DBRC";
	/** Document Before Void = DBVO */
	public static final String EVENTTYPE_DocumentBeforeVoid = "DBVO";
	/** Table After Change = TAC */
	public static final String EVENTTYPE_TableAfterChange = "TAC";
	/** Table After Change Replication = TACR */
	public static final String EVENTTYPE_TableAfterChangeReplication = "TACR";
	/** Table After Delete = TAD */
	public static final String EVENTTYPE_TableAfterDelete = "TAD";
	/** Table After New = TAN */
	public static final String EVENTTYPE_TableAfterNew = "TAN";
	/** Table After New Replication = TANR */
	public static final String EVENTTYPE_TableAfterNewReplication = "TANR";
	/** Table Before Change = TBC */
	public static final String EVENTTYPE_TableBeforeChange = "TBC";
	/** Table Before Delete = TBD */
	public static final String EVENTTYPE_TableBeforeDelete = "TBD";
	/** Table Before Delete Replication = TBDR */
	public static final String EVENTTYPE_TableBeforeDeleteReplication = "TBDR";
	/** Table Before New = TBN */
	public static final String EVENTTYPE_TableBeforeNew = "TBN";
	/** Set Event Type.
		@param EventType Type of Event
	*/
	public void setEventType (String EventType)
	{

		set_Value (COLUMNNAME_EventType, EventType);
	}

	/** Get Event Type.
		@return Type of Event
	  */
	public String getEventType()
	{
		return (String)get_Value(COLUMNNAME_EventType);
	}

	/** Set HTTP Status.
		@param HttpStatus HTTP response status code from the last delivery attempt
	*/
	public void setHttpStatus (int HttpStatus)
	{
		set_Value (COLUMNNAME_HttpStatus, Integer.valueOf(HttpStatus));
	}

	/** Get HTTP Status.
		@return HTTP response status code from the last delivery attempt
	  */
	public int getHttpStatus()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_HttpStatus);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Next Retry At.
		@param NextRetryAt When the next retry should be attempted. Calculated using exponential backoff per Standard Webhooks spec.
	*/
	public void setNextRetryAt (Timestamp NextRetryAt)
	{
		set_Value (COLUMNNAME_NextRetryAt, NextRetryAt);
	}

	/** Get Next Retry At.
		@return When the next retry should be attempted. Calculated using exponential backoff per Standard Webhooks spec.
	  */
	public Timestamp getNextRetryAt()
	{
		return (Timestamp)get_Value(COLUMNNAME_NextRetryAt);
	}

	/** Set Payload.
		@param Payload Full JSON payload sent in the webhook body. Standard Webhooks envelope with type, timestamp, and data fields.
	*/
	public void setPayload (String Payload)
	{
		set_Value (COLUMNNAME_Payload, Payload);
	}

	/** Get Payload.
		@return Full JSON payload sent in the webhook body. Standard Webhooks envelope with type, timestamp, and data fields.
	  */
	public String getPayload()
	{
		return (String)get_Value(COLUMNNAME_Payload);
	}

	@Deprecated(since="13") // use better methods with cache
	public com.trekglobal.idempiere.rest.api.model.I_REST_Webhook_Out getREST_Webhook_Out() throws RuntimeException
	{
		return (com.trekglobal.idempiere.rest.api.model.I_REST_Webhook_Out)MTable.get(getCtx(), com.trekglobal.idempiere.rest.api.model.I_REST_Webhook_Out.Table_ID)
			.getPO(getREST_Webhook_Out_ID(), get_TrxName());
	}

	/** Set REST Webhook Out.
		@param REST_Webhook_Out_ID Outbound webhook endpoint configuration
	*/
	public void setREST_Webhook_Out_ID (int REST_Webhook_Out_ID)
	{
		if (REST_Webhook_Out_ID < 1)
			set_ValueNoCheck (COLUMNNAME_REST_Webhook_Out_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_REST_Webhook_Out_ID, Integer.valueOf(REST_Webhook_Out_ID));
	}

	/** Get REST Webhook Out.
		@return Outbound webhook endpoint configuration
	  */
	public int getREST_Webhook_Out_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_Webhook_Out_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set REST Webhook Out Log.
		@param REST_Webhook_Out_Log_ID Outbound webhook delivery tracking
	*/
	public void setREST_Webhook_Out_Log_ID (int REST_Webhook_Out_Log_ID)
	{
		if (REST_Webhook_Out_Log_ID < 1)
			set_ValueNoCheck (COLUMNNAME_REST_Webhook_Out_Log_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_REST_Webhook_Out_Log_ID, Integer.valueOf(REST_Webhook_Out_Log_ID));
	}

	/** Get REST Webhook Out Log.
		@return Outbound webhook delivery tracking
	  */
	public int getREST_Webhook_Out_Log_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_Webhook_Out_Log_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set REST Webhook Out Log UU.
		@param REST_Webhook_Out_Log_UU REST Webhook Out Log UU
	*/
	public void setREST_Webhook_Out_Log_UU (String REST_Webhook_Out_Log_UU)
	{
		set_Value (COLUMNNAME_REST_Webhook_Out_Log_UU, REST_Webhook_Out_Log_UU);
	}

	/** Get REST Webhook Out Log UU.
		@return REST Webhook Out Log UU	  */
	public String getREST_Webhook_Out_Log_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_Webhook_Out_Log_UU);
	}

	/** Set Record ID.
		@param Record_ID Direct internal record ID
	*/
	public void setRecord_ID (int Record_ID)
	{
		if (Record_ID < 0)
			set_Value (COLUMNNAME_Record_ID, null);
		else
			set_Value (COLUMNNAME_Record_ID, Integer.valueOf(Record_ID));
	}

	/** Get Record ID.
		@return Direct internal record ID
	  */
	public int getRecord_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Record_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Response Body.
		@param ResponseBody First 2000 characters of the HTTP response body from the last attempt
	*/
	public void setResponseBody (String ResponseBody)
	{
		set_Value (COLUMNNAME_ResponseBody, ResponseBody);
	}

	/** Get Response Body.
		@return First 2000 characters of the HTTP response body from the last attempt
	  */
	public String getResponseBody()
	{
		return (String)get_Value(COLUMNNAME_ResponseBody);
	}

	/** Set Webhook Message ID.
		@param WebhookMessageId Unique message identifier (msg_XXX format). Remains constant across retries. Used as idempotency key by receivers.
	*/
	public void setWebhookMessageId (String WebhookMessageId)
	{
		set_Value (COLUMNNAME_WebhookMessageId, WebhookMessageId);
	}

	/** Get Webhook Message ID.
		@return Unique message identifier (msg_XXX format). Remains constant across retries. Used as idempotency key by receivers.
	  */
	public String getWebhookMessageId()
	{
		return (String)get_Value(COLUMNNAME_WebhookMessageId);
	}
}
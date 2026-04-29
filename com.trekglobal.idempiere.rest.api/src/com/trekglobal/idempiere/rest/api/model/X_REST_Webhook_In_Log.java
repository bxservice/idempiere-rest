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

/** Generated Model for REST_Webhook_In_Log
 *  @author iDempiere (generated)
 *  @version Release 14 - $Id$ */
@org.adempiere.base.Model(table="REST_Webhook_In_Log")
public class X_REST_Webhook_In_Log extends PO implements I_REST_Webhook_In_Log, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260413L;

    /** Standard Constructor */
    public X_REST_Webhook_In_Log (Properties ctx, int REST_Webhook_In_Log_ID, String trxName)
    {
      super (ctx, REST_Webhook_In_Log_ID, trxName);
      /** if (REST_Webhook_In_Log_ID == 0)
        {
			setIsError (false);
// N
			setProcessed (false);
// N
			setREST_Webhook_In_ID (0);
			setREST_Webhook_In_Log_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_Webhook_In_Log (Properties ctx, int REST_Webhook_In_Log_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_Webhook_In_Log_ID, trxName, virtualColumns);
      /** if (REST_Webhook_In_Log_ID == 0)
        {
			setIsError (false);
// N
			setProcessed (false);
// N
			setREST_Webhook_In_ID (0);
			setREST_Webhook_In_Log_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_Webhook_In_Log (Properties ctx, String REST_Webhook_In_Log_UU, String trxName)
    {
      super (ctx, REST_Webhook_In_Log_UU, trxName);
      /** if (REST_Webhook_In_Log_UU == null)
        {
			setIsError (false);
// N
			setProcessed (false);
// N
			setREST_Webhook_In_ID (0);
			setREST_Webhook_In_Log_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_Webhook_In_Log (Properties ctx, String REST_Webhook_In_Log_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_Webhook_In_Log_UU, trxName, virtualColumns);
      /** if (REST_Webhook_In_Log_UU == null)
        {
			setIsError (false);
// N
			setProcessed (false);
// N
			setREST_Webhook_In_ID (0);
			setREST_Webhook_In_Log_ID (0);
        } */
    }

    /** Load Constructor */
    public X_REST_Webhook_In_Log (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_REST_Webhook_In_Log[")
        .append(get_ID()).append("]");
      return sb.toString();
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

	/** Set Error.
		@param IsError An Error occurred in the execution
	*/
	public void setIsError (boolean IsError)
	{
		set_Value (COLUMNNAME_IsError, Boolean.valueOf(IsError));
	}

	/** Get Error.
		@return An Error occurred in the execution
	  */
	public boolean isError()
	{
		Object oo = get_Value(COLUMNNAME_IsError);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
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

	/** Set Processed.
		@param Processed The document has been processed
	*/
	public void setProcessed (boolean Processed)
	{
		set_Value (COLUMNNAME_Processed, Boolean.valueOf(Processed));
	}

	/** Get Processed.
		@return The document has been processed
	  */
	public boolean isProcessed()
	{
		Object oo = get_Value(COLUMNNAME_Processed);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Processed At.
		@param ProcessedAt Timestamp when processing completed
	*/
	public void setProcessedAt (Timestamp ProcessedAt)
	{
		set_Value (COLUMNNAME_ProcessedAt, ProcessedAt);
	}

	/** Get Processed At.
		@return Timestamp when processing completed
	  */
	public Timestamp getProcessedAt()
	{
		return (Timestamp)get_Value(COLUMNNAME_ProcessedAt);
	}

	@Deprecated(since="13") // use better methods with cache
	public com.trekglobal.idempiere.rest.api.model.I_REST_Webhook_In getREST_Webhook_In() throws RuntimeException
	{
		return (com.trekglobal.idempiere.rest.api.model.I_REST_Webhook_In)MTable.get(getCtx(), com.trekglobal.idempiere.rest.api.model.I_REST_Webhook_In.Table_ID)
			.getPO(getREST_Webhook_In_ID(), get_TrxName());
	}

	/** Set REST Webhook In.
		@param REST_Webhook_In_ID Inbound webhook endpoint configuration
	*/
	public void setREST_Webhook_In_ID (int REST_Webhook_In_ID)
	{
		if (REST_Webhook_In_ID < 1)
			set_ValueNoCheck (COLUMNNAME_REST_Webhook_In_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_REST_Webhook_In_ID, Integer.valueOf(REST_Webhook_In_ID));
	}

	/** Get REST Webhook In.
		@return Inbound webhook endpoint configuration
	  */
	public int getREST_Webhook_In_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_Webhook_In_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set REST Webhook In Log.
		@param REST_Webhook_In_Log_ID Inbound webhook delivery log
	*/
	public void setREST_Webhook_In_Log_ID (int REST_Webhook_In_Log_ID)
	{
		if (REST_Webhook_In_Log_ID < 1)
			set_ValueNoCheck (COLUMNNAME_REST_Webhook_In_Log_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_REST_Webhook_In_Log_ID, Integer.valueOf(REST_Webhook_In_Log_ID));
	}

	/** Get REST Webhook In Log.
		@return Inbound webhook delivery log
	  */
	public int getREST_Webhook_In_Log_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_Webhook_In_Log_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set REST Webhook In Log UU.
		@param REST_Webhook_In_Log_UU REST Webhook In Log UU
	*/
	public void setREST_Webhook_In_Log_UU (String REST_Webhook_In_Log_UU)
	{
		set_Value (COLUMNNAME_REST_Webhook_In_Log_UU, REST_Webhook_In_Log_UU);
	}

	/** Get REST Webhook In Log UU.
		@return REST Webhook In Log UU	  */
	public String getREST_Webhook_In_Log_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_Webhook_In_Log_UU);
	}

	/** Set Remote Address.
		@param RemoteAddr IP address of the caller
	*/
	public void setRemoteAddr (String RemoteAddr)
	{
		set_Value (COLUMNNAME_RemoteAddr, RemoteAddr);
	}

	/** Get Remote Address.
		@return IP address of the caller
	  */
	public String getRemoteAddr()
	{
		return (String)get_Value(COLUMNNAME_RemoteAddr);
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
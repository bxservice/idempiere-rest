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
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for REST_Webhook_Out_Event
 *  @author iDempiere (generated)
 *  @version Release 14 - $Id$ */
@org.adempiere.base.Model(table="REST_Webhook_Out_Event")
public class X_REST_Webhook_Out_Event extends PO implements I_REST_Webhook_Out_Event, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260413L;

    /** Standard Constructor */
    public X_REST_Webhook_Out_Event (Properties ctx, int REST_Webhook_Out_Event_ID, String trxName)
    {
      super (ctx, REST_Webhook_Out_Event_ID, trxName);
      /** if (REST_Webhook_Out_Event_ID == 0)
        {
			setAD_Table_ID (0);
			setEventModelValidator (null);
			setREST_Webhook_Out_Event_ID (0);
			setREST_Webhook_Out_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_Webhook_Out_Event (Properties ctx, int REST_Webhook_Out_Event_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_Webhook_Out_Event_ID, trxName, virtualColumns);
      /** if (REST_Webhook_Out_Event_ID == 0)
        {
			setAD_Table_ID (0);
			setEventModelValidator (null);
			setREST_Webhook_Out_Event_ID (0);
			setREST_Webhook_Out_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_Webhook_Out_Event (Properties ctx, String REST_Webhook_Out_Event_UU, String trxName)
    {
      super (ctx, REST_Webhook_Out_Event_UU, trxName);
      /** if (REST_Webhook_Out_Event_UU == null)
        {
			setAD_Table_ID (0);
			setEventModelValidator (null);
			setREST_Webhook_Out_Event_ID (0);
			setREST_Webhook_Out_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_Webhook_Out_Event (Properties ctx, String REST_Webhook_Out_Event_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_Webhook_Out_Event_UU, trxName, virtualColumns);
      /** if (REST_Webhook_Out_Event_UU == null)
        {
			setAD_Table_ID (0);
			setEventModelValidator (null);
			setREST_Webhook_Out_Event_ID (0);
			setREST_Webhook_Out_ID (0);
        } */
    }

    /** Load Constructor */
    public X_REST_Webhook_Out_Event (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_REST_Webhook_Out_Event[")
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

	/** EventModelValidator AD_Reference_ID=53237 */
	public static final int EVENTMODELVALIDATOR_AD_Reference_ID=53237;
	/** Document After Reactivate = DAAC */
	public static final String EVENTMODELVALIDATOR_DocumentAfterReactivate = "DAAC";
	/** Document After Close = DACL */
	public static final String EVENTMODELVALIDATOR_DocumentAfterClose = "DACL";
	/** Document After Complete = DACO */
	public static final String EVENTMODELVALIDATOR_DocumentAfterComplete = "DACO";
	/** Document After Post = DAPO */
	public static final String EVENTMODELVALIDATOR_DocumentAfterPost = "DAPO";
	/** Document After Prepare = DAPR */
	public static final String EVENTMODELVALIDATOR_DocumentAfterPrepare = "DAPR";
	/** Document After Reverse Accrual = DARA */
	public static final String EVENTMODELVALIDATOR_DocumentAfterReverseAccrual = "DARA";
	/** Document After Reverse Correct = DARC */
	public static final String EVENTMODELVALIDATOR_DocumentAfterReverseCorrect = "DARC";
	/** Document After Void = DAVO */
	public static final String EVENTMODELVALIDATOR_DocumentAfterVoid = "DAVO";
	/** Document Before Reactivate = DBAC */
	public static final String EVENTMODELVALIDATOR_DocumentBeforeReactivate = "DBAC";
	/** Document Before Close = DBCL */
	public static final String EVENTMODELVALIDATOR_DocumentBeforeClose = "DBCL";
	/** Document Before Complete = DBCO */
	public static final String EVENTMODELVALIDATOR_DocumentBeforeComplete = "DBCO";
	/** Document Before Post = DBPO */
	public static final String EVENTMODELVALIDATOR_DocumentBeforePost = "DBPO";
	/** Document Before Prepare = DBPR */
	public static final String EVENTMODELVALIDATOR_DocumentBeforePrepare = "DBPR";
	/** Document Before Reverse Accrual = DBRA */
	public static final String EVENTMODELVALIDATOR_DocumentBeforeReverseAccrual = "DBRA";
	/** Document Before Reverse Correct = DBRC */
	public static final String EVENTMODELVALIDATOR_DocumentBeforeReverseCorrect = "DBRC";
	/** Document Before Void = DBVO */
	public static final String EVENTMODELVALIDATOR_DocumentBeforeVoid = "DBVO";
	/** Table After Change = TAC */
	public static final String EVENTMODELVALIDATOR_TableAfterChange = "TAC";
	/** Table After Change Replication = TACR */
	public static final String EVENTMODELVALIDATOR_TableAfterChangeReplication = "TACR";
	/** Table After Delete = TAD */
	public static final String EVENTMODELVALIDATOR_TableAfterDelete = "TAD";
	/** Table After New = TAN */
	public static final String EVENTMODELVALIDATOR_TableAfterNew = "TAN";
	/** Table After New Replication = TANR */
	public static final String EVENTMODELVALIDATOR_TableAfterNewReplication = "TANR";
	/** Table Before Change = TBC */
	public static final String EVENTMODELVALIDATOR_TableBeforeChange = "TBC";
	/** Table Before Delete = TBD */
	public static final String EVENTMODELVALIDATOR_TableBeforeDelete = "TBD";
	/** Table Before Delete Replication = TBDR */
	public static final String EVENTMODELVALIDATOR_TableBeforeDeleteReplication = "TBDR";
	/** Table Before New = TBN */
	public static final String EVENTMODELVALIDATOR_TableBeforeNew = "TBN";
	/** Set Event Model Validator.
		@param EventModelValidator Event Model Validator
	*/
	public void setEventModelValidator (String EventModelValidator)
	{

		set_Value (COLUMNNAME_EventModelValidator, EventModelValidator);
	}

	/** Get Event Model Validator.
		@return Event Model Validator	  */
	public String getEventModelValidator()
	{
		return (String)get_Value(COLUMNNAME_EventModelValidator);
	}

	/** Set Exclude Column.
		@param ExcludeColumns Comma-separated column names to exclude from the webhook payload. Applied after IncludeColumns. Example: Password,WebhookSecre
	*/
	public void setExcludeColumns (String ExcludeColumns)
	{
		set_Value (COLUMNNAME_ExcludeColumns, ExcludeColumns);
	}

	/** Get Exclude Column.
		@return Comma-separated column names to exclude from the webhook payload. Applied after IncludeColumns. Example: Password,WebhookSecre
	  */
	public String getExcludeColumns()
	{
		return (String)get_Value(COLUMNNAME_ExcludeColumns);
	}

	/** Set Include Columns.
		@param IncludeColumns Comma-separated column names to include in the webhook payload. If empty, all columns are included. Example: DocumentNo,GrandTotal,DocStatus
	*/
	public void setIncludeColumns (String IncludeColumns)
	{
		set_Value (COLUMNNAME_IncludeColumns, IncludeColumns);
	}

	/** Get Include Columns.
		@return Comma-separated column names to include in the webhook payload. If empty, all columns are included. Example: DocumentNo,GrandTotal,DocStatus
	  */
	public String getIncludeColumns()
	{
		return (String)get_Value(COLUMNNAME_IncludeColumns);
	}

	/** Set Payload Template.
		@param PayloadTemplate Payload Template
	*/
	public void setPayloadTemplate (String PayloadTemplate)
	{
		set_Value (COLUMNNAME_PayloadTemplate, PayloadTemplate);
	}

	/** Get Payload Template.
		@return Payload Template	  */
	public String getPayloadTemplate()
	{
		return (String)get_Value(COLUMNNAME_PayloadTemplate);
	}

	/** Set REST Webhook Out Event.
		@param REST_Webhook_Out_Event_ID Event subscription for a webhook endpoint
	*/
	public void setREST_Webhook_Out_Event_ID (int REST_Webhook_Out_Event_ID)
	{
		if (REST_Webhook_Out_Event_ID < 1)
			set_ValueNoCheck (COLUMNNAME_REST_Webhook_Out_Event_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_REST_Webhook_Out_Event_ID, Integer.valueOf(REST_Webhook_Out_Event_ID));
	}

	/** Get REST Webhook Out Event.
		@return Event subscription for a webhook endpoint
	  */
	public int getREST_Webhook_Out_Event_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_Webhook_Out_Event_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set REST Webhook Out Event UU.
		@param REST_Webhook_Out_Event_UU REST Webhook Out Event UU
	*/
	public void setREST_Webhook_Out_Event_UU (String REST_Webhook_Out_Event_UU)
	{
		set_Value (COLUMNNAME_REST_Webhook_Out_Event_UU, REST_Webhook_Out_Event_UU);
	}

	/** Get REST Webhook Out Event UU.
		@return REST Webhook Out Event UU	  */
	public String getREST_Webhook_Out_Event_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_Webhook_Out_Event_UU);
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

	/** Set Sequence.
		@param SeqNo Method of ordering records; lowest number comes first
	*/
	public void setSeqNo (int SeqNo)
	{
		set_Value (COLUMNNAME_SeqNo, Integer.valueOf(SeqNo));
	}

	/** Get Sequence.
		@return Method of ordering records; lowest number comes first
	  */
	public int getSeqNo()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SeqNo);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}
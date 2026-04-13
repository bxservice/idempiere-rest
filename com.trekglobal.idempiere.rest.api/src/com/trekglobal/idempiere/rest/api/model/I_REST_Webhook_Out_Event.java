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

/** Generated Interface for REST_Webhook_Out_Event
 *  @author iDempiere (generated) 
 *  @version Release 14
 */
@SuppressWarnings("all")
public interface I_REST_Webhook_Out_Event 
{

    /** TableName=REST_Webhook_Out_Event */
    public static final String Table_Name = "REST_Webhook_Out_Event";

    /** AD_Table_ID=200441 */
    public static final int Table_ID = 200441;

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

    /** Column name AD_Table_ID */
    public static final String COLUMNNAME_AD_Table_ID = "AD_Table_ID";

	/** Set Table.
	  * Database Table information
	  */
	public void setAD_Table_ID (int AD_Table_ID);

	/** Get Table.
	  * Database Table information
	  */
	public int getAD_Table_ID();

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Table getAD_Table() throws RuntimeException;

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

    /** Column name EventModelValidator */
    public static final String COLUMNNAME_EventModelValidator = "EventModelValidator";

	/** Set Event Model Validator	  */
	public void setEventModelValidator (String EventModelValidator);

	/** Get Event Model Validator	  */
	public String getEventModelValidator();

    /** Column name ExcludeColumns */
    public static final String COLUMNNAME_ExcludeColumns = "ExcludeColumns";

	/** Set Exclude Column.
	  * Comma-separated column names to exclude from the webhook payload. Applied after IncludeColumns. Example: Password,WebhookSecre
	  */
	public void setExcludeColumns (String ExcludeColumns);

	/** Get Exclude Column.
	  * Comma-separated column names to exclude from the webhook payload. Applied after IncludeColumns. Example: Password,WebhookSecre
	  */
	public String getExcludeColumns();

    /** Column name IncludeColumns */
    public static final String COLUMNNAME_IncludeColumns = "IncludeColumns";

	/** Set Include Columns.
	  * Comma-separated column names to include in the webhook payload. If empty, all columns are included. Example: DocumentNo,GrandTotal,DocStatus
	  */
	public void setIncludeColumns (String IncludeColumns);

	/** Get Include Columns.
	  * Comma-separated column names to include in the webhook payload. If empty, all columns are included. Example: DocumentNo,GrandTotal,DocStatus
	  */
	public String getIncludeColumns();

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

    /** Column name PayloadTemplate */
    public static final String COLUMNNAME_PayloadTemplate = "PayloadTemplate";

	/** Set Payload Template	  */
	public void setPayloadTemplate (String PayloadTemplate);

	/** Get Payload Template	  */
	public String getPayloadTemplate();

    /** Column name REST_Webhook_Out_Event_ID */
    public static final String COLUMNNAME_REST_Webhook_Out_Event_ID = "REST_Webhook_Out_Event_ID";

	/** Set REST Webhook Out Event.
	  * Event subscription for a webhook endpoint
	  */
	public void setREST_Webhook_Out_Event_ID (int REST_Webhook_Out_Event_ID);

	/** Get REST Webhook Out Event.
	  * Event subscription for a webhook endpoint
	  */
	public int getREST_Webhook_Out_Event_ID();

    /** Column name REST_Webhook_Out_Event_UU */
    public static final String COLUMNNAME_REST_Webhook_Out_Event_UU = "REST_Webhook_Out_Event_UU";

	/** Set REST Webhook Out Event UU	  */
	public void setREST_Webhook_Out_Event_UU (String REST_Webhook_Out_Event_UU);

	/** Get REST Webhook Out Event UU	  */
	public String getREST_Webhook_Out_Event_UU();

    /** Column name REST_Webhook_Out_ID */
    public static final String COLUMNNAME_REST_Webhook_Out_ID = "REST_Webhook_Out_ID";

	/** Set REST Webhook Out.
	  * Outbound webhook endpoint configuration
	  */
	public void setREST_Webhook_Out_ID (int REST_Webhook_Out_ID);

	/** Get REST Webhook Out.
	  * Outbound webhook endpoint configuration
	  */
	public int getREST_Webhook_Out_ID();

	@Deprecated(since="13") // use better methods with cache
	public com.trekglobal.idempiere.rest.api.model.I_REST_Webhook_Out getREST_Webhook_Out() throws RuntimeException;

    /** Column name SeqNo */
    public static final String COLUMNNAME_SeqNo = "SeqNo";

	/** Set Sequence.
	  * Method of ordering records;
 lowest number comes first
	  */
	public void setSeqNo (int SeqNo);

	/** Get Sequence.
	  * Method of ordering records;
 lowest number comes first
	  */
	public int getSeqNo();

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
}

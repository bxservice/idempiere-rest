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

/** Generated Interface for REST_Webhook_Out
 *  @author iDempiere (generated) 
 *  @version Release 14
 */
@SuppressWarnings("all")
public interface I_REST_Webhook_Out 
{

    /** TableName=REST_Webhook_Out */
    public static final String Table_Name = "REST_Webhook_Out";

    /** AD_Table_ID=200440 */
    public static final int Table_ID = 200440;

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

    /** Column name ConsecutiveFailures */
    public static final String COLUMNNAME_ConsecutiveFailures = "ConsecutiveFailures";

	/** Set Consecutive Failures.
	  * Number of consecutive delivery failures. Resets to 0 on successful delivery.
	  */
	public void setConsecutiveFailures (int ConsecutiveFailures);

	/** Get Consecutive Failures.
	  * Number of consecutive delivery failures. Resets to 0 on successful delivery.
	  */
	public int getConsecutiveFailures();

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

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

    /** Column name EntityType */
    public static final String COLUMNNAME_EntityType = "EntityType";

	/** Set Entity Type.
	  * Dictionary Entity Type;
 Determines ownership and synchronization
	  */
	public void setEntityType (String EntityType);

	/** Get Entity Type.
	  * Dictionary Entity Type;
 Determines ownership and synchronization
	  */
	public String getEntityType();

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

    /** Column name IsPaused */
    public static final String COLUMNNAME_IsPaused = "IsPaused";

    /** Column name IsStandardWebhook */
    public static final String COLUMNNAME_IsStandardWebhook = "IsStandardWebhook";

	/** Set Standard Webhook.
	  * When enabled, payloads are wrapped in a Standard Webhooks envelope and signed with HMAC-SHA256 headers. When disabled, the payload is sent as-is without envelope or signature headers.
	  */
	public void setIsStandardWebhook (boolean IsStandardWebhook);

	/** Get Standard Webhook.
	  * When enabled, payloads are wrapped in a Standard Webhooks envelope and signed with HMAC-SHA256 headers. When disabled, the payload is sent as-is without envelope or signature headers.
	  */
	public boolean isStandardWebhook();

	/** Set Paused.
	  * When paused, no deliveries are attempted. Deliveries created during pause are queued and sent when unpaused. Auto-paused after consecutive failures exceed threshold.
	  */
	public void setIsPaused (boolean IsPaused);

	/** Get Paused.
	  * When paused, no deliveries are attempted. Deliveries created during pause are queued and sent when unpaused. Auto-paused after consecutive failures exceed threshold.
	  */
	public boolean isPaused();

    /** Column name LastFailureAt */
    public static final String COLUMNNAME_LastFailureAt = "LastFailureAt";

	/** Set Last Failure At.
	  * Timestamp of last failed delivery
	  */
	public void setLastFailureAt (Timestamp LastFailureAt);

	/** Get Last Failure At.
	  * Timestamp of last failed delivery
	  */
	public Timestamp getLastFailureAt();

    /** Column name LastSuccessAt */
    public static final String COLUMNNAME_LastSuccessAt = "LastSuccessAt";

	/** Set Last Success At.
	  * Timestamp of last successful delivery
	  */
	public void setLastSuccessAt (Timestamp LastSuccessAt);

	/** Get Last Success At.
	  * Timestamp of last successful delivery
	  */
	public Timestamp getLastSuccessAt();

    /** Column name MaxConsecutiveFailures */
    public static final String COLUMNNAME_MaxConsecutiveFailures = "MaxConsecutiveFailures";

	/** Set Max Consecutive Failures.
	  * Endpoint is auto-paused when consecutive failures reach this threshold. Set 0 to disable auto-pause.
	  */
	public void setMaxConsecutiveFailures (int MaxConsecutiveFailures);

	/** Get Max Consecutive Failures.
	  * Endpoint is auto-paused when consecutive failures reach this threshold. Set 0 to disable auto-pause.
	  */
	public int getMaxConsecutiveFailures();

    /** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();

    /** Column name PreviousSecret */
    public static final String COLUMNNAME_PreviousSecret = "PreviousSecret";

	/** Set Previous Secret.
	  * Previous signing secret, used during key rotation. Both secrets sign payloads until this one expires.
	  */
	public void setPreviousSecret (String PreviousSecret);

	/** Get Previous Secret.
	  * Previous signing secret, used during key rotation. Both secrets sign payloads until this one expires.
	  */
	public String getPreviousSecret();

    /** Column name PreviousSecretExpiry */
    public static final String COLUMNNAME_PreviousSecretExpiry = "PreviousSecretExpiry";

	/** Set Previous Secret Expiry.
	  * Expiration timestamp for the previous secret. After this time, only the current secret is used for signing.
	  */
	public void setPreviousSecretExpiry (Timestamp PreviousSecretExpiry);

	/** Get Previous Secret Expiry.
	  * Expiration timestamp for the previous secret. After this time, only the current secret is used for signing.
	  */
	public Timestamp getPreviousSecretExpiry();

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

    /** Column name REST_Webhook_Out_UU */
    public static final String COLUMNNAME_REST_Webhook_Out_UU = "REST_Webhook_Out_UU";

	/** Set REST Webhook Out UU	  */
	public void setREST_Webhook_Out_UU (String REST_Webhook_Out_UU);

	/** Get REST Webhook Out UU	  */
	public String getREST_Webhook_Out_UU();

    /** Column name URL */
    public static final String COLUMNNAME_URL = "URL";

	/** Set URL.
	  * Full URL address - e.g. http://www.idempiere.org
	  */
	public void setURL (String URL);

	/** Get URL.
	  * Full URL address - e.g. http://www.idempiere.org
	  */
	public String getURL();

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

    /** Column name WebhookSecret */
    public static final String COLUMNNAME_WebhookSecret = "WebhookSecret";

	/** Set Webhook Secret.
	  * HMAC-SHA256 signing secret in Standard Webhooks format (whsec_ prefix + base64). Used to sign outbound payloads so the receiver can verify authenticity.
	  */
	public void setWebhookSecret (String WebhookSecret);

	/** Get Webhook Secret.
	  * HMAC-SHA256 signing secret in Standard Webhooks format (whsec_ prefix + base64). Used to sign outbound payloads so the receiver can verify authenticity.
	  */
	public String getWebhookSecret();
}

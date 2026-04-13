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

/** Generated Interface for REST_Webhook_In
 *  @author iDempiere (generated) 
 *  @version Release 14
 */
@SuppressWarnings("all")
public interface I_REST_Webhook_In 
{

    /** TableName=REST_Webhook_In */
    public static final String Table_Name = "REST_Webhook_In";

    /** AD_Table_ID=200443 */
    public static final int Table_ID = 200443;

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

    /** Column name AD_Process_ID */
    public static final String COLUMNNAME_AD_Process_ID = "AD_Process_ID";

	/** Set Process.
	  * Process or Report
	  */
	public void setAD_Process_ID (int AD_Process_ID);

	/** Get Process.
	  * Process or Report
	  */
	public int getAD_Process_ID();

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Process getAD_Process() throws RuntimeException;

    /** Column name AD_User_ID */
    public static final String COLUMNNAME_AD_User_ID = "AD_User_ID";

	/** Set User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public void setAD_User_ID (int AD_User_ID);

	/** Get User/Contact.
	  * User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID();

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException;

    /** Column name AllowedIPs */
    public static final String COLUMNNAME_AllowedIPs = "AllowedIPs";

	/** Set Allowed IPs.
	  * Comma-separated IP addresses or CIDR blocks allowed to send webhooks. Empty means all IPs allowed. Example: 192.168.1.0/24,10.0.0.5
	  */
	public void setAllowedIPs (String AllowedIPs);

	/** Get Allowed IPs.
	  * Comma-separated IP addresses or CIDR blocks allowed to send webhooks. Empty means all IPs allowed. Example: 192.168.1.0/24,10.0.0.5
	  */
	public String getAllowedIPs();

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

    /** Column name EndpointKey */
    public static final String COLUMNNAME_EndpointKey = "EndpointKey";

	/** Set Endpoint Key.
	  * Unique URL path segment. The full URL is /api/v1/webhooks/
{
EndpointKey}
. Use lowercase with hyphens (e.g., pix-bb, boleto-itau, whatsapp-zapme). Must be URL-safe.
	  */
	public void setEndpointKey (String EndpointKey);

	/** Get Endpoint Key.
	  * Unique URL path segment. The full URL is /api/v1/webhooks/
{
EndpointKey}
. Use lowercase with hyphens (e.g., pix-bb, boleto-itau, whatsapp-zapme). Must be URL-safe.
	  */
	public String getEndpointKey();

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

    /** Column name IsVerifySignature */
    public static final String COLUMNNAME_IsVerifySignature = "IsVerifySignature";

	/** Set Verify Signature.
	  * When enabled, incoming requests must include valid Standard Webhooks signatures. Disable only for legacy providers that do not support HMAC signing (temporary migration path).
	  */
	public void setIsVerifySignature (boolean IsVerifySignature);

	/** Get Verify Signature.
	  * When enabled, incoming requests must include valid Standard Webhooks signatures. Disable only for legacy providers that do not support HMAC signing (temporary migration path).
	  */
	public boolean isVerifySignature();

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

    /** Column name REST_Webhook_In_UU */
    public static final String COLUMNNAME_REST_Webhook_In_UU = "REST_Webhook_In_UU";

	/** Set REST Webhook In UU	  */
	public void setREST_Webhook_In_UU (String REST_Webhook_In_UU);

	/** Get REST Webhook In UU	  */
	public String getREST_Webhook_In_UU();

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

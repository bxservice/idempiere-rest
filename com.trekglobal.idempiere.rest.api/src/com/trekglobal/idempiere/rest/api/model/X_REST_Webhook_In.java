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

/** Generated Model for REST_Webhook_In
 *  @author iDempiere (generated)
 *  @version Release 14 - $Id$ */
@org.adempiere.base.Model(table="REST_Webhook_In")
public class X_REST_Webhook_In extends PO implements I_REST_Webhook_In, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260413L;

    /** Standard Constructor */
    public X_REST_Webhook_In (Properties ctx, int REST_Webhook_In_ID, String trxName)
    {
      super (ctx, REST_Webhook_In_ID, trxName);
      /** if (REST_Webhook_In_ID == 0)
        {
			setAD_Process_ID (0);
			setEndpointKey (null);
			setEntityType (null);
// @SQL=SELECT CASE WHEN '@P|AdempiereSys:N@'='Y' THEN 'D' ELSE get_sysconfig('DEFAULT_ENTITYTYPE','U',0,0) END FROM Dual
			setIsVerifySignature (true);
// Y
			setName (null);
			setREST_Webhook_In_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_Webhook_In (Properties ctx, int REST_Webhook_In_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_Webhook_In_ID, trxName, virtualColumns);
      /** if (REST_Webhook_In_ID == 0)
        {
			setAD_Process_ID (0);
			setEndpointKey (null);
			setEntityType (null);
// @SQL=SELECT CASE WHEN '@P|AdempiereSys:N@'='Y' THEN 'D' ELSE get_sysconfig('DEFAULT_ENTITYTYPE','U',0,0) END FROM Dual
			setIsVerifySignature (true);
// Y
			setName (null);
			setREST_Webhook_In_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_Webhook_In (Properties ctx, String REST_Webhook_In_UU, String trxName)
    {
      super (ctx, REST_Webhook_In_UU, trxName);
      /** if (REST_Webhook_In_UU == null)
        {
			setAD_Process_ID (0);
			setEndpointKey (null);
			setEntityType (null);
// @SQL=SELECT CASE WHEN '@P|AdempiereSys:N@'='Y' THEN 'D' ELSE get_sysconfig('DEFAULT_ENTITYTYPE','U',0,0) END FROM Dual
			setIsVerifySignature (true);
// Y
			setName (null);
			setREST_Webhook_In_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_Webhook_In (Properties ctx, String REST_Webhook_In_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_Webhook_In_UU, trxName, virtualColumns);
      /** if (REST_Webhook_In_UU == null)
        {
			setAD_Process_ID (0);
			setEndpointKey (null);
			setEntityType (null);
// @SQL=SELECT CASE WHEN '@P|AdempiereSys:N@'='Y' THEN 'D' ELSE get_sysconfig('DEFAULT_ENTITYTYPE','U',0,0) END FROM Dual
			setIsVerifySignature (true);
// Y
			setName (null);
			setREST_Webhook_In_ID (0);
        } */
    }

    /** Load Constructor */
    public X_REST_Webhook_In (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_REST_Webhook_In[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_Process getAD_Process() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Process)MTable.get(getCtx(), org.compiere.model.I_AD_Process.Table_ID)
			.getPO(getAD_Process_ID(), get_TrxName());
	}

	/** Set Process.
		@param AD_Process_ID Process or Report
	*/
	public void setAD_Process_ID (int AD_Process_ID)
	{
		if (AD_Process_ID < 1)
			set_Value (COLUMNNAME_AD_Process_ID, null);
		else
			set_Value (COLUMNNAME_AD_Process_ID, Integer.valueOf(AD_Process_ID));
	}

	/** Get Process.
		@return Process or Report
	  */
	public int getAD_Process_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Process_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Deprecated(since="13") // use better methods with cache
	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getAD_User_ID(), get_TrxName());
	}

	/** Set User/Contact.
		@param AD_User_ID User within the system - Internal or Business Partner Contact
	*/
	public void setAD_User_ID (int AD_User_ID)
	{
		if (AD_User_ID < 1)
			set_Value (COLUMNNAME_AD_User_ID, null);
		else
			set_Value (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
	}

	/** Get User/Contact.
		@return User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Allowed IPs.
		@param AllowedIPs Comma-separated IP addresses or CIDR blocks allowed to send webhooks. Empty means all IPs allowed. Example: 192.168.1.0/24,10.0.0.5
	*/
	public void setAllowedIPs (String AllowedIPs)
	{
		set_Value (COLUMNNAME_AllowedIPs, AllowedIPs);
	}

	/** Get Allowed IPs.
		@return Comma-separated IP addresses or CIDR blocks allowed to send webhooks. Empty means all IPs allowed. Example: 192.168.1.0/24,10.0.0.5
	  */
	public String getAllowedIPs()
	{
		return (String)get_Value(COLUMNNAME_AllowedIPs);
	}

	/** Set Description.
		@param Description Optional short description of the record
	*/
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription()
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set Endpoint Key.
		@param EndpointKey Unique URL path segment. The full URL is /api/v1/webhooks/{EndpointKey}. Use lowercase with hyphens (e.g., pix-bb, boleto-itau, whatsapp-zapme). Must be URL-safe.
	*/
	public void setEndpointKey (String EndpointKey)
	{
		set_Value (COLUMNNAME_EndpointKey, EndpointKey);
	}

	/** Get Endpoint Key.
		@return Unique URL path segment. The full URL is /api/v1/webhooks/{EndpointKey}. Use lowercase with hyphens (e.g., pix-bb, boleto-itau, whatsapp-zapme). Must be URL-safe.
	  */
	public String getEndpointKey()
	{
		return (String)get_Value(COLUMNNAME_EndpointKey);
	}

	/** EntityType AD_Reference_ID=389 */
	public static final int ENTITYTYPE_AD_Reference_ID=389;
	/** Set Entity Type.
		@param EntityType Dictionary Entity Type; Determines ownership and synchronization
	*/
	public void setEntityType (String EntityType)
	{

		set_Value (COLUMNNAME_EntityType, EntityType);
	}

	/** Get Entity Type.
		@return Dictionary Entity Type; Determines ownership and synchronization
	  */
	public String getEntityType()
	{
		return (String)get_Value(COLUMNNAME_EntityType);
	}

	/** Set Verify Signature.
		@param IsVerifySignature When enabled, incoming requests must include valid Standard Webhooks signatures. Disable only for legacy providers that do not support HMAC signing (temporary migration path).
	*/
	public void setIsVerifySignature (boolean IsVerifySignature)
	{
		set_Value (COLUMNNAME_IsVerifySignature, Boolean.valueOf(IsVerifySignature));
	}

	/** Get Verify Signature.
		@return When enabled, incoming requests must include valid Standard Webhooks signatures. Disable only for legacy providers that do not support HMAC signing (temporary migration path).
	  */
	public boolean isVerifySignature()
	{
		Object oo = get_Value(COLUMNNAME_IsVerifySignature);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName()
	{
		return (String)get_Value(COLUMNNAME_Name);
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

	/** Set REST Webhook In UU.
		@param REST_Webhook_In_UU REST Webhook In UU
	*/
	public void setREST_Webhook_In_UU (String REST_Webhook_In_UU)
	{
		set_Value (COLUMNNAME_REST_Webhook_In_UU, REST_Webhook_In_UU);
	}

	/** Get REST Webhook In UU.
		@return REST Webhook In UU	  */
	public String getREST_Webhook_In_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_Webhook_In_UU);
	}

	/** Set Webhook Secret.
		@param WebhookSecret HMAC-SHA256 signing secret in Standard Webhooks format (whsec_ prefix + base64). Used to sign outbound payloads so the receiver can verify authenticity.
	*/
	public void setWebhookSecret (String WebhookSecret)
	{
		set_Value (COLUMNNAME_WebhookSecret, WebhookSecret);
	}

	/** Get Webhook Secret.
		@return HMAC-SHA256 signing secret in Standard Webhooks format (whsec_ prefix + base64). Used to sign outbound payloads so the receiver can verify authenticity.
	  */
	public String getWebhookSecret()
	{
		return (String)get_Value(COLUMNNAME_WebhookSecret);
	}
}
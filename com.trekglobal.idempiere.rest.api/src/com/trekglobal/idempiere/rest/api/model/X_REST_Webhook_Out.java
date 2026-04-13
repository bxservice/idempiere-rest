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

/** Generated Model for REST_Webhook_Out
 *  @author iDempiere (generated)
 *  @version Release 14 - $Id$ */
@org.adempiere.base.Model(table="REST_Webhook_Out")
public class X_REST_Webhook_Out extends PO implements I_REST_Webhook_Out, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260413L;

    /** Standard Constructor */
    public X_REST_Webhook_Out (Properties ctx, int REST_Webhook_Out_ID, String trxName)
    {
      super (ctx, REST_Webhook_Out_ID, trxName);
      /** if (REST_Webhook_Out_ID == 0)
        {
			setIsPaused (false);
// N
			setName (null);
			setREST_Webhook_Out_ID (0);
			setURL (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_Webhook_Out (Properties ctx, int REST_Webhook_Out_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_Webhook_Out_ID, trxName, virtualColumns);
      /** if (REST_Webhook_Out_ID == 0)
        {
			setIsPaused (false);
// N
			setName (null);
			setREST_Webhook_Out_ID (0);
			setURL (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_Webhook_Out (Properties ctx, String REST_Webhook_Out_UU, String trxName)
    {
      super (ctx, REST_Webhook_Out_UU, trxName);
      /** if (REST_Webhook_Out_UU == null)
        {
			setIsPaused (false);
// N
			setName (null);
			setREST_Webhook_Out_ID (0);
			setURL (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_Webhook_Out (Properties ctx, String REST_Webhook_Out_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_Webhook_Out_UU, trxName, virtualColumns);
      /** if (REST_Webhook_Out_UU == null)
        {
			setIsPaused (false);
// N
			setName (null);
			setREST_Webhook_Out_ID (0);
			setURL (null);
        } */
    }

    /** Load Constructor */
    public X_REST_Webhook_Out (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_REST_Webhook_Out[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** Set Consecutive Failures.
		@param ConsecutiveFailures Number of consecutive delivery failures. Resets to 0 on successful delivery.
	*/
	public void setConsecutiveFailures (int ConsecutiveFailures)
	{
		set_Value (COLUMNNAME_ConsecutiveFailures, Integer.valueOf(ConsecutiveFailures));
	}

	/** Get Consecutive Failures.
		@return Number of consecutive delivery failures. Resets to 0 on successful delivery.
	  */
	public int getConsecutiveFailures()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ConsecutiveFailures);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set Standard Webhook.
		@param IsStandardWebhook When enabled, payloads are wrapped in a Standard Webhooks envelope and signed with HMAC-SHA256 headers. When disabled, the payload is sent as-is without envelope or signature headers.
	*/
	public void setIsStandardWebhook (boolean IsStandardWebhook)
	{
		set_Value (COLUMNNAME_IsStandardWebhook, Boolean.valueOf(IsStandardWebhook));
	}

	/** Get Standard Webhook.
		@return When enabled, payloads are wrapped in a Standard Webhooks envelope and signed with HMAC-SHA256 headers. When disabled, the payload is sent as-is without envelope or signature headers.
	  */
	public boolean isStandardWebhook()
	{
		Object oo = get_Value(COLUMNNAME_IsStandardWebhook);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return true; // default: Standard Webhooks enabled
	}

	/** Set Paused.
		@param IsPaused When paused, no deliveries are attempted. Deliveries created during pause are queued and sent when unpaused. Auto-paused after consecutive failures exceed threshold.
	*/
	public void setIsPaused (boolean IsPaused)
	{
		set_Value (COLUMNNAME_IsPaused, Boolean.valueOf(IsPaused));
	}

	/** Get Paused.
		@return When paused, no deliveries are attempted. Deliveries created during pause are queued and sent when unpaused. Auto-paused after consecutive failures exceed threshold.
	  */
	public boolean isPaused()
	{
		Object oo = get_Value(COLUMNNAME_IsPaused);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Last Failure At.
		@param LastFailureAt Timestamp of last failed delivery
	*/
	public void setLastFailureAt (Timestamp LastFailureAt)
	{
		set_Value (COLUMNNAME_LastFailureAt, LastFailureAt);
	}

	/** Get Last Failure At.
		@return Timestamp of last failed delivery
	  */
	public Timestamp getLastFailureAt()
	{
		return (Timestamp)get_Value(COLUMNNAME_LastFailureAt);
	}

	/** Set Last Success At.
		@param LastSuccessAt Timestamp of last successful delivery
	*/
	public void setLastSuccessAt (Timestamp LastSuccessAt)
	{
		set_Value (COLUMNNAME_LastSuccessAt, LastSuccessAt);
	}

	/** Get Last Success At.
		@return Timestamp of last successful delivery
	  */
	public Timestamp getLastSuccessAt()
	{
		return (Timestamp)get_Value(COLUMNNAME_LastSuccessAt);
	}

	/** Set Max Consecutive Failures.
		@param MaxConsecutiveFailures Endpoint is auto-paused when consecutive failures reach this threshold. Set 0 to disable auto-pause.
	*/
	public void setMaxConsecutiveFailures (int MaxConsecutiveFailures)
	{
		set_Value (COLUMNNAME_MaxConsecutiveFailures, Integer.valueOf(MaxConsecutiveFailures));
	}

	/** Get Max Consecutive Failures.
		@return Endpoint is auto-paused when consecutive failures reach this threshold. Set 0 to disable auto-pause.
	  */
	public int getMaxConsecutiveFailures()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_MaxConsecutiveFailures);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set Previous Secret.
		@param PreviousSecret Previous signing secret, used during key rotation. Both secrets sign payloads until this one expires.
	*/
	public void setPreviousSecret (String PreviousSecret)
	{
		set_Value (COLUMNNAME_PreviousSecret, PreviousSecret);
	}

	/** Get Previous Secret.
		@return Previous signing secret, used during key rotation. Both secrets sign payloads until this one expires.
	  */
	public String getPreviousSecret()
	{
		return (String)get_Value(COLUMNNAME_PreviousSecret);
	}

	/** Set Previous Secret Expiry.
		@param PreviousSecretExpiry Expiration timestamp for the previous secret. After this time, only the current secret is used for signing.
	*/
	public void setPreviousSecretExpiry (Timestamp PreviousSecretExpiry)
	{
		set_Value (COLUMNNAME_PreviousSecretExpiry, PreviousSecretExpiry);
	}

	/** Get Previous Secret Expiry.
		@return Expiration timestamp for the previous secret. After this time, only the current secret is used for signing.
	  */
	public Timestamp getPreviousSecretExpiry()
	{
		return (Timestamp)get_Value(COLUMNNAME_PreviousSecretExpiry);
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

	/** Set REST Webhook Out UU.
		@param REST_Webhook_Out_UU REST Webhook Out UU
	*/
	public void setREST_Webhook_Out_UU (String REST_Webhook_Out_UU)
	{
		set_Value (COLUMNNAME_REST_Webhook_Out_UU, REST_Webhook_Out_UU);
	}

	/** Get REST Webhook Out UU.
		@return REST Webhook Out UU	  */
	public String getREST_Webhook_Out_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_Webhook_Out_UU);
	}

	/** Set URL.
		@param URL Full URL address - e.g. http://www.idempiere.org
	*/
	public void setURL (String URL)
	{
		set_Value (COLUMNNAME_URL, URL);
	}

	/** Get URL.
		@return Full URL address - e.g. http://www.idempiere.org
	  */
	public String getURL()
	{
		return (String)get_Value(COLUMNNAME_URL);
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
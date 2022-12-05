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

/** Generated Model for REST_AuthToken
 *  @author iDempiere (generated) 
 *  @version Release 10 - $Id$ */
@org.adempiere.base.Model(table="REST_AuthToken")
public class X_REST_AuthToken extends PO implements I_REST_AuthToken, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20221029L;

    /** Standard Constructor */
    public X_REST_AuthToken (Properties ctx, int REST_AuthToken_ID, String trxName)
    {
      super (ctx, REST_AuthToken_ID, trxName);
      /** if (REST_AuthToken_ID == 0)
        {
			setAD_Language (null);
			setAD_Role_ID (0);
			setAD_User_ID (0);
			setExpireInMinutes (0);
			setName (null);
			setProcessed (false);
// N
			setREST_AuthToken_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_AuthToken (Properties ctx, int REST_AuthToken_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_AuthToken_ID, trxName, virtualColumns);
      /** if (REST_AuthToken_ID == 0)
        {
			setAD_Language (null);
			setAD_Role_ID (0);
			setAD_User_ID (0);
			setExpireInMinutes (0);
			setName (null);
			setProcessed (false);
// N
			setREST_AuthToken_ID (0);
        } */
    }

    /** Load Constructor */
    public X_REST_AuthToken (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 6 - System - Client 
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
      StringBuilder sb = new StringBuilder ("X_REST_AuthToken[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** AD_Language AD_Reference_ID=106 */
	public static final int AD_LANGUAGE_AD_Reference_ID=106;
	/** Set Language.
		@param AD_Language Language for this entity
	*/
	public void setAD_Language (String AD_Language)
	{

		set_Value (COLUMNNAME_AD_Language, AD_Language);
	}

	/** Get Language.
		@return Language for this entity
	  */
	public String getAD_Language()
	{
		return (String)get_Value(COLUMNNAME_AD_Language);
	}

	public org.compiere.model.I_AD_Role getAD_Role() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Role)MTable.get(getCtx(), org.compiere.model.I_AD_Role.Table_ID)
			.getPO(getAD_Role_ID(), get_TrxName());
	}

	/** Set Role.
		@param AD_Role_ID Responsibility Role
	*/
	public void setAD_Role_ID (int AD_Role_ID)
	{
		if (AD_Role_ID < 0)
			set_Value (COLUMNNAME_AD_Role_ID, null);
		else
			set_Value (COLUMNNAME_AD_Role_ID, Integer.valueOf(AD_Role_ID));
	}

	/** Get Role.
		@return Responsibility Role
	  */
	public int getAD_Role_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Role_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_Session getAD_Session() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Session)MTable.get(getCtx(), org.compiere.model.I_AD_Session.Table_ID)
			.getPO(getAD_Session_ID(), get_TrxName());
	}

	/** Set Session.
		@param AD_Session_ID User Session Online or Web
	*/
	public void setAD_Session_ID (int AD_Session_ID)
	{
		if (AD_Session_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_Session_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_Session_ID, Integer.valueOf(AD_Session_ID));
	}

	/** Get Session.
		@return User Session Online or Web
	  */
	public int getAD_Session_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Session_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

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

	/** Set Expire in Minutes.
		@param ExpireInMinutes Expire in Minutes
	*/
	public void setExpireInMinutes (int ExpireInMinutes)
	{
		set_Value (COLUMNNAME_ExpireInMinutes, Integer.valueOf(ExpireInMinutes));
	}

	/** Get Expire in Minutes.
		@return Expire in Minutes	  */
	public int getExpireInMinutes()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_ExpireInMinutes);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Expires At.
		@param ExpiresAt Expires At
	*/
	public void setExpiresAt (Timestamp ExpiresAt)
	{
		set_Value (COLUMNNAME_ExpiresAt, ExpiresAt);
	}

	/** Get Expires At.
		@return Expires At	  */
	public Timestamp getExpiresAt()
	{
		return (Timestamp)get_Value(COLUMNNAME_ExpiresAt);
	}

	/** Set Expired.
		@param IsExpired Expired
	*/
	public void setIsExpired (boolean IsExpired)
	{
		set_Value (COLUMNNAME_IsExpired, Boolean.valueOf(IsExpired));
	}

	/** Get Expired.
		@return Expired	  */
	public boolean isExpired()
	{
		Object oo = get_Value(COLUMNNAME_IsExpired);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	public org.compiere.model.I_M_Warehouse getM_Warehouse() throws RuntimeException
	{
		return (org.compiere.model.I_M_Warehouse)MTable.get(getCtx(), org.compiere.model.I_M_Warehouse.Table_ID)
			.getPO(getM_Warehouse_ID(), get_TrxName());
	}

	/** Set Warehouse.
		@param M_Warehouse_ID Storage Warehouse and Service Point
	*/
	public void setM_Warehouse_ID (int M_Warehouse_ID)
	{
		if (M_Warehouse_ID < 1)
			set_Value (COLUMNNAME_M_Warehouse_ID, null);
		else
			set_Value (COLUMNNAME_M_Warehouse_ID, Integer.valueOf(M_Warehouse_ID));
	}

	/** Get Warehouse.
		@return Storage Warehouse and Service Point
	  */
	public int getM_Warehouse_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Warehouse_ID);
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

	/** Set Processed.
		@param Processed The document has been processed
	*/
	public void setProcessed (boolean Processed)
	{
		set_ValueNoCheck (COLUMNNAME_Processed, Boolean.valueOf(Processed));
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

	/** Set Process Now.
		@param Processing Process Now
	*/
	public void setProcessing (boolean Processing)
	{
		set_Value (COLUMNNAME_Processing, Boolean.valueOf(Processing));
	}

	/** Get Process Now.
		@return Process Now	  */
	public boolean isProcessing()
	{
		Object oo = get_Value(COLUMNNAME_Processing);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Rest Auth Token.
		@param REST_AuthToken_ID Rest Auth Token
	*/
	public void setREST_AuthToken_ID (int REST_AuthToken_ID)
	{
		if (REST_AuthToken_ID < 1)
			set_ValueNoCheck (COLUMNNAME_REST_AuthToken_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_REST_AuthToken_ID, Integer.valueOf(REST_AuthToken_ID));
	}

	/** Get Rest Auth Token.
		@return Rest Auth Token	  */
	public int getREST_AuthToken_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_AuthToken_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set REST_AuthToken_UU.
		@param REST_AuthToken_UU REST_AuthToken_UU
	*/
	public void setREST_AuthToken_UU (String REST_AuthToken_UU)
	{
		set_Value (COLUMNNAME_REST_AuthToken_UU, REST_AuthToken_UU);
	}

	/** Get REST_AuthToken_UU.
		@return REST_AuthToken_UU	  */
	public String getREST_AuthToken_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_AuthToken_UU);
	}

	/** Set Token.
		@param Token Token
	*/
	public void setToken (String Token)
	{
		set_ValueNoCheck (COLUMNNAME_Token, Token);
	}

	/** Get Token.
		@return Token	  */
	public String getToken()
	{
		return (String)get_Value(COLUMNNAME_Token);
	}
}

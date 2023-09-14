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

/** Generated Model for REST_OIDCProvider
 *  @author iDempiere (generated)
 *  @version Release 11 - $Id$ */
@org.adempiere.base.Model(table="REST_OIDCProvider")
public class X_REST_OIDCProvider extends PO implements I_REST_OIDCProvider, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20230914L;

    /** Standard Constructor */
    public X_REST_OIDCProvider (Properties ctx, int REST_OIDCProvider_ID, String trxName)
    {
      super (ctx, REST_OIDCProvider_ID, trxName);
      /** if (REST_OIDCProvider_ID == 0)
        {
			setName (null);
			setOIDC_ConfigurationURL (null);
			setREST_OIDCProvider_ID (0);
			setREST_OIDCProvider_UU (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_OIDCProvider (Properties ctx, int REST_OIDCProvider_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_OIDCProvider_ID, trxName, virtualColumns);
      /** if (REST_OIDCProvider_ID == 0)
        {
			setName (null);
			setOIDC_ConfigurationURL (null);
			setREST_OIDCProvider_ID (0);
			setREST_OIDCProvider_UU (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_OIDCProvider (Properties ctx, String REST_OIDCProvider_UU, String trxName)
    {
      super (ctx, REST_OIDCProvider_UU, trxName);
      /** if (REST_OIDCProvider_UU == null)
        {
			setName (null);
			setOIDC_ConfigurationURL (null);
			setREST_OIDCProvider_ID (0);
			setREST_OIDCProvider_UU (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_OIDCProvider (Properties ctx, String REST_OIDCProvider_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_OIDCProvider_UU, trxName, virtualColumns);
      /** if (REST_OIDCProvider_UU == null)
        {
			setName (null);
			setOIDC_ConfigurationURL (null);
			setREST_OIDCProvider_ID (0);
			setREST_OIDCProvider_UU (null);
        } */
    }

    /** Load Constructor */
    public X_REST_OIDCProvider (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 4 - System
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
      StringBuilder sb = new StringBuilder ("X_REST_OIDCProvider[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** Set Comment/Help.
		@param Help Comment or Hint
	*/
	public void setHelp (String Help)
	{
		set_Value (COLUMNNAME_Help, Help);
	}

	/** Get Comment/Help.
		@return Comment or Hint
	  */
	public String getHelp()
	{
		return (String)get_Value(COLUMNNAME_Help);
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

	/** Set Configuration URL.
		@param OIDC_ConfigurationURL OpenID Connect Configuration Endpoint
	*/
	public void setOIDC_ConfigurationURL (String OIDC_ConfigurationURL)
	{
		set_Value (COLUMNNAME_OIDC_ConfigurationURL, OIDC_ConfigurationURL);
	}

	/** Get Configuration URL.
		@return OpenID Connect Configuration Endpoint
	  */
	public String getOIDC_ConfigurationURL()
	{
		return (String)get_Value(COLUMNNAME_OIDC_ConfigurationURL);
	}

	/** Set Rest OpenID Connect Provider.
		@param REST_OIDCProvider_ID Rest OpenID Connect Provider
	*/
	public void setREST_OIDCProvider_ID (int REST_OIDCProvider_ID)
	{
		if (REST_OIDCProvider_ID < 1)
			set_ValueNoCheck (COLUMNNAME_REST_OIDCProvider_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_REST_OIDCProvider_ID, Integer.valueOf(REST_OIDCProvider_ID));
	}

	/** Get Rest OpenID Connect Provider.
		@return Rest OpenID Connect Provider	  */
	public int getREST_OIDCProvider_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_OIDCProvider_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set REST_OIDCProvider_UU.
		@param REST_OIDCProvider_UU REST_OIDCProvider_UU
	*/
	public void setREST_OIDCProvider_UU (String REST_OIDCProvider_UU)
	{
		set_ValueNoCheck (COLUMNNAME_REST_OIDCProvider_UU, REST_OIDCProvider_UU);
	}

	/** Get REST_OIDCProvider_UU.
		@return REST_OIDCProvider_UU	  */
	public String getREST_OIDCProvider_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_OIDCProvider_UU);
	}
}
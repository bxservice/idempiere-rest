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

/** Generated Model for REST_View_Access
 *  @author iDempiere (generated)
 *  @version Release 11 - $Id$ */
@org.adempiere.base.Model(table="REST_View_Access")
public class X_REST_View_Access extends PO implements I_REST_View_Access, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20241209L;

    /** Standard Constructor */
    public X_REST_View_Access (Properties ctx, int REST_View_Access_ID, String trxName)
    {
      super (ctx, REST_View_Access_ID, trxName);
      /** if (REST_View_Access_ID == 0)
        {
			setAD_Role_ID (0);
			setIsReadOnly (false);
// N
			setREST_View_Access_UU (null);
			setREST_View_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_View_Access (Properties ctx, int REST_View_Access_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_View_Access_ID, trxName, virtualColumns);
      /** if (REST_View_Access_ID == 0)
        {
			setAD_Role_ID (0);
			setIsReadOnly (false);
// N
			setREST_View_Access_UU (null);
			setREST_View_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_View_Access (Properties ctx, String REST_View_Access_UU, String trxName)
    {
      super (ctx, REST_View_Access_UU, trxName);
      /** if (REST_View_Access_UU == null)
        {
			setAD_Role_ID (0);
			setIsReadOnly (false);
// N
			setREST_View_Access_UU (null);
			setREST_View_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_View_Access (Properties ctx, String REST_View_Access_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_View_Access_UU, trxName, virtualColumns);
      /** if (REST_View_Access_UU == null)
        {
			setAD_Role_ID (0);
			setIsReadOnly (false);
// N
			setREST_View_Access_UU (null);
			setREST_View_ID (0);
        } */
    }

    /** Load Constructor */
    public X_REST_View_Access (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_REST_View_Access[")
        .append(get_UUID()).append("]");
      return sb.toString();
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
			set_ValueNoCheck (COLUMNNAME_AD_Role_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_Role_ID, Integer.valueOf(AD_Role_ID));
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

	/** Set Read Only.
		@param IsReadOnly Field is read only
	*/
	public void setIsReadOnly (boolean IsReadOnly)
	{
		set_Value (COLUMNNAME_IsReadOnly, Boolean.valueOf(IsReadOnly));
	}

	/** Get Read Only.
		@return Field is read only
	  */
	public boolean isReadOnly()
	{
		Object oo = get_Value(COLUMNNAME_IsReadOnly);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Rest view access uid.
		@param REST_View_Access_UU Rest view access uid
	*/
	public void setREST_View_Access_UU (String REST_View_Access_UU)
	{
		set_ValueNoCheck (COLUMNNAME_REST_View_Access_UU, REST_View_Access_UU);
	}

	/** Get Rest view access uid.
		@return Rest view access uid	  */
	public String getREST_View_Access_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_View_Access_UU);
	}

	public I_REST_View getREST_View() throws RuntimeException
	{
		return (I_REST_View)MTable.get(getCtx(), I_REST_View.Table_ID)
			.getPO(getREST_View_ID(), get_TrxName());
	}

	/** Set Rest View ID.
		@param REST_View_ID Rest View ID
	*/
	public void setREST_View_ID (int REST_View_ID)
	{
		if (REST_View_ID < 1)
			set_ValueNoCheck (COLUMNNAME_REST_View_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_REST_View_ID, Integer.valueOf(REST_View_ID));
	}

	/** Get Rest View ID.
		@return Rest View ID	  */
	public int getREST_View_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_View_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}
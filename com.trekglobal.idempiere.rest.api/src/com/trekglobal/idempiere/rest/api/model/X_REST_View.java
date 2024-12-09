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

/** Generated Model for REST_View
 *  @author iDempiere (generated)
 *  @version Release 11 - $Id$ */
@org.adempiere.base.Model(table="REST_View")
public class X_REST_View extends PO implements I_REST_View, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20241209L;

    /** Standard Constructor */
    public X_REST_View (Properties ctx, int REST_View_ID, String trxName)
    {
      super (ctx, REST_View_ID, trxName);
      /** if (REST_View_ID == 0)
        {
			setAD_Table_ID (0);
			setName (null);
			setREST_View_ID (0);
			setREST_View_UU (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_View (Properties ctx, int REST_View_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_View_ID, trxName, virtualColumns);
      /** if (REST_View_ID == 0)
        {
			setAD_Table_ID (0);
			setName (null);
			setREST_View_ID (0);
			setREST_View_UU (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_View (Properties ctx, String REST_View_UU, String trxName)
    {
      super (ctx, REST_View_UU, trxName);
      /** if (REST_View_UU == null)
        {
			setAD_Table_ID (0);
			setName (null);
			setREST_View_ID (0);
			setREST_View_UU (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_View (Properties ctx, String REST_View_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_View_UU, trxName, virtualColumns);
      /** if (REST_View_UU == null)
        {
			setAD_Table_ID (0);
			setName (null);
			setREST_View_ID (0);
			setREST_View_UU (null);
        } */
    }

    /** Load Constructor */
    public X_REST_View (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_REST_View[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

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

	/** Set Rest View UID.
		@param REST_View_UU Rest View UID
	*/
	public void setREST_View_UU (String REST_View_UU)
	{
		set_ValueNoCheck (COLUMNNAME_REST_View_UU, REST_View_UU);
	}

	/** Get Rest View UID.
		@return Rest View UID	  */
	public String getREST_View_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_View_UU);
	}

	/** Set Sql WHERE.
		@param WhereClause Fully qualified SQL WHERE clause
	*/
	public void setWhereClause (String WhereClause)
	{
		set_Value (COLUMNNAME_WhereClause, WhereClause);
	}

	/** Get Sql WHERE.
		@return Fully qualified SQL WHERE clause
	  */
	public String getWhereClause()
	{
		return (String)get_Value(COLUMNNAME_WhereClause);
	}
}
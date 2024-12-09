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

/** Generated Model for REST_ViewRelated
 *  @author iDempiere (generated)
 *  @version Release 11 - $Id$ */
@org.adempiere.base.Model(table="REST_ViewRelated")
public class X_REST_ViewRelated extends PO implements I_REST_ViewRelated, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20241209L;

    /** Standard Constructor */
    public X_REST_ViewRelated (Properties ctx, int REST_ViewRelated_ID, String trxName)
    {
      super (ctx, REST_ViewRelated_ID, trxName);
      /** if (REST_ViewRelated_ID == 0)
        {
			setIsRestAutoExpand (false);
// N
			setName (null);
			setREST_ViewRelated_ID (0);
			setREST_ViewRelated_UU (null);
			setREST_View_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_ViewRelated (Properties ctx, int REST_ViewRelated_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_ViewRelated_ID, trxName, virtualColumns);
      /** if (REST_ViewRelated_ID == 0)
        {
			setIsRestAutoExpand (false);
// N
			setName (null);
			setREST_ViewRelated_ID (0);
			setREST_ViewRelated_UU (null);
			setREST_View_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_ViewRelated (Properties ctx, String REST_ViewRelated_UU, String trxName)
    {
      super (ctx, REST_ViewRelated_UU, trxName);
      /** if (REST_ViewRelated_UU == null)
        {
			setIsRestAutoExpand (false);
// N
			setName (null);
			setREST_ViewRelated_ID (0);
			setREST_ViewRelated_UU (null);
			setREST_View_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_ViewRelated (Properties ctx, String REST_ViewRelated_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_ViewRelated_UU, trxName, virtualColumns);
      /** if (REST_ViewRelated_UU == null)
        {
			setIsRestAutoExpand (false);
// N
			setName (null);
			setREST_ViewRelated_ID (0);
			setREST_ViewRelated_UU (null);
			setREST_View_ID (0);
        } */
    }

    /** Load Constructor */
    public X_REST_ViewRelated (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_REST_ViewRelated[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** Set Auto Expand.
		@param IsRestAutoExpand Auto Expand
	*/
	public void setIsRestAutoExpand (boolean IsRestAutoExpand)
	{
		set_Value (COLUMNNAME_IsRestAutoExpand, Boolean.valueOf(IsRestAutoExpand));
	}

	/** Get Auto Expand.
		@return Auto Expand	  */
	public boolean isRestAutoExpand()
	{
		Object oo = get_Value(COLUMNNAME_IsRestAutoExpand);
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

	public I_REST_View getREST_RelatedRestView() throws RuntimeException
	{
		return (I_REST_View)MTable.get(getCtx(), I_REST_View.Table_ID)
			.getPO(getREST_RelatedRestView_ID(), get_TrxName());
	}

	/** Set Related rest view.
		@param REST_RelatedRestView_ID Related rest view
	*/
	public void setREST_RelatedRestView_ID (int REST_RelatedRestView_ID)
	{
		if (REST_RelatedRestView_ID < 1)
			set_Value (COLUMNNAME_REST_RelatedRestView_ID, null);
		else
			set_Value (COLUMNNAME_REST_RelatedRestView_ID, Integer.valueOf(REST_RelatedRestView_ID));
	}

	/** Get Related rest view.
		@return Related rest view	  */
	public int getREST_RelatedRestView_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_RelatedRestView_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Rest Related View.
		@param REST_ViewRelated_ID Rest Related View
	*/
	public void setREST_ViewRelated_ID (int REST_ViewRelated_ID)
	{
		if (REST_ViewRelated_ID < 1)
			set_ValueNoCheck (COLUMNNAME_REST_ViewRelated_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_REST_ViewRelated_ID, Integer.valueOf(REST_ViewRelated_ID));
	}

	/** Get Rest Related View.
		@return Rest Related View	  */
	public int getREST_ViewRelated_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_ViewRelated_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Rest related view uid.
		@param REST_ViewRelated_UU Rest related view uid
	*/
	public void setREST_ViewRelated_UU (String REST_ViewRelated_UU)
	{
		set_ValueNoCheck (COLUMNNAME_REST_ViewRelated_UU, REST_ViewRelated_UU);
	}

	/** Get Rest related view uid.
		@return Rest related view uid	  */
	public String getREST_ViewRelated_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_ViewRelated_UU);
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
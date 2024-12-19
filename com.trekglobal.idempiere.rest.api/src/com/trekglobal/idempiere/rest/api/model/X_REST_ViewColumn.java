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

/** Generated Model for REST_ViewColumn
 *  @author iDempiere (generated)
 *  @version Release 11 - $Id$ */
@org.adempiere.base.Model(table="REST_ViewColumn")
public class X_REST_ViewColumn extends PO implements I_REST_ViewColumn, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20241209L;

    /** Standard Constructor */
    public X_REST_ViewColumn (Properties ctx, int REST_ViewColumn_ID, String trxName)
    {
      super (ctx, REST_ViewColumn_ID, trxName);
      /** if (REST_ViewColumn_ID == 0)
        {
			setAD_Column_ID (0);
			setName (null);
			setREST_ViewColumn_ID (0);
			setREST_ViewColumn_UU (null);
			setREST_View_ID (0);
			setSeqNo (0);
// @SQL=SELECT NVL(MAX(SeqNo),0)+10 AS DefaultValue FROM REST_ViewColumn WHERE REST_View_ID=@REST_View_ID@
        } */
    }

    /** Standard Constructor */
    public X_REST_ViewColumn (Properties ctx, int REST_ViewColumn_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_ViewColumn_ID, trxName, virtualColumns);
      /** if (REST_ViewColumn_ID == 0)
        {
			setAD_Column_ID (0);
			setName (null);
			setREST_ViewColumn_ID (0);
			setREST_ViewColumn_UU (null);
			setREST_View_ID (0);
			setSeqNo (0);
// @SQL=SELECT NVL(MAX(SeqNo),0)+10 AS DefaultValue FROM REST_ViewColumn WHERE REST_View_ID=@REST_View_ID@
        } */
    }

    /** Standard Constructor */
    public X_REST_ViewColumn (Properties ctx, String REST_ViewColumn_UU, String trxName)
    {
      super (ctx, REST_ViewColumn_UU, trxName);
      /** if (REST_ViewColumn_UU == null)
        {
			setAD_Column_ID (0);
			setName (null);
			setREST_ViewColumn_ID (0);
			setREST_ViewColumn_UU (null);
			setREST_View_ID (0);
			setSeqNo (0);
// @SQL=SELECT NVL(MAX(SeqNo),0)+10 AS DefaultValue FROM REST_ViewColumn WHERE REST_View_ID=@REST_View_ID@
        } */
    }

    /** Standard Constructor */
    public X_REST_ViewColumn (Properties ctx, String REST_ViewColumn_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_ViewColumn_UU, trxName, virtualColumns);
      /** if (REST_ViewColumn_UU == null)
        {
			setAD_Column_ID (0);
			setName (null);
			setREST_ViewColumn_ID (0);
			setREST_ViewColumn_UU (null);
			setREST_View_ID (0);
			setSeqNo (0);
// @SQL=SELECT NVL(MAX(SeqNo),0)+10 AS DefaultValue FROM REST_ViewColumn WHERE REST_View_ID=@REST_View_ID@
        } */
    }

    /** Load Constructor */
    public X_REST_ViewColumn (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_REST_ViewColumn[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_Column getAD_Column() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Column)MTable.get(getCtx(), org.compiere.model.I_AD_Column.Table_ID)
			.getPO(getAD_Column_ID(), get_TrxName());
	}

	/** Set Column.
		@param AD_Column_ID Column in the table
	*/
	public void setAD_Column_ID (int AD_Column_ID)
	{
		if (AD_Column_ID < 1)
			set_Value (COLUMNNAME_AD_Column_ID, null);
		else
			set_Value (COLUMNNAME_AD_Column_ID, Integer.valueOf(AD_Column_ID));
	}

	/** Get Column.
		@return Column in the table
	  */
	public int getAD_Column_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Column_ID);
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

	public I_REST_View getREST_ReferenceView() throws RuntimeException
	{
		return (I_REST_View)MTable.get(getCtx(), I_REST_View.Table_ID)
			.getPO(getREST_ReferenceView_ID(), get_TrxName());
	}

	/** Set Reference Rest View.
		@param REST_ReferenceView_ID Reference Rest View
	*/
	public void setREST_ReferenceView_ID (int REST_ReferenceView_ID)
	{
		if (REST_ReferenceView_ID < 1)
			set_Value (COLUMNNAME_REST_ReferenceView_ID, null);
		else
			set_Value (COLUMNNAME_REST_ReferenceView_ID, Integer.valueOf(REST_ReferenceView_ID));
	}

	/** Get Reference Rest View.
		@return Reference Rest View	  */
	public int getREST_ReferenceView_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_ReferenceView_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Rest View Column ID.
		@param REST_ViewColumn_ID Rest View Column ID
	*/
	public void setREST_ViewColumn_ID (int REST_ViewColumn_ID)
	{
		if (REST_ViewColumn_ID < 1)
			set_ValueNoCheck (COLUMNNAME_REST_ViewColumn_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_REST_ViewColumn_ID, Integer.valueOf(REST_ViewColumn_ID));
	}

	/** Get Rest View Column ID.
		@return Rest View Column ID	  */
	public int getREST_ViewColumn_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_ViewColumn_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Rest View Column UID.
		@param REST_ViewColumn_UU Rest View Column UID
	*/
	public void setREST_ViewColumn_UU (String REST_ViewColumn_UU)
	{
		set_ValueNoCheck (COLUMNNAME_REST_ViewColumn_UU, REST_ViewColumn_UU);
	}

	/** Get Rest View Column UID.
		@return Rest View Column UID	  */
	public String getREST_ViewColumn_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_ViewColumn_UU);
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

	/** Set Sequence.
		@param SeqNo Method of ordering records; lowest number comes first
	*/
	public void setSeqNo (int SeqNo)
	{
		set_Value (COLUMNNAME_SeqNo, Integer.valueOf(SeqNo));
	}

	/** Get Sequence.
		@return Method of ordering records; lowest number comes first
	  */
	public int getSeqNo()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SeqNo);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}
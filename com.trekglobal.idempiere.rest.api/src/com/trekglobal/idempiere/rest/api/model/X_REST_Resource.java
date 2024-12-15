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

/** Generated Model for REST_Resource
 *  @author iDempiere (generated)
 *  @version Release 11 - $Id$ */
@org.adempiere.base.Model(table="REST_Resource")
public class X_REST_Resource extends PO implements I_REST_Resource, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20241213L;

    /** Standard Constructor */
    public X_REST_Resource (Properties ctx, int REST_Resource_ID, String trxName)
    {
      super (ctx, REST_Resource_ID, trxName);
      /** if (REST_Resource_ID == 0)
        {
			setName (null);
			setREST_ResourcePath (null);
			setREST_Resource_ID (0);
			setREST_Resource_UU (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_Resource (Properties ctx, int REST_Resource_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_Resource_ID, trxName, virtualColumns);
      /** if (REST_Resource_ID == 0)
        {
			setName (null);
			setREST_ResourcePath (null);
			setREST_Resource_ID (0);
			setREST_Resource_UU (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_Resource (Properties ctx, String REST_Resource_UU, String trxName)
    {
      super (ctx, REST_Resource_UU, trxName);
      /** if (REST_Resource_UU == null)
        {
			setName (null);
			setREST_ResourcePath (null);
			setREST_Resource_ID (0);
			setREST_Resource_UU (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_Resource (Properties ctx, String REST_Resource_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_Resource_UU, trxName, virtualColumns);
      /** if (REST_Resource_UU == null)
        {
			setName (null);
			setREST_ResourcePath (null);
			setREST_Resource_ID (0);
			setREST_Resource_UU (null);
        } */
    }

    /** Load Constructor */
    public X_REST_Resource (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_REST_Resource[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
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

	/** Set Resource Path.
		@param REST_ResourcePath Resource Path
	*/
	public void setREST_ResourcePath (String REST_ResourcePath)
	{
		set_Value (COLUMNNAME_REST_ResourcePath, REST_ResourcePath);
	}

	/** Get Resource Path.
		@return Resource Path	  */
	public String getREST_ResourcePath()
	{
		return (String)get_Value(COLUMNNAME_REST_ResourcePath);
	}

	/** Set REST Resource.
		@param REST_Resource_ID REST Resource
	*/
	public void setREST_Resource_ID (int REST_Resource_ID)
	{
		if (REST_Resource_ID < 1)
			set_ValueNoCheck (COLUMNNAME_REST_Resource_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_REST_Resource_ID, Integer.valueOf(REST_Resource_ID));
	}

	/** Get REST Resource.
		@return REST Resource	  */
	public int getREST_Resource_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_Resource_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set REST Resource UUID.
		@param REST_Resource_UU REST Resource UUID
	*/
	public void setREST_Resource_UU (String REST_Resource_UU)
	{
		set_ValueNoCheck (COLUMNNAME_REST_Resource_UU, REST_Resource_UU);
	}

	/** Get REST Resource UUID.
		@return REST Resource UUID	  */
	public String getREST_Resource_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_Resource_UU);
	}
}
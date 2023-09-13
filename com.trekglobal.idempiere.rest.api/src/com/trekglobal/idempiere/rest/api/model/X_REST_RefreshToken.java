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

/** Generated Model for REST_RefreshToken
 *  @author iDempiere (generated)
 *  @version Release 11 - $Id$ */
@org.adempiere.base.Model(table="REST_RefreshToken")
public class X_REST_RefreshToken extends PO implements I_REST_RefreshToken, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20230913L;

    /** Standard Constructor */
    public X_REST_RefreshToken (Properties ctx, int REST_RefreshToken_ID_ignored, String trxName)
    {
      super (ctx, REST_RefreshToken_ID_ignored, trxName);
      /** if (REST_RefreshToken_ID_ignored == 0)
        {
        } */
    }

    /** Standard Constructor */
    public X_REST_RefreshToken (Properties ctx, int REST_RefreshToken_ID_ignored, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_RefreshToken_ID_ignored, trxName, virtualColumns);
      /** if (REST_RefreshToken_ID_ignored == 0)
        {
        } */
    }

    /** Standard Constructor */
    public X_REST_RefreshToken (Properties ctx, String REST_RefreshToken_UU, String trxName)
    {
      super (ctx, REST_RefreshToken_UU, trxName);
      /** if (REST_RefreshToken_UU == null)
        {
        } */
    }

    /** Standard Constructor */
    public X_REST_RefreshToken (Properties ctx, String REST_RefreshToken_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_RefreshToken_UU, trxName, virtualColumns);
      /** if (REST_RefreshToken_UU == null)
        {
        } */
    }

    /** Load Constructor */
    public X_REST_RefreshToken (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_REST_RefreshToken[")
        .append(get_ID()).append("]");
      return sb.toString();
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

	/** Set Refresh Token.
		@param RefreshToken Refresh Token
	*/
	public void setRefreshToken (String RefreshToken)
	{
		set_Value (COLUMNNAME_RefreshToken, RefreshToken);
	}

	/** Get Refresh Token.
		@return Refresh Token	  */
	public String getRefreshToken()
	{
		return (String)get_Value(COLUMNNAME_RefreshToken);
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
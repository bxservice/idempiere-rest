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
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="REST_RefreshToken")
public class X_REST_RefreshToken extends PO implements I_REST_RefreshToken, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20241022L;

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
      StringBuilder sb = new StringBuilder ("X_REST_RefreshToken[")
        .append(get_UUID()).append("]");
      return sb.toString();
    }

	/** Set Absolute Expires At.
		@param AbsoluteExpiresAt Absolute Expires At
	*/
	public void setAbsoluteExpiresAt (Timestamp AbsoluteExpiresAt)
	{
		set_Value (COLUMNNAME_AbsoluteExpiresAt, AbsoluteExpiresAt);
	}

	/** Get Absolute Expires At.
		@return Absolute Expires At	  */
	public Timestamp getAbsoluteExpiresAt()
	{
		return (Timestamp)get_Value(COLUMNNAME_AbsoluteExpiresAt);
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

	/** Set Parent Token.
		@param ParentToken Parent Token
	*/
	public void setParentToken (String ParentToken)
	{
		set_ValueNoCheck (COLUMNNAME_ParentToken, ParentToken);
	}

	/** Get Parent Token.
		@return Parent Token	  */
	public String getParentToken()
	{
		return (String)get_Value(COLUMNNAME_ParentToken);
	}

	/** Set REST_RefreshToken_UU.
		@param REST_RefreshToken_UU REST_RefreshToken_UU
	*/
	public void setREST_RefreshToken_UU (String REST_RefreshToken_UU)
	{
		set_Value (COLUMNNAME_REST_RefreshToken_UU, REST_RefreshToken_UU);
	}

	/** Get REST_RefreshToken_UU.
		@return REST_RefreshToken_UU	  */
	public String getREST_RefreshToken_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_RefreshToken_UU);
	}

	/** Breach = B */
	public static final String REST_REVOKECAUSE_Breach = "B";
	/** Breach Chain = C */
	public static final String REST_REVOKECAUSE_BreachChain = "C";
	/** Logout = L */
	public static final String REST_REVOKECAUSE_Logout = "L";
	/** Manual Expire = M */
	public static final String REST_REVOKECAUSE_ManualExpire = "M";
	/** Password Change = P */
	public static final String REST_REVOKECAUSE_PasswordChange = "P";
	/** Set Revocation Cause.
		@param REST_RevokeCause Revocation Cause
	*/
	public void setREST_RevokeCause (String REST_RevokeCause)
	{

		set_Value (COLUMNNAME_REST_RevokeCause, REST_RevokeCause);
	}

	/** Get Revocation Cause.
		@return Revocation Cause	  */
	public String getREST_RevokeCause()
	{
		return (String)get_Value(COLUMNNAME_REST_RevokeCause);
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

	/** Set Revoked At.
		@param RevokedAt Revoked At
	*/
	public void setRevokedAt (Timestamp RevokedAt)
	{
		set_Value (COLUMNNAME_RevokedAt, RevokedAt);
	}

	/** Get Revoked At.
		@return Revoked At	  */
	public Timestamp getRevokedAt()
	{
		return (Timestamp)get_Value(COLUMNNAME_RevokedAt);
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
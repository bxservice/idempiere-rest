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

/** Generated Model for REST_OIDCService
 *  @author iDempiere (generated)
 *  @version Release 11 - $Id$ */
@org.adempiere.base.Model(table="REST_OIDCService")
public class X_REST_OIDCService extends PO implements I_REST_OIDCService, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20230707L;

    /** Standard Constructor */
    public X_REST_OIDCService (Properties ctx, int REST_OIDCService_ID, String trxName)
    {
      super (ctx, REST_OIDCService_ID, trxName);
      /** if (REST_OIDCService_ID == 0)
        {
			setIsAuthorization_OIDC (true);
// Y
			setIsValidateScope_OIDC (false);
// N
			setOIDC_Audience (null);
			setOIDC_AuthorityURL (null);
			setOIDC_ConfigurationURL (null);
			setOIDC_IssuerURL (null);
			setREST_OIDCProvider_ID (0);
			setREST_OIDCService_ID (0);
			setREST_OIDCService_UU (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_OIDCService (Properties ctx, int REST_OIDCService_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_OIDCService_ID, trxName, virtualColumns);
      /** if (REST_OIDCService_ID == 0)
        {
			setIsAuthorization_OIDC (true);
// Y
			setIsValidateScope_OIDC (false);
// N
			setOIDC_Audience (null);
			setOIDC_AuthorityURL (null);
			setOIDC_ConfigurationURL (null);
			setOIDC_IssuerURL (null);
			setREST_OIDCProvider_ID (0);
			setREST_OIDCService_ID (0);
			setREST_OIDCService_UU (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_OIDCService (Properties ctx, String REST_OIDCService_UU, String trxName)
    {
      super (ctx, REST_OIDCService_UU, trxName);
      /** if (REST_OIDCService_UU == null)
        {
			setIsAuthorization_OIDC (true);
// Y
			setIsValidateScope_OIDC (false);
// N
			setOIDC_Audience (null);
			setOIDC_AuthorityURL (null);
			setOIDC_ConfigurationURL (null);
			setOIDC_IssuerURL (null);
			setREST_OIDCProvider_ID (0);
			setREST_OIDCService_ID (0);
			setREST_OIDCService_UU (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_OIDCService (Properties ctx, String REST_OIDCService_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_OIDCService_UU, trxName, virtualColumns);
      /** if (REST_OIDCService_UU == null)
        {
			setIsAuthorization_OIDC (true);
// Y
			setIsValidateScope_OIDC (false);
// N
			setOIDC_Audience (null);
			setOIDC_AuthorityURL (null);
			setOIDC_ConfigurationURL (null);
			setOIDC_IssuerURL (null);
			setREST_OIDCProvider_ID (0);
			setREST_OIDCService_ID (0);
			setREST_OIDCService_UU (null);
        } */
    }

    /** Load Constructor */
    public X_REST_OIDCService (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_REST_OIDCService[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Authorization.
		@param IsAuthorization_OIDC Access token include authorization claims
	*/
	public void setIsAuthorization_OIDC (boolean IsAuthorization_OIDC)
	{
		set_Value (COLUMNNAME_IsAuthorization_OIDC, Boolean.valueOf(IsAuthorization_OIDC));
	}

	/** Get Authorization.
		@return Access token include authorization claims
	  */
	public boolean isAuthorization_OIDC()
	{
		Object oo = get_Value(COLUMNNAME_IsAuthorization_OIDC);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Validate Scope.
		@param IsValidateScope_OIDC Validate Rest API Endpoint against Scope claim
	*/
	public void setIsValidateScope_OIDC (boolean IsValidateScope_OIDC)
	{
		set_Value (COLUMNNAME_IsValidateScope_OIDC, Boolean.valueOf(IsValidateScope_OIDC));
	}

	/** Get Validate Scope.
		@return Validate Rest API Endpoint against Scope claim
	  */
	public boolean isValidateScope_OIDC()
	{
		Object oo = get_Value(COLUMNNAME_IsValidateScope_OIDC);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Audience.
		@param OIDC_Audience OpenID Connect Audience (aud) claim validation
	*/
	public void setOIDC_Audience (String OIDC_Audience)
	{
		set_Value (COLUMNNAME_OIDC_Audience, OIDC_Audience);
	}

	/** Get Audience.
		@return OpenID Connect Audience (aud) claim validation
	  */
	public String getOIDC_Audience()
	{
		return (String)get_Value(COLUMNNAME_OIDC_Audience);
	}

	/** Set Authority URL.
		@param OIDC_AuthorityURL OpenID Connect authority URL
	*/
	public void setOIDC_AuthorityURL (String OIDC_AuthorityURL)
	{
		set_Value (COLUMNNAME_OIDC_AuthorityURL, OIDC_AuthorityURL);
	}

	/** Get Authority URL.
		@return OpenID Connect authority URL
	  */
	public String getOIDC_AuthorityURL()
	{
		return (String)get_Value(COLUMNNAME_OIDC_AuthorityURL);
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

	/** Set Issuer URL.
		@param OIDC_IssuerURL URL for iss claim in JWT access token
	*/
	public void setOIDC_IssuerURL (String OIDC_IssuerURL)
	{
		set_Value (COLUMNNAME_OIDC_IssuerURL, OIDC_IssuerURL);
	}

	/** Get Issuer URL.
		@return URL for iss claim in JWT access token
	  */
	public String getOIDC_IssuerURL()
	{
		return (String)get_Value(COLUMNNAME_OIDC_IssuerURL);
	}

	public com.trekglobal.idempiere.rest.api.model.I_REST_OIDCProvider getREST_OIDCProvider() throws RuntimeException
	{
		return (com.trekglobal.idempiere.rest.api.model.I_REST_OIDCProvider)MTable.get(getCtx(), com.trekglobal.idempiere.rest.api.model.I_REST_OIDCProvider.Table_ID)
			.getPO(getREST_OIDCProvider_ID(), get_TrxName());
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

	/** Set Rest OpenID Connect Service.
		@param REST_OIDCService_ID Rest OpenID Connect Service
	*/
	public void setREST_OIDCService_ID (int REST_OIDCService_ID)
	{
		if (REST_OIDCService_ID < 1)
			set_ValueNoCheck (COLUMNNAME_REST_OIDCService_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_REST_OIDCService_ID, Integer.valueOf(REST_OIDCService_ID));
	}

	/** Get Rest OpenID Connect Service.
		@return Rest OpenID Connect Service	  */
	public int getREST_OIDCService_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_OIDCService_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set REST_OIDCService_UU.
		@param REST_OIDCService_UU REST_OIDCService_UU
	*/
	public void setREST_OIDCService_UU (String REST_OIDCService_UU)
	{
		set_ValueNoCheck (COLUMNNAME_REST_OIDCService_UU, REST_OIDCService_UU);
	}

	/** Get REST_OIDCService_UU.
		@return REST_OIDCService_UU	  */
	public String getREST_OIDCService_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_OIDCService_UU);
	}
}
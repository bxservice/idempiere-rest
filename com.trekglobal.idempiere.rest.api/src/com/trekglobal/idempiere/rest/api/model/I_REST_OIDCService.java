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
package com.trekglobal.idempiere.rest.api.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for REST_OIDCService
 *  @author iDempiere (generated) 
 *  @version Release 11
 */
@SuppressWarnings("all")
public interface I_REST_OIDCService 
{

    /** TableName=REST_OIDCService */
    public static final String Table_Name = "REST_OIDCService";

    /** AD_Table_ID=1000002 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 6 - System - Client 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(6);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Tenant.
	  * Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within tenant
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within tenant
	  */
	public int getAD_Org_ID();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name IsAuthorization_OIDC */
    public static final String COLUMNNAME_IsAuthorization_OIDC = "IsAuthorization_OIDC";

	/** Set Authorization.
	  * Access token include authorization claims
	  */
	public void setIsAuthorization_OIDC (boolean IsAuthorization_OIDC);

	/** Get Authorization.
	  * Access token include authorization claims
	  */
	public boolean isAuthorization_OIDC();

    /** Column name IsValidateScope_OIDC */
    public static final String COLUMNNAME_IsValidateScope_OIDC = "IsValidateScope_OIDC";

	/** Set Validate Scope.
	  * Validate Rest API Endpoint against Scope claim
	  */
	public void setIsValidateScope_OIDC (boolean IsValidateScope_OIDC);

	/** Get Validate Scope.
	  * Validate Rest API Endpoint against Scope claim
	  */
	public boolean isValidateScope_OIDC();

    /** Column name OIDC_Audience */
    public static final String COLUMNNAME_OIDC_Audience = "OIDC_Audience";

	/** Set Audience.
	  * OpenID Connect Audience (aud) claim validation
	  */
	public void setOIDC_Audience (String OIDC_Audience);

	/** Get Audience.
	  * OpenID Connect Audience (aud) claim validation
	  */
	public String getOIDC_Audience();

    /** Column name OIDC_AuthorityURL */
    public static final String COLUMNNAME_OIDC_AuthorityURL = "OIDC_AuthorityURL";

	/** Set Authority URL.
	  * OpenID Connect authority URL
	  */
	public void setOIDC_AuthorityURL (String OIDC_AuthorityURL);

	/** Get Authority URL.
	  * OpenID Connect authority URL
	  */
	public String getOIDC_AuthorityURL();

    /** Column name OIDC_ConfigurationURL */
    public static final String COLUMNNAME_OIDC_ConfigurationURL = "OIDC_ConfigurationURL";

	/** Set Configuration URL.
	  * OpenID Connect Configuration Endpoint
	  */
	public void setOIDC_ConfigurationURL (String OIDC_ConfigurationURL);

	/** Get Configuration URL.
	  * OpenID Connect Configuration Endpoint
	  */
	public String getOIDC_ConfigurationURL();

    /** Column name OIDC_IssuerURL */
    public static final String COLUMNNAME_OIDC_IssuerURL = "OIDC_IssuerURL";

	/** Set Issuer URL.
	  * URL for iss claim in JWT access token
	  */
	public void setOIDC_IssuerURL (String OIDC_IssuerURL);

	/** Get Issuer URL.
	  * URL for iss claim in JWT access token
	  */
	public String getOIDC_IssuerURL();

    /** Column name REST_OIDCProvider_ID */
    public static final String COLUMNNAME_REST_OIDCProvider_ID = "REST_OIDCProvider_ID";

	/** Set Rest OpenID Connect Provider	  */
	public void setREST_OIDCProvider_ID (int REST_OIDCProvider_ID);

	/** Get Rest OpenID Connect Provider	  */
	public int getREST_OIDCProvider_ID();

	public com.trekglobal.idempiere.rest.api.model.I_REST_OIDCProvider getREST_OIDCProvider() throws RuntimeException;

    /** Column name REST_OIDCService_ID */
    public static final String COLUMNNAME_REST_OIDCService_ID = "REST_OIDCService_ID";

	/** Set Rest OpenID Connect Service	  */
	public void setREST_OIDCService_ID (int REST_OIDCService_ID);

	/** Get Rest OpenID Connect Service	  */
	public int getREST_OIDCService_ID();

    /** Column name REST_OIDCService_UU */
    public static final String COLUMNNAME_REST_OIDCService_UU = "REST_OIDCService_UU";

	/** Set REST_OIDCService_UU	  */
	public void setREST_OIDCService_UU (String REST_OIDCService_UU);

	/** Get REST_OIDCService_UU	  */
	public String getREST_OIDCService_UU();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();
}

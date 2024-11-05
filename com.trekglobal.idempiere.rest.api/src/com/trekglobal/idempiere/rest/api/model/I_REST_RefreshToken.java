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

/** Generated Interface for REST_RefreshToken
 *  @author iDempiere (generated) 
 *  @version Release 12
 */
@SuppressWarnings("all")
public interface I_REST_RefreshToken 
{

    /** TableName=REST_RefreshToken */
    public static final String Table_Name = "REST_RefreshToken";

    /** AD_Table_ID=1000003 */
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

    /** Column name AbsoluteExpiresAt */
    public static final String COLUMNNAME_AbsoluteExpiresAt = "AbsoluteExpiresAt";

	/** Set Absolute Expires At	  */
	public void setAbsoluteExpiresAt (Timestamp AbsoluteExpiresAt);

	/** Get Absolute Expires At	  */
	public Timestamp getAbsoluteExpiresAt();

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

    /** Column name ExpiresAt */
    public static final String COLUMNNAME_ExpiresAt = "ExpiresAt";

	/** Set Expires At	  */
	public void setExpiresAt (Timestamp ExpiresAt);

	/** Get Expires At	  */
	public Timestamp getExpiresAt();

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

    /** Column name ParentToken */
    public static final String COLUMNNAME_ParentToken = "ParentToken";

	/** Set Parent Token	  */
	public void setParentToken (String ParentToken);

	/** Get Parent Token	  */
	public String getParentToken();

    /** Column name REST_RefreshToken_UU */
    public static final String COLUMNNAME_REST_RefreshToken_UU = "REST_RefreshToken_UU";

	/** Set REST_RefreshToken_UU	  */
	public void setREST_RefreshToken_UU (String REST_RefreshToken_UU);

	/** Get REST_RefreshToken_UU	  */
	public String getREST_RefreshToken_UU();

    /** Column name REST_RevokeCause */
    public static final String COLUMNNAME_REST_RevokeCause = "REST_RevokeCause";

	/** Set Revocation Cause	  */
	public void setREST_RevokeCause (String REST_RevokeCause);

	/** Get Revocation Cause	  */
	public String getREST_RevokeCause();

    /** Column name RefreshToken */
    public static final String COLUMNNAME_RefreshToken = "RefreshToken";

	/** Set Refresh Token	  */
	public void setRefreshToken (String RefreshToken);

	/** Get Refresh Token	  */
	public String getRefreshToken();

    /** Column name RevokedAt */
    public static final String COLUMNNAME_RevokedAt = "RevokedAt";

	/** Set Revoked At	  */
	public void setRevokedAt (Timestamp RevokedAt);

	/** Get Revoked At	  */
	public Timestamp getRevokedAt();

    /** Column name Token */
    public static final String COLUMNNAME_Token = "Token";

	/** Set Token	  */
	public void setToken (String Token);

	/** Get Token	  */
	public String getToken();

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

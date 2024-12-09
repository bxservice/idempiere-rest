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

/** Generated Interface for REST_ViewRelated
 *  @author iDempiere (generated) 
 *  @version Release 11
 */
@SuppressWarnings("all")
public interface I_REST_ViewRelated 
{

    /** TableName=REST_ViewRelated */
    public static final String Table_Name = "REST_ViewRelated";

    /** AD_Table_ID=1000007 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 4 - System 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(4);

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

    /** Column name IsRestAutoExpand */
    public static final String COLUMNNAME_IsRestAutoExpand = "IsRestAutoExpand";

	/** Set Auto Expand	  */
	public void setIsRestAutoExpand (boolean IsRestAutoExpand);

	/** Get Auto Expand	  */
	public boolean isRestAutoExpand();

    /** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();

    /** Column name REST_RelatedRestView_ID */
    public static final String COLUMNNAME_REST_RelatedRestView_ID = "REST_RelatedRestView_ID";

	/** Set Related rest view	  */
	public void setREST_RelatedRestView_ID (int REST_RelatedRestView_ID);

	/** Get Related rest view	  */
	public int getREST_RelatedRestView_ID();

	public I_REST_View getREST_RelatedRestView() throws RuntimeException;

    /** Column name REST_ViewRelated_ID */
    public static final String COLUMNNAME_REST_ViewRelated_ID = "REST_ViewRelated_ID";

	/** Set Rest Related View	  */
	public void setREST_ViewRelated_ID (int REST_ViewRelated_ID);

	/** Get Rest Related View	  */
	public int getREST_ViewRelated_ID();

    /** Column name REST_ViewRelated_UU */
    public static final String COLUMNNAME_REST_ViewRelated_UU = "REST_ViewRelated_UU";

	/** Set Rest related view uid	  */
	public void setREST_ViewRelated_UU (String REST_ViewRelated_UU);

	/** Get Rest related view uid	  */
	public String getREST_ViewRelated_UU();

    /** Column name REST_View_ID */
    public static final String COLUMNNAME_REST_View_ID = "REST_View_ID";

	/** Set Rest View ID	  */
	public void setREST_View_ID (int REST_View_ID);

	/** Get Rest View ID	  */
	public int getREST_View_ID();

	public I_REST_View getREST_View() throws RuntimeException;

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

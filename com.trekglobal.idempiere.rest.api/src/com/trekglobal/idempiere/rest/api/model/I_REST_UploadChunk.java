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

/** Generated Interface for REST_UploadChunk
 *  @author iDempiere (generated) 
 *  @version Release 13
 */
@SuppressWarnings("all")
public interface I_REST_UploadChunk 
{

    /** TableName=REST_UploadChunk */
    public static final String Table_Name = "REST_UploadChunk";

    /** AD_Table_ID=1000010 */
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

    /** Column name REST_ReceivedSize */
    public static final String COLUMNNAME_REST_ReceivedSize = "REST_ReceivedSize";

	/** Set Received Size	  */
	public void setREST_ReceivedSize (BigDecimal REST_ReceivedSize);

	/** Get Received Size	  */
	public BigDecimal getREST_ReceivedSize();

    /** Column name REST_SHA256 */
    public static final String COLUMNNAME_REST_SHA256 = "REST_SHA256";

	/** Set SHA-256	  */
	public void setREST_SHA256 (String REST_SHA256);

	/** Get SHA-256	  */
	public String getREST_SHA256();

    /** Column name REST_UploadChunk_ID */
    public static final String COLUMNNAME_REST_UploadChunk_ID = "REST_UploadChunk_ID";

	/** Set Upload Chunk	  */
	public void setREST_UploadChunk_ID (int REST_UploadChunk_ID);

	/** Get Upload Chunk	  */
	public int getREST_UploadChunk_ID();

    /** Column name REST_UploadChunk_UU */
    public static final String COLUMNNAME_REST_UploadChunk_UU = "REST_UploadChunk_UU";

	/** Set REST_UploadChunk_UU	  */
	public void setREST_UploadChunk_UU (String REST_UploadChunk_UU);

	/** Get REST_UploadChunk_UU	  */
	public String getREST_UploadChunk_UU();

    /** Column name REST_Upload_ID */
    public static final String COLUMNNAME_REST_Upload_ID = "REST_Upload_ID";

	/** Set Rest Upload	  */
	public void setREST_Upload_ID (int REST_Upload_ID);

	/** Get Rest Upload	  */
	public int getREST_Upload_ID();

	public com.trekglobal.idempiere.rest.api.model.I_REST_Upload getREST_Upload() throws RuntimeException;

    /** Column name SeqNo */
    public static final String COLUMNNAME_SeqNo = "SeqNo";

	/** Set Sequence.
	  * Method of ordering records;
 lowest number comes first
	  */
	public void setSeqNo (int SeqNo);

	/** Get Sequence.
	  * Method of ordering records;
 lowest number comes first
	  */
	public int getSeqNo();

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

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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for REST_Upload
 *  @author iDempiere (generated)
 *  @version Release 13 - $Id$ */
@org.adempiere.base.Model(table="REST_Upload")
public class X_REST_Upload extends PO implements I_REST_Upload, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250605L;

    /** Standard Constructor */
    public X_REST_Upload (Properties ctx, int REST_Upload_ID, String trxName)
    {
      super (ctx, REST_Upload_ID, trxName);
      /** if (REST_Upload_ID == 0)
        {
			setExpiresAt (new Timestamp( System.currentTimeMillis() ));
			setFileName (null);
			setFileSize (Env.ZERO);
			setREST_SHA256 (null);
			setREST_UploadStatus (null);
			setREST_Upload_ID (0);
			setREST_Upload_UU (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_Upload (Properties ctx, int REST_Upload_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_Upload_ID, trxName, virtualColumns);
      /** if (REST_Upload_ID == 0)
        {
			setExpiresAt (new Timestamp( System.currentTimeMillis() ));
			setFileName (null);
			setFileSize (Env.ZERO);
			setREST_SHA256 (null);
			setREST_UploadStatus (null);
			setREST_Upload_ID (0);
			setREST_Upload_UU (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_Upload (Properties ctx, String REST_Upload_UU, String trxName)
    {
      super (ctx, REST_Upload_UU, trxName);
      /** if (REST_Upload_UU == null)
        {
			setExpiresAt (new Timestamp( System.currentTimeMillis() ));
			setFileName (null);
			setFileSize (Env.ZERO);
			setREST_SHA256 (null);
			setREST_UploadStatus (null);
			setREST_Upload_ID (0);
			setREST_Upload_UU (null);
        } */
    }

    /** Standard Constructor */
    public X_REST_Upload (Properties ctx, String REST_Upload_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_Upload_UU, trxName, virtualColumns);
      /** if (REST_Upload_UU == null)
        {
			setExpiresAt (new Timestamp( System.currentTimeMillis() ));
			setFileName (null);
			setFileSize (Env.ZERO);
			setREST_SHA256 (null);
			setREST_UploadStatus (null);
			setREST_Upload_ID (0);
			setREST_Upload_UU (null);
        } */
    }

    /** Load Constructor */
    public X_REST_Upload (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_REST_Upload[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Image.
		@param AD_Image_ID Image or Icon
	*/
	public void setAD_Image_ID (int AD_Image_ID)
	{
		if (AD_Image_ID < 1)
			set_Value (COLUMNNAME_AD_Image_ID, null);
		else
			set_Value (COLUMNNAME_AD_Image_ID, Integer.valueOf(AD_Image_ID));
	}

	/** Get Image.
		@return Image or Icon
	  */
	public int getAD_Image_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Image_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set File Name.
		@param FileName Name of the local file or URL
	*/
	public void setFileName (String FileName)
	{
		set_Value (COLUMNNAME_FileName, FileName);
	}

	/** Get File Name.
		@return Name of the local file or URL
	  */
	public String getFileName()
	{
		return (String)get_Value(COLUMNNAME_FileName);
	}

	/** Set File Size.
		@param FileSize Size of the File in bytes
	*/
	public void setFileSize (BigDecimal FileSize)
	{
		set_Value (COLUMNNAME_FileSize, FileSize);
	}

	/** Get File Size.
		@return Size of the File in bytes
	  */
	public BigDecimal getFileSize()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_FileSize);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Chunk Size.
		@param REST_ChunkSize Chunk Size
	*/
	public void setREST_ChunkSize (BigDecimal REST_ChunkSize)
	{
		set_Value (COLUMNNAME_REST_ChunkSize, REST_ChunkSize);
	}

	/** Get Chunk Size.
		@return Chunk Size	  */
	public BigDecimal getREST_ChunkSize()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_REST_ChunkSize);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Content Type.
		@param REST_ContentType Content Type
	*/
	public void setREST_ContentType (String REST_ContentType)
	{
		set_Value (COLUMNNAME_REST_ContentType, REST_ContentType);
	}

	/** Get Content Type.
		@return Content Type	  */
	public String getREST_ContentType()
	{
		return (String)get_Value(COLUMNNAME_REST_ContentType);
	}

	/** Set SHA-256.
		@param REST_SHA256 SHA-256
	*/
	public void setREST_SHA256 (String REST_SHA256)
	{
		set_ValueNoCheck (COLUMNNAME_REST_SHA256, REST_SHA256);
	}

	/** Get SHA-256.
		@return SHA-256	  */
	public String getREST_SHA256()
	{
		return (String)get_Value(COLUMNNAME_REST_SHA256);
	}

	/** Archive = ARCHIVE */
	public static final String REST_UPLOADLOCATION_Archive = "ARCHIVE";
	/** Attachment = ATTACHMENT */
	public static final String REST_UPLOADLOCATION_Attachment = "ATTACHMENT";
	/** Image = IMAGE */
	public static final String REST_UPLOADLOCATION_Image = "IMAGE";
	/** Set Upload Location.
		@param REST_UploadLocation Upload Location
	*/
	public void setREST_UploadLocation (String REST_UploadLocation)
	{

		set_Value (COLUMNNAME_REST_UploadLocation, REST_UploadLocation);
	}

	/** Get Upload Location.
		@return Upload Location	  */
	public String getREST_UploadLocation()
	{
		return (String)get_Value(COLUMNNAME_REST_UploadLocation);
	}

	/** Canceled = CANCELED */
	public static final String REST_UPLOADSTATUS_Canceled = "CANCELED";
	/** Completed = COMPLETED */
	public static final String REST_UPLOADSTATUS_Completed = "COMPLETED";
	/** Failed = FAILED */
	public static final String REST_UPLOADSTATUS_Failed = "FAILED";
	/** Initiated = INITIATED */
	public static final String REST_UPLOADSTATUS_Initiated = "INITIATED";
	/** Processing = PROCESSING */
	public static final String REST_UPLOADSTATUS_Processing = "PROCESSING";
	/** Uploading = UPLOADING */
	public static final String REST_UPLOADSTATUS_Uploading = "UPLOADING";
	/** Set Upload Status.
		@param REST_UploadStatus Upload Status
	*/
	public void setREST_UploadStatus (String REST_UploadStatus)
	{

		set_Value (COLUMNNAME_REST_UploadStatus, REST_UploadStatus);
	}

	/** Get Upload Status.
		@return Upload Status	  */
	public String getREST_UploadStatus()
	{
		return (String)get_Value(COLUMNNAME_REST_UploadStatus);
	}

	/** Set Rest Upload.
		@param REST_Upload_ID Rest Upload
	*/
	public void setREST_Upload_ID (int REST_Upload_ID)
	{
		if (REST_Upload_ID < 1)
			set_ValueNoCheck (COLUMNNAME_REST_Upload_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_REST_Upload_ID, Integer.valueOf(REST_Upload_ID));
	}

	/** Get Rest Upload.
		@return Rest Upload	  */
	public int getREST_Upload_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_Upload_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set REST_Upload_UU.
		@param REST_Upload_UU REST_Upload_UU
	*/
	public void setREST_Upload_UU (String REST_Upload_UU)
	{
		set_Value (COLUMNNAME_REST_Upload_UU, REST_Upload_UU);
	}

	/** Get REST_Upload_UU.
		@return REST_Upload_UU	  */
	public String getREST_Upload_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_Upload_UU);
	}
}
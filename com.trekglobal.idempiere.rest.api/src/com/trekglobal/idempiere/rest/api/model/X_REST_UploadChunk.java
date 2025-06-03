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
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for REST_UploadChunk
 *  @author iDempiere (generated)
 *  @version Release 13 - $Id$ */
@org.adempiere.base.Model(table="REST_UploadChunk")
public class X_REST_UploadChunk extends PO implements I_REST_UploadChunk, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20250527L;

    /** Standard Constructor */
    public X_REST_UploadChunk (Properties ctx, int REST_UploadChunk_ID, String trxName)
    {
      super (ctx, REST_UploadChunk_ID, trxName);
      /** if (REST_UploadChunk_ID == 0)
        {
			setREST_ReceivedSize (Env.ZERO);
			setREST_SHA256 (null);
			setREST_UploadChunk_ID (0);
			setREST_UploadChunk_UU (null);
			setREST_Upload_ID (0);
			setSeqNo (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_UploadChunk (Properties ctx, int REST_UploadChunk_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_UploadChunk_ID, trxName, virtualColumns);
      /** if (REST_UploadChunk_ID == 0)
        {
			setREST_ReceivedSize (Env.ZERO);
			setREST_SHA256 (null);
			setREST_UploadChunk_ID (0);
			setREST_UploadChunk_UU (null);
			setREST_Upload_ID (0);
			setSeqNo (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_UploadChunk (Properties ctx, String REST_UploadChunk_UU, String trxName)
    {
      super (ctx, REST_UploadChunk_UU, trxName);
      /** if (REST_UploadChunk_UU == null)
        {
			setREST_ReceivedSize (Env.ZERO);
			setREST_SHA256 (null);
			setREST_UploadChunk_ID (0);
			setREST_UploadChunk_UU (null);
			setREST_Upload_ID (0);
			setSeqNo (0);
        } */
    }

    /** Standard Constructor */
    public X_REST_UploadChunk (Properties ctx, String REST_UploadChunk_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, REST_UploadChunk_UU, trxName, virtualColumns);
      /** if (REST_UploadChunk_UU == null)
        {
			setREST_ReceivedSize (Env.ZERO);
			setREST_SHA256 (null);
			setREST_UploadChunk_ID (0);
			setREST_UploadChunk_UU (null);
			setREST_Upload_ID (0);
			setSeqNo (0);
        } */
    }

    /** Load Constructor */
    public X_REST_UploadChunk (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_REST_UploadChunk[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Received Size.
		@param REST_ReceivedSize Received Size
	*/
	public void setREST_ReceivedSize (BigDecimal REST_ReceivedSize)
	{
		set_Value (COLUMNNAME_REST_ReceivedSize, REST_ReceivedSize);
	}

	/** Get Received Size.
		@return Received Size	  */
	public BigDecimal getREST_ReceivedSize()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_REST_ReceivedSize);
		if (bd == null)
			 return Env.ZERO;
		return bd;
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

	/** Set Upload Chunk.
		@param REST_UploadChunk_ID Upload Chunk
	*/
	public void setREST_UploadChunk_ID (int REST_UploadChunk_ID)
	{
		if (REST_UploadChunk_ID < 1)
			set_ValueNoCheck (COLUMNNAME_REST_UploadChunk_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_REST_UploadChunk_ID, Integer.valueOf(REST_UploadChunk_ID));
	}

	/** Get Upload Chunk.
		@return Upload Chunk	  */
	public int getREST_UploadChunk_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_REST_UploadChunk_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set REST_UploadChunk_UU.
		@param REST_UploadChunk_UU REST_UploadChunk_UU
	*/
	public void setREST_UploadChunk_UU (String REST_UploadChunk_UU)
	{
		set_Value (COLUMNNAME_REST_UploadChunk_UU, REST_UploadChunk_UU);
	}

	/** Get REST_UploadChunk_UU.
		@return REST_UploadChunk_UU	  */
	public String getREST_UploadChunk_UU()
	{
		return (String)get_Value(COLUMNNAME_REST_UploadChunk_UU);
	}

	public com.trekglobal.idempiere.rest.api.model.I_REST_Upload getREST_Upload() throws RuntimeException
	{
		return (com.trekglobal.idempiere.rest.api.model.I_REST_Upload)MTable.get(getCtx(), com.trekglobal.idempiere.rest.api.model.I_REST_Upload.Table_ID)
			.getPO(getREST_Upload_ID(), get_TrxName());
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
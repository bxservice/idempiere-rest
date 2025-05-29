/**********************************************************************
* This file is part of iDempiere ERP Open Source                      *
* http://www.idempiere.org                                            *
*                                                                     *
* Copyright (C) Contributors                                          *
*                                                                     *
* This program is free software; you can redistribute it and/or       *
* modify it under the terms of the GNU General Public License         *
* as published by the Free Software Foundation; either version 2      *
* of the License, or (at your option) any later version.              *
*                                                                     *
* This program is distributed in the hope that it will be useful,     *
* but WITHOUT ANY WARRANTY; without even the implied warranty of      *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the        *
* GNU General Public License for more details.                        *
*                                                                     *
* You should have received a copy of the GNU General Public License   *
* along with this program; if not, write to the Free Software         *
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,          *
* MA 02110-1301, USA.                                                 *
*                                                                     *
* Contributors:                                                       *
* - Trek Global Corporation                                           *
* - Heng Sin Low                                                      *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.Env;

public class MRestUpload extends X_REST_Upload {

	private static final long serialVersionUID = -1586483030692148483L;

	public MRestUpload(Properties ctx, int REST_Upload_ID, String trxName) {
		super(ctx, REST_Upload_ID, trxName);
	}

	public MRestUpload(Properties ctx, int REST_Upload_ID, String trxName, String... virtualColumns) {
		super(ctx, REST_Upload_ID, trxName, virtualColumns);
	}

	public MRestUpload(Properties ctx, String REST_Upload_UU, String trxName) {
		super(ctx, REST_Upload_UU, trxName);
	}

	public MRestUpload(Properties ctx, String REST_Upload_UU, String trxName, String... virtualColumns) {
		super(ctx, REST_Upload_UU, trxName, virtualColumns);
	}

	public MRestUpload(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Find upload by upload uuid.
	 * @param uploadId upload uuid
	 * @return MRestUpload instance or null if not found
	 */
	public static MRestUpload get(String uploadId) {
		Query query = new Query(Env.getCtx(), I_REST_Upload.Table_Name, "REST_Upload_UU=?", null);
		return query.setParameters(uploadId).first();
	}

	/**
	 * Set the status of the upload.
	 * @param status
	 */
	public void setStatus(String status) {
		setREST_UploadStatus(status);
	}
	
	/**
	 * Get the status of the upload.
	 * @return status of the upload
	 */
	public String getStatus() {
		return getREST_UploadStatus();
	}
	
	/**
	 * Set the chunk size for the upload.
	 * @param chunkSize
	 */
	public void setChunkSize(long chunkSize) {
		setREST_ChunkSize(BigDecimal.valueOf(chunkSize));
	}
	
	/**
	 * Get the chunk size for the upload.
	 * @return chunk size for the upload
	 */
	public long getChunkSize() {
		return getREST_ChunkSize() != null ? getREST_ChunkSize().longValue() : 0L;
	}
	
	/**
	 * Set the content type of the upload.
	 * @param contentType
	 */
	public void setContentType(String contentType) {
		setREST_ContentType(contentType);
	}	
	
	/**
	 * Get the content type of the upload.
	 * @return content type of the upload
	 */
	public String getContentType() {
		return getREST_ContentType();
	}
}

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

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.Env;

public class MRestUploadChunk extends X_REST_UploadChunk {

	private static final long serialVersionUID = -4887198754499988534L;

	public MRestUploadChunk(Properties ctx, int REST_UploadChunk_ID, String trxName) {
		super(ctx, REST_UploadChunk_ID, trxName);
	}

	public MRestUploadChunk(Properties ctx, int REST_UploadChunk_ID, String trxName, String... virtualColumns) {
		super(ctx, REST_UploadChunk_ID, trxName, virtualColumns);
	}

	public MRestUploadChunk(Properties ctx, String REST_UploadChunk_UU, String trxName) {
		super(ctx, REST_UploadChunk_UU, trxName);
	}

	public MRestUploadChunk(Properties ctx, String REST_UploadChunk_UU, String trxName, String... virtualColumns) {
		super(ctx, REST_UploadChunk_UU, trxName, virtualColumns);
	}

	public MRestUploadChunk(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Find upload chunks by upload uuid.
	 * @param uploadId upload uuid
	 * @return MRestUploadChunk list or empty list if not found
	 */
	public static List<MRestUploadChunk> findByUploadId(int uploadId) {
		Query query = new Query(Env.getCtx(), I_REST_UploadChunk.Table_Name, "REST_Upload_ID=?", null);
		return query.setParameters(uploadId).list();
	}

	/**
	 * Get total size of the chunk data received.
	 * @return total size of the chunk data received.
	 */
	public long getReceivedSize() {
		return getREST_ReceivedSize() != null ? getREST_ReceivedSize().longValue() : 0L;
	}
}

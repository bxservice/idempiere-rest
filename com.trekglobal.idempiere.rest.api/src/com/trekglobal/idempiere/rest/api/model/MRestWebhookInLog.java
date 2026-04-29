/***********************************************************************
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
 **********************************************************************/

package com.trekglobal.idempiere.rest.api.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.Util;

/**
 * Business model for REST_Webhook_In_Log.
 *
 * @author muriloht Murilo H. Torquato &lt;murilo@muriloht.com&gt;
 */
public class MRestWebhookInLog extends X_REST_Webhook_In_Log {

	private static final long serialVersionUID = 20260411L;

	public MRestWebhookInLog(Properties ctx, int REST_Webhook_In_Log_ID, String trxName) {
		super(ctx, REST_Webhook_In_Log_ID, trxName);
	}

	public MRestWebhookInLog(Properties ctx, String REST_Webhook_In_Log_UU, String trxName) {
		super(ctx, REST_Webhook_In_Log_UU, trxName);
	}

	public MRestWebhookInLog(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Check if a webhook message has already been received (deduplication).
	 * @param ctx context
	 * @param inboundId REST_Webhook_In_ID
	 * @param messageId the webhook message ID from the sender
	 * @param trxName transaction
	 * @return true if a log entry with this inboundId + messageId already exists
	 */
	public static boolean isDuplicate(Properties ctx, int inboundId, String messageId, String trxName) {
		if (Util.isEmpty(messageId, true))
			return false;

		return new Query(ctx, Table_Name,
				"REST_Webhook_In_ID=? AND WebhookMessageId=?", trxName)
				.setParameters(inboundId, messageId)
				.match();
	}

	/**
	 * Mark this log entry as successfully processed.
	 * Sets Processed=true, ProcessedAt=now, HttpStatus=200.
	 */
	public void markProcessed() {
		setProcessed(true);
		setProcessedAt(new Timestamp(System.currentTimeMillis()));
		setHttpStatus(200);
		saveEx();
	}

	/**
	 * Mark this log entry as failed with an error message.
	 * Sets IsError=true, ErrorMessage, ProcessedAt=now.
	 * @param errorMessage the error description
	 */
	public void markError(String errorMessage) {
		setIsError(true);
		setErrorMessage(errorMessage);
		setProcessedAt(new Timestamp(System.currentTimeMillis()));
		saveEx();
	}
}

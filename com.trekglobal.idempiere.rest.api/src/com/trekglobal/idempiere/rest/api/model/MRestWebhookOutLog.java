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
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.compiere.model.Query;

/**
 * Business model for REST_Webhook_Out_Log.
 * Manages webhook delivery lifecycle: pending, success, failed, abandoned.
 * Implements exponential backoff with jitter for retry scheduling.
 *
 * @author muriloht Murilo H. Torquato &lt;murilo@muriloht.com&gt;
 */
public class MRestWebhookOutLog extends X_REST_Webhook_Out_Log {

	private static final long serialVersionUID = 20260411L;

	/**
	 * Backoff delays in seconds, indexed by attempt number.
	 * Attempt 0 = immediate (first delivery), then exponential growth up to 24h.
	 * {0s, 5s, 5m, 30m, 2h, 5h, 10h, 14h, 20h, 24h}
	 */
	public static final int[] BACKOFF_DELAYS = {0, 5, 300, 1800, 7200, 18000, 36000, 50400, 72000, 86400};

	public MRestWebhookOutLog(Properties ctx, int REST_Webhook_Out_Log_ID, String trxName) {
		super(ctx, REST_Webhook_Out_Log_ID, trxName);
	}

	public MRestWebhookOutLog(Properties ctx, String REST_Webhook_Out_Log_UU, String trxName) {
		super(ctx, REST_Webhook_Out_Log_UU, trxName);
	}

	public MRestWebhookOutLog(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Get pending deliveries that are due for retry.
	 * @param ctx context
	 * @param maxAttempts maximum number of attempts before abandoning
	 * @param trxName transaction name
	 * @return list of deliveries with DeliveryStatus=P, NextRetryAt<=now, Attempts<maxAttempts
	 */
	public static List<MRestWebhookOutLog> getPendingRetries(Properties ctx, int maxAttempts, String trxName) {
		return new Query(ctx, Table_Name,
				"DeliveryStatus=?"
				+ " AND (NextRetryAt IS NULL OR NextRetryAt<=getDate())"
				+ " AND Attempts<?",
				trxName)
				.setParameters(DELIVERYSTATUS_Pending, maxAttempts)
				.setOrderBy("NextRetryAt NULLS FIRST, Created")
				.list();
	}

	/**
	 * Mark delivery as successful.
	 * @param httpStatus HTTP status code from endpoint
	 * @param responseBody response body from endpoint
	 */
	public void markSuccess(int httpStatus, String responseBody) {
		setDeliveryStatus(DELIVERYSTATUS_Success);
		setHttpStatus(httpStatus);
		setResponseBody(truncate(responseBody, 2000));
		setAttempts(getAttempts() + 1);
		setNextRetryAt(null);
		setErrorMessage(null);
		saveEx();
	}

	/**
	 * Mark delivery as failed and schedule next retry with exponential backoff + jitter.
	 * @param httpStatus HTTP status code (0 if connection error)
	 * @param responseBody response body from endpoint (may be null)
	 * @param errorMessage error description
	 */
	public void markFailed(int httpStatus, String responseBody, String errorMessage) {
		int nextAttempt = getAttempts() + 1;
		setDeliveryStatus(DELIVERYSTATUS_Pending);
		setHttpStatus(httpStatus);
		setResponseBody(truncate(responseBody, 2000));
		setErrorMessage(truncate(errorMessage, 2000));
		setAttempts(nextAttempt);

		long delaySec = getBackoffDelay(nextAttempt);
		Timestamp nextRetry = new Timestamp(System.currentTimeMillis() + (delaySec * 1000L));
		setNextRetryAt(nextRetry);

		saveEx();
	}

	/**
	 * Mark delivery as permanently abandoned (no more retries).
	 */
	public void markAbandoned() {
		setDeliveryStatus(DELIVERYSTATUS_Abandoned);
		setNextRetryAt(null);
		saveEx();
	}

	/**
	 * Calculate backoff delay for the given attempt number, with +/-10% jitter.
	 * @param attempt the attempt number (1-based: 1 = first retry after initial failure)
	 * @return delay in seconds with jitter applied
	 */
	public static long getBackoffDelay(int attempt) {
		int index = Math.min(attempt, BACKOFF_DELAYS.length - 1);
		long baseSec = BACKOFF_DELAYS[index];
		if (baseSec == 0)
			return 0;

		// Apply +/-10% jitter
		long jitterRange = Math.max(1, baseSec / 10);
		long jitter = ThreadLocalRandom.current().nextLong(-jitterRange, jitterRange + 1);
		return Math.max(1, baseSec + jitter);
	}

	/**
	 * Truncate a string to the given max length.
	 */
	private static String truncate(String value, int maxLen) {
		if (value == null)
			return null;
		if (value.length() <= maxLen)
			return value;
		return value.substring(0, maxLen);
	}
}

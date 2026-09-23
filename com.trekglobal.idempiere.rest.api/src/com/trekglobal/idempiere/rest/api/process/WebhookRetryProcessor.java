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
* - Murilo Torino                                                     *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.process;

import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MSysConfig;
import org.compiere.process.SvrProcess;

import com.trekglobal.idempiere.rest.api.model.MRestWebhookOutLog;
import com.trekglobal.idempiere.rest.api.model.MRestWebhookOut;
import com.trekglobal.idempiere.rest.api.webhook.WebhookDispatcher;

/**
 * Scheduled process that retries failed outbound webhook deliveries.
 * Should be configured in AD_Scheduler at System level (AD_Client_ID=0)
 * to run every 10 minutes. The query is cross-tenant — it finds pending
 * deliveries from all clients.
 *
 * Queries pending deliveries (status=P, NextRetryAt past, Attempts below max),
 * dispatches each via WebhookDispatcher, and marks abandoned after max retries.
 * Works in batches of {@link #REST_WEBHOOK_RETRY_BATCH_SIZE} rows, committing
 * after each batch, and stops after {@link #REST_WEBHOOK_RETRY_MAX_RUNTIME}
 * seconds so memory stays bounded regardless of backlog size.
 *
 * @author muriloht Murilo H. Torquato &lt;murilo@muriloht.com&gt;
 */
@org.adempiere.base.annotation.Process
public class WebhookRetryProcessor extends SvrProcess {

	public static final String REST_WEBHOOK_MAX_RETRIES = "REST_WEBHOOK_MAX_RETRIES";
	/** SysConfig: rows loaded per batch. */
	public static final String REST_WEBHOOK_RETRY_BATCH_SIZE = "REST_WEBHOOK_RETRY_BATCH_SIZE";
	/** SysConfig: max seconds per run; the next scheduled run continues the backlog. */
	public static final String REST_WEBHOOK_RETRY_MAX_RUNTIME = "REST_WEBHOOK_RETRY_MAX_RUNTIME";

	private int success = 0;
	private int failed = 0;
	private int abandoned = 0;
	private int skippedPaused = 0;

	@Override
	protected void prepare() {
		// No parameters
	}

	@Override
	protected String doIt() throws Exception {
		// Use System (0) for global default — SysConfig falls back to system-level value
		int maxRetries = MSysConfig.getIntValue(REST_WEBHOOK_MAX_RETRIES, 10, 0);
		int batchSize = Math.max(1, MSysConfig.getIntValue(REST_WEBHOOK_RETRY_BATCH_SIZE, 500, 0));
		long deadline = System.currentTimeMillis()
				+ MSysConfig.getIntValue(REST_WEBHOOK_RETRY_MAX_RUNTIME, 300, 0) * 1000L;

		int processed = 0;
		int lastId = 0;
		boolean timedOut = false;

		while (true) {
			List<MRestWebhookOutLog> batch = MRestWebhookOutLog.getPendingRetries(
					getCtx(), maxRetries, lastId, batchSize, get_TrxName());
			for (MRestWebhookOutLog delivery : batch) {
				lastId = delivery.get_ID();
				processDelivery(delivery, maxRetries);
			}
			processed += batch.size();
			// Persist progress per batch so a crash does not roll back the whole run
			commitEx();

			if (batch.size() < batchSize)
				break;
			if (System.currentTimeMillis() > deadline) {
				timedOut = true;
				break;
			}
		}

		if (processed == 0) {
			return "@NotFound@";
		}

		return "Processed: " + processed
				+ " (success=" + success
				+ ", failed=" + failed
				+ ", abandoned=" + abandoned
				+ ", skippedPaused=" + skippedPaused + ")"
				+ (timedOut ? " - time limit reached, continuing next run" : "");
	}

	private void processDelivery(MRestWebhookOutLog delivery, int maxRetries) {
		try {
			// Endpoint may have been paused after the batch was loaded
			MRestWebhookOut endpoint = new MRestWebhookOut(getCtx(),
					delivery.getREST_Webhook_Out_ID(), get_TrxName());
			if (endpoint.get_ID() > 0 && endpoint.isPaused()) {
				skippedPaused++;
				return;
			}

			if (delivery.getAttempts() >= maxRetries) {
				delivery.markAbandoned();
				abandoned++;
				return;
			}

			boolean ok = WebhookDispatcher.dispatch(getCtx(), delivery.get_ID(), null);
			if (ok) {
				success++;
			} else {
				failed++;
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Retry failed for delivery " + delivery.get_ID(), e);
			try {
				delivery.markFailed(0, null,
						"Dispatcher error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			} catch (Exception markErr) {
				log.log(Level.SEVERE, "Failed to mark delivery " + delivery.get_ID() + " as failed", markErr);
			}
			failed++;
		}
	}
}

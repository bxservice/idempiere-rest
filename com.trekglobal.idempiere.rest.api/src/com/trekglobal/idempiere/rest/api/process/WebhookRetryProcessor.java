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
 *
 * @author muriloht Murilo H. Torquato &lt;murilo@muriloht.com&gt;
 */
@org.adempiere.base.annotation.Process
public class WebhookRetryProcessor extends SvrProcess {

	public static final String REST_WEBHOOK_MAX_RETRIES = "REST_WEBHOOK_MAX_RETRIES";

	@Override
	protected void prepare() {
		// No parameters
	}

	@Override
	protected String doIt() throws Exception {
		// Use System (0) for global default — SysConfig falls back to system-level value
		int maxRetries = MSysConfig.getIntValue(REST_WEBHOOK_MAX_RETRIES, 10, 0);

		List<MRestWebhookOutLog> pending = MRestWebhookOutLog.getPendingRetries(
				getCtx(), maxRetries, get_TrxName());

		if (pending.isEmpty()) {
			return "@NotFound@";
		}

		int success = 0;
		int failed = 0;
		int abandoned = 0;
		int skippedPaused = 0;

		for (MRestWebhookOutLog delivery : pending) {
			try {
				MRestWebhookOut endpoint = new MRestWebhookOut(getCtx(),
						delivery.getREST_Webhook_Out_ID(), get_TrxName());
				if (endpoint.get_ID() > 0 && endpoint.isPaused()) {
					skippedPaused++;
					continue;
				}

				if (delivery.getAttempts() >= maxRetries) {
					delivery.markAbandoned();
					abandoned++;
					continue;
				}

				boolean ok = WebhookDispatcher.dispatch(getCtx(), delivery.get_ID(), null);
				if (ok) {
					success++;
				} else {
					failed++;
				}
			} catch (Exception e) {
				log.log(Level.WARNING, "Retry failed for delivery " + delivery.get_ID(), e);
				failed++;
			}
		}

		return "Processed: " + pending.size()
				+ " (success=" + success
				+ ", failed=" + failed
				+ ", abandoned=" + abandoned
				+ ", skippedPaused=" + skippedPaused + ")";
	}
}

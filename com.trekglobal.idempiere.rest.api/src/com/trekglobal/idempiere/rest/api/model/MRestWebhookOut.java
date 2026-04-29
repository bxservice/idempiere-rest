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

import org.compiere.model.Query;
import org.compiere.util.Env;

import com.trekglobal.idempiere.rest.api.webhook.WebhookEventHandler;

/**
 * Business model for REST_Webhook_Out.
 *
 * @author muriloht Murilo H. Torquato &lt;murilo@muriloht.com&gt;
 */
public class MRestWebhookOut extends X_REST_Webhook_Out {

	private static final long serialVersionUID = 20260411L;

	public MRestWebhookOut(Properties ctx, int REST_Webhook_Out_ID, String trxName) {
		super(ctx, REST_Webhook_Out_ID, trxName);
	}

	public MRestWebhookOut(Properties ctx, String REST_Webhook_Out_UU, String trxName) {
		super(ctx, REST_Webhook_Out_UU, trxName);
	}

	public MRestWebhookOut(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Get all active endpoints for event capture, including paused ones.
	 * Paused endpoints still need deliveries created (queued) so events
	 * are not lost — dispatch is skipped until the endpoint is unpaused.
	 * Includes System-level endpoints (AD_Client_ID=0) alongside the
	 * current tenant's endpoints.
	 * @param ctx context
	 * @param trxName transaction name
	 * @return list of active endpoints (including paused and system-level)
	 */
	public static List<MRestWebhookOut> getEndpointsForEventCapture(Properties ctx, String trxName) {
		int clientId = Env.getAD_Client_ID(ctx);
		return new Query(ctx, Table_Name, "AD_Client_ID IN (0, ?)", trxName)
				.setParameters(clientId)
				.setOnlyActiveRecords(true)
				.setOrderBy("Name")
				.list();
	}

	/**
	 * Increment the consecutive failure counter. If the counter reaches
	 * MaxConsecutiveFailures (when > 0), the endpoint is automatically paused.
	 */
	private static final int DEFAULT_MAX_CONSECUTIVE_FAILURES = 50;

	public void incrementFailure() {
		setConsecutiveFailures(getConsecutiveFailures() + 1);
		setLastFailureAt(new Timestamp(System.currentTimeMillis()));
		int max = getMaxConsecutiveFailures();
		if (max <= 0) max = DEFAULT_MAX_CONSECUTIVE_FAILURES;
		if (getConsecutiveFailures() >= max) {
			setIsPaused(true);
		}
		saveEx();
	}

	/**
	 * Record a successful delivery. Resets the consecutive failure counter
	 * and updates the last success timestamp.
	 */
	public void recordSuccess() {
		setConsecutiveFailures(0);
		setLastSuccessAt(new Timestamp(System.currentTimeMillis()));
		saveEx();
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (success)
			WebhookEventHandler.scheduleReinitialize(get_TrxName());
		return true;
	}

	@Override
	protected boolean afterDelete(boolean success) {
		if (success)
			WebhookEventHandler.scheduleReinitialize(get_TrxName());
		return true;
	}
}

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
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.trekglobal.idempiere.rest.api.webhook.WebhookEventHandler;
import com.trekglobal.idempiere.rest.api.webhook.WebhookSignature;

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
	 * Atomically increment the consecutive failure counter and pause the endpoint
	 * if the threshold is reached. Uses a single SQL UPDATE so concurrent
	 * dispatches don't lose increments.
	 */
	public void incrementFailure() {
		int max = getMaxConsecutiveFailures();
		Timestamp now = new Timestamp(System.currentTimeMillis());
		if (max > 0) {
			DB.executeUpdateEx(
					"UPDATE " + Table_Name
					+ " SET ConsecutiveFailures = ConsecutiveFailures + 1,"
					+ "     LastFailureAt = ?,"
					+ "     IsPaused = CASE WHEN ConsecutiveFailures + 1 >= ? THEN 'Y' ELSE IsPaused END"
					+ " WHERE REST_Webhook_Out_ID = ?",
					new Object[] { now, max, get_ID() }, get_TrxName());
		} else {
			DB.executeUpdateEx(
					"UPDATE " + Table_Name
					+ " SET ConsecutiveFailures = ConsecutiveFailures + 1,"
					+ "     LastFailureAt = ?"
					+ " WHERE REST_Webhook_Out_ID = ?",
					new Object[] { now, get_ID() }, get_TrxName());
		}
	}

	/**
	 * Atomically reset the consecutive failure counter and stamp the last success
	 * timestamp.
	 */
	public void recordSuccess() {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		DB.executeUpdateEx(
				"UPDATE " + Table_Name
				+ " SET ConsecutiveFailures = 0,"
				+ "     LastSuccessAt = ?"
				+ " WHERE REST_Webhook_Out_ID = ?",
				new Object[] { now, get_ID() }, get_TrxName());
	}

	/**
	 * Auto-generate a Standard Webhooks-format secret when this endpoint is
	 * configured as Standard Webhook and no secret was provided. Follows the
	 * same convention as Stripe/Svix: sender generates, operator copies the
	 * value to share with the receiver.
	 *
	 * <p>If the operator pasted a specific secret (e.g., one dictated by an
	 * upstream provider), it is preserved as-is. Auto-gen only fires when the
	 * field is empty.
	 *
	 * <p>Raw mode endpoints ({@code IsStandardWebhook='N'}) don't need a
	 * secret — no signing happens — so we leave the field alone.
	 */
	@Override
	protected boolean beforeSave(boolean newRecord) {
		if (getConsecutiveFailures() < 0) {
			log.saveError("Error", "ConsecutiveFailures cannot be negative");
			return false;
		}
		if (getMaxConsecutiveFailures() < 0) {
			log.saveError("Error", "MaxConsecutiveFailures cannot be negative");
			return false;
		}
		if (isStandardWebhook() && Util.isEmpty(getWebhookSecret(), true)) {
			setWebhookSecret(WebhookSignature.generateSecret());
		}
		return true;
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

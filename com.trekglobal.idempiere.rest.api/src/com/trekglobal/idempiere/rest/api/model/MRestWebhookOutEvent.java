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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.Util;

import com.trekglobal.idempiere.rest.api.webhook.WebhookEventHandler;

/**
 * Business model for REST_Webhook_Out_Event.
 *
 * @author muriloht Murilo H. Torquato &lt;murilo@muriloht.com&gt;
 */
public class MRestWebhookOutEvent extends X_REST_Webhook_Out_Event {

	private static final long serialVersionUID = 20260411L;

	public MRestWebhookOutEvent(Properties ctx, int REST_Webhook_Out_Event_ID, String trxName) {
		super(ctx, REST_Webhook_Out_Event_ID, trxName);
	}

	public MRestWebhookOutEvent(Properties ctx, String REST_Webhook_Out_Event_UU, String trxName) {
		super(ctx, REST_Webhook_Out_Event_UU, trxName);
	}

	public MRestWebhookOutEvent(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Get all active event subscriptions across all active endpoints.
	 * Used by WebhookEventHandler to determine which table/event combinations to register.
	 * @param ctx context
	 * @return list of all active subscriptions with active parent endpoints
	 */
	public static List<MRestWebhookOutEvent> getAllActiveSubscriptions(Properties ctx) {
		List<MRestWebhookOut> endpoints = MRestWebhookOut.getEndpointsForEventCapture(ctx, null);
		List<MRestWebhookOutEvent> result = new ArrayList<>();
		for (MRestWebhookOut endpoint : endpoints)
			result.addAll(getByEndpoint(ctx, endpoint.getREST_Webhook_Out_ID(), null));
		return result;
	}

	/**
	 * Get all active event subscriptions for a given endpoint, ordered by SeqNo.
	 * @param ctx context
	 * @param REST_Webhook_Out_ID parent endpoint ID
	 * @param trxName transaction name
	 * @return list of event subscriptions
	 */
	public static List<MRestWebhookOutEvent> getByEndpoint(Properties ctx, int REST_Webhook_Out_ID, String trxName) {
		return new Query(ctx, Table_Name, "REST_Webhook_Out_ID=?", trxName)
				.setParameters(REST_Webhook_Out_ID)
				.setOnlyActiveRecords(true)
				.setOrderBy("SeqNo")
				.list();
	}

	/**
	 * Check if this event subscription matches the given table and event type.
	 * @param AD_Table_ID the table ID
	 * @param eventModelValidator the model validator event type
	 * @return true if both table and event type match
	 */
	public boolean matchesEvent(int AD_Table_ID, String eventModelValidator) {
		if (getAD_Table_ID() != AD_Table_ID)
			return false;
		if (Util.isEmpty(eventModelValidator, true))
			return false;
		return eventModelValidator.equals(getEventModelValidator());
	}

	/**
	 * Parse IncludeColumns into a string array.
	 * @return array of column names to include, or empty array if not set
	 */
	public String[] getIncludeColumnsArray() {
		String include = getIncludeColumns();
		if (Util.isEmpty(include, true))
			return new String[0];
		return include.split("\\s*,\\s*");
	}

	/**
	 * Parse ExcludeColumns into a string array.
	 * @return array of column names to exclude, or empty array if not set
	 */
	public String[] getExcludeColumnsArray() {
		String exclude = getExcludeColumns();
		if (Util.isEmpty(exclude, true))
			return new String[0];
		return exclude.split("\\s*,\\s*");
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

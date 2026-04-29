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
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.Util;

import com.trekglobal.idempiere.rest.api.webhook.WebhookIPAllowlist;

/**
 * Business model for REST_Webhook_In.
 *
 * @author muriloht Murilo H. Torquato &lt;murilo@muriloht.com&gt;
 */
public class MRestWebhookIn extends X_REST_Webhook_In {

	private static final long serialVersionUID = 20260411L;

	/** Cache by EndpointKey — 60 minute TTL */
	private static CCache<String, MRestWebhookIn> s_cache =
			new CCache<>(Table_Name + "_EndpointKey", 40, 60);

	public MRestWebhookIn(Properties ctx, int REST_Webhook_In_ID, String trxName) {
		super(ctx, REST_Webhook_In_ID, trxName);
	}

	public MRestWebhookIn(Properties ctx, String REST_Webhook_In_UU, String trxName) {
		super(ctx, REST_Webhook_In_UU, trxName);
	}

	public MRestWebhookIn(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	/**
	 * Get webhook inbound configuration by endpoint key (cached).
	 * @param ctx context
	 * @param endpointKey the unique endpoint key
	 * @param trxName transaction
	 * @return MRestWebhookIn or null if not found / inactive
	 */
	public static MRestWebhookIn getByEndpointKey(Properties ctx, String endpointKey, String trxName) {
		if (Util.isEmpty(endpointKey, true))
			return null;

		MRestWebhookIn cached = s_cache.get(endpointKey);
		if (cached != null)
			return cached;

		MRestWebhookIn result = new Query(ctx, Table_Name, "EndpointKey=?", trxName)
				.setParameters(endpointKey)
				.setOnlyActiveRecords(true)
				.first();

		if (result != null)
			s_cache.put(endpointKey, result);

		return result;
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		try {
			WebhookIPAllowlist.validate(getAllowedIPs());
		} catch (IllegalArgumentException e) {
			log.saveError("Error", e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (success)
			s_cache.reset();
		return success;
	}

	@Override
	protected boolean afterDelete(boolean success) {
		if (success)
			s_cache.reset();
		return success;
	}

	/**
	 * Reset the endpoint key cache.
	 */
	public static void resetCache() {
		s_cache.reset();
	}
}

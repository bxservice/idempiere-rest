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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.Util;

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
	 * Check whether the given remote address is allowed to call this webhook.
	 * If AllowedIPs is empty/null, all addresses are allowed.
	 * Supports plain IP addresses and CIDR notation (e.g. 10.0.0.0/8).
	 *
	 * @param remoteAddr the caller's IP address
	 * @return true if the address is allowed (or no restriction configured)
	 */
	public boolean isIPAllowed(String remoteAddr) {
		String allowedIPs = getAllowedIPs();
		if (Util.isEmpty(allowedIPs, true))
			return true;
		if (Util.isEmpty(remoteAddr, true))
			return false;

		String[] entries = allowedIPs.split(",");
		for (String entry : entries) {
			String trimmed = entry.trim();
			if (trimmed.isEmpty())
				continue;

			if (trimmed.contains("/")) {
				// CIDR notation
				if (isInCIDR(remoteAddr, trimmed))
					return true;
			} else {
				// Exact match
				if (trimmed.equals(remoteAddr))
					return true;
			}
		}
		return false;
	}

	/**
	 * Reset the endpoint key cache.
	 */
	public static void resetCache() {
		s_cache.reset();
	}

	/**
	 * Check if an IP address is within a CIDR range.
	 * @param ip the IP address to check
	 * @param cidr the CIDR range (e.g. "192.168.1.0/24")
	 * @return true if the IP is within the CIDR range
	 */
	private static boolean isInCIDR(String ip, String cidr) {
		try {
			String[] parts = cidr.split("/");
			if (parts.length != 2)
				return false;

			InetAddress cidrAddress = InetAddress.getByName(parts[0].trim());
			InetAddress remoteAddress = InetAddress.getByName(ip.trim());

			int prefixLength = Integer.parseInt(parts[1].trim());

			byte[] cidrBytes = cidrAddress.getAddress();
			byte[] remoteBytes = remoteAddress.getAddress();

			if (cidrBytes.length != remoteBytes.length)
				return false;

			int fullBytes = prefixLength / 8;
			int remainingBits = prefixLength % 8;

			for (int i = 0; i < fullBytes; i++) {
				if (cidrBytes[i] != remoteBytes[i])
					return false;
			}

			if (remainingBits > 0 && fullBytes < cidrBytes.length) {
				int mask = 0xFF << (8 - remainingBits);
				if ((cidrBytes[fullBytes] & mask) != (remoteBytes[fullBytes] & mask))
					return false;
			}

			return true;
		} catch (UnknownHostException | NumberFormatException e) {
			return false;
		}
	}
}

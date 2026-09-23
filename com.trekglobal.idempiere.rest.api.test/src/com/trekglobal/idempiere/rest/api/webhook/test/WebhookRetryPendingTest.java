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
**********************************************************************/
package com.trekglobal.idempiere.rest.api.webhook.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.compiere.util.Env;
import org.junit.jupiter.api.Test;

import com.trekglobal.idempiere.rest.api.json.test.RestTestCase;
import com.trekglobal.idempiere.rest.api.model.MRestWebhookOut;
import com.trekglobal.idempiere.rest.api.model.MRestWebhookOutLog;

/**
 * Tests for {@link MRestWebhookOutLog#iteratePendingRetries} (#537):
 * rows of paused endpoints are not loaded by the retry processor.
 */
public class WebhookRetryPendingTest extends RestTestCase {

	private static final int MAX_ATTEMPTS = 10;

	private MRestWebhookOut endpoint(String name, boolean paused) {
		MRestWebhookOut ep = new MRestWebhookOut(Env.getCtx(), 0, getTrxName());
		ep.setName(name);
		ep.setURL("https://example.invalid/hook");
		ep.setIsPaused(paused);
		ep.saveEx();
		return ep;
	}

	private int pendingDelivery(MRestWebhookOut ep) {
		MRestWebhookOutLog log = new MRestWebhookOutLog(Env.getCtx(), 0, getTrxName());
		log.setREST_Webhook_Out_ID(ep.getREST_Webhook_Out_ID());
		log.setWebhookMessageId("msg_test_" + System.nanoTime());
		log.setEventType(MRestWebhookOutLog.EVENTTYPE_DocumentAfterComplete);
		log.setRecord_ID(0);
		log.setPayload("{}");
		log.setDeliveryStatus(MRestWebhookOutLog.DELIVERYSTATUS_Pending);
		log.setAttempts(0);
		log.saveEx();
		return log.get_ID();
	}

	private Set<Integer> pendingIds() {
		Set<Integer> ids = new HashSet<>();
		Iterator<MRestWebhookOutLog> it = MRestWebhookOutLog.iteratePendingRetries(Env.getCtx(), MAX_ATTEMPTS, getTrxName());
		while (it.hasNext())
			ids.add(it.next().get_ID());
		return ids;
	}

	@Test
	public void pausedEndpointRowsAreNotLoaded() {
		int active = pendingDelivery(endpoint("retry-test-active", false));
		int paused = pendingDelivery(endpoint("retry-test-paused", true));

		Set<Integer> loaded = pendingIds();
		assertTrue(loaded.contains(active), "active endpoint row must be loaded");
		assertFalse(loaded.contains(paused), "paused endpoint row must not be loaded");
	}
}

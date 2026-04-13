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
package com.trekglobal.idempiere.rest.api.webhook;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.compiere.Adempiere;
import org.compiere.model.MTable;
import org.compiere.model.MSysConfig;
import org.compiere.model.PO;
import org.compiere.model.ServerStateChangeEvent;
import org.compiere.model.ServerStateChangeListener;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.TrxEventListener;
import org.compiere.util.Util;
import org.osgi.service.event.Event;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.model.MRestWebhookOut;
import com.trekglobal.idempiere.rest.api.model.MRestWebhookOutEvent;
import com.trekglobal.idempiere.rest.api.model.MRestWebhookOutLog;

/**
 * OSGi EventHandler that captures iDempiere model/document events
 * and creates webhook deliveries for subscribed endpoints.
 *
 * Registers table-specific events at startup based on active REST_Webhook_Out_Event
 * configurations. Re-registers dynamically when subscriptions change (via
 * MRestWebhookOutEvent/MRestWebhookOut afterSave/afterDelete).
 *
 * Dispatch happens post-commit via TrxEventListener.afterCommit for DOC_AFTER_*
 * events. PO_POST_* events fire after commit, so dispatch is immediate.
 *
 * @author muriloht Murilo H. Torquato &lt;murilo@muriloht.com&gt;
 */
public class WebhookEventHandler extends AbstractEventHandler {
	private static final CLogger log = CLogger.getCLogger(WebhookEventHandler.class);

	public static final String REST_WEBHOOK_ENABLED = "REST_WEBHOOK_ENABLED";
	public static final String REST_WEBHOOK_MAX_PAYLOAD_SIZE = "REST_WEBHOOK_MAX_PAYLOAD_SIZE";

	/** Maps IEventTopics to AD_Table_ScriptValidator event codes */
	private static final Map<String, String> TOPIC_TO_EVENT = new HashMap<>();
	/** Reverse map: event codes to IEventTopics */
	private static final Map<String, String> EVENT_CODE_TO_TOPIC = new HashMap<>();
	static {
		TOPIC_TO_EVENT.put(IEventTopics.PO_POST_CREATE,          "TAN");
		TOPIC_TO_EVENT.put(IEventTopics.PO_POST_UPADTE,          "TAC");
		TOPIC_TO_EVENT.put(IEventTopics.PO_POST_DELETE,          "TAD");
		TOPIC_TO_EVENT.put(IEventTopics.DOC_AFTER_COMPLETE,      "DACO");
		TOPIC_TO_EVENT.put(IEventTopics.DOC_AFTER_VOID,          "DAVO");
		TOPIC_TO_EVENT.put(IEventTopics.DOC_AFTER_CLOSE,         "DACL");
		TOPIC_TO_EVENT.put(IEventTopics.DOC_AFTER_REACTIVATE,    "DAAC");
		TOPIC_TO_EVENT.put(IEventTopics.DOC_AFTER_REVERSECORRECT,"DARC");

		for (Map.Entry<String, String> e : TOPIC_TO_EVENT.entrySet())
			EVENT_CODE_TO_TOPIC.put(e.getValue(), e.getKey());
	}

	/** Singleton reference used by reinitialize() */
	private static volatile WebhookEventHandler instance;

	@Override
	protected void initialize() {
		if (!Adempiere.isStarted()) {
			Adempiere.addServerStateChangeListener(new ServerStateChangeListener() {
				@Override
				public void stateChange(ServerStateChangeEvent e) {
					if (e.getEventType() == ServerStateChangeEvent.SERVER_START && Adempiere.isStarted())
						initialize();
				}
			});
			return;
		}

		instance = this;

		List<MRestWebhookOutEvent> subscriptions =
				MRestWebhookOutEvent.getAllActiveSubscriptions(Env.getCtx());

		if (subscriptions.isEmpty()) {
			if (log.isLoggable(Level.INFO))
				log.info("WebhookEventHandler: no active subscriptions, no events registered");
			return;
		}

		// Register table-specific events — deduplicate (tableName, topic) pairs
		Set<String> registered = new HashSet<>();
		for (MRestWebhookOutEvent sub : subscriptions) {
			String tableName = MTable.getTableName(Env.getCtx(), sub.getAD_Table_ID());
			if (tableName == null) continue;
			String topic = EVENT_CODE_TO_TOPIC.get(sub.getEventModelValidator());
			if (topic == null) continue;
			if (registered.add(topic + "/" + tableName))
				registerTableEvent(topic, tableName);
		}

		if (log.isLoggable(Level.INFO))
			log.info("WebhookEventHandler: registered " + registered.size() + " table/event combinations");
	}

	/**
	 * Unregister all current subscriptions and re-register based on the
	 * current active configuration. Called from MRestWebhookOutEvent and
	 * MRestWebhookOut afterSave/afterDelete after commit.
	 */
	public static void reinitialize() {
		WebhookEventHandler handler = instance;
		if (handler == null || handler.eventManager == null) return;
		synchronized (handler) {
			handler.eventManager.unregister(handler);
			handler.initialize();
		}
	}

	/**
	 * Schedule a reinitialize after the current transaction commits, or immediately if no transaction is active.
	 * Called from MRestWebhookOut and MRestWebhookOutEvent afterSave/afterDelete.
	 * @param trxName the current transaction name (may be null)
	 */
	public static void scheduleReinitialize(String trxName) {
		Trx trx = trxName != null ? Trx.get(trxName, false) : null;
		if (trx != null) {
			trx.addTrxEventListener(new TrxEventListener() {
				@Override
				public void afterCommit(Trx trx, boolean success) {
					if (success)
						reinitialize();
				}
				@Override public void afterRollback(Trx trx, boolean success) {}
				@Override public void afterClose(Trx trx) {}
			});
		} else {
			reinitialize();
		}
	}

	@Override
	protected void doHandleEvent(Event event) {
		try {
			String topic = event.getTopic();
			String eventCode = TOPIC_TO_EVENT.get(topic);
			if (eventCode == null) return;

			PO po = getPO(event);
			if (po == null) return;

			int AD_Client_ID = po.getAD_Client_ID();

			// Master switch check
			if (!MSysConfig.getBooleanValue(REST_WEBHOOK_ENABLED, true, AD_Client_ID)) return;

			int AD_Table_ID = po.get_Table_ID();

			// PO_POST_* events fire via postEvent() in a SEPARATE thread — the PO's
			// transaction belongs to the original thread and cannot be used here.
			// DOC_AFTER_* events fire via sendEvent() in the SAME thread — the trx is safe.
			// Always use null for reads; writes use the trx only for same-thread DOC events.
			boolean isPOPostEvent = IEventTopics.PO_POST_CREATE.equals(topic)
					|| IEventTopics.PO_POST_UPADTE.equals(topic)
					|| IEventTopics.PO_POST_DELETE.equals(topic);
			String poTrxName = po.get_TrxName();
			String effectiveTrxName = (!isPOPostEvent && poTrxName != null
					&& Trx.get(poTrxName, false) != null) ? poTrxName : null;

			// Always null for reads — endpoint config is not part of the PO transaction
			List<MRestWebhookOut> endpoints = MRestWebhookOut.getEndpointsForEventCapture(
					po.getCtx(), null);
			if (endpoints.isEmpty()) return;

			for (MRestWebhookOut endpoint : endpoints) {
				try {
					createDelivery(po, endpoint, eventCode, AD_Table_ID, effectiveTrxName);
				} catch (Exception e) {
					log.log(Level.WARNING, "Failed to create webhook delivery for endpoint "
							+ endpoint.getName() + ": " + e.getMessage(), e);
				}
			}
		} catch (Exception e) {
			// Never let webhook errors break the business transaction
			log.log(Level.SEVERE, "WebhookEventHandler error: " + e.getMessage(), e);
		}
	}

	private void createDelivery(PO po, MRestWebhookOut endpoint, String eventCode,
			int AD_Table_ID, String effectiveTrxName) {
		// Find the matching event config — null for reads (not part of PO transaction)
		String payloadTemplate = null;
		String[] includes = null;
		String[] excludes = null;
		boolean matched = false;
		List<MRestWebhookOutEvent> events = MRestWebhookOutEvent.getByEndpoint(
				po.getCtx(), endpoint.getREST_Webhook_Out_ID(), null);
		for (MRestWebhookOutEvent eventConfig : events) {
			if (eventConfig.matchesEvent(AD_Table_ID, eventCode)) {
				payloadTemplate = eventConfig.getPayloadTemplate();
				includes = eventConfig.getIncludeColumnsArray();
				excludes = eventConfig.getExcludeColumnsArray();
				matched = true;
				break;
			}
		}
		if (!matched)
			return;

		// Build payload — envelope or raw depending on IsStandardWebhook flag
		String payload;
		if (endpoint.isStandardWebhook()) {
			// Standard Webhooks: wrap in {data, type, timestamp} envelope
			if (!Util.isEmpty(payloadTemplate, true)) {
				String resolvedData = WebhookPayloadTemplateResolver.resolve(payloadTemplate, po);
				JsonObject envelope = new JsonObject();
				envelope.addProperty("type", po.get_TableName() + "." + eventCode);
				envelope.addProperty("timestamp", Instant.now().toString());
				try {
					envelope.add("data", JsonParser.parseString(resolvedData));
				} catch (Exception e) {
					log.warning("PayloadTemplate resolved to invalid JSON for endpoint "
							+ endpoint.getName() + ", wrapping as string: " + e.getMessage());
					envelope.addProperty("data", resolvedData);
				}
				payload = envelope.toString();
			} else {
				IPOSerializer serializer = IPOSerializer.getPOSerializer(po.get_TableName(), po.getClass());
				JsonObject data = serializer.toJson(po, includes, excludes);
				JsonObject envelope = new JsonObject();
				envelope.addProperty("type", po.get_TableName() + "." + eventCode);
				envelope.addProperty("timestamp", Instant.now().toString());
				envelope.add("data", data);
				payload = envelope.toString();
			}
		} else {
			// Raw mode: send payload as-is, no envelope
			if (!Util.isEmpty(payloadTemplate, true)) {
				payload = WebhookPayloadTemplateResolver.resolve(payloadTemplate, po);
				if (payload == null) payload = "{}";
			} else {
				IPOSerializer serializer = IPOSerializer.getPOSerializer(po.get_TableName(), po.getClass());
				payload = serializer.toJson(po, includes, excludes).toString();
			}
		}

		// Check payload size
		int maxPayloadSize = MSysConfig.getIntValue(REST_WEBHOOK_MAX_PAYLOAD_SIZE, 20480, po.getAD_Client_ID());
		if (payload.length() > maxPayloadSize) {
			log.warning("Webhook payload exceeds max size (" + payload.length() + " > " + maxPayloadSize
					+ ") for " + po.get_TableName() + " ID=" + po.get_ID() + ", endpoint=" + endpoint.getName());
			return;
		}

		String msgId = "msg_" + UUID.randomUUID().toString().replace("-", "");

		// Create delivery record (within active trx for DOC_AFTER_*, autocommit for PO_POST_*)
		MRestWebhookOutLog delivery = new MRestWebhookOutLog(po.getCtx(), 0, effectiveTrxName);
		delivery.setAD_Org_ID(po.getAD_Org_ID());
		delivery.setREST_Webhook_Out_ID(endpoint.getREST_Webhook_Out_ID());
		delivery.setWebhookMessageId(msgId);
		delivery.setEventType(eventCode);
		delivery.setAD_Table_ID(AD_Table_ID);
		delivery.setRecord_ID(po.get_ID());
		delivery.setPayload(payload);
		delivery.saveEx();

		if (endpoint.isPaused()) {
			if (log.isLoggable(Level.INFO))
				log.info("Endpoint " + endpoint.getName() + " is paused — delivery "
						+ delivery.get_ID() + " queued for later dispatch");
			return;
		}

		final int deliveryId = delivery.get_ID();
		final int clientId = po.getAD_Client_ID();

		if (effectiveTrxName != null) {
			Trx trx = Trx.get(effectiveTrxName, false);
			if (trx != null) {
				trx.addTrxEventListener(new TrxEventListener() {
					@Override
					public void afterCommit(Trx trx, boolean success) {
						if (success)
							WebhookDispatcher.dispatchAsync(deliveryId, clientId);
					}
					@Override public void afterRollback(Trx trx, boolean success) {}
					@Override public void afterClose(Trx trx) {}
				});
				return;
			}
		}
		// PO_POST_* or no trx context — dispatch immediately
		WebhookDispatcher.dispatchAsync(deliveryId, clientId);
	}
}

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

import java.util.Properties;
import java.util.logging.Level;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.compiere.model.MSysConfig;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.ServerProcessCtl;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import com.trekglobal.idempiere.rest.api.model.MRestWebhookIn;
import com.trekglobal.idempiere.rest.api.model.MRestWebhookInLog;
import com.trekglobal.idempiere.rest.api.webhook.WebhookSignature.WebhookVerificationException;

/**
 * Handles inbound webhook requests.
 * Verifies Standard Webhooks signatures, checks IP allowlist,
 * deduplicates by webhook-id, and routes payloads to configured
 * iDempiere processes.
 *
 * Transaction strategy:
 *   Phase 1 — dedicated trx for dedup check + log entry creation (atomic insert).
 *             Committed before process runs.
 *   Phase 2 — process runs in its own transaction (ServerProcessCtl manages it).
 *   Phase 3 — log status update (markProcessed/markError) runs as autocommit
 *             since the log entry is already committed to DB.
 *
 * @author muriloht Murilo H. Torquato &lt;murilo@muriloht.com&gt;
 */
public class WebhookInboundHandler {

	private static final CLogger log = CLogger.getCLogger(WebhookInboundHandler.class);

	public static final String REST_WEBHOOK_INBOUND_ENABLED = "REST_WEBHOOK_INBOUND_ENABLED";
	public static final String PROCESS_PARAM_PAYLOAD = "WebhookPayload";

	private WebhookInboundHandler() {
	}

	public static Response handle(String endpointKey, String body, HttpHeaders headers, String remoteAddr) {
		Properties ctx = Env.getCtx();

		// Lookup endpoint by key (before master switch — need AD_Client_ID for client-level SysConfig)
		MRestWebhookIn inbound = MRestWebhookIn.getByEndpointKey(ctx, endpointKey, null);
		if (inbound == null) {
			return Response.status(Response.Status.NOT_FOUND)
					.entity("{\"error\":\"Unknown endpoint\"}")
					.build();
		}

		// Set iDempiere context from endpoint configuration
		Env.setContext(ctx, Env.AD_CLIENT_ID, inbound.getAD_Client_ID());
		Env.setContext(ctx, Env.AD_ORG_ID, inbound.getAD_Org_ID());

		// Master switch (client-level — checked after endpoint lookup to honor per-tenant config)
		if (!MSysConfig.getBooleanValue(REST_WEBHOOK_INBOUND_ENABLED, true, inbound.getAD_Client_ID())) {
			return Response.status(Response.Status.SERVICE_UNAVAILABLE)
					.entity("{\"error\":\"Inbound webhooks disabled\"}")
					.build();
		}

		// IP allowlist check
		if (!inbound.isIPAllowed(remoteAddr)) {
			log.warning("Inbound webhook " + endpointKey + " rejected IP: " + remoteAddr);
			return Response.status(Response.Status.FORBIDDEN)
					.entity("{\"error\":\"IP not allowed\"}")
					.build();
		}

		// Signature verification (Standard Webhooks HMAC-SHA256)
		if (inbound.isVerifySignature()) {
			String webhookId        = getHeader(headers, "webhook-id");
			String webhookTimestamp = getHeader(headers, "webhook-timestamp");
			String webhookSignature = getHeader(headers, "webhook-signature");
			String secret = inbound.getWebhookSecret();
			if (secret == null || secret.isEmpty()) {
				log.warning("Inbound webhook " + endpointKey
						+ " has signature verification enabled but no secret configured");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity("{\"error\":\"Endpoint misconfigured\"}")
						.build();
			}
			try {
				WebhookSignature.verify(secret, webhookId, webhookTimestamp, webhookSignature, body);
			} catch (WebhookVerificationException e) {
				log.warning("Webhook signature verification failed for " + endpointKey + ": " + e.getMessage());
				return Response.status(Response.Status.UNAUTHORIZED)
						.entity("{\"error\":\"Signature verification failed\"}")
						.build();
			}
		}

		String webhookMsgId = getHeader(headers, "webhook-id");

		// -----------------------------------------------------------------------
		// PHASE 1: Dedup check + log entry creation — committed before process runs.
		// Using a dedicated transaction so the INSERT is visible to concurrent
		// requests (unique constraint catches duplicates) before process executes.
		// -----------------------------------------------------------------------
		MRestWebhookInLog logEntry;
		String trxName = Trx.createTrxName("webhook_in");
		Trx trx = Trx.get(trxName, true);
		try {
			// Soft dedup check inside the transaction
			if (webhookMsgId != null
					&& MRestWebhookInLog.isDuplicate(ctx, inbound.get_ID(), webhookMsgId, trxName)) {
				return Response.ok("{\"status\":\"duplicate\"}").build();
			}

			logEntry = new MRestWebhookInLog(ctx, 0, trxName);
			logEntry.setAD_Org_ID(inbound.getAD_Org_ID());
			logEntry.setREST_Webhook_In_ID(inbound.get_ID());
			logEntry.setWebhookMessageId(webhookMsgId);
			logEntry.setPayload(body);
			if (!Util.isEmpty(remoteAddr, true))
				logEntry.setRemoteAddr(remoteAddr);

			try {
				logEntry.saveEx();
			} catch (Exception e) {
				// Hard dedup: unique constraint violation on (Inbound_ID, MessageId)
				if (e.getMessage() != null && e.getMessage().contains("unique")) {
					log.info("Duplicate webhook detected via constraint for " + endpointKey
							+ " msgId=" + webhookMsgId);
					return Response.ok("{\"status\":\"duplicate\"}").build();
				}
				throw e;
			}

			trx.commit();
			// Detach logEntry from the now-closed transaction so Phase 3
			// markProcessed/markError use autocommit instead of the dead trxName
			logEntry.set_TrxName(null);

		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to create inbound webhook log for " + endpointKey, e);
			trx.rollback();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("{\"error\":\"Internal error\"}")
					.build();
		} finally {
			trx.close();
		}

		// -----------------------------------------------------------------------
		// PHASE 2: Process execution — ServerProcessCtl manages its own transaction.
		// -----------------------------------------------------------------------
		ProcessInfo pi = new ProcessInfo("Webhook: " + endpointKey, inbound.getAD_Process_ID());
		pi.setAD_Client_ID(inbound.getAD_Client_ID());
		int userId = inbound.getAD_User_ID();
		if (userId <= 0) userId = 100;
		pi.setAD_User_ID(userId);
		pi.setParameter(new ProcessInfoParameter[] {
				new ProcessInfoParameter(PROCESS_PARAM_PAYLOAD, body, null, null, null)
		});

		try {
			ServerProcessCtl.process(pi, null);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Process execution failed for inbound webhook " + endpointKey, e);
			logEntry.markError(e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("{\"error\":\"Internal error\"}")
					.build();
		}

		// -----------------------------------------------------------------------
		// PHASE 3: Update log status — autocommit (log entry already committed in Phase 1).
		// -----------------------------------------------------------------------
		if (pi.isError()) {
			logEntry.markError(pi.getSummary());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("{\"error\":\"Processing failed\",\"message\":\""
							+ escapeJson(pi.getSummary()) + "\"}")
					.build();
		}

		logEntry.markProcessed();
		return Response.ok("{\"status\":\"ok\"}").build();
	}

	private static String getHeader(HttpHeaders headers, String name) {
		if (headers == null) return null;
		return headers.getHeaderString(name);
	}

	private static String escapeJson(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}
}

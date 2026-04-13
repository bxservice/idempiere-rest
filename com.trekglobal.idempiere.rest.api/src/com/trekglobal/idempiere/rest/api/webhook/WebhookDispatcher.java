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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.adempiere.util.ServerContext;
import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.trekglobal.idempiere.rest.api.model.MRestWebhookOutLog;
import com.trekglobal.idempiere.rest.api.model.MRestWebhookOut;

/**
 * Sends outbound webhook HTTP POST requests per Standard Webhooks spec.
 * Handles signing, timeouts, response processing, and endpoint failure tracking.
 *
 * @author muriloht Murilo H. Torquato &lt;murilo@muriloht.com&gt;
 */
public class WebhookDispatcher {
	private static final CLogger log = CLogger.getCLogger(WebhookDispatcher.class);

	public static final String REST_WEBHOOK_TIMEOUT_MS = "REST_WEBHOOK_TIMEOUT_MS";

	private static final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.followRedirects(HttpClient.Redirect.NEVER) // Standard Webhooks: don't follow redirects
			.build();

	private WebhookDispatcher() {}

	/**
	 * Dispatch a delivery asynchronously using CompletableFuture.
	 * Called from afterCommit listener or retry processor.
	 */
	public static void dispatchAsync(int deliveryId, int AD_Client_ID) {
		CompletableFuture.runAsync(() -> {
			try {
				Properties ctx = new Properties();
				ServerContext.setCurrentInstance(ctx);
				Env.setContext(ctx, Env.AD_CLIENT_ID, AD_Client_ID);

				dispatch(ctx, deliveryId, null);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Async webhook dispatch failed for delivery " + deliveryId, e);
			} finally {
				ServerContext.dispose();
			}
		});
	}

	/**
	 * Dispatch a single webhook delivery.
	 * @param ctx context
	 * @param deliveryId REST_Webhook_Out_Log_ID
	 * @param trxName transaction name (nullable)
	 * @return true if successful (HTTP 2xx), false otherwise
	 */
	public static boolean dispatch(Properties ctx, int deliveryId, String trxName) {
		MRestWebhookOutLog delivery = new MRestWebhookOutLog(ctx, deliveryId, trxName);
		if (delivery.get_ID() == 0) {
			log.warning("Webhook delivery not found: " + deliveryId);
			return false;
		}

		MRestWebhookOut endpoint = new MRestWebhookOut(ctx,
				delivery.getREST_Webhook_Out_ID(), trxName);
		if (endpoint.get_ID() == 0 || !endpoint.isActive()) {
			log.info("Webhook endpoint inactive or not found for delivery " + deliveryId);
			delivery.markAbandoned();
			return false;
		}
		if (endpoint.isPaused()) {
			log.info("Webhook endpoint paused, skipping dispatch for delivery " + deliveryId);
			return false;
		}

		String msgId = delivery.getWebhookMessageId();
		String payload = delivery.getPayload();
		int timeoutMs = MSysConfig.getIntValue(REST_WEBHOOK_TIMEOUT_MS, 15000,
				endpoint.getAD_Client_ID());

		HttpRequest request;
		if (endpoint.isStandardWebhook()) {
			// Standard Webhooks: validate secret, sign, add webhook-* headers
			String secret = endpoint.getWebhookSecret();
			if (Util.isEmpty(secret, true)) {
				log.severe("Webhook endpoint " + endpoint.getName()
						+ " has no secret configured — abandoning delivery " + deliveryId);
				delivery.markAbandoned();
				return false;
			}

			String previousSecret = endpoint.getPreviousSecret();
			Timestamp previousExpiry = endpoint.getPreviousSecretExpiry();
			if (previousSecret != null && previousExpiry != null
					&& previousExpiry.before(new Timestamp(System.currentTimeMillis()))) {
				previousSecret = null; // Expired
			}

			long timestamp = Instant.now().getEpochSecond();
			String signature;
			try {
				signature = WebhookSignature.signWithRotation(
						secret, previousSecret, msgId, timestamp, payload);
			} catch (IllegalArgumentException e) {
				log.severe("Invalid webhook secret for endpoint " + endpoint.getName()
						+ ": " + e.getMessage() + " — abandoning delivery " + deliveryId);
				delivery.markAbandoned();
				return false;
			}

			request = HttpRequest.newBuilder()
					.uri(URI.create(endpoint.getURL()))
					.timeout(Duration.ofMillis(timeoutMs))
					.header("Content-Type", "application/json")
					.header("webhook-id", msgId)
					.header("webhook-timestamp", String.valueOf(timestamp))
					.header("webhook-signature", signature)
					.POST(HttpRequest.BodyPublishers.ofString(payload))
					.build();
		} else {
			// Raw mode: plain POST, no signing, no webhook-* headers
			request = HttpRequest.newBuilder()
					.uri(URI.create(endpoint.getURL()))
					.timeout(Duration.ofMillis(timeoutMs))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(payload))
					.build();
		}

		try {

			HttpResponse<String> response = httpClient.send(request,
					HttpResponse.BodyHandlers.ofString());

			int httpStatus = response.statusCode();
			String responseBody = response.body();

			if (httpStatus >= 200 && httpStatus < 300) {
				// Success
				delivery.markSuccess(httpStatus, responseBody);
				endpoint.recordSuccess();
				return true;
			} else if (httpStatus == 410) {
				// Gone -- permanently disable endpoint
				log.warning("Webhook endpoint returned 410 Gone, disabling: " + endpoint.getName());
				delivery.markAbandoned();
				endpoint.setIsPaused(true);
				endpoint.saveEx();
				return false;
			} else {
				// Failure -- schedule retry
				delivery.markFailed(httpStatus, responseBody,
						"HTTP " + httpStatus);
				endpoint.incrementFailure();
				return false;
			}
		} catch (Exception e) {
			// Network error -- schedule retry
			delivery.markFailed(0, null, e.getClass().getSimpleName() + ": " + e.getMessage());
			endpoint.incrementFailure();
			return false;
		}
	}
}

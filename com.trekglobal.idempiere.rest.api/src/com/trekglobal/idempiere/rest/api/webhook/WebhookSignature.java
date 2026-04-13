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

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Standard Webhooks HMAC-SHA256 signing and verification utility.
 *
 * Implements the Standard Webhooks specification:
 * - Signing content: "msgId.timestamp.body"
 * - Signature format: "v1,&lt;base64&gt;"
 * - Secret format: "whsec_&lt;base64(key)&gt;"
 * - Multiple signatures space-delimited for key rotation
 *
 * @author muriloht Murilo H. Torquato &lt;murilo@muriloht.com&gt;
 * @see <a href="https://www.standardwebhooks.com">Standard Webhooks</a>
 * @see <a href="https://github.com/standard-webhooks/standard-webhooks/blob/main/spec/standard-webhooks.md">Standard Webhooks Spec</a>
 */
public class WebhookSignature {

	public static final String SECRET_PREFIX = "whsec_";
	public static final String SIGNATURE_PREFIX = "v1,";
	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final int MIN_KEY_BYTES = 24;
	private static final int DEFAULT_KEY_BYTES = 32;
	private static final long DEFAULT_TOLERANCE_SECONDS = 300; // 5 minutes

	private WebhookSignature() {
	}

	/**
	 * Generate a new webhook secret in Standard Webhooks format.
	 * @return secret string with whsec_ prefix (e.g., "whsec_MfKQ9r8GKYqr...")
	 */
	public static String generateSecret() {
		byte[] key = new byte[DEFAULT_KEY_BYTES];
		new SecureRandom().nextBytes(key);
		return SECRET_PREFIX + Base64.getEncoder().encodeToString(key);
	}

	/**
	 * Sign a webhook payload per Standard Webhooks spec.
	 *
	 * @param secret the webhook secret (whsec_ prefixed)
	 * @param msgId the webhook message ID (msg_XXX format)
	 * @param timestamp unix timestamp in seconds
	 * @param body the raw JSON body
	 * @return signature string in "v1,<base64>" format
	 * @throws IllegalArgumentException if secret format is invalid
	 */
	public static String sign(String secret, String msgId, long timestamp, String body) {
		byte[] key = decodeSecret(secret);
		String content = msgId + "." + timestamp + "." + body;
		byte[] signature = hmacSHA256(key, content);
		return SIGNATURE_PREFIX + Base64.getEncoder().encodeToString(signature);
	}

	/**
	 * Verify a webhook signature per Standard Webhooks spec.
	 *
	 * @param secret the webhook secret (whsec_ prefixed)
	 * @param msgId value of webhook-id header
	 * @param timestampStr value of webhook-timestamp header
	 * @param signatureHeader value of webhook-signature header (space-delimited signatures)
	 * @param body the raw request body
	 * @return true if any v1 signature matches
	 * @throws WebhookVerificationException if timestamp is outside tolerance or signature invalid
	 */
	public static boolean verify(String secret, String msgId, String timestampStr,
			String signatureHeader, String body) {
		return verify(secret, msgId, timestampStr, signatureHeader, body, DEFAULT_TOLERANCE_SECONDS);
	}

	/**
	 * Verify a webhook signature with custom tolerance.
	 *
	 * @param secret the webhook secret (whsec_ prefixed)
	 * @param msgId value of webhook-id header
	 * @param timestampStr value of webhook-timestamp header
	 * @param signatureHeader value of webhook-signature header
	 * @param body the raw request body
	 * @param toleranceSeconds max age of timestamp in seconds (0 to skip check)
	 * @return true if signature is valid
	 * @throws WebhookVerificationException on verification failure
	 */
	public static boolean verify(String secret, String msgId, String timestampStr,
			String signatureHeader, String body, long toleranceSeconds) {
		if (msgId == null || timestampStr == null || signatureHeader == null || body == null) {
			throw new WebhookVerificationException("Missing required webhook headers");
		}

		long timestamp;
		try {
			timestamp = Long.parseLong(timestampStr);
		} catch (NumberFormatException e) {
			throw new WebhookVerificationException("Invalid webhook-timestamp: " + timestampStr);
		}

		// Replay protection: reject stale timestamps
		if (toleranceSeconds > 0) {
			long now = System.currentTimeMillis() / 1000;
			if (Math.abs(now - timestamp) > toleranceSeconds) {
				throw new WebhookVerificationException(
						"Webhook timestamp too old or too far in the future (tolerance: " + toleranceSeconds + "s)");
			}
		}

		String expectedSignature = sign(secret, msgId, timestamp, body);
		byte[] expectedBytes = Base64.getDecoder().decode(
				expectedSignature.substring(SIGNATURE_PREFIX.length()));

		// Check each space-delimited signature in the header
		String[] signatures = signatureHeader.split(" ");
		for (String sig : signatures) {
			sig = sig.trim();
			if (!sig.startsWith(SIGNATURE_PREFIX)) {
				continue; // Skip non-v1 signatures
			}
			try {
				byte[] actualBytes = Base64.getDecoder().decode(sig.substring(SIGNATURE_PREFIX.length()));
				if (MessageDigest.isEqual(expectedBytes, actualBytes)) {
					return true; // Constant-time comparison
				}
			} catch (IllegalArgumentException e) {
				// Invalid base64, skip this signature
			}
		}

		throw new WebhookVerificationException("No matching webhook signature found");
	}

	/**
	 * Decode a whsec_-prefixed secret into raw key bytes.
	 */
	static byte[] decodeSecret(String secret) {
		if (secret == null || !secret.startsWith(SECRET_PREFIX)) {
			throw new IllegalArgumentException(
					"Webhook secret must start with '" + SECRET_PREFIX + "' prefix");
		}
		byte[] key = Base64.getDecoder().decode(secret.substring(SECRET_PREFIX.length()));
		if (key.length < MIN_KEY_BYTES) {
			throw new IllegalArgumentException(
					"Webhook secret key must be at least " + MIN_KEY_BYTES + " bytes");
		}
		return key;
	}

	/**
	 * Compute HMAC-SHA256.
	 */
	private static byte[] hmacSHA256(byte[] key, String content) {
		try {
			Mac mac = Mac.getInstance(HMAC_SHA256);
			mac.init(new SecretKeySpec(key, HMAC_SHA256));
			return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new RuntimeException("HMAC-SHA256 computation failed", e);
		}
	}

	/**
	 * Build the webhook-signature header value, optionally with dual signatures
	 * for secret rotation.
	 *
	 * @param currentSecret current webhook secret
	 * @param previousSecret previous webhook secret (nullable)
	 * @param msgId message ID
	 * @param timestamp unix timestamp
	 * @param body payload
	 * @return signature header value (space-delimited if two secrets)
	 */
	public static String signWithRotation(String currentSecret, String previousSecret,
			String msgId, long timestamp, String body) {
		String currentSig = sign(currentSecret, msgId, timestamp, body);
		if (previousSecret == null || previousSecret.isEmpty()) {
			return currentSig;
		}
		String previousSig = sign(previousSecret, msgId, timestamp, body);
		return currentSig + " " + previousSig;
	}

	/**
	 * Exception thrown when webhook signature verification fails.
	 */
	public static class WebhookVerificationException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public WebhookVerificationException(String message) {
			super(message);
		}
	}
}

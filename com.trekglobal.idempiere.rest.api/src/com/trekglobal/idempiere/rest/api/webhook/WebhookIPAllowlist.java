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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Matches a remote IP address against a comma-separated allowlist of IPs
 * and CIDR ranges. Supports IPv4, IPv6 and IPv4-mapped IPv6 normalization.
 *
 * <p>Pure-Java utility; no iDempiere dependencies. All methods are
 * fail-closed: any parsing error returns {@code false} from
 * {@link #isAllowed} or throws {@link IllegalArgumentException} from
 * {@link #validate}.
 */
public final class WebhookIPAllowlist {

	private WebhookIPAllowlist() {
	}

	/**
	 * Check whether {@code remoteAddr} is allowed by the comma-separated
	 * {@code allowedIPs} list. Empty/null list means "allow all".
	 *
	 * @param allowedIPs comma-separated list of IPs and/or CIDR ranges
	 * @param remoteAddr the caller's IP address
	 * @return true if allowed (or no restriction configured)
	 */
	public static boolean isAllowed(String allowedIPs, String remoteAddr) {
		if (allowedIPs == null || allowedIPs.trim().isEmpty())
			return true;
		if (remoteAddr == null || remoteAddr.trim().isEmpty())
			return false;

		for (String entry : allowedIPs.split(",")) {
			String trimmed = entry.trim();
			if (trimmed.isEmpty())
				continue;

			if (trimmed.contains("/")) {
				if (isInCIDR(remoteAddr, trimmed))
					return true;
			} else {
				if (trimmed.equals(remoteAddr))
					return true;
			}
		}
		return false;
	}

	/**
	 * Validate that every entry in {@code allowedIPs} parses as either a
	 * literal IP or a valid CIDR range. Empty/null is considered valid
	 * ("allow all").
	 *
	 * @throws IllegalArgumentException if any entry is malformed
	 */
	public static void validate(String allowedIPs) {
		if (allowedIPs == null || allowedIPs.trim().isEmpty())
			return;

		for (String entry : allowedIPs.split(",")) {
			String trimmed = entry.trim();
			if (trimmed.isEmpty())
				continue;

			if (trimmed.contains("/")) {
				validateCIDR(trimmed);
			} else {
				validateLiteralIP(trimmed);
			}
		}
	}

	private static void validateLiteralIP(String ip) {
		try {
			parseNumericAddress(ip);
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("Invalid IP address: " + ip);
		}
	}

	private static void validateCIDR(String cidr) {
		String[] parts = cidr.split("/");
		if (parts.length != 2)
			throw new IllegalArgumentException("Invalid CIDR (expected ADDRESS/PREFIX): " + cidr);

		InetAddress addr;
		try {
			addr = parseNumericAddress(parts[0].trim());
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException("Invalid CIDR address: " + cidr);
		}

		int prefix;
		try {
			prefix = Integer.parseInt(parts[1].trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid CIDR prefix: " + cidr);
		}

		int maxBits = addr.getAddress().length * 8;
		if (prefix < 0 || prefix > maxBits)
			throw new IllegalArgumentException(
					"CIDR prefix out of range [0, " + maxBits + "]: " + cidr);
	}

	/**
	 * Test whether {@code ip} falls within {@code cidr}. Returns false on
	 * any parsing error so this is safe to call on user input.
	 *
	 * <p>Visible for testing.
	 */
	static boolean isInCIDR(String ip, String cidr) {
		try {
			String[] parts = cidr.split("/");
			if (parts.length != 2)
				return false;

			byte[] cidrBytes = parseNumericAddress(parts[0].trim()).getAddress();
			byte[] remoteBytes = normalize(parseNumericAddress(ip.trim()));

			int prefixLength = Integer.parseInt(parts[1].trim());

			if (cidrBytes.length != remoteBytes.length)
				return false;
			if (prefixLength < 0 || prefixLength > cidrBytes.length * 8)
				return false;

			int fullBytes = prefixLength / 8;
			int remainingBits = prefixLength % 8;

			for (int i = 0; i < fullBytes; i++) {
				if (cidrBytes[i] != remoteBytes[i])
					return false;
			}

			if (remainingBits > 0) {
				int mask = (0xFF << (8 - remainingBits)) & 0xFF;
				int cidrByte = cidrBytes[fullBytes] & 0xFF;
				int remoteByte = remoteBytes[fullBytes] & 0xFF;
				if ((cidrByte & mask) != (remoteByte & mask))
					return false;
			}

			return true;
		} catch (UnknownHostException | NumberFormatException e) {
			return false;
		}
	}

	/**
	 * If {@code addr} is an IPv4-mapped IPv6 address (::ffff:a.b.c.d),
	 * unwrap it to the underlying 4-byte IPv4 form so it can match
	 * IPv4 rules. Otherwise return the raw bytes.
	 */
	private static byte[] normalize(InetAddress addr) {
		byte[] bytes = addr.getAddress();
		if (addr instanceof Inet6Address && isIPv4Mapped(bytes)) {
			return Arrays.copyOfRange(bytes, 12, 16);
		}
		return bytes;
	}

	/** Detect ::ffff:0:0/96 prefix (IPv4-mapped IPv6). */
	private static boolean isIPv4Mapped(byte[] bytes) {
		if (bytes.length != 16)
			return false;
		for (int i = 0; i < 10; i++) {
			if (bytes[i] != 0)
				return false;
		}
		return bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF;
	}

	/**
	 * Parse a numeric IPv4 or IPv6 string. Rejects hostnames by checking
	 * the input contains only IP-literal characters before delegating to
	 * {@link InetAddress#getByName}, avoiding accidental DNS lookups.
	 */
	private static InetAddress parseNumericAddress(String s) throws UnknownHostException {
		if (s == null || s.isEmpty())
			throw new UnknownHostException("empty");
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			boolean ok = (c >= '0' && c <= '9')
					|| (c >= 'a' && c <= 'f')
					|| (c >= 'A' && c <= 'F')
					|| c == '.' || c == ':' || c == '%';
			if (!ok)
				throw new UnknownHostException("not a numeric IP literal: " + s);
		}
		return InetAddress.getByName(s);
	}
}

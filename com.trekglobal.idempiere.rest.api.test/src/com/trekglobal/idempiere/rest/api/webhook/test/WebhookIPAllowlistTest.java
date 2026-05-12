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
package com.trekglobal.idempiere.rest.api.webhook.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import com.trekglobal.idempiere.rest.api.json.test.RestTestCase;
import com.trekglobal.idempiere.rest.api.webhook.WebhookIPAllowlist;

public class WebhookIPAllowlistTest extends RestTestCase {

	// -------- isAllowed: allowlist empty/null --------

	@Test
	public void emptyAllowlistAllowsAll() {
		assertTrue(WebhookIPAllowlist.isAllowed(null, "1.2.3.4"));
		assertTrue(WebhookIPAllowlist.isAllowed("", "1.2.3.4"));
		assertTrue(WebhookIPAllowlist.isAllowed("   ", "1.2.3.4"));
	}

	@Test
	public void emptyRemoteAddressIsRejectedWhenAllowlistConfigured() {
		assertFalse(WebhookIPAllowlist.isAllowed("10.0.0.0/8", null));
		assertFalse(WebhookIPAllowlist.isAllowed("10.0.0.0/8", ""));
		assertFalse(WebhookIPAllowlist.isAllowed("10.0.0.0/8", "  "));
	}

	// -------- isAllowed: exact IPv4 match --------

	@Test
	public void exactIPv4Match() {
		assertTrue(WebhookIPAllowlist.isAllowed("1.2.3.4", "1.2.3.4"));
		assertFalse(WebhookIPAllowlist.isAllowed("1.2.3.4", "1.2.3.5"));
	}

	@Test
	public void exactIPv6Match() {
		assertTrue(WebhookIPAllowlist.isAllowed("2001:db8::1", "2001:db8::1"));
		assertFalse(WebhookIPAllowlist.isAllowed("2001:db8::1", "2001:db8::2"));
	}

	@Test
	public void multipleEntriesCommaSeparated() {
		String list = "1.1.1.1, 2.2.2.2, 10.0.0.0/8";
		assertTrue(WebhookIPAllowlist.isAllowed(list, "1.1.1.1"));
		assertTrue(WebhookIPAllowlist.isAllowed(list, "2.2.2.2"));
		assertTrue(WebhookIPAllowlist.isAllowed(list, "10.5.6.7"));
		assertFalse(WebhookIPAllowlist.isAllowed(list, "3.3.3.3"));
	}

	// -------- isAllowed: CIDR IPv4 --------

	@Test
	public void cidrIPv4ByteAlignedPrefix() {
		assertTrue(WebhookIPAllowlist.isAllowed("10.0.0.0/8", "10.0.0.1"));
		assertTrue(WebhookIPAllowlist.isAllowed("10.0.0.0/8", "10.255.255.255"));
		assertFalse(WebhookIPAllowlist.isAllowed("10.0.0.0/8", "11.0.0.1"));

		assertTrue(WebhookIPAllowlist.isAllowed("192.168.1.0/24", "192.168.1.42"));
		assertFalse(WebhookIPAllowlist.isAllowed("192.168.1.0/24", "192.168.2.42"));
	}

	@Test
	public void cidrIPv4UnalignedPrefix() {
		// /20 = 255.255.240.0 mask, range 10.0.0.0–10.0.15.255
		assertTrue(WebhookIPAllowlist.isAllowed("10.0.0.0/20", "10.0.15.255"));
		assertFalse(WebhookIPAllowlist.isAllowed("10.0.0.0/20", "10.0.16.0"));

		// /4 — high nibble compare; 192 = 1100xxxx, range 192.0.0.0–207.255.255.255
		assertTrue(WebhookIPAllowlist.isAllowed("192.0.0.0/4", "192.168.1.1"));
		assertTrue(WebhookIPAllowlist.isAllowed("192.0.0.0/4", "207.255.255.255"));
		assertFalse(WebhookIPAllowlist.isAllowed("192.0.0.0/4", "208.0.0.0"));
	}

	@Test
	public void cidrIPv4HighBitsExerciseSignedByteHandling() {
		// Both sides have the high bit set — used to be a sign-extension trap
		assertTrue(WebhookIPAllowlist.isAllowed("200.0.0.0/4", "207.1.2.3"));
		assertFalse(WebhookIPAllowlist.isAllowed("200.0.0.0/4", "224.0.0.0"));

		assertTrue(WebhookIPAllowlist.isAllowed("128.0.0.0/1", "200.0.0.0"));
		assertTrue(WebhookIPAllowlist.isAllowed("128.0.0.0/1", "255.255.255.255"));
		assertFalse(WebhookIPAllowlist.isAllowed("128.0.0.0/1", "127.0.0.0"));
	}

	@Test
	public void cidrPrefixZeroMatchesEverythingInFamily() {
		assertTrue(WebhookIPAllowlist.isAllowed("0.0.0.0/0", "1.2.3.4"));
		assertTrue(WebhookIPAllowlist.isAllowed("0.0.0.0/0", "255.255.255.255"));
		// /0 in IPv4 should not match an IPv6 caller (different family)
		assertFalse(WebhookIPAllowlist.isAllowed("0.0.0.0/0", "2001:db8::1"));
	}

	@Test
	public void cidrPrefixMaxIsExactMatch() {
		assertTrue(WebhookIPAllowlist.isAllowed("1.2.3.4/32", "1.2.3.4"));
		assertFalse(WebhookIPAllowlist.isAllowed("1.2.3.4/32", "1.2.3.5"));
	}

	// -------- isAllowed: CIDR IPv6 --------

	@Test
	public void cidrIPv6() {
		assertTrue(WebhookIPAllowlist.isAllowed("2001:db8::/32", "2001:db8::1"));
		assertTrue(WebhookIPAllowlist.isAllowed("2001:db8::/32", "2001:db8:ffff::1"));
		assertFalse(WebhookIPAllowlist.isAllowed("2001:db8::/32", "2001:db9::1"));

		assertTrue(WebhookIPAllowlist.isAllowed("::/0", "2001:db8::1"));
		assertTrue(WebhookIPAllowlist.isAllowed("fe80::/10", "fe80::1"));
		assertFalse(WebhookIPAllowlist.isAllowed("fe80::/10", "fec0::1"));
	}

	@Test
	public void ipv4MappedIPv6IsNormalizedToIPv4() {
		// ::ffff:10.0.0.1 should match a 10.0.0.0/8 rule
		assertTrue(WebhookIPAllowlist.isAllowed("10.0.0.0/8", "::ffff:10.0.0.1"));
		assertTrue(WebhookIPAllowlist.isAllowed("1.2.3.4", "::ffff:1.2.3.4"));
		assertFalse(WebhookIPAllowlist.isAllowed("10.0.0.0/8", "::ffff:11.0.0.1"));
	}

	@Test
	public void ipv4RuleDoesNotMatchUnmappedIPv6() {
		assertFalse(WebhookIPAllowlist.isAllowed("10.0.0.0/8", "2001:db8::1"));
		assertFalse(WebhookIPAllowlist.isAllowed("1.2.3.4", "2001:db8::1"));
	}

	// -------- isAllowed: malformed inputs are fail-closed --------

	@Test
	public void malformedRulesAreSkipped() {
		// Single garbage rule with no match → false
		assertFalse(WebhookIPAllowlist.isAllowed("not-an-ip", "1.2.3.4"));
		assertFalse(WebhookIPAllowlist.isAllowed("1.2.3.4/abc", "1.2.3.4"));
		assertFalse(WebhookIPAllowlist.isAllowed("1.2.3.4/-1", "1.2.3.4"));
		assertFalse(WebhookIPAllowlist.isAllowed("1.2.3.4/40", "1.2.3.4"));
		assertFalse(WebhookIPAllowlist.isAllowed("1.2.3.4/8/8", "1.2.3.4"));

		// Garbage rule alongside a valid one → still matches the valid one
		assertTrue(WebhookIPAllowlist.isAllowed("not-an-ip, 1.2.3.4", "1.2.3.4"));
	}

	@Test
	public void hostnamesInRulesDoNotTriggerDNSAndDoNotMatch() {
		// parseNumericAddress rejects non-numeric input → fail closed
		assertFalse(WebhookIPAllowlist.isAllowed("example.com", "1.2.3.4"));
		assertFalse(WebhookIPAllowlist.isAllowed("example.com/24", "1.2.3.4"));
	}

	// -------- validate: accepts valid configurations --------

	@Test
	public void validateAcceptsEmpty() {
		assertDoesNotThrow(() -> WebhookIPAllowlist.validate(null));
		assertDoesNotThrow(() -> WebhookIPAllowlist.validate(""));
		assertDoesNotThrow(() -> WebhookIPAllowlist.validate("   "));
	}

	@Test
	public void validateAcceptsLiteralIPs() {
		assertDoesNotThrow(() -> WebhookIPAllowlist.validate("1.2.3.4"));
		assertDoesNotThrow(() -> WebhookIPAllowlist.validate("2001:db8::1"));
		assertDoesNotThrow(() -> WebhookIPAllowlist.validate("1.2.3.4, 5.6.7.8"));
	}

	@Test
	public void validateAcceptsCIDRs() {
		assertDoesNotThrow(() -> WebhookIPAllowlist.validate("10.0.0.0/8"));
		assertDoesNotThrow(() -> WebhookIPAllowlist.validate("0.0.0.0/0"));
		assertDoesNotThrow(() -> WebhookIPAllowlist.validate("1.2.3.4/32"));
		assertDoesNotThrow(() -> WebhookIPAllowlist.validate("2001:db8::/32"));
		assertDoesNotThrow(() -> WebhookIPAllowlist.validate("::/0"));
		assertDoesNotThrow(() -> WebhookIPAllowlist.validate("::1/128"));
	}

	@Test
	public void validateAcceptsMixedList() {
		assertDoesNotThrow(() -> WebhookIPAllowlist.validate(
				"1.2.3.4, 10.0.0.0/8, 2001:db8::/32, ::1"));
	}

	// -------- validate: rejects malformed configurations --------

	@Test
	public void validateRejectsHostname() {
		assertThrows(IllegalArgumentException.class,
				() -> WebhookIPAllowlist.validate("example.com"));
		assertThrows(IllegalArgumentException.class,
				() -> WebhookIPAllowlist.validate("example.com/24"));
	}

	@Test
	public void validateRejectsBadCIDRPrefix() {
		assertThrows(IllegalArgumentException.class,
				() -> WebhookIPAllowlist.validate("1.2.3.4/abc"));
		assertThrows(IllegalArgumentException.class,
				() -> WebhookIPAllowlist.validate("1.2.3.4/-1"));
		assertThrows(IllegalArgumentException.class,
				() -> WebhookIPAllowlist.validate("1.2.3.4/33"));
		assertThrows(IllegalArgumentException.class,
				() -> WebhookIPAllowlist.validate("2001:db8::/129"));
	}

	@Test
	public void validateRejectsMalformedCIDR() {
		assertThrows(IllegalArgumentException.class,
				() -> WebhookIPAllowlist.validate("1.2.3.4/8/8"));
		assertThrows(IllegalArgumentException.class,
				() -> WebhookIPAllowlist.validate("/24"));
	}

	@Test
	public void validateRejectsAnyEntryEvenIfOthersValid() {
		// One bad entry should fail the whole list
		assertThrows(IllegalArgumentException.class,
				() -> WebhookIPAllowlist.validate("1.2.3.4, not-an-ip"));
	}
}

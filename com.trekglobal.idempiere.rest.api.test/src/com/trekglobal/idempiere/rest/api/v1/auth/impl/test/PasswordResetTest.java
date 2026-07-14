/******************************************************************************
 * Project: Trek Global ERP                                                   *
 * Copyright (C) Trek Global Corporation                			          *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *                                                                            *
 * Contributors:                                                              *
 * - Diego Ruiz                                                               *
 *****************************************************************************/
package com.trekglobal.idempiere.rest.api.v1.auth.impl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.compiere.model.MPasswordResetToken;
import org.compiere.model.X_AD_PasswordResetToken;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.trekglobal.idempiere.rest.api.json.test.RestTestCase;
import com.trekglobal.idempiere.rest.api.v1.auth.PasswordResetCompletion;
import com.trekglobal.idempiere.rest.api.v1.auth.PasswordResetRequest;
import com.trekglobal.idempiere.rest.api.v1.auth.PasswordResetVerification;
import com.trekglobal.idempiere.rest.api.v1.auth.impl.AuthServiceImpl;

/**
 * Integration tests for the code-based password-reset REST endpoints on
 * {@link AuthServiceImpl} (IDEMPIERE-7060). Focus is on endpoint wiring and HTTP
 * status/body mapping - the deep flow and the anti-enumeration guarantees are
 * covered at the service level by {@code org.idempiere.test.base.PasswordResetTest}.
 *
 * @author Diego Ruiz
 */
public class PasswordResetTest extends RestTestCase {

	@Mock
	private HttpServletRequest request;
	@InjectMocks
	private AuthServiceImpl authService;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		authService = new AuthServiceImpl();
		when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");
		authService.setRequest(request);
	}

	private static String uniqueEmail(String tag) {
		return "pwreset-rest-" + tag + "-" + System.nanoTime() + "@example.com";
	}

	private static int countTokens(String email) {
		return DB.getSQLValueEx(null, "SELECT COUNT(*) FROM AD_PasswordResetToken WHERE EMail=?", email);
	}

	@Test
	void requestWithMissingEmailReturnsBadRequest() {
		// null body
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
				authService.requestPasswordReset(null).getStatus(), "null body must be rejected");
		// null email
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
				authService.requestPasswordReset(new PasswordResetRequest()).getStatus(),
				"missing email must be rejected");
		// blank email (no token may be created for a structurally invalid request)
		String blank = "   ";
		PasswordResetRequest body = new PasswordResetRequest();
		body.setEmail(blank);
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
				authService.requestPasswordReset(body).getStatus(), "blank email must be rejected");
		assertEquals(0, countTokens(blank), "a rejected request must create no token row");
	}

	@Test
	void verifyWithMissingFieldsReturnsBadRequest() {
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
				authService.verifyPasswordResetCode(null).getStatus(), "null body must be rejected");

		PasswordResetVerification noCode = new PasswordResetVerification();
		noCode.setEmail(uniqueEmail("nocode"));
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
				authService.verifyPasswordResetCode(noCode).getStatus(), "missing code must be rejected");

		PasswordResetVerification noEmail = new PasswordResetVerification();
		noEmail.setCode("000000");
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
				authService.verifyPasswordResetCode(noEmail).getStatus(), "missing email must be rejected");
	}

	@Test
	void completeWithMissingFieldsReturnsBadRequest() {
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
				authService.completePasswordReset(null).getStatus(), "null body must be rejected");

		PasswordResetCompletion noToken = new PasswordResetCompletion();
		noToken.setNewPassword("SomeNewPassw0rd!");
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
				authService.completePasswordReset(noToken).getStatus(), "missing verified token must be rejected");

		PasswordResetCompletion noPassword = new PasswordResetCompletion();
		noPassword.setVerifiedToken("some-token");
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
				authService.completePasswordReset(noPassword).getStatus(), "missing new password must be rejected");
	}

	@Test
	void requestForUnknownEmailReturnsNeutralOkAndCreatesNoToken() {
		String email = uniqueEmail("neutral");
		PasswordResetRequest body = new PasswordResetRequest();
		body.setEmail(email);
		body.setLanguage("en_US");

		Response response = authService.requestPasswordReset(body);

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(),
				"an unknown email must get the same neutral 200 as a registered one");
		assertEquals(0, countTokens(email), "no reset token row may be created for an unknown email");
	}

	@Test
	void requestTwiceRapidlyIsRateLimited() {
		String email = uniqueEmail("ratelimit");
		PasswordResetRequest body = new PasswordResetRequest();
		body.setEmail(email);

		// first request -> neutral 200 (records a virtual request in memory)
		assertEquals(Response.Status.OK.getStatusCode(), authService.requestPasswordReset(body).getStatus());
		// second request within the cooldown -> 429, exactly like a registered email would be throttled
		Response second = authService.requestPasswordReset(body);
		assertEquals(429, second.getStatus(), "a second rapid request must be rate-limited (429)");
		assertEquals(0, countTokens(email), "rate-limited unknown email must still create no token row");
	}

	@Test
	void verifyUnknownEmailWrongCodeAlwaysReturnsBadRequest() {
		String email = uniqueEmail("verify");
		String invalid = Msg.getMsg(Env.getCtx(), "PasswordResetInvalidCode");
		PasswordResetVerification body = new PasswordResetVerification();
		body.setEmail(email);
		body.setCode("000000");

		// first wrong attempt -> 400 with the invalid-code message
		Response first = authService.verifyPasswordResetCode(body);
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), first.getStatus());
		assertTrue(first.getEntity().toString().contains(invalid),
				"verify failure should carry the invalid-code message");

		// further attempts past the lockout must keep the SAME status (400): the status never
		// reveals lockout timing, so it cannot be used to tell registered from unknown emails
		for (int i = 0; i < 8; i++) {
			assertEquals(Response.Status.BAD_REQUEST.getStatusCode(),
					authService.verifyPasswordResetCode(body).getStatus(),
					"every verify failure must map to 400 regardless of attempt count");
		}
	}

	@Test
	void completeWithInvalidTokenReturnsBadRequest() {
		PasswordResetCompletion body = new PasswordResetCompletion();
		body.setVerifiedToken("not-a-real-verified-token");
		body.setNewPassword("SomeNewPassw0rd!");

		Response response = authService.completePasswordReset(body);
		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus(),
				"an unknown verified token must be rejected with 400");
	}

	@Test
	void verifyWithMatchingCodeReturnsVerifiedToken() {
		String email = uniqueEmail("happy");
		String code = "135790";
		// a pending token is all verifyCode needs (it does not look up a user); seed one directly
		MPasswordResetToken token = new MPasswordResetToken(Env.getCtx(), 0, null);
		token.setEMail(email);
		token.setOneTimeCode(code); // stored encrypted, decrypted on read for the compare
		token.setTokenStatus(X_AD_PasswordResetToken.TOKENSTATUS_Pending);
		token.setAttemptsUsed(0);
		token.setExpiration(new Timestamp(System.currentTimeMillis() + 600000L));
		token.saveEx();
		try {
			PasswordResetVerification body = new PasswordResetVerification();
			body.setEmail(email);
			body.setCode(code);

			Response response = authService.verifyPasswordResetCode(body);

			assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(),
					"a correct code must verify");
			assertTrue(response.getEntity().toString().contains("verifiedToken"),
					"a successful verify must return a verifiedToken");
		} finally {
			DB.executeUpdateEx("DELETE FROM AD_PasswordResetToken WHERE EMail=?", new Object[] { email }, null);
		}
	}

}

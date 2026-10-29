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
* - Heng Sin Low                                                      *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.v1.auth.impl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.compiere.dbPort.Convert;
import org.compiere.model.I_AD_UserPreference;
import org.compiere.model.MRole;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.idempiere.test.DictionaryIDs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trekglobal.idempiere.rest.api.json.test.RestTestCase;
import com.trekglobal.idempiere.rest.api.util.MigrationScriptContext;
import com.trekglobal.idempiere.rest.api.v1.auth.LoginCredential;
import com.trekglobal.idempiere.rest.api.v1.auth.LoginParameters;
import com.trekglobal.idempiere.rest.api.v1.auth.filter.RequestFilter;
import com.trekglobal.idempiere.rest.api.v1.auth.filter.ResponseFilter;
import com.trekglobal.idempiere.rest.api.v1.auth.impl.AuthServiceImpl;

public class RequestFilterMigrationHeaderTest extends RestTestCase {

	@Mock
	private HttpServletRequest httpRequest;

	private AuthServiceImpl authService;
	private RequestFilter requestFilter;

	@BeforeEach
	public void setUpMocks() {
		assumeFalse(Ini.isClient(), "Migration script header test requires server mode");
		MockitoAnnotations.openMocks(this);
		authService = new AuthServiceImpl();
		when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");
		authService.setRequest(httpRequest);
		requestFilter = new RequestFilter();
		clearMigrationScriptContext(Env.getCtx());
	}

	@AfterEach
	public void tearDownMigrationScriptContext() {
		Convert.closeLogMigrationScript();
		MigrationScriptContext.clearSessionFileNameCache();
		clearMigrationScriptContext(Env.getCtx());
	}

	private void clearMigrationScriptContext(Properties ctx) {
		Env.setContext(ctx, Ini.P_LOGMIGRATIONSCRIPT, (String) null);
		Env.setContext(ctx, "P|" + Ini.P_LOGMIGRATIONSCRIPT, (String) null);
		Env.setContext(ctx, Ini.P_ADEMPIERESYS, (String) null);
		Env.setContext(ctx, "P|" + Ini.P_ADEMPIERESYS, (String) null);
		Env.setContext(ctx, I_AD_UserPreference.COLUMNNAME_MigrationScriptComment, (String) null);
		Env.setContext(ctx, "P|" + I_AD_UserPreference.COLUMNNAME_MigrationScriptComment, (String) null);
		Env.setContext(ctx, MigrationScriptContext.MIGRATION_SCRIPT_FILE_NAME, (String) null);
		Env.setContext(ctx, "P|" + MigrationScriptContext.MIGRATION_SCRIPT_FILE_NAME, (String) null);
	}

	private String createToken(String userName, String password, int clientId, int roleId, int orgId) {
		try (MockedStatic<MRole> mockedRule = mockStatic(MRole.class, Mockito.CALLS_REAL_METHODS)) {
			mockedRule.when(() -> MRole.getWhereRoleType(anyString(), anyString())).thenReturn(null);
			LoginCredential credential = new LoginCredential();
			credential.setUserName(userName);
			credential.setPassword(password);
			LoginParameters params = new LoginParameters();
			params.setClientId(String.valueOf(clientId));
			params.setRoleId(String.valueOf(roleId));
			params.setOrganizationId(String.valueOf(orgId));
			params.setLanguage("en_US");
			credential.setParameters(params);
	
			Response response = authService.authenticate(credential);
			assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
			JsonObject json = JsonParser.parseString(response.getEntity().toString()).getAsJsonObject();
			return json.get("token").getAsString();
		}
	}

	private ContainerRequestContext mockRequestContext(String token, String logMigration, String adempiereSys, String comment) {
		ContainerRequestContext ctx = mock(ContainerRequestContext.class);
		UriInfo uriInfo = mock(UriInfo.class);
		when(uriInfo.getPath()).thenReturn("v1/models/AD_Message");
		when(uriInfo.getPath(true)).thenReturn("v1/models/AD_Message");
		when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<String, String>());
		when(ctx.getUriInfo()).thenReturn(uriInfo);
		when(ctx.getMethod()).thenReturn(HttpMethod.POST);
		when(ctx.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
		when(ctx.getHeaderString(RequestFilter.LOG_MIGRATION_SCRIPT_HEADER)).thenReturn(logMigration);
		when(ctx.getHeaderString(RequestFilter.ADEMPIERE_SYS_HEADER)).thenReturn(adempiereSys);
		when(ctx.getHeaderString(RequestFilter.MIGRATION_SCRIPT_COMMENT_HEADER)).thenReturn(comment);

		Map<String, Object> properties = new HashMap<>();
		doAnswer(invocation -> {
			properties.put(invocation.getArgument(0), invocation.getArgument(1));
			return null;
		}).when(ctx).setProperty(ArgumentMatchers.anyString(), ArgumentMatchers.any());
		doAnswer(invocation -> properties.get(invocation.getArgument(0))).when(ctx).getProperty(ArgumentMatchers.anyString());

		return ctx;
	}

	@Test
	public void testSystemAdminHonorsMigrationHeadersAndIsolatesRequestContext() throws Exception {
		String token = createToken("SuperUser", "System",
				DictionaryIDs.AD_Client.SYSTEM.id, DictionaryIDs.AD_Role.SYSTEM_ADMINISTRATOR.id, DictionaryIDs.AD_Org.GLOBAL.id);

		ContainerRequestContext reqWithHeaders = mockRequestContext(token, "Y", "Y", "IDEMPIERE-1234 Short description");
		requestFilter.filter(reqWithHeaders);
		verify(reqWithHeaders, never()).abortWith(ArgumentMatchers.any());
		verify(reqWithHeaders).setProperty(ArgumentMatchers.anyString(), ArgumentMatchers.eq(Boolean.TRUE));

		assertEquals("Y", Env.getCtx().getProperty(Ini.P_LOGMIGRATIONSCRIPT));
		assertEquals("Y", Env.getContext(Env.getCtx(), "P|" + Ini.P_LOGMIGRATIONSCRIPT));
		assertTrue(Convert.isLogMigrationScript());

		assertEquals("Y", Env.getCtx().getProperty(Ini.P_ADEMPIERESYS));
		assertEquals("Y", Env.getContext(Env.getCtx(), "P|" + Ini.P_ADEMPIERESYS));

		assertEquals("IDEMPIERE-1234 Short description", Env.getContext(Env.getCtx(), I_AD_UserPreference.COLUMNNAME_MigrationScriptComment));
		assertEquals("IDEMPIERE-1234 Short description", Env.getContext(Env.getCtx(), "P|" + I_AD_UserPreference.COLUMNNAME_MigrationScriptComment));

		String fileName = Env.getContext(Env.getCtx(), MigrationScriptContext.MIGRATION_SCRIPT_FILE_NAME);
		assertTrue(fileName.endsWith("_IDEMPIERE-1234.sql"));

		// Subsequent request with the same session token but without migration headers must not inherit Y
		ContainerRequestContext reqWithoutHeaders = mockRequestContext(token, null, null, null);
		requestFilter.filter(reqWithoutHeaders);
		verify(reqWithoutHeaders, never()).abortWith(ArgumentMatchers.any());
		verify(reqWithoutHeaders, never()).setProperty(ArgumentMatchers.anyString(), ArgumentMatchers.any());

		assertFalse(Convert.isLogMigrationScript());
		assertFalse("Y".equalsIgnoreCase(Env.getCtx().getProperty(Ini.P_ADEMPIERESYS)));
	}

	@Test
	public void testGardenWorldClientIgnoresMigrationHeaders() throws Exception {
		String token = createToken("GardenAdmin", "GardenAdmin",
				GARDEN_WORLD_CLIENT, GARDEN_WORLD_ADMIN_ROLE, GARDEN_WORLD_HQ_ORG);

		ContainerRequestContext req = mockRequestContext(token, "Y", "Y", "IDEMPIERE-1234 Should be ignored");
		requestFilter.filter(req);
		verify(req, never()).abortWith(ArgumentMatchers.any());
		verify(req, never()).setProperty(ArgumentMatchers.anyString(), ArgumentMatchers.any());

		assertFalse(Convert.isLogMigrationScript());
		assertFalse("Y".equalsIgnoreCase(Env.getCtx().getProperty(Ini.P_ADEMPIERESYS)));
		assertNotEquals("IDEMPIERE-1234 Should be ignored", Env.getContext(Env.getCtx(), I_AD_UserPreference.COLUMNNAME_MigrationScriptComment));
	}

	@Test
	public void testInvalidValuesAndCommentSanitization() throws Exception {
		String token = createToken("SuperUser", "System",
				DictionaryIDs.AD_Client.SYSTEM.id, DictionaryIDs.AD_Role.SYSTEM_ADMINISTRATOR.id, DictionaryIDs.AD_Org.GLOBAL.id);

		ContainerRequestContext req = mockRequestContext(token, "maybe", "1",
				"IDEMPIERE-1234 first line\r\nsecond\tline");
		requestFilter.filter(req);
		verify(req, never()).abortWith(ArgumentMatchers.any());

		assertEquals("N", Env.getCtx().getProperty(Ini.P_LOGMIGRATIONSCRIPT));
		assertFalse(Convert.isLogMigrationScript());
		assertEquals("Y", Env.getCtx().getProperty(Ini.P_ADEMPIERESYS));
		assertEquals("IDEMPIERE-1234 first line second line",
				Env.getContext(Env.getCtx(), I_AD_UserPreference.COLUMNNAME_MigrationScriptComment));
	}

	@Test
	public void testMigrationScriptFileNameReusedPerSessionAndComment() {
		int sessionId = 1000001;
		String fileName = MigrationScriptContext.getOrCreateMigrationScriptFileName(sessionId, "IDEMPIERE-9876 First change");
		assertTrue(fileName.endsWith("_IDEMPIERE-9876.sql"));
		assertEquals(fileName, MigrationScriptContext.getOrCreateMigrationScriptFileName(sessionId, "IDEMPIERE-9876 Second change"));
		assertNotEquals(fileName, MigrationScriptContext.getOrCreateMigrationScriptFileName(sessionId, "IDEMPIERE-9877 Other change"));

		String noTicketFile = MigrationScriptContext.getOrCreateMigrationScriptFileName(sessionId, "no ticket");
		assertTrue(noTicketFile.endsWith("_PlaceholderForTicket.sql"));
		assertEquals(noTicketFile, MigrationScriptContext.getOrCreateMigrationScriptFileName(sessionId, "no ticket"));

		String nullCommentFile = MigrationScriptContext.getOrCreateMigrationScriptFileName(sessionId, null);
		assertTrue(nullCommentFile.endsWith("_PlaceholderForTicket.sql"));
		assertEquals(nullCommentFile, MigrationScriptContext.getOrCreateMigrationScriptFileName(sessionId, ""));

		MigrationScriptContext.cleanSessionCache(sessionId);
		assertTrue(Convert.getMigrationScriptFileName(null).endsWith("_PlaceholderForTicket.sql"));
	}

	@Test
	public void testMultipleRequestsReuseSameMigrationScriptFile() throws Exception {
		String token = createToken("SuperUser", "System",
				DictionaryIDs.AD_Client.SYSTEM.id, DictionaryIDs.AD_Role.SYSTEM_ADMINISTRATOR.id, DictionaryIDs.AD_Org.GLOBAL.id);

		ResponseFilter responseFilter = new ResponseFilter();

		// Request 1
		ContainerRequestContext req1 = mockRequestContext(token, "Y", "Y", "IDEMPIERE-9999 First Statement");
		requestFilter.filter(req1);
		String fileName1 = Env.getContext(Env.getCtx(), MigrationScriptContext.MIGRATION_SCRIPT_FILE_NAME);
		assertNotNull(fileName1);
		assertTrue(fileName1.endsWith("_IDEMPIERE-9999.sql"));

		Convert.logMigrationScript("INSERT INTO AD_Message (AD_Message_ID, Value, MsgText) VALUES (999901, 'M1', 'T1')",
				"INSERT INTO AD_Message (AD_Message_ID, Value, MsgText) VALUES (999901, 'M1', 'T1')");
		responseFilter.filter(req1, null);

		// Request 2 with same session and ticket
		ContainerRequestContext req2 = mockRequestContext(token, "Y", "Y", "IDEMPIERE-9999 Second Statement");
		requestFilter.filter(req2);
		String fileName2 = Env.getContext(Env.getCtx(), MigrationScriptContext.MIGRATION_SCRIPT_FILE_NAME);
		assertEquals(fileName1, fileName2);

		Convert.logMigrationScript("INSERT INTO AD_Message (AD_Message_ID, Value, MsgText) VALUES (999902, 'M2', 'T2')",
				"INSERT INTO AD_Message (AD_Message_ID, Value, MsgText) VALUES (999902, 'M2', 'T2')");
		responseFilter.filter(req2, null);

		// Verify file on disk
		String folderPg = Convert.getMigrationScriptFolder("postgresql");
		File filePg = new File(folderPg + fileName1);
		try {
			assertTrue(filePg.exists(), "Migration script file should exist: " + filePg.getAbsolutePath());
			String content = Files.readString(filePg.toPath());
			assertTrue(content.contains("999901"), "Should contain first statement");
			assertTrue(content.contains("999902"), "Should contain second statement");
			// Header must only appear once
			int firstIdx = content.indexOf("register_migration_script");
			int lastIdx = content.lastIndexOf("register_migration_script");
			assertTrue(firstIdx >= 0, "Should contain register_migration_script");
			assertEquals(firstIdx, lastIdx, "register_migration_script should only be registered once");
		} finally {
			filePg.delete();
			String folderOr = Convert.getMigrationScriptFolder("oracle");
			File fileOr = new File(folderOr + fileName1);
			if (fileOr.exists())
				fileOr.delete();
		}
	}

	@Test
	public void testCopyMigrationScriptContext() {
		Properties source = new Properties();
		Env.setContext(source, Ini.P_LOGMIGRATIONSCRIPT, "Y");
		Env.setContext(source, "P|" + Ini.P_LOGMIGRATIONSCRIPT, "Y");
		Env.setContext(source, Ini.P_ADEMPIERESYS, "Y");
		Env.setContext(source, "P|" + Ini.P_ADEMPIERESYS, "Y");
		Env.setContext(source, I_AD_UserPreference.COLUMNNAME_MigrationScriptComment, "IDEMPIERE-1234 Copy");
		Env.setContext(source, "P|" + I_AD_UserPreference.COLUMNNAME_MigrationScriptComment, "IDEMPIERE-1234 Copy");
		Env.setContext(source, MigrationScriptContext.MIGRATION_SCRIPT_FILE_NAME, "202610270800_IDEMPIERE-1234.sql");
		Env.setContext(source, "P|" + MigrationScriptContext.MIGRATION_SCRIPT_FILE_NAME, "202610270800_IDEMPIERE-1234.sql");

		Properties target = new Properties();
		MigrationScriptContext.copy(source, target);

		assertEquals("Y", target.getProperty(Ini.P_LOGMIGRATIONSCRIPT));
		assertEquals("Y", target.getProperty("P|" + Ini.P_LOGMIGRATIONSCRIPT));
		assertEquals("Y", target.getProperty(Ini.P_ADEMPIERESYS));
		assertEquals("Y", target.getProperty("P|" + Ini.P_ADEMPIERESYS));
		assertEquals("IDEMPIERE-1234 Copy", target.getProperty(I_AD_UserPreference.COLUMNNAME_MigrationScriptComment));
		assertEquals("IDEMPIERE-1234 Copy", target.getProperty("P|" + I_AD_UserPreference.COLUMNNAME_MigrationScriptComment));
		assertEquals("202610270800_IDEMPIERE-1234.sql", target.getProperty(MigrationScriptContext.MIGRATION_SCRIPT_FILE_NAME));
		assertEquals("202610270800_IDEMPIERE-1234.sql", target.getProperty("P|" + MigrationScriptContext.MIGRATION_SCRIPT_FILE_NAME));
	}
}

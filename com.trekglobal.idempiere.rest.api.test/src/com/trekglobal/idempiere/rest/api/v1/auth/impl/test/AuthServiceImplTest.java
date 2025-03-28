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

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.compiere.util.Env;
import org.idempiere.test.AbstractTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trekglobal.idempiere.rest.api.v1.auth.LoginCredential;
import com.trekglobal.idempiere.rest.api.v1.auth.filter.RequestFilter;
import com.trekglobal.idempiere.rest.api.v1.auth.impl.AuthServiceImpl;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthServiceImplTest extends AbstractTestCase {

	@Mock
	private HttpServletRequest request;

	@InjectMocks
	private AuthServiceImpl authService;

	@BeforeEach
	public void authenticate() {
		MockitoAnnotations.openMocks(this);
		authService = new AuthServiceImpl();
		when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");
		authService.setRequest(request);

		LoginCredential credential = new LoginCredential();
		credential.setUserName("GardenAdmin");
		credential.setPassword("GardenAdmin");
		Response response = authService.authenticate(credential);
		Env.setContext(Env.getCtx(), RequestFilter.LOGIN_NAME, "GardenAdmin");
		String jsonString = response.getEntity().toString();
		JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

		JsonArray clientsArray = jsonObject.getAsJsonArray("clients");

		ArrayList<String> idList = new ArrayList<String>();
		for (int i = 0; i < clientsArray.size(); i++) {
			idList.add(clientsArray.get(i).getAsJsonObject().get("id").getAsString());
		}
		String idListString = String.join(",", idList);
		Env.setContext(Env.getCtx(), RequestFilter.LOGIN_CLIENTS, idListString);	

	}

	@Test
	@Order(1)
	void getRolesWithValidClientIdReturnsRoles() {
		int clientId = 11;

		Response response = authService.getRoles(clientId);

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity().toString().contains("roles"));
	}

	@Test
	@Order(2)
	void getRolesWithInvalidClientIdReturnsUnauthorized() {
		int clientId = -1;
		Response response = authService.getRoles(clientId);

		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
	}

	@Test
	void getOrganizationsWithValidClientIdAndRoleIdReturnsOrganizations() {
		int clientId = 11;
		int roleId = 102;

		Response response = authService.getOrganizations(clientId, roleId);

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity().toString().contains("organizations"));
	}

	@Test
	void getOrganizationsWithInvalidClientIdOrRoleIdReturnsUnauthorized() {
		int clientId = -1;
		int roleId = 200;

		Response response = authService.getOrganizations(clientId, roleId);

		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
	}

	@Test
	void getWarehousesWithValidClientIdRoleIdAndOrganizationIdReturnsWarehouses() {
		int clientId = 11; //GardenWorld
		int roleId = 102; //GardenAdmin
		int organizationId = 11; //HQ

		Response response = authService.getWarehouses(clientId, roleId, organizationId);

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity().toString().contains("warehouses"));
	}

	@Test
	void getWarehousesWithInvalidClientIdRoleIdOrOrganizationIdReturnsUnauthorized() {
		int clientId = -1;
		int roleId = 200;
		int organizationId = 300;

		Response response = authService.getWarehouses(clientId, roleId, organizationId);

		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
	}

	@Test
	void getClientLanguageWithValidClientIdReturnsLanguage() {
		int clientId = 11;

		Response response = authService.getClientLanguage(clientId);

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity().toString().contains("AD_Language"));
	}

	@Test
	void getClientLanguageWithInvalidClientIdReturnsBadRequest() {
		int clientId = -1;
		Response response = authService.getClientLanguage(clientId);

		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}

}
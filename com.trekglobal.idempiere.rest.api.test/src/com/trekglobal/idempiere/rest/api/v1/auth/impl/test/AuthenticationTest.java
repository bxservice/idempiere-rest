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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.compiere.model.MRoleOrgAccess;
import org.compiere.util.Env;
import org.idempiere.test.AbstractTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.trekglobal.idempiere.rest.api.v1.auth.LoginCredential;
import com.trekglobal.idempiere.rest.api.v1.auth.impl.AuthServiceImpl;

public class AuthenticationTest extends AbstractTestCase {

	@Mock
	private HttpServletRequest request;
	@InjectMocks
	private AuthServiceImpl authService;


	@BeforeEach
	public void setUp() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		MockitoAnnotations.openMocks(this);
		authService = new AuthServiceImpl();

		when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");
		authService.setRequest(request);
	}

	@Test
	void authenticateWithValidCredentialsReturnsOkResponse() {
		LoginCredential credential = new LoginCredential();
		credential.setUserName("GardenAdmin");
		credential.setPassword("GardenAdmin");

		Response response = authService.authenticate(credential);
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	}

	@Test
	void authenticateWithMultipleClientsReturnsClientList() {
		LoginCredential credential = new LoginCredential();
		credential.setUserName("SuperUser");
		credential.setPassword("System");

		Response response = authService.authenticate(credential);

		assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity().toString().contains("clients"));
	}
	
	@Test
	void authenticateWithSingleClientRoleAndOrgSetsLoginParameters() {
	    MRoleOrgAccess[] roleOrgAccess = MRoleOrgAccess.getOfRole(Env.getCtx(), 103); // GardenUser Role
	    
	    try {
	        // Deactivate all except the first one
	        for (int i = 1; i < roleOrgAccess.length; i++) {
	            roleOrgAccess[i].setIsActive(false);
	            roleOrgAccess[i].saveEx();
	        }

	        // Setup credentials
	        LoginCredential credential = new LoginCredential();
	        credential.setUserName("GardenUser");
	        credential.setPassword("GardenUser");

	        // Authenticate
	        Response response = authService.authenticate(credential);

	        // Validate response
	        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
	        assertTrue(response.getEntity().toString().contains("clientId"));
	        assertTrue(response.getEntity().toString().contains("roleId"));

	    } finally {
	    	
	        // Deactivate all except the first one
	        for (int i = 1; i < roleOrgAccess.length; i++) {
	            roleOrgAccess[i].setIsActive(true);
	            roleOrgAccess[i].saveEx();
	        }
	    }
	}

	@Test
	void authenticateWithInvalidCredentialsReturnsUnauthorizedResponse() {
		LoginCredential credential = new LoginCredential();
		credential.setUserName("invalidUser");
		credential.setPassword("invalidPassword");

		Response response = authService.authenticate(credential);
		assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
	}

}
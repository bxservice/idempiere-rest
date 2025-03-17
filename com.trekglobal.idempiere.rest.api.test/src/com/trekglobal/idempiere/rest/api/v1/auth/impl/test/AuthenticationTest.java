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

import javax.ws.rs.core.Response;

import org.idempiere.test.AbstractTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.trekglobal.idempiere.rest.api.v1.auth.AuthService;
import com.trekglobal.idempiere.rest.api.v1.auth.LoginCredential;
import com.trekglobal.idempiere.rest.api.v1.auth.impl.AuthServiceImpl;

public class AuthenticationTest extends AbstractTestCase {

	private AuthService authService;

	@BeforeEach
	public void setUp() {
		authService = new AuthServiceImpl();
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
	void authenticateWithSingleClientAndRoleSetsLoginParameters() {
        LoginCredential credential = new LoginCredential();
        credential.setUserName("GardenUser");
        credential.setPassword("GardenUser");

        Response response = authService.authenticate(credential);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains("clientId"));
        assertTrue(response.getEntity().toString().contains("roleId"));
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
    void authenticateWithInvalidCredentialsReturnsUnauthorizedResponse() {
          LoginCredential credential = new LoginCredential();
          credential.setUserName("invalidUser");
          credential.setPassword("invalidPassword");

          Response response = authService.authenticate(credential);
          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
      }

}
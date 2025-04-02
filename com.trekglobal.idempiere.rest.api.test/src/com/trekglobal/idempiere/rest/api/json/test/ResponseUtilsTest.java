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
* - Diego Ruiz                                                        *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.json.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.idempiere.test.AbstractTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.ResponseUtils;

public class ResponseUtilsTest extends AbstractTestCase {

	private Exception exception;
	
	@Mock
    private IDempiereRestException restException;

    @BeforeEach
    public void setUp() {
		MockitoAnnotations.openMocks(this);
        exception = new Exception("General exception");
    }

    @Test
    public void getResponseErrorFromGeneralException() {
        Response response = ResponseUtils.getResponseErrorFromException(exception, "Error Title");

        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        String entity = response.getEntity().toString();
        assertTrue(entity.contains("Error Title"));
        assertTrue(entity.contains("General exception"));
    }

    @Test
    public void getResponseErrorFromRestException() {
        when(restException.getErrorResponseStatus()).thenReturn(Status.BAD_REQUEST);
        when(restException.getTitle()).thenReturn("Invalid Data");
        when(restException.getMessage()).thenReturn("Missing required fields");

        Response response = ResponseUtils.getResponseErrorFromException(restException, "Ignored Title");

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        String entity = response.getEntity().toString();
        assertTrue(entity.contains("Invalid Data"));
        assertTrue(entity.contains("Missing required fields"));
    }

    @Test
    public void getResponseErrorWithDetailText() {
        Response response = ResponseUtils.getResponseErrorFromException(exception, "Error Title", "Detail Text");

        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        String entity = response.getEntity().toString();
        assertTrue(entity.contains("Error Title"));
        assertTrue(entity.contains("Detail Text"));
        assertTrue(entity.contains("General exception"));
    }

    @Test
    public void getResponseErrorWithStatusAndText() {
        Response response = ResponseUtils.getResponseError(Status.FORBIDDEN, "Access Denied", "User lacks permissions", "Contact admin");
        
        assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
        String entity = response.getEntity().toString();
        assertTrue(entity.contains("Access Denied"));
        assertTrue(entity.contains("User lacks permissions"));
        assertTrue(entity.contains("Contact admin"));
    }
}

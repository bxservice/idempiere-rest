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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.compiere.model.PO;
import org.compiere.model.X_AD_User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.ModelHelper;
import com.trekglobal.idempiere.rest.api.model.MRestView;

public class ModelHelperTest extends RestTestCase {

	private ModelHelper modelHelper;

	@Mock
	MRestView mockView;
	
	@BeforeEach
	public void setUp() {
        MockitoAnnotations.openMocks(this);
		modelHelper = new ModelHelper("AD_User", "isActive eq true", "Created", 0, 0);
	}
	
    @Test
    void setViewWithDifferentTableThrowsException() {
        when(mockView.getAD_Table_ID()).thenReturn(1000000);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            modelHelper.setView(mockView);
        });

        assertEquals("Rest view belongs to a different table from what this ModelHelper is using", exception.getMessage());
    }
    
    @Test
    void setViewWithValidTable() {
        when(mockView.getAD_Table_ID()).thenReturn(X_AD_User.Table_ID);
	    assertDoesNotThrow(() -> modelHelper.setView(mockView));
    }

	@Test
	public void getPOsFromRequestReturnsCorrectList() {
		List<PO> result = modelHelper.getPOsFromRequest();
		assertNotNull(result);
	    assertFalse(result.isEmpty(), "The result list should not be empty");
	}
	
	@Test
	public void getPOsFromRequestReturnsCorrectListWithTopSize() {
		int top = 2;
		modelHelper = new ModelHelper("AD_User", "isActive eq true", "Created", top, 0);
		List<PO> result = modelHelper.getPOsFromRequest();
		assertNotNull(result);
	    assertFalse(result.isEmpty(), "The result list should not be empty");
	    assertEquals(top, result.size());
	}
	
	@Test
	public void getPOsFromRequestThrowsExceptionOnWrongTableName() {
		modelHelper = new ModelHelper("AD_Users", "isActive eq true", "Created", 0, 0);
		assertThrows(IDempiereRestException.class, () -> {
			modelHelper.getPOsFromRequest();
        });
	}
	
	@Test
	public void getPOsFromRequestThrowsExceptionOnWrongFilter() {
		modelHelper = new ModelHelper("AD_User", "isActive = 'Y'", "Created", 0, 0);
		IDempiereRestException exception = assertThrows(IDempiereRestException.class, () -> {
			modelHelper.getPOsFromRequest();
        });
    	
	    assertNotNull(exception, "Exception should not be null");
        assertEquals(Status.BAD_REQUEST, exception.getErrorResponseStatus());
	}
	
	@Test
	public void getPOsFromRequestThrowsExceptionWithInvalidValidationRuleID() {
		modelHelper = new ModelHelper("AD_Users", "isActive eq true", "Created", 0, 0, "-1", "");
		IDempiereRestException exception = assertThrows(IDempiereRestException.class, () -> {
			modelHelper.getPOsFromRequest();
        });
		
		assertNotNull(exception, "Exception should not be null");
        assertEquals(Status.NOT_FOUND, exception.getErrorResponseStatus());
	}

	@Test
	public void getSQLStamentIncludesViewWhereClause() {
		String whereClause = "AD_User_ID = 100 AND Description IS NOT NULL";
        when(mockView.getAD_Table_ID()).thenReturn(X_AD_User.Table_ID);
        when(mockView.getWhereClause()).thenReturn(whereClause);
        modelHelper.setView(mockView);
        modelHelper.getPOsFromRequest();
		
		assertNotNull(modelHelper.getSQLStatement(), "SQL Statement should not be null");
		assertTrue(modelHelper.getSQLStatement().contains(whereClause));
	}
	
	@Test
	public void getPOsFromRequestReturnsSortedList() {
	    modelHelper = new ModelHelper("AD_User", "isActive eq true", "Created ASC", 0, 0);
	    List<PO> result = modelHelper.getPOsFromRequest();

	    assertFalse(result.isEmpty(), "Result list should not be empty");

	    // Assert: Check if the list is sorted by 'Created' field in ascending order
	    for (int i = 1; i < result.size(); i++) {
	        Timestamp previous = result.get(i - 1).getCreated();
	        Timestamp current = result.get(i).getCreated();
	        assertTrue(previous.before(current) || previous.equals(current),
	                   "List is not sorted in ascending order by Created");
	    }
	}
	
}

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.ws.rs.core.Response.Status;

import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.idempiere.test.AbstractTestCase;
import org.junit.jupiter.api.Test;

import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.RestUtils;

public class RestUtilsTest extends AbstractTestCase {

	@Test
    void isUUIDReturnsTrueForValidUUID() {
        assertTrue(RestUtils.isUUID("123e4567-e89b-12d3-a456-426614174000"));
    }

    @Test
    void isUUIDReturnsFalseForInvalidUUID() {
        assertFalse(RestUtils.isUUID("invalid-uuid"));
        assertFalse(RestUtils.isUUID("100"));
        assertFalse(RestUtils.isUUID("550e8400-e29b-41d4-a716-44665544000Z")); //Invalid UUID because it contains non HEX characters
    }

    @Test
    void testGetQuery_WithUUID() {
        String tableName = "C_Order";
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        boolean fullyQualified = false;
        boolean RW = false;
        assertNotNull(RestUtils.getQuery(tableName, uuid, fullyQualified, RW));
    }
    
    @Test
    void testGetQuery_WithNumericID() {
        String tableName = "C_Order";
        String numericID = "100";
        boolean fullyQualified = false;
        boolean RW = false;
        assertNotNull(RestUtils.getQuery(tableName, numericID, fullyQualified, RW));
    }
    
    @Test
    void testGetQuery_WithInvalidTableName() {
        String tableName = "C_Order_Line";
        String emptyID = "100";
        boolean fullyQualified = false;
        boolean RW = false;
        assertThrows(IDempiereRestException.class, () -> {
        	RestUtils.getQuery(tableName, emptyID, fullyQualified, RW);
        });
    }
    
    @Test
    void getKeyColumnNameReturnsValidColumnNameForValidTable() {
        String keyColumn = RestUtils.getKeyColumnName("AD_User");
        assertNotNull(keyColumn);
        assertEquals("AD_User_ID", keyColumn);
    }

    @Test
    void getKeyColumnNameThrowsExceptionForInvalidTable() {
    	IDempiereRestException exception = assertThrows(IDempiereRestException.class, () -> {
            RestUtils.getKeyColumnName("ADUser");
        });
    	
        assertEquals("Invalid Table Name", exception.getTitle());
        assertEquals(Status.BAD_REQUEST, exception.getErrorResponseStatus());
    }
    
    @Test
    void testTableWithMultiplePrimaryKeysThrowsException() {
    	IDempiereRestException exception = assertThrows(IDempiereRestException.class, () -> {
            RestUtils.getKeyColumnName("AD_User_OrgAccess");
        });
    	
        assertEquals("Wrong detail", exception.getTitle());
        assertEquals(Status.INTERNAL_SERVER_ERROR, exception.getErrorResponseStatus());
    }

    @Test
    void testGetPOWithValidID() {
    	PO po = RestUtils.getPO("AD_User", "104", true, true);
        assertNotNull(po);
        assertTrue(po instanceof MUser);
        assertEquals(104, po.get_ID());
    }
    
    @Test
    void testGetPOWithValidUUID() {
    	PO po = RestUtils.getPO("AD_User", "eb6dee9e-b7ef-4d40-bd7e-c2c3ecd9d79c", true, true);
        assertNotNull(po);
        assertTrue(po instanceof MUser);
        assertEquals(102, po.get_ID()); //GardenUser
    }

    @Test
    void testGetPOWithInvalidID() {
        assertThrows(IDempiereRestException.class, () -> {
            RestUtils.getPO("AD_User", "invalid-id", true, true);
        });
    }
    
    @Test
    void testGetPOWithEmptyID() {
        PO po = RestUtils.getPO("AD_User", "", true, true);
        assertNull(po);
    }
    
    @Test
    void testGetPOWithInvalidRecordID() {
        PO po = RestUtils.getPO("AD_User", "1", true, true);
        assertNull(po);
    }
    
    @Test
    void testGetPOWithUnauthorizedRecordID() {
        PO po = RestUtils.getPO("AD_User", "10", true, true); //System User
        assertNull(po);
    }

    @Test
    void testValidSingleTableColumnSelection() {
        HashMap<String, ArrayList<String>> result = RestUtils.getIncludes("AD_User", "Name,AD_User_ID", "");
        assertNotNull(result);
        assertTrue(result.containsKey("AD_User"));
        assertTrue(result.get("AD_User").contains("Name"));
        assertFalse(result.get("AD_User").contains("Value"));
    }

    @Test
    void getIncludesThrowsExceptionForInvalidColumn() {
    	IDempiereRestException exception = assertThrows(IDempiereRestException.class, () -> {
            RestUtils.getIncludes("AD_User", "InvalidColumn", "Details");
        });
    	
        assertEquals(Status.BAD_REQUEST, exception.getErrorResponseStatus());
    }
    
    @Test
    void testValidDetailTableSelection() {
        HashMap<String, ArrayList<String>> result = RestUtils.getIncludes("AD_User", "Name,AD_User_Roles/AD_Role_ID", "AD_User_Roles");
        assertNotNull(result);
        assertTrue(result.containsKey("AD_User_Roles"));
        assertTrue(result.get("AD_User_Roles").contains("AD_Role_ID"));
        assertFalse(result.get("AD_User_Roles").contains("AD_User_Roles_UU"));
    }

    @Test
    void getSelectedColumnsReturnsNonEmptyArrayForValidInputs() {
        assertTrue(RestUtils.getSelectedColumns("AD_User", "Name,Email").length > 0);
    }

    @Test
    void getSelectedColumnsThrowsExceptionForInvalidColumn() {
    	IDempiereRestException exception = assertThrows(IDempiereRestException.class, () -> {
            RestUtils.getSelectedColumns("AD_User", "InvalidColumn");
        });
        
        assertEquals(Status.BAD_REQUEST, exception.getErrorResponseStatus());
    }
    
    @Test
    void testValidColumnSelection() {
        String[] result = RestUtils.getSelectedColumns("AD_User", "Name");
        assertNotNull(result);
        assertEquals(1, result.length, "Result should contain one column");
        assertEquals("Name", result[0], "Result should contain the selected column");
    }
    
    @Test
    void testMultipleValidColumnsSelection() {
        String[] result = RestUtils.getSelectedColumns("AD_User", "Name,Email");
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.length, "Result should contain two columns");
        assertTrue(Arrays.asList(result).contains("Name"), "Result should contain the first selected column");
        assertTrue(Arrays.asList(result).contains("Email"), "Result should contain the second selected column");
    }
    
    @Test
    void testEmptySelectClauseOrTableNameReturnsEmptyArray() {
        assertEquals(0, RestUtils.getSelectedColumns("", "Name").length, "Should return an empty array for empty table name");
        assertEquals(0, RestUtils.getSelectedColumns("AD_User", "").length, "Should return an empty array for empty select clause");
    }
    
    @Test
    void getTableAndCheckAccessReturnsNonNullTableForValidInputs() {
        assertNotNull(RestUtils.getTableAndCheckAccess("AD_User"));
    }

    @Test
    void getTableAndCheckAccessThrowsExceptionForInvalidTableName() {
        assertThrows(IDempiereRestException.class, () -> {
            RestUtils.getTableAndCheckAccess("InvalidTable");
        });
    }

    @Test
    void hasAccessReturnsTrueForValidInputs() {
        assertTrue(RestUtils.hasAccess(MTable.get(Env.getCtx(), "AD_User"), true));
    }

    @Test
    void hasAccessReturnsFalseForInvalidInputs() {
        assertFalse(RestUtils.hasAccess(MTable.get(Env.getCtx(), "InvalidTable"), true));
    }

    @Test
    void getViewReturnsNullForInvalidName() {
        assertNull(RestUtils.getView("InvalidViewName"));
    }

    @Test
    void getLinkKeyColumnNameReturnsValidColumnNameForValidInputs() {
        assertNotNull(RestUtils.getLinkKeyColumnName("C_ORder", "C_OrderLine"));
    }

    @Test
    void getLinkKeyColumnNameThrowsExceptionForInvalidInputs() {
        assertThrows(IDempiereRestException.class, () -> {
            RestUtils.getLinkKeyColumnName("InvalidTable", "AD_User_Role");
        });
    }

    @Test
    void isValidDetailTableReturnsTrueForValidInputs() {
        assertTrue(RestUtils.isValidDetailTable(MTable.get(Env.getCtx(), "C_OrderLine"), "C_Order_ID"));
    }

    @Test
    void isValidDetailTableReturnsFalseForInvalidInputs() {
        assertFalse(RestUtils.isValidDetailTable(MTable.get(Env.getCtx(), "InvalidTable"), "InvalidColumn"));
    }

    @Test
    void isReturnUULookupReturnsFalseForInvalidTable() {
        assertFalse(RestUtils.isReturnUULookup("InvalidTable"));
    }

    @Test
    void isStringInCommaSeparatedListReturnsTrueForValidInputs() {
        assertTrue(RestUtils.isStringInCommaSeparatedList("AD_User,AD_Role", "AD_User"));
    }

    @Test
    void isStringInCommaSeparatedListReturnsFalseForInvalidInputs() {
        assertFalse(RestUtils.isStringInCommaSeparatedList("AD_User,AD_Role", "InvalidTable"));
    }
}

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
package com.trekglobal.idempiere.rest.api.json.expand.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

import static org.mockito.Mockito.mockStatic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.compiere.model.MSysConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.expand.ExpandUtils;
import com.trekglobal.idempiere.rest.api.json.test.RestTestCase;

public class ExpandUtilsTest extends RestTestCase {

	@BeforeEach
	public void setUp() {
	}

	@Test
	void testIsRecordIDTableIDFK() {
		assertTrue(ExpandUtils.isRecordIDTableIDFK("Record_ID"));
		assertTrue(ExpandUtils.isRecordIDTableIDFK("record_id"));   // case-insensitive
		assertFalse(ExpandUtils.isRecordIDTableIDFK("AD_Table_ID"));
	}

	// Operators extraction

	final List<String> ops =
			Arrays.asList("$select=Name,Price", "$filter=IsActive eq true AND IsSOTrx eq false",
					"$orderby=Created", "$top=25", "$skip=5", "$expand=Lines");

	@Test 
	void getSelectClause() {
		assertEquals("Name,Price", ExpandUtils.getSelectClause(ops));
	}
	
	@Test
	void getFilterClause() {
		assertEquals("IsActive eq true AND IsSOTrx eq false", ExpandUtils.getFilterClause(ops));
	}
	
	@Test void getOrderByClause() {
		assertEquals("Created", ExpandUtils.getOrderByClause(ops)); 
	}
	
	@Test
	void getExpandClause() { 
		assertEquals("Lines", ExpandUtils.getExpandClause(ops));
	}
	
	@Test
	void getTopClause() { 
		assertEquals(25, ExpandUtils.getTopClause(ops));
	}
	
	@Test 
	void getSkipClause() {
		assertEquals(5,  ExpandUtils.getSkipClause(ops));
	}

	@Test
	void topSkipMustBeNumeric() {
		List<String> bad = List.of("$top=abc");
		assertThrows(IDempiereRestException.class, () -> ExpandUtils.getTopClause(bad));
	}
	
	// TableOperatorMap
    @Test
    void getTableNamesOperatorsMapNoOperators() {
        String expand = "C_OrderLine,C_InvoiceLine";
        Map<String, List<String>> map = ExpandUtils.getTableNamesOperatorsMap(expand);

        assertEquals(2, map.size());
        assertTrue(map.get("C_OrderLine").isEmpty());
        assertTrue(map.get("C_InvoiceLine").isEmpty());
    }

    @Test
    void getTableNamesOperatorsMapWithOperators() {
        String expand = "C_OrderLine($select=Line,Linenetamt ; $filter=LineNetAmt gt 1000 ; $orderby=Line),"
                      + "C_InvoiceLine($filter=IsActive eq true)";
        Map<String, List<String>> map = ExpandUtils.getTableNamesOperatorsMap(expand);

        List<String> orderOps   = map.get("C_OrderLine");
        List<String> invoiceOps = map.get("C_InvoiceLine");

        assertEquals(List.of("$select=Line,Linenetamt", "$filter=LineNetAmt gt 1000", "$orderby=Line"), orderOps);
        assertEquals(List.of("$filter=IsActive eq true"), invoiceOps);
    }
    
    @Test
    void returnsEmptyMapForEmptyExpandParameter() {
        String expand = "";
        Map<String, List<String>> result = ExpandUtils.getTableNamesOperatorsMap(expand);

        assertTrue(result.isEmpty());
    }
    
    @Test
    void getExpandOperatorsWithDifferentSeparator() {
    	try (MockedStatic<MSysConfig> cfg = mockStatic(MSysConfig.class)) {
            cfg.when(() -> MSysConfig.getValue("REST_EXPAND_SEPARATOR", ";"))
               .thenReturn("^");

            String expand = "C_OrderLine($select=Line,Linenetamt ^ $filter=LineNetAmt gt 1000 ^ $orderby=Line)";

            Map<String, List<String>> map = ExpandUtils.getTableNamesOperatorsMap(expand);
            List<String> orderOps   = map.get("C_OrderLine");
            assertEquals(List.of("$select=Line,Linenetamt", "$filter=LineNetAmt gt 1000", "$orderby=Line"), orderOps);

        }
    }
    
    @Test
    void addsDetailSQLCommandToJsonSuccessfully() {
        Map<String, String> tableSQLMap = new HashMap<>();
        tableSQLMap.put("Table1", "SELECT * FROM Table1");
        JsonObject json = new JsonObject();

        ExpandUtils.addDetailSQLCommandToJson(tableSQLMap, json);

        Assertions.assertTrue(json.has("sql-command-Table1"));
        Assertions.assertEquals("SELECT * FROM Table1", json.get("sql-command-Table1").getAsString());
    }
    
    /** Unit tests for ExpandUtils.addDetailDataToJson(..). */
    
    @Test
    void copiesAllDetailEntries() {
        JsonArray lines  = new JsonArray(); lines.add("L-1"); lines.add("L-2");
        JsonArray labels = new JsonArray(); labels.add("A");  labels.add("B");

        Map<String, JsonElement> map = new HashMap<>();
        map.put("C_OrderLine", lines);
        map.put("assigned-labels", labels);

        JsonObject target = new JsonObject(); // initially empty
        ExpandUtils.addDetailDataToJson(map, target);

        assertEquals(2, target.size());
        assertSame(lines,  target.get("C_OrderLine"));
        assertSame(labels, target.get("assigned-labels"));
    }
    
    @Test
    void targetRemainsUnchangedWhenDetailIsEmpty() {
        Map<String, JsonElement> empty = Map.of();
        JsonObject target = new JsonObject();
        target.addProperty("preExisting", 1);
        ExpandUtils.addDetailDataToJson(empty, target);

        assertEquals(1, target.size()); // still only the original property
        assertEquals(1, target.get("preExisting").getAsInt());
    }

}
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
package com.trekglobal.idempiere.rest.api.json.filter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.adempiere.exceptions.AdempiereException;
import org.junit.jupiter.api.Test;

import com.trekglobal.idempiere.rest.api.json.filter.ODataUtils;
import com.trekglobal.idempiere.rest.api.json.test.RestTestCase;

public class ODataUtilsTest extends RestTestCase {

	@Test
	void returnsSQLFunctionForLowercaseMethod() {
	    String result = ODataUtils.getSQLFunction(ODataUtils.LOWER, "TestColumn", false);
	    assertEquals("lower(TestColumn)", result);
	}
	
	@Test
	void returnsSQLFunctionForUppercaseMethod() {
	    String result = ODataUtils.getSQLFunction(ODataUtils.UPPER, "TestColumn", false);
	    assertEquals("upper(TestColumn)", result);
	}
	
	@Test
	void throwsExceptionForUnsupportedSQLFunction() {
	    assertThrows(AdempiereException.class, () -> {
	        ODataUtils.getSQLFunction("unsupportedMethod", "TestColumn", false);
	    });
	}

	@Test
	void returnsSQLMethodOperatorForContains() {
	    String result = ODataUtils.getSQLMethodOperator(ODataUtils.CONTAINS, false);
	    assertEquals(" LIKE ", result);
	}

	@Test
	void throwsExceptionForUnsupportedSQLMethodOperator() {
	    assertThrows(AdempiereException.class, () -> {
	        ODataUtils.getSQLMethodOperator("unsupportedMethod", false);
	    });
	}
	
	@Test
    void getOperatorReturnsSqlSymbol() {
        assertEquals("=",  ODataUtils.getOperator(ODataUtils.EQUALS));
        assertEquals("OR", ODataUtils.getOperator(ODataUtils.OR));
        assertNull(        ODataUtils.getOperator("unknown"));
    }
	
	// ──────────────────────────────────────────────────────────────────────────
    // isMethodCall / getMethodCall / parameter helpers
    // ──────────────────────────────────────────────────────────────────────────
	private final String withParams = "contains(Name,'John')";  // 2‑arg form
    private final String singleArg  = "tolower(Name)";          // 1‑arg form
	private final String withParamsAndSpaces = "startswith(Name , 'John Spaces')";  // 2‑arg form

    
    @Test 
    void detectsMethodCall() {
        assertTrue (ODataUtils.isMethodCall(withParams));
        assertTrue (ODataUtils.isMethodCall(singleArg));
        assertTrue (ODataUtils.isMethodCall(withParamsAndSpaces));
        assertFalse(ODataUtils.isMethodCall("Name eq 'John'"));
    }

    @Test 
    void extractsMethodName() {
        assertEquals(ODataUtils.CONTAINS, ODataUtils.getMethodCall(withParams));
        assertEquals(ODataUtils.LOWER,    ODataUtils.getMethodCall(singleArg));
        assertEquals(ODataUtils.STARTSWITH, ODataUtils.getMethodCall(withParamsAndSpaces));
    }

    @Test 
    void parameterParsingFor2ArgMethod() {
        String first  = ODataUtils.getFirstParameter(ODataUtils.CONTAINS, withParams);
        String second = ODataUtils.getSecondParameter(ODataUtils.CONTAINS, withParams);

        assertEquals("Name",   first);
        assertEquals("'John'", second);
    }
    
    @Test 
    void parameterParsingFor2ArgWithSpacesMethod() {
        String first  = ODataUtils.getFirstParameter(ODataUtils.STARTSWITH, withParamsAndSpaces);
        String second = ODataUtils.getSecondParameter(ODataUtils.STARTSWITH, withParamsAndSpaces);

        assertEquals("Name",   first);
        assertEquals("'John Spaces'", second);
    }

    @Test 
    void parameterParsingFor1ArgMethod() {
        String first  = ODataUtils.getFirstParameter(ODataUtils.LOWER, singleArg);
        String second = ODataUtils.getSecondParameter(ODataUtils.LOWER, singleArg);

        assertEquals("Name", first);
        assertNull(second);
    }

    @Test
    void unsupportedMethodThrows() {
        String expr = "unsupportedMethod(Name)";
        var ex = assertThrows(AdempiereException.class,
                              () -> ODataUtils.isMethodCall(expr));
        assertTrue(ex.getMessage().contains("not implemented"));
    }	

}
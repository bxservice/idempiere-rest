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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.compiere.model.GridField;
import org.compiere.model.MColumn;
import org.compiere.util.DisplayType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonPrimitive;
import com.trekglobal.idempiere.rest.api.json.NumericTypeConverter;

public class NumericConverterTest extends RestTestCase {
	
	private NumericTypeConverter converter;
	
	@Mock
	private MColumn mockColumn;
	@Mock
    private GridField mockField;

    @BeforeEach
    public void setUp() {
    	MockitoAnnotations.openMocks(this);
        converter = new NumericTypeConverter();
    }

	@Test
	public void testToJsonValue_IntegerColumn() {
        when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.Integer);
        Object result = converter.toJsonValue(mockColumn, 123);
	    assertEquals(123, result);
	}
	
	@Test
	public void toJsonValueReturnsIntegerForIntegerDisplayTypeAndNonDoubleValue() {
        when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.Integer);
	    Object result = converter.toJsonValue(mockColumn, 125.888);
	    assertEquals(125, result);
	}
	
	@Test
	public void toJsonValueReturnsIntegerForIntegerDisplayTypes() {
        when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.RecordID);
	    Object result = converter.toJsonValue(mockColumn, 125);
	    assertTrue(result instanceof Integer);
	    
        when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.ID);
        result = converter.toJsonValue(mockColumn, 125);
        assertTrue(result instanceof Integer);
	}
	
    @Test
    public void testToJsonValue_BigDecimalColumn() {
        when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.Amount);
        Object result = converter.toJsonValue(mockColumn, new BigDecimal("45.67"));
        assertEquals(new BigDecimal("45.67"), result);
    }
    
    @Test
    public void testToJsonValue_NonNumericColumn() {
        when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.String);
        Object result = converter.toJsonValue(mockColumn, 789);
	    assertNull(result);
    }
    
    @Test
    public void testToJsonValue_IntegerField() {
        when(mockField.getDisplayType()).thenReturn(DisplayType.ID);
        Object jsonValue = converter.toJsonValue(mockField, 321);
        assertEquals(321, jsonValue);
    }

    @Test
    public void testToJsonValue_NumberField() {
        when(mockField.getDisplayType()).thenReturn(DisplayType.Number);
        Object jsonValue = converter.toJsonValue(mockField, 12.34);
        assertEquals(12.34, jsonValue);
    }
    
	@Test
	public void fromJsonValueParsesIntegerForIntegerDisplayType() {
	    when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.Integer);
	    Object result = converter.fromJsonValue(mockColumn, new JsonPrimitive(123));
	    assertEquals(123, result);
	}

	@Test
	public void fromJsonValueParsesBigDecimalForNumericDisplayType() {
	    when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.Amount);
	    Object result = converter.fromJsonValue(mockColumn, new JsonPrimitive(123.45));
	    assertEquals(new BigDecimal("123.45"), result);
	}

	@Test
	public void fromJsonValueParsesStringToIntegerForIntegerDisplayType() {
	    when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.Integer);
	    Object result = converter.fromJsonValue(mockColumn, new JsonPrimitive("123"));
	    assertEquals(123, result);
	}

	@Test
	public void fromJsonValueReturnsNullForNonNumericDisplayType() {
	    when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.String);
	    Object result = converter.fromJsonValue(mockColumn, new JsonPrimitive("non-numeric"));
	    assertNull(result);
	}

	@Test
	public void toJsonValueHandlesGridFieldWithIntegerDisplayType() {
	    when(mockField.getDisplayType()).thenReturn(DisplayType.Integer);
	    Object result = converter.toJsonValue(mockField, 456);
	    assertEquals(456, result);
	}

	@Test
	public void fromJsonValueHandlesGridFieldWithNumericDisplayType() {
	    when(mockField.getDisplayType()).thenReturn(DisplayType.Amount);
	    Object result = converter.fromJsonValue(mockField, new JsonPrimitive(789.01));
	    assertEquals(new BigDecimal("789.01"), result);
	}

}

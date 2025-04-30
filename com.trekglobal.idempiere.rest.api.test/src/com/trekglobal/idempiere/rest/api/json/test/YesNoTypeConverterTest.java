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
import static org.mockito.Mockito.when;

import org.compiere.model.GridField;
import org.compiere.model.MColumn;
import org.compiere.util.DisplayType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonPrimitive;
import com.trekglobal.idempiere.rest.api.json.YesNoTypeConverter;

public class YesNoTypeConverterTest extends RestTestCase {
	
	private YesNoTypeConverter converter;
	@Mock
	private MColumn mockColumn;
	@Mock
    private GridField mockField;

    @BeforeEach
    public void setUp() {
    	MockitoAnnotations.openMocks(this);
        converter = new YesNoTypeConverter();
        when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.YesNo);
        when(mockField.getDisplayType()).thenReturn(DisplayType.YesNo);
    }

    @Test
    public void toJsonValueReturnsTrueForBooleanTrue() {
        Object result = converter.toJsonValue(mockColumn, true);
        assertEquals(true, result);
    }

    @Test
    public void toJsonValueReturnsFalseForBooleanFalse() {
        Object result = converter.toJsonValue(mockColumn, false);
        assertEquals(false, result);
    }

    @Test
    public void toJsonValueReturnsTrueForStringY() {
        Object result = converter.toJsonValue(mockColumn, "Y");
        assertEquals(true, result);
    }

    @Test
    public void toJsonValueReturnsFalseForStringN() {
        Object result = converter.toJsonValue(mockColumn, "N");
        assertEquals(false, result);
    }

    @Test
    public void toJsonValueReturnsTrueForStringTrue() {
        Object result = converter.toJsonValue(mockColumn, "true");
        assertEquals(true, result);
    }

    @Test
    public void toJsonValueReturnsFalseForStringFalse() {
        Object result = converter.toJsonValue(mockColumn, "false");
        assertEquals(false, result);
    }
    
    @Test
    public void toJsonValueReturnsFalseForUnexpectedString() {
        Object result = converter.toJsonValue(mockColumn, "something");
        assertEquals(false, result);
    }

    @Test
    public void fromJsonValueReturnsYForBooleanTrue() {
        JsonPrimitive value = new JsonPrimitive(true);
        Object result = converter.fromJsonValue(mockColumn, value);
        assertEquals("Y", result);
    }

    @Test
    public void fromJsonValueReturnsNForBooleanFalse() {
        JsonPrimitive value = new JsonPrimitive(false);
        Object result = converter.fromJsonValue(mockColumn, value);
        assertEquals("N", result);
    }

    @Test
    public void fromJsonValueReturnsYForStringY() {
        JsonPrimitive value = new JsonPrimitive("Y");
        Object result = converter.fromJsonValue(mockColumn, value);
        assertEquals("Y", result);
    }

    @Test
    public void fromJsonValueReturnsNForStringN() {
        JsonPrimitive value = new JsonPrimitive("N");
        Object result = converter.fromJsonValue(mockColumn, value);
        assertEquals("N", result);
    }

    @Test
    public void fromJsonValueReturnsYForStringTrue() {
        JsonPrimitive value = new JsonPrimitive("true");
        Object result = converter.fromJsonValue(mockColumn, value);
        assertEquals("Y", result);
    }

    @Test
    public void fromJsonValueReturnsNForStringFalse() {
        JsonPrimitive value = new JsonPrimitive("false");
        Object result = converter.fromJsonValue(mockColumn, value);
        assertEquals("N", result);
    }

    @Test
    public void toJsonValueHandlesGridFieldWithBooleanTrue() {
        when(mockField.getDisplayType()).thenReturn(DisplayType.YesNo);
        Object result = converter.toJsonValue(mockField, true);
        assertEquals(true, result);
    }

    @Test
    public void fromJsonValueHandlesGridFieldWithBooleanFalse() {
        when(mockField.getDisplayType()).thenReturn(DisplayType.YesNo);
        JsonPrimitive value = new JsonPrimitive(false);
        Object result = converter.fromJsonValue(mockField, value);
        assertEquals("N", result);
    }
    
    @Test
    public void fromJsonValueHandlesGridFieldWithString() {
        when(mockField.getDisplayType()).thenReturn(DisplayType.YesNo);
        JsonPrimitive value = new JsonPrimitive("no boolean");
        Object result = converter.fromJsonValue(mockField, value);
        assertEquals("N", result);
    }

}

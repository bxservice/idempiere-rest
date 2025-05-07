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
import static org.mockito.Mockito.when;

import org.compiere.model.GridField;
import org.compiere.model.MColumn;
import org.compiere.model.MImage;
import org.compiere.util.DisplayType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.trekglobal.idempiere.rest.api.json.JSONTypeConverter;

public class JSONTypeConverterTest extends RestTestCase {
	
	private JSONTypeConverter converter;
	
	@Mock
	private MColumn mockColumn;
	@Mock
	private GridField mockField;
	@Mock
    private MImage image;

    @BeforeEach
    public void setUp() {
    	MockitoAnnotations.openMocks(this);
        converter = new JSONTypeConverter();
    }

    public void toJsonValueParsesValidJsonString() {
        String jsonString = "{\"key\":\"value\"}";
        Object result = converter.toJsonValue(mockColumn, jsonString);
        assertEquals(JsonParser.parseString(jsonString), result);
    }

    @Test
    public void toJsonValueReturnsNullForEmptyString() {
        String emptyString = "";
        Object result = converter.toJsonValue(mockColumn, emptyString);
        assertNull(result);
    }

    @Test
    public void toJsonValueReturnsNullForNullValue() {
        Object result = converter.toJsonValue(mockColumn, null);
        assertNull(result);
    }

    @Test
    public void fromJsonValueConvertsJsonElementToString() {
        JsonElement jsonElement = JsonParser.parseString("{\"key\":\"value\"}");
        Object result = converter.fromJsonValue(mockColumn, jsonElement);
        assertEquals("{\"key\":\"value\"}", result);
    }

    @Test
    public void fromJsonValueHandlesNullJsonElement() {
        Object result = converter.fromJsonValue(mockColumn, null);
        assertNull(result);
    }

    @Test
    public void toJsonValueHandlesGridFieldWithValidJsonString() {
        when(mockField.getDisplayType()).thenReturn(DisplayType.String);
        String jsonString = "{\"key\":\"value\"}";
        Object result = converter.toJsonValue(mockField, jsonString);
        assertEquals(JsonParser.parseString(jsonString), result);
    }

    @Test
    public void fromJsonValueHandlesGridFieldWithJsonElement() {
        when(mockField.getDisplayType()).thenReturn(DisplayType.String);
        JsonElement jsonElement = JsonParser.parseString("{\"key\":\"value\"}");
        Object result = converter.fromJsonValue(mockField, jsonElement);
        assertEquals("{\"key\":\"value\"}", result);
    }

}

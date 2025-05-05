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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.compiere.model.GridField;
import org.compiere.model.MColumn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonPrimitive;
import com.trekglobal.idempiere.rest.api.json.BinaryTypeConverter;

public class BinaryConverterTest extends RestTestCase {
	
	private BinaryTypeConverter converter;
	
	@Mock
	private MColumn mockColumn;
	@Mock
    private GridField mockField;

    @BeforeEach
    public void setUp() {
    	MockitoAnnotations.openMocks(this);
        converter = new BinaryTypeConverter();
    }

	@Test
	public void toJsonValueReturnsBase64StringForValidByteArray() {
	    byte[] value = new byte[]{1, 2, 3};
	    Object result = converter.toJsonValue(mockColumn, value);
	    assertEquals("AQID", result);
	}

	@Test
	public void toJsonValueReturnsNullForNullByteArray() {
	    Object result = converter.toJsonValue(mockColumn, null);
	    assertNull(result);
	}

	@Test
	public void fromJsonValueParsesBase64StringToByteArray() {
	    JsonPrimitive value = new JsonPrimitive("AQID");
	    Object result = converter.fromJsonValue(mockColumn, value);
	    assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) result);
	}

	@Test
	public void fromJsonValueReturnsNullForNullJsonElement() {
	    Object result = converter.fromJsonValue(mockColumn, null);
	    assertNull(result);
	}

	@Test
	public void toJsonValueHandlesGridFieldWithValidByteArray() {
	    byte[] value = new byte[]{4, 5, 6};
	    Object result = converter.toJsonValue(mockField, value);
	    assertEquals("BAUG", result);
	}

	@Test
	public void fromJsonValueHandlesGridFieldWithBase64String() {
	    JsonPrimitive value = new JsonPrimitive("BAUG");
	    Object result = converter.fromJsonValue(mockField, value);
	    assertArrayEquals(new byte[]{4, 5, 6}, (byte[]) result);
	}

}

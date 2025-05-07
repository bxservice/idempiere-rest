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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.model.MLocation;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.LocationTypeConverter;

public class LocationTypeConverterTest extends RestTestCase {

	private LocationTypeConverter converter;

	@Mock
	private MColumn mockColumn;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		converter = new LocationTypeConverter();
	}

	@Test
	public void toJsonValueReturnsJsonObjectForValidLocation() {
		when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.Location);
		when(mockColumn.getColumnName()).thenReturn("C_Location_ID");
		when(mockColumn.getReferenceTableName()).thenReturn("C_Location");

		MLocation mockLocation = mock(MLocation.class);
		try (MockedStatic<MLocation> mocked = mockStatic(MLocation.class)) {
			mocked.when(() -> MLocation.get(123456)).thenReturn(mockLocation);
			when(mockLocation.get_ID()).thenReturn(123456);
			when(mockLocation.get_Value("City")).thenReturn("New York");
			when(mockLocation.get_Value("C_Region_ID")).thenReturn(200);

			Object result = converter.toJsonValue(mockColumn, 123456, null);
			JsonObject json = (JsonObject) result;

			assertEquals(123456, json.get("id").getAsInt());
			assertEquals("New York", json.get("City").getAsString());
			assertNotNull(json.get("C_Region_ID"));
			assertEquals("c_location", json.get("model-name").getAsString());
		}
	}

	@Test
	public void toJsonValueReturnsJsonObjectForInvalidLocation() {
		when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.Location);
		when(mockColumn.getColumnName()).thenReturn("C_Location_ID");
		when(mockColumn.getReferenceTableName()).thenReturn("C_Location");

		MLocation mockLocation = mock(MLocation.class);
		try (MockedStatic<MLocation> mocked = mockStatic(MLocation.class)) {
			mocked.when(() -> MLocation.get(123456)).thenReturn(mockLocation);
			when(mockLocation.get_ID()).thenReturn(123456);
			//invalid columns
			when(mockLocation.get_Value("Cities")).thenReturn("New York");
			when(mockLocation.get_Value("Region_ID")).thenReturn(200); 

			Object result = converter.toJsonValue(mockColumn, 123456, null);
			JsonObject json = (JsonObject) result;

			assertEquals(123456, json.get("id").getAsInt());
			assertNull(json.get("City"));
			assertNull(json.get("C_Region_ID"));
			assertEquals("c_location", json.get("model-name").getAsString());
		}
	}

	@Test
	public void toJsonValueReturnsNullForNullValue() {
		when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.Location);

		Object result = converter.toJsonValue(mockColumn, null, null);
		assertNull(result);
	}

	@Test
	public void fromJsonValueParsesValidJsonObjectToLocationId() {
		JsonObject json = new JsonObject();
		json.addProperty("City", "Los Angeles");

		Object result = converter.fromJsonValue(mockColumn, json, null);
        assertTrue(result instanceof Integer);        
        MLocation location = new MLocation(Env.getCtx(), (Integer) result, null);
        assertEquals(location.getCity(), "Los Angeles");
        location.deleteEx(true);
	}

	@Test
	public void fromJsonValueReturnsNullForInvalidJsonObject() {
		JsonObject json = new JsonObject();
		json.addProperty("invalidField", "value");

        try {
            converter.fromJsonValue(mockColumn, json);
            fail("Expected AdempiereException to be thrown");
        } catch (AdempiereException e) {
            assertTrue(e.getMessage().contains("does not exist"));
        }
	}

	@Test
	public void fromJsonValueHandlesNullJsonElement() {
		Object result = converter.fromJsonValue(mockColumn, null, null);
		assertNull(result);
	}

}

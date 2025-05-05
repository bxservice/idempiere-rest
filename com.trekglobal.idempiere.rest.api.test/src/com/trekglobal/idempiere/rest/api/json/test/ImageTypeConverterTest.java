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
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.compiere.model.MColumn;
import org.compiere.model.MImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.ImageTypeConverter;

public class ImageTypeConverterTest extends RestTestCase {
	
	private ImageTypeConverter converter;
	
	@Mock
	private MColumn mockColumn;
	@Mock
    private MImage image;

    @BeforeEach
    public void setUp() {
    	MockitoAnnotations.openMocks(this);
        converter = new ImageTypeConverter();
    }

    @Test
    public void toJsonValueReturnsJsonObjectForValidImage() {
        when(image.getBinaryData()).thenReturn(new byte[]{1, 2, 3});
        when(image.get_ID()).thenReturn(100);
        when(image.get_TableName()).thenReturn("AD_Image");
        when(mockColumn.getColumnName()).thenReturn("ImageColumn");
        mockStatic(MImage.class);
        when(MImage.get(100)).thenReturn(image);

        Object result = converter.toJsonValue(mockColumn, 100);
        JsonObject json = (JsonObject) result;

        assertEquals(100, json.get("id").getAsInt());
        assertEquals("AQID", json.get("data").getAsString());
        assertEquals("ad_image", json.get("model-name").getAsString());
    }

    @Test
    public void toJsonValueReturnsNullForNullValue() {
        Object result = converter.toJsonValue(mockColumn, null);
        assertNull(result);
    }

    @Test
    public void toJsonValueReturnsNullForInvalidImageId() {
        when(MImage.get(999)).thenReturn(null);

        Object result = converter.toJsonValue(mockColumn, 999);
        assertNull(result);
    }

    /*@Test
    public void fromJsonValueParsesValidJsonObjectToImageId() {
        JsonObject json = new JsonObject();
        json.addProperty("data", "AQID");
        json.addProperty("file_name", "test.png");
        json.addProperty("url", "http://idempiere.org/image.png");

        Object result = converter.fromJsonValue(mockColumn, json);
        assertTrue(result instanceof Integer);    
    }*/

    @Test
    public void fromJsonValueReturnsNullForInvalidJsonObject() {
        JsonObject json = new JsonObject();
        json.addProperty("invalidField", "value");

        Object result = converter.fromJsonValue(mockColumn, json);
        assertNull(result);
    }

    @Test
    public void fromJsonValueReturnsNullForNullJsonElement() {
        Object result = converter.fromJsonValue(mockColumn, null);
        assertNull(result);
    }

}

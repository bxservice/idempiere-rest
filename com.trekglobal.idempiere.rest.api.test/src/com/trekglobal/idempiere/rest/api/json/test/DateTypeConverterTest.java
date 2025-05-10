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
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.compiere.model.MColumn;
import org.compiere.util.DisplayType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonPrimitive;
import com.trekglobal.idempiere.rest.api.json.DateTypeConverter;
import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;

public class DateTypeConverterTest extends RestTestCase {
	
	private DateTypeConverter converter;
	
	@Mock
	private MColumn mockColumn;

    @BeforeEach
    public void setUp() {
    	MockitoAnnotations.openMocks(this);
        converter = new DateTypeConverter();
    }

    @Test
    public void toJsonValueFormatsDateCorrectlyForDateDisplayType() {
        when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.Date);
        Date date = new Date();
        String expected = new SimpleDateFormat(DateTypeConverter.ISO8601_DATE_PATTERN).format(date);
        Object result = converter.toJsonValue(mockColumn, date);
        assertEquals(expected, result);
    }

    @Test
    public void toJsonValueFormatsDateCorrectlyForDateTimeDisplayType() {
        when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.DateTime);
        Date date = new Date();
        String expected = new SimpleDateFormat(DateTypeConverter.ISO8601_DATETIME_PATTERN).format(date);
        Object result = converter.toJsonValue(mockColumn, date);
        assertEquals(expected, result);
    }

    @Test
    public void toJsonValueReturnsNullForNullDate() {
        when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.Date);
        Object result = converter.toJsonValue(mockColumn, null);
        assertNull(result);
    }

    @Test
    public void fromJsonValueParsesValidDateStringForDateDisplayType() {
        when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.Date);
        String dateString = "2023-10-01";
        JsonPrimitive jsonValue = new JsonPrimitive(dateString);
        Timestamp result = (Timestamp) converter.fromJsonValue(mockColumn, jsonValue);
        assertEquals(dateString, new SimpleDateFormat(DateTypeConverter.ISO8601_DATE_PATTERN).format(result));
    }

    @Test
    public void fromJsonValueParsesValidDateTimeStringForDateTimeDisplayType() {
        when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.DateTime);
        String dateTimeString = "2023-10-01T12:34:56Z";
        JsonPrimitive jsonValue = new JsonPrimitive(dateTimeString);
        Timestamp result = (Timestamp) converter.fromJsonValue(mockColumn, jsonValue);
        assertEquals(dateTimeString, new SimpleDateFormat(DateTypeConverter.ISO8601_DATETIME_PATTERN).format(result));
    }

    @Test
    public void fromJsonValueThrowsExceptionForInvalidDateString() {
        when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.Date);
        JsonPrimitive jsonValue = new JsonPrimitive("invalid-date");
        try {
            converter.fromJsonValue(mockColumn, jsonValue);
            fail("Expected IDempiereRestException to be thrown");
        } catch (IDempiereRestException e) {
            assertTrue(e.getTitle().contains("Invalid date"));
        }
    }

    @Test
    public void getPatternReturnsNullForUnsupportedDisplayType() {
        when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.String);
        Object result = converter.toJsonValue(mockColumn, new Date());
        assertNull(result);
    }

}

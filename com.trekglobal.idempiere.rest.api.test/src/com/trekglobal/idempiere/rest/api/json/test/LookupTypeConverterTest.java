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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.Lookup;
import org.compiere.model.MColumn;
import org.compiere.util.DisplayType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.LookupTypeConverter;

public class LookupTypeConverterTest extends RestTestCase {

	private LookupTypeConverter converter;

	@Mock
	private MColumn mockColumn;
	@Mock
	private Lookup lookup;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		converter = new LookupTypeConverter();
	}

	@Test
	public void fromJsonValueReturnsIdForValidUuidInJsonObject() {
	    JsonObject json = new JsonObject();
	    json.addProperty("uid", "605450c7-24a7-4da9-990d-a8e5e7dd57eb"); //Agri-Tech
	    when(mockColumn.getReferenceTableName()).thenReturn("C_BPartner");
	    when(mockColumn.getColumnName()).thenReturn("C_BPartner_ID");

	    Object result = converter.fromJsonValue(mockColumn, json);
	    assertEquals(200000, result);
	}

	@Test
	public void fromJsonValueThrowsExceptionForInvalidUuidInJsonObject() {
	    JsonObject json = new JsonObject();
	    json.addProperty("uid", "invalid-uuid");

	    when(mockColumn.getReferenceTableName()).thenReturn("C_BPartner");

	    assertThrows(AdempiereException.class, () -> converter.fromJsonValue(mockColumn, json));
	}

	@Test
	public void toJsonValueReturnsNullForUnsupportedDisplayType() {
	    when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.Binary);

	    Object result = converter.toJsonValue(mockColumn, 123, null);
	    assertNull(result);
	}

	@Test
	public void fromJsonValueReturnsNullForEmptyJsonObject() {
	    JsonObject json = new JsonObject();
	    when(mockColumn.getAD_Reference_ID()).thenReturn(DisplayType.Table);

	    assertThrows(AdempiereException.class, () -> converter.fromJsonValue(mockColumn, json));
	}

}

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

import java.util.Date;

import org.compiere.model.MColumn;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.trekglobal.idempiere.rest.api.json.TypeConverterUtils;

public class TypeConverterUtilsTest extends RestTestCase {

	@Test
	public void toPropertyNameConvertsColumnNameWithoutUnderscore() {
	    String result = TypeConverterUtils.toPropertyName("ColumnName");
	    assertEquals("columnName", result);
	}

	@Test
	public void toPropertyNameReturnsSameNameForColumnWithUnderscore() {
	    String result = TypeConverterUtils.toPropertyName("COLUMN_NAME");
	    assertEquals("COLUMN_NAME", result);
	}
	
	@Test
	public void toPropertyNameReturnsNullWhenNull() {
	    String result = TypeConverterUtils.toPropertyName(null);
	    assertEquals(null, result);
	}
	
	@Test
	public void toPropertyNameReturnsEmptyWhenEmpty() {
	    String result = TypeConverterUtils.toPropertyName("");
	    assertEquals("", result);
	}

	@Test
	public void toJsonValueReturnsStringForStringColumn() {
		MColumn column = MColumn.get(327); //Test table > Description column
	    Object result = TypeConverterUtils.toJsonValue(column, "TestValue");
	    assertEquals("TestValue", result);
	}

	@Test
	public void toJsonValueReturnsNullForNullValue() {
		MColumn column = MColumn.get(327); //Test table > Description column
	    Object result = TypeConverterUtils.toJsonValue(column, null);
	    assertNull(result);
	}
	
	@Test
	public void toJsonValueReturnsNumberForNumericColumn() {
		MColumn column = MColumn.get(330); //Test table > Number column
	    Object result = TypeConverterUtils.toJsonValue(column, 123456.8);
	    assertEquals(123456.8, result);
	}
	
	@Test
	public void toJsonValueReturnsNullForStringOnNumericColumn() {
		MColumn column = MColumn.get(330); //Test table > Number column
	    Object result = TypeConverterUtils.toJsonValue(column, "Test");
	    assertNull(result);
	}
	
	@Test
	public void toJsonValueReturnsDateForDate() {
		MColumn column = MColumn.get(331); //Test table > Date column
		Date date = new Date();
	    Object result = TypeConverterUtils.toJsonValue(column, date);
	    assertTrue(result instanceof String);
	}
	
	@Test
	public void toJsonValueReturnsNullForStringOnDateColumn() {
		MColumn column = MColumn.get(331); //Test table > Date column
	    Object result = TypeConverterUtils.toJsonValue(column, 123456.8);
	    assertNull(result);
	}
	
	@Test
	public void toJsonValueReturnsTrueForYesString() {
		MColumn column = MColumn.get(335); //Test table > isActive column
	    Object result = TypeConverterUtils.toJsonValue(column, 'Y');
	    assertEquals(true, result);
	}
	
	@Test
	public void toJsonValueReturnsFalseForNoString() {
		MColumn column = MColumn.get(335); //Test table > isActive column
	    Object result = TypeConverterUtils.toJsonValue(column, 'N');
	    assertEquals(false, result);
	}
	
	@Test
	public void toJsonValueReturnsTrueForBooleanTrue() {
		MColumn column = MColumn.get(335); //Test table > isActive column
	    Object result = TypeConverterUtils.toJsonValue(column, true);
	    assertEquals(true, result);
	}
	
	@Test
	public void toJsonValueReturnsTrueForStringTrue() {
		MColumn column = MColumn.get(335); //Test table > isActive column
	    Object result = TypeConverterUtils.toJsonValue(column, "true");
	    assertEquals(true, result);
	}
	
	@Test
	public void toJsonValueReturnsFalseForStringDifferentThanTrue() {
		MColumn column = MColumn.get(335); //Test table > isActive column
	    Object result = TypeConverterUtils.toJsonValue(column, "Yes");
	    assertEquals(false, result);
	}
	
	@Test
	public void toJsonValueReturnsJsonObjectForLocation() {
		MColumn column = MColumn.get(3890); //Test table > Address column
	    Object result = TypeConverterUtils.toJsonValue(column, 109);
	    assertTrue(result instanceof JsonObject);
        assertTrue(((JsonObject) result).has("Address1"));
	}
	
	@Test
	public void toJsonValueReturnsNullForWrongValueLocation() {
		MColumn column = MColumn.get(3890); //Test table > Address column
	    Object result = TypeConverterUtils.toJsonValue(column, "Test address");
	    assertNull(result);
	}
	
	@Test
	public void toJsonValueReturnsNullForNullValueLocation() {
		MColumn column = MColumn.get(3890); //Test table > Address column
	    Object result = TypeConverterUtils.toJsonValue(column, null);
	    assertNull(result);
	}
	
	@Test
	public void toJsonValueReturnsJsonObjectForLocator() {
		MColumn column = MColumn.get(5374); //Test table > Locator column
	    Object result = TypeConverterUtils.toJsonValue(column, 101);
	    assertTrue(result instanceof JsonObject);
        assertTrue(((JsonObject) result).has("propertyLabel"));
	}
	
	@Test
	public void toJsonValueReturnsNullForNullLookup() {
		MColumn column = MColumn.get(5374); //Test table > Locator column
	    Object result = TypeConverterUtils.toJsonValue(column, null);
	    assertNull(result);
	}
	
	@Test
	public void toJsonValueReturnsBase64StringForBinary() {
        byte[] bytes = { 1, 2, 3, 4 };
		MColumn column = MColumn.get(10011); //Test table > Binary Data column
	    Object result = TypeConverterUtils.toJsonValue(column, bytes);
        assertTrue(result instanceof String);
        assertEquals("AQIDBA==", result);
    }
	
	@Test
	public void toJsonValueReturnsNullForBinaryNull() {
		MColumn column = MColumn.get(10011); //Test table > Binary Data column
	    Object result = TypeConverterUtils.toJsonValue(column, null);
	    assertNull(result);
	}
	
	@Test
	public void toJsonValueReturnsJsonObjectForImageID() {
		MColumn column = MColumn.get(58113); //C_Bpartner table > Logo column
	    Object result = TypeConverterUtils.toJsonValue(column, 100);
	    assertTrue(result instanceof JsonObject);
        assertTrue(((JsonObject) result).has("propertyLabel"));
   }
	
	@Test
	public void toJsonValueReturnsJsonObjectForImageIDNonExisting() {
		MColumn column = MColumn.get(58113); //C_Bpartner table > Logo column
	    Object result = TypeConverterUtils.toJsonValue(column, 50);
	    assertNull(result);
   }
	
	@Test
	public void toJsonValueReturnsNullForImageNull() {
		MColumn column = MColumn.get(58113); //C_Bpartner table > Logo column
	    Object result = TypeConverterUtils.toJsonValue(column, null); 
	    assertNull(result);
	}
	
	@Test
	public void toJsonValueReturnsJsonObjectForJsonObject() {
		MColumn column = MColumn.get(216570); //Test table > JsonData column
	    Object result = TypeConverterUtils.toJsonValue(column, "{\"key\": \"value\"}");
	    assertTrue(result instanceof JsonObject);
        assertTrue(((JsonObject) result).has("key"));
        assertEquals("value", ((JsonObject) result).get("key").getAsString());
	}
	
	@Test
	public void toJsonValueReturnsNullForEmptyOrNullJson() {
		MColumn column = MColumn.get(216570); //C_Bpartner table > JsonData column
	    assertNull(TypeConverterUtils.toJsonValue(column, ""));
	    assertNull(TypeConverterUtils.toJsonValue(column, null));
	}
	
	@Test
	public void fromJsonValueReturnsStringForTextColumn() {
		MColumn column = MColumn.get(327); //Test table > Description column
	    Object result = TypeConverterUtils.fromJsonValue(column, new JsonPrimitive("TestValue"));
	    assertEquals("TestValue", result);
	}

	@Test
	public void fromJsonValueReturnsNullForJsonNullTextColumn() {
		MColumn column = MColumn.get(327); //Test table > Description column
	    Object result = TypeConverterUtils.fromJsonValue(column, JsonNull.INSTANCE);
	    assertNull(result);
	}
	
	@Test
	public void fromJsonValueReturnsNullForNumericColumn() {
		MColumn column = MColumn.get(330); //Test table > Number column
	    Object result = TypeConverterUtils.fromJsonValue(column, JsonNull.INSTANCE);
	    assertNull(result);
	}

	@Test
	public void slugifyConvertsTextToSlug() {
	    String result = TypeConverterUtils.slugify("Test Input Text!");
	    assertEquals("test-input-text", result);
	    
	    result = TypeConverterUtils.slugify("a-b-c");
	    assertEquals("a-b-c", result);
	}

	@Test
	public void slugifyHandlesEmptyInput() {
	    String result = TypeConverterUtils.slugify("");
	    assertEquals("", result);
	}
	
	@Test
    public void slugify_removesPunctuationAndLowercases() {
        String input = "Hello, World!";
        String expected = "hello-world";
        assertEquals(expected, TypeConverterUtils.slugify(input));
    }

    @Test
    public void slugify_handlesAccentsAndNonLatinCharacters() {
        String input = "Café déjà vu";
        String expected = "cafe-deja-vu";
        assertEquals(expected, TypeConverterUtils.slugify(input));
    }

    @Test
    public void slugify_replacesMultipleSeparatorsWithSingleDash() {
        String input = "A  --  B";
        String expected = "a-b";
        assertEquals(expected, TypeConverterUtils.slugify(input));
    }

    @Test
    public void slugify_trimsLeadingAndTrailingDashes() {
        String input = " --- Hello --- ";
        String expected = "hello";
        assertEquals(expected, TypeConverterUtils.slugify(input));
    }

    @Test
    public void slugify_keepsHyphenAndUnderscore() {
        String input = "foo-bar_baz";
        String expected = "foo-bar_baz";
        assertEquals(expected, TypeConverterUtils.slugify(input));
    }

    @Test
    public void slugify_handlesOnlyPunctuation() {
        assertEquals("", TypeConverterUtils.slugify("!!!...,,,")); // becomes empty
    }

    @Test
    public void slugify_collapsesConsecutiveSeparators() {
        String input = "foo    bar";
        String expected = "foo-bar";
        assertEquals(expected, TypeConverterUtils.slugify(input));
    }

    @Test
    public void slugify_preservesCaseAsLower() {
        String input = "This IS Mixed CASE";
        String expected = "this-is-mixed-case";
        assertEquals(expected, TypeConverterUtils.slugify(input));
    }
	
}

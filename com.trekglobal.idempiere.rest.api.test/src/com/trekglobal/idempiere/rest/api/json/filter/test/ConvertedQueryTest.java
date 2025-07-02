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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MColumn;
import org.compiere.util.DisplayType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.filter.ConvertedQuery;
import com.trekglobal.idempiere.rest.api.json.test.RestTestCase;

public class ConvertedQueryTest extends RestTestCase {

    private ConvertedQuery query;

    @BeforeEach
    public void setup() {
        query = new ConvertedQuery();
    }

    @Test
    public void testConstructor_initialState() {
        assertEquals("", query.getWhereClause());
        assertNotNull(query.getParameters());
        assertTrue(query.getParameters().isEmpty());
    }

    @Test
    public void testAppendWhereClause() {
        query.appendWhereClause("WHERE Name = 'John'");
        assertEquals("WHERE Name = 'John'", query.getWhereClause());
        query.appendWhereClause(" AND Age > 20");
        assertEquals("WHERE Name = 'John' AND Age > 20", query.getWhereClause());
    }

    @Test
    public void testAddParameter_basic() {
        query.addParameter("param1");
        query.addParameter(0, "param0");

        List<Object> params = query.getParameters();
        assertEquals(2, params.size());
        assertEquals("param0", params.get(0));
        assertEquals("param1", params.get(1));
    }

    // Mocks for MColumn
    private MColumn mockColumn(int displayType, String columnName, boolean secure, boolean encrypted) {
        MColumn col = Mockito.mock(MColumn.class);
        Mockito.when(col.getAD_Reference_ID()).thenReturn(displayType);
        Mockito.when(col.getColumnName()).thenReturn(columnName);
        Mockito.when(col.isSecure()).thenReturn(secure);
        Mockito.when(col.isEncrypted()).thenReturn(encrypted);
        Mockito.when(col.getName()).thenReturn(columnName);
        return col;
    }

    @Test
    public void testAddParameter_nullColumn_doesNothing() {
        query.addParameter(null, "anything");
        assertTrue(query.getParameters().isEmpty());
    }

    @Test
    public void testAddParameter_secureColumn_throws() {
        MColumn col = mockColumn(DisplayType.String, "TestColumn", true, false);
        AdempiereException thrown = assertThrows(AdempiereException.class,
            () -> query.addParameter(col, "value"));
        assertTrue(thrown.getMessage().contains("Cannot query the column"));
    }

    @Test
    public void testAddParameter_encryptedColumn_throws() {
        MColumn col = mockColumn(DisplayType.String, "TestColumn", false, true);
        AdempiereException thrown = assertThrows(AdempiereException.class,
            () -> query.addParameter(col, "value"));
        assertTrue(thrown.getMessage().contains("Cannot query the column"));
    }

    @Test
    public void testAddParameter_integerTypes() {
        // DisplayType.isID returns true for these
        MColumn col = mockColumn(DisplayType.ID, "Some_ID", false, false);
        query.addParameter(col, "123");
        assertEquals(1, query.getParameters().size());
        assertEquals(123, query.getParameters().get(0));

        // Special case: columnName = EntityType or AD_Language should not parse Integer
        MColumn colEntityType = mockColumn(DisplayType.ID, "EntityType", false, false);
        query.addParameter(colEntityType, "456");
        // Should be added as string because of special case
        assertEquals("456", query.getParameters().get(1));

        MColumn colLang = mockColumn(DisplayType.ID, "AD_Language", false, false);
        query.addParameter(colLang, "es_CO");
        assertEquals("es_CO", query.getParameters().get(2));
    }

    @Test
    public void testAddParameter_integerDisplayType() {
        MColumn col = mockColumn(DisplayType.Integer, "Age", false, false);
        query.addParameter(col, "25");
        assertEquals(25, query.getParameters().get(0));
    }

    @Test
    public void testAddParameter_buttonEndingWithID() {
        MColumn col = mockColumn(DisplayType.Button, "User_ID", false, false);
        query.addParameter(col, "55");
        assertEquals(55, query.getParameters().get(0));
    }

    @Test
    public void testAddParameter_numericDisplayType() {
        MColumn col = mockColumn(DisplayType.Number, "Amount", false, false);
        query.addParameter(col, "123.45");
        assertEquals(new BigDecimal("123.45"), query.getParameters().get(0));
    }

    @Test
    public void testAddParameter_dateDisplayType() throws Exception {
        MColumn colDate = mockColumn(DisplayType.Date, "DateCol", false, false);
        String dateString = "2023-07-01";
        query.addParameter(colDate, dateString);
        Object param = query.getParameters().get(0);
        assertTrue(param instanceof Timestamp);

        // Confirm parsed date matches
        SimpleDateFormat sdf = DisplayType.getDateFormat_JDBC();
        Timestamp expected = new Timestamp(sdf.parse(dateString).getTime());
        assertEquals(expected, param);
    }

    @Test
    public void testAddParameter_timestampDisplayType() throws Exception {
        MColumn colTS = mockColumn(DisplayType.DateTime, "TSCol", false, false);
        String tsString = "2023-07-01 10:20:30";
        query.addParameter(colTS, tsString);
        Object param = query.getParameters().get(0);
        assertTrue(param instanceof Timestamp);

        SimpleDateFormat tsFormat = DisplayType.getTimestampFormat_Default();
        Timestamp expected = new Timestamp(tsFormat.parse(tsString).getTime());
        assertEquals(expected, param);
    }

    @Test
    public void testAddParameter_yesNoDisplayType() {
        MColumn col = mockColumn(DisplayType.YesNo, "Active", false, false);
        query.addParameter(col, "Y");
        assertEquals(true, query.getParameters().get(0));
        query.addParameter(col, "true");
        assertEquals(true, query.getParameters().get(1));
        query.addParameter(col, "N");
        assertEquals(false, query.getParameters().get(2));
        query.addParameter(col, "false");
        assertEquals(false, query.getParameters().get(3));
    }

    @Test
    public void testAddParameter_stringDisplayType_withQuotes() {
        MColumn col = mockColumn(DisplayType.String, "Name", false, false);
        query.addParameter(col, "'John'");
        assertEquals("John", query.getParameters().get(0));
    }

    @Test
    public void testAddParameter_stringDisplayType_withoutQuotes_throws() {
        MColumn col = mockColumn(DisplayType.String, "Name", false, false);
        IDempiereRestException ex = assertThrows(IDempiereRestException.class,
            () -> query.addParameter(col, "John"));
        assertTrue(ex.getMessage().contains("String values must be put between single quotes"));
    }

    @Test
    public void testAddParameter_unsupportedDisplayType_addsRaw() {
        MColumn col = mockColumn(9999, "UnknownCol", false, false);
        query.addParameter(col, "rawValue");
        assertEquals("rawValue", query.getParameters().get(0));
    }

    @Test
    public void testAddParameter_invalidNumber_throwsBadRequest() {
        MColumn col = mockColumn(DisplayType.Integer, "Age", false, false);
        IDempiereRestException ex = assertThrows(IDempiereRestException.class,
            () -> query.addParameter(col, "abc"));
        assertTrue(ex.getMessage().contains("Error convertig parameter"));
    }

    @Test
    public void testExtractFromStringValue() {
        assertEquals("John", ConvertedQuery.extractFromStringValue("'John'"));
        assertEquals("", ConvertedQuery.extractFromStringValue("''"));
        assertThrows(StringIndexOutOfBoundsException.class, () -> 
            ConvertedQuery.extractFromStringValue("J"));
    }

}
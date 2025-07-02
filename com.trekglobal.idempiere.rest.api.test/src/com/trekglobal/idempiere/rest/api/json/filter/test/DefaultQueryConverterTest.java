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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.compiere.model.MColumn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.filter.ConvertedQuery;
import com.trekglobal.idempiere.rest.api.json.filter.DefaultQueryConverter;
import com.trekglobal.idempiere.rest.api.json.filter.IQueryConverter;
import com.trekglobal.idempiere.rest.api.json.test.RestTestCase;
import com.trekglobal.idempiere.rest.api.model.MRestView;

public class DefaultQueryConverterTest extends RestTestCase {

	private DefaultQueryConverter converter;

    @Mock
    private MRestView mockView;

    @Mock
    private MColumn mockColumn;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        converter = new DefaultQueryConverter();
    }

    @Test
    public void testSplitSimpleExpression() {
        List<String> parts = DefaultQueryConverter.split("field eq 'value'");
        assertEquals(3, parts.size());
        assertEquals("field", parts.get(0));
        assertEquals("eq", parts.get(1));
        assertEquals("'value'", parts.get(2));
    }

    @Test
    public void testSplitWithNestedParentheses() {
        List<String> parts = DefaultQueryConverter.split("contains(tolower(Name),'admin') and Age gt 25");
        assertEquals(5, parts.size());
        assertEquals("contains(tolower(Name),'admin')", parts.get(0));
        assertEquals("and", parts.get(1));
        assertEquals("Age", parts.get(2));
        assertEquals("gt", parts.get(3));
        assertEquals("25", parts.get(4));
    }

    @Test
    public void testGetQueryConverterReturnsSelf() {
        IQueryConverter q = converter.getQueryConverter("DEFAULT");
        assertSame(converter, q);
    }

    @Test
    public void testGetQueryConverterReturnsNull() {
        assertNull(converter.getQueryConverter("OTHER"));
    }
    
    @Test
    void convertsValidQueryStatementToConvertedQuery() {

        DefaultQueryConverter converter = new DefaultQueryConverter();
        ConvertedQuery result = converter.convertStatement(null, "C_Order", "DocumentNo eq 'SO123456'");

        assertNotNull(result);
        assertTrue(result.getWhereClause().contains("DocumentNo= ?"));
        assertEquals(1, result.getParameters().size());
        assertEquals("SO123456", result.getParameters().get(0));
    }
    
    @Test
    void throwsExceptionForInvalidOperatorInQueryStatement() {
        DefaultQueryConverter converter = new DefaultQueryConverter();

        assertThrows(IDempiereRestException.class, () -> {
            converter.convertStatement(null, "C_Order", "DocumentNo <> 'Value'");
        });
    }
    
    @Test
    void handlesNullQueryStatementGracefully() {
        DefaultQueryConverter converter = new DefaultQueryConverter();
        ConvertedQuery result = converter.convertStatement(null, "C_order", null);

        assertNotNull(result);
        assertTrue(result.getWhereClause().isEmpty());
        assertTrue(result.getParameters().isEmpty());
    }
    
    @Test
    void convertsQueryWithNestedFunctionsSuccessfully() {
        DefaultQueryConverter converter = new DefaultQueryConverter();
        ConvertedQuery result = converter.convertStatement(null, "C_Order", "contains(tolower(DocumentNo),'value')");

        assertNotNull(result);
        assertTrue(result.getWhereClause().contains("lower(DocumentNo) LIKE ?"));
        assertEquals(1, result.getParameters().size());
        assertEquals("%value%", result.getParameters().get(0));
    }
    
    @Test
    void throwsExceptionForInvalidQueryStatementToConvertedQuery() {
        DefaultQueryConverter converter = new DefaultQueryConverter();
        IDempiereRestException ex = assertThrows(IDempiereRestException.class,
                () -> converter.convertStatement(null, "C_Order", "Name eq 'SO123456'"));
        
        assertTrue(ex.getMessage().contains("Invalid column for filter"));
    }
    
    @Test
    void convertsInvalidQueryStatementToConvertedQuery() {
        DefaultQueryConverter converter = new DefaultQueryConverter();
        IDempiereRestException ex = assertThrows(IDempiereRestException.class,
                () -> converter.convertStatement(null, "AD_User", "Password eq '123456'"));
        
        assertTrue(ex.getMessage().contains("Invalid column for filter"));
    }

}
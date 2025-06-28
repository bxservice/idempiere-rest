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
package com.trekglobal.idempiere.rest.api.json.expand.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.compiere.model.I_C_BPartner;
import org.compiere.model.MBPartner;
import org.compiere.util.Env;
import org.idempiere.test.DictionaryIDs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.expand.ExpandParser;
import com.trekglobal.idempiere.rest.api.json.test.RestTestCase;

public class ExpandParserTest extends RestTestCase {

	private MBPartner po;
	
	@BeforeEach
	public void setUp() {
		po = MBPartner.get(Env.getCtx(), DictionaryIDs.C_BPartner.C_AND_W.id);
	}
	
    @Test
    public void testExpandParserEmptyExpandParameter() {
        ExpandParser parser = new ExpandParser(po, null);
        assertTrue(parser.getTableNameSQLStatementMap().isEmpty());
        assertTrue(parser.getTableNameChildArrayMap().isEmpty());
    }

    @Test
    public void testExpandParserInvalidExpandKey() {
    	IDempiereRestException exception = 
    			assertThrows(IDempiereRestException.class, () -> new ExpandParser(po, "InvalidKey"));
    	
        assertEquals(Status.NOT_FOUND, exception.getErrorResponseStatus());
    }
    
    @Test
    public void testExpandParserInvalidDetailTable() {
    	IDempiereRestException exception = 
    			assertThrows(IDempiereRestException.class, () -> new ExpandParser(po, "M_ProductPrice"));
    	
        assertEquals(Status.INTERNAL_SERVER_ERROR, exception.getErrorResponseStatus());
        assertEquals("Wrong detail", exception.getTitle());
    }
    
    @Test
    public void testExpandParserDetailTableWithTrailingSpaces() {
        //Trailing spaces should be ignored and AD_User should be parsed correctly
        ExpandParser parser = new ExpandParser(po, " AD_User ");
        
        assertEquals(1, parser.getTableNameSQLStatementMap().size());
        assertEquals(1, parser.getTableNameChildArrayMap().size());
    }
    
    @Test
    public void testExpandParserMultipleExpand() {
        ExpandParser parser = new ExpandParser(po, "AD_User,C_BP_BankAccount");
        assertEquals(2, parser.getTableNameSQLStatementMap().size());
        assertEquals(2, parser.getTableNameChildArrayMap().size());
    }
    
    @Test
    public void testExpandParserMasterTable() {
        ExpandParser parser = new ExpandParser(po, "C_BP_Group_ID");
        assertEquals(1, parser.getTableNameSQLStatementMap().size());
        assertEquals(1, parser.getTableNameChildArrayMap().size());
    }
    
    @Test
    public void testExpandParserWithOperatorsAndTrailingSpaces() {
        ExpandParser parser = new ExpandParser(po, "C_OrderLine ($select=Line,Linenetamt ; $filter=LineNetAmt gt 1000 ; $orderby=Line), AD_User");
        
        assertEquals(2, parser.getTableNameSQLStatementMap().size());
        assertEquals(2, parser.getTableNameChildArrayMap().size());
    }
    
    /** Unit tests for ExpandParser#getFilterClause(...) */
    @Test
    void regularKey_NoOperators() {
        ExpandParser parser = new ExpandParser(po, "");
        String result = parser.getFilterClause(Collections.emptyList(), "C_BPartner_ID", 100);

        assertEquals("C_BPartner_ID eq 100", result);
    }
    
    @Test
    void recordID_AddsTableId() {
        ExpandParser parser = new ExpandParser(po, "");
        String result = parser.getFilterClause(Collections.emptyList(), "Record_ID", 100);

        assertEquals("Record_ID eq 100 AND AD_Table_ID eq " + I_C_BPartner.Table_ID, result);
    }
    
    @Test
    void addsRequestFilterClause() {
        ExpandParser parser = new ExpandParser(po, "");
        List<String> ops = List.of("$filter=IsActive eq true");

        String result = parser.getFilterClause(ops, "C_BPartner_ID", 100);

        assertEquals("C_BPartner_ID eq 100 AND IsActive eq true", result);
    }
    
    @Test
    void recordIDPlusFilter() {
        ExpandParser parser = new ExpandParser(po, "");
        List<String> ops = List.of("$filter=IsCustomer eq true");
        String result = parser.getFilterClause(ops,"Record_ID",100);

        assertEquals(
            "Record_ID eq 100 AND AD_Table_ID eq "+ + I_C_BPartner.Table_ID + " AND IsCustomer eq true",
            result);
    }
    
}
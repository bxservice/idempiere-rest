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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.GridField;
import org.compiere.model.MProduct;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.X_C_OrderLine;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.trekglobal.idempiere.rest.api.json.DefaultPOSerializer;

public class DefaultPOSerializerTest extends RestTestCase {

	private DefaultPOSerializer serializer;
	
    @BeforeEach
    public void setUp() {
    	serializer = new DefaultPOSerializer();
    }
    
    @Test
    public void testSerializeBasicPOWithExcludes() {
        MProduct po = new MProduct(Env.getCtx(), 50019, null); //Assembly Area
        String[] excludes = { "AD_Org_ID", "Name" };
        
        JsonObject result = serializer.toJson(po, null, new String[0], excludes);
        
        assertEquals(50019, result.get("id").getAsInt());
        assertEquals("034b061d-b387-424d-83e6-2d43d31199f2", result.get("uid").getAsString());
        assertEquals("m_product", result.get("model-name").getAsString());
        assertNotNull(result.get("AD_Client_ID"));
        assertNull(result.get("AD_Org_ID"));
        assertNull(result.get("Name"));
    }
    
    @Test
    public void testSerializeBasicPOWithIncludes() {
        MProduct po = new MProduct(Env.getCtx(), 50019, null); //Assembly Area
        String[] includes = { "AD_Org_ID", "Name", "IsActive" };
        
        JsonObject result = serializer.toJson(po, null, includes, new String[0]);
        
        assertEquals(50019, result.get("id").getAsInt());
        assertEquals("034b061d-b387-424d-83e6-2d43d31199f2", result.get("uid").getAsString());
        assertEquals("m_product", result.get("model-name").getAsString());
        assertNull(result.get("AD_Client_ID"));
        assertNull(result.get("Description"));
        assertNotNull(result.get("AD_Org_ID"));
        assertNotNull(result.get("Name"));
    }

    @Test
    void excludesSecureAndEncryptedColumnsFromSerialization() {
        MUser gardenAdmin = MUser.get(101);

        String[] includes = { "Name", "Password" };

        JsonObject result = serializer.toJson(gardenAdmin, includes, null);

        assertEquals("d4a5259d-edbb-4c28-a7b4-6ffb5ff69442", result.get("uid").getAsString());
        assertEquals("ad_user", result.get("model-name").getAsString());
        assertEquals("GardenAdmin", result.get("Name").getAsString());
        assertNull(result.get("Password"));
    }
    
    @Test
    void throwsExceptionForNonExistingColumnsInDeserialization() {
        MUser gardenAdmin = MUser.get(101);
        JsonObject json = new JsonObject();
        json.addProperty("NonExistingColumn", "Value");

        assertThrows(AdempiereException.class, () -> {
            serializer.fromJson(json, gardenAdmin);
        });
    }
    
    @Test
    public void testLogMessageOnContextPopulation() {
    	TestLogHandler logHandler = new TestLogHandler();
        Logger logger = CLogger.getCLogger(GridField.class);
        logger.addHandler(logHandler);

        // Setup
        DefaultPOSerializer serializer = new DefaultPOSerializer();
      
        // Create test JSON
        JsonObject json = new JsonObject();
        json.add("C_Order_ID", new JsonPrimitive(144));
        json.add("description", new JsonPrimitive("Testing"));
        
        // Execute
        serializer.fromJson(json, MTable.get(X_C_OrderLine.Table_ID), null);
        
        // Verify partial log message
        String logContent = logHandler.getLogContent();
        assertFalse(logContent.contains("WARNING"), "Log should not contain WARNING level");
        assertFalse(logContent.contains("Default SQL variable parse"), "Log should not contain parse error message");
    }
    
    // Custom handler to capture log messages
    private class TestLogHandler extends Handler {
        private StringBuilder log = new StringBuilder();

        @Override
        public void publish(LogRecord record) {
            log.append(record.getLevel()).append(":").append(record.getMessage()).append("\n");
        }

        public String getLogContent() {
            return log.toString();
        }

        @Override
        public void flush() {}

        @Override
        public void close() throws SecurityException {}
    }

}

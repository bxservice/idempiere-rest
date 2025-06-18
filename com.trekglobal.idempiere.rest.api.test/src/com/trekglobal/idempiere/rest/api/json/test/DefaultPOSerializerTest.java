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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProduct;
import org.compiere.model.MUser;
import org.compiere.util.Env;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
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

}

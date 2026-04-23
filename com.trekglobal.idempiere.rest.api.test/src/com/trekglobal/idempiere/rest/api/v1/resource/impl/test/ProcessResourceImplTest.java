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
 * - Elaine Tan                                                               *
 *****************************************************************************/
package com.trekglobal.idempiere.rest.api.v1.resource.impl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import javax.ws.rs.core.Response;

import org.compiere.model.MPInstance;
import org.compiere.util.Env;
import org.idempiere.test.DictionaryIDs;
import org.idempiere.tracking.AuditTraceContext;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trekglobal.idempiere.rest.api.json.test.RestTestCase;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.ProcessResourceImpl;

public class ProcessResourceImplTest extends RestTestCase {

	@Test
	void runJobWithExternalTraceId() {
		ProcessResourceImpl processResource = new ProcessResourceImpl();
		
		String externalTraceId = UUID.randomUUID().toString();
		AuditTraceContext.setExternalTraceId(externalTraceId);				
		try {
			String processSlug = "c_bpartner-validate";
			String jsonText = "{\"C_BPartner_ID\": " + DictionaryIDs.C_BPartner.SEED_FARM.id 
					+ ", \"notification-type\": \"N\" }";
			Response response = processResource.runJob(processSlug, jsonText);
			assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
			String jsonString = response.getEntity().toString();
			JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
			int id = jsonObject.getAsJsonObject().get("id").getAsInt();
			assertTrue(id > 0, "Failed to create background process instance");
			
			MPInstance pinstance = new MPInstance(Env.getCtx(), id, null);
	        assertEquals(id, pinstance.get_ID(), "Failed to retrieve background process instance");
			assertEquals(externalTraceId, pinstance.getExternalTraceId(), "Unexpected ExternalTraceId");
		} finally {
			AuditTraceContext.clear();
		}

	}

}
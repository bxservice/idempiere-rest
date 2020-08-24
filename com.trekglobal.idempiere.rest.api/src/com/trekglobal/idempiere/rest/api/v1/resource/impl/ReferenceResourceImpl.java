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
* - BX Service GmbH                                                   *
* - Diego Ruiz                                                        *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.v1.resource.impl;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.MRefList;
import org.compiere.model.MReference;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.ValueNamePair;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.json.TypeConverterUtils;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.v1.resource.ReferenceResource;

/**
 * 
 * @author druiz
 *
 */
public class ReferenceResourceImpl implements ReferenceResource {

	public ReferenceResourceImpl() {
	}

	@Override
	public Response getList(String refID) {

		boolean isUUID = TypeConverterUtils.isUUID(refID);
		String keyColumn = isUUID ? PO.getUUIDColumnName(MReference.Table_Name) : 
			MReference.COLUMNNAME_AD_Reference_ID;

		Query query = new Query(Env.getCtx(), MReference.Table_Name, keyColumn + "=?", null);
		MReference ref = isUUID ? query.setParameters(refID).first()
				   : query.setParameters(Integer.parseInt(refID)).first();

		if (ref == null) {
			return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid reference id").append("No match found for AD_Reference_ID: ").append(refID).build().toString())
					.build();
    	} else if (MReference.VALIDATIONTYPE_ListValidation.equals(ref.getValidationType())) {
        	IPOSerializer serializer = IPOSerializer.getPOSerializer(MReference.Table_Name, MTable.getClass(MReference.Table_Name));
        	JsonObject referenceJsonObject = serializer.toJson(ref, new String[] {"AD_Reference_ID", "AD_Reference_UU", "Name", "Description", "Help", "ValidationType", "VFormat"}, null);
        	JsonArray refListArray = new JsonArray();
        	for(ValueNamePair refList : MRefList.getList(Env.getCtx(), ref.getAD_Reference_ID(), false)) {
        		JsonObject json = new JsonObject();
    			json.addProperty("value", refList.getValue());
    			json.addProperty("name", refList.getName());
    			refListArray.add(json);
        	}
        	referenceJsonObject.add("reflist", refListArray);

        	return Response.ok(referenceJsonObject.toString()).build();
    	} else {
    		//Reference TableValidation and DataValidation not implemented
    		return Response.status(Status.NOT_IMPLEMENTED)
    				.entity(new ErrorBuilder().status(Status.NOT_IMPLEMENTED).title("References with table or data validation are not implemented.").append("Not implemented AD_Reference_ID: ").append(refID).build().toString())
    				.build();
    	}
	}

}
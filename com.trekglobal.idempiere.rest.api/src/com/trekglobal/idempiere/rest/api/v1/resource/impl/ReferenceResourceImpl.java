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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.MColumn;
import org.compiere.model.MRefList;
import org.compiere.model.MRefTable;
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

		MReference ref = (MReference) TypeConverterUtils.getPO(MReference.Table_Name, refID, false, false);

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
    	} else if (MReference.VALIDATIONTYPE_TableValidation.equals(ref.getValidationType())) {
    		
    		MRefTable refTable = MRefTable.get(ref.getAD_Reference_ID());
    		if (refTable == null || refTable.get_ID() == 0)
    			return Response.status(Status.NOT_FOUND)
    					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid reference table id").append("No ref table match found for AD_Reference_ID: ").append(refID).build().toString())
    					.build();

    		MTable table = new MTable(Env.getCtx(), refTable.getAD_Table_ID(), null);
    		Query query = new Query(Env.getCtx(), table, refTable.getWhereClause(), null);
    		List<PO> list = query
    				.setApplyAccessFilter(true, false)
    				.setOnlyActiveRecords(true)
    				.setOrderBy(refTable.getOrderByClause())
    				.list();
    		
        	IPOSerializer serializer = IPOSerializer.getPOSerializer(MReference.Table_Name, MTable.getClass(table.getTableName()));
        	JsonArray array = new JsonArray();
    		if (list != null && !list.isEmpty()) {
            	ArrayList<String> includes = new ArrayList<String>();
            	
            	includes.add(MColumn.getColumnName(Env.getCtx(), refTable.getAD_Key()));
           		includes.add(MColumn.getColumnName(Env.getCtx(), refTable.getAD_Display()));
            	if (refTable.isValueDisplayed())
            		includes.add("Value");

    			for (PO po : list) {
    				JsonObject json = serializer.toJson(po, includes.toArray(new String[includes.size()]), null);
    				array.add(json);
    			}
    		}

			JsonObject json = new JsonObject();
			json.add("reftable", array);
    		return Response.ok(json.toString()).build();
    	} else {
    		//Reference DataValidation not implemented
    		return Response.status(Status.NOT_IMPLEMENTED)
    				.entity(new ErrorBuilder().status(Status.NOT_IMPLEMENTED).title("References with data validation are not implemented.").append("Not implemented AD_Reference_ID: ").append(refID).build().toString())
    				.build();
    	}
	}

}
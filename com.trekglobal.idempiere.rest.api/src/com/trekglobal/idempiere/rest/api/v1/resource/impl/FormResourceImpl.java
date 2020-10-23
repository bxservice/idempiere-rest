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

import java.util.List;
import java.util.logging.Level;

import javax.ws.rs.core.Response;

import org.compiere.model.MForm;
import org.compiere.model.MRole;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.json.TypeConverterUtils;
import com.trekglobal.idempiere.rest.api.json.filter.ConvertedQuery;
import com.trekglobal.idempiere.rest.api.json.filter.IQueryConverter;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.v1.resource.FormResource;

/**
 * 
 * @author druiz
 *
 */
public class FormResourceImpl implements FormResource {

	private final static CLogger log = CLogger.getCLogger(FormResourceImpl.class);

	public FormResourceImpl() {
	}

	@Override
	public Response getForms(String filter) {
		JsonArray formArray = new JsonArray();
		IQueryConverter converter = IQueryConverter.getQueryConverter("DEFAULT");
		try {
			ConvertedQuery convertedStatement = converter.convertStatement(MForm.Table_Name, filter);
			
			if (log.isLoggable(Level.INFO)) log.info("Where Clause: " + convertedStatement.getWhereClause());
			
			Query query = new Query(Env.getCtx(), MForm.Table_Name, convertedStatement.getWhereClause(), null);
			query.setApplyAccessFilter(true).setOnlyActiveRecords(true).setOrderBy("Name");
			query.setParameters(convertedStatement.getParameters());
			query.setOrderBy(" AD_Form_ID");
			int rowCount = query.count();

			List<MForm> forms = query.list();
			MRole role = MRole.getDefault();
			IPOSerializer serializer = IPOSerializer.getPOSerializer(MForm.Table_Name, MTable.getClass(MForm.Table_Name));
			for(MForm form : forms) {
				if (role.getFormAccess(form.getAD_Form_ID()) == null)
					continue;
					
				JsonObject jsonObject = serializer.toJson(form, new String[] {"AD_Form_ID", "AD_Form_UU", "Name", "Description", "Help", "Classname", "EntityType"}, null);
				jsonObject.addProperty("slug", TypeConverterUtils.slugify(form.getName()));
				formArray.add(jsonObject);
				
			}
			return Response.ok(formArray.toString())
					.header("X-Row-Count", rowCount)
					.build();
		} catch (Exception ex) {
			return Response.status(converter.getResponseStatus())
					.entity(new ErrorBuilder().status(converter.getResponseStatus())
							.title("GET Error")
							.append("Get forms with exception: ")
							.append(ex.getMessage())
							.build().toString())
					.build();
		}

	}
}

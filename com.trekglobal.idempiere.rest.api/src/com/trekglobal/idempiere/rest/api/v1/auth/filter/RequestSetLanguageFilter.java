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
* - Trek Global Corporation                                           *
* - Heng Sin Low                                                      *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.v1.auth.filter;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.compiere.model.MLanguage;
import org.compiere.util.Env;
import org.compiere.util.Util;

@Provider
@Priority(Priorities.ENTITY_CODER)
/**
 * Request Set Language Filter
 * Set Language Context based on locale query parameter
 * @author Igor Pojzl, Cloudempiere
 *
 */
public class RequestSetLanguageFilter implements ContainerRequestFilter {
	
	private final String LANGUAGE_KEY = "locale";

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		MultivaluedMap<String, String> queryParams = requestContext.getUriInfo().getQueryParameters();
		String AD_Language = queryParams.getFirst(LANGUAGE_KEY);
		if(Util.isEmpty(AD_Language))
			return;
		
		if(isValidLanguage(AD_Language))
			Env.setContext(Env.getCtx(), Env.LANGUAGE, AD_Language);	// Set Language
	}
	
	/**
	 * Validate is AD_Language String exist and it is System Language
	 * @param AD_Language
	 * @return true if Valid else false
	 */
	private boolean isValidLanguage(String AD_Language) {
		MLanguage language = MLanguage.get(Env.getCtx(), AD_Language);
		return language != null && language.isSystemLanguage();	// Language Exists and It is System Language
	} 
}

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
package com.trekglobal.idempiere.rest.api.v1;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.jackson.JacksonFeature;

import com.trekglobal.idempiere.rest.api.v1.auth.filter.RequestFilter;
import com.trekglobal.idempiere.rest.api.v1.auth.filter.ResponseFilter;
import com.trekglobal.idempiere.rest.api.v1.auth.impl.AuthServiceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.CacheResourceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.FileResourceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.FormResourceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.InfoResourceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.ModelResourceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.NodeResourceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.ProcessResourceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.ReferenceResourceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.ServerResourceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.StatusLineResourceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.WindowResourceImpl;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.WorkflowResourceImpl;

/**
 * @author hengsin
 *
 */
public class ApplicationV1 extends Application {

	/**
	 * 
	 */
	public ApplicationV1() {
	}

	@Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<Class<?>>();
        
        classes.add(AuthServiceImpl.class);
        classes.add(RequestFilter.class);
        classes.add(ResponseFilter.class);
        classes.add(JacksonFeature.class);
        classes.add(ModelResourceImpl.class);
        classes.add(WindowResourceImpl.class);
        classes.add(FormResourceImpl.class);
        classes.add(ProcessResourceImpl.class);
        classes.add(ReferenceResourceImpl.class);
        classes.add(FileResourceImpl.class);
        classes.add(CacheResourceImpl.class);
        classes.add(NodeResourceImpl.class);
        classes.add(ServerResourceImpl.class);
        classes.add(InfoResourceImpl.class);
        classes.add(WorkflowResourceImpl.class);
        classes.add(StatusLineResourceImpl.class);
        
        return classes;
    }	
}

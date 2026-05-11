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
package com.trekglobal.idempiere.rest.api;

import org.adempiere.base.IMappedColumnCalloutFactory;
import org.adempiere.plugin.utils.Incremental2PackActivator;
import org.idempiere.model.IMappedModelFactory;
import org.idempiere.process.IMappedProcessFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.trekglobal.idempiere.rest.api.webhook.WebhookDispatcher;

/**
 * 
 * @author hengsin
 *
 */
@Component(immediate = true, service = {})
public class Activator extends Incremental2PackActivator {
	
	@Reference(service = IMappedModelFactory.class, cardinality = ReferenceCardinality.MANDATORY)
	private IMappedModelFactory mappedModelFactory;
	
	@Reference(service = IMappedProcessFactory.class, cardinality = ReferenceCardinality.MANDATORY)
	private IMappedProcessFactory mappedProcessFactory;

	@Reference(service = IMappedColumnCalloutFactory.class, cardinality = ReferenceCardinality.MANDATORY)
	private IMappedColumnCalloutFactory mappedColumnCalloutFactory;

	@Override
	public void start(BundleContext context) throws Exception {		
		super.start(context);
	}

	@Activate
	public void activate(BundleContext context) {
		mappedModelFactory.scan(context, "com.trekglobal.idempiere.rest.api.model");
		mappedProcessFactory.scan(context, "com.trekglobal.idempiere.rest.api.process");
		mappedColumnCalloutFactory.scan(context, "com.trekglobal.idempiere.rest.api.model");

	}

	public void deactivate(BundleContext context) {
		
	}

	@Override
	protected void afterPackIn() {
		super.afterPackIn();
		context.registerService(Activator.class, this, null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		WebhookDispatcher.shutdown();
		super.stop(context);
	}

}

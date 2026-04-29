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

import org.adempiere.base.Core;
import org.adempiere.plugin.utils.Incremental2PackActivator;
import org.osgi.framework.BundleContext;

import com.trekglobal.idempiere.rest.api.webhook.WebhookDispatcher;

/**
 * 
 * @author hengsin
 *
 */
public class Activator extends Incremental2PackActivator {
	
	@Override
	public void start(BundleContext context) throws Exception {
		Core.getMappedModelFactory().scan(context, "com.trekglobal.idempiere.rest.api.model");
		Core.getMappedProcessFactory().scan(context, "com.trekglobal.idempiere.rest.api.process");
		Core.getMappedColumnCalloutFactory().scan(context, "com.trekglobal.idempiere.rest.api.model");

		super.start(context);
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

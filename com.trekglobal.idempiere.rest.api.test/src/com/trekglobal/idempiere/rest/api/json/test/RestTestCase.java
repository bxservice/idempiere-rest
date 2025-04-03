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
package com.trekglobal.idempiere.rest.api.json.test;

import java.util.concurrent.CountDownLatch;

import org.idempiere.test.AbstractTestCase;
import org.junit.jupiter.api.BeforeAll;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public abstract class RestTestCase extends AbstractTestCase {

	private static final CountDownLatch serviceLatch = new CountDownLatch(1);
	
	public RestTestCase() {
	}

	@BeforeAll
	public static void beforeAll() {
		 ServiceTracker<com.trekglobal.idempiere.rest.api.Activator, com.trekglobal.idempiere.rest.api.Activator> tracker = new ServiceTracker<>(
	                Activator.context, com.trekglobal.idempiere.rest.api.Activator.class.getName(), null) {

	            @Override
	            public com.trekglobal.idempiere.rest.api.Activator addingService(ServiceReference<com.trekglobal.idempiere.rest.api.Activator> reference) {
	            	com.trekglobal.idempiere.rest.api.Activator service = super.addingService(reference);
	                serviceLatch.countDown(); // Signal that afterPackIn() is done
	                return service;
	            }

	            @Override
	            public void removedService(ServiceReference<com.trekglobal.idempiere.rest.api.Activator> reference, com.trekglobal.idempiere.rest.api.Activator service) {
	                super.removedService(reference, service);
	            }
	        };
	    tracker.open(); // Start tracking
	    try {
	    	//wait for afterPackIn to finish
			serviceLatch.await();
		} catch (InterruptedException e) {
		}
	}
}

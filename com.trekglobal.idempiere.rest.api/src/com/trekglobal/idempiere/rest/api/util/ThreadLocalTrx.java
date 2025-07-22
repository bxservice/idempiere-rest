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
package com.trekglobal.idempiere.rest.api.util;

import org.compiere.util.Trx;

public final class ThreadLocalTrx implements AutoCloseable {

	private static final InheritableThreadLocal<String> trxName = new InheritableThreadLocal<>();
	
	public ThreadLocalTrx(String prefix) {
		start(prefix);
	}
	
	/**
	 * Set transaction name for the current thread.
	 * @param name transaction name
	 */
	public static void start(String prefix) {
		if (trxName.get() == null) {
			Trx trx = Trx.get(Trx.createTrxName(prefix), true);
			trxName.set(trx.getTrxName());
			trx.start();
		}
	}

	/**
	 * Get transaction name for the current thread.
	 * @return transaction name
	 */
	public static String getTrxName() {
		return trxName.get();
	}

	@Override
	public void close() {
		String name = trxName.get();
		if (name != null) {
			Trx trx = Trx.get(name, false);
			if (trx != null) {
				trx.close();
			}
			trxName.remove();
		}		
	}	
}

/***********************************************************************
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
 * - Carlos Ruiz - globalqss - BX Service                              *
 **********************************************************************/

package com.trekglobal.idempiere.rest.api.model;

import java.util.ArrayList;
import java.util.Arrays;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MClient;
import org.compiere.model.MRole;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.osgi.service.event.Event;

/**
 * This event handler for Hibiscus integration
 * 
 * @author Carlos Ruiz - globalqss - BX Service
 */
public class RESTEventHandler extends AbstractEventHandler {
	/** Logger */
	private static CLogger log = CLogger.getCLogger(RESTEventHandler.class);

	/**
	 * Initialize Validation
	 * Validate password change on AD_User
	 */
	@Override
	protected void initialize() {
		log.info("");

		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MUser.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MRole.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MClient.Table_Name);
	} // initialize

	/**
	 * Model Change of a monitored Table.
	 * 
	 * @param event
	 * @exception Exception if the recipient wishes the change to be not accept.
	 */
	@Override
	protected void doHandleEvent(Event event) {
		String type = event.getTopic();

		PO po = getPO(event);
		log.info(po + " Type: " + type);

		if (po instanceof MUser && type.equals(IEventTopics.PO_AFTER_CHANGE)) {
			if (po.is_ValueChanged(MUser.COLUMNNAME_Password)) {
				MUser user = (MUser) po;
				expireTokens(user);
			} else if (po.is_ValueChanged(MUser.COLUMNNAME_IsActive)) {
				MUser user = (MUser) po;
				if (!user.isActive()) {
					MAuthToken.deactivateTokens(user.getAD_User_ID(), -1, -1, user.get_TrxName());
					expireTokens(user);
				}
			}
		} else if (po instanceof MRole && type.equals(IEventTopics.PO_AFTER_CHANGE)) {
			if (po.is_ValueChanged(MRole.COLUMNNAME_IsActive)) {
				MRole role = (MRole) po;
				if (!role.isActive())
					MAuthToken.deactivateTokens(-1, role.getAD_Role_ID(), -1, role.get_TrxName());
			}
		} else if (po instanceof MClient && type.equals(IEventTopics.PO_AFTER_CHANGE)) {
			if (po.is_ValueChanged(MClient.COLUMNNAME_IsActive)) {
				MClient client = (MClient) po;
				if (!client.isActive()) {
					MAuthToken.deactivateTokens(-1, -1, client.getAD_Client_ID(), client.get_TrxName());
					MRefreshToken.expireTokens("AD_Client_ID=?", MRefreshToken.REST_REVOKECAUSE_ManualExpire, new ArrayList<>(Arrays.asList(client.getAD_Client_ID())));
				}
			}
		}		
	} // doHandleEvent

	/**
	 * Expire all user tokens because of password change
	 * @param user
	 */
	public void expireTokens(MUser user) {
		log.info("");
		MRefreshToken.expireTokens("CreatedBy=?", MRefreshToken.REST_REVOKECAUSE_PasswordChange, new ArrayList<>(Arrays.asList(user.getAD_User_ID())));
	} // expireTokens

}
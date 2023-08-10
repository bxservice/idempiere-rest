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
package com.trekglobal.idempiere.rest.api.model;

import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.util.Env;

/**
 * @author hengsin
 */
@Callout(tableName = I_REST_OIDCService.Table_Name, columnName = I_REST_OIDCService.COLUMNNAME_OIDC_AuthorityURL)
public class CalloutRestOIDCService implements IColumnCallout {

	/**
	 * Default constructor
	 */
	public CalloutRestOIDCService() {
	}

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		if (value != null) {
			String url = value.toString();
			Integer providerId = (Integer) mTab.getValue(I_REST_OIDCService.COLUMNNAME_REST_OIDCProvider_ID);
			if (providerId != null) {
				MOIDCProvider provider = new MOIDCProvider(Env.getCtx(), providerId, null);
				String configurationURL = provider.getOIDC_ConfigurationURL();
				configurationURL = configurationURL.replace("@"+I_REST_OIDCService.COLUMNNAME_OIDC_AuthorityURL+"@", url);
				mTab.setValue(I_REST_OIDCService.COLUMNNAME_OIDC_ConfigurationURL, configurationURL);
				mTab.setValue(I_REST_OIDCService.COLUMNNAME_OIDC_IssuerURL, url);
			}
		}
		return null;
	}

}

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
* - Elaine Tan	                                                      *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.oidc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.compiere.model.MOrg;
import org.compiere.model.MRole;
import org.compiere.model.MUser;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * @author etantg
 */
public abstract class AbstractOIDCProvider implements IOIDCProvider {
	/**
	 * @param AD_Client_ID
	 * @param AD_Role_ID
	 * @param user
	 * @return AD_Role_ID if user+AD_Role_ID has only 1 org, otherwise -1
	 */
	public int getSingleOrgIdOnly(int AD_Client_ID, int AD_Role_ID, MUser user) {
		Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, AD_Client_ID);
		MRole role = new MRole(Env.getCtx(), AD_Role_ID, null);
		role.setAD_User_ID(user.get_ID());
		MOrg[] orgs = MOrg.getOfClient(AD_Client_ID);
		int AD_Org_ID = -1;
		for(MOrg org : orgs) {
			if (role.isOrgAccess(org.getAD_Org_ID(), false)) {
				if (AD_Org_ID >= 0)
					return -1;
				AD_Org_ID = org.getAD_Org_ID();
			}
		}
		return AD_Org_ID;
	}

	/**
	 * @param AD_Client_ID
	 * @param user
	 * @return AD_Role_ID if user has only 1 role, otherwise -1
	 */
	public int getSingleRoleIDOnly(int AD_Client_ID, MUser user) {
		StringBuilder sql = new StringBuilder("SELECT AD_Role_ID FROM AD_User_Roles WHERE IsActive='Y' AND AD_Client_ID=? AND AD_User_ID=?");
		try (PreparedStatement stmt = DB.prepareStatement(sql.toString(), (String)null)) {
			stmt.setInt(1, AD_Client_ID);
			stmt.setInt(2, user.get_ID());
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				int AD_Role_ID = rs.getInt(1);
				if (!rs.next())
					return AD_Role_ID;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}
}

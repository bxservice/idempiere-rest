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
**********************************************************************/
package com.trekglobal.idempiere.rest.api.v1.resource.impl.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.ws.rs.core.Response;

import org.compiere.model.SystemIDs;
import org.compiere.util.Env;
import org.junit.jupiter.api.Test;

import com.trekglobal.idempiere.rest.api.json.test.RestTestCase;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.MenuTreeResourceImpl;

/**
 * Tests for {@code GET v1/menutree/me} (#541).
 * @author Cloudempiere
 */
public class MenuTreeResourceImplTest extends RestTestCase {

	@Test
	void sessionMenuTreeIdResolvesTheMenuTree() {
		int treeId = MenuTreeResourceImpl.sessionMenuTreeId(Env.getAD_Role_ID(Env.getCtx()));
		assertTrue(treeId > 0, "No menu tree resolved for the session role");
	}

	@Test
	void unknownRoleFallsBackToPrimaryMenu() {
		assertEquals(SystemIDs.TREE_MENUPRIMARY, MenuTreeResourceImpl.sessionMenuTreeId(-1));
	}

	@Test
	void myMenuIsTheMenuTreeOfTheSessionRole() {
		MenuTreeResourceImpl resource = new MenuTreeResourceImpl();
		int treeId = MenuTreeResourceImpl.sessionMenuTreeId(Env.getAD_Role_ID(Env.getCtx()));

		Response mine = resource.getMyMenu();
		Response byId = resource.getMenu(String.valueOf(treeId));

		assertEquals(Response.Status.OK.getStatusCode(), mine.getStatus());
		assertEquals(byId.getEntity().toString(), mine.getEntity().toString());
	}

}

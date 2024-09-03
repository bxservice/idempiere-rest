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
 * - BX Service GmbH                                                   *
 * - Carlos Ruiz                                                       *
 **********************************************************************/
package com.trekglobal.idempiere.rest.api.v1.resource.impl;

import java.util.Enumeration;
import java.util.logging.Level;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.MMenu;
import org.compiere.model.MTable;
import org.compiere.model.MTree;
import org.compiere.model.MTreeNode;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.IDempiereRestException;
import com.trekglobal.idempiere.rest.api.json.IPOSerializer;
import com.trekglobal.idempiere.rest.api.json.RestUtils;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.v1.resource.MenuTreeResource;

/**
 * 
 * @author Carlos Ruiz
 *
 */
public class MenuTreeResourceImpl implements MenuTreeResource {

	private final static CLogger log = CLogger.getCLogger(MenuTreeResourceImpl.class);

	public MenuTreeResourceImpl() {
	}

	@Override
	public Response getMenu(String id) {
		try {
			boolean isUUID = RestUtils.isUUID(id);
			int menuTreeId = isUUID ? getMenuTreeID(id) : Integer.valueOf(id);

			JsonObject jsonRoot = new JsonObject(); 
			MTree mTree = new MTree(Env.getCtx(), menuTreeId, false, true, null);
			MTreeNode rootNode = mTree.getRoot();
        	jsonRoot.addProperty(MMenu.COLUMNNAME_Name, rootNode.getName());
        	jsonRoot.addProperty(MMenu.COLUMNNAME_Description, rootNode.getDescription());
            JsonArray menuEntries = new JsonArray();
            generateMenu(menuEntries, rootNode);
            jsonRoot.add("entries", menuEntries);
			return Response.ok(jsonRoot.toString()).build();
		} catch (Exception ex) {
			Status status = Status.INTERNAL_SERVER_ERROR;
			if (ex instanceof IDempiereRestException)
				status = ((IDempiereRestException) ex).getErrorResponseStatus();

			log.log(Level.SEVERE, ex.getMessage(), ex);
			return Response.status(status)
					.entity(new ErrorBuilder().status(status)
							.title("GET Error")
							.append("Get menu tree with exception: ")
							.append(ex.getMessage())
							.build().toString())
					.build();
		}
	}
	
	private void generateMenu(JsonArray menuEntries, MTreeNode mNode) {
		final String[] includedColumns = {
			MMenu.COLUMNNAME_Name,
			MMenu.COLUMNNAME_Description,
			MMenu.COLUMNNAME_IsSOTrx,
			MMenu.COLUMNNAME_PredefinedContextVariables,
			MMenu.COLUMNNAME_EntityType,
			MMenu.COLUMNNAME_Action,
			MMenu.COLUMNNAME_AD_Window_ID,
			MMenu.COLUMNNAME_AD_Workflow_ID,
			MMenu.COLUMNNAME_AD_Task_ID,
			MMenu.COLUMNNAME_AD_Process_ID,
			MMenu.COLUMNNAME_AD_Form_ID,
			MMenu.COLUMNNAME_AD_InfoWindow_ID
		};
        Enumeration<?> nodeEnum = mNode.children();
        while (nodeEnum.hasMoreElements()) {
            MTreeNode mChildNode = (MTreeNode)nodeEnum.nextElement();
            if (mChildNode.getChildCount() != 0) {
                MMenu menu = MMenu.get(mChildNode.getNode_ID());
                IPOSerializer serializer = IPOSerializer.getPOSerializer(MMenu.Table_Name, MTable.getClass(MMenu.Table_Name));
                JsonObject nodeMenuObject = serializer.toJson(menu, includedColumns, null);
                JsonArray childMenuEntries = new JsonArray();
                generateMenu(childMenuEntries, mChildNode);
                nodeMenuObject.add("entries", childMenuEntries);
                menuEntries.add(nodeMenuObject);
            } else {
                if (mChildNode.getNode_ID() > 0) {
                    MMenu entry = MMenu.get(mChildNode.getNode_ID());
                    IPOSerializer serializer = IPOSerializer.getPOSerializer(MMenu.Table_Name, MTable.getClass(MMenu.Table_Name));
                    JsonObject nodeEntryObject = serializer.toJson(entry, includedColumns, null);
                    menuEntries.add(nodeEntryObject);
                }
            }
        }
	}
	
	private int getMenuTreeID(String uuid) {
		String sql = "SELECT AD_Tree_ID FROM AD_Tree WHERE AD_Tree_UU = ?";
		return DB.getSQLValue(null, sql, uuid);
	}

}


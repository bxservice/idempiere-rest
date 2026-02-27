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
* - Ray Lee                                                          *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.v1.resource.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.GridWindow;
import org.compiere.model.MQuery;
import org.compiere.model.MRole;
import org.compiere.model.MTab;
import org.compiere.model.MWindow;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.trekglobal.idempiere.rest.api.json.TypeConverterUtils;
import com.trekglobal.idempiere.rest.api.util.ErrorBuilder;
import com.trekglobal.idempiere.rest.api.v1.resource.CalloutResource;

/**
 * Implementation for callout execution endpoint.
 * Fires iDempiere callouts without saving the record,
 * returning only the fields that were changed by the callout.
 */
public class CalloutResourceImpl implements CalloutResource {

	private static final CLogger log = CLogger.getCLogger(CalloutResourceImpl.class);

	public CalloutResourceImpl() {
	}

	@Override
	public Response fireCallout(String windowSlug, String tabSlug, String jsonText) {
		Properties ctx = Env.getCtx();

		try {
			// 1. Parse and validate request body
			if (Util.isEmpty(jsonText, true)) {
				return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Invalid request").append("Request body is empty").build().toString())
					.build();
			}
			JsonObject request = JsonParser.parseString(jsonText).getAsJsonObject();
			if (!request.has("columnName") || !request.get("columnName").isJsonPrimitive()
					|| !request.get("columnName").getAsJsonPrimitive().isString()) {
				return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Invalid request").append("Missing or invalid required field: columnName (expected string)").build().toString())
					.build();
			}
			if (!request.has("value")) {
				return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Invalid request").append("Missing required field: value").build().toString())
					.build();
			}
			if (request.has("record") && !request.get("record").isJsonObject()) {
				return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Invalid request").append("Invalid field: record (expected object)").build().toString())
					.build();
			}
			if (request.has("recordId") && !(request.get("recordId").isJsonPrimitive()
					&& request.get("recordId").getAsJsonPrimitive().isNumber())) {
				return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Invalid request").append("Invalid field: recordId (expected number)").build().toString())
					.build();
			}
			String columnName = request.get("columnName").getAsString();
			JsonElement valueElement = request.get("value");
			JsonObject record = request.has("record") ? request.getAsJsonObject("record") : new JsonObject();
			int recordId = request.has("recordId") ? request.get("recordId").getAsInt() : 0;
			if (recordId < 0) {
				return Response.status(Status.BAD_REQUEST)
					.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Invalid request").append("Invalid field: recordId must be >= 0").build().toString())
					.build();
			}

			// 2. Resolve window from slug
			MRole role = MRole.getDefault();
			Query query = new Query(ctx, MWindow.Table_Name, "slugify(name)=?", null);
			query.setApplyAccessFilter(true).setOnlyActiveRecords(true);
			query.setParameters(windowSlug);
			MWindow window = query.first();
			if (window == null) {
				return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid window name").append("No match found for window name: ").append(windowSlug).build().toString())
					.build();
			}
			Boolean windowAccess = role.getWindowAccess(window.getAD_Window_ID());
			if (windowAccess == null || windowAccess.booleanValue() == false) {
				return Response.status(Status.FORBIDDEN)
					.entity(new ErrorBuilder().status(Status.FORBIDDEN).title("Access denied").append("Access denied for window: ").append(windowSlug).build().toString())
					.build();
			}

			// 3. Find target tab ID via MTab (base language name for slug matching)
			MTab[] mtabs = window.getTabs(false, null);
			int targetTabId = 0;
			for (MTab mtab : mtabs) {
				if (TypeConverterUtils.slugify(mtab.getName()).equals(tabSlug)) {
					targetTabId = mtab.getAD_Tab_ID();
					break;
				}
			}
			if (targetTabId == 0) {
				return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid tab name").append("No match found for tab name: ").append(tabSlug).build().toString())
					.build();
			}

			// 4. Set window context and find matching GridTab by AD_Tab_ID
			Env.setContext(ctx, 1, "IsSOTrx", window.isSOTrx());
			GridWindow gridWindow = GridWindow.get(ctx, 1, window.getAD_Window_ID());
			GridTab targetTab = null;
			int targetTabIndex = -1;

			for (int i = 0; i < gridWindow.getTabCount(); i++) {
				GridTab gridTab = gridWindow.getTab(i);
				if (gridTab.getAD_Tab_ID() == targetTabId) {
					targetTab = gridTab;
					targetTabIndex = i;
					break;
				}
			}
			if (targetTab == null) {
				return Response.status(Status.NOT_FOUND)
					.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Invalid tab name").append("No match found for tab name: ").append(tabSlug).build().toString())
					.build();
			}

			// 5. Initialize the tab
			if (!gridWindow.isTabInitialized(targetTabIndex)) {
				gridWindow.initTab(targetTabIndex);
			}

			// 6. Load existing record or create new
			if (recordId > 0) {
				String keyColumn = targetTab.getTableModel().getKeyColumnName();
				if (Util.isEmpty(keyColumn, true)) {
					return Response.status(Status.BAD_REQUEST)
						.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Invalid table").append("Table has no single key column: ").append(targetTab.getTableName()).build().toString())
						.build();
				}
				MQuery gridTabQuery = new MQuery(targetTab.getTableName());
				gridTabQuery.addRestriction(keyColumn, "=", recordId);
				targetTab.setQuery(gridTabQuery);
				targetTab.query(false);
				if (targetTab.getRowCount() == 0) {
					return Response.status(Status.NOT_FOUND)
						.entity(new ErrorBuilder().status(Status.NOT_FOUND).title("Record not found").append("No record found with ID: ").append(String.valueOf(recordId)).build().toString())
						.build();
				}
			} else {
				MQuery emptyQuery = new MQuery("");
				emptyQuery.addRestriction("1=2");
				emptyQuery.setRecordCount(0);
				targetTab.setQuery(emptyQuery);
				targetTab.query(false);
				if (!targetTab.dataNew(false)) {
					return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Initialization error").append("Unable to initialize new record context").build().toString())
						.build();
				}
			}

			// 7. Set all record fields, fire callout, and collect changes
			boolean tabMutated = (recordId <= 0);
			try {
				GridField triggerField = null;
				for (int i = 0; i < targetTab.getFieldCount(); i++) {
					GridField gf = targetTab.getField(i);
					String colName = gf.getColumnName();
					if (colName.equals(columnName)) {
						triggerField = gf;
						continue;
					}
					if (record.has(colName)) {
						JsonElement je = record.get(colName);
						if (je.isJsonNull()) {
							targetTab.setValue(gf, null);
						} else {
							Object val = TypeConverterUtils.fromJsonValue(gf, je);
							if (val == null) {
								return Response.status(Status.BAD_REQUEST)
									.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Invalid value").append("Invalid value for field: ").append(colName).build().toString())
									.build();
							}
							targetTab.setValue(gf, val);
						}
						tabMutated = true;
					}
				}

				if (triggerField == null) {
					return Response.status(Status.BAD_REQUEST)
						.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Invalid column").append("Column not found in tab: ").append(columnName).build().toString())
						.build();
				}

				// 7. Snapshot all field values BEFORE the trigger
				Map<String, Object> beforeValues = new HashMap<>();
				for (int i = 0; i < targetTab.getFieldCount(); i++) {
					GridField gf = targetTab.getField(i);
					beforeValues.put(gf.getColumnName(), gf.getValue());
				}

				// 8. Set the trigger column — fires the callout
				Object triggerValue = TypeConverterUtils.fromJsonValue(triggerField, valueElement);
				if (triggerValue == null && !valueElement.isJsonNull()) {
					return Response.status(Status.BAD_REQUEST)
						.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Invalid value").append("Invalid value for trigger field: ").append(columnName).build().toString())
						.build();
				}
				targetTab.setValue(triggerField, triggerValue);
				tabMutated = true;
				String calloutMsg = targetTab.processFieldChange(triggerField);

				// 9. Compare before/after and collect changed fields
				JsonObject changedFields = new JsonObject();
				for (int i = 0; i < targetTab.getFieldCount(); i++) {
					GridField gf = targetTab.getField(i);
					String colName = gf.getColumnName();
					if (colName.equals(columnName))
						continue;

					Object afterVal = gf.getValue();
					Object beforeVal = beforeValues.get(colName);

					boolean changed = false;
					if (beforeVal == null && afterVal != null) changed = true;
					else if (beforeVal != null && afterVal == null) changed = true;
					else if (beforeVal != null && !beforeVal.equals(afterVal)) changed = true;

					if (changed) {
						Object jsonValue = TypeConverterUtils.toJsonValue(gf, afterVal);
						if (jsonValue == null) {
							changedFields.add(colName, com.google.gson.JsonNull.INSTANCE);
						} else if (jsonValue instanceof Number) {
							changedFields.addProperty(colName, (Number) jsonValue);
						} else if (jsonValue instanceof Boolean) {
							changedFields.addProperty(colName, (Boolean) jsonValue);
						} else if (jsonValue instanceof String) {
							changedFields.addProperty(colName, (String) jsonValue);
						} else if (jsonValue instanceof JsonElement) {
							changedFields.add(colName, (JsonElement) jsonValue);
						} else {
							changedFields.addProperty(colName, jsonValue.toString());
						}
					}
				}

				// 10. Build response
				JsonObject response = new JsonObject();
				response.add("changedFields", changedFields);
				if (!Util.isEmpty(calloutMsg, true)) {
					response.addProperty("message", calloutMsg);
				}
				return Response.ok(response.toString()).build();
			} finally {
				// Always discard — do NOT save
				if (tabMutated) {
					targetTab.dataIgnore();
				}
			}

		} catch (JsonSyntaxException | IllegalStateException e) {
			log.log(Level.WARNING, "Invalid request body", e);
			return Response.status(Status.BAD_REQUEST)
				.entity(new ErrorBuilder().status(Status.BAD_REQUEST).title("Invalid request").append("Invalid JSON request body").build().toString())
				.build();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Callout error", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(new ErrorBuilder().status(Status.INTERNAL_SERVER_ERROR).title("Callout error").append("Internal server error").build().toString())
				.build();
		}
	}
}

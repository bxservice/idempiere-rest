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
* - Murilo Torino                                                     *
**********************************************************************/
package com.trekglobal.idempiere.rest.api.webhook;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Util;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Resolves a PayloadTemplate JSON string by replacing {@code @ColumnName@}
 * variables with values from a PO (Persistent Object).
 *
 * <h3>Behavior:</h3>
 * <ul>
 *   <li>{@code @ColumnName@} is replaced by the column's value from the PO</li>
 *   <li>String values are returned as-is (the surrounding JSON quotes come from the template)</li>
 *   <li>Numeric values (Integer, BigDecimal) are returned unquoted</li>
 *   <li>Boolean values are returned as {@code true}/{@code false}</li>
 *   <li>Timestamps are formatted as ISO 8601 UTC (via {@code Instant.toString()})</li>
 *   <li>Null values are returned as the string {@code null} (JSON null)</li>
 *   <li>Unknown column names are left as empty string with a warning log</li>
 * </ul>
 *
 * <h3>Example:</h3>
 * <pre>
 * Template: {"order": "@DocumentNo@", "total": @GrandTotal@, "status": "@DocStatus@"}
 * Result:   {"order": "PO-001", "total": 1500.00, "status": "CO"}
 * </pre>
 *
 * <p><b>Note:</b> For numeric/boolean values that should appear unquoted in JSON,
 * do NOT wrap them in quotes in the template:
 * {@code "total": @GrandTotal@} produces {@code "total": 1500.00}
 * {@code "total": "@GrandTotal@"} produces {@code "total": "1500.00"}</p>
 *
 * @author muriloht Murilo H. Torquato &lt;murilo@muriloht.com&gt;
 */
public class WebhookPayloadTemplateResolver {

	private static final CLogger log = CLogger.getCLogger(WebhookPayloadTemplateResolver.class);

	/** Pattern matching @ColumnName@ — column names are alphanumeric + underscore */
	private static final Pattern VARIABLE_PATTERN = Pattern.compile("@([A-Za-z_][A-Za-z0-9_]*)@");

	private WebhookPayloadTemplateResolver() {
	}

	/**
	 * Resolve a payload template by substituting @ColumnName@ variables
	 * with values from the given PO.
	 *
	 * @param template the JSON template string with @ColumnName@ placeholders
	 * @param po the source persistent object to read values from
	 * @return the resolved JSON string, or null if template is empty
	 */
	public static String resolve(String template, PO po) {
		if (Util.isEmpty(template, true) || po == null)
			return null;

		Matcher matcher = VARIABLE_PATTERN.matcher(template);
		StringBuffer result = new StringBuffer();

		while (matcher.find()) {
			String columnName = matcher.group(1);
			String replacement = resolveColumn(po, columnName);
			// Escape backslashes and dollar signs for appendReplacement
			matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(result);

		String resolved = result.toString();

		// Post-process: remove entries whose value resolved to null.
		// Follows the same convention as DefaultPOSerializer — null fields are omitted.
		try {
			JsonElement parsed = JsonParser.parseString(resolved);
			if (parsed.isJsonObject()) {
				removeNullEntries(parsed.getAsJsonObject());
				return parsed.toString();
			}
		} catch (Exception e) {
			// Template is not a JSON object (e.g. a plain value or malformed) — return as-is
		}

		return resolved;
	}

	/**
	 * Recursively remove entries whose value is JsonNull or the literal string "null"
	 * (produced by resolveColumn when the PO value is null).
	 */
	private static void removeNullEntries(JsonObject obj) {
		for (Map.Entry<String, JsonElement> entry : new java.util.ArrayList<>(obj.entrySet())) {
			JsonElement value = entry.getValue();
			if (value instanceof JsonNull || (value.isJsonPrimitive() && "null".equals(value.getAsString()))) {
				obj.remove(entry.getKey());
			} else if (value.isJsonObject()) {
				removeNullEntries(value.getAsJsonObject());
			}
		}
	}

	/**
	 * Resolve a single column value from the PO.
	 *
	 * @param po the persistent object
	 * @param columnName the column name to resolve
	 * @return the string representation of the value
	 */
	private static String resolveColumn(PO po, String columnName) {
		int colIndex = po.get_ColumnIndex(columnName);
		if (colIndex < 0) {
			log.warning("PayloadTemplate: unknown column '" + columnName
					+ "' in table " + po.get_TableName());
			return "";
		}

		Object value = po.get_Value(columnName);
		if (value == null)
			return "null";

		return formatValue(value);
	}

	/**
	 * Format a value for JSON output.
	 * The caller determines quoting based on their template:
	 * - "@Col@" → "value" (quoted by template)
	 * - @Col@ → value (unquoted, suitable for numbers/booleans)
	 *
	 * @param value the value to format
	 * @return string representation
	 */
	private static String formatValue(Object value) {
		if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue() ? "true" : "false";
		}
		if (value instanceof Integer || value instanceof Long) {
			return value.toString();
		}
		if (value instanceof BigDecimal) {
			return ((BigDecimal) value).toPlainString();
		}
		if (value instanceof Timestamp) {
			return ((Timestamp) value).toInstant().toString();
		}
		// String and everything else — escape for JSON safety
		return escapeJsonValue(value.toString());
	}

	/**
	 * Escape special characters in a string for safe JSON embedding.
	 * Handles: backslash, double quote, newline, carriage return, tab.
	 */
	private static String escapeJsonValue(String s) {
		if (s == null)
			return "";
		return s.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}
}

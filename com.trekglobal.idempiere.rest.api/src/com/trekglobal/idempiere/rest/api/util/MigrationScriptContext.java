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

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.container.ContainerRequestContext;

import org.compiere.dbPort.Convert;
import org.compiere.model.I_AD_UserPreference;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Util;

/**
 * Helper for the System administrator only migration script request headers.<br/>
 * The headers are only honored for client 0 (System), role 0 (System Administrator)
 * and a user that is an administrator.
 *
 * @author hengsin
 */
public final class MigrationScriptContext {

	public static final String LOG_MIGRATION_SCRIPT_HEADER = "X-ID-LogMigrationScript";
	public static final String ADEMPIERE_SYS_HEADER = "X-ID-AdempiereSys";
	public static final String MIGRATION_SCRIPT_COMMENT_HEADER = "X-ID-MigrationScriptComment";

	public static final String MIGRATION_SCRIPT_FILE_NAME = Convert.CONTEXT_MIGRATION_SCRIPT_FILE_NAME;

	private static final Map<String, String> s_sessionFileNameCache = new ConcurrentHashMap<>();

	private static final String[] CONTEXT_KEYS = new String[] {
		Ini.P_LOGMIGRATIONSCRIPT,
		"P|" + Ini.P_LOGMIGRATIONSCRIPT,
		Ini.P_ADEMPIERESYS,
		"P|" + Ini.P_ADEMPIERESYS,
		I_AD_UserPreference.COLUMNNAME_MigrationScriptComment,
		"P|" + I_AD_UserPreference.COLUMNNAME_MigrationScriptComment,
		MIGRATION_SCRIPT_FILE_NAME,
		"P|" + MIGRATION_SCRIPT_FILE_NAME
	};

	private static final String MIGRATION_SCRIPT_APPLIED = MigrationScriptContext.class.getName() + ".applied";

	private MigrationScriptContext() {
	}

	/**
	 * Apply the migration script headers of the request to the current context.
	 * Nothing is applied if the request has none of the headers or if the current
	 * user is not a System administrator.
	 *
	 * @param requestContext request context
	 */
	public static void apply(ContainerRequestContext requestContext) {
		String logHeader = requestContext.getHeaderString(LOG_MIGRATION_SCRIPT_HEADER);
		String adempiereSysHeader = requestContext.getHeaderString(ADEMPIERE_SYS_HEADER);
		String commentHeader = requestContext.getHeaderString(MIGRATION_SCRIPT_COMMENT_HEADER);
		if (logHeader == null && adempiereSysHeader == null && commentHeader == null)
			return;

		Properties ctx = Env.getCtx();
		if (!isSeedUserAndTenant(ctx))
			return;

		String logMigrationScript = parseYesNo(logHeader);
		String adempiereSys = parseYesNo(adempiereSysHeader);
		String comment = sanitizeComment(commentHeader);

		if (logMigrationScript != null)
			setContext(ctx, Ini.P_LOGMIGRATIONSCRIPT, logMigrationScript);
		if (adempiereSys != null)
			setContext(ctx, Ini.P_ADEMPIERESYS, adempiereSys);
		if (comment != null)
			setContext(ctx, I_AD_UserPreference.COLUMNNAME_MigrationScriptComment, comment);

		if ("Y".equalsIgnoreCase(ctx.getProperty(Ini.P_LOGMIGRATIONSCRIPT))) {
			String currentComment = comment != null ? comment : Env.getContext(ctx, I_AD_UserPreference.COLUMNNAME_MigrationScriptComment);
			int sessionId = Env.getContextAsInt(ctx, Env.AD_SESSION_ID);
			String fileName = getOrCreateMigrationScriptFileName(sessionId, currentComment);
			setContext(ctx, MIGRATION_SCRIPT_FILE_NAME, fileName);
		}

		if (logMigrationScript != null || comment != null)
			requestContext.setProperty(MIGRATION_SCRIPT_APPLIED, Boolean.TRUE);
	}

	/**
	 * Resolve or create the migration script file name for the given session and comment.
	 *
	 * @param sessionId AD_Session_ID
	 * @param comment migration script comment
	 * @return migration script file name
	 */
	public static String getOrCreateMigrationScriptFileName(int sessionId, String comment) {
		String normalizedComment = sanitizeComment(comment);
		if (sessionId <= 0)
			return Convert.getMigrationScriptFileName(normalizedComment);
		String key = getSessionCacheKey(sessionId, normalizedComment);
		return s_sessionFileNameCache.computeIfAbsent(key, k -> Convert.getMigrationScriptFileName(normalizedComment));
	}

	/**
	 * Flush and close the migration script files if migration script headers
	 * have been applied to the request.
	 *
	 * @param requestContext request context
	 */
	public static void closeMigrationScriptFiles(ContainerRequestContext requestContext) {
		if (requestContext == null || !Boolean.TRUE.equals(requestContext.getProperty(MIGRATION_SCRIPT_APPLIED)))
			return;
		// Synchronize on Convert.class to be mutually exclusive with Convert.logMigrationScript
		synchronized (Convert.class) {
			Convert.closeLogMigrationScript();
		}
	}

	/**
	 * Copy the migration script context values from source to target.
	 *
	 * @param source source context
	 * @param target target context
	 */
	public static void copy(Properties source, Properties target) {
		if (source == null || target == null)
			return;
		for (String key : CONTEXT_KEYS) {
			String value = source.getProperty(key);
			if (value != null)
				Env.setContext(target, key, value);
		}
	}

	/**
	 * Clear cached migration script file names associated with a session.
	 *
	 * @param sessionId session id
	 */
	public static void cleanSessionCache(int sessionId) {
		if (sessionId <= 0)
			return;
		String prefix = sessionId + "#";
		s_sessionFileNameCache.keySet().removeIf(key -> key.startsWith(prefix));
	}

	/**
	 * Clear the entire session migration script file name cache.
	 */
	public static void clearSessionFileNameCache() {
		s_sessionFileNameCache.clear();
	}

	private static String getSessionCacheKey(int sessionId, String comment) {
		String ticket = null;
		if (comment != null) {
			Matcher m = Pattern.compile("(IDEMPIERE-[0-9]*)").matcher(comment);
			if (m.find())
				ticket = m.group(1);
		}
		String identifier = ticket != null ? ticket : (comment != null ? comment : "");
		return sessionId + "#" + identifier;
	}

	private static boolean isSeedUserAndTenant(Properties ctx) {
		return (Env.getAD_Client_ID(ctx) <= 20 && Env.getAD_User_ID(ctx) <= 102);
	}

	private static String parseYesNo(String value) {
		if (Util.isEmpty(value, true))
			return null;
		String trimmed = value.trim();
		if ("Y".equalsIgnoreCase(trimmed)
				|| "TRUE".equalsIgnoreCase(trimmed)
				|| "YES".equalsIgnoreCase(trimmed)
				|| "1".equals(trimmed))
			return "Y";
		return "N";
	}

	private static String sanitizeComment(String comment) {
		if (Util.isEmpty(comment, true))
			return null;
		// The comment is written as a single line comment in the generated migration script
		String sanitized = comment.replaceAll("[\\r\\n]+", " ").replaceAll("\\s+", " ").trim();
		return Util.isEmpty(sanitized, true) ? null : sanitized;
	}

	private static void setContext(Properties ctx, String key, String value) {
		Env.setContext(ctx, key, value);
		Env.setContext(ctx, "P|" + key, value);
	}
}

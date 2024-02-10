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
 * - Carlos Ruiz - globalqss - bx-service                              *
 **********************************************************************/

package com.trekglobal.idempiere.rest.api.model;

import java.sql.Timestamp;

import org.compiere.util.DB;

/**
 * Model class for REST_RefreshToken - temporary table to persist refresh tokens vs auth tokens
 * @author Carlos Ruiz
 */
public class MRefreshToken {

	public MRefreshToken(String token, String refreshToken, Timestamp expiryDate) {
		setToken(token);
		setRefreshToken(refreshToken);
		setExpiresAt(expiryDate);
	}

	private final static String insertSql = "INSERT INTO REST_RefreshToken (Token, RefreshToken, ExpiresAt) VALUES (?,?,?)";
	private final static String selectSql = "SELECT Token FROM REST_RefreshToken WHERE RefreshToken = ?";
	private final static String deleteSql = "DELETE FROM REST_RefreshToken WHERE ExpiresAt<getdate()";
	private final static String deleteRefreshTokenSql = "DELETE FROM REST_RefreshToken WHERE RefreshToken = ?";
	private final static String deleteTokenSql = "DELETE FROM REST_RefreshToken WHERE Token = ?";

	/**
	 * Get an auth token based on a refresh token
	 * @param refresh_token
	 * @return
	 */
	public static String getAuthToken(String refresh_token) {
		deleteExpired();
		return DB.getSQLValueStringEx(null, selectSql, refresh_token);
	}

	/**
	 * Delete all expired refresh tokens
	 */
	private static void deleteExpired() {
		deleteExpired(null);
	}

	/**
	 * Delete all expired refresh tokens using transaction
	 */
	public static void deleteExpired(String trxName) {
		DB.executeUpdateEx(deleteSql, trxName);
	}

	/**
	 * Delete refresh token based on a previous refresh token
	 * @param RefreshToken
	 */
	public static void deleteRefreshToken(String refreshToken) {
		DB.executeUpdateEx(deleteRefreshTokenSql, new Object[] {refreshToken},  null);
	}

	/**
	 * Delete refresh token based on a token
	 * @param RefreshToken
	 */
	public static void deleteToken(String token) {
		DB.executeUpdateEx(deleteTokenSql, new Object[] {token},  null);
	}

	/**
	 * Persist record in the database
	 * @return
	 */
	public boolean save() {
		int no = DB.executeUpdateEx(insertSql, new Object[] {m_Token, m_RefreshToken, m_ExpiresAt}, null);
		return no == 1;
	}

	private Timestamp m_ExpiresAt;
	private String m_Token;
	private String m_RefreshToken;

	/**
	 * Set Expires At.
	 * 
	 * @param ExpiresAt Expires At
	 */
	public void setExpiresAt(Timestamp ExpiresAt) {
		m_ExpiresAt = ExpiresAt;
	}

	/**
	 * Get Expires At.
	 * 
	 * @return Expires At
	 */
	public Timestamp getExpiresAt() {
		return m_ExpiresAt;
	}

	/**
	 * Set Refresh Token.
	 * 
	 * @param RefreshToken Refresh Token
	 */
	public void setRefreshToken(String RefreshToken) {
		m_RefreshToken = RefreshToken;
	}

	/**
	 * Get Refresh Token.
	 * 
	 * @return Refresh Token
	 */
	public String getRefreshToken() {
		return m_RefreshToken;
	}

	/**
	 * Set Token.
	 * 
	 * @param Token Token
	 */
	public void setToken(String Token) {
		m_Token = Token;
	}

	/**
	 * Get Token.
	 * 
	 * @return Token
	 */
	public String getToken() {
		return m_Token;
	}

}

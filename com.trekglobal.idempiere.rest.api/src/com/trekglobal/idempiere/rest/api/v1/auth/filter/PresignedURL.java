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
package com.trekglobal.idempiere.rest.api.v1.auth.filter;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.compiere.model.MClient;
import org.compiere.model.MRole;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

import com.trekglobal.idempiere.rest.api.json.DateTypeConverter;
import com.trekglobal.idempiere.rest.api.json.RestUtils;

public class PresignedURL {

	private static final String SIGNATURE = "X-ID-SIGNATURE";
	private static final String CREDENTIAL = "X-ID-CREDENTIAL";
	private static final String DATE_CREATED = "X-ID-DATE";
	private static final String EXPIRES = "X-ID-EXPIRES";
	
	private static final String REST_HIDE_PRESIGNED_URL_ERRORS = "REST_HIDE_PRESIGNED_URL_ERRORS";
	// Default is "Something went wrong ..."
	private static final String REST_PRESIGNED_URL_GENERIC_ERROR = "REST_Presigned_URL_Generic_Error";
	
	/**
	 * Is the request context contains a presigned URL?
	 * @param requestContext
	 * @return true if the request context contains a presigned URL, false otherwise
	 */
	public static boolean isPresignedURL(ContainerRequestContext requestContext) {
		return requestContext != null && !Util.isEmpty(requestContext.getUriInfo().getQueryParameters().getFirst(SIGNATURE), true);
	}
	
	/**
	 * Build a generic error response for pre-signed URL errors
	 * @return error response for pre-signed URL errors
	 */
	public static Response buildGenericErrorResponse() {
		return Response.status(Response.Status.BAD_REQUEST)
				.entity(Msg.getMsg(Env.getCtx(), REST_PRESIGNED_URL_GENERIC_ERROR))
				.build();
	}
	
	/**
	 * Is hide errors for pre-signed URL enabled?
	 * @return true if hide errors for pre-signed URL is enabled, false otherwise
	 */
	public static boolean isHideErrors() {
		return "Y".equals(MSysConfig.getValue(REST_HIDE_PRESIGNED_URL_ERRORS, "N"));
	}
	
	/**
	 * Validate the signature of the presigned URL in the request context.
	 * If the signature is valid, it will set the context variables for tenantId, userId, and roleId.
	 * If the signature is invalid or missing, it will abort the request with a 401 Unauthorized response.
	 * 
	 * @param requestContext
	 */
	public static void validateSignature(ContainerRequestContext requestContext) {
		boolean hideErrors = isHideErrors();
		String signature = requestContext.getUriInfo().getQueryParameters().getFirst(SIGNATURE);
		if (!Util.isEmpty(signature, true)) {
			String credential = requestContext.getUriInfo().getQueryParameters().getFirst(CREDENTIAL);
			String dateCreated = requestContext.getUriInfo().getQueryParameters().getFirst(DATE_CREATED);
			String expires = requestContext.getUriInfo().getQueryParameters().getFirst(EXPIRES);
			String url = requestContext.getUriInfo().getPath();
			String method = requestContext.getMethod();
			if (Util.isEmpty(credential, true) || Util.isEmpty(dateCreated, true) || Util.isEmpty(expires, true)) {
				if (hideErrors)
					requestContext.abortWith(buildGenericErrorResponse());
				else
					requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
				return;
			}
			//tenantId, userId, roleId, httpMethod, url
			String[] credentials = decodeCredential(credential);
			if (credentials == null || credentials.length != 5) {
				if (hideErrors)
					requestContext.abortWith(buildGenericErrorResponse());
				else
					requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
				return;
			}
			try {
				int tenantId = Integer.parseInt(credentials[0]);
				int userId = Integer.parseInt(credentials[1]);
				int roleId = Integer.parseInt(credentials[2]);
				String httpMethod = credentials[3];
				String urlFromCredential = credentials[4];
				if (!method.equalsIgnoreCase(httpMethod)) {
					if (hideErrors)
						requestContext.abortWith(buildGenericErrorResponse());
					else
						requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
					return;
				}
				if (!url.startsWith(urlFromCredential)) {
					if (hideErrors)
						requestContext.abortWith(buildGenericErrorResponse());
					else
						requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
					return;
				}
				MClient client = MClient.get(tenantId);
				if (client == null || client.getAD_Client_ID() != tenantId) {
					if (hideErrors)
						requestContext.abortWith(buildGenericErrorResponse());
					else
						requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
					return;
				}
				MUser user = MUser.get(userId);
				if (user == null || user.getAD_User_ID() != userId || user.getAD_Client_ID() != tenantId) {
					if (hideErrors)
						requestContext.abortWith(buildGenericErrorResponse());
					else
						requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
					return;
				}
				MRole role = MRole.get(Env.getCtx(), roleId);
				if (role == null || role.getAD_Role_ID() != roleId || role.getAD_Client_ID() != tenantId) {
					if (hideErrors)
						requestContext.abortWith(buildGenericErrorResponse());
					else	
						requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
					return;
				}
				SimpleDateFormat dateFormat = new SimpleDateFormat(DateTypeConverter.ISO8601_DATETIME_PATTERN);
				Timestamp created = new Timestamp(dateFormat.parse(dateCreated).getTime());
				int expiresInSeconds = Integer.parseInt(expires);
				Timestamp expiresAt = new Timestamp(created.getTime() + expiresInSeconds * 1000L);
				Timestamp now = new Timestamp(System.currentTimeMillis());
				if (expiresAt.before(now) || expiresAt.equals(now)) {
					if (hideErrors)
						requestContext.abortWith(buildGenericErrorResponse());
					else
						requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Presigned URL has expired").build());
					return;
				}
				String expectedSignature = PresignedURL.generateSignature(method, urlFromCredential, client, user, role, dateCreated, expiresInSeconds);
				if (!signature.equals(expectedSignature)) {
					if (hideErrors)
						requestContext.abortWith(buildGenericErrorResponse());
					else
						requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Signature mismatch").build());
					return;
				}
				
				//update context
				Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, client.getAD_Client_ID());
				Env.setContext(Env.getCtx(), Env.AD_USER_ID, user.getAD_User_ID());
				Env.setContext(Env.getCtx(), Env.AD_ROLE_ID, role.getAD_Role_ID());
				RestUtils.setSessionContextVariables(Env.getCtx());
			} catch (Exception e) {
				if (hideErrors)
					requestContext.abortWith(buildGenericErrorResponse());
				else
					requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
				return;
			}
		} else {
			if (hideErrors)
				requestContext.abortWith(buildGenericErrorResponse());
			else
				requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).build());
			return;
		}
	}

	/**
	 * Create a signature for the presigned URL using HMAC SHA-256.
	 * @param method
	 * @param url
	 * @param client
	 * @param user
	 * @param role
	 * @param created
	 * @param expiresInSeconds
	 * @return signature as a string
	 */
	private static String generateSignature(String method, String url, MClient client, MUser user, MRole role,
			String created, long expiresInSeconds) {
		String key = user.getPassword();
		StringBuilder data = new StringBuilder();
		data.append(method)
		  .append(" ")
		  .append(url)
		  .append("/")
		  .append(client.getAD_Client_ID())
		  .append("/")
		  .append(user.getAD_User_ID())
		  .append("/")
		  .append(role.getAD_Role_ID())
		  .append("/")
		  .append(created)
		  .append("/")
		  .append(expiresInSeconds);
		String signature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, key).hmacHex(data.toString());
		return signature;
	}
	
	/**
	 * Create base 64 encoded credential string from context (tenantId, userId, roleId, httpMethod
	 * @param httpMethod
	 * @param url
	 * @return base 64 encoded credential string
	 * @throws GeneralSecurityException 
	 * @throws NoSuchAlgorithmException 
	 */
	private static String encodeCredential(String httpMethod, String url)  {
		//tenantId, userId, roleId, httpMethod, url
		StringBuilder credential = new StringBuilder();
		credential.append(Env.getAD_Client_ID(Env.getCtx()))
		          .append(",")
		          .append(Env.getAD_User_ID(Env.getCtx()))
		          .append(",")
		          .append(Env.getAD_Role_ID(Env.getCtx()))
		          .append(",")
		          .append(httpMethod)
		          .append(",")
		          .append(url);
		try {
			Cipher cipher = newCredentialCipher(true);
			byte[] cipherBytes = cipher.doFinal(credential.toString().getBytes(StandardCharsets.UTF_8));
			String encodedTxt = Base64.encodeBase64URLSafeString(cipherBytes);
			return encodedTxt;
		} catch (Exception e) {
			throw new AdempiereException("Failed to encode credential: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Decode the base 64 encoded credential string to get tenantId, userId, roleId, httpMethod.
	 * @param credential
	 * @return tenantId, userId, roleId, httpMethod, url
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	 * @throws NoSuchPaddingException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	private static String[] decodeCredential(String credential) {
		if (Util.isEmpty(credential, true)) {
			return null;
		}
		try {
			byte[] decodedBytes = Base64.decodeBase64(credential);
			Cipher cipher = newCredentialCipher(false);
			byte[] cipherText = cipher.doFinal(decodedBytes);
			String[] parts = new String(cipherText, StandardCharsets.UTF_8).split(",");
			if (parts.length != 5) {
				return null;
			}
			return parts;
		} catch (Exception e) {
			throw new AdempiereException("Failed to decode credential: " + e.getMessage(), e);
		}
	}

	private static Cipher newCredentialCipher(boolean forEncryption)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		byte[] keyBytes = getCredentialKeyBytes();
		Cipher cipher = Cipher.getInstance("AES");
		SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
		if (forEncryption)
			cipher.init(Cipher.ENCRYPT_MODE, key);
		else
			cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher;
	}

	private static byte[] getCredentialKeyBytes() {
		byte[] keyBytes = DB.getSQLValueStringEx(null, 
				"select ad_package_imp_uu from ad_package_imp where name=? and pk_version=? and pk_status=?", 
				new Object[] {"com.trekglobal.idempiere.rest.api", "1.0.0", "Completed successfully"})
				.replace("-", "").getBytes(StandardCharsets.UTF_8);
		return keyBytes;
	}
	
	/**
	 * Create a presigned URL query parameters with the given HTTP method, URL, and expiration time in seconds.
	 * 
	 * @param httpMethod the HTTP method (e.g., GET, POST)
	 * @param url the base URL to be signed
	 * @param expiresInSeconds the number of seconds until the URL expires
	 * @return a presigned URL query parameters as a string
	 */
	public static String createPresignedURLParams(String httpMethod, String url, long expiresInSeconds) {
		StringBuilder presignedURLParams = new StringBuilder();
		MClient client = MClient.get(Env.getAD_Client_ID(Env.getCtx()));
		MUser user = MUser.get(Env.getAD_User_ID(Env.getCtx()));
		MRole role = MRole.getDefault();
		Timestamp created = new Timestamp(System.currentTimeMillis());
		String dateCreated = new SimpleDateFormat(DateTypeConverter.ISO8601_DATETIME_PATTERN).format(created);
		String signature = PresignedURL.generateSignature(httpMethod, url, client, user, role, dateCreated, expiresInSeconds);
		presignedURLParams.append("?").append(CREDENTIAL).append("=").append(PresignedURL.encodeCredential(httpMethod, url))
		           .append("&").append(DATE_CREATED).append("=").append(dateCreated)
		           .append("&").append(EXPIRES).append("=").append(expiresInSeconds)
		           .append("&").append(SIGNATURE).append("=").append(signature);
		return presignedURLParams.toString();
	}
}

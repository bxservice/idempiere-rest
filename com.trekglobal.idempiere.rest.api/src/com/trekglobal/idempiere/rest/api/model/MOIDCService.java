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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.interfaces.RSAPublicKey;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Arrays;
import java.util.Properties;

import javax.ws.rs.container.ContainerRequestContext;

import org.compiere.model.MSession;
import org.compiere.model.MSysConfig;
import org.compiere.model.MWarehouse;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CCache;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.cache.ImmutablePOCache;
import org.idempiere.cache.ImmutablePOSupport;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.RestUtils;
import com.trekglobal.idempiere.rest.api.oidc.AuthenticatedUser;
import com.trekglobal.idempiere.rest.api.oidc.IOIDCProvider;

/**
 * @author hengsin
 */
public class MOIDCService extends X_REST_OIDCService implements ImmutablePOSupport {

	private static final long serialVersionUID = 3717089346846528233L;

	private static ImmutablePOCache<String, MOIDCService> s_issuerCache = new ImmutablePOCache<>(Table_Name, 10);
	
	private static CCache<String, AuthenticatedUser> s_authCache = new CCache<>("AuthenticatedUser_Cache", 40, 
			MSysConfig.getIntValue("REST_TOKEN_EXPIRE_IN_MINUTES", 60));
	
	// revoke cache should live longer than auth cache
	private static CCache<String, Boolean> s_revokeCache = new CCache<>("Revoke_Token_Cache", 40, 
			MSysConfig.getIntValue("REST_TOKEN_EXPIRE_IN_MINUTES", 60)*2);

	// 24 hour cache for JwkProvider
	private static CCache<String, JwkProvider> s_jwkProviderCache = new CCache<>("OIDC_JwkProvider_Cache", 20, 1440);

	/** HTTP header for AD_Role.Name */
	public static final String ROLE_HEADER = "X-ID-Role";

	/** HTTP header for AD_Org.Value. */ 
	public static final String ORG_HEADER = "X-ID-Organization";

	/** HTTP header for M_Warehouse.Name */
	public static final String WAREHOUSE_HEADER = "X-ID-Warehouse";

	/** HTTP header for AD_Language */
	public static final String LANGUAGE_HEADER = "X-ID-Language";
	
	/** HTTP header for AD_Org.Value. */ 
	public static final String IDTOKEN_HEADER = "X-ID-IdToken";
	
	/**
	 * @param ctx
	 * @param REST_OIDCService_ID
	 * @param trxName
	 */
	public MOIDCService(Properties ctx, int REST_OIDCService_ID, String trxName) {
		super(ctx, REST_OIDCService_ID, trxName);
	}

	/**
	 * @param ctx
	 * @param REST_OIDCService_ID
	 * @param trxName
	 * @param virtualColumns
	 */
	public MOIDCService(Properties ctx, int REST_OIDCService_ID, String trxName, String... virtualColumns) {
		super(ctx, REST_OIDCService_ID, trxName, virtualColumns);
	}

	/**
	 * @param ctx
	 * @param REST_OIDCService_UU
	 * @param trxName
	 */
	public MOIDCService(Properties ctx, String REST_OIDCService_UU, String trxName) {
		super(ctx, REST_OIDCService_UU, trxName);
	}

	/**
	 * @param ctx
	 * @param REST_OIDCService_UU
	 * @param trxName
	 * @param virtualColumns
	 */
	public MOIDCService(Properties ctx, String REST_OIDCService_UU, String trxName, String... virtualColumns) {
		super(ctx, REST_OIDCService_UU, trxName, virtualColumns);
	}

	/**
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MOIDCService(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
	/**
	 * @param ctx
	 * @param copy
	 */
	public MOIDCService(Properties ctx, MOIDCService copy) {
		this(ctx, copy, (String) null);
	}
	
	/**
	 * @param ctx
	 * @param copy
	 * @param trxName
	 */
	public MOIDCService(Properties ctx, MOIDCService copy, String trxName) {
		this(ctx, 0, trxName);
		copyPO(copy);
	}

	/**
	 * Find MOIDCService from issuer URL and audience
	 * @param issuer
	 * @param audience
	 * @return Matching MOIDCService or null
	 */
	public static MOIDCService fromIssuerAndAudience(String issuer, String audience) {
		String key = issuer+"|"+audience;
		if (s_issuerCache.containsKey(key)) {
			return s_issuerCache.get(Env.getCtx(), key, e -> new MOIDCService(Env.getCtx(), e)); 
		} else {
			Query query = new Query(Env.getCtx(), Table_Name, "%s=? AND %s=?".formatted(COLUMNNAME_OIDC_IssuerURL, COLUMNNAME_OIDC_Audience), null);
			MOIDCService service = query.setParameters(issuer, audience).setOnlyActiveRecords(true).firstOnly();
			if (service != null) {
				s_issuerCache.put(key, service, e -> new MOIDCService(Env.getCtx(), e));
			}
			return service;
		}
	}
	
	/**
	 * Get MOIDCService from OIDC access token
	 * @param token
	 * @return Matching MOIDCService or null
	 * @throws JWTVerificationException if token is an OIDC access token and there's no matching service configuration
	 */
	public static MOIDCService findMatchingOIDCService(String token) {
		DecodedJWT decoded = JWT.decode(token);
		Instant expire = decoded.getExpiresAtAsInstant(); 
		if (expire != null && !expire.isAfter(Instant.now()))
			throw new JWTVerificationException("Token has expired");
		
		Claim alg = decoded.getHeaderClaim("alg");
		Claim typ = decoded.getHeaderClaim("typ");
		Claim kid = decoded.getHeaderClaim("kid");
		
		Claim iss = decoded.getClaim("iss");
		Claim aud = decoded.getClaim("aud");
		
		Claim client_id = decoded.getClaim("client_id");
		
		MOIDCService service = null;
		if (isWithStringValue(alg) && isWithStringValue(typ) && "JWT".equals(typ.asString()) && isWithStringValue(kid) &&
			isWithStringValue(iss) && isWithStringValue(aud)) {
			service = fromIssuerAndAudience(iss.asString(), aud.asString());
			if (service == null)
				throw new JWTVerificationException("No matching OpenID Connect service configuration for access token");			
		} else if (isWithStringValue(kid) && isWithStringValue(client_id)) {
			service = fromIssuerAndAudience(iss.asString(), client_id.asString());
			if (service == null)
				throw new JWTVerificationException("No matching OpenID Connect service configuration for access token");
		}
		return service;
	}
	
	/**
	 * @param claim
	 * @return true if claim exist and with value
	 */
	private static boolean isWithStringValue(Claim claim) {
		return !claim.isMissing() && !claim.isNull() && claim.asString() != null;
	}
	
	/**
	 * Validate OIDC access token
	 * @param token
	 * @param requestContext
	 */
	public void validateAccessToken(String token, ContainerRequestContext requestContext) {
		// first check if token is revoked
		if (s_revokeCache.containsKey(token)) {
			throw new JWTVerificationException("Token has been revoked");
		}
		
		AuthenticatedUser authenticatedUser = s_authCache.get(token);
		if (authenticatedUser != null) {
			processAuthenticatedUser(requestContext, authenticatedUser);
			return;
		}
		
		MOIDCProvider oidcProvider = new MOIDCProvider(Env.getCtx(), getREST_OIDCProvider_ID(), null);
		IOIDCProvider service = oidcProvider.getProvider();
		if (service == null)
			throw new JWTVerificationException("No provider service register for %s".formatted(oidcProvider.getName()));
		
		DecodedJWT decodedJwt = getDecodedJWT(token);
        	
		if(isValidateScope_OIDC()) {
			String path = requestContext.getUriInfo().getPath();
			Claim scopeClaim = decodedJwt.getClaim("scope");
			if (scopeClaim.isMissing() || scopeClaim.isNull())
				throw new JWTVerificationException("Missing scope claim");
			String scopeText = scopeClaim.asString();
			String[] scopes = scopeText.split(" ");
			boolean match = Arrays.stream(scopes).anyMatch(e -> e.equals(path));
			if (!match)
				throw new JWTVerificationException("API path not part of scope");
		}
		
		//get user, role, tenant and org
		authenticatedUser = service.getAuthenticatedUser(decodedJwt, requestContext, this);
		if (s_revokeCache.containsKey(token)) {
			throw new JWTVerificationException("Token has been revoked");
		}
		s_authCache.put(token, authenticatedUser);
		
		processAuthenticatedUser(requestContext, authenticatedUser);
	
	}

	/**
	 * @param requestContext
	 * @param authenticatedUser
	 */
	private void processAuthenticatedUser(ContainerRequestContext requestContext, AuthenticatedUser authenticatedUser) {
		Env.setContext(Env.getCtx(), Env.AD_CLIENT_ID, authenticatedUser.getTenantId());
		Env.setContext(Env.getCtx(), Env.AD_USER_ID, authenticatedUser.getUserId());
		if (authenticatedUser.getRoleId() >= 0)
			Env.setContext(Env.getCtx(), Env.AD_ROLE_ID, authenticatedUser.getRoleId());
		if (authenticatedUser.getOrganizationId() >= 0)
			Env.setContext(Env.getCtx(), Env.AD_ORG_ID, authenticatedUser.getOrganizationId());
		if (authenticatedUser.getsessionId() > 0)
			Env.setContext(Env.getCtx(), Env.AD_SESSION_ID, authenticatedUser.getsessionId());
		String AD_Language = requestContext.getHeaderString(LANGUAGE_HEADER);
		if (!Util.isEmpty(AD_Language))
			Env.setContext(Env.getCtx(), Env.LANGUAGE, AD_Language);
		String warehouseName = requestContext.getHeaderString(WAREHOUSE_HEADER);
		if (!Util.isEmpty(warehouseName)) {
			Query warehouseQuery = new Query(Env.getCtx(), MWarehouse.Table_Name, "AD_Client_ID=? AND Name=?", null);
			MWarehouse wh = warehouseQuery.setOnlyActiveRecords(true).setParameters(authenticatedUser.getTenantId(), warehouseName).first();
			if (wh != null)
				Env.setContext(Env.getCtx(), Env.M_WAREHOUSE_ID, wh.get_ID());
		}
		RestUtils.setSessionContextVariables(Env.getCtx());
	}

	@Override
	public PO markImmutable() {
		if (is_Immutable())
			return this;

		super.makeImmutable();
		return this;
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		boolean success = super.beforeSave(newRecord);
		if (success && newRecord) {
			String authorityURL = getOIDC_AuthorityURL();
			if (!Util.isEmpty(authorityURL)) {
				if (Util.isEmpty(getOIDC_IssuerURL()))
					setOIDC_IssuerURL(authorityURL);
				if (Util.isEmpty(getOIDC_ConfigurationURL()) && getREST_OIDCProvider_ID() > 0) {
					MOIDCProvider provider = new MOIDCProvider(Env.getCtx(), getREST_OIDCProvider_ID(), null);
					String configurationURL = provider.getOIDC_ConfigurationURL();
					configurationURL = configurationURL.replace("@"+I_REST_OIDCService.COLUMNNAME_OIDC_AuthorityURL+"@", authorityURL);
					setOIDC_ConfigurationURL(configurationURL);
				}
			}
		}
		return success;
	}
	
	private DecodedJWT getDecodedJWT(String token) {
		String wellKnownUrl = getOIDC_ConfigurationURL();
		JwkProvider provider = s_jwkProviderCache.get(wellKnownUrl);
		
		if (provider == null) {
			//get jwks from openid configuration endpoint
			String jwksUrl = null;
	        HttpClient httpClient = HttpClient.newBuilder()
	        		.connectTimeout(java.time.Duration.ofSeconds(10))
	        		.build();
	        HttpRequest request = HttpRequest.newBuilder()
	                .uri(URI.create(wellKnownUrl))
	                .timeout(java.time.Duration.ofSeconds(30))
	                .GET()
	                .build();
	
	        try {
	            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	
	            // Parse the JSON response
	            JsonObject json = new Gson().fromJson(response.body(), JsonObject.class);
	
	            // Extract the JWKS URL
	            if (json.has("jwks_uri") && !json.get("jwks_uri").isJsonNull()) {
	            	jwksUrl = json.get("jwks_uri").getAsString();
	            } else {
	            	throw new JWTVerificationException("Configuration endpoint did not return jwks_uri");
	            }
	        } catch (IOException | InterruptedException e) {
	        	if (e instanceof InterruptedException) {
	        		Thread.currentThread().interrupt();
	        	}
	        	throw new JWTVerificationException(e.getMessage(), e);            
	        }
	        
	        if (jwksUrl != null) {
	        	try {
					provider = new UrlJwkProvider(new URL(jwksUrl));
					s_jwkProviderCache.put(wellKnownUrl, provider);
				} catch (MalformedURLException e) {
					throw new JWTVerificationException(e.getMessage(), e);
				}
	        } else {
	        	throw new JWTVerificationException("Failed to retrieve jwks_uri from Configuration URL");
	        }
		}
        
        DecodedJWT decodedJwt = JWT.decode(token);
    	try {
        	//verify signature, audience and exp
            Jwk jwk = provider.get(decodedJwt.getKeyId());

            Algorithm algorithm = null;
            String alg = decodedJwt.getAlgorithm();
            if ("RS256".equals(alg)) {
            	algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            } else if ("RS384".equals(alg)) {
            	algorithm = Algorithm.RSA384((RSAPublicKey) jwk.getPublicKey(), null);
            } else if ("RS512".equals(alg)) {
            	algorithm = Algorithm.RSA512((RSAPublicKey) jwk.getPublicKey(), null);
            } else {
            	throw new JWTVerificationException("Unsupported algorithm: " + alg);
            }

            Verification verification = JWT.require(algorithm)
					  .acceptExpiresAt(0)
					  .withIssuer(getOIDC_IssuerURL());
            Claim aud = decodedJwt.getClaim("aud");
            if (isWithStringValue(aud)) {
            	verification.withAudience(getOIDC_Audience());
            } else {
            	Claim client_id = decodedJwt.getClaim("client_id");
            	 if (isWithStringValue(client_id))
            		 verification.withClaim("client_id", getOIDC_Audience());
            }
            JWTVerifier verifier = verification.build();
            decodedJwt = verifier.verify(decodedJwt);
        } catch (JWTVerificationException | JwkException e) {
        	if (e instanceof JWTVerificationException)
        		throw (JWTVerificationException)e;
        	else
        		throw new JWTVerificationException(e.getMessage(), e);
        }
        
        return decodedJwt;
	}
	
	public DecodedJWT getDecodedIdToken(ContainerRequestContext requestContext) {
		String idToken = requestContext.getHeaderString(MOIDCService.IDTOKEN_HEADER);
		if (Util.isEmpty(idToken))
			return null;
		return getDecodedJWT(idToken);
	}
	
	/**
	 * Remove OAuth/OIDC token from authenticated cache
	 * @param token the OAuth/OIDC token
	 * @return true if token found and removed
	 */
	public static boolean revokeToken(String token) {
		if (Util.isEmpty(token))
			return false;
		// Always add to revoke cache to handle race condition where token is being validated
		// but not yet in auth cache.
		s_revokeCache.put(token, Boolean.TRUE);
		AuthenticatedUser authenticatedUser = s_authCache.remove(token);
		int sessionId = authenticatedUser.getsessionId();
		if (sessionId > 0) {
			Env.setContext(Env.getCtx(), Env.AD_SESSION_ID, sessionId);		
			MSession session = new MSession(Env.getCtx(), sessionId, null);
			session.setWebSession(session.getWebSession() + "-logout");
			session.logout();
		}
		
		return authenticatedUser != null;
	}
	
}

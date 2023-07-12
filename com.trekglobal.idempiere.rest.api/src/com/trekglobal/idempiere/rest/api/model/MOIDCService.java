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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Properties;

import javax.ws.rs.container.ContainerRequestContext;

import org.compiere.model.MAcctSchema;
import org.compiere.model.MClientInfo;
import org.compiere.model.MRole;
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
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.oidc.AuthenticatedUser;
import com.trekglobal.idempiere.rest.api.oidc.IOIDCProvider;

/**
 * @author hengsin
 */
public class MOIDCService extends X_REST_OIDCService implements ImmutablePOSupport {

	private static final long serialVersionUID = 8307568192267187340L;

	private static ImmutablePOCache<String, MOIDCService> s_issuerCache = new ImmutablePOCache<>(Table_Name, 10);
	
	private static CCache<String, AuthenticatedUser> s_authCache = new CCache<>("AuthenticatedUser_Cache", 40, 10);

	/** HTTP header for AD_Role.Name */
	public static final String ROLE_HEADER = "X-ID-Role";

	/** HTTP header for AD_Org.Value. */ 
	public static final String ORG_HEADER = "X-ID-Organization";

	/** HTTP header for M_Warehouse.Name */
	public static final String WAREHOUSE_HEADER = "X-ID-Warehouse";

	/** HTTP header for AD_Language */
	public static final String LANGUAGE_HEADER = "X-ID-Language";
	
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
		Claim azp = decoded.getClaim("azp");
		
		MOIDCService service = null;
		if (isWithStringValue(alg) && isWithStringValue(typ) && "JWT".equals(typ.asString()) && isWithStringValue(kid) &&
			isWithStringValue(iss) && isWithStringValue(aud) && isWithStringValue(azp)) {
			service = fromIssuerAndAudience(iss.asString(), aud.asString());
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
		AuthenticatedUser authenticatedUser = s_authCache.get(token);
		if (authenticatedUser != null) {
			processAuthenticatedUser(requestContext, authenticatedUser);
			return;
		}
		
		MOIDCProvider oidcProvider = new MOIDCProvider(Env.getCtx(), getREST_OIDCProvider_ID(), null);
		IOIDCProvider service = oidcProvider.getProvider();
		if (service == null)
			throw new JWTVerificationException("No provider service register for %s".formatted(oidcProvider.getName()));
		
		//get jwks from openid configuration endpoint
		String jwksUrl = null;
		String wellKnownUrl = getOIDC_ConfigurationURL();
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wellKnownUrl))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse the JSON response
            JsonObject json = new Gson().fromJson(response.body(), JsonObject.class);

            // Extract the JWKS URL
            jwksUrl = json.get("jwks_uri").getAsString();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);            
        }
        
        if (jwksUrl != null) {
        	DecodedJWT decodedJwt = JWT.decode(token);
        	try {
	        	//verify signature, audience and exp
	        	JwkProvider provider = new UrlJwkProvider(new URL(jwksUrl));
	            Jwk jwk = provider.get(decodedJwt.getKeyId());
	
	            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
	            JWTVerifier verifier = JWT.require(algorithm)
	            						  .acceptExpiresAt(0)
	            						  .withIssuer(getOIDC_IssuerURL())
	            						  .withAudience(getOIDC_Audience())
	            						  .build();
	            decodedJwt = verifier.verify(decodedJwt);
            } catch (JWTVerificationException | JwkException | MalformedURLException e) {
            	if (e instanceof JWTVerificationException)
            		throw (JWTVerificationException)e;
            	else
            		throw new JWTVerificationException(e.getMessage(), e);
            }
        	
        	if( isValidateScope_OIDC()) {
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
        	s_authCache.put(token, authenticatedUser);
        	
        	processAuthenticatedUser(requestContext, authenticatedUser);
        } else {
        	throw new JWTVerificationException("Failed to retrieve jwks_uri from Configuration URL");
        }
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
		
		if (authenticatedUser.getRoleId() >= 0) {
			if (MRole.getDefault(Env.getCtx(), false).isShowAcct())
				Env.setContext(Env.getCtx(), "#ShowAcct", "Y");
			else
				Env.setContext(Env.getCtx(), "#ShowAcct", "N");
		}
		
		Env.setContext(Env.getCtx(), "#Date", new Timestamp(System.currentTimeMillis()));
		
		if (MClientInfo.get(Env.getCtx(), authenticatedUser.getTenantId()).getC_AcctSchema1_ID() > 0) {
			MAcctSchema primary = MAcctSchema.get(Env.getCtx(), MClientInfo.get(Env.getCtx(), authenticatedUser.getTenantId()).getC_AcctSchema1_ID());
			Env.setContext(Env.getCtx(), "$C_AcctSchema_ID", primary.getC_AcctSchema_ID());
			Env.setContext(Env.getCtx(), "$C_Currency_ID", primary.getC_Currency_ID());
			Env.setContext(Env.getCtx(), "$HasAlias", primary.isHasAlias());
		}
		
		MAcctSchema[] ass = MAcctSchema.getClientAcctSchema(Env.getCtx(), authenticatedUser.getTenantId());
		if(ass != null && ass.length > 1) {
			for(MAcctSchema as : ass) {
				if (as.getAD_OrgOnly_ID() != 0) {
					if (as.isSkipOrg(authenticatedUser.getOrganizationId())) {
						continue;
					} else  {
						Env.setContext(Env.getCtx(), "$C_AcctSchema_ID", as.getC_AcctSchema_ID());
						Env.setContext(Env.getCtx(), "$C_Currency_ID", as.getC_Currency_ID());
						Env.setContext(Env.getCtx(), "$HasAlias", as.isHasAlias());
						break;
					}
				}
			}
		}
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
		
}

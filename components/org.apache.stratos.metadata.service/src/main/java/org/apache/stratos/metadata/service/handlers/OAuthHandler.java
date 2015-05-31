/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.metadata.service.handlers;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;
import org.apache.stratos.metadata.service.context.AuthenticationContext;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.core.util.KeyStoreManager;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.security.interfaces.RSAPublicKey;


/**
 * This class responsible for OAuth based authentication/authorization. A client
 * has to bring a valid OAuth token from a
 * a OAuth provider. This class intercept the request and calls the
 * OAuthTokenValidation endpoint of the provider.
 */
public class OAuthHandler extends AbstractAuthenticationAuthorizationHandler {
    public static final String BEARER = "Bearer ";
    public static final String APPLICATION = "applications";
    private static Log log = LogFactory.getLog(OAuthHandler.class);
    private static String SUPPORTED_AUTHENTICATION_TYPE = "Bearer";
    private static String oauthValidationEndpoint;
    private static String username;
    private static String password;

    public void setOauthValidationEndpoint(String oauthValidationEndpoint) {
        OAuthHandler.oauthValidationEndpoint = oauthValidationEndpoint;
    }

    public void setUsername(String username) {
        OAuthHandler.username = username;
    }

    public void setPassword(String password) {
        OAuthHandler.password = password;
    }

    @Override
    public boolean canHandle(String authHeaderPrefix) {
        return SUPPORTED_AUTHENTICATION_TYPE.equals(authHeaderPrefix);
    }

    @Override
    public Response handle(Message message, ClassResourceInfo classResourceInfo) {
        try {
            HttpHeaders httpHeaders = new HttpHeadersImpl(message);
            String header = httpHeaders.getRequestHeaders().getFirst("Authorization");
            // if the authorization token has Bearer..
            if (header.startsWith(BEARER)) {
                String accessToken = header.substring(7).trim();
                boolean valid;
                String appId_in_token = extractAppIdFromIdToken(accessToken);
                String requestUrl = (String) message.get(Message.REQUEST_URI);
                String basePath = (String) message.get(Message.BASE_PATH);
                String requestedAppId = extractApplicationIdFromUrl(requestUrl, basePath);

                if (org.apache.commons.lang3.StringUtils.isEmpty(appId_in_token) || org.apache.commons.lang3.StringUtils.isEmpty(requestedAppId)) {
                    valid = false;
                } else {
                    valid = appId_in_token.equals(requestedAppId);
                    if(!valid){
                        log.error("The token presented is only valid for " + appId_in_token + " , but it tries to access metadata for " + requestedAppId);
                    }
                }

                if (!valid) {
                    return Response.status(Response.Status.FORBIDDEN).build();
                }
            }else{
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        } catch (Exception e) {
            log.error("Error while validating access token", e);
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        AuthenticationContext.setAuthenticated(true);
        return null;
    }

    private String extractApplicationIdFromUrl(String url, String basePath) {
        String appId = null;
        String segments[] = url.split("/");
        for (int i = 0; i < segments.length; i++) {
            if (APPLICATION.equals(segments[i])) {
                appId = segments[i + 1];
                break;
            }
        }
        return appId;
    }

    private String extractAppIdFromIdToken(String token) {
        String appId = null;
        KeyStoreManager keyStoreManager = KeyStoreManager.getInstance(MultitenantConstants.SUPER_TENANT_ID);
        try {
            keyStoreManager.getDefaultPrimaryCertificate();
            JWSVerifier verifier =
                    new RSASSAVerifier((RSAPublicKey) keyStoreManager.getDefaultPublicKey());
            SignedJWT jwsObject = SignedJWT.parse(token);
            if (jwsObject.verify(verifier)) {
                appId = jwsObject.getJWTClaimsSet().getStringClaim("appId");
            }

        } catch (Exception e) {
            String message = "Could not extract application id from id token";
            log.error(message, e);
        }
        return appId;
    }
}

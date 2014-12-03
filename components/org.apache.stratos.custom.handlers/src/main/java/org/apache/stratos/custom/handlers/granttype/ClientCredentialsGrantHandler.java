/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.custom.handlers.granttype;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.openidconnect.as.messages.IDTokenBuilder;
import org.apache.oltu.openidconnect.as.messages.IDTokenException;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dto.OAuth2AccessTokenRespDTO;
import org.wso2.carbon.identity.oauth2.token.OAuthTokenReqMessageContext;
import org.wso2.carbon.identity.oauth2.token.handlers.grant.AbstractAuthorizationGrantHandler;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Grant Handler for Grant Type : client_credentials
 */
public class ClientCredentialsGrantHandler extends AbstractAuthorizationGrantHandler {

    private static Log log = LogFactory.getLog(ClientCredentialsGrantHandler.class);
    private static ConcurrentHashMap<Integer, Key> privateKeys =
            new ConcurrentHashMap<Integer, Key>();
    private static ConcurrentHashMap<Integer, Certificate> publicCerts =
            new ConcurrentHashMap<Integer, Certificate>();

    @Override
    public boolean validateGrant(OAuthTokenReqMessageContext tokReqMsgCtx)
            throws IdentityOAuth2Exception {
        // By this time, we have already validated client credentials.
        tokReqMsgCtx.setScope(tokReqMsgCtx.getOauth2AccessTokenReqDTO().getScope());
        return true;
    }

    public boolean issueRefreshToken() throws IdentityOAuth2Exception {
        return false;
    }

    public boolean isOfTypeApplicationUser() throws IdentityOAuth2Exception {
        return false;
    }

    public OAuth2AccessTokenRespDTO issue(OAuthTokenReqMessageContext tokReqMsgCtx) throws IdentityOAuth2Exception {

        OAuthServerConfiguration config = OAuthServerConfiguration.getInstance();
        String issuer = config.getOpenIDConnectIDTokenIssuerIdentifier();
        String subject = tokReqMsgCtx.getAuthorizedUser();
        String audience = tokReqMsgCtx.getOauth2AccessTokenReqDTO().getClientId();
        String authorizedParty = tokReqMsgCtx.getOauth2AccessTokenReqDTO().getClientId();

        OAuth2AccessTokenRespDTO tokenRespDTO = getTokenDTO(tokReqMsgCtx);
        int lifetime = Integer.parseInt(config.getOpenIDConnectIDTokenExpiration()) * 1000;
        int curTime = (int) Calendar.getInstance().getTimeInMillis();

        String applicationId = tokReqMsgCtx.getScope()[0];

        IDTokenBuilder builder =
                new IDTokenBuilder().setIssuer(issuer)
                        .setSubject(subject)
                        .setAudience(audience)
                        .setAuthorizedParty(authorizedParty)
                        .setExpiration(curTime + lifetime)
                        .setIssuedAt((int) Calendar.getInstance().getTimeInMillis())
                        .setClaim("appId", applicationId);

        String plainIDToken;
        try {
            plainIDToken = builder.buildIDToken();
        } catch (IDTokenException e) {
            String message = "Error while building ID token";
            throw new RuntimeException(message, e);
        }

        String signedJwtKey;
        try {
            PlainJWT plainJWT = PlainJWT.parse(plainIDToken);
            plainIDToken = plainJWT.serialize();
            signedJwtKey = signJWT(plainIDToken, tokReqMsgCtx);
        } catch (ParseException e) {
            String message = "Error while passing ID token";
            throw new RuntimeException(message, e);
        }

        tokenRespDTO.setIDToken(signedJwtKey);
        return tokenRespDTO;
    }

    private OAuth2AccessTokenRespDTO getTokenDTO(OAuthTokenReqMessageContext tokReqMsgCtx) throws IdentityOAuth2Exception {
        return super.issue(tokReqMsgCtx);
    }

    protected String signJWT(String payLoad, OAuthTokenReqMessageContext request)
            throws IdentityOAuth2Exception {
        JWSAlgorithm jwsAlgorithm =
                mapSignatureAlgorithm(OAuthServerConfiguration.getInstance()
                        .getSignatureAlgorithm());
        if (JWSAlgorithm.RS256.equals(jwsAlgorithm) || JWSAlgorithm.RS384.equals(jwsAlgorithm) ||
                JWSAlgorithm.RS512.equals(jwsAlgorithm)) {
            return signJWTWithRSA(payLoad, jwsAlgorithm, request);
        }
        log.error("UnSupported Signature Algorithm");
        throw new IdentityOAuth2Exception("UnSupported Signature Algorithm");
    }

    protected String signJWTWithRSA(String payLoad, JWSAlgorithm jwsAlgorithm,
                                    OAuthTokenReqMessageContext request)
            throws IdentityOAuth2Exception {
        try {
            String tenantDomain = request.getOauth2AccessTokenReqDTO().getTenantDomain();
            int tenantId = request.getTenantID();
            if (tenantDomain == null) {
                tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
            }
            if (tenantId == 0) {
                tenantId = MultitenantConstants.SUPER_TENANT_ID;
            }
            Key privateKey = null;

            if (!(privateKeys.containsKey(tenantId))) {
                // get tenant's key store manager
                KeyStoreManager tenantKSM = KeyStoreManager.getInstance(tenantId);

                if (!tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
                    // derive key store name
                    String ksName = tenantDomain.trim().replace(".", "-");
                    String jksName = ksName + ".jks";
                    // obtain private key
                    privateKey = tenantKSM.getPrivateKey(jksName, tenantDomain);

                } else {
                    try {
                        privateKey = tenantKSM.getDefaultPrivateKey();
                    } catch (Exception e) {
                        log.error("Error while obtaining private key for super tenant", e);
                    }
                }
                if (privateKey != null) {
                    privateKeys.put(tenantId, privateKey);
                }
            } else {
                privateKey = privateKeys.get(tenantId);
            }

            Certificate publicCert;

            if (!(publicCerts.containsKey(tenantId))) {
                // get tenant's key store manager
                KeyStoreManager tenantKSM = KeyStoreManager.getInstance(tenantId);

                KeyStore keyStore;
                if (!tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
                    // derive key store name
                    String ksName = tenantDomain.trim().replace(".", "-");
                    String jksName = ksName + ".jks";
                    keyStore = tenantKSM.getKeyStore(jksName);
                    publicCert = keyStore.getCertificate(tenantDomain);
                } else {
                    publicCert = tenantKSM.getDefaultPrimaryCertificate();
                }
                if (publicCert != null) {
                    publicCerts.put(tenantId, publicCert);
                }
            } else {
                publicCert = publicCerts.get(tenantId);
            }

            JWSSigner signer = new RSASSASigner((RSAPrivateKey) privateKey);
            SignedJWT signedJWT =
                    new SignedJWT(new JWSHeader(jwsAlgorithm),
                            PlainJWT.parse(payLoad).getJWTClaimsSet());
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (KeyStoreException e) {
            log.error("Error in obtaining tenant's keystore", e);
            throw new IdentityOAuth2Exception("Error in obtaining tenant's keystore", e);
        } catch (JOSEException e) {
            log.error("Error in obtaining tenant's keystore", e);
            throw new IdentityOAuth2Exception("Error in obtaining tenant's keystore", e);
        } catch (Exception e) {
            log.error("Error in obtaining tenant's keystore", e);
            throw new IdentityOAuth2Exception("Error in obtaining tenant's keystore", e);
        }
    }

    protected JWSAlgorithm mapSignatureAlgorithm(String signatureAlgorithm)
            throws IdentityOAuth2Exception {
        if ("SHA256withRSA".equals(signatureAlgorithm)) {
            return JWSAlgorithm.RS256;
        } else if ("SHA384withRSA".equals(signatureAlgorithm)) {
            return JWSAlgorithm.RS384;
        } else if ("SHA512withRSA".equals(signatureAlgorithm)) {
            return JWSAlgorithm.RS512;
        } else if ("SHA256withHMAC".equals(signatureAlgorithm)) {
            return JWSAlgorithm.HS256;
        } else if ("SHA384withHMAC".equals(signatureAlgorithm)) {
            return JWSAlgorithm.HS384;
        } else if ("SHA512withHMAC".equals(signatureAlgorithm)) {
            return JWSAlgorithm.HS512;
        } else if ("SHA256withEC".equals(signatureAlgorithm)) {
            return JWSAlgorithm.ES256;
        } else if ("SHA384withEC".equals(signatureAlgorithm)) {
            return JWSAlgorithm.ES384;
        } else if ("SHA512withEC".equals(signatureAlgorithm)) {
            return JWSAlgorithm.ES512;
        }
        log.error("Unsupported Signature Algorithm in identity.xml");
        throw new IdentityOAuth2Exception("Unsupported Signature Algorithm in identity.xml");
    }
}

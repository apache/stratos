/*
 *  Copyright (c) WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.apache.stratos.custom.handlers.authentication;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.apache.axiom.util.base64.Base64Utils;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.custom.handlers.internal.SignedJWTAuthenticatorServiceComponent;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.core.security.AuthenticatorsConfiguration;
import org.wso2.carbon.core.services.authentication.CarbonServerAuthenticator;
import org.wso2.carbon.core.services.util.CarbonAuthenticationUtil;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.user.api.TenantManager;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.utils.AuthenticationObserver;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.HttpServletRequest;
import java.security.interfaces.RSAPublicKey;

/**
 * SignedJWTAuthenticator Authenticate a user by a JWT token. JWT token should contains
 * a username as a claim and that user should be a valid user.
 */
public class SignedJWTAuthenticator implements CarbonServerAuthenticator {

    private static final int DEFAULT_PRIORITY_LEVEL = 20;
    private static final String AUTHENTICATOR_NAME = "SignedJWTAuthenticator";
    private static final String AUTHORIZATION_HEADER_TYPE = "Bearer";
    private static final String SIGNED_JWT_AUTH_USERNAME = "Username";

    private static final Log log = LogFactory.getLog(SignedJWTAuthenticator.class);

    @Override
    public int getPriority() {
        AuthenticatorsConfiguration authenticatorsConfiguration =
                AuthenticatorsConfiguration.getInstance();
        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig =
                authenticatorsConfiguration.getAuthenticatorConfig(AUTHENTICATOR_NAME);
        if (authenticatorConfig != null && authenticatorConfig.getPriority() > 0) {
            return authenticatorConfig.getPriority();
        }
        return DEFAULT_PRIORITY_LEVEL;
    }

    @Override
    public boolean isDisabled() {
        AuthenticatorsConfiguration authenticatorsConfiguration =
                AuthenticatorsConfiguration.getInstance();
        AuthenticatorsConfiguration.AuthenticatorConfig authenticatorConfig =
                authenticatorsConfiguration.getAuthenticatorConfig(AUTHENTICATOR_NAME);
        return authenticatorConfig != null && authenticatorConfig.isDisabled();
    }

    @Override
    public boolean authenticateWithRememberMe(MessageContext msgCxt) {
        return false;
    }

    @Override
    public String getAuthenticatorName() {
        return AUTHENTICATOR_NAME;
    }

    @Override
    public boolean isAuthenticated(MessageContext msgCxt) {
        boolean isAuthenticated = false;
        HttpServletRequest request =
                (HttpServletRequest) msgCxt.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        try {
            //Get the filesystem keystore default primary certificate
            KeyStoreManager keyStoreManager = KeyStoreManager.getInstance(
                    MultitenantConstants.SUPER_TENANT_ID);
            keyStoreManager.getDefaultPrimaryCertificate();

            String authorizationHeader = request.getHeader(HTTPConstants.HEADER_AUTHORIZATION);
            String headerData = decodeAuthorizationHeader(authorizationHeader);

            JWSVerifier verifier =
                    new RSASSAVerifier((RSAPublicKey) keyStoreManager.getDefaultPublicKey());
            SignedJWT jwsObject = SignedJWT.parse(headerData);

            if (jwsObject.verify(verifier)) {
                String userName = jwsObject.getJWTClaimsSet().getStringClaim(SIGNED_JWT_AUTH_USERNAME);
                String tenantDomain = MultitenantUtils.getTenantDomain(userName);
                userName = MultitenantUtils.getTenantAwareUsername(userName);
                TenantManager tenantManager = SignedJWTAuthenticatorServiceComponent
                        .getRealmService().getTenantManager();
                int tenantId = tenantManager.getTenantId(tenantDomain);

                handleAuthenticationStarted(tenantId);

                UserStoreManager userStore = SignedJWTAuthenticatorServiceComponent
                        .getRealmService().getTenantUserRealm(tenantId).getUserStoreManager();
                if (userStore.isExistingUser(userName)) {
                    isAuthenticated = true;
                }

                if (isAuthenticated) {
                    CarbonAuthenticationUtil.onSuccessAdminLogin(request.getSession(), userName,
                            tenantId, tenantDomain,
                            "Signed JWT Authentication");
                    handleAuthenticationCompleted(tenantId, true);
                    return true;
                } else {
                    log.error(
                            "Authentication Request is rejected. User does not exists in UserStore");
                    CarbonAuthenticationUtil
                            .onFailedAdminLogin(request.getSession(), userName, tenantId,
                                    "Signed JWT Authentication",
                                    "User does not exists in UserStore");
                    handleAuthenticationCompleted(tenantId, false);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Error authenticating the user " + e.getMessage(), e);
        }
        return isAuthenticated;
    }

    @Override
    public boolean isHandle(MessageContext msgCxt) {
        HttpServletRequest request =
                (HttpServletRequest) msgCxt.getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        String authorizationHeader = request.getHeader(HTTPConstants.HEADER_AUTHORIZATION);
        if (authorizationHeader != null) {
            String authType = getAuthType(authorizationHeader);
            if (authType != null && authType.equalsIgnoreCase(AUTHORIZATION_HEADER_TYPE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the authentication type in authorization header.
     *
     * @param authorizationHeader The authorization header - Authorization: Bearer QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
     * @return The authentication type mentioned in authorization header.
     */
    private String getAuthType(String authorizationHeader) {
        String[] splitValues = null;
        if (authorizationHeader != null) {
            splitValues = authorizationHeader.trim().split(" ");
        }
        if (splitValues == null || splitValues.length == 0) {
            return null;
        }
        return splitValues[0].trim();
    }

    private String decodeAuthorizationHeader(String authorizationHeader) {
        String[] splitValues = authorizationHeader.trim().split(" ");
        byte[] decodedBytes = Base64Utils.decode(splitValues[1].trim());
        if (decodedBytes != null) {
            return new String(decodedBytes);
        } else {
            log.debug(
                    "Error decoding authorization header. Could not retrieve user name and password.");
            return null;
        }
    }

    private void handleAuthenticationStarted(int tenantId) {
        BundleContext bundleContext = SignedJWTAuthenticatorServiceComponent.getBundleContext();
        if (bundleContext != null) {
            ServiceTracker tracker =
                    new ServiceTracker(bundleContext,
                            AuthenticationObserver.class.getName(), null);
            tracker.open();
            Object[] services = tracker.getServices();
            if (services != null) {
                for (Object service : services) {
                    ((AuthenticationObserver) service).startedAuthentication(tenantId);
                }
            }
            tracker.close();
        }
    }

    private void handleAuthenticationCompleted(int tenantId, boolean isSuccessful) {
        BundleContext bundleContext = SignedJWTAuthenticatorServiceComponent.getBundleContext();
        if (bundleContext != null) {
            ServiceTracker tracker =
                    new ServiceTracker(bundleContext,
                            AuthenticationObserver.class.getName(), null);
            tracker.open();
            Object[] services = tracker.getServices();
            if (services != null) {
                for (Object service : services) {
                    ((AuthenticationObserver) service).completedAuthentication(
                            tenantId, isSuccessful);
                }
            }
            tracker.close();
        }
    }

}

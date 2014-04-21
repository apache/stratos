/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

/**
 * This class is used to maintain a mapping between the session indexes of the SSO Identity Provider
 * end and the relying party end. When a user is authenticated and logged-in using SSO, an entry is
 * added to the validSessionMap where Idp-session-index --> RP-Session-id.
 * 
 * When he logs out from either of SSO relying party, a SAML2 LogoutRequest is sent to all the
 * relying party service providers who have established sessions with the Identity Provider at that
 * moment. When a relying party receives a logout request, it should validate the request and
 * extract the IdP session index from the request. Then it should identify the sessionId of the
 * corresponding user which represents the session established at the relying party end. Then it
 * removes that session from the validSessionMap and includes it to the invalidSessionsMap. So when
 * a user tries to do some activity thereafter he should be logged-out from the system.
 * 
 * This class maintains two maps to maintain valid sessions and invalid sessions. This class is
 * implemented as a singleton because there should be only one SSOSessionManager per instance.
 */
public class CarbonSSOSessionManager {

    private static Log log = LogFactory.getLog(CarbonSSOSessionManager.class);

    /**
     * CarbonSSOSessionManager instance which is used as the singleton instance
     */
    private static CarbonSSOSessionManager instance = new CarbonSSOSessionManager();

    /**
     * This hash map is used to maintain a map of valid sessions. IdpSessionIndex is used as the key
     * while the RPSessionId is used as the value.
     */
    private ConcurrentHashMap<String, String> validSessionMap = new ConcurrentHashMap<String, String>();

    /**
     * This hash map is used to maintain the invalid sessions. RPSessionIndex is used as the key
     * while IdpSessionIndex is used as the value.
     */
    private ConcurrentHashMap<String, String> invalidSessionsMap = new ConcurrentHashMap<String, String>();

    /**
     * Private Constructor since we are implementing a Singleton here
     */
    private CarbonSSOSessionManager() {

    }

    /**
     * Get the CarbonSSOSessionManager instance.
     * 
     * @return CarbonSSOSessionManager instance
     */
    public static CarbonSSOSessionManager getInstance() {
        return instance;
    }

    /**
     * Add a new session mapping : IdpSessionIndex --> localSessionId
     * 
     * @param idPSessionIndex session index sent along in the SAML Response
     * @param localSessionId id of the current session established locally.
     */
    public void addSessionMapping(String idPSessionIndex, String localSessionId) {
        validSessionMap.put(idPSessionIndex, localSessionId);
    }

    /**
     * make a session invalid after receiving the single logout request from the identity provider
     * 
     * @param idPSessionIndex session index established at the identity provider's end
     */
    public void makeSessionInvalid(String idPSessionIndex) {
        if (validSessionMap.containsKey(idPSessionIndex)) {
            // add the invalid session to the invalidSessionMap
            invalidSessionsMap.put(validSessionMap.get(idPSessionIndex), idPSessionIndex);
            // remove the invalid session from the valid session map
            validSessionMap.remove(idPSessionIndex);
        }
    }

    /**
     * Check whether a particular session is valid.
     * 
     * @param localSessionId session id established locally
     * @return true, if the session is valid, false otherwise
     */
    public boolean isSessionValid(String localSessionId) {
        boolean isSessionValid = true;
        if (invalidSessionsMap.containsKey(localSessionId)) {
            isSessionValid = false;
        }
        return isSessionValid;
    }

    /**
     * Remove invalid session from the invalid session map. This needs to be done before completing
     * the sign out.
     * 
     * @param localSessionId SessionId established locally
     */
    public void removeInvalidSession(String localSessionId) {
        if (invalidSessionsMap.containsKey(localSessionId)) {
            invalidSessionsMap.remove(localSessionId);
        }
    }

    /**
     * This method checks whether the request is for a SSO authentication related page or servlet.
     * If it is so, the session invalidation should be skipped.
     * 
     * @param request Request, HTTPServletRequest
     * @return true, if session invalidation should be skipped.
     */
    public boolean skipSSOSessionInvalidation(HttpServletRequest request,
            CarbonUIAuthenticator uiAuthenticator) {

        String requestedURI = request.getRequestURI();

        if (uiAuthenticator != null) {
            List<String> skippingUrls = uiAuthenticator.getSessionValidationSkippingUrls();
            return skip(requestedURI, skippingUrls);
        } else {
            return false;

        }
    }

    /**
     * Skips authentication for given URI's.
     * 
     * @param request The request to access a page.
     * @return <code>true</code> if request doesnt need to authenticate, else <code>false</code>.
     */
    public boolean skipAuthentication(HttpServletRequest request) {

        String requestedURI = request.getRequestURI();
        CarbonUIAuthenticator uiAuthenticator = CarbonUILoginUtil.getAuthenticator(request);

        if (uiAuthenticator != null) {
            List<String> skippingUrls = uiAuthenticator.getAuthenticationSkippingUrls();
            return skip(requestedURI, skippingUrls);
        } else {
            return false;

        }
    }

    /**
     * 
     * @param request
     * @return
     */
    public String getRequestedUrl(HttpServletRequest request, CarbonUIAuthenticator uiAuthenticator) {
        String requestedURI = request.getRequestURI();
        boolean skipSessionValidation = skipSSOSessionInvalidation(request, uiAuthenticator);
        boolean isSessionValid = isSessionValid(request.getSession().getId());

        if (!skipSessionValidation && !isSessionValid) {
            requestedURI = "/carbon/admin/logout_action.jsp";
            if(log.isDebugEnabled()) {
            	log.debug("Request URI changed to " + requestedURI);
            }
        }

        if (skipSessionValidation && !isSessionValid) {
            removeInvalidSession(request.getSession().getId());
        }

        return requestedURI;
    }

    /**
     * 
     * @param requestedURI
     * @param skippingUrls
     * @return
     */
    private boolean skip(String requestedURI, List<String> skippingUrls) {

        for (String skippingUrl : skippingUrls) {
            if (requestedURI.contains(skippingUrl)) {
                return true;
            }
        }

        return false;
    }

}

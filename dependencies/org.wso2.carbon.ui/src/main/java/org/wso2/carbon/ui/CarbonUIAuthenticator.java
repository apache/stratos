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

import org.wso2.carbon.core.common.AuthenticationException;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * This is the interface of the Authenticator to Carbon UI framework. Implement this interface in a
 * bundle and drop-in to the framework and it will be automatically called. The authenticator has
 * the freedom to decide how it does the authentication.
 * 
 * When an authentication request is received by the framework each authenticator will be checked to
 * see who handles the request, i.e. isHandle() method of each authenticator will be called in the
 * priority order given by the getPriority() method. Highest priority authenticator that returns
 * true for the isHandle() method will be given the responsibility of authentication.
 * 
 * Priority is defined as higher the number higher the priority.
 */
public interface CarbonUIAuthenticator {

    /**
     * This method will check whether given request can be handled by the authenticator. If
     * authenticator is capable of handling given request this method will return <code>true</code>.
     * Else this will return <code>false</code>.
     * 
     * @param object The request to authenticate.
     * @return <code>true</code> if this authenticator can handle the request, else
     *         <code>false</code>.
     */
    boolean canHandle(HttpServletRequest request);

    /**
     * Authenticates the given request.
     * 
     * @param object The request to be authenticate.
     * @return <code>true</code> if authentication is successful, else <code>false</code>.
     * @throws AuthenticationException If an error occurred during authentication process.
     */
    void authenticate(HttpServletRequest request) throws AuthenticationException;

    /**
     * Handles authentication during a session expiration. Usually the request is a cookie.
     * 
     * @param object The request to be re-authenticated.
     * @return <code>true</code> if re-authentication is successful, else <code>false</code>.
     * @throws AuthenticationException If an error occurred during authentication process.
     */
    void authenticateWithCookie(HttpServletRequest request) throws AuthenticationException;

    /**
     * Invalidates the authentication session. TODO why we need this ? An authenticator should not
     * maintain a session
     * 
     * @param object The request to invalidate TODO (?)
     * @throws Exception If an error occurred during authentication process.
     */
    void unauthenticate(Object object) throws Exception;

    /**
     * Gets the authenticator priority.
     * 
     * @return The integer representing the priority. Higher the value, higher the priority.
     */
    int getPriority();

    /**
     * Returns the name of the authenticator.
     * 
     * @return The name of the authenticator.
     */
    String getAuthenticatorName();

    /**
     * By default all the authenticators found in the system are enabled. Can use this property to
     * control default behavior.
     * 
     * @return <code>true</code> if this authenticator is disabled, else <code>false</code>.
     */
    boolean isDisabled();

    /**
     * Gets a list of urls to skip authentication. Also see
     * CarbonSecuredHttpContext.skipSSOSessionInvalidation for more information.
     * 
     * @return A list of urls to skip authentication.
     */
    List<String> getAuthenticationSkippingUrls();

    /**
     * Gets a list of urls to skip session validation. Also see
     * CarbonSecuredHttpContext.skipSSOSessionInvalidation for more information.
     * 
     * @return A list of urls to skip session validation.
     */
    List<String> getSessionValidationSkippingUrls();

    /**
     * If Authenticator does not need to have login page - set this to true.
     * 
     * @return
     */
    boolean skipLoginPage();

}

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
package org.wso2.carbon.ui.tracker;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.wso2.carbon.ui.CarbonSecuredHttpContext;
import org.wso2.carbon.ui.CarbonUIAuthenticator;

public class AuthenticatorRegistry {

    private static Log log = LogFactory.getLog(AuthenticatorRegistry.class);
    private static ServiceTracker authTracker;
    private static CarbonUIAuthenticator[] authenticators;
    private static Object lock = new Object();

    public static final String AUTHENTICATOR_TYPE = "authenticator.type";

    public static void init(BundleContext bc) throws Exception {
        try {
            authTracker = new ServiceTracker(bc, CarbonUIAuthenticator.class.getName(), null);
            authTracker.open();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    // At the time getCarbonAuthenticator() method been invoked, all the authenticators should be
    // registered.
    public static CarbonUIAuthenticator getCarbonAuthenticator(HttpServletRequest request) {

        HttpSession session = request.getSession();
        CarbonUIAuthenticator authenticator;

        if ((authenticator = (CarbonUIAuthenticator) session
                .getAttribute(CarbonSecuredHttpContext.CARBON_AUTHNETICATOR)) != null) {
            return authenticator;
        }

        if (authenticators == null || authenticators.length == 0 || authenticators[0] == null) {
            synchronized (lock) {
                if (authenticators == null || authenticators.length == 0
                        || authenticators[0] == null) {
                    Object[] objects = authTracker.getServices();
                    // cast each object - cannot cast object array
                    authenticators = new CarbonUIAuthenticator[objects.length];
                    int i = 0;
                    for (Object obj : objects) {
                        authenticators[i] = (CarbonUIAuthenticator) obj;
                        i++;
                    }
                    Arrays.sort(authenticators, new AuthenticatorComparator());
                }
            }
        }

        for (CarbonUIAuthenticator auth : authenticators) {
            if (!auth.isDisabled() && auth.canHandle(request)) {
                session.setAttribute(CarbonSecuredHttpContext.CARBON_AUTHNETICATOR, auth);
                return auth;
            }
        }

        return null;
    }
}

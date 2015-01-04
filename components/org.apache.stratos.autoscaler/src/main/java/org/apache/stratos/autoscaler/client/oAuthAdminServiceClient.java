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
package org.apache.stratos.autoscaler.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.Constants;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.identity.oauth.stub.OAuthAdminServiceException;
import org.wso2.carbon.identity.oauth.stub.OAuthAdminServiceStub;
import org.wso2.carbon.identity.oauth.stub.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.utils.CarbonUtils;

import java.rmi.RemoteException;

public class OAuthAdminServiceClient {

    public static final String GRANT_TYPE = "client-credentials";
    private static final Log log = LogFactory.getLog(OAuthAdminServiceClient.class);
    private static final String OAUTH_2_0 = "oauth-2.0";
    private static OAuthAdminServiceClient serviceClient;
    private final OAuthAdminServiceStub stub;

    public OAuthAdminServiceClient(String epr) throws AxisFault {

        XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
        int autosclaerSocketTimeout   = conf.getInt("autoscaler.identity.clientTimeout", 180000);

        try {
            ServerConfiguration serverConfig = CarbonUtils.getServerConfiguration();
            String trustStorePath = serverConfig.getFirstProperty("Security.TrustStore.Location");
            String trustStorePassword = serverConfig.getFirstProperty("Security.TrustStore.Password");
            String type = serverConfig.getFirstProperty("Security.TrustStore.Type");
            System.setProperty("javax.net.ssl.trustStore", trustStorePath);
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
            System.setProperty("javax.net.ssl.trustStoreType", type);

            stub = new OAuthAdminServiceStub(epr);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, autosclaerSocketTimeout);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, autosclaerSocketTimeout);
            //String username = CarbonContext.getThreadLocalCarbonContext().getUsername();
            //TODO StratosAuthenticationHandler does not set to carbon context, thus user name becomes null.
            // For the moment username is hardcoded since above is fixed.
            String username = "admin";
            Utility.setAuthHeaders(stub._getServiceClient(), username);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate identity service client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new AxisFault(msg, axisFault);
        }
    }

    public static OAuthAdminServiceClient getServiceClient() throws AxisFault {
        if (serviceClient == null) {
            synchronized (OAuthAdminServiceClient.class) {
                if (serviceClient == null) {
                    XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
                    String hostname   = conf.getString("autoscaler.identity.hostname", "localhost");
                    int port = conf.getInt("autoscaler.cloudController.port", Constants.IS_DEFAULT_PORT);
                    String epr = "https://" + hostname + ":" + port + "/" + Constants.OAUTH_SERVICE_SFX;
                    serviceClient = new OAuthAdminServiceClient(epr);
                }
            }
        }
        return serviceClient;
    }

    public void registerOauthApplication(String appName) throws RemoteException, OAuthAdminServiceException {
        OAuthConsumerAppDTO oAuthConsumerDTO = new OAuthConsumerAppDTO();
        oAuthConsumerDTO.setApplicationName(appName);
        oAuthConsumerDTO.setOAuthVersion(OAUTH_2_0);
        oAuthConsumerDTO.setGrantTypes(GRANT_TYPE);
        if(log.isDebugEnabled()){
            log.debug(String.format("Trying to register oAuth application [%s]", appName));
        }
        stub.registerOAuthApplicationData(oAuthConsumerDTO);
    }

    public OAuthConsumerAppDTO getOAuthApplication(String name) throws RemoteException, OAuthAdminServiceException {
        return stub.getOAuthApplicationDataByAppName(name);
    }

    public void removeOauthApplication(String appName) throws RemoteException, OAuthAdminServiceException {
        if(log.isDebugEnabled()){
            log.debug(String.format("Removing oAuth application %s", appName));
        }
        stub.removeOAuthApplicationData(appName);
    }
}

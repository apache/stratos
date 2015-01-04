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

import org.apache.amber.oauth2.client.OAuthClient;
import org.apache.amber.oauth2.client.URLConnectionClient;
import org.apache.amber.oauth2.client.request.OAuthClientRequest;
import org.apache.amber.oauth2.client.response.OAuthClientResponse;
import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.axis2.AxisFault;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.Constants;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.identity.application.common.model.xsd.InboundAuthenticationRequestConfig;
import org.wso2.carbon.identity.application.common.model.xsd.OutboundProvisioningConfig;
import org.wso2.carbon.identity.application.common.model.xsd.Property;
import org.wso2.carbon.identity.application.common.model.xsd.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.stub.IdentityApplicationManagementServiceIdentityApplicationManagementException;
import org.wso2.carbon.identity.application.mgt.stub.IdentityApplicationManagementServiceStub;
import org.wso2.carbon.identity.oauth.stub.OAuthAdminServiceException;
import org.wso2.carbon.identity.oauth.stub.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.utils.CarbonUtils;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class IdentityApplicationManagementServiceClient {

    private static final Log log = LogFactory.getLog(IdentityApplicationManagementServiceClient.class);
    private static final String ID_TOKEN = "id_token";

    private static IdentityApplicationManagementServiceClient serviceClient;
    private final IdentityApplicationManagementServiceStub stub;

    public IdentityApplicationManagementServiceClient(String epr) throws AxisFault {

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

            stub = new IdentityApplicationManagementServiceStub(epr);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, autosclaerSocketTimeout);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, autosclaerSocketTimeout);
            Utility.setAuthHeaders(stub._getServiceClient(), "admin");

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate identity service client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new AxisFault(msg, axisFault);
        }
    }

    public static IdentityApplicationManagementServiceClient getServiceClient() throws AxisFault {
        if (serviceClient == null) {
            synchronized (IdentityApplicationManagementServiceClient.class) {
                if (serviceClient == null) {
                    XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
                    String hostname   = conf.getString("autoscaler.identity.hostname", "localhost");
                    int port = conf.getInt("autoscaler.cloudController.port", Constants.IS_DEFAULT_PORT);
                    String epr = "https://" + hostname + ":" + port + "/" + Constants.IDENTITY_APPLICATION_SERVICE_SFX;
                    serviceClient = new IdentityApplicationManagementServiceClient(epr);
                }
            }
        }
        return serviceClient;
    }

    public String createServiceProvider(String appName, String spName, String compositeAppId) throws RemoteException, OAuthAdminServiceException, OAuthProblemException, OAuthSystemException {
        OAuthConsumerAppDTO oAuthApplication = null;
        String accessToken;

        oAuthApplication = OAuthAdminServiceClient.getServiceClient().getOAuthApplication(appName);

        if(oAuthApplication == null){
            return null;
        }

        String consumerKey = oAuthApplication.getOauthConsumerKey();
        String consumerSecret = oAuthApplication.getOauthConsumerSecret();

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setApplicationName(spName);

        try {
            stub.createApplication(serviceProvider);
        } catch (IdentityApplicationManagementServiceIdentityApplicationManagementException e) {
            throw new RuntimeException(e);
        }
        try {
            serviceProvider = stub.getApplication(spName);
        } catch (IdentityApplicationManagementServiceIdentityApplicationManagementException e) {
            throw new RuntimeException(e);
        }

        serviceProvider.setOutboundProvisioningConfig(new OutboundProvisioningConfig());

        List<InboundAuthenticationRequestConfig> authRequestList = new ArrayList<InboundAuthenticationRequestConfig>();


        if (consumerKey != null) {
            InboundAuthenticationRequestConfig opicAuthenticationRequest =
                    new InboundAuthenticationRequestConfig();
            opicAuthenticationRequest.setInboundAuthKey(consumerKey);
            opicAuthenticationRequest.setInboundAuthType("oauth2");
            if (consumerSecret != null && !consumerSecret.isEmpty()) {
                Property property = new Property();
                property.setName("oauthConsumerSecret");
                property.setValue(consumerSecret);
                Property[] properties = {property};
                opicAuthenticationRequest.setProperties(properties);
            }
            authRequestList.add(opicAuthenticationRequest);
        }

        String passiveSTSRealm = spName;
        if (passiveSTSRealm != null) {
            InboundAuthenticationRequestConfig opicAuthenticationRequest =
                    new InboundAuthenticationRequestConfig();
            opicAuthenticationRequest.setInboundAuthKey(passiveSTSRealm);
            opicAuthenticationRequest.setInboundAuthType("passivests");
            authRequestList.add(opicAuthenticationRequest);
        }

        String openidRealm = spName;
        if (openidRealm != null) {
            InboundAuthenticationRequestConfig opicAuthenticationRequest =
                    new InboundAuthenticationRequestConfig();
            opicAuthenticationRequest.setInboundAuthKey(openidRealm);
            opicAuthenticationRequest.setInboundAuthType("openid");
            authRequestList.add(opicAuthenticationRequest);
        }

        if (authRequestList.size() > 0) {
            serviceProvider.getInboundAuthenticationConfig()
                    .setInboundAuthenticationRequestConfigs(authRequestList.toArray(new InboundAuthenticationRequestConfig[authRequestList.size()]));
        }

        try {
            stub.updateApplication(serviceProvider);
        } catch (IdentityApplicationManagementServiceIdentityApplicationManagementException e) {
            throw new RuntimeException(e);
        }

        accessToken = getIdToken(compositeAppId, consumerKey, consumerSecret);
        return accessToken;
    }

    public void removeApplication(String appName) throws RemoteException, IdentityApplicationManagementServiceIdentityApplicationManagementException {
        if(log.isDebugEnabled()){
            log.debug(String.format("Removing application %s", appName));
        }
        stub.deleteApplication(appName);
    }

    private String getIdToken(String compositeAppId, String consumerKey, String consumerSecret) throws OAuthSystemException, OAuthProblemException {
        XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
        String hostname   = conf.getString("autoscaler.identity.hostname", "localhost");
        int port = conf.getInt("autoscaler.cloudController.port", Constants.IS_DEFAULT_PORT);
        String tokenEndpoint = "https://" + hostname + ":" + port + "/" + Constants.TOKEN_ENDPOINT_SFX;
            OAuthClientRequest accessRequest = OAuthClientRequest.tokenLocation(tokenEndpoint)
                    .setGrantType(GrantType.CLIENT_CREDENTIALS)
                    .setClientId(consumerKey)
                    .setClientSecret(consumerSecret)
                    .setScope(compositeAppId)
                    .buildBodyMessage();
            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

            OAuthClientResponse oAuthResponse = oAuthClient.accessToken(accessRequest);
        return oAuthResponse.getParam(ID_TOKEN);
    }
}

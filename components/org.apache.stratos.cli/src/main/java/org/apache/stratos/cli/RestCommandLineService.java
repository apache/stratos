/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.stratos.cli.utils.RowMapper;
import org.apache.stratos.cli.utils.CommandLineUtils;

import javax.net.ssl.*;

public class RestCommandLineService {

	private static final Logger logger = LoggerFactory.getLogger(RestCommandLineService.class);

    private RestClient restClientService;

    private final String initializeEndpoint = "/stratos/admin/init";
    private final String listAvailableCartridgesRestEndpoint = "/stratos/admin/cartridge/list";
    private final String listSubscribedCartridgesRestEndpoint = "/stratos/admin/cartridge/list/subscribed";
    private final String subscribCartridgeRestEndpoint = "/stratos/admin/cartridge/subscribe";
    private final String addTenantEndPoint = "/stratos/admin/tenant";
    private final String unsubscribeTenantEndPoint = "/stratos/admin/cartridge/unsubscribe";
    private final String cartridgeDeploymentEndPoint = "/stratos/admin/cartridge/definition";
    private final String partitionDeploymentEndPoint = "/stratos/admin/policy/deployment/partition";

    private static class SingletonHolder {
		private final static RestCommandLineService INSTANCE = new RestCommandLineService();
	}

	public static RestCommandLineService getInstance() {
		return SingletonHolder.INSTANCE;
	}

    public boolean login(String serverURL, String username, String password, boolean validateLogin) throws Exception {
        try {
            // Following code will avoid validating certificate
            SSLContext sc;
            // Get SSL context
            sc = SSLContext.getInstance("SSL");
            // Create empty HostnameVerifier
            HostnameVerifier hv = new HostnameVerifier() {
                public boolean verify(String urlHostName, SSLSession session) {
                    return true;
                }
            };
            // Create a trust manager that does not validate certificate
            // chains
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
            } };
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLContext.setDefault(sc);
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception e) {
            throw new RuntimeException("Error while authentication process!", e);
        }

        // Initialized client
        try {
            initializeRestClient(serverURL, username, password);
        } catch (AxisFault e) {
            System.out.println("Error connecting to the back-end");
            throw new CommandException(e);
        }

        try {
            if (validateLogin) {
                restClientService.doPost(restClientService.getUrl() + initializeEndpoint, "",
                        restClientService.getUsername(), restClientService.getPassword());
                if (logger.isDebugEnabled()) {
                    logger.debug("Tenant Domain {}", restClientService.getUsername());
                }
                System.out.println("Loggin successfull");
                return true;
            } else {
                // Just return true as we don't need to validate
                return true;
            }
        } catch (ClientProtocolException e) {
            System.out.println("Authentication failed!");
            return false;
        }
    }

    private void initializeRestClient(String serverURL, String username, String password) throws AxisFault {
        HttpTransportProperties.Authenticator authenticator = new HttpTransportProperties.Authenticator();
        authenticator.setUsername(username);
        authenticator.setPassword(password);
        authenticator.setPreemptiveAuthentication(true);

        RestClient restClient;
        ConfigurationContext configurationContext = null;
        try {
            configurationContext = ConfigurationContextFactory.createDefaultConfigurationContext();
        } catch (Exception e) {
            String msg = "Backend error occurred. Please contact the service admins!";
            throw new AxisFault(msg, e);
        }
        HashMap<String, TransportOutDescription> transportsOut = configurationContext
                .getAxisConfiguration().getTransportsOut();
        for (TransportOutDescription transportOutDescription : transportsOut.values()) {
            transportOutDescription.getSender().init(configurationContext, transportOutDescription);
        }

        restClient = new RestClient(serverURL, username, password);
        //restClient = new ApplicationManagementServiceStub(configurationContext, serverURL + "/services/ApplicationManagementService");
        //ServiceClient client = stub._getServiceClient();
        //Options option = client.getOptions();
        //option.setManageSession(true);
        //option.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, authenticator);
        //option.setTimeOutInMilliSeconds(300000);
        this.restClientService = restClient;
    }

    public void listAvailableCartridges() throws CommandException {
        try {
            String resultString = restClientService.doGet(restClientService.getUrl() + listAvailableCartridgesRestEndpoint,
                    restClientService.getUsername(), restClientService.getPassword());

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            CartridgeList cartridgeList = gson.fromJson(resultString, CartridgeList.class);

            if (cartridgeList == null) {
                System.out.println("Available cartridge list is null");
            }

            CartridgeList multiTelentCartridgeList = new CartridgeList();
            CartridgeList singleTeneCartridgetList = new CartridgeList();

            ArrayList<Cartridge> multiTenetCartridge = new ArrayList<Cartridge>();
            ArrayList<Cartridge> singleTentCartridge = new ArrayList<Cartridge>();

            for (Cartridge cartridge : cartridgeList.getCartridge()) {
                if (cartridge.isMultiTenant()) {
                    multiTenetCartridge.add(cartridge);
                }
                else {
                    singleTentCartridge.add(cartridge);
                }
            }

            multiTelentCartridgeList.setCartridge(multiTenetCartridge);
            singleTeneCartridgetList.setCartridge(singleTentCartridge);

            RowMapper<Cartridge> cartridgeMapper = new RowMapper<Cartridge>() {

                public String[] getData(Cartridge cartridge) {
                    String[] data = new String[3];
                    data[0] = cartridge.getCartridgeType();
                    data[1] = cartridge.getDisplayName();
                    data[2] = cartridge.getVersion();
                    return data;
                }
            };

            if (multiTenetCartridge.size() == 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No multi-tenant cartridges available");
                }
                System.out.println("There are no multi-tenant cartridges available");
            }
            else {
                Cartridge[] cartridges = new Cartridge[multiTelentCartridgeList.getCartridge().size()];
                cartridges = multiTelentCartridgeList.getCartridge().toArray(cartridges);

                System.out.println("Available Multi-Tenant Cartridges:");
                CommandLineUtils.printTable(cartridges, cartridgeMapper, "Type", "Name", "Version");
                System.out.println();
            }

            if (singleTentCartridge.size() == 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No single-tenant cartridges available");
                }
                System.out.println("There are no single-tenant cartridges available");
            }
            else {
                Cartridge[] cartridges1 = new Cartridge[singleTeneCartridgetList.getCartridge().size()];
                cartridges1 = singleTeneCartridgetList.getCartridge().toArray(cartridges1   );

                System.out.println("Available Single-Tenant Cartridges:");
                CommandLineUtils.printTable(cartridges1, cartridgeMapper, "Type", "Name", "Version");
                System.out.println();
            }
        } catch (Exception e) {
                e.printStackTrace();
        }
    }

    public void listSubscribedCartridges(final boolean full) throws CommandException {
        try {

            String resultString = restClientService.doGet(restClientService.getUrl() + listSubscribedCartridgesRestEndpoint,
                    restClientService.getUsername(), restClientService.getPassword());

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            CartridgeList cartridgeList = gson.fromJson(resultString, CartridgeList.class);

            if (cartridgeList == null) {
                System.out.println("Subscribe cartridge list is null");
            }

            Cartridge[] cartridges = new Cartridge[cartridgeList.getCartridge().size()];
            cartridges = cartridgeList.getCartridge().toArray(cartridges);

            if (cartridges.length == 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No subscribed cartridges found");
                }
                System.out.println("There are no subscribed cartridges");
                return;
            }

            RowMapper<Cartridge> cartridgeMapper = new RowMapper<Cartridge>() {

                public String[] getData(Cartridge cartridge) {
                    String[] data = full ? new String[9] : new String[7];
                    data[0] = cartridge.getCartridgeType();
                    data[1] = cartridge.getDisplayName();
                    data[2] = cartridge.getVersion();
                    data[3] = cartridge.isMultiTenant() ? "Multi-Tenant" : "Single-Tenant";
                    data[4] = cartridge.getCartridgeAlias();
                    data[5] = cartridge.getStatus();
                    data[6] = cartridge.isMultiTenant() ? "N/A" : String.valueOf(cartridge.getActiveInstances());
                    if (full) {
                        data[7] = getAccessURLs(cartridge);
                        data[8] = cartridge.getRepoURL() != null ? cartridge.getRepoURL() : "";
                    }
                    return data;
                }
            };

            List<String> headers = new ArrayList<String>();
            headers.add("Type");
            headers.add("Name");
            headers.add("Version");
            headers.add("Tenancy Model");
            headers.add("Alias");
            headers.add("Status");
            headers.add("Running Instances");
            if (full) {
                headers.add("Access URL(s)");
                headers.add("Repo URL");
            }

            System.out.println("Subscribed Cartridges:");
            CommandLineUtils.printTable(cartridges, cartridgeMapper, headers.toArray(new String[headers.size()]));
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String cartridgeType, String alias, String policy, String externalRepoURL,
                          boolean privateRepo, String username, String password, String dataCartridgeType, String dataCartridgeAlias)
            throws CommandException {

        CartridgeInfoBean cartridgeInfoBean = new CartridgeInfoBean();
        cartridgeInfoBean.setCartridgeType(null);
        cartridgeInfoBean.setAlias(null);
        cartridgeInfoBean.setPolicy(null);
        cartridgeInfoBean.setRepoURL(null);
        cartridgeInfoBean.setPrivateRepo(false);
        cartridgeInfoBean.setRepoUsername(null);
        cartridgeInfoBean.setRepoPassword(null);
        cartridgeInfoBean.setDataCartridgeType(dataCartridgeType);
        cartridgeInfoBean.setDataCartridgeAlias(dataCartridgeAlias);

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        String jsonSubscribeString = gson.toJson(cartridgeInfoBean, CartridgeInfoBean.class);
        String completeJsonSubscribeString = "{\"cartridgeInfoBean\":" + jsonSubscribeString + "}";

        SubscriptionInfo subcriptionConnectInfo = null;
        if (StringUtils.isNotBlank(dataCartridgeType) && StringUtils.isNotBlank(dataCartridgeAlias)) {
            System.out.format("Subscribing to data cartridge %s with alias %s.%n", dataCartridgeType,
                    dataCartridgeAlias);
            try {
                System.out.println("First try");
                String subscription = restClientService.doPost(restClientService.getUrl() + subscribCartridgeRestEndpoint,
                        completeJsonSubscribeString, restClientService.getUsername(), restClientService.getPassword());

                if (subscription.equals("" + CliConstants.RESPONSE_NO_CONTENT)) {
                    System.out.println("Duplicate alias. Please choose different alias");
                    return;
                }

                String subscriptionJSON =  subscription.substring(20, subscription.length() -1);
                subcriptionConnectInfo = gson.fromJson(subscriptionJSON, SubscriptionInfo.class);

                System.out.format("You have successfully subscribed to %s cartridge with alias %s.%n",
                        dataCartridgeType, dataCartridgeAlias);
                System.out.format("%nSubscribing to %s cartridge and connecting with %s data cartridge.%n", alias,
                        dataCartridgeAlias);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            cartridgeInfoBean.setCartridgeType(cartridgeType);
            cartridgeInfoBean.setAlias(alias);
            cartridgeInfoBean.setPolicy(policy);
            cartridgeInfoBean.setRepoURL(externalRepoURL);
            cartridgeInfoBean.setPrivateRepo(privateRepo);
            cartridgeInfoBean.setRepoUsername(username);
            cartridgeInfoBean.setRepoPassword(password);
            cartridgeInfoBean.setDataCartridgeType(dataCartridgeType);
            cartridgeInfoBean.setDataCartridgeAlias(dataCartridgeAlias);

            System.out.println("Second try");

            jsonSubscribeString = gson.toJson(cartridgeInfoBean, CartridgeInfoBean.class);
            completeJsonSubscribeString = "{\"cartridgeInfoBean\":" + jsonSubscribeString + "}";

            String subscriptionOutput = restClientService.doPost(restClientService.getUrl() + subscribCartridgeRestEndpoint,
                    completeJsonSubscribeString, restClientService.getUsername(), restClientService.getPassword());

            if (subscriptionOutput.equals("" + CliConstants.RESPONSE_NO_CONTENT)) {
                System.out.println("Duplicate alias. Please choose different alias");
                return;
            }

            String  subscriptionOutputJSON=  subscriptionOutput.substring(20, subscriptionOutput.length() -1);
            SubscriptionInfo subcriptionInfo = gson.fromJson(subscriptionOutputJSON, SubscriptionInfo.class);

            System.out.format("You have successfully subscribed to %s cartridge with alias %s.%n", cartridgeType, alias);

            String repoURL = null;
            String hostnames = null;
            String hostnamesLabel = null;
            if (subcriptionInfo != null) {
                repoURL = subcriptionInfo.getRepositoryURL();
                hostnames = subcriptionInfo.getHostname();
                hostnamesLabel = "host name";

                if (repoURL != null) {
                    System.out.println("GIT Repository URL: " + repoURL);
                }

                //Cartridge cart = stub.getCartridgeInfo(alias);
                //System.out.format("Your application is being published here. %s%n", getAccessURLs(cart));
            }
            if (subcriptionConnectInfo != null) {
                hostnames += ", " + subcriptionConnectInfo.getHostname();
                hostnamesLabel = "host names";

                //Cartridge cart = stub.getCartridgeInfo(alias);
                //System.out.format("Your data application is being published here. %s%n", getAccessURLs(cart));
            }
            if (externalRepoURL != null) {
                String takeTimeMsg = "(this might take few minutes... depending on repo size)\n";
                System.out.println(takeTimeMsg);
            }

            System.out.format("Please map the %s \"%s\" to ELB IP%n", hostnamesLabel, hostnames);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addTenant(String admin, String firstName, String lastaName, String password, String domain, String email, String active){
        try {
            TenantInfoBean tenantInfo = new TenantInfoBean();
            tenantInfo.setAdmin(admin);
            tenantInfo.setFirstname(firstName);
            tenantInfo.setLastname(lastaName);
            tenantInfo.setAdminPassword(password);
            tenantInfo.setTenantDomain(domain);
            tenantInfo.setEmail(email);
            tenantInfo.setActive(active);

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            String jsonString = gson.toJson(tenantInfo, TenantInfoBean.class);
            String completeJsonString = "{\"tenantInfoBean\":" + jsonString + "}";

            String result = restClientService.doPost(restClientService.getUrl() + addTenantEndPoint,
                    completeJsonString, restClientService.getUsername(), restClientService.getPassword());

            if (Integer.parseInt(result) == CliConstants.RESPONSE_AUTHORIZATION_FAIL) {
                System.out.println("Invalid operation. Authorization failed");
            } else if (Integer.parseInt(result) == CliConstants.RESPONSE_NO_CONTENT) {
                System.out.println("Tenant added successfully");
            } else if (Integer.parseInt(result) == CliConstants.RESPONSE_INTERNAL_SERVER_ERROR) {
                System.out.println("Domain is not available to register. Please check domain name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unsubscribe(String alias) throws CommandException {
        try {
            restClientService.doPost(restClientService.getUrl() + unsubscribeTenantEndPoint, alias,
                    restClientService.getUsername(), restClientService.getPassword());
            System.out.println("You have successfully unsubscribed " + alias);
        } catch ( Exception e) {
            e.printStackTrace();
        }
    }

    public void deployCartridgeDefinition (String cartridgeDefinition) {
        try {
            String result = restClientService.doPost(restClientService.getUrl() + cartridgeDeploymentEndPoint,
                    cartridgeDefinition, restClientService.getUsername(), restClientService.getPassword());

            if (Integer.parseInt(result) == CliConstants.RESPONSE_AUTHORIZATION_FAIL) {
                System.out.println("Invalid operations. Authorization failed");
            }
            else {
                System.out.println("You have successfully deployed the cartridge");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deployPartition (String partitionDefinition) {
        try {
            String result = restClientService.doPost(restClientService.getUrl() + partitionDeploymentEndPoint,
                    partitionDefinition, restClientService.getUsername(), restClientService.getPassword());

            System.out.println(result);

            if (Integer.parseInt(result) == CliConstants.RESPONSE_AUTHORIZATION_FAIL) {
                System.out.println("Invalid operations. Authorization failed");
            }
            else {
                System.out.println("You have successfully deployed the cartridge");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deployAutoscalingPolicy (String autoScalingPolicy) {
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class CartridgeList  {
        private ArrayList<Cartridge> cartridge;

        public ArrayList<Cartridge> getCartridge() {
            return cartridge;
        }

        public void setCartridge(ArrayList<Cartridge> cartridge) {
            this.cartridge = cartridge;
        }

        CartridgeList() {
            cartridge = new ArrayList<Cartridge>();
        }
    }

    private String getAccessURLs(Cartridge cartridge) {
        String[] accessURLs = cartridge.getAccessURLs();
        StringBuilder urlBuilder = new StringBuilder();
        if (accessURLs != null) {
            for (int i = 0; i < accessURLs.length; i++) {
                String url = accessURLs[i];
                if (url != null) {
                    if (i > 0) {
                        urlBuilder.append(", ");
                    }
                    urlBuilder.append(url);
                }
            }
        }
        return urlBuilder.toString();
    }
}

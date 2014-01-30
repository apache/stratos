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
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.stratos.cli.beans.*;
import org.apache.stratos.cli.beans.autoscaler.partition.Partition;
import org.apache.stratos.cli.beans.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.cli.beans.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.cli.beans.cartridge.Cartridge;
import org.apache.stratos.cli.beans.cartridge.CartridgeInfoBean;
import org.apache.stratos.cli.beans.topology.Cluster;
import org.apache.stratos.cli.beans.topology.Member;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;
import org.apache.stratos.cli.utils.CommandLineUtils;
import org.apache.stratos.cli.utils.RowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RestCommandLineService {

	private static final Logger logger = LoggerFactory.getLogger(RestCommandLineService.class);

    private RestClient restClientService;

    // REST endpoints
    private final String initializeEndpoint = "/stratos/admin/init";
    private final String listAvailableCartridgesRestEndpoint = "/stratos/admin/cartridge/list";
    private final String describeAvailableCartridgeRestEndpoint = "/stratos/admin/cartridge/list/";
    private final String listSubscribedCartridgesRestEndpoint = "/stratos/admin/cartridge/list/subscribed";
    private final String listClusterRestEndpoint = "/stratos/admin/cluster/";
    private final String subscribCartridgeRestEndpoint = "/stratos/admin/cartridge/subscribe";
    private final String addTenantEndPoint = "/stratos/admin/tenant";
    private final String unsubscribeTenantEndPoint = "/stratos/admin/cartridge/unsubscribe";
    private final String cartridgeDeploymentEndPoint = "/stratos/admin/cartridge/definition";
    private final String partitionDeploymentEndPoint = "/stratos/admin/policy/deployment/partition";
    private final String autoscalingPolicyDeploymentEndPoint = "/stratos/admin/policy/autoscale";
    private final String deploymentPolicyDeploymentEndPoint = "/stratos/admin/policy/deployment";
    private final String describeParitionRestEndPoint = "/stratos/admin/partition/";
    private final String listParitionRestEndPoint = "/stratos/admin/partition";
    private final String describeAutoscalePolicyRestEndPoint = "/stratos/admin/policy/autoscale/";
    private final String listAutoscalePolicyRestEndPoint = "/stratos/admin/policy/autoscale";
    private final String describeDeploymentPolicyRestEndPoint = "/stratos/admin/policy/deployment/";
    private final String listDeploymentPolicyRestEndPoint = "/stratos/admin/policy/deployment";

    private static class SingletonHolder {
		private final static RestCommandLineService INSTANCE = new RestCommandLineService();
	}

	public static RestCommandLineService getInstance() {
		return SingletonHolder.INSTANCE;
	}

    // Loing method. This will authenticate the user
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

        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {

            if (validateLogin) {
                restClientService.doPost(httpClient, restClientService.getUrl() + initializeEndpoint, "",
                        restClientService.getUsername(), restClientService.getPassword());
                if (logger.isDebugEnabled()) {
                    logger.debug("Tenant Domain {}", restClientService.getUsername());
                }
                return true;
            } else {
                // Just return true as we don't need to validate
                return true;
            }
        } catch (ClientProtocolException e) {
            System.out.println("Authentication failed!");
            return false;
        } catch (ConnectException e) {
            System.out.println("Could not connect to stratos manager");
            return false;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // Initialize the rest client and set username and password of the user
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

    // List currently available multi tenant and single tenant cartridges
    public void listAvailableCartridges() throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl() + listAvailableCartridgesRestEndpoint,
                    restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();
            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while listing available cartridges");
                return;
            }

            String resultString = getHttpResponseString(response);
            if (resultString == null) {
                return;
            }

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            CartridgeList cartridgeList = gson.fromJson(resultString, CartridgeList.class);

            if (cartridgeList == null) {
                System.out.println("Available cartridge list is null");
                return;
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
            handleException("Exception in listing available cartridges", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // List currently available multi tenant and single tenant cartridges
    public void describeAvailableCartridges(String type) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl() + listAvailableCartridgesRestEndpoint,
                    restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();
            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while listing available cartridges");
                return;
            }

            String resultString = getHttpResponseString(response);
            if (resultString == null) {
                return;
            }

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            CartridgeList cartridgeList = gson.fromJson(resultString, CartridgeList.class);

            if (cartridgeList == null) {
                System.out.println("Available cartridge list is null");
                return;
            }

            for (Cartridge tmp : cartridgeList.getCartridge()) {
                if(tmp.getCartridgeType().equalsIgnoreCase(type)) {
                    System.out.println("The cartridge is:");
                    System.out.println(gson.toJson(tmp));
                    return;
                }
            }
            System.out.println("No matching cartridge found...");
        } catch (Exception e) {
            handleException("Exception in listing available cartridges", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // List subscribe cartridges
    public void listSubscribedCartridges(final boolean full) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl() + listSubscribedCartridgesRestEndpoint,
                    restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();
            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while listing subscribe cartridges");
                return;
            }

            String resultString = getHttpResponseString(response);

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            CartridgeList cartridgeList = gson.fromJson(resultString, CartridgeList.class);

            if (cartridgeList == null) {
                System.out.println("Subscribe cartridge list is null");
                return;
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
            handleException("Exception in listing subscribe cartridges", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    public void listMembersOfCluster(String cartridgeType, String alias) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl() + listClusterRestEndpoint
                    + cartridgeType + "/" + alias,
                    restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();
            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while listing members of a cluster");
                return;
            }

            String resultString = getHttpResponseString(response);
            String tmp;
            if(resultString.startsWith("{\"cluster\"")) {
               tmp = resultString.substring("{\"cluster\"".length() + 1, resultString.length()-1);
               resultString = tmp;
            }
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            Cluster cluster = gson.fromJson(resultString, Cluster.class);

            if (cluster == null) {
                System.out.println("Subscribe cartridge list is null");
                return;
            }

            Member[] members = new Member[cluster.getMember().size()];
            members = cluster.getMember().toArray(members);
            System.out.println("Subscribe cartridge list is :" +  cluster.getMember().size());


            if (members.length == 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No subscribed cartridges found");
                }
                System.out.println("There are no subscribed cartridges");
                return;
            }

            RowMapper<Member> memberMapper = new RowMapper<Member>() {

                public String[] getData(Member member) {
                    String[] data = new String[7];
                    data[0] = member.getServiceName();
                    data[1] = member.getClusterId();
                    data[2] = member.getNetworkPartitionId();
                    data[3] = member.getPartitionId();
                    data[4] = member.getMemberIp();
                    data[5] = member.getStatus().toString();
                    data[6] = member.getLbClusterId() != null ? member.getLbClusterId() : "";
                    return data;
                }
            };

            List<String> headers = new ArrayList<String>();
            headers.add("ServiceName");
            headers.add("ClusterId");
            headers.add("NewtworkPartitionId");
            headers.add("PartitionId");
            headers.add("MemberIp");
            headers.add("Status");
            headers.add("LBCluster");

            System.out.println("List of members in the [cluster]: " + alias);
            CommandLineUtils.printTable(members, memberMapper, headers.toArray(new String[headers.size()]));

            System.out.println("List of LB members for the [cluster]: " + "TODO" );
        } catch (Exception e) {
            System.out.println("error while getting Cluster.....");
            System.out.println(e.fillInStackTrace());
            System.out.println("error while getting Cluster.....");

            e.printStackTrace();
            System.out.println();

            handleException("Exception in listing subscribe cartridges", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method does the cartridge subscription
    public void subscribe(String cartridgeType, String alias, String externalRepoURL, boolean privateRepo, String username,
                          String password, String dataCartridgeType, String dataCartridgeAlias, String asPolicy, String depPolicy)
            throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();

        CartridgeInfoBean cartridgeInfoBean = new CartridgeInfoBean();
        cartridgeInfoBean.setCartridgeType(null);
        cartridgeInfoBean.setAlias(null);
        cartridgeInfoBean.setRepoURL(null);
        cartridgeInfoBean.setPrivateRepo(false);
        cartridgeInfoBean.setRepoUsername(null);
        cartridgeInfoBean.setRepoPassword(null);
        cartridgeInfoBean.setAutoscalePolicy(null);
        cartridgeInfoBean.setDeploymentPolicy(null);
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
                HttpResponse response = restClientService.doPost(httpClient, restClientService.getUrl() + subscribCartridgeRestEndpoint,
                        completeJsonSubscribeString, restClientService.getUsername(), restClientService.getPassword());

                String responseCode = "" + response.getStatusLine().getStatusCode();
                if (responseCode.equals(CliConstants.RESPONSE_AUTHORIZATION_FAIL)) {
                    System.out.println("Invalid operation. Authorization failed");
                    return;
                } else if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                    System.out.println("Error occured while subscribing cartridge");
                    return;
                }

                String subscription = getHttpResponseString(response);

                if (subscription == null) {
                    System.out.println("Error in response");
                    return;
                }

                String subscriptionJSON =  subscription.substring(20, subscription.length() -1);
                subcriptionConnectInfo = gson.fromJson(subscriptionJSON, SubscriptionInfo.class);

                System.out.format("You have successfully subscribed to %s cartridge with alias %s.%n",
                        dataCartridgeType, dataCartridgeAlias);
                System.out.format("%nSubscribing to %s cartridge and connecting with %s data cartridge.%n", alias,
                        dataCartridgeAlias);
            } catch (Exception e) {
                handleException("Exception in subscribing to data cartridge", e);
            }
            finally {
                httpClient.getConnectionManager().shutdown();
            }
        }

        if (httpClient == null) {
            httpClient = new DefaultHttpClient();
        }

        try {
            cartridgeInfoBean.setCartridgeType(cartridgeType);
            cartridgeInfoBean.setAlias(alias);
            cartridgeInfoBean.setRepoURL(externalRepoURL);
            cartridgeInfoBean.setPrivateRepo(privateRepo);
            cartridgeInfoBean.setRepoUsername(username);
            cartridgeInfoBean.setRepoPassword(password);
            cartridgeInfoBean.setDataCartridgeType(dataCartridgeType);
            cartridgeInfoBean.setDataCartridgeAlias(dataCartridgeAlias);
            cartridgeInfoBean.setAutoscalePolicy(asPolicy);
            cartridgeInfoBean.setDeploymentPolicy(depPolicy);

            jsonSubscribeString = gson.toJson(cartridgeInfoBean, CartridgeInfoBean.class);
            completeJsonSubscribeString = "{\"cartridgeInfoBean\":" + jsonSubscribeString + "}";

            HttpResponse response = restClientService.doPost(httpClient, restClientService.getUrl() + subscribCartridgeRestEndpoint,
                    completeJsonSubscribeString, restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();
            if (responseCode.equals(CliConstants.RESPONSE_AUTHORIZATION_FAIL)) {
                System.out.println("Invalid operation. Authorization failed");
                return;
            } else if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while subscribing cartridge");
                return;
            }

            String subscriptionOutput = getHttpResponseString(response);

            if (subscriptionOutput == null) {
                System.out.println("Error in response");
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

            System.out.format("Please map the %s \"%s\" to LB IP%n", hostnamesLabel, hostnames);
        } catch (Exception e) {
            handleException("Exception in subscribing to cartridge", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to create the new tenant
    public void addTenant(String admin, String firstName, String lastaName, String password, String domain, String email)
            throws CommandException{
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            TenantInfoBean tenantInfo = new TenantInfoBean();
            tenantInfo.setAdmin(admin);
            tenantInfo.setFirstname(firstName);
            tenantInfo.setLastname(lastaName);
            tenantInfo.setAdminPassword(password);
            tenantInfo.setTenantDomain(domain);
            tenantInfo.setEmail(email);

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            String jsonString = gson.toJson(tenantInfo, TenantInfoBean.class);
            String completeJsonString = "{\"tenantInfoBean\":" + jsonString + "}";

            HttpResponse response = restClientService.doPost(httpClient, restClientService.getUrl() + addTenantEndPoint,
                    completeJsonString, restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();

            if (responseCode.equals(CliConstants.RESPONSE_AUTHORIZATION_FAIL)) {
                System.out.println("Invalid operation. Authorization failed");
                return;
            } else if (responseCode.equals(CliConstants.RESPONSE_OK)){
                System.out.println("Tenant added successfully");
                return;
            } else if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while creating tenant");
                return;
            } else {
                System.out.println ("Unhandle error");
                return;
            }

        } catch (Exception e) {
            handleException("Exception in creating tenant", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to unsubscribe cartridges
    public void unsubscribe(String alias) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            restClientService.doPost(httpClient, restClientService.getUrl() + unsubscribeTenantEndPoint, alias,
                    restClientService.getUsername(), restClientService.getPassword());
            System.out.println("You have successfully unsubscribed " + alias);
        } catch ( Exception e) {
            handleException("Exception in un-subscribing cartridge", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to deploy cartridge definitions
    public void deployCartridgeDefinition (String cartridgeDefinition) throws CommandException{
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doPost(httpClient, restClientService.getUrl() + cartridgeDeploymentEndPoint,
                    cartridgeDefinition, restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();

            if (responseCode.equals(CliConstants.RESPONSE_AUTHORIZATION_FAIL)) {
                System.out.println("Invalid operations. Authorization failed");
                return;
            } else if (responseCode.equals(CliConstants.RESPONSE_NO_CONTENT)) {
                System.out.println("You have successfully deployed the cartridge");
                return;
            } else if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while deploying cartridge definition");
                return;
            }
        } catch (Exception e) {
            handleException("Exception in deploy cartridge definition", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to deploy partitions
    public void deployPartition (String partitionDefinition) throws CommandException{
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doPost(httpClient, restClientService.getUrl() + partitionDeploymentEndPoint,
                    partitionDefinition, restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();

            if (responseCode.equals(CliConstants.RESPONSE_AUTHORIZATION_FAIL)) {
                System.out.println("Invalid operations. Authorization failed");
                return;
            } else if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while deploying partition");
                return;
            }

            String result = getHttpResponseString(response);

            if (result.equals("true")) {
                System.out.println("You have successfully deployed the partition");
                return;
            }

        } catch (Exception e) {
            handleException("Exception in deploying partitions", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to deploy autoscalling polices
    public void deployAutoscalingPolicy (String autoScalingPolicy) throws CommandException{
        DefaultHttpClient httpClient= new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doPost(httpClient, restClientService.getUrl() + autoscalingPolicyDeploymentEndPoint,
                    autoScalingPolicy, restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();
            if (responseCode.equals("" + CliConstants.RESPONSE_AUTHORIZATION_FAIL)) {
                System.out.println("Invalid operations. Authorization failed");
                return;
            } else if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while deploying autoscaling policy");
                return;
            }

            String result = getHttpResponseString(response);

            if (result.equals("true")) {
                System.out.println("You have successfully deployed the autoscaling policy");
                return;
            }

        } catch (Exception e) {
            handleException("Exception in deploying autoscale police", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to deploy deployment polices
    public void deployDeploymentPolicy (String deploymentPolicy) throws CommandException{
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doPost(httpClient, restClientService.getUrl() + deploymentPolicyDeploymentEndPoint,
                    deploymentPolicy, restClientService.getUsername(), restClientService.getPassword());
            System.out.println(deploymentPolicy);
            String responseCode = "" + response.getStatusLine().getStatusCode();
            if (responseCode.equals("" + CliConstants.RESPONSE_AUTHORIZATION_FAIL)) {
                System.out.println("Invalid operations. Authorization failed");
                return;
            } else if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while deploying deployment policy");
                return;
            }

            String result = getHttpResponseString(response);

            if (result.equals("true")) {
                System.out.println("You have successfully deployed the deployment policy");
                return;
            }

        } catch (Exception e) {
            handleException("Exception in deploying deployment policy", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method list available partitons
    public void listPartitions() throws CommandException{
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl() + listParitionRestEndPoint,
                    restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();
            if (responseCode.equals("" + CliConstants.RESPONSE_AUTHORIZATION_FAIL)) {
                System.out.println("Invalid operations. Authorization failed");
                return;
            } else if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while listing partitions");
                return;
            }

            String resultString = getHttpResponseString(response);

            if (resultString == null) {
                System.out.println("Response content is empty");
                return;
            }

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            PartitionList partitionList = gson.fromJson(resultString, PartitionList.class);

            if (partitionList == null) {
                System.out.println("Partition list is empty");
                return;
            }

            RowMapper<Partition> partitionMapper = new RowMapper<Partition>() {

                public String[] getData(Partition partition) {
                    String[] data = new String[2];
                    data[0] = partition.getId();
                    data[1] = partition.getProvider();
                    return data;
                }
            };

            Partition[] partitions = new Partition[partitionList.getPartition().size()];
            partitions = partitionList.getPartition().toArray(partitions);

            System.out.println("Available Partitions:" );
            CommandLineUtils.printTable(partitions, partitionMapper, "ID", "Provider");
            System.out.println();

        } catch (Exception e) {
            handleException("Exception in listing partitions", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method list autoscale policies
    public void listAutoscalePolicies() throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl() + listAutoscalePolicyRestEndPoint,
                    restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();
            if (responseCode.equals("" + CliConstants.RESPONSE_AUTHORIZATION_FAIL)) {
                System.out.println("Invalid operations. Authorization failed");
                return;
            } else if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while listing autoscase policies");
                return;
            }

            String resultString = getHttpResponseString(response);

            if (resultString == null) {
                System.out.println("Response content is empty");
                return;
            }
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            AutoscalePolicyList policyList = gson.fromJson(resultString, AutoscalePolicyList.class);

            if (policyList == null) {
                System.out.println("Autoscale policy list is empty");
                return;
            }

            RowMapper<AutoscalePolicy> partitionMapper = new RowMapper<AutoscalePolicy>() {

                public String[] getData(AutoscalePolicy policy) {
                    String[] data = new String[1];
                    data[0] = policy.getId();
                    return data;
                }
            };

            AutoscalePolicy[] policyArry = new AutoscalePolicy[policyList.getAutoscalePolicy().size()];
            policyArry = policyList.getAutoscalePolicy().toArray(policyArry);

            System.out.println("Available Autoscale Policies:");
            CommandLineUtils.printTable(policyArry, partitionMapper, "ID");

        } catch (Exception e) {
            handleException("Exception in listing autoscale policies", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method list deployment policies
    public void listDeploymentPolicies() throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl() + listDeploymentPolicyRestEndPoint,
                    restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();
            if (responseCode.equals("" + CliConstants.RESPONSE_AUTHORIZATION_FAIL)) {
                System.out.println("Invalid operations. Authorization failed");
                return;
            } else if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while listing deployment policies");
                return;
            }

            String resultString = getHttpResponseString(response);
            if (resultString == null) {
                System.out.println("Response content is empty");
                return;
            }

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            DeploymentPolicyList policyList = gson.fromJson(resultString, DeploymentPolicyList.class);

            if (policyList == null) {
                System.out.println("Deployment policy list is empty");
                return;
            }
            RowMapper<DeploymentPolicy> partitionMapper = new RowMapper<DeploymentPolicy>() {

                public String[] getData(DeploymentPolicy policy) {
                    String[] data = new String[1];
                    data[0] = policy.getId();
                    return data;
                }
            };

            DeploymentPolicy[] policyArry = new DeploymentPolicy[policyList.getDeploymentPolicy().size()];
            policyArry = policyList.getDeploymentPolicy().toArray(policyArry);

            System.out.println("Available Deployment Policies:");
            CommandLineUtils.printTable(policyArry, partitionMapper, "ID");
            System.out.println();

        } catch (Exception e) {
            handleException("Exception in listing deployment polices", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method list deployment policies
    public void describeDeploymentPolicies(String id) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl()
                    + listDeploymentPolicyRestEndPoint,
                    restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();
            if (responseCode.equals("" + CliConstants.RESPONSE_AUTHORIZATION_FAIL)) {
                System.out.println("Invalid operations. Authorization failed");
                return;
            } else if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while listing deployment policies");
                return;
            }

            String resultString = getHttpResponseString(response);
            if (resultString == null) {
                System.out.println("Response content is empty");
                return;
            }

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            DeploymentPolicyList deploymentPolicyList = gson.fromJson(resultString, DeploymentPolicyList.class);

            if (deploymentPolicyList == null) {
                System.out.println("Deployment policy list is empty");
                return;
            }
            for (DeploymentPolicy policy : deploymentPolicyList.getDeploymentPolicy()) {
                if(policy.getId().equals(id)) {
                    System.out.println("The Deployment policy is: \n");
                    System.out.println(gson.toJson(policy));
                    return;
                }
            }

            System.out.println("No matching Deployment policy found");

            /*RowMapper<DeploymentPolicy> partitionMapper = new RowMapper<DeploymentPolicy>() {

                public String[] getData(DeploymentPolicy policy) {
                    String[] data = new String[1];
                    data[0] = policy.getId();
                    return data;
                }
            };

            DeploymentPolicy[] deploymentPolicies = new DeploymentPolicy[1];
            deploymentPolicies[0] = deploymentPolicy;

            System.out.println("The Deployment policy is: \n");
            System.out.println(resultString);*/
            //CommandLineUtils.printTable(deploymentPolicies, partitionMapper, "ID");

        } catch (Exception e) {
            handleException("Exception in listing deployment polices", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }



 // This method list deployment policies
    public void describePartition(String id) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl()
                    + listParitionRestEndPoint,
                    restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();
            if (responseCode.equals("" + CliConstants.RESPONSE_AUTHORIZATION_FAIL)) {
                System.out.println("Invalid operations. Authorization failed");
                return;
            } else if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while listing deployment policies");
                return;
            }
            String resultString = getHttpResponseString(response);
            if (resultString == null) {
                System.out.println("Response content is empty");
                return;
            }


           if (resultString == null) {
               System.out.println("Response content is empty");
               return;
           }

           GsonBuilder gsonBuilder = new GsonBuilder();
           Gson gson = gsonBuilder.create();
           PartitionList partitionList = gson.fromJson(resultString, PartitionList.class);

            for (Partition partition : partitionList.getPartition()) {
                if(partition.getId().equals(id)) {
                    System.out.println("The Partition is:");
                    System.out.println(gson.toJson(partition));
                    return;
                }
            }
            System.out.println("No matching partition found...");


            /*GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            Partition partition = gson.fromJson(resultString, Partition.class);

            if (partition == null) {
                System.out.println("Deployment policy list is empty");
                return;
            }
            RowMapper<Partition> partitionMapper = new RowMapper<Partition>() {

                public String[] getData(Partition policy) {
                    String[] data = new String[1];
                    data[0] = policy.getId();
                    return data;
                }
            };

            System.out.println("The Partition is:");
            System.out.println(resultString);
            System.out.println(resultString);  */
            //CommandLineUtils.printTable(deploymentPolicies, partitionMapper, "ID");

        } catch (Exception e) {
            handleException("Exception in listing deployment polices", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    public void describeAutoScalingPolicy(String id) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl()
                    + listAutoscalePolicyRestEndPoint,
                    restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();
            if (responseCode.equals("" + CliConstants.RESPONSE_AUTHORIZATION_FAIL)) {
                System.out.println("Invalid operations. Authorization failed");
                return;
            } else if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Error occured while listing deployment policies");
                return;
            }

            String resultString = getHttpResponseString(response);
            if (resultString == null) {
                System.out.println("Response content is empty");
                return;
            }

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            AutoscalePolicyList policyList = gson.fromJson(resultString, AutoscalePolicyList.class);

            if (policyList == null) {
                System.out.println("Deployment policy list is empty");
                return;
            }
            for(AutoscalePolicy policy : policyList.getAutoscalePolicy()) {
               if(policy.getId().equalsIgnoreCase(id)) {
                   System.out.println("Autoscaling policy is:");
                   System.out.println(gson.toJson(policy));
                   return;
               }
            }
            System.out.println("No matching Autoscale Policy found...");


            /*RowMapper<AutoscalePolicy> partitionMapper = new RowMapper<AutoscalePolicy>() {

                public String[] getData(AutoscalePolicy policy) {
                    String[] data = new String[1];
                    data[0] = policy.getId();
                    return data;
                }
            };

            System.out.println("The Autoscaling Policy is: \n");
            System.out.println(resultString);*/
            //CommandLineUtils.printTable(deploymentPolicies, partitionMapper, "ID");

        } catch (Exception e) {
            handleException("Exception in listing deployment polices", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }



    // This class convert JSON string to deploymentpolicylist object
    private class DeploymentPolicyList {
        private ArrayList<DeploymentPolicy> deploymentPolicy;


        public ArrayList<DeploymentPolicy> getDeploymentPolicy() {
            return deploymentPolicy;
        }

        public void setDeploymentPolicy(ArrayList<DeploymentPolicy> deploymentPolicy) {
            this.deploymentPolicy = deploymentPolicy;
        }

        DeploymentPolicyList() {
            deploymentPolicy = new ArrayList<DeploymentPolicy>();
        }
    }

    // This class convert JSON string to autoscalepolicylist object
    private class AutoscalePolicyList {
        private ArrayList<AutoscalePolicy> autoscalePolicy;

        public ArrayList<AutoscalePolicy> getAutoscalePolicy() {
            return autoscalePolicy;
        }

        public void setAutoscalePolicy(ArrayList<AutoscalePolicy> autoscalePolicy) {
            this.autoscalePolicy = autoscalePolicy;
        }

        AutoscalePolicyList() {
            autoscalePolicy = new ArrayList<AutoscalePolicy>();
        }
    }

    // This class convert JSON string to PartitionLIst object
    private class PartitionList {
        private ArrayList<Partition> partition;

        public ArrayList<Partition> getPartition() {
            return partition;
        }

        public void setPartition(ArrayList<Partition> partition) {
            this.partition = partition;
        }

        PartitionList() {
            partition = new ArrayList<Partition>();
        }
    }

    // This class is for convert JSON string to CartridgeList object
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

    // This will return access url from a given cartridge
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

    // This method gives the HTTP response string
    private String getHttpResponseString (HttpResponse response) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

            String output;
            String result = "";
            while ((output = reader.readLine()) != null) {
                result += output;
            }
            return result;
        } catch (SocketException e) {
            System.out.println("Connection problem");
            return null;
        } catch (NullPointerException e) {
            System.out.println("Null value return from server");
            return null;
        } catch (IOException e) {
            System.out.println("IO error");
            return null;
        }
    }

    // This is for handle exception
    private void handleException(String key, Exception e, Object... args) throws CommandException {
        if (logger.isDebugEnabled()) {
            logger.debug("Displaying message for {}. Exception thrown is {}", key, e.getClass());
        }
        String message = CommandLineUtils.getMessage(key, args);
        if (logger.isErrorEnabled()) {
            logger.error(message);
        }
        System.out.println(message);
        throw new CommandException(message, e);
    }
}

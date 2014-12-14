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
import com.google.gson.reflect.TypeToken;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.exception.ExceptionMapper;
import org.apache.stratos.cli.utils.CliConstants;
import org.apache.stratos.cli.utils.CliUtils;
import org.apache.stratos.cli.utils.RowMapper;
import org.apache.stratos.common.beans.ApplicationBean;
import org.apache.stratos.common.beans.GroupBean;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.beans.UserInfoBean;
import org.apache.stratos.common.beans.autoscaler.partition.Partition;
import org.apache.stratos.common.beans.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.common.beans.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.common.beans.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.common.beans.cartridge.definition.IaasProviderBean;
import org.apache.stratos.common.beans.cartridge.definition.PortMappingBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesGroup;
import org.apache.stratos.common.beans.kubernetes.KubernetesHost;
import org.apache.stratos.common.beans.topology.Cluster;
import org.apache.stratos.common.beans.topology.Member;
import org.apache.stratos.manager.dto.Cartridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RestCommandLineService {

    private static final Logger log = LoggerFactory.getLogger(RestCommandLineService.class);

    private RestClient restClient;

    // REST endpoints
    private static final String API_CONTEXT = "/api/v4.1";
    private static final String ENDPOINT_INIT = API_CONTEXT + "/init";

    private static final String ENDPOINT_ADD_TENANT = API_CONTEXT + "/tenants";
    private static final String ENDPOINT_ADD_USER = API_CONTEXT + "/users";

    private static final String ENDPOINT_DEPLOY_CARTRIDGE = API_CONTEXT + "/cartridges";
    private static final String ENDPOINT_DEPLOY_PARTITION = API_CONTEXT + "/partitions";
    private static final String ENDPOINT_DEPLOY_AUTOSCALING_POLICY = API_CONTEXT + "/autoscalingPolicies";
    private static final String ENDPOINT_DEPLOY_DEPLOYMENT_POLICY = API_CONTEXT + "/deploymentPolicies";
    private static final String ENDPOINT_DEPLOY_KUBERNETES_CLUSTER = API_CONTEXT + "/kubernetesCluster";
    private static final String ENDPOINT_DEPLOY_KUBERNETES_HOST = API_CONTEXT + "/kubernetesCluster/{kubernetesClusterId}/minion";
    private static final String ENDPOINT_DEPLOY_SERVICE_GROUP = API_CONTEXT + "/groups";
    private static final String ENDPOINT_DEPLOY_APPLICATION = API_CONTEXT + "/applications";

    private static final String ENDPOINT_UNDEPLOY_KUBERNETES_CLUSTER= API_CONTEXT + "/kubernetesCluster/{id}";
    private static final String ENDPOINT_UNDEPLOY_KUBERNETES_HOST = API_CONTEXT + "/kubernetesCluster/{kubernetesClusterId}/hosts/{id}";
    private static final String ENDPOINT_UNDEPLOY_SERVICE_GROUP = API_CONTEXT + "/groups/{id}";
    private static final String ENDPOINT_UNDEPLOY_APPLICATION = API_CONTEXT + "/applications/{id}";
    private static final String ENDPOINT_UNDEPLOY_CARTRIDGE = API_CONTEXT + "/cartridges/{id}";

    private static final String ENDPOINT_LIST_PARTITIONS = API_CONTEXT + "/partitions";
    private static final String ENDPOINT_LIST_AUTOSCALING_POLICIES = API_CONTEXT + "/autoscalingPolicies";
    private static final String ENDPOINT_LIST_DEPLOYMENT_POLICIES = API_CONTEXT + "/deploymentPolicies";
    private static final String ENDPOINT_LIST_CARTRIDGES = API_CONTEXT + "/cartridges";
    private static final String ENDPOINT_LIST_TENANTS = API_CONTEXT + "/tenants";
    private static final String ENDPOINT_LIST_USERS = API_CONTEXT + "/users";
    private static final String ENDPOINT_LIST_KUBERNETES_CLUSTERS = API_CONTEXT + "/kubernetesCluster";
    private static final String ENDPOINT_LIST_KUBERNETES_HOSTS = API_CONTEXT + "/kubernetesCluster/{kubernetesClusterId}/hosts";
    private static final String ENDPOINT_LIST_SERVICE_GROUP = API_CONTEXT + "/groups/{groupDefinitionName}";
    private static final String ENDPOINT_LIST_APPLICATION = API_CONTEXT + "/applications";

    private static final String ENDPOINT_GET_APPLICATION = API_CONTEXT + "/applications/{appId}";
    private static final String ENDPOINT_GET_AUTOSCALING_POLICY = API_CONTEXT + "/autoscalingPolicies/{id}";
    private static final String ENDPOINT_GET_DEPLOYMENT_POLICY = API_CONTEXT + "/deploymentPolicies/{id}";
    private static final String ENDPOINT_GET_CARTRIDGE = API_CONTEXT + "/cartridges/{cartridgeType}";
    private static final String ENDPOINT_GET_CARTRIDGE_OF_TENANT = API_CONTEXT + "/subscriptions/{id}/cartridges";
    private static final String ENDPOINT_GET_CLUSTER_OF_TENANT = API_CONTEXT + "/clusters/";
    private static final String ENDPOINT_GET_KUBERNETES_GROUP = API_CONTEXT + "/kubernetesCluster/{kubernetesClusterId}";
    private static final String ENDPOINT_GET_KUBERNETES_MASTER = API_CONTEXT + "/kubernetesCluster/{kubernetesClusterId}/master";
    private static final String ENDPOINT_GET_KUBERNETES_HOST = API_CONTEXT + "/kubernetesCluster/{kubernetesClusterId}/hosts";

    private static final String ENDPOINT_UPDATE_KUBERNETES_MASTER = API_CONTEXT + "/kubernetesCluster/{kubernetesClusterId}/master";
    private static final String ENDPOINT_UPDATE_KUBERNETES_HOST = API_CONTEXT + "/kubernetesCluster/{kubernetesClusterId}/minion/{minionId}";

    private static final String ENDPOINT_SYNCHRONIZE_ARTIFACTS = API_CONTEXT + "/repo/synchronize/{subscriptionAlias}";
    private static final String ENDPOINT_ACTIVATE_TENANT = API_CONTEXT + "/tenants/activate/{tenantDomain}";
    private static final String ENDPOINT_DEACTIVATE_TENANT = API_CONTEXT + "/tenants/deactivate/{tenantDomain}";

    private static final String ENDPOINT_UPDATE_SUBSCRIPTION_PROPERTIES = API_CONTEXT + "/subscriptions/{subscriptionAlias}/properties";
    private static final String ENDPOINT_UPDATE_DEPLOYMENT_POLICY = API_CONTEXT + "/deploymentPolicies";
    private static final String ENDPOINT_UPDATE_AUTOSCALING_POLICY = API_CONTEXT + "/autoscalePolicies";


    private static class SingletonHolder {
        private final static RestCommandLineService INSTANCE = new RestCommandLineService();
    }

    public static RestCommandLineService getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public Gson getGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        return gsonBuilder.create();
    }

    // Login method. This will authenticate the user
    public boolean login(String serverURL, String username, String password, boolean validateLogin) throws Exception {
        try {
            // Avoid validating SSL certificate
            SSLContext sc = SSLContext.getInstance("SSL");
            // Create empty HostnameVerifier
            HostnameVerifier hv = new HostnameVerifier() {
                public boolean verify(String urlHostName, SSLSession session) {
                    return true;
                }
            };
            // Create a trust manager that does not validate certificate
            // chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
            }};
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            SSLContext.setDefault(sc);
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception e) {
            throw new RuntimeException("Error while authentication process!", e);
        }

        // Initialize client
        try {
            initializeRestClient(serverURL, username, password);

            if (log.isDebugEnabled()) {
                log.debug("Initialized REST Client for user {}", username);
            }
        } catch (AxisFault e) {
            System.out.println("Error connecting to the back-end");
            throw new CommandException(e);
        }

        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            if (validateLogin) {
                HttpResponse response = restClient.doGet(httpClient, restClient.getBaseURL() + ENDPOINT_INIT);

                if (response != null) {
                    int responseCode = response.getStatusLine().getStatusCode();
                    if (responseCode == 200) {
                        return true;
                    } else {
                        System.out.println("Invalid STRATOS_URL");
                    }
                }
                return false;
            } else {
                // Just return true as we don't need to validate
                return true;
            }
        } catch (ConnectException e) {
            String message = "Could not connect to stratos manager";
            System.out.println(message);
            log.error(message, e);
            return false;
        } catch (java.lang.NoSuchMethodError e) {
            String message = "Authentication failed!";
            System.out.println(message);
            log.error(message, e);
            return false;
        } catch (Exception e) {
            String message = "An unknown error occurred: " + e.getMessage();
            System.out.println(message);
            log.error(message, e);
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

        this.restClient = new RestClient(serverURL, username, password);
    }

    public CartridgeDefinitionBean getCartridge(String cartridgeType) throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<CartridgeDefinitionBean>>() {
            }.getType();
            List<CartridgeDefinitionBean> cartridgeList = (List<CartridgeDefinitionBean>) restClient.listEntity(ENDPOINT_LIST_CARTRIDGES,
                    listType, "cartridges");

            for (int i = 0; i < cartridgeList.size(); i++) {
                CartridgeDefinitionBean cartridge = cartridgeList.get(i);
                if (cartridgeType.equals(cartridge.getType())) {
                    return cartridge;
                }
            }
        } catch (Exception e) {
            String message = "Error in getting cartridge";
            System.out.println(message);
            log.error(message, e);
        }
        return null;
    }

    public ArrayList<CartridgeDefinitionBean> listCartridgesByServiceGroup(String serviceGroup) throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<CartridgeDefinitionBean>>() {
            }.getType();
            List<CartridgeDefinitionBean> cartridgeList = (List<CartridgeDefinitionBean>) restClient.listEntity(ENDPOINT_LIST_CARTRIDGES,
                    listType, "cartridges");

            ArrayList<CartridgeDefinitionBean> arrayList = new ArrayList<CartridgeDefinitionBean>();
            for (int i = 0; i < cartridgeList.size(); i++) {
                if (serviceGroup.equals(cartridgeList.get(i).getServiceGroup())) {
                    arrayList.add(cartridgeList.get(i));
                }
            }
            return arrayList;
        } catch (Exception e) {
            String message = "Error in listing cartridges";
            System.out.println(message);
            log.error(message, e);
            return null;
        }
    }

    // List currently available multi tenant and single tenant cartridges
    public void listCartridges() throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<CartridgeDefinitionBean>>() {
            }.getType();
            List<CartridgeDefinitionBean> cartridgeList = (List<CartridgeDefinitionBean>) restClient.listEntity(ENDPOINT_LIST_CARTRIDGES,
                    listType, "cartridges");

            if ((cartridgeList == null) || (cartridgeList.size() == 0)) {
                System.out.println("No cartridges found");
                return;
            }

            RowMapper<CartridgeDefinitionBean> cartridgeMapper = new RowMapper<CartridgeDefinitionBean>() {
                public String[] getData(CartridgeDefinitionBean cartridge) {
                    String[] data = new String[6];
                    data[0] = cartridge.getType();
                    data[1] = cartridge.getCategory();
                    data[2] = cartridge.getDisplayName();
                    data[3] = cartridge.getDescription();
                    data[4] = cartridge.getVersion();
                    data[5] = String.valueOf(cartridge.isMultiTenant());
                    return data;
                }
            };

            CartridgeDefinitionBean[] cartridges = new CartridgeDefinitionBean[cartridgeList.size()];
            cartridges = cartridgeList.toArray(cartridges);

            System.out.println("Cartridges found:");
            CliUtils.printTable(cartridges, cartridgeMapper, "Type", "Category", "Name", "Description", "Version",
                    "Multi-Tenant");
        } catch (Exception e) {
            String message = "Error in listing cartridges";
            System.out.println(message);
            log.error(message, e);
        }
    }

    public void describeCartridge(final String cartridgeType) throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<CartridgeDefinitionBean>>() {
            }.getType();
            // GET /cartridges/{cartridgeType} not available, hence using the list method
            List<CartridgeDefinitionBean> cartridgeList = (List<CartridgeDefinitionBean>) restClient.listEntity(ENDPOINT_LIST_CARTRIDGES,
                    listType, "cartridges");

            CartridgeDefinitionBean cartridge = null;
            for(CartridgeDefinitionBean item : cartridgeList) {
                if(item.getType().equals(cartridgeType)) {
                    cartridge = item;
                    break;
                }
            }

            if (cartridge == null) {
                System.out.println("Cartridge not found");
                return;
            }

            System.out.println("-------------------------------------");
            System.out.println("Cartridge Information:");
            System.out.println("-------------------------------------");
            System.out.println("Type: " + cartridge.getType());
            System.out.println("Category: " + cartridge.getCategory());
            System.out.println("Name: " + cartridge.getDisplayName());
            System.out.println("Description: " + cartridge.getDescription());
            System.out.println("Version: " + cartridge.getVersion());
            System.out.println("Multi-Tenant: " + cartridge.isMultiTenant());
            System.out.println("Hostname: " + cartridge.getHost());

            if(cartridge.getIaasProvider() != null) {
                RowMapper<IaasProviderBean> cartridgeMapper = new RowMapper<IaasProviderBean>() {
                    public String[] getData(IaasProviderBean row) {
                        String[] data = new String[5];
                        data[0] = row.getProvider();
                        data[1] = row.getType();
                        data[2] = row.getName();
                        data[3] = row.getImageId();
                        data[4] = String.valueOf(row.getMaxInstanceLimit());
                        return data;
                    }
                };

                IaasProviderBean[] iaasProviders = new IaasProviderBean[cartridgeList.size()];
                iaasProviders = cartridge.getIaasProvider().toArray(iaasProviders);

                System.out.println("-------------------------------------");
                System.out.println("IaaS Providers: ");
                System.out.println("-------------------------------------");
                CliUtils.printTable(iaasProviders, cartridgeMapper, "Provider", "Type", "Name", "Image ID",
                        "Max Instance Limit");
            }
            System.out.println("-------------------------------------");
        } catch (Exception e) {
            String message = "Error in describing cartridge: " + cartridgeType;
            System.out.println(message);
            log.error(message, e);
        }
    }

    // Lists subscribed cartridge info (from alias)
    public void describeCartridgeSubscription(String alias) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            CartridgeWrapper cartridgeWrapper = (CartridgeWrapper) restClient.getEntity(ENDPOINT_GET_CARTRIDGE_OF_TENANT,
                    CartridgeWrapper.class, "{id}", alias, "cartridge subscription");

            if((cartridgeWrapper == null) || (cartridgeWrapper.getCartridge() == null)) {
                System.out.println("Cartridge subscription not found: " + alias);
                return;
            }

            Cartridge cartridge = cartridgeWrapper.getCartridge();

            // Get LB IP s
            //Map<String, Set<String>> lbIpMap = getLbIpList(cartridge, httpClient);
            //final Set<String> lbPrivateIpSet = lbIpMap.get("private");
            //final Set<String> lbFloatingIpSet = lbIpMap.get("floating");
            Cartridge[] cartridges = new Cartridge[1];
            cartridges[0] = cartridge;

            System.out.println("\nSubscribed Cartridges Info\n");
            System.out.println("\tType : " + cartridge.getCartridgeType());
            System.out.println("\tName : " + cartridge.getDisplayName());
            System.out.println("\tVersion : " + cartridge.getVersion());
            //System.out.println("\tPublic : " + cartridge.getIsPublic());
            String tenancy = cartridge.isMultiTenantSpecified() ? "Multi-Tenant" : "Single-Tenant";
            System.out.println("\tTenancy Model	: " + tenancy);
            System.out.println("\tAlias : " + cartridge.getCartridgeAlias());
            System.out.println("\tStatus : " + cartridge.getStatus());
            String instanceCount = String.valueOf(cartridge.getActiveInstances());
            System.out.println("\tRunning Instances	: " + instanceCount);
            //System.out.println("\tAccess URL(s) : " + getAccessURLs(cartridge));
            if (cartridge.getRepoURL() != null) {
                System.out.println("\tRepo URL : " + cartridge.getRepoURL());
            }
            //System.out.println("\tLB Private IP	: " + lbPrivateIpSet.toString());
            //if (lbFloatingIpSet != null) {
            //    System.out.println("\tLB Floating IP : " + lbFloatingIpSet.toString());
            //}
            //if (cartridge.getProvider().equals("data")) {
//                System.out.println("\tDB-username : " + cartridge.getDbUserName());
//                System.out.println("\tDB-password : " + cartridge.getPassword());
//                System.out.println("\tDB-Host IP (private)  : " + cartridge.getIp());
//                if (cartridge.getPublicIp() != null) {
//                    System.out.println("\tDB-Host IP (floating) : "
//                            + cartridge.getPublicIp());
//                }
//            }
        } catch (Exception e) {
            String message = "Error in getting cartridge subscription";
            System.out.println(message);
            log.error(message, e);
        }
    }

    public void listMembersOfCluster(String cartridgeType, String alias) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {

            Member[] members = getMembers(cartridgeType, alias, httpClient);

            if (members == null) {
                // these conditions are handled in the getMembers method
                return;
            }

            if (members.length == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("No members found");
                }
                System.out.println("No members found for the corresponding cluster for type " + cartridgeType
                        + ", alias " + alias);
                return;
            }

            System.out.println("\nList of members in the cluster: " + alias);
            for (Member member : members) {
                System.out.println("\n\tServiceName : " + member.getServiceName());
                System.out.println("\tClusterId : " + member.getClusterId());
                System.out.println("\tNewtworkPartitionId : " + member.getNetworkPartitionId());
                System.out.println("\tPartitionId : " + member.getPartitionId());
                System.out.println("\tStatus : " + member.getStatus());
                if (member.getLbClusterId() != null) {
                    System.out.println("\tLBCluster : " + member.getLbClusterId());
                }
                System.out.println("\tMemberPrivateIp : " + member.getMemberIp());
                System.out.println("\tMemberFloatingIp : " + member.getMemberPublicIp());
                System.out.println("\tMember Properties : " + member.getProperty());
                System.out.println("\t-----------------------");
            }

            System.out.println("==================================================");
            System.out.println("List of LB members for the [cluster]: " + alias);

            // Invoke  clusters/{clusterId}
            for (Member m : members) {
                HttpResponse responseCluster = restClient.doGet(httpClient, restClient.getBaseURL() + ENDPOINT_GET_CLUSTER_OF_TENANT
                        + m.getLbClusterId());

                String responseCode = "" + responseCluster.getStatusLine().getStatusCode();
                String resultStringCluster = CliUtils.getHttpResponseString(responseCluster);

                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();

                if (!responseCode.equals(CliConstants.RESPONSE_OK)) {
                    ExceptionMapper exception = gson.fromJson(resultStringCluster, ExceptionMapper.class);
                    System.out.println(exception);
                    break;
                }
                if (resultStringCluster != null && !resultStringCluster.isEmpty()) {

                    printLBs(resultStringCluster);
                }
            }

            System.out.println("==================================================");

        } catch (Exception e) {
            String message = "Error in listing members";
            System.out.println(message);
            log.error(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private Member[] getMembers(String cartridgeType, String alias, DefaultHttpClient httpClient) throws Exception {
        try {
            HttpResponse response = restClient.doGet(httpClient, restClient.getBaseURL()
                    + ENDPOINT_GET_CLUSTER_OF_TENANT + alias);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            Gson gson = new Gson();
            if (!responseCode.equals(CliConstants.RESPONSE_OK)) {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return null;
            }

            Cluster cluster = getClusterObjectFromString(CliUtils.getHttpResponseString(response));

            if (cluster == null) {
                System.out.println("No existing subscriptions found for alias " + alias);
                return null;
            }

            Member[] members = new Member[cluster.getMember().size()];
            members = cluster.getMember().toArray(members);

            return members;
        } catch (Exception e) {
            String message = "Error in listing members";
            System.out.println(message);
            log.error(message, e);
            return null;
        }
    }

    private Cluster getClusterObjectFromString(String resultString) {
        String tmp;
        if (resultString.startsWith("{\"cluster\"")) {
            tmp = resultString.substring("{\"cluster\"".length() + 1, resultString.length() - 1);
            resultString = tmp;
        }
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        return gson.fromJson(resultString, Cluster.class);
    }

    private void printLBs(String resultString) {

        Cluster cluster = getClusterObjectFromString(resultString);

        if (cluster == null) {
            System.out.println("Subscribe cartridge list is null");
            return;
        }

        Member[] members = new Member[cluster.getMember().size()];
        members = cluster.getMember().toArray(members);

        if (members.length == 0) {
            if (log.isDebugEnabled()) {
                log.debug("No subscribed cartridges found");
            }
            System.out.println("There are no subscribed cartridges");
            return;
        }

        for (Member member : members) {
            System.out.println("\n\tServiceName : " + member.getServiceName());
            System.out.println("\tClusterId : " + member.getClusterId());
            System.out.println("\tNewtworkPartitionId : " + member.getNetworkPartitionId());
            System.out.println("\tPartitionId : " + member.getPartitionId());
            System.out.println("\tStatus : " + member.getStatus());
            if (member.getLbClusterId() != null) {
                System.out.println("\tLBCluster : " + member.getLbClusterId());
            }
            System.out.println("\tMemberPrivateIp : " + member.getMemberIp());
            System.out.println("\tMemberFloatingIp : " + member.getMemberPublicIp());
            System.out.println("\t-----------------------");
        }
    }

    // This method helps to create the new tenant
    public void addTenant(String admin, String firstName, String lastaName, String password, String domain, String email)
            throws CommandException {
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

            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL()
                    + ENDPOINT_ADD_TENANT, jsonString);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("Tenant added successfully");
                return;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            handleException("Exception in creating tenant", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to create the new user
    public void addUser(String userName, String credential, String role, String firstName, String lastName, String email, String profileName)
            throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            UserInfoBean userInfoBean = new UserInfoBean();
            userInfoBean.setUserName(userName);
            userInfoBean.setCredential(credential);
            userInfoBean.setRole(role);
            userInfoBean.setFirstName(firstName);
            userInfoBean.setLastName(lastName);
            userInfoBean.setEmail(email);
            userInfoBean.setProfileName(profileName);

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            String jsonString = gson.toJson(userInfoBean, UserInfoBean.class);

            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL()
                    + ENDPOINT_ADD_USER, jsonString);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            if (responseCode.equals(CliConstants.RESPONSE_CREATED)) {
                System.out.println("User added successfully");
                return;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            handleException("Exception in creating User", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to delete the created tenant
    public void deleteTenant(String tenantDomain) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doDelete(httpClient, restClient.getBaseURL()
                    + ENDPOINT_ADD_TENANT + "/" + tenantDomain);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have succesfully delete " + tenantDomain + " tenant");
                return;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            handleException("Exception in deleting " + tenantDomain + " tenant", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to delete the created user
    public void deleteUser(String userName) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doDelete(httpClient, restClient.getBaseURL()
                    + ENDPOINT_ADD_USER + "/" + userName);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_NO_CONTENT)) {
                System.out.println("You have succesfully deleted " + userName + " user");
                return;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            handleException("Exception in deleting " + userName + " user", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to deactivate the created tenant
    public void deactivateTenant(String tenantDomain) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL()
                    + ENDPOINT_DEACTIVATE_TENANT + "/" + tenantDomain, "");

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have succesfully deactivate " + tenantDomain + " tenant");
                return;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            handleException("Exception in deactivating " + tenantDomain + " tenant", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to activate, deactivated tenant
    public void activateTenant(String tenantDomain) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL()
                    + ENDPOINT_ACTIVATE_TENANT + "/" + tenantDomain, "");

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have succesfully activated tenant: " + tenantDomain);
                return;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            handleException("Error in activating tenant: " + tenantDomain, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to list all tenants
    public void listAllTenants() throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<TenantInfoBean>>() {
            }.getType();
            List<TenantInfoBean> tenantInfoList = (List<TenantInfoBean>)restClient.listEntity(ENDPOINT_LIST_TENANTS,
                    listType, "tenants");

            if ((tenantInfoList == null) || (tenantInfoList.size() == 0)) {
                System.out.println("No tenants found");
                return;
            }

            RowMapper<TenantInfoBean> rowMapper = new RowMapper<TenantInfoBean>() {
                public String[] getData(TenantInfoBean tenantInfo) {
                    String[] data = new String[5];
                    data[0] = tenantInfo.getTenantDomain();
                    data[1] = "" + tenantInfo.getTenantId();
                    data[2] = tenantInfo.getEmail();
                    data[3] = tenantInfo.isActive() ? "Active" : "De-active";
                    //data[4] = tenantInfo.getCreatedDate();
                    return data;
                }
            };

            TenantInfoBean[] tenantArray = new TenantInfoBean[tenantInfoList.size()];
            tenantArray = tenantInfoList.toArray(tenantArray);

            System.out.println("Tenants:");
            CliUtils.printTable(tenantArray, rowMapper, "Domain", "Tenant ID", "Email", "State");
        } catch (Exception e) {
            String message = "Error in listing users";
            System.out.println(message);
            log.error(message, e);
        }
    }

    // This method helps to list all users
    public void listAllUsers() throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<UserInfoBean>>() {
            }.getType();
            List<UserInfoBean> userInfoList = (List<UserInfoBean>) restClient.listEntity(ENDPOINT_LIST_USERS,
                    listType, "users");

            if ((userInfoList == null) || (userInfoList.size() == 0)) {
                System.out.println("No users found");
                return;
            }

            RowMapper<UserInfoBean> rowMapper = new RowMapper<UserInfoBean>() {
                public String[] getData(UserInfoBean userInfo) {
                    String[] data = new String[2];
                    data[0] = userInfo.getUserName();
                    data[1] = userInfo.getRole();
                    return data;
                }
            };

            UserInfoBean[] usersArray = new UserInfoBean[userInfoList.size()];
            usersArray = userInfoList.toArray(usersArray);

            System.out.println("Users:");
            CliUtils.printTable(usersArray, rowMapper, "Username", "Role");
        } catch (Exception e) {
            String message = "Error in listing users";
            System.out.println(message);
            log.error(message, e);
        }
    }

    // This method helps to deploy cartridge definitions
    public void deployCartridgeDefinition(String cartridgeDefinition) throws CommandException {
        restClient.deployEntity(ENDPOINT_DEPLOY_CARTRIDGE, cartridgeDefinition, "cartridge");
    }

    // This method helps to undeploy cartridge definitions
    public void undeployCartrigdeDefinition(String id) throws CommandException {
        restClient.undeployEntity(ENDPOINT_UNDEPLOY_CARTRIDGE, "cartridge", id);
    }

    // This method helps to deploy partitions
    public void deployPartition(String partitionDefinition) throws CommandException {
        restClient.deployEntity(ENDPOINT_DEPLOY_PARTITION, partitionDefinition, "partition");
    }

    // This method helps to deploy autoscalling polices
    public void deployAutoscalingPolicy(String autoScalingPolicy) throws CommandException {
        restClient.deployEntity(ENDPOINT_DEPLOY_AUTOSCALING_POLICY, autoScalingPolicy, "autoscaling policy");
    }

   // This method helps to update an autoscaling policy
    public void updateAutoscalingPolicy(String autoScalingPolicy) throws CommandException {
        restClient.updateEntity(ENDPOINT_UPDATE_AUTOSCALING_POLICY, autoScalingPolicy, "autoscaling policy");
    }

    // This method helps to list applications
    public void listApplications() throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<ApplicationBean>>() {
            }.getType();
            List<ApplicationBean> list = (List<ApplicationBean>) restClient.listEntity(ENDPOINT_LIST_APPLICATION,
                    listType, "applications");

            if ((list == null) || (list.size() == 0)) {
                System.out.println("No applications found");
                return;
            }

            RowMapper<ApplicationBean> rowMapper = new RowMapper<ApplicationBean>() {
                public String[] getData(ApplicationBean definition) {
                    String[] data = new String[1];
                    data[0] = definition.getId();
                    return data;
                }
            };

            ApplicationBean[] array = new ApplicationBean[list.size()];
            array = list.toArray(array);

            System.out.println("Applications found:");
            CliUtils.printTable(array, rowMapper, "Application ID");
        } catch (Exception e) {
            String message = "Error in listing applications";
            System.out.println(message);
            log.error(message, e);
        }
    }

    // This method helps to deploy deployment polices
    public void deployDeploymentPolicy(String deploymentPolicy) throws CommandException {
        restClient.deployEntity(ENDPOINT_DEPLOY_DEPLOYMENT_POLICY, deploymentPolicy, "deployment policy");
    }

    // This method helps to update a deployment policy
    public void updateDeploymentPolicy(String deploymentPolicy) throws CommandException {
        restClient.updateEntity(ENDPOINT_UPDATE_DEPLOYMENT_POLICY, deploymentPolicy, "deployment policy");
    }

    // This method lists available partitions
    public void listPartitions() throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<Partition>>() {
            }.getType();
            List<Partition> list = (List<Partition>) restClient.listEntity(ENDPOINT_LIST_PARTITIONS,
                    listType, "partitions");

            if ((list == null) || (list.size() == 0)) {
                System.out.println("No partitions found");
                return;
            }

            RowMapper<Partition> rowMapper = new RowMapper<Partition>() {

                public String[] getData(Partition partition) {
                    String[] data = new String[3];
                    data[0] = partition.getId();
                    data[1] = partition.getProvider();
                    data[2] = partition.getIsPublic() ? "Public" : "Private";
                    return data;
                }
            };

            Partition[] partitions = new Partition[list.size()];
            partitions = list.toArray(partitions);

            System.out.println("Partitions found:");
            CliUtils.printTable(partitions, rowMapper, "ID", "Provider", "Accessibility");
        } catch (Exception e) {
            String message = "Error in listing partitions";
            System.out.println(message);
            log.error(message, e);
        }
    }

    public void listAutoscalingPolicies() throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<AutoscalePolicy>>() {
            }.getType();
            List<AutoscalePolicy> list = (List<AutoscalePolicy>) restClient.listEntity(ENDPOINT_LIST_AUTOSCALING_POLICIES,
                    listType, "autoscaling policies");

            if ((list == null) || (list == null) || (list.size() == 0)) {
                System.out.println("No autoscaling policies found");
                return;
            }

            RowMapper<AutoscalePolicy> rowMapper = new RowMapper<AutoscalePolicy>() {

                public String[] getData(AutoscalePolicy policy) {
                    String[] data = new String[2];
                    data[0] = policy.getId();
                    data[1] = policy.getIsPublic() ? "Public" : "Private";
                    return data;
                }
            };

            AutoscalePolicy[] array = new AutoscalePolicy[list.size()];
            array = list.toArray(array);

            System.out.println("Autoscaling policies found:");
            CliUtils.printTable(array, rowMapper, "ID", "Accessibility");
        } catch (Exception e) {
            String message = "Error in listing autoscaling policies";
            System.out.println(message);
            log.error(message, e);
        }
    }

    public void listDeploymentPolicies() throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<DeploymentPolicy>>() {
            }.getType();
            List<DeploymentPolicy> list = (List<DeploymentPolicy>) restClient.listEntity(ENDPOINT_LIST_DEPLOYMENT_POLICIES,
                    listType, "deployment policies");

            if ((list == null) || (list.size() == 0)) {
                System.out.println("No deployment policies found");
                return;
            }

            RowMapper<DeploymentPolicy> rowMapper = new RowMapper<DeploymentPolicy>() {

                public String[] getData(DeploymentPolicy policy) {
                    String[] data = new String[2];
                    data[0] = policy.getId();
                    data[1] = policy.getIsPublic() ? "Public" : "Private";
                    return data;
                }
            };

            DeploymentPolicy[] array = new DeploymentPolicy[list.size()];
            array = list.toArray(array);

            System.out.println("Deployment policies found:");
            CliUtils.printTable(array, rowMapper, "ID", "Accessibility");
        } catch (Exception e) {
            String message = "Error in listing deployment policies";
            System.out.println(message);
            log.error(message, e);
        }
    }

    public void describeDeploymentPolicy(String id) throws CommandException {
        try {
            DeploymentPolicy policy = (DeploymentPolicy) restClient.getEntity(ENDPOINT_GET_DEPLOYMENT_POLICY,
                    DeploymentPolicy.class, "{id}", id, "deployment policy");

	        if (policy == null) {
	            System.out.println("Deployment policy not found: " + id);
	            return;
	        }

	        System.out.println("Deployment policy: " + id);
	        System.out.println(getGson().toJson(policy));
	    } catch (Exception e) {
	        String message = "Error in describing deployment policy: " + id;
	        System.out.println(message);
	        log.error(message, e);
	    }
    }

    public void describeAutoScalingPolicy(String id) throws CommandException {
        try {
            AutoscalePolicy policy = (AutoscalePolicy) restClient.getEntity(ENDPOINT_GET_AUTOSCALING_POLICY,
                    AutoscalePolicy.class, "{id}", id, "autoscaling policy");

            if (policy == null) {
                System.out.println("Autoscaling policy not found: " + id);
                return;
            }

            System.out.println("Autoscaling policy: " + id);
            System.out.println(getGson().toJson(policy));
        } catch (Exception e) {
            String message = "Error in describing autoscaling policy: " + id;
            System.out.println(message);
            log.error(message, e);
        }
    }

    public void deployKubernetesCluster(String entityBody) {
        restClient.deployEntity(ENDPOINT_DEPLOY_KUBERNETES_CLUSTER, entityBody, "kubernetes cluster");
    }

    public void listKubernetesClusters() {
        try {
            Type listType = new TypeToken<ArrayList<KubernetesHost>>() {
            }.getType();
            List<KubernetesGroup> list = (List<KubernetesGroup>) restClient.
                    listEntity(ENDPOINT_LIST_KUBERNETES_CLUSTERS, listType, "kubernetes cluster");
            if ((list != null) && (list.size() > 0)) {
                RowMapper<KubernetesGroup> partitionMapper = new RowMapper<KubernetesGroup>() {
                    public String[] getData(KubernetesGroup kubernetesGroup) {
                        String[] data = new String[2];
                        data[0] = kubernetesGroup.getGroupId();
                        data[1] = kubernetesGroup.getDescription();
                        return data;
                    }
                };

                KubernetesGroup[] array = new KubernetesGroup[list.size()];
                array = list.toArray(array);
                System.out.println("Kubernetes groups found:");
                CliUtils.printTable(array, partitionMapper, "Group ID", "Description");
            } else {
                System.out.println("No kubernetes groups found");
                return;
            }
        } catch (Exception e) {
            String message = "Error in listing kubernetes groups";
            System.out.println(message);
            log.error(message, e);
        }
    }

    public void undeployKubernetesCluster(String clusterId) {
        restClient.undeployEntity(ENDPOINT_UNDEPLOY_KUBERNETES_CLUSTER, "kubernetes cluster", clusterId);
    }

    public void deployKubernetesHost(String entityBody, String clusterId) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doPut(httpClient, restClient.getBaseURL()
                    + ENDPOINT_DEPLOY_KUBERNETES_HOST.replace("{kubernetesClusterId}", clusterId), entityBody);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have succesfully deployed host to Kubernetes cluster: " + clusterId);
                return;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            handleException("Error in deploying host to Kubernetes cluster: " + clusterId, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    public void listKubernetesHosts(String clusterId) {
        try {
            Type listType = new TypeToken<ArrayList<KubernetesHost>>() {
            }.getType();
            List<KubernetesHost> list = (List<KubernetesHost>) restClient.listEntity(ENDPOINT_LIST_KUBERNETES_HOSTS.replace("{kubernetesClusterId}", clusterId),
                    listType, "kubernetes host");
            if ((list != null) && (list.size() > 0)) {
                RowMapper<KubernetesHost> partitionMapper = new RowMapper<KubernetesHost>() {
                    public String[] getData(KubernetesHost kubernetesHost) {
                        String[] data = new String[3];
                        data[0] = kubernetesHost.getHostId();
                        data[1] = kubernetesHost.getHostname();
                        data[2] = kubernetesHost.getHostIpAddress();
                        return data;
                    }
                };

                KubernetesHost[] array = new KubernetesHost[list.size()];
                array = list.toArray(array);
                System.out.println("Kubernetes hosts found:");
                CliUtils.printTable(array, partitionMapper, "Host ID", "Hostname", "IP Address");
            } else {
                System.out.println("No kubernetes hosts found");
                return;
            }
        } catch (Exception e) {
            String message = "Error in listing kubernetes hosts";
            System.out.println(message);
            log.error(message, e);
        }
    }

    public void undeployKubernetesHost(String clusterId, String hostId) {
        restClient.undeployEntity(ENDPOINT_UNDEPLOY_KUBERNETES_HOST.replace("{kubernetesClusterId}", clusterId), "kubernetes host", hostId);
    }

    public void updateKubernetesMaster(String entityBody, String clusterId) {
    	System.out.println(ENDPOINT_UPDATE_KUBERNETES_MASTER.replace("{kubernetesClusterId}", clusterId));
        restClient.updateEntity(ENDPOINT_UPDATE_KUBERNETES_MASTER.replace("{kubernetesClusterId}", clusterId), entityBody, "kubernetes master");
    }

    public void updateKubernetesHost(String entityBody, String clusterId, String hostId) {
    	System.out.println((ENDPOINT_UPDATE_KUBERNETES_HOST.replace("{kubernetesClusterId}", clusterId)).replace("{minionId}", hostId));
        restClient.updateEntity((ENDPOINT_UPDATE_KUBERNETES_HOST.replace("{kubernetesClusterId}", clusterId)).replace("{minionId}", hostId), entityBody, "kubernetes host");
    }

    public void synchronizeArtifacts(String cartridgeAlias) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL() + ENDPOINT_SYNCHRONIZE_ARTIFACTS.replace("{subscriptionAlias}", cartridgeAlias), cartridgeAlias);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println(String.format("Synchronizing artifacts for cartridge subscription alias: %s", cartridgeAlias));
                return;
            } else {
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }
        } catch (Exception e) {
            String message = "Error in synchronizing artifacts for cartridge subscription alias: " + cartridgeAlias;
            System.out.println(message);
            log.error(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to update cartridge subscription properties
    public void updateSubscritptionProperties(String alias, String subscriptionJson) {
        String url = ENDPOINT_UPDATE_SUBSCRIPTION_PROPERTIES.replace("{subscriptionAlias}", alias);
        restClient.updateEntity(url, subscriptionJson, "subscription alias: "+alias);
    }

    // This method helps to deploy service groups
    public void deployServiceGroup (String entityBody) {
        restClient.deployEntity(ENDPOINT_DEPLOY_SERVICE_GROUP, entityBody, "service group");
    }

    // This method helps to undeploy service groups
    public void undeployServiceGroup (String groupDefinitionName) throws CommandException {
        restClient.undeployEntity(ENDPOINT_UNDEPLOY_SERVICE_GROUP, "service group", groupDefinitionName);
    }

    // This method helps to describe service group definition
    public void describeServiceGroup (String groupDefinitionName) {
        try {
            GroupBean bean = (GroupBean) restClient.listEntity(ENDPOINT_LIST_SERVICE_GROUP.replace("{groupDefinitionName}", groupDefinitionName),
                    GroupBean.class, "serviceGroup");

            if (bean == null) {
                System.out.println("Service group not found: " + groupDefinitionName);
                return;
            }

            System.out.println("Service Group : " + groupDefinitionName);
            System.out.println(getGson().toJson(bean));
        } catch (Exception e) {
            String message = "Error in describing service group: " + groupDefinitionName;
            System.out.println(message);
            log.error(message, e);
        }
    }

    // This method helps to deploy applications
    public void deployApplication (String entityBody) {
        restClient.deployEntity(ENDPOINT_DEPLOY_APPLICATION, entityBody, "application");
    }

    // This method helps to undeploy applications
    public void undeployApplication(String id) throws CommandException {
        restClient.undeployEntity(ENDPOINT_UNDEPLOY_APPLICATION, "applicationId", id);
    }

    // This method helps to describe applications
    public void describeApplication (String applicationID) {
        try {
            Type listType = new TypeToken<ArrayList<ApplicationBean>>() {
            }.getType();
            List<ApplicationBean> applications = (List<ApplicationBean>) restClient
                    .listEntity(ENDPOINT_GET_APPLICATION.replace("{appId}", applicationID),
                            listType, "applications");

            if (applications == null) {
                System.out.println("Application not found: " + applicationID);
                return;
            }

            System.out.println("Application: " + applicationID);
            System.out.println(getGson().toJson(applications));
        } catch (Exception e) {
            String message = "Error in describing application: " + applicationID;
            System.out.println(message);
            log.error(message, e);
        }
    }

    // This will return access url from a given cartridge
    private String getAccessURLs(CartridgeDefinitionBean cartridge) {
        List<PortMappingBean> portMappings = cartridge.getPortMapping();
        StringBuilder urlBuilder = new StringBuilder();

        for (PortMappingBean portMapping : portMappings) {
            String url = portMapping.getProtocol() + "://" + cartridge.getHost() + ":" + portMapping.getProxyPort() + "/";
            urlBuilder.append(url).append(", ");
        }

        return urlBuilder.toString();
    }

    // This is for handle exception
    private void handleException(String key, Exception e, Object... args) throws CommandException {
        if (log.isDebugEnabled()) {
            log.debug("Displaying message for {}. Exception thrown is {}", key, e.getClass());
        }

        String message = CliUtils.getMessage(key, args);

        if (log.isErrorEnabled()) {
            log.error(message);
        }

        System.out.println(message);
        throw new CommandException(message, e);
    }

    // This class is to convert JSON string to Cartridge object
    public class CartridgeWrapper {
        private Cartridge cartridge;

        public Cartridge getCartridge() {
            return cartridge;
        }

        public void setCartridge(Cartridge cartridge) {
            this.cartridge = cartridge;
        }

        public CartridgeWrapper() {
        }
    }

    public boolean isMultiTenant(String type) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doGet(httpClient, restClient.getBaseURL() + ENDPOINT_LIST_CARTRIDGES);

            String responseCode = "" + response.getStatusLine().getStatusCode();
            String resultString = CliUtils.getHttpResponseString(response);
            if (resultString == null) {
                return false;
            }

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (!responseCode.equals(CliConstants.RESPONSE_OK)) {
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return false;
            }

            Type listType = new TypeToken<ArrayList<CartridgeDefinitionBean>>() {}.getType();
            List<CartridgeDefinitionBean> cartridgeList = gson.fromJson(resultString, listType);

            if (cartridgeList == null) {
                System.out.println("Available cartridge list is null");
                return false;
            }

            ArrayList<CartridgeDefinitionBean> multiTenetCartridge = new ArrayList<CartridgeDefinitionBean>();

            for (CartridgeDefinitionBean cartridge : cartridgeList) {
                if (cartridge.isMultiTenant() && cartridge.getType().equals(type)) {
                    multiTenetCartridge.add(cartridge);
                }
            }

            return multiTenetCartridge.size() > 0;

        } catch (Exception e) {
            handleException("Exception in listing cartridges", e);
            return false;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
}

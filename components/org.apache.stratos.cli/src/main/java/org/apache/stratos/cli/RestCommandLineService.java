/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.exception.ExceptionMapper;
import org.apache.stratos.cli.utils.CliConstants;
import org.apache.stratos.cli.utils.CliUtils;
import org.apache.stratos.cli.utils.RowMapper;
import org.apache.stratos.common.beans.ResponseMessageBean;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.beans.UserInfoBean;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.common.beans.application.domain.mapping.DomainMappingBean;
import org.apache.stratos.common.beans.application.signup.ApplicationSignUpBean;
import org.apache.stratos.common.beans.cartridge.CartridgeBean;
import org.apache.stratos.common.beans.cartridge.CartridgeGroupBean;
import org.apache.stratos.common.beans.cartridge.IaasProviderBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesClusterBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesHostBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesMasterBean;
import org.apache.stratos.common.beans.partition.NetworkPartitionBean;
import org.apache.stratos.common.beans.policy.autoscale.AutoscalePolicyBean;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.common.beans.policy.deployment.DeploymentPolicyBean;
import org.apache.stratos.common.beans.topology.ClusterBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.apache.stratos.cli.utils.CliUtils.getHttpResponseString;

public class RestCommandLineService {

    private static final Logger log = LoggerFactory.getLogger(RestCommandLineService.class);

    private RestClient restClient;

    // REST endpoints
    private static final String API_CONTEXT = "/api/v4.1";
    private static final String ENDPOINT_INIT = API_CONTEXT + "/init";

    private static final String ENDPOINT_ADD_TENANT = API_CONTEXT + "/tenants";
    private static final String ENDPOINT_ADD_USER = API_CONTEXT + "/users";
    private static final String ENDPOINT_ADD_APPLICATION = API_CONTEXT + "/applications";

    private static final String ENDPOINT_DEPLOY_CARTRIDGE = API_CONTEXT + "/cartridges";
    private static final String ENDPOINT_DEPLOY_AUTOSCALING_POLICY = API_CONTEXT + "/autoscalingPolicies";
    private static final String ENDPOINT_DEPLOY_APPLICATION_POLICY = API_CONTEXT + "/applicationPolicies";
    private static final String ENDPOINT_DEPLOY_DEPLOYMENT_POLICY = API_CONTEXT + "/deploymentPolicies";
    private static final String ENDPOINT_DEPLOY_KUBERNETES_CLUSTER = API_CONTEXT + "/kubernetesClusters";
    private static final String ENDPOINT_DEPLOY_KUBERNETES_HOST = API_CONTEXT + "/kubernetesClusters/{kubernetesClusterId}/minion";
    private static final String ENDPOINT_DEPLOY_SERVICE_GROUP = API_CONTEXT + "/cartridgeGroups";
    private static final String ENDPOINT_DEPLOY_APPLICATION = API_CONTEXT + "/applications/{applicationId}/deploy/{applicationPolicyId}";
    private static final String ENDPOINT_DEPLOY_NETWORK_PARTITION = API_CONTEXT + "/networkPartitions";

    private static final String ENDPOINT_UNDEPLOY_KUBERNETES_CLUSTER = API_CONTEXT + "/kubernetesClusters/{id}";
    private static final String ENDPOINT_UNDEPLOY_KUBERNETES_HOST = API_CONTEXT + "/kubernetesClusters/{kubernetesClusterId}/hosts/{id}";
    private static final String ENDPOINT_UNDEPLOY_SERVICE_GROUP = API_CONTEXT + "/cartridgeGroups/{id}";
    private static final String ENDPOINT_UNDEPLOY_APPLICATION = API_CONTEXT + "/applications/{id}/undeploy";
    private static final String ENDPOINT_UNDEPLOY_CARTRIDGE = API_CONTEXT + "/cartridges/{id}";

    private static final String ENDPOINT_REMOVE_APPLICATION = API_CONTEXT + "/applications/{appId}";
    private static final String ENDPOINT_REMOVE_NETWORK_PARTITION = API_CONTEXT + "/networkPartitions/{id}";
    private static final String ENDPOINT_REMOVE_AUTOSCALINGPOLICY = API_CONTEXT + "/autoscalingPolicies/{policyId}";
    private static final String ENDPOINT_REMOVE_DEPLOYMENT_POLICY = API_CONTEXT + "/deploymentPolicies/{policyId}";
    private static final String ENDPOINT_REMOVE_APPLICATION_POLICY = API_CONTEXT + "/applicationPolicies/{policyId}";

    private static final String ENDPOINT_LIST_AUTOSCALING_POLICIES = API_CONTEXT + "/autoscalingPolicies";
    private static final String ENDPOINT_LIST_DEPLOYMENT_POLICIES = API_CONTEXT + "/deploymentPolicies";
    private static final String ENDPOINT_LIST_APPLICATION_POLICIES = API_CONTEXT + "/applicationPolicies";
    private static final String ENDPOINT_LIST_CARTRIDGES = API_CONTEXT + "/cartridges";
    private static final String ENDPOINT_LIST_CARTRIDGE_GROUPS = API_CONTEXT + "/cartridgeGroups";
    private static final String ENDPOINT_LIST_TENANTS = API_CONTEXT + "/tenants";
    private static final String ENDPOINT_LIST_USERS = API_CONTEXT + "/users";
    private static final String ENDPOINT_LIST_KUBERNETES_CLUSTERS = API_CONTEXT + "/kubernetesClusters";
    private static final String ENDPOINT_LIST_KUBERNETES_HOSTS = API_CONTEXT + "/kubernetesClusters/{kubernetesClusterId}/hosts";
    private static final String ENDPOINT_LIST_SERVICE_GROUP = API_CONTEXT + "/cartridgeGroups/{groupDefinitionName}";
    private static final String ENDPOINT_LIST_APPLICATION = API_CONTEXT + "/applications";
    private static final String ENDPOINT_LIST_NETWORK_PARTITIONS = API_CONTEXT + "/networkPartitions";
    private static final String ENDPOINT_LIST_CARTRIDGES_BY_FILTER = API_CONTEXT + "/cartridges/filter/{filter}";
    private static final String ENDPOINT_LIST_TENANTS_BY_PARTIAL_DOMAIN = API_CONTEXT + "/tenants/search/{tenantDomain}";

    private static final String ENDPOINT_DOMAIN_MAPPINGS = API_CONTEXT + "/applications/{applicationId}/domainMappings";
    private static final String ENDPOINT_REMOVE_DOMAIN_MAPPINGS = API_CONTEXT +
            "/applications/{applicationId}/domainMappings/{domainName}";

    private static final String ENDPOINT_GET_APPLICATION = API_CONTEXT + "/applications/{appId}";
    private static final String ENDPOINT_GET_AUTOSCALING_POLICY = API_CONTEXT + "/autoscalingPolicies/{id}";
    private static final String ENDPOINT_GET_DEPLOYMENT_POLICY = API_CONTEXT + "/deploymentPolicies/{deploymentPolicyId}";
    private static final String ENDPOINT_GET_APPLICATION_POLICY = API_CONTEXT + "/applicationPolicies/{applicationPolicyId}";
    private static final String ENDPOINT_GET_KUBERNETES_MASTER = API_CONTEXT + "/kubernetesClusters/{kubernetesClusterId}/master";
    private static final String ENDPOINT_GET_KUBERNETES_HOST_CLUSTER = API_CONTEXT + "/kubernetesClusters/{kubernetesClusterId}";
    private static final String ENDPOINT_GET_NETWORK_PARTITION = API_CONTEXT + "/networkPartitions/{networkPartitionId}";
    private static final String ENDPOINT_GET_APPLICATION_RUNTIME = API_CONTEXT + "/applications/{applicationId}/runtime";

    private static final String ENDPOINT_UPDATE_KUBERNETES_MASTER = API_CONTEXT + "/kubernetesClusters/{kubernetesClusterId}/master";
    private static final String ENDPOINT_UPDATE_KUBERNETES_HOST = API_CONTEXT + "/kubernetesClusters/host";

    private static final String ENDPOINT_SYNCHRONIZE_ARTIFACTS = API_CONTEXT + "/repo/synchronize/{subscriptionAlias}";
    private static final String ENDPOINT_ACTIVATE_TENANT = API_CONTEXT + "/tenants/activate/{tenantDomain}";
    private static final String ENDPOINT_DEACTIVATE_TENANT = API_CONTEXT + "/tenants/deactivate/{tenantDomain}";
    private static final String ENDPOINT_APPLICATION_SIGNUP = API_CONTEXT + "/applications/{applicationId}/signup";

    private static final String ENDPOINT_UPDATE_DEPLOYMENT_POLICY = API_CONTEXT + "/deploymentPolicies";
    private static final String ENDPOINT_UPDATE_APPLICATION = API_CONTEXT + "/applications";
    private static final String ENDPOINT_UPDATE_APPLICATION_POLICY = API_CONTEXT + "/applicationPolicies";
    private static final String ENDPOINT_UPDATE_AUTOSCALING_POLICY = API_CONTEXT + "/autoscalingPolicies";
    private static final String ENDPOINT_UPDATE_USER = API_CONTEXT + "/users";
    private static final String ENDPOINT_UPDATE_TENANT = API_CONTEXT + "/tenants";


    private static class SingletonHolder {
        private final static RestCommandLineService INSTANCE = new RestCommandLineService();
    }

    public static RestCommandLineService getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public Gson getGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        return gsonBuilder.setPrettyPrinting().create();
    }

    /**
     * Authenticate and login to stratos server.
     *
     * @param serverURL     URL of the server
     * @param username      username
     * @param password      password
     * @param validateLogin validate login
     * @return boolean
     * @throws Exception
     */
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
            // Create a trust manager that does not validate certificate chains
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
            String message = "Error connecting to the stratos server";
            printError(message, e);
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
                        System.out.println("Invalid value is set for STRATOS_URL");
                    }
                }
                return false;
            } else {
                // Just return true as we don't need to validate
                return true;
            }
        } catch (ConnectException e) {
            String message = "Could not connect to stratos manager";
            printError(message, e);
            return false;
        } catch (java.lang.NoSuchMethodError e) {
            String message = "Authentication failed!";
            printError(message, e);
            return false;
        } catch (Exception e) {
            if (e.getCause() instanceof MalformedChallengeException) {
                String message = "Authentication failed. Please check your username/password";
                printError(message, e);
                return false;
            }
            String message = "An unknown error occurred: " + e.getMessage();
            printError(message, e);
            return false;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Initialize the rest client and set username and password of the user
     *
     * @param serverURL server URL
     * @param username  username
     * @param password  password
     * @throws AxisFault
     */
    private void initializeRestClient(String serverURL, String username, String password) throws AxisFault {
        HttpTransportProperties.Authenticator authenticator = new HttpTransportProperties.Authenticator();
        authenticator.setUsername(username);
        authenticator.setPassword(password);
        authenticator.setPreemptiveAuthentication(true);

        ConfigurationContext configurationContext;
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

    /**
     * List available cartridges
     *
     * @throws CommandException
     */
    public void listCartridges() throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<CartridgeBean>>() {
            }.getType();
            List<CartridgeBean> cartridgeList = (List<CartridgeBean>) restClient.listEntity(ENDPOINT_LIST_CARTRIDGES,
                    listType, "cartridges");

            if ((cartridgeList == null) || (cartridgeList.size() == 0)) {
                System.out.println("No cartridges found");
                return;
            }

            RowMapper<CartridgeBean> cartridgeMapper = new RowMapper<CartridgeBean>() {
                public String[] getData(CartridgeBean cartridge) {
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

            CartridgeBean[] cartridges = new CartridgeBean[cartridgeList.size()];
            cartridges = cartridgeList.toArray(cartridges);

            System.out.println("Cartridges found:");
            CliUtils.printTable(cartridges, cartridgeMapper, "Type", "Category", "Name", "Description", "Version",
                    "Multi-Tenant");
        } catch (Exception e) {
            String message = "Error in listing cartridges";
            printError(message, e);
        }
    }

    /**
     * List cartridges By Filter
     *
     * @param filter cartridge-type
     * @throws CommandException
     */
    public void listCartridgesByFilter(String filter) throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<CartridgeBean>>() {
            }.getType();
            List<CartridgeBean> cartridgeList = (List<CartridgeBean>) restClient.listEntity(ENDPOINT_LIST_CARTRIDGES_BY_FILTER.replace("{filter}", filter),
                    listType, "cartridges");

            System.out.println("Test:" + ENDPOINT_LIST_CARTRIDGES_BY_FILTER.replace("{filter}", filter));

            if ((cartridgeList == null) || (cartridgeList.size() == 0)) {
                System.out.println("No cartridges found");
                return;
            }

            RowMapper<CartridgeBean> cartridgeMapper = new RowMapper<CartridgeBean>() {
                public String[] getData(CartridgeBean cartridge) {
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

            CartridgeBean[] cartridges = new CartridgeBean[cartridgeList.size()];
            cartridges = cartridgeList.toArray(cartridges);

            System.out.println("Cartridges found:");
            CliUtils.printTable(cartridges, cartridgeMapper, "Type", "Category", "Name", "Description", "Version",
                    "Multi-Tenant");
        } catch (Exception e) {
            String message = "Error in listing cartridges";
            printError(message, e);
        }
    }


    /**
     * List cartridge groups
     *
     * @throws CommandException
     */
    public void listCartridgeGroups() throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<CartridgeGroupBean>>() {
            }.getType();
            List<CartridgeGroupBean> cartridgeGroupList = (List<CartridgeGroupBean>) restClient.listEntity(ENDPOINT_LIST_CARTRIDGE_GROUPS,
                    listType, "Cartridge Groups");

            if ((cartridgeGroupList == null) || (cartridgeGroupList.size() == 0)) {
                System.out.println("No cartridge groups found");
                return;
            }

            RowMapper<CartridgeGroupBean> cartridgeGroupMapper = new RowMapper<CartridgeGroupBean>() {
                public String[] getData(CartridgeGroupBean cartridgeGroup) {
                    String[] data = new String[3];
                    data[0] = cartridgeGroup.getName();
                    data[1] = cartridgeGroup.getCartridges() == null ? "" : String.valueOf(cartridgeGroup.getCartridges().size());
                    data[2] = cartridgeGroup.getGroups() == null ? "0" : String.valueOf(cartridgeGroup.getGroups().size());
                    return data;
                }
            };
            CartridgeGroupBean[] cartridgeGroups = new CartridgeGroupBean[cartridgeGroupList.size()];
            cartridgeGroups = cartridgeGroupList.toArray(cartridgeGroups);

            System.out.println("Cartridge Groups found:");
            CliUtils.printTable(cartridgeGroups, cartridgeGroupMapper, "Name", "No. of cartridges", "No of groups");
        } catch (Exception e) {
            String message = "Error in listing cartridge groups";
            printError(message, e);
        }
    }

    /**
     * Describe a cartridge
     *
     * @param cartridgeType Type of the cartridge
     * @throws CommandException
     */
    public void describeCartridge(final String cartridgeType) throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<CartridgeBean>>() {
            }.getType();
            // GET /cartridges/{cartridgeType} not available, hence using the list method
            List<CartridgeBean> cartridgeList = (List<CartridgeBean>) restClient.listEntity(ENDPOINT_LIST_CARTRIDGES,
                    listType, "cartridges");

            CartridgeBean cartridge = null;
            for (CartridgeBean item : cartridgeList) {
                if (item.getType().equals(cartridgeType)) {
                    cartridge = item;
                    break;
                }
            }

            if (cartridge == null) {
                System.out.println("Cartridge not found");
                return;
            }

            System.out.println(getGson().toJson(cartridge));

        } catch (Exception e) {
            String message = "Error in describing cartridge: " + cartridgeType;
            printError(message, e);
        }
    }

    private ClusterBean getClusterObjectFromString(String resultString) {
        String tmp;
        if (resultString.startsWith("{\"cluster\"")) {
            tmp = resultString.substring("{\"cluster\"".length() + 1, resultString.length() - 1);
            resultString = tmp;
        }
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        return gson.fromJson(resultString, ClusterBean.class);
    }

    /**
     * Add new tenant
     *
     * @param admin     username
     * @param firstName first name
     * @param lastName  last name
     * @param password  password
     * @param domain    domain name
     * @param email     email
     * @throws CommandException
     */
    public void addTenant(String admin, String firstName, String lastName, String password, String domain, String email)
            throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            TenantInfoBean tenantInfo = new TenantInfoBean();
            tenantInfo.setAdmin(admin);
            tenantInfo.setFirstName(firstName);
            tenantInfo.setLastName(lastName);
            tenantInfo.setAdminPassword(password);
            tenantInfo.setTenantDomain(domain);
            tenantInfo.setEmail(email);

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            String jsonString = gson.toJson(tenantInfo, TenantInfoBean.class);
            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL()
                    + ENDPOINT_ADD_TENANT, jsonString);

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == 201) {
                System.out.println("Tenant added successfully: " + domain);
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                String errorMsg = gson.fromJson(resultString, ResponseMessageBean.class).getMessage();
                System.out.println(errorMsg);
            }
        } catch (Exception e) {
            String message = "Could not add tenant: " + domain;
            printError(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Describe a tenant
     *
     * @param domainName domain name
     * @throws org.apache.stratos.cli.exception.CommandException
     */
    public void describeTenant(final String domainName) throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<TenantInfoBean>>() {
            }.getType();
            List<TenantInfoBean> tenantList = (List<TenantInfoBean>) restClient.listEntity(ENDPOINT_LIST_TENANTS,
                    listType, "tenant");

            TenantInfoBean tenant = null;
            for (TenantInfoBean item : tenantList) {
                if (item.getTenantDomain().equals(domainName)) {
                    tenant = item;
                    break;
                }
            }

            if (tenant == null) {
                System.out.println("Tenant not found");
                return;
            }

            System.out.println("-------------------------------------");
            System.out.println("Tenant Information:");
            System.out.println("-------------------------------------");
            System.out.println("Tenant domain: " + tenant.getTenantDomain());
            System.out.println("ID: " + tenant.getTenantId());
            System.out.println("Email: " + tenant.getEmail());
            System.out.println("Active: " + tenant.isActive());
            System.out.println("Created date: " + new Date(tenant.getCreatedDate()));

        } catch (Exception e) {
            String message = "Error in describing tenant: " + domainName;
            printError(message, e);
        }
    }

    /**
     * List tenants by a partial domain search
     *
     * @param partialDomain Part of the domain name
     * @throws org.apache.stratos.cli.exception.CommandException
     */
    public void listTenantsByPartialDomain(String partialDomain) throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<TenantInfoBean>>() {
            }.getType();
            List<TenantInfoBean> tenantList = (List<TenantInfoBean>) restClient.listEntity(ENDPOINT_LIST_TENANTS_BY_PARTIAL_DOMAIN.replace("{tenantDomain}", partialDomain),
                    listType, "tenants");

            if ((tenantList == null) || (tenantList.size() == 0)) {
                System.out.println("No tenants found");
                return;
            }

            RowMapper<TenantInfoBean> tenantMapper = new RowMapper<TenantInfoBean>() {
                public String[] getData(TenantInfoBean tenant) {
                    String[] data = new String[5];
                    data[0] = tenant.getTenantDomain();
                    data[1] = String.valueOf(tenant.getTenantId());
                    data[2] = String.valueOf(tenant.isActive());
                    data[3] = tenant.getEmail();
                    data[4] = new Date(tenant.getCreatedDate()).toString();
                    return data;
                }
            };

            TenantInfoBean[] tenants = new TenantInfoBean[tenantList.size()];
            tenants = tenantList.toArray(tenants);

            System.out.println("Tenants found:");
            CliUtils.printTable(tenants, tenantMapper, "tenantDomain", "tenantID", "active", "email", "createdDate");
        } catch (Exception e) {
            String message = "Error in listing tenants";
            printError(message, e);
        }
    }

    /**
     * Update an existing tenant
     *
     * @param id        tenant id
     * @param admin     username
     * @param firstName first name
     * @param lastName  last name
     * @param password  password
     * @param domain    domain name
     * @param email     email
     * @throws CommandException
     */
    public void updateTenant(int id, String admin, String firstName, String lastName, String password, String domain, String email)
            throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            TenantInfoBean tenantInfo = new TenantInfoBean();
            tenantInfo.setAdmin(admin);
            tenantInfo.setFirstName(firstName);
            tenantInfo.setLastName(lastName);
            tenantInfo.setAdminPassword(password);
            tenantInfo.setTenantDomain(domain);
            tenantInfo.setEmail(email);
            tenantInfo.setTenantId(id);

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            String jsonString = gson.toJson(tenantInfo, TenantInfoBean.class);

            HttpResponse response = restClient.doPut(httpClient, restClient.getBaseURL()
                    + ENDPOINT_UPDATE_TENANT, jsonString);

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode < 200 || responseCode >= 300) {
                String resultString = CliUtils.getHttpResponseString(response);
                String errorMsg = gson.fromJson(resultString, ResponseMessageBean.class).getMessage();
                System.out.println(errorMsg);

            } else {
                System.out.println("Tenant updated successfully: " + domain);
            }
        } catch (Exception e) {
            String message = "Could not update tenant: " + domain;
            printError(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Add user
     *
     * @param userName    username
     * @param credential  password
     * @param role        user role
     * @param firstName   first name
     * @param lastName    last name
     * @param email       email
     * @param profileName profile name
     * @throws CommandException
     */
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

            String result = getHttpResponseString(response);
            System.out.println(gson.fromJson(result, ResponseMessageBean.class).getMessage());

        } catch (Exception e) {
            String message = "Could not add user: " + userName;
            printError(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Update user
     *
     * @param userName    username
     * @param credential  password
     * @param role        user role
     * @param firstName   first name
     * @param lastName    last name
     * @param email       email
     * @param profileName profile name
     * @throws CommandException
     */
    public void updateUser(String userName, String credential, String role, String firstName, String lastName, String email, String profileName)
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

            HttpResponse response = restClient.doPut(httpClient, restClient.getBaseURL()
                    + ENDPOINT_UPDATE_USER, jsonString);

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode < 200 || responseCode >= 300) {
                CliUtils.printError(response);
            } else {
                System.out.println("User updated successfully: " + userName);
            }
        } catch (Exception e) {
            String message = "Could not update user: " + userName;
            printError(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Delete tenant
     *
     * @param tenantDomain domain name of the tenant
     * @throws CommandException
     */
    public void deleteTenant(String tenantDomain) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doDelete(httpClient, restClient.getBaseURL()
                    + ENDPOINT_ADD_TENANT + "/" + tenantDomain);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have successfully deleted the tenant: " + tenantDomain);
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            String message = "Could not delete tenant: " + tenantDomain;
            printError(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Delete user
     *
     * @param userName username
     * @throws CommandException
     */
    public void deleteUser(String userName) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doDelete(httpClient, restClient.getBaseURL()
                    + ENDPOINT_ADD_USER + "/" + userName);

            String resultString = CliUtils.getHttpResponseString(response);
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            System.out.println(gson.fromJson(resultString, ResponseMessageBean.class).getMessage());
        } catch (Exception e) {
            String message = "Could not delete user: " + userName;
            printError(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Deactivate tenant
     *
     * @param tenantDomain domain name of the tenant
     * @throws CommandException
     */
    public void deactivateTenant(String tenantDomain) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doPut(httpClient, restClient.getBaseURL()
                    + ENDPOINT_DEACTIVATE_TENANT.replace("{tenantDomain}", tenantDomain), "");

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have successfully deactivated the tenant: " + tenantDomain);
                return;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                String errorMsg = gson.fromJson(resultString, ResponseMessageBean.class).getMessage();
                System.out.println(errorMsg);
            }

        } catch (Exception e) {
            String message = "Could not de-activate tenant: " + tenantDomain;
            printError(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Activate tenant
     *
     * @param tenantDomain domain name of the tenant
     * @throws CommandException
     */
    public void activateTenant(String tenantDomain) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doPut(httpClient, restClient.getBaseURL()
                    + ENDPOINT_ACTIVATE_TENANT.replace("{tenantDomain}", tenantDomain), "");

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have successfully activated the tenant: " + tenantDomain);
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                String errorMsg = gson.fromJson(resultString, ResponseMessageBean.class).getMessage();
                System.out.println(errorMsg);
            }

        } catch (Exception e) {
            String message = "Could not activate tenant: " + tenantDomain;
            printError(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * List tenants
     *
     * @throws CommandException
     */
    public void listTenants() throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<TenantInfoBean>>() {
            }.getType();
            List<TenantInfoBean> tenantInfoList = (List<TenantInfoBean>) restClient.listEntity(ENDPOINT_LIST_TENANTS,
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
                    data[4] = new Date(tenantInfo.getCreatedDate()).toString();
                    return data;
                }
            };

            TenantInfoBean[] tenantArray = new TenantInfoBean[tenantInfoList.size()];
            tenantArray = tenantInfoList.toArray(tenantArray);

            System.out.println("Tenants:");
            CliUtils.printTable(tenantArray, rowMapper, "Domain", "Tenant ID", "Email", "State", "Created Date");
        } catch (Exception e) {
            String message = "Could not list tenants";
            printError(message, e);
        }
    }

    /**
     * List all users
     *
     * @throws CommandException
     */
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
            String message = "Could not list users";
            printError(message, e);
        }
    }

    /**
     * Add cartridge
     *
     * @param cartridgeDefinition cartridge definition
     * @throws CommandException
     */
    public void addCartridge(String cartridgeDefinition) throws CommandException {
        restClient.deployEntity(ENDPOINT_DEPLOY_CARTRIDGE, cartridgeDefinition, "cartridge");
    }

    /**
     * Update cartridge
     *
     * @param cartridgeDefinition cartridge definition
     * @throws CommandException
     */
    public void updateCartridge(String cartridgeDefinition) throws CommandException {
        restClient.updateEntity(ENDPOINT_DEPLOY_CARTRIDGE, cartridgeDefinition, "cartridge");
    }

    /**
     * Undeploy cartridge
     *
     * @param cartridgeId cartridge Id
     * @throws CommandException
     */
    public void undeployCartrigdeDefinition(String cartridgeId) throws CommandException {
        restClient.undeployEntity(ENDPOINT_UNDEPLOY_CARTRIDGE, "cartridge", cartridgeId);
    }

    /**
     * Deploy autoscaling policy
     *
     * @param autoScalingPolicy autoscaling policy definition
     * @throws CommandException
     */
    public void addAutoscalingPolicy(String autoScalingPolicy) throws CommandException {
        restClient.deployEntity(ENDPOINT_DEPLOY_AUTOSCALING_POLICY, autoScalingPolicy, "autoscaling policy");
    }

    /**
     * Update autoscaling policy
     *
     * @param autoScalingPolicy autoscaling policy definition
     * @throws CommandException
     */
    public void updateAutoscalingPolicy(String autoScalingPolicy) throws CommandException {
        restClient.updateEntity(ENDPOINT_UPDATE_AUTOSCALING_POLICY, autoScalingPolicy, "autoscaling policy");
    }

    /**
     * List applications
     *
     * @throws CommandException
     */
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
                public String[] getData(ApplicationBean applicationDefinition) {
                    String[] data = new String[3];
                    data[0] = applicationDefinition.getApplicationId();
                    data[1] = applicationDefinition.getAlias();
                    data[2] = applicationDefinition.getStatus();
                    return data;
                }
            };

            ApplicationBean[] array = new ApplicationBean[list.size()];
            array = list.toArray(array);

            System.out.println("Applications found:");
            CliUtils.printTable(array, rowMapper, "Application ID", "Alias", "Status");
        } catch (Exception e) {
            String message = "Could not list applications";
            printError(message, e);
        }
    }

    /**
     * List autoscaling policies
     *
     * @throws CommandException
     */
    public void listAutoscalingPolicies() throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<AutoscalePolicyBean>>() {
            }.getType();
            List<AutoscalePolicyBean> list = (List<AutoscalePolicyBean>) restClient.listEntity(ENDPOINT_LIST_AUTOSCALING_POLICIES,
                    listType, "autoscaling policies");

            if ((list == null) || (list.size() == 0)) {
                System.out.println("No autoscaling policies found");
                return;
            }

            RowMapper<AutoscalePolicyBean> rowMapper = new RowMapper<AutoscalePolicyBean>() {

                public String[] getData(AutoscalePolicyBean policy) {
                    String[] data = new String[4];
                    data[0] = policy.getId();
                    data[1] = String.valueOf(policy.getLoadThresholds().getRequestsInFlight().getThreshold());
                    data[2] = String.valueOf(policy.getLoadThresholds().getMemoryConsumption().getThreshold());
                    data[3] = String.valueOf(policy.getLoadThresholds().getLoadAverage().getThreshold());
                    return data;
                }
            };

            AutoscalePolicyBean[] array = new AutoscalePolicyBean[list.size()];
            array = list.toArray(array);

            System.out.println("Autoscaling policies found:");
            CliUtils.printTable(array, rowMapper, "ID", "Requests In Flight Threshold", "Memory Consumption Threshold",
                    "Load Average Threshold");
        } catch (Exception e) {
            String message = "Could not list autoscaling policies";
            printError(message, e);
        }
    }

    /**
     * Describe deployment policy
     *
     * @param deploymentPolicyId deployment policy id
     * @throws CommandException
     */
    public void describeDeploymentPolicy(String deploymentPolicyId) throws CommandException {
        try {
            DeploymentPolicyBean policy = (DeploymentPolicyBean) restClient.getEntity(ENDPOINT_GET_DEPLOYMENT_POLICY,
                    DeploymentPolicyBean.class, "{deploymentPolicyId}", deploymentPolicyId, "deployment policy");

            if (policy == null) {
                System.out.println("Deployment policy not found: " + deploymentPolicyId);
                return;
            }

            System.out.println("Deployment policy: " + deploymentPolicyId);
            System.out.println(getGson().toJson(policy));
        } catch (Exception e) {
            String message = "Error in describing deployment policy: " + deploymentPolicyId;
            printError(message, e);
        }
    }

    /**
     * Describe application policy
     *
     * @param applicationPolicyId application policy id
     * @throws CommandException
     */
    public void describeApplicationPolicy(String applicationPolicyId) throws CommandException {
        try {
            ApplicationPolicyBean policy = (ApplicationPolicyBean) restClient.getEntity(ENDPOINT_GET_APPLICATION_POLICY,
                    ApplicationPolicyBean.class, "{applicationPolicyId}", applicationPolicyId, "application policy");

            if (policy == null) {
                System.out.println("Application policy not found: " + applicationPolicyId);
                return;
            }

            System.out.println("Application policy: " + applicationPolicyId);
            System.out.println(getGson().toJson(policy));
        } catch (Exception e) {
            String message = "Error in describing application policy: " + applicationPolicyId;
            printError(message, e);
        }
    }

    /**
     * Describe autoscaling policy
     *
     * @param autoscalingPolicyId application policy id
     * @throws CommandException
     */
    public void describeAutoScalingPolicy(String autoscalingPolicyId) throws CommandException {
        try {
            AutoscalePolicyBean policy = (AutoscalePolicyBean) restClient.getEntity(ENDPOINT_GET_AUTOSCALING_POLICY,
                    AutoscalePolicyBean.class, "{id}", autoscalingPolicyId, "autoscaling policy");

            if (policy == null) {
                System.out.println("Autoscaling policy not found: " + autoscalingPolicyId);
                return;
            }

            System.out.println("Autoscaling policy: " + autoscalingPolicyId);
            System.out.println(getGson().toJson(policy));
        } catch (Exception e) {
            String message = "Could not describe autoscaling policy: " + autoscalingPolicyId;
            printError(message, e);
        }
    }

    /**
     * Add Kubernetes Cluster
     *
     * @param entityBody Kubernetes Cluster definition
     * @throws CommandException
     */
    public void addKubernetesCluster(String entityBody) throws CommandException {
        restClient.deployEntity(ENDPOINT_DEPLOY_KUBERNETES_CLUSTER, entityBody, "kubernetes cluster");
    }

    /**
     * List Kubernetes Clusters
     *
     * @throws CommandException
     */
    public void listKubernetesClusters() throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<KubernetesClusterBean>>() {
            }.getType();
            List<KubernetesClusterBean> list = (List<KubernetesClusterBean>) restClient.
                    listEntity(ENDPOINT_LIST_KUBERNETES_CLUSTERS, listType, "kubernetes cluster");
            if ((list != null) && (list.size() > 0)) {
                RowMapper<KubernetesClusterBean> partitionMapper = new RowMapper<KubernetesClusterBean>() {
                    public String[] getData(KubernetesClusterBean kubernetesCluster) {
                        String[] data = new String[2];
                        data[0] = kubernetesCluster.getClusterId();
                        data[1] = kubernetesCluster.getDescription();
                        return data;
                    }
                };

                KubernetesClusterBean[] array = new KubernetesClusterBean[list.size()];
                array = list.toArray(array);
                System.out.println("Kubernetes clusters found:");
                CliUtils.printTable(array, partitionMapper, "Group ID", "Description");
            } else {
                System.out.println("No Kubernetes clusters found");
            }
        } catch (Exception e) {
            String message = "Could not list Kubernetes clusters";
            printError(message, e);
        }
    }

    /**
     * Undeploy Kubernetes Cluster
     *
     * @param clusterId cluster id
     * @throws CommandException
     */
    public void undeployKubernetesCluster(String clusterId) throws CommandException {
        restClient.undeployEntity(ENDPOINT_UNDEPLOY_KUBERNETES_CLUSTER, "kubernetes cluster", clusterId);
    }

    /**
     * Add Kubernetes Host
     *
     * @param entityBody kubernetes host definition
     * @param clusterId  cluster id
     * @throws CommandException
     */
    public void addKubernetesHost(String entityBody, String clusterId) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL()
                    + ENDPOINT_DEPLOY_KUBERNETES_HOST.replace("{kubernetesClusterId}", clusterId), entityBody);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK) || responseCode.equals(CliConstants.RESPONSE_CREATED)) {
                System.out.println("You have successfully deployed host to Kubernetes cluster: " + clusterId);
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }

        } catch (Exception e) {
            String message = "Could not add host to Kubernetes cluster: " + clusterId;
            printError(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * List Kubernetes Hosts
     *
     * @param clusterId cluster id
     * @throws CommandException
     */
    public void listKubernetesHosts(String clusterId) throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<KubernetesHostBean>>() {
            }.getType();
            List<KubernetesHostBean> list = (List<KubernetesHostBean>) restClient.listEntity(ENDPOINT_LIST_KUBERNETES_HOSTS.replace("{kubernetesClusterId}", clusterId),
                    listType, "kubernetes host");
            if ((list != null) && (list.size() > 0)) {
                RowMapper<KubernetesHostBean> partitionMapper = new RowMapper<KubernetesHostBean>() {
                    public String[] getData(KubernetesHostBean kubernetesHost) {
                        String[] data = new String[4];
                        data[0] = kubernetesHost.getHostId();
                        data[1] = kubernetesHost.getHostname();
                        data[2] = emptyStringIfNullOrEmpty(kubernetesHost.getPrivateIPAddress());
                        data[3] = emptyStringIfNullOrEmpty(kubernetesHost.getPublicIPAddress());
                        return data;
                    }
                };

                KubernetesHostBean[] array = new KubernetesHostBean[list.size()];
                array = list.toArray(array);
                System.out.println("Kubernetes hosts found:");
                CliUtils.printTable(array, partitionMapper, "Host ID", "Hostname", "Private IP Address",
                        "Public IP Address");
            } else {
                System.out.println("No kubernetes hosts found");
            }
        } catch (Exception e) {
            String message = "Could not list kubernetes hosts";
            printError(message, e);
        }
    }

    private String emptyStringIfNullOrEmpty(String value) {
        return StringUtils.isBlank(value) ? "" : value;
    }

    /**
     * Get the master of a Kubernetes Cluster
     *
     * @param clusterId cluster id
     * @throws CommandException
     */
    public void getKubernetesMaster(String clusterId) throws CommandException {
        try {
            Type listType = new TypeToken<KubernetesMasterBean>() {
            }.getType();
            KubernetesMasterBean master = (KubernetesMasterBean) restClient
                    .getEntity(ENDPOINT_GET_KUBERNETES_MASTER, KubernetesMasterBean.class, "{kubernetesClusterId}", clusterId,
                            "network partition");

            if (master == null) {
                System.out.println("Kubernetes master not found in: " + clusterId);
                return;
            }

            System.out.println("Cluster: " + clusterId);
            System.out.println(getGson().toJson(master));
        } catch (Exception e) {
            String message = "Could not get the master of " + clusterId;
            printError(message, e);
        }
    }

    /**
     * Describe a Kubernetes cluster
     *
     * @param clusterId cluster id
     * @throws CommandException
     */
    public void describeKubernetesCluster(String clusterId) throws CommandException {
        try {
            Type listType = new TypeToken<KubernetesClusterBean>() {
            }.getType();
            KubernetesClusterBean cluster = (KubernetesClusterBean) restClient
                    .getEntity(ENDPOINT_GET_KUBERNETES_HOST_CLUSTER, KubernetesClusterBean.class, "{kubernetesClusterId}", clusterId,
                            "kubernetes cluster");

            if (cluster == null) {
                System.out.println("Kubernetes cluster not found: " + clusterId);
                return;
            }

            System.out.println("Kubernetes cluster: " + clusterId);
            System.out.println(getGson().toJson(cluster));
        } catch (Exception e) {
            String message = "Could not describe Kubernetes cluster: " + clusterId;
            printError(message, e);
        }
    }

    /**
     * Add Domain mappings
     *
     * @param applicationId       application id
     * @param resourceFileContent domain mapping definition
     * @throws CommandException
     */
    public void addDomainMappings(String applicationId, String resourceFileContent) throws CommandException {
        String endpoint = ENDPOINT_DOMAIN_MAPPINGS.replace("{applicationId}", applicationId);
        restClient.deployEntity(endpoint, resourceFileContent, "domain mappings");
    }

    /**
     * List domain mappings
     *
     * @param applicationId application id
     * @throws CommandException
     */
    public void listDomainMappings(String applicationId) throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<DomainMappingBean>>() {
            }.getType();
            List<DomainMappingBean> list = (List<DomainMappingBean>) restClient.listEntity(
                    ENDPOINT_DOMAIN_MAPPINGS.replace("{applicationId}", applicationId),
                    listType, "domain mappings");
            if (list == null) {
                System.out.println("No domain mappings found in application: " + applicationId);
                return;
            }

            RowMapper<DomainMappingBean> rowMapper = new RowMapper<DomainMappingBean>() {
                public String[] getData(DomainMappingBean domainMappingBean) {
                    String[] data = new String[2];
                    data[0] = domainMappingBean.getDomainName();
                    data[1] = domainMappingBean.getContextPath();
                    return data;
                }
            };

            DomainMappingBean[] array = new DomainMappingBean[list.size()];
            array = list.toArray(array);
            System.out.println("Domain mappings found in application: " + applicationId);
            CliUtils.printTable(array, rowMapper, "Domain Name", "Context Path");
        } catch (Exception e) {
            String message = "Could not list domain mappings in application: " + applicationId;
            printError(message, e);
        }
    }

    /**
     * Remove Domain Mappings
     *
     * @param applicationId application id
     * @throws CommandException
     */
    public void removeDomainMappings(String applicationId, String domainName) throws CommandException {
        String endpoint = (ENDPOINT_REMOVE_DOMAIN_MAPPINGS.replace("{applicationId}",
                applicationId)).replace("{domainName}", domainName);
        restClient.undeployEntity(endpoint, "domain mappings", applicationId);
    }

    /**
     * Undeploy Kubernetes Host
     *
     * @param clusterId cluster id
     * @param hostId    host id
     * @throws CommandException
     */
    public void undeployKubernetesHost(String clusterId, String hostId) throws CommandException {
        restClient.undeployEntity(ENDPOINT_UNDEPLOY_KUBERNETES_HOST.replace("{kubernetesClusterId}", clusterId), "kubernetes host", hostId);
    }

    /**
     * Update Kubernetes Master
     *
     * @param entityBody Kubernetes master definition
     * @param clusterId  cluster id
     * @throws CommandException
     */
    public void updateKubernetesMaster(String entityBody, String clusterId) throws CommandException {
        restClient.updateEntity(ENDPOINT_UPDATE_KUBERNETES_MASTER.replace("{kubernetesClusterId}", clusterId), entityBody, "kubernetes master");
    }

    /**
     * Update Kubernetes Host
     *
     * @param entityBody Kubernetes host definition
     * @throws CommandException
     */
    public void updateKubernetesHost(String entityBody) throws CommandException {
        restClient.updateEntity(ENDPOINT_UPDATE_KUBERNETES_HOST, entityBody, "kubernetes host");
    }

    /**
     * Synchronize artifacts
     *
     * @param cartridgeAlias alias of the cartridge
     * @throws CommandException
     */
    public void synchronizeArtifacts(String cartridgeAlias) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL() + ENDPOINT_SYNCHRONIZE_ARTIFACTS.replace("{subscriptionAlias}", cartridgeAlias), cartridgeAlias);

            String responseCode = "" + response.getStatusLine().getStatusCode();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println(String.format("Synchronizing artifacts for cartridge subscription alias: %s", cartridgeAlias));
            } else {
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();
                String resultString = CliUtils.getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
            }
        } catch (Exception e) {
            String message = "Could not synchronize artifacts for cartridge subscription alias: " + cartridgeAlias;
            printError(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Add cartridge group
     *
     * @param entityBody cartridge group definition
     * @throws CommandException
     */
    public void addCartridgeGroup(String entityBody) throws CommandException {
        restClient.deployEntity(ENDPOINT_DEPLOY_SERVICE_GROUP, entityBody, "cartridge group");
    }

    /**
     * Undeploy Cartridge group
     *
     * @param groupDefinitionName cartridge group name
     * @throws CommandException
     */
    public void undeployServiceGroup(String groupDefinitionName) throws CommandException {
        restClient.undeployEntity(ENDPOINT_UNDEPLOY_SERVICE_GROUP, "cartridge group", groupDefinitionName);
    }

    /**
     * Describe service group
     *
     * @param groupDefinitionName cartridge group name
     * @throws CommandException
     */
    public void describeServiceGroup(String groupDefinitionName) throws CommandException {
        try {
            CartridgeGroupBean bean = (CartridgeGroupBean) restClient.listEntity(ENDPOINT_LIST_SERVICE_GROUP.replace("{groupDefinitionName}", groupDefinitionName),
                    CartridgeGroupBean.class, "serviceGroup");

            if (bean == null) {
                System.out.println("Cartridge group not found: " + groupDefinitionName);
                return;
            }

            System.out.println("Service Group : " + groupDefinitionName);
            System.out.println(getGson().toJson(bean));
        } catch (Exception e) {
            String message = "Could not describe cartridge group: " + groupDefinitionName;
            printError(message, e);
        }
    }

    /**
     * Add application
     *
     * @param entityBody application definition
     * @throws CommandException
     */
    public void addApplication(String entityBody) throws CommandException {
        restClient.deployEntity(ENDPOINT_ADD_APPLICATION, entityBody, "application");
    }

    /**
     * Deploy application
     *
     * @param applicationId       application id
     * @param applicationPolicyId application policy id
     * @throws CommandException
     */
    public void deployApplication(String applicationId, String applicationPolicyId) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            String url = (ENDPOINT_DEPLOY_APPLICATION.replace("{applicationId}", applicationId)).replace("{applicationPolicyId}", applicationPolicyId);
            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL()
                    + url, "");

            String result = getHttpResponseString(response);

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            System.out.println(gson.fromJson(result, ResponseMessageBean.class).getMessage());

        } catch (Exception e) {
            String message = "Could not deploy application: " + applicationId;
            printError(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }

        //restClient.deployEntity((ENDPOINT_DEPLOY_APPLICATION.replace("{applicationId}", applicationId)).replace("{applicationPolicyId",applicationPolicyId),"application");
    }

    /**
     * Undeploy application
     *
     * @param applicationId application id
     * @throws CommandException
     */
    public void undeployApplication(String applicationId) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClient.doPost(httpClient, restClient.getBaseURL()
                    + ENDPOINT_UNDEPLOY_APPLICATION.replace("{id}", applicationId), "");
            String result = getHttpResponseString(response);

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            System.out.println(gson.fromJson(result, ResponseMessageBean.class).getMessage());
        } catch (Exception e) {
            String message = "Could not undeploy application: " + applicationId;
            printError(message, e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Remove application
     *
     * @param applicationId application id
     * @throws CommandException This method helps to remove applications
     */
    public void deleteApplication(String applicationId) throws CommandException {
        restClient.deleteEntity(ENDPOINT_REMOVE_APPLICATION.replace("{appId}", applicationId), applicationId,
                "application");
    }

    /**
     * Update application
     *
     * @param entityBody application definition
     * @throws CommandException
     */
    public void updateApplication(String entityBody) throws CommandException {
        restClient.updateEntity(ENDPOINT_UPDATE_APPLICATION, entityBody, "application");
    }

    /**
     * Delete autoscaling policy
     *
     * @param autoscalingPolicyId autoscaling policy id
     * @throws CommandException
     */
    public void deleteAutoSclaingPolicy(String autoscalingPolicyId) throws CommandException {
        restClient.deleteEntity(ENDPOINT_REMOVE_AUTOSCALINGPOLICY.replace("{policyId}", autoscalingPolicyId), autoscalingPolicyId,
                "Auto-scaling policy");
    }

    /**
     * Describe application
     *
     * @param applicationId application id
     * @throws CommandException
     */
    public void describeApplication(String applicationId) throws CommandException {
        try {
            ApplicationBean application = (ApplicationBean) restClient
                    .getEntity(ENDPOINT_GET_APPLICATION, ApplicationBean.class, "{appId}", applicationId,
                            "application");

            if (application == null) {
                System.out.println("Application not found: " + applicationId);
                return;
            }

            System.out.println("Application: " + applicationId);
            System.out.println(getGson().toJson(application));
        } catch (Exception e) {
            String message = "Could not describe application: " + applicationId;
            printError(message, e);
        }
    }

    /**
     * Describe application runtime
     *
     * @param applicationId application id
     * @throws CommandException
     */
    public void describeApplicationRuntime(String applicationId) throws CommandException {
        try {
            ApplicationBean application = (ApplicationBean) restClient
                    .getEntity(ENDPOINT_GET_APPLICATION_RUNTIME, ApplicationBean.class, "{applicationId}", applicationId,
                            "application");

            if (application == null) {
                System.out.println("Application not found: " + applicationId);
                return;
            }

            System.out.println("Application: " + applicationId);
            System.out.println(getGson().toJson(application));
        } catch (Exception e) {
            String message = "Could not describe application runtime: " + applicationId;
            printError(message, e);
        }
    }

    /**
     * Add application signup
     *
     * @param applicationId application id
     * @param entityBody    application signup definition
     * @throws CommandException
     */
    public void addApplicationSignup(String entityBody, String applicationId) throws CommandException {
        restClient.deployEntity(ENDPOINT_APPLICATION_SIGNUP.replace("{applicationId}", applicationId), entityBody, "application signup");
    }

    /**
     * Describe application signup
     *
     * @param applicationId application id
     * @throws CommandException
     */
    public void describeApplicationSignup(String applicationId) throws CommandException {
        try {
            ApplicationSignUpBean bean = (ApplicationSignUpBean) restClient.listEntity(ENDPOINT_APPLICATION_SIGNUP.replace("{applicationId}", applicationId),
                    ApplicationSignUpBean.class, "applicationSignup");

            if (bean == null) {
                System.out.println("Application sign up not found for application: " + applicationId);
                return;
            }

            System.out.println("Application signup for application : " + applicationId);
            System.out.println(getGson().toJson(bean));
        } catch (Exception e) {
            String message = "Could not describe application signup for application: " + applicationId;
            printError(message, e);
        }
    }

    /**
     * Delete application signup
     *
     * @param applicationId application id
     * @throws CommandException
     */
    public void deleteApplicationSignup(String applicationId) throws CommandException {
        restClient.deleteEntity(ENDPOINT_APPLICATION_SIGNUP.replace("{applicationId}", applicationId), applicationId,
                "application signup");
    }

    /**
     * Handle exception
     *
     * @throws CommandException
     */
    private void handleException(String key, Exception e, Object... args) throws CommandException {
        if (log.isDebugEnabled()) {
            log.debug("Displaying message for {}. Exception thrown is {}", key, e.getClass());
        }

        String message = CliUtils.getMessage(key, args);
        log.error(message);
        System.out.println(message);
        throw new CommandException(message, e);
    }

    /**
     * Print error on console and log
     *
     * @param message message
     * @param e       exception
     */
    private void printError(String message, Throwable e) {
        // CLI console only get system output
        System.out.println(message);
        // Log error
        log.error(message, e);
    }

    /**
     * Add network partitions
     *
     * @param networkPartitionDefinition network partition definition
     * @throws CommandException
     */
    public void addNetworkPartition(String networkPartitionDefinition) throws CommandException {
        restClient.deployEntity(ENDPOINT_DEPLOY_NETWORK_PARTITION, networkPartitionDefinition, "network partition");
    }

    /**
     * Remove network partition
     *
     * @param networkPartitionId application id
     * @throws CommandException
     */
    public void removeNetworkPartition(String networkPartitionId) throws CommandException {
        restClient.deleteEntity(ENDPOINT_REMOVE_NETWORK_PARTITION.replace("{id}", networkPartitionId), networkPartitionId,
                "network-partition");
    }

    /**
     * List network partitions
     *
     * @throws CommandException
     */
    public void listNetworkPartitions() throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<NetworkPartitionBean>>() {
            }.getType();
            List<NetworkPartitionBean> networkPartitionsList = (List<NetworkPartitionBean>) restClient.listEntity(ENDPOINT_LIST_NETWORK_PARTITIONS,
                    listType, "network-partitions");

            if ((networkPartitionsList == null) || (networkPartitionsList.size() == 0)) {
                System.out.println("No network partitions found");
                return;
            }

            RowMapper<NetworkPartitionBean> networkPartitionMapper = new RowMapper<NetworkPartitionBean>() {
                public String[] getData(NetworkPartitionBean partition) {
                    String[] data = new String[2];
                    data[0] = partition.getId();
                    data[1] = String.valueOf(partition.getPartitions().size());
                    return data;
                }
            };

            NetworkPartitionBean[] partitions = new NetworkPartitionBean[networkPartitionsList.size()];
            partitions = networkPartitionsList.toArray(partitions);

            System.out.println("Network partitions found:");
            CliUtils.printTable(partitions, networkPartitionMapper, "Network Partition ID", "Number of Partitions");
        } catch (Exception e) {
            String message = "Error in listing network partitions";
            printError(message, e);
        }
    }

    /**
     * Update network partitions
     *
     * @param networkPartitionDefinition network partition definition
     * @throws CommandException
     */
    public void updateNetworkPartition(String networkPartitionDefinition) throws CommandException {
        restClient.updateEntity(ENDPOINT_DEPLOY_NETWORK_PARTITION, networkPartitionDefinition, "network-partition");
    }

    /**
     * Describe a network partition
     *
     * @param partitionId partition id
     * @throws CommandException
     */
    public void describeNetworkPartition(final String partitionId) throws CommandException {
        try {
            NetworkPartitionBean partition = (NetworkPartitionBean) restClient
                    .getEntity(ENDPOINT_GET_NETWORK_PARTITION, NetworkPartitionBean.class, "{networkPartitionId}", partitionId,
                            "network partition");

            if (partition == null) {
                System.out.println("Network partition not found: " + partitionId);
                return;
            }

            System.out.println("Partition: " + partitionId);
            System.out.println(getGson().toJson(partition));
        } catch (Exception e) {
            String message = "Could not describe partition: " + partitionId;
            printError(message, e);
        }
    }

    /**
     * Deploy deployment policy
     *
     * @param deploymentPolicy deployment policy definition
     * @throws CommandException
     */
    public void addDeploymentPolicy(String deploymentPolicy) throws CommandException {
        restClient.deployEntity(ENDPOINT_DEPLOY_DEPLOYMENT_POLICY, deploymentPolicy, "deployment policy");
    }

    /**
     * Deploy application policy
     *
     * @param applicationPolicy application policy definition
     * @throws CommandException
     */
    public void addApplicationPolicy(String applicationPolicy) throws CommandException {
        restClient.deployEntity(ENDPOINT_DEPLOY_APPLICATION_POLICY, applicationPolicy, "application policy");
    }

    /**
     * Update deployment policy
     *
     * @param deploymentPolicy deployment policy definition
     * @throws CommandException
     */
    public void updateDeploymentPolicy(String deploymentPolicy) throws CommandException {
        restClient.updateEntity(ENDPOINT_UPDATE_DEPLOYMENT_POLICY, deploymentPolicy, "deployment policy");
    }

    /**
     * Delete deployment policy
     *
     * @param deploymentPolicyId deployment policy definition
     * @throws CommandException
     */
    public void deleteDeploymentPolicy(String deploymentPolicyId) throws CommandException {
        restClient.deleteEntity(ENDPOINT_REMOVE_DEPLOYMENT_POLICY.replace("{policyId}", deploymentPolicyId), deploymentPolicyId,
                "deployment policy");
    }

    /**
     * List deployment policies
     *
     * @throws CommandException
     */
    public void listDeploymentPolicies() throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<DeploymentPolicyBean>>() {
            }.getType();
            List<DeploymentPolicyBean> list = (List<DeploymentPolicyBean>) restClient.listEntity(ENDPOINT_LIST_DEPLOYMENT_POLICIES,
                    listType, "deployment policies");

            if ((list == null) || (list.size() == 0)) {
                System.out.println("No deployment policies found");
                return;
            }

            RowMapper<DeploymentPolicyBean> rowMapper = new RowMapper<DeploymentPolicyBean>() {

                public String[] getData(DeploymentPolicyBean policy) {
                    String[] data = new String[2];
                    data[0] = policy.getId();
                    data[1] = String.valueOf(policy.getNetworkPartitions().size());
                    return data;
                }
            };

            DeploymentPolicyBean[] array = new DeploymentPolicyBean[list.size()];
            array = list.toArray(array);

            System.out.println("Deployment policies found:");
            CliUtils.printTable(array, rowMapper, "ID", "Accessibility");
        } catch (Exception e) {
            String message = "Could not list deployment policies";
            printError(message, e);
        }
    }

    /**
     * List application policies
     *
     * @throws CommandException
     */
    public void listApplicationPolicies() throws CommandException {
        try {
            Type listType = new TypeToken<ArrayList<ApplicationPolicyBean>>() {
            }.getType();
            List<ApplicationPolicyBean> list = (List<ApplicationPolicyBean>) restClient.listEntity(ENDPOINT_LIST_APPLICATION_POLICIES,
                    listType, "application policies");

            if ((list == null) || (list.size() == 0)) {
                System.out.println("No application policies found");
                return;
            }

            RowMapper<ApplicationPolicyBean> rowMapper = new RowMapper<ApplicationPolicyBean>() {

                public String[] getData(ApplicationPolicyBean policy) {
                    String[] data = new String[3];
                    data[0] = policy.getId();
                    data[1] = String.valueOf(policy.getNetworkPartitions().length);
                    data[2] = policy.getAlgorithm();
                    return data;
                }
            };

            ApplicationPolicyBean[] array = new ApplicationPolicyBean[list.size()];
            array = list.toArray(array);

            System.out.println("Application policies found:");
            CliUtils.printTable(array, rowMapper, "ID", "No of network partitions", "algorithm");
        } catch (Exception e) {
            String message = "Could not list application policies";
            printError(message, e);
        }
    }

    /**
     * Delete application policy
     * param applicationPolicyId application policy id
     *
     * @throws CommandException
     */
    public void deleteApplicationPolicy(String applicationPolicyId) throws CommandException {
        restClient.deleteEntity(ENDPOINT_REMOVE_APPLICATION_POLICY.replace("{policyId}", applicationPolicyId), applicationPolicyId,
                "application policy");
    }

    /**
     * Update application policy
     *
     * @param applicationPolicy application policy definition
     * @throws CommandException
     */
    public void updateApplicationPolicy(String applicationPolicy) throws CommandException {
        restClient.updateEntity(ENDPOINT_UPDATE_APPLICATION_POLICY, applicationPolicy, "application policy");
    }


}
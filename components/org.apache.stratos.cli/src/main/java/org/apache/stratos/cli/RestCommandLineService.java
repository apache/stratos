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
import org.apache.stratos.cli.beans.SubscriptionInfo;
import org.apache.stratos.cli.beans.TenantInfoBean;
import org.apache.stratos.cli.beans.autoscaler.partition.Partition;
import org.apache.stratos.cli.beans.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.cli.beans.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.cli.beans.cartridge.Cartridge;
import org.apache.stratos.cli.beans.cartridge.CartridgeInfoBean;
import org.apache.stratos.cli.beans.cartridge.PortMapping;
import org.apache.stratos.cli.beans.cartridge.ServiceDefinitionBean;
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
import java.util.*;

public class RestCommandLineService {

	private static final Logger logger = LoggerFactory.getLogger(RestCommandLineService.class);

    private RestClient restClientService;

    // REST endpoints
    private final String initializeEndpoint = "/stratos/admin/init";
    private final String initializeCookieEndpoint = "/stratos/admin/cookie";
    private final String listAvailableCartridgesRestEndpoint = "/stratos/admin/cartridge/available/list";
    private final String listSubscribedCartridgesRestEndpoint = "/stratos/admin/cartridge/list/subscribed";
    private final String listSubscribedCartridgeInfoRestEndpoint = "/stratos/admin/cartridge/info/";
    private final String listClusterRestEndpoint = "/stratos/admin/cluster/";
    private final String subscribCartridgeRestEndpoint = "/stratos/admin/cartridge/subscribe";
    private final String addTenantEndPoint = "/stratos/admin/tenant";
    private final String unsubscribeTenantEndPoint = "/stratos/admin/cartridge/unsubscribe";
    private final String cartridgeDeploymentEndPoint = "/stratos/admin/cartridge/definition";
    private final String partitionDeploymentEndPoint = "/stratos/admin/policy/deployment/partition";
    private final String autoscalingPolicyDeploymentEndPoint = "/stratos/admin/policy/autoscale";
    private final String deploymentPolicyDeploymentEndPoint = "/stratos/admin/policy/deployment";
    private final String listParitionRestEndPoint = "/stratos/admin/partition";
    private final String listAutoscalePolicyRestEndPoint = "/stratos/admin/policy/autoscale";
    private final String listDeploymentPolicyRestEndPoint = "/stratos/admin/policy/deployment";
    private final String deployServiceEndPoint = "/stratos/admin/service/definition";
    private final String listDeployServicesRestEndPoint = "/stratos/admin/service";
    private final String deactivateTenantRestEndPoint = "/stratos/admin/tenant/deactivate";
    private final String activateTenantRestEndPoint = "/stratos/admin/tenant/activate";
    private final String listAllTenantRestEndPoint = "/stratos/admin/tenant/list";

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
                HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl() + initializeCookieEndpoint,
                        restClientService.getUsername(), restClientService.getPassword());

                if (response != null) {
                    String responseCode = "" + response.getStatusLine().getStatusCode();
                    if ( (responseCode.equals(CliConstants.RESPONSE_OK)) && (response.toString().contains("WWW-Authenticate: Basic"))) {
                        return true;
                    }
                    else {
                        System.out.println("Invalid STRATOS_URL");
                        return false;
                    }
                }

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
            String resultString = getHttpResponseString(response);
            if (resultString == null) {
            	return;
            }
            
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            
            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
            	ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
            	System.out.println(exception);
                return;
            }

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
                    String[] data = new String[5];
                    data[0] = cartridge.getCartridgeType();
                    data[1] = cartridge.getDisplayName();
                    data[2] = cartridge.getDescription();
                    data[3] = cartridge.getVersion();
                    data[4] = String.valueOf(cartridge.isMultiTenant());

                    return data;
                }
            };

            if (multiTenetCartridge.size() == 0) {
            	String message = "Cannot find any deployed multi-tenant Cartridge. "
            			+ "Please deploy a Cartridge using [" + CliConstants.CARTRIDGE_DEPLOYMENT + "] command.";
                if (logger.isDebugEnabled()) {
                    logger.debug(message);
                }
                System.out.println(message);
            }
            else {
                Cartridge[] cartridges = new Cartridge[multiTelentCartridgeList.getCartridge().size()];
                cartridges = multiTelentCartridgeList.getCartridge().toArray(cartridges);

                System.out.println("Available Multi-Tenant Cartridges:");
                CommandLineUtils.printTable(cartridges, cartridgeMapper, "Type", "Name", "Description", "Version", "Multitenanted");
                System.out.println();
            }

            if (singleTentCartridge.size() == 0) {
            	String message = "Cannot find any deployed single-tenant Cartridge. "
            			+ "Please deploy a Cartridge using [" + CliConstants.CARTRIDGE_DEPLOYMENT + "] command.";
                if (logger.isDebugEnabled()) {
                    logger.debug(message);
                }
                System.out.println(message);
            }
            else {
                Cartridge[] cartridges1 = new Cartridge[singleTeneCartridgetList.getCartridge().size()];
                cartridges1 = singleTeneCartridgetList.getCartridge().toArray(cartridges1   );

                System.out.println("Available Single-Tenant Cartridges:");
                CommandLineUtils.printTable(cartridges1, cartridgeMapper, "Type", "Name", "Description", "Version", "Multitenanted");
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
            String resultString = getHttpResponseString(response);
            if (resultString == null) {
            	return;
            }
            
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            
            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
            	ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
            	System.out.println(exception);
                return;
            }

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
            System.out.println("Cannot find a matching Cartridge for [type] "+type);
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
            String resultString = getHttpResponseString(response);
            
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            
            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
            	ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
            	System.out.println(exception);
                return;
            }

            CartridgeList cartridgeList = gson.fromJson(resultString, CartridgeList.class);

            if (cartridgeList == null) {
                System.out.println("Subscribe cartridge list is null");
                return;
            }
            
            CartridgeList applicationCartridgeList = new CartridgeList();
            
            // Filter out LB cartridges
            List<Cartridge> allCartridges = cartridgeList.getCartridge();
            for (Cartridge cartridge : allCartridges) {
				if( ! cartridge.isLoadBalancer()) {
					applicationCartridgeList.getCartridge().add(cartridge);
				}
			}

            Cartridge[] cartridges = new Cartridge[applicationCartridgeList.getCartridge().size()];
            cartridges = applicationCartridgeList.getCartridge().toArray(cartridges);

            if (cartridges.length == 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No subscribed cartridges found");
                }
                System.out.println("There are no subscribed cartridges");
                return;
            }

            RowMapper<Cartridge> cartridgeMapper = new RowMapper<Cartridge>() {

                public String[] getData(Cartridge cartridge) {
                    String[] data = full ? new String[10] : new String[8];
                    data[0] = cartridge.getCartridgeType();
                    data[1] = cartridge.getDisplayName();
                    data[2] = cartridge.getVersion();
                    data[3] = cartridge.isMultiTenant() ? "Multi-Tenant" : "Single-Tenant";
                    data[4] = cartridge.getCartridgeAlias();
                    data[5] = cartridge.getStatus();
                    data[6] = cartridge.isMultiTenant() ? "N/A" : String.valueOf(cartridge.getActiveInstances());
                    data[7] = cartridge.getHostName();
                    if (full) {
                        data[8] = getAccessURLs(cartridge);
                        data[9] = cartridge.getRepoURL() != null ? cartridge.getRepoURL() : "";
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
            //headers.add("LB Cluster ID");
            headers.add("Host Name");
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

    // Lists subscribed cartridge info (from alias)
    public void listSubscribedCartridgeInfo(String alias) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl() + listSubscribedCartridgeInfoRestEndpoint
            		+alias, restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();
            String resultString = getHttpResponseString(response);

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            
            if ( !responseCode.equals(CliConstants.RESPONSE_OK)) {
            	ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
            	System.out.println(exception);
                return;
            }

            CartridgeWrapper cartridgeWrapper = gson.fromJson(resultString, CartridgeWrapper.class);
            Cartridge cartridge = cartridgeWrapper.getCartridge();

            if (cartridge == null) {
                System.out.println("Cartridge is null");
                return;
            }

            // Get LB IP s
            Map<String, Set<String>> lbIpMap = getLbIpList(cartridge, httpClient);
            final Set<String> lbPrivateIpSet = lbIpMap.get("private");
            final Set<String> lbFloatingIpSet = lbIpMap.get("floating");
            Cartridge[] cartridges = new Cartridge[1];
            cartridges[0] = cartridge;
          
                        
            System.out.println("\nSubscribed Cartridges Info :");
            System.out.println("\tType : " + cartridge.getCartridgeType());
            System.out.println("\tName : "	+ cartridge.getDisplayName());
            System.out.println("\tVersion : "	+ cartridge.getVersion());
            String tenancy  = cartridge.isMultiTenant() ? "Multi-Tenant" : "Single-Tenant";
            System.out.println("\tTenancy Model	: "	+ tenancy);
            System.out.println("\tAlias : "	+ cartridge.getCartridgeAlias());
            System.out.println("\tStatus : "	+ cartridge.getStatus());
            String instanceCount  = cartridge.isMultiTenant() ? "N/A" : String.valueOf(cartridge.getActiveInstances());
            System.out.println("\tRunning Instances	: " + instanceCount);
            System.out.println("\tAccess URL(s) : " + getAccessURLs(cartridge));
			if (cartridge.getRepoURL() != null) {
				System.out.println("\tRepo URL : " + cartridge.getRepoURL());
			}
			System.out.println("\tLB Private ip	: "	+ lbPrivateIpSet.toString());
			if (lbFloatingIpSet != null) {
				System.out.println("\tLB Floating Ip : " +  lbFloatingIpSet.toString());
			}
			if (cartridge.getProvider().equals("data")) {
				System.out.println("\tDB-username : " +cartridge.getDbUserName());
				System.out.println("\tDB-password : "	+cartridge.getPassword());
				System.out.println("\tDB-HostIP (private)  : "	+cartridge.getIp());
				if (cartridge.getPublicIp() != null) {
					System.out.println("\tDB-HostIP (floating) : "
							+ cartridge.getPublicIp());
				}
			}
            System.out.println();
        } catch (Exception e) {
            handleException("Exception in listing subscribe cartridges", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
    
    private Map<String, Set<String>> getLbIpList(Cartridge cartridge, DefaultHttpClient httpClient) throws Exception{
    	try {
            Map<String, Set<String>> privateFloatingLBIPMap = new HashMap<String, Set<String>>();
            Set<String> lbFloatingIpSet = new HashSet<String>();
            Set<String> lbPrivateIpSet = new HashSet<String>();
            Member[] members = getMembers(cartridge.getCartridgeType(), cartridge.getCartridgeAlias(), httpClient);

            Set<String> lbClusterIdSet = new HashSet<String>();

            for (Member member : members) {
                lbClusterIdSet.add(member.getLbClusterId());
                cartridge.setIp(member.getMemberIp());
                cartridge.setPublicIp(member.getMemberPublicIp());
            }

            // Invoke  cluster/{clusterId}
            for (String clusterId : lbClusterIdSet) {
                HttpResponse responseCluster = restClientService.doGet(httpClient, restClientService.getUrl() + listClusterRestEndpoint
                        +"clusterId/"+ clusterId, restClientService.getUsername(), restClientService.getPassword());

                String responseCode = "" + responseCluster.getStatusLine().getStatusCode();
                String resultStringCluster = getHttpResponseString(responseCluster);

                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();

                if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                    ExceptionMapper exception = gson.fromJson(resultStringCluster, ExceptionMapper.class);
                    System.out.println(exception);
                    return null;
                }

                Cluster cluster = getClusterObjectFromString(resultStringCluster);

                if (cluster == null) {
                    System.out.println("Subscribe cartridge list is null");
                    return null;
                }

                Member[] lbMembers = new Member[cluster.getMember().size()];
                lbMembers = cluster.getMember().toArray(lbMembers);

                for (Member lbMember : lbMembers) {
                    lbPrivateIpSet.add(lbMember.getMemberIp());
                    lbFloatingIpSet.add(lbMember.getMemberPublicIp());
                }

            }
            privateFloatingLBIPMap.put("private", lbPrivateIpSet);
            privateFloatingLBIPMap.put("floating", lbFloatingIpSet);

            return privateFloatingLBIPMap;
        } catch (Exception e) {
            handleException("Exception in get LB ip list", e);
            return null;
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
                if (logger.isDebugEnabled()) {
                    logger.debug("No Members found");
                }
                System.out.println("No members found for the corresponding cluster for type " + cartridgeType
                        + ", alias " + alias);
                return;
            }

            System.out.println("\nList of members in the [cluster]: " + alias);
            for (Member member : members) {
            	System.out.println("\n\tServiceName : "+member.getServiceName());
            	System.out.println("\tClusterId : "+member.getClusterId());
            	System.out.println("\tNewtworkPartitionId : "+member.getNetworkPartitionId());
            	System.out.println("\tPartitionId : "+member.getPartitionId());
            	System.out.println("\tStatus : "+member.getStatus());
            	if(member.getLbClusterId() != null) {
            	System.out.println("\tLBCluster : "+member.getLbClusterId());
            	}
            	System.out.println("\tMemberPrivateIp : "+member.getMemberIp());
            	System.out.println("\tMemberFloatingIp : "+member.getMemberPublicIp());
            	System.out.println("\t-----------------------");
			}

            System.out.println("==================================================");
            System.out.println("List of LB members for the [cluster]: " + alias );
            
            // Invoke  cluster/{clusterId}
            for (Member m : members) {
            	HttpResponse responseCluster = restClientService.doGet(httpClient, restClientService.getUrl() + listClusterRestEndpoint
                        +"clusterId/"+ m.getLbClusterId(), restClientService.getUsername(), restClientService.getPassword());

                String responseCode = "" + responseCluster.getStatusLine().getStatusCode();
                String resultStringCluster = getHttpResponseString(responseCluster);

                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();

                if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                    ExceptionMapper exception = gson.fromJson(resultStringCluster, ExceptionMapper.class);
                    System.out.println(exception);
                    break;
                }

                printLBs(resultStringCluster);                
			}
            
        } catch (Exception e) {
            handleException("Exception in listing subscribe cartridges", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

	private Member[] getMembers(String cartridgeType, String alias, DefaultHttpClient httpClient) throws Exception{
        try {
            HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl() + listClusterRestEndpoint
                    + cartridgeType + "/" + alias,
                    restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();
            
            Gson gson = new Gson();
            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
            	String resultString = getHttpResponseString(response);
            	ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
            	System.out.println(exception);
                return null;
            }

            Cluster cluster = getClusterObjectFromString(getHttpResponseString(response));

            if (cluster == null) {
                System.out.println("No existing subscriptions found for alias " + alias);
                return null;
            }

            Member[] members = new Member[cluster.getMember().size()];
            members = cluster.getMember().toArray(members);
         
		    return members;
        } catch (Exception e) {
            handleException("Exception in get member", e);
            return null;
        }
	}

	private Cluster getClusterObjectFromString(String resultString) {
		String tmp;
		if(resultString.startsWith("{\"cluster\"")) {
		   tmp = resultString.substring("{\"cluster\"".length() + 1, resultString.length()-1);
		   resultString = tmp;
		}
		GsonBuilder gsonBuilder = new GsonBuilder();
		Gson gson = gsonBuilder.create();

		Cluster cluster = gson.fromJson(resultString, Cluster.class);
		return cluster;
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
            if (logger.isDebugEnabled()) {
                logger.debug("No subscribed cartridges found");
            }
            System.out.println("There are no subscribed cartridges");
            return;
        }
        
        for (Member member : members) {
        	System.out.println("\n\tServiceName : "+member.getServiceName());
        	System.out.println("\tClusterId : "+member.getClusterId());
        	System.out.println("\tNewtworkPartitionId : "+member.getNetworkPartitionId());
        	System.out.println("\tPartitionId : "+member.getPartitionId());
        	System.out.println("\tStatus : "+member.getStatus());
        	if(member.getLbClusterId() != null) {
        	System.out.println("\tLBCluster : "+member.getLbClusterId());
        	}
        	System.out.println("\tMemberPrivateIp : "+member.getMemberIp());
        	System.out.println("\tMemberFloatingIp : "+member.getMemberPublicIp());
        	System.out.println("\t-----------------------");
		}
	}

	// This method does the cartridge subscription
    public void subscribe(String cartridgeType, String alias, String externalRepoURL, boolean privateRepo, String username,
                          String password,String asPolicy,
                          String depPolicy, String size, boolean remoOnTermination, boolean persistanceMapping,
                          boolean enableCommits)
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
        cartridgeInfoBean.setSize(size);
        cartridgeInfoBean.setRemoveOnTermination(remoOnTermination);
        cartridgeInfoBean.setPersistanceRequired(persistanceMapping);
        cartridgeInfoBean.setCommitsEnabled(enableCommits);

        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();

        String jsonSubscribeString = gson.toJson(cartridgeInfoBean, CartridgeInfoBean.class);

        SubscriptionInfo subcriptionConnectInfo = null;

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
            cartridgeInfoBean.setAutoscalePolicy(asPolicy);
            cartridgeInfoBean.setDeploymentPolicy(depPolicy);
            cartridgeInfoBean.setSize(size);
            cartridgeInfoBean.setRemoveOnTermination(remoOnTermination);
            cartridgeInfoBean.setPersistanceRequired(persistanceMapping);
            cartridgeInfoBean.setCommitsEnabled(enableCommits);
            
            jsonSubscribeString = gson.toJson(cartridgeInfoBean, CartridgeInfoBean.class);

            HttpResponse response = restClientService.doPost(httpClient, restClientService.getUrl() + subscribCartridgeRestEndpoint,
                    jsonSubscribeString, restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();

            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
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
            }

            if (subcriptionConnectInfo != null) {
                hostnames += ", " + subcriptionConnectInfo.getHostname();
                hostnamesLabel = "host names";

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

            HttpResponse response = restClientService.doPost(httpClient, restClientService.getUrl() + addTenantEndPoint,
                    jsonString, restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();

            if (responseCode.equals(CliConstants.RESPONSE_OK)){
                System.out.println("Tenant added successfully");
                return;
            } else {
            	String resultString = getHttpResponseString(response);
            	ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
            	System.out.println(exception);
                return;
            }

        } catch (Exception e) {
            handleException("Exception in creating tenant", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to delete the created tenant
    public void deleteTenant(String tenantDomain) throws CommandException{
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doDelete(httpClient, restClientService.getUrl()
                    + addTenantEndPoint + "/" + tenantDomain, restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have succesfully delete " + tenantDomain + " tenant");
                return;
            } else {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

        } catch (Exception e) {
            handleException("Exception in deleting " + tenantDomain + " tenant", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to deactivate the created tenant
    public void deactivateTenant(String tenantDomain) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doPost(httpClient, restClientService.getUrl()
                    + deactivateTenantRestEndPoint + "/" + tenantDomain, "", restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have succesfully deactivate " + tenantDomain + " tenant");
                return;
            } else {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
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
            HttpResponse response = restClientService.doPost(httpClient, restClientService.getUrl()
                    + activateTenantRestEndPoint + "/" + tenantDomain, "", restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have succesfully activate " + tenantDomain + " tenant");
                return;
            } else {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

        } catch (Exception e) {
            handleException("Exception in activating " + tenantDomain + " tenant", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to list all tenants
    public void listAllTenants() throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl() + listAllTenantRestEndPoint,
                    restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();
            String resultString = getHttpResponseString(response);

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

            if (resultString == null) {
                System.out.println("Response content is empty");
                return;
            }

            TenantInfoList tenantInfoList = gson.fromJson(resultString, TenantInfoList.class);

            if (tenantInfoList == null) {
                System.out.println("Tenant information list is empty");
                return;
            }

            RowMapper<TenantInfoBean> tenantInfoMapper = new RowMapper<TenantInfoBean>() {

                public String[] getData(TenantInfoBean tenantInfo) {
                    String[] data = new String[5];
                    data[0] = tenantInfo.getTenantDomain();
                    data[1] = "" + tenantInfo.getTenantId();
                    data[2] = tenantInfo.getEmail();
                    data[3] = tenantInfo.isActive() ? "Active" : "De-active";
                    data[4] = tenantInfo.getCreatedDate();
                    return data;
                }
            };

            TenantInfoBean[] tenants = new TenantInfoBean[tenantInfoList.getTenantInfoBean().size()];
            tenants = tenantInfoList.getTenantInfoBean().toArray(tenants);

            if (tenants.length == 0) {
            	String message = "Cannot find any Tenant. "
            			+ "Please create a new tenant using [" + CliConstants.ADD_TENANT + "] command.";
                if (logger.isDebugEnabled()) {
                    logger.debug(message);
                }
                System.out.println(message);
                return;
            }

            System.out.println("Available Tenants:" );
            CommandLineUtils.printTable(tenants, tenantInfoMapper, "Domain", "Tenant ID", "Email", "State", "Created Date");
            System.out.println();

        } catch (Exception e) {
            handleException("Exception in listing partitions", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to unsubscribe cartridges
    public void unsubscribe(String alias) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doPost(httpClient, restClientService.getUrl() + unsubscribeTenantEndPoint, alias,
                    restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

             if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have successfully unsubscribed " + alias + " cartridge");
                return;
            } else {
                 String resultString = getHttpResponseString(response);
                 ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                 System.out.println(exception);
                 return;
            }

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

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have successfully deployed the cartridge");
                return;
            } else {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

        } catch (Exception e) {
            handleException("Exception in deploy cartridge definition", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to undeploy cartridge definitions
    public void undeployCartrigdeDefinition (String id) throws CommandException{
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doDelete(httpClient, restClientService.getUrl()
                    + cartridgeDeploymentEndPoint + "/" + id, restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have succesfully undeploy " + id + " cartridge");
                return;
            } else {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

        } catch (Exception e) {
            handleException("Exception in undeploying " + id + " cartridge", e);
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
            String resultString = getHttpResponseString(response);

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            } else {
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

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have successfully deployed the autoscaling policy");
                return;
            } else {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

        } catch (Exception e) {
            handleException("Exception in deploying autoscale police", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to deploy multi-tenant service cluster
    public void deployService (String deployService) throws CommandException{
        DefaultHttpClient httpClient= new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doPost(httpClient, restClientService.getUrl() + deployServiceEndPoint,
                    deployService, restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have succesfully deploy the multi-tenant service cluster");
                return;
            } else {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

        } catch (Exception e) {
            handleException("Exception in deploying multi-tenant service cluster", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method helps to undeploy multi-tenant service cluster
    public void undeployService(String id) throws  CommandException{
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doDelete(httpClient, restClientService.getUrl()
                    + deployServiceEndPoint + "/" + id, restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have succesfully undeploy multi-tenant service cluster");
                return;
            } else {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

        } catch (Exception e) {
            handleException("Exception in undeploying multi-tenant service cluster", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method list deploy services
    public void listDeployServices() throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl() + listDeployServicesRestEndPoint,
                    restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

            String resultString = getHttpResponseString(response);

            if (resultString == null) {
                System.out.println("Response content is empty");
                return;
            }

            ServiceDefinitionList definitionList = gson.fromJson(resultString, ServiceDefinitionList.class);

            if (definitionList == null) {
                System.out.println("Deploy service list is empty");
                return;
            }

            RowMapper<ServiceDefinitionBean> deployServiceMapper = new RowMapper<ServiceDefinitionBean>() {

                public String[] getData(ServiceDefinitionBean definition) {
                    String[] data = new String[7];
                    data[0] = definition.getServiceName();
                    data[1] = definition.getCartridgeType();
                    data[2] = definition.getDeploymentPolicyName();
                    data[3] = definition.getAutoscalingPolicyName();
                    data[4] = definition.getClusterDomain();
                    data[5] = definition.getClusterSubDomain();
                    data[6] = definition.getTenantRange();
                    return data;
                }
            };

            ServiceDefinitionBean[] definitionArry = new ServiceDefinitionBean[definitionList.getServiceDefinition().size()];
            definitionArry = definitionList.getServiceDefinition().toArray(definitionArry);

            if (definitionArry.length == 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No deploy services are found");
                }
                System.out.println("There are no deploy services available");
                return;
            }

            List<String> headers = new ArrayList<String>();
            headers.add("Service Name");
            headers.add("Cartridge Type");
            headers.add("Deployment Policy Name");
            headers.add("Autoscaling Policy Name");
            headers.add("Cluster Domain");
            headers.add("Cluster Sub Domain");
            headers.add("Tenant Range");

            System.out.println("Available Deploy Services :");
            CommandLineUtils.printTable(definitionArry, deployServiceMapper, headers.toArray(new String[headers.size()]));

        } catch (Exception e) {
            handleException("Exception in listing deploy services", e);
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

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if (responseCode.equals(CliConstants.RESPONSE_OK)) {
                System.out.println("You have successfully deployed the deployment policy");
                return;
            } else {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
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
            String resultString = getHttpResponseString(response);

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

            if (resultString == null) {
                System.out.println("Response content is empty");
                return;
            }

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

            if (partitions.length == 0) {
            	String message = "Cannot find any deployed Partition. "
            			+ "Please deploy a Partition using [" + CliConstants.PARTITION_DEPLOYMENT + "] command.";
                if (logger.isDebugEnabled()) {
                    logger.debug(message);
                }
                System.out.println(message);
                return;
            }

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

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

            String resultString = getHttpResponseString(response);

            if (resultString == null) {
                System.out.println("Response content is empty");
                return;
            }

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

            if (policyArry.length == 0) {
            	String message = "Cannot find any deployed auto-scaling policy. "
            			+ "Please deploy a policy using [" + CliConstants.AUTOSCALING_POLICY_DEPLOYMENT + "] command.";
                if (logger.isDebugEnabled()) {
					logger.debug(message);
                }
                System.out.println(message);
                return;
            }

            System.out.println("Available Auto-scaling Policies:");
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

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

            String resultString = getHttpResponseString(response);
            if (resultString == null) {
                System.out.println("Response content is empty");
                return;
            }

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

            if (policyArry.length == 0) {
            	String message = "Cannot find any deployed deployment policy. "
            			+ "Please deploy a policy using [" + CliConstants.DEPLOYMENT_POLICY_DEPLOYMENT + "] command.";
                if (logger.isDebugEnabled()) {
                    logger.debug(message);
                }
                System.out.println(message);
                return;
            }

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
                    + listDeploymentPolicyRestEndPoint, restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

            String resultString = getHttpResponseString(response);
            if (resultString == null) {
                System.out.println("Response content is empty");
                return;
            }

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
                    + listParitionRestEndPoint, restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

            String resultString = getHttpResponseString(response);
            if (resultString == null) {
                System.out.println("Response content is empty");
                return;
            }

            PartitionList partitionList = gson.fromJson(resultString, PartitionList.class);

            for (Partition partition : partitionList.getPartition()) {
                if(partition.getId().equals(id)) {
                    System.out.println("The Partition is:");
                    System.out.println(gson.toJson(partition));
                    return;
                }
            }
            System.out.println("Cannot find a matching Partition for [id] "+id);
        } catch (Exception e) {
            handleException("Exception in listing deployment polices", e);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    // This method describe about auto scaling policies
    public void describeAutoScalingPolicy(String id) throws CommandException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = restClientService.doGet(httpClient, restClientService.getUrl()
                    + listAutoscalePolicyRestEndPoint, restClientService.getUsername(), restClientService.getPassword());

            String responseCode = "" + response.getStatusLine().getStatusCode();


            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();

            if ( ! responseCode.equals(CliConstants.RESPONSE_OK)) {
                String resultString = getHttpResponseString(response);
                ExceptionMapper exception = gson.fromJson(resultString, ExceptionMapper.class);
                System.out.println(exception);
                return;
            }

            String resultString = getHttpResponseString(response);
            if (resultString == null) {
                System.out.println("Response content is empty");
                return;
            }

            AutoscalePolicyList policyList = gson.fromJson(resultString, AutoscalePolicyList.class);

            if (policyList == null) {
                System.out.println("Autoscale policy list is empty");
                return;
            }

            for(AutoscalePolicy policy : policyList.getAutoscalePolicy()) {
               if(policy.getId().equalsIgnoreCase(id)) {
                   System.out.println("Autoscale policy is:");
                   System.out.println(gson.toJson(policy));
                   return;
               }
            }
            System.out.println("No matching Autoscale Policy found...");

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

    // This class convert JSON string to servicedefinitionbean object
    private class ServiceDefinitionList {
        private ArrayList<ServiceDefinitionBean> serviceDefinition;

        public ArrayList<ServiceDefinitionBean> getServiceDefinition() {
            return serviceDefinition;
        }

        public void setServiceDefinition(ArrayList<ServiceDefinitionBean> serviceDefinition) {
            this.serviceDefinition = serviceDefinition;
        }

        ServiceDefinitionList() {
            serviceDefinition = new ArrayList<ServiceDefinitionBean>();
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

    // This class convert JSON string to TenantInfoBean object
    private class TenantInfoList {
        private ArrayList<TenantInfoBean> tenantInfoBean;

        public ArrayList<TenantInfoBean> getTenantInfoBean() {
            return tenantInfoBean;
        }

        public void setTenantInfoBean(ArrayList<TenantInfoBean> tenantInfoBean) {
            this.tenantInfoBean = tenantInfoBean;
        }

        TenantInfoList() {
            tenantInfoBean = new ArrayList<TenantInfoBean>();
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
    	PortMapping[] portMappings = cartridge.getPortMappings();
    	StringBuilder urlBuilder = new StringBuilder();

        for (PortMapping portMapping : portMappings) {
			String url = portMapping.getProtocol()+"://"+ cartridge.getHostName() + ":" + portMapping.getProxyPort() + "/";
			urlBuilder.append(url).append(",");
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
    
    /**
     * To map RestApiException of back-end.
     * @author nirmal
     *
     */
    public class ExceptionMapper {
    	@Override
		public String toString() {
			return Error.toString();
		}

		private ErrorWrapper Error;

		public ErrorWrapper getError() {
			return Error;
		}

		public void setError(ErrorWrapper error) {
			Error = error;
		}
    	
    }
    
    public class ErrorWrapper {
    	private String errorCode;
    	private String errorMessage;

        public String getErrorCode() {
    		return errorCode;
    	}

        public void setErrorCode(String errorCode) {
    		this.errorCode = errorCode;
    	}

        public String getErrorMessage() {
    		return errorMessage;
    	}

        public void setErrorMessage(String errorMessage) {
    		this.errorMessage = errorMessage;
    	}

        @Override
    	public String toString() {
    		return "Exception [errorCode=" + errorCode
    				+ ", errorMessage=" + errorMessage + "]";
    	}
    	
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
}

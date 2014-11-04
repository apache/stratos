package org.apache.stratos.manager.utils;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.stub.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.stub.pojo.Persistence;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceUnregisteredCartridgeExceptionException;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.cloud.controller.stub.pojo.Property;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.manager.dao.DataCartridge;
import org.apache.stratos.manager.dao.PortMapping;
import org.apache.stratos.manager.dto.Policy;
import org.apache.stratos.manager.dto.SubscriptionInfo;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.UnregisteredCartridgeException;
import org.apache.stratos.manager.repository.Repository;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * This class contains utility methods used by ApplicationManagementService.
 */
public class ApplicationManagementUtil {

    private static Log log = LogFactory.getLog(ApplicationManagementUtil.class);
    //private static volatile CloudControllerServiceClient serviceClient;

    protected static String getAppDeploymentDirPath(String cartridge, AxisConfiguration axisConfig) {
        return axisConfig.getRepository().getPath() + File.separator + cartridge;
    }

    public static CartridgeSubscriptionInfo createCartridgeSubscription(CartridgeInfo cartridgeInfo,
                                                                    String policyName,
                                                                    String cartridgeType,
                                                                    String cartridgeName,
                                                                    int tenantId,
                                                                    String tenantDomain,
                                                                    Repository repository,
                                                                    String hostName,
                                                                    String clusterDomain,
                                                                    String clusterSubDomain,
                                                                    String mgtClusterDomain,
                                                                    String mgtClusterSubDomain,
                                                                    DataCartridge dataCartridge,
                                                                    String state,
                                                                    String subscribeKey) {

        CartridgeSubscriptionInfo cartridgeSubscriptionInfo = new CartridgeSubscriptionInfo();
        cartridgeSubscriptionInfo.setCartridge(cartridgeType);
        cartridgeSubscriptionInfo.setAlias(cartridgeName);
        cartridgeSubscriptionInfo.setClusterDomain(clusterDomain);
        cartridgeSubscriptionInfo.setClusterSubdomain(clusterSubDomain);
        cartridgeSubscriptionInfo.setMgtClusterDomain(mgtClusterDomain);
        cartridgeSubscriptionInfo.setMgtClusterSubDomain(mgtClusterSubDomain);
        cartridgeSubscriptionInfo.setHostName(hostName);
        cartridgeSubscriptionInfo.setPolicy(policyName);
        cartridgeSubscriptionInfo.setRepository(repository);
        cartridgeSubscriptionInfo.setPortMappings(createPortMappings(cartridgeInfo));
        cartridgeSubscriptionInfo.setProvider(cartridgeInfo.getProvider());
        cartridgeSubscriptionInfo.setDataCartridge(dataCartridge);
        cartridgeSubscriptionInfo.setTenantId(tenantId);
        cartridgeSubscriptionInfo.setTenantDomain(tenantDomain);
        cartridgeSubscriptionInfo.setBaseDirectory(cartridgeInfo.getBaseDir());
        //cartridgeSubscriptionInfo.setState("PENDING");
        cartridgeSubscriptionInfo.setState(state);
        cartridgeSubscriptionInfo.setSubscriptionKey(subscribeKey);
        return cartridgeSubscriptionInfo;
    }



	private static List<PortMapping> createPortMappings(CartridgeInfo cartridgeInfo) {
        List<PortMapping> portMappings = new ArrayList<PortMapping>();

        if (cartridgeInfo.getPortMappings() != null) {
            for (org.apache.stratos.cloud.controller.stub.pojo.PortMapping portMapping : cartridgeInfo.getPortMappings()) {
                PortMapping portMap = new PortMapping();
                portMap.setPrimaryPort(portMapping.getPort());
                portMap.setProxyPort(portMapping.getProxyPort());
                portMap.setType(portMapping.getProtocol());
                portMappings.add(portMap);
            }
        }
        return portMappings;
    }

    public static int getTenantId(ConfigurationContext configurationContext) {
        int tenantId = MultitenantUtils.getTenantId(configurationContext);
        if(log.isDebugEnabled()) {
            log.debug("Returning tenant ID : " + tenantId);
        }
        return tenantId;
    }

		
	
    public static String generatePassword() {

        final int PASSWORD_LENGTH = 8;
        StringBuffer sb = new StringBuffer();
        for (int x = 0; x < PASSWORD_LENGTH; x++) {
            sb.append((char) ((int) (Math.random() * 26) + 97));
        }
        return sb.toString();

    }

   
    public static java.util.Properties setRegisterServiceProperties(Policy policy, int tenantId, String alias) {
    	
    	DecimalFormat df = new DecimalFormat("##.##");
        df.setParseBigDecimal(true);

        java.util.Properties properties = new java.util.Properties();
        List<Property> allProperties = new ArrayList<Property>();
        // min_app_instances
        Property property = new Property();
        property.setName("min_app_instances");
        property.setValue(df.format(policy.getMinAppInstances()));
        allProperties.add(property);
        
        
     // max_app_instances
        property = new Property();
        property.setName("max_app_instances");
        property.setValue(df.format(policy.getMaxAppInstances()));
        allProperties.add(property);
        
        // max_requests_per_second
        property = new Property();
        property.setName("max_requests_per_second");
        property.setValue(df.format(policy.getMaxRequestsPerSecond()));
        allProperties.add(property);
        
        // alarming_upper_rate
        property = new Property();
        property.setName("alarming_upper_rate");
        property.setValue(df.format(policy.getAlarmingUpperRate()));
        allProperties.add(property);
        
     // alarming_lower_rate
        property = new Property();
        property.setName("alarming_lower_rate");
        property.setValue(df.format(policy.getAlarmingLowerRate()));
        allProperties.add(property);
        
        // scale_down_factor
        property = new Property();
        property.setName("scale_down_factor");
        property.setValue(df.format(policy.getScaleDownFactor()));
        allProperties.add(property);
        
     // rounds_to_average
        property = new Property();
        property.setName("rounds_to_average");
        property.setValue(df.format(policy.getRoundsToAverage()));
        allProperties.add(property);
        
       // tenant id
        property = new Property();
        property.setName("tenant_id");
        property.setValue(String.valueOf(tenantId));
        allProperties.add(property);
        
        // alias
        property = new Property();
        property.setName("alias");
        property.setValue(String.valueOf(alias));
        allProperties.add(property);
        
        return addToJavaUtilProperties(allProperties);
    }

    private static java.util.Properties addToJavaUtilProperties(List<Property> allProperties) {
        java.util.Properties properties = new java.util.Properties();
        for (Property property : allProperties) {
            properties.put(property.getName(), property.getValue());
        }
        return properties;
    }

    private static String convertRepoURL(String gitURL) {
        String convertedHttpURL = null;
        if (gitURL != null && gitURL.startsWith("git@")) {
            StringBuilder httpRepoUrl = new StringBuilder();
            httpRepoUrl.append("http://");
            String[] urls = gitURL.split(":");
            String[] hostNameArray = urls[0].split("@");
            String hostName = hostNameArray[1];
            httpRepoUrl.append(hostName).append("/").append(urls[1]);
            convertedHttpURL = httpRepoUrl.toString();
        } else if (gitURL != null && gitURL.startsWith("http")) {
            convertedHttpURL = gitURL;
        }
        return convertedHttpURL;
    }

    public static void addDNSEntry(String alias, String cartridgeType) {
        //new DNSManager().addNewSubDomain(alias + "." + cartridgeType, System.getProperty(CartridgeConstants.ELB_IP));
    }

    public static SubscriptionInfo createSubscriptionResponse(CartridgeSubscriptionInfo cartridgeSubscriptionInfo, Repository repository) {
    	SubscriptionInfo subscriptionInfo = new SubscriptionInfo();
    	
        if (repository != null && repository.getUrl() != null) {
        	subscriptionInfo.setRepositoryURL(convertRepoURL(repository.getUrl()));
        }
        
        subscriptionInfo.setHostname(cartridgeSubscriptionInfo.getHostName());
        
        return subscriptionInfo;
    }

    
    
    public static void registerService(String cartridgeType, String domain, String subDomain,
                                       StringBuilder payload, String tenantRange, String hostName,
                                       String autoscalingPoliyName, String deploymentPolicyName,
                                       Properties properties, Persistence persistence)
            throws ADCException, UnregisteredCartridgeException {
        log.info("Register service..");
        try {
            CloudControllerServiceClient.getServiceClient().register(domain, cartridgeType, payload.toString(), tenantRange,
                    hostName, properties, autoscalingPoliyName, deploymentPolicyName, persistence );
        } catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
            String msg = "Exception is occurred in register service operation. Reason :" + e.getMessage();
            log.error(msg, e);
            throw new UnregisteredCartridgeException("Not a registered cartridge " + cartridgeType, cartridgeType, e);
        } catch (RemoteException e) {
        	log.error("Remote Error", e);
        	throw new ADCException("An error occurred in subscribing process", e);
        }
    }

    

}

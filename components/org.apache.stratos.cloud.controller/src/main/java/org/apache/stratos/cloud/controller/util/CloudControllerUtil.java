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
package org.apache.stratos.cloud.controller.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.config.CloudControllerConfig;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.exception.InvalidIaasProviderException;
import org.apache.stratos.cloud.controller.exception.InvalidKubernetesClusterException;
import org.apache.stratos.cloud.controller.exception.InvalidKubernetesHostException;
import org.apache.stratos.cloud.controller.exception.InvalidKubernetesMasterException;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.registry.RegistryManager;
import org.apache.stratos.common.Property;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesCluster;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesHost;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesMaster;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import com.google.common.net.InetAddresses;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;


public class CloudControllerUtil {
	private static final Log log = LogFactory.getLog(CloudControllerUtil.class);

    public static Iaas createIaasInstance(IaasProvider iaasProvider)
            throws InvalidIaasProviderException {
        try {
            if(iaasProvider.getClassName() == null) {
                String msg = "You have not specified a class which represents the iaas of type: ["
                        + iaasProvider.getType() + "].";
                log.error(msg);
                throw new InvalidIaasProviderException(msg);
            }

            Constructor<?> c = Class.forName(iaasProvider.getClassName()).getConstructor(IaasProvider.class);
            Iaas iaas = (Iaas) c.newInstance(iaasProvider);
            return iaas;
        } catch (Exception e) {
            String msg = "Class [" + iaasProvider.getClassName()
                    + "] which represents the iaas of type: ["
                    + iaasProvider.getType() + "] has failed to instantiate.";
            log.error(msg, e);
            throw new InvalidIaasProviderException(msg, e);
        }
    }

    public static Cartridge toCartridge(CartridgeConfig config) {
        if (config == null) {
            return null;
        }
        Cartridge cartridge = new Cartridge();

	    // populate cartridge
	    cartridge.setType(config.getType());
	    cartridge.setDisplayName(config.getDisplayName());
	    cartridge.setDescription(config.getDescription());
	    cartridge.setHostName(config.getHostName());
	    String[] deploymentDirs = config.getDeploymentDirs();
	    if ((deploymentDirs != null) && (deploymentDirs.length > 0)) {
		    cartridge.setDeploymentDirs(Arrays.asList(deploymentDirs));
	    }
	    cartridge.setProvider(config.getProvider());
	    cartridge.setCategory(config.getCategory());
	    cartridge.setVersion(config.getVersion());
	    cartridge.setBaseDir(config.getBaseDir());
        
        if (config.getPortMappings() != null) {
			cartridge.setPortMappings(Arrays.asList(config.getPortMappings()));
		}
        
        if(config.getPersistence() != null){
        	cartridge.setPersistence(config.getPersistence());
        }
        cartridge.setMultiTenant(config.isMultiTenant());
        cartridge.setTenantPartitions(config.getTenantPartitions());
        cartridge.setDefaultAutoscalingPolicy(config.getDefaultAutoscalingPolicy());
        cartridge.setDefaultDeploymentPolicy(config.getDefaultDeploymentPolicy());
        cartridge.setServiceGroup(config.getServiceGroup());
	    cartridge.setMetadataKeys(config.getMetadataKeys());

        org.apache.stratos.common.Properties props = config.getProperties();
        if (props != null) {
            for (Property prop : props.getProperties()) {
                cartridge.addProperty(prop.getName(), String.valueOf(prop.getValue()));
            }
        }
        
        // populate LB config
        cartridge.setLbConfig(config.getLbConfig());

        List<IaasProvider> iaases = CloudControllerConfig.getInstance().getIaasProviders();

        // populate IaaSes
        IaasConfig[] iaasConfigs = config.getIaasConfigs();
        if (iaasConfigs != null) {
            for (IaasConfig iaasConfig : iaasConfigs) {
                if (iaasConfig != null) {
                    IaasProvider iaasProvider = null;
                    if (iaases != null) {
                        // check whether this is a reference to a predefined IaaS.
                        for (IaasProvider iaas : iaases) {
                            if (iaas.getType().equals(iaasConfig.getType())) {
                                iaasProvider = new IaasProvider(iaas);
                                break;
                            }
                        }
                    }

                    if (iaasProvider == null) {
                        iaasProvider = new IaasProvider();
                        iaasProvider.setType(iaasConfig.getType());
                    }

                    String className = iaasConfig.getClassName();
                    if (className != null) {
                        iaasProvider.setClassName(className);
                    }

                    String name = iaasConfig.getName();
                    if (name != null) {
                        iaasProvider.setName(name);
                    }

                    String identity = iaasConfig.getIdentity();
                    if (identity != null) {
                        iaasProvider.setIdentity(identity);
                    }

                    String credential = iaasConfig.getCredential();
                    if (credential != null) {
                        iaasProvider.setCredential(credential);
                    }

                    String provider = iaasConfig.getProvider();
                    if (provider != null) {
                        iaasProvider.setProvider(provider);
                    }
                    String imageId = iaasConfig.getImageId();
                    if (imageId != null) {
                        iaasProvider.setImage(imageId);
                    }
                    
                    byte[] payload = iaasConfig.getPayload();
                    if (payload != null) {
                        iaasProvider.setPayload(payload);
                    }

                    org.apache.stratos.common.Properties props1 = iaasConfig.getProperties();
                    if (props1 != null) {
                        for (Property prop : props1.getProperties()) {
                            iaasProvider.addProperty(prop.getName(), String.valueOf(prop.getValue()));
                        }
                    }
                    
                    NetworkInterfaces networkInterfaces = iaasConfig.getNetworkInterfaces();
                    if (networkInterfaces != null && networkInterfaces.getNetworkInterfaces() != null) {
                        iaasProvider.setNetworkInterfaces(networkInterfaces.getNetworkInterfaces());
                    }

                    cartridge.addIaasProvider(iaasProvider);
                }
            }
        }

        if(config.getExportingProperties() != null){
            cartridge.setExportingProperties(config.getExportingProperties());
        }

        return cartridge;
    }
	  
    public static CartridgeInfo toCartridgeInfo(Cartridge cartridge) {

		CartridgeInfo cartridgeInfo = new CartridgeInfo();
		cartridgeInfo.setType(cartridge.getType());
		cartridgeInfo.setDisplayName(cartridge.getDisplayName());
		cartridgeInfo.setDescription(cartridge.getDescription());
		cartridgeInfo.setHostName(cartridge.getHostName());
		cartridgeInfo.setDeploymentDirs(cartridge.getDeploymentDirs());
		cartridgeInfo.setProvider(cartridge.getProvider());
	    cartridgeInfo.setCategory(cartridge.getCategory());
		cartridgeInfo.setVersion(cartridge.getVersion());
		cartridgeInfo.setMultiTenant(cartridge.isMultiTenant());
		cartridgeInfo.setBaseDir(cartridge.getBaseDir());
		cartridgeInfo.setLbConfig(cartridge.getLbConfig());
        cartridgeInfo.setTenantPartitions(cartridge.getTenantPartitions());
		cartridgeInfo.setDefaultAutoscalingPolicy(cartridge.getDefaultAutoscalingPolicy());
        cartridgeInfo.setDefaultDeploymentPolicy(cartridge.getDefaultDeploymentPolicy());
		cartridgeInfo.setPortMappings(cartridge.getPortMappings()
                .toArray(new PortMapping[cartridge.getPortMappings()
                        .size()]));
		cartridgeInfo.setAppTypes(cartridge.getAppTypeMappings()
                .toArray(new AppType[cartridge.getAppTypeMappings()
                        .size()]));
        cartridgeInfo.setServiceGroup(cartridge.getServiceGroup());
		
		List<Property> propList = new ArrayList<Property>();
        cartridgeInfo.setPersistence(cartridge.getPersistence());
		
		for (Iterator<?> iterator = cartridge.getProperties().entrySet().iterator(); iterator.hasNext();) {
	        @SuppressWarnings("unchecked")
            Map.Entry<String, String> entry = (Entry<String, String>) iterator.next();
	        
	        Property prop = new Property(entry.getKey(), entry.getValue());
	        propList.add(prop);
        }
		Property[] props = new Property[propList.size()];
		
		cartridgeInfo.setProperties(propList.toArray(props));
	    cartridgeInfo.setMetadataKeys(cartridge.getMetadataKeys());

		return cartridgeInfo;
	}

    public static void sleep(long time){
    	try {
    		Thread.sleep(time);
    	} catch (InterruptedException ignore) {}
    	
    }
	
    public static String getProperty(org.apache.stratos.common.Properties properties, String key, String defaultValue) {
        Properties props = toJavaUtilProperties(properties);

        return getProperty(props, key, defaultValue);
    }

    public static String getProperty(Properties properties, String key, String defaultValue) {
        if (key != null && properties != null) {
            for (Iterator<Entry<Object, Object>> iterator = properties.entrySet().iterator(); iterator.hasNext();) {
                Entry<Object, Object> type = iterator.next();
                String propName = type.getKey().toString();
                String propValue = type.getValue().toString();
                if (key.equals(propName)) {
                    return propValue;
                }
            }
        }

        return defaultValue;
    }

    public static String getProperty(Properties properties, String key) {
        return getProperty(properties, key, null);
    }

    public static String getProperty(org.apache.stratos.common.Properties properties, String key) {
        Properties props = toJavaUtilProperties(properties);

        return getProperty(props, key);
    }

    public static org.apache.stratos.common.Properties addProperty(org.apache.stratos.common.Properties properties,
            String key, String value) {
        Property property = new Property();
        property.setName(key);
        property.setValue(value);

        org.apache.stratos.common.Properties newProperties = new org.apache.stratos.common.Properties();
        newProperties.setProperties(ArrayUtils.add(properties.getProperties(), property));
        return newProperties;
    }
	
	/**
	 * Converts org.apache.stratos.messaging.util.Properties to java.util.Properties
	 * @param properties org.apache.stratos.messaging.util.Properties
	 * @return java.util.Properties
	 */
    public static Properties toJavaUtilProperties(
        org.apache.stratos.common.Properties properties) {
        Properties javaUtilsProperties = new Properties();

        if (properties != null && properties.getProperties() != null) {

            for (Property property : properties.getProperties()) {
                if((property != null) && (property.getValue() != null)) {
                    javaUtilsProperties.put(property.getName(), property.getValue());
                }
            }

        }

        return javaUtilsProperties;
    }
    
    public static void persistTopology(Topology topology) {
      try {
          RegistryManager.getInstance().persist(CloudControllerConstants.TOPOLOGY_RESOURCE, topology);
      } catch (RegistryException e) {
          String msg = "Failed to persist the Topology in registry. ";
          log.fatal(msg, e);
      }
    }

    public static Topology retrieveTopology() {
        try {
            Object dataObj = RegistryManager.getInstance().read(CloudControllerConstants.TOPOLOGY_RESOURCE);
            return (Topology) dataObj;
        } catch (Exception e) {
            String msg = "Unable to retrieve data from registry";
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

	
	public static void handleException(String msg, Exception e){
		log.error(msg, e);
		throw new CloudControllerException(msg, e);
	}
	
	public static void handleException(String msg){
		log.error(msg);
		throw new CloudControllerException(msg);
	}

	public static String getPartitionIds(Partition[] partitions) {
		StringBuilder str = new StringBuilder("");
		for (Partition partition : partitions) {
			str.append(partition.getId()+", ");
		}
		
		String partitionStr = str.length() == 0 ? str.toString() : str.substring(0, str.length()-2);
		return "[" +partitionStr+ "]";
	}
	
	public static String replaceDotsWithDash(String id) {
		if (id.indexOf('.') != -1) {
			id = id.replace('.', '-');
		}
		return id;
	}
	
	public static void validateKubernetesCluster(KubernetesCluster kubernetesCluster) throws InvalidKubernetesClusterException {
        CloudControllerContext context = CloudControllerContext.getInstance();
	    
	    if (kubernetesCluster == null) {
            throw new InvalidKubernetesClusterException("Kubernetes cluster can not be null");
        }
        if (StringUtils.isEmpty(kubernetesCluster.getClusterId())) {
            throw new InvalidKubernetesClusterException("Kubernetes cluster groupId can not be empty");
        }
        if (context.kubernetesClusterExists(kubernetesCluster)) {
            throw new InvalidKubernetesClusterException(String.format("Kubernetes cluster already exists " +
                    "[id] %s", kubernetesCluster.getClusterId()));
        }
        if (kubernetesCluster.getKubernetesMaster() == null) {
            throw new InvalidKubernetesClusterException("Mandatory field master has not been set " +
                    "for the Kubernetes cluster [id] " + kubernetesCluster.getClusterId());
        }
        if (kubernetesCluster.getPortRange() == null) {
            throw new InvalidKubernetesClusterException("Mandatory field portRange has not been set " +
                    "for the Kubernetes cluster [id] " + kubernetesCluster.getClusterId());
        }

        // Port range validation
        if (kubernetesCluster.getPortRange().getUpper() > CloudControllerConstants.PORT_RANGE_MAX ||
                kubernetesCluster.getPortRange().getUpper() < CloudControllerConstants.PORT_RANGE_MIN ||
                kubernetesCluster.getPortRange().getLower() > CloudControllerConstants.PORT_RANGE_MAX ||
                kubernetesCluster.getPortRange().getLower() < CloudControllerConstants.PORT_RANGE_MIN ||
                kubernetesCluster.getPortRange().getUpper() < kubernetesCluster.getPortRange().getLower()) {
            throw new InvalidKubernetesClusterException("Port range is invalid in kubernetes cluster " +
                    "[kubenetes-cluster-id] " + kubernetesCluster.getClusterId() + " " +
                    " [valid-min] " + CloudControllerConstants.PORT_RANGE_MIN + " [valid-max] " +
                    CloudControllerConstants.PORT_RANGE_MAX);
        }
        try {
            validateKubernetesMaster(kubernetesCluster.getKubernetesMaster());
            validateKubernetesHosts(kubernetesCluster.getKubernetesHosts());

            // check whether master already exists
            if (context.kubernetesHostExists(kubernetesCluster.getKubernetesMaster().getHostId())) {
                throw new InvalidKubernetesClusterException("Kubernetes host already exists [id] " +
                        kubernetesCluster.getKubernetesMaster().getHostId());
            }

            // Check for duplicate hostIds
            if (kubernetesCluster.getKubernetesHosts() != null) {
                List<String> hostIds = new ArrayList<String>();
                hostIds.add(kubernetesCluster.getKubernetesMaster().getHostId());

                for (KubernetesHost kubernetesHost : kubernetesCluster.getKubernetesHosts()) {
                    if (hostIds.contains(kubernetesHost.getHostId())) {
                        throw new InvalidKubernetesClusterException(
                                String.format("Kubernetes host [id] %s already defined in the request", kubernetesHost.getHostId()));
                    }

                    // check whether host already exists
                    if (context.kubernetesHostExists(kubernetesHost.getHostId())) {
                        throw new InvalidKubernetesClusterException("Kubernetes host already exists [id] " +
                                kubernetesHost.getHostId());
                    }

                    hostIds.add(kubernetesHost.getHostId());
                }
            }

        } catch (InvalidKubernetesHostException e) {
            throw new InvalidKubernetesClusterException(e.getMessage());
        } catch (InvalidKubernetesMasterException e) {
            throw new InvalidKubernetesClusterException(e.getMessage());
        }
    }
	
	private static void validateKubernetesHosts(KubernetesHost[] kubernetesHosts) throws InvalidKubernetesHostException {
        if (kubernetesHosts == null || kubernetesHosts.length == 0) {
            return;
        }
        for (KubernetesHost kubernetesHost : kubernetesHosts) {
            validateKubernetesHost(kubernetesHost);
        }
    }

    public static void validateKubernetesHost(KubernetesHost kubernetesHost) throws InvalidKubernetesHostException {
        if (kubernetesHost == null) {
            throw new InvalidKubernetesHostException("Kubernetes host can not be null");
        }
        if (StringUtils.isEmpty(kubernetesHost.getHostId())) {
            throw new InvalidKubernetesHostException("Kubernetes host id can not be empty");
        }
        if (kubernetesHost.getHostIpAddress() == null) {
            throw new InvalidKubernetesHostException("Mandatory field Kubernetes host IP address has not been set " +
                    "for [hostId] " + kubernetesHost.getHostId());
        }
        if (!InetAddresses.isInetAddress(kubernetesHost.getHostIpAddress())) {
            throw new InvalidKubernetesHostException("Kubernetes host ip address is invalid: " + kubernetesHost.getHostIpAddress());
        }
    }

    public static void validateKubernetesMaster(KubernetesMaster kubernetesMaster) throws InvalidKubernetesMasterException {
        try {
            validateKubernetesHost(kubernetesMaster);
        } catch (InvalidKubernetesHostException e) {
            throw new InvalidKubernetesMasterException(e.getMessage());
        }
    }
}

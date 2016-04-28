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

import com.google.common.net.InetAddresses;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.config.CloudControllerConfig;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesCluster;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesHost;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesMaster;
import org.apache.stratos.cloud.controller.exception.InvalidIaasProviderException;
import org.apache.stratos.cloud.controller.exception.InvalidKubernetesClusterException;
import org.apache.stratos.cloud.controller.exception.InvalidKubernetesHostException;
import org.apache.stratos.cloud.controller.exception.InvalidKubernetesMasterException;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.registry.RegistryManager;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.domain.LoadBalancingIPType;
import org.apache.stratos.messaging.domain.topology.Topology;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class CloudControllerUtil {
    private static final Log log = LogFactory.getLog(CloudControllerUtil.class);

    public static Iaas createIaasInstance(IaasProvider iaasProvider) throws InvalidIaasProviderException {
        try {
            if (iaasProvider.getClassName() == null) {
                throw new InvalidIaasProviderException(
                        String.format("IaaS provider implementation class name is not specified for IaaS [type] %s",
                                iaasProvider.getType()));
            }
            Constructor<?> c = Class.forName(iaasProvider.getClassName()).getConstructor(IaasProvider.class);
            return (Iaas) c.newInstance(iaasProvider);
        } catch (Exception e) {
            throw new InvalidIaasProviderException(
                    String.format("Failed to instantiate IaaS provider class for [type] %s", iaasProvider.getType()),
                    e);
        }
    }

    public static void extractIaaSProvidersFromCartridge(Cartridge cartridge) throws InvalidIaasProviderException {
        if (cartridge == null) {
            return;
        }
        List<IaasProvider> iaases = CloudControllerConfig.getInstance().getIaasProviders();

        // populate IaaSes
        IaasConfig[] iaasConfigs = cartridge.getIaasConfigs();
        if (iaasConfigs != null) {
            for (IaasConfig iaasConfig : iaasConfigs) {
                if (iaasConfig != null) {
                    IaasProvider matchingIaasProviderInCC = null;
                    if (iaases != null) {
                        // check whether this is a reference to a predefined IaaS.
                        for (IaasProvider iaas : iaases) {
                            if (iaas.getType().equals(iaasConfig.getType())) {
                                matchingIaasProviderInCC = iaas;
                                break;
                            }
                        }
                    }

                    if (matchingIaasProviderInCC == null) {
                        matchingIaasProviderInCC = new IaasProvider();
                        matchingIaasProviderInCC.setType(iaasConfig.getType());
                    }

                    IaasProvider iaasProvider = createUpdatedIaasProviderObject(iaasConfig, matchingIaasProviderInCC);

                    CloudControllerContext.getInstance().addIaasProvider(cartridge.getType(), iaasProvider);
                }
            }
        }
    }

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ignore) {
        }

    }

    public static String getProperty(org.apache.stratos.common.Properties properties, String key, String defaultValue) {
        Properties props = toJavaUtilProperties(properties);

        return getProperty(props, key, defaultValue);
    }

    private static String getProperty(Properties properties, String key, String defaultValue) {
        if (key != null && properties != null) {
            for (Entry<Object, Object> type : properties.entrySet()) {
                String propName = type.getKey().toString();
                String propValue = type.getValue().toString();
                if (key.equals(propName)) {
                    return propValue;
                }
            }
        }

        return defaultValue;
    }

    private static String getProperty(Properties properties, String key) {
        return getProperty(properties, key, null);
    }

    public static String getProperty(org.apache.stratos.common.Properties properties, String key) {
        Properties props = toJavaUtilProperties(properties);

        return getProperty(props, key);
    }

    /**
     * Converts org.apache.stratos.messaging.util.Properties to java.util.Properties
     *
     * @param properties org.apache.stratos.messaging.util.Properties
     * @return java.util.Properties
     */
    public static Properties toJavaUtilProperties(org.apache.stratos.common.Properties properties) {
        Properties javaUtilsProperties = new Properties();

        if (properties != null && properties.getProperties() != null) {

            for (Property property : properties.getProperties()) {
                if ((property != null) && (property.getValue() != null)) {
                    javaUtilsProperties.put(property.getName(), property.getValue());
                }
            }

        }

        return javaUtilsProperties;
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

    public static String getPartitionIds(Partition[] partitions) {
        StringBuilder str = new StringBuilder("");
        for (Partition partition : partitions) {
            str.append(partition.getId()).append(", ");
        }

        String partitionStr = str.length() == 0 ? str.toString() : str.substring(0, str.length() - 2);
        return "[" + partitionStr + "]";
    }

    public static void validateKubernetesCluster(KubernetesCluster kubernetesCluster)
            throws InvalidKubernetesClusterException {
        if (kubernetesCluster == null) {
            throw new InvalidKubernetesClusterException("Kubernetes cluster can not be null");
        }
        if (StringUtils.isEmpty(kubernetesCluster.getClusterId())) {
            throw new InvalidKubernetesClusterException("Kubernetes cluster groupId can not be empty");
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

            // Check for duplicate hostIds
            if (kubernetesCluster.getKubernetesHosts() != null) {
                List<String> hostIds = new ArrayList<>();
                hostIds.add(kubernetesCluster.getKubernetesMaster().getHostId());

                for (KubernetesHost kubernetesHost : kubernetesCluster.getKubernetesHosts()) {
                    if (hostIds.contains(kubernetesHost.getHostId())) {
                        throw new InvalidKubernetesClusterException(
                                String.format("Kubernetes host [id] %s already defined in the request",
                                        kubernetesHost.getHostId()));
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

    private static void validateKubernetesHosts(KubernetesHost[] kubernetesHosts)
            throws InvalidKubernetesHostException {
        if (kubernetesHosts == null || kubernetesHosts.length == 0) {
            return;
        }
        for (KubernetesHost kubernetesHost : kubernetesHosts) {
            validateKubernetesHost(kubernetesHost);
        }
    }

    public static void validateKubernetesHost(KubernetesHost kubernetesHost) throws InvalidKubernetesHostException {
        if (kubernetesHost == null) {
            throw new InvalidKubernetesHostException("Kubernetes host is null");
        }
        if (StringUtils.isBlank(kubernetesHost.getHostId())) {
            throw new InvalidKubernetesHostException("Kubernetes host id cannot be empty");
        }
        if (StringUtils.isBlank(kubernetesHost.getPrivateIPAddress())) {
            throw new InvalidKubernetesHostException("Kubernetes host private IP address has not been set: " +
                    "[host-id] " + kubernetesHost.getHostId());
        }
    }

    public static void validateKubernetesMaster(KubernetesMaster kubernetesMaster)
            throws InvalidKubernetesMasterException {

        if (StringUtils.isBlank(kubernetesMaster.getEndpoint()) &&
                StringUtils.isBlank(kubernetesMaster.getPrivateIPAddress())) {
            throw new InvalidKubernetesMasterException("Kubernetes master private IP address or endpoint has not " +
                    "been set.");
        }
        if (StringUtils.isNotBlank(kubernetesMaster.getEndpoint()) &&
                StringUtils.isNotBlank(kubernetesMaster.getPrivateIPAddress())) {
            throw new InvalidKubernetesMasterException("Both kubenretes master private IP address and " +
                    "endpoint has been set. Please set either endpoint or private ip.");
        }

    }

    public static LoadBalancingIPType getLoadBalancingIPTypeEnumFromString(String loadBalancingIPType) {
        if (CloudControllerConstants.LOADBALANCING_IP_TYPE_PUBLIC.equals(loadBalancingIPType)) {
            return LoadBalancingIPType.Public;
        } else {
            return LoadBalancingIPType.Private;
        }
    }

    public static String getAliasFromClusterId(String clusterId) {
        return StringUtils.substringBefore(StringUtils.substringAfter(clusterId, "."), ".");
    }

    public static IaasProvider getUpdatedIaasProviderInstance (Cartridge cartridge, Partition partition)
            throws InvalidIaasProviderException {

        IaasConfig cartridgeIaasConfig = null;
        for (IaasConfig anIaasConfig : cartridge.getIaasConfigs()) {
            if (anIaasConfig.getType().equals(partition.getProvider())) {
                cartridgeIaasConfig = anIaasConfig;
            }
        }

        // get the correct IaaS Provider config from cloud-controller.xml
        IaasProvider ccIaasProvider = CloudControllerConfig.getInstance().getIaasProvider(partition
                .getProvider());

        if (ccIaasProvider == null) {
            String errorMsg = "No Iaas Provider configuration found in cloud-controller.xml for " +
                    "type " + partition.getProvider();
            log.error(errorMsg);
            throw new InvalidIaasProviderException(errorMsg);
        }

        IaasProvider iaasProvider = null;
        // update with new cloud-controller.xml and cartridge definition changes
        if (cartridgeIaasConfig == null) {
            iaasProvider = ccIaasProvider;
        } else {
            iaasProvider = createUpdatedIaasProviderObject(cartridgeIaasConfig, ccIaasProvider);
        }

        // update with Partition properties
        if (partition.getProperties() != null && partition.getProperties().getProperties() != null) {
            for (Property property : partition.getProperties().getProperties()) {
                iaasProvider.addProperty(property.getName(), property.getValue());
            }
        }

        if (log.isDebugEnabled()) {
            logProperties(iaasProvider.getType(), iaasProvider.getProperties(), cartridge.getType(),
                    partition.getId());
        }

        if (log.isDebugEnabled()) {
            logNetworkInterfaces(iaasProvider.getType(), iaasProvider.getNetworkInterfaces());
        }

        return iaasProvider;
    }

    private static IaasProvider createUpdatedIaasProviderObject (IaasConfig cartridgeIaasConfig,
                                                                 IaasProvider ccIaasProvider)
            throws InvalidIaasProviderException {

        // create a deep copy of the IaaSProvider, not a reference
        IaasProvider newIaasProvider = new IaasProvider(ccIaasProvider);

        // priority order
        // 1. cartridge definition
        // 2. cloud-controller.xml
        newIaasProvider.setClassName(selectAttribute("className", cartridgeIaasConfig.getClassName(),
                ccIaasProvider.getClassName(), true));

        // should not log identity details even in debug logs
        newIaasProvider.setIdentity(selectAttribute("identity", cartridgeIaasConfig.getIdentity(),
                ccIaasProvider.getIdentity(), false));

        // should not log credentials details even in debug logs
        newIaasProvider.setCredential(selectAttribute("credential", cartridgeIaasConfig.getCredential(),
                ccIaasProvider.getCredential(), false));

        newIaasProvider.setProvider(selectAttribute("provider", cartridgeIaasConfig.getProvider(),
                ccIaasProvider.getProvider(), true));

        newIaasProvider.setImage(selectAttribute("imageId", cartridgeIaasConfig.getImageId(),
                ccIaasProvider.getImage(), true));

        byte[] payload = cartridgeIaasConfig.getPayload();
        if (payload != null) {
            newIaasProvider.setPayload(payload);
        }

        Map<String, String> ccIaasProperties = ccIaasProvider.getProperties();
        if (ccIaasProperties != null) {
            for (Map.Entry<String, String> ccIaasProperty : ccIaasProperties.entrySet()) {
                newIaasProvider.addProperty(ccIaasProperty.getKey(), ccIaasProperty.getValue());
            }
        }

        // add properties defined in Cartridge and cloud-controller.xml
        org.apache.stratos.common.Properties cartridgeIaasProperties = cartridgeIaasConfig.getProperties();
        if (cartridgeIaasProperties != null) {
            for (Property prop : cartridgeIaasProperties.getProperties()) {
                newIaasProvider.addProperty(prop.getName(), String.valueOf(prop.getValue()));
            }
        }

        NetworkInterfaces networkInterfacesInCartridge = cartridgeIaasConfig.getNetworkInterfaces();
        NetworkInterface[] networkInterfacesInCC = ccIaasProvider.getNetworkInterfaces();
        if (networkInterfacesInCartridge != null && networkInterfacesInCartridge.getNetworkInterfaces().length > 0) {
            newIaasProvider.setNetworkInterfaces(networkInterfacesInCartridge
                    .getNetworkInterfaces());
        } else if (networkInterfacesInCC != null && networkInterfacesInCC.length > 0) {
            newIaasProvider.setNetworkInterfaces(networkInterfacesInCC);
        } else {
            log.debug("No network interface definition set for IaaS provider " + newIaasProvider.getType());
        }

        return newIaasProvider;
    }

    private static String selectAttribute(String attributeName, String attributeDefinedInCartridge,
                                          String attributeDefinedInCC, boolean logInfo) throws InvalidIaasProviderException {

        if (attributeDefinedInCartridge != null) {
            if (log.isDebugEnabled() && logInfo) {
                log.debug("Selected " + attributeName + "=" +
                        attributeDefinedInCartridge + " from Cartridge Definition");
            }
            return attributeDefinedInCartridge;
        } else if (attributeDefinedInCC != null) {
            if (log.isDebugEnabled() && logInfo) {
                log.debug("Selected " + attributeName + "=" +
                        attributeDefinedInCC + " from cloud-controller.xml configuration");
            }
            return attributeDefinedInCC;
        } else {
            String errorMsg = "Iaas Provider attribute " + attributeName + " not set in " +
                    "either cartridge definition of cloud-controller.xml";
            log.error(errorMsg);
            throw new InvalidIaasProviderException(errorMsg);
        }
    }

    private static void logNetworkInterfaces (String iaasProviderType, NetworkInterface[] networkInterfaces) {

        if (networkInterfaces != null) {
            log.debug("All Network interfaces in IaasProvider object for type: " +
                    iaasProviderType);
            for (NetworkInterface nwInterface : networkInterfaces) {
                log.debug("Interface " + nwInterface.toString());
            }
        }
    }

    private static void logProperties(String iaasProviderType, Map<String, String> properties,
                                      String cartridgeType, String partitionId) {

        if (properties != null) {
            log.debug("Properties defined in IaasProvider object for type: " +
                    iaasProviderType + ", cartridge type: " + cartridgeType + ", partition: " +
                    partitionId);
            for (Map.Entry<String, String> property : properties.entrySet()) {
                log.debug("Property key: " + property.getKey() + ", value: " +
                        property.getValue());
            }
        }
    }
}

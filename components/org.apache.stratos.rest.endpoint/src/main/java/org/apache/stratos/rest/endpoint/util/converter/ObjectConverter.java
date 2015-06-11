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

package org.apache.stratos.rest.endpoint.util.converter;

import org.apache.commons.lang.StringUtils;
import org.apache.stratos.autoscaler.stub.deployment.policy.ApplicationPolicy;
import org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.stub.partition.NetworkPartitionRef;
import org.apache.stratos.autoscaler.stub.partition.PartitionRef;
import org.apache.stratos.autoscaler.stub.pojo.*;
import org.apache.stratos.autoscaler.stub.pojo.Dependencies;
import org.apache.stratos.autoscaler.stub.pojo.ServiceGroup;
import org.apache.stratos.cloud.controller.stub.domain.*;
import org.apache.stratos.common.beans.application.*;
import org.apache.stratos.common.beans.application.domain.mapping.DomainMappingBean;
import org.apache.stratos.common.beans.application.signup.ApplicationSignUpBean;
import org.apache.stratos.common.beans.artifact.repository.ArtifactRepositoryBean;
import org.apache.stratos.common.beans.cartridge.*;
import org.apache.stratos.common.beans.kubernetes.*;
import org.apache.stratos.common.beans.partition.NetworkPartitionBean;
import org.apache.stratos.common.beans.partition.NetworkPartitionReferenceBean;
import org.apache.stratos.common.beans.partition.PartitionBean;
import org.apache.stratos.common.beans.partition.PartitionReferenceBean;
import org.apache.stratos.common.beans.policy.autoscale.*;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.common.beans.policy.deployment.DeploymentPolicyBean;
import org.apache.stratos.common.beans.topology.*;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.manager.service.stub.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.manager.service.stub.domain.application.signup.ArtifactRepository;
import org.apache.stratos.manager.service.stub.domain.application.signup.DomainMapping;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.Group;
import org.apache.stratos.messaging.domain.instance.ApplicationInstance;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.KubernetesService;
import org.apache.stratos.messaging.domain.topology.Port;
import org.apache.stratos.rest.endpoint.exception.ServiceGroupDefinitionException;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;

import java.util.*;

public class ObjectConverter {

    public static Cartridge convertCartridgeBeanToStubCartridgeConfig(
            CartridgeBean cartridgeBean) {

        if (cartridgeBean == null) {
            return null;
        }

        Cartridge cartridge = new Cartridge();

        cartridge.setType(cartridgeBean.getType());
        cartridge.setHostName(cartridgeBean.getHost());
        cartridge.setProvider(cartridgeBean.getProvider());
        cartridge.setCategory(cartridgeBean.getCategory());
        cartridge.setVersion(cartridgeBean.getVersion());
        cartridge.setMultiTenant(cartridgeBean.isMultiTenant());
        cartridge.setDisplayName(cartridgeBean.getDisplayName());
        cartridge.setDescription(cartridgeBean.getDescription());
        cartridge.setLoadBalancingIPType(cartridgeBean.getLoadBalancingIPType());
        if (cartridgeBean.getMetadataKeys() != null) {
            cartridge.setMetadataKeys(cartridgeBean.getMetadataKeys().
                    toArray(new String[cartridgeBean.getMetadataKeys().size()]));
        }
        // deployment information
        if (cartridgeBean.getDeployment() != null) {
            cartridge.setBaseDir(cartridgeBean.getDeployment().getBaseDir());
            if (cartridgeBean.getDeployment().getDir() != null && !cartridgeBean.getDeployment().getDir().isEmpty()) {
                cartridge.setDeploymentDirs(cartridgeBean.getDeployment().getDir().
                        toArray(new String[cartridgeBean.getDeployment().getDir().size()]));
            }
        }
        // port mapping
        if (cartridgeBean.getPortMapping() != null && !cartridgeBean.getPortMapping().isEmpty()) {
            cartridge.setPortMappings(convertPortMappingBeansToStubPortMappings(cartridgeBean.getPortMapping()));
        }

        // persistence mapping
        if (cartridgeBean.getPersistence() != null) {
            cartridge.setPersistence(convertPersistenceBeanToStubPersistence(cartridgeBean.getPersistence()));
        }

        // iaas providers
        if (cartridgeBean.getIaasProvider() != null && !cartridgeBean.getIaasProvider().isEmpty()) {
            cartridge.setIaasConfigs(convertIaasProviderBeansToStubIaasConfig(cartridgeBean.getIaasProvider()));
        }

        // properties
        if (cartridgeBean.getProperty() != null && !cartridgeBean.getProperty().isEmpty()) {
            cartridge.setProperties(convertPropertyBeansToCCStubProperties(cartridgeBean.getProperty()));
        }
        return cartridge;
    }

    private static PortMapping[] convertPortMappingBeansToStubPortMappings(List<PortMappingBean> portMappingBeans) {

        if (portMappingBeans == null) {
            return null;
        }

        //convert to an array
        PortMappingBean[] portMappingBeanArray = new PortMappingBean[portMappingBeans.size()];
        portMappingBeans.toArray(portMappingBeanArray);
        PortMapping[] portMappingArray = new PortMapping[portMappingBeanArray.length];

        for (int i = 0; i < portMappingBeanArray.length; i++) {
            PortMapping portMapping = new PortMapping();
            portMapping.setName(portMappingBeanArray[i].getName());
            portMapping.setProtocol(portMappingBeanArray[i].getProtocol());
            portMapping.setPort(portMappingBeanArray[i].getPort());
            portMapping.setProxyPort(portMappingBeanArray[i].getProxyPort());
            portMappingArray[i] = portMapping;
        }

        return portMappingArray;
    }

    private static List<PortMappingBean> convertPortMappingsToStubPortMappingBeans(
            PortMapping[] portMappingps) {

        if (portMappingps == null || portMappingps[0] == null) {
            return null;
        }
        List<PortMappingBean> portMappingBeans = new ArrayList<PortMappingBean>();

        for (PortMapping portMapping : portMappingps) {
            PortMappingBean portMappingBean = new PortMappingBean();
            portMappingBean.setName(portMapping.getName());
            portMappingBean.setProtocol(portMapping.getProtocol());
            portMappingBean.setPort(portMapping.getPort());
            portMappingBean.setProxyPort(portMapping.getProxyPort());
            portMappingBeans.add(portMappingBean);
        }
        return portMappingBeans;
    }

    /**
     * Convert Persistence To PersistenceBean
     *
     * @param iaasConfigs iaasConfigs
     * @return PersistenceBean
     */
    private static List<IaasProviderBean> convertIaaSProviderToIaaSProviderBean(IaasConfig[] iaasConfigs) {
        if (iaasConfigs == null || iaasConfigs[0] == null) {
            return null;
        }

        List<IaasProviderBean> iaasProviderBeans = new ArrayList<IaasProviderBean>();
        for (IaasConfig iaasConfig : iaasConfigs) {
            IaasProviderBean iaasProviderBean = new IaasProviderBean();
            iaasProviderBean.setType(iaasConfig.getType());
            iaasProviderBean.setImageId(iaasConfig.getImageId());
            iaasProviderBean.setName(iaasConfig.getName());
            iaasProviderBean.setClassName(iaasConfig.getClassName());
            iaasProviderBean.setCredential(iaasConfig.getCredential());
            iaasProviderBean.setIdentity(iaasConfig.getIdentity());
            iaasProviderBean.setProvider(iaasConfig.getProvider());

            if (iaasConfig.getProperties() != null) {
                //set the Properties instance to IaasConfig instance
                iaasProviderBean.setProperty(convertCCStubPropertiesToPropertyBeanList(
                        iaasConfig.getProperties()));
            }

            if (iaasConfig.getNetworkInterfaces() != null) {
                iaasProviderBean.setNetworkInterfaces(ObjectConverter.
                        convertNetworkInterfacesToNetworkInterfaceBeans(
                                iaasConfig.getNetworkInterfaces()));
            }

            iaasProviderBeans.add(iaasProviderBean);
        }

        return iaasProviderBeans;
    }

    private static IaasConfig[] convertIaasProviderBeansToStubIaasConfig(List<IaasProviderBean> iaasProviderBeans) {

        if (iaasProviderBeans == null) {
            return null;
        }

        //convert to an array
        IaasProviderBean[] iaasProviderBeansArray = new IaasProviderBean[iaasProviderBeans.size()];
        iaasProviderBeans.toArray(iaasProviderBeansArray);
        IaasConfig[] iaasConfigsArray = new IaasConfig[iaasProviderBeansArray.length];

        for (int i = 0; i < iaasProviderBeansArray.length; i++) {
            IaasConfig iaasConfig = new IaasConfig();
            iaasConfig.setType(iaasProviderBeansArray[i].getType());
            iaasConfig.setImageId(iaasProviderBeansArray[i].getImageId());
            iaasConfig.setName(iaasProviderBeansArray[i].getName());
            iaasConfig.setClassName(iaasProviderBeansArray[i].getClassName());
            iaasConfig.setCredential(iaasProviderBeansArray[i].getCredential());
            iaasConfig.setIdentity(iaasProviderBeansArray[i].getIdentity());
            iaasConfig.setProvider(iaasProviderBeansArray[i].getProvider());

            if (iaasProviderBeansArray[i].getProperty() != null && !iaasProviderBeansArray[i].getProperty().isEmpty()) {
                //set the Properties instance to IaasConfig instance
                iaasConfig.setProperties(convertPropertyBeansToCCStubProperties(iaasProviderBeansArray[i].getProperty()));
            }

            if (iaasProviderBeansArray[i].getNetworkInterfaces() != null
                    && !iaasProviderBeansArray[i].getNetworkInterfaces().isEmpty()) {
                iaasConfig.setNetworkInterfaces(ObjectConverter.convertNetworkInterfaceBeansToNetworkInterfaces(
                        iaasProviderBeansArray[i].getNetworkInterfaces()));
            }

            iaasConfigsArray[i] = iaasConfig;
        }
        return iaasConfigsArray;
    }

    private static Persistence convertPersistenceBeanToStubPersistence(
            org.apache.stratos.common.beans.cartridge.PersistenceBean persistenceBean) {

        if (persistenceBean == null) {
            return null;
        }

        Persistence persistence = new Persistence();
        persistence.setPersistenceRequired(true);
        VolumeBean[] volumeBean = new VolumeBean[persistenceBean.getVolume().size()];
        persistenceBean.getVolume().toArray(volumeBean);
        Volume[] volumes = new Volume[persistenceBean.getVolume().size()];
        for (int i = 0; i < volumes.length; i++) {
            Volume volume = new Volume();
            volume.setId(volumeBean[i].getId());
            volume.setVolumeId(volumeBean[i].getVolumeId());
            if (StringUtils.isEmpty(volume.getVolumeId())) {
                volume.setSize(Integer.parseInt(volumeBean[i].getSize()));
            }

            volume.setDevice(volumeBean[i].getDevice());
            volume.setRemoveOntermination(volumeBean[i].isRemoveOnTermination());
            volume.setMappingPath(volumeBean[i].getMappingPath());
            volume.setSnapshotId(volumeBean[i].getSnapshotId());

            volumes[i] = volume;
        }
        persistence.setVolumes(volumes);
        return persistence;

    }

    private static org.apache.stratos.cloud.controller.stub.Properties convertPropertyBeansToCCStubProperties(
            List<org.apache.stratos.common.beans.PropertyBean> propertyBeans) {

        if (propertyBeans == null) {
            return null;
        }

        List<org.apache.stratos.cloud.controller.stub.Property> stubPropertiesList =
                new ArrayList<org.apache.stratos.cloud.controller.stub.Property>();

        for (org.apache.stratos.common.beans.PropertyBean propertyBean : propertyBeans) {
            org.apache.stratos.cloud.controller.stub.Property stubProperty
                    = new org.apache.stratos.cloud.controller.stub.Property();
            stubProperty.setName(propertyBean.getName());
            stubProperty.setValue(propertyBean.getValue());
            stubPropertiesList.add(stubProperty);
        }

        org.apache.stratos.cloud.controller.stub.Properties stubProperties
                = new org.apache.stratos.cloud.controller.stub.Properties();
        org.apache.stratos.cloud.controller.stub.Property[] stubPropertiesArray =
                stubPropertiesList.toArray(new org.apache.stratos.cloud.controller.stub.Property[stubPropertiesList.size()]);
        stubProperties.setProperties(stubPropertiesArray);

        return stubProperties;
    }

    private static NetworkInterfaces convertNetworkInterfaceBeansToNetworkInterfaces(
            List<NetworkInterfaceBean> networkInterfaceBeans) {

        if (networkInterfaceBeans == null) {
            return null;
        }

        NetworkInterface[] networkInterfacesArray = new NetworkInterface[networkInterfaceBeans.size()];

        int i = 0;
        for (NetworkInterfaceBean nib : networkInterfaceBeans) {
            NetworkInterface networkInterface = new NetworkInterface();
            networkInterface.setNetworkUuid(nib.getNetworkUuid());
            networkInterface.setFixedIp(nib.getFixedIp());
            networkInterface.setPortUuid(nib.getPortUuid());
            if (nib.getFloatingNetworks() != null && !nib.getFloatingNetworks().isEmpty()) {
                networkInterface.setFloatingNetworks(
                        ObjectConverter.convertFloatingNetworkBeansToFloatingNetworks(nib.getFloatingNetworks()));
            }

            networkInterfacesArray[i++] = networkInterface;
        }

        NetworkInterfaces networkInterfaces = new NetworkInterfaces();
        networkInterfaces.setNetworkInterfaces(networkInterfacesArray);
        return networkInterfaces;
    }

    private static List<NetworkInterfaceBean> convertNetworkInterfacesToNetworkInterfaceBeans(
            NetworkInterfaces networkInterfaces) {

        if (networkInterfaces == null || networkInterfaces.getNetworkInterfaces() == null ||
                networkInterfaces.getNetworkInterfaces()[0] == null) {
            return null;
        }

        List<NetworkInterfaceBean> networkInterfaceBeans = new ArrayList<NetworkInterfaceBean>();

        for (NetworkInterface networkInterface : networkInterfaces.getNetworkInterfaces()) {
            NetworkInterfaceBean networkInterfaceBean = new NetworkInterfaceBean();
            networkInterfaceBean.setNetworkUuid(networkInterface.getNetworkUuid());
            networkInterfaceBean.setFixedIp(networkInterface.getFixedIp());
            networkInterfaceBean.setPortUuid(networkInterface.getPortUuid());
            if (networkInterface.getFloatingNetworks() != null &&
                    networkInterface.getFloatingNetworks().getFloatingNetworks() != null &&
                    networkInterface.getFloatingNetworks().getFloatingNetworks()[0] != null) {
                networkInterfaceBean.setFloatingNetworks(
                        ObjectConverter.convertFloatingNetworksToFloatingNetworkBeans(
                                networkInterface.getFloatingNetworks()));
            }
            networkInterfaceBeans.add(networkInterfaceBean);
        }
        return networkInterfaceBeans;
    }

    private static FloatingNetworks convertFloatingNetworkBeansToFloatingNetworks(
            List<FloatingNetworkBean> floatingNetworkBeans) {

        if (floatingNetworkBeans == null) {
            return null;
        }

        FloatingNetwork[] floatingNetworksArray = new FloatingNetwork[floatingNetworkBeans.size()];

        int i = 0;
        for (FloatingNetworkBean floatingNetworkBean : floatingNetworkBeans) {
            FloatingNetwork floatingNetwork = new FloatingNetwork();
            floatingNetwork.setName(floatingNetworkBean.getName());
            floatingNetwork.setNetworkUuid(floatingNetworkBean.getNetworkUuid());
            floatingNetwork.setFloatingIP(floatingNetworkBean.getFloatingIP());
            floatingNetworksArray[i++] = floatingNetwork;
        }

        FloatingNetworks floatingNetworks = new FloatingNetworks();
        floatingNetworks.setFloatingNetworks(floatingNetworksArray);
        return floatingNetworks;
    }

    private static List<FloatingNetworkBean> convertFloatingNetworksToFloatingNetworkBeans(
            FloatingNetworks floatingNetworks) {
        List<FloatingNetworkBean> floatingNetworkBeans = new ArrayList<FloatingNetworkBean>();

        for (FloatingNetwork floatingNetwork : floatingNetworks.getFloatingNetworks()) {
            FloatingNetworkBean floatingNetworkBean = new FloatingNetworkBean();
            floatingNetworkBean.setName(floatingNetwork.getName());
            floatingNetworkBean.setNetworkUuid(floatingNetwork.getNetworkUuid());
            floatingNetworkBean.setFloatingIP(floatingNetwork.getFloatingIP());
            floatingNetworkBeans.add(floatingNetworkBean);
        }
        return floatingNetworkBeans;
    }

    private static org.apache.stratos.cloud.controller.stub.domain.Partition convertPartitionToStubPartition
            (PartitionBean partition) {

        if (partition == null) {
            return null;
        }

        org.apache.stratos.cloud.controller.stub.domain.Partition stubPartition = new
                org.apache.stratos.cloud.controller.stub.domain.Partition();
        stubPartition.setId(partition.getId());
        stubPartition.setProperties(convertPropertyBeansToCCStubProperties(partition.getProperty()));

        return stubPartition;
    }

    public static org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy convertToCCAutoscalerPojo(
            AutoscalePolicyBean autoscalePolicyBean) {

        if (autoscalePolicyBean == null) {
            return null;
        }

        org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy autoscalePolicy = new
                org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy();

        autoscalePolicy.setId(autoscalePolicyBean.getId());
        autoscalePolicy.setDescription(autoscalePolicyBean.getDescription());
        autoscalePolicy.setDisplayName(autoscalePolicyBean.getDisplayName());

        if (autoscalePolicyBean.getLoadThresholds() != null) {

            org.apache.stratos.autoscaler.stub.autoscale.policy.LoadThresholds loadThresholds = new
                    org.apache.stratos.autoscaler.stub.autoscale.policy.LoadThresholds();

            if (autoscalePolicyBean.getLoadThresholds().getLoadAverage() != null) {

                //set load average information
                loadThresholds.setLoadAverageThreshold(
                        autoscalePolicyBean.getLoadThresholds().getLoadAverage().getThreshold());
            }
            if (autoscalePolicyBean.getLoadThresholds().getRequestsInFlight() != null) {

                //set request in flight information
                loadThresholds.setRequestsInFlightThreshold(
                        autoscalePolicyBean.getLoadThresholds().getRequestsInFlight().getThreshold());
            }
            if (autoscalePolicyBean.getLoadThresholds().getMemoryConsumption() != null) {

                //set memory consumption information
                loadThresholds.setMemoryConsumptionThreshold(
                        autoscalePolicyBean.getLoadThresholds().
                                getMemoryConsumption().getThreshold());
            }

            autoscalePolicy.setLoadThresholds(loadThresholds);
        }

        return autoscalePolicy;
    }

    public static NetworkPartitionBean convertCCStubNetworkPartitionToNetworkPartition(
            org.apache.stratos.cloud.controller.stub.domain.NetworkPartition stubNetworkPartition) {
        if (stubNetworkPartition == null) {
            return null;
        }

        NetworkPartitionBean networkPartition = new NetworkPartitionBean();
        networkPartition.setId(stubNetworkPartition.getId());
        networkPartition.setProvider(stubNetworkPartition.getProvider());
        if (stubNetworkPartition.getPartitions() != null) {
            List<PartitionBean> partitionList = new ArrayList<PartitionBean>();
            for (org.apache.stratos.cloud.controller.stub.domain.Partition stubPartition : stubNetworkPartition.getPartitions()) {
                if (stubPartition != null) {
                    partitionList.add(convertCCStubPartitionToPartition(stubPartition));
                }
            }
            networkPartition.setPartitions(partitionList);
        }

        if (stubNetworkPartition.getProperties() != null) {
            List<org.apache.stratos.common.beans.PropertyBean> propertyBeanList
                    = new ArrayList<org.apache.stratos.common.beans.PropertyBean>();
            if (stubNetworkPartition.getProperties() != null) {
                if (stubNetworkPartition.getProperties().getProperties() != null) {
                    for (org.apache.stratos.cloud.controller.stub.Property stubProperty :
                            stubNetworkPartition.getProperties().getProperties()) {
                        if (stubProperty != null) {
                            propertyBeanList.add(convertStubPropertyToPropertyBean(stubProperty));
                        }
                    }
                }
            }
            networkPartition.setProperties(propertyBeanList);
        }

        return networkPartition;
    }

    public static ApplicationPolicyBean convertASStubApplicationPolicyToApplicationPolicy(ApplicationPolicy applicationPolicy) {

        if (applicationPolicy == null) {
            return null;
        }

        ApplicationPolicyBean applicationPolicyBean = new ApplicationPolicyBean();
        applicationPolicyBean.setId(applicationPolicy.getId());
        applicationPolicyBean.setAlgorithm(applicationPolicy.getAlgorithm());
        applicationPolicyBean.setNetworkPartitions(applicationPolicy.getNetworkPartitions());

        if (applicationPolicy.getProperties() != null) {
            List<org.apache.stratos.common.beans.PropertyBean> propertyBeanList
                    = new ArrayList<org.apache.stratos.common.beans.PropertyBean>();
            for (org.apache.stratos.autoscaler.stub.Property stubProperty : applicationPolicy.getProperties().getProperties()) {
                if (stubProperty != null) {
                    org.apache.stratos.common.beans.PropertyBean propertyBean
                            = new org.apache.stratos.common.beans.PropertyBean();
                    propertyBean.setName(stubProperty.getName());
                    propertyBean.setValue(String.valueOf(stubProperty.getValue()));
                    propertyBeanList.add(propertyBean);
                }
            }
            applicationPolicyBean.setProperties(propertyBeanList);
        }

        return applicationPolicyBean;
    }

    public static ApplicationPolicyBean[] convertASStubApplicationPoliciesToApplicationPolicies(
            ApplicationPolicy[] applicationPolicies) {

        ApplicationPolicyBean[] applicationPolicyBeans;
        if (applicationPolicies == null) {
            applicationPolicyBeans = new ApplicationPolicyBean[0];
            return applicationPolicyBeans;
        }

        applicationPolicyBeans = new ApplicationPolicyBean[applicationPolicies.length];
        for (int i = 0; i < applicationPolicies.length; i++) {
            applicationPolicyBeans[i] = convertASStubApplicationPolicyToApplicationPolicy(applicationPolicies[i]);
        }
        return applicationPolicyBeans;
    }

    private static org.apache.stratos.autoscaler.stub.Properties getASPropertiesFromCommonProperties(
            List<org.apache.stratos.common.beans.PropertyBean> propertyBeans) {

        if (propertyBeans == null || propertyBeans.isEmpty()) {
            return null;
        }

        //convert to an array
        org.apache.stratos.common.beans.PropertyBean[] propertyBeansArray
                = new org.apache.stratos.common.beans.PropertyBean[propertyBeans.size()];
        propertyBeans.toArray(propertyBeansArray);
        org.apache.stratos.autoscaler.stub.Property[] propertyArray =
                new org.apache.stratos.autoscaler.stub.Property[propertyBeansArray.length];

        for (int j = 0; j < propertyBeansArray.length; j++) {
            org.apache.stratos.autoscaler.stub.Property property = new org.apache.stratos.autoscaler.stub.Property();
            property.setName(propertyBeansArray[j].getName());
            property.setValue(propertyBeansArray[j].getValue());
            propertyArray[j] = property;
        }

        org.apache.stratos.autoscaler.stub.Properties properties = new org.apache.stratos.autoscaler.stub.Properties();
        properties.setProperties(propertyArray);
        return properties;
    }

    private static PartitionBean convertCCStubPartitionToPartition(org.apache.stratos.cloud.controller.stub.domain.Partition stubPartition) {

        if (stubPartition == null) {
            return null;
        }
        PartitionBean partition = new PartitionBean();
        partition.setId(stubPartition.getId());
        partition.setDescription(stubPartition.getDescription());
        if (stubPartition.getProperties() != null) {
            List<org.apache.stratos.common.beans.PropertyBean> propertyBeanList
                    = new ArrayList<org.apache.stratos.common.beans.PropertyBean>();
            if (stubPartition.getProperties().getProperties() != null) {
                for (org.apache.stratos.cloud.controller.stub.Property stubProperty :
                        stubPartition.getProperties().getProperties()) {
                    if (stubProperty != null) {
                        propertyBeanList.add(convertStubPropertyToPropertyBean(stubProperty));
                    }
                }
            }
            partition.setProperty(propertyBeanList);
        }
        return partition;
    }


    public static org.apache.stratos.cloud.controller.stub.domain.NetworkPartition
    convertNetworkPartitionToCCStubNetworkPartition(NetworkPartitionBean networkPartitionBean) {

        org.apache.stratos.cloud.controller.stub.domain.NetworkPartition networkPartition
                = new org.apache.stratos.cloud.controller.stub.domain.NetworkPartition();
        networkPartition.setId(networkPartitionBean.getId());
        networkPartition.setProvider(networkPartitionBean.getProvider());
        if (networkPartitionBean.getPartitions() != null && !networkPartitionBean.getPartitions().isEmpty()) {
            networkPartition.setPartitions(convertToStubPartitions(networkPartitionBean.getPartitions()));
        }
        if (networkPartitionBean.getProperties() != null && !networkPartitionBean.getProperties().isEmpty()) {
            networkPartition.setProperties(convertPropertyBeansToCCStubProperties(networkPartitionBean.getProperties()));
        }
        return networkPartition;
    }

    private static List<NetworkPartitionReferenceBean> convertASStubNetworkPartitionsToNetworkPartitionReferences(
            NetworkPartitionRef[] networkPartitions) {

        List<NetworkPartitionReferenceBean> networkPartitionBeans = new ArrayList<NetworkPartitionReferenceBean>();
        for (NetworkPartitionRef networkPartitionRef : networkPartitions) {
            NetworkPartitionReferenceBean networkPartitionReferenceBean = new NetworkPartitionReferenceBean();
            networkPartitionReferenceBean.setId(networkPartitionRef.getId());
            networkPartitionReferenceBean.setPartitionAlgo(networkPartitionRef.getPartitionAlgo());
            networkPartitionReferenceBean.setPartitions(
                    convertASStubPartitionsToPartitions(networkPartitionRef.getPartitionRefs()));
            networkPartitionBeans.add(networkPartitionReferenceBean);
        }

        return networkPartitionBeans;
    }

    public static ClusterBean convertClusterToClusterBean(Cluster cluster, String alias) {
        ClusterBean clusterBean = new
                ClusterBean();
        clusterBean.setAlias(alias);
        clusterBean.setServiceName(cluster.getServiceName());
        clusterBean.setClusterId(cluster.getClusterId());
        clusterBean.setLbCluster(cluster.isLbCluster());
        clusterBean.setTenantRange(cluster.getTenantRange());
        clusterBean.setProperty(convertJavaUtilPropertiesToPropertyBeans(cluster.getProperties()));
        clusterBean.setMember(new ArrayList<MemberBean>());
        clusterBean.setHostNames(new ArrayList<String>());
        Collection<ClusterInstance> clusterInstances = cluster.getClusterInstances();
        List<InstanceBean> instancesList =
                new ArrayList<InstanceBean>();
        if (clusterInstances != null) {
            for (ClusterInstance clusterInstance : clusterInstances) {
                InstanceBean instance =
                        new InstanceBean();
                instance.setInstanceId(clusterInstance.getInstanceId());
                instance.setStatus(clusterInstance.getStatus().toString());
                instancesList.add(instance);
            }
            clusterBean.setInstances(instancesList);
        }

        for (org.apache.stratos.messaging.domain.topology.Member member : cluster.getMembers()) {
            MemberBean memberBean = new MemberBean();
            memberBean.setServiceName(member.getServiceName());
            memberBean.setClusterId(member.getClusterId());
            memberBean.setMemberId(member.getMemberId());
            memberBean.setClusterInstanceId(member.getClusterInstanceId());

            memberBean.setLbClusterId(member.getLbClusterId());
            memberBean.setNetworkPartitionId(member.getNetworkPartitionId());
            memberBean.setPartitionId(member.getPartitionId());
            if (member.getDefaultPrivateIP() == null) {
                memberBean.setDefaultPrivateIP("NULL");
            } else {
                memberBean.setDefaultPrivateIP(member.getDefaultPrivateIP());
            }
            if (member.getDefaultPublicIP() == null) {
                memberBean.setDefaultPublicIP("NULL");
            } else {
                memberBean.setDefaultPublicIP(member.getDefaultPublicIP());
            }
            memberBean.setMemberPrivateIPs(member.getMemberPrivateIPs());
            memberBean.setMemberPublicIPs(member.getMemberPublicIPs());
            memberBean.setStatus(member.getStatus().toString());
            memberBean.setProperty(convertJavaUtilPropertiesToPropertyBeans(member.getProperties()));
            clusterBean.getMember().add(memberBean);
        }

        for (String hostname : cluster.getHostNames()) {
            clusterBean.getHostNames().add(hostname);
        }
        return clusterBean;
    }

    public static ClusterInstanceBean convertClusterToClusterInstanceBean(String instanceId,
                                                                          Cluster cluster, String alias) {
        ClusterInstanceBean clusterInstanceBean = new ClusterInstanceBean();
        clusterInstanceBean.setAlias(alias);
        clusterInstanceBean.setServiceName(cluster.getServiceName());
        clusterInstanceBean.setClusterId(cluster.getClusterId());
        clusterInstanceBean.setInstanceId(instanceId);
        clusterInstanceBean.setParentInstanceId(instanceId);
        if (cluster.getInstanceContexts(instanceId) != null) {
            clusterInstanceBean.setStatus(cluster.getInstanceContexts(instanceId).
                    getStatus().toString());
        }
        clusterInstanceBean.setTenantRange(cluster.getTenantRange());
        clusterInstanceBean.setMember(new ArrayList<MemberBean>());
        clusterInstanceBean.setHostNames(new ArrayList<String>());


        for (org.apache.stratos.messaging.domain.topology.Member member : cluster.getMembers()) {
            if (member.getClusterInstanceId().equals(instanceId)) {
                MemberBean memberBean = new MemberBean();
                memberBean.setClusterId(member.getClusterId());
                memberBean.setLbClusterId(member.getLbClusterId());
                memberBean.setNetworkPartitionId(member.getNetworkPartitionId());
                memberBean.setPartitionId(member.getPartitionId());
                memberBean.setMemberId(member.getMemberId());
                memberBean.setDefaultPrivateIP(member.getDefaultPrivateIP());
                memberBean.setDefaultPublicIP(member.getDefaultPublicIP());
                memberBean.setMemberPrivateIPs(member.getMemberPrivateIPs());
                memberBean.setMemberPublicIPs(member.getMemberPublicIPs());
                memberBean.setPorts(convertStubPortsToPortMappingBeans(member.getPorts()));
                memberBean.setServiceName(member.getServiceName());
                memberBean.setStatus(member.getStatus().toString());
                memberBean.setProperty(convertJavaUtilPropertiesToPropertyBeans(member.getProperties()));

                clusterInstanceBean.getMember().add(memberBean);
            }
        }
        clusterInstanceBean.setAccessUrls(cluster.getAccessUrls());
        for (String hostname : cluster.getHostNames()) {
            clusterInstanceBean.getHostNames().add(hostname);
        }
        clusterInstanceBean.setKubernetesServices(convertKubernetesServiceToKubernetesServiceBean(
                cluster.getKubernetesServices()));
        return clusterInstanceBean;
    }

    private static List<PortMappingBean> convertStubPortsToPortMappingBeans(Collection<Port> ports) {
        List<PortMappingBean> portMappingBeans = new ArrayList<PortMappingBean>();
        if (ports != null) {
            for (Port port : ports) {
                PortMappingBean portMappingBean = new PortMappingBean();
                portMappingBean.setProtocol(port.getProtocol());
                portMappingBean.setPort(port.getValue());
                portMappingBean.setProxyPort(port.getProxy());

                portMappingBeans.add(portMappingBean);
            }
        }
        return portMappingBeans;
    }

    private static List<KubernetesServiceBean> convertKubernetesServiceToKubernetesServiceBean(
            List<KubernetesService> kubernetesServices) {
        List<KubernetesServiceBean> kubernetesServiceBeans = new ArrayList<KubernetesServiceBean>();
        if (kubernetesServices != null) {
            for (KubernetesService kubernetesService : kubernetesServices) {

                KubernetesServiceBean kubernetesServiceBean = new KubernetesServiceBean();
                kubernetesServiceBean.setId(kubernetesService.getId());
                kubernetesServiceBean.setPublicIPs(kubernetesService.getPublicIPs());
                kubernetesServiceBean.setPortalIP(kubernetesService.getPortalIP());
                kubernetesServiceBean.setProtocol(kubernetesService.getProtocol());
                kubernetesServiceBean.setPort(kubernetesService.getPort());
                kubernetesServiceBean.setContainerPort(kubernetesService.getContainerPort());

                kubernetesServiceBeans.add(kubernetesServiceBean);
            }
        }
        return kubernetesServiceBeans;
    }

    private static org.apache.stratos.cloud.controller.stub.domain.Partition[] convertToStubPartitions
            (List<PartitionBean> partitionList) {

        org.apache.stratos.cloud.controller.stub.domain.Partition[] partitions
                = new org.apache.stratos.cloud.controller.stub.domain.Partition[partitionList.size()];
        for (int i = 0; i < partitionList.size(); i++) {
            partitions[i] = convertPartitionToStubPartition(partitionList.get(i));
        }
        return partitions;
    }


    private static List<PartitionReferenceBean> convertASStubPartitionsToPartitions(
            PartitionRef[] partitions) {

        List<PartitionReferenceBean> partitionBeans = new ArrayList<PartitionReferenceBean>();
        for (PartitionRef partition : partitions) {
            PartitionReferenceBean partitionBean = new PartitionReferenceBean();
            partitionBean.setId(partition.getId());
            partitionBean.setPartitionMax(partition.getPartitionMax());
            partitionBeans.add(partitionBean);
        }
        return partitionBeans;
    }

    public static PartitionBean[] populatePartitionPojos(org.apache.stratos.cloud.controller.stub.domain.Partition[]
                                                                 partitions) {

        PartitionBean[] partitionBeans;
        if (partitions == null) {
            partitionBeans = new PartitionBean[0];
            return partitionBeans;
        }

        partitionBeans = new PartitionBean[partitions.length];
        for (int i = 0; i < partitions.length; i++) {
            partitionBeans[i] = populatePartitionPojo(partitions[i]);
        }
        return partitionBeans;
    }

    public static PartitionBean populatePartitionPojo(org.apache.stratos.cloud.controller.stub.domain.Partition partition) {

        PartitionBean partitionBeans = new PartitionBean();
        if (partition == null) {
            return partitionBeans;
        }

        partitionBeans.setId(partition.getId());
        partitionBeans.setDescription(partition.getDescription());
        //properties
        if (partition.getProperties() != null) {
            List<org.apache.stratos.common.beans.PropertyBean> propertyBeans
                    = convertCCStubPropertiesToPropertyBeans(partition.getProperties());
            partitionBeans.setProperty(propertyBeans);
        }
        return partitionBeans;
    }

    private static List<org.apache.stratos.common.beans.PropertyBean> convertJavaUtilPropertiesToPropertyBeans(
            java.util.Properties properties) {

        List<org.apache.stratos.common.beans.PropertyBean> propertyBeans = null;
        if (properties != null && !properties.isEmpty()) {
            Enumeration<?> e = properties.propertyNames();
            propertyBeans = new ArrayList<org.apache.stratos.common.beans.PropertyBean>();

            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String value = properties.getProperty(key);
                org.apache.stratos.common.beans.PropertyBean propertyBean
                        = new org.apache.stratos.common.beans.PropertyBean();
                propertyBean.setName(key);
                propertyBean.setValue(value);
                propertyBeans.add(propertyBean);
            }
        }
        return propertyBeans;
    }

    public static AutoscalePolicyBean[] convertStubAutoscalePoliciesToAutoscalePolicies(
            org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy[] autoscalePolicies) {

        AutoscalePolicyBean[] autoscalePolicyBeans;
        if (autoscalePolicies == null) {
            autoscalePolicyBeans = new AutoscalePolicyBean[0];
            return autoscalePolicyBeans;
        }

        autoscalePolicyBeans = new AutoscalePolicyBean[autoscalePolicies.length];
        for (int i = 0; i < autoscalePolicies.length; i++) {
            autoscalePolicyBeans[i] = convertStubAutoscalePolicyToAutoscalePolicy(autoscalePolicies[i]);
        }
        return autoscalePolicyBeans;
    }

    public static AutoscalePolicyBean convertStubAutoscalePolicyToAutoscalePolicy(
            org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy autoscalePolicy) {
        if (autoscalePolicy == null) {
            return null;
        }

        AutoscalePolicyBean autoscalePolicyBean = new AutoscalePolicyBean();
        autoscalePolicyBean.setId(autoscalePolicy.getId());
        autoscalePolicyBean.setDescription(autoscalePolicy.getDescription());
        autoscalePolicyBean.setDisplayName(autoscalePolicy.getDisplayName());
        autoscalePolicyBean.setDescription(autoscalePolicy.getDescription());
        if (autoscalePolicy.getLoadThresholds() != null) {
            autoscalePolicyBean.setLoadThresholds(convertStubLoadThresholdsToLoadThresholds(autoscalePolicy.getLoadThresholds()));
        }

        return autoscalePolicyBean;
    }

    private static LoadThresholdsBean convertStubLoadThresholdsToLoadThresholds(
            org.apache.stratos.autoscaler.stub.autoscale.policy.LoadThresholds loadThresholds) {

        LoadThresholdsBean loadThresholdBean = new LoadThresholdsBean();
        if (loadThresholds.getLoadAverageThreshold() != 0) {
            LoadAverageThresholdsBean loadAverage = new LoadAverageThresholdsBean();
            loadAverage.setThreshold(loadThresholds.getLoadAverageThreshold());
            loadThresholdBean.setLoadAverage(loadAverage);
        }
        if (loadThresholds.getMemoryConsumptionThreshold() != 0) {
            MemoryConsumptionThresholdsBean memoryConsumption = new MemoryConsumptionThresholdsBean();
            memoryConsumption.setThreshold(loadThresholds.getMemoryConsumptionThreshold());
            loadThresholdBean.setMemoryConsumption(memoryConsumption);
        }
        if (loadThresholds.getRequestsInFlightThreshold() != 0) {
            RequestsInFlightThresholdsBean requestsInFlight = new RequestsInFlightThresholdsBean();
            requestsInFlight.setThreshold(loadThresholds.getRequestsInFlightThreshold());
            loadThresholdBean.setRequestsInFlight(requestsInFlight);
        }

        return loadThresholdBean;
    }

    public static NetworkPartitionBean[] convertCCStubNetworkPartitionsToNetworkPartitions(
            org.apache.stratos.cloud.controller.stub.domain.NetworkPartition[] networkPartitions) {

        NetworkPartitionBean[] networkPartitionGroupsBeans;
        if (networkPartitions == null) {
            networkPartitionGroupsBeans = new NetworkPartitionBean[0];
            return networkPartitionGroupsBeans;
        }

        networkPartitionGroupsBeans = new NetworkPartitionBean[networkPartitions.length];

        for (int i = 0; i < networkPartitions.length; i++) {
            networkPartitionGroupsBeans[i] = convertCCStubNetworkPartitionToNetworkPartition(networkPartitions[i]);
        }

        return networkPartitionGroupsBeans;
    }

    public static org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesCluster
    convertToCCKubernetesClusterPojo(KubernetesClusterBean kubernetesClusterBean) {

        org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesCluster kubernetesCluster = new
                org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesCluster();

        kubernetesCluster.setClusterId(kubernetesClusterBean.getClusterId());
        kubernetesCluster.setDescription(kubernetesClusterBean.getDescription());
        kubernetesCluster.setKubernetesMaster(convertStubKubernetesMasterToKubernetesMaster(
                kubernetesClusterBean.getKubernetesMaster()));
        kubernetesCluster.setPortRange(convertPortRangeToStubPortRange(kubernetesClusterBean.getPortRange()));
        kubernetesCluster.setKubernetesHosts(convertToASKubernetesHostsPojo(kubernetesClusterBean.getKubernetesHosts()));
        kubernetesCluster.setProperties((convertPropertyBeansToCCStubProperties(kubernetesClusterBean.getProperty())));

        return kubernetesCluster;
    }

    private static org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost[]
    convertToASKubernetesHostsPojo(List<KubernetesHostBean> kubernetesHosts) {

        if (kubernetesHosts == null || kubernetesHosts.isEmpty()) {
            return null;
        }
        int kubernetesHostCount = kubernetesHosts.size();
        org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost[] kubernetesHostsArr
                = new org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost[kubernetesHostCount];
        for (int i = 0; i < kubernetesHostCount; i++) {
            KubernetesHostBean kubernetesHostBean = kubernetesHosts.get(i);
            kubernetesHostsArr[i] = convertKubernetesHostToStubKubernetesHost(kubernetesHostBean);
        }
        return kubernetesHostsArr;
    }

    private static org.apache.stratos.cloud.controller.stub.domain.kubernetes.PortRange
    convertPortRangeToStubPortRange(PortRangeBean portRangeBean) {

        if (portRangeBean == null) {
            return null;
        }
        org.apache.stratos.cloud.controller.stub.domain.kubernetes.PortRange
                portRange = new org.apache.stratos.cloud.controller.stub.domain.kubernetes.PortRange();
        portRange.setLower(portRangeBean.getLower());
        portRange.setUpper(portRangeBean.getUpper());
        return portRange;
    }

    public static org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost
    convertKubernetesHostToStubKubernetesHost(KubernetesHostBean kubernetesHostBean) {

        if (kubernetesHostBean == null) {
            return null;
        }

        org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost
                kubernetesHost = new org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost();
        kubernetesHost.setHostId(kubernetesHostBean.getHostId());
        kubernetesHost.setPrivateIPAddress(kubernetesHostBean.getPrivateIPAddress());
        kubernetesHost.setPublicIPAddress(kubernetesHostBean.getPublicIPAddress());
        kubernetesHost.setHostname(kubernetesHostBean.getHostname());
        kubernetesHost.setProperties(convertPropertyBeansToCCStubProperties(kubernetesHostBean.getProperty()));

        return kubernetesHost;
    }

    public static org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesMaster
    convertStubKubernetesMasterToKubernetesMaster(KubernetesMasterBean kubernetesMasterBean) {

        if (kubernetesMasterBean == null) {
            return null;
        }

        org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesMaster
                kubernetesMaster = new org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesMaster();
        kubernetesMaster.setHostId(kubernetesMasterBean.getHostId());
        kubernetesMaster.setPrivateIPAddress(kubernetesMasterBean.getPrivateIPAddress());
        kubernetesMaster.setPublicIPAddress(kubernetesMasterBean.getPublicIPAddress());
        kubernetesMaster.setHostname(kubernetesMasterBean.getHostname());
        kubernetesMaster.setProperties(convertPropertyBeansToCCStubProperties(kubernetesMasterBean.getProperty()));

        return kubernetesMaster;
    }

    public static KubernetesClusterBean[] convertStubKubernetesClustersToKubernetesClusters(
            org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesCluster[] kubernetesClusters) {

        if (kubernetesClusters == null) {
            return null;
        }
        KubernetesClusterBean[] kubernetesClustersBean = new KubernetesClusterBean[kubernetesClusters.length];
        for (int i = 0; i < kubernetesClusters.length; i++) {
            kubernetesClustersBean[i] = convertStubKubernetesClusterToKubernetesCluster(kubernetesClusters[i]);
        }
        return kubernetesClustersBean;
    }

    public static KubernetesClusterBean convertStubKubernetesClusterToKubernetesCluster(
            org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesCluster kubernetesCluster) {
        if (kubernetesCluster == null) {
            return null;
        }
        KubernetesClusterBean kubernetesClusterBean = new KubernetesClusterBean();
        kubernetesClusterBean.setClusterId(kubernetesCluster.getClusterId());
        kubernetesClusterBean.setDescription(kubernetesCluster.getDescription());
        kubernetesClusterBean.setPortRange(convertStubPortRangeToPortRange(kubernetesCluster.getPortRange()));
        kubernetesClusterBean.setKubernetesHosts(convertStubKubernetesHostsToKubernetesHosts(kubernetesCluster.getKubernetesHosts()));
        kubernetesClusterBean.setKubernetesMaster(convertStubKubernetesMasterToKubernetesMaster(
                kubernetesCluster.getKubernetesMaster()));
        kubernetesClusterBean.setProperty(convertCCStubPropertiesToPropertyBeans(kubernetesCluster.getProperties()));
        return kubernetesClusterBean;
    }

    public static KubernetesMasterBean convertStubKubernetesMasterToKubernetesMaster(
            org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesMaster kubernetesMaster) {
        if (kubernetesMaster == null) {
            return null;
        }
        KubernetesMasterBean kubernetesMasterBean = new KubernetesMasterBean();
        kubernetesMasterBean.setHostId(kubernetesMaster.getHostId());
        kubernetesMasterBean.setHostname(kubernetesMaster.getHostname());
        kubernetesMasterBean.setPrivateIPAddress(kubernetesMaster.getPrivateIPAddress());
        kubernetesMasterBean.setPublicIPAddress(kubernetesMaster.getPublicIPAddress());
        kubernetesMasterBean.setProperty(convertCCStubPropertiesToPropertyBeans(kubernetesMaster.getProperties()));
        return kubernetesMasterBean;
    }

    public static List<KubernetesHostBean> convertStubKubernetesHostsToKubernetesHosts(
            org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost[] kubernetesHosts) {
        if (kubernetesHosts == null) {
            return null;
        }
        List<KubernetesHostBean> kubernetesHostList = new ArrayList<KubernetesHostBean>();
        for (org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost kubernetesHost : kubernetesHosts) {
            kubernetesHostList.add(convertStubKubernetesHostToKubernetesHost(kubernetesHost));
        }
        return kubernetesHostList;
    }

    private static KubernetesHostBean convertStubKubernetesHostToKubernetesHost(
            org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost kubernetesHost) {
        if (kubernetesHost == null) {
            return null;
        }
        KubernetesHostBean kubernetesHostBean = new KubernetesHostBean();
        kubernetesHostBean.setHostId(kubernetesHost.getHostId());
        kubernetesHostBean.setHostname(kubernetesHost.getHostname());
        kubernetesHostBean.setPrivateIPAddress(kubernetesHost.getPrivateIPAddress());
        kubernetesHostBean.setPublicIPAddress(kubernetesHost.getPublicIPAddress());
        kubernetesHostBean.setProperty(convertCCStubPropertiesToPropertyBeans(kubernetesHost.getProperties()));
        return kubernetesHostBean;
    }

    private static List<org.apache.stratos.common.beans.PropertyBean> convertCCStubPropertiesToPropertyBeans(
            org.apache.stratos.cloud.controller.stub.Properties properties) {
        if (properties == null || properties.getProperties() == null) {
            return null;
        }
        List<org.apache.stratos.common.beans.PropertyBean> propertyBeanList
                = new ArrayList<org.apache.stratos.common.beans.PropertyBean>();
        for (int i = 0; i < properties.getProperties().length; i++) {
            propertyBeanList.add(convertStubPropertyToPropertyBean(properties.getProperties()[i]));
        }
        return propertyBeanList;
    }

    private static org.apache.stratos.common.beans.PropertyBean convertStubPropertyToPropertyBean(
            org.apache.stratos.cloud.controller.stub.Property propertyE) {
        if (propertyE == null) {
            return null;
        }
        org.apache.stratos.common.beans.PropertyBean propertyBean
                = new org.apache.stratos.common.beans.PropertyBean();
        propertyBean.setName(propertyE.getName());
        propertyBean.setValue(propertyE.getValue());
        return propertyBean;
    }

    private static PortRangeBean convertStubPortRangeToPortRange(
            org.apache.stratos.cloud.controller.stub.domain.kubernetes.PortRange portRange) {
        if (portRange == null) {
            return null;
        }
        PortRangeBean portRangeBean = new PortRangeBean();
        portRangeBean.setUpper(portRange.getUpper());
        portRangeBean.setLower(portRange.getLower());
        return portRangeBean;
    }

    public static ApplicationContext convertApplicationDefinitionToStubApplicationContext(
            ApplicationBean applicationDefinition) {

        org.apache.stratos.autoscaler.stub.pojo.ApplicationContext applicationContext =
                new org.apache.stratos.autoscaler.stub.pojo.ApplicationContext();
        applicationContext.setApplicationId(applicationDefinition.getApplicationId());
        applicationContext.setAlias(applicationDefinition.getAlias());
        applicationContext.setMultiTenant(applicationDefinition.isMultiTenant());
        applicationContext.setName(applicationDefinition.getName());
        applicationContext.setDescription(applicationDefinition.getDescription());
        applicationContext.setStatus(applicationDefinition.getStatus());

        // convert and set components
        if (applicationDefinition.getComponents() != null) {
            org.apache.stratos.autoscaler.stub.pojo.ComponentContext componentContext =
                    new org.apache.stratos.autoscaler.stub.pojo.ComponentContext();

            // top level Groups
            if (applicationDefinition.getComponents().getGroups() != null) {
                componentContext.setGroupContexts(
                        convertGroupDefinitionsToStubGroupContexts(applicationDefinition.getComponents().getGroups()));
            }
            // top level dependency information
            if (applicationDefinition.getComponents().getDependencies() != null) {
                componentContext.setDependencyContext(
                        convertDependencyDefinitionsToDependencyContexts(applicationDefinition.getComponents().getDependencies()));
            }
            // top level cartridge context information
            if (applicationDefinition.getComponents().getCartridges() != null) {
                componentContext.setCartridgeContexts(
                        convertCartridgeReferenceBeansToStubCartridgeContexts(applicationDefinition.getComponents().getCartridges()));
            }
            applicationContext.setComponents(componentContext);
        }
        return applicationContext;
    }

    public static ApplicationBean convertStubApplicationContextToApplicationDefinition(
            ApplicationContext applicationContext) {
        if (applicationContext == null) {
            return null;
        }

        ApplicationBean applicationDefinition = new ApplicationBean();
        applicationDefinition.setApplicationId(applicationContext.getApplicationId());
        applicationDefinition.setAlias(applicationContext.getAlias());
        applicationDefinition.setMultiTenant(applicationContext.getMultiTenant());
        applicationDefinition.setName(applicationContext.getName());
        applicationDefinition.setDescription(applicationContext.getDescription());
        applicationDefinition.setStatus(applicationContext.getStatus());

        // convert and set components
        if (applicationContext.getComponents() != null) {
            applicationDefinition.setComponents(new ComponentBean());
            // top level Groups
            if (applicationContext.getComponents().getGroupContexts() != null) {
                applicationDefinition.getComponents().setGroups(
                        convertStubGroupContextsToGroupDefinitions(applicationContext.getComponents().getGroupContexts()));
            }
            // top level dependency information
            if (applicationContext.getComponents().getDependencyContext() != null) {
                applicationDefinition.getComponents().setDependencies(
                        convertStubDependencyContextsToDependencyDefinitions(applicationContext.getComponents().getDependencyContext()));
            }
            // top level cartridge context information
            if (applicationContext.getComponents().getCartridgeContexts() != null) {
                applicationDefinition.getComponents().setCartridges(
                        convertStubCartridgeContextsToCartridgeReferenceBeans(applicationContext.getComponents().getCartridgeContexts()));
            }
        }
        return applicationDefinition;
    }

    private static List<CartridgeGroupReferenceBean> convertStubGroupContextsToGroupDefinitions(GroupContext[] groupContexts) {
        List<CartridgeGroupReferenceBean> groupDefinitions = new ArrayList<CartridgeGroupReferenceBean>();
        if (groupContexts != null) {
            for (GroupContext groupContext : groupContexts) {
                if (groupContext != null) {
                    CartridgeGroupReferenceBean groupDefinition = new CartridgeGroupReferenceBean();
                    groupDefinition.setAlias(groupContext.getAlias());
                    groupDefinition.setGroupMaxInstances(groupContext.getGroupMaxInstances());
                    groupDefinition.setGroupMinInstances(groupContext.getGroupMinInstances());
                    groupDefinition.setName(groupContext.getName());
                    groupDefinition.setGroups(convertStubGroupContextsToGroupDefinitions(groupContext.getGroupContexts()));
                    groupDefinition.setCartridges(convertStubCartridgeContextsToCartridgeReferenceBeans(
                            groupContext.getCartridgeContexts()));
                    groupDefinitions.add(groupDefinition);
                }
            }
        }
        return groupDefinitions;
    }

    private static DependencyBean convertStubDependencyContextsToDependencyDefinitions(DependencyContext dependencyContext) {
        DependencyBean dependencyBean = new DependencyBean();
        dependencyBean.setTerminationBehaviour(dependencyContext.getTerminationBehaviour());

        if (dependencyContext.getStartupOrdersContexts() != null) {
            List<StartupOrderBean> startupOrderBeans = convertStringArrayToStartupOrderBeans(
                    dependencyContext.getStartupOrdersContexts());
            dependencyBean.setStartupOrders(startupOrderBeans);
        }
        if (dependencyContext.getScalingDependents() != null) {
            List<ScalingDependentsBean> scalingDependentBeans = convertStringArrayToDependentScalingBeans(
                    dependencyContext.getScalingDependents());
            dependencyBean.setScalingDependents(scalingDependentBeans);
        }
        return dependencyBean;
    }

    private static List<StartupOrderBean> convertStringArrayToStartupOrderBeans(String[] startupOrders) {
        List<StartupOrderBean> startupOrderBeans = new ArrayList<StartupOrderBean>();
        if (startupOrders != null) {
            for (String aliasArrayList : startupOrders) {
                if (StringUtils.isNotBlank(aliasArrayList)) {
                    String[] aliasArray = aliasArrayList.split(",");
                    StartupOrderBean startupOrderBean = new StartupOrderBean();
                    for (String alias : aliasArray) {
                        if (StringUtils.isNotBlank(alias)) {
                            startupOrderBean.addAlias(alias);
                        }
                    }
                    startupOrderBeans.add(startupOrderBean);
                }
            }
        }
        return startupOrderBeans;
    }

    private static List<ScalingDependentsBean> convertStringArrayToDependentScalingBeans(String[] scalingDependents) {
        List<ScalingDependentsBean> scalingDependentBeans = new ArrayList<ScalingDependentsBean>();
        if (scalingDependents != null) {
            for (String aliasArrayList : scalingDependents) {
                if (StringUtils.isNotBlank(aliasArrayList)) {
                    String[] aliasArray = aliasArrayList.split(",");
                    ScalingDependentsBean scalingDependentsBean = new ScalingDependentsBean();
                    for (String alias : aliasArray) {
                        if (StringUtils.isNotBlank(alias)) {
                            scalingDependentsBean.addAlias(alias);
                        }
                    }
                    scalingDependentBeans.add(scalingDependentsBean);
                }
            }
        }
        return scalingDependentBeans;
    }

    private static List<CartridgeReferenceBean> convertStubCartridgeContextsToCartridgeReferenceBeans(
            CartridgeContext[] cartridgeContexts) {
        List<CartridgeReferenceBean> cartridgeDefinitions = new ArrayList<CartridgeReferenceBean>();
        if (cartridgeContexts != null) {
            for (CartridgeContext cartridgeContext : cartridgeContexts) {
                if (cartridgeContext != null) {
                    CartridgeReferenceBean cartridgeDefinition = new CartridgeReferenceBean();
                    cartridgeDefinition.setType(cartridgeContext.getType());
                    cartridgeDefinition.setCartridgeMin(cartridgeContext.getCartridgeMin());
                    cartridgeDefinition.setCartridgeMax(cartridgeContext.getCartridgeMax());
                    cartridgeDefinition.setSubscribableInfo(convertStubSubscribableInfoContextToSubscribableInfo(
                            cartridgeContext.getSubscribableInfoContext()));
                    cartridgeDefinitions.add(cartridgeDefinition);
                }
            }
        }
        return cartridgeDefinitions;
    }

    private static SubscribableInfo convertStubSubscribableInfoContextToSubscribableInfo(
            SubscribableInfoContext subscribableInfoContext) {
        SubscribableInfo subscribableInfo = new SubscribableInfo();
        subscribableInfo.setAlias(subscribableInfoContext.getAlias());
        subscribableInfo.setAutoscalingPolicy(subscribableInfoContext.getAutoscalingPolicy());
        if (!CommonUtil.isEmptyArray(subscribableInfoContext.getDependencyAliases())) {
            subscribableInfo.setDependencyAliases(subscribableInfoContext.getDependencyAliases());
        }
        subscribableInfo.setDeploymentPolicy(subscribableInfoContext.getDeploymentPolicy());
        subscribableInfo.setMinMembers(subscribableInfoContext.getMinMembers());
        subscribableInfo.setMaxMembers(subscribableInfoContext.getMaxMembers());
        subscribableInfo.setProperty(convertStubPropertiesToPropertyBeanList(subscribableInfoContext.getProperties()));

        if (subscribableInfoContext.getArtifactRepositoryContext() != null) {
            ArtifactRepositoryContext artifactRepositoryContext = subscribableInfoContext.getArtifactRepositoryContext();

            ArtifactRepositoryBean artifactRepository = new ArtifactRepositoryBean();
            artifactRepository.setAlias(artifactRepositoryContext.getAlias());
            artifactRepository.setRepoUrl(artifactRepositoryContext.getRepoUrl());
            artifactRepository.setPrivateRepo(artifactRepositoryContext.getPrivateRepo());
            artifactRepository.setRepoUsername(artifactRepositoryContext.getRepoUsername());
            artifactRepository.setRepoPassword(artifactRepositoryContext.getRepoPassword());

            subscribableInfo.setArtifactRepository(artifactRepository);
        }
        if (subscribableInfoContext.getPersistenceContext() != null) {
            PersistenceContext persistenceContext = subscribableInfoContext.getPersistenceContext();

            PersistenceBean persistenceBean = new PersistenceBean();
            persistenceBean.setRequired(true);
            persistenceBean.setVolume(convertStubVolumeToVolume(persistenceContext.getVolumes()));

            subscribableInfo.setPersistence(persistenceBean);
        }
        return subscribableInfo;
    }

    private static List<VolumeBean> convertStubVolumeToVolume(VolumeContext[] volumes) {

        List<VolumeBean> volumeBeans = new ArrayList<VolumeBean>();
        for (VolumeContext volumeContext : volumes) {

            VolumeBean volumeBean = new VolumeBean();
            volumeBean.setRemoveOnTermination(volumeContext.getRemoveOntermination());
            volumeBean.setVolumeId(volumeContext.getVolumeId());
            volumeBean.setMappingPath(volumeContext.getMappingPath());
            volumeBean.setDevice(volumeContext.getDevice());
            volumeBean.setSize(String.valueOf(volumeContext.getSize()));
            volumeBean.setSnapshotId(volumeContext.getSnapshotId());
            volumeBean.setId(volumeContext.getId());

        }
        return volumeBeans;

    }

    private static List<org.apache.stratos.common.beans.PropertyBean>
    convertStubPropertiesToPropertyBeanList(org.apache.stratos.autoscaler.stub.Properties properties) {

        List<org.apache.stratos.common.beans.PropertyBean> propertyBeanList =
                new ArrayList<org.apache.stratos.common.beans.PropertyBean>();
        if ((properties != null) && (properties.getProperties() != null)) {
            for (org.apache.stratos.autoscaler.stub.Property property : properties.getProperties()) {
                if ((property != null)) {
                    org.apache.stratos.common.beans.PropertyBean propertyBean =
                            new org.apache.stratos.common.beans.PropertyBean();
                    propertyBean.setName(property.getName());
                    propertyBean.setValue(String.valueOf(property.getValue()));
                    propertyBeanList.add(propertyBean);
                }
            }
        }
        return propertyBeanList;
    }

    private static List<org.apache.stratos.common.beans.PropertyBean>
    convertCCStubPropertiesToPropertyBeanList(org.apache.stratos.cloud.controller.stub.Properties properties) {

        List<org.apache.stratos.common.beans.PropertyBean> propertyBeanList =
                new ArrayList<org.apache.stratos.common.beans.PropertyBean>();
        if ((properties != null) && (properties.getProperties() != null)) {
            for (org.apache.stratos.cloud.controller.stub.Property property : properties.getProperties()) {
                if ((property != null)) {
                    org.apache.stratos.common.beans.PropertyBean propertyBean =
                            new org.apache.stratos.common.beans.PropertyBean();
                    propertyBean.setName(property.getName());
                    propertyBean.setValue(String.valueOf(property.getValue()));
                    propertyBeanList.add(propertyBean);
                }
            }
        }
        return propertyBeanList;
    }

    private static CartridgeContext[] convertCartridgeReferenceBeansToStubCartridgeContexts(
            List<CartridgeReferenceBean> cartridges) {

        if (cartridges == null) {
            return null;
        }

        CartridgeContext[] cartridgeContextArray = new CartridgeContext[cartridges.size()];
        int i = 0;
        for (CartridgeReferenceBean cartridgeDefinition : cartridges) {
            CartridgeContext context = new CartridgeContext();
            context.setCartridgeMax(cartridgeDefinition.getCartridgeMax());
            context.setCartridgeMin(cartridgeDefinition.getCartridgeMin());
            context.setType(cartridgeDefinition.getType());
            context.setSubscribableInfoContext(convertSubscribableInfo(cartridgeDefinition.getSubscribableInfo()));
            cartridgeContextArray[i++] = context;
        }

        return cartridgeContextArray;
    }

    private static SubscribableInfoContext convertSubscribableInfo(SubscribableInfo subscribableInfo) {

        if (subscribableInfo == null) {
            return null;
        }

        SubscribableInfoContext infoContext = new SubscribableInfoContext();
        infoContext.setAlias(subscribableInfo.getAlias());
        infoContext.setAutoscalingPolicy(subscribableInfo.getAutoscalingPolicy());
        infoContext.setDependencyAliases(subscribableInfo.getDependencyAliases());
        infoContext.setDeploymentPolicy(subscribableInfo.getDeploymentPolicy());
        infoContext.setMaxMembers(subscribableInfo.getMaxMembers());
        infoContext.setMinMembers(subscribableInfo.getMinMembers());

        if (subscribableInfo.getArtifactRepository() != null) {
            ArtifactRepositoryBean artifactRepository = subscribableInfo.getArtifactRepository();

            ArtifactRepositoryContext artifactRepositoryContext = new ArtifactRepositoryContext();
            artifactRepositoryContext.setAlias(infoContext.getAlias());
            artifactRepositoryContext.setPrivateRepo(artifactRepository.isPrivateRepo());
            artifactRepositoryContext.setRepoUrl(artifactRepository.getRepoUrl());
            artifactRepositoryContext.setRepoUsername(artifactRepository.getRepoUsername());
            artifactRepositoryContext.setRepoPassword(artifactRepository.getRepoPassword());
            infoContext.setArtifactRepositoryContext(artifactRepositoryContext);
        }

        if (subscribableInfo.getPersistence() != null) {
            PersistenceBean persistenceBean = subscribableInfo.getPersistence();

            PersistenceContext persistenceContext = new PersistenceContext();
            persistenceContext.setPersistenceRequired(true);
            persistenceContext.setVolumes(convertVolumeToStubVolume(persistenceBean.getVolume()));

            infoContext.setPersistenceContext(persistenceContext);
        }
        infoContext.setProperties(convertPropertyBeansToStubProperties(subscribableInfo.getProperty()));
        return infoContext;
    }

    private static VolumeContext[] convertVolumeToStubVolume(List<VolumeBean> volumes) {

        if (volumes == null) {
            return null;
        }

        List<VolumeContext> volumeContexts = new ArrayList<VolumeContext>();
        for (VolumeBean volumeBean : volumes) {

            VolumeContext volumeContext = new VolumeContext();
            volumeContext.setRemoveOntermination(volumeBean.isRemoveOnTermination());
            volumeContext.setVolumeId(volumeBean.getVolumeId());
            volumeContext.setMappingPath(volumeBean.getMappingPath());
            volumeContext.setDevice(volumeBean.getDevice());

            // When volumeId is specified, volume size is not relevant.
            if (volumeBean.getSize() != null) {
                volumeContext.setSize(Integer.parseInt(volumeBean.getSize()));
            }

            volumeContext.setSnapshotId(volumeBean.getSnapshotId());
            volumeContext.setId(volumeBean.getId());

            volumeContexts.add(volumeContext);
        }
        return volumeContexts.toArray(new VolumeContext[volumeContexts.size()]);
    }

    private static org.apache.stratos.autoscaler.stub.Properties convertPropertyBeansToStubProperties(
            List<org.apache.stratos.common.beans.PropertyBean> property) {
        org.apache.stratos.autoscaler.stub.Properties prop = new org.apache.stratos.autoscaler.stub.Properties();
        if (property != null) {
            for (org.apache.stratos.common.beans.PropertyBean propertyBean : property) {
                org.apache.stratos.autoscaler.stub.Property p = new org.apache.stratos.autoscaler.stub.Property();
                p.setName(propertyBean.getName());
                p.setValue(propertyBean.getValue());
                prop.addProperties(p);
            }
        }
        return prop;
    }


    private static DependencyContext convertDependencyDefinitionsToDependencyContexts(DependencyBean dependencyBean) {
        if (dependencyBean == null) {
            return null;
        }

        DependencyContext dependencyContext = new DependencyContext();
        dependencyContext.setTerminationBehaviour(dependencyBean.getTerminationBehaviour());

        if (dependencyBean.getStartupOrders() != null) {
            List<String> startupOrders = convertStartupOrdersBeansToStringList(dependencyBean.getStartupOrders());
            dependencyContext.setStartupOrdersContexts(startupOrders.toArray(new String[startupOrders.size()]));
        }
        if (dependencyBean.getScalingDependents() != null) {
            List<String> scalingDependents = convertScalingDependentsBeansToStringList(dependencyBean.getScalingDependents());
            dependencyContext.setScalingDependents(scalingDependents.toArray(new String[scalingDependents.size()]));
        }
        return dependencyContext;
    }

    private static List<String> convertScalingDependentsBeansToStringList(List<ScalingDependentsBean> scalingDependentsBeans) {
        List<String> scalingDependents = new ArrayList<String>();
        if (scalingDependentsBeans != null) {
            for (ScalingDependentsBean scalingDependentsBean : scalingDependentsBeans) {
                StringBuilder stringBuilder = new StringBuilder();
                for (String alias : scalingDependentsBean.getAliases()) {
                    if (stringBuilder.length() > 0) {
                        stringBuilder.append(",");
                    }
                    stringBuilder.append(alias);
                }
                scalingDependents.add(stringBuilder.toString());
            }
        }
        return scalingDependents;
    }

    private static org.apache.stratos.autoscaler.stub.pojo.GroupContext[]
    convertGroupDefinitionsToStubGroupContexts(List<CartridgeGroupReferenceBean> groupDefinitions) {

        if (groupDefinitions == null) {
            return null;
        }

        GroupContext[] groupContexts = new GroupContext[groupDefinitions.size()];
        int i = 0;
        for (CartridgeGroupReferenceBean groupDefinition : groupDefinitions) {
            GroupContext groupContext = new GroupContext();
            groupContext.setName(groupDefinition.getName());
            groupContext.setAlias(groupDefinition.getAlias());
            groupContext.setGroupMaxInstances(groupDefinition.getGroupMaxInstances());
            groupContext.setGroupMinInstances(groupDefinition.getGroupMinInstances());
            groupContext.setDeploymentPolicy(groupDefinition.getDeploymentPolicy());

            // Groups
            if (groupDefinition.getGroups() != null) {
                groupContext.setGroupContexts(convertGroupDefinitionsToStubGroupContexts(groupDefinition.getGroups()));
            }

            // Cartridges
            if (groupDefinition.getCartridges() != null) {
                groupContext.setCartridgeContexts(convertCartridgeReferenceBeansToStubCartridgeContexts(groupDefinition.getCartridges()));
            }
            groupContexts[i++] = groupContext;
        }
        return groupContexts;
    }

    /**
     * Convert Persistence To PersistenceBean
     *
     * @param baseDir     base directory
     * @param directories directories
     * @return DeploymentBean
     */
    private static DeploymentBean convertDeploymentToDeploymentBean(String[] directories,
                                                                    String baseDir) {
        if (baseDir == null || directories == null || directories[0] == null) {
            return null;
        }

        DeploymentBean deploymentBean = new DeploymentBean();
        deploymentBean.setBaseDir(baseDir);
        deploymentBean.setDir(Arrays.asList(baseDir));
        return deploymentBean;
    }

    /**
     * Convert Persistence To PersistenceBean
     *
     * @param persistence persistence
     * @return PersistenceBean
     */
    private static PersistenceBean convertPersistenceToPersistenceBean(Persistence persistence) {
        if (persistence == null) {
            return null;
        }

        PersistenceBean persistenceBean = new PersistenceBean();
        persistenceBean.setRequired(persistence.isPersistenceRequiredSpecified());
        persistenceBean.setVolume(convertVolumesToVolumeBeans(persistence.getVolumes()));
        return persistenceBean;
    }

    /**
     * Convert Volumes To VolumeBeans
     *
     * @param volumes Volumes
     * @return VolumeBeans
     */
    private static List<VolumeBean> convertVolumesToVolumeBeans(Volume[] volumes) {
        List<VolumeBean> list = new ArrayList<VolumeBean>();
        for (Volume volume : volumes) {
            VolumeBean volumeBean = new VolumeBean();
            volumeBean.setId(volume.getId());
            volumeBean.setDevice(volume.getDevice());
            volumeBean.setSize(String.valueOf(volume.getSize()));
            volumeBean.setSnapshotId(volume.getSnapshotId());
            list.add(volumeBean);
        }
        return list;
    }

    /**
     * Converts Cartridge to CartridgeDefinitionBean
     *
     * @param cartridgeInfo cartridge Info
     * @return CartridgeBean
     */
    public static CartridgeBean convertCartridgeToCartridgeDefinitionBean(Cartridge cartridgeInfo) {
        CartridgeBean cartridge = new CartridgeBean();
        cartridge.setType(cartridgeInfo.getType());
        cartridge.setProvider(cartridgeInfo.getProvider());
        cartridge.setCategory(cartridgeInfo.getCategory());
        cartridge.setHost(cartridgeInfo.getHostName());
        cartridge.setDisplayName(cartridgeInfo.getDisplayName());
        cartridge.setDescription(cartridgeInfo.getDescription());
        cartridge.setVersion(cartridgeInfo.getVersion());
        cartridge.setMultiTenant(cartridgeInfo.getMultiTenant());
        cartridge.setDescription(cartridgeInfo.getDescription());
        cartridge.setLoadBalancingIPType(cartridgeInfo.getLoadBalancingIPType());

        if (cartridgeInfo.getMetadataKeys() != null && cartridgeInfo.getMetadataKeys()[0] != null) {
            cartridge.setMetadataKeys(Arrays.asList(cartridgeInfo.getMetadataKeys()));
        }

        //convert persistence
        cartridge.setPersistence(convertPersistenceToPersistenceBean(cartridgeInfo.getPersistence()));

        //convert deployment
        cartridge.setDeployment(convertDeploymentToDeploymentBean(cartridgeInfo.getDeploymentDirs(),
                cartridgeInfo.getBaseDir()));

        //convert IaaSProvider
        cartridge.setIaasProvider(convertIaaSProviderToIaaSProviderBean(
                cartridgeInfo.getIaasConfigs()));

        //Convert Port-mappings
        cartridge.setPortMapping(convertPortMappingsToStubPortMappingBeans(
                cartridgeInfo.getPortMappings()));

        //convert properties
        cartridge.setProperty(convertCCStubPropertiesToPropertyBeans(cartridgeInfo.getProperties()));

        return cartridge;
    }

    public static ApplicationInfoBean convertApplicationToApplicationBean(Application application) {

        if (application == null) {
            return null;
        }

        ApplicationInfoBean applicationBean = new ApplicationInfoBean();
        applicationBean.setId(application.getUniqueIdentifier());
        applicationBean.setName(application.getName());
        applicationBean.setDescription(application.getDescription());
        applicationBean.setTenantDomain(application.getTenantDomain());
        applicationBean.setTenantAdminUsername(application.getTenantAdminUserName());
        return applicationBean;
    }

    public static ApplicationInfoBean convertApplicationToApplicationInstanceBean(Application application) {

        if (application == null) {
            return null;
        }

        ApplicationInfoBean applicationBean = new ApplicationInfoBean();
        applicationBean.setId(application.getUniqueIdentifier());
        applicationBean.setName(application.getName());
        applicationBean.setStatus(application.getStatus().name());
        applicationBean.setDescription(application.getDescription());
        applicationBean.setTenantDomain(application.getTenantDomain());
        applicationBean.setTenantAdminUsername(application.getTenantAdminUserName());
        applicationBean.setApplicationInstances(convertApplicationInstancesToApplicationInstances(application));
        return applicationBean;
    }


    private static List<ApplicationInstanceBean> convertApplicationInstancesToApplicationInstances(
            Application application) {

        if (application == null) {
            return null;
        }

        List<ApplicationInstanceBean> applicationInstanceList = new ArrayList<ApplicationInstanceBean>();
        Collection<ApplicationInstance> applicationInstancesInTopology =
                application.getInstanceIdToInstanceContextMap().values();

        for (ApplicationInstance applicationInstance : applicationInstancesInTopology) {
            ApplicationInstanceBean instance = new ApplicationInstanceBean();
            instance.setInstanceId(applicationInstance.getInstanceId());
            instance.setApplicationId(application.getUniqueIdentifier());
            instance.setParentInstanceId(applicationInstance.getParentId());
            instance.setStatus(applicationInstance.getStatus().toString());
            applicationInstanceList.add(instance);
        }

        return applicationInstanceList;
    }

    public static List<GroupInstanceBean> convertGroupToGroupInstancesBean(String instanceId, Group group) {
        if (group == null) {
            return null;
        }

        List<GroupInstanceBean> groupInstanceBeans = new ArrayList<GroupInstanceBean>();
        if (group.getInstanceContexts(instanceId) != null) {
            GroupInstance groupInstance = group.getInstanceContexts(instanceId);
            GroupInstanceBean groupInstanceBean = new GroupInstanceBean();
            groupInstanceBean.setParentInstanceId(instanceId);
            groupInstanceBean.setInstanceId(groupInstance.getInstanceId());
            groupInstanceBean.setStatus(groupInstance.getStatus().toString());
            groupInstanceBean.setGroupId(group.getUniqueIdentifier());
            groupInstanceBeans.add(groupInstanceBean);

        } else {
            List<org.apache.stratos.messaging.domain.instance.Instance> groupInstances =
                    group.getInstanceContextsWithParentId(instanceId);
            for (org.apache.stratos.messaging.domain.instance.Instance groupInstance : groupInstances) {
                GroupInstanceBean groupInstanceBean = new GroupInstanceBean();
                groupInstanceBean.setParentInstanceId(instanceId);
                groupInstanceBean.setInstanceId(groupInstance.getInstanceId());
                groupInstanceBean.setStatus(((GroupInstance) groupInstance).getStatus().toString());
                groupInstanceBean.setGroupId(group.getUniqueIdentifier());
                groupInstanceBeans.add(groupInstanceBean);
            }
        }

        return groupInstanceBeans;
    }


    public static org.apache.stratos.common.beans.TenantInfoBean convertCarbonTenantInfoBeanToTenantInfoBean(
            TenantInfoBean carbonTenantInfoBean) {

        if (carbonTenantInfoBean == null) {
            return null;
        }

        org.apache.stratos.common.beans.TenantInfoBean tenantInfoBean =
                new org.apache.stratos.common.beans.TenantInfoBean();
        tenantInfoBean.setTenantId(carbonTenantInfoBean.getTenantId());
        tenantInfoBean.setTenantDomain(carbonTenantInfoBean.getTenantDomain());
        tenantInfoBean.setActive(carbonTenantInfoBean.isActive());
        tenantInfoBean.setAdmin(carbonTenantInfoBean.getAdmin());
        tenantInfoBean.setEmail(carbonTenantInfoBean.getEmail());
        tenantInfoBean.setAdminPassword(carbonTenantInfoBean.getAdminPassword());
        tenantInfoBean.setFirstName(carbonTenantInfoBean.getFirstname());
        tenantInfoBean.setLastName(carbonTenantInfoBean.getLastname());
        tenantInfoBean.setCreatedDate(carbonTenantInfoBean.getCreatedDate().getTimeInMillis());
        return tenantInfoBean;
    }

    public static TenantInfoBean convertTenantInfoBeanToCarbonTenantInfoBean(
            org.apache.stratos.common.beans.TenantInfoBean tenantInfoBean) {

        if (tenantInfoBean == null) {
            return null;
        }

        TenantInfoBean carbonTenantInfoBean = new TenantInfoBean();
        carbonTenantInfoBean.setTenantId(tenantInfoBean.getTenantId());
        carbonTenantInfoBean.setTenantDomain(tenantInfoBean.getTenantDomain());
        carbonTenantInfoBean.setActive(tenantInfoBean.isActive());
        carbonTenantInfoBean.setAdmin(tenantInfoBean.getAdmin());
        carbonTenantInfoBean.setEmail(tenantInfoBean.getEmail());
        carbonTenantInfoBean.setAdminPassword(tenantInfoBean.getAdminPassword());
        carbonTenantInfoBean.setFirstname(tenantInfoBean.getFirstName());
        carbonTenantInfoBean.setLastname(tenantInfoBean.getLastName());
        if (tenantInfoBean.getCreatedDate() > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(tenantInfoBean.getCreatedDate());
            carbonTenantInfoBean.setCreatedDate(calendar);
        }
        return carbonTenantInfoBean;
    }

    public static ServiceGroup convertServiceGroupDefinitionToASStubServiceGroup(CartridgeGroupBean groupBean)
            throws ServiceGroupDefinitionException {

        if (groupBean == null) {
            return null;
        }

        ServiceGroup servicegroup = new ServiceGroup();
        List<CartridgeGroupBean> groupsDefinitions = groupBean.getGroups();
        List<String> cartridgesDefinitions = groupBean.getCartridges();

        servicegroup.setName(groupBean.getName());

        if (groupsDefinitions == null) {
            groupsDefinitions = new ArrayList<CartridgeGroupBean>(0);
        }

        if (cartridgesDefinitions == null) {
            cartridgesDefinitions = new ArrayList<String>(0);
        }

        ServiceGroup[] subGroups = new ServiceGroup[groupsDefinitions.size()];
        String[] cartridges = new String[cartridgesDefinitions.size()];

        int i = 0;
        for (CartridgeGroupBean groupDefinition : groupsDefinitions) {
            subGroups[i] = convertServiceGroupDefinitionToASStubServiceGroup(groupDefinition);
            ++i;
        }

        servicegroup.setGroups(subGroups);
        cartridges = cartridgesDefinitions.toArray(cartridges);
        servicegroup.setCartridges(cartridges);

        DependencyBean dependencyBean = groupBean.getDependencies();

        if (dependencyBean != null) {
            Dependencies dependencies = new Dependencies();
            List<StartupOrderBean> startupOrderBeans = dependencyBean.getStartupOrders();
            if (startupOrderBeans != null) {
                List<String> startupOrders = convertStartupOrdersBeansToStringList(dependencyBean.getStartupOrders());
                dependencies.setStartupOrders(startupOrders.toArray(new String[startupOrders.size()]));
            }
            // validate termination behavior
            validateTerminationBehavior(dependencyBean.getTerminationBehaviour());
            dependencies.setTerminationBehaviour(dependencyBean.getTerminationBehaviour());
            if (dependencyBean.getScalingDependents() != null) {
                List<String> scalingDependents = convertScalingDependentsBeansToStringList(dependencyBean.getScalingDependents());
                dependencies.setScalingDependants(scalingDependents.toArray(new String[scalingDependents.size()]));
            }
            servicegroup.setDependencies(dependencies);
        }
        return servicegroup;
    }

    private static List<String> convertStartupOrdersBeansToStringList(List<StartupOrderBean> startupOrderBeans) {
        List<String> startupOrders = new ArrayList<String>();
        if (startupOrderBeans != null) {
            for (StartupOrderBean startupOrderBean : startupOrderBeans) {
                StringBuilder stringBuilder = new StringBuilder();
                for (String alias : startupOrderBean.getAliases()) {
                    if (stringBuilder.length() > 0) {
                        stringBuilder.append(",");
                    }
                    stringBuilder.append(alias);
                }
                startupOrders.add(stringBuilder.toString());
            }
        }
        return startupOrders;
    }

    public static CartridgeGroupBean convertStubServiceGroupToServiceGroupDefinition(ServiceGroup serviceGroup) {
        if (serviceGroup == null) {
            return null;
        }

        CartridgeGroupBean serviceGroupDefinition = new CartridgeGroupBean();
        serviceGroupDefinition.setName(serviceGroup.getName());
        String[] cartridges = serviceGroup.getCartridges();
        ServiceGroup[] groups = serviceGroup.getGroups();
        org.apache.stratos.autoscaler.stub.pojo.Dependencies dependencies = serviceGroup.getDependencies();

        List<CartridgeGroupBean> groupDefinitions = new ArrayList<CartridgeGroupBean>(groups.length);
        for (ServiceGroup group : groups) {
            if (group != null) {
                groupDefinitions.add(convertStubServiceGroupToServiceGroupDefinition(group));
            }
        }

        if (dependencies != null) {
            DependencyBean dependencyBean = new DependencyBean();
            String[] startupOrders = dependencies.getStartupOrders();
            if (startupOrders != null && startupOrders[0] != null) {
                List<String> startupOrdersDef = Arrays.asList(startupOrders);
                //dependencyBean.setStartupOrders(startupOrdersDef);
            }

            String[] scalingDependants = dependencies.getScalingDependants();
            if (scalingDependants != null && scalingDependants[0] != null) {
                List<String> scalingDependenciesDef = Arrays.asList(scalingDependants);
                //dependencyBean.setScalingDependents(scalingDependenciesDef);
            }

            dependencyBean.setTerminationBehaviour(dependencies.getTerminationBehaviour());
            serviceGroupDefinition.setDependencies(dependencyBean);
        }

        List<String> cartridgesDef = new ArrayList<String>(Arrays.asList(cartridges));
        if (cartridges[0] != null) {
            serviceGroupDefinition.setCartridges(cartridgesDef);
        }

        serviceGroupDefinition.setGroups(groupDefinitions);
        return serviceGroupDefinition;
    }

    /**
     * Validates terminationBehavior. The terminationBehavior should be one of the following:
     * 1. terminate-none
     * 2. terminate-dependents
     * 3. terminate-all
     *
     * @throws ServiceGroupDefinitionException if terminationBehavior is different to what is
     *                                         listed above
     */
    private static void validateTerminationBehavior(String terminationBehavior) throws ServiceGroupDefinitionException {

        if (!(terminationBehavior == null || "terminate-none".equals(terminationBehavior) ||
                "terminate-dependents".equals(terminationBehavior) || "terminate-all".equals(terminationBehavior))) {
            throw new ServiceGroupDefinitionException("Invalid Termination Behaviour specified: [ " +
                    terminationBehavior + " ], should be one of 'terminate-none', 'terminate-dependents', " +
                    " 'terminate-all' ");
        }
    }

    public static ApplicationSignUp convertApplicationSignUpBeanToStubApplicationSignUp(
            ApplicationSignUpBean applicationSignUpBean) {

        if (applicationSignUpBean == null) {
            return null;
        }
        ApplicationSignUp applicationSignUp = new ApplicationSignUp();

        if (applicationSignUpBean.getArtifactRepositories() != null) {
            List<ArtifactRepository> artifactRepositoryList = new ArrayList<ArtifactRepository>();
            for (ArtifactRepositoryBean artifactRepositoryBean : applicationSignUpBean.getArtifactRepositories()) {
                ArtifactRepository artifactRepository = new ArtifactRepository();

                artifactRepository.setAlias(artifactRepositoryBean.getAlias());
                artifactRepository.setPrivateRepo(artifactRepositoryBean.isPrivateRepo());
                artifactRepository.setRepoUrl(artifactRepositoryBean.getRepoUrl());
                artifactRepository.setRepoUsername(artifactRepositoryBean.getRepoUsername());
                artifactRepository.setRepoPassword(artifactRepositoryBean.getRepoPassword());

                artifactRepositoryList.add(artifactRepository);
            }
            ArtifactRepository[] artifactRepositoryArray = artifactRepositoryList.toArray(new ArtifactRepository[
                    artifactRepositoryList.size()]);
            applicationSignUp.setArtifactRepositories(artifactRepositoryArray);
        }
        return applicationSignUp;
    }

    public static ApplicationSignUpBean convertStubApplicationSignUpToApplicationSignUpBean(ApplicationSignUp applicationSignUp) {

        if (applicationSignUp == null) {
            return null;
        }
        ApplicationSignUpBean applicationSignUpBean = new ApplicationSignUpBean();

        if (applicationSignUp.getArtifactRepositories() != null) {
            List<ArtifactRepositoryBean> artifactRepositoryBeanList = new ArrayList<ArtifactRepositoryBean>();
            for (ArtifactRepository artifactRepository : applicationSignUp.getArtifactRepositories()) {
                if (artifactRepository != null) {
                    ArtifactRepositoryBean artifactRepositoryBean = new ArtifactRepositoryBean();

                    artifactRepositoryBean.setAlias(artifactRepository.getAlias());
                    artifactRepositoryBean.setPrivateRepo(artifactRepository.getPrivateRepo());
                    artifactRepositoryBean.setRepoUrl(artifactRepository.getRepoUrl());
                    artifactRepositoryBean.setRepoUsername(artifactRepository.getRepoUsername());
                    artifactRepositoryBean.setRepoPassword(artifactRepository.getRepoPassword());

                    artifactRepositoryBeanList.add(artifactRepositoryBean);
                }
            }
            applicationSignUpBean.setArtifactRepositories(artifactRepositoryBeanList);
        }
        return applicationSignUpBean;
    }

    public static DomainMapping convertDomainMappingBeanToStubDomainMapping(DomainMappingBean domainMappingBean) {

        if (domainMappingBean == null) {
            return null;
        }

        DomainMapping domainMapping = new DomainMapping();
        domainMapping.setDomainName(domainMappingBean.getDomainName());
        domainMapping.setContextPath(domainMappingBean.getContextPath());
        return domainMapping;
    }

    public static DomainMappingBean convertStubDomainMappingToDomainMappingBean(DomainMapping domainMapping) {

        if (domainMapping == null) {
            return null;
        }

        DomainMappingBean domainMappingBean = new DomainMappingBean();
        domainMappingBean.setDomainName(domainMapping.getDomainName());
        domainMappingBean.setContextPath(domainMapping.getContextPath());
        return domainMappingBean;
    }

    public static DeploymentPolicyBean convertCCStubDeploymentPolicyToDeploymentPolicy(DeploymentPolicy deploymentPolicy) {

        if (deploymentPolicy == null) {
            return null;
        }

        DeploymentPolicyBean deploymentPolicyBean = new DeploymentPolicyBean();
        deploymentPolicyBean.setId(deploymentPolicy.getDeploymentPolicyID());
        deploymentPolicyBean.setNetworkPartitions(convertASStubNetworkPartitionsToNetworkPartitionReferences(
                deploymentPolicy.getNetworkPartitionRefs()));
        return deploymentPolicyBean;
    }

    public static ApplicationPolicy convertApplicationPolicyBeanToStubAppPolicy(
            ApplicationPolicyBean applicationPolicyBean) {

        if (applicationPolicyBean == null) {
            return null;
        }

        ApplicationPolicy applicationPolicy = new ApplicationPolicy();
        applicationPolicy.setId(applicationPolicyBean.getId());
        applicationPolicy.setAlgorithm(applicationPolicyBean.getAlgorithm());
        applicationPolicy.setNetworkPartitions(applicationPolicyBean.getNetworkPartitions());
        if (applicationPolicyBean.getProperties() != null) {
            if (!applicationPolicyBean.getProperties().isEmpty()) {
                applicationPolicy.setProperties(getASPropertiesFromCommonProperties(applicationPolicyBean.getProperties()));
            }
        }
        return applicationPolicy;
    }


    public static DeploymentPolicy convertDeploymentPolicyBeanToASDeploymentPolicy(
            DeploymentPolicyBean deploymentPolicyBean) {

        if (deploymentPolicyBean == null) {
            return null;
        }

        DeploymentPolicy deploymentPolicy = new DeploymentPolicy();
        deploymentPolicy.setDeploymentPolicyID(deploymentPolicyBean.getId());
        if (deploymentPolicyBean.getNetworkPartitions() != null) {
            deploymentPolicy.setNetworkPartitionRefs(convertNetworkPartitionToASStubNetworkPartition(
                    deploymentPolicyBean.getNetworkPartitions()));
        }

        return deploymentPolicy;
    }


    public static DeploymentPolicyBean[] convertASStubDeploymentPoliciesToDeploymentPolicies(
            DeploymentPolicy[] deploymentPolicies) {
        DeploymentPolicyBean[] deploymentPolicyBeans;
        if (null == deploymentPolicies) {
            deploymentPolicyBeans = new DeploymentPolicyBean[0];
            return deploymentPolicyBeans;
        }

        deploymentPolicyBeans = new DeploymentPolicyBean[deploymentPolicies.length];
        for (int i = 0; i < deploymentPolicies.length; i++) {
            deploymentPolicyBeans[i] = convertASStubDeploymentPolicyToDeploymentPolicy(deploymentPolicies[i]);
        }
        return deploymentPolicyBeans;
    }


    private static DeploymentPolicyBean convertASStubDeploymentPolicyToDeploymentPolicy(DeploymentPolicy deploymentPolicy) {

        if (deploymentPolicy == null) {
            return null;
        }

        DeploymentPolicyBean deploymentPolicyBean = new DeploymentPolicyBean();
        deploymentPolicyBean.setId(deploymentPolicy.getDeploymentPolicyID());
        deploymentPolicyBean.setNetworkPartitions(convertASStubNetworkPartitionRefsToNetworkPartitions(
                deploymentPolicy.getNetworkPartitionRefs()));
        return deploymentPolicyBean;
    }

    private static List<NetworkPartitionReferenceBean> convertASStubNetworkPartitionRefsToNetworkPartitions(
            NetworkPartitionRef[] networkPartitions) {

        List<NetworkPartitionReferenceBean> networkPartitionBeans = new ArrayList<NetworkPartitionReferenceBean>();
        for (NetworkPartitionRef networkPartition : networkPartitions) {
            NetworkPartitionReferenceBean networkPartitionReferenceBean = new NetworkPartitionReferenceBean();
            networkPartitionReferenceBean.setId(networkPartition.getId());
            networkPartitionReferenceBean.setPartitionAlgo(networkPartition.getPartitionAlgo());
            networkPartitionReferenceBean.setPartitions(
                    convertASStubPartitionRefsToPartitionReferences(networkPartition.getPartitionRefs()));
            networkPartitionBeans.add(networkPartitionReferenceBean);
        }

        return networkPartitionBeans;
    }


    private static List<PartitionReferenceBean> convertASStubPartitionRefsToPartitionReferences(
            PartitionRef[] partitions) {

        List<PartitionReferenceBean> partitionRefBeans = new ArrayList<PartitionReferenceBean>();
        for (PartitionRef partition : partitions) {
            PartitionReferenceBean partitionRefBean = new PartitionReferenceBean();
            partitionRefBean.setId(partition.getId());
            partitionRefBean.setPartitionMax(partition.getPartitionMax());
            partitionRefBeans.add(partitionRefBean);
        }
        return partitionRefBeans;
    }

    private static PartitionRef convertPartitionToASStubPartition(
            PartitionReferenceBean partitionReferenceBean) {

        if (partitionReferenceBean == null) {
            return null;
        }

        PartitionRef stubPartition = new PartitionRef();
        stubPartition.setId(partitionReferenceBean.getId());
        stubPartition.setPartitionMax(partitionReferenceBean.getPartitionMax());
        return stubPartition;
    }


    private static PartitionRef[] convertToASStubPartitions
            (List<PartitionReferenceBean> partitionReferenceBeanList) {

        PartitionRef[] partitions
                = new PartitionRef[partitionReferenceBeanList.size()];
        for (int i = 0; i < partitionReferenceBeanList.size(); i++) {
            partitions[i] = convertPartitionToASStubPartition(partitionReferenceBeanList.get(i));
        }
        return partitions;
    }


    private static NetworkPartitionRef[] convertNetworkPartitionToASStubNetworkPartition(
            List<NetworkPartitionReferenceBean> networkPartitionReferenceBeans) {

        List<NetworkPartitionRef> networkPartitionRefList =
                new ArrayList<NetworkPartitionRef>();
        for (NetworkPartitionReferenceBean networkPartitionReferenceBean : networkPartitionReferenceBeans) {
            NetworkPartitionRef networkPartitionRef = new NetworkPartitionRef();
            networkPartitionRef.setId(networkPartitionReferenceBean.getId());
            networkPartitionRef.setPartitionAlgo(networkPartitionReferenceBean.getPartitionAlgo());
            if(networkPartitionReferenceBean.getPartitions() != null) {
                networkPartitionRef.setPartitionRefs(convertToASStubPartitions(
                        networkPartitionReferenceBean.getPartitions()));
            }
            networkPartitionRefList.add(networkPartitionRef);
        }
        return networkPartitionRefList.toArray(new NetworkPartitionRef[networkPartitionRefList.size()]);
    }
}

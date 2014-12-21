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
import org.apache.stratos.autoscaler.stub.deployment.partition.ChildLevelNetworkPartition;
import org.apache.stratos.autoscaler.stub.deployment.partition.ChildLevelPartition;
import org.apache.stratos.autoscaler.stub.deployment.policy.ChildPolicy;
import org.apache.stratos.autoscaler.stub.pojo.*;
import org.apache.stratos.cloud.controller.stub.domain.*;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.beans.ApplicationBean;
import org.apache.stratos.common.beans.GroupBean;
import org.apache.stratos.common.beans.autoscaler.partition.ApplicationLevelNetworkPartition;
import org.apache.stratos.common.beans.autoscaler.partition.Partition;
import org.apache.stratos.common.beans.autoscaler.policy.autoscale.*;
import org.apache.stratos.common.beans.autoscaler.policy.deployment.ApplicationPolicy;
import org.apache.stratos.common.beans.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.common.beans.cartridge.definition.*;
import org.apache.stratos.common.beans.cartridge.definition.PropertyBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesGroup;
import org.apache.stratos.common.beans.kubernetes.KubernetesHost;
import org.apache.stratos.common.beans.kubernetes.KubernetesMaster;
import org.apache.stratos.common.beans.kubernetes.PortRange;
import org.apache.stratos.common.beans.subscription.domain.SubscriptionDomainBean;
import org.apache.stratos.common.beans.topology.*;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.manager.composite.application.beans.*;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.grouping.definitions.DependencyDefinitions;
import org.apache.stratos.manager.subscription.SubscriptionDomain;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.Group;
import org.apache.stratos.messaging.domain.instance.ApplicationInstance;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;

import java.util.*;

public class ObjectConverter {

    public static CartridgeConfig convertCartridgeDefinitionBeanToStubCartridgeConfig(
            CartridgeDefinitionBean cartridgeDefinitionBean) {

        CartridgeConfig cartridgeConfig = new CartridgeConfig();

	    cartridgeConfig.setType(cartridgeDefinitionBean.getType());
	    cartridgeConfig.setHostName(cartridgeDefinitionBean.getHost());
	    cartridgeConfig.setProvider(cartridgeDefinitionBean.getProvider());
	    cartridgeConfig.setCategory(cartridgeDefinitionBean.getCategory());
	    cartridgeConfig.setVersion(cartridgeDefinitionBean.getVersion());
	    cartridgeConfig.setMultiTenant(cartridgeDefinitionBean.isMultiTenant());
	    cartridgeConfig.setIsPublic(cartridgeDefinitionBean.isPublic());
	    cartridgeConfig.setDisplayName(cartridgeDefinitionBean.getDisplayName());
	    cartridgeConfig.setDescription(cartridgeDefinitionBean.getDescription());
	    cartridgeConfig.setDefaultAutoscalingPolicy(cartridgeDefinitionBean.getDefaultAutoscalingPolicy());
	    cartridgeConfig.setDefaultDeploymentPolicy(cartridgeDefinitionBean.getDefaultDeploymentPolicy());
	    cartridgeConfig.setServiceGroup(cartridgeDefinitionBean.getServiceGroup());

        //deployment information
        if (cartridgeDefinitionBean.getDeployment() != null) {
            cartridgeConfig.setBaseDir(cartridgeDefinitionBean.getDeployment().getBaseDir());
            if (cartridgeDefinitionBean.getDeployment().getDir() != null && !cartridgeDefinitionBean.getDeployment().getDir().isEmpty()) {
                cartridgeConfig.setDeploymentDirs(cartridgeDefinitionBean.getDeployment().getDir().
                        toArray(new String[cartridgeDefinitionBean.getDeployment().getDir().size()]));
            }
        }
        //port mapping
        if (cartridgeDefinitionBean.getPortMapping() != null && !cartridgeDefinitionBean.getPortMapping().isEmpty()) {
            cartridgeConfig.setPortMappings(convertPortMappingBeansToStubPortMappings(cartridgeDefinitionBean.getPortMapping()));
        }

        //persistance mapping
        if (cartridgeDefinitionBean.getPersistence() != null) {
            cartridgeConfig.setPersistence(convertPersistenceBeanToStubPersistence(cartridgeDefinitionBean.getPersistence()));
        }

        //IaaS
        if (cartridgeDefinitionBean.getIaasProvider() != null && !cartridgeDefinitionBean.getIaasProvider().isEmpty()) {
            cartridgeConfig.setIaasConfigs(convertIaasProviderBeansToStubIaasConfig(cartridgeDefinitionBean.getIaasProvider()));
        }
        //Properties
        if (cartridgeDefinitionBean.getProperty() != null && !cartridgeDefinitionBean.getProperty().isEmpty()) {
            cartridgeConfig.setProperties(convertPropertyBeansToCCStubProperties(cartridgeDefinitionBean.getProperty()));
        }

        if (cartridgeDefinitionBean.getExportingProperties() != null) {
            List<String> propertiesList = cartridgeDefinitionBean.getExportingProperties();
            String[] propertiesArray = propertiesList.toArray(new String[propertiesList.size()]);
            cartridgeConfig.setExportingProperties(propertiesArray);
        }

        if (cartridgeDefinitionBean.getContainer() != null) {
            cartridgeConfig.setContainer(convertContainerBeanToStubContainer(cartridgeDefinitionBean.getContainer()));
        }

        return cartridgeConfig;
    }

    private static Container convertContainerBeanToStubContainer(ContainerBean containerBean) {
        Container container = new Container();
        container.setDockerFileRepo(containerBean.getDockerfileRepo());
        container.setImageName(containerBean.getImageName());
        //container.setProperties(convertPropertyBeansToStubProperties(containerBean.getProperty()));
        return container;
    }

    private static PortMapping[] convertPortMappingBeansToStubPortMappings(List<PortMappingBean> portMappingBeans) {

        //convert to an array
        PortMappingBean[] portMappingBeanArray = new PortMappingBean[portMappingBeans.size()];
        portMappingBeans.toArray(portMappingBeanArray);
        PortMapping[] portMappingArray = new PortMapping[portMappingBeanArray.length];

        for (int i = 0; i < portMappingBeanArray.length; i++) {
            PortMapping portMapping = new PortMapping();
            portMapping.setProtocol(portMappingBeanArray[i].getProtocol());
            portMapping.setPort(Integer.toString(portMappingBeanArray[i].getPort()));
            portMapping.setProxyPort(Integer.toString(portMappingBeanArray[i].getProxyPort()));
            portMappingArray[i] = portMapping;
        }

        return portMappingArray;
    }

    private static IaasConfig[] convertIaasProviderBeansToStubIaasConfig(List<IaasProviderBean> iaasProviderBeans) {

        //convert to an array
        IaasProviderBean[] iaasProviderBeansArray = new IaasProviderBean[iaasProviderBeans.size()];
        iaasProviderBeans.toArray(iaasProviderBeansArray);
        IaasConfig[] iaasConfigsArray = new IaasConfig[iaasProviderBeansArray.length];

        for (int i = 0; i < iaasProviderBeansArray.length; i++) {
            IaasConfig iaasConfig = new IaasConfig();
            iaasConfig.setType(iaasProviderBeansArray[i].getType());
            iaasConfig.setImageId(iaasProviderBeansArray[i].getImageId());
            iaasConfig.setMaxInstanceLimit(iaasProviderBeansArray[i].getMaxInstanceLimit());
            iaasConfig.setName(iaasProviderBeansArray[i].getName());
            iaasConfig.setClassName(iaasProviderBeansArray[i].getClassName());
            iaasConfig.setCredential(iaasProviderBeansArray[i].getCredential());
            iaasConfig.setIdentity(iaasProviderBeansArray[i].getIdentity());
            iaasConfig.setProvider(iaasProviderBeansArray[i].getProvider());

            if (iaasProviderBeansArray[i].getProperty() != null && !iaasProviderBeansArray[i].getProperty().isEmpty()) {
                //set the Properties instance to IaasConfig instance
                iaasConfig.setProperties(convertPropertyBeansToCCStubProperties(iaasProviderBeansArray[i].getProperty()));
            }

            if (iaasProviderBeansArray[i].getNetworkInterfaces() != null && !iaasProviderBeansArray[i].getNetworkInterfaces().isEmpty()) {
                iaasConfig.setNetworkInterfaces(ObjectConverter.convertNetworkInterfaceBeansToNetworkInterfaces(iaasProviderBeansArray[i].getNetworkInterfaces()));
            }

            iaasConfigsArray[i] = iaasConfig;
        }
        return iaasConfigsArray;
    }

    public static Persistence convertPersistenceBeanToStubPersistence(PersistenceBean persistenceBean) {
        Persistence persistence = new Persistence();
        persistence.setPersistanceRequired(persistenceBean.isRequired());
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

    public static Properties convertPropertyBeansToProperties(List<PropertyBean> propertyBeans) {
        PropertyBean[] propertyBeansArray = new PropertyBean[propertyBeans.size()];
        propertyBeans.toArray(propertyBeansArray);
        Property[] propertyArray = new Property[propertyBeansArray.length];

        for (int j = 0; j < propertyBeansArray.length; j++) {
            Property property = new Property();
            property.setName(propertyBeansArray[j].getName());
            property.setValue(propertyBeansArray[j].getValue());
            propertyArray[j] = property;
        }

        Properties properties = new Properties();
        properties.setProperties(propertyArray);
        return properties;
    }
    
    public static org.apache.stratos.cloud.controller.stub.Properties convertPropertyBeansToCCStubProperties(
            List<PropertyBean> propertyBeans) {

        //convert to an array
        PropertyBean[] propertyBeansArray = new PropertyBean[propertyBeans.size()];
        propertyBeans.toArray(propertyBeansArray);
        org.apache.stratos.cloud.controller.stub.Property[] propertyArray = new org.apache.stratos.cloud.controller.stub.Property[propertyBeansArray.length];

        for (int j = 0; j < propertyBeansArray.length; j++) {
            org.apache.stratos.cloud.controller.stub.Property property = new org.apache.stratos.cloud.controller.stub.Property();
            property.setName(propertyBeansArray[j].getName());
            property.setValue(propertyBeansArray[j].getValue());
            propertyArray[j] = property;
        }

        org.apache.stratos.cloud.controller.stub.Properties properties = new org.apache.stratos.cloud.controller.stub.Properties();
        properties.setProperties(propertyArray);
        return properties;
    }


    public static org.apache.stratos.autoscaler.stub.Properties convertProperyBeansToStubProperties(
            List<PropertyBean> propertyBeans) {
        if (propertyBeans == null || propertyBeans.isEmpty()) {
            return null;
        }

        //convert to an array
        PropertyBean[] propertyBeansArray = new PropertyBean[propertyBeans.size()];
        propertyBeans.toArray(propertyBeansArray);
        org.apache.stratos.autoscaler.stub.Property[] propertyArray = new org.apache.stratos.autoscaler.stub.Property[propertyBeansArray.length];

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

    private static NetworkInterfaces convertNetworkInterfaceBeansToNetworkInterfaces(List<NetworkInterfaceBean> networkInterfaceBeans) {
        NetworkInterface[] networkInterfacesArray = new NetworkInterface[networkInterfaceBeans.size()];

        int i = 0;
        for (NetworkInterfaceBean nib : networkInterfaceBeans) {
            NetworkInterface networkInterface = new NetworkInterface();
            networkInterface.setNetworkUuid(nib.getNetworkUuid());
            networkInterface.setFixedIp(nib.getFixedIp());
            networkInterface.setPortUuid(nib.getPortUuid());
            if (nib.getFloatingNetworks() != null && !nib.getFloatingNetworks().isEmpty()) {
            	networkInterface.setFloatingNetworks(ObjectConverter.convertFloatingNetworkBeansToFloatingNetworks(nib.getFloatingNetworks()));
            }

            networkInterfacesArray[i++] = networkInterface;
        }

        NetworkInterfaces networkInterfaces = new NetworkInterfaces();
        networkInterfaces.setNetworkInterfaces(networkInterfacesArray);
        return networkInterfaces;
    }
    
    private static FloatingNetworks convertFloatingNetworkBeansToFloatingNetworks(List<FloatingNetworkBean> floatingNetworkBeans) {

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

    public static org.apache.stratos.autoscaler.stub.deployment.partition.Partition convertToCCPartitionPojo
            (Partition partitionBean) {

        org.apache.stratos.autoscaler.stub.deployment.partition.Partition partition = new
                org.apache.stratos.autoscaler.stub.deployment.partition.Partition();

        partition.setId(partitionBean.getId());
        partition.setDescription(partitionBean.getDescription());
        partition.setIsPublic(partitionBean.isPublic());
        partition.setProvider(partitionBean.getProvider());

        if (partitionBean.getProperty() != null && !partitionBean.getProperty().isEmpty()) {
            partition.setProperties(convertProperyBeansToStubProperties(partitionBean.getProperty()));
        }

        return partition;
    }

    public static org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy convertToCCAutoscalerPojo(AutoscalePolicy
                                                                                                                        autoscalePolicyBean) {

        org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy autoscalePolicy = new
                org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy();

        autoscalePolicy.setId(autoscalePolicyBean.getId());
        autoscalePolicy.setDescription(autoscalePolicyBean.getDescription());
        autoscalePolicy.setIsPublic(autoscalePolicyBean.getIsPublic());
        autoscalePolicy.setDisplayName(autoscalePolicyBean.getDisplayName());
        autoscalePolicy.setInstanceRoundingFactor(autoscalePolicyBean.getInstanceRoundingFactor());

        if (autoscalePolicyBean.getLoadThresholds() != null) {

            org.apache.stratos.autoscaler.stub.autoscale.policy.LoadThresholds loadThresholds = new
                    org.apache.stratos.autoscaler.stub.autoscale.policy.LoadThresholds();

            if (autoscalePolicyBean.getLoadThresholds().getLoadAverage() != null) {

                //set load average information
                org.apache.stratos.autoscaler.stub.autoscale.policy.LoadAverageThresholds loadAverage = new
                        org.apache.stratos.autoscaler.stub.autoscale.policy.LoadAverageThresholds();
                loadAverage.setUpperLimit(autoscalePolicyBean.getLoadThresholds().getLoadAverage().getThreshold());
                //set load average
                loadThresholds.setLoadAverage(loadAverage);
            }
            if (autoscalePolicyBean.getLoadThresholds().getRequestsInFlight() != null) {

                org.apache.stratos.autoscaler.stub.autoscale.policy.RequestsInFlightThresholds requestsInFlight = new
                        org.apache.stratos.autoscaler.stub.autoscale.policy.RequestsInFlightThresholds();
                //set request in flight information
                requestsInFlight.setUpperLimit(autoscalePolicyBean.getLoadThresholds().getRequestsInFlight().getThreshold());
                //set request in flight
                loadThresholds.setRequestsInFlight(requestsInFlight);
            }
            if (autoscalePolicyBean.getLoadThresholds().getMemoryConsumption() != null) {

                org.apache.stratos.autoscaler.stub.autoscale.policy.MemoryConsumptionThresholds memoryConsumption = new
                        org.apache.stratos.autoscaler.stub.autoscale.policy.MemoryConsumptionThresholds();

                //set memory consumption information
                memoryConsumption.setUpperLimit(autoscalePolicyBean.getLoadThresholds().getMemoryConsumption().getThreshold());
                //set memory consumption
                loadThresholds.setMemoryConsumption(memoryConsumption);
            }

            autoscalePolicy.setLoadThresholds(loadThresholds);
        }

        return autoscalePolicy;
    }

    public static org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy convetToASDeploymentPolicyPojo(
            DeploymentPolicy deploymentPolicyBean) {

        org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy deploymentPolicy =
                new org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy();

        deploymentPolicy.setApplicationId(deploymentPolicyBean.getApplicationId());
        deploymentPolicy.setDescription(deploymentPolicyBean.getDescription());
        deploymentPolicy.setIsPublic(deploymentPolicyBean.isPublic());
        if (deploymentPolicyBean.getApplicationPolicy() != null
                && deploymentPolicyBean.getApplicationPolicy().getNetworkPartition() != null
                && !deploymentPolicyBean.getApplicationPolicy().getNetworkPartition().isEmpty()) {
            deploymentPolicy
                    .setApplicationLevelNetworkPartitions(convertToCCPartitionGroup(deploymentPolicyBean.getApplicationPolicy().getNetworkPartition()));
        }

        if (deploymentPolicyBean.getChildPolicies() != null && !deploymentPolicyBean.getChildPolicies().isEmpty()) {
            deploymentPolicy.setChildPolicies(convertChildPoliciesToStubChildPolicies(deploymentPolicyBean.getChildPolicies()));
        }

        return deploymentPolicy;
    }

    public static DeploymentPolicy convertStubDeploymentPolicyToDeploymentPolicy(
            org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy stubDeploymentPolicy) {

        if(stubDeploymentPolicy == null) {
            return null;
        }

        DeploymentPolicy deploymentPolicy = new DeploymentPolicy();
        deploymentPolicy.setApplicationId(stubDeploymentPolicy.getApplicationId());
        deploymentPolicy.setDescription(stubDeploymentPolicy.getDescription());
        deploymentPolicy.setPublic(stubDeploymentPolicy.getIsPublic());
        if (stubDeploymentPolicy.getApplicationLevelNetworkPartitions() != null) {
            org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition[]
                    networkPartitions = stubDeploymentPolicy.getApplicationLevelNetworkPartitions();
            if(networkPartitions != null) {
                deploymentPolicy.setApplicationPolicy(new ApplicationPolicy());
                List<ApplicationLevelNetworkPartition> networkPartitionList = new ArrayList<ApplicationLevelNetworkPartition>();
                for(org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition
                        networkPartition : networkPartitions) {
                    if(networkPartition != null) {
                        networkPartitionList.add(convertStubNetworkPartitionToNetworkPartition(networkPartition));
                    }
                }
                deploymentPolicy.getApplicationPolicy().setNetworkPartition(networkPartitionList);
            }
        }

        if (stubDeploymentPolicy.getChildPolicies() != null) {
            List<org.apache.stratos.common.beans.autoscaler.policy.deployment.ChildPolicy> childPolicyList =
                    new ArrayList<org.apache.stratos.common.beans.autoscaler.policy.deployment.ChildPolicy>();
            for(org.apache.stratos.autoscaler.stub.deployment.policy.ChildPolicy stubChildDeploymentPolicy :
                    stubDeploymentPolicy.getChildPolicies()) {
                if(stubChildDeploymentPolicy != null) {
                    childPolicyList.add(convertStubChildPolicyToChildPolicy(stubChildDeploymentPolicy));
                }
            }
            deploymentPolicy.setChildPolicies(childPolicyList);
        }
        return deploymentPolicy;
    }

    private static org.apache.stratos.common.beans.autoscaler.policy.deployment.ChildPolicy
    convertStubChildPolicyToChildPolicy(ChildPolicy stubChildDeploymentPolicy) {
        if(stubChildDeploymentPolicy == null) {
            return null;
        }
        org.apache.stratos.common.beans.autoscaler.policy.deployment.ChildPolicy childPolicy = new
                org.apache.stratos.common.beans.autoscaler.policy.deployment.ChildPolicy();
        childPolicy.setAlias(stubChildDeploymentPolicy.getAlias());
        if(stubChildDeploymentPolicy.getChildLevelNetworkPartitions() != null) {
            List<org.apache.stratos.common.beans.autoscaler.partition.ChildLevelNetworkPartition> networkPartitionList
                    = new ArrayList<org.apache.stratos.common.beans.autoscaler.partition.ChildLevelNetworkPartition>();
            for(org.apache.stratos.autoscaler.stub.deployment.partition.ChildLevelNetworkPartition
                    stubChildLevelNetworkPartition : stubChildDeploymentPolicy.getChildLevelNetworkPartitions()) {
                networkPartitionList.add(convertStubChildLevelNetworkPartitionToChildLevelNetworkPartition(stubChildLevelNetworkPartition));
            }
            childPolicy.setNetworkPartition(networkPartitionList);
        }
        return childPolicy;
    }

    private static org.apache.stratos.common.beans.autoscaler.partition.ChildLevelNetworkPartition
    convertStubChildLevelNetworkPartitionToChildLevelNetworkPartition(
            ChildLevelNetworkPartition stubChildLevelNetworkPartition) {
        if(stubChildLevelNetworkPartition == null) {
            return null;
        }
        org.apache.stratos.common.beans.autoscaler.partition.ChildLevelNetworkPartition childLevelNetworkPartition =
                new org.apache.stratos.common.beans.autoscaler.partition.ChildLevelNetworkPartition();
        childLevelNetworkPartition.setId(stubChildLevelNetworkPartition.getId());
        childLevelNetworkPartition.setPartitionAlgo(stubChildLevelNetworkPartition.getPartitionAlgo());
        if(stubChildLevelNetworkPartition.getChildLevelPartitions() != null) {
            List<org.apache.stratos.common.beans.autoscaler.partition.ChildLevelPartition> partitionList =
                    new ArrayList<org.apache.stratos.common.beans.autoscaler.partition.ChildLevelPartition>();
            for(org.apache.stratos.autoscaler.stub.deployment.partition.ChildLevelPartition stubChildLevelPartition : stubChildLevelNetworkPartition.getChildLevelPartitions()) {
                partitionList.add(convertStubChildLevelPartitionToChildLevelPartition(stubChildLevelPartition));
            }
            childLevelNetworkPartition.setPartitions(partitionList);
        }
        return childLevelNetworkPartition;
    }

    private static org.apache.stratos.common.beans.autoscaler.partition.ChildLevelPartition
    convertStubChildLevelPartitionToChildLevelPartition(ChildLevelPartition stubChildLevelPartition) {
        if(stubChildLevelPartition == null) {
            return null;
        }
        org.apache.stratos.common.beans.autoscaler.partition.ChildLevelPartition childLevelPartition =
                new org.apache.stratos.common.beans.autoscaler.partition.ChildLevelPartition();
        childLevelPartition.setId(stubChildLevelPartition.getPartitionId());
        childLevelPartition.setMax(stubChildLevelPartition.getMax());
        return childLevelPartition;
    }

    private static ApplicationLevelNetworkPartition convertStubNetworkPartitionToNetworkPartition(
            org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition stubNetworkPartition) {
        if(stubNetworkPartition == null) {
            return null;
        }

        ApplicationLevelNetworkPartition networkPartition = new ApplicationLevelNetworkPartition();
        networkPartition.setId(stubNetworkPartition.getId());
        networkPartition.setActiveByDefault(stubNetworkPartition.getActiveByDefault());
        if(stubNetworkPartition.getPartitions() != null) {
            List<Partition> partitionList = new ArrayList<Partition>();
            for(org.apache.stratos.autoscaler.stub.deployment.partition.Partition stubPartition :
                    stubNetworkPartition.getPartitions()) {
                if(stubPartition != null) {
                    partitionList.add(convertStubPartitionToPartition(stubPartition));
                }
            }
            networkPartition.setPartitions(partitionList);
        }
        return networkPartition;
    }

    private static Partition convertStubPartitionToPartition(org.apache.stratos.autoscaler.stub.deployment.partition.Partition stubPartition) {
        if(stubPartition == null) {
            return null;
        }
        Partition partition = new Partition();
        partition.setId(stubPartition.getId());
        partition.setPublic(stubPartition.getIsPublic());
        partition.setDescription(stubPartition.getDescription());
        partition.setProvider(stubPartition.getProvider());
        if(stubPartition.getProperties() != null) {
            List<PropertyBean> propertyBeanList = new ArrayList<PropertyBean>();
            for(org.apache.stratos.autoscaler.stub.Property stubProperty : stubPartition.getProperties().getProperties()) {
                if(stubProperty != null) {
                    propertyBeanList.add(convertStubPropertyToPropertyBean(stubProperty));
                }
            }
            partition.setProperty(propertyBeanList);
        }
        return partition;
    }

    private static PropertyBean convertStubPropertyToPropertyBean(org.apache.stratos.autoscaler.stub.Property stubProperty) {
        if(stubProperty == null) {
            return null;
        }

        PropertyBean propertyBean = new PropertyBean();
        propertyBean.setName(stubProperty.getName());
        propertyBean.setValue(stubProperty.getValue());
        return propertyBean;
    }

    private static org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition[] convertToCCPartitionGroup(List<ApplicationLevelNetworkPartition> networkPartitionBeans) {

        org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition[]
                appNWPartitions = new
                org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition
                [networkPartitionBeans.size()];

        for (int i = 0; i < networkPartitionBeans.size(); i++) {
            org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition appNWPartition = new
                    org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition();
            appNWPartition.setId(networkPartitionBeans.get(i).getId());
            appNWPartition.setActiveByDefault(networkPartitionBeans.get(i).isActiveByDefault());
            if (networkPartitionBeans.get(i).getPartitions() != null && !networkPartitionBeans.get(i).getPartitions().isEmpty()) {
                appNWPartition.setPartitions(convertToCCPartitionPojos(networkPartitionBeans.get(i).getPartitions()));
            }

            appNWPartitions[i] = appNWPartition;
        }

        return appNWPartitions;
    }

    private static ChildPolicy[] convertChildPoliciesToStubChildPolicies(List<org.apache.stratos.common.beans.autoscaler.policy.deployment.ChildPolicy> childPolicies) {
        ChildPolicy[] stubChildPolicyArray = new ChildPolicy[childPolicies.size()];
        for (int i = 0; i < childPolicies.size(); i++) {
            ChildPolicy childPolicy = new ChildPolicy();
            childPolicy.setAlias(childPolicies.get(i).getAlias());
            childPolicy.setChildLevelNetworkPartitions(convertToCCChildNetworkPartition(childPolicies.get(i).getNetworkPartition()));
            stubChildPolicyArray[i] = childPolicy;
        }
        return stubChildPolicyArray;
    }

    private static ChildLevelNetworkPartition[] convertToCCChildNetworkPartition(List<org.apache.stratos.common.beans.autoscaler.partition.ChildLevelNetworkPartition> networkPartitions) {

        ChildLevelNetworkPartition[] childLevelNetworkPartitions = new ChildLevelNetworkPartition[networkPartitions.size()];

        for (int i = 0; i < networkPartitions.size(); i++) {
            ChildLevelNetworkPartition childLevelNetworkPartition = new ChildLevelNetworkPartition();
            childLevelNetworkPartition.setId(networkPartitions.get(i).getId());
            childLevelNetworkPartition.setPartitionAlgo(networkPartitions.get(i).getPartitionAlgo());
            childLevelNetworkPartition.setChildLevelPartitions(convertToCCChildPartitionPojos(networkPartitions.get(i).getPartitions()));

            childLevelNetworkPartitions[i] = childLevelNetworkPartition;
        }

        return childLevelNetworkPartitions;
    }

    public static org.apache.stratos.common.beans.topology.Cluster convertClusterToClusterBean(Cluster cluster, String alias) {
        org.apache.stratos.common.beans.topology.Cluster clusterBean = new
                org.apache.stratos.common.beans.topology.Cluster();
        clusterBean.setAlias(alias);
        clusterBean.setServiceName(cluster.getServiceName());
        clusterBean.setClusterId(cluster.getClusterId());
        clusterBean.setLbCluster(cluster.isLbCluster());
        clusterBean.setTenantRange(cluster.getTenantRange());
        clusterBean.setProperty(convertJavaUtilPropertiesToPropertyBeans(cluster.getProperties()));
        clusterBean.setMember(new ArrayList<Member>());
        clusterBean.setHostNames(new ArrayList<String>());
        Collection<ClusterInstance> clusterInstances = cluster.getClusterInstances();
        List<org.apache.stratos.common.beans.topology.Instance> instancesList =
        		new ArrayList<org.apache.stratos.common.beans.topology.Instance>();
		if (clusterInstances != null) {
			for (ClusterInstance clusterInstance : clusterInstances) {
				org.apache.stratos.common.beans.topology.Instance instance =
						new org.apache.stratos.common.beans.topology.Instance();
				instance.setInstanceId(clusterInstance.getInstanceId());
				instance.setStatus(clusterInstance.getStatus().toString());
				instancesList.add(instance);
			}
			clusterBean.setInstances(instancesList);
		}

        for (org.apache.stratos.messaging.domain.topology.Member member : cluster.getMembers()) {
            Member memberBean = new Member();
            memberBean.setServiceName(member.getServiceName());
            memberBean.setClusterId(member.getClusterId());
            memberBean.setMemberId(member.getMemberId());
            memberBean.setInstanceId(member.getInstanceId());
            memberBean.setClusterInstanceId(member.getClusterInstanceId());

            memberBean.setLbClusterId(member.getLbClusterId());
            memberBean.setNetworkPartitionId(member.getNetworkPartitionId());
            memberBean.setPartitionId(member.getPartitionId());
            if (member.getMemberIp() == null) {
                memberBean.setMemberIp("NULL");
            } else {
                memberBean.setMemberIp(member.getMemberIp());
            }
            if (member.getMemberPublicIp() == null) {
                memberBean.setMemberPublicIp("NULL");
            } else {
                memberBean.setMemberPublicIp(member.getMemberPublicIp());
            }
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
        clusterInstanceBean.setMember(new ArrayList<Member>());
        clusterInstanceBean.setHostNames(new ArrayList<String>());

        for (org.apache.stratos.messaging.domain.topology.Member member : cluster.getMembers()) {
            if (member.getClusterInstanceId().equals(instanceId)) {
                Member memberBean = new Member();
                memberBean.setClusterId(member.getClusterId());
                memberBean.setLbClusterId(member.getLbClusterId());
                memberBean.setNetworkPartitionId(member.getNetworkPartitionId());
                memberBean.setPartitionId(member.getPartitionId());
                memberBean.setMemberId(member.getMemberId());
                if (member.getMemberIp() == null) {
                    memberBean.setMemberIp("NULL");
                } else {
                    memberBean.setMemberIp(member.getMemberIp());
                }
                if (member.getMemberPublicIp() == null) {
                    memberBean.setMemberPublicIp("NULL");
                } else {
                    memberBean.setMemberPublicIp(member.getMemberPublicIp());
                }
                memberBean.setServiceName(member.getServiceName());
                memberBean.setStatus(member.getStatus().toString());
                memberBean.setProperty(convertJavaUtilPropertiesToPropertyBeans(member.getProperties()));
                clusterInstanceBean.getMember().add(memberBean);
            }

        }

        for (String hostname : cluster.getHostNames()) {
            clusterInstanceBean.getHostNames().add(hostname);
        }
        return clusterInstanceBean;
    }

    private static org.apache.stratos.autoscaler.stub.deployment.partition.Partition[] convertToCCPartitionPojos
            (List<Partition> partitionList) {

        org.apache.stratos.autoscaler.stub.deployment.partition.Partition[] partitions =
                new org.apache.stratos.autoscaler.stub.deployment.partition.Partition[partitionList.size()];
        for (int i = 0; i < partitionList.size(); i++) {
            partitions[i] = convertToCCPartitionPojo(partitionList.get(i));
        }

        return partitions;
    }

    private static ChildLevelPartition[] convertToCCChildPartitionPojos
            (List<org.apache.stratos.common.beans.autoscaler.partition.ChildLevelPartition> partitionList) {

        ChildLevelPartition[] childLevelPartitions = new ChildLevelPartition[partitionList.size()];
        for (int i = 0; i < partitionList.size(); i++) {
            ChildLevelPartition childLevelPartition = new ChildLevelPartition();
            childLevelPartition.setPartitionId(partitionList.get(i).getId());
            childLevelPartition.setMax(partitionList.get(i).getMax());

            childLevelPartitions[i] = childLevelPartition;
        }

        return childLevelPartitions;
    }

    public static Partition[] populatePartitionPojos(org.apache.stratos.cloud.controller.stub.domain.Partition[]
                                                             partitions) {

        Partition[] partitionBeans;
        if (partitions == null) {
            partitionBeans = new Partition[0];
            return partitionBeans;
        }

        partitionBeans = new Partition[partitions.length];
        for (int i = 0; i < partitions.length; i++) {
            partitionBeans[i] = populatePartitionPojo(partitions[i]);
        }
        return partitionBeans;
    }

    public static Partition populatePartitionPojo(org.apache.stratos.cloud.controller.stub.domain.Partition
                                                          partition) {

        Partition partitionBeans = new Partition();
        if (partition == null) {
            return partitionBeans;
        }

        partitionBeans.setId(partition.getId());
        partitionBeans.setDescription(partition.getDescription());
        partitionBeans.setPublic(partition.getIsPublic());
        partitionBeans.setProvider(partition.getProvider());
        /*partitionBeans.partitionMin = partition.getPartitionMin();
        partitionBeans.partitionMax = partition.getPartitionMax();*/
        //properties 
        if (partition.getProperties() != null) {
            List<PropertyBean> propertyBeans = convertCCStubPropertiesToPropertyBeans(partition.getProperties());
            partitionBeans.setProperty(propertyBeans);
        }

        return partitionBeans;
    }

    public static List<SubscriptionDomainBean> populateSubscriptionDomainPojos(List<SubscriptionDomain> subscriptionDomains) {

        List<SubscriptionDomainBean> subscriptionDomainBeans = new ArrayList<SubscriptionDomainBean>();

        if (subscriptionDomains == null) {
            return subscriptionDomainBeans;
        }

        for (SubscriptionDomain subscriptionDomain : subscriptionDomains) {
            subscriptionDomainBeans.add(populateSubscriptionDomainPojo(subscriptionDomain));
        }

        return subscriptionDomainBeans;
    }

    public static SubscriptionDomainBean populateSubscriptionDomainPojo(SubscriptionDomain subscriptionDomain) {

        SubscriptionDomainBean subscriptionDomainBean = new SubscriptionDomainBean();

        if (subscriptionDomain == null) {
            return subscriptionDomainBean;
        }
        subscriptionDomainBean.setDomainName(subscriptionDomain.getDomainName());
        subscriptionDomainBean.setApplicationContext(subscriptionDomain.getApplicationContext());

        return subscriptionDomainBean;
    }

    private static List<PropertyBean> convertJavaUtilPropertiesToPropertyBeans(java.util.Properties properties) {

        List<PropertyBean> propertyBeans = null;
        if (properties != null && !properties.isEmpty()) {
            Enumeration<?> e = properties.propertyNames();
            propertyBeans = new ArrayList<PropertyBean>();

            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String value = properties.getProperty(key);
                PropertyBean propertyBean = new PropertyBean();
                propertyBean.setName(key);
                propertyBean.setValue(value);
                propertyBeans.add(propertyBean);
            }
        }
        return propertyBeans;
    }

    public static AutoscalePolicy[] convertStubAutoscalePoliciesToAutoscalePolicies(
            org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy[] autoscalePolicies) {

        AutoscalePolicy[] autoscalePolicyBeans;
        if (autoscalePolicies == null) {
            autoscalePolicyBeans = new AutoscalePolicy[0];
            return autoscalePolicyBeans;
        }

        autoscalePolicyBeans = new AutoscalePolicy[autoscalePolicies.length];
        for (int i = 0; i < autoscalePolicies.length; i++) {
            autoscalePolicyBeans[i] = convertStubAutoscalePolicyToAutoscalePolicy(autoscalePolicies[i]);
        }
        return autoscalePolicyBeans;
    }

    public static AutoscalePolicy convertStubAutoscalePolicyToAutoscalePolicy(org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy
                                                                                      autoscalePolicy) {
        if (autoscalePolicy == null) {
            return null;
        }

        AutoscalePolicy autoscalePolicyBean = new AutoscalePolicy();
        autoscalePolicyBean.setId(autoscalePolicy.getId());
        autoscalePolicyBean.setDescription(autoscalePolicy.getDescription());
        autoscalePolicyBean.setIsPublic(autoscalePolicy.getIsPublic());
        autoscalePolicyBean.setDisplayName(autoscalePolicy.getDisplayName());
        autoscalePolicyBean.setDescription(autoscalePolicy.getDescription());
        autoscalePolicyBean.setInstanceRoundingFactor(autoscalePolicy.getInstanceRoundingFactor());
        if (autoscalePolicy.getLoadThresholds() != null) {
            autoscalePolicyBean.setLoadThresholds(convertStubLoadThreasholdsToLoadThresholds(autoscalePolicy.getLoadThresholds()));
        }

        return autoscalePolicyBean;
    }

    private static LoadThresholds convertStubLoadThreasholdsToLoadThresholds(org.apache.stratos.autoscaler.stub.autoscale.policy.LoadThresholds
                                                                                     loadThresholds) {

        LoadThresholds loadThresholdBean = new LoadThresholds();
        if (loadThresholds.getLoadAverage() != null) {
            LoadAverageThresholds loadAverage = new LoadAverageThresholds();
            loadAverage.setThreshold(loadThresholds.getLoadAverage().getUpperLimit());
            loadThresholdBean.setLoadAverage(loadAverage);
        }
        if (loadThresholds.getMemoryConsumption() != null) {
            MemoryConsumptionThresholds memoryConsumption = new MemoryConsumptionThresholds();
            memoryConsumption.setThreshold(loadThresholds.getMemoryConsumption().getUpperLimit());
            loadThresholdBean.setMemoryConsumption(memoryConsumption);
        }
        if (loadThresholds.getRequestsInFlight() != null) {
            RequestsInFlightThresholds requestsInFlight = new RequestsInFlightThresholds();
            requestsInFlight.setThreshold(loadThresholds.getRequestsInFlight().getUpperLimit());
            loadThresholdBean.setRequestsInFlight(requestsInFlight);
        }

        return loadThresholdBean;
    }

    public static ApplicationLevelNetworkPartition convertStubApplicationLevelNetworkPartitionToApplicationLevelNetworkPartition(
            org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition stubApplicationLevelNetworkPartition) {

        ApplicationLevelNetworkPartition networkPartitionBean = new ApplicationLevelNetworkPartition();
        if (stubApplicationLevelNetworkPartition == null) {
            return networkPartitionBean;
        }

        networkPartitionBean.setId(stubApplicationLevelNetworkPartition.getId());
        networkPartitionBean.setActiveByDefault(stubApplicationLevelNetworkPartition.getActiveByDefault());

        //FIXME update with new deployment policy pattern
//        networkPartitionBean.partitionAlgo = partitionGroup.getPartitionAlgo();
//        if (partitionGroup.getPartitions() != null && partitionGroup.getPartitions().length > 0) {
//            partitionGroupBean.partition = convertStubPartitionsToPartitions(partitionGroup.getPartitions());
//        }

        return networkPartitionBean;
    }

    public static ApplicationLevelNetworkPartition[] convertStubApplicationLevelNetworkPartitionsToApplicationLevelNetworkPartitions(
            org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition[] partitionGroups) {

        ApplicationLevelNetworkPartition[] networkPartitionGroupsBeans;
        if (partitionGroups == null) {
            networkPartitionGroupsBeans = new ApplicationLevelNetworkPartition[0];
            return networkPartitionGroupsBeans;
        }

        networkPartitionGroupsBeans = new ApplicationLevelNetworkPartition[partitionGroups.length];

        for (int i = 0; i < partitionGroups.length; i++) {
            networkPartitionGroupsBeans[i] = convertStubApplicationLevelNetworkPartitionToApplicationLevelNetworkPartition(partitionGroups[i]);
        }

        return networkPartitionGroupsBeans;
    }

    public static ServiceDefinitionBean convertToServiceDefinitionBean(Service service) {

        ServiceDefinitionBean serviceDefinitionBean = new ServiceDefinitionBean();
        serviceDefinitionBean.setCartridgeType(service.getType());
        serviceDefinitionBean.setTenantRange(service.getTenantRange());
        serviceDefinitionBean.setClusterDomain(service.getClusterId());
        serviceDefinitionBean.setIsPublic(service.getIsPublic());
        serviceDefinitionBean.setAutoscalingPolicyName(service.getAutoscalingPolicyName());
        serviceDefinitionBean.setDeploymentPolicyName(service.getDeploymentPolicyName());

        return serviceDefinitionBean;
    }

    public static List<ServiceDefinitionBean> convertToServiceDefinitionBeans(Collection<Service> services) {

        List<ServiceDefinitionBean> serviceDefinitionBeans = new ArrayList<ServiceDefinitionBean>();

        for (Service service : services) {
            serviceDefinitionBeans.add(convertToServiceDefinitionBean(service));
        }
        return serviceDefinitionBeans;
    }

    public static org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesGroup convertToCCKubernetesGroupPojo(KubernetesGroup kubernetesGroupBean) {

        org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesGroup kubernetesGroup = new
                org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesGroup();

        kubernetesGroup.setGroupId(kubernetesGroupBean.getGroupId());
        kubernetesGroup.setDescription(kubernetesGroupBean.getDescription());
        kubernetesGroup.setKubernetesMaster(convertStubKubernetesMasterToKubernetesMaster(kubernetesGroupBean.getKubernetesMaster()));
        kubernetesGroup.setPortRange(convertPortRangeToStubPortRange(kubernetesGroupBean.getPortRange()));
        kubernetesGroup.setKubernetesHosts(convertToASKubernetesHostsPojo(kubernetesGroupBean.getKubernetesHosts()));
        kubernetesGroup.setProperties((convertPropertyBeansToCCStubProperties(kubernetesGroupBean.getProperty())));

        return kubernetesGroup;
    }

    private static org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost[]
        convertToASKubernetesHostsPojo(List<KubernetesHost> kubernetesHosts) {

        if (kubernetesHosts == null || kubernetesHosts.isEmpty()) {
            return null;
        }
        int kubernetesHostCount = kubernetesHosts.size();
        org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost[]
                kubernetesHostsArr = new org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost[kubernetesHostCount];
        for (int i = 0; i < kubernetesHostCount; i++) {
            KubernetesHost kubernetesHostBean = kubernetesHosts.get(i);
            kubernetesHostsArr[i] = convertKubernetesHostToStubKubernetesHost(kubernetesHostBean);
        }
        return kubernetesHostsArr;
    }


    private static org.apache.stratos.cloud.controller.stub.kubernetes.PortRange
        convertPortRangeToStubPortRange(PortRange portRangeBean) {

        if (portRangeBean == null) {
            return null;
        }
        org.apache.stratos.cloud.controller.stub.kubernetes.PortRange
                portRange = new org.apache.stratos.cloud.controller.stub.kubernetes.PortRange();
        portRange.setLower(portRangeBean.getLower());
        portRange.setUpper(portRangeBean.getUpper());
        return portRange;
    }

    public static org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost
        convertKubernetesHostToStubKubernetesHost(KubernetesHost kubernetesHostBean) {

        if (kubernetesHostBean == null) {
            return null;
        }

        org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost
                kubernetesHost = new org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost();
        kubernetesHost.setHostId(kubernetesHostBean.getHostId());
        kubernetesHost.setHostIpAddress(kubernetesHostBean.getHostIpAddress());
        kubernetesHost.setHostname(kubernetesHostBean.getHostname());
        kubernetesHost.setProperties(convertPropertyBeansToCCStubProperties(kubernetesHostBean.getProperty()));

        return kubernetesHost;
    }

    public static org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesMaster
        convertStubKubernetesMasterToKubernetesMaster(KubernetesMaster kubernetesMasterBean) {

        if (kubernetesMasterBean == null) {
            return null;
        }

        org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesMaster
                kubernetesMaster = new org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesMaster();
        kubernetesMaster.setHostId(kubernetesMasterBean.getHostId());
        kubernetesMaster.setHostIpAddress(kubernetesMasterBean.getHostIpAddress());
        kubernetesMaster.setHostname(kubernetesMasterBean.getHostname());
        kubernetesMaster.setEndpoint(kubernetesMasterBean.getEndpoint());
        kubernetesMaster.setProperties(convertPropertyBeansToCCStubProperties(kubernetesMasterBean.getProperty()));

        return kubernetesMaster;
    }

    public static KubernetesGroup[] convertStubKubernetesGroupsToKubernetesGroups(org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesGroup[] kubernetesGroups) {

        if (kubernetesGroups == null) {
            return null;
        }
        KubernetesGroup[] kubernetesGroupsBean = new KubernetesGroup[kubernetesGroups.length];
        for (int i = 0; i < kubernetesGroups.length; i++) {
            kubernetesGroupsBean[i] = convertStubKubernetesGroupToKubernetesGroup(kubernetesGroups[i]);
        }
        return kubernetesGroupsBean;
    }

    public static KubernetesGroup convertStubKubernetesGroupToKubernetesGroup(org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesGroup kubernetesGroup) {
        if (kubernetesGroup == null) {
            return null;
        }
        KubernetesGroup kubernetesGroupBean = new KubernetesGroup();
        kubernetesGroupBean.setGroupId(kubernetesGroup.getGroupId());
        kubernetesGroupBean.setDescription(kubernetesGroup.getDescription());
        kubernetesGroupBean.setPortRange(convertStubPortRangeToPortRange(kubernetesGroup.getPortRange()));
        kubernetesGroupBean.setKubernetesHosts(convertStubKubernetesHostsToKubernetesHosts(kubernetesGroup.getKubernetesHosts()));
        kubernetesGroupBean.setKubernetesMaster(convertStubKubernetesMasterToKubernetesMaster(kubernetesGroup.getKubernetesMaster()));
        kubernetesGroupBean.setProperty(convertCCStubPropertiesToPropertyBeans(kubernetesGroup.getProperties()));
        return kubernetesGroupBean;
    }

    public static KubernetesMaster convertStubKubernetesMasterToKubernetesMaster(org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesMaster kubernetesMaster) {
        if (kubernetesMaster == null) {
            return null;
        }
        KubernetesMaster kubernetesMasterBean = new KubernetesMaster();
        kubernetesMasterBean.setHostId(kubernetesMaster.getHostId());
        kubernetesMasterBean.setHostname(kubernetesMaster.getHostname());
        kubernetesMasterBean.setHostIpAddress(kubernetesMaster.getHostIpAddress());
        kubernetesMasterBean.setProperty(convertCCStubPropertiesToPropertyBeans(kubernetesMaster.getProperties()));
        kubernetesMasterBean.setEndpoint(kubernetesMaster.getEndpoint());
        return kubernetesMasterBean;
    }

    public static List<KubernetesHost> convertStubKubernetesHostsToKubernetesHosts(org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost[] kubernetesHosts) {
        if (kubernetesHosts == null) {
            return null;
        }
        List<KubernetesHost> kubernetesHostList = new ArrayList<KubernetesHost>();
        for (int i = 0; i < kubernetesHosts.length; i++) {
            kubernetesHostList.add(convertStubKubernetesHostToKubernetesHost(kubernetesHosts[i]));
        }
        return kubernetesHostList;
    }

    private static KubernetesHost convertStubKubernetesHostToKubernetesHost(
            org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost kubernetesHost) {
        if (kubernetesHost == null) {
            return null;
        }
        KubernetesHost kubernetesHostBean = new KubernetesHost();
        kubernetesHostBean.setHostId(kubernetesHost.getHostId());
        kubernetesHostBean.setHostname(kubernetesHost.getHostname());
        kubernetesHostBean.setHostIpAddress(kubernetesHost.getHostIpAddress());
        kubernetesHostBean.setProperty(convertCCStubPropertiesToPropertyBeans(kubernetesHost.getProperties()));
        return kubernetesHostBean;
    }
    
    private static List<PropertyBean> convertCCStubPropertiesToPropertyBeans(org.apache.stratos.cloud.controller.stub.Properties properties) {
        if (properties == null || properties.getProperties() == null) {
            return null;
        }
        List<PropertyBean> propertyBeanList = new ArrayList<PropertyBean>();
        for (int i = 0; i < properties.getProperties().length; i++) {
            propertyBeanList.add(convertStubPropertyToPropertyBean(properties.getProperties()[i]));
        }
        return propertyBeanList;
    }

    private static PropertyBean convertAsStubPropertyToPropertyBean(org.apache.stratos.autoscaler.stub.Property propertyE) {
        if (propertyE == null) {
            return null;
        }
        PropertyBean propertyBean = new PropertyBean();
        propertyBean.setName(propertyE.getName());
        propertyBean.setValue(propertyE.getValue());
        return propertyBean;
    }
    
    private static PropertyBean convertStubPropertyToPropertyBean(org.apache.stratos.cloud.controller.stub.Property propertyE) {
        if (propertyE == null) {
            return null;
        }
        PropertyBean propertyBean = new PropertyBean();
        propertyBean.setName(propertyE.getName());
        propertyBean.setValue(propertyE.getValue());
        return propertyBean;
    }

    private static PortRange convertStubPortRangeToPortRange(org.apache.stratos.cloud.controller.stub.kubernetes.PortRange portRange) {
        if (portRange == null) {
            return null;
        }
        PortRange portRangeBean = new PortRange();
        portRangeBean.setUpper(portRange.getUpper());
        portRangeBean.setLower(portRange.getLower());
        return portRangeBean;
    }

    public static ApplicationContext convertApplicationDefinitionToStubApplicationContext(ApplicationDefinition applicationDefinition) {

        org.apache.stratos.autoscaler.stub.pojo.ApplicationContext applicationContext =
                new org.apache.stratos.autoscaler.stub.pojo.ApplicationContext();
        applicationContext.setApplicationId(applicationDefinition.getApplicationId());
        applicationContext.setName(applicationDefinition.getName());
        applicationContext.setDescription(applicationDefinition.getDescription());
        applicationContext.setAlias(applicationDefinition.getAlias());
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
                        convertCartridgeDefinitionsToStubCartridgeContexts(applicationDefinition.getComponents().getCartridges()));
            }
            applicationContext.setComponents(componentContext);
        }
        return applicationContext;
    }

    public static ApplicationDefinition convertStubApplicationContextToApplicationDefinition(
            ApplicationContext applicationContext) {
        if(applicationContext == null) {
            return null;
        }

        ApplicationDefinition applicationDefinition = new ApplicationDefinition();
        applicationDefinition.setApplicationId(applicationContext.getApplicationId());
        applicationDefinition.setName(applicationContext.getName());
        applicationDefinition.setDescription(applicationContext.getDescription());
        applicationDefinition.setStatus(applicationContext.getStatus());
        applicationDefinition.setAlias(applicationContext.getAlias());

        // convert and set components
        if (applicationContext.getComponents() != null) {
            applicationDefinition.setComponents(new ComponentDefinition());
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
                        convertStubCartridgeContextsToCartridgeDefinitions(applicationContext.getComponents().getCartridgeContexts()));
            }
        }
        return applicationDefinition;
    }

    private static List<GroupDefinition> convertStubGroupContextsToGroupDefinitions(GroupContext[] groupContexts) {
        List<GroupDefinition> groupDefinitions = new ArrayList<GroupDefinition>();
        if(groupContexts != null) {
            for (GroupContext groupContext : groupContexts) {
                if(groupContext != null) {
                    GroupDefinition groupDefinition = new GroupDefinition();
                    groupDefinition.setAlias(groupContext.getAlias());
                    groupDefinition.setGroupMaxInstances(groupContext.getGroupMaxInstances());
                    groupDefinition.setGroupMinInstances(groupContext.getGroupMinInstances());
                    groupDefinition.setGroupScalingEnabled(groupContext.getGroupScalingEnabled());
                    groupDefinition.setName(groupContext.getName());
                    groupDefinition.setGroups(convertStubGroupContextsToGroupDefinitions(groupContext.getGroupContexts()));
                    groupDefinition.setCartridges(convertStubCartridgeContextsToCartridgeDefinitions(
                            groupContext.getCartridgeContexts()));
                    groupDefinitions.add(groupDefinition);
                }
            }
        }
        return groupDefinitions;
    }

    private static DependencyDefinitions convertStubDependencyContextsToDependencyDefinitions(DependencyContext dependencyContext) {
        DependencyDefinitions dependencyDefinitions = new DependencyDefinitions();
        dependencyDefinitions.setTerminationBehaviour(dependencyContext.getTerminationBehaviour());

            if(dependencyContext.getStartupOrdersContexts() != null) {
                List<String> startupOrders = new ArrayList<String>();
                for(String item : dependencyContext.getStartupOrdersContexts()) {
                    startupOrders.add(item);
                }
                dependencyDefinitions.setStartupOrders(startupOrders);
            }
            if (dependencyContext.getScalingDependents() != null) {
                List<String> scalingDependents = new ArrayList<String>();
                for(String item : dependencyContext.getScalingDependents()) {
                    scalingDependents.add(item);
                }
                dependencyDefinitions.setScalingDependants(scalingDependents);
            }
        return dependencyDefinitions;
    }

    private static List<CartridgeDefinition> convertStubCartridgeContextsToCartridgeDefinitions(CartridgeContext[] cartridgeContexts) {
        List<CartridgeDefinition> cartridgeDefinitions = new ArrayList<CartridgeDefinition>();
        if(cartridgeContexts != null) {
            for (CartridgeContext cartridgeContext : cartridgeContexts) {
                if(cartridgeContext != null) {
                    CartridgeDefinition cartridgeDefinition = new CartridgeDefinition();
                    cartridgeDefinition.setType(cartridgeContext.getType());
                    cartridgeDefinition.setCartridgeMin(cartridgeContext.getCartridgeMin());
                    cartridgeDefinition.setCartridgeMax(cartridgeContext.getCartridgeMax());
                    cartridgeDefinition.setSubscribableInfo(convertStubSubscribableInfoContextToSubscribableInfo(cartridgeContext.getSubscribableInfoContext()));
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
        if(!CommonUtil.isEmptyArray(subscribableInfoContext.getDependencyAliases())) {
            subscribableInfo.setDependencyAliases(subscribableInfoContext.getDependencyAliases());
        }
        subscribableInfo.setDeploymentPolicy(subscribableInfoContext.getDeploymentPolicy());
        subscribableInfo.setMinMembers(subscribableInfoContext.getMinMembers());
        subscribableInfo.setMaxMembers(subscribableInfoContext.getMaxMembers());
        subscribableInfo.setPrivateRepo(subscribableInfoContext.getPrivateRepo());
        subscribableInfo.setProperty(convertStubPropertiesToPropertyBeanList(subscribableInfoContext.getProperties()));
        subscribableInfo.setRepoPassword(subscribableInfoContext.getRepoPassword());
        subscribableInfo.setRepoUsername(subscribableInfoContext.getRepoUsername());
        subscribableInfo.setRepoUrl(subscribableInfoContext.getRepoUrl());
        return subscribableInfo;
    }

    private static List<org.apache.stratos.manager.composite.application.beans.PropertyBean>
        convertStubPropertiesToPropertyBeanList(org.apache.stratos.autoscaler.stub.Properties properties) {

        List<org.apache.stratos.manager.composite.application.beans.PropertyBean> propertyBeanList =
                new ArrayList<org.apache.stratos.manager.composite.application.beans.PropertyBean>();
        if((properties != null) && (properties.getProperties() != null)) {
            for (org.apache.stratos.autoscaler.stub.Property property : properties.getProperties()) {
                if(property != null) {
                    org.apache.stratos.manager.composite.application.beans.PropertyBean propertyBean =
                            new org.apache.stratos.manager.composite.application.beans.PropertyBean();
                    propertyBean.setName(property.getName());
                    propertyBean.setValue(property.getValue());
                    propertyBeanList.add(propertyBean);
                }
            }
        }
        return propertyBeanList;
    }

    private static CartridgeContext[] convertCartridgeDefinitionsToStubCartridgeContexts(
            List<CartridgeDefinition> cartridges) {

    	CartridgeContext[] cartridgeContextArray = new CartridgeContext[cartridges.size()];
    	int i = 0;
    	for (CartridgeDefinition cartridgeDefinition : cartridges) {
    		CartridgeContext context = new CartridgeContext();
    		context.setCartridgeMax(cartridgeDefinition.getCartridgeMax());
    		context.setCartridgeMin(cartridgeDefinition.getCartridgeMin());
    		context.setType(cartridgeDefinition.getType());
    		context.setSubscribableInfoContext(convertSubscribableInfo(cartridgeDefinition.getSubscribableInfo()));  
    		cartridgeContextArray[i++] = context;
        }
    	
	    return cartridgeContextArray;
    }

	private static SubscribableInfoContext convertSubscribableInfo(
            SubscribableInfo subscribableInfo) {
		SubscribableInfoContext infoContext = new SubscribableInfoContext();
		infoContext.setAlias(subscribableInfo.getAlias());
		infoContext.setAutoscalingPolicy(subscribableInfo.getAutoscalingPolicy());
		infoContext.setDependencyAliases(subscribableInfo.getDependencyAliases());
		infoContext.setDeploymentPolicy(subscribableInfo.getDeploymentPolicy());
		infoContext.setMaxMembers(subscribableInfo.getMaxMembers());
		infoContext.setMinMembers(subscribableInfo.getMinMembers());
		infoContext.setRepoPassword(subscribableInfo.getRepoPassword());
		infoContext.setRepoUrl(subscribableInfo.getRepoUrl());
		infoContext.setRepoUsername(subscribableInfo.getRepoUsername());
		infoContext.setProperties(convertPropertyBeansToStubProperties(subscribableInfo.getProperty()));
		
	    return infoContext;
    }

	private static org.apache.stratos.autoscaler.stub.Properties convertPropertyBeansToStubProperties(
            List<org.apache.stratos.manager.composite.application.beans.PropertyBean> property) {
		org.apache.stratos.autoscaler.stub.Properties prop = new org.apache.stratos.autoscaler.stub.Properties();
		if (property != null) {
			for (org.apache.stratos.manager.composite.application.beans.PropertyBean propertyBean : property) {
				org.apache.stratos.autoscaler.stub.Property p = new org.apache.stratos.autoscaler.stub.Property();
				p.setName(propertyBean.getName());
				p.setValue(propertyBean.getValue());
				prop.addProperties(p);
			}
		}
	    return prop;
    }

	
    private static DependencyContext convertDependencyDefinitionsToDependencyContexts(DependencyDefinitions dependencyDefinitions) {
        DependencyContext dependencyContext = new DependencyContext();
        dependencyContext.setTerminationBehaviour(dependencyDefinitions.getTerminationBehaviour());

        if (dependencyDefinitions != null){
            if(dependencyDefinitions.getStartupOrders() != null) {
                String[] startupOrders = new String[dependencyDefinitions.getStartupOrders().size()];
                startupOrders = dependencyDefinitions.getStartupOrders().toArray(startupOrders);
                dependencyContext.setStartupOrdersContexts(startupOrders);
            }
            if (dependencyDefinitions.getScalingDependants() != null) {
                String[] scalingDependents = new String[dependencyDefinitions.getScalingDependants().size()];
                scalingDependents = dependencyDefinitions.getScalingDependants().toArray(scalingDependents);
                dependencyContext.setScalingDependents(scalingDependents);
            }
        }
        return dependencyContext;
    }

    private static org.apache.stratos.autoscaler.stub.pojo.GroupContext[]
        convertGroupDefinitionsToStubGroupContexts(List<GroupDefinition> groupDefinitions) {

        GroupContext[] groupContexts = new GroupContext[groupDefinitions.size()];
        int i = 0;
        for (GroupDefinition groupDefinition : groupDefinitions) {
            GroupContext groupContext = new GroupContext();
            groupContext.setName(groupDefinition.getName());
            groupContext.setAlias(groupDefinition.getAlias());
            groupContext.setGroupMaxInstances(groupDefinition.getGroupMaxInstances());
            groupContext.setGroupMinInstances(groupDefinition.getGroupMinInstances());
            groupContext.setGroupScalingEnabled(groupDefinition.isGroupScalingEnabled);
           
            // Groups
            if (groupDefinition.getGroups() != null) {
                groupContext.setGroupContexts(convertGroupDefinitionsToStubGroupContexts(groupDefinition.getGroups()));
            }
            
            groupContext.setCartridgeContexts(convertCartridgeDefinitionsToStubCartridgeContexts(groupDefinition.getCartridges()));
            groupContexts[i++] = groupContext;
        }

        return groupContexts;
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
        //applicationBean.set(convertApplicationToApplicationInstanceBean(application));
        return applicationBean;
    }

    public static ApplicationInfoBean convertApplicationToApplicationInstanceBean(Application application) {
        if (application == null) {
            return null;
        }

        ApplicationInfoBean applicationBean = new
                ApplicationInfoBean();
        applicationBean.setId(application.getUniqueIdentifier());
        applicationBean.setName(application.getName());
        applicationBean.setDescription(application.getDescription());
        applicationBean.setTenantDomain(application.getTenantDomain());
        applicationBean.setTenantAdminUsername(application.getTenantAdminUserName());
        applicationBean.setApplicationInstances(convertApplicationInstancesToApplicationInstances(application));
        return applicationBean;
    }

    private static List<ApplicationInstanceBean> convertApplicationInstancesToApplicationInstances(
            Application application) {
        List<ApplicationInstanceBean> applicationInstanceList = new ArrayList<ApplicationInstanceBean>();
        Collection<ApplicationInstance> applicationInstancesInTopology =
                application.getInstanceIdToInstanceContextMap().values();

        if (applicationInstancesInTopology != null) {
            for (ApplicationInstance applicationInstance : applicationInstancesInTopology) {
                ApplicationInstanceBean instance = new ApplicationInstanceBean();
                instance.setInstanceId(applicationInstance.getInstanceId());
                instance.setApplicationId(application.getUniqueIdentifier());
                instance.setParentInstanceId(applicationInstance.getParentId());
                instance.setStatus(applicationInstance.getStatus().toString());
                applicationInstanceList.add(instance);
            }
        }
        return applicationInstanceList;
    }

    public static GroupBean convertGroupToGroupBean(Group group) {
        if (group == null) {
            return null;
        }

        GroupBean groupBean = new GroupBean();
        groupBean.setInstances(convertGroupInstancesToInstances(group));
        groupBean.setAlias(group.getUniqueIdentifier());
        groupBean.setAutoScalingPolicy(group.getAutoscalingPolicy());
        return groupBean;
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
            /*for(Group group1 : group.getGroups()) {
                groupInstanceBean.setGroupInstances(convertGroupToGroupInstancesBean(
                        groupInstance.getInstanceId(), group1));
            }*/
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
                /*for(Group group1 : group.getGroups()) {
                    groupInstanceBean.setGroupInstances(convertGroupToGroupInstancesBean(
                            groupInstance.getInstanceId(), group1));
                }*/
                groupInstanceBeans.add(groupInstanceBean);
            }
        }

        return groupInstanceBeans;
    }

    private static List<Instance> convertGroupInstancesToInstances(Group group) {
	    List<Instance> instanceList = new ArrayList<Instance>();
	    Collection<GroupInstance> instancesInTopology = group.getInstanceIdToInstanceContextMap().values();
	    if(instancesInTopology != null) {
	    	for (GroupInstance groupInstance : instancesInTopology) {
	            Instance instance = new Instance();
	            instance.setStatus(groupInstance.getStatus().toString());
	            instance.setInstanceId(groupInstance.getInstanceId());
	            instanceList.add(instance);
            }
	    }
	    return instanceList;
    }

    public static org.apache.stratos.common.beans.TenantInfoBean convertCarbonTenantInfoBeanToTenantInfoBean(
            TenantInfoBean carbonTenantInfoBean) {

        if(carbonTenantInfoBean == null) {
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
        tenantInfoBean.setFirstname(carbonTenantInfoBean.getFirstname());
        tenantInfoBean.setLastname(carbonTenantInfoBean.getLastname());
        tenantInfoBean.setCreatedDate(carbonTenantInfoBean.getCreatedDate().getTimeInMillis());
        return tenantInfoBean;
    }

    public static TenantInfoBean convertTenantInfoBeanToCarbonTenantInfoBean(
            org.apache.stratos.common.beans.TenantInfoBean tenantInfoBean) {

        if(tenantInfoBean == null) {
            return null;
        }

        TenantInfoBean carbonTenantInfoBean = new TenantInfoBean();
        carbonTenantInfoBean.setTenantId(tenantInfoBean.getTenantId());
        carbonTenantInfoBean.setTenantDomain(tenantInfoBean.getTenantDomain());
        carbonTenantInfoBean.setActive(tenantInfoBean.isActive());
        carbonTenantInfoBean.setAdmin(tenantInfoBean.getAdmin());
        carbonTenantInfoBean.setEmail(tenantInfoBean.getEmail());
        carbonTenantInfoBean.setAdminPassword(tenantInfoBean.getAdminPassword());
        carbonTenantInfoBean.setFirstname(tenantInfoBean.getFirstname());
        carbonTenantInfoBean.setLastname(tenantInfoBean.getLastname());
        if(tenantInfoBean.getCreatedDate() > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(tenantInfoBean.getCreatedDate());
            carbonTenantInfoBean.setCreatedDate(calendar);
        }
        return carbonTenantInfoBean;
    }
}

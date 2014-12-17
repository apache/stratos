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
import org.apache.stratos.cloud.controller.stub.domain.ServiceGroup;
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
import org.apache.stratos.common.beans.topology.Instance;
import org.apache.stratos.common.beans.topology.Member;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.manager.composite.application.beans.*;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.grouping.definitions.DependencyDefinitions;
import org.apache.stratos.manager.grouping.definitions.ServiceGroupDefinition;
import org.apache.stratos.manager.subscription.SubscriptionDomain;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.Group;
import org.apache.stratos.messaging.domain.instance.ApplicationInstance;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
import org.apache.stratos.messaging.domain.topology.Cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

public class ObjectConverter {

    public static CartridgeConfig populateCartridgeConfigPojo(CartridgeDefinitionBean cartridgeDefinitionBean) {

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
	    cartridgeConfig.setDeployerType(cartridgeDefinitionBean.getDeployerType());


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
            cartridgeConfig.setPortMappings(getPortMappingsAsArray(cartridgeDefinitionBean.getPortMapping()));
        }

        //persistance mapping
        if (cartridgeDefinitionBean.getPersistence() != null) {
            cartridgeConfig.setPersistence(getPersistence(cartridgeDefinitionBean.getPersistence()));
        }

        //IaaS
        if (cartridgeDefinitionBean.getIaasProvider() != null && !cartridgeDefinitionBean.getIaasProvider().isEmpty()) {
            cartridgeConfig.setIaasConfigs(getIaasConfigsAsArray(cartridgeDefinitionBean.getIaasProvider()));
        }
        //Properties
        if (cartridgeDefinitionBean.getProperty() != null && !cartridgeDefinitionBean.getProperty().isEmpty()) {
            cartridgeConfig.setProperties(getCCProperties(cartridgeDefinitionBean.getProperty()));
        }

        if (cartridgeDefinitionBean.getExportingProperties() != null) {
            List<String> propertiesList = cartridgeDefinitionBean.getExportingProperties();
            String[] propertiesArray = propertiesList.toArray(new String[propertiesList.size()]);
            cartridgeConfig.setExportingProperties(propertiesArray);
        }

        if (cartridgeDefinitionBean.getContainer() != null) {
            cartridgeConfig.setContainer(getContainer(cartridgeDefinitionBean.getContainer()));
        }

        return cartridgeConfig;
    }

    public static ServiceGroup populateServiceGroupPojo(ServiceGroupDefinition serviceGroupDefinition) {
        ServiceGroup servicegroup = new ServiceGroup();

        return servicegroup;
    }

    private static Container getContainer(ContainerBean container) {
        Container cn = new Container();
        cn.setDockerFileRepo(container.getDockerfileRepo());
        cn.setImageName(container.getImageName());
        //cn.setProperties(getProperties(container.property));
        return cn;
    }

    private static LoadbalancerConfig getLBConfig(LoadBalancerBean loadBalancer) {
        LoadbalancerConfig lbConfig = new LoadbalancerConfig();
        lbConfig.setType(loadBalancer.getType());
        if (loadBalancer.getProperty() != null && !loadBalancer.getProperty().isEmpty()) {
            lbConfig.setProperties(getCCProperties(loadBalancer.getProperty()));
        }
        return lbConfig;
    }

    private static PortMapping[] getPortMappingsAsArray(List<PortMappingBean> portMappingBeans) {

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

    private static IaasConfig[] getIaasConfigsAsArray(List<IaasProviderBean> iaasProviderBeans) {

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
                iaasConfig.setProperties(getCCProperties(iaasProviderBeansArray[i].getProperty()));
            }

            if (iaasProviderBeansArray[i].getNetworkInterfaces() != null && !iaasProviderBeansArray[i].getNetworkInterfaces().isEmpty()) {
                iaasConfig.setNetworkInterfaces(ObjectConverter.getNetworkInterfaces(iaasProviderBeansArray[i].getNetworkInterfaces()));
            }

            iaasConfigsArray[i] = iaasConfig;
        }
        return iaasConfigsArray;
    }

    public static Persistence getPersistence(PersistenceBean persistenceBean) {
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

    public static Properties getProperties(List<PropertyBean> propertyBeans) {

        //convert to an array
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
    
    public static org.apache.stratos.cloud.controller.stub.Properties getCCProperties(List<PropertyBean> propertyBeans) {

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


    public static org.apache.stratos.autoscaler.stub.Properties getASProperties(List<PropertyBean> propertyBeans) {
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

    private static NetworkInterfaces getNetworkInterfaces(List<NetworkInterfaceBean> networkInterfaceBeans) {
        NetworkInterface[] networkInterfacesArray = new NetworkInterface[networkInterfaceBeans.size()];

        int i = 0;
        for (NetworkInterfaceBean nib : networkInterfaceBeans) {
            NetworkInterface networkInterface = new NetworkInterface();
            networkInterface.setNetworkUuid(nib.getNetworkUuid());
            networkInterface.setFixedIp(nib.getFixedIp());
            networkInterface.setPortUuid(nib.getPortUuid());
            if (nib.getFloatingNetworks() != null && !nib.getFloatingNetworks().isEmpty()) {
            	networkInterface.setFloatingNetworks(ObjectConverter.getFloatingNetworks(nib.getFloatingNetworks()));
            }

            networkInterfacesArray[i++] = networkInterface;
        }

        NetworkInterfaces networkInterfaces = new NetworkInterfaces();
        networkInterfaces.setNetworkInterfaces(networkInterfacesArray);
        return networkInterfaces;
    }
    
    private static FloatingNetworks getFloatingNetworks(List<FloatingNetworkBean> floatingNetworkBeans) {
    	
    	FloatingNetwork[] floatingNetworksArray = new FloatingNetwork[floatingNetworkBeans.size()];
    	
    	int i =0;
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
            partition.setProperties(getASProperties(partitionBean.getProperty()));
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
                loadAverage.setLowerLimit(autoscalePolicyBean.getLoadThresholds().getLoadAverage().getLowerLimit());
                //set load average
                loadThresholds.setLoadAverage(loadAverage);
            }
            if (autoscalePolicyBean.getLoadThresholds().getRequestsInFlight() != null) {

                org.apache.stratos.autoscaler.stub.autoscale.policy.RequestsInFlightThresholds requestsInFlight = new
                        org.apache.stratos.autoscaler.stub.autoscale.policy.RequestsInFlightThresholds();
                //set request in flight information
                requestsInFlight.setUpperLimit(autoscalePolicyBean.getLoadThresholds().getRequestsInFlight().getThreshold());
                requestsInFlight.setLowerLimit(autoscalePolicyBean.getLoadThresholds().getRequestsInFlight().getLowerLimit());
                //set request in flight
                loadThresholds.setRequestsInFlight(requestsInFlight);
            }
            if (autoscalePolicyBean.getLoadThresholds().getMemoryConsumption() != null) {

                org.apache.stratos.autoscaler.stub.autoscale.policy.MemoryConsumptionThresholds memoryConsumption = new
                        org.apache.stratos.autoscaler.stub.autoscale.policy.MemoryConsumptionThresholds();

                //set memory consumption information
                memoryConsumption.setUpperLimit(autoscalePolicyBean.getLoadThresholds().getMemoryConsumption().getThreshold());
                memoryConsumption.setLowerLimit(autoscalePolicyBean.getLoadThresholds().getMemoryConsumption().getLowerLimit());
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
            deploymentPolicy.setChildPolicies(convertToCCChildPolicy(deploymentPolicyBean.getChildPolicies()));
        }

        return deploymentPolicy;
    }

    public static DeploymentPolicy convertStubDeploymentPolicyToDeploymentPolicy(
            org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy stubDeploymentPolicy) {

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
        childPolicy.setChildId(stubChildDeploymentPolicy.getId());
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

    private static ChildPolicy[] convertToCCChildPolicy(List<org.apache.stratos.common.beans.autoscaler.policy.deployment.ChildPolicy> childPolicies) {

        ChildPolicy[] childPolicies1 = new ChildPolicy[childPolicies.size()];

        for (int i = 0; i < childPolicies.size(); i++) {
            ChildPolicy childPolicy = new ChildPolicy();
            childPolicy.setId(childPolicies.get(i).getChildId());
            childPolicy.setChildLevelNetworkPartitions(convertToCCChildNetworkPartition(childPolicies.get(i).getNetworkPartition()));


            childPolicies1[i] = childPolicy;
        }

        return childPolicies1;
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

    public static org.apache.stratos.common.beans.topology.Cluster populateClusterPojos(Cluster cluster, String alias) {
        org.apache.stratos.common.beans.topology.Cluster cluster1 = new
                org.apache.stratos.common.beans.topology.Cluster();
        cluster1.setAlias(alias);
        cluster1.setServiceName(cluster.getServiceName());
        cluster1.setClusterId(cluster.getClusterId());
        cluster1.setLbCluster(cluster.isLbCluster());
        cluster1.setTenantRange(cluster.getTenantRange());
        cluster1.setProperty(getPropertyBeans(cluster.getProperties()));
        cluster1.setMember(new ArrayList<Member>());
        cluster1.setHostNames(new ArrayList<String>());
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
			cluster1.setInstances(instancesList);
		}

        for (org.apache.stratos.messaging.domain.topology.Member tmp : cluster.getMembers()) {
            Member member = new Member();
            member.setClusterId(tmp.getClusterId());
            member.setLbClusterId(tmp.getLbClusterId());
            member.setNetworkPartitionId(tmp.getNetworkPartitionId());
            member.setPartitionId(tmp.getPartitionId());
            member.setMemberId(tmp.getMemberId());
            if (tmp.getMemberIp() == null) {
                member.setMemberIp("NULL");
            } else {
                member.setMemberIp(tmp.getMemberIp());
            }
            if (tmp.getMemberPublicIp() == null) {
                member.setMemberPublicIp("NULL");
            } else {
                member.setMemberPublicIp(tmp.getMemberPublicIp());
            }
            member.setServiceName(tmp.getServiceName());
            member.setStatus(tmp.getStatus().toString());
            member.setProperty(getPropertyBeans(tmp.getProperties()));
            cluster1.getMember().add(member);
        }

        for (String tmp1 : cluster.getHostNames()) {
            cluster1.getHostNames().add(tmp1);
        }

        return cluster1;
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
            List<PropertyBean> propertyBeans = getPropertyBeans(partition.getProperties());
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

    private static List<PropertyBean> getPropertyBeans(Properties properties) {

        List<PropertyBean> propertyBeans = null;
        if (properties.getProperties() != null && properties.getProperties().length != 0) {
            Property[] propertyArr = properties.getProperties();
            propertyBeans = new ArrayList<PropertyBean>();
            for (int i = 0; i < propertyArr.length; i++) {
                PropertyBean propertyBean = new PropertyBean();
                propertyBean.setName(propertyArr[i].getName());
                propertyBean.setValue(propertyArr[i].getValue());
                propertyBeans.add(propertyBean);
            }
        }
        return propertyBeans;
    }
    
    private static List<PropertyBean> getPropertyBeans(org.apache.stratos.cloud.controller.stub.Properties properties) {

        List<PropertyBean> propertyBeans = null;
        if (properties.getProperties() != null && properties.getProperties().length != 0) {
            org.apache.stratos.cloud.controller.stub.Property[] propertyArr = properties.getProperties();
            propertyBeans = new ArrayList<PropertyBean>();
            for (int i = 0; i < propertyArr.length; i++) {
                PropertyBean propertyBean = new PropertyBean();
                propertyBean.setName(propertyArr[i].getName());
                propertyBean.setValue(propertyArr[i].getValue());
                propertyBeans.add(propertyBean);
            }
        }
        return propertyBeans;
    }

    private static List<PropertyBean> getPropertyBeans(java.util.Properties properties) {

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

    public static AutoscalePolicy[] populateAutoscalePojos(org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy[]
                                                                   autoscalePolicies) {

        AutoscalePolicy[] autoscalePolicyBeans;
        if (autoscalePolicies == null) {
            autoscalePolicyBeans = new AutoscalePolicy[0];
            return autoscalePolicyBeans;
        }

        autoscalePolicyBeans = new AutoscalePolicy[autoscalePolicies.length];
        for (int i = 0; i < autoscalePolicies.length; i++) {
            autoscalePolicyBeans[i] = populateAutoscalePojo(autoscalePolicies[i]);
        }
        return autoscalePolicyBeans;
    }

    public static AutoscalePolicy populateAutoscalePojo(org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy
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
            autoscalePolicyBean.setLoadThresholds(populateLoadThresholds(autoscalePolicy.getLoadThresholds()));
        }

        return autoscalePolicyBean;
    }

    private static LoadThresholds populateLoadThresholds(org.apache.stratos.autoscaler.stub.autoscale.policy.LoadThresholds
                                                                 loadThresholds) {

        LoadThresholds loadThresholdBean = new LoadThresholds();
        if (loadThresholds.getLoadAverage() != null) {
            LoadAverageThresholds loadAverage = new LoadAverageThresholds();
            loadAverage.setThreshold(loadThresholds.getLoadAverage().getUpperLimit());
            loadAverage.setLowerLimit(loadThresholds.getLoadAverage().getLowerLimit());
            loadThresholdBean.setLoadAverage(loadAverage);
        }
        if (loadThresholds.getMemoryConsumption() != null) {
            MemoryConsumptionThresholds memoryConsumption = new MemoryConsumptionThresholds();
            memoryConsumption.setThreshold(loadThresholds.getMemoryConsumption().getUpperLimit());
            memoryConsumption.setLowerLimit(loadThresholds.getMemoryConsumption().getLowerLimit());
            loadThresholdBean.setMemoryConsumption(memoryConsumption);
        }
        if (loadThresholds.getRequestsInFlight() != null) {
            RequestsInFlightThresholds requestsInFlight = new RequestsInFlightThresholds();
            requestsInFlight.setThreshold(loadThresholds.getRequestsInFlight().getUpperLimit());
            requestsInFlight.setLowerLimit(loadThresholds.getRequestsInFlight().getLowerLimit());
            loadThresholdBean.setRequestsInFlight(requestsInFlight);
        }

        return loadThresholdBean;
    }

    public static DeploymentPolicy[] populateDeploymentPolicyPojos(org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy[]
                                                                           deploymentPolicies) {
        DeploymentPolicy[] deploymentPolicyBeans;
        if (deploymentPolicies == null) {
            return null;
        }

        deploymentPolicyBeans = new DeploymentPolicy[deploymentPolicies.length];
        for (int i = 0; i < deploymentPolicies.length; i++) {
            deploymentPolicyBeans[i] = populateDeploymentPolicyPojo(deploymentPolicies[i]);
        }

        return deploymentPolicyBeans;
    }

    public static DeploymentPolicy populateDeploymentPolicyPojo(org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy
                                                                        deploymentPolicy) {

        if (deploymentPolicy == null) {
            return null;
        }

        DeploymentPolicy deploymentPolicyBean = new DeploymentPolicy();
        deploymentPolicyBean.setDescription(deploymentPolicy.getDescription());
        deploymentPolicyBean.setPublic(deploymentPolicy.getIsPublic());
        deploymentPolicyBean.setApplicationId(deploymentPolicy.getApplicationId());
        return deploymentPolicyBean;
    }

    public static ApplicationLevelNetworkPartition populatePartitionGroupPojo(org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition
                                                                                      partitionGroup) {

        ApplicationLevelNetworkPartition networkPartitionBean = new ApplicationLevelNetworkPartition();
        if (partitionGroup == null) {
            return networkPartitionBean;
        }

        networkPartitionBean.setId(partitionGroup.getId());
        //FIXME update with new deployment policy pattern
//        networkPartitionBean.partitionAlgo = partitionGroup.getPartitionAlgo();
//        if (partitionGroup.getPartitions() != null && partitionGroup.getPartitions().length > 0) {
//            partitionGroupBean.partition = getPartitionList(partitionGroup.getPartitions());
//        }

        return networkPartitionBean;
    }

    public static ApplicationLevelNetworkPartition[] populatePartitionGroupPojos(org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition[] partitionGroups) {

        ApplicationLevelNetworkPartition[] networkPartitionGroupsBeans;
        if (partitionGroups == null) {
            networkPartitionGroupsBeans = new ApplicationLevelNetworkPartition[0];
            return networkPartitionGroupsBeans;
        }

        networkPartitionGroupsBeans = new ApplicationLevelNetworkPartition[partitionGroups.length];

        for (int i = 0; i < partitionGroups.length; i++) {
            networkPartitionGroupsBeans[i] = populatePartitionGroupPojo(partitionGroups[i]);
        }

        return networkPartitionGroupsBeans;
    }

    private static List<Partition> getPartitionList(org.apache.stratos.cloud.controller.stub.domain.Partition[]
                                                            partitions) {

        List<Partition> partitionList = new ArrayList<Partition>();
        for (int i = 0; i < partitions.length; i++) {
            Partition partition = new Partition();
            partition.setId(partitions[i].getId());
            partition.setProvider(partitions[i].getProvider());
            /*partition.partitionMin = partitions[i].getPartitionMin();
            partition.partitionMax = partitions[i].getPartitionMax();*/
            if (partitions[i].getProperties() != null) {
                partition.setProperty(getPropertyBeans(partitions[i].getProperties()));
            }
            partitionList.add(partition);
        }

        return partitionList;
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
        kubernetesGroup.setKubernetesMaster(convertToCCKubernetesMasterPojo(kubernetesGroupBean.getKubernetesMaster()));
        kubernetesGroup.setPortRange(convertToASPortRange(kubernetesGroupBean.getPortRange()));
        kubernetesGroup.setKubernetesHosts(convertToASKubernetesHostsPojo(kubernetesGroupBean.getKubernetesHosts()));
        kubernetesGroup.setProperties((getCCProperties(kubernetesGroupBean.getProperty())));

        return kubernetesGroup;
    }

    private static org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost[] convertToASKubernetesHostsPojo(List<KubernetesHost> kubernetesHosts) {
        if (kubernetesHosts == null || kubernetesHosts.isEmpty()) {
            return null;
        }
        int kubernetesHostCount = kubernetesHosts.size();
        org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost[]
                kubernetesHostsArr = new org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost[kubernetesHostCount];
        for (int i = 0; i < kubernetesHostCount; i++) {
            KubernetesHost kubernetesHostBean = kubernetesHosts.get(i);
            kubernetesHostsArr[i] = convertToCCKubernetesHostPojo(kubernetesHostBean);
        }
        return kubernetesHostsArr;
    }


    private static org.apache.stratos.cloud.controller.stub.kubernetes.PortRange convertToASPortRange(PortRange portRangeBean) {
        if (portRangeBean == null) {
            return null;
        }
        org.apache.stratos.cloud.controller.stub.kubernetes.PortRange
                portRange = new org.apache.stratos.cloud.controller.stub.kubernetes.PortRange();
        portRange.setLower(portRangeBean.getLower());
        portRange.setUpper(portRangeBean.getUpper());
        return portRange;
    }

    public static org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost convertToCCKubernetesHostPojo(KubernetesHost kubernetesHostBean) {
        if (kubernetesHostBean == null) {
            return null;
        }

        org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost
                kubernetesHost = new org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost();
        kubernetesHost.setHostId(kubernetesHostBean.getHostId());
        kubernetesHost.setHostIpAddress(kubernetesHostBean.getHostIpAddress());
        kubernetesHost.setHostname(kubernetesHostBean.getHostname());
        kubernetesHost.setProperties(getCCProperties(kubernetesHostBean.getProperty()));

        return kubernetesHost;
    }

    public static org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesMaster convertToCCKubernetesMasterPojo(KubernetesMaster kubernetesMasterBean) {
        if (kubernetesMasterBean == null) {
            return null;
        }

        org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesMaster
                kubernetesMaster = new org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesMaster();
        kubernetesMaster.setHostId(kubernetesMasterBean.getHostId());
        kubernetesMaster.setHostIpAddress(kubernetesMasterBean.getHostIpAddress());
        kubernetesMaster.setHostname(kubernetesMasterBean.getHostname());
        kubernetesMaster.setEndpoint(kubernetesMasterBean.getEndpoint());
        kubernetesMaster.setProperties(getCCProperties(kubernetesMasterBean.getProperty()));

        return kubernetesMaster;
    }

    public static KubernetesGroup[] populateKubernetesGroupsPojo(org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesGroup[] kubernetesGroups) {

        if (kubernetesGroups == null) {
            return null;
        }
        KubernetesGroup[] kubernetesGroupsBean = new KubernetesGroup[kubernetesGroups.length];
        for (int i = 0; i < kubernetesGroups.length; i++) {
            kubernetesGroupsBean[i] = populateKubernetesGroupPojo(kubernetesGroups[i]);
        }
        return kubernetesGroupsBean;
    }

    public static KubernetesGroup populateKubernetesGroupPojo(org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesGroup kubernetesGroup) {
        if (kubernetesGroup == null) {
            return null;
        }
        KubernetesGroup kubernetesGroupBean = new KubernetesGroup();
        kubernetesGroupBean.setGroupId(kubernetesGroup.getGroupId());
        kubernetesGroupBean.setDescription(kubernetesGroup.getDescription());
        kubernetesGroupBean.setPortRange(populatePortRangePojo(kubernetesGroup.getPortRange()));
        kubernetesGroupBean.setKubernetesHosts(populateKubernetesHostsPojo(kubernetesGroup.getKubernetesHosts()));
        kubernetesGroupBean.setKubernetesMaster(populateKubernetesMasterPojo(kubernetesGroup.getKubernetesMaster()));
        kubernetesGroupBean.setProperty(populateCCProperties(kubernetesGroup.getProperties()));
        return kubernetesGroupBean;
    }

    public static KubernetesMaster populateKubernetesMasterPojo(org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesMaster kubernetesMaster) {
        if (kubernetesMaster == null) {
            return null;
        }
        KubernetesMaster kubernetesMasterBean = new KubernetesMaster();
        kubernetesMasterBean.setHostId(kubernetesMaster.getHostId());
        kubernetesMasterBean.setHostname(kubernetesMaster.getHostname());
        kubernetesMasterBean.setHostIpAddress(kubernetesMaster.getHostIpAddress());
        kubernetesMasterBean.setProperty(populateCCProperties(kubernetesMaster.getProperties()));
        kubernetesMasterBean.setEndpoint(kubernetesMaster.getEndpoint());
        return kubernetesMasterBean;
    }

    public static List<KubernetesHost> populateKubernetesHostsPojo(org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost[] kubernetesHosts) {
        if (kubernetesHosts == null) {
            return null;
        }
        List<KubernetesHost> kubernetesHostList = new ArrayList<KubernetesHost>();
        for (int i = 0; i < kubernetesHosts.length; i++) {
            kubernetesHostList.add(populateKubernetesHostPojo(kubernetesHosts[i]));
        }
        return kubernetesHostList;
    }

    private static KubernetesHost populateKubernetesHostPojo(org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost kubernetesHost) {
        if (kubernetesHost == null) {
            return null;
        }
        KubernetesHost kubernetesHostBean = new KubernetesHost();
        kubernetesHostBean.setHostId(kubernetesHost.getHostId());
        kubernetesHostBean.setHostname(kubernetesHost.getHostname());
        kubernetesHostBean.setHostIpAddress(kubernetesHost.getHostIpAddress());
        kubernetesHostBean.setProperty(populateCCProperties(kubernetesHost.getProperties()));
        return kubernetesHostBean;
    }

    private static List<PropertyBean> populateASProperties(org.apache.stratos.autoscaler.stub.Properties properties) {
        if (properties == null || properties.getProperties() == null) {
            return null;
        }
        List<PropertyBean> propertyBeanList = new ArrayList<PropertyBean>();
        for (int i = 0; i < properties.getProperties().length; i++) {
            propertyBeanList.add(populateASProperty(properties.getProperties()[i]));
        }
        return propertyBeanList;
    }
    
    private static List<PropertyBean> populateCCProperties(org.apache.stratos.cloud.controller.stub.Properties properties) {
        if (properties == null || properties.getProperties() == null) {
            return null;
        }
        List<PropertyBean> propertyBeanList = new ArrayList<PropertyBean>();
        for (int i = 0; i < properties.getProperties().length; i++) {
            propertyBeanList.add(populateCCProperty(properties.getProperties()[i]));
        }
        return propertyBeanList;
    }

    private static PropertyBean populateASProperty(org.apache.stratos.autoscaler.stub.Property propertyE) {
        if (propertyE == null) {
            return null;
        }
        PropertyBean propertyBean = new PropertyBean();
        propertyBean.setName(propertyE.getName());
        propertyBean.setValue(propertyE.getValue());
        return propertyBean;
    }
    
    private static PropertyBean populateCCProperty(org.apache.stratos.cloud.controller.stub.Property propertyE) {
        if (propertyE == null) {
            return null;
        }
        PropertyBean propertyBean = new PropertyBean();
        propertyBean.setName(propertyE.getName());
        propertyBean.setValue(propertyE.getValue());
        return propertyBean;
    }

    private static PortRange populatePortRangePojo(org.apache.stratos.cloud.controller.stub.kubernetes.PortRange portRange) {
        if (portRange == null) {
            return null;
        }
        PortRange portRangeBean = new PortRange();
        portRangeBean.setUpper(portRange.getUpper());
        portRangeBean.setLower(portRange.getLower());
        return portRangeBean;
    }

    public static ApplicationContext convertApplicationDefinitionToApplicationContext(ApplicationDefinition applicationDefinition) {

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
                        convertCartridgeDefinitionsToCartridgeContexts(applicationDefinition.getComponents().getCartridges()));
            }
            applicationContext.setComponents(componentContext);
        }
        return applicationContext;
    }

    public static ApplicationDefinition convertApplicationContextToApplicationDefinition(
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
                        convertGroupContextsToGroupDefinitions(applicationContext.getComponents().getGroupContexts()));
            }
            // top level dependency information
            if (applicationContext.getComponents().getDependencyContext() != null) {
                applicationDefinition.getComponents().setDependencies(
                        convertDependencyContextsToDependencyDefinitions(applicationContext.getComponents().getDependencyContext()));
            }
            // top level cartridge context information
            if (applicationContext.getComponents().getCartridgeContexts() != null) {
                applicationDefinition.getComponents().setCartridges(
                        convertCartridgeContextsToCartridgeDefinitions(applicationContext.getComponents().getCartridgeContexts()));
            }
        }
        return applicationDefinition;
    }

    private static List<GroupDefinition> convertGroupContextsToGroupDefinitions(GroupContext[] groupContexts) {
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
                    groupDefinition.setGroups(convertGroupContextsToGroupDefinitions(groupContext.getGroupContexts()));
                    groupDefinition.setCartridges(convertCartridgeContextsToCartridgeDefinitions(
                            groupContext.getCartridgeContexts()));
                    groupDefinitions.add(groupDefinition);
                }
            }
        }
        return groupDefinitions;
    }

    private static DependencyDefinitions convertDependencyContextsToDependencyDefinitions(DependencyContext dependencyContext) {
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

    private static List<CartridgeDefinition> convertCartridgeContextsToCartridgeDefinitions(CartridgeContext[] cartridgeContexts) {
        List<CartridgeDefinition> cartridgeDefinitions = new ArrayList<CartridgeDefinition>();
        if(cartridgeContexts != null) {
            for (CartridgeContext cartridgeContext : cartridgeContexts) {
                if(cartridgeContext != null) {
                    CartridgeDefinition cartridgeDefinition = new CartridgeDefinition();
                    cartridgeDefinition.setType(cartridgeContext.getType());
                    cartridgeDefinition.setCartridgeMin(cartridgeContext.getCartridgeMin());
                    cartridgeDefinition.setCartridgeMax(cartridgeContext.getCartridgeMax());
                    cartridgeDefinition.setSubscribableInfo(convertSubscribableInfoContextToSubscribableInfo(cartridgeContext.getSubscribableInfoContext()));
                    cartridgeDefinitions.add(cartridgeDefinition);
                }
            }
        }
        return cartridgeDefinitions;
    }

    private static SubscribableInfo convertSubscribableInfoContextToSubscribableInfo(
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

    private static CartridgeContext[] convertCartridgeDefinitionsToCartridgeContexts(
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
            
            groupContext.setCartridgeContexts(convertCartridgeDefinitionsToCartridgeContexts(groupDefinition.getCartridges()));
            groupContexts[i++] = groupContext;
        }

        return groupContexts;
    }


    public static ApplicationBean applicationToBean(Application application) {
        if (application == null) {
            return null;
        }

        ApplicationBean applicationBean = new ApplicationBean();
        applicationBean.setId(application.getUniqueIdentifier());
        applicationBean.setName(application.getName());
        applicationBean.setDescription(application.getDescription());
        applicationBean.setTenantDomain(application.getTenantDomain());
        applicationBean.setTenantAdminUsername(application.getTenantAdminUserName());
        applicationBean.setInstances(setApplicationInstances(application));
        return applicationBean;
    }

    private static List<Instance> setApplicationInstances(
            Application application) {
    	List<Instance> applicationInstanceList = new ArrayList<Instance>();
    	Collection<ApplicationInstance> applicationInstancesInTopology = 
    			application.getInstanceIdToInstanceContextMap().values();
    	
    	if(applicationInstancesInTopology != null) {
    		for (ApplicationInstance applicationInstance : applicationInstancesInTopology) {
    			Instance instance = new Instance();
    			instance.setInstanceId(applicationInstance.getInstanceId());
    			instance.setStatus(applicationInstance.getStatus().toString());
    			applicationInstanceList.add(instance);
            }
    	}
    	
	    return applicationInstanceList;
    }

	public static GroupBean toGroupBean(Group group) {
        if (group == null) {
            return null;
        }

        GroupBean groupBean = new GroupBean();
        groupBean.setInstances(setGroupInstances(group));
        groupBean.setAlias(group.getUniqueIdentifier());
        groupBean.setAutoScalingPolicy(group.getAutoscalingPolicy());
        return groupBean;
    }

	private static List<Instance> setGroupInstances(Group group) {
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
}

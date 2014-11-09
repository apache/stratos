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

package org.apache.stratos.rest.endpoint.bean.util.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.stratos.autoscaler.applications.pojo.xsd.ApplicationContext;
import org.apache.stratos.autoscaler.applications.pojo.xsd.DependencyContext;
import org.apache.stratos.autoscaler.applications.pojo.xsd.GroupContext;
import org.apache.stratos.autoscaler.applications.pojo.xsd.SubscribableInfoContext;
import org.apache.stratos.autoscaler.stub.pojo.PropertiesE;
import org.apache.stratos.autoscaler.stub.pojo.PropertyE;
import org.apache.stratos.cloud.controller.stub.pojo.CartridgeConfig;
import org.apache.stratos.cloud.controller.stub.pojo.Container;
import org.apache.stratos.cloud.controller.stub.pojo.IaasConfig;
import org.apache.stratos.cloud.controller.stub.pojo.LoadbalancerConfig;
import org.apache.stratos.cloud.controller.stub.pojo.NetworkInterface;
import org.apache.stratos.cloud.controller.stub.pojo.NetworkInterfaces;
import org.apache.stratos.cloud.controller.stub.pojo.Persistence;
import org.apache.stratos.cloud.controller.stub.pojo.PortMapping;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.cloud.controller.stub.pojo.Property;
import org.apache.stratos.cloud.controller.stub.pojo.ServiceGroup;
import org.apache.stratos.cloud.controller.stub.pojo.Volume;
import org.apache.stratos.manager.composite.application.beans.ApplicationDefinition;
import org.apache.stratos.manager.composite.application.beans.GroupDefinition;
import org.apache.stratos.manager.composite.application.beans.SubscribableDefinition;
import org.apache.stratos.manager.composite.application.beans.SubscribableInfo;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.grouping.definitions.DependencyDefinitions;
import org.apache.stratos.manager.grouping.definitions.ServiceGroupDefinition;
import org.apache.stratos.manager.subscription.SubscriptionDomain;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.Group;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.rest.endpoint.bean.ApplicationBean;
import org.apache.stratos.rest.endpoint.bean.GroupBean;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.PartitionGroup;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.LoadAverageThresholds;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.LoadThresholds;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.MemoryConsumptionThresholds;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.RequestsInFlightThresholds;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.ContainerBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.IaasProviderBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.LoadBalancerBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.NetworkInterfaceBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.PersistenceBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.PortMappingBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.PropertyBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.ServiceDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.VolumeBean;
import org.apache.stratos.rest.endpoint.bean.kubernetes.KubernetesGroup;
import org.apache.stratos.rest.endpoint.bean.kubernetes.KubernetesHost;
import org.apache.stratos.rest.endpoint.bean.kubernetes.KubernetesMaster;
import org.apache.stratos.rest.endpoint.bean.kubernetes.PortRange;
import org.apache.stratos.rest.endpoint.bean.subscription.domain.SubscriptionDomainBean;
import org.apache.stratos.rest.endpoint.bean.topology.Member;

public class PojoConverter {

    public static CartridgeConfig populateCartridgeConfigPojo(CartridgeDefinitionBean cartridgeDefinitionBean) {

        CartridgeConfig cartridgeConfig = new CartridgeConfig();

        cartridgeConfig.setType(cartridgeDefinitionBean.type);
        cartridgeConfig.setHostName(cartridgeDefinitionBean.host);
        cartridgeConfig.setProvider(cartridgeDefinitionBean.provider);
        cartridgeConfig.setVersion(cartridgeDefinitionBean.version);
        cartridgeConfig.setMultiTenant(cartridgeDefinitionBean.multiTenant);
        cartridgeConfig.setIsPublic(cartridgeDefinitionBean.isPublic);
        cartridgeConfig.setDisplayName(cartridgeDefinitionBean.displayName);
        cartridgeConfig.setDescription(cartridgeDefinitionBean.description);
        cartridgeConfig.setDefaultAutoscalingPolicy(cartridgeDefinitionBean.defaultAutoscalingPolicy);
        cartridgeConfig.setDefaultDeploymentPolicy(cartridgeDefinitionBean.defaultDeploymentPolicy);
        cartridgeConfig.setServiceGroup(cartridgeDefinitionBean.serviceGroup);
        cartridgeConfig.setDeployerType(cartridgeDefinitionBean.deployerType);

        
        //deployment information
        if (cartridgeDefinitionBean.deployment != null) {
            cartridgeConfig.setBaseDir(cartridgeDefinitionBean.deployment.baseDir);
            if (cartridgeDefinitionBean.deployment.dir != null && !cartridgeDefinitionBean.deployment.dir.isEmpty()) {
                cartridgeConfig.setDeploymentDirs(cartridgeDefinitionBean.deployment.dir.
                        toArray(new String[cartridgeDefinitionBean.deployment.dir.size()]));
            }
        }
        //port mapping
        if (cartridgeDefinitionBean.portMapping != null && !cartridgeDefinitionBean.portMapping.isEmpty()) {
            cartridgeConfig.setPortMappings(getPortMappingsAsArray(cartridgeDefinitionBean.portMapping));
        }

        //persistance mapping
        if (cartridgeDefinitionBean.persistence != null) {
            cartridgeConfig.setPersistence(getPersistence(cartridgeDefinitionBean.persistence));
        }

        //IaaS
        if (cartridgeDefinitionBean.iaasProvider != null && !cartridgeDefinitionBean.iaasProvider.isEmpty()) {
            cartridgeConfig.setIaasConfigs(getIaasConfigsAsArray(cartridgeDefinitionBean.iaasProvider));
        }
        //LB
        if (cartridgeDefinitionBean.loadBalancer != null) {
            cartridgeConfig.setLbConfig(getLBConfig(cartridgeDefinitionBean.loadBalancer));
        }
        //Properties
        if (cartridgeDefinitionBean.property != null && !cartridgeDefinitionBean.property.isEmpty()) {
            cartridgeConfig.setProperties(getProperties(cartridgeDefinitionBean.property));
        }

        if(cartridgeDefinitionBean.getExportingProperties() != null)
        {
            cartridgeConfig.setExportingProperties(cartridgeDefinitionBean.getExportingProperties());
        }
        
        if (cartridgeDefinitionBean.container != null) {
            cartridgeConfig.setContainer(getContainer(cartridgeDefinitionBean.container));
        }

        return cartridgeConfig;
    }
    
    public static ServiceGroup populateServiceGroupPojo (ServiceGroupDefinition serviceGroupDefinition ) {
    	ServiceGroup servicegroup = new ServiceGroup();
    	
    	// implement conversion (mostly List -> Array)
    	
    	return servicegroup;
    }


    private static Container getContainer(ContainerBean container) {
        Container cn = new Container();
        cn.setDockerFileRepo(container.dockerfileRepo);
        cn.setImageName(container.imageName);
        //cn.setProperties(getProperties(container.property));
        return cn;
    }

    private static LoadbalancerConfig getLBConfig(LoadBalancerBean loadBalancer) {
        LoadbalancerConfig lbConfig = new LoadbalancerConfig();
        lbConfig.setType(loadBalancer.type);
        if (loadBalancer.property != null && !loadBalancer.property.isEmpty()) {
            lbConfig.setProperties(getProperties(loadBalancer.property));
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
            portMapping.setProtocol(portMappingBeanArray[i].protocol);
            portMapping.setPort(Integer.toString(portMappingBeanArray[i].port));
            portMapping.setProxyPort(Integer.toString(portMappingBeanArray[i].proxyPort));
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
            iaasConfig.setType(iaasProviderBeansArray[i].type);
            iaasConfig.setImageId(iaasProviderBeansArray[i].imageId);
            iaasConfig.setMaxInstanceLimit(iaasProviderBeansArray[i].maxInstanceLimit);
            iaasConfig.setName(iaasProviderBeansArray[i].name);
            iaasConfig.setClassName(iaasProviderBeansArray[i].className);
            iaasConfig.setCredential(iaasProviderBeansArray[i].credential);
            iaasConfig.setIdentity(iaasProviderBeansArray[i].identity);
            iaasConfig.setProvider(iaasProviderBeansArray[i].provider);

            if (iaasProviderBeansArray[i].property != null && !iaasProviderBeansArray[i].property.isEmpty()) {
                //set the Properties instance to IaasConfig instance
                iaasConfig.setProperties(getProperties(iaasProviderBeansArray[i].property));
            }

            if (iaasProviderBeansArray[i].networkInterfaces != null && !iaasProviderBeansArray[i].networkInterfaces.isEmpty()) {
                iaasConfig.setNetworkInterfaces(PojoConverter.getNetworkInterfaces(iaasProviderBeansArray[i].networkInterfaces));
            }

            iaasConfigsArray[i] = iaasConfig;
        }
        return iaasConfigsArray;
    }

    public static Persistence getPersistence(PersistenceBean persistenceBean) {
        Persistence persistence = new Persistence();
        persistence.setPersistanceRequired(persistenceBean.isRequired);
        VolumeBean[] volumeBean = new VolumeBean[persistenceBean.volume.size()];
        persistenceBean.volume.toArray(volumeBean);
        Volume[] volumes = new Volume[persistenceBean.volume.size()];
        for (int i = 0; i < volumes.length; i++) {
            Volume volume = new Volume();
            volume.setId(volumeBean[i].id);
            volume.setVolumeId(volumeBean[i].volumeId);
            if (StringUtils.isEmpty(volume.getVolumeId())) {
                volume.setSize(Integer.parseInt(volumeBean[i].size));
            }

            volume.setDevice(volumeBean[i].device);
            volume.setRemoveOntermination(volumeBean[i].removeOnTermination);
            volume.setMappingPath(volumeBean[i].mappingPath);
            volume.setSnapshotId(volumeBean[i].snapshotId);

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
            property.setName(propertyBeansArray[j].name);
            property.setValue(propertyBeansArray[j].value);
            propertyArray[j] = property;
        }

        Properties properties = new Properties();
        properties.setProperties(propertyArray);
        return properties;
    }


    public static PropertiesE getASProperties(List<PropertyBean> propertyBeans) {
        if (propertyBeans == null || propertyBeans.isEmpty()) {
            return null;
        }

        //convert to an array
        PropertyBean[] propertyBeansArray = new PropertyBean[propertyBeans.size()];
        propertyBeans.toArray(propertyBeansArray);
        PropertyE[] propertyArray = new PropertyE[propertyBeansArray.length];

        for (int j = 0; j < propertyBeansArray.length; j++) {
            PropertyE property = new PropertyE();
            property.setName(propertyBeansArray[j].name);
            property.setValue(propertyBeansArray[j].value);
            propertyArray[j] = property;
        }

        PropertiesE properties = new PropertiesE();
        properties.setProperties(propertyArray);
        return properties;
    }

    private static NetworkInterfaces getNetworkInterfaces(List<NetworkInterfaceBean> networkInterfaceBeans) {
        NetworkInterface[] networkInterfacesArray = new NetworkInterface[networkInterfaceBeans.size()];

        int i = 0;
        for (NetworkInterfaceBean nib : networkInterfaceBeans) {
            NetworkInterface networkInterface = new NetworkInterface();
            networkInterface.setNetworkUuid(nib.networkUuid);
            networkInterface.setFixedIp(nib.fixedIp);
            networkInterface.setPortUuid(nib.portUuid);
            networkInterfacesArray[i++] = networkInterface;
        }

        NetworkInterfaces networkInterfaces = new NetworkInterfaces();
        networkInterfaces.setNetworkInterfaces(networkInterfacesArray);
        return networkInterfaces;
    }

    public static org.apache.stratos.cloud.controller.stub.deployment.partition.Partition convertToCCPartitionPojo
            (Partition partitionBean) {

        org.apache.stratos.cloud.controller.stub.deployment.partition.Partition partition = new
                org.apache.stratos.cloud.controller.stub.deployment.partition.Partition();

        partition.setId(partitionBean.id);
        partition.setDescription(partitionBean.description);
        partition.setIsPublic(partitionBean.isPublic);
        partition.setProvider(partitionBean.provider);
        partition.setPartitionMin(partitionBean.partitionMin);
        partition.setPartitionMax(partitionBean.partitionMax);

        if (partitionBean.property != null && !partitionBean.property.isEmpty()) {
            partition.setProperties(getProperties(partitionBean.property));
        }

        return partition;
    }

    public static org.apache.stratos.autoscaler.stub.policy.model.AutoscalePolicy convertToCCAutoscalerPojo(AutoscalePolicy
                                                                                                                    autoscalePolicyBean) {

        org.apache.stratos.autoscaler.stub.policy.model.AutoscalePolicy autoscalePolicy = new
                org.apache.stratos.autoscaler.stub.policy.model.AutoscalePolicy();

        autoscalePolicy.setId(autoscalePolicyBean.getId());
        autoscalePolicy.setDescription(autoscalePolicyBean.getDescription());
        autoscalePolicy.setIsPublic(autoscalePolicyBean.getIsPublic());
        autoscalePolicy.setDisplayName(autoscalePolicyBean.getDisplayName());

        if (autoscalePolicyBean.getLoadThresholds() != null) {

            org.apache.stratos.autoscaler.stub.policy.model.LoadThresholds loadThresholds = new
                    org.apache.stratos.autoscaler.stub.policy.model.LoadThresholds();

            if (autoscalePolicyBean.getLoadThresholds().loadAverage != null) {

                //set load average information
                org.apache.stratos.autoscaler.stub.policy.model.LoadAverageThresholds loadAverage = new
                        org.apache.stratos.autoscaler.stub.policy.model.LoadAverageThresholds();
                loadAverage.setUpperLimit(autoscalePolicyBean.getLoadThresholds().loadAverage.upperLimit);
                loadAverage.setLowerLimit(autoscalePolicyBean.getLoadThresholds().loadAverage.lowerLimit);
                //set load average
                loadThresholds.setLoadAverage(loadAverage);
            }
            if (autoscalePolicyBean.getLoadThresholds().requestsInFlight != null) {

                org.apache.stratos.autoscaler.stub.policy.model.RequestsInFlightThresholds requestsInFlight = new
                        org.apache.stratos.autoscaler.stub.policy.model.RequestsInFlightThresholds();
                //set request in flight information
                requestsInFlight.setUpperLimit(autoscalePolicyBean.getLoadThresholds().requestsInFlight.upperLimit);
                requestsInFlight.setLowerLimit(autoscalePolicyBean.getLoadThresholds().requestsInFlight.lowerLimit);
                //set request in flight
                loadThresholds.setRequestsInFlight(requestsInFlight);
            }
            if (autoscalePolicyBean.getLoadThresholds().memoryConsumption != null) {

                org.apache.stratos.autoscaler.stub.policy.model.MemoryConsumptionThresholds memoryConsumption = new
                        org.apache.stratos.autoscaler.stub.policy.model.MemoryConsumptionThresholds();

                //set memory consumption information
                memoryConsumption.setUpperLimit(autoscalePolicyBean.getLoadThresholds().memoryConsumption.upperLimit);
                memoryConsumption.setLowerLimit(autoscalePolicyBean.getLoadThresholds().memoryConsumption.lowerLimit);
                //set memory consumption
                loadThresholds.setMemoryConsumption(memoryConsumption);
            }

            autoscalePolicy.setLoadThresholds(loadThresholds);
        }

        return autoscalePolicy;
    }

    public static org.apache.stratos.autoscaler.stub.policy.model.DeploymentPolicy convetToCCDeploymentPolicyPojo(DeploymentPolicy
                                                                                                                               deploymentPolicyBean) {

        org.apache.stratos.autoscaler.stub.policy.model.DeploymentPolicy deploymentPolicy = new
                org.apache.stratos.autoscaler.stub.policy.model.DeploymentPolicy();

        deploymentPolicy.setId(deploymentPolicyBean.getId());
        deploymentPolicy.setDescription(deploymentPolicyBean.getDescription());
        deploymentPolicy.setIsPublic(deploymentPolicyBean.isPublic());
        if(deploymentPolicyBean.getPartitionGroup() != null && !deploymentPolicyBean.getPartitionGroup().isEmpty()) {
            deploymentPolicy.setPartitionGroups(convertToCCPartitionGroup(deploymentPolicyBean.getPartitionGroup()));
        }

        return deploymentPolicy;
    }

    private static org.apache.stratos.autoscaler.stub.partition.PartitionGroup[] convertToCCPartitionGroup(List<PartitionGroup> partitionGroupBeans) {

        org.apache.stratos.autoscaler.stub.partition.PartitionGroup[] partitionGroups = new
                org.apache.stratos.autoscaler.stub.partition.PartitionGroup[partitionGroupBeans.size()];

        for (int i = 0; i < partitionGroupBeans.size(); i++) {
            org.apache.stratos.autoscaler.stub.partition.PartitionGroup partitionGroup = new
                    org.apache.stratos.autoscaler.stub.partition.PartitionGroup();
            partitionGroup.setId(partitionGroupBeans.get(i).id);
            partitionGroup.setPartitionAlgo(partitionGroupBeans.get(i).partitionAlgo);

            if (partitionGroupBeans.get(i).partition != null && !partitionGroupBeans.get(i).partition.isEmpty()) {
                partitionGroup.setPartitions(convertToCCPartitionPojos(partitionGroupBeans.get(i).partition));
            }

            partitionGroups[i] = partitionGroup;
        }

        return partitionGroups;
    }

    public static org.apache.stratos.rest.endpoint.bean.topology.Cluster populateClusterPojos(Cluster cluster) {
        org.apache.stratos.rest.endpoint.bean.topology.Cluster cluster1 = new
                org.apache.stratos.rest.endpoint.bean.topology.Cluster();
        cluster1.serviceName = cluster.getServiceName();
        cluster1.clusterId = cluster.getClusterId();
        cluster1.isLbCluster = cluster.isLbCluster();
        cluster1.tenantRange = cluster.getTenantRange();
        cluster1.property = getPropertyBeans(cluster.getProperties());
        cluster1.member = new ArrayList<Member>();
        cluster1.hostNames = new ArrayList<String>();

        for (org.apache.stratos.messaging.domain.topology.Member tmp : cluster.getMembers()) {
            Member member = new Member();
            member.clusterId = tmp.getClusterId();
            member.lbClusterId = tmp.getLbClusterId();
            member.networkPartitionId = tmp.getNetworkPartitionId();
            member.partitionId = tmp.getPartitionId();
            member.memberId = tmp.getMemberId();
            if (tmp.getMemberIp() == null) {
                member.memberIp = "NULL";
            } else {
                member.memberIp = tmp.getMemberIp();
            }
            if (tmp.getMemberPublicIp() == null) {
                member.memberPublicIp = "NULL";
            } else {
                member.memberPublicIp = tmp.getMemberPublicIp();
            }
            member.serviceName = tmp.getServiceName();
            member.status = tmp.getStatus().toString();
            member.property = getPropertyBeans(tmp.getProperties());
            cluster1.member.add(member);
        }

        for (String tmp1 : cluster.getHostNames()) {
            cluster1.hostNames.add(tmp1);
        }

        return cluster1;
    }

    private static org.apache.stratos.cloud.controller.stub.deployment.partition.Partition[] convertToCCPartitionPojos
            (List<Partition> partitionList) {

        org.apache.stratos.cloud.controller.stub.deployment.partition.Partition[] partitions =
                new org.apache.stratos.cloud.controller.stub.deployment.partition.Partition[partitionList.size()];
        for (int i = 0; i < partitionList.size(); i++) {
            partitions[i] = convertToCCPartitionPojo(partitionList.get(i));
        }

        return partitions;
    }

    public static Partition[] populatePartitionPojos(org.apache.stratos.cloud.controller.stub.deployment.partition.Partition[]
                                                             partitions) {

        Partition[] partitionBeans;
        if (partitions == null) {
            partitionBeans = new Partition[0];
            return partitionBeans;
        }

        partitionBeans = new Partition[partitions.length];
        for (int i = 0 ; i < partitions.length ; i++) {
            partitionBeans[i] = populatePartitionPojo(partitions[i]);
        }
        return partitionBeans;
    }

    public static Partition populatePartitionPojo(org.apache.stratos.cloud.controller.stub.deployment.partition.Partition
                                                          partition) {

        Partition partitionBeans = new Partition();
        if (partition == null) {
            return partitionBeans;
        }

        partitionBeans.id = partition.getId();
        partitionBeans.description = partition.getDescription();
        partitionBeans.isPublic = partition.getIsPublic();
        partitionBeans.provider = partition.getProvider();
        partitionBeans.partitionMin = partition.getPartitionMin();
        partitionBeans.partitionMax = partition.getPartitionMax();
        //properties 
        if (partition.getProperties() != null) {
            List<PropertyBean> propertyBeans = getPropertyBeans(partition.getProperties());
            partitionBeans.property = propertyBeans;
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
        subscriptionDomainBean.domainName = subscriptionDomain.getDomainName();
        subscriptionDomainBean.applicationContext = subscriptionDomain.getApplicationContext();

        return subscriptionDomainBean;
    }

    private static List<PropertyBean> getPropertyBeans(Properties properties) {

        List<PropertyBean> propertyBeans = null;
        if (properties.getProperties() != null && properties.getProperties().length != 0) {
            Property[] propertyArr = properties.getProperties();
            propertyBeans = new ArrayList<PropertyBean>();
            for (int i = 0; i < propertyArr.length; i++) {
                PropertyBean propertyBean = new PropertyBean();
                propertyBean.name = propertyArr[i].getName();
                propertyBean.value = propertyArr[i].getValue();
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
                propertyBean.name = key;
                propertyBean.value = value;
                propertyBeans.add(propertyBean);
            }
        }
        return propertyBeans;
    }

    public static AutoscalePolicy[] populateAutoscalePojos(org.apache.stratos.autoscaler.stub.policy.model.AutoscalePolicy[]
                                                                   autoscalePolicies) {

        AutoscalePolicy[] autoscalePolicyBeans;
        if (autoscalePolicies == null) {
            autoscalePolicyBeans = new AutoscalePolicy[0];
            return autoscalePolicyBeans;
        }

        autoscalePolicyBeans = new AutoscalePolicy[autoscalePolicies.length];
        for (int i = 0 ; i < autoscalePolicies.length ; i++) {
            autoscalePolicyBeans[i] = populateAutoscalePojo(autoscalePolicies[i]);
        }
        return autoscalePolicyBeans;
    }

    public static AutoscalePolicy populateAutoscalePojo(org.apache.stratos.autoscaler.stub.policy.model.AutoscalePolicy
                                                                autoscalePolicy) {

        AutoscalePolicy autoscalePolicyBean = new AutoscalePolicy();
        if (autoscalePolicy == null) {
            return autoscalePolicyBean;
        }

        autoscalePolicyBean.setId(autoscalePolicy.getId());
        autoscalePolicyBean.setDescription(autoscalePolicy.getDescription());
        autoscalePolicyBean.setIsPublic(autoscalePolicy.getIsPublic());
        autoscalePolicyBean.setDisplayName(autoscalePolicy.getDisplayName());
        autoscalePolicyBean.setDescription(autoscalePolicy.getDescription());
        if (autoscalePolicy.getLoadThresholds() != null) {
            autoscalePolicyBean.setLoadThresholds(populateLoadThresholds(autoscalePolicy.getLoadThresholds()));
        }

        return autoscalePolicyBean;
    }

    private static LoadThresholds populateLoadThresholds(org.apache.stratos.autoscaler.stub.policy.model.LoadThresholds
                                                                 loadThresholds) {

        LoadThresholds loadThresholdBean = new LoadThresholds();
        if (loadThresholds.getLoadAverage() != null) {
            LoadAverageThresholds loadAverage = new LoadAverageThresholds();
            loadAverage.upperLimit = loadThresholds.getLoadAverage().getUpperLimit();
            loadAverage.lowerLimit = loadThresholds.getLoadAverage().getLowerLimit();
            loadThresholdBean.loadAverage = loadAverage;
        }
        if (loadThresholds.getMemoryConsumption() != null) {
            MemoryConsumptionThresholds memoryConsumption = new MemoryConsumptionThresholds();
            memoryConsumption.upperLimit = loadThresholds.getMemoryConsumption().getUpperLimit();
            memoryConsumption.lowerLimit = loadThresholds.getMemoryConsumption().getLowerLimit();
            loadThresholdBean.memoryConsumption = memoryConsumption;
        }
        if (loadThresholds.getRequestsInFlight() != null) {
            RequestsInFlightThresholds requestsInFlight = new RequestsInFlightThresholds();
            requestsInFlight.upperLimit = loadThresholds.getRequestsInFlight().getUpperLimit();
            requestsInFlight.lowerLimit = loadThresholds.getRequestsInFlight().getLowerLimit();
            loadThresholdBean.requestsInFlight = requestsInFlight;
        }

        return loadThresholdBean;
    }

    public static DeploymentPolicy[] populateDeploymentPolicyPojos(org.apache.stratos.autoscaler.stub.policy.model.DeploymentPolicy[]
                                                                           deploymentPolicies) {
        DeploymentPolicy[] deploymentPolicyBeans;
        if (deploymentPolicies == null) {
            return null;
        }

        deploymentPolicyBeans = new DeploymentPolicy[deploymentPolicies.length];
        for (int i = 0 ; i < deploymentPolicies.length ; i++) {
            deploymentPolicyBeans[i] = populateDeploymentPolicyPojo(deploymentPolicies[i]);
        }

        return deploymentPolicyBeans;
    }

    public static DeploymentPolicy populateDeploymentPolicyPojo(org.apache.stratos.autoscaler.stub.policy.model.DeploymentPolicy
                                                                        deploymentPolicy) {

        DeploymentPolicy deploymentPolicyBean = new DeploymentPolicy();
        if (deploymentPolicy == null) {
            return deploymentPolicyBean;
        }

        deploymentPolicyBean.setId(deploymentPolicy.getId());
        deploymentPolicyBean.setDescription(deploymentPolicy.getDescription());
        deploymentPolicyBean.setPublic(deploymentPolicy.getIsPublic());

        if (deploymentPolicy.getPartitionGroups() != null && deploymentPolicy.getPartitionGroups().length > 0) {
            deploymentPolicyBean.setPartitionGroup(Arrays.asList(populatePartitionGroupPojos(deploymentPolicy.getPartitionGroups())));
        }

        /*if (deploymentPolicy.getAllPartitions() != null && deploymentPolicy.getAllPartitions().length > 0) {
            deploymentPolicyBean.partition = Arrays.asList(populatePartitionPojos(deploymentPolicy.getAllPartitions()));
        }*/

        return deploymentPolicyBean;
    }

    public static PartitionGroup populatePartitionGroupPojo(org.apache.stratos.autoscaler.stub.partition.PartitionGroup
                                                                    partitionGroup) {

        PartitionGroup partitionGroupBean = new PartitionGroup();
        if (partitionGroup == null) {
            return partitionGroupBean;
        }

        partitionGroupBean.id = partitionGroup.getId();
        partitionGroupBean.partitionAlgo = partitionGroup.getPartitionAlgo();
        if (partitionGroup.getPartitions() != null && partitionGroup.getPartitions().length > 0) {
            partitionGroupBean.partition = getPartitionList(partitionGroup.getPartitions());
        }

        return partitionGroupBean;
    }

    public static PartitionGroup[] populatePartitionGroupPojos(org.apache.stratos.autoscaler.stub.partition.PartitionGroup[] partitionGroups) {

        PartitionGroup[] partitionGroupsBeans;
        if (partitionGroups == null) {
            partitionGroupsBeans = new PartitionGroup[0];
            return partitionGroupsBeans;
        }

        partitionGroupsBeans = new PartitionGroup[partitionGroups.length];

        for (int i = 0 ; i < partitionGroups.length ; i ++) {
            partitionGroupsBeans[i] = populatePartitionGroupPojo(partitionGroups[i]);
        }

        return partitionGroupsBeans;
    }

    private static List<Partition> getPartitionList(org.apache.stratos.cloud.controller.stub.deployment.partition.Partition[]
                                                            partitions) {

        List<Partition> partitionList = new ArrayList<Partition>();
        for (int i = 0; i < partitions.length; i++) {
            Partition partition = new Partition();
            partition.id = partitions[i].getId();
            partition.provider = partitions[i].getProvider();
            partition.partitionMin = partitions[i].getPartitionMin();
            partition.partitionMax = partitions[i].getPartitionMax();
            if (partitions[i].getProperties() != null) {
                partition.property = getPropertyBeans(partitions[i].getProperties());
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

    public static org.apache.stratos.autoscaler.stub.kubernetes.KubernetesGroup convertToASKubernetesGroupPojo(KubernetesGroup kubernetesGroupBean) {

        org.apache.stratos.autoscaler.stub.kubernetes.KubernetesGroup kubernetesGroup = new
                org.apache.stratos.autoscaler.stub.kubernetes.KubernetesGroup();

        kubernetesGroup.setGroupId(kubernetesGroupBean.getGroupId());
        kubernetesGroup.setDescription(kubernetesGroupBean.getDescription());
        kubernetesGroup.setKubernetesMaster(convertToASKubernetesMasterPojo(kubernetesGroupBean.getKubernetesMaster()));
        kubernetesGroup.setPortRange(convertToASPortRange(kubernetesGroupBean.getPortRange()));
        kubernetesGroup.setKubernetesHosts(convertToASKubernetesHostsPojo(kubernetesGroupBean.getKubernetesHosts()));
        kubernetesGroup.setProperties((getASProperties(kubernetesGroupBean.getProperty())));

        return kubernetesGroup;
    }

    private static org.apache.stratos.autoscaler.stub.kubernetes.KubernetesHost[] convertToASKubernetesHostsPojo(List<KubernetesHost> kubernetesHosts) {
        if (kubernetesHosts == null || kubernetesHosts.isEmpty()) {
            return null;
        }
        int kubernetesHostCount = kubernetesHosts.size();
        org.apache.stratos.autoscaler.stub.kubernetes.KubernetesHost[]
                kubernetesHostsArr = new org.apache.stratos.autoscaler.stub.kubernetes.KubernetesHost[kubernetesHostCount];
        for (int i = 0; i < kubernetesHostCount; i++) {
            KubernetesHost kubernetesHostBean = kubernetesHosts.get(i);
            kubernetesHostsArr[i] = convertToASKubernetesHostPojo(kubernetesHostBean);
        }
        return kubernetesHostsArr;
    }


    private static org.apache.stratos.autoscaler.stub.kubernetes.PortRange convertToASPortRange(PortRange portRangeBean) {
        if (portRangeBean == null) {
            return null;
        }
        org.apache.stratos.autoscaler.stub.kubernetes.PortRange
                portRange = new org.apache.stratos.autoscaler.stub.kubernetes.PortRange();
        portRange.setLower(portRangeBean.getLower());
        portRange.setUpper(portRangeBean.getUpper());
        return portRange;
    }

    public static org.apache.stratos.autoscaler.stub.kubernetes.KubernetesHost convertToASKubernetesHostPojo(KubernetesHost kubernetesHostBean) {
        if (kubernetesHostBean == null) {
            return null;
        }

        org.apache.stratos.autoscaler.stub.kubernetes.KubernetesHost
                kubernetesHost = new org.apache.stratos.autoscaler.stub.kubernetes.KubernetesHost();
        kubernetesHost.setHostId(kubernetesHostBean.getHostId());
        kubernetesHost.setHostIpAddress(kubernetesHostBean.getHostIpAddress());
        kubernetesHost.setHostname(kubernetesHostBean.getHostname());
        kubernetesHost.setProperties(getASProperties(kubernetesHostBean.getProperty()));

        return kubernetesHost;
    }

    public static org.apache.stratos.autoscaler.stub.kubernetes.KubernetesMaster convertToASKubernetesMasterPojo(KubernetesMaster kubernetesMasterBean) {
        if (kubernetesMasterBean == null) {
            return null;
        }

        org.apache.stratos.autoscaler.stub.kubernetes.KubernetesMaster
                kubernetesMaster = new org.apache.stratos.autoscaler.stub.kubernetes.KubernetesMaster();
        kubernetesMaster.setHostId(kubernetesMasterBean.getHostId());
        kubernetesMaster.setHostIpAddress(kubernetesMasterBean.getHostIpAddress());
        kubernetesMaster.setHostname(kubernetesMasterBean.getHostname());
        kubernetesMaster.setEndpoint(kubernetesMasterBean.getEndpoint());
        kubernetesMaster.setProperties(getASProperties(kubernetesMasterBean.getProperty()));

        return kubernetesMaster;
    }

    public static KubernetesGroup[] populateKubernetesGroupsPojo(org.apache.stratos.autoscaler.stub.kubernetes.KubernetesGroup[] kubernetesGroups) {

        if (kubernetesGroups == null){
            return null;
        }
        KubernetesGroup[] kubernetesGroupsBean = new KubernetesGroup[kubernetesGroups.length];
        for (int i = 0; i < kubernetesGroups.length; i++){
            kubernetesGroupsBean[i] = populateKubernetesGroupPojo(kubernetesGroups[i]);
        }
        return kubernetesGroupsBean;
    }

    public static KubernetesGroup populateKubernetesGroupPojo(org.apache.stratos.autoscaler.stub.kubernetes.KubernetesGroup kubernetesGroup) {
        if (kubernetesGroup == null){
            return null;
        }
        KubernetesGroup kubernetesGroupBean = new KubernetesGroup();
        kubernetesGroupBean.setGroupId(kubernetesGroup.getGroupId());
        kubernetesGroupBean.setDescription(kubernetesGroup.getDescription());
        kubernetesGroupBean.setPortRange(populatePortRangePojo(kubernetesGroup.getPortRange()));
        kubernetesGroupBean.setKubernetesHosts(populateKubernetesHostsPojo(kubernetesGroup.getKubernetesHosts()));
        kubernetesGroupBean.setKubernetesMaster(populateKubernetesMasterPojo(kubernetesGroup.getKubernetesMaster()));
        kubernetesGroupBean.setProperty(populateASProperties(kubernetesGroup.getProperties()));
        return kubernetesGroupBean;
    }

    public static KubernetesMaster populateKubernetesMasterPojo(org.apache.stratos.autoscaler.stub.kubernetes.KubernetesMaster kubernetesMaster) {
        if (kubernetesMaster == null){
            return null;
        }
        KubernetesMaster kubernetesMasterBean = new KubernetesMaster();
        kubernetesMasterBean.setHostId(kubernetesMaster.getHostId());
        kubernetesMasterBean.setHostname(kubernetesMaster.getHostname());
        kubernetesMasterBean.setHostIpAddress(kubernetesMaster.getHostIpAddress());
        kubernetesMasterBean.setProperty(populateASProperties(kubernetesMaster.getProperties()));
        kubernetesMasterBean.setEndpoint(kubernetesMaster.getEndpoint());
        return kubernetesMasterBean;
    }

    public static List<KubernetesHost> populateKubernetesHostsPojo(org.apache.stratos.autoscaler.stub.kubernetes.KubernetesHost[] kubernetesHosts) {
        if (kubernetesHosts == null){
            return null;
        }
        List<KubernetesHost> kubernetesHostList = new ArrayList<KubernetesHost>();
        for (int i = 0; i < kubernetesHosts.length; i++){
            kubernetesHostList.add(populateKubernetesHostPojo(kubernetesHosts[i]));
        }
        return kubernetesHostList;
    }

    private static KubernetesHost populateKubernetesHostPojo(org.apache.stratos.autoscaler.stub.kubernetes.KubernetesHost kubernetesHost) {
        if (kubernetesHost == null){
            return null;
        }
        KubernetesHost kubernetesHostBean = new KubernetesHost();
        kubernetesHostBean.setHostId(kubernetesHost.getHostId());
        kubernetesHostBean.setHostname(kubernetesHost.getHostname());
        kubernetesHostBean.setHostIpAddress(kubernetesHost.getHostIpAddress());
        kubernetesHostBean.setProperty(populateASProperties(kubernetesHost.getProperties()));
        return kubernetesHostBean;
    }

    private static List<PropertyBean> populateASProperties(PropertiesE properties) {
        if (properties == null || properties.getProperties() == null){
            return null;
        }
        List<PropertyBean> propertyBeanList = new ArrayList<PropertyBean>();
        for (int i = 0; i < properties.getProperties().length; i++){
            propertyBeanList.add(populateASProperty(properties.getProperties()[i]));
        }
        return propertyBeanList;
    }

    private static PropertyBean populateASProperty(PropertyE propertyE) {
        if (propertyE == null){
            return null;
        }
        PropertyBean propertyBean = new PropertyBean();
        propertyBean.name = propertyE.getName();
        propertyBean.value = propertyE.getValue();
        return propertyBean;
    }

    private static PortRange populatePortRangePojo(org.apache.stratos.autoscaler.stub.kubernetes.PortRange portRange) {
        if (portRange == null){
            return null;
        }
        PortRange portRangeBean = new PortRange();
        portRangeBean.setUpper(portRange.getUpper());
        portRangeBean.setLower(portRange.getLower());
        return portRangeBean;
    }
    
    public static ApplicationContext convertApplicationBeanToApplicationContext (ApplicationDefinition compositeAppDefinition) {

        org.apache.stratos.autoscaler.applications.pojo.xsd.ApplicationContext applicationContext =
                new org.apache.stratos.autoscaler.applications.pojo.xsd.ApplicationContext();
        applicationContext.setApplicationId(compositeAppDefinition.getApplicationId());
        applicationContext.setAlias(compositeAppDefinition.getAlias());

        // convert and set components
        if (compositeAppDefinition.getComponents() != null) {
            org.apache.stratos.autoscaler.applications.pojo.xsd.ComponentContext componentContext =
                    new org.apache.stratos.autoscaler.applications.pojo.xsd.ComponentContext();
            // top level subscribables
            if (compositeAppDefinition.getComponents().getSubscribables() != null) {
                componentContext.setSubscribableContexts(getSubscribableContextArrayFromSubscribableDefinitions(
                        compositeAppDefinition.getComponents().getSubscribables()));
            }
            // top level Groups
            if (compositeAppDefinition.getComponents().getGroups() != null) {
                componentContext.setGroupContexts(getgroupContextArrayFromGroupDefinitions(compositeAppDefinition.getComponents().getGroups()));
            }
            // top level dependency information
            if (compositeAppDefinition.getComponents().getDependencies() != null) {
                componentContext.setDependencyContext(getDependencyContextFromDependencyDefinition(compositeAppDefinition.getComponents().getDependencies()));
            }

            applicationContext.setComponents(componentContext);
        }

        // subscribable information
        applicationContext.setSubscribableInfoContext(getSubscribableInfoContextArrFromSubscribableInfoDefinition(compositeAppDefinition.getSubscribableInfo()));

        return applicationContext;
    }

    private static SubscribableInfoContext[] getSubscribableInfoContextArrFromSubscribableInfoDefinition (List<SubscribableInfo> subscribableInfos) {

        SubscribableInfoContext[] subscribableInfoContexts = new SubscribableInfoContext[subscribableInfos.size()];
        int i = 0;
        for (SubscribableInfo subscribableInfo : subscribableInfos) {
            SubscribableInfoContext subscribableInfoContext = new SubscribableInfoContext();
            subscribableInfoContext.setAlias(subscribableInfo.getAlias());
            subscribableInfoContext.setAutoscalingPolicy(subscribableInfo.getAutoscalingPolicy());
            subscribableInfoContext.setDeploymentPolicy(subscribableInfo.getDeploymentPolicy());
            subscribableInfoContext.setRepoUrl(subscribableInfo.getRepoUrl());
            subscribableInfoContext.setPrivateRepo(subscribableInfo.isPrivateRepo());
            subscribableInfoContext.setRepoUsername(subscribableInfo.getRepoUsername());
            subscribableInfoContext.setRepoPassword(subscribableInfo.getRepoPassword());
            subscribableInfoContext.setDependencyAliases(subscribableInfo.getDependencyAliases());
            if (subscribableInfo.getProperty() != null) {
            	org.apache.stratos.cloud.controller.stub.pojo.Properties properties = new Properties();
            	for (org.apache.stratos.manager.composite.application.beans.PropertyBean propertyBean : subscribableInfo.getProperty()) {
            		Property property = new Property();
            		property.setName(propertyBean.getName());
            		property.setValue(propertyBean.getValue());
            		properties.addProperties(property);
            	}
            	subscribableInfoContext.setProperties(properties);
            }
            subscribableInfoContexts[i++] =  subscribableInfoContext;
        }
        return subscribableInfoContexts;
    }

    private static DependencyContext getDependencyContextFromDependencyDefinition (DependencyDefinitions dependencyDefinitions) {

        DependencyContext dependencyContext = new DependencyContext();
        dependencyContext.setTerminationBehaviour(dependencyDefinitions.getTerminationBehaviour());
        
        if (dependencyDefinitions != null && dependencyDefinitions.getStartupOrders() != null) {
        	String [] startupOrders = new String [dependencyDefinitions.getStartupOrders().size()];
        	startupOrders = dependencyDefinitions.getStartupOrders().toArray(startupOrders);
        	dependencyContext.setStartupOrdersContexts(startupOrders);
        }

        return dependencyContext;
    }

    private static org.apache.stratos.autoscaler.applications.pojo.xsd.GroupContext[]
        getgroupContextArrayFromGroupDefinitions (List<GroupDefinition> groupDefinitions) {

        GroupContext[] groupContexts = new GroupContext[groupDefinitions.size()];
        int i = 0;
        for (GroupDefinition groupDefinition : groupDefinitions) {
            GroupContext groupContext = new GroupContext();
            groupContext.setName(groupDefinition.getName());
            groupContext.setAlias(groupDefinition.getAlias());
            groupContext.setDeploymentPolicy(groupDefinition.getDeploymentPolicy());
            groupContext.setAutoscalingPolicy(groupDefinition.getAutoscalingPolicy());
            // nested Subscribables
            if (groupDefinition.getSubscribables() != null) {
                groupContext.setSubscribableContexts(
                        getSubscribableContextArrayFromSubscribableDefinitions(groupDefinition.getSubscribables()));
            }
            // nested Groups
            if (groupDefinition.getSubGroups() != null) {
                groupContext.setGroupContexts(getgroupContextArrayFromGroupDefinitions(groupDefinition.getSubGroups()));
            }
            groupContexts[i++] = groupContext;
        }

        return groupContexts;
    }

    private static org.apache.stratos.autoscaler.applications.pojo.xsd.SubscribableContext []
        getSubscribableContextArrayFromSubscribableDefinitions(List<SubscribableDefinition> subscribableDefinitions) {

        org.apache.stratos.autoscaler.applications.pojo.xsd.SubscribableContext[] subscribableContexts =
                new org.apache.stratos.autoscaler.applications.pojo.xsd.SubscribableContext[subscribableDefinitions.size()];
        int i = 0;
        for (SubscribableDefinition subscribableDefinition : subscribableDefinitions) {
            org.apache.stratos.autoscaler.applications.pojo.xsd.SubscribableContext subscribableContext =
                    new org.apache.stratos.autoscaler.applications.pojo.xsd.SubscribableContext();
            subscribableContext.setType(subscribableDefinition.getType());
            subscribableContext.setAlias(subscribableDefinition.getAlias());
            subscribableContexts[i++] = subscribableContext;
        }

        return subscribableContexts;
    }


    public static ApplicationBean applicationToBean(Application application) {

        if(application == null){
            return null;
        }

        ApplicationBean applicationBean = new ApplicationBean();
        applicationBean.setId(application.getUniqueIdentifier());
        applicationBean.setTenantDomain(application.getTenantDomain());
        applicationBean.setTenantAdminUsername(application.getTenantAdminUserName());
        return applicationBean;
    }

    public static GroupBean toGroupBean(Group group) {
        if(group == null){
            return null;
        }

        GroupBean groupBean = new GroupBean();
        groupBean.setAlias(group.getUniqueIdentifier());
        groupBean.setDeploymentPolicy(group.getDeploymentPolicy());
        groupBean.setAutoScalingPolicy(group.getAutoscalingPolicy());
        return groupBean;
    }
	
}

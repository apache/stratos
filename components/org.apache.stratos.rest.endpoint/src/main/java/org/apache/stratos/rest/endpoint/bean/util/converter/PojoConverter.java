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

import org.apache.commons.lang.StringUtils;
import org.apache.stratos.autoscaler.stub.deployment.partition.ChildLevelPartition;
import org.apache.stratos.autoscaler.stub.deployment.partition.ChildLevelNetworkPartition;
import org.apache.stratos.autoscaler.stub.deployment.policy.ChildPolicy;
import org.apache.stratos.autoscaler.stub.pojo.ApplicationContext;
import org.apache.stratos.autoscaler.stub.pojo.DependencyContext;
import org.apache.stratos.autoscaler.stub.pojo.GroupContext;
import org.apache.stratos.autoscaler.stub.pojo.SubscribableInfoContext;
import org.apache.stratos.cloud.controller.stub.domain.*;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
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
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.ApplicationLevelNetworkPartition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.*;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.*;
import org.apache.stratos.rest.endpoint.bean.kubernetes.KubernetesGroup;
import org.apache.stratos.rest.endpoint.bean.kubernetes.KubernetesHost;
import org.apache.stratos.rest.endpoint.bean.kubernetes.KubernetesMaster;
import org.apache.stratos.rest.endpoint.bean.kubernetes.PortRange;
import org.apache.stratos.rest.endpoint.bean.subscription.domain.SubscriptionDomainBean;
import org.apache.stratos.rest.endpoint.bean.topology.Member;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

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
            cartridgeConfig.setProperties(getCCProperties(cartridgeDefinitionBean.property));
        }

        if (cartridgeDefinitionBean.getExportingProperties() != null) {
            cartridgeConfig.setExportingProperties(cartridgeDefinitionBean.getExportingProperties());
        }

        if (cartridgeDefinitionBean.container != null) {
            cartridgeConfig.setContainer(getContainer(cartridgeDefinitionBean.container));
        }

        return cartridgeConfig;
    }

    public static ServiceGroup populateServiceGroupPojo(ServiceGroupDefinition serviceGroupDefinition) {
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
            lbConfig.setProperties(getCCProperties(loadBalancer.property));
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
                iaasConfig.setProperties(getCCProperties(iaasProviderBeansArray[i].property));
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
    
    public static org.apache.stratos.cloud.controller.stub.Properties getCCProperties(List<PropertyBean> propertyBeans) {

        //convert to an array
        PropertyBean[] propertyBeansArray = new PropertyBean[propertyBeans.size()];
        propertyBeans.toArray(propertyBeansArray);
        org.apache.stratos.cloud.controller.stub.Property[] propertyArray = new org.apache.stratos.cloud.controller.stub.Property[propertyBeansArray.length];

        for (int j = 0; j < propertyBeansArray.length; j++) {
            org.apache.stratos.cloud.controller.stub.Property property = new org.apache.stratos.cloud.controller.stub.Property();
            property.setName(propertyBeansArray[j].name);
            property.setValue(propertyBeansArray[j].value);
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
            property.setName(propertyBeansArray[j].name);
            property.setValue(propertyBeansArray[j].value);
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
            networkInterface.setNetworkUuid(nib.networkUuid);
            networkInterface.setFixedIp(nib.fixedIp);
            networkInterface.setPortUuid(nib.portUuid);
            networkInterfacesArray[i++] = networkInterface;
        }

        NetworkInterfaces networkInterfaces = new NetworkInterfaces();
        networkInterfaces.setNetworkInterfaces(networkInterfacesArray);
        return networkInterfaces;
    }

    public static org.apache.stratos.autoscaler.stub.deployment.partition.Partition convertToCCPartitionPojo
            (Partition partitionBean) {

        org.apache.stratos.autoscaler.stub.deployment.partition.Partition partition = new
                org.apache.stratos.autoscaler.stub.deployment.partition.Partition();

        partition.setId(partitionBean.id);
        partition.setDescription(partitionBean.description);
        partition.setIsPublic(partitionBean.isPublic);
        partition.setProvider(partitionBean.provider);

        if (partitionBean.property != null && !partitionBean.property.isEmpty()) {
            partition.setProperties(getASProperties(partitionBean.property));
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

            if (autoscalePolicyBean.getLoadThresholds().loadAverage != null) {

                //set load average information
                org.apache.stratos.autoscaler.stub.autoscale.policy.LoadAverageThresholds loadAverage = new
                        org.apache.stratos.autoscaler.stub.autoscale.policy.LoadAverageThresholds();
                loadAverage.setUpperLimit(autoscalePolicyBean.getLoadThresholds().loadAverage.upperLimit);
                loadAverage.setLowerLimit(autoscalePolicyBean.getLoadThresholds().loadAverage.lowerLimit);
                //set load average
                loadThresholds.setLoadAverage(loadAverage);
            }
            if (autoscalePolicyBean.getLoadThresholds().requestsInFlight != null) {

                org.apache.stratos.autoscaler.stub.autoscale.policy.RequestsInFlightThresholds requestsInFlight = new
                        org.apache.stratos.autoscaler.stub.autoscale.policy.RequestsInFlightThresholds();
                //set request in flight information
                requestsInFlight.setUpperLimit(autoscalePolicyBean.getLoadThresholds().requestsInFlight.upperLimit);
                requestsInFlight.setLowerLimit(autoscalePolicyBean.getLoadThresholds().requestsInFlight.lowerLimit);
                //set request in flight
                loadThresholds.setRequestsInFlight(requestsInFlight);
            }
            if (autoscalePolicyBean.getLoadThresholds().memoryConsumption != null) {

                org.apache.stratos.autoscaler.stub.autoscale.policy.MemoryConsumptionThresholds memoryConsumption = new
                        org.apache.stratos.autoscaler.stub.autoscale.policy.MemoryConsumptionThresholds();

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

    public static org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy convetToASDeploymentPolicyPojo(
            DeploymentPolicy deploymentPolicyBean) {

        org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy deploymentPolicy =
                new org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy();

        deploymentPolicy.setId(deploymentPolicyBean.id);
        deploymentPolicy.setDescription(deploymentPolicyBean.description);
        deploymentPolicy.setIsPublic(deploymentPolicyBean.isPublic);
        if (deploymentPolicyBean.applicationPolicy != null
                && deploymentPolicyBean.applicationPolicy.networkPartition != null
                && !deploymentPolicyBean.applicationPolicy.networkPartition.isEmpty()) {
            deploymentPolicy
                    .setApplicationLevelNetworkPartitions(convertToCCPartitionGroup(deploymentPolicyBean.applicationPolicy.networkPartition));
            deploymentPolicy.setApplicationId(deploymentPolicyBean.applicationPolicy.applicationId);
        }

        if (deploymentPolicyBean.childPolicies != null && !deploymentPolicyBean.childPolicies.isEmpty()) {
            deploymentPolicy.setChildPolicies(convertToCCChildPolicy(deploymentPolicyBean.childPolicies));
        }

        return deploymentPolicy;
    }

    private static org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition[] convertToCCPartitionGroup(List<ApplicationLevelNetworkPartition> networkPartitionBeans) {

        org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition[]
                appNWPartitions = new
                org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition
                [networkPartitionBeans.size()];

        for (int i = 0; i < networkPartitionBeans.size(); i++) {
            org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition appNWPartition = new
                    org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition();
            appNWPartition.setId(networkPartitionBeans.get(i).id);
            appNWPartition.setActiveByDefault(networkPartitionBeans.get(i).activeByDefault);
            if (networkPartitionBeans.get(i).partitions != null && !networkPartitionBeans.get(i).partitions.isEmpty()) {
                appNWPartition.setPartitions(convertToCCPartitionPojos(networkPartitionBeans.get(i).partitions));
            }

            appNWPartitions[i] = appNWPartition;
        }

        return appNWPartitions;
    }

    private static ChildPolicy[] convertToCCChildPolicy(List<org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.ChildPolicy> childPolicies) {

        ChildPolicy[] childPolicies1 = new ChildPolicy[childPolicies.size()];

        for (int i = 0; i < childPolicies.size(); i++) {
            ChildPolicy childPolicy = new ChildPolicy();
            childPolicy.setId(childPolicies.get(i).childId);
            childPolicy.setChildLevelNetworkPartitions(convertToCCChildNetworkPartition(childPolicies.get(i).networkPartition));


            childPolicies1[i] = childPolicy;
        }

        return childPolicies1;
    }

    private static ChildLevelNetworkPartition[] convertToCCChildNetworkPartition(List<org.apache.stratos.rest.endpoint.bean.autoscaler.partition.ChildLevelNetworkPartition> networkPartitions) {

        ChildLevelNetworkPartition[] childLevelNetworkPartitions = new ChildLevelNetworkPartition[networkPartitions.size()];

        for (int i = 0; i < networkPartitions.size(); i++) {
            ChildLevelNetworkPartition childLevelNetworkPartition = new ChildLevelNetworkPartition();
            childLevelNetworkPartition.setId(networkPartitions.get(i).id);
            childLevelNetworkPartition.setPartitionAlgo(networkPartitions.get(i).partitionAlgo);
            childLevelNetworkPartition.setChildLevelPartitions(convertToCCChildPartitionPojos(networkPartitions.get(i).partitions));

            childLevelNetworkPartitions[i] = childLevelNetworkPartition;
        }

        return childLevelNetworkPartitions;
    }

    public static org.apache.stratos.rest.endpoint.bean.topology.Cluster populateClusterPojos(Cluster cluster, String alias) {
        org.apache.stratos.rest.endpoint.bean.topology.Cluster cluster1 = new
                org.apache.stratos.rest.endpoint.bean.topology.Cluster();
        cluster1.alias = alias;
        cluster1.serviceName = cluster.getServiceName();
        cluster1.clusterId = cluster.getClusterId();
        cluster1.isLbCluster = cluster.isLbCluster();
        cluster1.tenantRange = cluster.getTenantRange();
        cluster1.property = getPropertyBeans(cluster.getProperties());
        cluster1.member = new ArrayList<Member>();
        cluster1.hostNames = new ArrayList<String>();
        cluster1.status = cluster.getStatus(null).toString();

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
            (List<org.apache.stratos.rest.endpoint.bean.autoscaler.partition.ChildLevelPartition> partitionList) {

        ChildLevelPartition[] childLevelPartitions = new ChildLevelPartition[partitionList.size()];
        for (int i = 0; i < partitionList.size(); i++) {
            ChildLevelPartition childLevelPartition = new ChildLevelPartition();
            childLevelPartition.setPartitionId(partitionList.get(i).id);
            childLevelPartition.setMax(partitionList.get(i).max);

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

        partitionBeans.id = partition.getId();
        partitionBeans.description = partition.getDescription();
        partitionBeans.isPublic = partition.getIsPublic();
        partitionBeans.provider = partition.getProvider();
        /*partitionBeans.partitionMin = partition.getPartitionMin();
        partitionBeans.partitionMax = partition.getPartitionMax();*/
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
    
    private static List<PropertyBean> getPropertyBeans(org.apache.stratos.cloud.controller.stub.Properties properties) {

        List<PropertyBean> propertyBeans = null;
        if (properties.getProperties() != null && properties.getProperties().length != 0) {
            org.apache.stratos.cloud.controller.stub.Property[] propertyArr = properties.getProperties();
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

        AutoscalePolicy autoscalePolicyBean = new AutoscalePolicy();
        if (autoscalePolicy == null) {
            return autoscalePolicyBean;
        }

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

        DeploymentPolicy deploymentPolicyBean = new DeploymentPolicy();
        if (deploymentPolicy == null) {
            return deploymentPolicyBean;
        }

        deploymentPolicyBean.id = deploymentPolicy.getId();
        deploymentPolicyBean.description = deploymentPolicy.getDescription();
        deploymentPolicyBean.isPublic = deploymentPolicy.getIsPublic();
//TODO populate the Network partition based on new policy structure
//        if (deploymentPolicy.getApplicationLevelNetworkPartition() != null && deploymentPolicy.getApplicationLevelNetworkPartition().length > 0) {
//            deploymentPolicyBean.setPartitionGroup(Arrays.asList(populatePartitionGroupPojos(deploymentPolicy.getApplicationLevelNetworkPartition())));
//        }

        /*if (deploymentPolicy.getAllPartitions() != null && deploymentPolicy.getAllPartitions().length > 0) {
            deploymentPolicyBean.partition = Arrays.asList(populatePartitionPojos(deploymentPolicy.getAllPartitions()));
        }*/

        return deploymentPolicyBean;
    }

    public static ApplicationLevelNetworkPartition populatePartitionGroupPojo(org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition
                                                                                      partitionGroup) {

        ApplicationLevelNetworkPartition networkPartitionBean = new ApplicationLevelNetworkPartition();
        if (partitionGroup == null) {
            return networkPartitionBean;
        }

        networkPartitionBean.id = partitionGroup.getId();
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
            partition.id = partitions[i].getId();
            partition.provider = partitions[i].getProvider();
            /*partition.partitionMin = partitions[i].getPartitionMin();
            partition.partitionMax = partitions[i].getPartitionMax();*/
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

        if (kubernetesGroups == null) {
            return null;
        }
        KubernetesGroup[] kubernetesGroupsBean = new KubernetesGroup[kubernetesGroups.length];
        for (int i = 0; i < kubernetesGroups.length; i++) {
            kubernetesGroupsBean[i] = populateKubernetesGroupPojo(kubernetesGroups[i]);
        }
        return kubernetesGroupsBean;
    }

    public static KubernetesGroup populateKubernetesGroupPojo(org.apache.stratos.autoscaler.stub.kubernetes.KubernetesGroup kubernetesGroup) {
        if (kubernetesGroup == null) {
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
        if (kubernetesMaster == null) {
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
        if (kubernetesHosts == null) {
            return null;
        }
        List<KubernetesHost> kubernetesHostList = new ArrayList<KubernetesHost>();
        for (int i = 0; i < kubernetesHosts.length; i++) {
            kubernetesHostList.add(populateKubernetesHostPojo(kubernetesHosts[i]));
        }
        return kubernetesHostList;
    }

    private static KubernetesHost populateKubernetesHostPojo(org.apache.stratos.autoscaler.stub.kubernetes.KubernetesHost kubernetesHost) {
        if (kubernetesHost == null) {
            return null;
        }
        KubernetesHost kubernetesHostBean = new KubernetesHost();
        kubernetesHostBean.setHostId(kubernetesHost.getHostId());
        kubernetesHostBean.setHostname(kubernetesHost.getHostname());
        kubernetesHostBean.setHostIpAddress(kubernetesHost.getHostIpAddress());
        kubernetesHostBean.setProperty(populateASProperties(kubernetesHost.getProperties()));
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

    private static PropertyBean populateASProperty(org.apache.stratos.autoscaler.stub.Property propertyE) {
        if (propertyE == null) {
            return null;
        }
        PropertyBean propertyBean = new PropertyBean();
        propertyBean.name = propertyE.getName();
        propertyBean.value = propertyE.getValue();
        return propertyBean;
    }

    private static PortRange populatePortRangePojo(org.apache.stratos.autoscaler.stub.kubernetes.PortRange portRange) {
        if (portRange == null) {
            return null;
        }
        PortRange portRangeBean = new PortRange();
        portRangeBean.setUpper(portRange.getUpper());
        portRangeBean.setLower(portRange.getLower());
        return portRangeBean;
    }

    public static ApplicationContext convertApplicationBeanToApplicationContext(ApplicationDefinition compositeAppDefinition) {

        org.apache.stratos.autoscaler.stub.pojo.ApplicationContext applicationContext =
                new org.apache.stratos.autoscaler.stub.pojo.ApplicationContext();
        applicationContext.setApplicationId(compositeAppDefinition.getApplicationId());
        applicationContext.setAlias(compositeAppDefinition.getAlias());
        applicationContext.setDeploymentPolicy(compositeAppDefinition.getDeploymentPolicy());

        // convert and set components
        if (compositeAppDefinition.getComponents() != null) {
            org.apache.stratos.autoscaler.stub.pojo.ComponentContext componentContext =
                    new org.apache.stratos.autoscaler.stub.pojo.ComponentContext();
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

    private static SubscribableInfoContext[] getSubscribableInfoContextArrFromSubscribableInfoDefinition(List<SubscribableInfo> subscribableInfos) {

        SubscribableInfoContext[] subscribableInfoContexts = new SubscribableInfoContext[subscribableInfos.size()];
        int i = 0;
        for (SubscribableInfo subscribableInfo : subscribableInfos) {
            SubscribableInfoContext subscribableInfoContext = new SubscribableInfoContext();
            subscribableInfoContext.setAlias(subscribableInfo.getAlias());
            subscribableInfoContext.setAutoscalingPolicy(subscribableInfo.getAutoscalingPolicy());
            subscribableInfoContext.setDeploymentPolicy(subscribableInfo.getDeploymentPolicy());
            subscribableInfoContext.setRepoUrl(subscribableInfo.getRepoUrl());
            subscribableInfoContext.setMinMembers(subscribableInfo.getMinMembers());
            subscribableInfoContext.setMaxMembers(subscribableInfo.getMaxMembers());
            subscribableInfoContext.setPrivateRepo(subscribableInfo.isPrivateRepo());
            subscribableInfoContext.setRepoUsername(subscribableInfo.getRepoUsername());
            subscribableInfoContext.setRepoPassword(subscribableInfo.getRepoPassword());
            subscribableInfoContext.setDependencyAliases(subscribableInfo.getDependencyAliases());
            if (subscribableInfo.getProperty() != null) {
                org.apache.stratos.autoscaler.stub.Properties properties = new org.apache.stratos.autoscaler.stub.Properties();
                for (org.apache.stratos.manager.composite.application.beans.PropertyBean propertyBean : subscribableInfo.getProperty()) {
                    org.apache.stratos.autoscaler.stub.Property property = new org.apache.stratos.autoscaler.stub.Property();
                    property.setName(propertyBean.getName());
                    property.setValue(propertyBean.getValue());
                    properties.addProperties(property);
                }
                subscribableInfoContext.setProperties(properties);
            }
            subscribableInfoContexts[i++] = subscribableInfoContext;
        }
        return subscribableInfoContexts;
    }

    private static DependencyContext getDependencyContextFromDependencyDefinition(DependencyDefinitions dependencyDefinitions) {

        DependencyContext dependencyContext = new DependencyContext();
        dependencyContext.setTerminationBehaviour(dependencyDefinitions.getTerminationBehaviour());

        if (dependencyDefinitions != null && dependencyDefinitions.getStartupOrders() != null) {
            String[] startupOrders = new String[dependencyDefinitions.getStartupOrders().size()];
            startupOrders = dependencyDefinitions.getStartupOrders().toArray(startupOrders);
            dependencyContext.setStartupOrdersContexts(startupOrders);
        }

        return dependencyContext;
    }

    private static org.apache.stratos.autoscaler.stub.pojo.GroupContext[]
    getgroupContextArrayFromGroupDefinitions(List<GroupDefinition> groupDefinitions) {

        GroupContext[] groupContexts = new GroupContext[groupDefinitions.size()];
        int i = 0;
        for (GroupDefinition groupDefinition : groupDefinitions) {
            GroupContext groupContext = new GroupContext();
            groupContext.setName(groupDefinition.getName());
            groupContext.setAlias(groupDefinition.getAlias());
            groupContext.setDeploymentPolicy(groupDefinition.getDeploymentPolicy());
            groupContext.setGroupMaxInstances(groupDefinition.getGroupMaxInstances());
            groupContext.setGroupMinInstances(groupDefinition.getGroupMinInstances());
            groupContext.setGroupScalingEnabled(groupDefinition.isGroupScalingEnabled);
            groupContext.setGroupInstanceMonitoringEnabled(groupDefinition.isGroupInstanceMonitoringEnabled);
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

    private static org.apache.stratos.autoscaler.stub.pojo.SubscribableContext[]
    getSubscribableContextArrayFromSubscribableDefinitions(List<SubscribableDefinition> subscribableDefinitions) {

        org.apache.stratos.autoscaler.stub.pojo.SubscribableContext[] subscribableContexts =
                new org.apache.stratos.autoscaler.stub.pojo.SubscribableContext[subscribableDefinitions.size()];
        int i = 0;
        for (SubscribableDefinition subscribableDefinition : subscribableDefinitions) {
            org.apache.stratos.autoscaler.stub.pojo.SubscribableContext subscribableContext =
                    new org.apache.stratos.autoscaler.stub.pojo.SubscribableContext();
            subscribableContext.setType(subscribableDefinition.getType());
            subscribableContext.setAlias(subscribableDefinition.getAlias());
            subscribableContexts[i++] = subscribableContext;
        }

        return subscribableContexts;
    }


    public static ApplicationBean applicationToBean(Application application) {

        if (application == null) {
            return null;
        }

        ApplicationBean applicationBean = new ApplicationBean();
        applicationBean.setId(application.getUniqueIdentifier());
        applicationBean.setTenantDomain(application.getTenantDomain());
        applicationBean.setTenantAdminUsername(application.getTenantAdminUserName());
        return applicationBean;
    }

    public static GroupBean toGroupBean(Group group) {
        if (group == null) {
            return null;
        }

        GroupBean groupBean = new GroupBean();
        groupBean.setStatus(group.getStatus(null).toString());
        groupBean.setAlias(group.getUniqueIdentifier());
        //TODO*******groupBean.setDeploymentPolicy(group.getDeploymentPolicy());
        groupBean.setAutoScalingPolicy(group.getAutoscalingPolicy());
        return groupBean;
    }

}

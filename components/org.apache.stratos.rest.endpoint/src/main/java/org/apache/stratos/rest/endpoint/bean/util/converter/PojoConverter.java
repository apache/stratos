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

import org.apache.stratos.cloud.controller.stub.pojo.*;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.PartitionGroup;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.*;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.*;
import org.apache.stratos.rest.endpoint.bean.topology.Member;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PojoConverter {

    public static CartridgeConfig populateCartridgeConfigPojo (CartridgeDefinitionBean cartridgeDefinitionBean) {

        CartridgeConfig cartridgeConfig = new CartridgeConfig();

        cartridgeConfig.setType(cartridgeDefinitionBean.type);
        cartridgeConfig.setHostName(cartridgeDefinitionBean.host);
        cartridgeConfig.setProvider(cartridgeDefinitionBean.provider);
        cartridgeConfig.setVersion(cartridgeDefinitionBean.version);
        cartridgeConfig.setMultiTenant(cartridgeDefinitionBean.multiTenant);
        cartridgeConfig.setDisplayName(cartridgeDefinitionBean.displayName);
        cartridgeConfig.setDescription(cartridgeDefinitionBean.description);
        cartridgeConfig.setDefaultAutoscalingPolicy(cartridgeDefinitionBean.defaultAutoscalingPolicy);
        cartridgeConfig.setDefaultDeploymentPolicy(cartridgeDefinitionBean.defaultDeploymentPolicy);


        
        //deployment information
        if(cartridgeDefinitionBean.deployment != null) {
            cartridgeConfig.setBaseDir(cartridgeDefinitionBean.deployment.baseDir);
            if(cartridgeDefinitionBean.deployment.dir != null && !cartridgeDefinitionBean.deployment.dir.isEmpty()) {
                cartridgeConfig.setDeploymentDirs(cartridgeDefinitionBean.deployment.dir.
                        toArray(new String[cartridgeDefinitionBean.deployment.dir.size()]));
            }
        }
        //port mapping
        if(cartridgeDefinitionBean.portMapping != null && !cartridgeDefinitionBean.portMapping.isEmpty()) {
            cartridgeConfig.setPortMappings(getPortMappingsAsArray(cartridgeDefinitionBean.portMapping));
        }
        
        //persistance mapping
        if(cartridgeDefinitionBean.persistence != null) {
            cartridgeConfig.setPersistence(getPersistence(cartridgeDefinitionBean.persistence));
        }
        
        //IaaS
        if(cartridgeDefinitionBean.iaasProvider != null & !cartridgeDefinitionBean.iaasProvider.isEmpty()) {
            cartridgeConfig.setIaasConfigs(getIaasConfigsAsArray(cartridgeDefinitionBean.iaasProvider));
        }
        //LB
        if(cartridgeDefinitionBean.loadBalancer != null) {
            cartridgeConfig.setLbConfig(getLBConfig(cartridgeDefinitionBean.loadBalancer));
        }
        //Properties
        if(cartridgeDefinitionBean.property != null && !cartridgeDefinitionBean.property.isEmpty()) {
            cartridgeConfig.setProperties(getProperties(cartridgeDefinitionBean.property));
        }

        return cartridgeConfig;
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
        PortMappingBean [] portMappingBeanArray = new PortMappingBean[portMappingBeans.size()];
        portMappingBeans.toArray(portMappingBeanArray);
        PortMapping [] portMappingArray = new PortMapping[portMappingBeanArray.length];

        for (int i = 0 ; i < portMappingBeanArray.length ; i++) {
            PortMapping portMapping = new PortMapping();
            portMapping.setProtocol(portMappingBeanArray[i].protocol);
            portMapping.setPort(Integer.toString(portMappingBeanArray[i].port));
            portMapping.setProxyPort(Integer.toString(portMappingBeanArray[i].proxyPort));
            portMappingArray[i] = portMapping;
        }

        return portMappingArray;
    }

    private static IaasConfig[] getIaasConfigsAsArray (List<IaasProviderBean> iaasProviderBeans) {

        //convert to an array
        IaasProviderBean [] iaasProviderBeansArray = new IaasProviderBean[iaasProviderBeans.size()];
        iaasProviderBeans.toArray(iaasProviderBeansArray);
        IaasConfig [] iaasConfigsArray =  new IaasConfig[iaasProviderBeansArray.length];

        for (int i = 0 ; i < iaasProviderBeansArray.length ; i++) {
            IaasConfig iaasConfig = new IaasConfig();
            iaasConfig.setType(iaasProviderBeansArray[i].type);
            iaasConfig.setImageId(iaasProviderBeansArray[i].imageId);
            iaasConfig.setMaxInstanceLimit(iaasProviderBeansArray[i].maxInstanceLimit);
            iaasConfig.setName(iaasProviderBeansArray[i].name);
            iaasConfig.setClassName(iaasProviderBeansArray[i].className);
            iaasConfig.setCredential(iaasProviderBeansArray[i].credential);
            iaasConfig.setIdentity(iaasProviderBeansArray[i].identity);
            iaasConfig.setProvider(iaasProviderBeansArray[i].provider);

            if(iaasProviderBeansArray[i].property != null && !iaasProviderBeansArray[i].property.isEmpty()) {
                //set the Properties instance to IaasConfig instance
                iaasConfig.setProperties(getProperties(iaasProviderBeansArray[i].property));
            }
            iaasConfigsArray[i] = iaasConfig;
        }
        return iaasConfigsArray;
    }

    private static Persistence getPersistence(PersistenceBean persistenceBean) {
        Persistence persistence = new Persistence();
        persistence.setPersistanceRequired(persistenceBean.isRequired);
        VolumeBean[] volumeBean = new VolumeBean[persistenceBean.volume.size()];
        persistenceBean.volume.toArray(volumeBean);
        Volume[] volumes = new Volume[persistenceBean.volume.size()];
        for (int i = 0 ; i < volumes.length ; i++) {
            Volume volume = new Volume();
            volume.setSize(Integer.parseInt(volumeBean[i].size));
            volume.setDevice(volumeBean[i].device);
            volume.setRemoveOntermination(volumeBean[i].removeOnTermination);
            volume.setMappingPath(volumeBean[i].mappingPath);
            volumes[i] = volume;
        }
        persistence.setVolumes(volumes);
        return persistence;

    }

    private static Properties getProperties (List<PropertyBean> propertyBeans) {

        //convert to an array
        PropertyBean [] propertyBeansArray = new PropertyBean[propertyBeans.size()];
        propertyBeans.toArray(propertyBeansArray);
        Property[] propertyArray = new Property[propertyBeansArray.length];

        for (int j = 0 ; j < propertyBeansArray.length ; j++) {
            Property property = new Property();
            property.setName(propertyBeansArray[j].name);
            property.setValue(propertyBeansArray[j].value);
            propertyArray[j] = property;
        }

        Properties properties = new Properties();
        properties.setProperties(propertyArray);
        return properties;
    }

    public static org.apache.stratos.cloud.controller.stub.deployment.partition.Partition convertToCCPartitionPojo
            (Partition partitionBean) {

        org.apache.stratos.cloud.controller.stub.deployment.partition.Partition partition = new
                org.apache.stratos.cloud.controller.stub.deployment.partition.Partition();

        partition.setId(partitionBean.id);
        partition.setProvider(partitionBean.provider);
        partition.setPartitionMin(partitionBean.partitionMin);
        partition.setPartitionMax(partitionBean.partitionMax);

        if(partitionBean.property != null && !partitionBean.property.isEmpty()) {
            partition.setProperties(getProperties(partitionBean.property));
        }

        return partition;
    }

    public static org.apache.stratos.autoscaler.policy.model.AutoscalePolicy convertToCCAutoscalerPojo (AutoscalePolicy
                                                                                                                autoscalePolicyBean) {

        org.apache.stratos.autoscaler.policy.model.AutoscalePolicy autoscalePolicy = new
                org.apache.stratos.autoscaler.policy.model.AutoscalePolicy();

        autoscalePolicy.setId(autoscalePolicyBean.getId());
        autoscalePolicy.setDescription(autoscalePolicyBean.getDescription());
        autoscalePolicy.setDisplayName(autoscalePolicyBean.getDisplayName());

        if (autoscalePolicyBean.getLoadThresholds() != null) {

            org.apache.stratos.autoscaler.policy.model.LoadThresholds loadThresholds = new
                    org.apache.stratos.autoscaler.policy.model.LoadThresholds();

            if(autoscalePolicyBean.getLoadThresholds().loadAverage != null) {

                //set load average information
                org.apache.stratos.autoscaler.policy.model.LoadAverage loadAverage = new
                        org.apache.stratos.autoscaler.policy.model.LoadAverage();
                loadAverage.setAverage(autoscalePolicyBean.getLoadThresholds().loadAverage.average);
                loadAverage.setGradient(autoscalePolicyBean.getLoadThresholds().loadAverage.gradient);
                loadAverage.setSecondDerivative(autoscalePolicyBean.getLoadThresholds().loadAverage.secondDerivative);
                loadAverage.setScaleDownMarginOfGradient(autoscalePolicyBean.getLoadThresholds().loadAverage.scaleDownMarginOfGradient);
                loadAverage.setScaleDownMarginOfSecondDerivative(autoscalePolicyBean.getLoadThresholds().loadAverage.scaleDownMarginOfSecondDerivative);
                //set load average
                loadThresholds.setLoadAverage(loadAverage);
            }
            if (autoscalePolicyBean.getLoadThresholds().requestsInFlight != null) {

                org.apache.stratos.autoscaler.policy.model.RequestsInFlight requestsInFlight = new
                        org.apache.stratos.autoscaler.policy.model.RequestsInFlight();
                //set request in flight information
                requestsInFlight.setAverage(autoscalePolicyBean.getLoadThresholds().requestsInFlight.average);
                requestsInFlight.setGradient(autoscalePolicyBean.getLoadThresholds().requestsInFlight.gradient);
                requestsInFlight.setSecondDerivative(autoscalePolicyBean.getLoadThresholds().requestsInFlight.secondDerivative);
                requestsInFlight.setScaleDownMarginOfGradient(autoscalePolicyBean.getLoadThresholds().requestsInFlight.scaleDownMarginOfGradient);
                requestsInFlight.setScaleDownMarginOfSecondDerivative(autoscalePolicyBean.getLoadThresholds().requestsInFlight.scaleDownMarginOfSecondDerivative);
                //set request in flight
                loadThresholds.setRequestsInFlight(requestsInFlight);
            }
            if (autoscalePolicyBean.getLoadThresholds().memoryConsumption != null) {

                org.apache.stratos.autoscaler.policy.model.MemoryConsumption memoryConsumption = new
                        org.apache.stratos.autoscaler.policy.model.MemoryConsumption();

                //set memory consumption information
                memoryConsumption.setAverage(autoscalePolicyBean.getLoadThresholds().memoryConsumption.average);
                memoryConsumption.setGradient(autoscalePolicyBean.getLoadThresholds().memoryConsumption.gradient);
                memoryConsumption.setSecondDerivative(autoscalePolicyBean.getLoadThresholds().memoryConsumption.secondDerivative);
                memoryConsumption.setScaleDownMarginOfGradient(autoscalePolicyBean.getLoadThresholds().memoryConsumption.scaleDownMarginOfGradient);
                memoryConsumption.setScaleDownMarginOfSecondDerivative(autoscalePolicyBean.getLoadThresholds().memoryConsumption.scaleDownMarginOfSecondDerivative);
                //set memory consumption
                loadThresholds.setMemoryConsumption(memoryConsumption);
            }

            autoscalePolicy.setLoadThresholds(loadThresholds);
        }

        return autoscalePolicy;
    }

    public static org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy convetToCCDeploymentPolicyPojo (DeploymentPolicy
                                                                                                                           deploymentPolicyBean) {

        org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy deploymentPolicy = new
                org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy();

        deploymentPolicy.setId(deploymentPolicyBean.id);
        if(deploymentPolicyBean.partitionGroup != null && !deploymentPolicyBean.partitionGroup.isEmpty()) {
            deploymentPolicy.setPartitionGroups(convertToCCPartitionGroup(deploymentPolicyBean.partitionGroup));
        }

        return deploymentPolicy;
    }

    private static org.apache.stratos.autoscaler.partition.PartitionGroup[] convertToCCPartitionGroup (List<PartitionGroup> partitionGroupBeans) {

        org.apache.stratos.autoscaler.partition.PartitionGroup[] partitionGroups = new
                org.apache.stratos.autoscaler.partition.PartitionGroup[partitionGroupBeans.size()];

        for (int i = 0 ; i < partitionGroupBeans.size() ; i++) {
            org.apache.stratos.autoscaler.partition.PartitionGroup partitionGroup = new
                    org.apache.stratos.autoscaler.partition.PartitionGroup();
            partitionGroup.setId(partitionGroupBeans.get(i).id);
            partitionGroup.setPartitionAlgo(partitionGroupBeans.get(i).partitionAlgo);

            if(partitionGroupBeans.get(i).partition != null && !partitionGroupBeans.get(i).partition.isEmpty()) {
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
        cluster1.member = new ArrayList<Member>();
        cluster1.hostNames = new ArrayList<String>();

        for(org.apache.stratos.messaging.domain.topology.Member tmp : cluster.getMembers()) {
            Member member = new Member();
            member.clusterId = tmp.getClusterId();
            member.lbClusterId  = tmp.getLbClusterId();
            member.networkPartitionId = tmp.getNetworkPartitionId();
            member.partitionId = tmp.getPartitionId();
            member.memberId = tmp.getMemberId();
            if(tmp.getMemberIp() == null) {
                member.memberIp = "NULL";
            } else {
                member.memberIp = tmp.getMemberIp();
            }
            if(tmp.getMemberPublicIp() == null) {
            	member.memberPublicIp = "NULL";
            } else {
            	member.memberPublicIp = tmp.getMemberPublicIp();
            }
            member.serviceName = tmp.getServiceName();
            member.status = tmp.getStatus().toString();
            cluster1.member.add(member);
        }

        for(String tmp1 : cluster.getHostNames()) {
            cluster1.hostNames.add(tmp1);
        }

        return cluster1;
    }

    private static org.apache.stratos.cloud.controller.stub.deployment.partition.Partition[] convertToCCPartitionPojos
            (List<Partition> partitionList) {

        org.apache.stratos.cloud.controller.stub.deployment.partition.Partition[] partitions =
                new org.apache.stratos.cloud.controller.stub.deployment.partition.Partition[partitionList.size()];
        for (int i = 0; i < partitionList.size() ; i++) {
            partitions[i] = convertToCCPartitionPojo(partitionList.get(i));
        }

        return partitions;
    }

    public static Partition[] populatePartitionPojos (org.apache.stratos.cloud.controller.stub.deployment.partition.Partition[]
                                                             partitions) {

        Partition [] partitionBeans;
        if(partitions == null) {
            partitionBeans = new Partition[0];
            return partitionBeans;
        }

        partitionBeans = new Partition[partitions.length];
        for (int i = 0 ; i < partitions.length ; i++) {
            /*Partition partition = new Partition();
            partition.id = partitions[i].getId();
            partition.provider = partitions[i].getProvider();
            partition.partitionMin = partitions[i].getPartitionMin();
            partition.partitionMax = partitions[i].getPartitionMax();*/
            //properties are not added currently, TODO if required
            //if(partitions[i].getProperties() != null) {
            //    List<PropertyBean> propertyBeans = getPropertyBeans(partitions[i].getProperties());
            //    partition.property = propertyBeans;
            //}
            partitionBeans[i] = populatePartitionPojo(partitions[i]);
        }
        return partitionBeans;
    }

    public static Partition populatePartitionPojo (org.apache.stratos.cloud.controller.stub.deployment.partition.Partition
                                                             partition) {

        Partition partitionBeans = new Partition();
        if(partition == null) {
            return partitionBeans;
        }

        partitionBeans.id = partition.getId();
        partitionBeans.provider = partition.getProvider();
        partitionBeans.partitionMin = partition.getPartitionMin();
        partitionBeans.partitionMax = partition.getPartitionMax();
        //properties 
        if(partition.getProperties() != null) {
            List<PropertyBean> propertyBeans = getPropertyBeans(partition.getProperties());
            partitionBeans.property = propertyBeans;
        }

        return partitionBeans;
    }

    private static List<PropertyBean> getPropertyBeans (Properties properties) {

        List<PropertyBean> propertyBeans = null;
        if(properties.getProperties() != null && properties.getProperties().length != 0) {
            Property [] propertyArr = properties.getProperties();
            propertyBeans = new ArrayList<PropertyBean>();
            for (int i = 0; i < propertyArr.length ; i++) {
                PropertyBean propertyBean = new PropertyBean();
                propertyBean.name = propertyArr[i].getName();
                propertyBean.value = propertyArr[i].getValue();
                propertyBeans.add(propertyBean);
            }
        }
        return propertyBeans;
    }

    public static AutoscalePolicy[] populateAutoscalePojos(org.apache.stratos.autoscaler.policy.model.AutoscalePolicy[]
                                                                   autoscalePolicies) {

        AutoscalePolicy [] autoscalePolicyBeans;
        if(autoscalePolicies == null) {
            autoscalePolicyBeans = new AutoscalePolicy[0];
            return autoscalePolicyBeans;
        }

        autoscalePolicyBeans = new AutoscalePolicy[autoscalePolicies.length];
        for (int i = 0 ; i < autoscalePolicies.length ; i++) {
            /*AutoscalePolicy autoscalePolicy = new AutoscalePolicy();
            autoscalePolicy.id = autoscalePolicies[i].getId();
            autoscalePolicy.displayName = autoscalePolicies[i].getDisplayName();
            autoscalePolicy.description = autoscalePolicies[i].getDescription();
            if(autoscalePolicies[i].getLoadThresholds() != null) {
                autoscalePolicy.loadThresholds = populateLoadThresholds(autoscalePolicies[i].getLoadThresholds());
            }*/
            autoscalePolicyBeans[i] = populateAutoscalePojo(autoscalePolicies[i]);
        }
        return autoscalePolicyBeans;
    }

    public static AutoscalePolicy populateAutoscalePojo(org.apache.stratos.autoscaler.policy.model.AutoscalePolicy
                                                                   autoscalePolicy) {

        AutoscalePolicy autoscalePolicyBean = new AutoscalePolicy();
        if(autoscalePolicy == null) {
            return autoscalePolicyBean;
        }

        autoscalePolicyBean.setId(autoscalePolicy.getId());
        autoscalePolicyBean.setDisplayName(autoscalePolicy.getDisplayName());
        autoscalePolicyBean.setDescription(autoscalePolicy.getDescription());
        if(autoscalePolicy.getLoadThresholds() != null) {
            autoscalePolicyBean.setLoadThresholds(populateLoadThresholds(autoscalePolicy.getLoadThresholds()));
        }

        return autoscalePolicyBean;
    }

    private static LoadThresholds populateLoadThresholds (org.apache.stratos.autoscaler.policy.model.LoadThresholds
                                                                  loadThresholds) {

        LoadThresholds loadThresholdBean = new LoadThresholds();
        if(loadThresholds.getLoadAverage() != null) {
            LoadAverage loadAverage = new LoadAverage();
            loadAverage.average = loadThresholds.getLoadAverage().getAverage();
            loadAverage.gradient = loadThresholds.getLoadAverage().getGradient();
            loadAverage.scaleDownMarginOfGradient = loadThresholds.getLoadAverage().getScaleDownMarginOfGradient();
            loadAverage.scaleDownMarginOfSecondDerivative = loadThresholds.getLoadAverage().
                    getScaleDownMarginOfSecondDerivative();
            loadAverage.secondDerivative = loadThresholds.getLoadAverage().getSecondDerivative();
            loadThresholdBean.loadAverage = loadAverage;
        }
        if(loadThresholds.getMemoryConsumption() != null) {
            MemoryConsumption memoryConsumption = new MemoryConsumption();
            memoryConsumption.average = loadThresholds.getMemoryConsumption().getAverage();
            memoryConsumption.gradient = loadThresholds.getMemoryConsumption().getGradient();
            memoryConsumption.scaleDownMarginOfGradient = loadThresholds.getMemoryConsumption().
                    getScaleDownMarginOfGradient();
            memoryConsumption.scaleDownMarginOfSecondDerivative = loadThresholds.getMemoryConsumption().
                    getScaleDownMarginOfSecondDerivative();
            memoryConsumption.secondDerivative = loadThresholds.getMemoryConsumption().getSecondDerivative();
            loadThresholdBean.memoryConsumption = memoryConsumption;
        }
        if(loadThresholds.getRequestsInFlight() != null) {
            RequestsInFlight requestsInFlight = new RequestsInFlight();
            requestsInFlight.average = loadThresholds.getRequestsInFlight().getAverage();
            requestsInFlight.gradient = loadThresholds.getRequestsInFlight().getGradient();
            requestsInFlight.scaleDownMarginOfGradient = loadThresholds.getRequestsInFlight().
                    getScaleDownMarginOfGradient();
            requestsInFlight.scaleDownMarginOfSecondDerivative = loadThresholds.getRequestsInFlight().
                    getScaleDownMarginOfSecondDerivative();
            requestsInFlight.secondDerivative = loadThresholds.getRequestsInFlight().getSecondDerivative();
            loadThresholdBean.requestsInFlight = requestsInFlight;
        }

        return loadThresholdBean;
    }

    public static DeploymentPolicy[] populateDeploymentPolicyPojos(org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy[]
                                                                           deploymentPolicies) {
        DeploymentPolicy[] deploymentPolicyBeans;
        if(deploymentPolicies == null) {
            deploymentPolicyBeans = new DeploymentPolicy[0];
            return deploymentPolicyBeans;
        }

        deploymentPolicyBeans = new DeploymentPolicy[deploymentPolicies.length];
        for (int i = 0 ; i < deploymentPolicies.length ; i++) {
            //DeploymentPolicy deploymentPolicy = new DeploymentPolicy();
            //deploymentPolicy.id = deploymentPolicies[i].getId();

            //if(deploymentPolicies[i].getPartitionGroups() != null &&
            //        deploymentPolicies[i].getPartitionGroups().length > 0) {
            //    deploymentPolicy.partitionGroup = getPartitionGroups(deploymentPolicies[i].getPartitionGroups());
            //}

            deploymentPolicyBeans[i] = populateDeploymentPolicyPojo(deploymentPolicies[i]);
        }

        return deploymentPolicyBeans;
    }

    public static DeploymentPolicy populateDeploymentPolicyPojo (org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy
                                                                 deploymentPolicy) {

        DeploymentPolicy deploymentPolicyBean = new DeploymentPolicy();
        if(deploymentPolicy == null) {
            return deploymentPolicyBean;
        }

        deploymentPolicyBean.id = deploymentPolicy.getId();

        if (deploymentPolicy.getPartitionGroups() != null &&  deploymentPolicy.getPartitionGroups().length > 0) {
            deploymentPolicyBean.partitionGroup = Arrays.asList(populatePartitionGroupPojos(deploymentPolicy.getPartitionGroups()));
        }

        /*if (deploymentPolicy.getAllPartitions() != null && deploymentPolicy.getAllPartitions().length > 0) {
            deploymentPolicyBean.partition = Arrays.asList(populatePartitionPojos(deploymentPolicy.getAllPartitions()));
        }*/

        return deploymentPolicyBean;
    }

    public static PartitionGroup populatePartitionGroupPojo (org.apache.stratos.autoscaler.partition.PartitionGroup
                                                                         partitionGroup) {

        PartitionGroup partitionGroupBean = new PartitionGroup();
        if(partitionGroup == null){
            return partitionGroupBean;
        }

        partitionGroupBean.id = partitionGroup.getId();
        partitionGroupBean.partitionAlgo = partitionGroup.getPartitionAlgo();
        if(partitionGroup.getPartitions() != null && partitionGroup.getPartitions().length > 0) {
            partitionGroupBean.partition = getPartitionList(partitionGroup.getPartitions());
        }

        return partitionGroupBean;
    }

    public static PartitionGroup [] populatePartitionGroupPojos (org.apache.stratos.autoscaler.partition.PartitionGroup[] partitionGroups) {

        PartitionGroup[] partitionGroupsBeans;
        if(partitionGroups == null) {
            partitionGroupsBeans = new PartitionGroup[0];
            return partitionGroupsBeans;
        }

        partitionGroupsBeans = new PartitionGroup[partitionGroups.length];
        for (int i = 0 ; i < partitionGroups.length ; i ++) {
            /*PartitionGroup partitionGroup = new PartitionGroup();
            partitionGroup.id = partitionGroups[i].getId();
            partitionGroup.partitionAlgo = partitionGroups[i].getPartitionAlgo();

            if(partitionGroups[i].getPartitions() != null && partitionGroups[i].getPartitions().length > 0){
                partitionGroup.partition = getPartitionList(partitionGroups[i].getPartitions());
            }*/
            partitionGroupsBeans[i] = populatePartitionGroupPojo(partitionGroups[i]);
        }

        return partitionGroupsBeans;
    }

    private static List<Partition> getPartitionList(org.apache.stratos.cloud.controller.stub.deployment.partition.Partition[]
                                                         partitions) {

        List<Partition> partitionList = new ArrayList<Partition>();
        for (int i = 0 ; i < partitions.length ; i++) {
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

    public static ServiceDefinitionBean convertToServiceDefinitionBean (Service service) {

        ServiceDefinitionBean serviceDefinitionBean = new ServiceDefinitionBean();
        serviceDefinitionBean.setCartridgeType(service.getType());
        serviceDefinitionBean.setTenantRange(service.getTenantRange());
        serviceDefinitionBean.setClusterDomain(service.getClusterId());
        serviceDefinitionBean.setAutoscalingPolicyName(service.getAutoscalingPolicyName());
        serviceDefinitionBean.setDeploymentPolicyName(service.getDeploymentPolicyName());

        return serviceDefinitionBean;
    }

    public static List<ServiceDefinitionBean> convertToServiceDefinitionBeans (Collection<Service> services) {

        List<ServiceDefinitionBean> serviceDefinitionBeans = new ArrayList<ServiceDefinitionBean>();

        for (Service service : services) {
            serviceDefinitionBeans.add(convertToServiceDefinitionBean(service));
        }

        return serviceDefinitionBeans;
    }
}

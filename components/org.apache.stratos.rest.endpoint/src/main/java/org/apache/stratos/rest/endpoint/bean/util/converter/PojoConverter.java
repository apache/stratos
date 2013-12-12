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

import org.apache.stratos.cloud.controller.pojo.*;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.PartitionGroup;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.*;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.IaasProviderBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.LoadBalancerBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.PortMappingBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.PropertyBean;

import java.util.ArrayList;
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

    public static org.apache.stratos.cloud.controller.deployment.partition.Partition convertToCCPartitionPojo
            (Partition partitionBean) {

        org.apache.stratos.cloud.controller.deployment.partition.Partition partition = new
                org.apache.stratos.cloud.controller.deployment.partition.Partition();

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

        autoscalePolicy.setId(autoscalePolicyBean.id);
        autoscalePolicy.setDescription(autoscalePolicyBean.description);
        autoscalePolicy.setDisplayName(autoscalePolicyBean.displayName);

        if (autoscalePolicyBean.loadThresholds != null) {

            org.apache.stratos.autoscaler.policy.model.LoadThresholds loadThresholds = new
                    org.apache.stratos.autoscaler.policy.model.LoadThresholds();

            if(autoscalePolicyBean.loadThresholds.loadAverage != null) {

                //set load average information
                org.apache.stratos.autoscaler.policy.model.LoadAverage loadAverage = new
                        org.apache.stratos.autoscaler.policy.model.LoadAverage();
                loadAverage.setAverage(autoscalePolicyBean.loadThresholds.loadAverage.average);
                loadAverage.setGradient(autoscalePolicyBean.loadThresholds.loadAverage.gradient);
                loadAverage.setSecondDerivative(autoscalePolicyBean.loadThresholds.loadAverage.secondDerivative);
                loadAverage.setScaleDownMarginOfGradient(autoscalePolicyBean.loadThresholds.loadAverage.scaleDownMarginOfGradient);
                loadAverage.setScaleDownMarginOfSecondDerivative(autoscalePolicyBean.loadThresholds.loadAverage.scaleDownMarginOfSecondDerivative);
                //set load average
                loadThresholds.setLoadAverage(loadAverage);
            }
            if (autoscalePolicyBean.loadThresholds.requestsInFlight != null) {

                org.apache.stratos.autoscaler.policy.model.RequestsInFlight requestsInFlight = new
                        org.apache.stratos.autoscaler.policy.model.RequestsInFlight();
                //set request in flight information
                requestsInFlight.setAverage(autoscalePolicyBean.loadThresholds.requestsInFlight.average);
                requestsInFlight.setGradient(autoscalePolicyBean.loadThresholds.requestsInFlight.gradient);
                requestsInFlight.setSecondDerivative(autoscalePolicyBean.loadThresholds.requestsInFlight.secondDerivative);
                requestsInFlight.setScaleDownMarginOfGradient(autoscalePolicyBean.loadThresholds.requestsInFlight.scaleDownMarginOfGradient);
                requestsInFlight.setScaleDownMarginOfSecondDerivative(autoscalePolicyBean.loadThresholds.requestsInFlight.scaleDownMarginOfSecondDerivative);
                //set request in flight
                loadThresholds.setRequestsInFlight(requestsInFlight);
            }
            if (autoscalePolicyBean.loadThresholds.memoryConsumption != null) {

                org.apache.stratos.autoscaler.policy.model.MemoryConsumption memoryConsumption = new
                        org.apache.stratos.autoscaler.policy.model.MemoryConsumption();

                //set memory consumption information
                memoryConsumption.setAverage(autoscalePolicyBean.loadThresholds.memoryConsumption.average);
                memoryConsumption.setGradient(autoscalePolicyBean.loadThresholds.memoryConsumption.gradient);
                memoryConsumption.setSecondDerivative(autoscalePolicyBean.loadThresholds.memoryConsumption.secondDerivative);
                memoryConsumption.setScaleDownMarginOfGradient(autoscalePolicyBean.loadThresholds.memoryConsumption.scaleDownMarginOfGradient);
                memoryConsumption.setScaleDownMarginOfSecondDerivative(autoscalePolicyBean.loadThresholds.memoryConsumption.scaleDownMarginOfSecondDerivative);
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

    private static org.apache.stratos.cloud.controller.deployment.partition.Partition[] convertToCCPartitionPojos
            (List<Partition> partitionList) {

        org.apache.stratos.cloud.controller.deployment.partition.Partition[] partitions =
                new org.apache.stratos.cloud.controller.deployment.partition.Partition[partitionList.size()];
        for (int i = 0; i < partitionList.size() ; i++) {
            partitions[i] = convertToCCPartitionPojo(partitionList.get(i));
        }

        return partitions;
    }

    public static Partition[] populatePartitionPojos (org.apache.stratos.cloud.controller.deployment.partition.Partition[]
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

    public static Partition populatePartitionPojo (org.apache.stratos.cloud.controller.deployment.partition.Partition
                                                             partition) {

        Partition partitionBeans = new Partition();
        if(partition == null) {
            return partitionBeans;
        }

        partitionBeans.id = partition.getId();
        partitionBeans.provider = partition.getProvider();
        partitionBeans.partitionMin = partition.getPartitionMin();
        partitionBeans.partitionMax = partition.getPartitionMax();
        //properties are not added currently, TODO if required
        //if(partition[i].getProperties() != null) {
        //    List<PropertyBean> propertyBeans = getPropertyBeans(partition[i].getProperties());
        //    partition.property = propertyBeans;
        //}

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

        autoscalePolicyBean.id = autoscalePolicy.getId();
        autoscalePolicyBean.displayName = autoscalePolicy.getDisplayName();
        autoscalePolicyBean.description = autoscalePolicy.getDescription();
        if(autoscalePolicy.getLoadThresholds() != null) {
            autoscalePolicyBean.loadThresholds = populateLoadThresholds(autoscalePolicy.getLoadThresholds());
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
        //if(deploymentPolicy.getPartitionGroups() != null &&
        //        deploymentPolicy.getPartitionGroups().length > 0) {
        //    deploymentPolicy.partitionGroup = getPartitionGroups(deploymentPolicy.getPartitionGroups());
        //}

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

    private static List<Partition> getPartitionList(org.apache.stratos.cloud.controller.deployment.partition.Partition[]
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
}

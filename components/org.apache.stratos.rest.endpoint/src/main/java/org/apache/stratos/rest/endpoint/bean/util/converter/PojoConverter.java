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
            //cartridgeConfig.set
        }
        //Properties
        if(cartridgeDefinitionBean.property != null && !cartridgeDefinitionBean.property.isEmpty()) {
            cartridgeConfig.setProperties(getProperties(cartridgeDefinitionBean.property));
        }

        return cartridgeConfig;
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

    public static Partition[] populatePartitionPojo (org.apache.stratos.cloud.controller.deployment.partition.Partition[]
                                                              partitions) {

        Partition [] partitionBeans;
        if(partitions == null) {
            partitionBeans = new Partition[0];
            return partitionBeans;
        }

        partitionBeans = new Partition[partitions.length];
        for (int i = 0 ; i < partitions.length ; i++) {
            Partition partition = new Partition();
            partition.id = partitions[i].getId();
            partition.provider = partitions[i].getProvider();
            partition.partitionMin = partitions[i].getPartitionMin();
            partition.partitionMax = partitions[i].getPartitionMax();
            //properties are not added currently, TODO if required
            //if(partitions[i].getProperties() != null) {
            //    List<PropertyBean> propertyBeans = getPropertyBeans(partitions[i].getProperties());
            //    partition.property = propertyBeans;
            //}
            partitionBeans[i] = partition;
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

    public static AutoscalePolicy[] populateAutoscalePojo(org.apache.stratos.autoscaler.policy.model.AutoscalePolicy[]
                                                                   autoscalePolicies) {

        AutoscalePolicy [] autoscalePolicyBeans;
        if(autoscalePolicies == null) {
            autoscalePolicyBeans = new AutoscalePolicy[0];
            return autoscalePolicyBeans;
        }

        autoscalePolicyBeans = new AutoscalePolicy[autoscalePolicies.length];
        for (int i = 0 ; i < autoscalePolicies.length ; i++) {
            AutoscalePolicy autoscalePolicy = new AutoscalePolicy();
            autoscalePolicy.id = autoscalePolicies[i].getId();
            autoscalePolicy.displayName = autoscalePolicies[i].getDisplayName();
            autoscalePolicy.description = autoscalePolicies[i].getDescription();
            if(autoscalePolicies[i].getLoadThresholds() != null) {
                autoscalePolicy.loadThresholds = populateLoadThresholds(autoscalePolicies[i].getLoadThresholds());
            }
            autoscalePolicyBeans[i] = autoscalePolicy;
        }
        return autoscalePolicyBeans;
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

    public static DeploymentPolicy[] populateDeploymentPolicyPojo (org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy []
                                                                            deploymentPolicies) {
        DeploymentPolicy[] deploymentPolicyBeans;
        if(deploymentPolicies == null) {
            deploymentPolicyBeans = new DeploymentPolicy[0];
            return deploymentPolicyBeans;
        }

        deploymentPolicyBeans = new DeploymentPolicy[deploymentPolicies.length];
        for (int i = 0 ; i < deploymentPolicies.length ; i++) {
            DeploymentPolicy deploymentPolicy = new DeploymentPolicy();
            deploymentPolicy.id = deploymentPolicies[i].getId();

            //if(deploymentPolicies[i].getPartitionGroups() != null &&
            //        deploymentPolicies[i].getPartitionGroups().length > 0) {
            //    deploymentPolicy.partitionGroup = getPartitionGroups(deploymentPolicies[i].getPartitionGroups());
            //}

            deploymentPolicyBeans[i] = deploymentPolicy;
        }

        return deploymentPolicyBeans;
    }

    private static List<PartitionGroup> getPartitionGroups (org.apache.stratos.autoscaler.partition.xsd.PartitionGroup[] partitionGroups) {

        List<PartitionGroup> partitionGroupList = new ArrayList<PartitionGroup>();
        for (int i = 0 ; i < partitionGroups.length ; i ++) {
            PartitionGroup partitionGroup = new PartitionGroup();
            partitionGroup.id = partitionGroups[i].getId();
            partitionGroup.partitionAlgo = partitionGroups[i].getPartitionAlgo();

            if(partitionGroups[i].getPartitions() != null && partitionGroups[i].getPartitions().length > 0){
                partitionGroup.partition = getPartitionIdsList(partitionGroups[i].getPartitions());
            }

            partitionGroupList.add(partitionGroup);
        }

        return partitionGroupList;
    }

    private static List<String> getPartitionIdsList(org.apache.stratos.cloud.controller.deployment.partition.Partition[]
                                                            partitions) {

        ArrayList<String> partitionIdList = new ArrayList<String>();
        for (int i = 0 ; i < partitions.length ; i++) {
            partitionIdList.add(partitions[i].getId());
        }

        return partitionIdList;
    }
}

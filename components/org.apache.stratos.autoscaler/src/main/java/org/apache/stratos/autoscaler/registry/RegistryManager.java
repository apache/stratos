package org.apache.stratos.autoscaler.registry;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.pojo.ApplicationContext;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.pojo.ServiceGroup;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.NetworkPartition;
import org.apache.stratos.autoscaler.util.AutoscalerConstants;
import org.apache.stratos.autoscaler.util.Deserializer;
import org.apache.stratos.autoscaler.util.Serializer;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.cloud.controller.stub.domain.Partition;
import org.apache.stratos.messaging.domain.application.Application;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RegistryManager {

    private final static Log log = LogFactory.getLog(RegistryManager.class);
    private static Registry registryService;
    private static RegistryManager registryManager;

    private RegistryManager() {
        try {
            if (!registryService.resourceExists(AutoscalerConstants.AUTOSCALER_RESOURCE)) {
                registryService.put(AutoscalerConstants.AUTOSCALER_RESOURCE,
                        registryService.newCollection());
            }
        } catch (RegistryException e) {
            String msg =
                    "Failed to create the registry resource " +
                            AutoscalerConstants.AUTOSCALER_RESOURCE;
            log.error(msg, e);
            throw new AutoScalerException(msg, e);
        }
    }

    public static RegistryManager getInstance() {

        registryService = ServiceReferenceHolder.getInstance().getRegistry();

        synchronized (RegistryManager.class) {
            if (registryManager == null) {
                if (registryService == null) {
                    // log.warn("Registry Service is null. Hence unable to fetch data from registry.");
                    return registryManager;
                }
                registryManager = new RegistryManager();
            }
        }
        return registryManager;
    }


    /**
     * Persist an object in the local registry.
     *
     * @param dataObj      object to be persisted.
     * @param resourcePath resource path to be persisted.
     */
    private void persist(Object dataObj, String resourcePath) throws AutoScalerException {
        PrivilegedCarbonContext ctx = PrivilegedCarbonContext
                .getThreadLocalCarbonContext();
        ctx.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
        ctx.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

        try {
            registryService.beginTransaction();

            Resource nodeResource = registryService.newResource();
            nodeResource.setContent(Serializer.serializeToByteArray(dataObj));

            registryService.put(resourcePath, nodeResource);
            registryService.commitTransaction();
        } catch (Exception e) {
            try {
                registryService.rollbackTransaction();
            } catch (RegistryException e1) {
                if (log.isErrorEnabled()) {
                    log.error("Could not rollback transaction", e1);
                }
            }
            throw new AutoScalerException("Could not persist data in registry", e);
        }
    }

    public void persistAutoscalerPolicy(AutoscalePolicy autoscalePolicy) {
        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.AS_POLICY_RESOURCE + "/" + autoscalePolicy.getId();
        persist(autoscalePolicy, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Autoscaler policy written to registry: [id] %s [name] %s [description] %s",
                    autoscalePolicy.getId(), autoscalePolicy.getDisplayName(), autoscalePolicy.getDescription()));
        }
    }

    public void persistDeploymentPolicy(DeploymentPolicy deploymentPolicy) {
        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.DEPLOYMENT_POLICY_RESOURCE + "/" + deploymentPolicy.getApplicationId();
        persist(deploymentPolicy, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Deployment policy written to registry: %s", deploymentPolicy.toString()));
        }
    }

    /**
     * Persist network partition in the registry
     * @param networkPartition
     */
    public void persistNetworkPartition(NetworkPartition networkPartition) {
        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.NETWORK_PARTITIONS_RESOURCE + "/" + networkPartition.getId();
        persist(networkPartition, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Network partition written to registry: %s", networkPartition.toString()));
        }
    }

    /**
     * Get network partition resource paths
     * @return
     */
    private String[] getNetworkPartitionResourcePaths() {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);

            Object obj = retrieve(AutoscalerConstants.AUTOSCALER_RESOURCE +
                    AutoscalerConstants.NETWORK_PARTITIONS_RESOURCE);

            if (obj != null) {
                if (obj instanceof String[]) {
                    return (String[]) obj;
                } else {
                    log.warn("Expected object type not found for network partitions in registry");
                    return null;
                }
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        return null;
    }

    /**
     * Get network partitions
     * @return
     */
    public List<NetworkPartition> getNetworkPartitions() {
        List<NetworkPartition> networkPartitions = new ArrayList<NetworkPartition>();
        String[] networkPartitionResourcePaths = getNetworkPartitionResourcePaths();
        if(networkPartitionResourcePaths != null) {
            for (String resourcePath : networkPartitionResourcePaths) {
                if(StringUtils.isNotBlank(resourcePath)) {
                    NetworkPartition networkPartition = getNetworkPartitionByResourcePath(resourcePath);
                    networkPartitions.add(networkPartition);
                }
            }
        }
        return networkPartitions;
    }

    /**
     * Get network partition by network partition id
     * @param networkPartitionId
     * @return
     */
    public NetworkPartition getNetworkPartition(String networkPartitionId) {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);

            String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE +
                    AutoscalerConstants.NETWORK_PARTITIONS_RESOURCE + "/" + networkPartitionId;
            return getNetworkPartitionByResourcePath(resourcePath);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * Get network partition by resource path
     * @param resourcePath
     * @return
     */
    private NetworkPartition getNetworkPartitionByResourcePath(String resourcePath) {
        Object obj = retrieve(resourcePath);
        if (obj != null) {
            try {
                Object dataObj = Deserializer.deserializeFromByteArray((byte[]) obj);
                if (dataObj instanceof NetworkPartition) {
                    return (NetworkPartition) dataObj;
                } else {
                    return null;
                }
            } catch (Exception e) {
                String msg = "Could not read network partition from registry: [resource-path] " + resourcePath;
                log.warn(msg, e);
            }
        }
        return null;
    }

    /**
     * Remove network partition from the registry
     * @param networkPartitionId
     */
    public void removeNetworkPartition(String networkPartitionId) {
        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE +
                AutoscalerConstants.NETWORK_PARTITIONS_RESOURCE + "/" + networkPartitionId;
        this.delete(resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Network partition deleted from registry: [id] %s", networkPartitionId));
        }
    }

    /**
     * Update network partition in the registry
     * @param networkPartition
     */
    public void updateNetworkPartition(NetworkPartition networkPartition) {
        try {
            if (getNetworkPartition(networkPartition.getId()) == null) {
                throw new AutoScalerException(String.format("Network partition does not exist: " +
                        "[network-partition-id] %s", networkPartition.getId()));
            }

            persistNetworkPartition(networkPartition);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition updated successfully: %s", networkPartition.toString()));
            }
        } catch (Exception e) {
            log.error((String.format("Unable to update network partition [network-partition-id] %s",
                    networkPartition.getId())), e);
        }
    }

    public void persistApplication(Application application) {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);

            String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.APPLICATIONS_RESOURCE +
                    "/" + application.getUniqueIdentifier();
            persist(application, resourcePath);
            if (log.isDebugEnabled()) {
                log.debug("Application [ " + application.getUniqueIdentifier() +
                        " ] persisted successfully in the Autoscaler Registry");
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    public String[] getApplicationResourcePaths() {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);

            Object obj = retrieve(AutoscalerConstants.AUTOSCALER_RESOURCE +
                    AutoscalerConstants.APPLICATIONS_RESOURCE);

            if (obj != null) {
                if (obj instanceof String[]) {
                    return (String[]) obj;
                } else {
                    log.warn("Expected object type not found for Applications in Registry");
                    return null;
                }
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        return null;
    }

    public Application getApplication(String applicationId) {
        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.APPLICATIONS_RESOURCE +
                "/" + applicationId;
        return getApplicationByResourcePath(resourcePath);
    }

    public Application getApplicationByResourcePath(String applicationResourcePath) {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);

            Object obj = retrieve(applicationResourcePath);
            if (obj != null) {
                try {
                    Object dataObj = Deserializer.deserializeFromByteArray((byte[]) obj);
                    if (dataObj instanceof Application) {
                        return (Application) dataObj;
                    } else {
                        return null;
                    }
                } catch (Exception e) {
                    String msg = "Unable to retrieve data from Registry. Hence, any historical data will not get reflected.";
                    log.warn(msg, e);
                }
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        return null;
    }

    public void removeApplication(String applicationId) {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);

            delete(AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.APPLICATIONS_RESOURCE +
                    "/" + applicationId);

        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    public void persistApplicationContext(ApplicationContext applicationContext) {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);

            String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE +
                    AutoscalerConstants.APPLICATION_CONTEXTS_RESOURCE + "/" + applicationContext.getApplicationId();
            persist(applicationContext, resourcePath);
            if (log.isDebugEnabled()) {
                log.debug("Application context [" + applicationContext.getApplicationId() + "] " +
                        "persisted successfully in the Autoscaler Registry");
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    public String[] getApplicationContextResourcePaths() {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);

            Object obj = retrieve(AutoscalerConstants.AUTOSCALER_RESOURCE +
                    AutoscalerConstants.APPLICATION_CONTEXTS_RESOURCE);

            if (obj != null) {
                if (obj instanceof String[]) {
                    return (String[]) obj;
                } else {
                    log.warn("Expected object type not found for Applications in Registry");
                    return null;
                }
            }
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        return null;
    }

    public ApplicationContext getApplicationContext(String applicationId) {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);

            String applicationResourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE +
                    AutoscalerConstants.APPLICATION_CONTEXTS_RESOURCE + "/" + applicationId;
            return getApplicationContextByResourcePath(applicationResourcePath);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    public ApplicationContext getApplicationContextByResourcePath(String resourcePath) {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);

            Object obj = retrieve(resourcePath);
            if (obj != null) {
                try {
                    return (ApplicationContext) Deserializer.deserializeFromByteArray((byte[]) obj);
                } catch (Exception e) {
                    log.error("Could not deserialize application context", e);
                }
            }
            return null;
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    public void removeApplicationContext(String applicationId) {
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);

            delete(AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.APPLICATION_CONTEXTS_RESOURCE +
                    "/" + applicationId);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    public void persistServiceGroup(ServiceGroup servicegroup) {
        if (servicegroup == null || StringUtils.isEmpty(servicegroup.getName())) {
            throw new IllegalArgumentException("Cartridge group or group name can not be null");
        }
        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.SERVICE_GROUP + "/" + servicegroup.getName();
        persist(servicegroup, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Persisted cartridge group %s at path %s", servicegroup.getName(), resourcePath));
        }
    }

    public boolean serviceGroupExist(String serviceGroupName) {
        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.SERVICE_GROUP + "/" + serviceGroupName;
        return this.resourceExist(resourcePath);
    }

    private Object retrieve(String resourcePath) {
        try {
            Resource resource = registryService.get(resourcePath);
            return resource.getContent();
        } catch (ResourceNotFoundException ignore) {
            // this means, we've never persisted info in registry
            return null;
        } catch (RegistryException e) {
            String msg = "Failed to retrieve data from registry.";
            log.error(msg, e);
            throw new AutoScalerException(msg, e);
        }
    }

    private boolean resourceExist(String resourcePath) {
        return this.retrieve(resourcePath) != null;
    }

    public List<Partition> retrievePartitions() {
        List<Partition> partitionList = new ArrayList<Partition>();
        RegistryManager registryManager = RegistryManager.getInstance();
        String[] partitionsResourceList = (String[]) registryManager.retrieve(AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.PARTITION_RESOURCE);

        if (partitionsResourceList != null) {
            Partition partition;
            for (String resourcePath : partitionsResourceList) {
                Object serializedObj = registryManager.retrieve(resourcePath);
                if (serializedObj != null) {
                    try {

                        Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                        if (dataObj instanceof Partition) {
                            partition = (Partition) dataObj;
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Partition read from registry: [id] %s [provider] %s [min] %d [max] %d",
                                        partition.getId(), partition.getProvider(), partition.getPartitionMin(), partition.getPartitionMax()));
                            }
                            partitionList.add(partition);
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        String msg = "Unable to retrieve data from Registry. Hence, any historical partitions will not get reflected.";
                        log.warn(msg, e);
                    }
                }
            }
        }
        return partitionList;
    }

    public List<AutoscalePolicy> retrieveASPolicies() {
        List<AutoscalePolicy> asPolicyList = new ArrayList<AutoscalePolicy>();
        RegistryManager registryManager = RegistryManager.getInstance();
        String[] partitionsResourceList = (String[]) registryManager.retrieve(AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.AS_POLICY_RESOURCE);

        if (partitionsResourceList != null) {
            AutoscalePolicy asPolicy;
            for (String resourcePath : partitionsResourceList) {
                Object serializedObj = registryManager.retrieve(resourcePath);
                if (serializedObj != null) {
                    try {
                        Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                        if (dataObj instanceof AutoscalePolicy) {
                            asPolicy = (AutoscalePolicy) dataObj;
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Autoscaler policy read from registry: [id] %s [name] %s [description] %s",
                                        asPolicy.getId(), asPolicy.getDisplayName(), asPolicy.getDescription()));
                            }
                            asPolicyList.add(asPolicy);
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        String msg = "Unable to retrieve data from Registry. Hence, any historical autoscaler policies will not get reflected.";
                        log.warn(msg, e);
                    }
                }
            }
        }
        return asPolicyList;
    }

    public List<DeploymentPolicy> retrieveDeploymentPolicies() {
        List<DeploymentPolicy> depPolicyList = new ArrayList<DeploymentPolicy>();
        RegistryManager registryManager = RegistryManager.getInstance();
        String[] depPolicyResourceList = (String[]) registryManager.retrieve(AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.DEPLOYMENT_POLICY_RESOURCE);

        if (depPolicyResourceList != null) {
            DeploymentPolicy depPolicy;
            for (String resourcePath : depPolicyResourceList) {
                Object serializedObj = registryManager.retrieve(resourcePath);
                if (serializedObj != null) {
                    try {
                        Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                        if (dataObj instanceof DeploymentPolicy) {
                            depPolicy = (DeploymentPolicy) dataObj;
                            if (log.isDebugEnabled()) {
                                log.debug(depPolicy.toString());
                            }
                            depPolicyList.add(depPolicy);
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        String msg = "Unable to retrieve data from Registry. Hence, any historical deployment policies will not get reflected.";
                        log.warn(msg, e);
                    }
                }
            }
        }
        return depPolicyList;
    }

    public ServiceGroup getServiceGroup(String name) throws Exception {
        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.SERVICE_GROUP + "/" + name;
        Object serializedObj = registryManager.retrieve(resourcePath);
        ServiceGroup group = null;
        if (serializedObj != null) {

            Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
            if (dataObj instanceof ServiceGroup) {
                group = (ServiceGroup) dataObj;
                if (log.isDebugEnabled()) {
                    log.debug(group.toString());
                }
            } else {
                return null;
            }
        }

        return group;
    }

    public ServiceGroup[] getServiceGroups() {
        Object serializedObj;
        List<ServiceGroup> serviceGroupList = new ArrayList<ServiceGroup>();
        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.SERVICE_GROUP;
        if (registryManager.resourceExist(resourcePath)) {
            serializedObj = registryManager.retrieve(resourcePath);
        } else {
            return null;
        }

        String[] groupPathList = (String[]) serializedObj;

        if (groupPathList != null) {
            ServiceGroup serviceGroup;
            for (String groupPath : groupPathList) {
                serializedObj = registryManager.retrieve(groupPath);
                if (serializedObj != null) {
                    Object dataObj = null;
                    try {
                        dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                        if (dataObj instanceof ServiceGroup) {
                            serviceGroup = (ServiceGroup) dataObj;
                            serviceGroupList.add(serviceGroup);
                        }
                    } catch (IOException e) {
                        throw new AutoScalerException("Error occurred while retrieving cartridge group from Registry");
                    } catch (ClassNotFoundException e) {
                        throw new AutoScalerException("Error occurred while retrieving cartridge group from Registry");
                    }

                }
            }
        }

        ServiceGroup[] groupArr = new ServiceGroup[serviceGroupList.size()];
        groupArr = serviceGroupList.toArray(groupArr);
        return groupArr;
    }

    public void removeServiceGroup(String name) throws RegistryException {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Name of the cartridge group can not be empty");
        }

        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE +
                AutoscalerConstants.SERVICE_GROUP + "/" + name;
        if (registryService.resourceExists(resourcePath)) {
            registryService.delete(resourcePath);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Cartridge group %s is removed from registry", name));
            }
        } else {
            throw new AutoScalerException("No cartridge group is found with name" + name);
        }
    }

    public void removeAutoscalerPolicy(String policyID) {
        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.AS_POLICY_RESOURCE + "/" +
                              policyID;
        this.delete(resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Autoscaler policy deleted from registry: [id]",policyID));
        }

    }

    public void removeDeploymentPolicy(DeploymentPolicy depPolicy) {
        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.DEPLOYMENT_POLICY_RESOURCE;
        this.delete(resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Deployment policy deleted from registry: [id] %s",
                    depPolicy.getApplicationId()));
        }
    }

    private void delete(String resourcePath) {
        try {
            registryService.beginTransaction();
            registryService.delete(resourcePath);
            registryService.commitTransaction();
        } catch (RegistryException e) {
            try {
                registryService.rollbackTransaction();
            } catch (RegistryException e1) {
                if (log.isErrorEnabled()) {
                    log.error("Could not rollback transaction", e);
                }
            }
            log.error("Could not delete resource at " + resourcePath);
            throw new AutoScalerException("Could not delete data in registry at " + resourcePath, e);
        }

    }
}

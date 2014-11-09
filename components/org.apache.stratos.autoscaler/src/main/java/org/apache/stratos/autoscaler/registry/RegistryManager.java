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
import org.apache.stratos.autoscaler.NetworkPartitionLbHolder;
import org.apache.stratos.autoscaler.policy.model.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.pojo.ServiceGroup;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.autoscaler.util.Deserializer;
import org.apache.stratos.autoscaler.util.Serializer;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.common.kubernetes.KubernetesGroup;
import org.apache.stratos.messaging.domain.applications.Application;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.List;

public class RegistryManager {

    private final static Log log = LogFactory.getLog(RegistryManager.class);
    private static Registry registryService;
    private static RegistryManager registryManager;

    private RegistryManager() {
        try {
            if (!registryService.resourceExists(AutoScalerConstants.AUTOSCALER_RESOURCE)) {
                registryService.put(AutoScalerConstants.AUTOSCALER_RESOURCE,
                        registryService.newCollection());
            }
        } catch (RegistryException e) {
            String msg =
                    "Failed to create the registry resource " +
                            AutoScalerConstants.AUTOSCALER_RESOURCE;
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
                    log.error("Could not rollback transaction", e);
                }
            }
            throw new AutoScalerException("Could not persist data in registry", e);
        }
    }

    public void persistPartition(Partition partition) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.PARTITION_RESOURCE + "/" + partition.getId();
        persist(partition, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Partition written to registry: [id] %s [provider] %s [min] %d [max] %d",
                    partition.getId(), partition.getProvider(), partition.getPartitionMin(), partition.getPartitionMax()));
        }
    }

    public void persistNetworkPartitionIbHolder(NetworkPartitionLbHolder nwPartitionLbHolder) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants
                .NETWORK_PARTITION_LB_HOLDER_RESOURCE + "/" + nwPartitionLbHolder.getNetworkPartitionId();
        persist(nwPartitionLbHolder, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug("NetworkPartitionContext written to registry: " + nwPartitionLbHolder.toString());
        }
    }

    public void persistAutoscalerPolicy(AutoscalePolicy autoscalePolicy) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.AS_POLICY_RESOURCE + "/" + autoscalePolicy.getId();
        persist(autoscalePolicy, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Autoscaler policy written to registry: [id] %s [name] %s [description] %s",
                    autoscalePolicy.getId(), autoscalePolicy.getDisplayName(), autoscalePolicy.getDescription()));
        }
    }

    public void persistDeploymentPolicy(DeploymentPolicy deploymentPolicy) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.DEPLOYMENT_POLICY_RESOURCE + "/" + deploymentPolicy.getId();
        persist(deploymentPolicy, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(deploymentPolicy.toString());
        }
    }


    public void persistApplication(Application application) {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);

            String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.APPLICATIONS_RESOURCE +
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

            Object obj = retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE +
                    AutoScalerConstants.APPLICATIONS_RESOURCE);

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

    public Application getApplication(String applicationResourcePath) {

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);

            Object obj = retrieve(applicationResourcePath);
            if (obj != null) {
                try {
                    Object dataObj = Deserializer
                            .deserializeFromByteArray((byte[]) obj);
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
            /*if (obj != null) {
                if (obj instanceof Application) {
                    return (Application) obj;
                } else {
                    log.warn("Expected object type not found for Application " + applicationResourcePath + " in Registry");
                    return null;
                }
            }*/

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

            delete(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.APPLICATIONS_RESOURCE +
                    "/" + applicationId);

        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

    }

    public void persistServiceGroup(ServiceGroup servicegroup) {
        if (servicegroup == null || StringUtils.isEmpty(servicegroup.getName())) {
            throw new IllegalArgumentException("Service group or group name can not be null");
        }
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.SERVICE_GROUP + "/" + servicegroup.getName();
        persist(servicegroup, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Persisted service group %s at path %s", servicegroup.getName(), resourcePath));
        }
    }

    public boolean serviceGroupExist(String serviceGroupName) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.SERVICE_GROUP + "/" + serviceGroupName;
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
        String[] partitionsResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.PARTITION_RESOURCE);

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

    public List<NetworkPartitionLbHolder> retrieveNetworkPartitionLbHolders() {
        List<NetworkPartitionLbHolder> nwPartitionLbHolderList = new ArrayList<NetworkPartitionLbHolder>();
        RegistryManager registryManager = RegistryManager.getInstance();
        String[] partitionsResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE +
                AutoScalerConstants.NETWORK_PARTITION_LB_HOLDER_RESOURCE);

        if (partitionsResourceList != null) {
            NetworkPartitionLbHolder nwPartitionLbHolder;
            for (String resourcePath : partitionsResourceList) {
                Object serializedObj = registryManager.retrieve(resourcePath);
                if (serializedObj != null) {
                    try {

                        Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                        if (dataObj instanceof NetworkPartitionLbHolder) {
                            nwPartitionLbHolder = (NetworkPartitionLbHolder) dataObj;
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("NetworkPartitionLbHolder read from registry: " + nwPartitionLbHolder.toString()));
                            }
                            nwPartitionLbHolderList.add(nwPartitionLbHolder);
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        String msg = "Unable to retrieve data from Registry. Hence, any historical NetworkPartitionLbHolder will not get reflected.";
                        log.warn(msg, e);
                    }
                }
            }
        }
        return nwPartitionLbHolderList;
    }

    public List<AutoscalePolicy> retrieveASPolicies() {
        List<AutoscalePolicy> asPolicyList = new ArrayList<AutoscalePolicy>();
        RegistryManager registryManager = RegistryManager.getInstance();
        String[] partitionsResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.AS_POLICY_RESOURCE);

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
        String[] depPolicyResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.DEPLOYMENT_POLICY_RESOURCE);

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
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.SERVICE_GROUP + "/" + name;
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

    public ServiceGroup removeServiceGroup(String name) throws Exception {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE +
                AutoScalerConstants.SERVICE_GROUP + "/" + name;
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


    public void removeAutoscalerPolicy(AutoscalePolicy autoscalePolicy) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.AS_POLICY_RESOURCE + "/" + autoscalePolicy.getId();
        this.delete(resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Autoscaler policy deleted from registry: [id] %s [name] %s [description] %s",
                    autoscalePolicy.getId(), autoscalePolicy.getDisplayName(), autoscalePolicy.getDescription()));
        }

    }


    public void removeNetworkPartition(String networkPartition) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.NETWORK_PARTITION_LB_HOLDER_RESOURCE;
        this.delete(resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Network partition deleted from registry: [id] %s",
                    networkPartition));
        }
    }


    public void removeDeploymentPolicy(DeploymentPolicy depPolicy) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.DEPLOYMENT_POLICY_RESOURCE;
        this.delete(resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Deployment policy deleted from registry: [id] %s",
                    depPolicy.getId()));
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

    public void persistKubernetesGroup(KubernetesGroup kubernetesGroup) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.KUBERNETES_RESOURCE
                + "/" + kubernetesGroup.getGroupId();
        persist(kubernetesGroup, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("KubernetesGroup written to registry: [id] %s ", kubernetesGroup.getGroupId()));
        }
    }

    public List<KubernetesGroup> retrieveKubernetesGroups() {
        List<KubernetesGroup> kubernetesGroupList = new ArrayList<KubernetesGroup>();
        RegistryManager registryManager = RegistryManager.getInstance();
        String[] kubernetesGroupResourceList = (String[]) registryManager.retrieve(AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.KUBERNETES_RESOURCE);

        if (kubernetesGroupResourceList != null) {
            KubernetesGroup kubernetesGroup;
            for (String resourcePath : kubernetesGroupResourceList) {
                Object serializedObj = registryManager.retrieve(resourcePath);
                if (serializedObj != null) {
                    try {
                        Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                        if (dataObj instanceof KubernetesGroup) {
                            kubernetesGroup = (KubernetesGroup) dataObj;
                            if (log.isDebugEnabled()) {
                                log.debug(kubernetesGroup.toString());
                            }
                            kubernetesGroupList.add(kubernetesGroup);
                        } else {
                            return null;
                        }
                    } catch (Exception e) {
                        String msg = "Unable to retrieve data from Registry. Hence, any historical Kubernetes groups deployments will not get reflected.";
                        log.warn(msg, e);
                    }
                }
            }
        }
        return kubernetesGroupList;
    }

    public void removeKubernetesGroup(KubernetesGroup kubernetesGroup) {
        String resourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.KUBERNETES_RESOURCE + "/" + kubernetesGroup.getGroupId();
        this.delete(resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Kubernetes group deleted from registry: [id] %s", kubernetesGroup.getGroupId()));
        }
    }

}

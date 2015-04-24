/*
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
 */

package org.apache.stratos.autoscaler.registry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.algorithms.networkpartition.NetworkPartitionAlgorithmContext;
import org.apache.stratos.autoscaler.applications.pojo.ApplicationContext;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.pojo.ServiceGroup;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ApplicationPolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy;
import org.apache.stratos.autoscaler.util.AutoscalerConstants;
import org.apache.stratos.autoscaler.util.Deserializer;
import org.apache.stratos.autoscaler.util.Serializer;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
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

/**
 * Autoscaler registry manager.
 */
public class RegistryManager {

    private final static Log log = LogFactory.getLog(RegistryManager.class);

    private static Registry registryService;
    private static volatile RegistryManager instance;

    private RegistryManager() {
        try {
            registryService = ServiceReferenceHolder.getInstance().getRegistry();
            try {
                startTenantFlow();
                if (!registryService.resourceExists(AutoscalerConstants.AUTOSCALER_RESOURCE)) {
                    registryService.put(AutoscalerConstants.AUTOSCALER_RESOURCE, registryService.newCollection());
                }
            } finally {
                endTenantFlow();
            }
        } catch (RegistryException e) {
            String msg = "Failed to create the registry resource " + AutoscalerConstants.AUTOSCALER_RESOURCE;
            log.error(msg, e);
            throw new AutoScalerException(msg, e);
        }
    }

    public static RegistryManager getInstance() {
        if (instance == null) {
            synchronized (RegistryManager.class) {
                if (instance == null) {
                    instance = new RegistryManager();
                }
            }
        }
        return instance;
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
            String message = "Could not delete resource at " + resourcePath;
            log.error(message);
            throw new AutoScalerException(message, e);
        }
    }

    private void startTenantFlow() {
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        carbonContext.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
    }

    private void endTenantFlow() {
        PrivilegedCarbonContext.endTenantFlow();
    }

    private boolean resourceExist(String resourcePath) {
        return retrieve(resourcePath) != null;
    }

    /**
     * Persist an object in the local registry.
     *
     * @param dataObj      object to be persisted.
     * @param resourcePath resource path to be persisted.
     */
    private void persist(Object dataObj, String resourcePath) throws AutoScalerException {

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

    public void persistDeploymentPolicy(DeploymentPolicy deploymentPolicy) {
        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.DEPLOYMENT_POLICY_RESOURCE + "/" + deploymentPolicy.getDeploymentPolicyID();
        persist(deploymentPolicy, resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Deployment policy written to registry: %s", deploymentPolicy.toString()));
        }
    }

    public void persistAutoscalerPolicy(AutoscalePolicy autoscalePolicy) {
        try {
            startTenantFlow();
            String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE +
                    AutoscalerConstants.AS_POLICY_RESOURCE + "/" + autoscalePolicy.getId();
            persist(autoscalePolicy, resourcePath);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Autoscaler policy written to registry: [id] %s [name] %s [description] %s",
                        autoscalePolicy.getId(), autoscalePolicy.getDisplayName(), autoscalePolicy.getDescription()));
            }
        } catch (AutoScalerException e) {
            String message = "Unable to persist autoscaler policy [autoscaler-policy-id] " + autoscalePolicy.getId();
            log.error(message, e);
            throw e;
        } finally {
            endTenantFlow();
        }
    }

    public void persistApplication(Application application) {
        try {
            startTenantFlow();
            String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.APPLICATIONS_RESOURCE +
                    "/" + application.getUniqueIdentifier();
            persist(application, resourcePath);
            if (log.isDebugEnabled()) {
                log.debug("Application [ " + application.getUniqueIdentifier() +
                        " ] persisted successfully in the Autoscaler Registry");
            }
        } finally {
            endTenantFlow();
        }
    }

    public String[] getApplicationResourcePaths() {
        try {
            startTenantFlow();
            Object obj = retrieve(AutoscalerConstants.AUTOSCALER_RESOURCE +
                    AutoscalerConstants.APPLICATIONS_RESOURCE);

            if (obj != null) {
                if (obj instanceof String[]) {
                    return (String[]) obj;
                } else {
                    log.warn("Expected object type not found for applications in registry");
                }
            }
            return null;
        } finally {
            endTenantFlow();
        }
    }

    public Application getApplication(String applicationId) {
        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.APPLICATIONS_RESOURCE +
                "/" + applicationId;
        return getApplicationByResourcePath(resourcePath);
    }

    public Application getApplicationByResourcePath(String applicationResourcePath) {
        try {
            startTenantFlow();
            Object obj = retrieve(applicationResourcePath);
            if (obj != null) {
                try {
                    Object dataObj = Deserializer.deserializeFromByteArray((byte[]) obj);
                    if (dataObj instanceof Application) {
                        return (Application) dataObj;
                    }
                } catch (Exception e) {
                    String msg = "Unable to retrieve resource from registry: [resource-path] "
                            + applicationResourcePath;
                    log.warn(msg, e);
                }
            }
            return null;
        } finally {
            endTenantFlow();
        }
    }

    public void removeApplication(String applicationId) {
        try {
            startTenantFlow();
            delete(AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.APPLICATIONS_RESOURCE +
                    "/" + applicationId);
        } finally {
            endTenantFlow();
        }
    }

    public void persistApplicationContext(ApplicationContext applicationContext) {
        try {
            startTenantFlow();
            String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE +
                    AutoscalerConstants.APPLICATION_CONTEXTS_RESOURCE + "/" + applicationContext.getApplicationId();
            persist(applicationContext, resourcePath);
            if (log.isDebugEnabled()) {
                log.debug("Application context [" + applicationContext.getApplicationId() + "] " +
                        "persisted successfully in the autoscaler registry");
            }
        } finally {
            endTenantFlow();
        }
    }

    public String[] getApplicationContextResourcePaths() {
        try {
            startTenantFlow();
            Object obj = retrieve(AutoscalerConstants.AUTOSCALER_RESOURCE +
                    AutoscalerConstants.APPLICATION_CONTEXTS_RESOURCE);

            if (obj != null) {
                if (obj instanceof String[]) {
                    return (String[]) obj;
                } else {
                    log.warn("Expected object type not found for applications in registry");
                    return null;
                }
            }
        } finally {
            endTenantFlow();
        }
        return null;
    }

    public ApplicationContext getApplicationContext(String applicationId) {
        try {
            startTenantFlow();
            String applicationResourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE +
                    AutoscalerConstants.APPLICATION_CONTEXTS_RESOURCE + "/" + applicationId;
            return getApplicationContextByResourcePath(applicationResourcePath);
        } finally {
            endTenantFlow();
        }
    }

    public ApplicationContext getApplicationContextByResourcePath(String resourcePath) {
        try {
            startTenantFlow();
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
            endTenantFlow();
        }
    }

    public void removeApplicationContext(String applicationId) {
        try {
            startTenantFlow();
            delete(AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.APPLICATION_CONTEXTS_RESOURCE +
                    "/" + applicationId);
        } finally {
            endTenantFlow();
        }
    }

    public void persistServiceGroup(ServiceGroup servicegroup) {
        try {
            startTenantFlow();
            if (servicegroup == null || StringUtils.isEmpty(servicegroup.getName())) {
                throw new IllegalArgumentException("Cartridge group or group name can not be null");
            }
            String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.SERVICE_GROUP + "/" + servicegroup.getName();
            persist(servicegroup, resourcePath);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Persisted cartridge group %s at path %s", servicegroup.getName(), resourcePath));
            }
        } finally {
            endTenantFlow();
        }
    }

    public boolean serviceGroupExist(String serviceGroupName) {
        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.SERVICE_GROUP + "/" + serviceGroupName;
        return resourceExist(resourcePath);
    }

    /**
     * Retrieve Autoscaling policies from registry
     *
     * @return all the Autoscaling policies
     */
    public List<AutoscalePolicy> retrieveASPolicies() {
        try {
            startTenantFlow();
            List<AutoscalePolicy> asPolicyList = new ArrayList<AutoscalePolicy>();
            String[] partitionsResourceList = (String[]) retrieve(AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.AS_POLICY_RESOURCE);

            if (partitionsResourceList != null) {
                AutoscalePolicy asPolicy;
                for (String resourcePath : partitionsResourceList) {
                    Object serializedObj = retrieve(resourcePath);
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
                            String msg = "Unable to retrieve resource from registry: [resource-path] "
                                    + resourcePath;
                            log.warn(msg, e);
                        }
                    }
                }
            }
            return asPolicyList;
        } finally {
            endTenantFlow();
        }
    }

    /**
     * Retrieve deployment policies from registry
     *
     * @return all the deployment policies
     */
    public List<DeploymentPolicy> retrieveDeploymentPolicies() {
        try {
            startTenantFlow();
            List<DeploymentPolicy> depPolicyList = new ArrayList<DeploymentPolicy>();
            RegistryManager registryManager = RegistryManager.getInstance();
            String[] depPolicyResourceList = (String[]) registryManager.retrieve(AutoscalerConstants.AUTOSCALER_RESOURCE
                    + AutoscalerConstants.DEPLOYMENT_POLICY_RESOURCE);

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
        } finally {
            endTenantFlow();
        }
    }

    public List<ApplicationPolicy> retrieveApplicationPolicies() {
        try {
            startTenantFlow();
            List<ApplicationPolicy> applicationPolicyList = new ArrayList<ApplicationPolicy>();
            String[] applicationPoliciesResourceList = (String[]) retrieve(
                    AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.APPLICATION_POLICY_RESOURCE);

            if (applicationPoliciesResourceList != null) {
                ApplicationPolicy applicationPolicy;
                for (String resourcePath : applicationPoliciesResourceList) {
                    Object serializedObj = instance.retrieve(resourcePath);
                    if (serializedObj != null) {
                        try {
                            Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                            if (dataObj instanceof ApplicationPolicy) {
                                applicationPolicy = (ApplicationPolicy) dataObj;
                                if (log.isDebugEnabled()) {
                                    log.debug(String.format("Application policy read from registry %s",
                                            applicationPolicy.toString()));
                                }
                                applicationPolicyList.add(applicationPolicy);
                            } else {
                                return null;
                            }
                        } catch (Exception e) {
                            String msg = "Unable to retrieve resource from registry: [resource-path] "
                                    + resourcePath;
                            log.warn(msg, e);
                        }
                    }
                }
            }
            return applicationPolicyList;
        } finally {
            endTenantFlow();
        }
    }

    public List<NetworkPartitionAlgorithmContext> retrieveNetworkPartitionAlgorithmContexts() {
        try {
            startTenantFlow();
            List<NetworkPartitionAlgorithmContext> algorithmContexts = new ArrayList<NetworkPartitionAlgorithmContext>();
            String[] networkPartitionAlgoCtxtResourceList = (String[]) retrieve(
                    AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.NETWORK_PARTITION_ALGO_CTX_RESOURCE);

            if (networkPartitionAlgoCtxtResourceList != null) {
                NetworkPartitionAlgorithmContext algorithmContext;
                for (String resourcePath : networkPartitionAlgoCtxtResourceList) {
                    Object serializedObj = retrieve(resourcePath);
                    if (serializedObj != null) {
                        try {
                            Object dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                            if (dataObj instanceof NetworkPartitionAlgorithmContext) {
                                algorithmContext = (NetworkPartitionAlgorithmContext) dataObj;
                                if (log.isDebugEnabled()) {
                                    log.debug(String.format("Network partition algorithm context read from registry %s", algorithmContext.toString()));
                                }
                                algorithmContexts.add(algorithmContext);
                            } else {
                                return null;
                            }
                        } catch (Exception e) {
                            String msg = "Unable to retrieve resource from registry: [resource-path] "
                                    + resourcePath;
                            log.warn(msg, e);
                        }
                    }
                }
            }
            return algorithmContexts;
        } finally {
            endTenantFlow();
        }
    }

    public ServiceGroup getServiceGroup(String name) throws Exception {
        try {
            startTenantFlow();
            String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.SERVICE_GROUP + "/" + name;
            Object serializedObj = instance.retrieve(resourcePath);
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
        } finally {
            endTenantFlow();
        }
    }

    public ServiceGroup[] getServiceGroups() {
        try {
            startTenantFlow();
            Object serializedObj;
            List<ServiceGroup> serviceGroupList = new ArrayList<ServiceGroup>();
            String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.SERVICE_GROUP;
            if (instance.resourceExist(resourcePath)) {
                serializedObj = instance.retrieve(resourcePath);
            } else {
                return null;
            }

            String[] groupPathList = (String[]) serializedObj;

            if (groupPathList != null) {
                ServiceGroup serviceGroup;
                for (String groupPath : groupPathList) {
                    serializedObj = instance.retrieve(groupPath);
                    if (serializedObj != null) {
                        Object dataObj = null;
                        try {
                            dataObj = Deserializer.deserializeFromByteArray((byte[]) serializedObj);
                            if (dataObj instanceof ServiceGroup) {
                                serviceGroup = (ServiceGroup) dataObj;
                                serviceGroupList.add(serviceGroup);
                            }
                        } catch (IOException e) {
                            throw new AutoScalerException("Error occurred while retrieving cartridge group from registry");
                        } catch (ClassNotFoundException e) {
                            throw new AutoScalerException("Error occurred while retrieving cartridge group from registry");
                        }
                    }
                }
            }

            ServiceGroup[] serviceGroups = new ServiceGroup[serviceGroupList.size()];
            serviceGroups = serviceGroupList.toArray(serviceGroups);
            return serviceGroups;
        } finally {
            endTenantFlow();
        }
    }

    public void removeServiceGroup(String name) throws RegistryException {
        try {
            startTenantFlow();
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
        } finally {
            endTenantFlow();
        }
    }

    public void removeAutoscalerPolicy(String policyID) {
        try {
            startTenantFlow();
            String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.AS_POLICY_RESOURCE + "/" +
                    policyID;
            delete(resourcePath);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Autoscaler policy deleted from registry: [id]", policyID));
            }
        } finally {
            endTenantFlow();
        }
    }

    public void removeDeploymentPolicy(String deploymentPolicyID) {
        String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.DEPLOYMENT_POLICY_RESOURCE + "/" +
                deploymentPolicyID;
        this.delete(resourcePath);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Deployment policy deleted from registry: [id] %s",
                    deploymentPolicyID));
        }
    }

    public void removeApplicationPolicy(String applicationPolicyId) {
        try {
            startTenantFlow();
            String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.APPLICATION_POLICY_RESOURCE + "/" +
                    applicationPolicyId;
            delete(resourcePath);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Application policy deleted from registry [application-policy-id] %s", applicationPolicyId));
            }
        } finally {
            endTenantFlow();
        }
    }

    public void removeNetworkPartitionAlgorithmContext(String applicationPolicyId) {
        try {
            startTenantFlow();
            String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE + AutoscalerConstants.NETWORK_PARTITION_ALGO_CTX_RESOURCE + "/" +
                    applicationPolicyId;
            delete(resourcePath);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition algorithm context deleted from registry [application-policy-id] %s", applicationPolicyId));
            }
        } finally {
            endTenantFlow();
        }
    }

    public void persistApplicationPolicy(ApplicationPolicy applicationPolicy) {
        try {
            startTenantFlow();
            String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE +
                    AutoscalerConstants.APPLICATION_POLICY_RESOURCE + "/" + applicationPolicy.getId();
            persist(applicationPolicy, resourcePath);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Application policy written to registry : %s", applicationPolicy.getId()));
            }
        } finally {
            endTenantFlow();
        }
    }

    public void persistNetworkPartitionAlgorithmContext(NetworkPartitionAlgorithmContext algorithmContext) {
        try {
            startTenantFlow();
            String applicationId = algorithmContext.getApplicationId();
            String resourcePath = AutoscalerConstants.AUTOSCALER_RESOURCE +
                    AutoscalerConstants.NETWORK_PARTITION_ALGO_CTX_RESOURCE + "/" + applicationId;
            persist(algorithmContext, resourcePath);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition algorithm context written to registry : %s", applicationId));
            }
        } finally {
            endTenantFlow();
        }
    }
}

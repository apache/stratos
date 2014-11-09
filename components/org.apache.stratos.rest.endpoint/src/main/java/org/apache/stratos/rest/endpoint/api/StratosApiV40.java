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
package org.apache.stratos.rest.endpoint.api;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.exception.StratosException;
import org.apache.stratos.common.util.ClaimsMgtUtil;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.manager.dto.Cartridge;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.rest.endpoint.ServiceHolder;
import org.apache.stratos.rest.endpoint.Utils;
import org.apache.stratos.rest.endpoint.annotation.AuthorizationAction;
import org.apache.stratos.rest.endpoint.annotation.SuperTenantService;
import org.apache.stratos.rest.endpoint.bean.CartridgeInfoBean;
import org.apache.stratos.rest.endpoint.bean.StratosApiResponse;
import org.apache.stratos.rest.endpoint.bean.SubscriptionDomainRequest;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.PartitionGroup;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.ServiceDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.repositoryNotificationInfoBean.Payload;
import org.apache.stratos.rest.endpoint.bean.subscription.domain.SubscriptionDomainBean;
import org.apache.stratos.rest.endpoint.bean.topology.Cluster;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;
import org.apache.stratos.tenant.mgt.core.TenantPersistor;
import org.apache.stratos.tenant.mgt.util.TenantMgtUtil;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stratos API v4.0 for Stratos 4.0.0 release.
 * Please do not update this API, if modifications are needed use the latest API version.
 */
@Deprecated
@Path("/")
public class StratosApiV40 extends AbstractApi {
    private static Log log = LogFactory.getLog(StratosApiV40.class);
    @Context
    HttpServletRequest httpServletRequest;

    @POST
    @Path("/init")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public StratosApiResponse initialize ()
            throws RestAPIException {

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully logged in");
        return stratosApiResponse;
    }
    /*
    This method gets called by the client who are interested in using session mechanism to authenticate themselves in
    subsequent calls. This method call get authenticated by the basic authenticator.
    Once the authenticated call received, the method creates a session.
     */
    @GET
    @Path("/cookie")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getCookie(){

        HttpSession httpSession = httpServletRequest.getSession(true);//create session if not found
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        httpSession.setAttribute("userName",carbonContext.getUsername());
        httpSession.setAttribute("tenantDomain",carbonContext.getTenantDomain());
        httpSession.setAttribute("tenantId",carbonContext.getTenantId());

        String sessionId = httpSession.getId();
        return Response.ok().header("WWW-Authenticate", "Basic").type(MediaType.APPLICATION_JSON).
                entity(Utils.buildAuthenticationSuccessMessage(sessionId)).build();
    }

    @POST
    @Path("/cartridge/definition/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse deployCartridgeDefinition(CartridgeDefinitionBean cartridgeDefinitionBean)
            throws RestAPIException {

        return StratosApiV40Utils.deployCartridge(cartridgeDefinitionBean, getConfigContext(), getUsername(),
                getTenantDomain());

    }

    @DELETE
    @Path("/cartridge/definition/{cartridgeType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse unDeployCartridgeDefinition (@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {

        return StratosApiV40Utils.undeployCartridge(cartridgeType);
    }

    @POST
    @Path("/policy/deployment/partition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse deployPartition (Partition partition)
            throws RestAPIException {

        return StratosApiV40Utils.deployPartition(partition);
    }

    @POST
    @Path("/policy/autoscale")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse deployAutoscalingPolicyDefintion (AutoscalePolicy autoscalePolicy)
            throws RestAPIException {

        return StratosApiV40Utils.deployAutoscalingPolicy(autoscalePolicy);
    }

    @POST
    @Path("/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse deployDeploymentPolicyDefinition (DeploymentPolicy deploymentPolicy)
            throws RestAPIException {

        return StratosApiV40Utils.deployDeploymentPolicy(deploymentPolicy);
    }

    @GET
    @Path("/partition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Partition[] getPartitions () throws RestAPIException {

        return StratosApiV40Utils.getAvailablePartitions();
    }

    @GET
    @Path("/partition/{partitionId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Partition getPartition (@PathParam("partitionId") String partitionId) throws RestAPIException {

        return StratosApiV40Utils.getPartition(partitionId);
    }

    @GET
    @Path("/partition/group/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public PartitionGroup[] getPartitionGroups (@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {

        return StratosApiV40Utils.getPartitionGroups(deploymentPolicyId);
    }

    @GET
    @Path("/partition/{deploymentPolicyId}/{partitionGroupId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Partition [] getPartitions (@PathParam("deploymentPolicyId") String deploymentPolicyId,
                                       @PathParam("partitionGroupId") String partitionGroupId) throws RestAPIException {

        return StratosApiV40Utils.getPartitionsOfGroup(deploymentPolicyId, partitionGroupId);
    }

    @GET
    @Path("/partition/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Partition [] getPartitionsOfPolicy (@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {

        return StratosApiV40Utils.getPartitionsOfDeploymentPolicy(deploymentPolicyId);
    }

    @GET
    @Path("/policy/autoscale")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public AutoscalePolicy[] getAutoscalePolicies () throws RestAPIException {

        return StratosApiV40Utils.getAutoScalePolicies();
    }

    @GET
    @Path("/policy/autoscale/{autoscalePolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public AutoscalePolicy getAutoscalePolicies (@PathParam("autoscalePolicyId") String autoscalePolicyId)
            throws RestAPIException {

        return StratosApiV40Utils.getAutoScalePolicy(autoscalePolicyId);
    }

    @GET
    @Path("/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public DeploymentPolicy[] getDeploymentPolicies () throws RestAPIException {

        return StratosApiV40Utils.getDeploymentPolicies();
    }

    @GET
    @Path("/policy/deployment/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public DeploymentPolicy getDeploymentPolicies (@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {

        return StratosApiV40Utils.getDeploymentPolicy(deploymentPolicyId);
    }

    @GET
    @Path("{cartridgeType}/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public DeploymentPolicy[] getValidDeploymentPolicies (@PathParam("cartridgeType") String cartridgeType)
            throws RestAPIException {

        return StratosApiV40Utils.getDeploymentPolicies(cartridgeType);
    }

    @GET
    @Path("/cartridge/tenanted/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getAvailableMultiTenantCartridges() throws RestAPIException {
        List<Cartridge> cartridges = StratosApiV40Utils.getAvailableCartridges(null, true, getConfigContext());
        return cartridges.isEmpty() ? new Cartridge[0] : cartridges.toArray(new Cartridge[cartridges.size()]);
    }

    @GET
    @Path("/cartridge/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getAvailableSingleTenantCartridges() throws RestAPIException {
        List<Cartridge> cartridges = StratosApiV40Utils.getAvailableCartridges(null, false, getConfigContext());
        return cartridges.isEmpty() ? new Cartridge[0] : cartridges.toArray(new Cartridge[cartridges.size()]);
    }

    @GET
    @Path("/cartridge/available/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getAvailableCartridges() throws RestAPIException {
        List<Cartridge> cartridges = StratosApiV40Utils.getAvailableCartridges(null, null, getConfigContext());
        return cartridges.isEmpty() ? new Cartridge[0] : cartridges.toArray(new Cartridge[cartridges.size()]);
    }

    @GET
    @Path("/cartridge/list/subscribed")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getSubscribedCartridges() throws RestAPIException {
        List<Cartridge> cartridgeList = StratosApiV40Utils.getSubscriptions(null, null, getConfigContext());
        // Following is very important when working with axis2
        return cartridgeList.isEmpty() ? new Cartridge[0] : cartridgeList.toArray(new Cartridge[cartridgeList.size()]);
    }

    @GET
    @Path("/cartridge/list/subscribed/group/{serviceGroup}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getSubscribedCartridgesForServiceGroup(@PathParam("serviceGroup") String serviceGroup) throws RestAPIException {
        List<Cartridge> cartridgeList = StratosApiV40Utils.getSubscriptions(null, serviceGroup, getConfigContext());
        // Following is very important when working with axis2
        return cartridgeList.isEmpty() ? new Cartridge[0] : cartridgeList.toArray(new Cartridge[cartridgeList.size()]);
    }

    @GET
    @Path("/cartridge/info/{subscriptionAlias}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge getCartridgeInfo(@PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {
        return StratosApiV40Utils.getSubscription(subscriptionAlias, getConfigContext());
    }

    @GET
    @Path("/cartridge/available/info/{cartridgeType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge getAvailableSingleTenantCartridgeInfo(@PathParam("cartridgeType") String cartridgeType)
            throws RestAPIException {
        return StratosApiV40Utils.getAvailableCartridgeInfo(cartridgeType, null, getConfigContext());
    }

    @GET
    @Path("/cartridge/lb")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public List<Cartridge> getAvailableLbCartridges()
            throws RestAPIException {
        return StratosApiV40Utils.getAvailableLbCartridges(false, getConfigContext());
    }

    @GET
    @Path("/cartridge/active/{cartridgeType}/{subscriptionAlias}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public int getActiveInstances(@PathParam("cartridgeType") String cartridgeType,
                                  @PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {
        return StratosApiV40Utils.getActiveInstances(cartridgeType, subscriptionAlias, getConfigContext());
    }

    @POST
    @Path("/cartridge/subscribe")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response subscribe(CartridgeInfoBean cartridgeInfoBean) throws RestAPIException {

        return Response.status(404).build();
    }

    @GET
    @Path("/cluster/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cluster[] getClustersForTenant() throws RestAPIException {

        return StratosApiV40Utils.getClustersForTenant(getConfigContext());
    }

    @GET
    @Path("/cluster/{cartridgeType}/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cluster[] getClusters(@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {
        return StratosApiV40Utils.getClustersForCartridgeType(cartridgeType);
    }

    @GET
    @Path("/cluster/service/{cartridgeType}/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cluster[] getServiceClusters(@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {

        return StratosApiV40Utils.getClustersForTenantAndCartridgeType(getConfigContext(), cartridgeType);
    }

    @GET
    @Path("/cluster/{cartridgeType}/{subscriptionAlias}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cluster getCluster(@PathParam("cartridgeType") String cartridgeType,
                              @PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException, RestAPIException {

        return StratosApiV40Utils.getCluster(cartridgeType, subscriptionAlias, getConfigContext());
    }

    @GET
    @Path("/cluster/clusterId/{clusterId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cluster getCluster(@PathParam("clusterId") String clusterId) throws RestAPIException {
        Cluster cluster = null;
        if(log.isDebugEnabled()) {
            log.debug("Finding cluster for [id]: "+clusterId);
        }
        Cluster[] clusters = StratosApiV40Utils.getClustersForTenant(getConfigContext());
        if(log.isDebugEnabled()) {
            log.debug("Clusters retrieved from backend for cluster [id]: "+clusterId);
            for (Cluster c : clusters) {
                log.debug(c+"\n");
            }
        }
        for (Cluster clusterObj : clusters) {
            if (clusterObj.clusterId.equals(clusterId)){
                cluster = clusterObj;
                break;
            }
        }
        return cluster;
    }

    @POST
    @Path("/cartridge/unsubscribe")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public StratosApiResponse unsubscribe(String alias) throws RestAPIException {

        return StratosApiV40Utils.unsubscribe(alias, getTenantDomain());

    }

    @POST
    @Path("/tenant")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse addTenant(TenantInfoBean tenantInfoBean) throws RestAPIException {
        try {
            CommonUtil.validateEmail(tenantInfoBean.getEmail());
        } catch (Exception e) {
            String msg = "Invalid email is provided.";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        String tenantDomain = tenantInfoBean.getTenantDomain();
        try {
            TenantMgtUtil.validateDomain(tenantDomain);
        } catch (Exception e) {
            String msg = "Tenant domain validation error for tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        UserRegistry userRegistry = (UserRegistry) PrivilegedCarbonContext.getThreadLocalCarbonContext().
                getRegistry(RegistryType.USER_GOVERNANCE);
        if (userRegistry == null) {
            log.error("Security Alert! User registry is null. A user is trying create a tenant "
                    + " without an authenticated session.");
            throw new RestAPIException("Invalid data."); // obscure error message.
        }

        if (userRegistry.getTenantId() != MultitenantConstants.SUPER_TENANT_ID) {
            log.error("Security Alert! Non super tenant trying to create a tenant.");
            throw new RestAPIException("Invalid data."); // obscure error message.
        }
        Tenant tenant = TenantMgtUtil.initializeTenant(tenantInfoBean);
        TenantPersistor persistor = ServiceHolder.getTenantPersistor();
        // not validating the domain ownership, since created by super tenant
        int tenantId = 0; //TODO verify whether this is the correct approach (isSkeleton)
        try {
            tenantId = persistor.persistTenant(tenant, false, tenantInfoBean.getSuccessKey(),
                    tenantInfoBean.getOriginatedService(),false);
        } catch (Exception e) {
            String msg = "Error in persisting tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        tenantInfoBean.setTenantId(tenantId);

        try {
            TenantMgtUtil.addClaimsToUserStoreManager(tenant);
        } catch (Exception e) {
            String msg = "Error in granting permissions for tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        //Notify tenant addition
        try {
            TenantMgtUtil.triggerAddTenant(tenantInfoBean);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant addition.";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        // For the super tenant tenant creation, tenants are always activated as they are created.
        try {
            TenantMgtUtil.activateTenantInitially(tenantInfoBean, tenantId);
        } catch (Exception e) {
            String msg = "Error in initial activation of tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        try {
            TenantMgtUtil.prepareStringToShowThemeMgtPage(tenant.getId());
        } catch (RegistryException e) {
            String msg = "Error in preparing theme mgt page for tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully added new tenant with domain " + tenantInfoBean.getTenantDomain());
        return stratosApiResponse;
    }

    @PUT
    @Path("/tenant")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse updateTenant(TenantInfoBean tenantInfoBean) throws RestAPIException {

        try {
            return updateExistingTenant(tenantInfoBean);
        } catch (Exception e) {
            String msg = "Error in updating tenant " + tenantInfoBean.getTenantDomain();
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }

    private StratosApiResponse updateExistingTenant(TenantInfoBean tenantInfoBean) throws Exception {

        TenantManager tenantManager = ServiceHolder.getTenantManager();
        UserStoreManager userStoreManager;

        // filling the non-set admin and admin password first
        UserRegistry configSystemRegistry = ServiceHolder.getRegistryService().getConfigSystemRegistry(
                tenantInfoBean.getTenantId());

        String tenantDomain = tenantInfoBean.getTenantDomain();

        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " + tenantDomain
                    + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        Tenant tenant;
        try {
            tenant = (Tenant) tenantManager.getTenant(tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " +
                    tenantDomain + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        // filling the first and last name values
        if (tenantInfoBean.getFirstname() != null &&
                !tenantInfoBean.getFirstname().trim().equals("")) {
            try {
                CommonUtil.validateName(tenantInfoBean.getFirstname(), "First Name");
            } catch (Exception e) {
                String msg = "Invalid first name is provided.";
                log.error(msg, e);
                throw new Exception(msg, e);
            }
        }
        if (tenantInfoBean.getLastname() != null &&
                !tenantInfoBean.getLastname().trim().equals("")) {
            try {
                CommonUtil.validateName(tenantInfoBean.getLastname(), "Last Name");
            } catch (Exception e) {
                String msg = "Invalid last name is provided.";
                log.error(msg, e);
                throw new Exception(msg, e);
            }
        }

        tenant.setAdminFirstName(tenantInfoBean.getFirstname());
        tenant.setAdminLastName(tenantInfoBean.getLastname());
        TenantMgtUtil.addClaimsToUserStoreManager(tenant);

        // filling the email value
        if (tenantInfoBean.getEmail() != null && !tenantInfoBean.getEmail().equals("")) {
            // validate the email
            try {
                CommonUtil.validateEmail(tenantInfoBean.getEmail());
            } catch (Exception e) {
                String msg = "Invalid email is provided.";
                log.error(msg, e);
                throw new Exception(msg, e);
            }
            tenant.setEmail(tenantInfoBean.getEmail());
        }

        UserRealm userRealm = configSystemRegistry.getUserRealm();
        try {
            userStoreManager = userRealm.getUserStoreManager();
        } catch (UserStoreException e) {
            String msg = "Error in getting the user store manager for tenant, tenant domain: " +
                    tenantDomain + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        boolean updatePassword = false;
        if (tenantInfoBean.getAdminPassword() != null
                && !tenantInfoBean.getAdminPassword().equals("")) {
            updatePassword = true;
        }
        if (!userStoreManager.isReadOnly() && updatePassword) {
            // now we will update the tenant admin with the admin given
            // password.
            try {
                userStoreManager.updateCredentialByAdmin(tenantInfoBean.getAdmin(),
                        tenantInfoBean.getAdminPassword());
            } catch (UserStoreException e) {
                String msg = "Error in changing the tenant admin password, tenant domain: " +
                        tenantInfoBean.getTenantDomain() + ". " + e.getMessage() + " for: " +
                        tenantInfoBean.getAdmin();
                log.error(msg, e);
                throw new Exception(msg, e);
            }
        } else {
            //Password should be empty since no password update done
            tenantInfoBean.setAdminPassword("");
        }

        try {
            tenantManager.updateTenant(tenant);
        } catch (UserStoreException e) {
            String msg = "Error in updating the tenant for tenant domain: " + tenantDomain + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        //Notify tenant update to all listeners
        try {
            TenantMgtUtil.triggerUpdateTenant(tenantInfoBean);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant update.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully updated the tenant " + tenantDomain);
        return stratosApiResponse;
    }

    @GET
    @Path("/tenant/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean getTenant(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {

        try {
            return getTenantForDomain(tenantDomain);
        } catch (Exception e) {
            String msg = "Error in getting tenant information for tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }

    private TenantInfoBean getTenantForDomain (String tenantDomain) throws Exception {

        TenantManager tenantManager = ServiceHolder.getTenantManager();

        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " +
                    tenantDomain + ".";
            log.error(msg);
            throw new Exception(msg, e);
        }
        Tenant tenant;
        try {
            tenant = (Tenant) tenantManager.getTenant(tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant from the tenant manager.";
            log.error(msg);
            throw new Exception(msg, e);
        }

        TenantInfoBean bean = TenantMgtUtil.initializeTenantInfoBean(tenantId, tenant);

        // retrieve first and last names from the UserStoreManager
        bean.setFirstname(ClaimsMgtUtil.getFirstNamefromUserStoreManager(
                ServiceHolder.getRealmService(), tenantId));
        bean.setLastname(ClaimsMgtUtil.getLastNamefromUserStoreManager(
                ServiceHolder.getRealmService(), tenantId));

        //getting the subscription plan
        String activePlan = "";
        //TODO: usage plan using billing service

        if(activePlan != null && activePlan.trim().length() > 0){
            bean.setUsagePlan(activePlan);
        }else{
            bean.setUsagePlan("");
        }

        return bean;
    }

    @DELETE
    @Path("/tenant/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse deleteTenant(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        int tenantId = 0;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Error in deleting tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        try {
            //TODO: billing related info cleanup
            TenantMgtUtil.deleteTenantRegistryData(tenantId);
            TenantMgtUtil.deleteTenantUMData(tenantId);
            tenantManager.deleteTenant(tenantId);
            log.info("Deleted tenant with domain: " + tenantDomain + " and tenant id: " + tenantId +
                    " from the system.");
        } catch (Exception e) {
            String msg = "Error deleting tenant with domain: " + tenantDomain + " and tenant id: " +
                    tenantId + ".";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully deleted tenant " + tenantDomain);
        return stratosApiResponse;
    }

    @GET
    @Path("/tenant/list")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean[] retrieveTenants() throws RestAPIException {
        List<TenantInfoBean> tenantList = null;
        try {
            tenantList = getAllTenants();
        } catch (Exception e) {
            String msg = "Error in retrieving tenants";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        return tenantList.toArray(new TenantInfoBean[tenantList.size()]);
    }

    @GET
    @Path("tenant/search/{domain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean[] retrievePartialSearchTenants(@PathParam("domain")String domain) throws RestAPIException {
        List<TenantInfoBean> tenantList = null;
        try {
            tenantList = searchPartialTenantsDomains(domain);
        } catch (Exception e) {
            String msg = "Error in getting information for tenant " + domain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        return tenantList.toArray(new TenantInfoBean[tenantList.size()]);
    }

    @POST
    @Path("tenant/activate/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse activateTenant(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);

        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " + tenantDomain
                    + ".";
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {

            throw new RestAPIException( e);
        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();

        try {
            TenantMgtUtil.activateTenant(tenantDomain, tenantManager, tenantId);

        } catch (Exception e) {
            throw new RestAPIException( e);
        }

        //Notify tenant activation all listeners
        try {
            TenantMgtUtil.triggerTenantActivation(tenantId);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant activate.";
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

        stratosApiResponse.setMessage("Successfully activated tenant " + tenantDomain);
        return stratosApiResponse;
    }

    @POST
    @Path("tenant/availability/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public boolean isDomainAvailable(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {
        try {
            return CommonUtil.isDomainNameAvailable(tenantDomain);
        } catch (Exception e) {
            String msg = "Error in checking domain " + tenantDomain + " is available";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

    }

    @POST
    @Path("tenant/deactivate/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse deactivateTenant(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {

        TenantManager tenantManager = ServiceHolder.getTenantManager();
        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);

        } catch (UserStoreException e) {
            String msg =
                    "Error in retrieving the tenant id for the tenant domain: " +
                            tenantDomain + ".";
            log.error(msg, e);
            throw new RestAPIException(msg, e);

        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            throw new RestAPIException( e);
        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();

        try {
            TenantMgtUtil.deactivateTenant(tenantDomain, tenantManager, tenantId);
        } catch (Exception e) {
            throw new RestAPIException( e);
        }

        //Notify tenant deactivation all listeners
        try {
            TenantMgtUtil.triggerTenantDeactivation(tenantId);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant deactivate.";
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

        stratosApiResponse.setMessage("Successfully deactivated tenant " + tenantDomain);
        return stratosApiResponse;
    }

    @POST
    @Path("/service/definition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse deployService (ServiceDefinitionBean serviceDefinitionBean)
            throws RestAPIException {

        log.info("Service definition request.. : " + serviceDefinitionBean.getServiceName());
        // super tenant Deploying service (MT)
        // here an alias is generated
        return StratosApiV40Utils.deployService(serviceDefinitionBean.getCartridgeType(), UUID.randomUUID().toString(), serviceDefinitionBean.getAutoscalingPolicyName(),
                serviceDefinitionBean.getDeploymentPolicyName(), getTenantDomain(), getUsername(), getTenantId(),
                serviceDefinitionBean.getClusterDomain(), serviceDefinitionBean.getClusterSubDomain(),
                serviceDefinitionBean.getTenantRange());
    }

    @GET
    @Path("/service")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public ServiceDefinitionBean[] getServices() throws RestAPIException {
        List<ServiceDefinitionBean> serviceDefinitionBeans = StratosApiV40Utils.getdeployedServiceInformation();
        return serviceDefinitionBeans == null || serviceDefinitionBeans.isEmpty() ? new ServiceDefinitionBean[0] :
                serviceDefinitionBeans.toArray(new ServiceDefinitionBean[serviceDefinitionBeans.size()]);
    }

    @GET
    @Path("/service/{serviceType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public ServiceDefinitionBean getService(@PathParam("serviceType") String serviceType)throws RestAPIException {

        return StratosApiV40Utils.getDeployedServiceInformation(serviceType);
    }

    @GET
    @Path("/service/active")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public List<Cartridge> getActiveService()throws RestAPIException {

        return StratosApiV40Utils.getActiveDeployedServiceInformation(getConfigContext());
    }

    @DELETE
    @Path("/service/definition/{serviceType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse unDeployService (@PathParam("serviceType") String serviceType)
            throws RestAPIException {

        return StratosApiV40Utils.undeployService(serviceType);
    }

    @POST
    @Path("/reponotification")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public void getRepoNotification(Payload payload) throws RestAPIException {

        StratosApiV40Utils.getGitRepositoryNotification(payload);
    }

    @POST
    @Path("/cartridge/sync")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public StratosApiResponse synchronizeRepository(String alias) throws RestAPIException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Synchronizing Git repository for alias '%s'", alias));
        }
        CartridgeSubscription cartridgeSubscription = StratosApiV40Utils.getCartridgeSubscription(alias, getConfigContext());
        if (cartridgeSubscription != null && cartridgeSubscription.getRepository() != null && log.isDebugEnabled()) {
            log.debug(String.format("Found subscription for '%s'. Git repository: %s", alias, cartridgeSubscription
                    .getRepository().getUrl()));
        }
        return StratosApiV40Utils.synchronizeRepository(cartridgeSubscription);
    }

    private List<TenantInfoBean> getAllTenants() throws RestAPIException {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        Tenant[] tenants;
        try {
            tenants = (Tenant[]) tenantManager.getAllTenants();
        } catch (Exception e) {
            String msg = "Error in retrieving the tenant information";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        List<TenantInfoBean> tenantList = new ArrayList<TenantInfoBean>();
        for (Tenant tenant : tenants) {
            TenantInfoBean bean = TenantMgtUtil.getTenantInfoBeanfromTenant(tenant.getId(), tenant);
            tenantList.add(bean);
        }
        return tenantList;
    }

    private List<TenantInfoBean> searchPartialTenantsDomains(String domain) throws RestAPIException {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        Tenant[] tenants;
        try {
            domain = domain.trim();
            tenants = (Tenant[]) tenantManager.getAllTenantsForTenantDomainStr(domain);
        } catch (Exception e) {
            String msg = "Error in retrieving the tenant information.";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        List<TenantInfoBean> tenantList = new ArrayList<TenantInfoBean>();
        for (Tenant tenant : tenants) {
            TenantInfoBean bean = TenantMgtUtil.getTenantInfoBeanfromTenant(tenant.getId(), tenant);
            tenantList.add(bean);
        }
        return tenantList;
    }

    @POST
    @Path("/cartridge/{cartridgeType}/subscription/{subscriptionAlias}/domains")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response addSubscriptionDomains(@PathParam("cartridgeType") String cartridgeType,
                                           @PathParam("subscriptionAlias") String subscriptionAlias,
                                           SubscriptionDomainRequest request) throws RestAPIException {

        StratosApiResponse stratosApiResponse = StratosApiV40Utils.addSubscriptionDomains(getConfigContext(), cartridgeType, subscriptionAlias, request);
        return Response.ok().entity(stratosApiResponse).build();
    }

    @GET
    @Path("/cartridge/{cartridgeType}/subscription/{subscriptionAlias}/domains")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getSubscriptionDomains(@PathParam("cartridgeType") String cartridgeType,
                                           @PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {
        SubscriptionDomainBean[] subscriptionDomainBean = StratosApiV40Utils.getSubscriptionDomains(getConfigContext(), cartridgeType, subscriptionAlias).toArray(new SubscriptionDomainBean[0]);

        if(subscriptionDomainBean.length == 0){
            return Response.status(Response.Status.NOT_FOUND).build();
        }else{
            return Response.ok().entity(subscriptionDomainBean).build();
        }
    }

    @GET
    @Path("/cartridge/{cartridgeType}/subscription/{subscriptionAlias}/domains/{domainName}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getSubscriptionDomain(@PathParam("cartridgeType") String cartridgeType,
                                          @PathParam("subscriptionAlias") String subscriptionAlias, @PathParam("domainName") String domainName) throws RestAPIException {
        SubscriptionDomainBean subscriptionDomainBean = StratosApiV40Utils.getSubscriptionDomain(getConfigContext(), cartridgeType, subscriptionAlias, domainName);
        if(subscriptionDomainBean.domainName == null){
            return Response.status(Response.Status.NOT_FOUND).build();
        }else{
            return Response.ok().entity(subscriptionDomainBean).build();
        }
    }

    @DELETE
    @Path("/cartridge/{cartridgeType}/subscription/{subscriptionAlias}/domains/{domainName}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public StratosApiResponse removeSubscriptionDomain(@PathParam("cartridgeType") String cartridgeType,
                                                         @PathParam("subscriptionAlias") String subscriptionAlias,
                                                         @PathParam("domainName") String domainName) throws RestAPIException {
        return StratosApiV40Utils.removeSubscriptionDomain(getConfigContext(), cartridgeType, subscriptionAlias, domainName);
    }

    @GET
    @Path("/cartridge/{cartridgeType}/subscription/{subscriptionAlias}/load-balancer-cluster")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getLoadBalancerCluster(@PathParam("cartridgeType") String cartridgeType,
                                           @PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("GET /cartridge/%s/subscription/%s/load-balancer-cluster", cartridgeType, subscriptionAlias));
        }
        Cartridge subscription = StratosApiV40Utils.getSubscription(subscriptionAlias, getConfigContext());
        String lbClusterId = subscription.getLbClusterId();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Load balancer cluster-id found: %s", lbClusterId));
        }
        if (StringUtils.isNotBlank(lbClusterId)) {
            Cluster lbCluster = getCluster(lbClusterId);
            if (lbCluster != null) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Load balancer cluster found: %s", lbCluster.toString()));
                }
                Response.ok().entity(lbCluster).build();
            }
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
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
package org.apache.stratos.rest.endpoint.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.exception.StratosException;
import org.apache.stratos.common.util.ClaimsMgtUtil;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.manager.dto.Cartridge;
import org.apache.stratos.manager.dto.SubscriptionInfo;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.rest.endpoint.ServiceHolder;
import org.apache.stratos.rest.endpoint.Utils;
import org.apache.stratos.rest.endpoint.annotation.AuthorizationAction;
import org.apache.stratos.rest.endpoint.annotation.SuperTenantService;
import org.apache.stratos.rest.endpoint.bean.CartridgeInfoBean;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.PartitionGroup;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.ServiceDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.topology.Cluster;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;
import org.apache.stratos.tenant.mgt.core.TenantPersistor;
import org.apache.stratos.tenant.mgt.util.TenantMgtUtil;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
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

@Path("/admin/")
public class StratosAdmin extends AbstractAdmin {
    private static Log log = LogFactory.getLog(StratosAdmin.class);
    @Context
    HttpServletRequest httpServletRequest;

    @POST
    @Path("/init")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public void initialize ()
            throws RestAPIException {
    	
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
    public void deployCartridgeDefinition (CartridgeDefinitionBean cartridgeDefinitionBean)
            throws RestAPIException {

        ServiceUtils.deployCartridge(cartridgeDefinitionBean, getConfigContext(), getUsername(),
                                     getTenantDomain());
    }

    @DELETE
    @Path("/cartridge/definition/{cartridgeType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void unDeployCartridgeDefinition (@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {

        ServiceUtils.undeployCartridge(cartridgeType);
    }

    @POST
    @Path("/policy/deployment/partition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public boolean deployPartition (Partition partition)
            throws RestAPIException {

            return ServiceUtils.deployPartition(partition);
    }

    @POST
    @Path("/policy/autoscale")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public boolean deployAutoscalingPolicyDefintion (AutoscalePolicy autoscalePolicy)
            throws RestAPIException {

        return ServiceUtils.deployAutoscalingPolicy(autoscalePolicy);
    }

    @POST
    @Path("/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public boolean deployDeploymentPolicyDefinition (DeploymentPolicy deploymentPolicy)
            throws RestAPIException {

        return ServiceUtils.deployDeploymentPolicy(deploymentPolicy);
    }

    @GET
    @Path("/partition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Partition[] getPartitions () throws RestAPIException {

        return ServiceUtils.getAvailablePartitions();
    }

    @GET
    @Path("/partition/{partitionId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Partition getPartition (@PathParam("partitionId") String partitionId) throws RestAPIException {

        return ServiceUtils.getPartition(partitionId);
    }

    @GET
    @Path("/partition/group/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public PartitionGroup[] getPartitionGroups (@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {

        return ServiceUtils.getPartitionGroups(deploymentPolicyId);
    }

    @GET
    @Path("/partition/{deploymentPolicyId}/{partitionGroupId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Partition [] getPartitions (@PathParam("deploymentPolicyId") String deploymentPolicyId,
                                       @PathParam("partitionGroupId") String partitionGroupId) throws RestAPIException {

        return ServiceUtils.getPartitionsOfGroup(deploymentPolicyId, partitionGroupId);
    }
    
    @GET
    @Path("/partition/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Partition [] getPartitionsOfPolicy (@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {

        return ServiceUtils.getPartitionsOfDeploymentPolicy(deploymentPolicyId);
    }

    @GET
    @Path("/policy/autoscale")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public AutoscalePolicy[] getAutoscalePolicies () throws RestAPIException {

        return ServiceUtils.getAutoScalePolicies();
    }

    @GET
    @Path("/policy/autoscale/{autoscalePolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public AutoscalePolicy getAutoscalePolicies (@PathParam("autoscalePolicyId") String autoscalePolicyId)
            throws RestAPIException {

        return ServiceUtils.getAutoScalePolicy(autoscalePolicyId);
    }

    @GET
    @Path("/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public DeploymentPolicy[] getDeploymentPolicies () throws RestAPIException {

        return ServiceUtils.getDeploymentPolicies();
    }

    @GET
    @Path("/policy/deployment/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public DeploymentPolicy getDeploymentPolicies (@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {

        return ServiceUtils.getDeploymentPolicy(deploymentPolicyId);
    }

    @GET
    @Path("{cartridgeType}/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public DeploymentPolicy[] getValidDeploymentPolicies (@PathParam("cartridgeType") String cartridgeType)
            throws RestAPIException {

        return ServiceUtils.getDeploymentPolicies(cartridgeType);
    }

    @GET
    @Path("/cartridge/tenanted/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getAvailableMultiTenantCartridges() throws ADCException {
        List<Cartridge> cartridges = ServiceUtils.getAvailableCartridges(null, true, getConfigContext());
        return cartridges.isEmpty() ? new Cartridge[0] : cartridges.toArray(new Cartridge[cartridges.size()]);
    }

    @GET
    @Path("/cartridge/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getAvailableSingleTenantCartridges() throws ADCException {
        List<Cartridge> cartridges = ServiceUtils.getAvailableCartridges(null, false, getConfigContext());
        return cartridges.isEmpty() ? new Cartridge[0] : cartridges.toArray(new Cartridge[cartridges.size()]);
    }

    @GET
    @Path("/cartridge/list/subscribed")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getSubscribedCartridges() throws ADCException {
        List<Cartridge> cartridgeList = ServiceUtils.getSubscriptions(null, getConfigContext());
        // Following is very important when working with axis2
        return cartridgeList.isEmpty() ? new Cartridge[0] : cartridgeList.toArray(new Cartridge[cartridgeList.size()]);
    }
    
    @GET
    @Path("/cartridge/info/{subscriptionAlias}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge getCartridgeInfo(@PathParam("subscriptionAlias") String subscriptionAlias) throws ADCException {
        return ServiceUtils.getSubscription(subscriptionAlias, getConfigContext());
    }

    @POST
    @Path("/cartridge/subscribe")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public SubscriptionInfo subscribe(CartridgeInfoBean cartridgeInfoBean) {
        try {
            return ServiceUtils.subscribe(cartridgeInfoBean.getCartridgeType(),
                    cartridgeInfoBean.getAlias(),
                    cartridgeInfoBean.getAutoscalePolicy(),
                    cartridgeInfoBean.getDeploymentPolicy(),
                    cartridgeInfoBean.getRepoURL(),
                    cartridgeInfoBean.isPrivateRepo(),
                    cartridgeInfoBean.getRepoUsername(),
                    cartridgeInfoBean.getRepoPassword(),
                    cartridgeInfoBean.getDataCartridgeType(),
                    cartridgeInfoBean.getDataCartridgeAlias(),
                    getConfigContext(),
                    getUsername(),
                    getTenantDomain());
        } catch (Exception exception) {
            log.error(exception);
            return null;
        }
    }

    @GET
    @Path("/cluster/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cluster[] getClustersForTenant() throws ADCException {

        return ServiceUtils.getClustersForTenant(getConfigContext());
    }

    @GET
    @Path("/cluster/{cartridgeType}/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cluster[] getClusters(@PathParam("cartridgeType") String cartridgeType) throws ADCException {

        return ServiceUtils.getClustersForTenantAndCartridgeType(getConfigContext(), cartridgeType);
    }

    @GET
    @Path("/cluster/{cartridgeType}/{subscriptionAlias}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cluster getCluster(@PathParam("cartridgeType") String cartridgeType,
                              @PathParam("subscriptionAlias") String subscriptionAlias) throws ADCException, RestAPIException {

        return ServiceUtils.getCluster(cartridgeType, subscriptionAlias, getConfigContext());
    }
    
    @GET
    @Path("/cluster/clusterId/{clusterId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cluster getCluster(@PathParam("clusterId") String clusterId) throws ADCException {
    	Cluster cluster = null;
    	if(log.isDebugEnabled()) {
    		log.debug("Finding the Cluster for [id]: "+clusterId);
    	}
        Cluster[] clusters = ServiceUtils.getClustersForTenant(getConfigContext());
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
    public void unsubscribe(String alias) throws RestAPIException {
        try {
            ServiceUtils.unsubscribe(alias, getTenantDomain());
        } catch (Exception exception) {
            log.error(exception);
        }
    }

    @POST
    @Path("/tenant")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public String addTenant(TenantInfoBean tenantInfoBean) throws Exception {
        try {
            CommonUtil.validateEmail(tenantInfoBean.getEmail());
        } catch (Exception e) {
            String msg = "Invalid email is provided.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        String tenantDomain = tenantInfoBean.getTenantDomain();
        TenantMgtUtil.validateDomain(tenantDomain);
        UserRegistry userRegistry = (UserRegistry) PrivilegedCarbonContext.getThreadLocalCarbonContext().
                getRegistry(RegistryType.USER_GOVERNANCE);
        if (userRegistry == null) {
            log.error("Security Alert! User registry is null. A user is trying create a tenant "
                    + " without an authenticated session.");
            throw new Exception("Invalid data."); // obscure error message.
        }

        if (userRegistry.getTenantId() != MultitenantConstants.SUPER_TENANT_ID) {
            log.error("Security Alert! Non super tenant trying to create a tenant.");
            throw new Exception("Invalid data."); // obscure error message.
        }
        Tenant tenant = TenantMgtUtil.initializeTenant(tenantInfoBean);
        TenantPersistor persistor = ServiceHolder.getTenantPersistor();
        // not validating the domain ownership, since created by super tenant
        int tenantId = persistor.persistTenant(tenant, false, tenantInfoBean.getSuccessKey(),
                tenantInfoBean.getOriginatedService(),false); //TODO verify whether this is the correct approach (isSkeleton)
        tenantInfoBean.setTenantId(tenantId);

        TenantMgtUtil.addClaimsToUserStoreManager(tenant);

        //Notify tenant addition
        try {
            TenantMgtUtil.triggerAddTenant(tenantInfoBean);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant addition.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
        // For the super tenant tenant creation, tenants are always activated as they are created.
        TenantMgtUtil.activateTenantInitially(tenantInfoBean, tenantId);

        return TenantMgtUtil.prepareStringToShowThemeMgtPage(tenant.getId());
    }


    @PUT
    @Path("/tenant")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void updateTenant(TenantInfoBean tenantInfoBean) throws Exception {
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

    }

    @GET
    @Path("/tenant/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean getTenant(@PathParam("tenantDomain") String tenantDomain) throws Exception {
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
    public void deleteTenant(@PathParam("tenantDomain") String tenantDomain) throws Exception {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        int tenantId = tenantManager.getTenantId(tenantDomain);
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
            throw new Exception(msg, e);
        }
    }


    @GET
    @Path("/tenant/list")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean[] retrieveTenants() throws Exception {
        List<TenantInfoBean> tenantList = getAllTenants();
        return tenantList.toArray(new TenantInfoBean[tenantList.size()]);
    }

    @GET
    @Path("tenant/search/{domain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean[] retrievePartialSearchTenants(@PathParam("domain")String domain) throws Exception {
        List<TenantInfoBean> tenantList = searchPartialTenantsDomains(domain);
        return tenantList.toArray(new TenantInfoBean[tenantList.size()]);
    }


    @POST
    @Path("tenant/activate/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void activateTenant(@PathParam("tenantDomain") String tenantDomain) throws Exception {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " + tenantDomain
                    + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        TenantMgtUtil.activateTenant(tenantDomain, tenantManager, tenantId);

        //Notify tenant activation all listeners
        try {
            TenantMgtUtil.triggerTenantActivation(tenantId);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant activate.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

    }

    @POST
    @Path("tenant/availability/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public boolean isDomainAvailable(@PathParam("tenantDomain") String tenantDomain) throws Exception {
        return CommonUtil.isDomainNameAvailable(tenantDomain);

    }

    @POST
    @Path("tenant/deactivate/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void deactivateTenant(@PathParam("tenantDomain") String tenantDomain) throws Exception {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            String msg =
                    "Error in retrieving the tenant id for the tenant domain: " +
                            tenantDomain + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        TenantMgtUtil.deactivateTenant(tenantDomain, tenantManager, tenantId);

        //Notify tenant deactivation all listeners
        try {
            TenantMgtUtil.triggerTenantDeactivation(tenantId);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant deactivate.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }


    @POST
    @Path("/service/definition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void deployService (ServiceDefinitionBean serviceDefinitionBean)
            throws RestAPIException {

    	log.info("Service definition request.. : " + serviceDefinitionBean.getServiceName());
    	// super tenant Deploying service (MT) 
    	// here an alias is generated
       ServiceUtils.deployService(serviceDefinitionBean.getCartridgeType(), UUID.randomUUID().toString(), serviceDefinitionBean.getAutoscalingPolicyName(),
               serviceDefinitionBean.getDeploymentPolicyName(), getTenantDomain(), getUsername(), getTenantId(),
               serviceDefinitionBean.getClusterDomain(), serviceDefinitionBean.getClusterSubDomain(),
               serviceDefinitionBean.getTenantRange());
    }

    @DELETE
    @Path("/service/definition/{serviceType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void unDeployService (@PathParam("serviceType") String serviceType)
            throws RestAPIException {

        ServiceUtils.undeployService(serviceType);
    }


    private List<TenantInfoBean> getAllTenants() throws Exception {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        Tenant[] tenants;
        try {
            tenants = (Tenant[]) tenantManager.getAllTenants();
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant information.";
            throw new Exception(msg, e);
        }

        List<TenantInfoBean> tenantList = new ArrayList<TenantInfoBean>();
        for (Tenant tenant : tenants) {
            TenantInfoBean bean = TenantMgtUtil.getTenantInfoBeanfromTenant(tenant.getId(), tenant);
            tenantList.add(bean);
        }
        return tenantList;
    }

    private List<TenantInfoBean> searchPartialTenantsDomains(String domain) throws Exception {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        Tenant[] tenants;
        try {
            domain = domain.trim();
            tenants = (Tenant[]) tenantManager.getAllTenantsForTenantDomainStr(domain);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant information.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        List<TenantInfoBean> tenantList = new ArrayList<TenantInfoBean>();
        for (Tenant tenant : tenants) {
            TenantInfoBean bean = TenantMgtUtil.getTenantInfoBeanfromTenant(tenant.getId(), tenant);
            tenantList.add(bean);
        }
        return tenantList;
    }
}

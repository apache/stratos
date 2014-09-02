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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.exception.StratosException;
import org.apache.stratos.common.util.ClaimsMgtUtil;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.manager.dto.Cartridge;
import org.apache.stratos.manager.dto.SubscriptionInfo;
import org.apache.stratos.manager.exception.DomainMappingExistsException;
import org.apache.stratos.manager.exception.ServiceDoesNotExistException;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.subscription.SubscriptionDomain;
import org.apache.stratos.rest.endpoint.ServiceHolder;
import org.apache.stratos.rest.endpoint.Utils;
import org.apache.stratos.rest.endpoint.annotation.AuthorizationAction;
import org.apache.stratos.rest.endpoint.annotation.SuperTenantService;
import org.apache.stratos.rest.endpoint.bean.CartridgeInfoBean;
import org.apache.stratos.rest.endpoint.bean.StratosAdminResponse;
import org.apache.stratos.rest.endpoint.bean.SubscriptionDomainRequest;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.ServiceDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.repositoryNotificationInfoBean.Payload;
import org.apache.stratos.rest.endpoint.bean.repositoryNotificationInfoBean.Repository;
import org.apache.stratos.rest.endpoint.bean.subscription.domain.SubscriptionDomainBean;
import org.apache.stratos.rest.endpoint.bean.topology.Cluster;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;
import org.apache.stratos.rest.endpoint.exception.TenantNotFoundException;
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
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static javax.ws.rs.core.Response.ResponseBuilder;

@Path("/admin/")
public class StratosAdmin extends AbstractAdmin {
    private static Log log = LogFactory.getLog(StratosAdmin.class);
    @Context
    HttpServletRequest httpServletRequest;
    @Context
    UriInfo uriInfo;


    @POST
    @Path("/init")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public StratosAdminResponse initialize ()
            throws RestAPIException {


        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully logged in");
        return stratosAdminResponse;
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
    public Response deployCartridgeDefinition(CartridgeDefinitionBean cartridgeDefinitionBean)
            throws RestAPIException {

        ServiceUtils.deployCartridge(cartridgeDefinitionBean, getConfigContext(), getUsername(),
                getTenantDomain());
        URI url =  uriInfo.getAbsolutePathBuilder().path(cartridgeDefinitionBean.type).build();
        return Response.created(url).build();

    }

    @DELETE
    @Path("/cartridge/definition/{cartridgeType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response unDeployCartridgeDefinition (@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {

        ServiceUtils.undeployCartridge(cartridgeType);
        return Response.noContent().build();
    }

    @POST
    @Path("/policy/deployment/partition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response deployPartition (Partition partition)
            throws RestAPIException {

        ServiceUtils.deployPartition(partition);
        URI url =  uriInfo.getAbsolutePathBuilder().path(partition.id).build();
        return Response.created(url).build();
    }

    @POST
    @Path("/policy/autoscale")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response deployAutoscalingPolicyDefintion (AutoscalePolicy autoscalePolicy)
            throws RestAPIException {

        ServiceUtils.deployAutoscalingPolicy(autoscalePolicy);
        URI url =  uriInfo.getAbsolutePathBuilder().path(autoscalePolicy.getId()).build();
        return Response.created(url).build();
    }

    @POST
    @Path("/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response deployDeploymentPolicyDefinition (DeploymentPolicy deploymentPolicy)
            throws RestAPIException {

        ServiceUtils.deployDeploymentPolicy(deploymentPolicy);
        URI url =  uriInfo.getAbsolutePathBuilder().path(deploymentPolicy.id).build();
        return Response.created(url).build();
    }

    @GET
    @Path("/partition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getPartitions () throws RestAPIException {
        return Response.ok().entity(ServiceUtils.getAvailablePartitions()).build();
    }

    @GET
    @Path("/partition/{partitionId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getPartition (@PathParam("partitionId") String partitionId) throws RestAPIException {
        return Response.ok().entity(ServiceUtils.getPartition(partitionId)).build();
    }

    @GET
    @Path("/partition/group/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getPartitionGroups (@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {
        return Response.ok().entity(ServiceUtils.getPartitionGroups(deploymentPolicyId)).build();
    }

    @GET
    @Path("/partition/{deploymentPolicyId}/{partitionGroupId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getPartitions (@PathParam("deploymentPolicyId") String deploymentPolicyId,
                                       @PathParam("partitionGroupId") String partitionGroupId) throws RestAPIException {
        return Response.ok().entity(ServiceUtils.getPartitionsOfGroup(deploymentPolicyId, partitionGroupId)).build();
    }
    
    @GET
    @Path("/partition/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getPartitionsOfPolicy (@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {

        return Response.ok().entity(ServiceUtils.getPartitionsOfDeploymentPolicy(deploymentPolicyId)).build();
    }

    @GET
    @Path("/policy/autoscale")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getAutoscalePolicies () throws RestAPIException {
        return Response.ok().entity(ServiceUtils.getAutoScalePolicies()).build();
    }

    @GET
    @Path("/policy/autoscale/{autoscalePolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getAutoscalePolicies (@PathParam("autoscalePolicyId") String autoscalePolicyId)
            throws RestAPIException {
        return Response.ok().entity(ServiceUtils.getAutoScalePolicy(autoscalePolicyId)).build();
    }

    @GET
    @Path("/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getDeploymentPolicies () throws RestAPIException {
        return Response.ok().entity(ServiceUtils.getDeploymentPolicies()).build();
    }

    @GET
    @Path("/policy/deployment/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getDeploymentPolicies (@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {
        return Response.ok().entity(ServiceUtils.getDeploymentPolicy(deploymentPolicyId)).build();
    }

    @GET
    @Path("{cartridgeType}/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getValidDeploymentPolicies (@PathParam("cartridgeType") String cartridgeType)
            throws RestAPIException {
        return Response.ok().entity(ServiceUtils.getDeploymentPolicies(cartridgeType)).build();
    }

    @GET
    @Path("/cartridge/tenanted/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getAvailableMultiTenantCartridges() throws RestAPIException {
        List<Cartridge> cartridges = ServiceUtils.getAvailableCartridges(null, true, getConfigContext());
        ResponseBuilder rb = Response.ok();
        rb.entity(cartridges.isEmpty() ? new Cartridge[0] : cartridges.toArray(new Cartridge[cartridges.size()]));
        return rb.build();
    }

    @GET
    @Path("/cartridge/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getAvailableSingleTenantCartridges() throws RestAPIException {
        List<Cartridge> cartridges = ServiceUtils.getAvailableCartridges(null, false, getConfigContext());
        ResponseBuilder rb = Response.ok();
        rb.entity(cartridges.isEmpty() ? new Cartridge[0] : cartridges.toArray(new Cartridge[cartridges.size()]));
        return rb.build();
    }

    @GET
    @Path("/cartridge/available/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getAvailableCartridges() throws RestAPIException {
        List<Cartridge> cartridges = ServiceUtils.getAvailableCartridges(null, null, getConfigContext());
        ResponseBuilder rb = Response.ok();
        rb.entity(cartridges.isEmpty() ? new Cartridge[0] : cartridges.toArray(new Cartridge[cartridges.size()]));
        return rb.build();
    }

    @GET
    @Path("/cartridge/list/subscribed")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getSubscribedCartridges() throws RestAPIException {
        List<Cartridge> cartridgeList = ServiceUtils.getSubscriptions(null,null, getConfigContext());
        // Following is very important when working with axis2
        ResponseBuilder rb = Response.ok();
        rb.entity(cartridgeList.isEmpty() ? new Cartridge[0] : cartridgeList.toArray(new Cartridge[cartridgeList.size()]));
        return rb.build();
    }

    @GET
    @Path("/cartridge/list/subscribed/group/{serviceGroup}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getSubscribedCartridgesForServiceGroup(@PathParam("serviceGroup") String serviceGroup) throws RestAPIException {
        List<Cartridge> cartridgeList = ServiceUtils.getSubscriptions(null, serviceGroup, getConfigContext());
        // Following is very important when working with axis2
        ResponseBuilder rb = Response.ok();
        rb.entity(cartridgeList.isEmpty() ? new Cartridge[0] : cartridgeList.toArray(new Cartridge[cartridgeList.size()]));
        return rb.build();
    }
    
    @GET
    @Path("/cartridge/info/{subscriptionAlias}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getCartridgeInfo(@PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {
        ResponseBuilder rb = Response.ok();
        rb.entity(ServiceUtils.getSubscription(subscriptionAlias, getConfigContext()));
        return rb.build();
    }

    @GET
    @Path("/cartridge/available/info/{cartridgeType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getAvailableSingleTenantCartridgeInfo(@PathParam("cartridgeType") String cartridgeType)
                                            throws RestAPIException {
        ResponseBuilder rb = Response.ok();
        rb.entity(ServiceUtils.getAvailableCartridgeInfo(cartridgeType, null, getConfigContext()));
        return rb.build();
    }

    @GET
    @Path("/cartridge/lb")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getAvailableLbCartridges() throws RestAPIException {
    	List<Cartridge> lbCartridges = ServiceUtils.getAvailableLbCartridges(false, getConfigContext());
        return Response.ok().entity(lbCartridges.isEmpty() ? new Cartridge[0] : lbCartridges.toArray(new Cartridge[lbCartridges.size()])).build();
    }

    @GET
    @Path("/cartridge/active/{cartridgeType}/{subscriptionAlias}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getActiveInstances(@PathParam("cartridgeType") String cartridgeType,
                              @PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {
        ResponseBuilder rb = Response.ok();
        rb.entity(ServiceUtils.getActiveInstances(cartridgeType, subscriptionAlias, getConfigContext()));
        return rb.build();
    }

    @POST
    @Path("/cartridge/subscribe")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response subscribe(CartridgeInfoBean cartridgeInfoBean) throws RestAPIException {

        SubscriptionInfo subscriptionInfo= ServiceUtils.subscribe(cartridgeInfoBean,
                getConfigContext(),
                getUsername(),
                getTenantDomain());
        return Response.ok(subscriptionInfo).build();
    }

    @GET
    @Path("/cluster/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getClustersForTenant() throws RestAPIException {
        return Response.ok().entity(ServiceUtils.getClustersForTenant(getConfigContext())).build();
    }

    @GET
    @Path("/cluster/{cartridgeType}/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getClusters(@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {

        ResponseBuilder rb = Response.ok();
        rb.entity(ServiceUtils.getClustersForTenantAndCartridgeType(getConfigContext(), cartridgeType));
        return rb.build();
    }
    
    @GET
    @Path("/cluster/service/{cartridgeType}/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getServiceClusters(@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {
        ResponseBuilder rb = Response.ok();
        rb.entity(ServiceUtils.getClustersForTenantAndCartridgeType(getConfigContext(), cartridgeType));
        return rb.build();
    }

    @GET
    @Path("/cluster/{cartridgeType}/{subscriptionAlias}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getCluster(@PathParam("cartridgeType") String cartridgeType,
                              @PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException, RestAPIException {
        ResponseBuilder rb = Response.ok();
        rb.entity(ServiceUtils.getCluster(cartridgeType, subscriptionAlias, getConfigContext()));
        return rb.build();
    }
    
    @GET
    @Path("/cluster/clusterId/{clusterId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getCluster(@PathParam("clusterId") String clusterId) throws RestAPIException {
    	Cluster cluster = null;
    	if(log.isDebugEnabled()) {
    		log.debug("Finding cluster for [id]: "+clusterId);
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
        return Response.ok().entity(cluster).build();
    }

    @POST
    @Path("/cartridge/unsubscribe")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response unsubscribe(String alias) throws RestAPIException {
        ServiceUtils.unsubscribe(alias, getTenantDomain());
        return Response.noContent().build();
    }

    @POST
    @Path("/tenant")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response addTenant(TenantInfoBean tenantInfoBean) throws RestAPIException {
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

        URI url =  uriInfo.getAbsolutePathBuilder().path(tenant.getDomain()).build();
        return Response.created(url).build();
    }

    @PUT
    @Path("/tenant")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response updateTenant(TenantInfoBean tenantInfoBean) throws RestAPIException {

        try {
            updateExistingTenant(tenantInfoBean);

        } catch(TenantNotFoundException ex){
            Response.status(Response.Status.NOT_FOUND).build();
        }catch (Exception e) {
            String msg = "Error in updating tenant " + tenantInfoBean.getTenantDomain();
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        return Response.noContent().build();
    }

    private void updateExistingTenant(TenantInfoBean tenantInfoBean) throws Exception {

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
            throw new TenantNotFoundException(msg,e);
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
    public Response deleteTenant(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        int tenantId = 0;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
        } catch (org.wso2.carbon.user.api.UserStoreException e) {
            String msg = "Error in deleting tenant " + tenantDomain;
            log.error(msg, e);
            //throw new RestAPIException(msg);
            return Response.status(Response.Status.NOT_FOUND).build();
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

        return Response.noContent().build();
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
    public Response activateTenant(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {
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

        return Response.noContent().build();
    }

    @POST
    @Path("tenant/availability/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response isDomainAvailable(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {
        boolean available;
        try {
            available = CommonUtil.isDomainNameAvailable(tenantDomain);
        } catch (Exception e) {
            String msg = "Error in checking domain " + tenantDomain + " is available";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        return Response.ok(available).build();
    }

    @POST
    @Path("tenant/deactivate/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response deactivateTenant(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {

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

        return Response.noContent().build();
    }

    @POST
    @Path("/service/definition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response deployService (ServiceDefinitionBean serviceDefinitionBean)
            throws RestAPIException {

    	log.info("Service definition request.. : " + serviceDefinitionBean.getServiceName());
    	// super tenant Deploying service (MT) 
    	// here an alias is generated
       ServiceUtils.deployService(serviceDefinitionBean.getCartridgeType(), UUID.randomUUID().toString(), serviceDefinitionBean.getAutoscalingPolicyName(),
               serviceDefinitionBean.getDeploymentPolicyName(), getTenantDomain(), getUsername(), getTenantId(),
               serviceDefinitionBean.getClusterDomain(), serviceDefinitionBean.getClusterSubDomain(),
               serviceDefinitionBean.getTenantRange());

        URI url =  uriInfo.getAbsolutePathBuilder().path(serviceDefinitionBean.getServiceName()).build();
        return Response.created(url).build();
    }

    @GET
    @Path("/service")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public ServiceDefinitionBean[] getServices() throws RestAPIException {
        List<ServiceDefinitionBean> serviceDefinitionBeans = ServiceUtils.getdeployedServiceInformation();
        return serviceDefinitionBeans == null || serviceDefinitionBeans.isEmpty() ? new ServiceDefinitionBean[0] :
            serviceDefinitionBeans.toArray(new ServiceDefinitionBean[serviceDefinitionBeans.size()]);
    }

    @GET
    @Path("/service/{serviceType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getService(@PathParam("serviceType") String serviceType)throws RestAPIException {
        ResponseBuilder rb;
        ServiceDefinitionBean serviceDefinitionBean = ServiceUtils.getDeployedServiceInformation(serviceType);
        if(serviceDefinitionBean == null){
            rb = Response.status(Response.Status.NOT_FOUND);
        }else{
            rb = Response.ok(serviceDefinitionBean);
        }
        return rb.build();
    }

    @GET
    @Path("/service/active")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public List<Cartridge> getActiveService()throws RestAPIException {

        return ServiceUtils.getActiveDeployedServiceInformation(getConfigContext());
    }

    @DELETE
    @Path("/service/definition/{serviceType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response unDeployService (@PathParam("serviceType") String serviceType) throws RestAPIException {
        try {
            ServiceUtils.undeployService(serviceType);
        } catch (ServiceDoesNotExistException e) {
           return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @POST
    @Path("/reponotification")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getRepoNotification(Payload payload) throws RestAPIException {

        ServiceUtils.getGitRepositoryNotification(payload);
        return Response.noContent().build();
    }
    
	@POST
	@Path("/cartridge/sync")
	@Consumes("application/json")
	@AuthorizationAction("/permission/protected/manage/monitor/tenants")
	public Response synchronizeRepository(String alias) throws RestAPIException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Synchronizing Git repository for alias '%s'", alias));
		}
		CartridgeSubscription cartridgeSubscription = ServiceUtils.getCartridgeSubscription(alias, getConfigContext());
		if (cartridgeSubscription != null && cartridgeSubscription.getRepository() != null && log.isDebugEnabled()) {
			log.debug(String.format("Found subscription for '%s'. Git repository: %s", alias, cartridgeSubscription
					.getRepository().getUrl()));
		}
		ServiceUtils.synchronizeRepository(cartridgeSubscription);
        return Response.noContent().build();
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
        ServiceUtils.addSubscriptionDomains(getConfigContext(), cartridgeType, subscriptionAlias, request);
        return Response.noContent().build();
    }

    @GET
    @Path("/cartridge/{cartridgeType}/subscription/{subscriptionAlias}/domains")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getSubscriptionDomains(@PathParam("cartridgeType") String cartridgeType,@PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {

        SubscriptionDomainBean[] subscriptionDomainBean = ServiceUtils.getSubscriptionDomains(getConfigContext(), cartridgeType, subscriptionAlias).toArray(new SubscriptionDomainBean[0]);

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
    public Response getSubscriptionDomain(@PathParam("cartridgeType") String cartridgeType, @PathParam("subscriptionAlias") String subscriptionAlias, @PathParam("domainName") String domainName) throws RestAPIException {

        SubscriptionDomainBean subscriptionDomainBean = ServiceUtils.getSubscriptionDomain(getConfigContext(), cartridgeType, subscriptionAlias, domainName);
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
    public Response removeSubscriptionDomain(@PathParam("cartridgeType") String cartridgeType,
                                                         @PathParam("subscriptionAlias") String subscriptionAlias,
                                                         @PathParam("domainName") String domainName) throws RestAPIException {
        try {
            ServiceUtils.removeSubscriptionDomain(getConfigContext(), cartridgeType, subscriptionAlias, domainName);
        } catch (DomainMappingExistsException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
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
        Cartridge subscription = ServiceUtils.getSubscription(subscriptionAlias, getConfigContext());
        String lbClusterId = subscription.getLbClusterId();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Load balancer cluster-id found: %s", lbClusterId));
        }
        if (StringUtils.isNotBlank(lbClusterId)) {
            Response.fromResponse(getCluster(lbClusterId));
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}

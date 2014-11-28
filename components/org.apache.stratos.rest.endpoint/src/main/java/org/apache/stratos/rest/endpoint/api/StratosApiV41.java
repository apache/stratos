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
import org.apache.stratos.manager.composite.application.beans.ApplicationDefinition;
import org.apache.stratos.manager.dto.Cartridge;
import org.apache.stratos.manager.exception.DomainMappingExistsException;
import org.apache.stratos.manager.grouping.definitions.ServiceGroupDefinition;
import org.apache.stratos.manager.subscription.ApplicationSubscription;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.user.mgt.StratosUserManager;
import org.apache.stratos.manager.user.mgt.beans.UserInfoBean;
import org.apache.stratos.manager.user.mgt.exception.UserManagerException;
import org.apache.stratos.rest.endpoint.ServiceHolder;
import org.apache.stratos.rest.endpoint.Utils;
import org.apache.stratos.rest.endpoint.annotation.AuthorizationAction;
import org.apache.stratos.rest.endpoint.annotation.SuperTenantService;
import org.apache.stratos.rest.endpoint.bean.ApplicationBean;
import org.apache.stratos.rest.endpoint.bean.CartridgeInfoBean;
import org.apache.stratos.rest.endpoint.bean.StratosApiResponse;
import org.apache.stratos.rest.endpoint.bean.SubscriptionDomainRequest;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.kubernetes.KubernetesGroup;
import org.apache.stratos.rest.endpoint.bean.kubernetes.KubernetesHost;
import org.apache.stratos.rest.endpoint.bean.kubernetes.KubernetesMaster;
import org.apache.stratos.rest.endpoint.bean.repositoryNotificationInfoBean.Payload;
import org.apache.stratos.rest.endpoint.bean.subscription.domain.SubscriptionDomainBean;
import org.apache.stratos.rest.endpoint.bean.topology.Cluster;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;
import org.apache.stratos.rest.endpoint.exception.TenantNotFoundException;
import org.apache.stratos.tenant.mgt.core.TenantPersistor;
import org.apache.stratos.tenant.mgt.util.TenantMgtUtil;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
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

import static javax.ws.rs.core.Response.ResponseBuilder;

/**
 * Stratos API v4.1 for Stratos 4.1.0 release.
 */
@Path("/")
public class StratosApiV41 extends AbstractApi {
    private static Log log = LogFactory.getLog(StratosApiV41.class);

    @Context
    HttpServletRequest httpServletRequest;
    @Context
    UriInfo uriInfo;

    @GET
    @Path("/init")
    @AuthorizationAction("/permission/admin/restlogin")
    public Response initialize()
            throws RestAPIException {
        StratosApiResponse response = new StratosApiResponse();
        response.setMessage("Successfully authenticated");
        return Response.ok(response).build();
    }

    /**
     * This method gets called by the client who are interested in using session mechanism to authenticate
     * themselves in subsequent calls. This method call get authenticated by the basic authenticator.
     * Once the authenticated call received, the method creates a session and returns the session id.
     *
     * @return
     */
    @GET
    @Path("/session")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/restlogin")
    public Response getSession() {
        HttpSession httpSession = httpServletRequest.getSession(true);//create session if not found
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        httpSession.setAttribute("userName", carbonContext.getUsername());
        httpSession.setAttribute("tenantDomain", carbonContext.getTenantDomain());
        httpSession.setAttribute("tenantId", carbonContext.getTenantId());

        String sessionId = httpSession.getId();
        return Response.ok().header("WWW-Authenticate", "Basic").type(MediaType.APPLICATION_JSON).
                entity(Utils.buildAuthenticationSuccessMessage(sessionId)).build();
    }

    @POST
    @Path("/cartridges")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/cartridgeDefinition")
    public Response deployCartridgeDefinition(CartridgeDefinitionBean cartridgeDefinitionBean)
            throws RestAPIException {
        StratosApiV41Utils.deployCartridge(cartridgeDefinitionBean, getConfigContext(), getUsername(),
                getTenantDomain());
        URI url = uriInfo.getAbsolutePathBuilder().path(cartridgeDefinitionBean.type).build();
        return Response.created(url).build();

    }

    @GET
    @Path("/cartridges")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getCartridges() throws RestAPIException {
        List<Cartridge> cartridges = StratosApiV41Utils.getAvailableCartridges(null, null, getConfigContext());
        ResponseBuilder rb = Response.ok();
        rb.entity(cartridges.isEmpty() ? new Cartridge[0] : cartridges.toArray(new Cartridge[cartridges.size()]));
        return rb.build();
    }

    @DELETE
    @Path("/cartridges/{cartridgeType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/cartridgeDefinition")
    public Response unDeployCartridgeDefinition(@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {
        StratosApiV41Utils.undeployCartridge(cartridgeType);
        return Response.noContent().build();
    }

    @GET
    @Path("/cartridges/singleTenant")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getSingleTenantCartridges() throws RestAPIException {
        List<Cartridge> cartridges = StratosApiV41Utils.getAvailableCartridges(null, false, getConfigContext());
        ResponseBuilder rb = Response.ok();
        rb.entity(cartridges.isEmpty() ? new Cartridge[0] : cartridges.toArray(new Cartridge[cartridges.size()]));
        return rb.build();
    }

    @GET
    @Path("/cartridges/multiTenant")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getMultiTenantCartridges() throws RestAPIException {
        List<Cartridge> cartridges = StratosApiV41Utils.getAvailableCartridges(null, true, getConfigContext());
        ResponseBuilder rb = Response.ok();
        rb.entity(cartridges.isEmpty() ? new Cartridge[0] : cartridges.toArray(new Cartridge[cartridges.size()]));
        return rb.build();
    }
    
    @GET
    @Path("/cartridges/{cartridgeType}/deploymentPolicy")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/deploymentPolicy")
    public Response getValidDeploymentPoliciesOfCartridge(@PathParam("cartridgeType") String cartridgeType)
            throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getDeploymentPolicies(cartridgeType)).build();
    }
    
    @GET
    @Path("/cartridges/{cartridgeType}/singleTenant")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getAvailableSingleTenantCartridge(@PathParam("cartridgeType") String cartridgeType)
            throws RestAPIException {
        ResponseBuilder rb = Response.ok();
        rb.entity(StratosApiV41Utils.getAvailableCartridgeInfo(cartridgeType, null, getConfigContext()));
        return rb.build();
    }

    @GET
    @Path("/cartridges/loadBalancer")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getAvailableLbCartridges() throws RestAPIException {
        List<Cartridge> lbCartridges = StratosApiV41Utils.getAvailableLbCartridges(false, getConfigContext());
        return Response.ok().entity(lbCartridges.isEmpty() ? new Cartridge[0] : lbCartridges.toArray(new Cartridge[lbCartridges.size()])).build();
    }

    @POST
    @Path("/groups")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response deployServiceGroupDefinition(ServiceGroupDefinition serviceGroupDefinition)
            throws RestAPIException {
        StratosApiV41Utils.deployServiceGroupDefinition(serviceGroupDefinition);
        URI url = uriInfo.getAbsolutePathBuilder().path(serviceGroupDefinition.getName()).build();
        return Response.created(url).build();
    }

    @GET
    @Path("/groups/{groupDefinitionName}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getServiceGroupDefinition(@PathParam("groupDefinitionName") String groupDefinitionName)
            throws RestAPIException {
        ServiceGroupDefinition serviceGroupDefinition = StratosApiV41Utils.getServiceGroupDefinition(groupDefinitionName);
        Response.ResponseBuilder rb;
        if (serviceGroupDefinition != null) {
            rb = Response.ok().entity(serviceGroupDefinition);
        } else {
            rb = Response.status(Response.Status.NOT_FOUND);
        }

        return rb.build();
    }

    @GET
    @Path("/groups")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getServiceGroups()
            throws RestAPIException {
        ServiceGroupDefinition[] serviceGroups = StratosApiV41Utils.getServiceGroupDefinitions();
        Response.ResponseBuilder rb;
        if (serviceGroups != null) {
            rb = Response.ok().entity(serviceGroups);
        } else {
            rb = Response.status(Response.Status.NOT_FOUND);
        }

        return rb.build();
    }

    @DELETE
    @Path("/groups/{groupDefinitionName}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response undeployServiceGroupDefinition(@PathParam("groupDefinitionName") String groupDefinitionName)
            throws RestAPIException {

        StratosApiV41Utils.undeployServiceGroupDefinition(groupDefinitionName);
        return Response.noContent().build();
    }
    
    @GET
    @Path("/partitions")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/partition")
    public Response getPartitions() throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getAvailablePartitions()).build();
    }

    @POST
    @Path("/partitions")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/partition")
    public Response deployPartition(Partition partition)
            throws RestAPIException {

        StratosApiV41Utils.deployPartition(partition);
        URI url = uriInfo.getAbsolutePathBuilder().path(partition.id).build();
        return Response.created(url).build();
    }

    @GET
    @Path("/partitions/{partitionId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/partition")
    public Response getPartition(@PathParam("partitionId") String partitionId) throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getPartition(partitionId)).build();
    }

    @POST
    @Path("/deploymentPolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/deploymentPolicy")
    public Response deployDeploymentPolicyDefinition(DeploymentPolicy deploymentPolicy)
            throws RestAPIException {

        StratosApiV41Utils.deployDeploymentPolicy(deploymentPolicy);
        URI url = uriInfo.getAbsolutePathBuilder().path(deploymentPolicy.getId()).build();
        return Response.created(url).build();
    }

    @PUT
    @Path("/deploymentPolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/deploymentPolicy")
    public Response updateDeploymentPolicyDefinition(DeploymentPolicy deploymentPolicy)
            throws RestAPIException {

        StratosApiV41Utils.updateDeploymentPolicy(deploymentPolicy);
        return Response.ok().build();
    }

    @GET
    @Path("/deploymentPolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/deploymentPolicy")
    public Response getDeploymentPolicies() throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getDeploymentPolicies()).build();
    }

    @GET
    @Path("/deploymentPolicies/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/deploymentPolicy")
    public Response getDeploymentPolicy(@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getDeploymentPolicy(deploymentPolicyId)).build();
    }

    @GET
    @Path("/deploymentPolicies/{deploymentPolicyId}/partitionGroup")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/partition")
    public Response getPartitionGroups(@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getPartitionGroups(deploymentPolicyId)).build();
    }

    @GET
    @Path("/deploymentPolicies/{deploymentPolicyId}/partitionGroup/{partitionGroupId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/partition")
    public Response getPartitionGroup(@PathParam("deploymentPolicyId") String deploymentPolicyId,
                                      @PathParam("partitionGroupId") String partitionGroupId) throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getPartitionsOfGroup(deploymentPolicyId, partitionGroupId)).build();
    }

    @GET
    @Path("/deploymentPolicies/{deploymentPolicyId}/partition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/partition")
    public Response getPartitionsOfPolicy(@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {

        return Response.ok().entity(StratosApiV41Utils.getPartitionsOfDeploymentPolicy(deploymentPolicyId)).build();
    }

    @GET
    @Path("/autoscalePolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/autoscalingPolicy")
    public Response getAutoscalingPolicies() throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getAutoScalePolicies()).build();
    }

    @POST
    @Path("/autoscalePolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/autoscalingPolicy")
    public Response deployAutoscalingPolicyDefintion(AutoscalePolicy autoscalePolicy)
            throws RestAPIException {

        StratosApiV41Utils.deployAutoscalingPolicy(autoscalePolicy);
        URI url = uriInfo.getAbsolutePathBuilder().path(autoscalePolicy.getId()).build();
        return Response.created(url).build();
    }

    @PUT
    @Path("/autoscalePolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/autoscalingPolicy")
    public Response updateAutoscalingPolicyDefintion(AutoscalePolicy autoscalePolicy)
            throws RestAPIException {

        StratosApiV41Utils.updateAutoscalingPolicy(autoscalePolicy);
        return Response.ok().build();
    }

    @GET
    @Path("/autoscalePolicies/{autoscalePolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/autoscalingPolicy")
    public Response getAutoscalingPolicy(@PathParam("autoscalePolicyId") String autoscalePolicyId)
            throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getAutoScalePolicy(autoscalePolicyId)).build();
    }

    @GET
    @Path("/applications")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getApplications() throws RestAPIException {
        ApplicationBean[] applications = StratosApiV41Utils.getApplications();
        if (applications == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok().entity(applications).build();
        }
    }

    /**
     * This API resource provides information about the application denoted by the given appId. Details includes,
     * Application details, top level cluster details, details of the group and sub groups.
     *
     * @param applicationId Id of the application.
     * @return Json representing the application details with 200 as HTTP status. HTTP 404 is returned when there is
     * no application with given Id.
     * @throws RestAPIException is thrown in case of failure occurs.
     */

    @GET
    @Path("/applications/{applicationId}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getApplicationInfo(@PathParam("applicationId") String applicationId) throws RestAPIException {
        ApplicationBean application = StratosApiV41Utils.getApplicationInfo(applicationId);
        if (application == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok().entity(application).build();
        }
    }
    
    @POST
    @Path("/applications")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response deployApplicationDefinition(ApplicationDefinition applicationDefinitionBean)
            throws RestAPIException {
        StratosApiV41Utils.deployApplicationDefinition(applicationDefinitionBean, getConfigContext(),
                getUsername(), getTenantDomain());
        URI url = uriInfo.getAbsolutePathBuilder().path(applicationDefinitionBean.getApplicationId()).build();
        return Response.created(url).build();
    }

    @DELETE
    @Path("/applications/{applicationId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response unDeployApplicationDefinition(@PathParam("applicationId") String applicationId)
            throws RestAPIException {
        StratosApiV41Utils.unDeployApplication(applicationId, getConfigContext(), getUsername(),
                getTenantDomain());
        return Response.noContent().build();
    }

    @GET
    @Path("/subscriptions/{applicationId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getSubscriptionsOfApplication(@PathParam("applicationId") String applicationId) throws RestAPIException {
        ApplicationSubscription subscriptions = StratosApiV41Utils.getApplicationSubscriptions(applicationId, getConfigContext());
        if (subscriptions == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok().entity(subscriptions).build();
    }
    
    @GET
    @Path("/subscriptions/cartridges/groups/{serviceGroup}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getSubscribedCartridgesOfServiceGroup(@PathParam("serviceGroup") String serviceGroup) throws RestAPIException {
        List<Cartridge> cartridgeList = StratosApiV41Utils.getSubscriptions(null, serviceGroup, getConfigContext());
        // Following is very important when working with axis2
        ResponseBuilder rb = Response.ok();
        rb.entity(cartridgeList.isEmpty() ? new Cartridge[0] : cartridgeList.toArray(new Cartridge[cartridgeList.size()]));
        return rb.build();
    }

    @GET
    @Path("/subscriptions/{subscriptionAlias}/cartridges")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getSubscribedCartridgeInfo(@PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {
        ResponseBuilder rb = Response.ok();
        rb.entity(StratosApiV41Utils.getSubscription(subscriptionAlias, getConfigContext()));
        return rb.build();
    }

    @GET
    @Path("/subscriptions/{subscriptionAlias}/cartridges/{cartridgeType}/active")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/instance")
    public Response getActiveMembersCountOfSubscription(@PathParam("cartridgeType") String cartridgeType,
                                                        @PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {
        ResponseBuilder rb = Response.ok();
        rb.entity(StratosApiV41Utils.getActiveInstances(cartridgeType, subscriptionAlias, getConfigContext()));
        return rb.build();
    }
    
    @PUT
    @Path("/subscriptions/{subscriptionAlias}/properties")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/subscription")
    public Response updateSubscriptionProperties(@PathParam("subscriptionAlias") String alias, CartridgeInfoBean cartridgeInfoBean) throws RestAPIException {
        if (cartridgeInfoBean == null) {
            Response.notModified().build();
        }
        StratosApiV41Utils.updateSubscriptionProperties(getConfigContext(), alias, cartridgeInfoBean.getProperty());
        return Response.ok().build();
    }
    
    @POST
    @Path("/subscriptions/{subscriptionAlias}/domains")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/domain")
    public Response addSubscriptionDomains(@PathParam("subscriptionAlias") String subscriptionAlias,
                                           SubscriptionDomainRequest request) throws RestAPIException {
        StratosApiV41Utils.addSubscriptionDomains(getConfigContext(), subscriptionAlias, request);
        return Response.noContent().build();
    }

    @GET
    @Path("/subscriptions/{subscriptionAlias}/domains")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/domain")
    public Response getSubscriptionDomains(@PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {

        SubscriptionDomainBean[] subscriptionDomainBean = StratosApiV41Utils.getSubscriptionDomains(getConfigContext(), subscriptionAlias).toArray(new SubscriptionDomainBean[0]);

        if (subscriptionDomainBean.length == 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok().entity(subscriptionDomainBean).build();
        }
    }

    @GET
    @Path("/subscriptions/{subscriptionAlias}/domains/{domainName}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/domain")
    public Response getSubscriptionDomain(@PathParam("subscriptionAlias") String subscriptionAlias, @PathParam("domainName") String domainName) throws RestAPIException {

        SubscriptionDomainBean subscriptionDomainBean = StratosApiV41Utils.getSubscriptionDomain(getConfigContext(), subscriptionAlias, domainName);
        if (subscriptionDomainBean.domainName == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok().entity(subscriptionDomainBean).build();
        }
    }
    
    @DELETE
    @Path("/subscriptions/{subscriptionAlias}/domains/{domainName}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/domain")
    public Response removeSubscriptionDomain(@PathParam("subscriptionAlias") String subscriptionAlias,
                                             @PathParam("domainName") String domainName) throws RestAPIException {
        try {
            StratosApiV41Utils.removeSubscriptionDomain(getConfigContext(), subscriptionAlias, domainName);
        } catch (DomainMappingExistsException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/clusters")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cluster")
    public Response getClustersOfTenant() throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getClustersForTenant(getConfigContext())).build();
    }

    @GET
    @Path("/clusters/{cartridgeType}/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cluster")
    public Response getClustersOfCartridge(@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {

        ResponseBuilder rb = Response.ok();
        rb.entity(StratosApiV41Utils.getClustersForTenantAndCartridgeType(getConfigContext(), cartridgeType));
        return rb.build();
    }

    @GET
    @Path("/clusters/{subscriptionAlias}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cluster")
    public Response getClusterOfSubscription(@PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {
        ResponseBuilder rb = Response.ok();
        rb.entity(StratosApiV41Utils.getCluster(subscriptionAlias, getConfigContext()));
        return rb.build();
    }

    @GET
    @Path("/clusters/{clusterId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cluster")
    public Response getCluster(@PathParam("clusterId") String clusterId) throws RestAPIException {
        Cluster cluster = null;
        if (log.isDebugEnabled()) {
            log.debug("Finding cluster for [id]: " + clusterId);
        }
        Cluster[] clusters = StratosApiV41Utils.getClustersForTenant(getConfigContext());
        if (log.isDebugEnabled()) {
            log.debug("Clusters retrieved from backend for cluster [id]: " + clusterId);
            for (Cluster c : clusters) {
                log.debug(c + "\n");
            }
        }
        for (Cluster clusterObj : clusters) {
            if (clusterObj.clusterId.equals(clusterId)) {
                cluster = clusterObj;
                break;
            }
        }
        return Response.ok().entity(cluster).build();
    }

    @GET
    @Path("/clusters/{subscriptionAlias}/loadBalancer")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cluster")
    public Response getLoadBalancerCluster(@PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("GET /subscription/%s/load-balancer-cluster", subscriptionAlias));
        }
        Cartridge subscription = StratosApiV41Utils.getSubscription(subscriptionAlias, getConfigContext());
        String lbClusterId = subscription.getLbClusterId();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Load balancer cluster-id found: %s", lbClusterId));
        }
        if (StringUtils.isNotBlank(lbClusterId)) {
            Response.fromResponse(getCluster(lbClusterId));
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Path("/tenants")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/modify/tenants")
    @SuperTenantService(true)
    public Response addTenant(TenantInfoBean tenantInfoBean) throws RestAPIException {
        try {
            CommonUtil.validateEmail(tenantInfoBean.getEmail());
        } catch (Exception e) {
            String msg = "Invalid email is provided";
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
            log.error("Security alert! User registry is null. A user is trying create a tenant "
                    + " without an authenticated session.");
            throw new RestAPIException("Invalid data"); // obscure error message.
        }

        if (userRegistry.getTenantId() != MultitenantConstants.SUPER_TENANT_ID) {
            log.error("Security alert! None super tenant trying to create a tenant.");
            throw new RestAPIException("Invalid data"); // obscure error message.
        }

        Tenant tenant = TenantMgtUtil.initializeTenant(tenantInfoBean);
        TenantPersistor persistor = ServiceHolder.getTenantPersistor();
        // not validating the domain ownership, since created by super tenant
        int tenantId = 0; //TODO verify whether this is the correct approach (isSkeleton)
        try {
            tenantId = persistor.persistTenant(tenant, false, tenantInfoBean.getSuccessKey(),
                    tenantInfoBean.getOriginatedService(), false);
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

        URI url = uriInfo.getAbsolutePathBuilder().path(tenant.getDomain()).build();
        return Response.created(url).build();
    }

    @PUT
    @Path("/tenants")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/modify/tenants")
    @SuperTenantService(true)
    public Response updateTenant(TenantInfoBean tenantInfoBean) throws RestAPIException {

        try {
            updateExistingTenant(tenantInfoBean);

        } catch (TenantNotFoundException ex) {
            Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
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
            throw new TenantNotFoundException(msg, e);
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
    @Path("/tenants/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean getTenantByDomain(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {

        try {
            return getTenantForDomain(tenantDomain);
        } catch (Exception e) {
            String msg = "Error in getting tenant information for tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }

    private TenantInfoBean getTenantForDomain(String tenantDomain) throws Exception {

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

        if (activePlan != null && activePlan.trim().length() > 0) {
            bean.setUsagePlan(activePlan);
        } else {
            bean.setUsagePlan("");
        }

        return bean;
    }

    @DELETE
    @Path("/tenants/{tenantDomain}")
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
    @Path("/tenants")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean[] getTenants() throws RestAPIException {
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

    @GET
    @Path("/tenants/search/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean[] getPartialSearchTenants(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {
        List<TenantInfoBean> tenantList = null;
        try {
            tenantList = searchPartialTenantsDomains(tenantDomain);
        } catch (Exception e) {
            String msg = "Error in getting information for tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        return tenantList.toArray(new TenantInfoBean[tenantList.size()]);
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
    @Path("/tenants/activate/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/modify/tenants")
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
        }

        try {
            TenantMgtUtil.activateTenant(tenantDomain, tenantManager, tenantId);

        } catch (Exception e) {
            throw new RestAPIException(e);
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
    @Path("/tenants/deactivate/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/modify/tenants")
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

        }

        try {
            TenantMgtUtil.deactivateTenant(tenantDomain, tenantManager, tenantId);
        } catch (Exception e) {
            throw new RestAPIException(e);
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
    @Path("/repo/notify")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/sync")
    public Response notifyRepository(Payload payload) throws RestAPIException {

        StratosApiV41Utils.getGitRepositoryNotification(payload);
        return Response.noContent().build();
    }

    @POST
    @Path("/repo/synchronize/{subscriptionAlias}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/sync")
    public Response synchronizeRepositoryOfSubscription(@PathParam("subscriptionAlias") String alias) throws RestAPIException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Synchronizing Git repository for alias '%s'", alias));
        }
        CartridgeSubscription cartridgeSubscription = StratosApiV41Utils.getCartridgeSubscription(alias, getConfigContext());
        if (cartridgeSubscription != null && cartridgeSubscription.getRepository() != null && log.isDebugEnabled()) {
            log.debug(String.format("Found subscription for '%s'. Git repository: %s", alias, cartridgeSubscription
                    .getRepository().getUrl()));
        }
        StratosApiV41Utils.synchronizeRepository(cartridgeSubscription);
        return Response.noContent().build();
    }

    @POST
    @Path("/users")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/admin/manage/add/users")
    public Response addUser(UserInfoBean userInfoBean) throws RestAPIException {

        StratosUserManager stratosUserManager = new StratosUserManager();

        try {
            stratosUserManager.addUser(getTenantUserStoreManager(), userInfoBean);

        } catch (UserManagerException e) {
            throw new RestAPIException(e.getMessage());
        }
        log.info("Successfully added an user with Username " + userInfoBean.getUserName());
        URI url = uriInfo.getAbsolutePathBuilder().path(userInfoBean.getUserName()).build();
        return Response.created(url).build();
    }

    @DELETE
    @Path("/users/{userName}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/admin/manage/add/users")
    public Response deleteUser(@PathParam("userName") String userName) throws RestAPIException {

        StratosUserManager stratosUserManager = new StratosUserManager();

        try {
            stratosUserManager.deleteUser(getTenantUserStoreManager(), userName);

        } catch (UserManagerException e) {
            throw new RestAPIException(e.getMessage());
        }
        log.info("Successfully deleted an user with Username " + userName);
        return Response.noContent().build();
    }

    @PUT
    @Path("/users")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/admin/manage/add/users")
    public Response updateUser(UserInfoBean userInfoBean) throws RestAPIException {

        StratosUserManager stratosUserManager = new StratosUserManager();

        try {
            stratosUserManager.updateUser(getTenantUserStoreManager(), userInfoBean);

        } catch (UserManagerException e) {
            throw new RestAPIException(e.getMessage());
        }

        log.info("Successfully updated an user with Username " + userInfoBean.getUserName());
        return Response.noContent().build();
    }

    @GET
    @Path("/users")
    @Produces("application/json")
    @AuthorizationAction("/permission/admin/manage/add/users")
    public UserInfoBean[] listUsers() throws RestAPIException {

        StratosUserManager stratosUserManager = new StratosUserManager();
        List<UserInfoBean> userList;

        try {
            userList = stratosUserManager.getAllUsers(getTenantUserStoreManager());

        } catch (UserManagerException e) {
            throw new RestAPIException(e.getMessage());
        }
        return userList.toArray(new UserInfoBean[userList.size()]);
    }

    /**
     * Get Tenant UserStoreManager
     *
     * @return UserStoreManager
     * @throws UserManagerException
     */
    private static UserStoreManager getTenantUserStoreManager() throws UserManagerException {

        CarbonContext carbonContext = CarbonContext.getThreadLocalCarbonContext();
        UserRealm userRealm;
        UserStoreManager userStoreManager;

        try {
            userRealm = carbonContext.getUserRealm();
            userStoreManager = userRealm.getUserStoreManager();

        } catch (UserStoreException e) {
            String msg = "Error in retrieving UserStore Manager";
            log.error(msg, e);
            throw new UserManagerException(msg, e);
        }

        return userStoreManager;
    }

    @POST
    @Path("/kubernetesCluster")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/kubernetes")
    public Response deployKubernetesHostCluster(KubernetesGroup kubernetesGroup) throws RestAPIException {

        StratosApiV41Utils.deployKubernetesGroup(kubernetesGroup);
        URI url = uriInfo.getAbsolutePathBuilder().path(kubernetesGroup.getGroupId()).build();
        return Response.created(url).build();
    }

    @PUT
    @Path("/kubernetesCluster/{kubernetesClusterId}/minion")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/kubernetes")
    public Response deployKubernetesHost(@PathParam("kubernetesClusterId") String kubernetesClusterId, KubernetesHost kubernetesHost)
            throws RestAPIException {

        StratosApiV41Utils.deployKubernetesHost(kubernetesClusterId, kubernetesHost);
        URI url = uriInfo.getAbsolutePathBuilder().path(kubernetesHost.getHostId()).build();
        return Response.created(url).build();
    }

    @PUT
    @Path("/kubernetesCluster/{kubernetesClusterId}/master")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/kubernetes")
    public Response updateKubernetesMaster(KubernetesMaster kubernetesMaster) throws RestAPIException {
        try {
            StratosApiV41Utils.updateKubernetesMaster(kubernetesMaster);
            URI url = uriInfo.getAbsolutePathBuilder().path(kubernetesMaster.getHostId()).build();
            return Response.created(url).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    //TODO: Check need for this method
    @PUT
    @Path("/kubernetes/update/host")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/kubernetes")
    public Response updateKubernetesHost(KubernetesHost kubernetesHost) throws RestAPIException {
        try {
            StratosApiV41Utils.updateKubernetesHost(kubernetesHost);
            URI url = uriInfo.getAbsolutePathBuilder().path(kubernetesHost.getHostId()).build();
            return Response.created(url).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/kubernetesCluster")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/kubernetes")
    public Response getKubernetesHostClusters() throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getAvailableKubernetesGroups()).build();
    }

    @GET
    @Path("/kubernetesCluster/{kubernetesClusterId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/kubernetes")
    public Response getKubernetesHostCluster(@PathParam("kubernetesClusterId") String kubernetesClusterId) throws RestAPIException {
        try {
            return Response.ok().entity(StratosApiV41Utils.getKubernetesGroup(kubernetesClusterId)).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/kubernetesCluster/{kubernetesClusterId}/hosts")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/kubernetes")
    public Response getKubernetesHostsOfKubernetesCluster(@PathParam("kubernetesClusterId") String kubernetesClusterId) throws RestAPIException {
        try {
            return Response.ok().entity(StratosApiV41Utils.getKubernetesHosts(kubernetesClusterId)).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/kubernetesCluster/{kubernetesClusterId}/master")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/kubernetes")
    public Response getKubernetesMasterOfKubernetesCluster(@PathParam("kubernetesClusterId") String kubernetesClusterId) throws RestAPIException {
        try {
            return Response.ok().entity(StratosApiV41Utils.getKubernetesMaster(kubernetesClusterId)).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @Path("/kubernetesCluster/{kubernetesClusterId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/kubernetes")
    public Response unDeployKubernetesHostCluster(@PathParam("kubernetesClusterId") String kubernetesClusterId) throws RestAPIException {
        try {
            StratosApiV41Utils.undeployKubernetesGroup(kubernetesClusterId);
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @DELETE
    @Path("/kubernetesCluster/{kubernetesClusterId}/hosts/{hostId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/kubernetes")
    public Response unDeployKubernetesHostOfKubernetesCluster(@PathParam("hostId") String kubernetesHostId) throws RestAPIException {
        try {
            StratosApiV41Utils.undeployKubernetesHost(kubernetesHostId);
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

}

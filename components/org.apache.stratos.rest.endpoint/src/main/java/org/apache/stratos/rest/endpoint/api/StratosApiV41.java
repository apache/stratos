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
import org.apache.stratos.manager.exception.ServiceDoesNotExistException;
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
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.ServiceDefinitionBean;
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
import java.util.UUID;

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
    @Path("/application/definition/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    // Grouping
    public Response deployApplicationDefinition(ApplicationDefinition applicationDefinitionBean)
            throws RestAPIException {
         StratosApiV41Utils.deployApplicationDefinition(applicationDefinitionBean, getConfigContext(),
                 getUsername(), getTenantDomain());
         URI url =  uriInfo.getAbsolutePathBuilder().path(applicationDefinitionBean.getApplicationId()).build();
         return Response.created(url).build();
    }

    
    @DELETE
    @Path("/application/definition/{applicationId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    // Grouping
    public Response unDeployApplicationDefinition(@PathParam("applicationId") String applicationId)
            throws RestAPIException {
        StratosApiV41Utils.unDeployApplication(applicationId, getConfigContext(), getUsername(),
                getTenantDomain());
        return Response.noContent().build();
    }
    

    @POST
    @Path("/cartridge/definition/")
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

    @DELETE
    @Path("/cartridge/definition/{cartridgeType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/cartridgeDefinition")
    public Response unDeployCartridgeDefinition(@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {
        StratosApiV41Utils.undeployCartridge(cartridgeType);
        return Response.noContent().build();
    }

    @POST
    @Path("/group/definition/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response deployServiceGroupDefinition (ServiceGroupDefinition serviceGroupDefinition)
            throws RestAPIException {
        StratosApiV41Utils.deployServiceGroupDefinition(serviceGroupDefinition);
        URI url =  uriInfo.getAbsolutePathBuilder().path(serviceGroupDefinition.getName()).build();
        return Response.created(url).build();
    }

    @GET
    @Path("/group/definition/{groupDefinitionName}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getServiceGroupDefinition (@PathParam("groupDefinitionName") String groupDefinitionName)
            throws RestAPIException {
        Response.ResponseBuilder rb = Response.ok().entity(StratosApiV41Utils.getServiceGroupDefinition(groupDefinitionName));
        return rb.build();
    }

    @DELETE
    @Path("/group/definition/{groupDefinitionName}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response undeployServiceGroupDefinition (@PathParam("groupDefinitionName") String groupDefinitionName)
            throws RestAPIException {

        StratosApiV41Utils.undeployServiceGroupDefinition(groupDefinitionName);
        return Response.noContent().build();
    }

    @POST
    @Path("/policy/deployment/partition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/partition")
    public Response deployPartition(Partition partition)
            throws RestAPIException {

        StratosApiV41Utils.deployPartition(partition);
        URI url = uriInfo.getAbsolutePathBuilder().path(partition.id).build();
        return Response.created(url).build();
    }

    @POST
    @Path("/policy/autoscale")
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
    @Path("/policy/autoscale")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/autoscalingPolicy")
    public Response updateAutoscalingPolicyDefintion(AutoscalePolicy autoscalePolicy)
            throws RestAPIException {

        StratosApiV41Utils.updateAutoscalingPolicy(autoscalePolicy);
        return Response.ok().build();
    }

    @POST
    @Path("/policy/deployment")
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
    @Path("/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/deploymentPolicy")
    public Response updateDeploymentPolicyDefinition(DeploymentPolicy deploymentPolicy)
            throws RestAPIException {

        StratosApiV41Utils.updateDeploymentPolicy(deploymentPolicy);
        return Response.ok().build();
    }

    @GET
    @Path("/partition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/partition")
    public Response getPartitions() throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getAvailablePartitions()).build();
    }

    @GET
    @Path("/partition/{partitionId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/partition")
    public Response getPartition(@PathParam("partitiotnId") String partitionId) throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getPartition(partitionId)).build();
    }

    @GET
    @Path("/partition/group/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/partition")
    public Response getPartitionGroups(@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getPartitionGroups(deploymentPolicyId)).build();
    }

    @GET
    @Path("/partition/{deploymentPolicyId}/{partitionGroupId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/partition")
    public Response getPartitions(@PathParam("deploymentPolicyId") String deploymentPolicyId,
                                  @PathParam("partitionGroupId") String partitionGroupId) throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getPartitionsOfGroup(deploymentPolicyId, partitionGroupId)).build();
    }

    @GET
    @Path("/partition/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/partition")
    public Response getPartitionsOfPolicy(@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {

        return Response.ok().entity(StratosApiV41Utils.getPartitionsOfDeploymentPolicy(deploymentPolicyId)).build();
    }

    @GET
    @Path("/policy/autoscale")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/autoscalingPolicy")
    public Response getAutoscalePolicies() throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getAutoScalePolicies()).build();
    }

    @GET
    @Path("/policy/autoscale/{autoscalePolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/autoscalingPolicy")
    public Response getAutoscalePolicies(@PathParam("autoscalePolicyId") String autoscalePolicyId)
            throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getAutoScalePolicy(autoscalePolicyId)).build();
    }

    @GET
    @Path("/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/deploymentPolicy")
    public Response getDeploymentPolicies() throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getDeploymentPolicies()).build();
    }

    @GET
    @Path("/policy/deployment/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/deploymentPolicy")
    public Response getDeploymentPolicies(@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getDeploymentPolicy(deploymentPolicyId)).build();
    }

    @GET
    @Path("{cartridgeType}/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/deploymentPolicy")
    public Response getValidDeploymentPolicies(@PathParam("cartridgeType") String cartridgeType)
            throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getDeploymentPolicies(cartridgeType)).build();
    }

    @GET
    @Path("/cartridge/tenanted/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getAvailableMultiTenantCartridges() throws RestAPIException {
        List<Cartridge> cartridges = StratosApiV41Utils.getAvailableCartridges(null, true, getConfigContext());
        ResponseBuilder rb = Response.ok();
        rb.entity(cartridges.isEmpty() ? new Cartridge[0] : cartridges.toArray(new Cartridge[cartridges.size()]));
        return rb.build();
    }


    @GET
    @Path("/subsscriptions/{application_id}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getSubscriptionsOfApplication(@PathParam("application_id") String applicationId) throws RestAPIException {
        ApplicationSubscription subscriptions = StratosApiV41Utils.getApplicationSubscriptions(applicationId, getConfigContext());
        if(subscriptions  == null){
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return  Response.ok().entity(subscriptions).build();
    }

    @GET
    @Path("/application/")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getApplications() throws RestAPIException {
        ApplicationBean[] applications = StratosApiV41Utils.getApplications();
        if(applications == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }else{
            return  Response.ok().entity(applications).build();
        }
    }

    /**
     * This API resource provides information about the application denoted by the given appId. Details includes,
     * Application details, top level cluster details, details of the group and sub groups.
     * @param applicationId Id of the application.
     * @return Json representing the application details with 200 as HTTP status. HTTP 404 is returned when there is
     * no application with given Id.
     * @throws RestAPIException is thrown in case of failure occurs.
     */

    @GET
    @Path("/application/{appId}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getApplicationInfo(@PathParam("appId") String applicationId) throws RestAPIException {
        ApplicationBean application = StratosApiV41Utils.getApplicationInfo(applicationId);
        if(application == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }else{
            return  Response.ok().entity(application).build();
        }
    }

    @GET
    @Path("/cartridge/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getAvailableSingleTenantCartridges() throws RestAPIException {
        List<Cartridge> cartridges = StratosApiV41Utils.getAvailableCartridges(null, false, getConfigContext());
        ResponseBuilder rb = Response.ok();
        rb.entity(cartridges.isEmpty() ? new Cartridge[0] : cartridges.toArray(new Cartridge[cartridges.size()]));
        return rb.build();
    }

    @GET
    @Path("/cartridge/available/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getAvailableCartridges() throws RestAPIException {
        List<Cartridge> cartridges = StratosApiV41Utils.getAvailableCartridges(null, null, getConfigContext());
        ResponseBuilder rb = Response.ok();
        rb.entity(cartridges.isEmpty() ? new Cartridge[0] : cartridges.toArray(new Cartridge[cartridges.size()]));
        return rb.build();
    }

    @GET
    @Path("/cartridge/list/subscribed")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getSubscribedCartridges() throws RestAPIException {
        List<Cartridge> cartridgeList = StratosApiV41Utils.getSubscriptions(null, null, getConfigContext());
        // Following is very important when working with axis2
        ResponseBuilder rb = Response.ok();
        rb.entity(cartridgeList.isEmpty() ? new Cartridge[0] : cartridgeList.toArray(new Cartridge[cartridgeList.size()]));
        return rb.build();
    }

    @GET
    @Path("/cartridge/list/subscribed/group/{serviceGroup}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getSubscribedCartridgesForServiceGroup(@PathParam("serviceGroup") String serviceGroup) throws RestAPIException {
        List<Cartridge> cartridgeList = StratosApiV41Utils.getSubscriptions(null, serviceGroup, getConfigContext());
        // Following is very important when working with axis2
        ResponseBuilder rb = Response.ok();
        rb.entity(cartridgeList.isEmpty() ? new Cartridge[0] : cartridgeList.toArray(new Cartridge[cartridgeList.size()]));
        return rb.build();
    }

    @GET
    @Path("/cartridge/info/{subscriptionAlias}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getCartridgeInfo(@PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {
        ResponseBuilder rb = Response.ok();
        rb.entity(StratosApiV41Utils.getSubscription(subscriptionAlias, getConfigContext()));
        return rb.build();
    }

    @GET
    @Path("/cartridge/available/info/{cartridgeType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getAvailableSingleTenantCartridgeInfo(@PathParam("cartridgeType") String cartridgeType)
            throws RestAPIException {
        ResponseBuilder rb = Response.ok();
        rb.entity(StratosApiV41Utils.getAvailableCartridgeInfo(cartridgeType, null, getConfigContext()));
        return rb.build();
    }

    @GET
    @Path("/cartridge/lb")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getAvailableLbCartridges() throws RestAPIException {
        List<Cartridge> lbCartridges = StratosApiV41Utils.getAvailableLbCartridges(false, getConfigContext());
        return Response.ok().entity(lbCartridges.isEmpty() ? new Cartridge[0] : lbCartridges.toArray(new Cartridge[lbCartridges.size()])).build();
    }

    @GET
    @Path("/cartridge/active/{cartridgeType}/{subscriptionAlias}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/instance")
    public Response getActiveInstances(@PathParam("cartridgeType") String cartridgeType,
                                       @PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {
        ResponseBuilder rb = Response.ok();
        rb.entity(StratosApiV41Utils.getActiveInstances(cartridgeType, subscriptionAlias, getConfigContext()));
        return rb.build();
    }

//    @POST
//    @Path("/cartridge/subscribe")
//    @Produces("application/json")
//    @Consumes("application/json")
//    @AuthorizationAction("/permission/admin/manage/add/subscription")
//    public Response subscribe(CartridgeInfoBean cartridgeInfoBean) throws RestAPIException {
//
//        SubscriptionInfo subscriptionInfo = ServiceUtils.subscribe(cartridgeInfoBean,
//                getConfigContext(),
//                getUsername(),
//                getTenantDomain());
//        return Response.ok(subscriptionInfo).build();
//    }

    @GET
    @Path("/cluster/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cluster")
    public Response getClustersForTenant() throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getClustersForTenant(getConfigContext())).build();
    }

    @GET
    @Path("/cluster/{cartridgeType}/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cluster")
    public Response getClusters(@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {

        ResponseBuilder rb = Response.ok();
        rb.entity(StratosApiV41Utils.getClustersForTenantAndCartridgeType(getConfigContext(), cartridgeType));
        return rb.build();
    }

    @GET
    @Path("/cluster/service/{cartridgeType}/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cluster")
    public Response getServiceClusters(@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {
        ResponseBuilder rb = Response.ok();
        rb.entity(StratosApiV41Utils.getClustersForTenantAndCartridgeType(getConfigContext(), cartridgeType));
        return rb.build();
    }

    @GET
    @Path("/cluster/{cartridgeType}/{subscriptionAlias}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cluster")
    public Response getCluster(@PathParam("cartridgeType") String cartridgeType,
                               @PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException, RestAPIException {
        ResponseBuilder rb = Response.ok();
        rb.entity(StratosApiV41Utils.getCluster(cartridgeType, subscriptionAlias, getConfigContext()));
        return rb.build();
    }

    @GET
    @Path("/cluster/clusterId/{clusterId}")
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
    @Path("/tenant")
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
    @Path("/tenant")
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
    public TenantInfoBean[] retrievePartialSearchTenants(@PathParam("domain") String domain) throws RestAPIException {
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
    @Path("tenant/availability/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/modify/tenants")
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
    @Path("/service/definition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/service")
    public Response deployService(ServiceDefinitionBean serviceDefinitionBean)
            throws RestAPIException {

    	log.info("Service definition request.. : " + serviceDefinitionBean.getServiceName());
    	// super tenant Deploying service (MT) 
    	// here an alias is generated
       StratosApiV41Utils.deployService(serviceDefinitionBean.getCartridgeType(), UUID.randomUUID().toString(), serviceDefinitionBean.getAutoscalingPolicyName(),
               serviceDefinitionBean.getDeploymentPolicyName(), getTenantDomain(), getUsername(), getTenantId(),
               serviceDefinitionBean.getClusterDomain(), serviceDefinitionBean.getClusterSubDomain(),
               serviceDefinitionBean.getTenantRange(), serviceDefinitionBean.getIsPublic());

        URI url = uriInfo.getAbsolutePathBuilder().path(serviceDefinitionBean.getServiceName()).build();
        return Response.created(url).build();
    }

    @GET
    @Path("/service")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/service")
    public ServiceDefinitionBean[] getServices() throws RestAPIException {
        List<ServiceDefinitionBean> serviceDefinitionBeans = StratosApiV41Utils.getdeployedServiceInformation();
        return serviceDefinitionBeans == null || serviceDefinitionBeans.isEmpty() ? new ServiceDefinitionBean[0] :
                serviceDefinitionBeans.toArray(new ServiceDefinitionBean[serviceDefinitionBeans.size()]);
    }

    @GET
    @Path("/service/{serviceType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/service")
    public Response getService(@PathParam("serviceType") String serviceType) throws RestAPIException {
        ResponseBuilder rb;
        ServiceDefinitionBean serviceDefinitionBean = StratosApiV41Utils.getDeployedServiceInformation(serviceType);
        if (serviceDefinitionBean == null) {
            rb = Response.status(Response.Status.NOT_FOUND);
        } else {
            rb = Response.ok(serviceDefinitionBean);
        }
        return rb.build();
    }

    @GET
    @Path("/service/active")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/service")
    public List<Cartridge> getActiveService() throws RestAPIException {

        return StratosApiV41Utils.getActiveDeployedServiceInformation(getConfigContext());
    }

    @DELETE
    @Path("/service/definition/{serviceType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/service")
    public Response unDeployService(@PathParam("serviceType") String serviceType) throws RestAPIException {
        try {
            StratosApiV41Utils.undeployService(serviceType);
        } catch (ServiceDoesNotExistException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @POST
    @Path("/reponotification")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/sync")
    public Response getRepoNotification(Payload payload) throws RestAPIException {

        StratosApiV41Utils.getGitRepositoryNotification(payload);
        return Response.noContent().build();
    }

    @POST
    @Path("/cartridge/sync")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/sync")
    public Response synchronizeRepository(String alias) throws RestAPIException {
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
    @AuthorizationAction("/permission/admin/manage/add/domain")
    public Response addSubscriptionDomains(@PathParam("cartridgeType") String cartridgeType,

                                           @PathParam("subscriptionAlias") String subscriptionAlias,
                                           SubscriptionDomainRequest request) throws RestAPIException {
        StratosApiV41Utils.addSubscriptionDomains(getConfigContext(), cartridgeType, subscriptionAlias, request);
        return Response.noContent().build();
    }

    @GET
    @Path("/cartridge/{cartridgeType}/subscription/{subscriptionAlias}/domains")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/domain")
    public Response getSubscriptionDomains(@PathParam("cartridgeType") String cartridgeType, @PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {

        SubscriptionDomainBean[] subscriptionDomainBean = StratosApiV41Utils.getSubscriptionDomains(getConfigContext(), cartridgeType, subscriptionAlias).toArray(new SubscriptionDomainBean[0]);

        if (subscriptionDomainBean.length == 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok().entity(subscriptionDomainBean).build();
        }
    }

    @GET
    @Path("/cartridge/{cartridgeType}/subscription/{subscriptionAlias}/domains/{domainName}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/domain")
    public Response getSubscriptionDomain(@PathParam("cartridgeType") String cartridgeType, @PathParam("subscriptionAlias") String subscriptionAlias, @PathParam("domainName") String domainName) throws RestAPIException {

        SubscriptionDomainBean subscriptionDomainBean = StratosApiV41Utils.getSubscriptionDomain(getConfigContext(), cartridgeType, subscriptionAlias, domainName);
        if (subscriptionDomainBean.domainName == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok().entity(subscriptionDomainBean).build();
        }
    }

    @DELETE
    @Path("/cartridge/{cartridgeType}/subscription/{subscriptionAlias}/domains/{domainName}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/domain")
    public Response removeSubscriptionDomain(@PathParam("cartridgeType") String cartridgeType,
                                             @PathParam("subscriptionAlias") String subscriptionAlias,
                                             @PathParam("domainName") String domainName) throws RestAPIException {
        try {
            StratosApiV41Utils.removeSubscriptionDomain(getConfigContext(), cartridgeType, subscriptionAlias, domainName);
        } catch (DomainMappingExistsException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @GET
    @Path("/cartridge/{cartridgeType}/subscription/{subscriptionAlias}/load-balancer-cluster")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cluster")
    public Response getLoadBalancerCluster(@PathParam("cartridgeType") String cartridgeType,
                                           @PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("GET /cartridge/%s/subscription/%s/load-balancer-cluster", cartridgeType, subscriptionAlias));
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
    @Path("/user")
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
    @Path("/user/{userName}")
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
    @Path("/user")
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
    @Path("/user/list")
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
    @Path("/kubernetes/deploy/group")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/kubernetes")
    public Response deployKubernetesGroup(KubernetesGroup kubernetesGroup) throws RestAPIException {

        StratosApiV41Utils.deployKubernetesGroup(kubernetesGroup);
        URI url = uriInfo.getAbsolutePathBuilder().path(kubernetesGroup.getGroupId()).build();
        return Response.created(url).build();
    }

    @PUT
    @Path("/kubernetes/deploy/host/{kubernetesGroupId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/kubernetes")
    public Response deployKubernetesHost(@PathParam("kubernetesGroupId") String kubernetesGroupId, KubernetesHost kubernetesHost)
            throws RestAPIException {

        StratosApiV41Utils.deployKubernetesHost(kubernetesGroupId, kubernetesHost);
        URI url = uriInfo.getAbsolutePathBuilder().path(kubernetesHost.getHostId()).build();
        return Response.created(url).build();
    }

    @PUT
    @Path("/kubernetes/update/master")
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
    @Path("/kubernetes/group")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/kubernetes")
    public Response getKubernetesGroups() throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getAvailableKubernetesGroups()).build();
    }


    @GET
    @Path("/kubernetes/group/{kubernetesGroupId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/kubernetes")
    public Response getKubernetesGroup(@PathParam("kubernetesGroupId") String kubernetesGroupId) throws RestAPIException {
        try {
            return Response.ok().entity(StratosApiV41Utils.getKubernetesGroup(kubernetesGroupId)).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/kubernetes/hosts/{kubernetesGroupId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/kubernetes")
    public Response getKubernetesHosts(@PathParam("kubernetesGroupId") String kubernetesGroupId) throws RestAPIException {
        try {
            return Response.ok().entity(StratosApiV41Utils.getKubernetesHosts(kubernetesGroupId)).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/kubernetes/master/{kubernetesGroupId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/kubernetes")
    public Response getKubernetesMaster(@PathParam("kubernetesGroupId") String kubernetesGroupId) throws RestAPIException {
        try {
            return Response.ok().entity(StratosApiV41Utils.getKubernetesMaster(kubernetesGroupId)).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @Path("/kubernetes/group/{kubernetesGroupId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/kubernetes")
    public Response unDeployKubernetesGroup(@PathParam("kubernetesGroupId") String kubernetesGroupId) throws RestAPIException {
        try {
            StratosApiV41Utils.undeployKubernetesGroup(kubernetesGroupId);
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    @DELETE
    @Path("/kubernetes/host/{kubernetesHostId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/kubernetes")
    public Response unDeployKubernetesHost(@PathParam("kubernetesHostId") String kubernetesHostId) throws RestAPIException {
        try {
            StratosApiV41Utils.undeployKubernetesHost(kubernetesHostId);
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
    
}

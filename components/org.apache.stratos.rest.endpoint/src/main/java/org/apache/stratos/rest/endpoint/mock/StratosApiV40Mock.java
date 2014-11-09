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
package org.apache.stratos.rest.endpoint.mock;


import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.manager.dto.Cartridge;
import org.apache.stratos.manager.dto.SubscriptionInfo;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.user.mgt.beans.UserInfoBean;
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
import org.apache.stratos.rest.endpoint.bean.subscription.domain.SubscriptionDomainBean;
import org.apache.stratos.rest.endpoint.bean.topology.Cluster;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/")
public class StratosApiV40Mock {
    private static Log log = LogFactory.getLog(StratosApiV40Mock.class);
    @Context
    HttpServletRequest httpServletRequest;
    @Context
    UriInfo uriInfo;

    @POST
    @Path("/init")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public StratosApiResponse initialize ()
            throws RestAPIException {

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully logged in");
        return stratosApiResponse;
    }


    @GET
    @Path("/cookie")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getCookie(){
        HttpSession httpSession = httpServletRequest.getSession(true);//create session if not found
        String sessionId = httpSession.getId();
        return Response.ok().header("WWW-Authenticate", "Basic").type(MediaType.APPLICATION_JSON).
                entity(Utils.buildAuthenticationSuccessMessage(sessionId)).build();
    }

    @GET
    @Path("/cartridge/tenanted/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getAvailableMultiTenantCartridges() throws RestAPIException {
          return MockContext.getInstance().getAvailableMultiTenantCartridges();
    }

    @GET
    @Path("/cartridge/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getAvailableSingleTenantCartridges() throws RestAPIException {
         return MockContext.getInstance().getAvailableSingleTenantCartridges();
    }

    @GET
    @Path("/cartridge/available/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getAvailableCartridges() throws RestAPIException {
         return MockContext.getInstance().getAvailableCartridges();
    }

    @GET
    @Path("/cartridge/list/subscribed")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getSubscribedCartridges() throws RestAPIException {
         return MockContext.getInstance().getSubscribedCartridges();
    }

    @POST
    @Path("/cartridge/subscribe")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public SubscriptionInfo subscribe(CartridgeInfoBean cartridgeInfoBean) throws RestAPIException{
          return MockContext.getInstance().subscribeToCartridge(cartridgeInfoBean);
    }


    @GET
    @Path("/cartridge/info/{subscriptionAlias}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge getCartridgeInfo(@PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {
        return MockContext.getInstance().getCartridgeInfo(subscriptionAlias);
    }


    @POST
    @Path("/cartridge/unsubscribe")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public StratosApiResponse unsubscribe(String alias) throws RestAPIException{
          return MockContext.getInstance().unsubscribe(alias);
    }

    @POST
    @Path("/tenant")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse addTenant(TenantInfoBean tenantInfoBean) throws RestAPIException {
    	return MockContext.getInstance().addTenant(tenantInfoBean);
    }


    @PUT
    @Path("/tenant")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse updateTenant(TenantInfoBean tenantInfoBean) throws RestAPIException {
        return MockContext.getInstance().addTenant(tenantInfoBean);
    }

    @GET
    @Path("/tenant/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean getTenant(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {
        return MockContext.getInstance().getTenant(tenantDomain);
    }

    @DELETE
    @Path("/tenant/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse deleteTenant(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {
         return  MockContext.getInstance().deleteTenant(tenantDomain);
    }


    @GET
    @Path("/tenant/list")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean[] retrieveTenants() throws RestAPIException {
    	return MockContext.getInstance().getTenants();
    }

    @GET
    @Path("tenant/search/{domain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean[] retrievePartialSearchTenants(@PathParam("domain")String domain) throws RestAPIException {
            return MockContext.getInstance().retrievePartialSearchTenants(domain);
    }


    @POST
    @Path("tenant/activate/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void activateTenant(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {
            MockContext.getInstance().activateTenant(tenantDomain);
    }

    @POST
    @Path("tenant/deactivate/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse deactivateTenant(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {
        return  MockContext.getInstance().deactivateTenant(tenantDomain);
    }

   @POST
   @Path("/service/definition")
   @Produces("application/json")
   @Consumes("application/json")
   @AuthorizationAction("/permission/protected/manage/monitor/tenants")
   @SuperTenantService(true)
   public StratosApiResponse deployService(ServiceDefinitionBean serviceDefinitionBean)
           throws RestAPIException {

       log.info("Service definition request.. : " + serviceDefinitionBean.getServiceName());
       return MockContext.getInstance().deployService(serviceDefinitionBean);
   }
    @GET
    @Path("/service")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public ServiceDefinitionBean[] getServices () throws RestAPIException {
        return MockContext.getInstance().getServices();

    }

    @GET
    @Path("/service/{serviceType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public ServiceDefinitionBean getService(@PathParam("serviceType") String serviceType)throws RestAPIException {

        return MockContext.getInstance().getServiceType(serviceType);
    }

    @POST
    @Path("/cartridge/definition/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse deployCartridgeDefinition (CartridgeDefinitionBean cartridgeDefinitionBean)
            throws RestAPIException {
        return MockContext.getInstance().addCartirdgeDefinition(cartridgeDefinitionBean);
    }

    @DELETE
    @Path("/cartridge/definition/{cartridgeType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void unDeployCartridgeDefinition (@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {
         MockContext.getInstance().deleteCartridgeDefinition(cartridgeType);
    }

    @GET
    @Path("/cartridge/available/info/{cartridgeType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge getAvailableSingleTenantCartridgeInfo(@PathParam("cartridgeType") String cartridgeType)
            throws ADCException, RestAPIException {
        return MockContext.getInstance().getAvailableSingleTenantCartridgeInfo(cartridgeType);
    }

    @GET
    @Path("/cartridge/lb")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getAvailableLbCartridges() throws RestAPIException {
        return MockContext.getInstance().getAvailableLbCartridges();
    }



    @POST
    @Path("/policy/deployment/partition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse deployPartition (Partition partition)
            throws RestAPIException {
        return MockContext.getInstance().addPartition(partition);
    }

    @POST
    @Path("/policy/autoscale")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse deployAutoscalingPolicyDefintion (AutoscalePolicy autoscalePolicy)
            throws RestAPIException {
          return MockContext.getInstance().addAutoScalingPolicyDefinition(autoscalePolicy);

    }

    @POST
    @Path("/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public StratosApiResponse deployDeploymentPolicyDefinition (DeploymentPolicy deploymentPolicy)
            throws RestAPIException {
           return MockContext.getInstance().addDeploymentPolicyDefinition(deploymentPolicy);

    }

    @GET
    @Path("/partition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Partition[] getPartitions () throws RestAPIException {
            return MockContext.getInstance().getPartitions();

    }

    @GET
    @Path("/partition/{partitionId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Partition getPartition (@PathParam("partitionId") String partitionId) throws RestAPIException {
            return MockContext.getInstance().getPartition(partitionId);

    }

    @GET
    @Path("/partition/group/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public PartitionGroup[] getPartitionGroups (@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {
          return MockContext.getInstance().getPartitionGroups(deploymentPolicyId);

    }

    @GET
    @Path("/partition/{deploymentPolicyId}/{partitionGroupId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Partition[] getPartitions (@PathParam("deploymentPolicyId") String deploymentPolicyId,
                                       @PathParam("partitionGroupId") String partitionGroupId) throws RestAPIException {
           return MockContext.getInstance().getPartitions(deploymentPolicyId, partitionGroupId);

    }

    @GET
    @Path("/partition/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Partition [] getPartitionsOfPolicy (@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {
             return MockContext.getInstance().getPartitionsOfPolicy(deploymentPolicyId);

    }

    @GET
    @Path("/policy/autoscale")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public AutoscalePolicy[] getAutoscalePolicies () throws RestAPIException {
            return MockContext.getInstance().getAutoscalePolicies();

    }

    @GET
    @Path("/policy/autoscale/{autoscalePolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public AutoscalePolicy getAutoscalePolicies (@PathParam("autoscalePolicyId") String autoscalePolicyId)
            throws RestAPIException {
        return MockContext.getInstance().getAutoscalePolicies(autoscalePolicyId);

    }

    @GET
    @Path("/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public DeploymentPolicy[] getDeploymentPolicies () throws RestAPIException {
          return MockContext.getInstance().getDeploymentPolicies();

    }

    @GET
    @Path("/policy/deployment/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public DeploymentPolicy getDeploymentPolicies (@PathParam("deploymentPolicyId") String deploymentPolicyId)
            throws RestAPIException {
        return MockContext.getInstance().getDeploymentPolicies(deploymentPolicyId);

    }

    @GET
    @Path("/cluster/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cluster[] getClustersForTenant() throws RestAPIException {

        return MockContext.getInstance().getClusters();
    }

    @GET
    @Path("/cluster/{cartridgeType}/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cluster[] getClusters(@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {

        return MockContext.getInstance().getClusters();
    }

    @GET
    @Path("/cluster/{cartridgeType}/{subscriptionAlias}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cluster[] getCluster(@PathParam("cartridgeType") String cartridgeType,
                              @PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException, RestAPIException {

        return MockContext.getInstance().getClusters();
    }

    @GET
    @Path("/cluster/clusterId/{clusterId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cluster[] getCluster(@PathParam("clusterId") String clusterId) throws RestAPIException {
        return MockContext.getInstance().getClusters();
    }

    @GET
    @Path("{cartridgeType}/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public DeploymentPolicy[] getValidDeploymentPolicies (@PathParam("cartridgeType") String cartridgeType)
            throws RestAPIException {

        return MockContext.getInstance().getDeploymentPoliciesForCartridgeType(cartridgeType);
    }

    @POST
    @Path("/cartridge/{cartridgeType}/subscription/{subscriptionAlias}/domains")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public StratosApiResponse addSubscriptionDomains(@PathParam("cartridgeType") String cartridgeType,
                                                      @PathParam("subscriptionAlias") String subscriptionAlias,
                                                      SubscriptionDomainRequest request) throws RestAPIException {

        return MockContext.getInstance().addSubscriptionDomains(2, subscriptionAlias, request);
    }

    @GET
    @Path("/cartridge/{cartridgeType}/subscription/{subscriptionAlias}/domains")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public SubscriptionDomainBean[] getSubscriptionDomains(@PathParam("cartridgeType") String cartridgeType,
                                           @PathParam("subscriptionAlias") String subscriptionAlias) throws RestAPIException {

        return MockContext.getInstance().getSubscriptionDomains(2, subscriptionAlias).toArray(new SubscriptionDomainBean[0]);
    }

    @Path("/cartridge/{cartridgeType}/subscription/{subscriptionAlias}/domains/{domainName}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public SubscriptionDomainBean getSubscriptionDomain(@PathParam("cartridgeType") String cartridgeType,
            @PathParam("subscriptionAlias") String subscriptionAlias, @PathParam("domainName") String domainName) throws RestAPIException {

        return MockContext.getInstance().getSubscriptionDomain(2, cartridgeType, subscriptionAlias, domainName);
    }

    @DELETE
    @Path("/cartridge/{cartridgeType}/subscription/{subscriptionAlias}/domains/{domainName}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public StratosApiResponse removeSubscriptionDomain(@PathParam("cartridgeType") String cartridgeType,
                                                         @PathParam("subscriptionAlias") String subscriptionAlias,
                                                         @PathParam("domainName") String domainName) throws RestAPIException {

        return MockContext.getInstance().removeSubscriptionDomain(2, subscriptionAlias, domainName);
    }
    
    @POST
	@Path("/cartridge/sync")
	@Consumes("application/json")
	@AuthorizationAction("/permission/protected/manage/monitor/tenants")
	public Response synchronizeRepository(String alias) throws RestAPIException {
		return Response.noContent().build();
	}
    
    @POST
    @Path("/user")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/admin/manage/add/users")
    public Response addUser(UserInfoBean userInfoBean) throws RestAPIException {
    	MockContext.getInstance().addUser(userInfoBean);
        URI url = uriInfo.getAbsolutePathBuilder().path(userInfoBean.getUserName()).build();
        return Response.created(url).build();
    }

    @DELETE
    @Path("/user/{userName}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/admin/manage/add/users")
    public Response deleteUser(@PathParam("userName") String userName) throws RestAPIException {
    	MockContext.getInstance().deleteUser(userName);
        return Response.noContent().build();
    }

    @PUT
    @Path("/user")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/admin/manage/add/users")
    public Response updateUser(UserInfoBean userInfoBean) throws RestAPIException {
    	MockContext.getInstance().updateUser(userInfoBean);
        URI url = uriInfo.getAbsolutePathBuilder().path(userInfoBean.getUserName()).build();
        return Response.created(url).build();
    }

    @GET
    @Path("/user/list")
    @Produces("application/json")
    @AuthorizationAction("/permission/admin/manage/add/users")
    public UserInfoBean[] retrieveUsers() throws RestAPIException {
        List<UserInfoBean> userList = null;
        userList = MockContext.getInstance().getAllUsers();
        return userList.toArray(new UserInfoBean[userList.size()]);
    }

}

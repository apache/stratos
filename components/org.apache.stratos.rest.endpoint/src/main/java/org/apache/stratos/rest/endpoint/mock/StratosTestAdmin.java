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

import org.apache.catalina.tribes.membership.McastService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.manager.dto.Cartridge;
import org.apache.stratos.manager.dto.SubscriptionInfo;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.rest.endpoint.Utils;
import org.apache.stratos.rest.endpoint.annotation.AuthorizationAction;
import org.apache.stratos.rest.endpoint.annotation.SuperTenantService;
import org.apache.stratos.rest.endpoint.bean.CartridgeInfoBean;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.PartitionGroup;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/admin/")
public class StratosTestAdmin {
    private static Log log = LogFactory.getLog(StratosTestAdmin.class);
    @Context
    HttpServletRequest httpServletRequest;

    
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
    public Cartridge[] getAvailableMultiTenantCartridges() throws ADCException {
          return MockContext.getInstance().getAvailableMultiTenantCartridges();
    }

    @GET
    @Path("/cartridge/list")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getAvailableSingleTenantCartridges() throws ADCException {
         return MockContext.getInstance().getAvailableSingleTenantCartridges();
    }

    @GET
    @Path("/cartridge/list/subscribed")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Cartridge[] getSubscribedCartridges() throws ADCException {
         return MockContext.getInstance().getSubscribedCartridges();
    }

    @POST
    @Path("/cartridge/subscribe")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public SubscriptionInfo subscribe(CartridgeInfoBean cartridgeInfoBean) {
          return MockContext.getInstance().subscribeToCartridge(cartridgeInfoBean);
    }





    @POST
    @Path("/cartridge/unsubscribe")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public void unsubscribe(String alias){
          MockContext.getInstance().unsubscribe(alias);
    }

    @POST
    @Path("/tenant")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public String addTenant(TenantInfoBean tenantInfoBean) throws Exception {
         MockContext.getInstance().addTenant(tenantInfoBean);
         return "tenant added successfully...";  // why this string return type ??
    }


    @PUT
    @Path("/tenant")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void updateTenant(TenantInfoBean tenantInfoBean) throws Exception {
        MockContext.getInstance().addTenant(tenantInfoBean);
    }

    @GET
    @Path("/tenant/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean getTenant(@PathParam("tenantDomain") String tenantDomain) throws Exception {
        return MockContext.getInstance().getTenant(tenantDomain);
    }

    @DELETE
    @Path("/tenant/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void deleteTenant(@PathParam("tenantDomain") String tenantDomain) throws Exception {
         MockContext.getInstance().deleteTenant(tenantDomain);
    }


    @GET
    @Path("/tenant/list")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean[] retrieveTenants() throws Exception {
           return MockContext.getInstance().getTenants();
    }

    @GET
    @Path("tenant/search/{domain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public TenantInfoBean[] retrievePartialSearchTenants(@PathParam("domain")String domain) throws Exception {
            return MockContext.getInstance().retrievePartialSearchTenants(domain);
    }


    @POST
    @Path("tenant/activate/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void activateTenant(@PathParam("tenantDomain") String tenantDomain) throws Exception {
            MockContext.getInstance().activateTenant(tenantDomain);
    }

    @POST
    @Path("tenant/deactivate/{tenantDomain}")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void deactivateTenant(@PathParam("tenantDomain") String tenantDomain) throws Exception {
        MockContext.getInstance().deactivateTenant(tenantDomain);
    }

    @POST
    @Path("/cartridge/definition/")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public void deployCartridgeDefinition (CartridgeDefinitionBean cartridgeDefinitionBean)
            throws RestAPIException {
        MockContext.getInstance().addCartirdgeDefinition(cartridgeDefinitionBean);
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

    @POST
    @Path("/policy/deployment/partition")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public boolean deployPartition (Partition partition)
            throws RestAPIException {
        return MockContext.getInstance().addPartition(partition);
    }

    @POST
    @Path("/policy/autoscale")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public boolean deployAutoscalingPolicyDefintion (AutoscalePolicy autoscalePolicy)
            throws RestAPIException {
          return MockContext.getInstance().addAutoScalingPolicyDefinition(autoscalePolicy);

    }

    @POST
    @Path("/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public boolean deployDeploymentPolicyDefinition (DeploymentPolicy deploymentPolicy)
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
           return MockContext.getInstance().getPartitions(deploymentPolicyId,partitionGroupId);

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

   /* @GET
    @Path("{cartridgeType}/policy/deployment")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public DeploymentPolicy[] getValidDeploymentPolicies (@PathParam("cartridgeType") String cartridgeType)
            throws RestAPIException {
           return   MockContext.getInstance().getValidDeploymentPolicies(cartridgeType);

    }*/



}

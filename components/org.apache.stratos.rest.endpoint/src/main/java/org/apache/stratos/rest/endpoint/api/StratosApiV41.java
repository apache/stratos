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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.*;
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.common.beans.application.ApplicationNetworkPartitionIdListBean;
import org.apache.stratos.common.beans.application.GroupBean;
import org.apache.stratos.common.beans.application.domain.mapping.ApplicationDomainMappingsBean;
import org.apache.stratos.common.beans.application.domain.mapping.DomainMappingBean;
import org.apache.stratos.common.beans.application.signup.ApplicationSignUpBean;
import org.apache.stratos.common.beans.artifact.repository.GitNotificationPayloadBean;
import org.apache.stratos.common.beans.cartridge.CartridgeBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesClusterBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesHostBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesMasterBean;
import org.apache.stratos.common.beans.partition.NetworkPartitionBean;
import org.apache.stratos.common.beans.policy.autoscale.AutoscalePolicyBean;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.common.beans.policy.deployment.DeploymentPolicyBean;
import org.apache.stratos.common.beans.topology.ApplicationInfoBean;
import org.apache.stratos.common.beans.topology.ClusterBean;
import org.apache.stratos.rest.endpoint.Utils;
import org.apache.stratos.rest.endpoint.annotation.AuthorizationAction;
import org.apache.stratos.rest.endpoint.annotation.SuperTenantService;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;
import org.apache.stratos.rest.endpoint.exception.TenantNotFoundException;
import org.wso2.carbon.context.PrivilegedCarbonContext;

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

    /**
     * This method is used by clients such as the CLI to verify the Stratos manager URL.
     *
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/init")
    @AuthorizationAction("/permission/admin/restlogin")
    public Response initialize()
            throws RestAPIException {
        ApiResponseBean response = new ApiResponseBean();
        response.setMessage("Successfully authenticated");
        return Response.ok(response).build();
    }

    /**
     * This method gets called by the client who are interested in using session mechanism to authenticate
     * themselves in subsequent calls. This method call get authenticated by the basic authenticator.
     * Once the authenticated call received, the method creates a session and returns the session id.
     *
     * @return The session id related with the session
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

    /**
     * Creates the cartridge definition.
     *
     * @param cartridgeDefinitionBean the cartridge definition bean
     * @return 201 if cartridge is successfully created, 409 if cartridge already exists.
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/cartridges")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/addCartridge")
    public Response addCartridge(
            CartridgeBean cartridgeDefinitionBean) throws RestAPIException {

        String cartridgeType = cartridgeDefinitionBean.getType();
        CartridgeBean cartridgeBean = StratosApiV41Utils.getCartridgeForValidate(cartridgeType);
        if (cartridgeBean != null) {
            String msg = String.format("Cartridge already exists: [cartridge-type] %s", cartridgeType);
            log.warn(msg);
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponseBean(Response.Status.CONFLICT.getStatusCode(), msg)).build();
        }

        StratosApiV41Utils.addCartridge(cartridgeDefinitionBean);
        URI url = uriInfo.getAbsolutePathBuilder().path(cartridgeType).build();
        return Response.created(url).entity(new SuccessResponseBean(Response.Status.CREATED.getStatusCode(),
                String.format("Cartridge added successfully: [cartridge-type] %s", cartridgeType))).build();
    }

    /**
     * Creates the Deployment Policy Definition.
     *
     * @param deploymentPolicyDefinitionBean the deployment policy bean
     * @return 201 if deployment policy is successfully added
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/deploymentPolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/addDeploymentPolicy")
    public Response addDeploymentPolicy(
            DeploymentPolicyBean deploymentPolicyDefinitionBean) throws RestAPIException {

        try {
            String deploymentPolicyID = deploymentPolicyDefinitionBean.getId();
            // TODO :: Deployment policy validation
            StratosApiV41Utils.addDeploymentPolicy(deploymentPolicyDefinitionBean);
            URI url = uriInfo.getAbsolutePathBuilder().path(deploymentPolicyID).build();
            return Response.created(url).entity(new SuccessResponseBean(Response.Status.CREATED.getStatusCode(),
                    String.format("Deployment policy added successfully: " + "[deployment-policy-id] %s",
                            deploymentPolicyID))).build();
        } catch (RestAPIException e) {
            if (e.getCause().getMessage().contains("already exists")) {
                return Response.status(Response.Status.CONFLICT).build();
            } else {
                throw e;
            }
        }
    }

    /**
     * Get deployment policy by deployment policy id
     *
     * @return 200 if deployment policy is found, 404 if not
     * @throws RestAPIException
     */
    @GET
    @Path("/deploymentPolicies/{deploymentPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/getDeploymentPolicy")
    public Response getDeploymentPolicy(
            @PathParam("deploymentPolicyId") String deploymentPolicyId) throws RestAPIException {
        DeploymentPolicyBean deploymentPolicyBean = StratosApiV41Utils.getDeployementPolicy(deploymentPolicyId);
        if (deploymentPolicyBean == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(deploymentPolicyBean).build();
    }

    /**
     * Get deployment policies
     *
     * @return 200 with the list of deployment policies
     * @throws RestAPIException
     */
    @GET
    @Path("/deploymentPolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/getDeploymentPolicy")
    public Response getDeploymentPolicies()
            throws RestAPIException {
        DeploymentPolicyBean[] deploymentPolicies = StratosApiV41Utils.getDeployementPolicies();
        if (deploymentPolicies == null || deploymentPolicies.length == 0){
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(deploymentPolicies).build();
    }

    /**
     * Updates the Deployment Policy Definition.
     *
     * @param deploymentPolicyDefinitionBean the deployment policy bean
     * @return 200 if deployment policy is successfully updated
     * @throws RestAPIException the rest api exception
     */
    @PUT
    @Path("/deploymentPolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/updateDeploymentPolicy")
    public Response updateDeploymentPolicy(
            DeploymentPolicyBean deploymentPolicyDefinitionBean) throws RestAPIException {

        String deploymentPolicyID = deploymentPolicyDefinitionBean.getId();
        // TODO :: Deployment policy validation

        StratosApiV41Utils.updateDeploymentPolicy(deploymentPolicyDefinitionBean);
        URI url = uriInfo.getAbsolutePathBuilder().path(deploymentPolicyID).build();
        return Response.ok(url).entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Deployment policy updated successfully: " + "[deployment-policy-id] %s",
                        deploymentPolicyID))).build();
    }

    /**
     * Updates the Deployment Policy Definition.
     *
     * @param deploymentPolicyID the deployment policy id
     * @return 200 if deployment policy is successfully removed
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/deploymentPolicies/{depolymentPolicyID}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/removeDeploymentPolicy")
    public Response removeDeploymentPolicy(
            @PathParam("depolymentPolicyID") String deploymentPolicyID) throws RestAPIException {

        StratosApiV41Utils.removeDeploymentPolicy(deploymentPolicyID);
        URI url = uriInfo.getAbsolutePathBuilder().path(deploymentPolicyID).build();
        return Response.ok(url).entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Deployment policy removed successfully: " + "[deployment-policy-id] %s",
                        deploymentPolicyID))).build();
    }

    /**
     * Updates the cartridge definition.
     *
     * @param cartridgeDefinitionBean the cartridge definition bean
     * @return 201 if cartridge successfully updated
     * @throws RestAPIException the rest api exception
     */
    @PUT
    @Path("/cartridges")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/updateCartridge")
    public Response updateCartridge(
            CartridgeBean cartridgeDefinitionBean) throws RestAPIException {
        StratosApiV41Utils.updateCartridge(cartridgeDefinitionBean);
        URI url = uriInfo.getAbsolutePathBuilder().path(cartridgeDefinitionBean.getType()).build();
        return Response.created(url)
                .entity(new SuccessResponseBean(Response.Status.CREATED.getStatusCode(), "Cartridge updated successfully"))
                .build();

    }

    /**
     * Gets all available cartridges.
     *
     * @return 200 if cartridges exists, 404 if no cartridges found
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/cartridges")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/getCartridge")
    public Response getCartridges()
            throws RestAPIException {
        List<CartridgeBean> cartridges = StratosApiV41Utils.getAvailableCartridges(null, null, getConfigContext());
        if (cartridges == null ||  cartridges.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        CartridgeBean[] cartridgeArray = cartridges.toArray(new CartridgeBean[cartridges.size()]);
        return Response.ok().entity(cartridgeArray).build();
    }

    /**
     * Gets a single cartridge by type
     * @param cartridgeType Cartridge type
     * @return 200 if specified cartridge exists, 404 if not
     * @throws RestAPIException
     */
    @GET
    @Path("/cartridges/{cartridgeType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/getCartridge")
    public Response getCartridge(
            @PathParam("cartridgeType") String cartridgeType) throws RestAPIException {
        CartridgeBean cartridge;
        try {
            cartridge = StratosApiV41Utils.getCartridge(cartridgeType);
            return Response.ok().entity(cartridge).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * Returns cartridges by category.
     *
     * @param filter Filter
     * @param criteria Criteria
     * @return 200 if cartridges are found for specified filter, 404 if none found
     * @throws RestAPIException
     */
    @GET
    @Path("/cartridges/filter/{filter}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/getCartridgesByFilter")
    public Response getCartridgesByFilter(
            @DefaultValue("") @PathParam("filter") String filter, @QueryParam("criteria") String criteria)
            throws RestAPIException {
        List<CartridgeBean> cartridges = StratosApiV41Utils.
                getCartridgesByFilter(filter, criteria, getConfigContext());
        if (cartridges == null || cartridges.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        CartridgeBean[] cartridgeArray = cartridges.toArray(new CartridgeBean[cartridges.size()]);
        return Response.ok().entity(cartridgeArray).build();
    }

    /**
     * Returns a specific cartridge by category.
     *
     * @param filter Filter
     * @param cartridgeType Cartridge Type
     * @return 200 if a cartridge is found for specified filter, 404 if none found
     * @throws RestAPIException
     */
    @GET
    @Path("/cartridges/{cartridgeType}/filter/{filter}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/getCartridgesByFilter")
    public Response getCartridgeByFilter(
            @PathParam("cartridgeType") String cartridgeType, @DefaultValue("") @PathParam("filter") String filter)
            throws RestAPIException {
        CartridgeBean cartridge;
        try {
            cartridge = StratosApiV41Utils.getCartridgeByFilter(filter, cartridgeType, getConfigContext());
            return Response.ok().entity(cartridge).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * Deletes a cartridge definition.
     *
     * @param cartridgeType the cartridge type
     * @return 200 if cartridge is successfully removed
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/cartridges/{cartridgeType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/removeCartridge")
    public Response removeCartridge(
            @PathParam("cartridgeType") String cartridgeType) throws RestAPIException {
        StratosApiV41Utils.removeCartridge(cartridgeType);
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Cartridge deleted successfully: [cartridge-type] %s", cartridgeType))).build();

    }

    // API methods for cartridge groups

    /**
     * Creates the cartridge group definition.
     *
     * @param serviceGroupDefinition the cartridge group definition
     * @return 201 if group added successfully
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/cartridgeGroups")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/addServiceGroup")
    @SuperTenantService(true)
    public Response addServiceGroup(
            GroupBean serviceGroupDefinition) throws RestAPIException {
        try {
            StratosApiV41Utils.addServiceGroup(serviceGroupDefinition);
            URI url = uriInfo.getAbsolutePathBuilder().path(serviceGroupDefinition.getName()).build();
            return Response.created(url).entity(new SuccessResponseBean(Response.Status.CREATED.getStatusCode(),
                    String.format("Service Group added successfully: [service-group] %s",
                            serviceGroupDefinition.getName()))).build();
        } catch (RestAPIException e) {
            if (e.getCause().getMessage().contains("already exists")) {
                return Response.status(Response.Status.CONFLICT).build();
            } else {
                throw e;
            }
        }
    }

    /**
     * Gets the cartridge group definition.
     *
     * @param groupDefinitionName the group definition name
     * @return 200 if cartridge group found for group definition, 404 if none is found
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/cartridgeGroups/{groupDefinitionName}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/getServiceGroupDefinition")
    public Response getServiceGroupDefinition(
            @PathParam("groupDefinitionName") String groupDefinitionName) throws RestAPIException {
        GroupBean serviceGroupDefinition = StratosApiV41Utils.getServiceGroupDefinition(groupDefinitionName);
        Response.ResponseBuilder rb;
        if (serviceGroupDefinition != null) {
            rb = Response.ok().entity(serviceGroupDefinition);
        } else {
            rb = Response.status(Response.Status.NOT_FOUND);
        }

        return rb.build();
    }

    /**
     * Gets all cartridge groups created.
     *
     * @return 200 if cartridge groups are found, 404 if none found
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/cartridgeGroups")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/getServiceGroupDefinition")
    public Response getServiceGroups()
            throws RestAPIException {
        GroupBean[] serviceGroups = StratosApiV41Utils.getServiceGroupDefinitions();
        Response.ResponseBuilder rb;
        if (serviceGroups != null) {
            rb = Response.ok().entity(serviceGroups);
        } else {
            rb = Response.status(Response.Status.NOT_FOUND);
        }

        return rb.build();
    }

    /**
     * Delete cartridge group definition.
     *
     * @param groupDefinitionName the group definition name
     * @return 200 if cartridge group is successfully removed
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/cartridgeGroups/{groupDefinitionName}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/removeServiceGroup")
    @SuperTenantService(true)
    public Response removeServiceGroup(
            @PathParam("groupDefinitionName") String groupDefinitionName) throws RestAPIException {

        StratosApiV41Utils.removeServiceGroup(groupDefinitionName);
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Service Group deleted successfully: [service-group] %s", groupDefinitionName)))
                .build();
    }

    // API methods for network partitions

    /**
     * Add network partition
     *
     * @param networkPartitionBean Network Partition
     * @return 201 if network partition successfully added, 409 if network partition already exists
     * @throws RestAPIException
     */
    @POST
    @Path("/networkPartitions")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/addNetworkPartition")
    public Response addNetworkPartition(
            NetworkPartitionBean networkPartitionBean) throws RestAPIException {
        String networkPartitionId = networkPartitionBean.getId();
        NetworkPartitionBean networkPartition = StratosApiV41Utils.getNetworkPartition(networkPartitionId);
        if (networkPartition != null) {
            String msg = String
                    .format("Network partition already exists: [network-partition-id] %s", networkPartitionId);
            log.warn(msg);
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponseBean(Response.Status.CONFLICT.getStatusCode(), msg)).build();
        }
        StratosApiV41Utils.addNetworkPartition(networkPartitionBean);
        URI url = uriInfo.getAbsolutePathBuilder().path(networkPartitionId).build();
        return Response.created(url).entity(new SuccessResponseBean(Response.Status.CREATED.getStatusCode(),
                String.format("Network partition added successfully: [network-partition] %s", networkPartitionId)))
                .build();
    }

    /**
     * Get network partitions
     *
     * @return 200 if network partitions are found
     * @throws RestAPIException
     */
    @GET
    @Path("/networkPartitions")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/getNetworkPartitions")
    public Response getNetworkPartitions()
            throws RestAPIException {
        NetworkPartitionBean[] networkPartitions = StratosApiV41Utils.getNetworkPartitions();
        if (networkPartitions == null || networkPartitions.length == 0){
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(networkPartitions).build();
    }

    /**
     * Get network partition by network partition id
     *
     * @return 200 if specified network partition is found
     * @throws RestAPIException
     */
    @GET
    @Path("/networkPartitions/{networkPartitionId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/getNetworkPartitions")
    public Response getNetworkPartition(
            @PathParam("networkPartitionId") String networkPartitionId) throws RestAPIException {
        NetworkPartitionBean networkPartition = StratosApiV41Utils.getNetworkPartition(networkPartitionId);
        if (networkPartition == null){
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(networkPartition).build();
    }

    /**
     * Remove network partition by network partition id
     *
     * @return 200 if specified network partition is successfully deleted, 404 if specified network partition is not
     * found
     * @throws RestAPIException
     */
    @DELETE
    @Path("/networkPartitions/{networkPartitionId}")
    @AuthorizationAction("/permission/protected/manage/removeNetworkPartition")
    public Response removeNetworkPartition(
            @PathParam("networkPartitionId") String networkPartitionId) throws RestAPIException {
        NetworkPartitionBean networkPartition = StratosApiV41Utils.getNetworkPartition(networkPartitionId);
        if (networkPartition == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        StratosApiV41Utils.removeNetworkPartition(networkPartitionId);
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Network Partition deleted successfully: [network-partition] %s",
                        networkPartitionId))).build();
    }

    // API methods for applications

    /**
     * Add application
     *
     * @param applicationDefinition Application Definition
     * @return 201 if application is successfully added
     * @throws RestAPIException
     */
    @POST
    @Path("/applications")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/addApplication")
    public Response addApplication(ApplicationBean applicationDefinition) throws RestAPIException {
        try {
            StratosApiV41Utils.addApplication(applicationDefinition, getConfigContext(), getUsername(), getTenantDomain());

            URI url = uriInfo.getAbsolutePathBuilder().path(applicationDefinition.getApplicationId()).build();
            return Response.created(url).entity(new SuccessResponseBean(Response.Status.CREATED.getStatusCode(),
                    String.format("Application added successfully: [application] %s",
                            applicationDefinition.getApplicationId()))).build();
        } catch (RestAPIException e) {
            if (e.getMessage().contains("already exists")) {
                return Response.status(Response.Status.CONFLICT).build();
            } else {
                throw e;
            }
        }
    }

    /**
     * Add application
     *
     * @param applicationDefinition Application Definition
     * @return 201 if application is successfully added
     * @throws RestAPIException
     */
    @PUT
    @Path("/applications")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/addApplication")
    public Response updateApplication(ApplicationBean applicationDefinition) throws RestAPIException {
        try {
            StratosApiV41Utils.updateApplication(applicationDefinition, getConfigContext(), getUsername(), getTenantDomain());

            URI url = uriInfo.getAbsolutePathBuilder().path(applicationDefinition.getApplicationId()).build();
            return Response.created(url).entity(new SuccessResponseBean(Response.Status.CREATED.getStatusCode(),
                    String.format("Application added successfully: [application] %s",
                            applicationDefinition.getApplicationId()))).build();
        } catch (RestAPIException e) {
            if (e.getMessage().contains("already exists")) {
                return Response.status(Response.Status.CONFLICT).build();
            } else {
                throw e;
            }
        }
    }

    /**
     * Return applications
     *
     * @return 200 if applications are found
     * @throws RestAPIException
     */
    @GET
    @Path("/applications")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/getApplications")
    public Response getApplications() throws RestAPIException {
        List<ApplicationBean> applicationDefinitions = StratosApiV41Utils.getApplications();
        if (applicationDefinitions == null || applicationDefinitions.isEmpty()){
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        ApplicationBean[] applicationDefinitionsArray = applicationDefinitions
                .toArray(new ApplicationBean[applicationDefinitions.size()]);
        return Response.ok(applicationDefinitionsArray).build();
    }

    /**
     * Gets the application.
     *
     * @param applicationId the application id
     * @return 200 if specified application is found, 404 if not
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/applications/{applicationId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/getApplications")
    public Response getApplication(
            @PathParam("applicationId") String applicationId) throws RestAPIException {
        ApplicationBean applicationDefinition = StratosApiV41Utils.getApplication(applicationId);
        if (applicationDefinition == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(applicationDefinition).build();
    }

    /**
     * Deploy application.
     *
     * @param applicationId Application Id
     * @param applicationPolicyId the application policy id
     * @return 202 after deployment process is started. Deployment is asynchronous
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/applications/{applicationId}/deploy/{applicationPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/deployApplication")
    public Response deployApplication(
            @PathParam("applicationId") String applicationId,
            @PathParam("applicationPolicyId") String applicationPolicyId) throws RestAPIException {
        try {
            StratosApiV41Utils.deployApplication(applicationId, applicationPolicyId);
            return Response.accepted().entity(new SuccessResponseBean(Response.Status.ACCEPTED.getStatusCode(),
                    String.format("Application deployed successfully: [application] %s", applicationId))).build();
        } catch (RestAPIException e) {
            if (e.getMessage().contains("already in DEPLOYED")) {
                return Response.status(Response.Status.CONFLICT).build();
            } else {
                throw e;
            }
        }
    }

    /**
     * Adds an application policy
     * @param applicationPolicy Application Policy
     * @return 201 if the application policy is successfully added
     * @throws RestAPIException
     */
    @POST
    @Path("/applicationPolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/addApplicationPolicy")
    public Response addApplicationPolicy(
            ApplicationPolicyBean applicationPolicy) throws RestAPIException {
        try {
            StratosApiV41Utils.addApplicationPolicy(applicationPolicy);
            URI url = uriInfo.getAbsolutePathBuilder().path(applicationPolicy.getId()).build();
            return Response.created(url).entity(new SuccessResponseBean(Response.Status.CREATED.getStatusCode(),
                    String.format("Application policy added successfully: [application-policy] %s",
                            applicationPolicy.getId()))).build();
        } catch (RestAPIException e) {
            if (e.getMessage().contains("already exists")) {
                return Response.status(Response.Status.CONFLICT).build();
            } else {
                throw e;
            }
        }
    }

    /**
     * Retrieve specified application policy
     *
     * @param applicationPolicyId Application Policy Id
     * @return 200 if application policy is found
     * @throws RestAPIException
     */
    @GET
    @Path("/applicationPolicies/{applicationPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/getApplicationPolicy")
    public Response getApplicationPolicy(
            @PathParam("applicationPolicyId") String applicationPolicyId) throws RestAPIException {
        ApplicationPolicyBean applicationPolicyBean = StratosApiV41Utils.getApplicationPolicy(applicationPolicyId);
        if (applicationPolicyBean == null){
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(applicationPolicyBean).build();
    }

    /**
     * Retrieve all application policies
     *
     * @return 200
     * @throws RestAPIException
     */
    @GET
    @Path("/applicationPolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/getApplicationPolicies")
    public Response getApplicationPolicies()
            throws RestAPIException {
        ApplicationPolicyBean[] applicationPolicies = StratosApiV41Utils.getApplicationPolicies();
        if (applicationPolicies == null || applicationPolicies.length == 0){
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok().entity(applicationPolicies).build();
    }

    /**
     * Remove specified application policy
     *
     * @param applicationPolicyId Application Policy Id
     * @return 200 if application policy is successfully removed
     * @throws RestAPIException
     */
    @DELETE
    @Path("/applicationPolicies/{applicationPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/removeApplicationPolicy")
    public Response removeApplicationPolicy(
            @PathParam("applicationPolicyId") String applicationPolicyId) throws RestAPIException {

        StratosApiV41Utils.removeApplicationPolicy(applicationPolicyId);
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Application policy deleted successfully: [application-policy] %s",
                        applicationPolicyId))).build();
    }

    /**
     * Update application policy
     *
     * @param applicationPolicy Application Policy
     * @return 200 if application policies successfully updated
     * @throws RestAPIException
     */
    @PUT
    @Path("/applicationPolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/updateApplicationPolicy")
    public Response updateApplicationPolicy(
            ApplicationPolicyBean applicationPolicy) throws RestAPIException {

        StratosApiV41Utils.updateApplicationPolicy(applicationPolicy);
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Application policy updated successfully: [application-policy] %s",
                        applicationPolicy.getId()))).build();
    }

    /**
     * Get network partition ids used in an application
     *
     * @return 200
     * @throws RestAPIException
     */
    @GET
    @Path("/applications/{applicationId}/networkPartitions")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/getApplicationNetworkPartitions")
    public Response getApplicationNetworkPartitions(
            @PathParam("applicationId") String applicationId) throws RestAPIException {
        ApplicationNetworkPartitionIdListBean appNetworkPartitionsBean = StratosApiV41Utils
                .getApplicationNetworkPartitions(applicationId);
        if (appNetworkPartitionsBean == null ||
                (appNetworkPartitionsBean.getNetworkPartitionIds().size() == 1 &&
                        appNetworkPartitionsBean.getNetworkPartitionIds().get(0) == null)){
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(appNetworkPartitionsBean).build();
    }

    /**
     * Signs up for an application.
     *
     * @param applicationId         the application id
     * @param applicationSignUpBean the application sign up bean
     * @return 200 if application sign up was successfull
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/applications/{applicationId}/signup")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/addApplicationSignUp")
    public Response addApplicationSignUp(
            @PathParam("applicationId") String applicationId, ApplicationSignUpBean applicationSignUpBean)
            throws RestAPIException {
        StratosApiV41Utils.addApplicationSignUp(applicationId, applicationSignUpBean);
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Successfully signed up for: [application] %s", applicationId))).build();
    }

    /**
     * Gets the application sign up.
     *
     * @param applicationId the application id
     * @return 200 if specified application signup is found, 404 if not
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/applications/{applicationId}/signup")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/getApplicationSignUp")
    public Response getApplicationSignUp(
            @PathParam("applicationId") String applicationId) throws RestAPIException {
        ApplicationSignUpBean applicationSignUpBean;
        try {
            applicationSignUpBean = StratosApiV41Utils.getApplicationSignUp(applicationId);
            if (applicationSignUpBean == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(applicationSignUpBean).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * Removes the application sign up.
     *
     * @param applicationId the application id
     * @return 200 if specified application sign up is removed
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/applications/{applicationId}/signup")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/removeApplicationSignUp")
    public Response removeApplicationSignUp(
            @PathParam("applicationId") String applicationId) throws RestAPIException {
        StratosApiV41Utils.removeApplicationSignUp(applicationId);
        return Response.ok().build();
    }

    /**
     * Adds the domain mappings for an application.
     *
     * @param applicationId       the application id
     * @param domainMappingsBean the domain mappings bean
     * @return 200
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/applications/{applicationId}/domainMappings")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/addDomainMappings")
    public Response addDomainMappings(
            @PathParam("applicationId") String applicationId, ApplicationDomainMappingsBean domainMappingsBean)
            throws RestAPIException {
        StratosApiV41Utils.addApplicationDomainMappings(applicationId, domainMappingsBean);
        List<DomainMappingBean> mappings = domainMappingsBean.getDomainMappings();
        List<String> domainMappingList = new ArrayList<String>();
        for (DomainMappingBean domainMappingBean : mappings) {
            domainMappingList.add(domainMappingBean.getDomainName());
        }
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Domain Mappings added successfully: [domain-mappings] %s", domainMappingList)))
                .build();
    }

    /**
     * Removes the domain mappings for an application.
     *
     * @param applicationId       the application id
     * @param domainMapppingsBean the domain mapppings bean
     * @return 200
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/applications/{applicationId}/domainMappings")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/removeDomainMappings")
    public Response removeDomainMappings(
            @PathParam("applicationId") String applicationId, ApplicationDomainMappingsBean domainMapppingsBean)
            throws RestAPIException {
        StratosApiV41Utils.removeApplicationDomainMappings(applicationId, domainMapppingsBean);
        List<DomainMappingBean> mappings = domainMapppingsBean.getDomainMappings();
        List<String> domainMappingList = new ArrayList<String>();
        for (DomainMappingBean domainMappingBean : mappings) {
            domainMappingList.add(domainMappingBean.getDomainName());
        }
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Domain Mappings deleted successfully: [domain-mappings] %s", domainMappingList)))
                .build();
    }

    /**
     * Gets the domain mappings for an application.
     *
     * @param applicationId the application id
     * @return 200
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/applications/{applicationId}/domainMappings")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/getDomainMappings")
    public Response getDomainMappings(
            @PathParam("applicationId") String applicationId) throws RestAPIException {
        List<DomainMappingBean> domainMappingsBeanList = StratosApiV41Utils.getApplicationDomainMappings(applicationId);
        if (domainMappingsBeanList == null || domainMappingsBeanList.isEmpty()){
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        DomainMappingBean[] domainMappingsBeans = domainMappingsBeanList
                .toArray(new DomainMappingBean[domainMappingsBeanList.size()]);
        return Response.ok(domainMappingsBeans).build();
    }

    /**
     * Undeploy an application.
     *
     * @param applicationId the application id
     * @return 202 if undeployment process started, 404 if specified application is not found, 409 if application
     * status is not in DEPLOYED state
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/applications/{applicationId}/undeploy")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/undeployApplication")
    public Response undeployApplication(
            @PathParam("applicationId") String applicationId, @QueryParam("force") @DefaultValue("false") boolean force) throws RestAPIException {

        ApplicationBean applicationDefinition = StratosApiV41Utils.getApplication(applicationId);
        if (applicationDefinition == null) {
            log.info(String.format("Application does not exist [application-id] %s", applicationId));
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (!applicationDefinition.getStatus().equalsIgnoreCase(StratosApiV41Utils.APPLICATION_STATUS_DEPLOYED)) {
            String message = String.format("Could not undeploy since application is not in DEPLOYED status [application-id] %s [current status] %S", applicationId, applicationDefinition.getStatus());
            log.info(message);
            return Response.status(Response.Status.CONFLICT).entity(message).build();
        }
        StratosApiV41Utils.undeployApplication(applicationId, force);
        return Response.accepted().entity(new SuccessResponseBean(Response.Status.ACCEPTED.getStatusCode(),
                String.format("Application undeployed successfully: [application] %s", applicationId))).build();
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
    @Path("/applications/{applicationId}/runtime")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/getApplicationRuntime")
    public Response getApplicationRuntime(
            @PathParam("applicationId") String applicationId) throws RestAPIException {
        ApplicationInfoBean applicationRuntime = StratosApiV41Utils.getApplicationRuntime(applicationId);
        if (applicationRuntime == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok().entity(applicationRuntime).build();
        }
    }

    /**
     * Delete an application.
     *
     * @param applicationId the application id
     * @return 200 if application is successfully removed, 404 if specified application is not found, 409 if
     * application is not in CREATED state
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/applications/{applicationId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/removeApplication")
    @SuperTenantService(true)
    public Response removeApplication(
            @PathParam("applicationId") String applicationId) throws RestAPIException {
        ApplicationBean applicationDefinition = StratosApiV41Utils.getApplication(applicationId);
        if (applicationDefinition == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!applicationDefinition.getStatus().equalsIgnoreCase(StratosApiV41Utils.APPLICATION_STATUS_CREATED)) {
            return Response.status(Response.Status.CONFLICT).entity(new SuccessResponseBean(Response.Status.CONFLICT.getStatusCode(),
                    String.format("Could not delete since application is not in CREATED state : [application] %s [current-status] %S", applicationId, applicationDefinition.getStatus()))).build();
        }

        StratosApiV41Utils.removeApplication(applicationId);
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Application deleted successfully: [application] %s", applicationId))).build();
    }

    // API methods for autoscaling policies

    /**
     * Gets the autoscaling policies.
     *
     * @return 200
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/autoscalingPolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/getAutoscalingPolicies")
    public Response getAutoscalingPolicies()
            throws RestAPIException {
        AutoscalePolicyBean[] autoScalePolicies = StratosApiV41Utils.getAutoScalePolicies();
        if (autoScalePolicies == null || autoScalePolicies.length == 0){
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok().entity(autoScalePolicies).build();
    }

    /**
     * Gets the autoscaling policy.
     *
     * @param autoscalePolicyId the autoscale policy id
     * @return 200 if specified autoscaling policy is found, 404 if not
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/autoscalingPolicies/{autoscalePolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/getAutoscalingPolicies")
    public Response getAutoscalingPolicy(
            @PathParam("autoscalePolicyId") String autoscalePolicyId) throws RestAPIException {
        AutoscalePolicyBean autoScalePolicy = StratosApiV41Utils.getAutoScalePolicy(autoscalePolicyId);
        if (autoScalePolicy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok().entity(autoScalePolicy).build();
    }

    /**
     * Creates the autoscaling policy defintion.
     *
     * @param autoscalePolicy the autoscale policy
     * @return 201 if autoscale policy is successfully added
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/autoscalingPolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/addAutoscalingPolicy")
    public Response addAutoscalingPolicy(
            AutoscalePolicyBean autoscalePolicy) throws RestAPIException {

        try {
            StratosApiV41Utils.addAutoscalingPolicy(autoscalePolicy);
            URI url = uriInfo.getAbsolutePathBuilder().path(autoscalePolicy.getId()).build();
            return Response.created(url).entity(new SuccessResponseBean(Response.Status.CREATED.getStatusCode(),
                    String.format("Autoscaling policy added successfully: [autoscale-policy] %s",
                            autoscalePolicy.getId()))).build();
        } catch (RestAPIException e) {
            if (e.getMessage().contains("already exists")) {
                return Response.status(Response.Status.CONFLICT).build();
            } else {
                throw e;
            }
        }
    }

    /**
     * Update autoscaling policy.
     *
     * @param autoscalePolicy the autoscale policy
     * @return 200 if autoscale policy is successfully updated
     * @throws RestAPIException the rest api exception
     */
    @PUT
    @Path("/autoscalingPolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/updateAutoscalingPolicy")
    public Response updateAutoscalingPolicy(
            AutoscalePolicyBean autoscalePolicy) throws RestAPIException {

        StratosApiV41Utils.updateAutoscalingPolicy(autoscalePolicy);
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Autoscaling policy updated successfully: [autoscale-policy] %s",
                        autoscalePolicy.getId()))).build();
    }

    /**
     * Updates a network partition
     * @param networkPartition Network Partition
     * @return 200 if network partition is successfully updated
     * @throws RestAPIException
     */
    @PUT
    @Path("/networkPartitions")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/updateNetworkPartition")
    public Response updateNetworkPartition(
            NetworkPartitionBean networkPartition) throws RestAPIException {

        StratosApiV41Utils.updateNetworkPartition(networkPartition);
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Network Partition updated successfully: [network-partition] %s",
                        networkPartition.getId()))).build();
    }

    /**
     * Remove autoscaling policy.
     *
     * @param autoscalingPolicyId the autoscale policy
     * @return 200
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/autoscalingPolicies/{autoscalingPolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/removeAutoscalingPolicy")
    public Response removeAutoscalingPolicy(
            @PathParam("autoscalingPolicyId") String autoscalingPolicyId) throws RestAPIException {

        StratosApiV41Utils.removeAutoscalingPolicy(autoscalingPolicyId);
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Autoscaling policy deleted successfully: [autoscale-policy] %s",
                        autoscalingPolicyId))).build();
    }


    /**
     * Get cluster for a given cluster id
     *
     * @param clusterId id of the cluster
     * @return 200 if specified cluster is found, 404 if not
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/cluster/{clusterId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/cluster")
    public Response getCluster(
            @PathParam("clusterId") String clusterId) throws RestAPIException {

        ClusterBean clusterBean = StratosApiV41Utils.getClusterInfo(clusterId);
        if (clusterBean == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok().entity(clusterBean).build();
        }
    }


    // API methods for tenants

    /**
     * Adds the tenant.
     *
     * @param tenantInfoBean the tenant info bean
     * @return 201 if the tenant is successfully added
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/tenants")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/addTenant")
    @SuperTenantService(true)
    public Response addTenant(
            org.apache.stratos.common.beans.TenantInfoBean tenantInfoBean) throws RestAPIException {

        StratosApiV41Utils.addTenant(tenantInfoBean);
        URI url = uriInfo.getAbsolutePathBuilder().path(tenantInfoBean.getTenantDomain()).build();
        return Response.created(url).entity(
                new SuccessResponseBean(Response.Status.CREATED.getStatusCode(),
                        String.format("Tenant added successfully: [tenant] %s", tenantInfoBean.getTenantDomain()))).build();
    }

    /**
     * Update tenant.
     *
     * @param tenantInfoBean the tenant info bean
     * @return 200 if tenant is successfully updated, 404 if specified tenant not found
     * @throws RestAPIException the rest api exception
     */
    @PUT
    @Path("/tenants")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/updateTenant")
    @SuperTenantService(true)
    public Response updateTenant(
            org.apache.stratos.common.beans.TenantInfoBean tenantInfoBean) throws RestAPIException {

        try {
            StratosApiV41Utils.updateExistingTenant(tenantInfoBean);
        } catch (TenantNotFoundException ex) {
            Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            String msg = "Error in updating tenant " + tenantInfoBean.getTenantDomain();
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Tenant updated successfully: [tenant] %s", tenantInfoBean.getTenantDomain())))
                .build();
    }



    /**
     * Gets the tenant by domain.
     *
     * @param tenantDomain the tenant domain
     * @return 200 if tenant for specified tenant domain found
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/tenants/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/getTenantForDomain")
    @SuperTenantService(true)
    public Response getTenantForDomain(
            @PathParam("tenantDomain") String tenantDomain) throws RestAPIException {

        try {
            TenantInfoBean tenantInfo = StratosApiV41Utils.getTenantByDomain(tenantDomain);
            if (tenantInfo == null){
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok().entity(tenantInfo).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }


    /**
     * Delete tenant.
     *
     * @param tenantDomain the tenant domain
     * @return 406 - Use tenantDeactivate method
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/tenants/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/removeTenant")
    @SuperTenantService(true)
    public Response removeTenant(
            @PathParam("tenantDomain") String tenantDomain) throws RestAPIException {

        return Response.status(Response.Status.NOT_ACCEPTABLE)
                .entity(new ErrorResponseBean(Response.Status.NOT_ACCEPTABLE.getStatusCode(),
                        "Please use the tenant deactivate method")).build();

    }

    /**
     * Gets the tenants.
     *
     * @return 200
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/tenants")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/getTenants")
    @SuperTenantService(true)
    public Response getTenants()
            throws RestAPIException {
        try {
            List<org.apache.stratos.common.beans.TenantInfoBean> tenantList = StratosApiV41Utils.getAllTenants();
            if (tenantList == null || tenantList.isEmpty()){
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok().entity(tenantList.toArray(
                    new org.apache.stratos.common.beans.TenantInfoBean[tenantList.size()])).build();
        } catch (Exception e) {
            String msg = "Error in retrieving tenants";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }


    /**
     * Gets the partial search tenants.
     *
     * @param tenantDomain the tenant domain
     * @return 200
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/tenants/search/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/getTenants")
    @SuperTenantService(true)
    public Response getPartialSearchTenants(
            @PathParam("tenantDomain") String tenantDomain) throws RestAPIException {

        try {
            List<org.apache.stratos.common.beans.TenantInfoBean> tenantList = StratosApiV41Utils.searchPartialTenantsDomains(tenantDomain);
            if (tenantList == null || tenantList.isEmpty()){
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok().entity(tenantList.toArray(new org.apache.stratos.common.beans.TenantInfoBean[tenantList.size()])).build();
        } catch (Exception e) {
            String msg = "Error in getting information for tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }



    /**
     * Activate tenant.
     *
     * @param tenantDomain the tenant domain
     * @return 200 if tenant activated successfully
     * @throws RestAPIException the rest api exception
     */
    @PUT
    @Path("/tenants/activate/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/activateTenant")
    @SuperTenantService(true)
    public Response activateTenant(
            @PathParam("tenantDomain") String tenantDomain) throws RestAPIException {

        StratosApiV41Utils.activateTenant(tenantDomain);
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Tenant activated successfully: [tenant] %s", tenantDomain))).build();
    }

    /**
     * Deactivate tenant.
     *
     * @param tenantDomain the tenant domain
     * @return 200 if tenant deactivated successfully
     * @throws RestAPIException the rest api exception
     */
    @PUT
    @Path("/tenants/deactivate/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/deactivateTenant")
    @SuperTenantService(true)
    public Response deactivateTenant(
            @PathParam("tenantDomain") String tenantDomain) throws RestAPIException {

        StratosApiV41Utils.deactivateTenant(tenantDomain);
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("Tenant deactivated successfully: [tenant] %s", tenantDomain))).build();
    }

    // API methods for repositories

    /**
     * Notify artifact update event for specified repository
     * @param payload Git notification Payload
     * @return 204
     * @throws RestAPIException
     */
    @POST
    @Path("/repo/notify")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/notifyRepository")
    public Response notifyRepository(
            GitNotificationPayloadBean payload) throws RestAPIException {
        if (log.isInfoEnabled()) {
            log.info(String.format("Git update notification received."));
        }

        StratosApiV41Utils.notifyArtifactUpdatedEvent(payload);
        return Response.noContent().build();
    }

    // API methods for users

    /**
     * Adds the user.
     *
     * @param userInfoBean the user info bean
     * @return 201 if the user is successfully created
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/users")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/admin/manage/addUser")
    public Response addUser(
            UserInfoBean userInfoBean) throws RestAPIException {

        StratosApiV41Utils.addUser(userInfoBean);
        log.info("Successfully added an user with Username " + userInfoBean.getUserName());
        URI url = uriInfo.getAbsolutePathBuilder().path(userInfoBean.getUserName()).build();
        return Response.created(url).entity(new SuccessResponseBean(Response.Status.CREATED.getStatusCode(),
                String.format("User added successfully: [user] %s", userInfoBean.getUserName()))).build();
    }

    /**
     * Delete user.
     *
     * @param userName the user name
     * @return 200 if the user is successfully deleted
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/users/{userName}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/admin/manage/removeUser")
    public Response removeUser(
            @PathParam("userName") String userName) throws RestAPIException {

        StratosApiV41Utils.removeUser(userName);
        log.info("Successfully removed user: [username] " + userName);
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("User deleted successfully: [user] %s", userName))).build();
    }

    /**
     * Update user.
     *
     * @param userInfoBean the user info bean
     * @return 200 if the user is successfully updated
     * @throws RestAPIException the rest api exception
     */
    @PUT
    @Path("/users")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/admin/manage/updateUser")
    public Response updateUser(
            UserInfoBean userInfoBean) throws RestAPIException {

        StratosApiV41Utils.updateUser(userInfoBean);
        log.info("Successfully updated an user with Username " + userInfoBean.getUserName());
        return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                String.format("User updated successfully: [user] %s", userInfoBean.getUserName()))).build();
    }

    /**
     * Gets the users.
     *
     * @return 200
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/users")
    @Produces("application/json")
    @AuthorizationAction("/permission/admin/manage/getUsers")
    public Response getUsers()
            throws RestAPIException {

        List<UserInfoBean> userList = StratosApiV41Utils.getUsers();
        return Response.ok().entity(userList.toArray(new UserInfoBean[userList.size()])).build();

    }



    // API methods for Kubernetes clusters

    /**
     * Deploy kubernetes host cluster.
     *
     * @param kubernetesCluster the kubernetes cluster
     * @return 201 if the kubernetes cluster is successfully created
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/kubernetesClusters")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/addKubernetesHostCluster")
    public Response addKubernetesHostCluster(
            KubernetesClusterBean kubernetesCluster) throws RestAPIException {

        try {
            StratosApiV41Utils.addKubernetesCluster(kubernetesCluster);
            URI url = uriInfo.getAbsolutePathBuilder().path(kubernetesCluster.getClusterId()).build();
            return Response.created(url).entity(new SuccessResponseBean(Response.Status.CREATED.getStatusCode(),
                    String.format("Kubernetes Host Cluster added successfully: [kub-host-cluster] %s",
                            kubernetesCluster.getClusterId()))).build();
        } catch (RestAPIException e) {
            if (e.getMessage().contains("already exists")) {
                return Response.status(Response.Status.CONFLICT).build();
            } else {
                throw e;
            }
        }
    }

    /**
     * Deploy kubernetes host.
     *
     * @param kubernetesClusterId the kubernetes cluster id
     * @param kubernetesHost      the kubernetes host
     * @return 201 if the kubernetes host is successfully added
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/kubernetesClusters/{kubernetesClusterId}/minion")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/addKubernetesHost")
    public Response addKubernetesHost(
            @PathParam("kubernetesClusterId") String kubernetesClusterId, KubernetesHostBean kubernetesHost)
            throws RestAPIException {

        StratosApiV41Utils.addKubernetesHost(kubernetesClusterId, kubernetesHost);
        URI url = uriInfo.getAbsolutePathBuilder().path(kubernetesHost.getHostId()).build();
        return Response.created(url).entity(new SuccessResponseBean(Response.Status.CREATED.getStatusCode(),
                String.format("Kubernetes Host added successfully: [kub-host] %s", kubernetesHost.getHostId())))
                .build();
    }

    /**
     * Update kubernetes master.
     *
     * @param kubernetesMaster the kubernetes master
     * @return 200 if the kubernetes master is updated successfully, 404 if the kubernetes master is not found
     * @throws RestAPIException the rest api exception
     */
    @PUT
    @Path("/kubernetesClusters/{kubernetesClusterId}/master")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/updateKubernetesMaster")
    public Response updateKubernetesMaster(
            KubernetesMasterBean kubernetesMaster) throws RestAPIException {
        try {
            StratosApiV41Utils.updateKubernetesMaster(kubernetesMaster);
            URI url = uriInfo.getAbsolutePathBuilder().path(kubernetesMaster.getHostId()).build();
            return Response.ok(url).entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                    String.format("Kubernetes Master updated successfully: [kub-master] %s",
                            kubernetesMaster.getHostId()))).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    //TODO: Check need for this method
    @PUT
    @Path("/kubernetes/update/host")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/updateKubernetesHost")
    public Response updateKubernetesHost(
            KubernetesHostBean kubernetesHost) throws RestAPIException {
        try {
            StratosApiV41Utils.updateKubernetesHost(kubernetesHost);
            URI url = uriInfo.getAbsolutePathBuilder().path(kubernetesHost.getHostId()).build();
            return Response.ok(url).entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                    String.format("Kubernetes Host updated successfully: [kub-host] %s",
                            kubernetesHost.getHostId()))).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * Gets the kubernetes host clusters.
     *
     * @return 200
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/kubernetesClusters")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/getKubernetesHostClusters")
    public Response getKubernetesHostClusters()
            throws RestAPIException {
        KubernetesClusterBean[] availableKubernetesClusters = StratosApiV41Utils.getAvailableKubernetesClusters();
        if (availableKubernetesClusters == null || availableKubernetesClusters.length == 0){
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok().entity(availableKubernetesClusters).build();
    }

    /**
     * Gets the kubernetes host cluster.
     *
     * @param kubernetesClusterId the kubernetes cluster id
     * @return 200 if specified kubernetes host cluster is found, 404 if not
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/kubernetesClusters/{kubernetesClusterId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/getKubernetesHostClusters")
    public Response getKubernetesHostCluster(
            @PathParam("kubernetesClusterId") String kubernetesClusterId) throws RestAPIException {
        try {
            return Response.ok().entity(StratosApiV41Utils.getKubernetesCluster(kubernetesClusterId)).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * Gets the kubernetes hosts of kubernetes cluster.
     *
     * @param kubernetesClusterId the kubernetes cluster id
     * @return 200 if hosts are found in the specified kubernetes host cluster, 404 if not
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/kubernetesClusters/{kubernetesClusterId}/hosts")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/getKubernetesHostClusters")
    public Response getKubernetesHostsOfKubernetesCluster(
            @PathParam("kubernetesClusterId") String kubernetesClusterId) throws RestAPIException {
        try {
            return Response.ok().entity(StratosApiV41Utils.getKubernetesHosts(kubernetesClusterId)).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * Gets the kubernetes master of kubernetes cluster.
     *
     * @param kubernetesClusterId the kubernetes cluster id
     * @return 200 if master is found for specified kubernetes cluster, 404 if not
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/kubernetesClusters/{kubernetesClusterId}/master")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/getKubernetesHostClusters")
    public Response getKubernetesMasterOfKubernetesCluster(
            @PathParam("kubernetesClusterId") String kubernetesClusterId) throws RestAPIException {
        try {
            return Response.ok().entity(StratosApiV41Utils.getKubernetesMaster(kubernetesClusterId)).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * Un deploy kubernetes host cluster.
     *
     * @param kubernetesClusterId the kubernetes cluster id
     * @return 204 if Kubernetes cluster is successfully removed, 404 if the specified Kubernetes cluster is not found
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/kubernetesClusters/{kubernetesClusterId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/removeKubernetesHostCluster")
    public Response removeKubernetesHostCluster(
            @PathParam("kubernetesClusterId") String kubernetesClusterId) throws RestAPIException {
        try {
            StratosApiV41Utils.removeKubernetesCluster(kubernetesClusterId);
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new SuccessResponseBean(Response.Status.NOT_FOUND.getStatusCode(),
                            String.format("Could not find specified Kubernetes cluster: [kub-cluster] %s",
                                    kubernetesClusterId))).build();
        }
        return Response.noContent().build();
    }

    /**
     * Undeploy kubernetes host of kubernetes cluster.
     *
     * @param kubernetesHostId the kubernetes host id
     * @return 200 if hosts are successfully removed from the specified Kubernetes cluster, 404 if specified Kubernetes
     * cluster is not found.
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/kubernetesClusters/{kubernetesClusterId}/hosts/{hostId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/removeKubernetesHostCluster")
    public Response removeKubernetesHostOfKubernetesCluster(
            @PathParam("hostId") String kubernetesHostId) throws RestAPIException {
        try {
            StratosApiV41Utils.removeKubernetesHost(kubernetesHostId);
            return Response.ok().entity(new SuccessResponseBean(Response.Status.OK.getStatusCode(),
                    String.format("Kubernetes Host removed successfully: [kub-host] %s", kubernetesHostId)))
                    .build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

}

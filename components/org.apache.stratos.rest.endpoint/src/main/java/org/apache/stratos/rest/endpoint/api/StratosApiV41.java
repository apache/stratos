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
import org.apache.stratos.common.beans.application.GroupBean;
import org.apache.stratos.common.beans.application.domain.mapping.ApplicationDomainMappingsBean;
import org.apache.stratos.common.beans.application.domain.mapping.DomainMappingBean;
import org.apache.stratos.common.beans.application.signup.ApplicationSignUpBean;
import org.apache.stratos.common.beans.policy.autoscale.AutoscalePolicyBean;
import org.apache.stratos.common.beans.policy.deployment.DeploymentPolicyBean;
import org.apache.stratos.common.beans.cartridge.CartridgeBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesClusterBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesHostBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesMasterBean;
import org.apache.stratos.common.beans.artifact.repository.GitNotificationPayloadBean;
import org.apache.stratos.common.beans.topology.ApplicationInfoBean;
import org.apache.stratos.common.util.ClaimsMgtUtil;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.manager.user.management.StratosUserManager;
import org.apache.stratos.manager.user.management.exception.UserManagerException;
import org.apache.stratos.rest.endpoint.ServiceHolder;
import org.apache.stratos.rest.endpoint.Utils;
import org.apache.stratos.rest.endpoint.annotation.AuthorizationAction;
import org.apache.stratos.rest.endpoint.annotation.SuperTenantService;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;
import org.apache.stratos.rest.endpoint.exception.TenantNotFoundException;
import org.apache.stratos.rest.endpoint.util.converter.ObjectConverter;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.tenant.mgt.core.TenantPersistor;
import org.wso2.carbon.tenant.mgt.util.TenantMgtUtil;
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

    // API methods for cartridges

    /**
     * Creates the cartridge definition.
     *
     * @param cartridgeDefinitionBean the cartridge definition bean
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/cartridges")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/cartridgeDefinition")
    public Response addCartridge(CartridgeBean cartridgeDefinitionBean)
            throws RestAPIException {
        StratosApiV41Utils.addCartridge(cartridgeDefinitionBean);
        URI url = uriInfo.getAbsolutePathBuilder().path(cartridgeDefinitionBean.getType()).build();
        return Response.created(url).build();

    }

    /**
     * Gets all available cartridges.
     *
     * @return the cartridges
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/cartridges")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getCartridges() throws RestAPIException {
        List<CartridgeBean> cartridges = StratosApiV41Utils.getAvailableCartridges(null, null, getConfigContext());
        if (cartridges == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        CartridgeBean[] cartridgeArray = cartridges.isEmpty() ?
                new CartridgeBean[0] : cartridges.toArray(new CartridgeBean[cartridges.size()]);
        return Response.ok().entity(cartridgeArray).build();
    }

    @GET
    @Path("/cartridges/{cartridgeType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getCartridge(@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {
        CartridgeBean cartridge = StratosApiV41Utils.getCartridge(cartridgeType);
        if (cartridge == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok().entity(cartridge).build();
    }

    /**
     * Returns cartridges by category.
     *
     * @param filter
     * @param criteria
     * @return
     * @throws RestAPIException
     */
    @GET
    @Path("/cartridges/filter/{filter}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getCartridgesByFilter(@DefaultValue("") @PathParam("filter") String filter,
                                          @QueryParam("criteria") String criteria) throws RestAPIException {
        List<CartridgeBean> cartridges = StratosApiV41Utils.
                getCartridgesByFilter(filter, criteria, getConfigContext());
        if (cartridges == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        CartridgeBean[] cartridgeArray = cartridges.isEmpty() ?
                new CartridgeBean[0] : cartridges.toArray(new CartridgeBean[cartridges.size()]);
        return Response.ok().entity(cartridgeArray).build();
    }

    /**
     * Returns a specific cartridge by category.
     *
     * @param filter
     * @param cartridgeType
     * @return
     * @throws RestAPIException
     */
    @GET
    @Path("/cartridges/{cartridgeType}/filter/{filter}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/cartridge")
    public Response getCartridgeByFilter(@PathParam("cartridgeType") String cartridgeType,
                                         @DefaultValue("") @PathParam("filter") String filter) throws RestAPIException {
        CartridgeBean cartridge = StratosApiV41Utils.getCartridgeByFilter(filter, cartridgeType, getConfigContext());
        if (cartridge == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok().entity(cartridge).build();
    }

    /**
     * Deletes a cartridge definition.
     *
     * @param cartridgeType the cartridge type
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/cartridges/{cartridgeType}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/cartridgeDefinition")
    public Response removeCartridge(@PathParam("cartridgeType") String cartridgeType) throws RestAPIException {
        StratosApiV41Utils.removeCartridge(cartridgeType);
        return Response.noContent().build();
    }

    // API methods for cartridge groups

    /**
     * Creates the cartridge group definition.
     *
     * @param serviceGroupDefinition the cartridge group definition
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/cartridgeGroups")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response addServiceGroup(GroupBean serviceGroupDefinition)
            throws RestAPIException {
        StratosApiV41Utils.addServiceGroup(serviceGroupDefinition);
        URI url = uriInfo.getAbsolutePathBuilder().path(serviceGroupDefinition.getName()).build();
        return Response.created(url).build();
    }

    /**
     * Gets the cartridge group definition.
     *
     * @param groupDefinitionName the group definition name
     * @return the cartridge group definition
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/cartridgeGroups/{groupDefinitionName}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getServiceGroupDefinition(@PathParam("groupDefinitionName") String groupDefinitionName)
            throws RestAPIException {
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
     * @return the cartridge groups
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/cartridgeGroups")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getServiceGroups() throws RestAPIException {
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
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/cartridgeGroups/{groupDefinitionName}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response removeServiceGroup(@PathParam("groupDefinitionName") String groupDefinitionName)
            throws RestAPIException {

        StratosApiV41Utils.removeServiceGroup(groupDefinitionName);
        return Response.noContent().build();
    }

    // API methods for applications

    /**
     * Add application
     *
     * @param applicationDefinition
     * @return
     * @throws RestAPIException
     */
    @POST
    @Path("/applications")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response addApplication(ApplicationBean applicationDefinition)
            throws RestAPIException {
        StratosApiV41Utils.addApplication(applicationDefinition, getConfigContext(), getUsername(), getTenantDomain());
        URI url = uriInfo.getAbsolutePathBuilder().path(applicationDefinition.getApplicationId()).build();
        return Response.created(url).build();
    }

    /**
     * Return applications
     *
     * @return
     * @throws RestAPIException
     */
    @GET
    @Path("/applications")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getApplications() throws RestAPIException {
        List<ApplicationBean> applicationDefinitions = StratosApiV41Utils.getApplications();
        ApplicationBean[] applicationDefinitionsArray = applicationDefinitions.toArray(new ApplicationBean[applicationDefinitions.size()]);
        return Response.ok(applicationDefinitionsArray).build();
    }

    /**
     * Gets the application.
     *
     * @param applicationId the application id
     * @return the application
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/applications/{applicationId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getApplication(@PathParam("applicationId") String applicationId)
            throws RestAPIException {
        ApplicationBean applicationDefinition = StratosApiV41Utils.getApplication(applicationId);
        if (applicationDefinition == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(applicationDefinition).build();
    }

    /**
     * Deploy application.
     *
     * @param deploymentPolicy the deployment policy
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/applications/{applicationId}/deploy")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response deployApplication(@PathParam("applicationId") String applicationId, DeploymentPolicyBean deploymentPolicy)
            throws RestAPIException {
        StratosApiV41Utils.deployApplication(applicationId, deploymentPolicy);
        return Response.accepted().build();
    }

    /**
     * Gets the application's deployment policy.
     *
     * @param applicationId the application id
     * @return the application deployment policy
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/applications/{applicationId}/deploymentPolicy")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getApplicationDeploymentPolicy(@PathParam("applicationId") String applicationId)
            throws RestAPIException {
        DeploymentPolicyBean deploymentPolicy = StratosApiV41Utils.getDeploymentPolicy(applicationId);
        if (deploymentPolicy == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(deploymentPolicy).build();
    }

    /**
     * Signs up for an application.
     *
     * @param applicationId the application id
     * @param applicationSignUpBean the application sign up bean
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/applications/{applicationId}/signup")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response addApplicationSignUp(@PathParam("applicationId") String applicationId,
                                         ApplicationSignUpBean applicationSignUpBean) throws RestAPIException {
        StratosApiV41Utils.addApplicationSignUp(applicationId, applicationSignUpBean);
        return Response.ok().build();
    }

    /**
     * Gets the application sign up.
     *
     * @param applicationId the application id
     * @return the application sign up
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/applications/{applicationId}/signup")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getApplicationSignUp(@PathParam("applicationId") String applicationId) throws RestAPIException {
        ApplicationSignUpBean applicationSignUpBean = StratosApiV41Utils.getApplicationSignUp(applicationId);
        if (applicationSignUpBean == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(applicationSignUpBean).build();
    }

    /**
     * Removes the application sign up.
     *
     * @param applicationId the application id
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/applications/{applicationId}/signup")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response removeApplicationSignUp(@PathParam("applicationId") String applicationId) throws RestAPIException {
        StratosApiV41Utils.removeApplicationSignUp(applicationId);
        return Response.ok().build();
    }

    /**
     * Adds the domain mappings for an application.
     *
     * @param applicationId the application id
     * @param domainMapppingsBean the domain mapppings bean
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/applications/{applicationId}/domainMappings")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response addDomainMappings(@PathParam("applicationId") String applicationId,
                                         ApplicationDomainMappingsBean domainMapppingsBean) throws RestAPIException {
        StratosApiV41Utils.addApplicationDomainMappings(applicationId, domainMapppingsBean);
        return Response.ok().build();
    }

    /**
     * Removes the domain mappings for an application.
     *
     * @param applicationId the application id
     * @param domainMapppingsBean the domain mapppings bean
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/applications/{applicationId}/domainMappings")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response removeDomainMappings(@PathParam("applicationId") String applicationId,
                                     ApplicationDomainMappingsBean domainMapppingsBean) throws RestAPIException {
        StratosApiV41Utils.removeApplicationDomainMappings(applicationId, domainMapppingsBean);
        return Response.ok().build();
    }

    /**
     * Gets the domain mappings for an application.
     *
     * @param applicationId the application id
     * @return the domain mappings
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/applications/{applicationId}/domainMappings")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getDomainMappings(@PathParam("applicationId") String applicationId) throws RestAPIException {
        List<DomainMappingBean> domainMappingsBeanList = StratosApiV41Utils.getApplicationDomainMappings(applicationId);
        DomainMappingBean[] domainMappingsBeans = domainMappingsBeanList.toArray(
                new DomainMappingBean[domainMappingsBeanList.size()]);
        return Response.ok(domainMappingsBeans).build();
    }

    /**
     * Undeploy an application.
     *
     * @param applicationId the application id
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/applications/{applicationId}/undeploy")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response undeployApplication(@PathParam("applicationId") String applicationId)
            throws RestAPIException {
        StratosApiV41Utils.undeployApplication(applicationId);
        return Response.accepted().build();
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
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getApplicationRuntime(@PathParam("applicationId") String applicationId) throws RestAPIException {
        ApplicationInfoBean applicationRuntime = StratosApiV41Utils.
                getApplicationRuntime(applicationId);
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
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/applications/{applicationId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response removeApplication(@PathParam("applicationId") String applicationId)
            throws RestAPIException {
        ApplicationBean applicationDefinition = StratosApiV41Utils.getApplication(applicationId);
        if (applicationDefinition == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        StratosApiV41Utils.removeApplication(applicationId);
        return Response.noContent().build();
    }

    // API methods for autoscaling policies

    /**
     * Gets the autoscaling policies.
     *
     * @return the autoscaling policies
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/autoscalingPolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/autoscalingPolicy")
    public Response getAutoscalingPolicies() throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getAutoScalePolicies()).build();
    }

    /**
     * Gets the autoscaling policy.
     *
     * @param autoscalePolicyId the autoscale policy id
     * @return the autoscaling policy
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/autoscalingPolicies/{autoscalePolicyId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/autoscalingPolicy")
    public Response getAutoscalingPolicy(@PathParam("autoscalePolicyId") String autoscalePolicyId)
            throws RestAPIException {
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
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/autoscalingPolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/autoscalingPolicy")
    public Response addAutoscalingPolicy(AutoscalePolicyBean autoscalePolicy)
            throws RestAPIException {

        StratosApiV41Utils.addAutoscalingPolicy(autoscalePolicy);
        URI url = uriInfo.getAbsolutePathBuilder().path(autoscalePolicy.getId()).build();
        return Response.created(url).build();
    }

    /**
     * Update autoscaling policy defintion.
     *
     * @param autoscalePolicy the autoscale policy
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @PUT
    @Path("/autoscalingPolicies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/autoscalingPolicy")
    public Response updateAutoscalingPolicyDefintion(AutoscalePolicyBean autoscalePolicy)
            throws RestAPIException {

        StratosApiV41Utils.updateAutoscalingPolicy(autoscalePolicy);
        return Response.ok().build();
    }

    // API methods for tenants

    /**
     * Adds the tenant.
     *
     * @param tenantInfoBean the tenant info bean
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/tenants")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/modify/tenants")
    @SuperTenantService(true)
    public Response addTenant(org.apache.stratos.common.beans.TenantInfoBean tenantInfoBean) throws RestAPIException {
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
            throw new RestAPIException("Security alert! User registry is null. A user is trying create a tenant "
                    + " without an authenticated session."); 
        }

        if (userRegistry.getTenantId() != MultitenantConstants.SUPER_TENANT_ID) {
            log.error("Security alert! None super tenant trying to create a tenant.");
            throw new RestAPIException("Security alert! None super tenant trying to create a tenant.");
        }

        Tenant tenant = TenantMgtUtil.initializeTenant(
                ObjectConverter.convertTenantInfoBeanToCarbonTenantInfoBean(tenantInfoBean));
        TenantPersistor persistor = ServiceHolder.getTenantPersistor();
        // not validating the domain ownership, since created by super tenant
        int tenantId = 0; //TODO verify whether this is the correct approach (isSkeleton)
        try {
            tenantId = persistor.persistTenant(tenant, false, tenantInfoBean.getSuccessKey(),
                    tenantInfoBean.getOriginatedService(), false);
        } catch (Exception e) {
            String msg = "Could not add tenant: " + e.getMessage();
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
            TenantMgtUtil.triggerAddTenant(ObjectConverter.convertTenantInfoBeanToCarbonTenantInfoBean(tenantInfoBean));
        } catch (StratosException e) {
            String msg = "Error in notifying tenant addition.";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        // For the super tenant tenant creation, tenants are always activated as they are created.
        try {
            TenantMgtUtil.activateTenantInitially(ObjectConverter.convertTenantInfoBeanToCarbonTenantInfoBean(tenantInfoBean), tenantId);
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

    /**
     * Update tenant.
     *
     * @param tenantInfoBean the tenant info bean
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @PUT
    @Path("/tenants")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/modify/tenants")
    @SuperTenantService(true)
    public Response updateTenant(org.apache.stratos.common.beans.TenantInfoBean tenantInfoBean) throws RestAPIException {

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

    private void updateExistingTenant(org.apache.stratos.common.beans.TenantInfoBean tenantInfoBean) throws Exception {

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
            TenantMgtUtil.triggerUpdateTenant(ObjectConverter.convertTenantInfoBeanToCarbonTenantInfoBean(tenantInfoBean));
        } catch (StratosException e) {
            String msg = "Error in notifying tenant update.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    /**
     * Gets the tenant by domain.
     *
     * @param tenantDomain the tenant domain
     * @return the tenant by domain
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/tenants/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public org.apache.stratos.common.beans.TenantInfoBean getTenantForDomain(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {

        try {
            return getTenantByDomain(tenantDomain);
        } catch (Exception e) {
            String msg = "Error in getting tenant information for tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }

    private org.apache.stratos.common.beans.TenantInfoBean getTenantByDomain(String tenantDomain) throws Exception {

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

        org.apache.stratos.common.beans.TenantInfoBean bean =
                ObjectConverter.convertCarbonTenantInfoBeanToTenantInfoBean(TenantMgtUtil.initializeTenantInfoBean(tenantId, tenant));

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

    /**
     * Delete tenant.
     *
     * @param tenantDomain the tenant domain
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/tenants/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public Response removeTenant(@PathParam("tenantDomain") String tenantDomain) throws RestAPIException {
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

    /**
     * Gets the tenants.
     *
     * @return the tenants
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/tenants")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public org.apache.stratos.common.beans.TenantInfoBean[] getTenants() throws RestAPIException {
        List<org.apache.stratos.common.beans.TenantInfoBean> tenantList = null;
        try {
            tenantList = getAllTenants();
        } catch (Exception e) {
            String msg = "Error in retrieving tenants";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        return tenantList.toArray(new org.apache.stratos.common.beans.TenantInfoBean[tenantList.size()]);
    }

    private List<org.apache.stratos.common.beans.TenantInfoBean> getAllTenants() throws RestAPIException {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        Tenant[] tenants;
        try {
            tenants = (Tenant[]) tenantManager.getAllTenants();
        } catch (Exception e) {
            String msg = "Error in retrieving the tenant information";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        List<org.apache.stratos.common.beans.TenantInfoBean> tenantList = new ArrayList<org.apache.stratos.common.beans.TenantInfoBean>();
        for (Tenant tenant : tenants) {
            org.apache.stratos.common.beans.TenantInfoBean bean = ObjectConverter.convertCarbonTenantInfoBeanToTenantInfoBean(
                    TenantMgtUtil.getTenantInfoBeanfromTenant(tenant.getId(), tenant));
            tenantList.add(bean);
        }
        return tenantList;
    }

    /**
     * Gets the partial search tenants.
     *
     * @param tenantDomain the tenant domain
     * @return the partial search tenants
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/tenants/search/{tenantDomain}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    @SuperTenantService(true)
    public org.apache.stratos.common.beans.TenantInfoBean[] getPartialSearchTenants(@PathParam("tenantDomain") String tenantDomain)
            throws RestAPIException {

        List<org.apache.stratos.common.beans.TenantInfoBean> tenantList = null;
        try {
            tenantList = searchPartialTenantsDomains(tenantDomain);
        } catch (Exception e) {
            String msg = "Error in getting information for tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        return tenantList.toArray(new org.apache.stratos.common.beans.TenantInfoBean[tenantList.size()]);
    }

    private List<org.apache.stratos.common.beans.TenantInfoBean> searchPartialTenantsDomains(String domain) throws RestAPIException {
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

        List<org.apache.stratos.common.beans.TenantInfoBean> tenantList = new ArrayList<org.apache.stratos.common.beans.TenantInfoBean>();
        for (Tenant tenant : tenants) {
            org.apache.stratos.common.beans.TenantInfoBean bean = ObjectConverter.convertCarbonTenantInfoBeanToTenantInfoBean(
                    TenantMgtUtil.getTenantInfoBeanfromTenant(tenant.getId(), tenant));
            tenantList.add(bean);
        }
        return tenantList;
    }

    /**
     * Activate tenant.
     *
     * @param tenantDomain the tenant domain
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @PUT
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

    /**
     * Deactivate tenant.
     *
     * @param tenantDomain the tenant domain
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @PUT
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

    // API methods for repositories

    @POST
    @Path("/repo/notify")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/sync")
    public Response notifyRepository(GitNotificationPayloadBean payload) throws RestAPIException {
        if (log.isInfoEnabled()) {
            log.info(String.format("Git update"));
        }

        StratosApiV41Utils.notifyArtifactUpdatedEvent(payload);
        return Response.noContent().build();
    }

    // API methods for users

    /**
     * Adds the user.
     *
     * @param userInfoBean the user info bean
     * @return the response
     * @throws RestAPIException the rest api exception
     */
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

    /**
     * Delete user.
     *
     * @param userName the user name
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/users/{userName}")
    @Consumes("application/json")
    @Produces("application/json")
    @AuthorizationAction("/permission/admin/manage/add/users")
    public Response removeUser(@PathParam("userName") String userName) throws RestAPIException {

        StratosUserManager stratosUserManager = new StratosUserManager();

        try {
            stratosUserManager.removeUser(getTenantUserStoreManager(), userName);
        } catch (UserManagerException e) {
            throw new RestAPIException(e.getMessage());
        }
        log.info("Successfully removed user: [username] " + userName);
        return Response.noContent().build();
    }

    /**
     * Update user.
     *
     * @param userInfoBean the user info bean
     * @return the response
     * @throws RestAPIException the rest api exception
     */
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

    /**
     * Gets the users.
     *
     * @return the users
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/users")
    @Produces("application/json")
    @AuthorizationAction("/permission/admin/manage/add/users")
    public UserInfoBean[] getUsers() throws RestAPIException {

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

    // API methods for Kubernetes clusters

    /**
     * Deploy kubernetes host cluster.
     *
     * @param kubernetesCluster the kubernetes cluster
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @POST
    @Path("/kubernetesClusters")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/kubernetes")
    public Response addKubernetesHostCluster(KubernetesClusterBean kubernetesCluster) throws RestAPIException {

        StratosApiV41Utils.addKubernetesCluster(kubernetesCluster);
        URI url = uriInfo.getAbsolutePathBuilder().path(kubernetesCluster.getClusterId()).build();
        return Response.created(url).build();
    }

    /**
     * Deploy kubernetes host.
     *
     * @param kubernetesClusterId the kubernetes cluster id
     * @param kubernetesHost      the kubernetes host
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @PUT
    @Path("/kubernetesClusters/{kubernetesClusterId}/minion")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/kubernetes")
    public Response addKubernetesHost(@PathParam("kubernetesClusterId") String kubernetesClusterId, KubernetesHostBean kubernetesHost)
            throws RestAPIException {

        StratosApiV41Utils.addKubernetesHost(kubernetesClusterId, kubernetesHost);
        URI url = uriInfo.getAbsolutePathBuilder().path(kubernetesHost.getHostId()).build();
        return Response.created(url).build();
    }

    /**
     * Update kubernetes master.
     *
     * @param kubernetesMaster the kubernetes master
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @PUT
    @Path("/kubernetesClusters/{kubernetesClusterId}/master")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/kubernetes")
    public Response updateKubernetesMaster(KubernetesMasterBean kubernetesMaster) throws RestAPIException {
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
    public Response updateKubernetesHost(KubernetesHostBean kubernetesHost) throws RestAPIException {
        try {
            StratosApiV41Utils.updateKubernetesHost(kubernetesHost);
            URI url = uriInfo.getAbsolutePathBuilder().path(kubernetesHost.getHostId()).build();
            return Response.created(url).build();
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * Gets the kubernetes host clusters.
     *
     * @return the kubernetes host clusters
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/kubernetesClusters")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/kubernetes")
    public Response getKubernetesHostClusters() throws RestAPIException {
        return Response.ok().entity(StratosApiV41Utils.getAvailableKubernetesClusters()).build();
    }

    /**
     * Gets the kubernetes host cluster.
     *
     * @param kubernetesClusterId the kubernetes cluster id
     * @return the kubernetes host cluster
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/kubernetesClusters/{kubernetesClusterId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/view/kubernetes")
    public Response getKubernetesHostCluster(@PathParam("kubernetesClusterId") String kubernetesClusterId) throws RestAPIException {
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
     * @return the kubernetes hosts of kubernetes cluster
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/kubernetesClusters/{kubernetesClusterId}/hosts")
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

    /**
     * Gets the kubernetes master of kubernetes cluster.
     *
     * @param kubernetesClusterId the kubernetes cluster id
     * @return the kubernetes master of kubernetes cluster
     * @throws RestAPIException the rest api exception
     */
    @GET
    @Path("/kubernetesClusters/{kubernetesClusterId}/master")
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

    /**
     * Un deploy kubernetes host cluster.
     *
     * @param kubernetesClusterId the kubernetes cluster id
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/kubernetesClusters/{kubernetesClusterId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/kubernetes")
    public Response removeKubernetesHostCluster(@PathParam("kubernetesClusterId") String kubernetesClusterId) throws RestAPIException {
        try {
            StratosApiV41Utils.removeKubernetesCluster(kubernetesClusterId);
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    /**
     * Undeploy kubernetes host of kubernetes cluster.
     *
     * @param kubernetesHostId the kubernetes host id
     * @return the response
     * @throws RestAPIException the rest api exception
     */
    @DELETE
    @Path("/kubernetesClusters/{kubernetesClusterId}/hosts/{hostId}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/admin/manage/add/kubernetes")
    public Response removeKubernetesHostOfKubernetesCluster(@PathParam("hostId") String kubernetesHostId) throws RestAPIException {
        try {
            StratosApiV41Utils.removeKubernetesHost(kubernetesHostId);
        } catch (RestAPIException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

}

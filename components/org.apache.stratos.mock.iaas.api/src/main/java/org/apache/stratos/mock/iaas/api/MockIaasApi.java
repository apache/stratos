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

package org.apache.stratos.mock.iaas.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.mock.iaas.api.exception.MockIaasApiException;
import org.apache.stratos.mock.iaas.config.MockIaasConfig;
import org.apache.stratos.mock.iaas.domain.MockInstanceContext;
import org.apache.stratos.mock.iaas.domain.MockInstanceMetadata;
import org.apache.stratos.mock.iaas.services.MockIaasService;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Mock IaaS API.
 */
@Path("/")
public class MockIaasApi {

    private static final Log log = LogFactory.getLog(MockIaasApi.class);

    @Context
    private HttpServletRequest httpServletRequest;
    private MockIaasService mockIaasService;

    public MockIaasApi() {
    }

    @POST
    @Path("/instances")
    @Consumes("application/json")
    @Produces("application/json")
    public Response startInstance(MockInstanceContext mockInstanceContext) throws MockIaasApiException {
        try {
            // Validate mock iaas service
            validateMockIaasService();

            log.info(String.format("Starting mock instance: [member-id] %s", mockInstanceContext.getMemberId()));
            MockInstanceMetadata mockInstanceMetadata = getMockIaasService().startInstance(mockInstanceContext);
            log.info(String.format("Mock instance started successfully: [member-id] %s [instance-id] %s",
                    mockInstanceContext.getMemberId(), mockInstanceContext.getInstanceId()));
            return Response.ok(mockInstanceMetadata).build();
        } catch (Exception e) {
            String message = "Could not start mock instance";
            log.error(message, e);
            throw new MockIaasApiException(message, e);
        }
    }

    @GET
    @Path("/instances")
    @Produces("application/json")
    public Response getInstances() throws MockIaasApiException {
        try {
            // Validate mock iaas service
            validateMockIaasService();

            log.debug(String.format("Get mock instances"));

            List<MockInstanceMetadata> mockInstanceMetadataList = getMockIaasService().getInstances();
            MockInstanceMetadata[] mockInstanceMetadataArray = mockInstanceMetadataList.toArray(
                    new MockInstanceMetadata[mockInstanceMetadataList.size()]);
            return Response.ok(mockInstanceMetadataArray).build();
        } catch (Exception e) {
            String message = "Could not get mock instances";
            log.error(message, e);
            throw new MockIaasApiException(message, e);
        }
    }

    @GET
    @Path("/instances/{instanceId}")
    @Produces("application/json")
    public Response getInstance(@PathParam("instanceId") String instanceId) throws MockIaasApiException {
        try {
            // Validate mock iaas service
            validateMockIaasService();

            log.debug(String.format("Get mock instance: [instance-id] %s", instanceId));

            MockInstanceMetadata mockInstanceMetadata = getMockIaasService().getInstance(instanceId);
            if(mockInstanceMetadata == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            log.debug(String.format("Mock instance found: [instance-id] %s", instanceId));
            return Response.ok(mockInstanceMetadata).build();
        } catch (Exception e) {
            String message = "Could not get mock instance";
            log.error(message, e);
            throw new MockIaasApiException(message, e);
        }
    }

    @POST
    @Path("/instances/{instanceId}/allocateIpAddress")
    @Produces("application/json")
    public Response allocateIpAddress(@PathParam("instanceId") String instanceId) throws MockIaasApiException {
        try {
            // Validate mock iaas service
            validateMockIaasService();

            log.info(String.format("Allocating ip addresses: [instance-id] %s", instanceId));

            MockInstanceMetadata mockInstanceMetadata = getMockIaasService().getInstance(instanceId);
            if(mockInstanceMetadata == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            mockInstanceMetadata = getMockIaasService().allocateIpAddress(instanceId);
            log.info(String.format("IP addresses allocated: [instance-id] %s [default-private-ip] %s " +
                    "[default-public-ip] %s", instanceId, mockInstanceMetadata.getDefaultPrivateIp(),
                    mockInstanceMetadata.getDefaultPublicIp()));
            return Response.ok(mockInstanceMetadata).build();
        } catch (Exception e) {
            String message = String.format("Could not allocate ip address: [instance-id] %s", instanceId);
            log.error(message, e);
            throw new MockIaasApiException(message, e);
        }
    }

    @DELETE
    @Path("/instances/{instanceId}")
    public Response terminateInstance(@PathParam("instanceId") String instanceId) throws MockIaasApiException {
        try {
            // Validate mock iaas service
            validateMockIaasService();

            log.info(String.format("Terminating mock instance: [instance-id] %s", instanceId));
            getMockIaasService().terminateInstance(instanceId);
            log.info(String.format("Mock instance terminated successfully: [instance-id] %s", instanceId));
            return Response.ok().build();
        } catch (Exception e) {
            String message = "Could not start mock instance";
            log.error(message, e);
            throw new MockIaasApiException(message, e);
        }
    }

    /**
     * Get mock iaas service instance
     * @return
     */
    private MockIaasService getMockIaasService() {
        if(mockIaasService == null) {
            synchronized (MockIaasApi.class) {
                if(mockIaasService == null) {
                    try {
                        try {
                            mockIaasService = (MockIaasService) PrivilegedCarbonContext.getThreadLocalCarbonContext().
                                    getOSGiService(MockIaasService.class);
                        } catch (NullPointerException ignore) {
                            // Above carbon context method throws a NPE if service is not registered
                        }
                    } catch (Exception e) {
                        String message = "Could not initialize Mock IaaS API";
                        log.error(message, e);
                        throw new RuntimeException(message, e);
                    }
                }
            }
        }
        return mockIaasService;
    }

    /**
     * Validate mock iaas service.
     */
    private void validateMockIaasService() {
        if(getMockIaasService() == null) {
            throw new RuntimeException(String.format("Mock IaaS may have been disabled, please check %s file",
                    MockIaasConfig.MOCK_IAAS_CONFIG_FILE_NAME));
        }
    }
}

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

package org.apache.stratos.mock.iaas.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.apache.stratos.mock.iaas.client.rest.HttpResponse;
import org.apache.stratos.mock.iaas.client.rest.RestClient;
import org.apache.stratos.mock.iaas.domain.ErrorResponse;
import org.apache.stratos.mock.iaas.domain.MockInstanceContext;
import org.apache.stratos.mock.iaas.domain.MockInstanceMetadata;

import java.net.URI;

/**
 * Mock iaas api client.
 */
public class MockIaasApiClient {

    private static final Log log = LogFactory.getLog(MockIaasApiClient.class);
    private static final String INSTANCES_CONTEXT = "/instances/";
    private static final String INIT_CONTEXT = "/status";

    private RestClient restClient;
    private String endpoint;

    public MockIaasApiClient(String endpoint) {
        this.restClient = new RestClient();
        this.endpoint = endpoint;
    }

    public boolean isMockIaaSReady() {
        try {
            URI uri = new URIBuilder(endpoint + INIT_CONTEXT).build();
            HttpResponse response = restClient.doGet(uri);
            return response != null && response.getStatusCode() == 200;
        } catch (Exception e) {
            String message = "Could not check whether mock-iaas is active";
            throw new RuntimeException(message, e);
        }
    }

    public MockInstanceMetadata startInstance(MockInstanceContext mockInstanceContext) {
        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            String content = gson.toJson(mockInstanceContext);
            if (log.isDebugEnabled()) {
                log.debug("Start instance request body: " + content);
            }
            URI uri = new URIBuilder(endpoint + INSTANCES_CONTEXT).build();
            HttpResponse response = restClient.doPost(uri, content);
            if (log.isDebugEnabled()) {
                log.debug("Mock start instance call response: " + response.getContent());
            }
            if (response != null) {
                if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                    return gson.fromJson(response.getContent(), MockInstanceMetadata.class);
                } else {
                    ErrorResponse errorResponse = gson.fromJson(response.getContent(), ErrorResponse.class);
                    if (errorResponse != null) {
                        throw new RuntimeException(errorResponse.getErrorMessage());
                    }
                }
            }
            throw new RuntimeException("An unknown error occurred");
        } catch (Exception e) {
            String message = "Could not start mock instance";
            throw new RuntimeException(message, e);
        }
    }

    public boolean terminateInstance(String instanceId) {
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Terminate instance: [instance-id] %s", instanceId));
            }
            URI uri = new URIBuilder(endpoint + INSTANCES_CONTEXT + instanceId).build();
            HttpResponse response = restClient.doDelete(uri);
            if (response != null) {
                if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                    return true;
                } else {
                    GsonBuilder gsonBuilder = new GsonBuilder();
                    Gson gson = gsonBuilder.create();
                    ErrorResponse errorResponse = gson.fromJson(response.getContent(), ErrorResponse.class);
                    if (errorResponse != null) {
                        throw new RuntimeException(errorResponse.getErrorMessage());
                    }
                }
            }
            throw new RuntimeException("An unknown error occurred");
        } catch (Exception e) {
            String message = "Could not start mock instance";
            throw new RuntimeException(message, e);
        }
    }

    public MockInstanceMetadata allocateIpAddress(String instanceId) {
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Allocating ip address: [instance-id] %s", instanceId));
            }
            URI uri = new URIBuilder(endpoint + INSTANCES_CONTEXT + instanceId + "/allocateIpAddress").build();
            HttpResponse response = restClient.doPost(uri, new String());
            if (response != null) {
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();

                if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                    return gson.fromJson(response.getContent(), MockInstanceMetadata.class);
                } else {
                    ErrorResponse errorResponse = gson.fromJson(response.getContent(), ErrorResponse.class);
                    if (errorResponse != null) {
                        throw new RuntimeException(errorResponse.getErrorMessage());
                    }
                }
            }
            throw new RuntimeException("An unknown error occurred");
        } catch (Exception e) {
            String message = String.format("Could not allocate ip address: [instance-id] ", instanceId);
            throw new RuntimeException(message, e);
        }
    }

    public MockInstanceMetadata getInstance(String instanceId) {
        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            URI uri = new URIBuilder(endpoint + INSTANCES_CONTEXT + instanceId).build();
            HttpResponse response = restClient.doGet(uri);
            if (response != null) {
                if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                    return gson.fromJson(response.getContent(), MockInstanceMetadata.class);
                } else {
                    ErrorResponse errorResponse = gson.fromJson(response.getContent(), ErrorResponse.class);
                    if (errorResponse != null) {
                        throw new RuntimeException(errorResponse.getErrorMessage());
                    }
                }
            }
            throw new RuntimeException("An unknown error occurred");
        } catch (Exception e) {
            String message = "Could not start mock instance";
            throw new RuntimeException(message, e);
        }
    }
}

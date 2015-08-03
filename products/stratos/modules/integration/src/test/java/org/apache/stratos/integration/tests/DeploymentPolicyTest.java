/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.integration.tests;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.apache.stratos.common.beans.policy.deployment.DeploymentPolicyBean;
import org.apache.stratos.integration.tests.rest.ErrorResponse;
import org.apache.stratos.integration.tests.rest.HttpResponse;
import org.apache.stratos.integration.tests.rest.RestClient;

import java.net.URI;

/**
 * Test to handle Network partition CRUD operations
 */
public class DeploymentPolicyTest extends StratosArtifactsUtils {
    private static final Log log = LogFactory.getLog(StratosTestServerManager.class);
    String deploymentPolicies = "/deployment-policies/";
    String deploymentPoliciesUpdate = "/deployment-policies/update/";


    public boolean addDeploymentPolicy(String deploymentPolicyId, String endpoint, RestClient restClient) {
        try {
            String content = getJsonStringFromFile(deploymentPolicies + deploymentPolicyId);
            URI uri = new URIBuilder(endpoint + RestConstants.DEPLOYMENT_POLICIES).build();

            HttpResponse response = restClient.doPost(uri, content);
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

    public DeploymentPolicyBean getDeploymentPolicy(String deploymentPolicyId, String endpoint,
                                                    RestClient restClient) {
        try {
            URI uri = new URIBuilder(endpoint + RestConstants.DEPLOYMENT_POLICIES + "/" +
                    deploymentPolicyId).build();
            HttpResponse response = restClient.doGet(uri);
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            if (response != null) {
                if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                    return gson.fromJson(response.getContent(), DeploymentPolicyBean.class);
                } else if (response.getStatusCode() == 404) {
                    return null;
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

    public boolean updateDeploymentPolicy(String deploymentPolicyId, String endpoint, RestClient restClient) {
        try {
            String content = getJsonStringFromFile(deploymentPoliciesUpdate + deploymentPolicyId);
            URI uri = new URIBuilder(endpoint + RestConstants.DEPLOYMENT_POLICIES).build();
            HttpResponse response = restClient.doPut(uri, content);
            if (response != null) {
                if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                    return true;
                } else if(response.getContent().contains("it is used")) {
                    return false;
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

    public boolean removeDeploymentPolicy(String deploymentPolicyId, String endpoint, RestClient restClient) {
        try {
            URI uri = new URIBuilder(endpoint + RestConstants.DEPLOYMENT_POLICIES + "/" +
                    deploymentPolicyId).build();
            HttpResponse response = restClient.doDelete(uri);
            if (response != null) {
                if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                    return true;
                } else if(response.getContent().contains("is in use")) {
                    return false;
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
}

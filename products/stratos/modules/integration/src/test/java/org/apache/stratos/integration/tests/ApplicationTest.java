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
import org.apache.stratos.common.beans.application.ApplicationBean;
import org.apache.stratos.integration.tests.rest.ErrorResponse;
import org.apache.stratos.integration.tests.rest.HttpResponse;
import org.apache.stratos.integration.tests.rest.RestClient;

import java.net.URI;

/**
 * Test to handle autoscaling policy CRUD operations
 */
public class ApplicationTest extends StratosArtifactsUtils {
    private static final Log log = LogFactory.getLog(StratosTestServerManager.class);
    String applications = "/applications/simple/single-cartridge-app/";
    String applicationsUpdate = "/applications/simple/single-cartridge-app/update/";


    public boolean addApplication(String applicationId, String endpoint, RestClient restClient) {
        try {
            String content = getJsonStringFromFile(applications + applicationId);
            URI uri = new URIBuilder(endpoint + RestConstants.APPLICATIONS).build();

            HttpResponse response = restClient.doPost(uri, content, "admin", "admin");
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

    public boolean deployApplication(String applicationId, String applicationPolicyId,
                                     String endpoint, RestClient restClient) {
        try {
            URI uri = new URIBuilder(endpoint + RestConstants.APPLICATIONS + "/" + applicationId +
            RestConstants.APPLICATIONS_DEPLOY + "/" + applicationPolicyId).build();

            HttpResponse response = restClient.doPost(uri, "", "admin", "admin");
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

    public boolean undeployApplication(String applicationId,
                                     String endpoint, RestClient restClient) {
        try {
            URI uri = new URIBuilder(endpoint + RestConstants.APPLICATIONS + "/" + applicationId +
                    RestConstants.APPLICATIONS_UNDEPLOY).build();

            HttpResponse response = restClient.doPost(uri, "", "admin", "admin");
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

    public boolean forceUndeployApplication(String applicationId,
                                       String endpoint, RestClient restClient) {
        try {
            URI uri = new URIBuilder(endpoint + RestConstants.APPLICATIONS + "/" + applicationId +
                    RestConstants.APPLICATIONS_UNDEPLOY + "?force=true").build();

            HttpResponse response = restClient.doPost(uri, "", "admin", "admin");
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

    public ApplicationBean getApplication(String applicationId, String endpoint,
                                                    RestClient restClient) {
        try {
            URI uri = new URIBuilder(endpoint + RestConstants.APPLICATIONS + "/" +
                    applicationId).build();
            HttpResponse response = restClient.doGet(uri, "admin", "admin");
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            if (response != null) {
                if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                    return gson.fromJson(response.getContent(), ApplicationBean.class);
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

    public boolean updateApplication(String applicationId, String endpoint, RestClient restClient) {
        try {
            String content = getJsonStringFromFile(applicationsUpdate + applicationId);
            URI uri = new URIBuilder(endpoint + RestConstants.APPLICATIONS).build();
            HttpResponse response = restClient.doPut(uri, content, "admin", "admin");
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

    public boolean removeApplication(String applicationId, String endpoint, RestClient restClient) {
        try {
            URI uri = new URIBuilder(endpoint + RestConstants.APPLICATIONS + "/" +
                    applicationId).build();
            HttpResponse response = restClient.doDelete(uri, "admin", "admin");
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
}

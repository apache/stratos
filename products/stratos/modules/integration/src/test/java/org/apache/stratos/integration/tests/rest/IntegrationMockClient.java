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
package org.apache.stratos.integration.tests.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.stratos.mock.iaas.client.MockIaasApiClient;
import org.apache.stratos.mock.iaas.client.rest.HttpResponse;
import org.apache.stratos.mock.iaas.client.rest.HttpResponseHandler;

import java.net.URI;

/**
 * Mock client
 */
public class IntegrationMockClient extends MockIaasApiClient {
    private static final Log log = LogFactory.getLog(IntegrationMockClient.class);
    private static final String INSTANCES_CONTEXT = "/instances/";
    private DefaultHttpClient httpClient;
    private String endpoint;

    public IntegrationMockClient(String endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
        PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
        // Increase max total connection to 200
        cm.setMaxTotal(200);
        // Increase default max connection per route to 50
        cm.setDefaultMaxPerRoute(50);

        httpClient = new DefaultHttpClient(cm);
        httpClient = (DefaultHttpClient) WebClientWrapper.wrapClient(httpClient);
    }

    public boolean terminateInstance(String instanceId) {
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Terminate instance: [instance-id] %s", instanceId));
            }
            URI uri = new URIBuilder(endpoint + INSTANCES_CONTEXT + instanceId).build();
            org.apache.stratos.mock.iaas.client.rest.HttpResponse response = doDelete(uri);
            if (response != null) {
                if ((response.getStatusCode() >= 200) && (response.getStatusCode() < 300)) {
                    return true;
                } else {
                    GsonBuilder gsonBuilder = new GsonBuilder();
                    Gson gson = gsonBuilder.create();
                    org.apache.stratos.mock.iaas.domain.ErrorResponse errorResponse = gson.fromJson(response.getContent(), org.apache.stratos.mock.iaas.domain.ErrorResponse.class);
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

    public HttpResponse doDelete(URI resourcePath) throws Exception {
        HttpDelete httpDelete = null;
        try {
            httpDelete = new HttpDelete(resourcePath);
            httpDelete.addHeader("Content-Type", "application/json");

            return httpClient.execute(httpDelete, new HttpResponseHandler());
        } finally {
            releaseConnection(httpDelete);
        }
    }

    private void releaseConnection(HttpRequestBase request) {
        if (request != null) {
            request.releaseConnection();
        }
    }

}

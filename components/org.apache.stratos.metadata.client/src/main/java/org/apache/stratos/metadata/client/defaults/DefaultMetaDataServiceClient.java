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

package org.apache.stratos.metadata.client.defaults;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.stratos.metadata.client.beans.PropertyBean;
import org.apache.stratos.metadata.client.config.MetaDataClientConfig;
import org.apache.stratos.metadata.client.exception.MetaDataServiceClientException;
import org.apache.stratos.metadata.client.exception.RestClientException;
import org.apache.stratos.metadata.client.rest.DefaultRestClient;
import org.apache.stratos.metadata.client.rest.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;


public class DefaultMetaDataServiceClient implements MetaDataServiceClient {

    private static final Log log = LogFactory.getLog(DefaultMetaDataServiceClient.class);
    private final String baseUrl;
    private RestClient restClient;

    public DefaultMetaDataServiceClient() throws MetaDataServiceClientException {
        MetaDataClientConfig metaDataClientConfig = MetaDataClientConfig.getInstance();
        this.baseUrl = metaDataClientConfig.getMetaDataServiceBaseUrl();
        String username = metaDataClientConfig.getUsername();
        String password = metaDataClientConfig.getPassword();
        try {
            this.restClient = new DefaultRestClient(username, password);
        } catch (RestClientException e) {
            throw new MetaDataServiceClientException("Error occurred while creating REST client ", e);
        }
    }

    public DefaultMetaDataServiceClient(String baseUrl, RestClient restClient) {
        this.baseUrl = baseUrl;
        this.restClient = restClient;
    }

    public void addPropertyToCluster(String appId, String clusterId, String propertyKey, String propertyValue)
            throws MetaDataServiceClientException {

        StringBuilder applicationPath = new StringBuilder(baseUrl).append("application/").append(appId).append("/cluster/").append(clusterId).append("/property");

        PropertyBean property = new PropertyBean(propertyKey, propertyValue);
        try {
            restClient.doPost(applicationPath.toString(), property);
        } catch (RestClientException e) {
            String message = "Error occurred while adding property " + property.getKey();
            log.error(message);
            throw new MetaDataServiceClientException(message, e);
        }
    }

    public PropertyBean getProperty(String appId, String clusterId, String propertyName) throws MetaDataServiceClientException {
        StringBuilder applicationPath = new StringBuilder(baseUrl).
                append("application/").append(appId).append("/cluster/").
                append(clusterId).append("/property/").append(propertyName);
        HttpResponse response;
        try {
            response = restClient.doGet(applicationPath.toString());
        } catch (RestClientException e) {
            String message = "Error occurred while fetching property " + propertyName;
            throw new MetaDataServiceClientException(message, e);
        }

        String responseContent;
        try {
            responseContent = readResponseContent(response);
        } catch (IOException e) {
            throw new MetaDataServiceClientException("Error occurred while reading the response from metadata service", e);
        }
        Gson gson = new Gson();
        return gson.fromJson(responseContent, PropertyBean.class);
    }

    public void deleteApplicationProperties(String appId) throws MetaDataServiceClientException {
        StringBuilder applicationPath = new StringBuilder(baseUrl).append("application/").append(appId);
        HttpResponse response;
        try {
            response = restClient.doDelete(String.valueOf(applicationPath));
        } catch (RestClientException e) {
            String msg = "Error occured while deleting application";
            throw new MetaDataServiceClientException(msg, e);
        }
    }

    private String readResponseContent(HttpResponse response) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }

        return builder.toString();
    }

    public List<PropertyBean> getProperties(String appId, String clusterId)
            throws MetaDataServiceClientException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void terminate() {
        log.info("Terminating the metadata client");
        restClient = null;
    }
}

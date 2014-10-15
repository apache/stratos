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

package org.apache.stratos.metadata.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadata.client.beans.PropertyBean;
import org.apache.stratos.metadata.client.exception.MetaDataServiceClientExeption;
import org.apache.stratos.metadata.client.exception.RestClientException;
import org.apache.stratos.metadata.client.rest.DefaultRestClient;
import org.apache.stratos.metadata.client.rest.RestClient;

import java.util.Map;
import java.util.Set;


public class DefaultMetaDataServiceClient implements MetaDataServiceClient {

    private static final Log log = LogFactory.getLog(DefaultMetaDataServiceClient.class);

    private RestClient restClient;
    private final String baseUrl;

    public DefaultMetaDataServiceClient(String baseUrl) throws RestClientException {
        this.baseUrl = baseUrl;
        this.restClient = new DefaultRestClient();
    }

    public DefaultMetaDataServiceClient(String baseUrl, RestClient restClient) throws RestClientException {
        this.baseUrl = baseUrl;
        this.restClient = restClient;
    }

    public void initialize() {
        // initialization, if any
    }

    public void addPropertyToCluster(String appId, String clusterId, String propertyKey, String propertyValue)
            throws MetaDataServiceClientExeption {
        String applicationPath = baseUrl.concat("application/").concat(appId).concat("/cluster/").concat(clusterId).concat("/property");
        PropertyBean property = new PropertyBean(propertyKey, propertyValue);
        try {
            restClient.doPost(applicationPath, property);
        } catch (RestClientException e) {
            log.error(String.format("Error occurred while adding property %s", propertyKey));
        }
    }

    public Map<String, Set<String>> getProperties(String appId, String clusterId)
            throws MetaDataServiceClientExeption {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<String> getProperty(String appId, String propertyKey) throws MetaDataServiceClientExeption {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<String> getProperty(String appId, String clusterId, String propertyKey)
            throws MetaDataServiceClientExeption {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    public void terminate() throws MetaDataServiceClientExeption {
        restClient = null;
    }
}

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

import org.apache.stratos.metadata.client.exception.MetaDataServiceClientException;

public interface MetaDataServiceClient {

    /**
     * Initialize the MetaDataServiceClient. Should be called once before using the MetaDataServiceClient.
     */
    //public void initialize();

    /**
     * Adds a property key value pair for the relevant cluster of the specified app
     *
     * @param appId         Application id
     * @param clusterId     Cluster id
     * @param propertyKey   Key of the Property
     * @param propertyValue Value of the Property
     * @throws org.apache.stratos.metadata.client.exception.MetaDataServiceClientException
     */
    public void addPropertyToCluster(String appId, String clusterId, String propertyKey, String propertyValue) throws MetaDataServiceClientException;

    /**
     * Get all properties from metadata service.
     *
     * @param appId     Application id
     * @param clusterId Cluster id
     * @return List of properties
     * @throws org.apache.stratos.metadata.client.exception.MetaDataServiceClientException
     */
    public java.util.List<org.apache.stratos.metadata.client.beans.PropertyBean> getProperties(String appId, String clusterId) throws MetaDataServiceClientException;

    /**
     * Fetch a given property from metadata service.
     *
     * @param appId       Application id
     * @param clusterID   Cluster Id
     * @param propertyKey Name of the property
     * @return property
     * @throws org.apache.stratos.metadata.client.exception.MetaDataServiceClientException
     */
    public org.apache.stratos.metadata.client.beans.PropertyBean getProperty(String appId, String clusterID, String propertyKey) throws MetaDataServiceClientException;

    public void deleteApplicationProperties(String applicationId) throws MetaDataServiceClientException;

    /**
     * Shutdown the MetaDataServiceClient. Should be called once after using the client.
     */
    public void terminate();
}

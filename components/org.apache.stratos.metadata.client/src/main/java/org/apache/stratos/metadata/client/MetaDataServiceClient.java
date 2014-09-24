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

import org.apache.stratos.metadata.client.exception.MetaDataServiceClientExeption;

import java.util.Map;
import java.util.Set;

public interface MetaDataServiceClient {

    /**
     * Initialize the MetaDataServiceClient. Should be called once before using the MetaDataServiceClient.
     * @throws MetaDataServiceClientExeption
     */
    public void initialize ();

    /**
     * Adds a property key value pair for the relevant cluster of the specified app
     *
     * @param appId Application id
     * @param clusterId Cluster id
     * @param propertyKey Key of the Property
     * @param propertyValue Value of the Property
     * @throws MetaDataServiceClientExeption
     */
    public void addPropertyToCluster(String appId, String clusterId, String propertyKey, String propertyValue) throws MetaDataServiceClientExeption;

//    /**
//     * Adds a property key value pair for the specified app
//     *
//     * @param appId Application id
//     * @param propertyKey Key of the Property
//     * @param propertyValue Value of the Property
//     * @throws MetaDataServiceClientExeption
//     */
//    public void addPropertyToCluster (String appId, String propertyKey, String propertyValue) throws MetaDataServiceClientExeption;
//
//    /**
//     * Retrieves the property key value pairs for the relevant cluster of the specified app
//     *
//     * @param appId Application id
//     * @param clusterId Cluster id
//     * @return Map of Keys and Values for the specified cluster in the relevant app. Each key can have multiple Values.
//     * @throws MetaDataServiceClientExeption
//     */
    public Map<String, Set<String>> getProperties (String appId, String clusterId) throws MetaDataServiceClientExeption;

//    /**
//     * Retrieves the property key value pairs of the specified app
//     *
//     * @param appId Application id
//     * @return Map of Keys and Values for the specified app. Each key can have multiple Values.
//     * @throws MetaDataServiceClientExeption
//     */
//    public Map<String, Set<String>> getProperties (String appId) throws MetaDataServiceClientExeption;
//
//    /**
//     * Retrieves the property values for the specified key of the relevant app
//     *
//     * @param appId Application id
//     * @param propertyKey Key of the Property
//     * @return Set of Values for specified Key of the relevant app.
//     * @throws MetaDataServiceClientExeption
//     */
    public Set<String> getProperty (String appId, String propertyKey) throws MetaDataServiceClientExeption;

    /**
     * Retrieves the property values for the specified key of the relevant cluster and app
     *
     * @param appId Application id
     * @param clusterId Cluster id
     * @param propertyKey Key of the Property
     * @return Set of Values for specified Key of the relevant cluster in the relevant app.
     * @throws MetaDataServiceClientExeption
     */
    public Set<String> getProperty (String appId, String clusterId, String propertyKey) throws MetaDataServiceClientExeption;

    /**
     * Shutdown the MetaDataServiceClient. Should be called once after using the client.
     *
     * @throws MetaDataServiceClientExeption
     */
    public void terminate () throws MetaDataServiceClientExeption;
}

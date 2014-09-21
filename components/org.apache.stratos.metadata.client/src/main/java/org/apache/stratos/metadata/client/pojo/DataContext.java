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

package org.apache.stratos.metadata.client.pojo;

import java.util.Set;

public class DataContext {

    private String appId;

    private String clusterId;

    private String propertyKey;

    private Set<String> propertyValues;

    private String propertyValue;


    public DataContext (String appId, String propertyKey, Set<String> propertyValues) {

        this.appId = appId;
        this.propertyKey = propertyKey;
        this.propertyValues = propertyValues;

    }

    public DataContext (String appId, String clusterId, String propertyKey, Set<String> propertyValues) {

        this.appId = appId;
        this.clusterId = clusterId;
        this.propertyKey = propertyKey;
        this.propertyValues = propertyValues;
    }

    public DataContext (String appId, String propertyKey, String propertyValue) {

        this.appId = appId;
        this.propertyKey = propertyKey;
        this.propertyValue = propertyValue;
    }

    public DataContext (String appId, String clusterId, String propertyKey, String propertyValue) {

        this.appId = appId;
        this.clusterId = clusterId;
        this.propertyKey = propertyKey;
        this.propertyValue = propertyValue;
    }

    public String getAppId() {
        return appId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getPropertyKey() {
        return propertyKey;
    }

    public Set<String> getPropertyValues() {
        return propertyValues;
    }

    public String getPropertyValue() {
        return propertyValue;
    }
}

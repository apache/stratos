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

package org.apache.stratos.cloud.controller.pojo.payload;

import java.util.Properties;

/**
 * Holds payload/meta data related to a cluster
 */

public class MetaDataHolder {

    private String appId;

    private String groupName;

    private String clusterId;

    private Properties properties;

    public MetaDataHolder (String appId, String clusterId) {

        this.appId = appId;
        this.clusterId = clusterId;
    }

    public MetaDataHolder(String appId, String groupName, String clusterId) {

        this.appId = appId;
        this.groupName = groupName;
        this.clusterId = clusterId;
    }

    public String getAppId() {
        return appId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public boolean equals(Object other) {

        if(other == null || !(other instanceof MetaDataHolder)) {
            return false;
        }

        if(this == other) {
            return true;
        }

        MetaDataHolder that = (MetaDataHolder)other;

        if (this.groupName == null || that.groupName == null) {
            return this.appId.equals(that.appId) && this.clusterId.equals(that.clusterId);
        } else {
            return this.appId.equals(that.appId) && this.groupName.equals(that.groupName) &&
                    this.clusterId.equals(that.clusterId);
        }
    }

    public int hashCode () {

        if (this.getGroupName() == null) {
            return this.appId.hashCode() + this.clusterId.hashCode();
        } else {
            return this.appId.hashCode() + this.groupName.hashCode() + this.clusterId.hashCode();
        }
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}

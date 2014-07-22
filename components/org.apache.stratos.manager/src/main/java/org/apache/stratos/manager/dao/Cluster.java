/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.stratos.manager.dao;

import java.io.Serializable;

public class Cluster implements Serializable {

    private int id;
    private String clusterDomain;
    private String clusterSubDomain;
    private String mgtClusterDomain;
    private String mgtClusterSubDomain;
    private String hostName;
    private String serviceStatus;

    public Cluster() {
    }

    public String getHostName() {
        return hostName;
    }

    public String getClusterDomain() {
        return clusterDomain;
    }

    public void setClusterDomain(String clusterDomain) {
        this.clusterDomain = clusterDomain;
    }

    public String getClusterSubDomain() {
        return clusterSubDomain;
    }

    public void setClusterSubDomain(String clusterSubDomain) {
        this.clusterSubDomain = clusterSubDomain;
    }

    public String getMgtClusterDomain() {
        return mgtClusterDomain;
    }

    public void setMgtClusterDomain(String mgtClusterDomain) {
        this.mgtClusterDomain = mgtClusterDomain;
    }

    public String getMgtClusterSubDomain() {
        return mgtClusterSubDomain;
    }

    public void setMgtClusterSubDomain(String mgtClusterSubDomain) {
        this.mgtClusterSubDomain = mgtClusterSubDomain;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    //public int getId() {
    //    return id;
    //}

    public void setId(int id) {
        this.id = id;
    }

    public String getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(String serviceStatus) {
        this.serviceStatus = serviceStatus;
    }

    public String toString () {
        return clusterDomain;
    }
}

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

package org.apache.stratos.mock.iaas.domain;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Mock member context.
 */
@XmlRootElement(name = "mockInstanceContext")
public class MockInstanceContext implements Serializable {

    private static final long serialVersionUID = 5511318098405943180L;

    private String applicationId;
    private String serviceName;
    private String clusterId;
    private String memberId;
    private String clusterInstanceId;
    private String networkPartitionId;
    private String partitionId;
    private String defaultPrivateIP;
    private String defaultPublicIP;
    private String instanceId;

    public MockInstanceContext(){
    }

    public MockInstanceContext(String applicationId, String serviceName, String clusterId, String memberId,
                               String clusterInstanceId, String networkPartitionId, String partitionId) {
        this.setApplicationId(applicationId);
        this.setServiceName(serviceName);
        this.setClusterId(clusterId);
        this.setMemberId(memberId);
        this.setClusterInstanceId(clusterInstanceId);
        this.setNetworkPartitionId(networkPartitionId);
        this.setPartitionId(partitionId);
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getClusterInstanceId() {
        return clusterInstanceId;
    }

    public void setClusterInstanceId(String clusterInstanceId) {
        this.clusterInstanceId = clusterInstanceId;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public void setNetworkPartitionId(String networkPartitionId) {
        this.networkPartitionId = networkPartitionId;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(String partitionId) {
        this.partitionId = partitionId;
    }

    public String getDefaultPrivateIP() {
        return defaultPrivateIP;
    }

    public void setDefaultPrivateIP(String defaultPrivateIP) {
        this.defaultPrivateIP = defaultPrivateIP;
    }

    public String getDefaultPublicIP() {
        return defaultPublicIP;
    }

    public void setDefaultPublicIP(String defaultPublicIP) {
        this.defaultPublicIP = defaultPublicIP;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}

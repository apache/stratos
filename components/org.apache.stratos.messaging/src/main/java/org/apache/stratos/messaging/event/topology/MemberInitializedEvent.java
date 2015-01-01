/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.messaging.event.topology;


import java.io.Serializable;
import java.util.List;
import java.util.Properties;


/**
 * This event is fired by Cloud Controller once the member initialization process is done.
 */
public class MemberInitializedEvent extends TopologyEvent implements Serializable {
    private static final long serialVersionUID = 2672909702971355178L;

    private final String serviceName;
    private final String clusterId;
    private final String clusterInstanceId;
    private final String networkPartitionId;
    private final String partitionId;
    private final String memberId;

    private List<String> memberPublicIPs;
    private String defaultPublicIP;
    private List<String> memberPrivateIPs;
    private String defaultPrivateIP;
    private Properties properties;

    public MemberInitializedEvent(String serviceName, String clusterId, String clusterInstanceId, String memberId,
                                  String networkPartitionId, String partitionId) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        this.clusterInstanceId = clusterInstanceId;
        this.memberId = memberId;
        this.networkPartitionId = networkPartitionId;
        this.partitionId = partitionId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public String getMemberId() {
        return memberId;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getClusterInstanceId() {
        return clusterInstanceId;
    }

    public String getDefaultPublicIP() {
        return defaultPublicIP;
    }

    public void setDefaultPublicIP(String defaultPublicIP) {
        this.defaultPublicIP = defaultPublicIP;
    }

    public List<String> getMemberPublicIPs() {
        return memberPublicIPs;
    }

    public void setMemberPublicIPs(List<String> memberPublicIPs) {
        this.memberPublicIPs = memberPublicIPs;
    }

    public String getDefaultPrivateIP() {
        return defaultPrivateIP;
    }

    public void setDefaultPrivateIP(String defaultPrivateIP) {
        this.defaultPrivateIP = defaultPrivateIP;
    }

    public List<String> getMemberPrivateIPs() {
        return memberPrivateIPs;
    }

    public void setMemberPrivateIPs(List<String> memberPrivateIPs) {
        this.memberPrivateIPs = memberPrivateIPs;
    }
}

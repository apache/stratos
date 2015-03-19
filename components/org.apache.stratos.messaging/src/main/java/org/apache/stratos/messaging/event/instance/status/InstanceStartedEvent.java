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
package org.apache.stratos.messaging.event.instance.status;


import java.io.Serializable;

/**
 * This event is fired by cartridge agent when its started.
 */
public class InstanceStartedEvent extends InstanceStatusEvent implements Serializable {
    private static final long serialVersionUID = 7447068435627208619L;

    private final String applicationId;
    private final String serviceName;
    private final String clusterId;
    private final String memberId;
    private final String clusterInstanceId;
    private final String networkPartitionId;
    private final String partitionId;
    private String groupId;

    public InstanceStartedEvent(String applicationId, String serviceName, String clusterId, String memberId,
                                String clusterInstanceId, String networkPartitionId, String partitionId) {
        this.applicationId = applicationId;
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        this.memberId = memberId;
        this.clusterInstanceId = clusterInstanceId;
        this.networkPartitionId = networkPartitionId;
        this.partitionId = partitionId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getClusterInstanceId() {
        return clusterInstanceId;
    }

    @Override
    public String toString() {
        return String.format("[service-name] %s [cluster-id] %s [cluster-instance-id] %s [member-id] %s" +
                        "[network-partition-id] %s [partition-id] %s", getServiceName(), getClusterId(),
                getClusterInstanceId(), getMemberId(), getNetworkPartitionId(), getPartitionId());
    }
}

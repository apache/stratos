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

package org.apache.stratos.cloud.controller.iaases.mock;

import java.io.Serializable;

/**
 * Mock member context.
 */
public class MockMemberContext implements Serializable {
    private final String serviceName;
    private final String clusterId;
    private final String memberId;
    private final String networkPartitionId;
    private final String partitionId;
    private final String instanceId;

    public MockMemberContext(String serviceName, String clusterId, String memberId,
                             String networkPartitionId, String partitionId, String instanceId) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        this.memberId = memberId;
        this.networkPartitionId = networkPartitionId;
        this.partitionId = partitionId;
        this.instanceId = instanceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public String getInstanceId() {
        return instanceId;
    }
}

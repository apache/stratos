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
package org.apache.stratos.autoscaler.context.partition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.pojo.policy.deployment.partition.ChildLevelPartition;
import org.apache.stratos.cloud.controller.stub.domain.Partition;
import java.io.Serializable;
import java.util.*;
/**
 * This is an object that inserted to the rules engine.
 * Holds information about a partition.
 *
 *
 */

public class PartitionContext implements Serializable{

    private static final long serialVersionUID = -2920388667345980487L;
    private static final Log log = LogFactory.getLog(ClusterLevelPartitionContext.class);
    protected String partitionId;
    private Partition partition;
    private ChildLevelPartition childLevelPartition;
    private String networkPartitionId;
    // properties
    private Properties properties;

    // for the use of tests
    public PartitionContext(long memberExpiryTime) {

    }

    public PartitionContext(Partition partition, ChildLevelPartition childLevelPartition) {

        this.partition = partition;
        this.childLevelPartition = childLevelPartition;
    }

    public Partition getPartition() {
        return partition;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(String partitionId) {
        this.partitionId = partitionId;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public void setNetworkPartitionId(String networkPartitionId) {
        this.networkPartitionId = networkPartitionId;
    }

    public int getCurrentElementCount() {
        //TODO find and return correct member instance count
        return 0;
    }

    public ChildLevelPartition getChildLevelPartition() {
        return childLevelPartition;
    }
}

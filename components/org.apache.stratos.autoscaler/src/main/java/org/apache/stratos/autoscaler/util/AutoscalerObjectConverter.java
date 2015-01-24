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

package org.apache.stratos.autoscaler.util;

import org.apache.stratos.cloud.controller.stub.domain.Partition;

import java.util.ArrayList;
import java.util.List;

/**
 * Autoscaler object converter.
 */
public class AutoscalerObjectConverter {

    /**
     * Convert autoscaler partitions to cloud controller stub partitions
     * @param partitions
     * @return
     */
    public static org.apache.stratos.cloud.controller.stub.domain.Partition[]
        convertASPartitionsToCCStubPartitions(org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.Partition[] partitions) {

        List<Partition> cloudControllerPartitions = new ArrayList<Partition>();
        for(org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.Partition partition : partitions) {
            Partition cloudControllerPartition = convertASPartitionToCCPartition(partition);
            cloudControllerPartitions.add(cloudControllerPartition);
        }
        return cloudControllerPartitions.toArray(new Partition[cloudControllerPartitions.size()]);
    }

    /**
     * Convert autoscaler partition to cloud controller partition
     * @param partition
     * @return
     */
    public static org.apache.stratos.cloud.controller.stub.domain.Partition
        convertASPartitionToCCPartition(org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.Partition partition) {

        org.apache.stratos.cloud.controller.stub.domain.Partition cloudControllerPartition = new
                org.apache.stratos.cloud.controller.stub.domain.Partition();

        cloudControllerPartition.setId(partition.getId());
        cloudControllerPartition.setProvider(partition.getProvider());
        cloudControllerPartition.setDescription(partition.getDescription());
        cloudControllerPartition.setKubernetesClusterId(partition.getKubernetesClusterId());
        cloudControllerPartition.setIsPublic(partition.getIsPublic());
        cloudControllerPartition.setProperties(AutoscalerUtil.toStubProperties(partition.getProperties()));

        return cloudControllerPartition;
    }
}

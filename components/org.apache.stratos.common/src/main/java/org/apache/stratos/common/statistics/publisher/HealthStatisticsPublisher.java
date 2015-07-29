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

package org.apache.stratos.common.statistics.publisher;

/**
 * Health statistics publisher interface.
 */
public interface HealthStatisticsPublisher extends StatisticsPublisher {

    /**
     * Publish health statistics to complex event processor.
     *
     * @param timeStamp          time
     * @param clusterId          Cluster id of the member
     * @param clusterInstanceId  Cluster instance id of the member
     * @param networkPartitionId Network partition id of the member
     * @param memberId           Member id
     * @param partitionId        Partition id of the member
     * @param health             Health type: memory_consumption | load_average
     * @param value              Health type value
     */
    void publish(Long timeStamp, String clusterId, String clusterInstanceId, String networkPartitionId,
                 String memberId, String partitionId, String health, double value);
}

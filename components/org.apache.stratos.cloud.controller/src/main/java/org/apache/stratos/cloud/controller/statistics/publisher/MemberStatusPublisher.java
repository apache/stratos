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

package org.apache.stratos.cloud.controller.statistics.publisher;

import org.apache.stratos.common.statistics.publisher.StatisticsPublisher;

/**
 * Member Status Publisher Interface.
 */
public interface MemberStatusPublisher extends StatisticsPublisher {
    /**
     * Publishing member status.
     *
     * @param timestamp          Status changed time
     * @param applicationId      Application Id
     * @param clusterId          Cluster Id
     * @param clusterAlias       Cluster Alias
     * @param clusterInstanceId  Cluster Instance Id
     * @param serviceName        Service Name
     * @param networkPartitionId Network Partition Id
     * @param partitionId        Partition Id
     * @param memberId           Member Id
     * @param status             Member Status
     */
    void publish(Long timestamp, String applicationId, String clusterId,
                 String clusterAlias, String clusterInstanceId, String serviceName,
                 String networkPartitionId, String partitionId, String memberId, String status);
}

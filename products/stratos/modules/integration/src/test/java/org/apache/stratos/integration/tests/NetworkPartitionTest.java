/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.integration.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.partition.NetworkPartitionBean;
import org.apache.stratos.integration.tests.rest.RestClient;

/**
 * Test to handle Network partition CRUD operations
 */
public class NetworkPartitionTest {
    private static final Log log = LogFactory.getLog(NetworkPartitionTest.class);
    private static final String networkPartitions = "/network-partitions/mock/";
    private static final String networkPartitionsUpdate = "/network-partitions/mock/update/";
    private static final String entityName = "networkPartition";

    public boolean addNetworkPartition(String networkPartitionId, RestClient restClient) {
        return restClient.addEntity(networkPartitions + "/" + networkPartitionId,
                RestConstants.NETWORK_PARTITIONS, entityName);
    }

    public NetworkPartitionBean getNetworkPartition(String networkPartitionId,
                                                    RestClient restClient) {
        NetworkPartitionBean bean = (NetworkPartitionBean) restClient.
                getEntity(RestConstants.NETWORK_PARTITIONS, networkPartitionId,
                        NetworkPartitionBean.class, entityName);
        return bean;
    }

    public boolean updateNetworkPartition(String networkPartitionId, RestClient restClient) {
        return restClient.updateEntity(networkPartitionsUpdate + "/" + networkPartitionId,
                RestConstants.NETWORK_PARTITIONS, entityName);
    }

    public boolean removeNetworkPartition(String networkPartitionId, RestClient restClient) {
        return restClient.removeEntity(RestConstants.NETWORK_PARTITIONS, networkPartitionId, entityName);
    }
}

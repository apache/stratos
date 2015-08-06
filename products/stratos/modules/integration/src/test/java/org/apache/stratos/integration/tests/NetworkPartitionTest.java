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
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Test to handle Network partition CRUD operations
 */
public class NetworkPartitionTest extends StratosTestServerManager {
    private static final Log log = LogFactory.getLog(NetworkPartitionTest.class);

    @Test
    public void testNetworkPartition() {
        try {
            String networkPartitionId = "network-partition-3";
            log.info("Started network partition test case**************************************");

            boolean added = restClient.addEntity(RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            networkPartitionId + ".json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);

            assertEquals(added, true);
            NetworkPartitionBean bean = (NetworkPartitionBean) restClient.
                    getEntity(RestConstants.NETWORK_PARTITIONS, networkPartitionId,
                            NetworkPartitionBean.class, RestConstants.NETWORK_PARTITIONS_NAME);

            assertEquals(bean.getId(), "network-partition-3");
            assertEquals(bean.getPartitions().size(), 1);
            assertEquals(bean.getPartitions().get(0).getId(), "partition-1");
            assertEquals(bean.getPartitions().get(0).getProperty().get(0).getName(), "region");
            assertEquals(bean.getPartitions().get(0).getProperty().get(0).getValue(), "default");

            boolean updated = restClient.updateEntity(RestConstants.NETWORK_PARTITIONS_PATH + "/" +
                            networkPartitionId + "-v1.json",
                    RestConstants.NETWORK_PARTITIONS, RestConstants.NETWORK_PARTITIONS_NAME);

            assertEquals(updated, true);
            NetworkPartitionBean updatedBean = (NetworkPartitionBean) restClient.
                    getEntity(RestConstants.NETWORK_PARTITIONS, networkPartitionId,
                            NetworkPartitionBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(updatedBean.getId(), "network-partition-3");
            assertEquals(updatedBean.getPartitions().size(), 2);
            assertEquals(updatedBean.getPartitions().get(1).getId(), "partition-2");
            assertEquals(updatedBean.getPartitions().get(1).getProperty().get(0).getName(), "region");
            assertEquals(updatedBean.getPartitions().get(1).getProperty().get(0).getValue(), "default1");
            assertEquals(updatedBean.getPartitions().get(1).getProperty().get(1).getName(), "zone");
            assertEquals(updatedBean.getPartitions().get(1).getProperty().get(1).getValue(), "z1");

            boolean removed = restClient.removeEntity(RestConstants.NETWORK_PARTITIONS,
                    networkPartitionId, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(removed, true);

            NetworkPartitionBean beanRemoved = (NetworkPartitionBean) restClient.
                    getEntity(RestConstants.NETWORK_PARTITIONS, networkPartitionId,
                            NetworkPartitionBean.class, RestConstants.NETWORK_PARTITIONS_NAME);
            assertEquals(beanRemoved, null);

            log.info("Ended network partition test case**************************************");
        } catch (Exception e) {
            log.error("An error occurred while handling network partitions", e);
            assertTrue("An error occurred while handling network partitions", false);
        }
    }
}

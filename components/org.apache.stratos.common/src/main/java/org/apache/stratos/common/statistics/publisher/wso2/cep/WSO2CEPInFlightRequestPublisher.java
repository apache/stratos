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

package org.apache.stratos.common.statistics.publisher.wso2.cep;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.statistics.publisher.InFlightRequestPublisher;
import org.apache.stratos.common.statistics.publisher.ThriftStatisticsPublisher;
import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.StreamDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * WSO2 CEP in flight request count publisher.
 * <p/>
 * In-flight request count:
 * Number of requests being served at a given moment could be identified as in-flight request count.
 */
public class WSO2CEPInFlightRequestPublisher extends ThriftStatisticsPublisher implements InFlightRequestPublisher {
    private static final Log log = LogFactory.getLog(WSO2CEPInFlightRequestPublisher.class);

    private static final String STATS_PUBLISHER_ENABLED = "cep.stats.publisher.enabled";
    private static final String DATA_STREAM_NAME = "in_flight_requests";
    private static final String VERSION = "1.0.0";
    private static final String CEP_THRIFT_CLIENT_NAME = "cep";

    public WSO2CEPInFlightRequestPublisher() {
        super(createStreamDefinition(), STATS_PUBLISHER_ENABLED, CEP_THRIFT_CLIENT_NAME);
    }

    private static StreamDefinition createStreamDefinition() {
        try {
            // Create stream definition
            StreamDefinition streamDefinition = new StreamDefinition(DATA_STREAM_NAME, VERSION);
            streamDefinition.setNickName("lb stats");
            streamDefinition.setDescription("lb stats");
            List<Attribute> payloadData = new ArrayList<Attribute>();

            // Set payload definition
            payloadData.add(new Attribute("cluster_id", AttributeType.STRING));
            payloadData.add(new Attribute("cluster_instance_id", AttributeType.STRING));
            payloadData.add(new Attribute("network_partition_id", AttributeType.STRING));
            payloadData.add(new Attribute("in_flight_request_count", AttributeType.DOUBLE));
            streamDefinition.setPayloadData(payloadData);
            return streamDefinition;
        } catch (Exception e) {
            throw new RuntimeException("Could not create stream definition", e);
        }
    }

    /**
     * Publish in-flight request count of a cluster.
     *
     * @param clusterId
     * @param clusterInstanceId
     * @param networkPartitionId
     * @param inFlightRequestCount
     */
    @Override
    public void publish(String clusterId, String clusterInstanceId, String networkPartitionId,
                        int inFlightRequestCount) {
        // Set payload values
        List<Object> payload = new ArrayList<Object>();

        if (log.isDebugEnabled()) {
            log.debug(String.format("Publishing health statistics: [timestamp] %d [cluster] %s " +
                            "[cluster-instance] %s [network-partition] %s [in-flight-request-count] %d",
                    clusterId, clusterInstanceId, networkPartitionId, inFlightRequestCount));
        }
        payload.add(clusterId);
        payload.add(clusterInstanceId);
        payload.add(networkPartitionId);
        payload.add((double) inFlightRequestCount);

        super.publish(payload.toArray());
    }
}

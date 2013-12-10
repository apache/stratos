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

package org.apache.stratos.load.balancer.common.statistics;

import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.StreamDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * WSO2 CEP in flight request count publisher.
 *
 * In-flight request count:
 * Number of requests being served at a given moment could be identified as in-flight request count.
 */
public class WSO2CEPInFlightRequestPublisher extends WSO2CEPStatsPublisher {

    private static final String DATA_STREAM_NAME = "stratos.lb.stats";
    private static final String VERSION = "1.0.0";

    private static StreamDefinition createStreamDefinition() {
        try {
            StreamDefinition streamDefinition = new StreamDefinition(DATA_STREAM_NAME, VERSION);
            streamDefinition.setNickName("lb stats");
            streamDefinition.setDescription("lb stats");
            List<Attribute> payloadData = new ArrayList<Attribute>();
            // Payload definition
            payloadData.add(new Attribute("cluster_id", AttributeType.STRING));
            payloadData.add(new Attribute("partition_id", AttributeType.STRING));
            payloadData.add(new Attribute("in_flight_requests", AttributeType.INT));
            streamDefinition.setPayloadData(payloadData);
            return streamDefinition;
        }
        catch (Exception e) {
            throw new RuntimeException("Could not create stream definition", e);
        }
    }

    public WSO2CEPInFlightRequestPublisher() {
        super(createStreamDefinition());
    }

    /**
     * Publish in-flight request count of a cluster.
     *
     * @param clusterId
     * @param partitionId
     * @param inFlightRequestCount
     */
    public void publish(String clusterId, String partitionId, int inFlightRequestCount) {
        List<Object> payload = new ArrayList<Object>();
        // Payload values
        payload.add(clusterId);
        payload.add(partitionId);
        payload.add(inFlightRequestCount);
        super.publish(payload.toArray());
    }
}

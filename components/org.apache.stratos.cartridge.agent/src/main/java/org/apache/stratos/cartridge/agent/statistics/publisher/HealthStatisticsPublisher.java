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

package org.apache.stratos.cartridge.agent.statistics.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.statistics.publisher.WSO2CEPStatisticsPublisher;
import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.StreamDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Health statistics publisher for publishing statistics to CEP.
 */
public class HealthStatisticsPublisher extends WSO2CEPStatisticsPublisher {
    private static final Log log = LogFactory.getLog(HealthStatisticsPublisher.class);

    private static final String DATA_STREAM_NAME = "cartridge_agent_health_stats";
    private static final String VERSION = "1.0.0";

    private static StreamDefinition createStreamDefinition() {
        try {
            StreamDefinition streamDefinition = new StreamDefinition(DATA_STREAM_NAME, VERSION);
            streamDefinition.setNickName("agent health stats");
            streamDefinition.setDescription("agent health stats");
            List<Attribute> payloadData = new ArrayList<Attribute>();

            payloadData.add(new Attribute("cluster_id", AttributeType.STRING));
            payloadData.add(new Attribute("network_partition_id", AttributeType.STRING));
            payloadData.add(new Attribute("member_id", AttributeType.STRING));
            payloadData.add(new Attribute("partition_id", AttributeType.STRING));
            payloadData.add(new Attribute("health_description", AttributeType.STRING));
            payloadData.add(new Attribute("value", AttributeType.DOUBLE));

            streamDefinition.setPayloadData(payloadData);
            return streamDefinition;
        } catch (Exception e) {
            throw new RuntimeException("Could not create stream definition", e);
        }
    }

    public HealthStatisticsPublisher() {
        super(createStreamDefinition());
    }

    /**
     * Publish health statistics to cep.
     * @param clusterId
     * @param networkPartitionId
     * @param memberId
     * @param partitionId
     * @param health
     * @param value
     */
    public void publish(String clusterId, String networkPartitionId, String memberId, String partitionId, String health, double value) {
        if(log.isInfoEnabled()) {
            log.info(String.format("Publishing health statistics: [cluster] %s [network-partition] %s [partition] %s [member] %s [health] %s [value] %d",
                    clusterId, networkPartitionId, partitionId, memberId, health, value));
        }
        List<Object> payload = new ArrayList<Object>();
        // Payload values
        payload.add(clusterId);
        payload.add(networkPartitionId);
        payload.add(memberId);
        payload.add(partitionId);
        payload.add(health);
        payload.add(value);
        super.publish(payload.toArray());
    }
}

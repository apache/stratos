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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.agent.thrift.Agent;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;
import org.wso2.carbon.databridge.agent.thrift.conf.AgentConfiguration;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * WSO2 CEP statistics publisher for the load balancer.
 */
public class WSO2CEPStatsPublisher implements LoadBalancerStatsPublisher {
    private static final Log log = LogFactory.getLog(WSO2CEPStatsPublisher.class);

    private static final String CALL_CENTER_DATA_STREAM = "stratos.lb.stats";
    private static final String VERSION = "1.0.0";
    private AsyncDataPublisher asyncDataPublisher;
    private String ip;
    private String port;
    private boolean enabled = false;

    public WSO2CEPStatsPublisher() {
        ip = System.getProperty("thrift.receiver.ip");
        port = System.getProperty("thrift.receiver.port");
        enabled = Boolean.getBoolean("load.balancer.cep.stats.publisher.enabled");

        if(enabled) {
            init();
        }
    }

    private void init() {
        AgentConfiguration agentConfiguration = new AgentConfiguration();
        Agent agent = new Agent(agentConfiguration);

        // Initialize asynchronous data publisher
        asyncDataPublisher = new AsyncDataPublisher("tcp://"+ip+":"+port+"", "admin", "admin", agent);
        String streamDefinition = "{" +
                " 'name':'" + CALL_CENTER_DATA_STREAM + "'," +
                " 'version':'" + VERSION + "'," +
                " 'nickName': 'lb stats'," +
                " 'description': 'lb stats'," +
                " 'metaData':[]," +
                " 'payloadData':[" +
                " {'name':'cluster_id','type':'STRING'}," +
                " {'name':'in_flight_requests','type':'INT'}" +
                " ]" +
                "}";
        asyncDataPublisher.addStreamDefinition(streamDefinition, CALL_CENTER_DATA_STREAM, VERSION);
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if(this.enabled) {
            init();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void publish(Map<String, Integer> stats) {
        if(!isEnabled()) {
            throw new RuntimeException("Statistics publisher is not enabled");
        }

        for (Map.Entry<String, Integer> entry : stats.entrySet()) {

            Object[] payload = new Object[]{entry.getKey(), entry.getValue()};
            Event event = eventObject(null, null, payload, new HashMap<String, String>());
            try {
                if(log.isInfoEnabled()) {
                    log.info(String.format("Publishing statistics event: [stream] %s [version] %s [payload] %s", CALL_CENTER_DATA_STREAM, VERSION, Arrays.toString(payload)));
                }
                asyncDataPublisher.publish(CALL_CENTER_DATA_STREAM, VERSION, event);
            } catch (AgentException e) {
                log.error("Failed to publish events. ", e);
            }

        }
        stats = null;
    }

    private static Event eventObject(Object[] correlationData, Object[] metaData,
                                     Object[] payLoadData, HashMap<String, String> map) {
        Event event = new Event();
        event.setCorrelationData(correlationData);
        event.setMetaData(metaData);
        event.setPayloadData(payLoadData);
        event.setArbitraryDataMap(map);
        return event;
    }
}

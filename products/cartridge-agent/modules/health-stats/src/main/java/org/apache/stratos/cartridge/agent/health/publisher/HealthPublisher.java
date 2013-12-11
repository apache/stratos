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

package org.apache.stratos.cartridge.agent.health.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.agent.thrift.Agent;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;
import org.wso2.carbon.databridge.agent.thrift.conf.AgentConfiguration;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public class HealthPublisher implements Observer {

    private static final Log log = LogFactory.getLog(HealthPublisher.class);
    private static final String DATA_STREAM_NAME = "cartridge_agent_health_stats";
    private static final String VERSION = "1.0.0";
    private AsyncDataPublisher asyncDataPublisher;

    public HealthPublisher() {
        try {
            //TODO read following from a config file
            String ip = System.getProperty("thrift.receiver.ip");
            String port = System.getProperty("thrift.receiver.port");
            String keyFilePath = System.getProperty("key.file.path");

            AgentConfiguration agentConfiguration = new AgentConfiguration();

            // TODO get following from somewhere, without hard-coding.
            System.setProperty("javax.net.ssl.trustStore", keyFilePath);
            System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

            Agent agent = new Agent(agentConfiguration);

            //Using Asynchronous data publisher
            asyncDataPublisher = new AsyncDataPublisher("tcp://" + ip + ":" + port + "", "admin", "admin", agent);
            String streamDefinition = "{" +
                    " 'name':'" + DATA_STREAM_NAME + "'," +
                    " 'version':'" + VERSION + "'," +
                    " 'nickName': 'health stats'," +
                    " 'description': 'health stats'," +
                    " 'metaData':[]," +
                    " 'payloadData':[" +
                    " {'name':'health_description','type':'STRING'}," +
                    " {'name':'value','type':'DOUBLE'}," +
                    " {'name':'member_id','type':'STRING'}," +
                    " {'name':'cluster_id','type':'STRING'}," +
                    " {'name':'partition_id','type':'STRING'}" +
                    " ]" +
                    "}";
            asyncDataPublisher.addStreamDefinition(streamDefinition, DATA_STREAM_NAME, VERSION);

        } catch (NullPointerException e) {
            if (log.isErrorEnabled()) {
                log.error("Get null values", e);
            }
        }
    }

    public void update(Observable arg0, Object arg1) {
        if (arg1 != null && arg1 instanceof Map<?, ?>) {
            Map<String, Double> stats = (Map<String, Double>) arg1;
            publishEvents(stats);
        }
    }

    public void update(Object healthStatObj) {
        if (healthStatObj != null && healthStatObj instanceof Map<?, ?>) {
            Map<String, Double> stats = (Map<String, Double>) healthStatObj;
            publishEvents(stats);
        }
    }

    private void publishEvents(Map<String, Double> stats) {

        String memberID = System.getProperty("member.id");
        String clusterID = System.getProperty("cluster.id");
        String partitionId = System.getProperty("partition.id");

        for (Map.Entry<String, Double> entry : stats.entrySet()) {

            Object[] payload = new Object[]{entry.getKey(), entry.getValue(), memberID, clusterID,partitionId};
            Event event = eventObject(null, null, payload, new HashMap<String, String>());
            try {
                asyncDataPublisher.publish(DATA_STREAM_NAME, VERSION, event);
            } catch (AgentException e) {
                log.error("Failed to publish health stats. ", e);
            }

        }
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
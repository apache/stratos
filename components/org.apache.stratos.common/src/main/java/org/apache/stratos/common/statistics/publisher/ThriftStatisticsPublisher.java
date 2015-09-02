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

package org.apache.stratos.common.statistics.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.agent.thrift.Agent;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;
import org.wso2.carbon.databridge.agent.thrift.conf.AgentConfiguration;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.StreamDefinition;

import java.util.HashMap;

/**
 * Thrift statistics publisher.
 */
public class ThriftStatisticsPublisher implements StatisticsPublisher {

    private static final Log log = LogFactory.getLog(ThriftStatisticsPublisher.class);

    private StreamDefinition streamDefinition;
    private AsyncDataPublisher asyncDataPublisher;
    private String ip;
    private String port;
    private String username;
    private String password;
    private boolean enabled = false;

    /**
     * Credential information stored inside thrift-client-config.xml file
     * is parsed and assigned into ip,port,username and password fields
     *
     * @param streamDefinition      Thrift Event Stream Definition
     * @param thriftClientName      Thrift Client Name
     */
    public ThriftStatisticsPublisher(StreamDefinition streamDefinition, String thriftClientName) {
        ThriftClientConfig thriftClientConfig = ThriftClientConfig.getInstance();
        ThriftClientInfo thriftClientInfo = thriftClientConfig.getThriftClientInfo(thriftClientName);

        this.streamDefinition = streamDefinition;
        this.enabled = thriftClientInfo.isStatsPublisherEnabled();
        this.ip = thriftClientInfo.getIp();
        this.port = thriftClientInfo.getPort();
        this.username = thriftClientInfo.getUsername();
        this.password = thriftClientInfo.getPassword();

        if (enabled) {
            init();
        }
    }

    private void init() {
        AgentConfiguration agentConfiguration = new AgentConfiguration();
        Agent agent = new Agent(agentConfiguration);

        // Initialize asynchronous data publisher
        asyncDataPublisher = new AsyncDataPublisher("tcp://" + ip + ":" + port + "", username, password, agent);
        asyncDataPublisher.addStreamDefinition(streamDefinition);
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (this.enabled) {
            init();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void publish(Object[] payload) {
        if (!isEnabled()) {
            throw new RuntimeException("Statistics publisher is not enabled");
        }

        Event event = new Event();
        event.setPayloadData(payload);
        event.setArbitraryDataMap(new HashMap<String, String>());

        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Publishing thrift event: [stream] %s [version] %s",
                        streamDefinition.getName(), streamDefinition.getVersion()));
            }
            asyncDataPublisher.publish(streamDefinition.getName(), streamDefinition.getVersion(), event);
        } catch (AgentException e) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Could not publish thrift event: [stream] %s [version] %s",
                        streamDefinition.getName(), streamDefinition.getVersion()), e);
            }
        }
    }
}

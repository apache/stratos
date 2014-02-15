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

package org.apache.stratos.cartridge.agent.data.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.agent.thrift.Agent;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;
import org.wso2.carbon.databridge.agent.thrift.conf.AgentConfiguration;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.StreamDefinition;

import java.util.Date;

public abstract class DataPublisher {

    private static final Log log = LogFactory.getLog(DataPublisher.class);

    private StreamDefinition streamDefinition;
    private DataPublisherConfiguration dataPublisherConfig;
    private AsyncDataPublisher dataPublisher;
    private boolean isDataPublisherInitialized;

    public DataPublisher (DataPublisherConfiguration dataPublisherConfig, StreamDefinition streamDefinition) {

        this.dataPublisherConfig = dataPublisherConfig;
        this.streamDefinition = streamDefinition;
        this.setDataPublisherInitialized(false);
    }

    public void initialize () {

        AgentConfiguration agentConfiguration = new AgentConfiguration();
        //System.setProperty("javax.net.ssl.trustStore", "/home/isuru/wso2/S2/apache/stratos/alpha/wso2bam-2.4.0/repository/resources/security/client-truststore.jks");
        //System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        Agent agent = new Agent(agentConfiguration);

        dataPublisher = new AsyncDataPublisher(dataPublisherConfig.getMonitoringServerUrl(), dataPublisherConfig.getAdminUsername(),
                dataPublisherConfig.getAdminPassword(), agent);

        if (!dataPublisher.isStreamDefinitionAdded(streamDefinition.getName(), streamDefinition.getVersion())) {
            dataPublisher.addStreamDefinition(streamDefinition);
        }

        setDataPublisherInitialized(true);

        log.info("DataPublisher initialized");
    }

    protected void publish (DataContext dataContext) {

        Event event = new Event();
        event.setTimeStamp(new Date().getTime());
        event.setMetaData(dataContext.getMetaData());
        event.setPayloadData(dataContext.getPayloadData());

        try {
            dataPublisher.publish(streamDefinition.getName(), streamDefinition.getVersion(), event);

        } catch (AgentException e) {
            String errorMsg = "Error in publishing event";
            log.error(errorMsg, e);
            // no need to throw here
        }
    }

    protected void terminate () {

        dataPublisher.stop();
    }

    public boolean isDataPublisherInitialized() {
        return isDataPublisherInitialized;
    }

    public void setDataPublisherInitialized(boolean dataPublisherInitialized) {
        isDataPublisherInitialized = dataPublisherInitialized;
    }
}

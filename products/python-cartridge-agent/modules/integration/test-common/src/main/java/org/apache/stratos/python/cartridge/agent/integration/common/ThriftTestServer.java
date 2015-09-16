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

package org.apache.stratos.python.cartridge.agent.integration.common;

import org.apache.log4j.Logger;
import org.wso2.carbon.databridge.commons.Credentials;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.databridge.commons.utils.EventDefinitionConverterUtils;
import org.wso2.carbon.databridge.core.AgentCallback;
import org.wso2.carbon.databridge.core.DataBridge;
import org.wso2.carbon.databridge.core.Utils.AgentSession;
import org.wso2.carbon.databridge.core.definitionstore.InMemoryStreamDefinitionStore;
import org.wso2.carbon.databridge.core.exception.DataBridgeException;
import org.wso2.carbon.databridge.core.exception.StreamDefinitionStoreException;
import org.wso2.carbon.databridge.core.internal.authentication.AuthenticationHandler;
import org.wso2.carbon.databridge.receiver.thrift.ThriftDataReceiver;
import org.wso2.carbon.user.api.UserStoreException;

import java.net.SocketException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ThriftTestServer {
    private Logger log = Logger.getLogger(ThriftTestServer.class);
    private ThriftDataReceiver thriftDataReceiver;
    private InMemoryStreamDefinitionStore streamDefinitionStore;
    private AtomicInteger numberOfEventsReceived;
    private RestarterThread restarterThread;
    private DataBridge databridge;

    public void startTestServer() throws DataBridgeException, InterruptedException {
        ThriftTestServer thriftTestServer = new ThriftTestServer();
        thriftTestServer.start(7611);
        Thread.sleep(100000000);
        thriftTestServer.stop();
    }


    public void addStreamDefinition(StreamDefinition streamDefinition, int tenantId)
            throws StreamDefinitionStoreException {
        streamDefinitionStore.saveStreamDefinitionToStore(streamDefinition, tenantId);
    }

    public void addStreamDefinition(String streamDefinitionStr, int tenantId)
            throws StreamDefinitionStoreException, MalformedStreamDefinitionException {
        StreamDefinition streamDefinition = EventDefinitionConverterUtils.convertFromJson(streamDefinitionStr);
        getStreamDefinitionStore().saveStreamDefinitionToStore(streamDefinition, tenantId);
    }

    private InMemoryStreamDefinitionStore getStreamDefinitionStore() {
        if (streamDefinitionStore == null) {
            streamDefinitionStore = new InMemoryStreamDefinitionStore();
        }
        return streamDefinitionStore;
    }

    public void start(int receiverPort) throws DataBridgeException {
        DataPublisherTestUtil.setKeyStoreParams();
        streamDefinitionStore = getStreamDefinitionStore();
        numberOfEventsReceived = new AtomicInteger(0);
        databridge = new DataBridge(new AuthenticationHandler() {
            @Override
            public boolean authenticate(String userName,
                                        String password) {
                log.info("Thrift authentication returning true");
                return true;// allays authenticate to true

            }

            @Override
            public String getTenantDomain(String userName) {
                return "admin";
            }

            @Override
            public int getTenantId(String tenantDomain) throws UserStoreException {
                return -1234;
            }

            @Override
            public void initContext(AgentSession agentSession) {
                //To change body of implemented methods use File | Settings | File Templates.
                log.info("Initializing Thrift agent context");
            }

            @Override
            public void destroyContext(AgentSession agentSession) {

            }
        }, streamDefinitionStore, DataPublisherTestUtil.getDataBridgeConfigPath());

        thriftDataReceiver = new ThriftDataReceiver(receiverPort, databridge);

        databridge.subscribe(new AgentCallback() {
            @Override
            public void definedStream(StreamDefinition streamDefinition, int tenantId) {
                log.info("StreamDefinition defined:" + streamDefinition);
            }

            @Override
            public void removeStream(StreamDefinition streamDefinition, int tenantId) {
                log.info("StreamDefinition removed: " + streamDefinition);
            }

            @Override
            public void receive(List<Event> eventList, Credentials credentials) {
                numberOfEventsReceived.addAndGet(eventList.size());
                log.info("Number of received events: " + numberOfEventsReceived);
            }

        });

        String address = "localhost";
        log.info("Test Server starting on " + address);
        thriftDataReceiver.start(address);
        log.info("Test Server Started");
    }

    public int getNumberOfEventsReceived() {
        if (numberOfEventsReceived != null) {
            return numberOfEventsReceived.get();
        } else {
            return 0;
        }
    }

    public void resetReceivedEvents() {
        numberOfEventsReceived.set(0);
    }

    public void stop() {
        thriftDataReceiver.stop();
        log.info("Test Server Stopped");
    }

    public void stopAndStartDuration(int port, long stopAfterTimeMilliSeconds, long startAfterTimeMS)
            throws SocketException, DataBridgeException {
        restarterThread = new RestarterThread(port, stopAfterTimeMilliSeconds, startAfterTimeMS);
        Thread thread = new Thread(restarterThread);
        thread.start();
    }

    public int getEventsReceivedBeforeLastRestart() {
        return restarterThread.eventReceived;
    }


    class RestarterThread implements Runnable {
        int eventReceived;
        int port;

        long stopAfterTimeMilliSeconds;
        long startAfterTimeMS;

        RestarterThread(int port, long stopAfterTime, long startAfterTime) {
            this.port = port;
            stopAfterTimeMilliSeconds = stopAfterTime;
            startAfterTimeMS = startAfterTime;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(stopAfterTimeMilliSeconds);
            }
            catch (InterruptedException e) {
            }
            if (thriftDataReceiver != null) {
                thriftDataReceiver.stop();
            }

            eventReceived = getNumberOfEventsReceived();

            log.info("Number of events received in server shutdown :" + eventReceived);
            try {
                Thread.sleep(startAfterTimeMS);
            }
            catch (InterruptedException e) {
            }

            try {
                if (thriftDataReceiver != null) {
                    thriftDataReceiver.start(DataPublisherTestUtil.LOCAL_HOST);
                } else {
                    start(port);
                }
            }
            catch (DataBridgeException e) {
                log.error(e);
            }

        }
    }

    public ThriftDataReceiver getThriftDataReceiver() {
        return thriftDataReceiver;
    }

    public RestarterThread getRestarterThread() {
        return restarterThread;
    }

    public DataBridge getDatabridge() {
        return databridge;
    }
}
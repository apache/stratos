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

package org.apache.stratos.status.monitor.agent.clients;

import org.apache.stratos.status.monitor.agent.clients.service.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

/**
 * The class that initializes the client threads
 */
public class ClientThreadsInitializer {
    private static final Log log = LogFactory.getLog(ClientThreadsInitializer.class);

    public static void initializeThreads() throws IOException, SQLException, NamingException,
            JMSException, ParseException, XMLStreamException {

        new Thread() {
            public void run() {
                try {
                    ApplicationServerClient applicationServerClient = new ApplicationServerClient();
                    applicationServerClient.start();
                } catch (Exception e) {
                    String msg = "Error in starting the status monitoring thread for " +
                            "Application Server";
                    log.warn(msg);
                }
                try {
                    DataServerClient dataServerClient = new DataServerClient();
                    dataServerClient.start();
                } catch (Exception e) {
                    String msg = "Error in starting the status monitoring thread for " +
                            "Data Services Server";
                    log.warn(msg);
                }
                try {
                    ESBServerClient esbServerClient = new ESBServerClient();
                    esbServerClient.start();
                } catch (Exception e) {
                    String msg = "Error in starting the status monitoring thread for ESB";
                    log.warn(msg);
                }
                try {
                    GovernanceRegistryServerClient governanceRegistryServerClient = new GovernanceRegistryServerClient();
                    governanceRegistryServerClient.start();
                } catch (Exception e) {
                    String msg = "Error in starting the status monitoring thread for " +
                            "Governance Registry";
                    log.warn(msg);
                }
                try {
                    BPSServerClient bpsServerClient = new BPSServerClient();
                    bpsServerClient.start();
                } catch (Exception e) {
                    String msg = "Error in starting the status monitoring thread for " +
                            "Business Process Server";
                    log.warn(msg);
                }
                try {
                    MashupServerClient mashupServerClient = new MashupServerClient();
                    mashupServerClient.start();
                } catch (Exception e) {
                    String msg = "Error in starting the status monitoring thread for " +
                            "Mashup Server";
                    log.warn(msg);
                }
                try {
                    BRSServerClient brsServerClient = new BRSServerClient();
                    brsServerClient.start();
                } catch (Exception e) {
                    String msg = "Error in starting the status monitoring thread for " +
                            "Business Rules Server";
                    log.warn(msg);
                }
                try {
                    GadgetServerClient gadgetServerClient = new GadgetServerClient();
                    gadgetServerClient.start();
                } catch (Exception e) {
                    String msg = "Error in starting the status monitoring thread for " +
                            "Gadgets Server";
                    log.warn(msg);
                }
                try {
                    IdentityServerClient serverClient = new IdentityServerClient();
                    serverClient.start();
                } catch (Exception e) {
                    String msg = "Error in starting the status monitoring thread for " +
                            "Identity Server";
                    log.warn(msg);
                }
                try {
                    BAMServerClient bamServerClient = new BAMServerClient();
                    bamServerClient.start();
                } catch (Exception e) {
                    String msg = "Error in starting the status monitoring thread for " +
                            "Business Activity Monitor";
                    log.warn(msg);
                }
                try {
                    ManagerServiceClient managerServiceClient = new ManagerServiceClient();
                    managerServiceClient.start();
                } catch (Exception e) {
                    String msg = "Error in starting the status monitoring thread for " +
                            "Manager";
                    log.warn(msg);
                }
                try {
                    MessageBrokerServiceClient messageBrokerServiceClient = new MessageBrokerServiceClient();
                    messageBrokerServiceClient.start();
                } catch (Exception e) {
                    String msg = "Error in starting the status monitoring thread for " +
                            "Message Broker";
                    log.warn(msg);
                }
                try {
                    CEPServerClient cepServerClient = new CEPServerClient();
                    cepServerClient.start();
                } catch (Exception e) {
                    String msg = "Error in starting the status monitoring thread for " +
                            "CEP Server";
                    log.warn(msg);
                }
            }
        }.start();
    }
}

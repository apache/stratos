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

package org.apache.stratos.cartridge.agent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.util.MessagingUtil;

import java.lang.reflect.Constructor;

/**
 * Cartridge agent main class.
 */
public class Main {

    private static final Log log = LogFactory.getLog(Main.class);
    private static CartridgeAgent cartridgeAgent = null;

    public static void main(String[] args) {
        try {
            // Add shutdown hook
            final Thread mainThread = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        // Close event publisher connections to message broker
                        EventPublisherPool.close(MessagingUtil.Topics.INSTANCE_STATUS_TOPIC.getTopicName());
                        mainThread.join();
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            });

            // Configure log4j properties
            if(log.isDebugEnabled()) {
                log.debug("Configuring log4j.properties file path");
            }
            PropertyConfigurator.configure(System.getProperty("log4j.configuration"));

            // Initialize cartridge agent configuration
            CartridgeAgentConfiguration.getInstance();

            if (args.length >= 1) {
                String className = args[0];
                try {
                    Constructor<?> c = Class.forName(className)
                            .getConstructor();
                    cartridgeAgent = (CartridgeAgent) c.newInstance();
                    log.info("Loaded Cartridge Agent using [class] " + className);
                } catch (Exception e) {
                    String msg = String.format("Cannot load Cartridge Agent from [class name] %s, "
                            + "hence using the default agent.", className);
                    log.warn(msg, e);
                }
            }

            if (cartridgeAgent == null) {
                // load default agent
                cartridgeAgent = new CartridgeAgent();
                if (log.isDebugEnabled()) {
                    log.debug("Loading default Cartridge Agent.");
                }
            }
            // start agent
            Thread thread = new Thread(cartridgeAgent);
            thread.start();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e);
            }
            if (cartridgeAgent != null) {
                cartridgeAgent.terminate();
            }
        }

    }

}

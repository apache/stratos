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

package org.apache.stratos.cartridge.agent.event.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Event publisher main class.
 */
public class Main {
    private static final Log log = LogFactory.getLog(Main.class);

    public static void main(String[] args) {
        try {
            if (log.isInfoEnabled()) {
                log.info("Event publisher started");
            }
            if ((args != null) && (args.length == 4)) {
                EventPublisherClient client = new EventPublisherClient(args[0], Integer.parseInt(args[1]), args[2], args[3]);
                client.run();
                System.exit(0);
            } else {
                printInvalidArgs(args);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not publish event", e);
            }
            printInvalidArgs(args);
        }
        System.exit(-1);
    }

    private static void printInvalidArgs(String[] args) {
        if (log.isErrorEnabled()) {
            if(args != null) {
                StringBuilder builder = new StringBuilder();
                for(String arg : args) {
                    builder.append("[" + arg + "] ");
                }
                log.error(String.format("Arguments %s not valid. Event publisher could not be started.", builder.toString()));
            }
            else {
                log.error("Arguments not found. Event publisher could not be started.");
            }
            log.error("Expected: mb-ip-address mb-port event-class-name event-json-file-path");
        }
    }
}

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

package org.apache.stratos.cartridge.agent.event.subscriber;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.subscribe.TopicSubscriber;
import org.apache.stratos.messaging.util.Constants;

/**
 * Event publisher main class.
 */
public class Main {
    private static final Log log = LogFactory.getLog(Main.class);

    public static void main(String[] args) {
    	
    	
    	log.info(" ************** in main **************** ");
    	log.info(" arg0 : " + args[0]);
    	
    	System.setProperty("jndi.properties.dir", args[0]);
    	
        //initialting the subscriber
        TopicSubscriber subscriber = new TopicSubscriber(Constants.ARTIFACT_SYNCHRONIZATION_TOPIC);
        subscriber.setMessageListener(new ArtifactListener());
        Thread tsubscriber = new Thread(subscriber);
		tsubscriber.start();
		
     /* try {
            if (log.isInfoEnabled()) {
                log.info("Event subscriber started");
            }
            if ((args != null) && (args.length == 4)) {
            	
                EventSubscriberClient client = new EventSubscriberClient(args[0], Integer.parseInt(args[1]), args[2], args[3]);
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
        System.exit(-1);*/
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

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
package org.apache.stratos.messaging.broker.heartbeat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.connect.TopicConnector;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.event.ping.PingEvent;
import org.apache.stratos.messaging.util.Constants;

import javax.jms.JMSException;

/**
 * This health checker runs forever, and is responsible for checking the
 * connection
 * between this server and a topic provider.
 * This thread dies when the subjected connection lost.
 */
public class TopicHealthChecker implements Runnable {

	private static final Log log = LogFactory.getLog(TopicHealthChecker.class);
	private String topicName;
    private boolean terminated;

    public TopicHealthChecker(String name) {
		topicName = name;
	}

	@Override
	public void run() {
        if(log.isDebugEnabled()){
		    log.debug(topicName + " topic health checker is running... " );
        }
		TopicConnector testConnector = new TopicConnector();
		while (!terminated) {
			try {
				// Health checker needs to run with the smallest possible time interval
				// to detect a connection drop. Otherwise the subscriber will not
                // get reconnected after a connection drop.
				Thread.sleep(1000);
				testConnector.init(topicName);
                // A ping event is published to detect a session timeout
                EventPublisherPool.getPublisher(Constants.PING_TOPIC).publish(new PingEvent());
			} catch (Exception e) {
				// Implies connection is not established
				// sleep for 30 sec and retry
				try {
					log.error(topicName + " topic health checker is failed and will try to subscribe again in 30 sec");
					Thread.sleep(30000);
					break;
				} catch (InterruptedException ignore) {
				}
			} finally {
				try {
					testConnector.close();
				} catch (JMSException ignore) {
				}
			}

		}

	}

    public void terminate() {
        terminated = true;
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.broker.publish;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.connect.TopicConnector;
import org.apache.stratos.messaging.broker.connect.TopicConnectorFactory;
import org.apache.stratos.messaging.domain.exception.MessagingException;

/**
 * A topic publisher for publishing messages to a message broker topic.
 * Messages will be published in JSON format.
 */
public class TopicPublisher {

    private static final Log log = LogFactory.getLog(TopicPublisher.class);

    private static final int PUBLISH_RETRY_INTERVAL = 60000;

	private final String topicName;
	private final TopicConnector topicConnector;

    /**
	 * @param topicName topic name of this publisher instance.
	 */
	TopicPublisher(String topicName) {
		this.topicName = topicName;
        this.topicConnector = TopicConnectorFactory.createConnector();
		if (log.isDebugEnabled()) {
			log.debug(String.format("Topic publisher created: [topic] %s", topicName));
		}
	}

	/**
	 * Convert the object to its JSON representation and publish to the given topic.
	 */

	public void publish(Object messageObj, boolean retry) {
		synchronized (TopicPublisher.class) {
            Gson gson = new Gson();
            String message = gson.toJson(messageObj);
            boolean published = false;

            while (!published) {
                try {
                    topicConnector.connect();
                    topicConnector.publish(topicName, message);
                    published = true;
                } catch (Exception e) {
                    if (!retry) {
                        if (log.isDebugEnabled()) {
                            log.debug("Retry disabled for topic " + topicName);
                        }
                        throw new MessagingException(e);
                    }

                    if (log.isInfoEnabled()) {
                        log.info(String.format("Will try to re-publish in %d sec", (PUBLISH_RETRY_INTERVAL/1000)));
                    }
                    try {
                        Thread.sleep(PUBLISH_RETRY_INTERVAL);
                    } catch (InterruptedException ignore) {
                    }
                } finally {
                    try {
                        topicConnector.disconnect();
                    } catch (MessagingException ignore) {
                    }
                }
            }
        }
	}
}

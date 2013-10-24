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
package org.apache.stratos.autoscaler.message.receiver.health;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.event.processor.AverageRequestInFlightEventProcessor;
import org.apache.stratos.autoscaler.event.processor.GradientOfRequestInFlightEventProcessor;
import org.apache.stratos.autoscaler.event.processor.SecondDerivativeOfRequestInFlightEventProcessor;
import org.apache.stratos.autoscaler.message.receiver.TopologyManager;
import org.apache.stratos.messaging.util.Constants;

import javax.jms.TextMessage;


/**
 * A thread for processing topology messages and updating the topology data structure.
 */
public class HealthEventMessageDelegator implements Runnable {

    private static final Log log = LogFactory.getLog(HealthEventMessageDelegator.class);

    @Override
    public void run() {
		log.info("Health stat event message processor started");

        // instantiate all the relevant processors
        AverageRequestInFlightEventProcessor processor1 = new AverageRequestInFlightEventProcessor();
        GradientOfRequestInFlightEventProcessor processor2 = new GradientOfRequestInFlightEventProcessor();
        SecondDerivativeOfRequestInFlightEventProcessor processor3 = new SecondDerivativeOfRequestInFlightEventProcessor();

        // link all the relevant processors in the required order
        processor1.setNext(processor2);
        processor2.setNext(processor3);

        while (true) {
			try {
				TextMessage message = HealthEventQueue.getInstance().take();

				// retrieve the header
				String type = message.getStringProperty(Constants.EVENT_CLASS_NAME);
				// retrieve the actual message
				String json = message.getText();

				if (log.isDebugEnabled()) {
					log.debug(String.format("Event message received from queue: %s", type));
				}

				try {
					TopologyManager.acquireWriteLock();
					processor1.process(type, json);
				} finally {
					TopologyManager.releaseWriteLock();
				}

			} catch (Exception e) {
                String error = "Failed to retrieve the topology event message.";
            	log.error(error, e);
            	throw new RuntimeException(error, e);
            }
        }
    }
}

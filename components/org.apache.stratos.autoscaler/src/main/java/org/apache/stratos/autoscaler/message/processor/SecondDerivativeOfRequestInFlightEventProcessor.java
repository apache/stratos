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
package org.apache.stratos.autoscaler.message.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.message.receiver.TopicSubscriberManager;
import org.apache.stratos.autoscaler.event.AverageRequestsInFlightEvent;
import org.apache.stratos.autoscaler.event.SecondDerivativeOfRequestsInFlightEvent;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.util.Util;

import java.util.Map;

public class SecondDerivativeOfRequestInFlightEventProcessor implements HealthStatEventProcessor {

	private static final Log log = LogFactory.getLog(SecondDerivativeOfRequestInFlightEventProcessor.class);
	private HealthStatEventProcessor nextMsgProcessor;

	@Override
	public void setNext(HealthStatEventProcessor nextProcessor) {
		nextMsgProcessor = nextProcessor;
	}

	@Override
	public boolean process(String type, String message) {
		try {
			if (AverageRequestsInFlightEvent.class.getName().equals(type)) {

				// Parse complete message and build event
                SecondDerivativeOfRequestsInFlightEvent event
                        = (SecondDerivativeOfRequestsInFlightEvent) Util.jsonToObject(message,
                        SecondDerivativeOfRequestsInFlightEvent.class);

                String clusterId = event.getClusterId();
                    //Get all values of services Map from topology
                for (Service service : ((Map<String, Service>) TopicSubscriberManager.getTopology().getServices()).values())
                {
                    if(service.clusterExists(clusterId)){

                        ((Map<String, Service>) TopicSubscriberManager.getTopology().getServices())
                                .get(service.getServiceName()).getCluster(clusterId)
                                .setRequestsInFlightSecondDerivative(event.getValue());
                    }
                }

				return true;

			} else {
				if (nextMsgProcessor != null) {
					// ask the next processor to take care of the message.
					return nextMsgProcessor.process(type, message);
				}
			}
		} catch (Exception e) {
			if (nextMsgProcessor != null) {
				// ask the next processor to take care of the message.
				return nextMsgProcessor.process(type, message);
			} else {
				throw new RuntimeException(String.format("Failed to process the message: %s of type %s using any of the available processors.",
				                                         message, type));
			}
		}
		return false;
	}
}

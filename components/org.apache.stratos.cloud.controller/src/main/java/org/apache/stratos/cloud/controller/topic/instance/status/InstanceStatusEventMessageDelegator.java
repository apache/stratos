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
package org.apache.stratos.cloud.controller.topic.instance.status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.topology.TopologyBuilder;
import org.apache.stratos.messaging.event.instance.status.InstanceActivatedEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceMaintenanceModeEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceReadyToShutdownEvent;
import org.apache.stratos.messaging.event.instance.status.InstanceStartedEvent;
import org.apache.stratos.messaging.util.Constants;
import org.apache.stratos.messaging.util.Util;

import javax.jms.TextMessage;

public class InstanceStatusEventMessageDelegator implements Runnable {
    private static final Log log = LogFactory.getLog(InstanceStatusEventMessageDelegator.class);

    @Override
    public void run() {
        log.info("Instance status event message delegator started");

        while (true) {
            try {
                TextMessage message = InstanceStatusEventMessageQueue.getInstance().take();

                // retrieve the header
                String type = message.getStringProperty(Constants.EVENT_CLASS_NAME);
                log.info(String.format("Instance status event message received from queue: %s", type));

                if (InstanceStartedEvent.class.getName().equals(type)) {
                    // retrieve the actual message
                    String json = message.getText();
                    TopologyBuilder.handleMemberStarted((InstanceStartedEvent) Util.
                            jsonToObject(json, InstanceStartedEvent.class));
                } else if (InstanceActivatedEvent.class.getName().equals(type)) {
                    // retrieve the actual message
                    String json = message.getText();
                    TopologyBuilder.handleMemberActivated((InstanceActivatedEvent) Util.
                            jsonToObject(json, InstanceActivatedEvent.class));
                } else if (InstanceReadyToShutdownEvent.class.getName().equals(type)) {
                    //retrieve the actual message
                    String json = message.getText();
                    TopologyBuilder.handleMemberReadyToShutdown((InstanceReadyToShutdownEvent) Util.
                            jsonToObject(json, InstanceReadyToShutdownEvent.class));
                } else if (InstanceMaintenanceModeEvent.class.getName().equals(type)) {
                    //retrieve the actual message
                    String json = message.getText();
                    TopologyBuilder.handleMemberMaintenance((InstanceMaintenanceModeEvent) Util.
                            jsonToObject(json, InstanceMaintenanceModeEvent.class));
                } else {
                    log.warn("Event message received is not InstanceStartedEvent or InstanceActivatedEvent");
                }
            } catch (Exception e) {
                String error = "Failed to retrieve the instance status event message";
                log.error(error, e);
                // Commenting throwing the error. Otherwise thread will not execute if an exception is thrown.
                //throw new RuntimeException(error, e);
            }
        }
    }
}

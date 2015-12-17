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

package org.apache.stratos.messaging.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.message.receiver.application.ApplicationsEventReceiver;
import org.apache.stratos.messaging.message.receiver.application.signup.ApplicationSignUpEventReceiver;
import org.apache.stratos.messaging.message.receiver.cluster.status.ClusterStatusEventReceiver;
import org.apache.stratos.messaging.message.receiver.domain.mapping.DomainMappingEventReceiver;
import org.apache.stratos.messaging.message.receiver.health.stat.HealthStatEventReceiver;
import org.apache.stratos.messaging.message.receiver.initializer.InitializerEventReceiver;
import org.apache.stratos.messaging.message.receiver.tenant.TenantEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.osgi.service.component.ComponentContext;

/**
 * @scr.component name="org.apache.stratos.messaging.internal.MessagingServiceComponent"
 * immediate="true"
 */
public class MessagingServiceComponent {

    private static final Log log = LogFactory.getLog(MessagingServiceComponent.class);

    protected void activate(ComponentContext context) {
        try {
            log.info("Messaging Service bundle activated");
        } catch (Exception e) {
            log.error("Could not activate Messaging Service component", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        // deactivate all message receivers
        try {
            ApplicationSignUpEventReceiver.getInstance().terminate();
            ApplicationsEventReceiver.getInstance().terminate();
            ClusterStatusEventReceiver.getInstance().terminate();
            DomainMappingEventReceiver.getInstance().terminate();
            HealthStatEventReceiver.getInstance().terminate();
            InitializerEventReceiver.getInstance().terminate();
            TenantEventReceiver.getInstance().terminate();
            TopologyEventReceiver.getInstance().terminate();
            log.info("Messaging Service component is deactivated");
        } catch (Exception e) {
            log.error("Could not de-activate Messaging Service component", e);
        }
    }
}

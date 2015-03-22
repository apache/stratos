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
package org.apache.stratos.cloud.controller.messaging.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.config.CloudControllerConfig;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyManager;

/**
 * Topology event synchronizer publishes complete topology event periodically.
 */
public class TopologyEventSynchronizer implements Runnable {

    private static final Log log = LogFactory.getLog(TopologyEventSynchronizer.class);

    @Override
    public void run() {
        if (log.isDebugEnabled()) {
            log.debug("Executing topology synchronizer");
        }
        
        if(!CloudControllerConfig.getInstance().isTopologySyncEnabled()) {
            if(log.isWarnEnabled()) {
                log.warn("Topology synchronization is disabled");
            }
            return;
        }

        if(CloudControllerContext.getInstance().isTopologySyncRunning()) {
            if(log.isWarnEnabled()) {
                log.warn("Topology synchronization is already running");
            }
            return;
        }

        try {
            // Publish complete topology event
            if (TopologyManager.getTopology() != null) {
                CloudControllerContext.getInstance().setTopologySyncRunning(true);
                TopologyEventPublisher.sendCompleteTopologyEvent(TopologyManager.getTopology());
            }
        } finally {
            CloudControllerContext.getInstance().setTopologySyncRunning(false);
        }
    }
}

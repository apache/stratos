/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.message.processor.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.Applications;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.application.Group;
import org.apache.stratos.messaging.event.application.ApplicationCreatedEvent;
import org.apache.stratos.messaging.event.application.ApplicationUpdatedEvent;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.processor.application.updater.ApplicationsUpdater;
import org.apache.stratos.messaging.util.MessagingUtil;

import java.util.Set;

public class ApplicationUpdatedMessageProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(ApplicationUpdatedMessageProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {

        Applications applications = (Applications) object;

        if (ApplicationUpdatedEvent.class.getName().equals(type)) {
            if (!applications.isInitialized()) {
                return false;
            }

            ApplicationUpdatedEvent event = (ApplicationUpdatedEvent) MessagingUtil.
                    jsonToObject(message, ApplicationUpdatedEvent.class);
            if (event == null) {
                log.error("Unable to convert the JSON message to ApplicationCreatedEvent");
                return false;
            }

            ApplicationsUpdater.acquireWriteLockForApplications();
            try {
                return doProcess(event, applications);

            } finally {
                ApplicationsUpdater.releaseWriteLockForApplications();
            }

        } else {
            if (nextProcessor != null) {
                // ask the next processor to take care of the message.
                return nextProcessor.process(type, message, applications);
            } else {
                throw new RuntimeException(String.format("Failed to process message using available message processors: [type] %s [body] %s", type, message));
            }
        }
    }

    private boolean doProcess(ApplicationUpdatedEvent event, Applications applications) {

        // check if required properties are available
        if (event.getApplication() == null) {
            String errorMsg = "Application object of application updated event is invalid";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        if (event.getApplication().getUniqueIdentifier() == null ||
                event.getApplication().getUniqueIdentifier().isEmpty()) {
            String errorMsg = "App id of application updated event is invalid: [ " + event.getApplication().getUniqueIdentifier() + " ]";
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        // check if an Application with same name exists in applications
        if (applications.applicationExists(event.getApplication().getUniqueIdentifier())) {
            Application updatedApplication = event.getApplication();
            // add application update to applications Topology
            Application application = applications.
                    getApplication(event.getApplication().getUniqueIdentifier());
            //Update Application Recursively
            Set<Group> groups = application.getAllGroupsRecursively();
            for (Group group : groups) {
                Group updatedGroup = updatedApplication.getGroupRecursively(group.getUniqueIdentifier());

                if (updatedGroup != null) {
                    group.setGroupMaxInstances(updatedGroup.getGroupMaxInstances());
                    group.setGroupMinInstances(updatedGroup.getGroupMinInstances());
                } else {
                    log.warn("[Goup] " + group.getUniqueIdentifier() +
                            " cannot be found in [application] " + application.getUniqueIdentifier());
                }

            }

            Set<ClusterDataHolder> clusterDataHolders = application.getClusterDataRecursively();
            for (ClusterDataHolder dataHolder : clusterDataHolders) {
                Set<ClusterDataHolder> updatedClusters = updatedApplication.
                        getClusterDataRecursively();
                boolean clusterFound = false;
                for(ClusterDataHolder updatedCluster : updatedClusters) {
                    if (updatedCluster.getClusterId().equals(dataHolder.getClusterId())) {
                        dataHolder.setMinInstances(updatedCluster.getMinInstances());
                        dataHolder.setMaxInstances(updatedCluster.getMaxInstances());
                        clusterFound = true;
                        break;
                    }
                }
                if(!clusterFound) {
                    log.warn("[Cluster] " + dataHolder.getClusterId() +
                            " cannot be found in [application] " + application.getUniqueIdentifier());
                }

            }


            if (log.isInfoEnabled()) {
                log.info("Application with id [ " + event.getApplication().getUniqueIdentifier() + " ] updated");
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Application with id [ " + event.getApplication().getUniqueIdentifier() + " ] not exists");
            }

        }

        notifyEventListeners(event);
        return true;
    }
}

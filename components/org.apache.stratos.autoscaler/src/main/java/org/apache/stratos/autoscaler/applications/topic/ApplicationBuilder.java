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
package org.apache.stratos.autoscaler.applications.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.pojo.ApplicationClusterContext;
import org.apache.stratos.autoscaler.client.CloudControllerClient;
import org.apache.stratos.autoscaler.monitor.application.ApplicationMonitor;
import org.apache.stratos.autoscaler.monitor.group.GroupMonitor;
import org.apache.stratos.messaging.domain.applications.*;

import java.util.Collection;
import java.util.Set;

/**
 * This will build the application.
 */
public class ApplicationBuilder {
    private static final Log log = LogFactory.getLog(ApplicationBuilder.class);

    public static synchronized void handleCompleteApplication(Applications applications) {
        if (log.isDebugEnabled()) {
            log.debug("Handling complete application event");
        }

        ApplicationHolder.acquireReadLock();
        try {
            ApplicationsEventPublisher.sendCompleteApplicationsEvent(applications);
        } finally {
            ApplicationHolder.releaseReadLock();
        }
    }

    public static synchronized void handleApplicationCreated(Application application,
                                                             Set<ApplicationClusterContext> appClusterContexts) {
        if (log.isInfoEnabled()) {
            log.info("Handling application creation event: [application-id] " +
                    application.getUniqueIdentifier());
        }

        ApplicationHolder.acquireWriteLock();
        try {
            Applications applications = ApplicationHolder.getApplications();
            if (applications.getApplication(application.getUniqueIdentifier()) == null) {
                CloudControllerClient.getInstance().createApplicationClusters(application.getUniqueIdentifier(),
                        appClusterContexts);
                ApplicationHolder.persistApplication(application);
            } else {
                log.warn("Application already exists: [application-id] " + application.getUniqueIdentifier());
            }
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
        ApplicationsEventPublisher.sendApplicationCreatedEvent(application);
    }

    public static void handleApplicationActivatedEvent(String appId) {
        if (log.isInfoEnabled()) {
            log.info("Handling application activation event: [application-id] " + appId);
        }

        Applications applications = ApplicationHolder.getApplications();
        Application application = applications.getApplication(appId);
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application does not exist: [application-id] %s",
                    appId));
            return;
        }

        /*try {
            ApplicationHolder.acquireWriteLock();*/
        ApplicationStatus status = ApplicationStatus.Active;
        if (application.isStateTransitionValid(status)) {
            log.info(String.format("Updating application status: [application-id] %s [status] %s", appId, status));
            application.setStatus(status);
            updateApplicationMonitor(appId, status);
            ApplicationHolder.persistApplication(application);
            //publishing data
            //TODO ApplicationsEventPublisher.sendApplicationActivatedEvent(appId);
        } else {
            log.warn(String.format("Application state transition is not valid: [application-id] %s " +
                    " [current-status] %s [status-requested] %s", appId, application.getStatus(), status));
        }
        /*} finally {
            ApplicationHolder.releaseWriteLock();
        }*/

    }

    public static void handleApplicationUndeployed(String appId) {
        if (log.isInfoEnabled()) {
            log.info("Handling application terminating event: [application-id] " + appId);
        }

        Applications applications = ApplicationHolder.getApplications();
        Application application = applications.getApplication(appId);
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application does not exist: [application-id] %s",
                    appId));
            return;
        }

        try {
            ApplicationHolder.acquireWriteLock();
            ApplicationStatus status = ApplicationStatus.Terminating;
            if (application.isStateTransitionValid(status)) {
                log.info(String.format("Updating application status: " +
                        "[application-id] %s [status] %s", appId, status));
                application.setStatus(status);
                updateApplicationMonitor(appId, status);
                ApplicationHolder.persistApplication(application);
                ApplicationsEventPublisher.sendApplicationTerminatingEvent(appId);
            } else {
                log.warn(String.format("Application state transition is not valid: [application-id] %s " +
                        " [current-status] %s [status-requested] %s", appId, application.getStatus(), status));
            }
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
        ApplicationsEventPublisher.sendApplicationTerminatingEvent(appId);
    }

    public static void handleApplicationTerminatedEvent(String appId) {
        if (log.isInfoEnabled()) {
            log.info("Handling application terminated event: [application-id] " + appId);
        }

        Applications applications = ApplicationHolder.getApplications();
        /*try {
            ApplicationHolder.acquireWriteLock();*/

        if (!applications.applicationExists(appId)) {
            log.warn("Application does not exist: [application-id] " + appId);
        } else {
            Application application = applications.getApplication(appId);
            Set<ClusterDataHolder> clusterData = application.getClusterDataRecursively();

            ApplicationStatus status = ApplicationStatus.Terminated;
            if (application.isStateTransitionValid(status)) {
                // forcefully set status for now
                log.info(String.format("Updating application status: [application-id] %s [status] %s",
                        appId, status));
                application.setStatus(status);
                updateApplicationMonitor(appId, status);
                //removing the monitor
                AutoscalerContext.getInstance().removeAppMonitor(appId);
                //Removing the application from memory and registry
                ApplicationHolder.removeApplication(appId);
                log.info("Application is removed: [application-id] " + appId);

                ApplicationsEventPublisher.sendApplicationTerminatedEvent(appId, clusterData);
            } else {
                log.warn(String.format("Application state transition is not valid: [application-id] %s " +
                        " [current-status] %s [status-requested] %s", appId, application.getStatus(), status));
            }
        }

        /*} finally {
            ApplicationHolder.releaseWriteLock();
        }*/
    }

    public static void handleApplicationTerminatingEvent(String applicationId) {
        if (log.isInfoEnabled()) {
            log.info("Handling application terminating event: [application-id] " + applicationId);
        }

        ApplicationHolder.acquireWriteLock();

        try {
            Applications applications = ApplicationHolder.getApplications();
            if (!applications.applicationExists(applicationId)) {
                log.warn("Application does not exist: [application-id] " + applicationId);
                return;
            }

            Application application = applications.getApplication(applicationId);
            // check and update application status to 'Terminating'
            ApplicationStatus status = ApplicationStatus.Terminating;
            if (application.isStateTransitionValid(status)) {
                // for now anyway update the status forcefully
                log.info(String.format("Updating application status: [application-id] %s [status] %s",
                        applicationId, status));
                application.setStatus(status);
                updateApplicationMonitor(applicationId, status);
                ApplicationsEventPublisher.sendApplicationTerminatingEvent(applicationId);
            } else {
                log.warn(String.format("Application state transition is not valid: [application-id] %s " +
                        " [current-status] %s [status-requested] %s", applicationId, application.getStatus(), status));
            }
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
    }

    public static void handleGroupTerminatedEvent(String appId, String groupId) {
        if (log.isInfoEnabled()) {
            log.info("Handling group terminated event: [group-id] " + groupId +
                    " [application-id] " + appId);
        }

        Applications applications = ApplicationHolder.getApplications();
        Application application = applications.getApplication(appId);
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application does not exist: [application-id] %s",
                    appId));
            return;
        }

        Group group = application.getGroupRecursively(groupId);
        if (group == null) {
            log.warn(String.format("Group does not exist: [group-id] %s",
                    groupId));
            return;
        }

        /*try {
            ApplicationHolder.acquireWriteLock();*/
        GroupStatus status = GroupStatus.Terminated;
        if (group.isStateTransitionValid(status)) {
            log.info(String.format("Updating group status: [group-id] %s [status] %s", groupId, status));
            group.setStatus(status);
            //updating the groupMonitor
            updateGroupMonitor(appId, groupId, status);
            //publishing data
            //TODO ApplicationsEventPublisher.sendGroupTerminatedEvent(appId, groupId);
        } else {
            log.warn(String.format("Group state transition is not valid: [group-id] %s [current-status] %s " +
                    " [requested-status] %s", groupId, group.getStatus(), status));
        }
        ApplicationHolder.persistApplication(application);
        /*} finally {
            ApplicationHolder.releaseWriteLock();
        }*/
    }

    public static void handleGroupActivatedEvent(String appId, String groupId) {
        if (log.isInfoEnabled()) {
            log.info("Handling group activation for the [group-id]: " + groupId +
                    " in the [application-id] " + appId);
        }

        Applications applications = ApplicationHolder.getApplications();
        Application application = applications.getApplication(appId);
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application does not exist: [application-id] %s",
                    appId));
            return;
        }

        Group group = application.getGroupRecursively(groupId);
        if (group == null) {
            log.warn(String.format("Group does not exist: [group-id] %s",
                    groupId));
            return;
        }

        /*try {
            ApplicationHolder.acquireWriteLock();*/
        GroupStatus status = GroupStatus.Active;
        if (group.isStateTransitionValid(status)) {
            log.info(String.format("Updating group status: [group-id] %s [status] %s", groupId, status));
            group.setStatus(status);
            //updating the groupMonitor
            updateGroupMonitor(appId, groupId, status);
            //publishing data
            //TODO ApplicationsEventPublisher.sendGroupActivatedEvent(appId, groupId);
        } else {
            log.warn(String.format("Group state transition is not valid: [group-id] %s [current-status] %s " +
                    " [requested-status] %s", groupId, group.getStatus(), status));
        }
        ApplicationHolder.persistApplication(application);
        /*} finally {
            ApplicationHolder.releaseWriteLock();
        }*/
    }

    public static void handleGroupCreatedEvent(String appId, String groupId) {
        if (log.isInfoEnabled()) {
            log.info("Handling Group creation for the [group]: " + groupId +
                    " in the [application] " + appId);
        }

        Applications applications = ApplicationHolder.getApplications();
        Application application = applications.getApplication(appId);
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application %s does not exist",
                    appId));
            return;
        }

        Group group = application.getGroupRecursively(groupId);
        if (group == null) {
            log.warn(String.format("Group %s does not exist",
                    groupId));
            return;
        }

        /*try {
            ApplicationHolder.acquireWriteLock();*/
        GroupStatus status = GroupStatus.Created;
        if (group.isStateTransitionValid(status)) {
            log.info("Updating group status: [group-id] " + group.getUniqueIdentifier() + " [status] " + status);
            group.setStatus(status);
            //updating the groupMonitor
            updateGroupMonitor(appId, groupId, status);
            //publishing data
            //TODO ApplicationsEventPublisher.sendGroupCreatedEvent(appId, groupId);
        } else {
            log.warn("Group state transition is not valid: [group-id] " + groupId + " [current-state] " + group.getStatus()
                    + "[requested-state] " + status);
        }
        ApplicationHolder.persistApplication(application);
        /*} finally {
            ApplicationHolder.releaseWriteLock();
        }*/

    }

    public static void handleGroupInActivateEvent(String appId, String groupId) {
        if (log.isInfoEnabled()) {
            log.info("Handling group in-active event: [group]: " + groupId +
                    " [application-id] " + appId);
        }

        Applications applications = ApplicationHolder.getApplications();
        Application application = applications.getApplication(appId);
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application does not exist: [application-id] %s",
                    appId));
            return;
        }

        Group group = application.getGroupRecursively(groupId);
        if (group == null) {
            log.warn(String.format("Group does not exist: [group-id] %s",
                    groupId));
            return;
        }

        /*try {
            ApplicationHolder.acquireWriteLock();*/
        GroupStatus status = GroupStatus.Inactive;
        if (group.isStateTransitionValid(status)) {
            log.info("Updating group state: [group-id] " + groupId + " [status] " + status);
            group.setStatus(status);
            //updating the groupMonitor
            updateGroupMonitor(appId, groupId, status);
            //publishing data
            //TODO ApplicationsEventPublisher.sendGroupInActivateEvent(appId, groupId);
        } else {
            log.warn("Group state transition is not valid: [group-id] " + groupId + " [current-state] " + group.getStatus()
                    + "[requested-state] " + status);
        }
        ApplicationHolder.persistApplication(application);
        /*} finally {
            ApplicationHolder.releaseWriteLock();
        }*/
    }

    public static void handleGroupTerminatingEvent(String appId, String groupId) {
        if (log.isInfoEnabled()) {
            log.info("Handling group terminating: [group-id] " + groupId +
                    " [application-id] " + appId);
        }

        Applications applications = ApplicationHolder.getApplications();
        Application application = applications.getApplication(appId);
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application does not exist: [application-id] %s",
                    appId));
            return;
        }

        Group group = application.getGroupRecursively(groupId);
        if (group == null) {
            log.warn(String.format("Group does not exist: [group-id] %s",
                    groupId));
            return;
        }

        try {
            ApplicationHolder.acquireWriteLock();
            GroupStatus status = GroupStatus.Terminating;
            if (group.isStateTransitionValid(status)) {
                log.info("Updating group status to " + status + ": [group-id] " + group.getUniqueIdentifier());
                group.setStatus(status);
                //updating the groupMonitor
                updateGroupMonitor(appId, groupId, status);
                //publishing data
                //TODO ApplicationsEventPublisher.sendGroupTerminatingEvent(appId, groupId);
            } else {
                log.warn("Group state transition is not valid: [group-id] " + groupId + " [current-state] " + group.getStatus()
                        + "[requested-state] " + status);
            }
            ApplicationHolder.persistApplication(application);
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
    }

    private static void updateGroupStatusesRecursively(GroupStatus groupStatus, Collection<Group> groups) {

        for (Group group : groups) {
            if (!group.isStateTransitionValid(groupStatus)) {
                log.error("Invalid state transfer from " + group.getStatus() + " to " + groupStatus);
            }
            // force update for now
            group.setStatus(groupStatus);

            // go recursively and update
            if (group.getGroups() != null) {
                updateGroupStatusesRecursively(groupStatus, group.getGroups());
            }
        }
    }

    private static void updateApplicationMonitor(String appId, ApplicationStatus status) {
        //Updating the Application Monitor
        ApplicationMonitor applicationMonitor = AutoscalerContext.getInstance().getAppMonitor(appId);
        if (applicationMonitor != null) {
            applicationMonitor.setStatus(status);
        } else {
            log.warn("Application monitor cannot be found: [application-id] " + appId);
        }

    }

    private static void updateGroupMonitor(String appId, String groupId, GroupStatus status) {
        //Updating the Application Monitor
        ApplicationMonitor applicationMonitor = AutoscalerContext.getInstance().getAppMonitor(appId);
        if (applicationMonitor != null) {
            GroupMonitor monitor = (GroupMonitor) applicationMonitor.findGroupMonitorWithId(groupId);
            if (monitor != null) {
                monitor.setStatus(status);
            } else {
                log.warn("Group monitor cannot be found: [group-id] " + groupId +
                        " [application-id] " + appId);
            }
        }
    }
}

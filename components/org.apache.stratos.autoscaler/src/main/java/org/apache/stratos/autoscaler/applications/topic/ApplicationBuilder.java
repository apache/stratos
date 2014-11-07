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
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.TopologyInConsistentException;
import org.apache.stratos.autoscaler.monitor.application.ApplicationMonitor;
import org.apache.stratos.autoscaler.monitor.application.ApplicationMonitorFactory;
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
            log.debug("Handling complete application");
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
            log.info("Handling Application creation for the [application]: " +
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
                log.warn("Application [ " + application.getUniqueIdentifier() + " ] already exists in Applications");
            }
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
        ApplicationsEventPublisher.sendApplicationCreatedEvent(application);
    }

    public static void handleApplicationActivatedEvent(String appId) {
        if (log.isInfoEnabled()) {
            log.info("Handling Application activation for the [application]: " + appId);
        }

        Applications applications = ApplicationHolder.getApplications();
        Application application = applications.getApplication(appId);
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application %s does not exist",
                    appId));
            return;
        }

        /*try {
            ApplicationHolder.acquireWriteLock();*/
            ApplicationStatus status = ApplicationStatus.Active;
            if (application.isStateTransitionValid(status)) {
                application.setStatus(status);
                updateApplicationMonitor(appId, status);
                log.info("Application activated adding status started for Applications");
                ApplicationHolder.persistApplication(application);
                //publishing data
                ApplicationsEventPublisher.sendApplicationActivatedEvent(appId);
            }
        /*} finally {
            ApplicationHolder.releaseWriteLock();
        }*/

    }

    public static void handleApplicationUndeployed(String applicationId) {
        if (log.isInfoEnabled()) {
            log.info("Un-deploying the [application] " + applicationId + "by marking it as terminating..");
        }

        ApplicationHolder.acquireReadLock();
        try {
            ApplicationMonitor appMonitor = AutoscalerContext.getInstance().
                    getAppMonitor(applicationId);
            if (appMonitor != null) {
                // update the status as Terminating
                log.info("Application" + applicationId + " updated as terminating" );
                appMonitor.setStatus(ApplicationStatus.Terminating);
            } else {
                log.warn("Application Monitor cannot be found for the undeployed [application] "
                        + applicationId);
            }
        } finally {
            ApplicationHolder.releaseReadLock();

        }
        ApplicationsEventPublisher.sendApplicationTerminatingEvent(applicationId);
    }

    public static void handleApplicationTerminatedEvent(String appId) {
        if (log.isInfoEnabled()) {
            log.info("Handling Application termination for the [application]: " + appId);
        }

        Applications applications = ApplicationHolder.getApplications();
        /*try {
            ApplicationHolder.acquireWriteLock();*/

            if (!applications.applicationExists(appId)) {
                log.warn("Application with id [ " + appId + " ] doesn't exist in Applications");
            } else {
                Application application = applications.getApplication(appId);

                if (!application.isStateTransitionValid(ApplicationStatus.Terminated)) {
                    log.error("Invalid status change from " + application.getStatus() + " to " + ApplicationStatus.Terminated);
                }
                // forcefully set status for now
                application.setStatus(ApplicationStatus.Terminated);
                updateApplicationMonitor(appId, ApplicationStatus.Terminated);
                //removing the monitor
                AutoscalerContext.getInstance().removeAppMonitor(appId);
                //Removing the application from memory and registry
                ApplicationHolder.removeApplication(appId);
                log.info("[Application] " + appId + " is removed");
                ApplicationsEventPublisher.sendApplicationTerminatedEvent(appId);
            }

        /*} finally {
            ApplicationHolder.releaseWriteLock();
        }*/
    }

    public static void handleApplicationTerminatingEvent(String applicationId) {
        if (log.isInfoEnabled()) {
            log.info("Handling Application terminating for the [application]: " + applicationId);
        }

        ApplicationHolder.acquireWriteLock();

        try {
            Applications applications = ApplicationHolder.getApplications();
            if (!applications.applicationExists(applicationId)) {
                log.warn("Application with id [ " + applicationId + " ] doesn't exist in Applications");
                return;
            }

            Application application = applications.getApplication(applicationId);
            // check and update application status to 'Terminating'
            ApplicationStatus status = ApplicationStatus.Terminating;
            if (!application.isStateTransitionValid(status)) {
                log.error("Invalid state transfer from " + application.getStatus() + " to " +
                                                                ApplicationStatus.Terminating);
            }
            // for now anyway update the status forcefully
            application.setStatus(status);
            log.info("Application " + applicationId + "'s status updated to " +
                                                            ApplicationStatus.Terminating);
            updateApplicationMonitor(applicationId, status);
            ApplicationsEventPublisher.sendApplicationTerminatingEvent(applicationId);
        } finally {
            ApplicationHolder.releaseWriteLock();
        }

    }

    public static void handleGroupTerminatedEvent(String appId, String groupId) {
        if (log.isInfoEnabled()) {
            log.info("Handling Group termination for the [group]: " + groupId +
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
            GroupStatus status = GroupStatus.Terminated;
            if (group.isStateTransitionValid(status)) {
                log.info("Group Terminated adding status started for " + group.getUniqueIdentifier());
                group.setStatus(status);
                //updating the groupMonitor
                updateGroupMonitor(appId, groupId, status);
                //publishing data
                ApplicationsEventPublisher.sendGroupTerminatedEvent(appId, groupId);
            } else {
                log.warn("Terminated is not in the possible state list of [group] " + groupId);
            }
            ApplicationHolder.persistApplication(application);
        /*} finally {
            ApplicationHolder.releaseWriteLock();
        }*/
    }

    public static void handleGroupActivatedEvent(String appId, String groupId) {
        if (log.isInfoEnabled()) {
            log.info("Handling Group activation for the [group]: " + groupId +
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
            GroupStatus status = GroupStatus.Active;
            if (group.isStateTransitionValid(status)) {
                log.info("Group Active adding status started for " + group.getUniqueIdentifier());
                group.setStatus(status);
                //updating the groupMonitor
                updateGroupMonitor(appId, groupId, status);
                //publishing data
                ApplicationsEventPublisher.sendGroupActivatedEvent(appId, groupId);
            } else {
                log.warn("Active is not in the possible state list of [group] " + groupId);
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
                log.info("Group created adding status started for " + group.getUniqueIdentifier());
                group.setStatus(status);
                //updating the groupMonitor
                updateGroupMonitor(appId, groupId, status);
                //publishing data
                ApplicationsEventPublisher.sendGroupCreatedEvent(appId, groupId);
            } else {
                log.warn("Created is not in the possible state list of [group] " + groupId);
            }
            ApplicationHolder.persistApplication(application);
        /*} finally {
            ApplicationHolder.releaseWriteLock();
        }*/

    }

    public static void handleGroupInActivateEvent(String appId, String groupId) {
        if (log.isInfoEnabled()) {
            log.info("Handling Group in-active for the [group]: " + groupId +
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
            GroupStatus status = GroupStatus.Inactive;
            if (group.isStateTransitionValid(status)) {
                log.info("Group Inactive adding status started for " + group.getUniqueIdentifier());
                group.setStatus(status);
                //updating the groupMonitor
                updateGroupMonitor(appId, groupId, status);
                //publishing data
                ApplicationsEventPublisher.sendGroupInActivateEvent(appId, groupId);
            } else {
                log.warn("Inactive is not in the possible state list of [group] " + groupId);
            }
            ApplicationHolder.persistApplication(application);
        /*} finally {
            ApplicationHolder.releaseWriteLock();
        }*/
    }

    public static void handleGroupTerminatingEvent(String appId, String groupId) {
        if (log.isInfoEnabled()) {
            log.info("Handling Group terminating for the [group]: " + groupId +
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

        try {
            ApplicationHolder.acquireWriteLock();
            GroupStatus status = GroupStatus.Terminating;
            if (group.isStateTransitionValid(status)) {
                log.info("Group Terminating adding status started for " + group.getUniqueIdentifier());
                group.setStatus(status);
                //updating the groupMonitor
                updateGroupMonitor(appId, groupId, status);
                //publishing data
                ApplicationsEventPublisher.sendGroupTerminatingEvent(appId, groupId);
            } else {
                log.warn("Terminating is not in the possible state list of [group] " + groupId);
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
            log.warn("Application monitor cannot be found for the [application] " + appId);
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
                log.warn("Group monitor cannot be found for the [group] " + groupId + " for the " +
                        "[application] " + appId);
            }
        }

    }
}

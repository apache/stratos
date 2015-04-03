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
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.pojo.ApplicationClusterContext;
import org.apache.stratos.autoscaler.applications.pojo.ApplicationContext;
import org.apache.stratos.autoscaler.client.CloudControllerClient;
import org.apache.stratos.autoscaler.context.AutoscalerContext;
import org.apache.stratos.autoscaler.context.partition.network.GroupLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.NetworkPartitionContext;
import org.apache.stratos.autoscaler.event.publisher.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.component.ApplicationMonitor;
import org.apache.stratos.autoscaler.monitor.component.GroupMonitor;
import org.apache.stratos.messaging.domain.application.*;
import org.apache.stratos.messaging.domain.instance.ApplicationInstance;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.Collection;
import java.util.HashSet;
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

        try {
            ApplicationHolder.acquireReadLock();
            ApplicationsEventPublisher.sendCompleteApplicationsEvent(applications);
        } finally {
            ApplicationHolder.releaseReadLock();
        }
    }

    /**
     * Create application clusters in cloud controller and send application created event.
     * @param application
     * @param appClusterContexts
     */
    public static synchronized void handleApplicationCreatedEvent(Application application,
                                                                  ApplicationClusterContext[] appClusterContexts) {
        if (log.isDebugEnabled()) {
            log.debug("Handling application creation event: [application-id] " +
                    application.getUniqueIdentifier());
        }
        CloudControllerClient.getInstance().createApplicationClusters(application.getUniqueIdentifier(),
		                                                                  appClusterContexts);
        ApplicationHolder.persistApplication(application);
	    ApplicationsEventPublisher.sendApplicationCreatedEvent(application);
    }

    public static ApplicationInstance handleApplicationInstanceCreatedEvent(String appId,
                                                                            String networkPartitionId) {
        if (log.isDebugEnabled()) {
            log.debug("Handling application instance creation event: [application-id] " + appId);
        }
        ApplicationInstance applicationInstance = null;
        //acquiring write lock to add the required instances
        ApplicationHolder.acquireWriteLock();
        try {

            Applications applications = ApplicationHolder.getApplications();
            Application application = applications.getApplication(appId);
            //update the status of the Group
            if (application == null) {
                log.warn(String.format("Application does not exist: [application-id] %s",
                        appId));
                return null;
            }
            String instanceId = application.getNextInstanceId(appId);

            if (!application.containsInstanceContext(instanceId)) {
                //setting the status, persist and publish
                applicationInstance = new ApplicationInstance(appId, instanceId);
                applicationInstance.setNetworkPartitionId(networkPartitionId);
                application.addInstance(instanceId, applicationInstance);
                //updateApplicationMonitor(appId, status);
                ApplicationHolder.persistApplication(application);
                ApplicationsEventPublisher.sendApplicationInstanceCreatedEvent(appId, applicationInstance);
            } else {
                log.warn(String.format("Application Instance Context already exists" +
                        " [appId] %s [ApplicationInstanceId] %s", appId, instanceId));
            }
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
        return applicationInstance;
    }

    public static void handleApplicationInstanceActivatedEvent(String appId, String instanceId) {
        if (log.isDebugEnabled()) {
            log.debug("Handling application activation event: [application-id] " + appId +
                    " [instance] " + instanceId);
        }

        Applications applications = ApplicationHolder.getApplications();
        Application application = applications.getApplication(appId);
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application does not exist: [application-id] %s",
                    appId));
            return;
        }

        ApplicationStatus status = ApplicationStatus.Active;
        ApplicationInstance applicationInstance = application.getInstanceContexts(instanceId);
        if (applicationInstance.isStateTransitionValid(status)) {
            //setting the status, persist and publish
            application.setStatus(status, instanceId);
            updateApplicationMonitor(appId, status, applicationInstance.getNetworkPartitionId(),
                    instanceId);
            ApplicationHolder.persistApplication(application);
            ApplicationsEventPublisher.sendApplicationInstanceActivatedEvent(appId, instanceId);
        } else {
            log.warn(String.format("Application state transition is not valid: [application-id] %s " +
                            " [instance-id] %s [current-status] %s [status-requested] %s",
                    appId, instanceId, applicationInstance.getStatus(), status));
        }
    }

    public static void handleApplicationInstanceInactivateEvent(String appId, String instanceId) {
        if (log.isDebugEnabled()) {
            log.debug("Handling application Inactive event: [application-id] " + appId +
                            " [instance] " + instanceId);
        }

        Applications applications = ApplicationHolder.getApplications();
        Application application = applications.getApplication(appId);
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application does not exist: [application-id] %s",
                    appId));
            return;
        }

        ApplicationStatus status = ApplicationStatus.Inactive;
        ApplicationInstance applicationInstance = application.getInstanceContexts(instanceId);
        if (applicationInstance.isStateTransitionValid(status)) {
            //setting the status, persist and publish
            application.setStatus(status, instanceId);
            updateApplicationMonitor(appId, status, applicationInstance.getNetworkPartitionId(),
                                    instanceId);
            ApplicationHolder.persistApplication(application);
            ApplicationsEventPublisher.sendApplicationInstanceInactivatedEvent(appId, instanceId);
        } else {
            log.warn(String.format("Application state transition is not valid: [application-id] %s " +
                            " [instance-id] %s [current-status] %s [status-requested] %s",
                    appId, instanceId, applicationInstance.getStatus(), status));
        }
    }

    public static void handleApplicationInstanceTerminatingEvent(String appId, String instanceId) {
        if (log.isDebugEnabled()) {
            log.debug("Handling application terminating event: [application-id] " + appId +
                    " [instance] " + instanceId);
        }

        Applications applications = ApplicationHolder.getApplications();
        Application application = applications.getApplication(appId);
        // update the status of the Group
        if (application == null) {
            log.warn(String.format("Application does not exist: [application-id] %s", appId));
            return;
        }

        ApplicationStatus status = ApplicationStatus.Terminating;
        ApplicationInstance applicationInstance = application.getInstanceContexts(instanceId);
        if (applicationInstance.isStateTransitionValid(status)) {
            // setting the status, persist and publish
            application.setStatus(status, instanceId);
            updateApplicationMonitor(appId, status, applicationInstance.getNetworkPartitionId(), instanceId);
            ApplicationHolder.persistApplication(application);
            ApplicationsEventPublisher.sendApplicationInstanceInactivatedEvent(appId, instanceId);
        } else {
            log.warn(String.format("Application state transition is not valid: [application-id] %s " +
                            " [instance-id] %s [current-status] %s [status-requested] %s",
                    appId, instanceId, applicationInstance.getStatus(), status));
        }
    }

    public static void handleApplicationRemoval(String appId) {
        if (log.isDebugEnabled()) {
            log.debug("Handling application delete for [application-id] " + appId);
        }
        Set<ClusterDataHolder> appClusterDataToSend;
        Application application;
        ApplicationHolder.acquireWriteLock();
        try {
            Applications applications = ApplicationHolder.getApplications();
            application = applications.getApplication(appId);
            //update the status of the Group
            if (application == null) {
                log.warn(String.format("Application does not exist: [application-id] %s",
                        appId));
                return;
            } else {
                // Check whether given application is deployed
            	ApplicationContext applicationContext = AutoscalerContext.getInstance().getApplicationContext(appId);
            	if (applicationContext != null && applicationContext.getStatus().equals(ApplicationContext.STATUS_DEPLOYED)) {
            		log.warn(String.format("Application has been found in the ApplicationsTopology" +
            				": [application-id] %s, Please unDeploy the Application Policy " +
            				"before deleting the Application definition.",
            				appId));
            		return;
				}
            }

            //get cluster data to send in event before deleting the application
            appClusterDataToSend = new HashSet<ClusterDataHolder>();
            Set<ClusterDataHolder> appClusterData = application.getClusterDataRecursively();
            for (ClusterDataHolder currClusterData : appClusterData) {
                ClusterDataHolder newClusterData = new ClusterDataHolder(currClusterData.getServiceType(),
                        currClusterData.getClusterId());
                appClusterDataToSend.add(newClusterData);
            }

            AutoscalerContext.getInstance().removeApplicationContext(appId);
            ApplicationHolder.removeApplication(appId);

        } finally {
            ApplicationHolder.releaseWriteLock();
        }

        ApplicationsEventPublisher.sendApplicationDeletedEvent(appId, appClusterDataToSend);
    }

    public static void handleApplicationInstanceTerminatedEvent(String applicationId, String instanceId) {
        if (log.isDebugEnabled()) {
            log.debug("Handling application terminated event: [application-id] " + applicationId +
                    " [instance] " + instanceId);
        }

        Application application = ApplicationHolder.getApplications().getApplication(applicationId);
        ApplicationContext applicationContext = AutoscalerContext.getInstance().getApplicationContext(applicationId);

        if ((application == null) || (applicationContext == null)) {
            log.warn("Application does not exist: [application-id] " + applicationId);
        } else {
            ApplicationInstance applicationInstance = application.getInstanceContexts(instanceId);
            ApplicationStatus status = ApplicationStatus.Terminated;
            if (applicationInstance.isStateTransitionValid(status)) {
                //setting the status, persist and publish
                applicationInstance.setStatus(status);
                updateApplicationMonitor(applicationId, status, applicationInstance.getNetworkPartitionId(),
                                        instanceId);
                ApplicationMonitor applicationMonitor = AutoscalerContext.getInstance().
                        getAppMonitor(applicationId);
                NetworkPartitionContext networkPartitionContext = applicationMonitor.
                        getNetworkPartitionContext(applicationInstance.
                        getNetworkPartitionId());
                networkPartitionContext.removeInstanceContext(instanceId);
                applicationMonitor.removeInstance(instanceId);
                application.removeInstance(instanceId);
                ApplicationsEventPublisher.sendApplicationInstanceTerminatedEvent(applicationId, instanceId);

                //removing the monitor
                if (application.getInstanceContextCount() == 0 &&
                        applicationMonitor.isTerminating()) {
                    //Stopping the child threads
                    if (applicationMonitor.hasMonitors() && applicationMonitor.isTerminating()) {
                        for (Monitor monitor1 : applicationMonitor.getAliasToActiveMonitorsMap().values()) {
                            //destroying the drools
                            monitor1.destroy();
                        }
                    }
                    // stopping application thread
                    applicationMonitor.destroy();
                    AutoscalerContext.getInstance().removeAppMonitor(applicationId);

                    // update application status in application context
                    applicationContext.setStatus(ApplicationContext.STATUS_CREATED);
                    AutoscalerContext.getInstance().updateApplicationContext(applicationContext);

                    log.info("Application undeployed successfully: [application-id] " + applicationId);
                }
            } else {
                log.warn(String.format("Application state transition is not valid: [application-id] %s " +
                                " [current-status] %s [status-requested] %s", applicationId,
                        application.getInstanceContexts(instanceId).getStatus(),
                        status));
            }
        }
    }

    public static boolean handleApplicationUnDeployedEvent(String applicationId) {
        if (log.isDebugEnabled()) {
            log.debug("Handling application terminating event: [application-id] " + applicationId);
        }
        Set<ClusterDataHolder> clusterData;
        ApplicationHolder.acquireWriteLock();
        try {
            Applications applications = ApplicationHolder.getApplications();
            Application application = applications.getApplication(applicationId);
            //update the status of the Group
            if (application == null) {
                log.warn(String.format("Application does not exist: [application-id] %s", applicationId));
                return false;
            }
            clusterData = application.getClusterDataRecursively();
            Collection<ApplicationInstance> applicationInstances = application.
                    getInstanceIdToInstanceContextMap().values();

            for (ApplicationInstance instance : applicationInstances) {
                handleApplicationInstanceTerminatingEvent(applicationId, instance.getInstanceId());
            }
        } finally {
            ApplicationHolder.releaseWriteLock();
        }

        // if monitors is not found for any cluster, assume cluster is not there and send cluster terminating event.
        // this assumes the cluster monitor will not fail after creating members, but will only fail before
        for (ClusterDataHolder aClusterData : clusterData) {
            if (AutoscalerContext.getInstance().getClusterMonitor(aClusterData.getClusterId()) == null) {
                TopologyManager.acquireReadLockForCluster(aClusterData.getServiceType(),
                        aClusterData.getClusterId());
                try {
                    Service service = TopologyManager.getTopology().getService(aClusterData.getServiceType());
                    if (service != null) {
                        Cluster cluster = service.getCluster(aClusterData.getClusterId());
                        if (cluster != null) {
                            for (ClusterInstance instance : cluster.getInstanceIdToInstanceContextMap().values()) {
                                ClusterStatusEventPublisher.sendClusterStatusClusterTerminatingEvent(applicationId,
                                        aClusterData.getServiceType(),
                                        aClusterData.getClusterId(),
                                        instance.getInstanceId());
                            }
                        }
                    }
                } finally {
                    TopologyManager.releaseReadLockForCluster(aClusterData.getServiceType(),
                            aClusterData.getClusterId());
                }

            }
        }
        return true;
    }


    public static void handleGroupInstanceTerminatedEvent(String appId, String groupId, String instanceId) {
        if (log.isDebugEnabled()) {
            log.debug("Handling group terminated event: [group-id] " + groupId +
                    " [application-id] " + appId + " [instance] " + instanceId);
        }
        //lock has already been taken in the processor chain when calculating the status
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
            log.warn(String.format("Group does not exist: [group-id] %s when terminated group",
                    groupId));
            return;
        }

        GroupInstance groupInstance = group.getInstanceContexts(instanceId);
        GroupStatus status = GroupStatus.Terminated;
        String parentId;
        if (groupInstance != null) {
            if (groupInstance.isStateTransitionValid(status)) {
                //setting the status, persist and publish
                groupInstance.setStatus(status);
                parentId = groupInstance.getParentId();
                //removing the group instance and context
                GroupMonitor monitor = getGroupMonitor(appId, groupId);
                ApplicationMonitor applicationMonitor = AutoscalerContext.getInstance().
                        getAppMonitor(appId);

                if (monitor != null) {
                    if (monitor.hasMonitors() && applicationMonitor.isTerminating()) {
                        for (Monitor monitor1 : monitor.getAliasToActiveMonitorsMap().values()) {
                            //destroying the drools
                            monitor1.destroy();
                        }
                    }
                    GroupLevelNetworkPartitionContext networkPartitionContext =
                            (GroupLevelNetworkPartitionContext) monitor.
                                    getNetworkPartitionContext(groupInstance.getNetworkPartitionId());
                    networkPartitionContext.removeInstanceContext(instanceId);
                    if (groupInstance.getPartitionId() != null) {
                        networkPartitionContext.getPartitionCtxt(groupInstance.getPartitionId()).
                                removeActiveInstance(groupInstance);
                    }
                    monitor.removeInstance(instanceId);
                    group.removeInstance(instanceId);
                    ApplicationHolder.persistApplication(application);
                    ApplicationsEventPublisher.sendGroupInstanceTerminatedEvent(appId, groupId, instanceId);
                    monitor.setStatus(status, instanceId, parentId);
                }
            } else {
                log.warn("Group state transition is not valid: [group-id] " + groupId +
                        " [instance-id] " + instanceId + " [current-state] " + groupInstance.getStatus()
                        + "[requested-state] " + status);
            }

        } else {
            log.warn("Group Context is not found for [group-id] " + groupId +
                    " [instance-id] " + instanceId);
        }

    }

    public static void handleGroupInstanceActivatedEvent(String appId, String groupId, String instanceId) {
        if (log.isDebugEnabled()) {
            log.debug("Handling group activation for the [group-id]: " + groupId +
                    " in the [application-id] " + appId + " [instance] " + instanceId);
        }
        //lock has already been taken in the processor chain when calculating the status
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
            log.warn(String.format("Group does not exist: [group-id] %s when activating group",
                    groupId));
            return;
        }

        GroupInstance groupInstance = group.getInstanceContexts(instanceId);
        GroupStatus status = GroupStatus.Active;
        if (groupInstance != null) {
            if (groupInstance.isStateTransitionValid(status)) {
                //setting the status, persist and publish
                groupInstance.setStatus(status);
                updateGroupMonitor(appId, groupId, status, groupInstance.getNetworkPartitionId(),
                        instanceId, groupInstance.getParentId());
                ApplicationHolder.persistApplication(application);
                ApplicationsEventPublisher.sendGroupInstanceActivatedEvent(appId, groupId, instanceId);
            } else {
                log.warn("Group state transition is not valid: [group-id] " + groupId +
                        " [instance-id] " + instanceId + " [current-state] " + groupInstance.getStatus()
                        + "[requested-state] " + status);
            }

        } else {
            log.warn("Group Context is not found for [group-id] " + groupId +
                    " [instance-id] " + instanceId);
        }
    }

    public static GroupInstance handleGroupInstanceCreatedEvent(String appId, String groupId,
                                                                String parentId,
                                                                String networkPartitionId,
                                                                String partitionId) {
        ApplicationHolder.acquireWriteLock();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Handling Group instance creation for the [group]: " + groupId +
                        " in the [application] " + appId );
            }
            Applications applications = ApplicationHolder.getApplications();
            Application application = applications.getApplication(appId);
            //update the status of the Group
            if (application == null) {
                log.warn(String.format("Application %s does not exist",
                        appId));
                return null;
            }

            Group group = application.getGroupRecursively(groupId);
            if (group == null) {
                log.warn(String.format("Group %s does not exist when creating group",
                        groupId));
                return null;
            }

            GroupStatus status = GroupStatus.Created;
            String instanceId = parentId;
            int minGroupInstances = group.getGroupMinInstances();
            int maxGroupInstances = group.getGroupMaxInstances();
            /*
            * When min != 1 or max != 1, we need to generate
            * instance ids as it is having more than one group instances
            */
            if (minGroupInstances > 1 || maxGroupInstances > 1 || group.isGroupScalingEnabled()) {
                instanceId = group.getNextInstanceId(groupId);
            }

            if (!group.containsInstanceContext(instanceId)) {
                //setting the status, persist and publish
                GroupInstance groupInstance = null;
                groupInstance = new GroupInstance(groupId, instanceId);
                groupInstance.setParentId(parentId);
                groupInstance.setPartitionId(partitionId);
                groupInstance.setNetworkPartitionId(networkPartitionId);
                groupInstance.setStatus(status);
                group.addInstance(instanceId, groupInstance);
                //updateGroupMonitor(appId, groupId, status);
                ApplicationHolder.persistApplication(application);
                ApplicationsEventPublisher.sendGroupInstanceCreatedEvent(appId, groupId, groupInstance);
                return groupInstance;
            } else {
                log.warn("Group Instance Context already exists: [group-id] " + groupId +
                        " [Group-Instance-Id] " + instanceId);
            }
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
        return null;
    }


    public static void handleGroupInactivateEvent(String appId, String groupId, String instanceId) {
        if (log.isDebugEnabled()) {
            log.debug("Handling group inactive event: [group]: " + groupId +
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
            log.warn(String.format("Group does not exist: [group-id] %s when inactive group",
                    groupId));
            return;
        }

        GroupInstance groupInstance = group.getInstanceContexts(instanceId);
        GroupStatus status = GroupStatus.Inactive;
        if (groupInstance != null) {
            if (groupInstance.isStateTransitionValid(status)) {
                //setting the status, persist and publish
                groupInstance.setStatus(status);
                updateGroupMonitor(appId, groupId, status, groupInstance.getNetworkPartitionId(),
                        instanceId, groupInstance.getParentId());
                ApplicationHolder.persistApplication(application);
                ApplicationsEventPublisher.sendGroupInstanceInactivateEvent(appId, groupId, instanceId);
            } else {
                log.warn("Group state transition is not valid: [group-id] " + groupId +
                        " [instance-id] " + instanceId + " [current-state] " + groupInstance.getStatus()
                        + "[requested-state] " + status);
            }

        } else {
            log.warn("Group Context is not found for [group-id] " + groupId +
                    " [instance-id] " + instanceId);
        }
    }

    public static void handleGroupTerminatingEvent(String appId, String groupId, String instanceId) {
        if (log.isDebugEnabled()) {
            log.debug("Handling group terminating: [group-id] " + groupId +
                    " [application-id] " + appId + " [instance] " + instanceId);
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
            log.warn(String.format("Group does not exist: [group-id] %s when terminating group",
                    groupId));
            return;
        }

        try {
            ApplicationHolder.acquireWriteLock();
            GroupInstance groupInstance = group.getInstanceContexts(instanceId);
            GroupStatus status = GroupStatus.Terminating;
            if (groupInstance != null) {
                if (groupInstance.isStateTransitionValid(status)) {
                    //setting the status, persist and publish
                    groupInstance.setStatus(status);
                    updateGroupMonitor(appId, groupId, status, groupInstance.getNetworkPartitionId(),
                            instanceId, groupInstance.getParentId());
                    ApplicationHolder.persistApplication(application);
                    ApplicationsEventPublisher.sendGroupInstanceTerminatingEvent(appId, groupId, instanceId);
                } else {
                    log.warn("Group state transition is not valid: [group-id] " + groupId +
                            " [instance-id] " + instanceId + " [current-state] " + groupInstance.getStatus()
                            + "[requested-state] " + status);
                }

            } else {
                log.warn("Group Context is not found for [group-id] " + groupId +
                        " [instance-id] " + instanceId);
            }
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
    }

    private static void updateApplicationMonitor(String appId, ApplicationStatus status,
                                                 String networkPartitionId, String instanceId) {
        //Updating the Application Monitor
        ApplicationMonitor applicationMonitor = AutoscalerContext.getInstance().getAppMonitor(appId);
        NetworkPartitionContext context = applicationMonitor.
                getNetworkPartitionContext(networkPartitionId);
        if (applicationMonitor != null) {
            if(status == ApplicationStatus.Active) {
                context.movePendingInstanceToActiveInstances(instanceId);
            } else if(status == ApplicationStatus.Terminating) {
                applicationMonitor.setTerminating(true);

                if(context.getActiveInstance(instanceId) != null) {
                    context.moveActiveInstanceToTerminationPendingInstances(instanceId);
                } else if(context.getPendingInstance(instanceId) != null) {
                    context.movePendingInstanceToTerminationPendingInstances(instanceId);
                }
            } else if(status == ApplicationStatus.Terminated) {
                context.removeTerminationPendingInstance(instanceId);
            }
            applicationMonitor.setStatus(status, instanceId);
        } else {
            log.warn("Application monitor cannot be found: [application-id] " + appId);
        }

    }

    private static void updateGroupMonitor(String appId, String groupId,
                                           GroupStatus status, String networkPartitionId,
                                           String instanceId, String parentInstanceId) {
        GroupMonitor monitor = getGroupMonitor(appId, groupId);
        if (monitor != null) {
            NetworkPartitionContext context = monitor.getNetworkPartitionContext(networkPartitionId);
            if(status == GroupStatus.Active) {
                context.movePendingInstanceToActiveInstances(instanceId);
            } else if(status == GroupStatus.Terminating) {
                if(context.getActiveInstance(instanceId) != null) {
                    context.moveActiveInstanceToTerminationPendingInstances(instanceId);
                } else if(context.getPendingInstance(instanceId) != null) {
                    context.movePendingInstanceToTerminationPendingInstances(instanceId);
                }
            } else if(status == GroupStatus.Terminated) {
                context.removeTerminationPendingInstance(instanceId);
            }
            monitor.setStatus(status, instanceId, parentInstanceId);
        } else {
            log.warn("Group monitor cannot be found: [group-id] " + groupId +
                    " [application-id] " + appId);
        }
    }

    private static GroupMonitor getGroupMonitor(String appId, String groupId) {
        //Updating the Application Monitor
        ApplicationMonitor applicationMonitor = AutoscalerContext.getInstance().getAppMonitor(appId);
        if (applicationMonitor != null) {
            GroupMonitor monitor = (GroupMonitor) applicationMonitor.findGroupMonitorWithId(groupId);
            return monitor;
        }
        return null;
    }
}

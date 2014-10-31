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
import org.apache.stratos.messaging.domain.applications.*;

import java.util.Collection;
import java.util.Set;

/**
 * This will build the application.
 */
public class ApplicationBuilder {
    private static final Log log = LogFactory.getLog(ApplicationBuilder.class);


    /*public static synchronized void handleApplicationDeployed(Application application,
                                                              Set<ApplicationClusterContext> applicationClusterContexts,
                                                              Set<MetaDataHolder> metaDataHolders) {


        Applications applications = Appcation.getApplications();
        try {
            ApplicationHolder.acquireWriteLock();

            if (applications.applicationExists(application.getUniqueIdentifier())) {
                log.warn("Application with id [ " + application.getUniqueIdentifier() + " ] already exists in Applications");
                return;
            }
            List<Cluster> clusters = new ArrayList<Cluster>();
            for (ApplicationClusterContext applicationClusterContext : applicationClusterContexts) {
                Cluster cluster = new Cluster(applicationClusterContext.getCartridgeType(),
                        applicationClusterContext.getClusterId(), applicationClusterContext.getDeploymentPolicyName(),
                        applicationClusterContext.getAutoscalePolicyName(), application.getUniqueIdentifier());
                //cluster.setStatus(Status.Created);
                cluster.addHostName(applicationClusterContext.getHostName());
                cluster.setTenantRange(applicationClusterContext.getTenantRange());
                clusters.add(cluster);

                Service service = applications.getService(applicationClusterContext.getCartridgeType());
                if (service != null) {
                    service.addCluster(cluster);
                    log.info("Added Cluster " + cluster.toString() + " to Applications for Application with id: " + application.getUniqueIdentifier());
                } else {
                    log.error("Service " + applicationClusterContext.getCartridgeType() + " not found");
                    return;
                }
            }

            // add to Applications and update
            applications.addApplication(application);
            ApplicationHolder.persistApplication(applications);

            log.info("Application with id [ " + application.getUniqueIdentifier() + " ] added to Applications successfully");
            org.apache.stratos.messaging.event.applications.ApplicationCreatedEvent applicationCreatedEvent = new org.apache.stratos.messaging.event.applications.ApplicationCreatedEvent(application, clusters);
            ApplicationsEventPublisher.sendApplicationCreatedEvent(applicationCreatedEvent);

        } finally {
            ApplicationHolder.releaseWriteLock();
        }
    }*/

    /*public static synchronized void handleApplicationUndeployed(String applicationId) {

        Set<ClusterDataHolder> clusterData;

        // update the Application and Cluster Statuses as 'Terminating'
        ApplicationHolder.acquireWriteLock();

        try {

            Applications applications = ApplicationHolder.getApplications();

            if (!applications.applicationExists(applicationId)) {
                log.warn("Application with id [ " + applicationId + " ] doesn't exist in Applications");
                return;
            }

            Application application = applications.getApplication(applicationId);
            // check and update application status to 'Terminating'
            if (!application.isStateTransitionValid(ApplicationStatus.Terminating)) {
                log.error("Invalid state transfer from " + application.getStatus() + " to " + ApplicationStatus.Terminating);
            }
            // for now anyway update the status forcefully
            application.setStatus(ApplicationStatus.Terminating);

            // update all the Clusters' statuses to 'Terminating'
            clusterData = application.getClusterDataRecursively();
            for (ClusterDataHolder clusterDataHolder : clusterData) {
                Service service = applications.getService(clusterDataHolder.getServiceType());
                if (service != null) {
                    Cluster aCluster = service.getCluster(clusterDataHolder.getClusterId());
                    if (aCluster != null) {
                        // validate state transition
                        if (!aCluster.isStateTransitionValid(ClusterStatus.Terminating)) {
                            log.error("Invalid state transfer from " + aCluster.getStatus() + " to "
                                    + ClusterStatus.Terminating);
                        }
                        // for now anyway update the status forcefully
                        aCluster.setStatus(ClusterStatus.Terminating);

                    } else {
                        log.warn("Unable to find Cluster with cluster id " + clusterDataHolder.getClusterId() +
                                " in Applications");
                    }

                } else {
                    log.warn("Unable to remove cluster with cluster id: " + clusterDataHolder.getClusterId() + " from Applications, " +
                            " associated Service [ " + clusterDataHolder.getServiceType() + " ] not found");
                }
            }

            // update all Group's statuses to 'Terminating'
            if (application.getGroups() != null) {
                updateGroupStatusesRecursively(GroupStatus.Terminating, application.getGroups());
            }

            ApplicationHolder.persistApplication(applications);

        } finally {
            ApplicationHolder.releaseWriteLock();
        }

        ApplicationsEventPublisher.sendApplicationUndeployedEvent(applicationId, clusterData);
    }*/
    public static void handleApplicationCreated (Application application,
                                                 Set<ApplicationClusterContext> appClusterCtxts) {

        Applications applications = ApplicationHolder.getApplications();

        ApplicationHolder.acquireWriteLock();

        try {
            if (applications.getApplication(application.getUniqueIdentifier()) != null) {
                ApplicationHolder.persistApplication(application);
            } else {
                log.warn("Application [ " + application.getUniqueIdentifier() + " ] already exists in Applications");
            }

        } finally {
            ApplicationHolder.releaseWriteLock();
        }

        ApplicationsEventPublisher.sendApplicationCreatedEvent(application);
    }

    public static void handleApplicationUndeployed (String applicationId) {

        Applications applications = ApplicationHolder.getApplications();

        ApplicationHolder.acquireWriteLock();
        Application applicationToRemove = applications.getApplication(applicationId);
        Set<ClusterDataHolder> clusterData = null;

        try {
            if (applicationToRemove != null) {
                clusterData = applicationToRemove.getClusterDataRecursively();
                ApplicationHolder.removeApplication(applicationId);
            } else {
                log.warn("Application [ " + applicationId + " ] not found among existing Applications");
            }

        } finally {
            ApplicationHolder.releaseWriteLock();
        }

        ApplicationsEventPublisher.sendApplicationUndeployedEvent(applicationId, clusterData);
    }

    public static void handleGroupTerminatedEvent(String appId, String groupId) {
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
            group.setStatus(GroupStatus.Terminated);
            log.info("Group terminated adding status started for " + group.getUniqueIdentifier());

            ApplicationHolder.persistApplication(application);
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
        //publishing data
        ApplicationsEventPublisher.sendGroupTerminatedEvent(appId, groupId);
    }

    public static void handleGroupActivatedEvent(String appId, String groupId) {
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
            group.setStatus(GroupStatus.Active);
            log.info("Group activated adding status started for " + group.getUniqueIdentifier());

            ApplicationHolder.persistApplication(application);
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
        //publishing data
        ApplicationsEventPublisher.sendGroupActivatedEvent(application.getUniqueIdentifier(),
                group.getUniqueIdentifier());
    }

    public static void handleGroupCreatedEvent(String appId, String groupId) {
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
            group.setStatus(GroupStatus.Created);
            log.info("Group created adding status started for " + group.getUniqueIdentifier());

            ApplicationHolder.persistApplication(application);
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
        //publishing data
        ApplicationsEventPublisher.sendGroupCreatedEvent(appId, groupId);
    }

    public static void handleGroupInActivateEvent(String appId, String groupId) {
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
            group.setStatus(GroupStatus.Inactive);
            log.info("Group in-active adding status started for " + group.getUniqueIdentifier());

            ApplicationHolder.persistApplication(application);
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
        //publishing data
        ApplicationsEventPublisher.sendGroupInActivateEvent(appId, groupId);
    }

    public static void handleGroupTerminatingEvent(String appId, String groupId) {
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
            group.setStatus(GroupStatus.Terminating);
            log.info("Group terminating adding status started for " + group.getUniqueIdentifier());

            ApplicationHolder.persistApplication(application);
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
        //publishing data
        ApplicationsEventPublisher.sendGroupTerminatingEvent(appId, groupId);
    }

    public static void handleApplicationActivatedEvent(String appId) {
        Applications applications = ApplicationHolder.getApplications();
        Application application = applications.getApplication(appId);
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application %s does not exist",
                    appId));
            return;
        }

        try {
            ApplicationHolder.acquireWriteLock();
            application.setStatus(ApplicationStatus.Active);
            log.info("Application activated adding status started for Applications");

            ApplicationHolder.persistApplication(application);
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
        //publishing data
        ApplicationsEventPublisher.sendApplicationActivatedEvent(appId);
    }

    /*public static void handleApplicationCreatedEvent(ApplicationCreatedEvent event) {
        Applications applications = ApplicationHolder.getApplications();
        Application application = applications.getApplication(event.getAppId());
        //update the status of the Group
        if (application == null) {
            log.warn(String.format("Application %s does not exist",
                    event.getAppId()));
            return;
        }
        List<Cluster> clusters = new ArrayList<Cluster>();
        Set<ClusterDataHolder> allClusters = application.getClusterDataRecursively();

        for (ClusterDataHolder clusterDataHolder : allClusters) {
            String clusterId = clusterDataHolder.getClusterId();
            String serviceName = clusterDataHolder.getServiceType();
            clusters.add(ApplicationHolder.getApplications().getService(serviceName).getCluster(clusterId));
        }
        org.apache.stratos.messaging.event.applications.ApplicationCreatedEvent applicationActivatedEvent =
                new org.apache.stratos.messaging.event.applications.ApplicationCreatedEvent(
                        application, clusters);
        try {
            ApplicationHolder.acquireWriteLock();
            application.setStatus(ApplicationStatus.Created);
            log.info("Application created adding status started for Applications");

            ApplicationHolder.persistApplication(applications);
        } finally {
            ApplicationHolder.releaseWriteLock();
        }
        //publishing data
        ApplicationsEventPublisher.sendApplicationCreatedEvent(applicationActivatedEvent);
    }*/

    public static void handleApplicationTerminatingEvent(String appId) {

        String applicationId = appId;

        // update the Application Status as 'Terminating'
        ApplicationHolder.acquireWriteLock();

        try {

            Applications applications = ApplicationHolder.getApplications();

            if (!applications.applicationExists(applicationId)) {
                log.warn("Application with id [ " + applicationId + " ] doesn't exist in Applications");
                return;
            }

            Application application = applications.getApplication(applicationId);
            // check and update application status to 'Terminating'
            if (!application.isStateTransitionValid(ApplicationStatus.Terminating)) {
                log.error("Invalid state transfer from " + application.getStatus() + " to " + ApplicationStatus.Terminating);
            }
            // for now anyway update the status forcefully
            application.setStatus(ApplicationStatus.Terminating);
            log.info("Application " + applicationId + "'s status updated to " + ApplicationStatus.Terminating);

        } finally {
            ApplicationHolder.releaseWriteLock();
        }

        ApplicationsEventPublisher.sendApplicationTerminatingEvent(applicationId);
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

    public static void handleApplicationTerminatedEvent(String appId) {

        Applications applications = ApplicationHolder.getApplications();

        try {
            ApplicationHolder.acquireWriteLock();

            if (!applications.applicationExists(appId)) {
                log.warn("Application with id [ " + appId + " ] doesn't exist in Applications");
                //ApplicationsEventPublisher.sendApplicationRemovedEvent(applicationId, tenantId, tenantDomain);

            } else {
                Application application = applications.getApplication(appId);

                if (!application.isStateTransitionValid(ApplicationStatus.Terminated)) {
                    log.error("Invalid status change from " + application.getStatus() + " to " + ApplicationStatus.Terminated);
                }
                // forcefully set status for now
                application.setStatus(ApplicationStatus.Terminated);

                int tenantId = application.getTenantId();
                String tenantDomain = application.getTenantDomain();
                Set<ClusterDataHolder> clusterData = application.getClusterDataRecursively();
                // remove clusters
                /*for (ClusterDataHolder clusterDataHolder : clusterData) {
                    Service service = applications.getService(clusterDataHolder.getServiceType());
                    if (service != null) {
                        // remove Cluster
                        service.removeCluster(clusterDataHolder.getClusterId());

                        if (log.isDebugEnabled()) {
                            log.debug("Removed cluster with id " + clusterDataHolder.getClusterId());
                        }
                    } else {
                        log.warn("Unable to remove cluster with cluster id: " + clusterDataHolder.getClusterId() + " from Applications, " +
                                " associated Service [ " + clusterDataHolder.getServiceType() + " ] npt found");
                    }

                    // remove runtime data
                    FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();
                    dataHolder.removeClusterContext(clusterDataHolder.getClusterId());
                    if (log.isDebugEnabled()) {
                        log.debug("Removed Cluster Context for Cluster id: " + clusterDataHolder.getClusterId());
                    }

                    try {
                        RegistryManager.getInstance().persist(dataHolder);
                    } catch (RegistryException e) {
                        log.error("Unable to persist data in Registry", e);
                    }
                }


                // remove application
                applications.removeApplication(event.getAppId());
                ApplicationHolder.persistApplication(applications);

                deleteAppResourcesFromMetadataService(event);

                log.info("Removed application [ " + event.getAppId() + " ] from Applications");

                ApplicationsEventPublisher.sendApplicationTerminatedEvent(new org.apache.stratos.messaging.event.applications.ApplicationTerminatedEvent(event.getAppId(),
                        clusterData, tenantId, tenantDomain))*/
                ;
            }

        } finally {
            ApplicationHolder.releaseWriteLock();
        }
    }
}

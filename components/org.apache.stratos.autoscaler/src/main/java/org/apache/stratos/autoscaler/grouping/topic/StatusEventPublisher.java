package org.apache.stratos.autoscaler.grouping.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.application.status.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.util.Constants;

import java.util.Set;

/**
 * This will publish application related events to application status topic.
 */
public class StatusEventPublisher {
    private static final Log log = LogFactory.getLog(StatusEventPublisher.class);

    public static void sendClusterCreatedEvent(String appId, String serviceName, String clusterId) {
        try {
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                if (cluster.isStateTransitionValid(ClusterStatus.Active)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Cluster activated event for [application]: " + appId +
                                " [cluster]: " + clusterId);
                    }
                    AppStatusClusterActivatedEvent clusterActivatedEvent =
                            new AppStatusClusterActivatedEvent(appId, serviceName, clusterId);

                    publishEvent(clusterActivatedEvent);
                } else {
                    log.warn("Active is not in the possible state list of [cluster] " + clusterId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
        }
    }

    public static void sendClusterActivatedEvent(String appId, String serviceName, String clusterId) {
        try {
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                if (cluster.isStateTransitionValid(ClusterStatus.Active)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Cluster activated event for [application]: " + appId +
                                " [cluster]: " + clusterId);
                    }
                    AppStatusClusterActivatedEvent clusterActivatedEvent =
                            new AppStatusClusterActivatedEvent(appId, serviceName, clusterId);

                    publishEvent(clusterActivatedEvent);
                } else {
                    log.warn("Active is not in the possible state list of [cluster] " + clusterId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
        }
    }

    public static void sendClusterInActivateEvent(String appId, String serviceName, String clusterId) {
        try {
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                if (cluster.isStateTransitionValid(ClusterStatus.Inactive)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Cluster in-activate event for [application]: " + appId +
                                " [cluster]: " + clusterId);
                    }
                    AppStatusClusterInactivateEvent clusterInActivateEvent =
                            new AppStatusClusterInactivateEvent(appId, serviceName, clusterId);

                    publishEvent(clusterInActivateEvent);
                } else {
                    log.warn("In-active is not in the possible state list of [cluster] " + clusterId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);

        }
    }

    public static void sendClusterTerminatingEvent(String appId, String serviceName, String clusterId) {

        try {
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                if (cluster.isStateTransitionValid(ClusterStatus.Terminating)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Cluster Terminating event for [application]: " + appId +
                                " [cluster]: " + clusterId);
                    }
                    AppStatusClusterTerminatingEvent appStatusClusterTerminatingEvent =
                            new AppStatusClusterTerminatingEvent(appId, serviceName, clusterId);

                    publishEvent(appStatusClusterTerminatingEvent);
                } else {
                    log.warn("Terminating is not in the possible state list of [cluster] " + clusterId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);

        }

    }

    public static void sendClusterTerminatedEvent(String appId, String serviceName, String clusterId) {
        try {
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            Service service = TopologyManager.getTopology().getService(serviceName);
            if (service != null) {
                Cluster cluster = service.getCluster(clusterId);
                if (cluster.isStateTransitionValid(ClusterStatus.Terminated)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Cluster terminated event for [application]: " + appId +
                                " [cluster]: " + clusterId);
                    }
                    AppStatusClusterTerminatedEvent appStatusClusterTerminatedEvent =
                            new AppStatusClusterTerminatedEvent(appId, serviceName, clusterId);

                    publishEvent(appStatusClusterTerminatedEvent);
                } else {
                    log.warn("Terminated is not in the possible state list of [cluster] " + clusterId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForCluster(serviceName, clusterId);

        }
    }

    public static void sendGroupActivatedEvent(String appId, String groupId) {
        try {
            TopologyManager.acquireReadLockForApplication(appId);
            Application application = TopologyManager.getTopology().getApplication(appId);
            if (application != null) {
                Group group = application.getGroupRecursively(groupId);
                if (group.isStateTransitionValid(GroupStatus.Active)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Group activated event for [application]: " + appId +
                                " [group]: " + groupId);
                    }
                    AppStatusGroupActivatedEvent groupActivatedEvent =
                            new AppStatusGroupActivatedEvent(appId, groupId);

                    publishEvent(groupActivatedEvent);
                } else {
                    log.warn("Active is not in the possible state list of [group] " + groupId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForApplication(appId);
        }
    }

    public static void sendGroupInActivateEvent(String appId, String groupId) {
        try {
            TopologyManager.acquireReadLockForApplication(appId);
            Application application = TopologyManager.getTopology().getApplication(appId);
            if (application != null) {
                Group group = application.getGroupRecursively(groupId);
                if (group.isStateTransitionValid(GroupStatus.Inactive)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Group in-activate event for [application]: " + appId +
                                " [group]: " + groupId);
                    }
                    AppStatusGroupInactivateEvent appStatusGroupInactivateEvent = new
                            AppStatusGroupInactivateEvent(appId, groupId);

                    publishEvent(appStatusGroupInactivateEvent);
                } else {
                    log.warn("InActive is not in the possible state list of [group] " + groupId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForApplication(appId);
        }
    }

    public static void sendGroupTerminatingEvent(String appId, String groupId) {
        try {
            TopologyManager.acquireReadLockForApplication(appId);
            Application application = TopologyManager.getTopology().getApplication(appId);
            if (application != null) {
                Group group = application.getGroupRecursively(groupId);
                if (group.isStateTransitionValid(GroupStatus.Terminating)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Group terminating event for [application]: " + appId +
                                " [group]: " + groupId);
                    }
                    AppStatusGroupTerminatingEvent groupInTerminatingEvent =
                            new AppStatusGroupTerminatingEvent(appId, groupId);
                    publishEvent(groupInTerminatingEvent);
                } else {
                    log.warn("Terminating is not in the possible state list of [group] " + groupId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForApplication(appId);
        }
    }

    public static void sendGroupTerminatedEvent(String appId, String groupId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Group terminated event for [application]: " + appId +
                    " [group]: " + groupId);
        }

        try {
            TopologyManager.acquireReadLockForApplication(appId);
            Application application = TopologyManager.getTopology().getApplication(appId);
            if (application != null) {
                Group group = application.getGroupRecursively(groupId);
                if (group.isStateTransitionValid(GroupStatus.Terminated)) {
                    AppStatusGroupTerminatedEvent groupInTerminatedEvent =
                            new AppStatusGroupTerminatedEvent(appId, groupId);
                    publishEvent(groupInTerminatedEvent);
                } else {
                    log.warn("Terminated is not in the possible state list of [group] " + groupId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForApplication(appId);
        }


    }

    public static void sendApplicationActivatedEvent(String appId) {
        try {
            TopologyManager.acquireReadLockForApplication(appId);
            Application application = TopologyManager.getTopology().getApplication(appId);
            if (application != null) {
                if (application.isStateTransitionValid(ApplicationStatus.Active)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Application activated event for [application]: " + appId);
                    }
                    AppStatusApplicationActivatedEvent applicationActivatedEvent =
                            new AppStatusApplicationActivatedEvent(appId);

                    publishEvent(applicationActivatedEvent);
                } else {
                    log.warn("Active is not in the possible state list of [application] " + appId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForApplication(appId);
        }
    }

    public static void sendApplicationInactivatedEvent(String appId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Application In-activated event for [application]: " + appId);
        }

        try {
            TopologyManager.acquireReadLockForApplication(appId);
            Application application = TopologyManager.getTopology().getApplication(appId);
            if (application != null) {
                if (application.isStateTransitionValid(ApplicationStatus.Inactive)) {
                    AppStatusApplicationInactivatedEvent applicationInActivatedEvent =
                            new AppStatusApplicationInactivatedEvent(appId);
                    publishEvent(applicationInActivatedEvent);
                } else {
                    log.warn("Inactive is not in the possible state list of [application] " + appId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForApplication(appId);
        }
    }

    public static void sendApplicationTerminatingEvent(String appId) {
        try {
            TopologyManager.acquireReadLockForApplication(appId);
            Application application = TopologyManager.getTopology().getApplication(appId);
            if (application != null) {
                if (application.isStateTransitionValid(ApplicationStatus.Terminating)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Application terminated event for [application]: " + appId);
                    }
                    AppStatusApplicationTerminatingEvent applicationTerminatingEvent =
                            new AppStatusApplicationTerminatingEvent(appId);
                    publishEvent(applicationTerminatingEvent);
                } else {
                    log.warn("Terminating is not in the possible state list of [application] " + appId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForApplication(appId);
        }
    }

    public static void sendApplicationTerminatedEvent(String appId, Set<ClusterDataHolder> clusterData) {
        try {
            TopologyManager.acquireReadLockForApplication(appId);
            Application application = TopologyManager.getTopology().getApplication(appId);
            if (application != null) {
                if (application.isStateTransitionValid(ApplicationStatus.Terminated)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Application terminated event for [application]: " + appId);
                    }
                    AppStatusApplicationTerminatedEvent applicationTerminatedEvent =
                            new AppStatusApplicationTerminatedEvent(appId, clusterData);
                    publishEvent(applicationTerminatedEvent);
                } else {
                    log.warn("Terminated is not in the possible state list of [application] " + appId);
                }
            }
        } finally {
            TopologyManager.releaseReadLockForApplication(appId);
        }
    }

    public static void publishEvent(Event event) {
        //publishing events to application status topic
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(Constants.APPLICATION_STATUS_TOPIC);
        eventPublisher.publish(event);
    }

}

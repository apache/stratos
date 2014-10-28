package org.apache.stratos.autoscaler.grouping.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.application.status.*;
import org.apache.stratos.messaging.event.application.status.AppStatusApplicationActivatedEvent;
import org.apache.stratos.messaging.event.application.status.AppStatusApplicationInactivatedEvent;
import org.apache.stratos.messaging.event.application.status.AppStatusApplicationTerminatedEvent;
import org.apache.stratos.messaging.event.application.status.AppStatusApplicationTerminatingEvent;
import org.apache.stratos.messaging.event.application.status.AppStatusClusterActivatedEvent;
import org.apache.stratos.messaging.event.application.status.AppStatusClusterInactivateEvent;
import org.apache.stratos.messaging.event.application.status.AppStatusGroupActivatedEvent;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.util.Constants;

import java.util.Set;

/**
 * This will publish application related events to application status topic.
 */
public class StatusEventPublisher {
    private static final Log log = LogFactory.getLog(StatusEventPublisher.class);

    public static void sendClusterActivatedEvent(String appId, String serviceName, String clusterId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Cluster activated event for [application]: " + appId +
                    " [cluster]: " + clusterId);
        }

        AppStatusClusterActivatedEvent clusterActivatedEvent =
                                            new AppStatusClusterActivatedEvent(appId, serviceName, clusterId);

        publishEvent(clusterActivatedEvent);
    }

    public static void sendClusterInActivateEvent(String appId, String serviceName, String clusterId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Cluster in-activate event for [application]: " + appId +
                    " [cluster]: " + clusterId);
        }

        AppStatusClusterInactivateEvent clusterInActivateEvent =
                                        new AppStatusClusterInactivateEvent(appId, serviceName, clusterId);

        publishEvent(clusterInActivateEvent);
    }

    public static void sendClusterTerminatingEvent(String appId, String serviceName, String clusterId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Cluster in-activate event for [application]: " + appId +
                    " [cluster]: " + clusterId);
        }
        //TODO
        AppStatusClusterTerminatingEvent appStatusClusterTerminatingEvent =
                new AppStatusClusterTerminatingEvent(appId, serviceName, clusterId);

        publishEvent(appStatusClusterTerminatingEvent);
    }

    public static void sendClusterTerminatedEvent(String appId, String serviceName, String clusterId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Cluster in-activate event for [application]: " + appId +
                    " [cluster]: " + clusterId);
        }

        AppStatusClusterTerminatedEvent appStatusClusterTerminatedEvent =
                new AppStatusClusterTerminatedEvent(appId, serviceName, clusterId);

        publishEvent(appStatusClusterTerminatedEvent);
    }

    public static void sendGroupActivatedEvent(String appId, String groupId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Group activated event for [application]: " + appId +
                    " [group]: " + groupId);
        }

        try {
            TopologyManager.acquireReadLockForApplication(appId);
            Application application = TopologyManager.getTopology().getApplication(appId);
            if(application != null) {
                Group group = application.getGroupRecursively(groupId);
                if(group.getStatus().getNextStates().contains(GroupStatus.Active)) {
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

        if (log.isInfoEnabled()) {
            log.info("Publishing Group in-activate event for [application]: " + appId +
                    " [group]: " + groupId);
        }
        try {
            TopologyManager.acquireReadLockForApplication(appId);
            Application application = TopologyManager.getTopology().getApplication(appId);
            if(application != null) {
                Group group = application.getGroupRecursively(groupId);
                if(group.getStatus().getNextStates().contains(GroupStatus.Inactive)) {
                    AppStatusGroupInactivateEvent appStatusGroupInactivateEvent= new
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

        if (log.isInfoEnabled()) {
            log.info("Publishing Group terminating event for [application]: " + appId +
                    " [group]: " + groupId);
        }
        try {
            TopologyManager.acquireReadLockForApplication(appId);
            Application application = TopologyManager.getTopology().getApplication(appId);
            if(application != null) {
                Group group = application.getGroupRecursively(groupId);
                if(group.getStatus().getNextStates().contains(GroupStatus.Terminating)) {
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
            if(application != null) {
                Group group = application.getGroupRecursively(groupId);
                if(group.getStatus().getNextStates().contains(GroupStatus.Terminated)) {
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

        if (log.isInfoEnabled()) {
            log.info("Publishing Application activated event for [application]: " + appId);
        }

        AppStatusApplicationActivatedEvent applicationActivatedEvent = new AppStatusApplicationActivatedEvent(appId);

        publishEvent(applicationActivatedEvent);
    }

    public static void sendApplicationInactivatedEvent(String appId){
        if (log.isInfoEnabled()) {
            log.info("Publishing Application Inactivated event for [application]: " + appId);
        }

        AppStatusApplicationInactivatedEvent applicationInActivatedEvent = new AppStatusApplicationInactivatedEvent(appId);

        publishEvent(applicationInActivatedEvent);
    }

    public static void sendApplicationTerminatingEvent (String appId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Application terminated event for [application]: " + appId);
        }

        AppStatusApplicationTerminatingEvent applicationTerminatingEvent = new AppStatusApplicationTerminatingEvent(appId);

        publishEvent(applicationTerminatingEvent);
    }

    public static void sendApplicationTerminatedEvent (String appId, Set<ClusterDataHolder> clusterData) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Application terminated event for [application]: " + appId);
        }

        AppStatusApplicationTerminatedEvent applicationTerminatedEvent =
                new AppStatusApplicationTerminatedEvent(appId, clusterData);

        publishEvent(applicationTerminatedEvent);
    }

    public static void publishEvent(Event event) {
        //publishing events to application status topic
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(Constants.APPLICATION_STATUS_TOPIC);
        eventPublisher.publish(event);
    }

}

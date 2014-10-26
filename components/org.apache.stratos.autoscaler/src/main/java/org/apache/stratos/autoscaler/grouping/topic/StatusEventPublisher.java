package org.apache.stratos.autoscaler.grouping.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.topology.ClusterDataHolder;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.application.status.*;
import org.apache.stratos.messaging.event.application.status.ApplicationActivatedEvent;
import org.apache.stratos.messaging.event.application.status.ApplicationInactivatedEvent;
import org.apache.stratos.messaging.event.application.status.ApplicationTerminatedEvent;
import org.apache.stratos.messaging.event.application.status.ApplicationTerminatingEvent;
import org.apache.stratos.messaging.event.application.status.ClusterActivatedEvent;
import org.apache.stratos.messaging.event.application.status.ClusterInActivateEvent;
import org.apache.stratos.messaging.event.application.status.ClusterMaintenanceModeEvent;
import org.apache.stratos.messaging.event.application.status.GroupActivatedEvent;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.event.topology.GroupInactivateEvent;
import org.apache.stratos.messaging.util.Constants;

import java.util.Set;

/**
 * This will publish application related events to application status topic.
 */
public class StatusEventPublisher {
    private static final Log log = LogFactory.getLog(StatusEventPublisher.class);

    public static void sendClusterCreatedEvent(String appId, String serviceName, String clusterId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Cluster activated event for [application]: " + appId +
                    " [cluster]: " + clusterId);
        }


        ClusterCreatedEvent clusterCreatedEvent = new ClusterCreatedEvent(appId, serviceName, clusterId);

        publishEvent(clusterCreatedEvent);
    }

    public static void sendClusterActivatedEvent(String appId, String serviceName, String clusterId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Cluster activated event for [application]: " + appId +
                    " [cluster]: " + clusterId);
        }

        ClusterActivatedEvent clusterActivatedEvent =
                                            new ClusterActivatedEvent(appId, serviceName, clusterId);

        publishEvent(clusterActivatedEvent);
    }

    public static void sendClusterInActivateEvent(String appId, String serviceName, String clusterId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Cluster in-activate event for [application]: " + appId +
                    " [cluster]: " + clusterId);
        }

        ClusterInActivateEvent clusterInActivateEvent =
                                        new ClusterInActivateEvent(appId, serviceName, clusterId);

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

        GroupActivatedEvent groupActivatedEvent = new GroupActivatedEvent(appId, groupId);

        publishEvent(groupActivatedEvent);
    }

    public static void sendGroupInActivateEvent(String appId, String groupId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Group in-activate event for [application]: " + appId +
                    " [group]: " + groupId);
        }

        GroupInactivateEvent groupInactivateEvent = new GroupInactivateEvent(appId, groupId);

        publishEvent(groupInactivateEvent);
    }

    public static void sendGroupTerminatingEvent(String appId, String groupId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Group terminating event for [application]: " + appId +
                    " [group]: " + groupId);
        }

        GroupInTerminatingEvent groupInTerminatingEvent = new GroupInTerminatingEvent(appId, groupId);
        publishEvent(groupInTerminatingEvent);
    }

    public static void sendGroupTerminatedEvent(String appId, String groupId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Group terminated event for [application]: " + appId +
                    " [group]: " + groupId);
        }

        GroupInTerminatedEvent groupInTerminatedEvent = new GroupInTerminatedEvent(appId, groupId);
        publishEvent(groupInTerminatedEvent);
    }

    public static void sendApplicationActivatedEvent(String appId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Application activated event for [application]: " + appId);
        }

        ApplicationActivatedEvent applicationActivatedEvent = new ApplicationActivatedEvent(appId);

        publishEvent(applicationActivatedEvent);
    }

    public static void sendApplicationInactivatedEvent(String appId){
        if (log.isInfoEnabled()) {
            log.info("Publishing Application Inactivated event for [application]: " + appId);
        }

        ApplicationInactivatedEvent applicationInActivatedEvent = new ApplicationInactivatedEvent(appId);

        publishEvent(applicationInActivatedEvent);
    }

    public static void sendApplicationTerminatingEvent (String appId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Application terminated event for [application]: " + appId);
        }

        ApplicationTerminatingEvent applicationTerminatingEvent = new ApplicationTerminatingEvent(appId);

        publishEvent(applicationTerminatingEvent);
    }

    public static void sendApplicationTerminatedEvent (String appId, Set<ClusterDataHolder> clusterData) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Application terminated event for [application]: " + appId);
        }

        ApplicationTerminatedEvent applicationTerminatedEvent =
                new ApplicationTerminatedEvent(appId, clusterData);

        publishEvent(applicationTerminatedEvent);
    }

    public static void publishEvent(Event event) {
        //publishing events to application status topic
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(Constants.APPLICATION_STATUS_TOPIC);
        eventPublisher.publish(event);
    }

}

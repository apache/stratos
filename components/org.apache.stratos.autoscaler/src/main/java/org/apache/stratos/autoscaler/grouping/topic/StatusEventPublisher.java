package org.apache.stratos.autoscaler.grouping.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.application.status.*;
import org.apache.stratos.messaging.event.topology.ClusterCreatedEvent;
import org.apache.stratos.messaging.util.Constants;

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

        //TODO cluster
        ClusterCreatedEvent clusterActivatedEvent = new ClusterCreatedEvent(appId, serviceName, null);

        publishEvent(clusterActivatedEvent);
    }

    public static void sendClusterActivatedEvent(String appId, String serviceName, String clusterId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Cluster activated event for [application]: " + appId +
                    " [cluster]: " + clusterId);
        }

        ClusterActivatedEvent clusterActivatedEvent = new ClusterActivatedEvent(appId, serviceName, clusterId);

        publishEvent(clusterActivatedEvent);
    }

    public static void sendClusterInActivateEvent(String appId, String serviceName, String clusterId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Cluster in-activate event for [application]: " + appId +
                    " [cluster]: " + clusterId);
        }

        /*ClusterActivatedEvent clusterActivatedEvent = new ClusterActivatedEvent(appId, serviceName, clusterId);

        publishEvent(clusterActivatedEvent);*/
    }

    public static void sendClusterInMaintenanceEvent(String appId, String serviceName, String clusterId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Cluster in_maintenance event for [application]: " + appId +
                    " [cluster]: " + clusterId);
        }

        ClusterMaintenanceModeEvent clusterInMaintenanceEvent =
                new ClusterMaintenanceModeEvent(appId, serviceName, clusterId);

        publishEvent(clusterInMaintenanceEvent);
    }

    public static void sendGroupCreatedEvent(String appId, String groupId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Group activated event for [application]: " + appId +
                    " [group]: " + groupId);
        }

/*
        publishEvent(groupActivatedEvent);*/
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

        /*GroupActivatedEvent groupActivatedEvent = new GroupActivatedEvent(appId, groupId);

        publishEvent(groupActivatedEvent);*/
    }

    public static void sendApplicationActivatedEvent(String appId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Application activated event for [application]: " + appId);
        }

        ApplicationActivatedEvent applicationActivatedEvent = new ApplicationActivatedEvent(appId);

        publishEvent(applicationActivatedEvent);
    }

    public static void sendGroupInMaintenanceEvent(String appId, String groupId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Group in_maintenance event for [application]: " + appId +
                    " [group]: " + groupId);
        }

        GroupMaintenanceModeEvent groupMaintenanceModeEvent =
                new GroupMaintenanceModeEvent(appId, groupId);

        publishEvent(groupMaintenanceModeEvent);
    }

    public static void publishEvent(Event event) {
        //publishing events to application status topic
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(Constants.APPLICATION_STATUS_TOPIC);
        eventPublisher.publish(event);
    }

}

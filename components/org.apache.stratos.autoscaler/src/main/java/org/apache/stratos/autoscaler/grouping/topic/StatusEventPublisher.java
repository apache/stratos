package org.apache.stratos.autoscaler.grouping.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.application.status.ClusterActivatedEvent;
import org.apache.stratos.messaging.event.application.status.ClusterMaintenanceModeEvent;
import org.apache.stratos.messaging.event.application.status.GroupActivatedEvent;
import org.apache.stratos.messaging.event.application.status.GroupMaintenanceModeEvent;
import org.apache.stratos.messaging.util.Constants;

/**
 * Created by reka on 9/21/14.
 */
public class StatusEventPublisher {
    private static final Log log = LogFactory.getLog(StatusEventPublisher.class);

    public static void sendClusterActivatedEvent (String appId, String serviceName, String clusterId) {

        if(log.isInfoEnabled()) {
            log.info("Publishing Cluster activated event for [application]: " + appId +
                    " [cluster]: " + clusterId );
        }

        ClusterActivatedEvent clusterActivatedEvent = new ClusterActivatedEvent(appId,serviceName, clusterId);

        publishEvent(clusterActivatedEvent);
    }

    public static void sendClusterInMaintenanceEvent (String appId, String serviceName, String clusterId) {

        if(log.isInfoEnabled()) {
            log.info("Publishing Cluster in_maintenance event for [application]: " + appId +
                    " [cluster]: " + clusterId );
        }

        ClusterMaintenanceModeEvent clusterInMaintenanceEvent =
                                    new ClusterMaintenanceModeEvent(appId,serviceName, clusterId);

        publishEvent(clusterInMaintenanceEvent);
    }

    public static void sendGroupActivatedEvent (String appId, String groupId) {

        if(log.isInfoEnabled()) {
            log.info("Publishing Group activated event for [application]: " + appId +
                    " [group]: " + groupId );
        }

        GroupActivatedEvent groupActivatedEvent = new GroupActivatedEvent(appId, groupId);

        publishEvent(groupActivatedEvent);
    }

    public static void sendGroupInMaintenanceEvent (String appId, String groupId) {

        if(log.isInfoEnabled()) {
            log.info("Publishing Group in_maintenance event for [application]: " + appId +
                    " [group]: " + groupId );
        }

        GroupMaintenanceModeEvent groupMaintenanceModeEvent =
                new GroupMaintenanceModeEvent(appId, groupId);

        publishEvent(groupMaintenanceModeEvent);
    }

    public static void publishEvent(Event event) {
        //TODO change the topics for cluster and group accordingly
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(Constants.INSTANCE_STATUS_TOPIC);
        eventPublisher.publish(event);
    }

}

package org.apache.stratos.autoscaler.applications.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.Applications;
import org.apache.stratos.messaging.domain.applications.ClusterDataHolder;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.applications.*;
import org.apache.stratos.messaging.util.Util;

import java.util.Set;

/**
 * This will publish application related events to application status topic.
 */
public class ApplicationsEventPublisher {
    private static final Log log = LogFactory.getLog(ApplicationsEventPublisher.class);

    public static void sendCompleteApplicationsEvent (Applications completeApplications) {

        publishEvent(new CompleteApplicationsEvent(completeApplications));
    }

    public static void sendApplicationCreatedEvent (Application application) {

        publishEvent(new ApplicationCreatedEvent(application));
    }

    public static void sendGroupCreatedEvent(String appId, String groupId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Group created event for [application]: " + appId +
                    " [group]: " + groupId);
        }
        GroupResetEvent groupCreatedEvent =
                new GroupResetEvent(appId, groupId);

        publishEvent(groupCreatedEvent);
    }

    public static void sendGroupActivatedEvent(String appId, String groupId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Group activated event for [application]: " + appId +
                    " [group]: " + groupId);
        }
        GroupActivatedEvent groupActivatedEvent =
                new GroupActivatedEvent(appId, groupId);

        publishEvent(groupActivatedEvent);
    }

    public static void sendGroupInActivateEvent(String appId, String groupId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Group in-activate event for [application]: " + appId +
                    " [group]: " + groupId);
        }
        GroupInactivatedEvent groupInactivateEvent = new GroupInactivatedEvent(appId, groupId);

        publishEvent(groupInactivateEvent);
    }

    public static void sendGroupTerminatingEvent(String appId, String groupId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Group terminating event for [application]: " + appId +
                    " [group]: " + groupId);
        }
        GroupTerminatingEvent groupInTerminatingEvent =
                new GroupTerminatingEvent(appId, groupId);
        publishEvent(groupInTerminatingEvent);
    }

    public static void sendGroupTerminatedEvent(String appId, String groupId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Group terminated event for [application]: " + appId +
                    " [group]: " + groupId);
        }
        GroupTerminatedEvent groupInTerminatedEvent =
                new GroupTerminatedEvent(appId, groupId);
        publishEvent(groupInTerminatedEvent);
    }

    public static void sendApplicationActivatedEvent(String appId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Application activated event for [application]: " + appId);
        }
        ApplicationActivatedEvent applicationActivatedEvent =
                new ApplicationActivatedEvent(appId);

        publishEvent(applicationActivatedEvent);
    }

    public static void sendApplicationInactivatedEvent(String appId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Application In-activated event for [application]: " + appId);
        }
        ApplicationInactivatedEvent applicationInActivatedEvent =
                new ApplicationInactivatedEvent(appId);
        publishEvent(applicationInActivatedEvent);

    }

    public static void sendApplicationTerminatingEvent(String appId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Application terminating event for [application]: " + appId);
        }
        ApplicationTerminatingEvent applicationTerminatingEvent =
                new ApplicationTerminatingEvent(appId);
        publishEvent(applicationTerminatingEvent);
    }

    public static void sendApplicationTerminatedEvent(String appId, Set<ClusterDataHolder> clusterData) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Application terminated event for [application]: " + appId);
        }
        ApplicationTerminatedEvent applicationTerminatedEvent =
                new ApplicationTerminatedEvent(appId, clusterData);
        publishEvent(applicationTerminatedEvent);
    }

    public static void publishEvent(Event event) {
        //publishing events to application status topic
        String applicationTopic = Util.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(applicationTopic);
        eventPublisher.publish(event);
    }

}

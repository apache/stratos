package org.apache.stratos.autoscaler.applications.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.Applications;
import org.apache.stratos.messaging.domain.applications.ClusterDataHolder;
import org.apache.stratos.messaging.domain.instance.ApplicationInstance;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
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

    public static void sendApplicationInstanceCreatedEvent(String appId,
                                                           ApplicationInstance applicationInstance) {

        publishEvent(new ApplicationInstanceCreatedEvent(appId, applicationInstance));
    }
    public static void sendGroupInstanceCreatedEvent(String appId, String groupId,
                                                     GroupInstance groupInstance) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Group created event for [application]: " + appId +
                    " [group]: " + groupId);
        }
        GroupInstanceCreatedEvent groupCreatedEvent =
                new GroupInstanceCreatedEvent(appId, groupId, groupInstance);

        publishEvent(groupCreatedEvent);
    }

    public static void sendGroupInstanceActivatedEvent(String appId, String groupId, String instanceId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Group activated event for [application]: " + appId +
                    " [group]: " + groupId);
        }
        GroupInstanceActivatedEvent groupActivatedEvent =
                new GroupInstanceActivatedEvent(appId, groupId, instanceId);

        publishEvent(groupActivatedEvent);
    }

    public static void sendGroupInstanceInActivateEvent(String appId, String groupId, String instanceId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Group inactivate event for [application]: " + appId +
                    " [group]: " + groupId);
        }
        GroupInstanceInactivatedEvent groupInactivateEvent = new GroupInstanceInactivatedEvent(appId, groupId, instanceId);

        publishEvent(groupInactivateEvent);
    }

    public static void sendGroupInstanceTerminatingEvent(String appId, String groupId, String instanceId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Group terminating event for [application]: " + appId +
                    " [group]: " + groupId);
        }
        GroupInstanceTerminatingEvent groupInTerminatingEvent =
                new GroupInstanceTerminatingEvent(appId, groupId, instanceId);
        publishEvent(groupInTerminatingEvent);
    }

    public static void sendGroupInstanceTerminatedEvent(String appId, String groupId, String instanceId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Group terminated event for [application]: " + appId +
                    " [group]: " + groupId);
        }
        GroupInstanceTerminatedEvent groupInTerminatedEvent =
                new GroupInstanceTerminatedEvent(appId, groupId, instanceId);
        publishEvent(groupInTerminatedEvent);
    }

    public static void sendApplicationInstanceActivatedEvent(String appId, String instanceId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Application Active event for [application]: " + appId);
        }
        ApplicationInstanceActivatedEvent applicationActivatedEvent =
                new ApplicationInstanceActivatedEvent(appId, instanceId);

        publishEvent(applicationActivatedEvent);
    }

    public static void sendApplicationInstanceInactivatedEvent(String appId, String instanceId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Application Inactivated event for [application]: " + appId);
        }
        ApplicationInstanceInactivatedEvent applicationInActivatedEvent =
                new ApplicationInstanceInactivatedEvent(appId, instanceId);
        publishEvent(applicationInActivatedEvent);

    }

    public static void sendApplicationInstanceTerminatingEvent(String appId, String instanceId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Application terminating event for [application]: " + appId);
        }
        ApplicationInstanceTerminatingEvent applicationTerminatingEvent =
                new ApplicationInstanceTerminatingEvent(appId, instanceId);
        publishEvent(applicationTerminatingEvent);
    }

    public static void sendApplicationInstanceTerminatedEvent(String appId, String instanceId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Application terminated event for [application]: " + appId);
        }
        ApplicationInstanceTerminatedEvent applicationTerminatedEvent =
                new ApplicationInstanceTerminatedEvent(appId, instanceId);
        publishEvent(applicationTerminatedEvent);
    }

    public static void publishEvent(Event event) {
        //publishing events to application status topic
        String applicationTopic = Util.getMessageTopicName(event);
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(applicationTopic);
        eventPublisher.publish(event);
    }

}

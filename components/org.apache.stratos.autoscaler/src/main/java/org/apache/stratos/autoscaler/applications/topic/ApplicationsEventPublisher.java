package org.apache.stratos.autoscaler.applications.topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.applications.*;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.applications.*;
import org.apache.stratos.messaging.event.topology.ApplicationUndeployedEvent;
import org.apache.stratos.messaging.message.receiver.applications.ApplicationManager;
import org.apache.stratos.messaging.util.Constants;

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

    public static void sendApplicationUndeployedEvent (String appId, Set<ClusterDataHolder> clusterData) {

        publishEvent(new ApplicationUndeployedEvent(appId, clusterData));
    }

    public static void sendGroupCreatedEvent(String appId, String groupId) {
        try {
            ApplicationManager.acquireReadLockForApplication(appId);
            Application application = ApplicationManager.getApplications().getApplication(appId);
            if (application != null) {
                Group group = application.getGroupRecursively(groupId);
                if (group.isStateTransitionValid(GroupStatus.Created)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Group created event for [application]: " + appId +
                                " [group]: " + groupId);
                    }
                    GroupResetEvent groupCreatedEvent =
                            new GroupResetEvent(appId, groupId);

                    publishEvent(groupCreatedEvent);
                } else {
                    log.warn("Created is not in the possible state list of [group] " + groupId);
                }
            }
        } finally {
            ApplicationManager.releaseReadLockForApplication(appId);
        }
    }

    public static void sendGroupActivatedEvent(String appId, String groupId) {
        try {
            ApplicationManager.acquireReadLockForApplication(appId);
            Application application = ApplicationManager.getApplications().getApplication(appId);
            if (application != null) {
                Group group = application.getGroupRecursively(groupId);
                if (group.isStateTransitionValid(GroupStatus.Active)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Group activated event for [application]: " + appId +
                                " [group]: " + groupId);
                    }
                    GroupActivatedEvent groupActivatedEvent =
                            new GroupActivatedEvent(appId, groupId);

                    publishEvent(groupActivatedEvent);
                } else {
                    log.warn("Active is not in the possible state list of [group] " + groupId);
                }
            }
        } finally {
            ApplicationManager.releaseReadLockForApplication(appId);
        }
    }

    public static void sendGroupInActivateEvent(String appId, String groupId) {
        try {
            ApplicationManager.acquireReadLockForApplication(appId);
            Application application = ApplicationManager.getApplications().getApplication(appId);
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
            ApplicationManager.releaseReadLockForApplication(appId);
        }
    }

    public static void sendGroupTerminatingEvent(String appId, String groupId) {
        try {
            ApplicationManager.acquireReadLockForApplication(appId);
            Application application = ApplicationManager.getApplications().getApplication(appId);
            if (application != null) {
                Group group = application.getGroupRecursively(groupId);
                if (group.isStateTransitionValid(GroupStatus.Terminating)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Group terminating event for [application]: " + appId +
                                " [group]: " + groupId);
                    }
                    GroupTerminatingEvent groupInTerminatingEvent =
                            new GroupTerminatingEvent(appId, groupId);
                    publishEvent(groupInTerminatingEvent);
                } else {
                    log.warn("Terminating is not in the possible state list of [group] " + groupId);
                }
            }
        } finally {
            ApplicationManager.releaseReadLockForApplication(appId);
        }
    }

    public static void sendGroupTerminatedEvent(String appId, String groupId) {

        if (log.isInfoEnabled()) {
            log.info("Publishing Group terminated event for [application]: " + appId +
                    " [group]: " + groupId);
        }

        try {
            ApplicationManager.acquireReadLockForApplication(appId);
            Application application = ApplicationManager.getApplications().getApplication(appId);
            if (application != null) {
                Group group = application.getGroupRecursively(groupId);
                if (group.isStateTransitionValid(GroupStatus.Terminated)) {
                    GroupTerminatedEvent groupInTerminatedEvent =
                            new GroupTerminatedEvent(appId, groupId);
                    publishEvent(groupInTerminatedEvent);
                } else {
                    log.warn("Terminated is not in the possible state list of [group] " + groupId);
                }
            }
        } finally {
            ApplicationManager.releaseReadLockForApplication(appId);
        }


    }

    public static void sendApplicationActivatedEvent(String appId) {
        try {
            ApplicationManager.acquireReadLockForApplication(appId);
            Application application = ApplicationManager.getApplications().getApplication(appId);
            if (application != null) {
                if (application.isStateTransitionValid(ApplicationStatus.Active)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Application activated event for [application]: " + appId);
                    }
                    ApplicationActivatedEvent applicationActivatedEvent =
                            new ApplicationActivatedEvent(appId);

                    publishEvent(applicationActivatedEvent);
                } else {
                    log.warn("Active is not in the possible state list of [application] " + appId);
                }
            }
        } finally {
            ApplicationManager.releaseReadLockForApplication(appId);
        }
    }

    public static void sendApplicationInactivatedEvent(String appId) {
        if (log.isInfoEnabled()) {
            log.info("Publishing Application In-activated event for [application]: " + appId);
        }

        try {
            ApplicationManager.acquireReadLockForApplication(appId);
            Application application = ApplicationManager.getApplications().getApplication(appId);
            if (application != null) {
                if (application.isStateTransitionValid(ApplicationStatus.Inactive)) {
                    ApplicationInactivatedEvent applicationInActivatedEvent =
                            new ApplicationInactivatedEvent(appId);
                    publishEvent(applicationInActivatedEvent);
                } else {
                    log.warn("Inactive is not in the possible state list of [application] " + appId);
                }
            }
        } finally {
            ApplicationManager.releaseReadLockForApplication(appId);
        }
    }

    public static void sendApplicationTerminatingEvent(String appId) {
        try {
            ApplicationManager.acquireReadLockForApplication(appId);
            Application application = ApplicationManager.getApplications().getApplication(appId);
            if (application != null) {
                if (application.isStateTransitionValid(ApplicationStatus.Terminating)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Application terminated event for [application]: " + appId);
                    }
                    ApplicationTerminatingEvent applicationTerminatingEvent =
                            new ApplicationTerminatingEvent(appId);
                    publishEvent(applicationTerminatingEvent);
                } else {
                    log.warn("Terminating is not in the possible state list of [application] " + appId);
                }
            }
        } finally {
            ApplicationManager.releaseReadLockForApplication(appId);
        }
    }

    public static void sendApplicationTerminatedEvent(String appId, Set<ClusterDataHolder> clusterData) {
        try {
            ApplicationManager.acquireReadLockForApplication(appId);
            Application application = ApplicationManager.getApplications().getApplication(appId);
            if (application != null) {
                if (application.isStateTransitionValid(ApplicationStatus.Terminated)) {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Application terminated event for [application]: " + appId);
                    }
                    ApplicationTerminatedEvent applicationTerminatedEvent =
                            new ApplicationTerminatedEvent(appId, clusterData);
                    publishEvent(applicationTerminatedEvent);
                } else {
                    log.warn("Terminated is not in the possible state list of [application] " + appId);
                }
            }
        } finally {
            ApplicationManager.releaseReadLockForApplication(appId);
        }
    }

    public static void publishEvent(Event event) {
        //publishing events to application status topic
        EventPublisher eventPublisher = EventPublisherPool.getPublisher(Constants.APPLICATIONS_TOPIC);
        eventPublisher.publish(event);
    }

}

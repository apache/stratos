package org.apache.stratos.autoscaler.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.client.topology.TopologyEventMessageReceiver;
import org.apache.stratos.messaging.broker.subscribe.TopicSubscriber;
import org.apache.stratos.messaging.util.Constants;

/**
 * This class will initiate all the subscriber to topics
 */
public class TopicSubscriberManager {
    private static final Log log = LogFactory.getLog(TopicSubscriberManager.class);

    public void subscribeAllTopics(){
        TopicSubscriber topologyTopicSubscriber = new TopicSubscriber(Constants.TOPOLOGY_TOPIC);
        topologyTopicSubscriber.setMessageListener(new TopologyEventMessageReceiver());
        Thread topologyTopicSubscriberThread = new Thread(topologyTopicSubscriber);
        topologyTopicSubscriberThread.start();

        if (log.isDebugEnabled()) {
           log.debug("Topology event message receiver thread started");
        }

        TopicSubscriber healthStatTopicSubscriber = new TopicSubscriber(Constants.HEALTH_STAT_TOPIC);
        healthStatTopicSubscriber.setMessageListener(new TopologyEventMessageReceiver());
        Thread healthStatTopicSubscriberThread = new Thread(healthStatTopicSubscriber);
        healthStatTopicSubscriberThread.start();

        if (log.isDebugEnabled()) {
           log.debug("Health Stat event message receiver thread started");
        }

    }
}

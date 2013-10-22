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
        TopicSubscriber topicSubscriber = new TopicSubscriber(Constants.TOPOLOGY_TOPIC);
        topicSubscriber.setMessageListener(new TopologyEventMessageReceiver());
        Thread subscriberThread = new Thread(topicSubscriber);
        log.info("777777777777777777777777777777777777777777");
        subscriberThread.start();

        if (log.isDebugEnabled()) {
           log.debug("Topology event message receiver thread started");
        }

    }
}

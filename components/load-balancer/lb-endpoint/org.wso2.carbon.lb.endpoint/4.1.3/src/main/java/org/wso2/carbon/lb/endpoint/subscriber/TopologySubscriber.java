package org.wso2.carbon.lb.endpoint.subscriber;

import java.util.Properties;

import javax.jms.*;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.lb.endpoint.util.ConfigHolder;
import org.wso2.carbon.lb.endpoint.util.TopologyConstants;

public class TopologySubscriber {

	private static final Log log = LogFactory.getLog(TopologySubscriber.class);
	
    public static void subscribe(String topicName) {
        Properties initialContextProperties = new Properties();
        TopicSubscriber topicSubscriber = null;
        TopicSession topicSession = null;
        TopicConnection topicConnection = null;
        InitialContext initialContext = null;

        initialContextProperties.put("java.naming.factory.initial",
            "org.wso2.andes.jndi.PropertiesFileInitialContextFactory");

        String mbServerUrl = null;
        if (ConfigHolder.getInstance().getLbConfig() != null) {
            mbServerUrl = ConfigHolder.getInstance().getLbConfig().getLoadBalancerConfig().getMbServerUrl();
        }
        String connectionString =
            "amqp://admin:admin@clientID/carbon?brokerlist='tcp://" +
                (mbServerUrl == null ? TopologyConstants.DEFAULT_MB_SERVER_URL : mbServerUrl) + "'&reconnect='true'";
        initialContextProperties.put("connectionfactory.qpidConnectionfactory", connectionString);

        try {
            initialContext = new InitialContext(initialContextProperties);
            TopicConnectionFactory topicConnectionFactory =
                (TopicConnectionFactory) initialContext.lookup("qpidConnectionfactory");
            topicConnection = topicConnectionFactory.createTopicConnection();
            topicConnection.start();
            topicSession =
                topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            Topic topic = topicSession.createTopic(topicName);
            topicSubscriber =
                topicSession.createSubscriber(topic);

            topicSubscriber.setMessageListener(new TopologyListener());

        } catch (Exception e) {
            log.error(e.getMessage(), e);

            try {
                if (topicSubscriber != null) {
                    topicSubscriber.close();
                }

                if (topicSession != null) {
                    topicSession.close();
                }

                if (topicConnection != null) {
                    topicConnection.close();
                }
            } catch (JMSException e1) {
                // ignore
            }

        } 
        finally {
            // start the health checker
            Thread healthChecker = new Thread(new TopicHealthChecker(topicName, topicSubscriber));
            healthChecker.start();
        }
    }

}

package org.wso2.carbon.stratos.cloud.controller.topic;

import java.util.Properties;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.cloud.controller.runtime.FasterLookUpDataHolder;

public class ConfigurationPublisher {
	private TopicPublisher topicPublisher;
	private TopicSession topicSession;
	private TopicConnection topicConnection;
	private TopicConnectionFactory topicConnectionFactory;
	private static final Log log = LogFactory.getLog(ConfigurationPublisher.class);
	
	
	public ConfigurationPublisher() {
	    
		Properties initialContextProperties = new Properties();
		initialContextProperties.put("java.naming.factory.initial",
				"org.wso2.andes.jndi.PropertiesFileInitialContextFactory");
		String connectionString = "amqp://admin:admin@clientID/carbon?brokerlist='tcp://"+FasterLookUpDataHolder.getInstance().getMBServerUrl()+"'";
		initialContextProperties.put("connectionfactory.qpidConnectionfactory", connectionString);
		
		try {
			InitialContext initialContext = new InitialContext(initialContextProperties);
			topicConnectionFactory =
					(TopicConnectionFactory) initialContext.lookup("qpidConnectionfactory");
			
//			topicConnection.stop();
//			topicConnection.close();
			
		} catch (NamingException e) {
			log.error(e.getMessage(), e);
		} 
    }

	
	public void publish(String topicName, String message) {
		try {
			topicConnection = topicConnectionFactory.createTopicConnection();
			topicConnection.start();
			topicSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

			Topic topic = topicSession.createTopic(topicName);
			topicPublisher = topicSession.createPublisher(topic);
			TextMessage textMessage = topicSession.createTextMessage(message);

			topicPublisher.publish(textMessage);
			
			topicPublisher.close();
			topicSession.close();
			topicConnection.stop();
			topicConnection.close();
			
		}  catch (JMSException e) {
			log.error(e.getMessage(), e);
		}
	}

}

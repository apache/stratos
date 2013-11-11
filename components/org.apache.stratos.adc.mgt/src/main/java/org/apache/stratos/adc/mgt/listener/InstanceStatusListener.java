/**
 * 
 */
package org.apache.stratos.adc.mgt.listener;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.publisher.ArtifactUpdatePublisher;
import org.apache.stratos.adc.mgt.utils.PersistenceManager;
import org.apache.stratos.messaging.event.instance.status.MemberStartedEvent;
import org.apache.stratos.messaging.util.Constants;
import org.apache.stratos.messaging.util.Util;

/**
 * @author wso2
 * 
 */
public class InstanceStatusListener implements MessageListener {

	private static final Log log = LogFactory
			.getLog(InstanceStatusListener.class);

	@Override
	public void onMessage(Message message) {
		TextMessage receivedMessage = (TextMessage) message;
		String clusterId = null;

		log.info(" --- instance status message received --- ");
		try {
			String type = message.getStringProperty(Constants.EVENT_CLASS_NAME);
			// If member started event is received publish artifact update message
			if (MemberStartedEvent.class.getName().equals(type)) {
				String json = receivedMessage.getText();
				MemberStartedEvent event = (MemberStartedEvent) Util
						.jsonToObject(json, MemberStartedEvent.class);
				clusterId = event.getClusterId();
				log.info("--- cluster id is --- : " + clusterId);
				new ArtifactUpdatePublisher(
						PersistenceManager.getRepository(clusterId), clusterId)
						.publish();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

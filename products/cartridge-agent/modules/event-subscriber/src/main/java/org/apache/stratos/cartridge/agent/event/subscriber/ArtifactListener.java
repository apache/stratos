/**
 * 
 */
package org.apache.stratos.cartridge.agent.event.subscriber;

import java.io.File;
import java.util.Scanner;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.artifact.synchronization.ArtifactUpdatedEvent;
import org.apache.stratos.messaging.util.Util;

/**
 * @author wso2
 *
 */
public class ArtifactListener implements MessageListener{
	
	 private static final Log log = LogFactory.getLog(ArtifactListener.class);
	 private String script = "/opt/apache-stratos-cartridge-agent/git.sh";
	 private String launchParams = "/opt/apache-stratos-cartridge-agent/payload/launch-params";
	
	@Override
	public void onMessage(Message message) {
		
		// If cluster id of the node is equal to that of message's, invoke the script
		TextMessage receivedMessage = (TextMessage) message;
		log.info(" Message received on artifact update ");
		String json = null;
		try {
			json = receivedMessage.getText();
		} catch (Exception e) {
			//e.printStackTrace();
			log.error("Exception is occurred " + e.getMessage(), e);
		}
		
       // if(ArtifactUpdatedEvent.class.getName().equals(type)) {
		ArtifactUpdatedEvent event = (ArtifactUpdatedEvent) Util.jsonToObject(json, ArtifactUpdatedEvent.class);
		String clusterIdInPayload = readClusterIdFromPayload();
		String clusterIdInMessage = event.getClusterId();		
		String repoURL = event.getRepoURL();
		String repoPassword = event.getRepoPassword();
		String repoUsername = event.getRepoUserName();
				
		// execute script
		if(clusterIdInPayload != null && clusterIdInPayload.equals(clusterIdInMessage)) {
			
			try {
			String command = script + " " + repoUsername+ " " +repoPassword+ " "+repoURL+ " /";
			log.info("Executing command " + command);
			Process proc = Runtime.getRuntime().exec(command);
			proc.waitFor();
			} catch (Exception e) {
				//e.printStackTrace();
				log.error("Exception is occurred in executing script. " + e.getMessage(), e);
			}
		}
		
	}

	private String readClusterIdFromPayload() {
		String clusterId = null;
		// read launch params
		File file = new File(launchParams);

		try {
			Scanner scanner = new Scanner(file);

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] params = line.split(",");
				for (String string : params) {
					 String[] var = string.split("=");
					if("CLUSTER_ID".equals(var[0])){
						clusterId = var[1];
					}
				}
			}
			scanner.close();
		} catch (Exception e) {
			//e.printStackTrace();
			log.error("Exception is occurred in reading file. ", e);
		}
		
		return clusterId;
	}

}

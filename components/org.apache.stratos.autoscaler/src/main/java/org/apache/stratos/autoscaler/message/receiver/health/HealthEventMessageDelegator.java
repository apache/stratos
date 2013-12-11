/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.autoscaler.message.receiver.health;

import com.google.gson.stream.JsonReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.ClusterMonitor;
import org.apache.stratos.autoscaler.Constants;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.exception.SpawningException;
import org.apache.stratos.autoscaler.exception.TerminationException;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;

import javax.jms.TextMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;


/**
 * A thread for processing topology messages and updating the topology data structure.
 */
public class HealthEventMessageDelegator implements Runnable {

    private static final Log log = LogFactory.getLog(HealthEventMessageDelegator.class);
    private String eventName;
    private String clusterId;
    private String networkPartitionId;
    private Map<String, String> messageProperties;
    @Override
    public void run() {
		log.info("Health stat event message processor started");

        while (true) {
			try {
				TextMessage message = HealthEventQueue.getInstance().take();

				String messageText = message.getText();
				if(log.isDebugEnabled())
					log.debug("Health event message received. Message :" + messageText);

                messageProperties = setEventValues(messageText);
                this.clusterId = messageProperties.get("cluster_id");
                log.info("Received event " + eventName + " for cluster " + this.clusterId);

                if(Constants.AVERAGE_REQUESTS_IN_FLIGHT.equals(eventName)){                	
                	Float messageValue = Float.parseFloat(messageProperties.get("value"));
                    AutoscalerContext.getInstance().getMonitor(clusterId).getNetworkPartitionCtxt(networkPartitionId)
                            .setAverageRequestsInFlight(messageValue);

                }  else if(Constants.GRADIENT_OF_REQUESTS_IN_FLIGHT.equals(eventName)){                	
                	Float messageValue = Float.parseFloat(messageProperties.get("value"));
                    AutoscalerContext.getInstance().getMonitor(clusterId).getNetworkPartitionCtxt(networkPartitionId)
                            .setRequestsInFlightGradient(messageValue);

                }  else if(Constants.SECOND_DERIVATIVE_OF_REQUESTS_IN_FLIGHT.equals(eventName)){
                	Float messageValue = Float.parseFloat(messageProperties.get("value"));
                    AutoscalerContext.getInstance().getMonitor(clusterId).getNetworkPartitionCtxt(networkPartitionId)
                            .setRequestsInFlightSecondDerivative(messageValue);

                }else if (Constants.MEMBER_FAULT_EVENT_NAME.equals(eventName)){
                	
                	String memberId = messageProperties.get("member_id");
                	if(memberId == null || memberId.isEmpty())
                		log.error("MemberId is not included in the received message");
                	handleMemberfaultEvent(memberId);                	
                }
                
                // clear the message properties after handling the message.
                messageProperties.clear();
                
			} catch (Exception e) {
                String error = "Failed to retrieve the health stat event message." + e.getMessage();
            	log.error(error );
            }
        }
    }

    private void handleMemberfaultEvent(String memberId) {
		try {	
			
			ClusterMonitor monitor = AutoscalerContext.getInstance().getMonitor(this.clusterId);
//            TopologyManager.getTopology().get
			if(!monitor.memberExist(memberId)){
				// member has already terminated. So no action required
				return;
			}
				
			// terminate the faulty member
			CloudControllerClient ccClient = CloudControllerClient.getInstance();
			ccClient.terminate(memberId);
			
			// start a new member in the same Partition
			//ClusterContext clsCtx = AutoscalerContext.getInstance().getClusterMonitor(clusterId);
			String partitionId = monitor.getPartitonOfMember(memberId);
			Partition partition = monitor.getDeploymentPolicy().getPartitionById(partitionId);
			ccClient.spawnAnInstance(partition, clusterId);
			
		} catch (TerminationException e) {
			log.error(e);
		}catch(SpawningException e){
			log.error(e);
		}		
	}

	public Map<String, String> setEventValues(String json) {
    	
    	Map<String, String> properties = new HashMap<String, String>();
    	BufferedReader bufferedReader = new BufferedReader(new StringReader(json));
        JsonReader reader = new JsonReader(bufferedReader);
        try {

            reader.beginObject();

            if(reader.hasNext()) {
                eventName = reader.nextName();
                
                reader.beginObject();                
                while(reader.hasNext()){
                	String name = reader.nextName();
                	String value = reader.nextString();
                	properties.put(name, value);
                }
                               
            }
            reader.close(); 
            return properties;
        }catch(IOException e) {
            log.error( "Could not extract message header");
//            throw new RuntimeException("Could not extract message header", e);
        }
		return null;
    }
    
}

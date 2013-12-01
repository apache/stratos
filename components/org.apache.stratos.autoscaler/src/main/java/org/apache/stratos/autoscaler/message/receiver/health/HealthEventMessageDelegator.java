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
import org.apache.stratos.autoscaler.Constants;

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
    private Map<String, String> messageProperties;
    @Override
    public void run() {
		log.info("Health stat event message processor started");

        while (true) {
			try {
				TextMessage message = HealthEventQueue.getInstance().take();

				String messageText = message.getText();

                messageProperties = setEventValues(messageText);
                log.info("Received event " + eventName);
//                for (Service service :  TopologyManager.getTopology().getServices()){
//
//                    if(service.clusterExists(clusterId)){
//
//                        if(!AutoscalerContext.getInstance().clusterExists(clusterId)){
//
//                            Cluster cluster = service.getCluster(clusterId);
//                            AutoscalePolicy autoscalePolicy = PolicyManager.getInstance().getAutoscalePolicy(cluster.getAutoscalePolicyName());
//                            DeploymentPolicy deploymentPolicy = PolicyManager.getInstance().getDeploymentPolicy(cluster.getDeploymentPolicyName());
//
//                            ClusterContext clusterContext = new ClusterContext(clusterId, service.getServiceName(), deploymentPolicy.getAllPartitions());
//
//                            LoadThresholds loadThresholds = autoscalePolicy.getLoadThresholds();
//                            float averageLimit = loadThresholds.getRequestsInFlight().getAverage();
//                            float gradientLimit = loadThresholds.getRequestsInFlight().getGradient();
//                            float secondDerivative  = loadThresholds.getRequestsInFlight().getSecondDerivative();
//
//                            clusterContext.setAverageRequestsInFlight(averageLimit);
//                            clusterContext.setRequestsInFlightGradient(gradientLimit);
//                            clusterContext.setRequestsInFlightSecondDerivative(secondDerivative);
//
//                            AutoscalerContext.getInstance().addClusterContext(clusterContext);
//                        }
//                        break;
//                    }
//                }
                if(Constants.AVERAGE_REQUESTS_IN_FLIGHT.equals(eventName)){
                	String clusterId = messageProperties.get("cluster_id");
                	Float messageValue = Float.parseFloat(messageProperties.get("value"));
                    AutoscalerContext.getInstance().getClusterContext(clusterId).setAverageRequestsInFlight(messageValue);

                }  else if(Constants.GRADIENT_OF_REQUESTS_IN_FLIGHT.equals(eventName)){
                	String clusterId = messageProperties.get("cluster_id");
                	Float messageValue = Float.parseFloat(messageProperties.get("value"));
                    AutoscalerContext.getInstance().getClusterContext(clusterId).setRequestsInFlightGradient(messageValue);

                }  else if(Constants.SECOND_DERIVATIVE_OF_REQUESTS_IN_FLIGHT.equals(eventName)){
                	String clusterId = messageProperties.get("cluster_id");
                	Float messageValue = Float.parseFloat(messageProperties.get("value"));
                    AutoscalerContext.getInstance().getClusterContext(clusterId).setRequestsInFlightSecondDerivative(messageValue);

                }else if ("member_fault".equals(eventName)){
                	// member with 
                }
                
                // clear the message properties after handling the message.
                messageProperties.clear();
                
			} catch (Exception e) {
                String error = "Failed to retrieve the health stat event message.";
            	log.error(error);
            }
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

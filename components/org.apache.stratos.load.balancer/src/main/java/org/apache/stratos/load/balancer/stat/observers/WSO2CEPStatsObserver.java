/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.load.balancer.stat.observers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.agent.thrift.Agent;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;
import org.wso2.carbon.databridge.agent.thrift.conf.AgentConfiguration;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.utils.CarbonUtils;

public class WSO2CEPStatsObserver implements Observer{

	private static final Log log = LogFactory.getLog(WSO2CEPStatsObserver.class);
	private static final String CALL_CENTER_DATA_STREAM = "stratos.lb.stats";
	private static final String VERSION = "1.0.0";
	private AsyncDataPublisher asyncDataPublisher;
	
	public WSO2CEPStatsObserver() {
		AgentConfiguration agentConfiguration = new AgentConfiguration();
        // TODO get following from somewhere, without hard-coding.
        System.setProperty("javax.net.ssl.trustStore", CarbonUtils.getCarbonHome()+File.separator+"repository"+
                           File.separator+"resources"+File.separator+"security"+File.separator+"client-truststore.jks"	);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        
        Agent agent = new Agent(agentConfiguration);
        //TODO read following from a config file?
        String ip = System.getProperty("thrift.receiver.ip");
        String port = System.getProperty("thrift.receiver.port");
        //Using Asynchronous data publisher
        asyncDataPublisher = new AsyncDataPublisher("tcp://"+ip+":"+port+"", "admin", "admin", agent);
        String streamDefinition = "{" +
                                  " 'name':'" + CALL_CENTER_DATA_STREAM + "'," +
                                  " 'version':'" + VERSION + "'," +
                                  " 'nickName': 'lb stats'," +
                                  " 'description': 'lb stats'," +
                                  " 'metaData':[]," +
                                  " 'payloadData':[" +
                                  " {'name':'cluster_id','type':'STRING'}," +
                                  " {'name':'in_flight_requests','type':'INT'}" +
                                  " ]" +
                                  "}";
        asyncDataPublisher.addStreamDefinition(streamDefinition, CALL_CENTER_DATA_STREAM, VERSION);
    }
	
	public void update(Observable arg0, Object arg1) {
		if(arg1 != null && arg1 instanceof Map<?, ?>) {
			Map<String, Integer> stats = (Map<String, Integer>)arg1;
			publishEvents(stats);
		}
	}
	
    private void publishEvents(Map<String, Integer> stats) {
    	
    	for (Map.Entry<String, Integer> entry : stats.entrySet()) {
    		
    		Object[] payload = new Object[]{entry.getKey(), entry.getValue()};
    		Event event = eventObject(null, null, payload, new HashMap<String, String>());
    		try {
    			asyncDataPublisher.publish(CALL_CENTER_DATA_STREAM, VERSION, event);
    		} catch (AgentException e) {
    			log.error("Failed to publish events. ", e);
    		}
    		
    	}
    	stats = null;
    }
    
    private static Event eventObject(Object[] correlationData, Object[] metaData,
                                     Object[] payLoadData, HashMap<String, String> map) {
        Event event = new Event();
        event.setCorrelationData(correlationData);
        event.setMetaData(metaData);
        event.setPayloadData(payLoadData);
        event.setArbitraryDataMap(map);
        return event;
    }
}

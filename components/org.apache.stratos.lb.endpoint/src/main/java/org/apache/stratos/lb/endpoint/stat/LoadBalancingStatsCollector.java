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
package org.apache.stratos.lb.endpoint.stat;

import java.util.Map;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.stratos.lb.endpoint.stat.observers.WSO2CEPStatsObserver;

/**
 * This is the load balancing stats collector and any observer can get registered here 
 * and receive notifications periodically.
 * This is a Singleton object.
 * @author nirmal
 *
 */
public class LoadBalancingStatsCollector extends Observable{

	private static LoadBalancingStatsCollector collector;
	private Map<String, Integer> clusterIdToRequestInflightCountMap;
	private Thread notifier;
	
	private LoadBalancingStatsCollector() {
		clusterIdToRequestInflightCountMap = new ConcurrentHashMap<String, Integer>();
		if (notifier == null || (notifier != null && !notifier.isAlive())) {
			notifier = new Thread(new ObserverNotifier());
			notifier.start();
		}
    }
	
	public static LoadBalancingStatsCollector getInstance() {
		if (collector == null) {
			synchronized (LoadBalancingStatsCollector.class) {
				if (collector == null) {
					collector = new LoadBalancingStatsCollector();
					// add observers
					collector.addObserver(new WSO2CEPStatsObserver());
				}
			}
		}
		return collector;
	}
	
	public void incrementRequestInflightCount(String clusterId) {
		if(clusterId == null) {
			return;
		}
		
		int value = 1;
		if(clusterIdToRequestInflightCountMap.get(clusterId) != null) {
			value += clusterIdToRequestInflightCountMap.get(clusterId);
		}
		clusterIdToRequestInflightCountMap.put(clusterId, value);
		setChanged();
	}
	
	public void decrementRequestInflightCount(String clusterId) {
		if(clusterId == null) {
			return;
		}
		
		int value = -1;
		if(clusterIdToRequestInflightCountMap.get(clusterId) != null) {
			value += clusterIdToRequestInflightCountMap.get(clusterId);
		}
		clusterIdToRequestInflightCountMap.put(clusterId, value);
		setChanged();
	}
	
	
	/**
	 * This thread will notify all the observers of this subject.
	 * @author nirmal
	 *
	 */
	private class ObserverNotifier implements Runnable {

		@Override
        public void run() {
			while(true) {
				try {
	                Thread.sleep(15000);
                } catch (InterruptedException ignore) {
                }
				LoadBalancingStatsCollector.getInstance().notifyObservers(clusterIdToRequestInflightCountMap);
			}
	        
        }
		
	}
	
}

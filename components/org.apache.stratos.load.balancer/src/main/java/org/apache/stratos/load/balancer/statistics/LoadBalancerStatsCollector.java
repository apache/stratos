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
package org.apache.stratos.load.balancer.statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.statistics.observers.WSO2CEPStatsObserver;

/**
 * This is the load balancing stats collector and any observer can get registered here 
 * and receive notifications periodically.
 * This is a Singleton object.
 * @author nirmal
 *
 */
public class LoadBalancerStatsCollector extends Observable{
    private static final Log log = LogFactory.getLog(LoadBalancerStatsCollector.class);

	private static LoadBalancerStatsCollector collector;
	private Map<String, Integer> clusterIdToRequestInflightCountMap;
	private Thread notifier;
	
	private LoadBalancerStatsCollector() {
		clusterIdToRequestInflightCountMap = new ConcurrentHashMap<String, Integer>();
		if (notifier == null || (notifier != null && !notifier.isAlive())) {
			notifier = new Thread(new ObserverNotifier());
			notifier.start();
		}
    }
	
	public static LoadBalancerStatsCollector getInstance() {
		if (collector == null) {
			synchronized (LoadBalancerStatsCollector.class) {
				if (collector == null) {
					collector = new LoadBalancerStatsCollector();
					// add observers
					collector.addObserver(new WSO2CEPStatsObserver());
				}
			}
		}
		return collector;
	}

    public void setRequestInflightCount(String clusterId, int value) {
        if(clusterId == null) {
            return;
        }

        clusterIdToRequestInflightCountMap.put(clusterId, value);
        setChanged();
    }

    public void incrementRequestInflightCount(String clusterId) {
        incrementRequestInflightCount(clusterId, 1);
    }
	
	public void incrementRequestInflightCount(String clusterId, int value) {
		if(clusterId == null) {
			return;
		}

		if(clusterIdToRequestInflightCountMap.get(clusterId) != null) {
			value += clusterIdToRequestInflightCountMap.get(clusterId);
		}
		clusterIdToRequestInflightCountMap.put(clusterId, value);
		setChanged();
	}

    public void decrementRequestInflightCount(String clusterId) {
        decrementRequestInflightCount(clusterId , 1);
    }
	
	public void decrementRequestInflightCount(String clusterId, int value) {
		if(clusterId == null) {
			return;
		}

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
            if(log.isInfoEnabled()) {
                log.info("Load balancing statistics notifier thread started");
            }
			while(true) {
				try {
	                Thread.sleep(15000);
                } catch (InterruptedException ignore) {
                }
				LoadBalancerStatsCollector.getInstance().notifyObservers(new HashMap<String, Integer>(clusterIdToRequestInflightCountMap));
			}
        }
	}
}

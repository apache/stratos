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

package org.apache.stratos.manager.topology.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.exception.PersistenceManagerException;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.messaging.domain.topology.Cluster;

public class TopologyClusterInformationModel {

    private static final Log log = LogFactory.getLog(TopologyClusterInformationModel.class);

    //private Map<Integer, Set<CartridgeTypeContext>> tenantIdToCartridgeTypeContextMap;
    private static TopologyClusterInformationModel topologyClusterInformationModel;
    private Map<String, Cluster> clusterIdToClusterMap;
    private Map<String, Cluster> serviceNameToClusterMap;
    private DataInsertionAndRetrievalManager dataInsertionNRetrievalMgr;
    private boolean initialized;

    //locks
    private static volatile ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private static volatile ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private TopologyClusterInformationModel() {
        //tenantIdToCartridgeTypeContextMap = new HashMap<Integer, Set<CartridgeTypeContext>>();
        clusterIdToClusterMap = new HashMap<String, Cluster>();
        serviceNameToClusterMap = new HashMap<String, Cluster>();
        dataInsertionNRetrievalMgr = new DataInsertionAndRetrievalManager();
    }

    public static TopologyClusterInformationModel getInstance () {

        if(topologyClusterInformationModel == null) {
            synchronized (TopologyClusterInformationModel.class) {
                if (topologyClusterInformationModel == null) {
                    topologyClusterInformationModel = new TopologyClusterInformationModel();
                }
            }
        }

        return topologyClusterInformationModel;
    }
    
    public void addCluster (Cluster cluster) {

        if(log.isDebugEnabled()) {
    		log.debug(" Adding cluster ["+cluster+"] ");
    	}
    	clusterIdToClusterMap.put(cluster.getClusterId(), cluster);
    	serviceNameToClusterMap.put(cluster.getServiceName(), cluster);
    }   
    
    public Cluster getCluster (int tenantId, String subscriptionAlias) {

        String clusterId = dataInsertionNRetrievalMgr.getCartridgeSubscription(tenantId, subscriptionAlias).getClusterDomain();
        Cluster cluster = clusterIdToClusterMap.get(clusterId);
        if(log.isDebugEnabled()) {
            log.debug(" Found cluster ["+cluster+"] with id ["+clusterId+"] ");
        }
        return cluster;
    }
    
    public Cluster getCluster (String cartridgeType) {

    	Service service;
    	Cluster cluster = null;
		try {
			service = dataInsertionNRetrievalMgr.getService(cartridgeType);
		} catch (PersistenceManagerException e) {
			log.error("Exception occurred when retrieving service [" + cartridgeType +"] ");
			return null;
		}
		
		if(service != null) {
			String clusterId = service.getClusterId();
	    	cluster = clusterIdToClusterMap.get(clusterId);
	    	if(log.isDebugEnabled()) {
	    		log.debug(" Found cluster ["+cluster+"] with id ["+clusterId+"] ");
	    	}
		}    	
    	return cluster;
    }
    
    public Set<Cluster> getClusters (int tenantId, String cartridgeType) {
    	if(log.isDebugEnabled()) {
        	log.info(" Getting cluster for tenant id [" + tenantId + "] ");	
    	}
    	Collection<CartridgeSubscription> subscriptions = null;

        if(cartridgeType != null) {
    		subscriptions = dataInsertionNRetrievalMgr.getCartridgeSubscriptions(tenantId, cartridgeType);
    	}else {
    		subscriptions = dataInsertionNRetrievalMgr.getCartridgeSubscriptions(tenantId);
    	}

        Set<Cluster> clusterSet = new HashSet<Cluster>();
		if (subscriptions != null) {
			for (CartridgeSubscription cartridgeSubscription : subscriptions) {
				
				// TODO  -- check this. Sometimes getting clusterId some non-meaningful value
				String clusterId = cartridgeSubscription.getClusterDomain();
				if(log.isDebugEnabled()) {
					log.info("Finding cluster with id ["+clusterId+"] ");
				}
				Cluster foundCluster = clusterIdToClusterMap.get(clusterId);
				if (foundCluster != null) {
					clusterSet.add(foundCluster);
				}
			}
		}
    	return clusterSet;
    }
   
	public Set<Cluster> getClusters(String cartridgeType) {

		Set<Cluster> clusterSet = new HashSet<Cluster>();

		if (log.isDebugEnabled()) {
			log.info("Finding cluster with service name [" + cartridgeType
					+ "] ");
		}
		Cluster foundCluster = serviceNameToClusterMap.get(cartridgeType);
		if (foundCluster != null) {
			clusterSet.add(foundCluster);
		}

		return clusterSet;
	}
	
//    public void removeCluster (int tenantId, String cartridgeType, String subscriptionAlias) {
//
//        Set<CartridgeTypeContext> cartridgeTypeContextSet = null;
//        Set<SubscriptionAliasContext> subscriptionAliasContextSet = null;
//
//        writeLock.lock();
//        try {
//            //check if a set of CartridgeTypeContext instances already exist for given tenant Id
//            cartridgeTypeContextSet = tenantIdToCartridgeTypeContextMap.get(tenantId);
//            if(cartridgeTypeContextSet != null) {
//                CartridgeTypeContext cartridgeTypeContext = null;
//                //iterate through the set
//                Iterator<CartridgeTypeContext> typeCtxIterator = cartridgeTypeContextSet.iterator();
//                while (typeCtxIterator.hasNext()) {
//                    //see if the set contains a CartridgeTypeContext instance with the given cartridge type
//                    cartridgeTypeContext = typeCtxIterator.next();
//                    if (cartridgeTypeContext.getType().equals(cartridgeType)){
//                        //if so, get the SubscriptionAliasContext set
//                        subscriptionAliasContextSet = cartridgeTypeContext.getSubscriptionAliasContextSet();
//                        break;
//                    }
//                }
//                if(subscriptionAliasContextSet != null) {
//                    //iterate through the set
//                    Iterator<SubscriptionAliasContext> aliasIterator = subscriptionAliasContextSet.iterator();
//                    while (aliasIterator.hasNext()) {
//                        //see if the set contains a SubscriptionAliasContext instance with the given alias
//                        SubscriptionAliasContext subscriptionAliasContext = aliasIterator.next();
//                        if (subscriptionAliasContext.getSubscriptionAlias().equals(subscriptionAlias)) {
//                            //remove the existing one
//                            aliasIterator.remove();
//
//                            if (log.isDebugEnabled()) {
//                                log.debug("Removed cluster for tenant " + tenantId + ", type " + cartridgeType +
//                                        ", subscription alias " + subscriptionAlias);
//                            }
//
//                            break;
//                        }
//                    }
//                }
//            }
//
//        } finally {
//            writeLock.unlock();
//        }
//    }
    
    public void removeCluster (String clusterId) {

        if(log.isDebugEnabled()) {
    		log.debug(" Removing cluster ["+clusterId+"] ");    		
    	}
    	clusterIdToClusterMap.remove(clusterId);
    }

	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

//    private class CartridgeTypeContext {
//
//        private String type;
//        private Set<SubscriptionAliasContext> subscriptionAliasContextSet;
//
//        public CartridgeTypeContext (String type) {
//            this.type = type;
//        }
//
//        public void setSubscriptionAliasContextSet (Set<SubscriptionAliasContext> subscriptionAliasContextSet) {
//            this.subscriptionAliasContextSet = subscriptionAliasContextSet;
//        }
//
//        public String getType () {
//            return type;
//        }
//
//        public Set<SubscriptionAliasContext> getSubscriptionAliasContextSet () {
//            return subscriptionAliasContextSet;
//        }
//
//        public boolean equals(Object other) {
//
//            if(this == other) {
//                return true;
//            }
//            if(!(other instanceof CartridgeTypeContext)) {
//                return false;
//            }
//
//            CartridgeTypeContext that = (CartridgeTypeContext)other;
//            return this.type.equals(that.type);
//        }
//
//        public int hashCode () {
//            return type.hashCode();
//        }
//    }
//
//    private class SubscriptionAliasContext {
//
//        private String subscriptionAlias;
//        private Cluster cluster;
//
//        public SubscriptionAliasContext(String subscriptionAlias, Cluster cluster) {
//            this.subscriptionAlias = subscriptionAlias;
//            this.cluster = cluster;
//        }
//
//        public String getSubscriptionAlias () {
//            return subscriptionAlias;
//        }
//
//        public Cluster getCluster () {
//            return cluster;
//        }
//
//        public boolean equals(Object other) {
//
//            if(this == other) {
//                return true;
//            }
//            if(!(other instanceof SubscriptionAliasContext)) {
//                return false;
//            }
//
//            SubscriptionAliasContext that = (SubscriptionAliasContext)other;
//            return this.subscriptionAlias.equals(that.subscriptionAlias);
//        }
//
//        public int hashCode () {
//            return subscriptionAlias.hashCode();
//        }
//    }
}

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
package org.apache.stratos.cloud.controller.runtime;

import org.apache.stratos.cloud.controller.registry.RegistryManager;
import org.apache.stratos.cloud.controller.util.*;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.wso2.carbon.databridge.agent.thrift.DataPublisher;

import javax.jms.TextMessage;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This object holds all runtime data and provides faster access. This is a Singleton class.
 */
public class FasterLookUpDataHolder implements Serializable{

    private static final long serialVersionUID = -2662307358852779897L;

	private static volatile FasterLookUpDataHolder ctxt;

	/* We keep following maps in order to make the look up time, small. */

	/**
	 * Map of maps.
	 * Map 1:
	 * Key - domain
	 * value - {@link ServiceContext}
	 */
	private Map<String, ServiceContext> serviceCtxts;
	
	/**
	 * To make data retrieval from registry faster.
	 */
	private List<ServiceContext> serviceCtxtList;

	public List<ServiceContext> getServiceCtxtList() {
    	return serviceCtxtList;
    }

	/**
	 * Key - node id
	 * Value - {@link ServiceContext}
	 */
	private Map<String, ServiceContext> nodeIdToServiceCtxt;
	
	/**
	 * List of registered {@link Cartridge}s
	 */
	private List<Cartridge> cartridges;

	/**
	 * List of IaaS Providers.
	 */
	private List<IaasProvider> iaasProviders;


	private String serializationDir;
	private boolean enableBAMDataPublisher;
	private boolean enableTopologySync;
	private TopologyConfig topologyConfig;
	private String bamUsername = CloudControllerConstants.DEFAULT_BAM_SERVER_USER_NAME;
	private String bamPassword = CloudControllerConstants.DEFAULT_BAM_SERVER_PASSWORD;
	private String dataPublisherCron = CloudControllerConstants.PUB_CRON_EXPRESSION;
	private String cassandraConnUrl = CloudControllerConstants.DEFAULT_CASSANDRA_URL;
	private String cassandraUser = CloudControllerConstants.DEFAULT_CASSANDRA_USER;
	private String cassandraPassword = CloudControllerConstants.DEFAULT_CASSANDRA_PASSWORD;
	/**
	 * Key - node id 
	 * Value - Status of the instance
	 * This map is only used by BAM data publisher in CC.
	 */
	private Map<String, String> nodeIdToStatusMap = new HashMap<String, String>();
	
	/**
	 * Key - iaas type
	 * Value - # of running instance count
	 * This map will be used to track the running instances count in each IaaS
	 */
	private Map<String, Integer> iaasToActiveInstanceCountMap = new HashMap<String, Integer>();
	
	/**
     * Key - name of the topic
     * Value - corresponding EventPublisher
     */
    private transient Map<String, EventPublisher> topicToPublisherMap = new HashMap<String, EventPublisher>();

	private transient DataPublisher dataPublisher;
	private String streamId;
	private boolean isPublisherRunning;
	private boolean isTopologySyncRunning;

	private BlockingQueue<TextMessage> sharedTopologyDiffQueue = new LinkedBlockingQueue<TextMessage>();

	public static FasterLookUpDataHolder getInstance() {

		if (ctxt == null) {
			synchronized (FasterLookUpDataHolder.class) {
				if (ctxt == null && RegistryManager.getInstance() != null) {

					Object obj = RegistryManager.getInstance().retrieve();
					if (obj != null) {
						if (obj instanceof FasterLookUpDataHolder) {
							ctxt = (FasterLookUpDataHolder) obj;
							System.out.println("*********** FasterLookUpDataHolder ********");
						} else {
							System.out.println("*********** Not a FasterLookUpDataHolder *******");
						}

					} 
					
				}
				if(ctxt == null) {
					ctxt = new FasterLookUpDataHolder();
				}
			}
		}

		return ctxt;
	}

	private FasterLookUpDataHolder() {

		serviceCtxtList = new ArrayList<ServiceContext>();
		serviceCtxts = new ConcurrentHashMap<String,ServiceContext>();
		nodeIdToServiceCtxt = new LinkedHashMap<String, ServiceContext>();
		cartridges = new ArrayList<Cartridge>();

	}

	public void addServiceContext(ServiceContext ctx) {

		if (ctx == null) {
			return;
		}

		String domain = ctx.getClusterId();


		if (domain != null) {
			addToServiceCtxts(domain, ctx);
		}

	}

	public void removeServiceContext(ServiceContext ctxt) {

		if (ctxt == null) {
			return;
		}

		String domain = ctxt.getClusterId();

		if (domain != null) {
			if (serviceCtxts.containsKey(domain)) {
                serviceCtxts.remove(ctxt);
			}
		}
		
		serviceCtxtList.remove(ctxt);

	}

	public ServiceContext getServiceContextFromDomain(String domain) {

		if (serviceCtxts.get(domain) != null) {
			return serviceCtxts.get(domain);
		}
		return null;
	}

	public ServiceContext getServiceContextFromNodeId(String nodeId) {

		return nodeIdToServiceCtxt.get(nodeId);
	}
	
	public List<Object> getNodeIdsOfServiceCtxt(ServiceContext ctxt){
		return CloudControllerUtil.getKeysFromValue(nodeIdToServiceCtxt, ctxt);
	}

	public Map<String, ServiceContext> getServiceContexts() {
		return serviceCtxts;
	}

	public void addNodeId(String nodeId, ServiceContext ctxt) {
		nodeIdToServiceCtxt.put(nodeId, ctxt);
	}

	public void removeNodeId(String nodeId) {
		nodeIdToServiceCtxt.remove(nodeId);
	}
	
	public void setNodeIdToServiceContextMap(Map<String, ServiceContext> map) {
		nodeIdToServiceCtxt = map;
	}

	public Map<String, ServiceContext> getNodeIdToServiceContextMap() {
		return nodeIdToServiceCtxt;
	}

	private void addToServiceCtxts(String domainName, ServiceContext ctxt) {
        serviceCtxts.put(domainName, ctxt);
        serviceCtxtList.add(ctxt);

	}

	public List<Cartridge> getCartridges() {
		return cartridges;
	}

	public Cartridge getCartridge(String cartridgeType) {
		for (Cartridge cartridge : cartridges) {
			if (cartridge.getType().equals(cartridgeType)) {
				return cartridge;
			}
		}

		return null;

	}
	
	public void updateActiveInstanceCount(String iaasType, int count) {
		int currentCount = 0;
		if(iaasToActiveInstanceCountMap.containsKey(iaasType)){
			currentCount = iaasToActiveInstanceCountMap.get(iaasType);
		}
		iaasToActiveInstanceCountMap.put(iaasType, currentCount+count);
	}
	
	public int getActiveInstanceCount(String iaasType) {
		Integer count = iaasToActiveInstanceCountMap.get(iaasType);
		return count == null ? 0 : count;
	}

//	public void addCartridges(List<Cartridge> newCartridges) {
//		if (this.cartridges == null) {
//			this.cartridges = newCartridges;
//		} else {
//			for (Cartridge cartridge : newCartridges) {
//				int idx;
//				if ((idx = cartridges.indexOf(cartridge)) != -1) {
//					Cartridge ref = cartridges.get(idx);
//					ref = cartridge;
//				} else {
//					cartridges.add(cartridge);
//				}
//			}
//		}
//
//	}
	
	public void addCartridge(Cartridge newCartridges) {
	
		cartridges.add(newCartridges);
	}

	public void removeCartridges(List<Cartridge> cartridges) {
		if (this.cartridges != null) {
			this.cartridges.removeAll(cartridges);
		}

	}

	public List<IaasProvider> getIaasProviders() {
		return iaasProviders;
	}

	public void setIaasProviders(List<IaasProvider> iaasProviders) {
		this.iaasProviders = iaasProviders;
	}

	public String getSerializationDir() {
		return serializationDir;
	}

	public void setSerializationDir(String serializationDir) {
		this.serializationDir = serializationDir;
	}

	public String getBamUsername() {
		return bamUsername;
	}

	public void setBamUsername(String bamUsername) {
		this.bamUsername = bamUsername;
	}

	public String getBamPassword() {
		return bamPassword;
	}

	public void setBamPassword(String bamPassword) {
		this.bamPassword = bamPassword;
	}

	public String getDataPublisherCron() {
		return dataPublisherCron;
	}

	public void setDataPublisherCron(String dataPublisherCron) {
		this.dataPublisherCron = dataPublisherCron;
	}

	public Map<String, String> getNodeIdToStatusMap() {
		return nodeIdToStatusMap;
	}

	public void setNodeIdToStatusMap(Map<String, String> nodeIdToStatusMap) {
		this.nodeIdToStatusMap = nodeIdToStatusMap;
	}

	public DataPublisher getDataPublisher() {
		return dataPublisher;
	}

	public void setDataPublisher(DataPublisher dataPublisher) {
		this.dataPublisher = dataPublisher;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public boolean getEnableBAMDataPublisher() {
		return enableBAMDataPublisher;
	}

	public void setEnableBAMDataPublisher(boolean enableBAMDataPublisher) {
		this.enableBAMDataPublisher = enableBAMDataPublisher;
	}

	public String getCassandraConnUrl() {
		return cassandraConnUrl;
	}

	public void setCassandraConnUrl(String cassandraHostAddr) {
		this.cassandraConnUrl = cassandraHostAddr;
	}

	public String getCassandraUser() {
		return cassandraUser;
	}

	public void setCassandraUser(String cassandraUser) {
		this.cassandraUser = cassandraUser;
	}

	public String getCassandraPassword() {
		return cassandraPassword;
	}

	public void setCassandraPassword(String cassandraPassword) {
		this.cassandraPassword = cassandraPassword;
	}

	public boolean isPublisherRunning() {
		return isPublisherRunning;
	}

	public void setPublisherRunning(boolean isPublisherRunning) {
		this.isPublisherRunning = isPublisherRunning;
	}

	public BlockingQueue<TextMessage> getSharedTopologyDiffQueue() {
		return sharedTopologyDiffQueue;
	}

	public void setSharedTopologyDiffQueue(BlockingQueue<TextMessage> sharedTopologyDiffQueue) {
		this.sharedTopologyDiffQueue = sharedTopologyDiffQueue;
	}

	public boolean getEnableTopologySync() {
		return enableTopologySync;
	}

	public void setEnableTopologySync(boolean enableTopologySync) {
		this.enableTopologySync = enableTopologySync;
	}

	public boolean isTopologySyncRunning() {
	    return isTopologySyncRunning;
    }

	public void setTopologySyncRunning(boolean isTopologySyncRunning) {
	    this.isTopologySyncRunning = isTopologySyncRunning;
    }

	public TopologyConfig getTopologyConfig() {
		return topologyConfig;
	}

	public void setTopologyConfig(TopologyConfig topologyConfig) {
		this.topologyConfig = topologyConfig;
	}
	
	public EventPublisher getEventPublisher(String topic){
    	return topicToPublisherMap.get(topic);
    }
	
	public List<EventPublisher> getAllEventPublishers() {
		return new ArrayList<EventPublisher>(topicToPublisherMap.values());
	}
	
    public void addEventPublisher(EventPublisher publisher, String topicName) {
        topicToPublisherMap.put(topicName, publisher);
    }
}

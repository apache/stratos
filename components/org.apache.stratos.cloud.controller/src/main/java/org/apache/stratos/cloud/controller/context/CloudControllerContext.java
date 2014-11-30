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
package org.apache.stratos.cloud.controller.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.registry.RegistryManager;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;

/**
 * This object holds all runtime data and provides faster access. This is a Singleton class.
 */
public class CloudControllerContext implements Serializable{

    private static final long serialVersionUID = -2662307358852779897L;
    private static final Log log = LogFactory.getLog(CloudControllerContext.class);

    private static volatile CloudControllerContext instance;

	/* We keep following maps in order to make the look up time, small. */
	
	/**
     * Key - cluster id
     * Value - list of {@link MemberContext}
     */
    private Map<String, List<MemberContext>> clusterIdToMemberContext;

    /**
	 * Key - member id
	 * Value - {@link MemberContext}
	 */
	private Map<String, MemberContext> memberIdToContext;
	
	/**
     * Key - member id
     * Value - ScheduledFuture task
     */
    private transient Map<String, ScheduledFuture<?>> memberIdToScheduledTask;
	
	/**
	 * Key - Kubernetes cluster id
	 * Value - {@link org.apache.stratos.cloud.controller.domain.KubernetesClusterContext}
	 */
	private Map<String, KubernetesClusterContext> kubClusterIdToKubClusterContext;
	
	/**
	 * Key - cluster id
	 * Value - {@link org.apache.stratos.cloud.controller.domain.ClusterContext}
	 */
	private Map<String, ClusterContext> clusterIdToContext;
	
	/**
	 * This works as a cache to hold already validated partitions against a cartridge type.
	 * Key - cartridge type
	 * Value - list of partition ids
	 */
	private Map<String, List<String>> cartridgeTypeToPartitionIds = new ConcurrentHashMap<String, List<String>>();
	
	/**
     * Thread pool used in this task to execute parallel tasks.
     */
    private transient ExecutorService executor = Executors.newFixedThreadPool(20);
	
	/**
	 * List of registered {@link org.apache.stratos.cloud.controller.domain.Cartridge}s
	 */
	private List<Cartridge> cartridges;
	
	/**
	 * List of deployed service groups
	 */
	private List<ServiceGroup> serviceGroups;

	private String serializationDir;
	private String streamId;
	private boolean isPublisherRunning;
	private boolean isTopologySyncRunning;
    private boolean clustered;

    private transient AsyncDataPublisher dataPublisher;

    private CloudControllerContext() {
        // Initialize cloud controller context
        clusterIdToMemberContext = new ConcurrentHashMap<String, List<MemberContext>>();
        memberIdToContext = new ConcurrentHashMap<String, MemberContext>();
        memberIdToScheduledTask = new ConcurrentHashMap<String, ScheduledFuture<?>>();
        kubClusterIdToKubClusterContext = new ConcurrentHashMap<String, KubernetesClusterContext>();
        clusterIdToContext = new ConcurrentHashMap<String, ClusterContext>();
        cartridgeTypeToPartitionIds = new ConcurrentHashMap<String, List<String>>();
        cartridges = new ArrayList<Cartridge>();
        serviceGroups = new ArrayList<ServiceGroup>();

        if (log.isInfoEnabled()) {
            log.info("Cloud controller context initialized locally");
        }
    }

	public static CloudControllerContext getInstance() {
		if (instance == null) {
			synchronized (CloudControllerContext.class) {
				if (instance == null && RegistryManager.getInstance() != null) {
					Object obj = RegistryManager.getInstance().retrieve();
					if (obj != null) {
						if (obj instanceof CloudControllerContext) {
							instance = (CloudControllerContext) obj;
						}
					}
				}
				if(instance == null) {
					instance = new CloudControllerContext();
				}
			}
		}
		return instance;
	}

	public List<Cartridge> getCartridges() {
		return cartridges;
	}
	
	public void setCartridges(List<Cartridge> cartridges) {
	    this.cartridges = cartridges;
	}
	
	public void setServiceGroups(List<ServiceGroup> serviceGroups) {
		this.serviceGroups = serviceGroups;
	}
	
	public List<ServiceGroup> getServiceGroups() {
		return this.serviceGroups;
	}


	public Cartridge getCartridge(String cartridgeType) {
		for (Cartridge cartridge : cartridges) {
			if (cartridge.getType().equals(cartridgeType)) {
				return cartridge;
			}
		}
		return null;
	}
	
	public void addCartridge(Cartridge newCartridges) {
		cartridges.add(newCartridges);
	}

	public void removeCartridges(List<Cartridge> cartridges) {
		if (this.cartridges != null) {
			this.cartridges.removeAll(cartridges);
		}

	}
	
	public ServiceGroup getServiceGroup(String name) {
		for (ServiceGroup serviceGroup : serviceGroups) {
			if (serviceGroup.getName().equals(name)) {
				return serviceGroup;
			}
		}

		return null;
	}
	
	public void addServiceGroup(ServiceGroup newServiceGroup) {
		this.serviceGroups.add(newServiceGroup);
	}
	
	public void removeServiceGroup(List<ServiceGroup> serviceGroup) {
		if (this.serviceGroups != null) {
			this.serviceGroups.removeAll(serviceGroup);
		}
	}

	public String getSerializationDir() {
		return serializationDir;
	}

	public void setSerializationDir(String serializationDir) {
		this.serializationDir = serializationDir;
	}

	public AsyncDataPublisher getDataPublisher() {
		return dataPublisher;
	}

	public void setDataPublisher(AsyncDataPublisher dataPublisher) {
		this.dataPublisher = dataPublisher;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public boolean isPublisherRunning() {
		return isPublisherRunning;
	}

	public void setPublisherRunning(boolean isPublisherRunning) {
		this.isPublisherRunning = isPublisherRunning;
	}

	public boolean isTopologySyncRunning() {
	    return isTopologySyncRunning;
    }

	public void setTopologySyncRunning(boolean isTopologySyncRunning) {
	    this.isTopologySyncRunning = isTopologySyncRunning;
    }

    public void addMemberContext(MemberContext ctxt) {
        memberIdToContext.put(ctxt.getMemberId(), ctxt);
        
        List<MemberContext> ctxts;
        
        if((ctxts = clusterIdToMemberContext.get(ctxt.getClusterId())) == null) {
            ctxts = new ArrayList<MemberContext>();
        } 
        if(ctxts.contains(ctxt)) {
        	ctxts.remove(ctxt);
        }
        ctxts.add(ctxt);
        clusterIdToMemberContext.put(ctxt.getClusterId(), ctxts);
        if(log.isDebugEnabled()) {
        	
        	log.debug("Added Member Context to the information model. "+ctxt);
        }
    }
    
    public void addScheduledFutureJob(String memberId, ScheduledFuture<?> job) {
        memberIdToScheduledTask.put(memberId, job);
    }
    
    public List<MemberContext> removeMemberContextsOfCluster(String clusterId) {
        List<MemberContext> ctxts = clusterIdToMemberContext.remove(clusterId);
        if(ctxts == null) {
            return new ArrayList<MemberContext>();
        }
        for (MemberContext memberContext : ctxts) {
            String memberId = memberContext.getMemberId();
            memberIdToContext.remove(memberId);
            stopTask(memberIdToScheduledTask.remove(memberId));
        }
        if(log.isDebugEnabled()) {
        	
        	log.debug("Removed Member Context from the information model. "+ instance);
        }
        return ctxts;
    }
    
    public MemberContext removeMemberContext(String memberId, String clusterId) {
    	MemberContext returnedCtxt = memberIdToContext.remove(memberId);
        List<MemberContext> ctxts = clusterIdToMemberContext.get(clusterId);

        if (ctxts != null) {
            
            List<MemberContext> newCtxts =  new ArrayList<MemberContext>(ctxts);
            
            for (Iterator<MemberContext> iterator = newCtxts.iterator(); iterator.hasNext();) {
                MemberContext memberContext = (MemberContext) iterator.next();
                if(memberId.equals(memberContext.getMemberId())) {
                    if(log.isDebugEnabled()) {
                        
                        log.debug("MemberContext [id]: "+memberId+" removed from information model.");
                    }
                    iterator.remove();
                }
            }
            
            clusterIdToMemberContext.put(clusterId, newCtxts);
        }
        
        stopTask(memberIdToScheduledTask.remove(memberId));
        
        return returnedCtxt;
    }
    
    private void stopTask(ScheduledFuture<?> task) {
        if (task != null) {
            
            task.cancel(true);
            log.info("Scheduled Pod Activation Watcher task canceled.");
        }
    }
    
    public MemberContext getMemberContextOfMemberId(String memberId) {
        return memberIdToContext.get(memberId);
    }
    
    public List<MemberContext> getMemberContextsOfClusterId(String clusterId) {
        return clusterIdToMemberContext.get(clusterId);
    }

    public Map<String, List<MemberContext>> getClusterIdToMemberContext() {
        return clusterIdToMemberContext;
    }
    
    public void setClusterIdToMemberContext(Map<String, List<MemberContext>> clusterIdToMemberContext) {
        this.clusterIdToMemberContext = clusterIdToMemberContext;
    }

    public Map<String, MemberContext> getMemberIdToContext() {
        return memberIdToContext;
    }

    public void setMemberIdToContext(Map<String, MemberContext> memberIdToContext) {
        this.memberIdToContext = memberIdToContext;
    }

    public void addClusterContext(ClusterContext ctxt) {
        clusterIdToContext.put(ctxt.getClusterId(), ctxt);
    }
    
    public ClusterContext getClusterContext(String clusterId) {
        return clusterIdToContext.get(clusterId);
    }
    
    public ClusterContext removeClusterContext(String clusterId) {
        return clusterIdToContext.remove(clusterId);
    }
    
    public Map<String, ClusterContext> getClusterIdToContext() {
        return clusterIdToContext;
    }

    public void setClusterIdToContext(Map<String, ClusterContext> clusterIdToContext) {
        this.clusterIdToContext = clusterIdToContext;
    }

	public ExecutorService getExecutor() {
		return executor;
	}

	public void setExecutor(ExecutorService executor) {
		this.executor = executor;
	}

	public Map<String, List<String>> getCartridgeTypeToPartitionIds() {
		return cartridgeTypeToPartitionIds;
	}

	public void setCartridgeTypeToPartitionIds(
			Map<String, List<String>> cartridgeTypeToPartitionIds) {
		this.cartridgeTypeToPartitionIds = cartridgeTypeToPartitionIds;
	}
	
	public void addToCartridgeTypeToPartitionIdMap(String cartridgeType, String partitionId) {
		List<String> list = this.cartridgeTypeToPartitionIds.get(cartridgeType);
		
		if(list == null) {
			list = new ArrayList<String>();
		}
		
		list.add(partitionId);
		this.cartridgeTypeToPartitionIds.put(cartridgeType, list);
	}
	
	public void removeFromCartridgeTypeToPartitionIds(String cartridgeType) {
		this.cartridgeTypeToPartitionIds.remove(cartridgeType);
	}

	public Map<String, KubernetesClusterContext> getKubClusterIdToKubClusterContext() {
		return kubClusterIdToKubClusterContext;
	}
	
	public KubernetesClusterContext getKubernetesClusterContext(String kubClusterId) {
		return kubClusterIdToKubClusterContext.get(kubClusterId);
	}
	
	public void addKubernetesClusterContext(KubernetesClusterContext ctxt) {
		this.kubClusterIdToKubClusterContext.put(ctxt.getKubernetesClusterId(), ctxt);
	}

	public void setKubClusterIdToKubClusterContext(
			Map<String, KubernetesClusterContext> kubClusterIdToKubClusterContext) {
		this.kubClusterIdToKubClusterContext = kubClusterIdToKubClusterContext;
	}

    public Map<String, ScheduledFuture<?>> getMemberIdToScheduledTask() {
        return memberIdToScheduledTask;
    }

    public void setMemberIdToScheduledTask(Map<String, ScheduledFuture<?>> memberIdToScheduledTask) {
        this.memberIdToScheduledTask = memberIdToScheduledTask;
    }

    public boolean isClustered() {
        return clustered;
    }
}
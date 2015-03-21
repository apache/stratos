/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.stratos.autoscaler.context;

import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.algorithms.networkpartition.NetworkPartitionAlgorithmContext;
import org.apache.stratos.autoscaler.applications.pojo.ApplicationContext;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.autoscaler.monitor.component.ApplicationMonitor;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.common.services.DistributedObjectProvider;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * It holds all cluster monitors which are active in stratos.
 */
public class AutoscalerContext {
	
	private static final Log log = LogFactory.getLog(AutoscalerContext.class);

	private static final String AS_APPLICATION_ID_TO_APPLICATION_CTX_MAP = "AS_APPLICATION_ID_TO_APPLICATION_CTX_MAP";
	private static final String AS_CLUSTER_ID_TO_CLUSTER_MONITOR_MAP = "AS_CLUSTER_ID_TO_CLUSTER_MONITOR_MAP";
	private static final String AS_APPLICATION_ID_TO_APPLICATION_MONITOR_MAP = "AS_APPLICATION_ID_TO_APPLICATION_MONITOR_MAP";
	private static final String AS_PENDING_APPLICATION_MONITOR_LIST = "AS_PENDING_APPLICATION_MONITOR_LIST";
	private static final String AS_APPLICATIOIN_ID_TO_NETWORK_PARTITION_ALGO_CTX_MAP = "AS_APPLICATIOIN_ID_TO_NETWORK_PARTITION_ALGO_CTX_MAP";
	private boolean clustered;
	private boolean coordinator;
	
    private static volatile AutoscalerContext instance;
    private final transient DistributedObjectProvider distributedObjectProvider;

    // Map<ApplicationId, ApplicationContext>
    private Map<String, ApplicationContext> applicationContextMap;
    // Map<ClusterId, AbstractClusterMonitor>
    private Map<String, ClusterMonitor> clusterMonitors;
    // Map<ApplicationId, ApplicationMonitor>
    private Map<String, ApplicationMonitor> applicationMonitors;
    //pending application monitors
    private List<String> pendingApplicationMonitors;
    // Map<ApplicationId, NetworkPartitioinContext>
    private Map<String, NetworkPartitionAlgorithmContext> applicationIdToNetworkPartitionAlgorithmContextMap;

    private AutoscalerContext() {
    	// Check clustering status
        AxisConfiguration axisConfiguration = ServiceReferenceHolder.getInstance().getAxisConfiguration();
        if ((axisConfiguration != null) && (axisConfiguration.getClusteringAgent() != null)) {
            clustered = true;
        }

        // Initialize distributed object provider
        distributedObjectProvider = ServiceReferenceHolder.getInstance().getDistributedObjectProvider();
    	
    	applicationContextMap = readApplicationContextsFromRegistry();
        if(applicationContextMap == null) {
            applicationContextMap = distributedObjectProvider.getMap(AS_APPLICATION_ID_TO_APPLICATION_CTX_MAP);//new ConcurrentHashMap<String, ApplicationContext>();
        }
        setClusterMonitors(distributedObjectProvider.getMap(AS_CLUSTER_ID_TO_CLUSTER_MONITOR_MAP));
        setApplicationMonitors(distributedObjectProvider.getMap(AS_APPLICATION_ID_TO_APPLICATION_MONITOR_MAP));
        pendingApplicationMonitors = distributedObjectProvider.getList(AS_PENDING_APPLICATION_MONITOR_LIST);//new ArrayList<String>();
        applicationIdToNetworkPartitionAlgorithmContextMap = distributedObjectProvider.getMap(AS_APPLICATIOIN_ID_TO_NETWORK_PARTITION_ALGO_CTX_MAP);
    }

    private Map<String, ApplicationContext> readApplicationContextsFromRegistry() {
        String[] resourcePaths = RegistryManager.getInstance().getApplicationContextResourcePaths();
        if((resourcePaths == null) || (resourcePaths.length == 0)) {
            return null;
        }

        Map<String, ApplicationContext> applicationContextMap = distributedObjectProvider.getMap(AS_APPLICATION_ID_TO_APPLICATION_CTX_MAP);//new ConcurrentHashMap<String, ApplicationContext>();
        for(String resourcePath : resourcePaths) {
            ApplicationContext applicationContext = RegistryManager.getInstance().getApplicationContextByResourcePath(resourcePath);
            applicationContextMap.put(applicationContext.getApplicationId(), applicationContext);
        }
        return applicationContextMap;
    }
    
    public static AutoscalerContext getInstance() {
        if(instance == null) {
            synchronized (AutoscalerContext.class) {
                if(instance == null) {
                    instance = new AutoscalerContext();
                }
            }
        }
        return instance;
    }

    public void addClusterMonitor(ClusterMonitor clusterMonitor) {
        getClusterMonitors().put(clusterMonitor.getClusterId(), clusterMonitor);
    }

    public ClusterMonitor getClusterMonitor(String clusterId) {
        return getClusterMonitors().get(clusterId);
    }

    public ClusterMonitor removeClusterMonitor(String clusterId) {
        return getClusterMonitors().remove(clusterId);
    }

    public void addAppMonitor(ApplicationMonitor applicationMonitor) {
        getApplicationMonitors().put(applicationMonitor.getId(), applicationMonitor);
    }

    public ApplicationMonitor getAppMonitor(String applicationId) {
        return getApplicationMonitors().get(applicationId);
    }

    public void removeAppMonitor(String applicationId) {
        getApplicationMonitors().remove(applicationId);
    }

    public Map<String, ClusterMonitor> getClusterMonitors() {
        return clusterMonitors;
    }

    public void setClusterMonitors(Map<String, ClusterMonitor> clusterMonitors) {
        this.clusterMonitors = clusterMonitors;
    }

    public Map<String, ApplicationMonitor> getApplicationMonitors() {
        return applicationMonitors;
    }

    public void setApplicationMonitors(Map<String, ApplicationMonitor> applicationMonitors) {
        this.applicationMonitors = applicationMonitors;
    }

    public List<String> getPendingApplicationMonitors() {
        return pendingApplicationMonitors;
    }

    public void addApplicationPendingMonitor(String appId) {
        this.pendingApplicationMonitors.add(appId);
    }

    public void removeApplicationPendingMonitor(String appId) {
        this.pendingApplicationMonitors.remove(appId);
    }

    public boolean containsApplicationPendingMonitor(String appId) {
        return this.pendingApplicationMonitors.contains(appId);
    }

    public void addApplicationContext(ApplicationContext applicationContext) {
        applicationContextMap.put(applicationContext.getApplicationId(), applicationContext);
        RegistryManager.getInstance().persistApplicationContext(applicationContext);
    }

    public ApplicationContext removeApplicationContext(String applicationId) {
        RegistryManager.getInstance().removeApplicationContext(applicationId);
        return applicationContextMap.remove(applicationId);
    }

    public ApplicationContext getApplicationContext(String applicationId) {
        return applicationContextMap.get(applicationId);
    }

    public Collection<ApplicationContext> getApplicationContexts() {
        return applicationContextMap.values();
    }

    public void updateApplicationContext(ApplicationContext applicationContext) {
        applicationContextMap.put(applicationContext.getApplicationId(), applicationContext);
        RegistryManager.getInstance().persistApplicationContext(applicationContext);
    }
    
    public boolean isClustered() {
        return clustered;
    }

    public boolean isCoordinator() {
        return coordinator;
    }

    public void setCoordinator(boolean coordinator) {
        this.coordinator = coordinator;
    }
    
	public void addNetworkPartitionAlgorithmContext(NetworkPartitionAlgorithmContext algorithmContext) {
		String applicationId = algorithmContext.getApplicationId();
		if (log.isInfoEnabled()) {
			log.info(String.format("Adding network partition algorithm context : [application-id] %s", applicationId));
		}
		this.addNetworkPartitionAlgorithmContextToInformationModel(algorithmContext);
		RegistryManager.getInstance().persistNetworkPartitionAlgorithmContext(algorithmContext);

		if (log.isInfoEnabled()) {
			log.info(String.format("network partition algorithm context is added successfully: [application-policy-id] %s", applicationId));
		}

	}
	
	public boolean removeNetworkPartitionAlgorithmContext(String applicationId) {
		if (StringUtils.isEmpty(applicationId)) {
			throw new AutoScalerException("Application id cannot be empty");
		}
		this.removeNetworkPartitionAlgorithmContextInInformationModel(applicationId);
		RegistryManager.getInstance().removeNetworkPartitionAlgorithmContext(applicationId);
		if (log.isInfoEnabled()) {
			log.info(String.format("Network partition algorithm context is removed successfully: [id] %s", applicationId));
		}
		return true;
	}
	
	private void removeNetworkPartitionAlgorithmContextInInformationModel(String applicationId) {
		if (applicationIdToNetworkPartitionAlgorithmContextMap.containsKey(applicationId)) {
			if (log.isDebugEnabled()) {
				log.debug("Removing network partition algorithm context [application-id] " + applicationId);
			}
			applicationIdToNetworkPartitionAlgorithmContextMap.remove(applicationId);
		}
		else{
			throw new AutoScalerException("No such application id [" + applicationId + "] exists");
		}
	}

	private void addNetworkPartitionAlgorithmContextToInformationModel(NetworkPartitionAlgorithmContext algorithmContext) {
		String applicationId = algorithmContext.getApplicationId();
        if (!applicationIdToNetworkPartitionAlgorithmContextMap.containsKey(applicationId)) {
            if (log.isDebugEnabled()) {
                log.debug("Adding network partition algorithm context : " + applicationId);
            }
            applicationIdToNetworkPartitionAlgorithmContextMap.put(applicationId, algorithmContext);
        } else {
        	String errMsg = "Network partition algorithm context already exists : " + applicationId;
        	log.error(errMsg);
            throw new AutoScalerException(errMsg);
        }
    }
	
    public NetworkPartitionAlgorithmContext getNetworkPartitionAlgorithmContext(String applicationId) {
        return applicationIdToNetworkPartitionAlgorithmContextMap.get(applicationId);
    }
}

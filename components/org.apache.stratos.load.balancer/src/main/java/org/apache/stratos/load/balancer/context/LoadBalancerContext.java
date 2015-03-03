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

package org.apache.stratos.load.balancer.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines load balancer context information.
 */
public class LoadBalancerContext {

    private static final Log log = LogFactory.getLog(LoadBalancerContext.class);

    private static volatile LoadBalancerContext instance;

    // Map<ClusterId, ClusterContext>
    private Map<String, ClusterContext> clusterIdToClusterContextMap;
    // Map<Host/Domain-Name, DomainMappingContextPath>
    private Map<String, String> hostNameToDomainMappingContextPathMap;

    private LoadBalancerContext() {
        clusterIdToClusterContextMap = new ConcurrentHashMap<String, ClusterContext>();
        hostNameToDomainMappingContextPathMap = new ConcurrentHashMap<String, String>();
    }

    public static LoadBalancerContext getInstance() {
        if (instance == null) {
            synchronized (LoadBalancerContext.class) {
                if (instance == null) {
                    instance = new LoadBalancerContext();
                }
            }
        }
        return instance;
    }

    public void clear() {
        clusterIdToClusterContextMap.clear();
        hostNameToDomainMappingContextPathMap.clear();
    }

    public Collection<ClusterContext> getClusterContexts() {
        return clusterIdToClusterContextMap.values();
    }

    public ClusterContext getClusterContext(String clusterId) {
        return clusterIdToClusterContextMap.get(clusterId);
    }

    public boolean containsClusterContext(String clusterId) {
        return clusterIdToClusterContextMap.containsKey(clusterId);
    }

    public void addClusterContext(ClusterContext clusterContext) {
        clusterIdToClusterContextMap.put(clusterContext.getClusterId(), clusterContext);
    }

    public void removeClusterContext(String clusterId) {
        clusterIdToClusterContextMap.remove(clusterId);
    }

    public void addDomainMappingContextPath(String hostName, String appContext) {
        hostNameToDomainMappingContextPathMap.put(hostName, appContext);
    }

    public String getDomainMappingContextPath(String hostName) {
        return hostNameToDomainMappingContextPathMap.get(hostName);
    }

    public void removeDomainMappingContextPath(String hostName) {
        if(containsDomainMappingContextPath(hostName)) {
            hostNameToDomainMappingContextPathMap.remove(hostName);
        }
    }

    public boolean containsDomainMappingContextPath(String hostName) {
        return hostNameToDomainMappingContextPathMap.containsKey(hostName);
    }
}

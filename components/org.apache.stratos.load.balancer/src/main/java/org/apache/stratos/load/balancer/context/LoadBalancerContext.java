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
import org.apache.stratos.load.balancer.context.map.*;

/**
 * Defines load balancer context information.
 */
public class LoadBalancerContext {

    private static final Log log = LogFactory.getLog(LoadBalancerContext.class);

    private static volatile LoadBalancerContext instance;

    // Map<ClusterId, ClusterContext>
    private ClusterIdClusterContextMap clusterIdClusterContextMap;

    // Following maps are updated by load balancer topology & tenant receivers.
    // Map<ClusterId, Cluster>
    // Keep track of all clusters
    // Map<Host/Domain-Name, Cluster>
    // Keep tack of all clusters

    // Map<Host/Domain-Name, AppContext>
    private HostNameAppContextMap hostNameAppContextMap;
    // Map<HostName, Map<TenantId, Cluster>>
    // Map<MemberIp, Hostname>
    // Keep track of cluster hostnames of of all members  against their ip addresses
    private MemberIpHostnameMap memberIpHostnameMap;

    private LoadBalancerContext() {
        clusterIdClusterContextMap = new ClusterIdClusterContextMap();
        hostNameAppContextMap = new HostNameAppContextMap();
        memberIpHostnameMap = new MemberIpHostnameMap();
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
        clusterIdClusterContextMap.clear();
        hostNameAppContextMap.clear();
        memberIpHostnameMap.clear();
    }

    public ClusterIdClusterContextMap getClusterIdClusterContextMap() {
        return clusterIdClusterContextMap;
    }

    public HostNameAppContextMap getHostNameContextPathMap() {
        return hostNameAppContextMap;
    }

    public MemberIpHostnameMap getMemberIpHostnameMap() {
        return memberIpHostnameMap;
    }
}

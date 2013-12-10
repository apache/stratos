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

package org.apache.stratos.adc.mgt.topology.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TopologyClusterModel {

    private static final Log log = LogFactory.getLog(TopologyClusterModel.class);
    private Map<TenantIdAndAliasTopologyKey, Cluster> tenantIdAndAliasTopologyKeyToClusterMap;
    private Map<Integer, List<Cluster>> tenantIdToClusterMap;
    private Map<TenantIdAndTypeTopologyKey , List<Cluster>> tenantIdAndTypeTopologyKeyToClusterMap;
    private static TopologyClusterModel topologyClusterModel;

    //locks
    private static volatile ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static volatile ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private static volatile ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private TopologyClusterModel() {
        tenantIdAndAliasTopologyKeyToClusterMap = new HashMap<TenantIdAndAliasTopologyKey, Cluster>();
        tenantIdAndTypeTopologyKeyToClusterMap = new HashMap<TenantIdAndTypeTopologyKey, List<Cluster>>();
        tenantIdToClusterMap = new HashMap<Integer, List<Cluster>>();
    }

    public static TopologyClusterModel getInstance () {
        if(topologyClusterModel == null) {
            synchronized (TopologyClusterModel.class) {
                if (topologyClusterModel == null) {
                    topologyClusterModel = new TopologyClusterModel();
                }
            }
        }

        return topologyClusterModel;
    }

    public void addCluster (int tenantId, String cartridgeType, String subscriptionAlias, Cluster cluster) {

        List<Cluster> clusters;
        writeLock.lock();

        try {
            //[Tenant Id + Subscription Alias] -> Cluster map
            tenantIdAndAliasTopologyKeyToClusterMap.put(new TenantIdAndAliasTopologyKey(tenantId, subscriptionAlias), cluster);

            //Tenant Id -> Cluster map
            clusters = tenantIdToClusterMap.get(tenantId);
            if(clusters == null) {
                clusters = new ArrayList<Cluster>();
                clusters.add(cluster);
                tenantIdToClusterMap.put(tenantId, clusters);
            } else {
                clusters.add(cluster);
            }

            //[Tenant Id + Cartridge Type] -> Cluster map
            clusters = tenantIdAndTypeTopologyKeyToClusterMap.get(new TenantIdAndTypeTopologyKey(tenantId, cartridgeType));
            if(clusters == null) {
                clusters = new ArrayList<Cluster>();
                clusters.add(cluster);
                tenantIdAndTypeTopologyKeyToClusterMap.put(new TenantIdAndTypeTopologyKey(tenantId, cartridgeType), clusters);
            } else {
                clusters.add(cluster);
            }

        } finally {
            writeLock.unlock();
        }
    }

    public Cluster getCluster (int tenantId, String subscriptionAlias) {

        readLock.lock();
        try {
            return tenantIdAndAliasTopologyKeyToClusterMap.get(new TenantIdAndAliasTopologyKey(tenantId, subscriptionAlias));

        } finally {
            readLock.unlock();
        }
    }

    public List<Cluster> getClusters (int tenantId, String cartridgeType) {

        readLock.lock();
        try {
            return tenantIdAndTypeTopologyKeyToClusterMap.get(new TenantIdAndTypeTopologyKey(tenantId, cartridgeType));

        } finally {
            readLock.unlock();
        }
    }

    public List<Cluster> getClusters (int tenantId) {

        readLock.lock();
        try {
            return tenantIdToClusterMap.get(tenantId);

        } finally {
            readLock.unlock();
        }
    }

    public void removeCluster (int tenantId, String subscriptionAlias) {
        tenantIdAndAliasTopologyKeyToClusterMap.remove(new TenantIdAndAliasTopologyKey(tenantId, subscriptionAlias));
    }

    private class TenantIdAndAliasTopologyKey {

        private int tenantId;
        private String subscriptionAlias;

        public TenantIdAndAliasTopologyKey (int tenantId, String subscriptionAlias) {

            this.tenantId = tenantId;
            this.subscriptionAlias = subscriptionAlias;
        }

        public boolean equals(Object other) {

            if(this == other) {
                return true;
            }
            if(!(other instanceof TenantIdAndAliasTopologyKey)) {
                return false;
            }

            TenantIdAndAliasTopologyKey that = (TenantIdAndAliasTopologyKey)other;
            return ((this.tenantId == that.tenantId) && (this.subscriptionAlias == that.subscriptionAlias));
        }

        public int hashCode () {

            int subscriptionAliasHashCode = 0;
            if(subscriptionAlias != null) {
                subscriptionAliasHashCode = subscriptionAlias.hashCode();
            }

            return (tenantId * 3 + subscriptionAliasHashCode * 5);
        }
    }

    public class TenantIdAndTypeTopologyKey {

        private int tenantId;
        private String subscriptionAlias;

        public TenantIdAndTypeTopologyKey (int tenantId, String subscriptionAlias) {

            this.tenantId = tenantId;
            this.subscriptionAlias = subscriptionAlias;
        }

        public boolean equals(Object other) {

            if(this == other) {
                return true;
            }
            if(!(other instanceof TenantIdAndTypeTopologyKey)) {
                return false;
            }

            TenantIdAndTypeTopologyKey that = (TenantIdAndTypeTopologyKey)other;
            return ((this.tenantId == that.tenantId) && (this.subscriptionAlias == that.subscriptionAlias));
        }

        public int hashCode () {

            int subscriptionAliasHashCode = 0;
            if(subscriptionAlias != null) {
                subscriptionAliasHashCode = subscriptionAlias.hashCode();
            }

            return (tenantId * 3 + subscriptionAliasHashCode * 5);
        }
    }
}

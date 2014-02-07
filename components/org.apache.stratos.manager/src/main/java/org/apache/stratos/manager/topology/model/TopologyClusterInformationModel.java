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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TopologyClusterInformationModel {

    private static final Log log = LogFactory.getLog(TopologyClusterInformationModel.class);

    private Map<Integer, Set<CartridgeTypeContext>> tenantIdToCartridgeTypeContextMap;
    private static TopologyClusterInformationModel topologyClusterInformationModel;

    //locks
    private static volatile ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private static volatile ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private static volatile ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private TopologyClusterInformationModel() {
        tenantIdToCartridgeTypeContextMap = new HashMap<Integer, Set<CartridgeTypeContext>>();
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

    public void addCluster (int tenantId, String cartridgeType, String subscriptionAlias, Cluster cluster) {

        Set<CartridgeTypeContext> cartridgeTypeContextSet = null;
        Set<SubscriptionAliasContext> subscriptionAliasContextSet = null;

        writeLock.lock();
        try {
            //check if a set of CartridgeTypeContext instances already exist for given tenant Id
            cartridgeTypeContextSet = tenantIdToCartridgeTypeContextMap.get(tenantId);
            if(cartridgeTypeContextSet != null) {
                CartridgeTypeContext cartridgeTypeContext = null;
                //iterate through the set
                Iterator<CartridgeTypeContext> typeCtxIterator = cartridgeTypeContextSet.iterator();
                while (typeCtxIterator.hasNext()) {
                    //see if the set contains a CartridgeTypeContext instance with the given cartridge type
                    cartridgeTypeContext = typeCtxIterator.next();
                    if (cartridgeTypeContext.getType().equals(cartridgeType)){
                        //if so, get the SubscriptionAliasContext set
                        subscriptionAliasContextSet = cartridgeTypeContext.getSubscriptionAliasContextSet();
                        break;
                    }
                }
                //check if a SubscriptionAliasContext set is not found
                if(subscriptionAliasContextSet == null) {
                    //no SubscriptionAliasContext instance
                    //create a new SubscriptionAliasContext instance
                    SubscriptionAliasContext subscriptionAliasContext = new SubscriptionAliasContext(subscriptionAlias,
                            cluster);
                    //create a SubscriptionAliasContext set
                    subscriptionAliasContextSet = new HashSet<SubscriptionAliasContext>();
                    //add the created SubscriptionAliasContext instance to SubscriptionAliasContext set
                    subscriptionAliasContextSet.add(subscriptionAliasContext);
                    //set it to the CartridgeTypeContext instance
                    cartridgeTypeContext = new CartridgeTypeContext(cartridgeType);
                    cartridgeTypeContext.setSubscriptionAliasContextSet(subscriptionAliasContextSet);
                    //add to the cartridgeTypeContextSet
                    cartridgeTypeContextSet.add(cartridgeTypeContext);

                    if (log.isDebugEnabled()) {
                        log.debug("New cluster added : " + cluster.toString());
                        Collection<Member> members = cluster.getMembers();
                        if (members != null && !members.isEmpty()) {
                            for (Member member : members) {
                                log.debug("[ " + member.getServiceName() + ", " + member.getClusterId() + ", "+ member.getMemberId()  + " ]");
                            }
                        }
                    }

                } else {
                    //iterate through the set
                    /*Iterator<SubscriptionAliasContext> aliasIterator = subscriptionAliasContextSet.iterator();
                    while (aliasIterator.hasNext()) {
                        //see if the set contains a SubscriptionAliasContext instance with the given alias
                        SubscriptionAliasContext subscriptionAliasContext = aliasIterator.next();
                        if (subscriptionAliasContext.getSubscriptionAlias().equals(subscriptionAlias)) {
                            //remove the existing one
                            aliasIterator.remove();
                            break;
                        }
                    }*/
                    // remove the existing one
                    subscriptionAliasContextSet.remove(new SubscriptionAliasContext(subscriptionAlias, null));

                    //now, add the new cluster object
                    subscriptionAliasContextSet.add(new SubscriptionAliasContext(subscriptionAlias, cluster));

                    if (log.isDebugEnabled()) {
                        log.debug("Existing cluster found, updated : " + cluster.toString());
                        Collection<Member> members = cluster.getMembers();
                        if (members != null && !members.isEmpty()) {
                            for (Member member : members) {
                                log.debug("[ " + member.getServiceName() + ", " + member.getClusterId() + ", "+ member.getMemberId()  + " ]");
                            }
                        }
                    }
                }

            } else {
                //no entries for this tenant, go from down to top creating relevant objects and populating them
                //create a new SubscriptionAliasContext instance
                SubscriptionAliasContext subscriptionAliasContext = new SubscriptionAliasContext(subscriptionAlias,
                        cluster);
                //create a SubscriptionAliasContext set
                subscriptionAliasContextSet = new HashSet<SubscriptionAliasContext>();
                //add the created SubscriptionAliasContext instance to SubscriptionAliasContext set
                subscriptionAliasContextSet.add(subscriptionAliasContext);

                //create a new CartridgeTypeContext instance
                CartridgeTypeContext cartridgeTypeContext = new CartridgeTypeContext(cartridgeType);
                //link the SubscriptionAliasContextSet to it
                cartridgeTypeContext.setSubscriptionAliasContextSet(subscriptionAliasContextSet);

                //Create CartridgeTypeContext instance
                cartridgeTypeContextSet = new HashSet<CartridgeTypeContext>();
                //link the SubscriptionAliasContext set to CartridgeTypeContext instance
                //////////////cartridgeTypeContext.setSubscriptionAliasContextSet(subscriptionAliasContextSet);
                cartridgeTypeContextSet.add(cartridgeTypeContext);

                //link the CartridgeTypeContext set to the [tenant Id -> CartridgeTypeContext] map
                tenantIdToCartridgeTypeContextMap.put(tenantId, cartridgeTypeContextSet);

                if (log.isDebugEnabled()) {
                    log.debug("New cluster added : " + cluster.toString());
                    Collection<Member> members = cluster.getMembers();
                    if (members != null && !members.isEmpty()) {
                        for (Member member : members) {
                            log.debug("[ " + member.getServiceName() + ", " + member.getClusterId() + ", "+ member.getMemberId()  + " ]");
                        }
                    }
                }
            }

        } finally {
            writeLock.unlock();
        }
    }

    public Cluster getCluster (int tenantId, String cartridgeType, String subscriptionAlias) {

        Set<CartridgeTypeContext> cartridgeTypeContextSet = null;
        Set<SubscriptionAliasContext> subscriptionAliasContextSet = null;

        readLock.lock();
        try {
            //check if a set of CartridgeTypeContext instances already exist for given tenant Id
            cartridgeTypeContextSet = tenantIdToCartridgeTypeContextMap.get(tenantId);
            if(cartridgeTypeContextSet != null) {
                CartridgeTypeContext cartridgeTypeContext = null;
                //iterate through the set
                Iterator<CartridgeTypeContext> typeCtxIterator = cartridgeTypeContextSet.iterator();
                while (typeCtxIterator.hasNext()) {
                    //see if the set contains a CartridgeTypeContext instance with the given cartridge type
                    cartridgeTypeContext = typeCtxIterator.next();
                    if (cartridgeTypeContext.getType().equals(cartridgeType)){
                        //if so, get the SubscriptionAliasContext set
                        subscriptionAliasContextSet = cartridgeTypeContext.getSubscriptionAliasContextSet();
                        break;
                    }
                }
                if(subscriptionAliasContextSet != null) {
                    //iterate through the set
                    Iterator<SubscriptionAliasContext> aliasIterator = subscriptionAliasContextSet.iterator();
                    while (aliasIterator.hasNext()) {
                        //see if the set contains a SubscriptionAliasContext instance with the given alias
                        SubscriptionAliasContext subscriptionAliasContext = aliasIterator.next();
                        if (subscriptionAliasContext.equals(new SubscriptionAliasContext(subscriptionAlias, null))) {

                            if (log.isDebugEnabled()) {
                                log.debug("Matching cluster found for tenant " + tenantId + ", type " + cartridgeType +
                                        ", subscription alias " + subscriptionAlias + ": " + subscriptionAliasContext.getCluster().toString());
                                Collection<Member> members = subscriptionAliasContext.getCluster().getMembers();
                                if (members != null && !members.isEmpty()) {
                                    for (Member member : members) {
                                        log.debug("[ " + member.getServiceName() + ", " + member.getClusterId() + ", "+ member.getMemberId()  + " ]");
                                    }
                                }
                            }

                            return subscriptionAliasContext.getCluster();
                        }
                    }
                }
            }

        } finally {
            readLock.unlock();
        }

        return null;
    }

    public Set<Cluster> getClusters (int tenantId, String cartridgeType) {

        Set<CartridgeTypeContext> cartridgeTypeContextSet = null;
        Set<SubscriptionAliasContext> subscriptionAliasContextSet = null;
        Set<Cluster> clusterSet = null;

        readLock.lock();
        try {
            cartridgeTypeContextSet = tenantIdToCartridgeTypeContextMap.get(tenantId);
            if(cartridgeTypeContextSet != null) {
                //iterate through the set
                Iterator<CartridgeTypeContext> typeCtxIterator = cartridgeTypeContextSet.iterator();
                while (typeCtxIterator.hasNext()) {
                    //iterate and get each of SubscriptionAliasContext sets
                    CartridgeTypeContext cartridgeTypeContext = typeCtxIterator.next();

                    if (cartridgeType != null) {
                        // check if CartridgeTypeContext instance matches the cartridgeType
                        if (cartridgeTypeContext.equals(new CartridgeTypeContext(cartridgeType))) {

                            subscriptionAliasContextSet = cartridgeTypeContext.getSubscriptionAliasContextSet();

                            if (subscriptionAliasContextSet != null) {
                                //iterate and convert to Cluster set
                                Iterator<SubscriptionAliasContext> aliasCtxIterator = subscriptionAliasContextSet.iterator();

                                clusterSet = new HashSet<Cluster>();
                                while (aliasCtxIterator.hasNext()) {
                                    Cluster cluster = aliasCtxIterator.next().getCluster();
                                    // add the cluster to the set
                                    clusterSet.add(cluster);

                                    if (log.isDebugEnabled()) {
                                        log.debug("Matching cluster found for tenant " + tenantId + " : " + cluster.toString());
                                        Collection<Member> members = cluster.getMembers();
                                        if (members != null && !members.isEmpty()) {
                                            for (Member member : members) {
                                                log.debug("[ " + member.getServiceName() + ", " + member.getClusterId() + ", "+ member.getMemberId()  + " ]");
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    } else {
                        // no cartridgeType specified
                        subscriptionAliasContextSet = cartridgeTypeContext.getSubscriptionAliasContextSet();

                        if (subscriptionAliasContextSet != null) {
                            //iterate and convert to Cluster set
                            Iterator<SubscriptionAliasContext> aliasCtxIterator = subscriptionAliasContextSet.iterator();

                            clusterSet = new HashSet<Cluster>();
                            while (aliasCtxIterator.hasNext()) {
                                Cluster cluster = aliasCtxIterator.next().getCluster();
                                // add the cluster to the set
                                clusterSet.add(cluster);

                                if (log.isDebugEnabled()) {
                                    log.debug("Matching cluster found for tenant " + tenantId + ", type " + cartridgeType + " : " + cluster.toString());
                                    Collection<Member> members = cluster.getMembers();
                                    if (members != null && !members.isEmpty()) {
                                        for (Member member : members) {
                                            log.debug("[ " + member.getServiceName() + ", " + member.getClusterId() + ", "+ member.getMemberId()  + " ]");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } finally {
            readLock.unlock();
        }

        return clusterSet;
    }

    public void removeCluster (int tenantId, String cartridgeType, String subscriptionAlias) {

        Set<CartridgeTypeContext> cartridgeTypeContextSet = null;
        Set<SubscriptionAliasContext> subscriptionAliasContextSet = null;

        writeLock.lock();
        try {
            //check if a set of CartridgeTypeContext instances already exist for given tenant Id
            cartridgeTypeContextSet = tenantIdToCartridgeTypeContextMap.get(tenantId);
            if(cartridgeTypeContextSet != null) {
                CartridgeTypeContext cartridgeTypeContext = null;
                //iterate through the set
                Iterator<CartridgeTypeContext> typeCtxIterator = cartridgeTypeContextSet.iterator();
                while (typeCtxIterator.hasNext()) {
                    //see if the set contains a CartridgeTypeContext instance with the given cartridge type
                    cartridgeTypeContext = typeCtxIterator.next();
                    if (cartridgeTypeContext.getType().equals(cartridgeType)){
                        //if so, get the SubscriptionAliasContext set
                        subscriptionAliasContextSet = cartridgeTypeContext.getSubscriptionAliasContextSet();
                        break;
                    }
                }
                if(subscriptionAliasContextSet != null) {
                    //iterate through the set
                    Iterator<SubscriptionAliasContext> aliasIterator = subscriptionAliasContextSet.iterator();
                    while (aliasIterator.hasNext()) {
                        //see if the set contains a SubscriptionAliasContext instance with the given alias
                        SubscriptionAliasContext subscriptionAliasContext = aliasIterator.next();
                        if (subscriptionAliasContext.getSubscriptionAlias().equals(subscriptionAlias)) {
                            //remove the existing one
                            aliasIterator.remove();

                            if (log.isDebugEnabled()) {
                                log.debug("Removed cluster for tenant " + tenantId + ", type " + cartridgeType +
                                        ", subscription alias " + subscriptionAlias);
                            }

                            break;
                        }
                    }
                }
            }

        } finally {
            writeLock.unlock();
        }
    }

    private class CartridgeTypeContext {

        private String type;
        private Set<SubscriptionAliasContext> subscriptionAliasContextSet;

        public CartridgeTypeContext (String type) {
            this.type = type;
        }

        public void setSubscriptionAliasContextSet (Set<SubscriptionAliasContext> subscriptionAliasContextSet) {
            this.subscriptionAliasContextSet = subscriptionAliasContextSet;
        }

        public String getType () {
            return type;
        }

        public Set<SubscriptionAliasContext> getSubscriptionAliasContextSet () {
            return subscriptionAliasContextSet;
        }

        public boolean equals(Object other) {

            if(this == other) {
                return true;
            }
            if(!(other instanceof CartridgeTypeContext)) {
                return false;
            }

            CartridgeTypeContext that = (CartridgeTypeContext)other;
            return this.type.equals(that.type);
        }

        public int hashCode () {
            return type.hashCode();
        }
    }

    private class SubscriptionAliasContext {

        private String subscriptionAlias;
        private Cluster cluster;

        public SubscriptionAliasContext(String subscriptionAlias, Cluster cluster) {
            this.subscriptionAlias = subscriptionAlias;
            this.cluster = cluster;
        }

        public String getSubscriptionAlias () {
            return subscriptionAlias;
        }

        public Cluster getCluster () {
            return cluster;
        }

        public boolean equals(Object other) {

            if(this == other) {
                return true;
            }
            if(!(other instanceof SubscriptionAliasContext)) {
                return false;
            }

            SubscriptionAliasContext that = (SubscriptionAliasContext)other;
            return this.subscriptionAlias.equals(that.subscriptionAlias);
        }

        public int hashCode () {
            return subscriptionAlias.hashCode();
        }
    }
}

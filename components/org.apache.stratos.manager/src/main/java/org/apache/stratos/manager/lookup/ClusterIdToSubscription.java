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

package org.apache.stratos.manager.lookup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.subscription.CartridgeSubscription;

import java.io.Serializable;
import java.util.*;

public class ClusterIdToSubscription implements Serializable {

    private static final Log log = LogFactory.getLog(ClusterIdToSubscription.class);

    // Map: Cluster Id (Domain) -> Set<CartridgeSubscription>
    private Map<String, Set<CartridgeSubscription>> clusterIdToCartridgeSubscription;

    public ClusterIdToSubscription() {
        clusterIdToCartridgeSubscription = new HashMap<String, Set<CartridgeSubscription>>();
    }

    public void addSubscription (CartridgeSubscription cartridgeSubscription) {

        //clusterIdToCartridgeSubscription.put(cartridgeSubscription.getClusterDomain(), cartridgeSubscription);
        String clusterDomain = cartridgeSubscription.getClusterDomain();
        if (clusterIdToCartridgeSubscription.containsKey(clusterDomain)) {
            Set<CartridgeSubscription> existingSubscriptions = clusterIdToCartridgeSubscription.get(clusterDomain);
            // if an existing subscription is present, remove it
            if(existingSubscriptions.remove(cartridgeSubscription)){
                if(log.isDebugEnabled()) {
                    log.debug("Removed the existing Cartridge Subscription for cluster id " + clusterDomain + " in [Cluster Id -> Set<CartridgeSubscription>] map");
                }
            }
            // add or update
            existingSubscriptions.add(cartridgeSubscription);
            if(log.isDebugEnabled()) {
                log.debug("Added Cartridge Subscription for cluster id " + clusterDomain + " in [Cluster Id -> Set<CartridgeSubscription>] map");
            }

        } else {
            // create a new set and add it
            Set<CartridgeSubscription> subscriptions = new HashSet<CartridgeSubscription>();
            subscriptions.add(cartridgeSubscription);
            clusterIdToCartridgeSubscription.put(clusterDomain, subscriptions);
        }
    }

    public Set<CartridgeSubscription> getSubscription (String clusterId) {

        return clusterIdToCartridgeSubscription.get(clusterId);
    }

    public void removeSubscription (String clusterId, String subscriptionAlias) {

        /*if (clusterIdToCartridgeSubscription.remove(clusterId) != null) {
            if (log.isDebugEnabled()) {
                log.debug("Deleted the subscription for cluster " + clusterId + " from [Cluster Id -> CartridgeSubscription] map");
            }
        }*/
        // remove Subscription from clusterIdToCartridgeSubscription map
        Set<CartridgeSubscription> existingSubscriptions = clusterIdToCartridgeSubscription.get(clusterId);
        if (existingSubscriptions != null && !existingSubscriptions.isEmpty()) {
            // iterate through the set
            Iterator<CartridgeSubscription> iterator = existingSubscriptions.iterator();
            while (iterator.hasNext()) {
                CartridgeSubscription cartridgeSubscription = iterator.next();
                // if a matching CartridgeSubscription is found, remove
                if (cartridgeSubscription.getAlias().equals(subscriptionAlias)) {
                    iterator.remove();
                    if (log.isDebugEnabled()) {
                        log.debug("Deleted the subscription for cluster id " + clusterId + " from [Cluster Id -> Set<CartridgeSubscription>] map");
                    }
                    break;
                }
            }
        }

        // if the Subscriptions set is empty now, remove it from cartridgeTypeToSubscriptions map
        if (existingSubscriptions != null && existingSubscriptions.isEmpty()) {
            clusterIdToCartridgeSubscription.remove(clusterId);
            if (log.isDebugEnabled()) {
                log.debug("Deleted the subscriptions set for cluster id " + clusterId + " from [Cluster Id -> Set<CartridgeSubscription>] map");
            }
        }
    }
}

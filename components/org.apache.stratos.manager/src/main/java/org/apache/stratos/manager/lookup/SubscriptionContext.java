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

public class SubscriptionContext implements Serializable {

    private static final Log log = LogFactory.getLog(SubscriptionContext.class);

    // Map: Cartridge Type -> Set<CartridgeSubscription>
    private Map<String, Set<CartridgeSubscription>> cartridgeTypeToSubscriptions;

    // Map: Subscription Alias -> CartridgeSubscription
    private Map<String, CartridgeSubscription> aliasToSubscription;

    public SubscriptionContext () {

        cartridgeTypeToSubscriptions = new HashMap<String, Set<CartridgeSubscription>>();
        aliasToSubscription = new HashMap<String, CartridgeSubscription>();
    }

    public void addSubscription (CartridgeSubscription cartridgeSubscription) {

        String cartridgeType = cartridgeSubscription.getType();
        if (cartridgeTypeToSubscriptions.containsKey(cartridgeType)) {
            Set<CartridgeSubscription> existingSubscriptions = cartridgeTypeToSubscriptions.get(cartridgeType);
            // if an existing subscription is present, remove it
            if(existingSubscriptions.remove(cartridgeSubscription)){
                if(log.isDebugEnabled()) {
                    log.debug("Removed the existing Cartridge Subscription for type " + cartridgeType + ", alias " + cartridgeSubscription.getAlias() +
                    " in [Cartridge Type -> Set<CartridgeSubscription>] map");
                }
            }
            // add or update
            existingSubscriptions.add(cartridgeSubscription);
            if(log.isDebugEnabled()) {
                log.debug("Added Cartridge Subscription for type " + cartridgeType + ", alias " + cartridgeSubscription.getAlias() +
                " in [Cartridge Type -> Set<CartridgeSubscription>] map");
            }

        } else {
            // create a new set and add it
            Set<CartridgeSubscription> subscriptions = new HashSet<CartridgeSubscription>();
            subscriptions.add(cartridgeSubscription);
            cartridgeTypeToSubscriptions.put(cartridgeType, subscriptions);
        }

        // put Subscription to aliasToSubscription map
        if (aliasToSubscription.put(cartridgeSubscription.getAlias(), cartridgeSubscription) != null) {
            if(log.isDebugEnabled()) {
                log.debug("Overwrote the existing Cartridge Subscription for alias " + cartridgeSubscription.getAlias() +
                " in [Subscription Alias -> CartridgeSubscription] map");
            }
        }
    }

    public Collection<CartridgeSubscription> getSubscriptions () {

        return aliasToSubscription.values();
    }

    public Collection<CartridgeSubscription> getSubscriptionsOfType (String cartridgeType) {

        return cartridgeTypeToSubscriptions.get(cartridgeType);
    }



    public CartridgeSubscription getSubscriptionForAlias (String subscriptionAlias) {

        return aliasToSubscription.get(subscriptionAlias);
    }

    public void deleteSubscription (String type, String subscriptionAlias) {

        // remove Subscription from cartridgeTypeToSubscriptions map
        Set<CartridgeSubscription> existingSubscriptions = cartridgeTypeToSubscriptions.get(type);
        if (existingSubscriptions != null && !existingSubscriptions.isEmpty()) {
            // iterate through the set
            Iterator<CartridgeSubscription> iterator = existingSubscriptions.iterator();
            while (iterator.hasNext()) {
                CartridgeSubscription cartridgeSubscription = iterator.next();
                // if a matching CartridgeSubscription is found, remove
                if (cartridgeSubscription.getAlias().equals(subscriptionAlias)) {
                    iterator.remove();
                    if (log.isDebugEnabled()) {
                        log.debug("Deleted the subscription for alias " + subscriptionAlias + " and type " + type + " from [Type -> Set<CartridgeSubscription>] map");
                    }
                    break;
                }
            }
        }

        // if the Subscriptions set is empty now, remove it from cartridgeTypeToSubscriptions map
        if (existingSubscriptions == null || existingSubscriptions.isEmpty()) {
            cartridgeTypeToSubscriptions.remove(type);
            if (log.isDebugEnabled()) {
                log.debug("Deleted the subscriptions set for type " + type + " from [Type -> Set<CartridgeSubscription>] map");
            }
        }

        // remove from aliasToSubscription map
        if (aliasToSubscription.remove(subscriptionAlias) != null) {
            if (log.isDebugEnabled()) {
                log.debug("Deleted the subscription for alias " + subscriptionAlias + " from [Alias -> CartridgeSubscription] map");
            }
        }
    }

    public boolean isEmpty () {

        return cartridgeTypeToSubscriptions.isEmpty() && aliasToSubscription.isEmpty();
    }

}

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

package org.apache.stratos.adc.mgt.lookup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SubscriptionContext implements Serializable {

    private static final Log log = LogFactory.getLog(SubscriptionContext.class);

    // Map: Cartridge Type -> Set<CartridgeSubscription>
    private Map<String, Set<CartridgeSubscription>> cartridgeTypeToSubscriptions;

    // Map: Subscription Alias -> CartridgeSubscription
    private Map<String, CartridgeSubscription> aliasToSubscription;

    //locks
    private static volatile ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private static volatile ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    public SubscriptionContext () {

        cartridgeTypeToSubscriptions = new HashMap<String, Set<CartridgeSubscription>>();
        aliasToSubscription = new HashMap<String, CartridgeSubscription>();
    }

    public void addSubscription (CartridgeSubscription cartridgeSubscription) {

        writeLock.lock();
        try {
            add(cartridgeSubscription);

        } finally {
            writeLock.unlock();
        }
    }

    private void add (CartridgeSubscription cartridgeSubscription) {

        String cartridgeType = cartridgeSubscription.getType();
        if (cartridgeTypeToSubscriptions.containsKey(cartridgeType)) {
            Set<CartridgeSubscription> existingSubscriptions = cartridgeTypeToSubscriptions.get(cartridgeType);
            // if an existing subscription is present, remove it
            if(existingSubscriptions.remove(cartridgeSubscription)){
                if(log.isDebugEnabled()) {
                    log.debug("Removed the existing Cartridge Subscription for type " + cartridgeType + ", alias " + cartridgeSubscription.getAlias());
                }
            }
            // add or update
            existingSubscriptions.add(cartridgeSubscription);

        } else {
            // create a new set and add it
            Set<CartridgeSubscription> subscriptions = new HashSet<CartridgeSubscription>();
            subscriptions.add(cartridgeSubscription);
            cartridgeTypeToSubscriptions.put(cartridgeType, subscriptions);
        }

        // put to aliasToSubscription map
        if (aliasToSubscription.put(cartridgeSubscription.getAlias(), cartridgeSubscription) != null) {
            if(log.isDebugEnabled()) {
                log.debug("Overwrote the existing Cartridge Subscription for alias " + cartridgeSubscription.getAlias());
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

}

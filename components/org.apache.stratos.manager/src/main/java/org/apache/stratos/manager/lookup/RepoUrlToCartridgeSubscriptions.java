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

import java.util.*;

public class RepoUrlToCartridgeSubscriptions {

    private static final Log log = LogFactory.getLog(RepoUrlToCartridgeSubscriptions.class);

    // Map: Repository URL -> Set<CartridgeSubscription>
    private Map<String, Set<CartridgeSubscription>> repoUrlToCartridgeSubscriptions;

    public RepoUrlToCartridgeSubscriptions() {
        repoUrlToCartridgeSubscriptions = new HashMap<String, Set<CartridgeSubscription>>();
    }

    public void addSubscription (CartridgeSubscription cartridgeSubscription) {

        if (cartridgeSubscription.getRepository() == null) {
            if (log.isDebugEnabled()) {
                log.debug("No repository URL found for Cartridge Subscription for alias " + cartridgeSubscription.getAlias() +
                        ", not adding to [Repo URL -> Set<CartridgeSubscription>] map");
            }
            return;
        }

        String repoUrl = cartridgeSubscription.getRepository().getUrl();

        if (repoUrlToCartridgeSubscriptions.containsKey(repoUrl)) {
            Set<CartridgeSubscription> existingSubscriptions = repoUrlToCartridgeSubscriptions.get(repoUrl);
            // if an existing subscription is present, remove it
            if(existingSubscriptions.remove(cartridgeSubscription)){
                if(log.isDebugEnabled()) {
                    log.debug("Removed the existing Cartridge Subscription for repo URL " + repoUrl + " in [Repo URL -> Set<CartridgeSubscription>] map");
                }
            }
            // add or update
            existingSubscriptions.add(cartridgeSubscription);
            if(log.isDebugEnabled()) {
                log.debug("Added Cartridge Subscription for repo URL " + repoUrl + " in [Repo URL -> Set<CartridgeSubscription>] map");
            }

        } else {
            // create a new set and add it
            Set<CartridgeSubscription> subscriptions = new HashSet<CartridgeSubscription>();
            subscriptions.add(cartridgeSubscription);
            repoUrlToCartridgeSubscriptions.put(repoUrl, subscriptions);
            if(log.isDebugEnabled()) {
                log.debug("Added Cartridge Subscription for repo URL " + repoUrl + " in [Repo URL -> Set<CartridgeSubscription>] map");
            }
        }
    }

    public Set<CartridgeSubscription> getSubscriptions(String repoUrl) {

        return repoUrlToCartridgeSubscriptions.get(repoUrl);
    }

    public void removeSubscription (String repoUrl, String subscriptionAlias) {

        if (repoUrl == null) {
            if (log.isDebugEnabled()) {
                log.debug("Repository URL is null for subscription alias " + subscriptionAlias);
            }
            return;
        }

        // remove Subscription from repoUrlToCartridgeSubscriptions map
        Set<CartridgeSubscription> existingSubscriptions = repoUrlToCartridgeSubscriptions.get(repoUrl);
        if (existingSubscriptions != null && !existingSubscriptions.isEmpty()) {
            // iterate through the set
            Iterator<CartridgeSubscription> iterator = existingSubscriptions.iterator();
            while (iterator.hasNext()) {
                CartridgeSubscription cartridgeSubscription = iterator.next();
                // if a matching CartridgeSubscription is found, remove
                if (cartridgeSubscription.getAlias().equals(subscriptionAlias)) {
                    iterator.remove();
                    if (log.isDebugEnabled()) {
                        log.debug("Deleted the subscription for repo URL " + repoUrl + " from [Repo URL -> Set<CartridgeSubscription>] map");
                    }
                    break;
                }
            }
        }

        // if the Subscriptions set is empty now, remove it from cartridgeTypeToSubscriptions map
        if (existingSubscriptions == null || existingSubscriptions.isEmpty()) {
            repoUrlToCartridgeSubscriptions.remove(repoUrl);
            if (log.isDebugEnabled()) {
                log.debug("Deleted the subscriptions set for repo URL " + repoUrl + " from [Repo URL -> Set<CartridgeSubscription>] map");
            }
        }
    }
}

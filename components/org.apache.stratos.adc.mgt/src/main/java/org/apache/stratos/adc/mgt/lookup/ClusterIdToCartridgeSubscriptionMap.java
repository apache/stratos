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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ClusterIdToCartridgeSubscriptionMap implements Serializable {

    private static final Log log = LogFactory.getLog(ClusterIdToCartridgeSubscriptionMap.class);

    private Map<String, CartridgeSubscription> clusterItToCartridgeSubscrptionMap;

    public ClusterIdToCartridgeSubscriptionMap () {
        clusterItToCartridgeSubscrptionMap = new HashMap<String, CartridgeSubscription>();
    }

    public Map<String, CartridgeSubscription> getSubscriptionAliasToCartridgeSubscriptionMap() {
        return clusterItToCartridgeSubscrptionMap;
    }

    public void addSubscription(String clusterId, CartridgeSubscription cartridgeSubscription) {

        if(clusterItToCartridgeSubscrptionMap.put(clusterId, cartridgeSubscription) != null) {
            log.info("Overwrote the previous CartridgeSubscription value for cluster " + clusterId);
        }
    }

    public boolean isEmpty () {
        return clusterItToCartridgeSubscrptionMap.isEmpty();
    }

    public void removeSubscription (String clusterId) {

        if(clusterItToCartridgeSubscrptionMap.remove(clusterId) == null) {
            log.info("No CartridgeSubscription entry found for cluster " + clusterId);
        }
    }

    public CartridgeSubscription getCartridgeSubscription (String clusterId) {
        return clusterItToCartridgeSubscrptionMap.get(clusterId);
    }

    public Collection<CartridgeSubscription> getCartridgeSubscriptions () {
        return clusterItToCartridgeSubscrptionMap.values();
    }
}

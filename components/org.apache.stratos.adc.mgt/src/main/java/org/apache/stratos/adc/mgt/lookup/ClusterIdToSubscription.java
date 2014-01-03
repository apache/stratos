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
import java.util.HashMap;
import java.util.Map;

public class ClusterIdToSubscription implements Serializable {

    private static final Log log = LogFactory.getLog(ClusterIdToSubscription.class);

    // Map: Cluster Id (Domain) -> CartridgeSubscription
    private Map<String, CartridgeSubscription> clusterIdToCartridgeSubscription;

    public ClusterIdToSubscription() {
        clusterIdToCartridgeSubscription = new HashMap<String, CartridgeSubscription>();
    }

    public void addSubscription (CartridgeSubscription cartridgeSubscription) {

        clusterIdToCartridgeSubscription.put(cartridgeSubscription.getClusterDomain(), cartridgeSubscription);
    }

    public CartridgeSubscription getSubscription (String clusterId) {

        return clusterIdToCartridgeSubscription.get(clusterId);
    }

    public void removeSubscription (String clusterId) {

        if (clusterIdToCartridgeSubscription.remove(clusterId) != null) {
            if (log.isDebugEnabled()) {
                log.debug("Deleted the subscription for cluster " + clusterId + " from [Cluster Id -> CartridgeSubscription] map");
            }
        }
    }
}

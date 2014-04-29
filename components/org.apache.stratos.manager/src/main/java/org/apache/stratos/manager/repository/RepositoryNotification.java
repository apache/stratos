/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.manager.repository;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.publisher.InstanceNotificationPublisher;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscription.CartridgeSubscription;

import java.util.Set;

public class RepositoryNotification {

    private static final Log log = LogFactory.getLog(RepositoryNotification.class);

    public void updateRepository(String url) {

        if ( StringUtils.isNotBlank(url))  {

            Set<CartridgeSubscription> cartridgeSubscriptions = new DataInsertionAndRetrievalManager().
                    getCartridgeSubscriptionForRepository(url);

            if (cartridgeSubscriptions == null || cartridgeSubscriptions.isEmpty()) {
                // No subscriptions, return
                if (log.isDebugEnabled()) {
                    log.debug("No subscription information found for repo url : " + url);
                }

                return;
            }

            for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {
            	updateRepository(cartridgeSubscription);
            }
        }
    }
    
	public void updateRepository(CartridgeSubscription cartridgeSubscription) {
		if (cartridgeSubscription.getRepository() != null) {
			InstanceNotificationPublisher publisher = new InstanceNotificationPublisher();
			publisher.sendArtifactUpdateEvent(cartridgeSubscription.getRepository(),
					cartridgeSubscription.getCluster().getClusterDomain(),
					String.valueOf(cartridgeSubscription.getSubscriber().getTenantId()));

			if (log.isDebugEnabled()) {
				log.debug("Git pull request from " + cartridgeSubscription.getRepository()
						+ "repository, for the tenant "
						+ String.valueOf(cartridgeSubscription.getSubscriber().getTenantId()));
			}

		} else {
			if (log.isDebugEnabled()) {
				log.debug("No repository found for subscription with alias: " + cartridgeSubscription.getAlias()
						+ ", type: " + cartridgeSubscription.getType() + ". Not sending the Artifact Updated event");
			}
		}
	}
}

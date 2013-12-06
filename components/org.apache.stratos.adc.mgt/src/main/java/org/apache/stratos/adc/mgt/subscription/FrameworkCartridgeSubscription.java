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

package org.apache.stratos.adc.mgt.subscription;

import org.apache.stratos.adc.mgt.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.adc.mgt.dto.Policy;
import org.apache.stratos.adc.mgt.exception.*;
import org.apache.stratos.adc.mgt.payload.PayloadArg;
import org.apache.stratos.adc.mgt.repository.Repository;
import org.apache.stratos.adc.mgt.subscriber.Subscriber;
import org.apache.stratos.adc.mgt.utils.ApplicationManagementUtil;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;

import java.util.Properties;

public class FrameworkCartridgeSubscription extends AbstractCartridgeSubscription {

    /**
     * Constructor
     *
     * @param cartridgeInfo CartridgeInfo subscription
     */
    public FrameworkCartridgeSubscription(CartridgeInfo cartridgeInfo) {
        super(cartridgeInfo);
    }

    @Override
    public void createSubscription(Subscriber subscriber, String alias, Policy autoscalingPolicy,
                                   Repository repository) throws
            InvalidCartridgeAliasException, DuplicateCartridgeAliasException, ADCException,
            RepositoryCredentialsRequiredException, RepositoryTransportException, UnregisteredCartridgeException,
            AlreadySubscribedException, RepositoryRequiredException, InvalidRepositoryException, PolicyException {

        super.createSubscription(subscriber, alias, autoscalingPolicy, repository);
        subscriptionTenancyBehaviour.createSubscription();
    }

    @Override
    public CartridgeSubscriptionInfo registerSubscription(Properties properties) throws ADCException, UnregisteredCartridgeException {

        subscriptionTenancyBehaviour.registerSubscription(null);

        return ApplicationManagementUtil.createCartridgeSubscription(getCartridgeInfo(), getAutoscalingPolicy(),
                getType(), getAlias(), getSubscriber().getTenantId(), getSubscriber().getTenantDomain(),
                getRepository(), getCluster().getHostName(), getCluster().getClusterDomain(), getCluster().getClusterSubDomain(),
                getCluster().getMgtClusterDomain(), getCluster().getMgtClusterSubDomain(), null, "PENDING", getSubscriptionKey());
    }

    @Override
    public void removeSubscription() throws ADCException, NotSubscribedException {

        subscriptionTenancyBehaviour.removeSubscription();
        super.cleanupSubscription();
    }

    public PayloadArg createPayloadParameters () throws ADCException {

        PayloadArg payloadArg = super.createPayloadParameters();
        return subscriptionTenancyBehaviour.createPayloadParameters(payloadArg);
    }
}

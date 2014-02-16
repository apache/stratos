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

package org.apache.stratos.manager.subscription;

import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.pojo.Properties;
import org.apache.stratos.manager.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.manager.exception.*;
import org.apache.stratos.manager.lb.category.LoadBalancerCategory;
import org.apache.stratos.manager.repository.Repository;
import org.apache.stratos.manager.subscriber.Subscriber;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionTenancyBehaviour;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;

public class LBCartridgeSubscription extends CartridgeSubscription {

    private LoadBalancerCategory loadBalancerCategory;

    /**
     * Constructor
     *
     * @param cartridgeInfo CartridgeInfo instance
     * @param subscriptionTenancyBehaviour SubscriptionTenancyBehaviour instance
     * @param loadBalancerCategory LoadBalancerCategory instance
     */
    public LBCartridgeSubscription(CartridgeInfo cartridgeInfo, SubscriptionTenancyBehaviour
            subscriptionTenancyBehaviour, LoadBalancerCategory loadBalancerCategory) {

        super(cartridgeInfo, subscriptionTenancyBehaviour);
        setLoadBalancerCategory(loadBalancerCategory);
    }

    public void createSubscription (Subscriber subscriber, String alias, String autoscalingPolicy,
                                    String deploymentPolicyName, Repository repository)
            throws ADCException, PolicyException, UnregisteredCartridgeException, InvalidCartridgeAliasException,
            DuplicateCartridgeAliasException, RepositoryRequiredException, AlreadySubscribedException,
            RepositoryCredentialsRequiredException, InvalidRepositoryException, RepositoryTransportException {

        setSubscriber(subscriber);
        setAlias(alias);
        setAutoscalingPolicyName(autoscalingPolicy);
        setDeploymentPolicyName(deploymentPolicyName);
        setRepository(repository);

        setPayloadData(getLoadBalancerCategory().create(getAlias(), getCluster(), getSubscriber(), getRepository(), getCartridgeInfo(),
                getSubscriptionKey(), getCustomPayloadEntries()));
    }

    @Override
    public CartridgeSubscriptionInfo registerSubscription(Properties properties) throws ADCException, UnregisteredCartridgeException {

        getLoadBalancerCategory().register (getCartridgeInfo(), getCluster(), getPayloadData(), getAutoscalingPolicyName(),
                getDeploymentPolicyName(), properties);

        return ApplicationManagementUtil.createCartridgeSubscription(getCartridgeInfo(), getAutoscalingPolicyName(),
                getType(), getAlias(), getSubscriber().getTenantId(), getSubscriber().getTenantDomain(),
                getRepository(), getCluster().getHostName(), getCluster().getClusterDomain(), getCluster().getClusterSubDomain(),
                getCluster().getMgtClusterDomain(), getCluster().getMgtClusterSubDomain(), null, getSubscriptionStatus(), getSubscriptionKey());
    }

    @Override
    public void removeSubscription() throws ADCException, NotSubscribedException {

        getLoadBalancerCategory().remove(getCluster().getClusterDomain(), getAlias());;
    }

    public Repository manageRepository (String repoURL, String repoUserName, String repoUserPassword,
                                        boolean privateRepo, String cartridgeAlias, CartridgeInfo cartridgeInfo,
                                        String tenantDomain) {

        //no repository for data cartridge instances
        return null;
    }

    public LoadBalancerCategory getLoadBalancerCategory() {
        return loadBalancerCategory;
    }

    public void setLoadBalancerCategory(LoadBalancerCategory loadBalancerCategory) {
        this.loadBalancerCategory = loadBalancerCategory;
    }
}

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.stub.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.manager.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.AlreadySubscribedException;
import org.apache.stratos.manager.exception.DuplicateCartridgeAliasException;
import org.apache.stratos.manager.exception.InvalidCartridgeAliasException;
import org.apache.stratos.manager.exception.InvalidRepositoryException;
import org.apache.stratos.manager.exception.NotSubscribedException;
import org.apache.stratos.manager.exception.PolicyException;
import org.apache.stratos.manager.exception.RepositoryCredentialsRequiredException;
import org.apache.stratos.manager.exception.RepositoryRequiredException;
import org.apache.stratos.manager.exception.RepositoryTransportException;
import org.apache.stratos.manager.exception.UnregisteredCartridgeException;
import org.apache.stratos.manager.lb.category.LoadBalancerCategory;
import org.apache.stratos.manager.repository.Repository;
import org.apache.stratos.manager.subscriber.Subscriber;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionTenancyBehaviour;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;

import java.util.Set;

public class LBCartridgeSubscription extends CartridgeSubscription {

    private LoadBalancerCategory loadBalancerCategory;
    private static Log log = LogFactory.getLog(LBCartridgeSubscription.class);

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
        // If LB subscription is for MT service, payload data should not be set
//        if(!loadBalancerCategory.isLoadBalancedServiceMultiTenant()) {
//        	setPayloadData(getLoadBalancerCategory().create(getAlias(), getCluster(), getSubscriber(), getRepository(), getCartridgeInfo(),
//                    getSubscriptionKey(), getCustomPayloadEntries()));
//        }
    }

    
    @Override
    public CartridgeSubscriptionInfo registerSubscription(Properties properties) throws ADCException, UnregisteredCartridgeException {
    	//if(!loadBalancerCategory.isLoadBalancedServiceMultiTenant()) {
    		//if(log.isDebugEnabled()) {
    		 //log.debug("Loadbalanced service is single tenant.");
    		//}
    		getLoadBalancerCategory().register (getCartridgeInfo(), getCluster(), getPayloadData(), getAutoscalingPolicyName(),
    	                getDeploymentPolicyName(), properties);
    	//}


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
                                        boolean privateRepo) {

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

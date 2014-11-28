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
package org.apache.stratos.manager.subscription.filter;

import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.stub.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.stub.pojo.LoadbalancerConfig;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.cloud.controller.stub.pojo.Property;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.AlreadySubscribedException;
import org.apache.stratos.manager.exception.DuplicateCartridgeAliasException;
import org.apache.stratos.manager.exception.InvalidCartridgeAliasException;
import org.apache.stratos.manager.exception.InvalidRepositoryException;
import org.apache.stratos.manager.exception.PolicyException;
import org.apache.stratos.manager.exception.RepositoryCredentialsRequiredException;
import org.apache.stratos.manager.exception.RepositoryRequiredException;
import org.apache.stratos.manager.exception.RepositoryTransportException;
import org.apache.stratos.manager.exception.UnregisteredCartridgeException;
import org.apache.stratos.manager.lb.category.DefaultLoadBalancerCategory;
import org.apache.stratos.manager.lb.category.ExistingLoadBalancerCategory;
import org.apache.stratos.manager.lb.category.LBDataContext;
import org.apache.stratos.manager.lb.category.LoadBalancerCategory;
import org.apache.stratos.manager.lb.category.ServiceLevelLoadBalancerCategory;
import org.apache.stratos.manager.manager.CartridgeSubscriptionManager;
import org.apache.stratos.manager.publisher.CartridgeSubscriptionDataPublisher;
import org.apache.stratos.manager.repository.Repository;
import org.apache.stratos.manager.subscriber.Subscriber;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.subscription.SubscriptionData;
import org.apache.stratos.manager.subscription.factory.CartridgeSubscriptionFactory;
import org.apache.stratos.manager.subscription.utils.CartridgeSubscriptionUtils;
import org.apache.stratos.manager.utils.CartridgeConstants;

/**
 *	Responsible for making a subscription for a Load Balancer Cluster,
 *	when required.
 */
public class LBCreationSubscriptionFilter implements SubscriptionFilter {
	
	private static Log log = LogFactory.getLog(LBCreationSubscriptionFilter.class);

	@Override
	public Properties execute(CartridgeInfo cartridgeInfo, SubscriptionData subscriptionData) throws ADCException {
		
		LBDataContext lbDataCtxt = null;
        CartridgeSubscription lbCartridgeSubscription = null;
        Properties lbCartridgeSubscriptionProperties  = new Properties();
        Properties filterProperties = new Properties();

		try {
			// get lb config reference
			LoadbalancerConfig lbConfig = cartridgeInfo.getLbConfig();
			if (lbConfig == null || lbConfig.getProperties() == null) {
				// no LB ref
				if (log.isDebugEnabled()) {
					log.debug("This Service does not require a load balancer. "
							+ "[Service Name] "
							+ subscriptionData.getCartridgeType());
				}

			} else {

				// LB ref found, get relevant LB Context data
				lbDataCtxt = CartridgeSubscriptionUtils
						.getLoadBalancerDataContext(
								subscriptionData.getTenantId(),
								subscriptionData.getCartridgeType(),
								subscriptionData.getDeploymentPolicyName(),
								lbConfig);

				// subscribe to LB
				lbCartridgeSubscription = subscribeToLB(subscriptionData,
						lbDataCtxt, cartridgeInfo);

                if (lbDataCtxt.getLbProperperties() != null && !lbDataCtxt.getLbProperperties().isEmpty()) {
                    List<Property> lbProperperties = lbDataCtxt.getLbProperperties();
                    lbCartridgeSubscriptionProperties.setProperties(lbProperperties.toArray(new Property[lbProperperties.size()]));
                    for (Property property : lbProperperties){
                        if (StratosConstants.LOAD_BALANCER_REF.equals(property.getName())) {
                            filterProperties.addProperties(property);
                        }
                    }
                }

				if (lbCartridgeSubscription != null) {
					// determine the LB cluster id, if available
					Property lbClusterIdProp = new Property();
					lbClusterIdProp.setName(CartridgeConstants.LB_CLUSTER_ID);
					lbClusterIdProp.setValue(lbCartridgeSubscription
							.getClusterDomain());
					lbCartridgeSubscriptionProperties
							.addProperties(lbClusterIdProp);
                    filterProperties.addProperties(lbClusterIdProp);

					// register LB cartridge subscription
					if (log.isDebugEnabled()) {
						log.debug(" Registering LB Cartridge subscription ");
					}
					CartridgeSubscriptionManager.registerCartridgeSubscription(
							lbCartridgeSubscription,
							lbCartridgeSubscriptionProperties,
							subscriptionData.getPersistence());
				}
			}
		} catch (Exception e) {
        	log.error(e.getMessage(), e);
        	throw new ADCException(e.getMessage(), e);
        }
        
        return filterProperties;
	}
	
	private CartridgeSubscription subscribeToLB (SubscriptionData subscriptionData, LBDataContext lbDataContext,
            CartridgeInfo serviceCartridgeInfo)

            throws ADCException, InvalidCartridgeAliasException,
            DuplicateCartridgeAliasException, PolicyException, UnregisteredCartridgeException, RepositoryRequiredException, RepositoryCredentialsRequiredException,
            RepositoryTransportException, AlreadySubscribedException, InvalidRepositoryException {

        
        if (lbDataContext.getLbCategory() == null || lbDataContext.getLbCategory().equals(StratosConstants.NO_LOAD_BALANCER)) {
            // no load balancer subscription required generate SubscriptionKey
            log.info("No LB subscription required for the Subscription with alias: " + subscriptionData.getCartridgeAlias() + ", type: " +
                    subscriptionData.getCartridgeType());
            return null;
        }

        LoadBalancerCategory loadBalancerCategory = null;

        String lbAlias = "lb" + lbDataContext.getLbCartridgeInfo().getType() + new Random().nextInt();

        if (lbDataContext.getLbCategory().equals(StratosConstants.EXISTING_LOAD_BALANCERS)) {
            loadBalancerCategory = new ExistingLoadBalancerCategory();

        } else if (lbDataContext.getLbCategory().equals(StratosConstants.DEFAULT_LOAD_BALANCER)) {
            loadBalancerCategory = new DefaultLoadBalancerCategory();

        } else if (lbDataContext.getLbCategory().equals(StratosConstants.SERVICE_AWARE_LOAD_BALANCER)) {
            loadBalancerCategory = new ServiceLevelLoadBalancerCategory();
        }

        if (loadBalancerCategory == null) {
            throw new ADCException("The given Load Balancer category " + lbDataContext.getLbCategory() + " not found");
        }

        if(lbDataContext.getLbCartridgeInfo().getMultiTenant()) {
            throw new ADCException("LB Cartridge must be single tenant");
        }
        // Set the load balanced service type
        loadBalancerCategory.setLoadBalancedServiceType(subscriptionData.getCartridgeType());
        
		// Set if the load balanced service is multi tenant or not
        loadBalancerCategory.setLoadBalancedServiceMultiTenant(serviceCartridgeInfo.getMultiTenant());

        // set the relevant deployment policy
        loadBalancerCategory.setDeploymentPolicyName(lbDataContext.getDeploymentPolicy());

        // Create the CartridgeSubscription instance
        CartridgeSubscription cartridgeSubscription = CartridgeSubscriptionFactory.getLBCartridgeSubscriptionInstance(lbDataContext, loadBalancerCategory);

        // Generate and set the key
        String subscriptionKey = CartridgeSubscriptionUtils.generateSubscriptionKey();
        cartridgeSubscription.setSubscriptionKey(subscriptionKey);

        // Create repository
        Repository repository = cartridgeSubscription.manageRepository(null, "",  "", false);

        // Create subscriber
        Subscriber subscriber = new Subscriber(subscriptionData.getTenantAdminUsername(), subscriptionData.getTenantId(), subscriptionData.getTenantDomain());

        // create subscription
        cartridgeSubscription.createSubscription(subscriber, lbAlias, lbDataContext.getAutoscalePolicy(),
                lbDataContext.getDeploymentPolicy(), repository);

        // add LB category to the payload
        if (cartridgeSubscription.getPayloadData() != null) {
            cartridgeSubscription.getPayloadData().add(CartridgeConstants.LB_CATEGORY, lbDataContext.getLbCategory());
        }

                // publishing to bam
             	CartridgeSubscriptionDataPublisher.publish(subscriptionData.getTenantId(),
             				subscriptionData.getTenantAdminUsername(), lbAlias,
             				lbDataContext.getLbCartridgeInfo().getType(),
             				subscriptionData.getRepositoryURL(),
             				serviceCartridgeInfo.getMultiTenant(),
             				lbDataContext.getDeploymentPolicy(),
             				lbDataContext.getAutoscalePolicy(),
             				cartridgeSubscription.getCluster().getClusterDomain(), 
             				cartridgeSubscription.getHostName(),
             				cartridgeSubscription.getMappedDomain(), "Subscribed");
        
        log.info("Tenant [" + subscriptionData.getTenantId() + "] with username [" + subscriptionData.getTenantAdminUsername() +
                " subscribed to " + "] Cartridge with Alias " + lbAlias + ", Cartridge Type: " + lbDataContext.getLbCartridgeInfo().getType() +
                ", Autoscale Policy: " + lbDataContext.getAutoscalePolicy() + ", Deployment Policy: " + lbDataContext.getDeploymentPolicy());

        return cartridgeSubscription;
    }

}

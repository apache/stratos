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

package org.apache.stratos.manager.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.pojo.LoadbalancerConfig;
import org.apache.stratos.cloud.controller.pojo.Properties;
import org.apache.stratos.cloud.controller.pojo.Property;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceUnregisteredCartridgeExceptionException;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.manager.dto.SubscriptionInfo;
import org.apache.stratos.manager.exception.*;
import org.apache.stratos.manager.lb.category.*;
import org.apache.stratos.manager.repository.Repository;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscriber.Subscriber;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.subscription.PersistenceContext;
import org.apache.stratos.manager.subscription.SubscriptionData;
import org.apache.stratos.manager.subscription.factory.CartridgeSubscriptionFactory;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionMultiTenantBehaviour;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionSingleTenantBehaviour;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionTenancyBehaviour;
import org.apache.stratos.manager.subscription.utils.CartridgeSubscriptionUtils;
import org.apache.stratos.manager.topology.model.TopologyClusterInformationModel;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;
import org.apache.stratos.manager.utils.CartridgeConstants;
import org.apache.stratos.messaging.util.Constants;
import org.wso2.carbon.context.CarbonContext;

import java.util.Collection;
import java.util.Random;

/**
 * Manager class for the purpose of managing CartridgeSubscriptionInfo subscriptions, groupings, etc.
 */
public class CartridgeSubscriptionManager {

    private static Log log = LogFactory.getLog(CartridgeSubscriptionManager.class);
    //private static DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();
    
    public SubscriptionInfo subscribeToCartridgeWithProperties(SubscriptionData subscriptionData)  throws ADCException,
                                                                                            InvalidCartridgeAliasException,
                                                                                            DuplicateCartridgeAliasException,
                                                                                            PolicyException,
                                                                                            UnregisteredCartridgeException,
                                                                                            RepositoryRequiredException,
                                                                                            RepositoryCredentialsRequiredException,
                                                                                            RepositoryTransportException,
                                                                                            AlreadySubscribedException,
                                                                                            InvalidRepositoryException {

        // validate cartridge alias
        CartridgeSubscriptionUtils.validateCartridgeAlias(subscriptionData.getTenantId(), subscriptionData.getCartridgeType(), subscriptionData.getCartridgeAlias());

        CartridgeInfo cartridgeInfo;
        try {
            cartridgeInfo = CloudControllerServiceClient.getServiceClient().getCartridgeInfo(subscriptionData.getCartridgeType());

        } catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
            String message = subscriptionData.getCartridgeType() + " is not a valid cartridgeSubscription type. Please try again with a valid cartridgeSubscription type.";
            log.error(message);
            throw new ADCException(message, e);

        } catch (Exception e) {
            String message = "Error getting info for " + subscriptionData.getCartridgeType();
            log.error(message, e);
            throw new ADCException(message, e);
        }

        // check if this subscription requires Persistence Mapping, and its supported by the cartridge definition
        Properties persistenceMappingProperties = null;
        if (subscriptionData.getPersistanceContext() != null) {
            persistenceMappingProperties = getPersistenceMappingProperties(subscriptionData.getPersistanceContext(), cartridgeInfo);
        }

        Properties serviceCartridgeSubscriptionProperties = null;
        LBDataContext lbDataCtxt = null;
        CartridgeSubscription lbCartridgeSubscription = null;
        Properties lbCartridgeSubscriptionProperties = null;

        // get lb config reference
        LoadbalancerConfig lbConfig = cartridgeInfo.getLbConfig();
        if (lbConfig == null || lbConfig.getProperties() == null) {
            // no LB ref
            if (log.isDebugEnabled()) {
                log.debug("This Service does not require a load balancer. " + "[Service Name] " +
                        subscriptionData.getCartridgeType());
            }

        } else {

            // LB ref found, get relevant LB Context data
            lbDataCtxt = CartridgeSubscriptionUtils.getLoadBalancerDataContext(subscriptionData.getTenantId(), subscriptionData.getCartridgeType(),
                    subscriptionData.getDeploymentPolicyName(), lbConfig);

            // subscribe to LB
            lbCartridgeSubscription = subscribeToLB (subscriptionData, lbDataCtxt);

            lbCartridgeSubscriptionProperties =  new Properties();
            if (lbDataCtxt.getLbProperperties() != null && !lbDataCtxt.getLbProperperties().isEmpty()) {
                lbCartridgeSubscriptionProperties.setProperties(lbDataCtxt.getLbProperperties().toArray(new Property[0]));
            }
        }

        // subscribe to relevant service cartridge
        CartridgeSubscription serviceCartridgeSubscription = subscribe (subscriptionData, cartridgeInfo);
        serviceCartridgeSubscriptionProperties = new Properties();

        // lb related properties
        if (lbDataCtxt.getLoadBalancedServiceProperties() != null && !lbDataCtxt.getLoadBalancedServiceProperties().isEmpty()) {
            serviceCartridgeSubscriptionProperties.setProperties(lbDataCtxt.getLoadBalancedServiceProperties().toArray(new Property[0]));
        }

        // Persistence Mapping related properties
        if (persistenceMappingProperties != null && persistenceMappingProperties.getProperties().length > 0) {
            // add the properties to send to CC via register method
            for (Property persistenceMappingProperty : persistenceMappingProperties.getProperties()) {
                serviceCartridgeSubscriptionProperties.addProperties(persistenceMappingProperty);
            }
        }

        if (lbCartridgeSubscription != null) {
            // register LB cartridge subscription
            registerCartridgeSubscription(lbCartridgeSubscription, lbCartridgeSubscriptionProperties);
        }

        // register service cartridge subscription
        return registerCartridgeSubscription(serviceCartridgeSubscription, serviceCartridgeSubscriptionProperties);
    }

    private CartridgeSubscription subscribeToLB (SubscriptionData subscriptionData, LBDataContext lbDataContext)

            throws ADCException, InvalidCartridgeAliasException,
            DuplicateCartridgeAliasException, PolicyException, UnregisteredCartridgeException, RepositoryRequiredException, RepositoryCredentialsRequiredException,
            RepositoryTransportException, AlreadySubscribedException, InvalidRepositoryException {

        if (lbDataContext.getLbCategory() == null || lbDataContext.getLbCategory().equals(Constants.NO_LOAD_BALANCER)) {
            // no load balancer subscription required
            log.info("No LB subscription required for the Subscription with alias: " + subscriptionData.getCartridgeAlias() + ", type: " +
                    subscriptionData.getCartridgeType());
            return null;
        }

        LoadBalancerCategory loadBalancerCategory = null;

        String lbAlias = "lb" + lbDataContext.getLbCartridgeInfo().getType() + new Random().nextInt();

        if (lbDataContext.getLbCategory().equals(Constants.EXISTING_LOAD_BALANCERS)) {
            loadBalancerCategory = new ExistingLoadBalancerCategory();

        } else if (lbDataContext.getLbCategory().equals(Constants.DEFAULT_LOAD_BALANCER)) {
            loadBalancerCategory = new DefaultLoadBalancerCategory();

        } else if (lbDataContext.getLbCategory().equals(Constants.SERVICE_AWARE_LOAD_BALANCER)) {
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


        log.info("Tenant [" + subscriptionData.getTenantId() + "] with username [" + subscriptionData.getTenantAdminUsername() +
                " subscribed to " + "] Cartridge with Alias " + lbAlias + ", Cartridge Type: " + lbDataContext.getLbCartridgeInfo().getType() +
                ", Autoscale Policy: " + lbDataContext.getAutoscalePolicy() + ", Deployment Policy: " + lbDataContext.getDeploymentPolicy());

        return cartridgeSubscription;
    }

    private CartridgeSubscription subscribe (SubscriptionData subscriptionData, CartridgeInfo cartridgeInfo)

            throws ADCException, InvalidCartridgeAliasException,
            DuplicateCartridgeAliasException, PolicyException, UnregisteredCartridgeException, RepositoryRequiredException, RepositoryCredentialsRequiredException,
            RepositoryTransportException, AlreadySubscribedException, InvalidRepositoryException {


        // Decide tenancy behaviour
        SubscriptionTenancyBehaviour tenancyBehaviour;
        if(cartridgeInfo.getMultiTenant()) {
            tenancyBehaviour = new SubscriptionMultiTenantBehaviour();
        } else {
            tenancyBehaviour = new SubscriptionSingleTenantBehaviour();
        }

        // Create the CartridgeSubscription instance
        CartridgeSubscription cartridgeSubscription = CartridgeSubscriptionFactory.getCartridgeSubscriptionInstance(cartridgeInfo, tenancyBehaviour);

        // Generate and set the key
        String subscriptionKey = CartridgeSubscriptionUtils.generateSubscriptionKey();
        cartridgeSubscription.setSubscriptionKey(subscriptionKey);

        // Create repository
        Repository repository = cartridgeSubscription.manageRepository(subscriptionData.getRepositoryURL(), subscriptionData.getRepositoryUsername(),
                subscriptionData.getRepositoryPassword(),
                subscriptionData.isPrivateRepository());

        // Create subscriber
        Subscriber subscriber = new Subscriber(subscriptionData.getTenantAdminUsername(), subscriptionData.getTenantId(), subscriptionData.getTenantDomain());

        //create subscription
        cartridgeSubscription.createSubscription(subscriber, subscriptionData.getCartridgeAlias(), subscriptionData.getAutoscalingPolicyName(),
                                                subscriptionData.getDeploymentPolicyName(), repository);

        log.info("Tenant [" + subscriptionData.getTenantId() + "] with username [" + subscriptionData.getTenantAdminUsername() +
                " subscribed to " + "] Cartridge with Alias " + subscriptionData.getCartridgeAlias() + ", Cartridge Type: " +
                subscriptionData.getCartridgeType() + ", Repo URL: " + subscriptionData.getRepositoryURL() + ", Autoscale Policy: " +
                subscriptionData.getAutoscalingPolicyName() + ", Deployment Policy: " + subscriptionData.getDeploymentPolicyName());

        return cartridgeSubscription;
    }

    /**
     * Registers the cartridge subscription for the given CartridgeSubscriptionInfo object
     *
     * @param cartridgeSubscription CartridgeSubscription subscription
     *
     * @return SubscriptionInfo object populated with relevant information
     * @throws ADCException
     * @throws UnregisteredCartridgeException
     */
    private SubscriptionInfo registerCartridgeSubscription(CartridgeSubscription cartridgeSubscription, Properties properties)
            throws ADCException, UnregisteredCartridgeException {

        CartridgeSubscriptionInfo cartridgeSubscriptionInfo = cartridgeSubscription.registerSubscription(properties);

        //set status as 'SUBSCRIBED'
        cartridgeSubscription.setSubscriptionStatus(CartridgeConstants.SUBSCRIBED);

        try {
            new DataInsertionAndRetrievalManager().cacheAndPersistSubcription(cartridgeSubscription);

        } catch (PersistenceManagerException e) {
            String errorMsg = "Error saving subscription for tenant " +
                    cartridgeSubscription.getSubscriber().getTenantDomain() + ", alias " + cartridgeSubscription.getType();
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }

        log.info("Successful Subscription: " + cartridgeSubscription.toString());

        // Publish tenant subscribed event to message broker
        CartridgeSubscriptionUtils.publishTenantSubscribedEvent(cartridgeSubscription.getSubscriber().getTenantId(),
                cartridgeSubscription.getCartridgeInfo().getType());

        return ApplicationManagementUtil.
                createSubscriptionResponse(cartridgeSubscriptionInfo, cartridgeSubscription.getRepository());
    }

    public Collection<CartridgeSubscription> getCartridgeSubscriptions (int tenantId, String type) throws ADCException {

        if (type == null || type.isEmpty()) {
            return new DataInsertionAndRetrievalManager().getCartridgeSubscriptions(tenantId);

        } else {
            return new DataInsertionAndRetrievalManager().getCartridgeSubscriptions(tenantId, type);
        }
    }

    public CartridgeSubscription getCartridgeSubscription (int tenantId, String subscriptionAlias) {

        return new DataInsertionAndRetrievalManager().getCartridgeSubscription(tenantId, subscriptionAlias);
    }

    /**
     * Unsubscribe from a Cartridge
     *
     * @param tenantDomain Tenant's domain
     * @param alias Alias given at subscription time
     * @throws ADCException
     * @throws NotSubscribedException
     */
    public void unsubscribeFromCartridge (String tenantDomain, String alias)
            throws ADCException, NotSubscribedException {

        DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();

        CartridgeSubscription cartridgeSubscription = dataInsertionAndRetrievalManager.getCartridgeSubscription(CarbonContext.getThreadLocalCarbonContext().getTenantId(), alias);
        if(cartridgeSubscription != null) {
            cartridgeSubscription.removeSubscription();

            // Remove the information from Topology Model
            TopologyClusterInformationModel.getInstance().removeCluster(cartridgeSubscription.getSubscriber().getTenantId(),
                    cartridgeSubscription.getType(), cartridgeSubscription.getAlias());

            // remove subscription
            try {
                dataInsertionAndRetrievalManager.removeSubscription(cartridgeSubscription.getSubscriber().getTenantId(), alias);

            } catch (PersistenceManagerException e) {
                String errorMsg = "Error removing subscription for tenant " + tenantDomain + ", alias " + cartridgeSubscription.getAlias();
                log.error(errorMsg);
                throw new ADCException(errorMsg, e);
            }

            // Publish tenant un-subscribed event to message broker
            CartridgeSubscriptionUtils.publishTenantUnSubscribedEvent(cartridgeSubscription.getSubscriber().getTenantId(),
                    cartridgeSubscription.getCartridgeInfo().getType());
        }
        else {
            String errorMsg = "No cartridge subscription found with [alias] " + alias + " for [tenant] " + tenantDomain;
            log.error(errorMsg);
            throw new NotSubscribedException(errorMsg, alias);
        }
    }

    private Properties getPersistenceMappingProperties (PersistenceContext persistenceCtxt, CartridgeInfo cartridgeInfo) throws ADCException {

        if (!cartridgeInfo.isPersistenceSpecified()) {
            // Persistence Mapping not supported in the cartridge definition - error
            String errorMsg = "Persistence Mapping not supported by the cartridge type " + cartridgeInfo.getType();
            log.error(errorMsg);
            throw new ADCException(errorMsg);
        }

        Properties persistenceMappingProperties = new Properties();
        persistenceMappingProperties.setProperties(new Property[]{persistenceCtxt.getPersistanceRequiredProperty(), persistenceCtxt.getSizeProperty(),
                persistenceCtxt.getDeleteOnTerminationProperty()});

        return persistenceMappingProperties;
    }
    
    /**
     * 
     * Returns a collection of Cartridge subscriptions for a particular tenant and a cartridge type
     * 
     * @param tenantId
     * @param cartridgeType
     * @return
     */
    public Collection<CartridgeSubscription> isCartridgeSubscribed(int tenantId, String cartridgeType) {
    	
    	DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();
        return dataInsertionAndRetrievalManager.getCartridgeSubscriptions(tenantId, cartridgeType);
    }
}

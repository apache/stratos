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
import org.apache.stratos.cloud.controller.pojo.Property;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.manager.dto.SubscriptionInfo;
import org.apache.stratos.manager.exception.*;
import org.apache.stratos.manager.repository.Repository;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscriber.Subscriber;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.subscription.SubscriptionData;
import org.apache.stratos.manager.subscription.factory.CartridgeSubscriptionFactory;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionMultiTenantBehaviour;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionSingleTenantBehaviour;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionTenancyBehaviour;
import org.apache.stratos.manager.subscription.utils.CartridgeSubscriptionUtils;
import org.apache.stratos.manager.topology.model.TopologyClusterInformationModel;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;
import org.apache.stratos.manager.utils.CartridgeConstants;
import org.apache.stratos.manager.utils.RepoPasswordMgtUtil;
import org.wso2.carbon.context.CarbonContext;

import java.util.Collection;

/**
 * Manager class for the purpose of managing CartridgeSubscriptionInfo subscriptions, groupings, etc.
 */
public class CartridgeSubscriptionManager {

    private static Log log = LogFactory.getLog(CartridgeSubscriptionManager.class);
    //private static DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();
    
    public CartridgeSubscription subscribeToCartridgeWithProperties(SubscriptionData subscriptionData)  throws ADCException,
                                                                                            InvalidCartridgeAliasException,
                                                                                            DuplicateCartridgeAliasException,
                                                                                            PolicyException,
                                                                                            UnregisteredCartridgeException,
                                                                                            RepositoryRequiredException,
                                                                                            RepositoryCredentialsRequiredException,
                                                                                            RepositoryTransportException,
                                                                                            AlreadySubscribedException,
                                                                                            InvalidRepositoryException {

        int tenantId = subscriptionData.getTenantId();
        String cartridgeType = subscriptionData.getCartridgeType();
        String cartridgeAlias =  subscriptionData.getCartridgeAlias();
        Property [] props = subscriptionData.getProperties();
        String repositoryPassword = subscriptionData.getRepositoryPassword();
        String repositoryUsername = subscriptionData.getRepositoryUsername();
        boolean isPrivateRepository = subscriptionData.isPrivateRepository();
        String repositoryURL = subscriptionData.getRepositoryURL();
        String tenantDomain = subscriptionData.getTenantDomain();
        String tenantAdminUsername = subscriptionData.getTenantAdminUsername();
        String autoscalingPolicyName = subscriptionData.getAutoscalingPolicyName();
        String deploymentPolicyName = subscriptionData.getDeploymentPolicyName();
        String lbClusterId = subscriptionData.getLbClusterId();

        // validate cartridge alias
        CartridgeSubscriptionUtils.validateCartridgeAlias(tenantId, cartridgeType, cartridgeAlias);

        CartridgeInfo cartridgeInfo;
        try {
            cartridgeInfo =
                            CloudControllerServiceClient.getServiceClient().getCartridgeInfo(cartridgeType);
            if (props != null) {
                // TODO: temp fix, need to do a proper fix
                Property[] cartridgeInfoProperties = cartridgeInfo.getProperties();
                if(cartridgeInfoProperties != null) {
                     int length = cartridgeInfoProperties.length + props.length;
                    Property[] combined = new Property[length];
                    System.arraycopy(cartridgeInfoProperties, 0, combined, 0, cartridgeInfoProperties.length);
                    System.arraycopy(props, 0, combined, cartridgeInfoProperties.length, props.length);
                    cartridgeInfo.setProperties(combined);
                } else {
                    cartridgeInfo.setProperties(props);
                }

            }

        } catch (UnregisteredCartridgeException e) {
            String message =
                             cartridgeType +
                                     " is not a valid cartridgeSubscription type. Please try again with a valid cartridgeSubscription type.";
            log.error(message);
            throw e;

        } catch (Exception e) {
            String message = "Error getting info for " + cartridgeType;
            log.error(message, e);
            throw new ADCException(message, e);
        }

        //Decide tenancy behaviour
        SubscriptionTenancyBehaviour tenancyBehaviour;
        if(cartridgeInfo.getMultiTenant()) {
            tenancyBehaviour = new SubscriptionMultiTenantBehaviour();
        } else {
            tenancyBehaviour = new SubscriptionSingleTenantBehaviour();
        }

        //Create the CartridgeSubscription instance
        CartridgeSubscription cartridgeSubscription = CartridgeSubscriptionFactory.
                getCartridgeSubscriptionInstance(cartridgeInfo, tenancyBehaviour);


        String subscriptionKey = CartridgeSubscriptionUtils.generateSubscriptionKey();

        String encryptedRepoPassword = repositoryPassword != null && !repositoryPassword.isEmpty() ?
                RepoPasswordMgtUtil.encryptPassword(repositoryPassword, subscriptionKey) : "";
        
        //Create repository
        Repository repository = cartridgeSubscription.manageRepository(repositoryURL,
                                                                       repositoryUsername,
                                                                       encryptedRepoPassword,
                                                                       isPrivateRepository,
                                                                       cartridgeAlias,
                                                                       cartridgeInfo, tenantDomain);

        //Create subscriber
        Subscriber subscriber = new Subscriber(tenantAdminUsername, tenantId, tenantDomain);

        //Set the key
        cartridgeSubscription.setSubscriptionKey(subscriptionKey);

        //create subscription
        cartridgeSubscription.createSubscription(subscriber, cartridgeAlias, autoscalingPolicyName,
                                                deploymentPolicyName, repository);

        // set the lb cluster id if its available
        if (lbClusterId != null && !lbClusterId.isEmpty()) {
            cartridgeSubscription.setLbClusterId(lbClusterId);
        }

        log.info("Tenant [" + tenantId + "] with username [" + tenantAdminUsername +
                 " subscribed to " + "] Cartridge Alias " + cartridgeAlias + ", Cartridge Type: " +
                 cartridgeType + ", Repo URL: " + repositoryURL + ", Policy: " +
                 autoscalingPolicyName);


        // Publish tenant subscribed event to message broker
        CartridgeSubscriptionUtils.publishTenantSubscribedEvent(cartridgeSubscription.getSubscriber().getTenantId(),
                cartridgeSubscription.getCartridgeInfo().getType());

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
    public SubscriptionInfo registerCartridgeSubscription(CartridgeSubscription cartridgeSubscription)
            throws ADCException, UnregisteredCartridgeException {

        CartridgeSubscriptionInfo cartridgeSubscriptionInfo = cartridgeSubscription.registerSubscription(null);

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
            String errorMsg = "No cartridge subscription found with alias " + alias + " for tenant " + tenantDomain;
            log.error(errorMsg);
            throw new NotSubscribedException(errorMsg, alias);
        }
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

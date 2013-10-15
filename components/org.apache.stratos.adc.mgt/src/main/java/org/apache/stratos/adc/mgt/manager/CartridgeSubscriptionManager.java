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

package org.apache.stratos.adc.mgt.manager;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.client.CloudControllerServiceClient;
import org.apache.stratos.adc.mgt.connector.CartridgeInstanceConnector;
import org.apache.stratos.adc.mgt.connector.CartridgeInstanceConnectorFactory;
import org.apache.stratos.adc.mgt.dao.CartridgeSubscription;
import org.apache.stratos.adc.mgt.dto.Policy;
import org.apache.stratos.adc.mgt.dto.SubscriptionInfo;
import org.apache.stratos.adc.mgt.exception.*;
import org.apache.stratos.adc.mgt.instance.CartridgeInstance;
import org.apache.stratos.adc.mgt.instance.factory.CartridgeInstanceFactory;
import org.apache.stratos.adc.mgt.payload.Payload;
import org.apache.stratos.adc.mgt.payload.PayloadArg;
import org.apache.stratos.adc.mgt.payload.PayloadFactory;
import org.apache.stratos.adc.mgt.repository.Repository;
import org.apache.stratos.adc.mgt.subscriber.Subscriber;
import org.apache.stratos.adc.mgt.utils.ApplicationManagementUtil;
import org.apache.stratos.adc.mgt.utils.PersistenceManager;
import org.apache.stratos.adc.mgt.utils.PolicyHolder;
import org.apache.stratos.cloud.controller.util.xsd.CartridgeInfo;
import org.wso2.carbon.context.CarbonContext;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Manager class for the purpose of managing CartridgeInstance subscriptions, groupings, etc.
 */
public class CartridgeSubscriptionManager {

    private static Log log = LogFactory.getLog(CartridgeSubscriptionManager.class);

    /**
     * Subscribes to a cartridge
     *
     * @param cartridgeType Cartridge type
     * @param cartridgeAlias Cartridge alias
     * @param autoscalingPolicyName Autoscaling policy name
     * @param tenantDomain Subscriing tenant's domain
     * @param tenantId Subscribing tenant's Id
     * @param tenantAdminUsername Subscribing tenant's admin user name
     * @param repositoryType Type of repository
     * @param repositoryURL Repository URL
     * @param isPrivateRepository If a private or a public repository
     * @param repositoryUsername Repository username
     * @param repositoryPassword Repository password
     *
     * @return Subscribed CartridgeInstance object
     * @throws ADCException
     * @throws InvalidCartridgeAliasException
     * @throws DuplicateCartridgeAliasException
     * @throws PolicyException
     * @throws UnregisteredCartridgeException
     * @throws RepositoryRequiredException
     * @throws RepositoryCredentialsRequiredException
     * @throws RepositoryTransportException
     * @throws AlreadySubscribedException
     * @throws InvalidRepositoryException
     */
    public CartridgeInstance subscribeToCartridge (String cartridgeType, String cartridgeAlias,
                                                  String autoscalingPolicyName, String tenantDomain, int tenantId,
                                                  String tenantAdminUsername, String repositoryType,
                                                  String repositoryURL, boolean isPrivateRepository,
                                                  String repositoryUsername, String repositoryPassword)

            throws ADCException, InvalidCartridgeAliasException, DuplicateCartridgeAliasException, PolicyException,
            UnregisteredCartridgeException, RepositoryRequiredException, RepositoryCredentialsRequiredException,
            RepositoryTransportException, AlreadySubscribedException, InvalidRepositoryException {

        //validate cartridge alias
        ApplicationManagementUtil.validateCartridgeAlias(cartridgeAlias, cartridgeType);

        Policy autoScalingPolicy;
        if(autoscalingPolicyName != null && !autoscalingPolicyName.isEmpty()) {
            autoScalingPolicy = PolicyHolder.getInstance().getPolicy(autoscalingPolicyName);
        } else {
            autoScalingPolicy = PolicyHolder.getInstance().getDefaultPolicy();
        }

        if(autoScalingPolicy == null) {
            throw new PolicyException("Could not load the auto scaling policy.");
        }

        CartridgeInfo cartridgeInfo;
        try {
            cartridgeInfo = CloudControllerServiceClient.getServiceClient().getCartridgeInfo(cartridgeType);

        } catch (UnregisteredCartridgeException e) {
            String message = cartridgeType
                    + " is not a valid cartridgeInstance type. Please try again with a valid cartridgeInstance type.";
            log.error(message);
            throw e;

        } catch (Exception e) {
            String message = "Error getting info for " + cartridgeType;
            log.error(message, e);
            throw new ADCException(message, e);
        }

        Subscriber subscriber = new Subscriber(tenantAdminUsername, tenantId, tenantDomain);

        CartridgeInstance cartridgeInstance = CartridgeInstanceFactory.getCartridgeInstance(cartridgeInfo);
        Repository repository = cartridgeInstance.manageRepository(repositoryURL, repositoryUsername,
                repositoryPassword, isPrivateRepository, cartridgeAlias, cartridgeInfo, tenantDomain);

        cartridgeInstance.subscribe(subscriber, cartridgeAlias, autoScalingPolicy, repository);

        log.info("Tenant [" + tenantId + "] with username [" + tenantAdminUsername +
                " subscribed to " + "] Cartridge Alias " + cartridgeAlias + ", Cartridge Type: " + cartridgeType +
                ", Repo URL: " + repositoryURL + ", Policy: " + autoscalingPolicyName);

        PayloadArg payloadArg = cartridgeInstance.createPayloadParameters();
        Payload payload = PayloadFactory.getPayloadInstance(cartridgeInfo.getProvider(), cartridgeType,
                "/tmp/" + tenantDomain + "-" + cartridgeAlias + ".zip");
        payload.populatePayload(payloadArg);

        //set the payload to the cartridge instance
        cartridgeInstance.setPayload(payload);

        //CartridgeInstanceCache.getCartridgeInstanceCache().
        //        addCartridgeInstance(new CartridgeInstanceCacheKey(tenantId, cartridgeAlias), cartridgeInstance);

        return cartridgeInstance;
    }

    /**
     * Connects / groups cartridges
     *
     * @param tenantDomain Tenant's domain
     * @param cartridgeInstance CartridgeInstance object to which the CartridgeInstance denoted by
     *                          connectingCartridgeAlias will be connected to
     * @param connectingCartridgeAlias Alias of the connecting cartridge
     *
     * @throws ADCException
     * @throws NotSubscribedException
     * @throws AxisFault
     */
    public void connectCartridges (String tenantDomain, CartridgeInstance cartridgeInstance,
                                   String connectingCartridgeAlias)
            throws ADCException, NotSubscribedException, AxisFault {

        //TODO: retrieve from the cache and connect. For now, new objects are created

        CartridgeInstance connectingCartridgeInstance = getCartridgeInstance(tenantDomain, connectingCartridgeAlias);

        if(cartridgeInstance == null) {
            String errorMsg = "No cartridge instance found in cache for tenant " + tenantDomain + ", alias " +
                    cartridgeInstance.getAlias() + ",  connecting aborted";
            log.error(errorMsg);
            return;
        }

        if(connectingCartridgeInstance == null) {
            String errorMsg = "No cartridge instance found in cache for tenant " + tenantDomain + ", alias " +
                    connectingCartridgeAlias + ",  connecting aborted";
            log.error(errorMsg);
            return;
        }

        CartridgeInstanceConnector cartridgeInstanceConnector = CartridgeInstanceConnectorFactory.
                getCartridgeInstanceConnector(connectingCartridgeInstance.getType());

        cartridgeInstance.connect(connectingCartridgeAlias);

        //PayloadArg payloadArg = cartridgeInstance.createPayloadParameters();

        //get additional payload params for connecting cartridges
        Properties payloadProperties = cartridgeInstanceConnector.createConnection(cartridgeInstance,
                connectingCartridgeInstance);
        StringBuilder connectionParamsBuilder = new StringBuilder();
        Set<Map.Entry<Object,Object>> payloadParamEntries = payloadProperties.entrySet();

        for (Map.Entry<Object, Object> payloadParamEntry : payloadParamEntries) {
            connectionParamsBuilder.append(",");
            connectionParamsBuilder.append(payloadParamEntry.getKey().toString());
            connectionParamsBuilder.append("=");
            connectionParamsBuilder.append(payloadParamEntry.getValue().toString());
        }

        //add additional connection relates parameters to the payload
        cartridgeInstance.getPayload().populatePayload(connectionParamsBuilder.toString());

        /*
        payloadArg.setUserDefinedPayload(connectionParamsBuilder.toString());
        Payload payload = PayloadFactory.getPayloadInstance(cartridgeInstance.getCartridgeInfo().getProvider(),
                cartridgeInstance.getType(),
                "/tmp/" + tenantDomain + "-" + cartridgeInstance.getAlias() + ".zip");
        payload.populatePayload(payloadArg);
        payload.createPayload();
        */
    }

    /**
     * Registers the cartridge subscription for the given CartridgeInstance object
     *
     * @param cartridgeInstance CartridgeInstance instance
     *
     * @return SubscriptionInfo object populated with relevant information
     * @throws ADCException
     * @throws UnregisteredCartridgeException
     */
    public SubscriptionInfo registerCartridgeSubscription(CartridgeInstance cartridgeInstance)
            throws ADCException, UnregisteredCartridgeException {

        /*CartridgeInstance cartridgeInstance = CartridgeInstanceCache.getCartridgeInstanceCache().
                getCartridgeInstance(new CartridgeInstanceCacheKey(tenantId, alias));

        if(cartridgeInstance == null) {
            throw new ADCException("Unable to find cartridge with alias " + alias + ", for tenant Id " + tenantId +
                    " in cache");
        }*/

        CartridgeSubscription cartridgeSubscription = cartridgeInstance.registerSubscription(null);

        int subscriptionId;
        try {
            subscriptionId = PersistenceManager.persistSubscription(cartridgeSubscription);

        } catch (Exception e) {
            String errorMsg = "Error saving subscription for tenant " +
                    cartridgeInstance.getSubscriber().getTenantDomain() + ", alias " + cartridgeInstance.getType();
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }

        cartridgeInstance.setSubscriptionId(subscriptionId);
        ApplicationManagementUtil.addDNSEntry(cartridgeSubscription.getAlias(), cartridgeInstance.getType());

        return ApplicationManagementUtil.
                createSubscriptionResponse(cartridgeSubscription, cartridgeInstance.getRepository());
    }

    /**
     * Unsubscribe from a CartridgeInstance
     *
     * @param tenantDomain Tenant's domain
     * @param alias Alias of the CartridgeInstance to unsubscribe from
     * @throws ADCException
     * @throws NotSubscribedException
     */
    public void unsubscribeFromCartridge (String tenantDomain, String alias)
            throws ADCException, NotSubscribedException {

        //TODO: retrieve from the cache and connect. For now, new objects are created

        CartridgeInstance cartridgeInstance = getCartridgeInstance(tenantDomain, alias);

        if(cartridgeInstance != null) {
            cartridgeInstance.unsubscribe();
            //CartridgeInstanceCache.getCartridgeInstanceCache().removeCartridgeInstance(cartridgeInstanceCacheKey);
        }
        else {
            if(log.isDebugEnabled()) {
                log.debug("No cartridge instance found with alias " + alias + " for tenant " + tenantDomain);
            }
        }
    }

    /*public List<CartridgeInstance> getCartridgeInstances (int tenantId) throws ADCException, NotSubscribedException {

        List<CartridgeSubscription> cartridgeSubscriptions = getCartridgeSubscriptions(tenantId);
        List<CartridgeInstance> cartridgeInstances = new ArrayList<CartridgeInstance>();
        CartridgeInfo cartridgeInfo;

        for(CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {
            try {
                cartridgeInfo = CloudControllerServiceClient.getServiceClient().
                        getCartridgeInfo(cartridgeSubscription.getCartridge());
                cartridgeInstances.add(populateCartridgeInstanceInformation(cartridgeInfo, cartridgeSubscription));

            } catch (Exception e) {
                throw new ADCException(e.getMessage(), e);
            }
        }

        return cartridgeInstances;
    }*/

    /**
     * Creates and returns a CartridgeInstance object
     *
     * @param tenantDomain Tenant's domain
     * @param alias Alias of the CartridgeInstance
     *
     * @return CartridgeInstance object populated with relevant information
     * @throws ADCException
     * @throws NotSubscribedException
     */
    public CartridgeInstance getCartridgeInstance (String tenantDomain, String alias)
            throws ADCException, NotSubscribedException {

        CartridgeSubscription cartridgeSubscription = getCartridgeSubscription(tenantDomain, alias);

        CartridgeInfo cartridgeInfo;
        try {
            cartridgeInfo = CloudControllerServiceClient.getServiceClient().
                    getCartridgeInfo(cartridgeSubscription.getCartridge());
        } catch (Exception e) {
            throw new ADCException(e.getMessage(), e);
        }

        return populateCartridgeInstanceInformation(cartridgeInfo, cartridgeSubscription);
    }

    private CartridgeSubscription getCartridgeSubscription (String tenantDomain, String alias)
            throws ADCException, NotSubscribedException {

        CartridgeSubscription subscription;
        try {
            subscription = PersistenceManager.getSubscription(tenantDomain, alias);

        } catch (Exception e) {
            String msg = "Failed to get subscriptions for " + tenantDomain;
            log.error(msg, e);
            throw new ADCException(msg, e);
        }

        if (subscription == null) {
            String msg = "Tenant " + tenantDomain + " has not subscribed for cartridges";
            log.error(msg);
            throw new NotSubscribedException(msg, msg);
        }

        return subscription;

    }

    private List<CartridgeSubscription> getCartridgeSubscriptions (int tenantId) throws ADCException, NotSubscribedException {

        List<CartridgeSubscription> subscriptions;
        try {
            subscriptions = PersistenceManager.getSubscriptionsForTenant(tenantId);

        } catch (Exception e) {
            String msg = "Failed to get subscriptions for " + tenantId;
            log.error(msg, e);
            throw new ADCException(msg, e);
        }

        if (subscriptions == null) {
            String msg = "Tenant " + tenantId + " has not subscribed for cartridges";
            log.error(msg);
            throw new NotSubscribedException(msg, msg);
        }

        return subscriptions;
    }

    private CartridgeInstance populateCartridgeInstanceInformation (CartridgeInfo cartridgeInfo,
                                                                    CartridgeSubscription cartridgeSubscription)
            throws ADCException {

        CartridgeInstance cartridgeInstance = CartridgeInstanceFactory.getCartridgeInstance(cartridgeInfo);

        cartridgeInstance.setSubscriptionId(cartridgeSubscription.getSubscriptionId());
        cartridgeInstance.setAlias(cartridgeSubscription.getAlias());
        cartridgeInstance.setHostName(cartridgeSubscription.getHostName());
        cartridgeInstance.setClusterDomain(cartridgeSubscription.getClusterDomain());
        cartridgeInstance.setClusterSubDomain(cartridgeSubscription.getClusterSubdomain());
        cartridgeInstance.setMgtClusterDomain(cartridgeSubscription.getMgtClusterDomain());
        cartridgeInstance.setMgtClusterSubDomain(cartridgeSubscription.getMgtClusterSubDomain());
        Policy autoScalingPolicy;
        if(cartridgeSubscription.getPolicy() != null && !cartridgeSubscription.getPolicy().isEmpty()) {
            autoScalingPolicy = PolicyHolder.getInstance().getPolicy(cartridgeSubscription.getPolicy());
        } else {
            autoScalingPolicy = PolicyHolder.getInstance().getDefaultPolicy();
        }
        cartridgeInstance.setAutoscalingPolicy(autoScalingPolicy);
        Subscriber subscriber = new Subscriber(CarbonContext.getThreadLocalCarbonContext().getUsername(),
                cartridgeSubscription.getTenantId(), cartridgeSubscription.getTenantDomain());
        cartridgeInstance.setSubscriber(subscriber);
        cartridgeInstance.setRepository(cartridgeSubscription.getRepository());

        return cartridgeInstance;
    }
}

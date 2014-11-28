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
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceUnregisteredCartridgeExceptionException;
import org.apache.stratos.cloud.controller.stub.pojo.*;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.dto.SubscriptionInfo;
import org.apache.stratos.manager.exception.*;
import org.apache.stratos.manager.internal.DataHolder;
import org.apache.stratos.manager.lb.category.*;
import org.apache.stratos.manager.repository.Repository;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscriber.Subscriber;
import org.apache.stratos.manager.subscription.*;
import org.apache.stratos.manager.subscription.factory.CartridgeSubscriptionFactory;
import org.apache.stratos.manager.subscription.filter.LBCreationSubscriptionFilter;
import org.apache.stratos.manager.subscription.filter.SubscriptionFilter;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionMultiTenantBehaviour;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionSingleTenantBehaviour;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionTenancyBehaviour;
import org.apache.stratos.manager.subscription.utils.CartridgeSubscriptionUtils;
import org.apache.stratos.manager.topology.model.TopologyClusterInformationModel;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;
import org.apache.stratos.manager.utils.CartridgeConstants;
import org.apache.stratos.manager.utils.RepoPasswordMgtUtil;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.event.tenant.SubscriptionDomainAddedEvent;
import org.apache.stratos.messaging.event.tenant.SubscriptionDomainRemovedEvent;
import org.apache.stratos.messaging.util.Util;
import org.wso2.carbon.context.CarbonContext;
import org.apache.stratos.manager.publisher.CartridgeSubscriptionDataPublisher;

import java.util.*;


/**
 * Manager class for the purpose of managing CartridgeSubscriptionInfo subscriptions, groupings, etc.
 */
public class CartridgeSubscriptionManager {

    private static Log log = LogFactory.getLog(CartridgeSubscriptionManager.class);
    //private static DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();

    public GroupSubscription createGroupSubscription (String groupName, String groupAlias, int tenantId)
            throws GroupSubscriptionException {

        DataInsertionAndRetrievalManager dataInsertionAndRetrievalMgr = new DataInsertionAndRetrievalManager();
        GroupSubscription groupSubscription;

        try {
            groupSubscription = dataInsertionAndRetrievalMgr.getGroupSubscription(tenantId, groupName, groupAlias);

        } catch (PersistenceManagerException e) {
            throw new GroupSubscriptionException(e);
        }

        if (groupSubscription != null) {
            // Group Subscription already exists with same Group name and alias
            throw new GroupSubscriptionException("Group Subscription already exists with name [ " + groupName + " ], alias [ " + groupAlias + " ]");
        }

        return new GroupSubscription(groupName, groupAlias);
    }

    public ApplicationSubscription createApplicationSubscription (String appId, int tenantId)  throws ApplicationSubscriptionException {

    	if (log.isDebugEnabled()) {
            log.debug("create Application Subscription for appId: " + appId + " and tenantId: " + tenantId);
        }
    	
        DataInsertionAndRetrievalManager dataInsertionAndRetrievalMgr = new DataInsertionAndRetrievalManager();
        ApplicationSubscription appSubscription;

        try {
            appSubscription = dataInsertionAndRetrievalMgr.getApplicationSubscription(tenantId, appId);

        } catch (PersistenceManagerException e) {
            log.error("failed to retrieve application Subscription for appId: " + appId + " and tenantId: " + tenantId + " e:" + e);
            throw new ApplicationSubscriptionException(e);
        }

        if (appSubscription != null) {
            // Composite App Subscription already exists with same app id
           log.error("app Id already exists, failed to createappSubscription for appId: " + appId + " and tenantId: " + tenantId);
           throw new ApplicationSubscriptionException("Composite App Subscription already exists with Id [ " +  appId + " ]");
        } else {
        	
        	if (log.isDebugEnabled()) {
        		log.debug("creating new application subscription for app:" + appId );
        	}
        	
        	appSubscription = new ApplicationSubscription(appId);
        	// persist 
        	try {
				this.persistApplicationSubscription(appSubscription);
			} catch (ADCException e) {
				// TODO Auto-generated catch block
				log.error("Failed to persist applicaiton subscription for appId: " + appId +
																			" and tenantId: " + tenantId + " e:" + e);
			}
        }

        return new ApplicationSubscription(appId);
    }
    
    public void removeApplicationSubscription (String appId, int tenantId)  throws ApplicationSubscriptionException {

    	if (log.isDebugEnabled()) {
            log.debug("remove Application Subscription for appId: " + appId + " and tenantId: " + tenantId);
        }
    	
        DataInsertionAndRetrievalManager dataInsertionAndRetrievalMgr = new DataInsertionAndRetrievalManager();
        ApplicationSubscription appSubscription = null;

        try {
        	appSubscription = dataInsertionAndRetrievalMgr.getApplicationSubscription(tenantId, appId);

        } catch (PersistenceManagerException e) {
            log.error("failed to retrieve Application Subscription for appId: " + appId + " and tenantId: " + tenantId + "with exception:" + e);
            throw new ApplicationSubscriptionException(e);
        }

        if (appSubscription != null) {
        	
        	try {
				dataInsertionAndRetrievalMgr.removeApplicationSubscription(tenantId, appId);
			} catch (PersistenceManagerException e) {
				log.error("failed to remove Application Subscription for appId: " + appId + " and tenantId: " + tenantId + " with exception:" + e);
				throw new ApplicationSubscriptionException(e);
			}
        	
        	if (log.isDebugEnabled()) {
                log.debug("successfully removed Application Subscription for appId: " + appId + " and tenantId: " + tenantId);
            }
        	
        } 
    }
    
    public ApplicationSubscription getApplicationSubscription (String appId, int tenantId)  throws ApplicationSubscriptionException {
    	if (log.isDebugEnabled()) {
            log.debug("get Application Subscription for appId: " + appId + " and tenantId: " + tenantId);
        }
    	
        DataInsertionAndRetrievalManager dataInsertionAndRetrievalMgr = new DataInsertionAndRetrievalManager();
        ApplicationSubscription appSubscription = null;

        try {
            appSubscription = dataInsertionAndRetrievalMgr.getApplicationSubscription(tenantId, appId);

        } catch (PersistenceManagerException e) {
            log.error("failed to Application Subscription for appId: " + appId + " and tenantId: " + tenantId + " e:" + e);
            throw new ApplicationSubscriptionException(e);
        }
        
        return appSubscription;
    }

    public CartridgeSubscription createCartridgeSubscription (SubscriptionData subscriptionData) throws ADCException,
            InvalidCartridgeAliasException, DuplicateCartridgeAliasException, PolicyException, UnregisteredCartridgeException,
            RepositoryRequiredException, RepositoryCredentialsRequiredException, RepositoryTransportException,
            AlreadySubscribedException, InvalidRepositoryException {


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

        // subscribe to relevant service cartridge
        CartridgeSubscription serviceCartridgeSubscription = subscribe (subscriptionData, cartridgeInfo, null);

        return serviceCartridgeSubscription;
    }

    public void persistCartridgeSubscription (CartridgeSubscription cartridgeSubscription) throws ADCException {

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
        Set<String> clusterIds = new HashSet<String>();
        clusterIds.add(cartridgeSubscription.getCluster().getClusterDomain());
        CartridgeSubscriptionUtils.publishTenantSubscribedEvent(cartridgeSubscription.getSubscriber().getTenantId(),
                cartridgeSubscription.getCartridgeInfo().getType(), clusterIds);
    }

    public void persistGroupSubscription (GroupSubscription groupSubscription) throws ADCException {

        try {
            new DataInsertionAndRetrievalManager().persistGroupSubscription(groupSubscription);

        } catch (PersistenceManagerException e) {
            throw new ADCException(e);
        }
    }

    public void persistApplicationSubscription (ApplicationSubscription compositeAppSubscription) throws ADCException {

        try {
            new DataInsertionAndRetrievalManager().persistApplicationSubscription(compositeAppSubscription);

        } catch (PersistenceManagerException e) {
            throw new ADCException(e);
        }
    }

    /**
     * 
     * @param subscriptionData
     * @return
     * @throws Exception since the caller doesn't react upon specific exceptions, simply throw Generic Exception class.
     */
    public static SubscriptionInfo subscribeToCartridgeWithProperties(SubscriptionData subscriptionData)  throws Exception {

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

        // For MT subscriptions check whether there are active instances
        if(cartridgeInfo.getMultiTenant() && !activeInstancesAvailable(subscriptionData)) {
        	String msg = "No active instances are found for cartridge [" + subscriptionData.getCartridgeType() + "]";
        	log.error(msg);
        	throw new ADCException(msg);
        }
        
        // check if this subscription requires Persistence Mapping, and its supported by the cartridge definition
        Properties persistenceMappingProperties = null;
        if (subscriptionData.getPersistanceContext() != null) {
            persistenceMappingProperties = getPersistenceMappingProperties(subscriptionData.getPersistanceContext(), cartridgeInfo);
        }

        Properties serviceCartridgeSubscriptionProperties = null;
        Properties propertiesReturnedByFilters = new Properties();
        
        List<SubscriptionFilter> subscriptionFilters = new ArrayList<SubscriptionFilter>();
        subscriptionFilters.add(new LBCreationSubscriptionFilter());
        
        for (SubscriptionFilter subscriptionFilter : subscriptionFilters) {
			// execute filters
        	appendProperties(propertiesReturnedByFilters, subscriptionFilter.execute(cartridgeInfo, subscriptionData));
        	
		}

        // subscribe to relevant service cartridge
        CartridgeSubscription serviceCartridgeSubscription = subscribe (subscriptionData, cartridgeInfo, getLBClusterId(propertiesReturnedByFilters));
        
        if (subscriptionData.getProperties() != null) {
        	serviceCartridgeSubscriptionProperties = subscriptionData.getProperties();
        } else {
        	
        	serviceCartridgeSubscriptionProperties = new Properties();
        }

        // add properties returned by filters
		if (propertiesReturnedByFilters.getProperties() != null && propertiesReturnedByFilters.getProperties().length > 0) {
			for (Property prop : propertiesReturnedByFilters.getProperties()) {

				serviceCartridgeSubscriptionProperties.addProperties(prop);
			}
		}

        // Persistence Mapping related properties
        if (persistenceMappingProperties != null && persistenceMappingProperties.getProperties().length > 0) {
            // add the properties to send to CC via register method
            for (Property persistenceMappingProperty : persistenceMappingProperties.getProperties()) {
                serviceCartridgeSubscriptionProperties.addProperties(persistenceMappingProperty);
            }
        }

        // register service cartridge subscription
        return registerCartridgeSubscription(serviceCartridgeSubscription, serviceCartridgeSubscriptionProperties, subscriptionData.getPersistence());
    }

    private static String getLBClusterId(Properties propertiesReturnedByFilters) {
		if (propertiesReturnedByFilters != null
				&& propertiesReturnedByFilters.getProperties() != null) {
			for (Property prop : propertiesReturnedByFilters.getProperties()) {
				if (prop == null) {
					continue;
				}
				if (prop.getName().equals(CartridgeConstants.LB_CLUSTER_ID)) {
					return prop.getValue();
				}
			}
		}
		return null;
	}

	private static void appendProperties(
			Properties propertiesReturnedByFilters, Properties newProperties) {

		if (newProperties.getProperties() == null) {
			return;
		}
    	for (Property property : newProperties.getProperties()) {
    		if (property != null) {
    			
    			propertiesReturnedByFilters.addProperties(property);
    		}
		}
    	
	}

	private static boolean activeInstancesAvailable(SubscriptionData subscriptionData) {
      Cluster cluster = TopologyClusterInformationModel.getInstance().getCluster(subscriptionData.getCartridgeType());
      if(cluster != null) {
          Collection<Member> members = cluster.getMembers();
          for (Member member : members) {
  			if(member.isActive()) {
                if(log.isDebugEnabled()) {
                    log.debug("Active member found for cluster  [" + cluster +"]");
                }
  				return true;
  			}
  		} 
      }
      if(log.isDebugEnabled()) {
    	  log.debug("Active member not found for cluster  [" + cluster +"]");
      }
	  return false;
	}

	

    private static CartridgeSubscription subscribe (SubscriptionData subscriptionData, CartridgeInfo cartridgeInfo, String lbClusterId)

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

        
        // For MT cartridges subscription key should not be generated for every subscription,
        // instead use the already generated key at the time of service deployment
        String subscriptionKey = null;
        if(cartridgeInfo.getMultiTenant()) {
        	try {
				Service service = new DataInsertionAndRetrievalManager().getService(subscriptionData.getCartridgeType());
				if(service != null) {
					subscriptionKey = service.getSubscriptionKey();
				}else {
					String msg = "Could not find service for cartridge type [" + subscriptionData.getCartridgeType() + "] " ;
					log.error(msg);				
					throw new ADCException(msg);
				}
			} catch (Exception e) {
				String msg = "Exception has occurred in get service for cartridge type [" + subscriptionData.getCartridgeType() + "] " ;
				log.error(msg);				
				throw new ADCException(msg, e);
			}
        }else {
        	// Generate and set the key
            subscriptionKey = CartridgeSubscriptionUtils.generateSubscriptionKey();

        }
        
        cartridgeSubscription.setSubscriptionKey(subscriptionKey);

        if(log.isDebugEnabled()) {
            log.debug("Repository with url: " + subscriptionData.getRepositoryURL() +
                    " username: " + subscriptionData.getRepositoryUsername() +
                    " Type: " + subscriptionData.getRepositoryType());
        }

        // Create subscriber
        Subscriber subscriber = new Subscriber(subscriptionData.getTenantAdminUsername(), subscriptionData.getTenantId(), subscriptionData.getTenantDomain());
        cartridgeSubscription.setSubscriber(subscriber);
        cartridgeSubscription.setAlias(subscriptionData.getCartridgeAlias());

        // Create repository
        Repository repository = cartridgeSubscription.manageRepository(subscriptionData.getRepositoryURL(), subscriptionData.getRepositoryUsername(),
        		subscriptionData.getRepositoryPassword(),
                subscriptionData.isPrivateRepository());

        // Update repository attributes
        if(repository != null) {
        	
            repository.setCommitEnabled(subscriptionData.isCommitsEnabled());
            
            // Encrypt repository password
            String encryptedRepoPassword;
            String repositoryPassword = repository.getPassword();
            if(repositoryPassword != null && !repositoryPassword.isEmpty()) {
            	encryptedRepoPassword = RepoPasswordMgtUtil.encryptPassword(repositoryPassword, subscriptionKey);
            } else {
            	encryptedRepoPassword = "";
            }
            repository.setPassword(encryptedRepoPassword);
            
        }

        // set the LB cluster id relevant to this service cluster
        cartridgeSubscription.setLbClusterId(lbClusterId);

        //create subscription
        cartridgeSubscription.createSubscription(subscriber, subscriptionData.getCartridgeAlias(), subscriptionData.getAutoscalingPolicyName(),
                                                subscriptionData.getDeploymentPolicyName(), repository);

		// publishing to bam
		CartridgeSubscriptionDataPublisher.publish(
				subscriptionData.getTenantId(),
				subscriptionData.getTenantAdminUsername(),
				subscriptionData.getCartridgeAlias(),
				subscriptionData.getCartridgeType(),
				subscriptionData.getRepositoryURL(),
				cartridgeInfo.getMultiTenant(),
				subscriptionData.getAutoscalingPolicyName(),
				subscriptionData.getDeploymentPolicyName(),
				cartridgeSubscription.getCluster().getClusterDomain(),
				cartridgeSubscription.getHostName(),
				cartridgeSubscription.getMappedDomain(), "Subscribed");
        
        // Add whether the subscription is enabled upstream git commits
        if(cartridgeSubscription.getPayloadData() != null) {
            cartridgeSubscription.getPayloadData().add(CartridgeConstants.COMMIT_ENABLED, String.valueOf(subscriptionData.isCommitsEnabled()));
        }

        if(subscriptionData.getProperties() != null){
            for(Property property : subscriptionData.getProperties().getProperties()){
                if (property.getName().startsWith(CartridgeConstants.CUSTOM_PAYLOAD_PARAM_NAME_PREFIX)) {
                    String payloadParamName = property.getName();
                    cartridgeSubscription.getPayloadData().add(payloadParamName.substring(payloadParamName.indexOf(".") + 1), property.getValue());
                }
            }
        }

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
     * @param persistence
     * @return SubscriptionInfo object populated with relevant information
     * @throws ADCException
     * @throws UnregisteredCartridgeException
     */
    public static SubscriptionInfo registerCartridgeSubscription(CartridgeSubscription cartridgeSubscription, Properties properties, Persistence persistence)
            throws ADCException, UnregisteredCartridgeException {

        CartridgeSubscriptionInfo cartridgeSubscriptionInfo = cartridgeSubscription.registerSubscription(properties, persistence);

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
        Set<String> clusterIds = new HashSet<String>();
        clusterIds.add(cartridgeSubscription.getCluster().getClusterDomain());
        CartridgeSubscriptionUtils.publishTenantSubscribedEvent(cartridgeSubscription.getSubscriber().getTenantId(), cartridgeSubscription.getCartridgeInfo().getType(), clusterIds);

        return ApplicationManagementUtil.
                createSubscriptionResponse(cartridgeSubscriptionInfo, cartridgeSubscription.getRepository());
    }

    public static void addSubscriptionDomain(int tenantId, String subscriptionAlias, String domainName, String applicationContext)
            throws ADCException {

        CartridgeSubscription cartridgeSubscription;
        try {
            cartridgeSubscription = getCartridgeSubscription(tenantId, subscriptionAlias);
            if(cartridgeSubscription == null) {
                throw new ADCException("Cartridge subscription not found");
            }

                if(!isSubscriptionDomainValid(domainName)) {
                    throw new ADCException(String.format("Domain name %s already registered", domainName));
                }

            cartridgeSubscription.addSubscriptionDomain(new SubscriptionDomain(domainName, applicationContext));
            new DataInsertionAndRetrievalManager().cacheAndPersistSubcription(cartridgeSubscription);

        } catch (PersistenceManagerException e) {
            String errorMsg = "Could not add domain to cartridge subscription: [tenant-id] " + tenantId + " [subscription-alias] " + subscriptionAlias +
            " [domain-name] " + domainName + " [application-context] " + applicationContext;
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }

        log.info("Successfully added domains to cartridge subscription: [tenant-id] " + tenantId + " [subscription-alias] " + subscriptionAlias +
                " [domain-name] " + domainName + " [application-context] " +applicationContext);


        Set<String> clusterIds = new HashSet<String>();
        clusterIds.add(cartridgeSubscription.getCluster().getClusterDomain());
        SubscriptionDomainAddedEvent event = new SubscriptionDomainAddedEvent(tenantId, cartridgeSubscription.getType(),
                clusterIds, domainName, applicationContext);
	    String topic = Util.getMessageTopicName(event);
	    EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
        eventPublisher.publish(event);
    }

    public static void removeSubscriptionDomain(int tenantId, String subscriptionAlias, String domainName) throws ADCException, DomainSubscriptionDoesNotExist {

        CartridgeSubscription cartridgeSubscription;
        try {
            cartridgeSubscription = getCartridgeSubscription(tenantId, subscriptionAlias);
            if(cartridgeSubscription == null) {
                throw new DomainSubscriptionDoesNotExist("Cartridge subscription not found", domainName);
            }
            cartridgeSubscription.removeSubscriptionDomain(domainName);
            new DataInsertionAndRetrievalManager().cacheAndPersistSubcription(cartridgeSubscription);

        } catch (PersistenceManagerException e) {
            String errorMsg = "Could not remove domain from cartridge subscription: [tenant-id] " + tenantId + " [subscription-alias] " + subscriptionAlias +
                    " [domain-name] " + domainName;
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }

        log.info("Successfully removed domain from cartridge subscription: [tenant-id] " + tenantId + " [subscription-alias] " + subscriptionAlias +
                " [domain-name] " + domainName);



        Set<String> clusterIds = new HashSet<String>();
        clusterIds.add(cartridgeSubscription.getCluster().getClusterDomain());
        SubscriptionDomainRemovedEvent event = new SubscriptionDomainRemovedEvent(tenantId, cartridgeSubscription.getType(),
                clusterIds, domainName);
	    String topic = Util.getMessageTopicName(event);
	    EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
        eventPublisher.publish(event);
    }

    public static List<SubscriptionDomain> getSubscriptionDomains(int tenantId, String subscriptionAlias)
            throws ADCException {

        try {
            CartridgeSubscription cartridgeSubscription = getCartridgeSubscription(tenantId, subscriptionAlias);
            if(cartridgeSubscription == null) {
                throw new ADCException("Cartridge subscription not found");
            }
            
            //return (List<SubscriptionDomain>) cartridgeSubscription.getSubscriptionDomains();
            return new ArrayList<SubscriptionDomain>(cartridgeSubscription.getSubscriptionDomains());
        } catch (Exception e) {
            String errorMsg = "Could not get domains of cartridge subscription: [tenant-id] " + tenantId + " [subscription-alias] " + subscriptionAlias;
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }
    }
    
    public static SubscriptionDomain getSubscriptionDomain(int tenantId, String subscriptionAlias, String domain)
            throws ADCException {

        try {
            CartridgeSubscription cartridgeSubscription = getCartridgeSubscription(tenantId, subscriptionAlias);
            if(cartridgeSubscription == null) {
                throw new ADCException("Cartridge subscription not found");
            }
            
            return cartridgeSubscription.getSubscriptionDomain(domain);
        } catch (Exception e) {
            String errorMsg = "Could not check [domain] "+domain+" against cartridge subscription: [tenant-id] " 
            					+ tenantId + " [subscription-alias] " + subscriptionAlias;
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }
    }

    public static boolean isSubscriptionDomainValid(String domainName) throws ADCException {
        try {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Validating domain: %s", domainName));
            }
            org.wso2.carbon.user.core.tenant.TenantManager tenantManager = DataHolder.getRealmService().getTenantManager();
            org.wso2.carbon.user.api.Tenant[] tenants = tenantManager.getAllTenants();
            if((tenants != null) && (tenants.length > 0)) {
                DataInsertionAndRetrievalManager manager = new DataInsertionAndRetrievalManager();
                for (org.wso2.carbon.user.api.Tenant tenant : tenants) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Reading subscriptions for tenant: [tenant-id] %d [tenant-domain] %s",
                                tenant.getId(), tenant.getDomain()));
                    }
                    Collection<CartridgeSubscription> subscriptions = manager.getCartridgeSubscriptions(tenant.getId());
                    if (subscriptions == null) {
                        continue;
                    }
                    for (CartridgeSubscription subscription : subscriptions) {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Reading domain names in subscription: [alias] %s [domain-names] %s",
                                    subscription.getAlias(), subscription.getSubscriptionDomains()));
                        }
                        if (subscription.subscriptionDomainExists(domainName)) {
                            return false;
                        }
                    }
                }
            }
            if(log.isDebugEnabled()) {
                log.debug(String.format("Domain name %s is valid", domainName));
            }
            return true;
        } catch (Exception e) {
            String errorMsg = "Could not validate domain:  " + domainName;
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }
    }

    public static Collection<CartridgeSubscription> getCartridgeSubscriptions (int tenantId, String type) throws ADCException {

        if (type == null || type.isEmpty()) {
            return new DataInsertionAndRetrievalManager().getCartridgeSubscriptions(tenantId);

        } else {
            return new DataInsertionAndRetrievalManager().getCartridgeSubscriptions(tenantId, type);
        }
    }

    public static CartridgeSubscription getCartridgeSubscription (int tenantId, String subscriptionAlias) {

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
    public static void unsubscribeFromCartridge (String tenantDomain, String alias)
            throws ADCException, NotSubscribedException {

        DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();

        CartridgeSubscription cartridgeSubscription = dataInsertionAndRetrievalManager.getCartridgeSubscription(CarbonContext.getThreadLocalCarbonContext().getTenantId(), alias);
        if(cartridgeSubscription != null) {
            cartridgeSubscription.removeSubscription();

            // Remove the information from Topology Model
            // Not needed now. TopologyModel is now changed so that information is taken from subscriptions
            //TopologyClusterInformationModel.getInstance().removeCluster(cartridgeSubscription.getSubscriber().getTenantId(),
            //        cartridgeSubscription.getType(), cartridgeSubscription.getAlias());

            // remove subscription
            try {
                dataInsertionAndRetrievalManager.removeSubscription(cartridgeSubscription.getSubscriber().getTenantId(), alias);

            } catch (PersistenceManagerException e) {
                String errorMsg = "Error removing subscription for tenant " + tenantDomain + ", alias " + cartridgeSubscription.getAlias();
                log.error(errorMsg);
                throw new ADCException(errorMsg, e);
            }

            // Publish tenant un-subscribed event to message broker

            Set<String> clusterIds = new HashSet<String>();
            clusterIds.add(cartridgeSubscription.getCluster().getClusterDomain());
            CartridgeSubscriptionUtils.publishTenantUnSubscribedEvent(
                    cartridgeSubscription.getSubscriber().getTenantId(),
                    cartridgeSubscription.getCartridgeInfo().getType(), clusterIds);
            
			// publishing to the unsubscribed event details to bam
			CartridgeSubscriptionDataPublisher.publish(cartridgeSubscription
					.getSubscriber().getTenantId(), cartridgeSubscription
					.getSubscriber().getAdminUserName(), cartridgeSubscription
					.getAlias(), cartridgeSubscription.getType(),
					"",
					cartridgeSubscription.getCartridgeInfo().getMultiTenant(),
					cartridgeSubscription.getAutoscalingPolicyName(),
					cartridgeSubscription.getDeploymentPolicyName(),
					cartridgeSubscription.getCluster().getClusterDomain(),
					cartridgeSubscription.getHostName(), cartridgeSubscription
							.getMappedDomain(), "unsubscribed");
        }
        else {
            String errorMsg = "No cartridge subscription found with [alias] " + alias + " for [tenant] " + tenantDomain;
            log.error(errorMsg);
            throw new NotSubscribedException(errorMsg, alias);
        }
    }

    private static Properties getPersistenceMappingProperties (PersistenceContext persistenceCtxt, CartridgeInfo cartridgeInfo) throws ADCException {

        if (!cartridgeInfo.isPersistenceSpecified()) {
            // Persistence Mapping not supported in the cartridge definition - error
            String errorMsg = "Persistence Mapping not supported by the cartridge type " + cartridgeInfo.getType();
            log.error(errorMsg);
            throw new ADCException(errorMsg);
        }

        Properties persistenceMappingProperties = new Properties();
        persistenceMappingProperties.setProperties(new Property[]{persistenceCtxt.getPersistanceRequiredProperty(), persistenceCtxt.getSizeProperty(),
                persistenceCtxt.getDeleteOnTerminationProperty(), persistenceCtxt.getVolumeIdProperty()});

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
    public static Collection<CartridgeSubscription> isCartridgeSubscribed(int tenantId, String cartridgeType) {
    	
    	DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();
        return dataInsertionAndRetrievalManager.getCartridgeSubscriptions(tenantId, cartridgeType);
    }

    public Collection<CartridgeSubscription> getCartridgeSubscriptionsForType (String cartridgeType) {

        return new DataInsertionAndRetrievalManager().getCartridgeSubscriptions(cartridgeType);
    }
}

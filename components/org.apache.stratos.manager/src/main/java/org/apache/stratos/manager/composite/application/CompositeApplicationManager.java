package org.apache.stratos.manager.composite.application;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.composite.application.beans.CompositeAppDefinition;
import org.apache.stratos.manager.composite.application.parser.CompositeApplicationParser;
import org.apache.stratos.manager.composite.application.parser.DefaultCompositeApplicationParser;
import org.apache.stratos.manager.composite.application.structure.CompositeAppContext;
import org.apache.stratos.manager.composite.application.structure.GroupContext;
import org.apache.stratos.manager.composite.application.structure.SubscribableContext;
import org.apache.stratos.manager.exception.*;
import org.apache.stratos.manager.manager.CartridgeSubscriptionManager;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.subscription.CompositeAppSubscription;
import org.apache.stratos.manager.subscription.GroupSubscription;
import org.apache.stratos.manager.subscription.SubscriptionData;

//Grouping
public class CompositeApplicationManager {
	
	private static Log log = LogFactory.getLog(CompositeApplicationManager.class);

    CartridgeSubscriptionManager cartridgeSubscriptionManager;

    public CompositeApplicationManager () {
        cartridgeSubscriptionManager = new CartridgeSubscriptionManager();
    }
	
	public void deployCompositeApplication (CompositeAppDefinition compositeAppDefinition, int tenantId, String tenantDomain,
                                            String tenantAdminUsername) throws CompositeApplicationException, CompositeApplicationDefinitionException,
            PersistenceManagerException {

        CompositeApplicationParser compositeAppParser = new DefaultCompositeApplicationParser();
        CompositeAppContext compositeAppContext = compositeAppParser.parse(compositeAppDefinition);

        log.info("Composite Application [ Id: " + compositeAppDefinition.getApplicationId() + " , alias: "
                + compositeAppDefinition.getAlias() + " ] deployed successfully");

        // create the CompositeAppSubscription
        CompositeAppSubscription compositeAppSubscription;
        try {
            compositeAppSubscription = cartridgeSubscriptionManager.createCompositeAppSubscription(compositeAppContext.getAppId(), tenantId);

        } catch (CompositeAppSubscriptionException e) {
            throw new CompositeApplicationDefinitionException(e);
        }

        // keep track of all CartridgeSubscriptions, against the alias
        Map<String, CartridgeSubscription> aliasToCartridgeSubscription = new HashMap<String, CartridgeSubscription>();

        // Keep track of all Group Subscriptions
        Map<String, GroupSubscription> groupAliasToGroupSubscription = new HashMap<String, GroupSubscription>();

        // traverse through the Composite App Structure and create Cartridge Subscriptions
        if(compositeAppContext.getSubscribableContexts() != null) {
            // Subscriptions relevant to top level Subscribables

            for (CartridgeSubscription cartridgeSubscription : getCartridgeSybscriptionsForSubscribables(compositeAppContext.getSubscribableContexts(),
                    tenantId, tenantDomain, tenantAdminUsername)) {

                // check if a Cartridge Subscription already exists with this alias for this Composite App
                if (cartridgeSubscriptionExistsForAlias(aliasToCartridgeSubscription, cartridgeSubscription.getAlias())) {
                    throw new CompositeApplicationException("Cartridge Subscription with alias [ " + cartridgeSubscription.getAlias()
                            + " ] already exists in Composite Application [ " + compositeAppSubscription.getAppId() + " ]");
                }

                aliasToCartridgeSubscription.put(cartridgeSubscription.getAlias(), cartridgeSubscription);
            }
            // get top level cartridge aliases to add to Composite App Subscription
            compositeAppSubscription.addCartridgeSubscriptionAliases(getCartrigdeSubscriptionAliases(compositeAppContext.getSubscribableContexts()));
        }

        if (compositeAppContext.getGroupContexts() != null) {
            // Subscriptions relevant to Groups

            for (CartridgeSubscription cartridgeSubscription : getCartridgeSubscriptionForGroups(compositeAppContext.getGroupContexts(), tenantId,
                    tenantDomain, tenantAdminUsername)) {

                // check if a Cartridge Subscription already exists with this alias for this Composite App
                if (cartridgeSubscriptionExistsForAlias(aliasToCartridgeSubscription, cartridgeSubscription.getAlias())) {
                    throw new CompositeApplicationException("Cartridge Subscription with alias [ " + cartridgeSubscription.getAlias()
                            + " ] already exists in Composite Application [ " + compositeAppSubscription.getAppId() + " ]");
                }

                aliasToCartridgeSubscription.put(cartridgeSubscription.getAlias(), cartridgeSubscription);
            }

            // create Group Subscriptions and collect them
            for (GroupSubscription groupSubscription : getGroupSubscriptions(compositeAppContext.getGroupContexts(), tenantId)) {

                // check if a Group Subscription already exists with this alias for this Composite App
                if (groupSubscriptionExistsForAlias(groupAliasToGroupSubscription, groupSubscription.getGroupAlias())) {
                    throw new CompositeApplicationException("Group Subscription with alias [ " + groupSubscription.getGroupAlias()
                            + " ] already exists in Composite Application [ " + compositeAppSubscription.getAppId() + " ]");
                }

                groupAliasToGroupSubscription.put(groupSubscription.getGroupAlias(), groupSubscription);
            }

            // set top level group aliases to Composite App Subscription
            compositeAppSubscription.addGroupSubscriptionAliases(getGroupSubscriptionAliases(compositeAppContext.getGroupContexts()));
        }
	}

    private Set<String> getCartrigdeSubscriptionAliases (Set<SubscribableContext> subscribableContexts) throws CompositeApplicationException {

        Set<String> cartridgeSubscriptionAliases = new HashSet<String>();
        for (SubscribableContext subscribableContext : subscribableContexts) {
            cartridgeSubscriptionAliases.add(subscribableContext.getAlias());
        }

        return cartridgeSubscriptionAliases;
    }

    private Set<GroupSubscription> getGroupSubscriptions (Set<GroupContext> groupContexts, int tenantID) throws CompositeApplicationException {

        Set<GroupSubscription> groupSubscriptions = new HashSet<GroupSubscription>();
        for (GroupContext groupContext : groupContexts) {
            // create Group Subscriptions for this Group
            GroupSubscription groupSubscription;
            try {
                groupSubscription = cartridgeSubscriptionManager.createGroupSubscription(groupContext.getName(), groupContext.getAlias(), tenantID);

            } catch (GroupSubscriptionException e) {
                throw new CompositeApplicationException(e);
            }
            if (groupContext.getSubscribableContexts() != null) {
                groupSubscription.addCartridgeSubscriptionAliases(getCartrigdeSubscriptionAliases(groupContext.getSubscribableContexts()));
            }

            // nested Group
            if (groupContext.getGroupContexts() != null) {
                groupSubscription.addGroupSubscriptionAliases(getGroupSubscriptionAliases(groupContext.getGroupContexts()));
                // need to recurse to get other nested groups, if any
                getGroupSubscriptions(groupContext.getGroupContexts(), tenantID);
            }

            groupSubscriptions.add(groupSubscription);
        }

        return groupSubscriptions;
    }

    private Set<String> getGroupSubscriptionAliases (Set<GroupContext> groupContexts) throws CompositeApplicationException {

        Set<String> topLevelGroupAliases = new HashSet<String>();

        for (GroupContext topLevelGroupCtxt : groupContexts) {
            topLevelGroupAliases.add(topLevelGroupCtxt.getAlias());
        }

        return topLevelGroupAliases;
    }

    private Set<CartridgeSubscription> getCartridgeSubscriptionForGroups (Set<GroupContext> groupContexts,
                                                                          int tenantId, String tenantDomain,
                                                                          String tenantAdminUsername)
            throws CompositeApplicationException {

        Set<CartridgeSubscription> cartridgeSubscriptions = new HashSet<CartridgeSubscription>();
        for (GroupContext groupContext : groupContexts) {
            // create Subscriptions for the Group's top level Subscribables
            if (groupContext.getSubscribableContexts() != null) {
                cartridgeSubscriptions.addAll(getCartridgeSybscriptionsForSubscribables(groupContext.getSubscribableContexts(),
                        tenantId, tenantDomain, tenantAdminUsername));
            }
            // create Subscriptions for the nested Group's Subscribables
            if (groupContext.getGroupContexts() != null) {
                cartridgeSubscriptions.addAll(getCartridgeSubscriptionForGroups(groupContext.getGroupContexts(), tenantId,
                        tenantDomain, tenantAdminUsername));
            }
        }

        return cartridgeSubscriptions;
    }

    private Set<CartridgeSubscription> getCartridgeSybscriptionsForSubscribables (Set<SubscribableContext> subscribableContexts,
                                                                                  int tenantId, String tenantDomain,
                                                                                  String tenantAdminUsername)
            throws CompositeApplicationException {

        Set<CartridgeSubscription> cartridgeSubscriptions = new HashSet<CartridgeSubscription>();

        for (SubscribableContext subscribableContext : subscribableContexts) {
            cartridgeSubscriptions.add(getCartridgeSubscription(subscribableContext, tenantId, tenantDomain, tenantAdminUsername));
        }

        return cartridgeSubscriptions;
    }

    private CartridgeSubscription getCartridgeSubscription (SubscribableContext subscribableContext, int tenantId, String tenantDomain,
                                                            String tenantAdminUsername) throws CompositeApplicationException {

        SubscriptionData subscriptionData = new SubscriptionData();
        subscriptionData.setCartridgeType(subscribableContext.getCartridgeType());
        subscriptionData.setCartridgeAlias(subscribableContext.getAlias());
        subscriptionData.setAutoscalingPolicyName(subscribableContext.getAutoscalingPolicy());
        subscriptionData.setDeploymentPolicyName(subscribableContext.getDeploymentPolicy());
        subscriptionData.setTenantId(tenantId);
        subscriptionData.setTenantDomain(tenantDomain);
        subscriptionData.setTenantAdminUsername(tenantAdminUsername);

        if (subscribableContext.getRepoUrl() != null && !subscribableContext.getRepoUrl().isEmpty()) {
            subscriptionData.setRepositoryType("git");
            subscriptionData.setRepositoryURL(subscribableContext.getRepoUrl());
            subscriptionData.setPrivateRepository(subscribableContext.isPrivateRepo());
            subscriptionData.setRepositoryUsername(subscribableContext.getUsername());
            subscriptionData.setRepositoryPassword(subscribableContext.getPassword());
        }

        CartridgeSubscriptionManager cartridgeSubscriptionManager = new CartridgeSubscriptionManager();
        try {
            return cartridgeSubscriptionManager.createCartridgeSubscription(subscriptionData);

        } catch (ADCException e) {
            throw new CompositeApplicationException(e);
        } catch (InvalidCartridgeAliasException e) {
            throw new CompositeApplicationException(e);
        } catch (DuplicateCartridgeAliasException e) {
            throw new CompositeApplicationException(e);
        } catch (PolicyException e) {
            throw new CompositeApplicationException(e);
        } catch (UnregisteredCartridgeException e) {
            throw new CompositeApplicationException(e);
        } catch (RepositoryRequiredException e) {
            throw new CompositeApplicationException(e);
        } catch (RepositoryCredentialsRequiredException e) {
            throw new CompositeApplicationException(e);
        } catch (RepositoryTransportException e) {
            throw new CompositeApplicationException(e);
        } catch (AlreadySubscribedException e) {
            throw new CompositeApplicationException(e);
        } catch (InvalidRepositoryException e) {
            throw new CompositeApplicationException(e);
        }

    }

    private boolean cartridgeSubscriptionExistsForAlias (Map<String, CartridgeSubscription> aliasToCartridgeSubscription,
                                                                 String newCartridgeSubscriptionAlias) {

        return aliasToCartridgeSubscription.get(newCartridgeSubscriptionAlias) != null;
    }


    private boolean groupSubscriptionExistsForAlias (Map<String, GroupSubscription> groupAliasToGroupSubscription,
                                                         String newGroupSubscriptionAlias) {

        return groupAliasToGroupSubscription.get(newGroupSubscriptionAlias) != null;
    }
	
//	public void unDeployCompositeApplication(String configApplicationAlias) throws ADCException {
//		if (log.isDebugEnabled()) {
//			log.debug("undeploying composite application " + configApplicationAlias);
//		}
//		// unregister application
//		unRegisterCompositeApplication(configApplicationAlias);
//		if (log.isDebugEnabled()) {
//			log.debug("publishing composite application removed event" + configApplicationAlias);
//		}
//		ApplicationUtils.publishApplicationUnDeployEvent(configApplicationAlias);
//		if (log.isDebugEnabled()) {
//			log.debug("composite application successfully removed " + configApplicationAlias);
//		}
//	}
	
//	private void registerCompositeApplication(ConfigCompositeApplication configCompositeApplication) throws ADCException {
//
//		try {
//			if (log.isDebugEnabled()) {
//				log.debug("registering composite application " + configCompositeApplication.getAlias());
//			}
//			DataInsertionAndRetrievalManager mgr = new DataInsertionAndRetrievalManager();
//			mgr.persistCompositeApplication ( configCompositeApplication);
//
//			if (log.isDebugEnabled()) {
//				log.debug("testing to retrieve persisted composite application ");
//				Collection<ConfigCompositeApplication> apps = mgr.getCompositeApplications();
//				log.debug("retrieved persisted composite application " + apps.size());
//				for (ConfigCompositeApplication app : apps) {
//					log.debug("retrieved persisted composite application " + app.getAlias());
//				}
//			}
//
//        } catch (PersistenceManagerException e) {
//            String errorMsg = "Error saving composite application " + configCompositeApplication.getAlias();
//            log.error(errorMsg);
//            throw new ADCException(errorMsg, e);
//        }
//
//        log.info("Successfully registered composite application " + configCompositeApplication.getAlias());
//
//	}
	
//	private void unRegisterCompositeApplication(String configApplicationAlias) throws ADCException {
//
//		try {
//			if (log.isDebugEnabled()) {
//				log.debug("unregistering composite application " + configApplicationAlias);
//			}
//			DataInsertionAndRetrievalManager mgr = new DataInsertionAndRetrievalManager();
//			mgr.removeCompositeApplication(configApplicationAlias);
//
//			if (log.isDebugEnabled()) {
//				log.debug("removed persisted composite application successfully");
//			}
//
//        } catch (PersistenceManagerException e) {
//            String errorMsg = "Error undeploying composite application " + configApplicationAlias;
//            log.error(errorMsg);
//            throw new ADCException(errorMsg, e);
//        }
//
//        log.info("Successfully undeployed composite application " + configApplicationAlias);
//
//	}
	
//	public void restoreCompositeApplications () throws ADCException {
//		try {
//			if (log.isDebugEnabled()) {
//				log.debug("restoring composite applications " );
//			}
//			DataInsertionAndRetrievalManager mgr = new DataInsertionAndRetrievalManager();
//			Collection<ConfigCompositeApplication> apps = mgr.getCompositeApplications();
//
//			if (apps == null) {
//				if (log.isDebugEnabled()) {
//					log.debug("no composite application configured");
//				}
//				return;
//			}
//			if (log.isDebugEnabled()) {
//				log.debug("retrieved persisted composite application " + apps.size());
//				for (ConfigCompositeApplication app : apps) {
//					log.debug("retrieved persisted composite application " + app.getAlias());
//				}
//			}
//			// sending application created event to restore in Toplogy
//			for (ConfigCompositeApplication app : apps) {
//				log.debug("restoring composite application " + app.getAlias());
//				ApplicationUtils.publishApplicationCreatedEvent(app);
//			}
//
//        } catch (PersistenceManagerException e) {
//            String errorMsg = "Error restoring composite application ";
//            log.error(errorMsg);
//            throw new ADCException(errorMsg, e);
//        }
//	}

}

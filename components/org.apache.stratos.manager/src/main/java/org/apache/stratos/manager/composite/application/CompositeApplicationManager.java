package org.apache.stratos.manager.composite.application;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.composite.application.beans.CompositeAppDefinition;
import org.apache.stratos.manager.composite.application.parser.CompositeApplicationParser;
import org.apache.stratos.manager.composite.application.parser.DefaultCompositeApplicationParser;
import org.apache.stratos.manager.composite.application.structure.CompositeAppContext;
import org.apache.stratos.manager.composite.application.structure.GroupContext;
import org.apache.stratos.manager.composite.application.structure.SubscribableContext;
import org.apache.stratos.manager.composite.application.utils.ApplicationUtils;
import org.apache.stratos.manager.exception.*;
import org.apache.stratos.manager.manager.CartridgeSubscriptionManager;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.subscription.SubscriptionData;
import org.apache.stratos.messaging.domain.topology.ConfigCompositeApplication;

//Grouping
public class CompositeApplicationManager {
	
	private static Log log = LogFactory.getLog(CompositeApplicationManager.class);
	
	public void deployCompositeApplication (CompositeAppDefinition compositeAppDefinition, int tenantId, String tenantDomain,
                                            String tenantAdminUsername) throws CompositeApplicationException, CompositeApplicationDefinitionException,
            PersistenceManagerException {

        CompositeApplicationParser compositeAppParser = new DefaultCompositeApplicationParser();
        CompositeAppContext compositeAppContext = compositeAppParser.parse(compositeAppDefinition);

        log.info("Composite Application [ Id: " + compositeAppDefinition.getApplicationId() + " , alias: "
                + compositeAppDefinition.getAlias() + " ] deployed successfully");
        
        //DataInsertionAndRetrievalManager mgr = new DataInsertionAndRetrievalManager();
        //mgr.persistCompositeApplication(compositeAppDefinition);
        Set<CartridgeSubscription> cartridgeSubscriptions = new HashSet<CartridgeSubscription>();

        // traverse through the Composite App Structure and create Cartridge Subscriptions
        if(compositeAppContext.getSubscribableContexts() != null) {
            // Subscription relevant to top level Subscribables
            cartridgeSubscriptions.addAll(getCartridgeSybscriptionsForSubscribables(compositeAppContext.getSubscribableContexts(),
                    tenantId, tenantDomain, tenantAdminUsername));
        }

        if (compositeAppContext.getGroupContexts() != null) {
            // Subscriptions relevant to Groups
            cartridgeSubscriptions.addAll(getCartridgeSubscriptionForGroups(compositeAppContext.getGroupContexts(), tenantId,
                    tenantDomain, tenantAdminUsername));
        }
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
	
	public void unDeployCompositeApplication(String configApplicationAlias) throws ADCException {
		if (log.isDebugEnabled()) {
			log.debug("undeploying composite application " + configApplicationAlias);
		}
		// unregister application
		unRegisterCompositeApplication(configApplicationAlias);
		if (log.isDebugEnabled()) {
			log.debug("publishing composite application removed event" + configApplicationAlias);
		}
		ApplicationUtils.publishApplicationUnDeployEvent(configApplicationAlias);
		if (log.isDebugEnabled()) {
			log.debug("composite application successfully removed " + configApplicationAlias);
		}
	}
	
	private void registerCompositeApplication(ConfigCompositeApplication configCompositeApplication) throws ADCException {
		
		try {
			if (log.isDebugEnabled()) {
				log.debug("registering composite application " + configCompositeApplication.getAlias());
			}
			DataInsertionAndRetrievalManager mgr = new DataInsertionAndRetrievalManager();
			mgr.persistCompositeApplication ( configCompositeApplication);
			
			if (log.isDebugEnabled()) {
				log.debug("testing to retrieve persisted composite application ");
				Collection<ConfigCompositeApplication> apps = mgr.getCompositeApplications();
				log.debug("retrieved persisted composite application " + apps.size());
				for (ConfigCompositeApplication app : apps) {
					log.debug("retrieved persisted composite application " + app.getAlias());
				}
			}

        } catch (PersistenceManagerException e) {
            String errorMsg = "Error saving composite application " + configCompositeApplication.getAlias();
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }

        log.info("Successfully registered composite application " + configCompositeApplication.getAlias());
		
	}
	
	private void unRegisterCompositeApplication(String configApplicationAlias) throws ADCException {
		
		try {
			if (log.isDebugEnabled()) {
				log.debug("unregistering composite application " + configApplicationAlias);
			}
			DataInsertionAndRetrievalManager mgr = new DataInsertionAndRetrievalManager();
			mgr.removeCompositeApplication(configApplicationAlias); 
			
			if (log.isDebugEnabled()) {
				log.debug("removed persisted composite application successfully");
			}

        } catch (PersistenceManagerException e) {
            String errorMsg = "Error undeploying composite application " + configApplicationAlias;
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }

        log.info("Successfully undeployed composite application " + configApplicationAlias);
		
	}
	
	public void restoreCompositeApplications () throws ADCException {
		try {
			if (log.isDebugEnabled()) {
				log.debug("restoring composite applications " );
			}
			DataInsertionAndRetrievalManager mgr = new DataInsertionAndRetrievalManager();
			Collection<ConfigCompositeApplication> apps = mgr.getCompositeApplications();
			
			if (apps == null) {
				if (log.isDebugEnabled()) {			
					log.debug("no composite application configured");
				}
				return;
			}
			if (log.isDebugEnabled()) {			
				log.debug("retrieved persisted composite application " + apps.size());
				for (ConfigCompositeApplication app : apps) {
					log.debug("retrieved persisted composite application " + app.getAlias());
				}
			}
			// sending application created event to restore in Toplogy
			for (ConfigCompositeApplication app : apps) {
				log.debug("restoring composite application " + app.getAlias());
				ApplicationUtils.publishApplicationCreatedEvent(app);
			}

        } catch (PersistenceManagerException e) {
            String errorMsg = "Error restoring composite application ";
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }
	}

}

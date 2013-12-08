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

package org.apache.stratos.adc.mgt.service;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.client.CloudControllerServiceClient;
import org.apache.stratos.adc.mgt.custom.domain.RegistryManager;
import org.apache.stratos.adc.mgt.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.adc.mgt.dto.*;
import org.apache.stratos.adc.mgt.exception.*;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
import org.apache.stratos.adc.mgt.internal.DataHolder;
import org.apache.stratos.adc.mgt.manager.CartridgeSubscriptionManager;
import org.apache.stratos.adc.mgt.utils.ApplicationManagementUtil;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
import org.apache.stratos.adc.mgt.utils.PersistenceManager;
import org.apache.stratos.adc.mgt.utils.PolicyHolder;
import org.apache.stratos.adc.topology.mgt.service.TopologyManagementService;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.event.tenant.TenantSubscribedEvent;
import org.apache.stratos.messaging.event.tenant.TenantUnSubscribedEvent;
import org.apache.stratos.messaging.util.Constants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.utils.DataPaginator;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 *
 *
 */
public class ApplicationManagementService extends AbstractAdmin {

    private static final Log log = LogFactory.getLog(ApplicationManagementService.class);
    private RegistryManager registryManager = new RegistryManager();
    private CartridgeSubscriptionManager cartridgeSubsciptionManager = new CartridgeSubscriptionManager();

    /*
     * Instantiate RepoNotificationService. Since this service is in the same 
     * component (org.apache.stratos.adc.mgt), a new object is created.
     * TODO: Is there a better way to get this service?
     */
    private RepoNotificationService repoNotificationService = new RepoNotificationService();

	/**
	 * Get Available Cartridges
	 * 
	 * @return Available Cartridges
	 */
	public Cartridge[] getAvailableCartridges(boolean multiTenant) throws ADCException {
		List<Cartridge> cartridges = getAvailableCartridges(null, multiTenant);
		// Following is very important when working with axis2
		return cartridges.isEmpty() ? new Cartridge[0] : cartridges.toArray(new Cartridge[cartridges.size()]);
	}

	/**
	 * Get Subscribed Cartridges
	 * 
	 * @return Subscribed Cartridges
	 */
	public Cartridge[] getSubscribedCartridges() throws ADCException {
		checkSuperTenant();
		List<Cartridge> cartridgeList = getSubscribedCartridges(null);
		// Following is very important when working with axis2
		return cartridgeList.isEmpty() ? new Cartridge[0] : cartridgeList.toArray(new Cartridge[cartridgeList.size()]);
	}

	/**
	 * Get available cartridges
	 */
	public CartridgeWrapper getPagedAvailableCartridges(String cartridgeSearchString, int pageNumber, boolean multiTenant)
			throws ADCException {
		checkSuperTenant();
		CartridgeWrapper cartridgeWrapper = new CartridgeWrapper();
		List<Cartridge> cartridges = getAvailableCartridges(cartridgeSearchString, multiTenant);
    	
		// TODO Improve pagination
		if (!cartridges.isEmpty()) {
			// Paginate only if cartridges are there.
			DataPaginator.doPaging(pageNumber, cartridges, cartridgeWrapper);
		} else {
			// Must set this value as axis2 stub client will return an array
			// with length = 1 and null element if cartridges[] is null
			cartridgeWrapper.set(cartridges);
		}
		return cartridgeWrapper;
	}

	private List<Cartridge> getAvailableCartridges(String cartridgeSearchString, Boolean multiTenant) throws ADCException {
		List<Cartridge> cartridges = new ArrayList<Cartridge>();
		
		if (log.isDebugEnabled()) {
			log.debug("Getting available cartridges. Search String: " + cartridgeSearchString + ", Multi-Tenant: " + multiTenant);
		}
		
		boolean allowMultipleSubscription = new Boolean(
				System.getProperty(CartridgeConstants.FEATURE_MULTI_TENANT_MULTIPLE_SUBSCRIPTION_ENABLED));

		try {
			Pattern searchPattern = getSearchStringPattern(cartridgeSearchString);

			String[] availableCartridges = CloudControllerServiceClient.getServiceClient().getRegisteredCartridges();

			if (availableCartridges != null) {
				for (String cartridgeType : availableCartridges) {
					CartridgeInfo cartridgeInfo = null;
					try {
						cartridgeInfo = CloudControllerServiceClient.getServiceClient().getCartridgeInfo(cartridgeType);
					} catch (Exception e) {
						if (log.isWarnEnabled()) {
							log.warn("Error when calling getCartridgeInfo for " + cartridgeType + ", Error: "
									+ e.getMessage());
						}
					}
					if (cartridgeInfo == null) {
						// This cannot happen. But continue
						if (log.isDebugEnabled()) {
							log.debug("Cartridge Info not found: " + cartridgeType);
						}
						continue;
					}
					
					if (multiTenant != null && !multiTenant && cartridgeInfo.getMultiTenant()) {
						// Need only Single-Tenant cartridges
						continue;
					} else if (multiTenant != null && multiTenant && !cartridgeInfo.getMultiTenant()) {
						// Need only Multi-Tenant cartridges
						continue;
					}
					
					if (!cartridgeMatches(cartridgeInfo, searchPattern)) {
						continue;
					}
					
					Cartridge cartridge = new Cartridge();
					cartridge.setCartridgeType(cartridgeType);
					cartridge.setProvider(cartridgeInfo.getProvider());
					cartridge.setDisplayName(cartridgeInfo.getDisplayName());
					cartridge.setDescription(cartridgeInfo.getDescription());
					cartridge.setVersion(cartridgeInfo.getVersion());
					cartridge.setMultiTenant(cartridgeInfo.getMultiTenant());
					cartridge.setStatus(CartridgeConstants.NOT_SUBSCRIBED);
					cartridge.setCartridgeAlias("-");
					cartridge.setActiveInstances(0);
					cartridges.add(cartridge);
					
					if (cartridgeInfo.getMultiTenant() && !allowMultipleSubscription) {
						// If the cartridge is multi-tenant. We should not let users
						// createSubscription twice.
						if (PersistenceManager.isAlreadySubscribed(cartridgeType,
								ApplicationManagementUtil.getTenantId(getConfigContext()))) {
							if (log.isDebugEnabled()) {
								log.debug("Already subscribed to " + cartridgeType
										+ ". This multi-tenant cartridge will not be available to createSubscription");
							}
							cartridge.setStatus(CartridgeConstants.SUBSCRIBED);
						}
					}
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("There are no available cartridges");
				}
			}
		} catch (Exception e) {
			String msg = "Error when getting available cartridges. " + e.getMessage();
			log.error(msg, e);
			throw new ADCException("An error occurred getting available cartridges ", e);
		}

		Collections.sort(cartridges);

		if (log.isDebugEnabled()) {
			log.debug("Returning available cartridges " + cartridges.size());
		}

		return cartridges;
	}

	/**
	 * Get subscribed cartridges
	 */
	public CartridgeWrapper getPagedSubscribedCartridges(String cartridgeSearchString, int pageNumber)
			throws ADCException {
		checkSuperTenant();
		CartridgeWrapper cartridgeWrapper = new CartridgeWrapper();
		List<Cartridge> cartridges = getSubscribedCartridges(cartridgeSearchString);

		// TODO Improve pagination
		if (!cartridges.isEmpty()) {
			// Paginate only if cartridges are there.
			DataPaginator.doPaging(pageNumber, cartridges, cartridgeWrapper);
		} else {
			cartridgeWrapper.set(cartridges);
		}
		return cartridgeWrapper;
	}

	private List<Cartridge> getSubscribedCartridges(String cartridgeSearchString) throws ADCException {
		List<Cartridge> cartridges = new ArrayList<Cartridge>();
		
		if (log.isDebugEnabled()) {
			log.debug("Getting subscribed cartridges. Search String: " + cartridgeSearchString);
		}

		try {
			Pattern searchPattern = getSearchStringPattern(cartridgeSearchString);

			List<CartridgeSubscriptionInfo> subscriptionList = PersistenceManager
					.retrieveSubscribedCartridges(ApplicationManagementUtil.getTenantId(getConfigContext()));

			if (subscriptionList != null && !subscriptionList.isEmpty()) {
				for (CartridgeSubscriptionInfo subscription : subscriptionList) {
					CartridgeInfo cartridgeInfo = null;
					try {
						cartridgeInfo = CloudControllerServiceClient.getServiceClient().getCartridgeInfo(
								subscription.getCartridge());
					} catch (Exception e) {
						if (log.isWarnEnabled()) {
							log.warn("Error when calling getCartridgeInfo for " + subscription.getCartridge()
									+ ", Error: " + e.getMessage());
						}
					}
					if (cartridgeInfo == null) {
						// This cannot happen. But continue
						if (log.isDebugEnabled()) {
							log.debug("Cartridge Info not found: " + subscription.getCartridge());
						}
						continue;
					}
					if (!cartridgeMatches(cartridgeInfo, subscription, searchPattern)) {
						continue;
					}
					TopologyManagementService topologyMgtService = DataHolder.getTopologyMgtService();
					String[] ips = topologyMgtService.getActiveIPs(subscription.getCartridge(),
							subscription.getClusterDomain(), subscription.getClusterSubdomain());
					Cartridge cartridge = ApplicationManagementUtil.populateCartridgeInfo(cartridgeInfo, subscription, ips, getTenantDomain());
					cartridges.add(cartridge);
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("There are no subscribed cartridges");
				}
			}
		} catch (Exception e) {
			String msg = "Error when getting subscribed cartridges. " + e.getMessage();
			log.error(msg, e);
			throw new ADCException("An Error occurred when getting subscribed cartridges.", e);
		}

		Collections.sort(cartridges);

		if (log.isDebugEnabled()) {
			log.debug("Returning subscribed cartridges " + cartridges.size());
		}

		return cartridges;
	}

    private Pattern getSearchStringPattern(String searchString) {
        if (log.isDebugEnabled()) {
            log.debug("Creating search pattern for " + searchString);
        }
        if (searchString != null) {
            // Copied from org.wso2.carbon.webapp.mgt.WebappAdmin.doesWebappSatisfySearchString(WebApplication, String)
            String regex = searchString.toLowerCase().replace("..?", ".?").replace("..*", ".*").replaceAll("\\?", ".?")
                    .replaceAll("\\*", ".*?");
            if (log.isDebugEnabled()) {
                log.debug("Created regex: " + regex + " for search string " + searchString);
            }

            Pattern pattern = Pattern.compile(regex);
            return pattern;
        }
        return null;
    }

    // TODO: Improve search method
    private boolean cartridgeMatches(CartridgeInfo cartridgeInfo, Pattern pattern) {
        if (pattern != null) {
            boolean matches = false;
            if (cartridgeInfo.getDisplayName() != null) {
                matches = pattern.matcher(cartridgeInfo.getDisplayName().toLowerCase()).find();
            }
            if (!matches && cartridgeInfo.getDescription() != null) {
                matches = pattern.matcher(cartridgeInfo.getDescription().toLowerCase()).find();
            }
            return matches;
        }
        return true;
    }

    private boolean cartridgeMatches(CartridgeInfo cartridgeInfo, CartridgeSubscriptionInfo cartridgeSubscriptionInfo, Pattern pattern) {
        if (pattern != null) {
            boolean matches = false;
            if (cartridgeInfo.getDisplayName() != null) {
                matches = pattern.matcher(cartridgeInfo.getDisplayName().toLowerCase()).find();
            }
            if (!matches && cartridgeInfo.getDescription() != null) {
                matches = pattern.matcher(cartridgeInfo.getDescription().toLowerCase()).find();
            }
            if (!matches && cartridgeSubscriptionInfo.getCartridge() != null) {
                matches = pattern.matcher(cartridgeSubscriptionInfo.getCartridge().toLowerCase()).find();
            }
            if (!matches && cartridgeSubscriptionInfo.getAlias() != null) {
                matches = pattern.matcher(cartridgeSubscriptionInfo.getAlias().toLowerCase()).find();
            }
            return matches;
        }
        return true;
    }
    
    public Cartridge getCartridgeInfo(String alias) throws ADCException, NotSubscribedException {
    	checkSuperTenant();
    	return ApplicationManagementUtil.getCartridgeInfo(alias, getTenantDomain());
    }

    public String[] getSubscribedCartridgeAliases() throws AxisFault {
        try {
            List<CartridgeSubscriptionInfo> subscriptionList = PersistenceManager
                    .retrieveSubscribedCartridges(ApplicationManagementUtil.getTenantId(getConfigContext()));
            List<String> subscribedAliases = new ArrayList<String>();
            for (CartridgeSubscriptionInfo cartridgeSubscriptionInfo : subscriptionList) {

                if(cartridgeSubscriptionInfo.getProvider().equalsIgnoreCase(CartridgeConstants.PROVIDER_NAME_WSO2) &&
                        getCartridgeInfo(cartridgeSubscriptionInfo.getAlias()).isMultiTenant()) {

                    subscribedAliases.add(cartridgeSubscriptionInfo.getCartridge());
                } else {
                    subscribedAliases.add(cartridgeSubscriptionInfo.getAlias());
                }
            }
            if(subscribedAliases.size() == 0) {
                return new String[]{""};
            } else {
                return subscribedAliases.toArray(new String[subscribedAliases.size()]);
            }

        } catch (Exception e) {
            String msg = "Exception in getting subscribed cartridge aliases :" + e.getMessage();
            log.error(msg, e);
            throw new AxisFault("An error occurred while getting subscribed cartridge aliases", e);
        }
    }
    
	public PolicyDefinition[] getPolicyDefinitions() {
		List<PolicyDefinition> policyDefinitions = PolicyHolder.getInstance().getPolicyDefinitions();
		return policyDefinitions == null || policyDefinitions.isEmpty() ? new PolicyDefinition[0] : policyDefinitions
				.toArray(new PolicyDefinition[policyDefinitions.size()]);
	}

	/**
	 * Subscribe to a cartridge
	 */
	public SubscriptionInfo subscribe(String cartridgeType, String alias, String policy, String repoURL,
			boolean privateRepo, String repoUsername, String repoPassword, String dataCartridgeType,
			String dataCartridgeAlias) throws ADCException, PolicyException, UnregisteredCartridgeException,
            InvalidCartridgeAliasException, DuplicateCartridgeAliasException, RepositoryRequiredException,
            AlreadySubscribedException, RepositoryCredentialsRequiredException, InvalidRepositoryException,
            RepositoryTransportException {

		checkSuperTenant();

		//return ApplicationManagementUtil.doSubscribe(cartridgeType, alias, policy, repoURL, privateRepo, repoUsername,
		//		repoPassword, dataCartridgeType, dataCartridgeAlias, getUsername(),
		//		ApplicationManagementUtil.getTenantId(getConfigContext()), getTenantDomain());

        CartridgeSubscription cartridgeSubscription = cartridgeSubsciptionManager.subscribeToCartridge(cartridgeType,
                alias.trim(), "economyPolicy", "economy-deployment", getTenantDomain(), ApplicationManagementUtil.getTenantId(configurationContext),
                getUsername(), "git", repoURL, privateRepo, repoUsername, repoPassword);

        if(dataCartridgeAlias != null && !dataCartridgeAlias.trim().isEmpty()) {

            dataCartridgeAlias = dataCartridgeAlias.trim();

            CartridgeSubscription connectingCartridgeSubscription = null;
            try {
                connectingCartridgeSubscription = cartridgeSubsciptionManager.getCartridgeSubscription(getTenantDomain(),
                        dataCartridgeAlias);
            } catch (NotSubscribedException e) {
                log.error(e.getMessage(), e);
            }
            if (connectingCartridgeSubscription != null) {
                // Publish tenant subscribed event
                publishTenantSubscribedEvent(getTenantId(), connectingCartridgeSubscription.getCartridgeInfo().getType());

                try {
                    cartridgeSubsciptionManager.connectCartridges(getTenantDomain(), cartridgeSubscription,
                            connectingCartridgeSubscription.getAlias());

                } catch (NotSubscribedException e) {
                    log.error(e.getMessage(), e);

                } catch (AxisFault axisFault) {
                    log.error(axisFault.getMessage(), axisFault);
                }
            } else {
                log.error("Failed to connect. No cartridge subscription found for tenant " +
                        ApplicationManagementUtil.getTenantId(configurationContext) + " with alias " + alias);
            }
        }

        return cartridgeSubsciptionManager.registerCartridgeSubscription(cartridgeSubscription);

	}

    private void publishTenantSubscribedEvent(int tenantId, String serviceName) {
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Publishing tenant subscribed event: [tenant-id] %d [service] %s", tenantId, serviceName));
            }
            TenantSubscribedEvent subscribedEvent = new TenantSubscribedEvent(tenantId, serviceName);
            EventPublisher eventPublisher = new EventPublisher(Constants.TENANT_TOPIC);
            eventPublisher.publish(subscribedEvent);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Could not publish tenant subscribed event: [tenant-id] %d [service] %s", tenantId, serviceName), e);
            }
        }
    }

    private void publishTenantUnSubscribedEvent(int tenantId, String serviceName) {
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Publishing tenant un-subscribed event: [tenant-id] %d [service] %s", tenantId, serviceName));
            }
            TenantUnSubscribedEvent event = new TenantUnSubscribedEvent(tenantId, serviceName);
            EventPublisher eventPublisher = new EventPublisher(Constants.TENANT_TOPIC);
            eventPublisher.publish(event);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Could not publish tenant un-subscribed event: [tenant-id] %d [service] %s", tenantId, serviceName), e);
            }
        }
    }

    /**
     * Unsubscribing the cartridge
     *
     * @param alias name of the cartridge to be unsubscribed
     */
    public void unsubscribe(String alias) throws ADCException, NotSubscribedException {

        checkSuperTenant();

        // Find subscription of alias
        CartridgeSubscriptionInfo subscription;
        String errorMessage = String.format("Tenant %s is not subscribed for %s", getTenantDomain(), alias);
        try {
            subscription = PersistenceManager.getSubscription(getTenantDomain(), alias);
            if(subscription == null) {
                throw new ADCException(errorMessage);
            }
        } catch (Exception e) {
            throw new ADCException(errorMessage);
        }

        // Un-subscribe from cartridge
        cartridgeSubsciptionManager.unsubscribeFromCartridge(getTenantDomain(), alias);

        // Publish tenant un-subscribed event
        String serviceName = subscription.getCartridge();
        publishTenantUnSubscribedEvent(getTenantId(), serviceName);


        /*CartridgeSubscriptionInfo subscription = null;
        
        try {
			subscription = PersistenceManager.getSubscription(getTenantDomain(), alias);
		} catch (Exception e) {
			String msg = "Failed to get subscription for " + getTenantDomain() + " and alias " + alias;
            log.error(msg, e);
			throw new ADCException(msg, e);
		}

        if (subscription == null) {
            String msg = "Tenant " + getTenantDomain() + " is not subscribed for " + alias;
            log.error(msg);
            throw new NotSubscribedException("You have not subscribed for " + alias, alias);
        }
        
        try {
            String clusterDomain = subscription.getClusterDomain();
            String clusterSubDomain = subscription.getClusterSubdomain();

            if (log.isDebugEnabled()) {
                log.debug("Finding cartridge information for " + subscription.getCartridge());
            }
            CartridgeInfo cartridgeInfo = CloudControllerServiceClient.getServiceClient().getCartridgeInfo(subscription.getCartridge());
            if (log.isDebugEnabled()) {
                log.debug("Found " + cartridgeInfo.getDisplayName() + " for " + subscription.getCartridge());
            }
            if (!cartridgeInfo.getMultiTenant()) {
                log.info("Terminating all instances of " + clusterDomain + " " + clusterSubDomain);
                CloudControllerServiceClient.getServiceClient().terminateAllInstances(clusterDomain, clusterSubDomain);
                log.info("All instances terminated.");
                log.info("Unregistering services...");
                CloudControllerServiceClient.getServiceClient().unregisterService(clusterDomain, clusterSubDomain);
                log.info("Successfully terminated instances ..");
            } else {
                if (log.isInfoEnabled()) {
                    log.info("Cartridge "
                            + subscription.getCartridge()
                            + " is a multi-tenant cartridge and therefore will not terminate all instances and unregister services");
                }
            }

            new RepositoryFactory().destroyRepository(alias, getTenantDomain(), getUsername());
            log.info("Repo is destroyed successfully.. ");

            PersistenceManager.updateSubscriptionState(subscription.getSubscriptionId(), "UNSUBSCRIBED");
            new DNSManager().removeSubDomain(subscription.getHostName());
            registryManager.removeDomainMappingFromRegistry(subscription.getHostName());
            TopologyManagementService topologyMgtService = DataHolder.getTopologyMgtService();
			
            String[] ips = topologyMgtService.getActiveIPs(subscription.getCartridge(),
					subscription.getClusterDomain(), subscription.getClusterSubdomain());
            PersistenceManager.updateInstanceState("INACTIVE", ips, subscription.getClusterDomain(), subscription.getClusterSubdomain(), subscription.getCartridge());

        } catch (ADCException e) {
        	log.error(e.getMessage(), e);
        	throw e;
        } catch (Exception e1) {
            String msg1 = "Exception occurred :" + e1.getMessage();
            log.error(msg1);
            throw new ADCException("Unsubscribe failed for cartridge " + alias, e1);
        }*/
    }


    public String addDomainMapping(String mappedDomain, String cartridgeAlias) throws ADCException, DomainMappingExistsException, NotSubscribedException {
    	checkSuperTenant();
    	// TODO Following was in CLI. Fix this
		//		if (domain.indexOf('.') == -1) {
		//			System.out.println("\nOwn domain should include a '.' ");
		//		}

        CartridgeSubscriptionInfo subscription = null;
        String actualHost = null;
        
        try {
			subscription = PersistenceManager.getSubscription(getTenantDomain(), cartridgeAlias);
		} catch (Exception e) {
			String msg = "Failed to get subscription for " + getTenantDomain() + " and alias " + cartridgeAlias;
            log.error(msg, e);
			throw new ADCException(msg, e);
		}
        if (subscription == null) {
        	String msg = "Tenant " + getTenantDomain() + " is not subscribed for " + cartridgeAlias;
            log.error(msg);
            throw new NotSubscribedException("You have not subscribed for " + cartridgeAlias, cartridgeAlias);
        }

        try {
        	actualHost = getActualHost(cartridgeAlias);
            registryManager.addDomainMappingToRegistry(mappedDomain, actualHost);
            log.info("Domain mapping is added for " + mappedDomain + " tenant: " + getTenantDomain());
            PersistenceManager.updateDomainMapping(
                    ApplicationManagementUtil.getTenantId(getConfigContext()), cartridgeAlias, mappedDomain);
        } catch (RegistryException e) {
            String msg = "Unable to add the mapping due to registry transaction error";
            log.error(msg, e);
            throw new ADCException("Unable to add the mapping due to internal error!", e);
        } catch (DomainMappingExistsException e) {
            String msg = "Domain mapping already exists.";
            log.error(msg, e);
            throw e;
        } catch (Exception e) {
            String msg = "Error occurred. Reason : " + e.getMessage();
            log.error(msg, e);
            throw new ADCException(msg, e);
        }
        return actualHost;
    }

	private String getActualHost(String cartridgeName) throws Exception {
		return PersistenceManager.getHostNameForCartridgeName(
				ApplicationManagementUtil.getTenantId(getConfigContext()), cartridgeName);
	}

    public void removeDomainMapping(String cartridgeAlias) throws ADCException, NotSubscribedException {
    	checkSuperTenant();
        CartridgeSubscriptionInfo subscription = null;
        String actualHost = null;
        
        try {
			subscription = PersistenceManager.getSubscription(getTenantDomain(), cartridgeAlias);
		} catch (Exception e) {
			String msg = "Failed to get subscription for " + getTenantDomain() + " and alias " + cartridgeAlias;
            log.error(msg, e);
			throw new ADCException(msg, e);
		}
        if (subscription == null) {
        	String msg = "Tenant " + getTenantDomain() + " is not subscribed for " + cartridgeAlias;
            log.error(msg);
            throw new NotSubscribedException("You have not subscribed for " + cartridgeAlias, cartridgeAlias);
        }
        
        try {
        	actualHost = getActualHost(cartridgeAlias);
            registryManager.removeDomainMappingFromRegistry(actualHost);
            log.info("Domain mapping is removed for " + actualHost + " tenant: " + getTenantDomain());
            PersistenceManager.updateDomainMapping(ApplicationManagementUtil.getTenantId(getConfigContext()),
                    cartridgeAlias, null);
        } catch (RegistryException e) {
            String msg = "Unable to remove the mapping due to registry transaction error";
            log.error(msg, e);
            throw new ADCException("Unable to remove the mapping due to internal error!", e);
        } catch (Exception e) {
            String msg = "Error occurred. Reason : " + e.getMessage();
            log.error(msg, e);
            throw new ADCException(msg, e);
        }
    }

	public void synchronizeRepository(String cartridgeAlias) throws ADCException, NotSubscribedException {
		checkSuperTenant();
        CartridgeSubscriptionInfo subscription = null;
        
        // Validating subscription
        try {
			subscription = PersistenceManager.getSubscription(getTenantDomain(), cartridgeAlias);
		} catch (Exception e) {
			String msg = "Failed to get subscription for " + getTenantDomain() + " and alias " + cartridgeAlias;
            log.error(msg, e);
			throw new ADCException(msg, e);
		}
        if (subscription == null) {
        	String msg = "Tenant " + getTenantDomain() + " is not subscribed for " + cartridgeAlias;
            log.error(msg);
            throw new NotSubscribedException("You have not subscribed for " + cartridgeAlias, cartridgeAlias);
        }
		
		try {
			repoNotificationService.notifyRepoUpdate(getTenantDomain(), cartridgeAlias);
		} catch (Exception e) {
			throw new ADCException(e.getMessage() != null ? e.getMessage() : "Failed to synchronize repository", e);
		}
	}

	public String getTenantDomain() {
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        return carbonContext.getTenantDomain();
	}

    public int getTenantId() {
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        return carbonContext.getTenantId();
    }
    
	/**
	 * Allow to check whether features are enabled in the back-end
	 * 
	 * @param key
	 *            The propery key
	 * @return {@code true} if feature is enabled
	 */
	public boolean isFeatureEnabled(String key) {
		if (key != null && key.startsWith("feature.")) {
			return new Boolean(System.getProperty(key));
		}
		return false;
	}

	
	public RepositoryInformation testRepositoryConnection(String repoURL, String repoUsername, String repoPassword, boolean privateRepo)
			throws RepositoryRequiredException, ADCException, RepositoryCredentialsRequiredException,
			InvalidRepositoryException, RepositoryTransportException {
		return ApplicationManagementUtil.validateRepository(repoURL, repoUsername, repoPassword, privateRepo, true);
	}
	
	// TODO Remove following when we support cartridge subscription for Super-tenant
	private void checkSuperTenant() throws ADCException {
		if (log.isDebugEnabled()) {
			log.debug("Checking whether super tenant accesses the service methods. Tenant ID: "
					+ ApplicationManagementUtil.getTenantId(getConfigContext()) + ", Tenant Domain: " + getTenantDomain());
		}
		if (MultitenantConstants.SUPER_TENANT_ID == ApplicationManagementUtil.getTenantId(getConfigContext())) {
			throw new ADCException("Super Tenant is not allowed to complete requested operation");
		}
	}
}

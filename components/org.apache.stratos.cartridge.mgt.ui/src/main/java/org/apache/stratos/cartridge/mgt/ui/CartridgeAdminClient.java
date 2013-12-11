/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.cartridge.mgt.ui;

import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.dto.CartridgeWrapper;
import org.apache.stratos.adc.mgt.dto.PolicyDefinition;
import org.apache.stratos.adc.mgt.dto.RepositoryInformation;
import org.apache.stratos.adc.mgt.dto.SubscriptionInfo;
import org.apache.stratos.adc.mgt.stub.ApplicationManagementServiceADCExceptionException;
import org.apache.stratos.adc.mgt.stub.ApplicationManagementServiceAlreadySubscribedExceptionException;
import org.apache.stratos.adc.mgt.stub.ApplicationManagementServiceDomainMappingExistsExceptionException;
import org.apache.stratos.adc.mgt.stub.ApplicationManagementServiceDuplicateCartridgeAliasExceptionException;
import org.apache.stratos.adc.mgt.stub.ApplicationManagementServiceInvalidCartridgeAliasExceptionException;
import org.apache.stratos.adc.mgt.stub.ApplicationManagementServiceInvalidRepositoryExceptionException;
import org.apache.stratos.adc.mgt.stub.ApplicationManagementServiceNotSubscribedExceptionException;
import org.apache.stratos.adc.mgt.stub.ApplicationManagementServicePolicyExceptionException;
import org.apache.stratos.adc.mgt.stub.ApplicationManagementServiceRepositoryCredentialsRequiredExceptionException;
import org.apache.stratos.adc.mgt.stub.ApplicationManagementServiceRepositoryRequiredExceptionException;
import org.apache.stratos.adc.mgt.stub.ApplicationManagementServiceRepositoryTransportExceptionException;
import org.apache.stratos.adc.mgt.stub.ApplicationManagementServiceStub;
import org.apache.stratos.adc.mgt.stub.ApplicationManagementServiceUnregisteredCartridgeExceptionException;

/**
 * Client which communicates with the Application Management service of ADC
 */
public class CartridgeAdminClient {
    public static final String BUNDLE = "org.apache.stratos.cartridge.mgt.ui.i18n.Resources";
    public static final int MILLISECONDS_PER_MINUTE = 60 * 1000;
    private static final Log log = LogFactory.getLog(CartridgeAdminClient.class);
    private ResourceBundle bundle;
    public ApplicationManagementServiceStub stub;

	public CartridgeAdminClient(String cookie, String backendServerURL, ConfigurationContext configCtx, Locale locale) throws AxisFault {
		if (log.isDebugEnabled()) {
			log.debug("Creating CartridgeAdminClient for " + backendServerURL);
		}
		if (cookie == null || cookie.trim().length() == 0) {
			if (log.isDebugEnabled()) {
				log.debug("Cookie not found. Cannot create CartridgeAdminClient for " + backendServerURL);
			}
			throw new RuntimeException("Session has expired");
		}
		String serviceURL = backendServerURL + "ApplicationManagementService";
		bundle = ResourceBundle.getBundle(BUNDLE, locale);

		stub = new ApplicationManagementServiceStub(configCtx, serviceURL);
		ServiceClient client = stub._getServiceClient();
		Options option = client.getOptions();
		option.setManageSession(true);
		option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
		option.setProperty(Constants.Configuration.ENABLE_MTOM, Constants.VALUE_TRUE);
	}

	public CartridgeWrapper getPagedAvailableCartridges(String cartridgeSearchString, int pageNumber, boolean multiTenant) {
		try {
			return stub.getPagedAvailableCartridges(cartridgeSearchString, pageNumber, multiTenant);
		} catch (ApplicationManagementServiceADCExceptionException e) {
			handleException("cannot.list.available.cartridges", e);
		} catch (RemoteException e) {
			handleException("remote.error", e);
		}
		return null;
	}
    
	public CartridgeWrapper getPagedSubscribedCartridges(String cartridgeSearchString, int pageNumber) {
		try {
			return stub.getPagedSubscribedCartridges(cartridgeSearchString, pageNumber);
		} catch (ApplicationManagementServiceADCExceptionException e) {
			handleException("cannot.list.subscribed.cartridges", e);
		} catch (RemoteException e) {
			handleException("remote.error", e);
		}
		return null;
	}

	public String addDomainMapping(String mappedDomain, String cartridgeAlias) {
		String host = null;
		try {
			host = stub.addDomainMapping(mappedDomain, cartridgeAlias);
		} catch (ApplicationManagementServiceADCExceptionException e) {
			handleException("cannot.mapdomain", e);
		} catch (ApplicationManagementServiceNotSubscribedExceptionException e) {
			handleException("notsubscribed.error", e, cartridgeAlias);
		} catch (RemoteException e) {
			handleException("remote.error", e);
		} catch (ApplicationManagementServiceDomainMappingExistsExceptionException e) {
			handleException("domainmapping.exists.error", e, mappedDomain, cartridgeAlias);
		}

		return host;
	}
	
	public void removeDomainMapping(String cartridgeAlias) {
		try {
			stub.removeDomainMapping(cartridgeAlias);
		} catch (RemoteException e) {
			handleException("remote.error", e);
		} catch (ApplicationManagementServiceADCExceptionException e) {
			handleException("cannot.removedomain", e);
		} catch (ApplicationManagementServiceNotSubscribedExceptionException e) {
			handleException("notsubscribed.error", e, cartridgeAlias);
		}
	}
	
	public void synchronizeRepository(String cartridgeAlias) {
		try {
			stub.synchronizeRepository(cartridgeAlias);
		} catch (RemoteException e) {
			handleException("remote.error", e);
		} catch (ApplicationManagementServiceADCExceptionException e) {
			handleException("cannot.syncrepo", e);
		} catch (ApplicationManagementServiceNotSubscribedExceptionException e) {
			handleException("notsubscribed.error", e, cartridgeAlias);
		}
	}
	
	public RepositoryInformation testRepositoryConnection(String repoURL, String repoUsername, String repoPassword, boolean privateRepo) {
		try {
			return stub.testRepositoryConnection(repoURL, repoUsername, repoPassword, privateRepo);
		} catch (RemoteException e) {
			handleException("remote.error", e);
		} catch (ApplicationManagementServiceADCExceptionException e) {
			handleException("cannot.testrepo", e);
		} catch (ApplicationManagementServiceRepositoryRequiredExceptionException e) {
			handleException("repository.required", e);
		} catch (ApplicationManagementServiceRepositoryTransportExceptionException e) {
			handleException("repository.transport.error", e, repoURL);
		} catch (ApplicationManagementServiceRepositoryCredentialsRequiredExceptionException e) {
			handleException("repository.credentials.required", e, repoURL);
		} catch (ApplicationManagementServiceInvalidRepositoryExceptionException e) {
			handleException("repository.invalid.error", e, repoURL);
		}
		return null;
	}
	
    public void unsubscribe(String alias) {
        try {
            stub.unsubscribe(alias);
        } catch (RemoteException e) {
        	handleException("remote.error", e);
        } catch (ApplicationManagementServiceADCExceptionException e) {
        	handleException("cannot.unsubscribe", e);
        } catch (ApplicationManagementServiceNotSubscribedExceptionException e) {
        	handleException("notsubscribed.error", e, alias);
		}
    }

    public PolicyDefinition[] getPolicyDefinitions() {
        try {
            return stub.getPolicyDefinitions();
        } catch (RemoteException e) {
            handleException("remote.error", e);
        }
        return null;
    }
    
    public boolean isFeatureEnabled(String feature) {
        try {
            return stub.isFeatureEnabled(feature);
        } catch (RemoteException e) {
            handleException("remote.error", e);
        }
        return false;
    }

    public SubscriptionInfo subscribeToCartridge(String cartridgeType,
                                       String cartridgeAlias,
                                       String policy,
                                       String repositoryUrl,
                                       boolean privateRepo,
                                       String repoUserName,
                                       String repoPassword,
                                       String otherCartridgeType,
                                       String otherCartridgeAlias) {
    	
		if (log.isInfoEnabled()) {
			log.info("Subscribing to a Cartridge: " + cartridgeType + ", Alias: " + cartridgeAlias);
		}
		
		if (otherCartridgeAlias != null && otherCartridgeAlias.trim().length() > 0) {
			// currently passing empty strings for repo user name and
			// passwords
			try {
				stub.subscribe(otherCartridgeType, otherCartridgeAlias, policy, null, false, "", "", null, null);
			} catch (RemoteException e) {
				handleException("remote.error", e);
			} catch (ApplicationManagementServiceADCExceptionException e) {
				handleException("cannot.subscribe", e);
			} catch (ApplicationManagementServiceRepositoryRequiredExceptionException e) {
				handleException("repository.required", e);
			} catch (ApplicationManagementServiceUnregisteredCartridgeExceptionException e) {
				handleException("cartridge.notregistered", e, otherCartridgeType);
			} catch (ApplicationManagementServiceInvalidCartridgeAliasExceptionException e) {
				handleException("cartridge.invalid.alias", e, otherCartridgeAlias);
			} catch (ApplicationManagementServiceAlreadySubscribedExceptionException e) {
				handleException("cartridge.already.subscribed", e, e.getFaultMessage().getAlreadySubscribedException()
						.getCartridgeType());
			} catch (ApplicationManagementServiceDuplicateCartridgeAliasExceptionException e) {
				handleException("cartridge.alias.duplicate", e, otherCartridgeAlias);
			} catch (ApplicationManagementServicePolicyExceptionException e) {
				handleException("policy.error", e);
			} catch (ApplicationManagementServiceRepositoryTransportExceptionException e) {
				handleException("repository.transport.error", e, repositoryUrl);
			} catch (ApplicationManagementServiceRepositoryCredentialsRequiredExceptionException e) {
				handleException("repository.credentials.required", e, repositoryUrl);
			} catch (ApplicationManagementServiceInvalidRepositoryExceptionException e) {
				handleException("repository.invalid.error", e, repositoryUrl);
			}
		}
		
		try {
			return stub.subscribe(cartridgeType, cartridgeAlias, policy, repositoryUrl, privateRepo, repoUserName,
					repoPassword, otherCartridgeType, otherCartridgeAlias);
		} catch (RemoteException e) {
			handleException("remote.error", e);
		} catch (ApplicationManagementServiceADCExceptionException e) {
			handleException("cannot.subscribe", e);
		} catch (ApplicationManagementServiceRepositoryRequiredExceptionException e) {
			handleException("repository.required", e);
		} catch (ApplicationManagementServiceUnregisteredCartridgeExceptionException e) {
			handleException("cartridge.notregistered", e, cartridgeType);
		} catch (ApplicationManagementServiceInvalidCartridgeAliasExceptionException e) {
			handleException("cartridge.invalid.alias", e, cartridgeAlias);
		} catch (ApplicationManagementServiceAlreadySubscribedExceptionException e) {
			handleException("cartridge.already.subscribed", e, e.getFaultMessage().getAlreadySubscribedException()
					.getCartridgeType());
		} catch (ApplicationManagementServiceDuplicateCartridgeAliasExceptionException e) {
			handleException("cartridge.alias.duplicate", e, cartridgeAlias);
		} catch (ApplicationManagementServicePolicyExceptionException e) {
			handleException("policy.error", e);
		} catch (ApplicationManagementServiceRepositoryTransportExceptionException e) {
			handleException("repository.transport.error", e, repositoryUrl);
		} catch (ApplicationManagementServiceRepositoryCredentialsRequiredExceptionException e) {
			handleException("repository.credentials.required", e, repositoryUrl);
		} catch (ApplicationManagementServiceInvalidRepositoryExceptionException e) {
			handleException("repository.invalid.error", e, repositoryUrl);
		}
		return null;
    }

    private void handleException(String msgKey, Exception e, Object... args) {
        String msg = bundle.getString(msgKey);
        if (args != null) {
        	msg = MessageFormat.format(msg, args);
        }
        log.error(msg, e);
        throw new RuntimeException(msg, e);
    }

}

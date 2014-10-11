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
package org.apache.stratos.cli;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliUtils;
import org.apache.stratos.cli.utils.RowMapper;
import org.apache.stratos.manager.dto.Cartridge;
import org.apache.stratos.manager.dto.PolicyDefinition;
import org.apache.stratos.manager.dto.SubscriptionInfo;
import org.apache.stratos.manager.stub.ApplicationManagementServiceADCExceptionException;
import org.apache.stratos.manager.stub.ApplicationManagementServiceAlreadySubscribedExceptionException;
import org.apache.stratos.manager.stub.ApplicationManagementServiceDomainMappingExistsExceptionException;
import org.apache.stratos.manager.stub.ApplicationManagementServiceDuplicateCartridgeAliasExceptionException;
import org.apache.stratos.manager.stub.ApplicationManagementServiceInvalidCartridgeAliasExceptionException;
import org.apache.stratos.manager.stub.ApplicationManagementServiceInvalidRepositoryExceptionException;
import org.apache.stratos.manager.stub.ApplicationManagementServiceNotSubscribedExceptionException;
import org.apache.stratos.manager.stub.ApplicationManagementServicePolicyExceptionException;
import org.apache.stratos.manager.stub.ApplicationManagementServiceRepositoryCredentialsRequiredExceptionException;
import org.apache.stratos.manager.stub.ApplicationManagementServiceRepositoryRequiredExceptionException;
import org.apache.stratos.manager.stub.ApplicationManagementServiceRepositoryTransportExceptionException;
import org.apache.stratos.manager.stub.ApplicationManagementServiceStub;
import org.apache.stratos.manager.stub.ApplicationManagementServiceUnregisteredCartridgeExceptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class CommandLineService {

	private static final Logger logger = LoggerFactory.getLogger(CommandLineService.class);

	private ApplicationManagementServiceStub stub;

	private CommandLineService() {
	}

	private static class SingletonHolder {
		private final static CommandLineService INSTANCE = new CommandLineService();
	}

	public static CommandLineService getInstance() {
		return SingletonHolder.INSTANCE;
	}
	
	private void initializeApplicationManagementStub(String serverURL, String username, String password) throws AxisFault {
		HttpTransportProperties.Authenticator authenticator = new HttpTransportProperties.Authenticator();
        authenticator.setUsername(username);
        authenticator.setPassword(password);
        authenticator.setPreemptiveAuthentication(true);
		
        ApplicationManagementServiceStub stub;
        ConfigurationContext configurationContext = null;
        try {
            configurationContext = ConfigurationContextFactory.createDefaultConfigurationContext();
        } catch (Exception e) {
            String msg = "Backend error occurred. Please contact the service admins!";
            throw new AxisFault(msg, e);
        }
        HashMap<String, TransportOutDescription> transportsOut = configurationContext
                .getAxisConfiguration().getTransportsOut();
        for (TransportOutDescription transportOutDescription : transportsOut.values()) {
            transportOutDescription.getSender().init(configurationContext, transportOutDescription);
        }
        stub = new ApplicationManagementServiceStub(configurationContext, serverURL + "/services/ApplicationManagementService");
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, authenticator);
        option.setTimeOutInMilliSeconds(300000);
        this.stub = stub;
    }

	public boolean login(String serverURL, String username, String password, boolean validateLogin) throws CommandException {
		try {
			// Following code will avoid validating certificate
			SSLContext sc;
			// Get SSL context
			sc = SSLContext.getInstance("SSL");
			// Create empty HostnameVerifier
			HostnameVerifier hv = new HostnameVerifier() {
				public boolean verify(String urlHostName, SSLSession session) {
					return true;
				}
			};
			// Create a trust manager that does not validate certificate
			// chains
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}
			} };
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			SSLContext.setDefault(sc);
			HttpsURLConnection.setDefaultHostnameVerifier(hv);
		} catch (Exception e) {
			throw new RuntimeException("Error while authentication process!", e);
		}

		// Initialize Service Stub
		try {
			initializeApplicationManagementStub(serverURL, username, password);
		} catch (AxisFault e) {
			System.out.println("Error connecting to the back-end");
			throw new CommandException(e);
		}
		
		try {
			if (validateLogin) {
				String tenantDomain = stub.getTenantDomain();
				if (logger.isDebugEnabled()) {
					logger.debug("Tenant Domain {}", tenantDomain);
				}
				return (tenantDomain != null);
			} else {
				// Just return true as we don't need to validate
				return true;
			}
		} catch (RemoteException e) {
			System.out.println("Authentication failed!");
			throw new CommandException(e);
		}
	}

	public void listSubscribedCartridges(final boolean full) throws CommandException {
		try {
			Cartridge[] cartridges = stub.getSubscribedCartridges();

			if (cartridges == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("No subscribed cartridges found");
				}
				System.out.println("There are no subscribed cartridges");
				return;
			}

			RowMapper<Cartridge> cartridgeMapper = new RowMapper<Cartridge>() {

				public String[] getData(Cartridge cartridge) {
					String[] data = full ? new String[9] : new String[7];
					data[0] = cartridge.getCartridgeType();
					data[1] = cartridge.getDisplayName();
					data[2] = cartridge.getVersion();
					data[3] = cartridge.getMultiTenant() ? "Multi-Tenant" : "Single-Tenant";
					data[4] = cartridge.getCartridgeAlias();
					data[5] = cartridge.getStatus();
					data[6] = cartridge.getMultiTenant() ? "N/A" : String.valueOf(cartridge.getActiveInstances());
					if (full) {
						data[7] = getAccessURLs(cartridge);
						data[8] = cartridge.getRepoURL() != null ? cartridge.getRepoURL() : "";
					}
					return data;
				}
			};
			
			List<String> headers = new ArrayList<String>();
			headers.add("Type");
			headers.add("Name");
			headers.add("Version");
			headers.add("Tenancy Model");
			headers.add("Alias");
			headers.add("Status");
			headers.add("Running Instances");
			if (full) {
				headers.add("Access URL(s)");
				headers.add("Repo URL");
			}

			System.out.println("Subscribed Cartridges:");
			CliUtils.printTable(cartridges, cartridgeMapper, headers.toArray(new String[headers.size()]));

			System.out.println();

		} catch (ApplicationManagementServiceADCExceptionException e) {
			handleException("cannot.list.subscribed.cartridges", e);
		} catch (RemoteException e) {
			handleException(e);
		}
	}

	public void listAvailableCartridges() throws CommandException {
		try {
            Cartridge[] multiTenantCatridges = stub.getAvailableCartridges(true);

			if (multiTenantCatridges == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("No multi-tenant cartridges available");
				}
				System.out.println("There are no multi-tenant cartridges available");
			}

			RowMapper<Cartridge> cartridgeMapper = new RowMapper<Cartridge>() {

				public String[] getData(Cartridge cartridge) {
					String[] data = new String[3];
					data[0] = cartridge.getCartridgeType();
					data[1] = cartridge.getDisplayName();
					data[2] = cartridge.getVersion();
					return data;
				}
			};

			System.out.println("Available Multi-Tenant Cartridges:");
			CliUtils.printTable(multiTenantCatridges, cartridgeMapper, "Type", "Name", "Version");
			System.out.println();
			
			Cartridge[] singleTenantCatridges = stub.getAvailableCartridges(false);

			if (singleTenantCatridges == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("No single-tenant cartridges available");
				}
				System.out.println("There are no single-tenant cartridges available");
			}
			
			System.out.println("Available Single-Tenant Cartridges:");
			CliUtils.printTable(singleTenantCatridges, cartridgeMapper, "Type", "Name", "Version");
			System.out.println();
		} catch (ApplicationManagementServiceADCExceptionException e) {
			handleException("cannot.list.available.cartridges", e);
		} catch (RemoteException e) {
			handleException(e);
		}
	}
	
	public void listAvailablePolicies() throws CommandException {
		try {
			PolicyDefinition[] policies = stub.getPolicyDefinitions();

			if (policies == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("No policies available");
				}
				System.out.println("There are no policies available");
			}

			RowMapper<PolicyDefinition> policyMapper = new RowMapper<PolicyDefinition>() {

				public String[] getData(PolicyDefinition policyDefinition) {
					String[] data = new String[3];
					data[0] = policyDefinition.getName();
					data[1] = policyDefinition.getDescription();
					data[2] = policyDefinition.getDefaultPolicy() ? "Yes" : "No";
					return data;
				}
			};

			CliUtils.printTable(policies, policyMapper, "Policy Name", "Description", "Default");
			System.out.println();
		} catch (RemoteException e) {
			handleException(e);
		}
	}

	public void info(String alias) throws CommandException {
		try {
            Cartridge cartridge = null;
            try {
                cartridge = stub.getCartridgeInfo(alias);
            } catch (ApplicationManagementServiceADCExceptionException e) {
            	handleException(e);
                return;
            } catch (ApplicationManagementServiceNotSubscribedExceptionException e) {
            	handleException("notsubscribed.error", e, alias);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Cartridge Info: {}", new Gson().toJson(cartridge));
			}
			final String FORMAT = "%-20s: %s%n";
			System.out.println();
			System.out.println("Cartridge Information");
			System.out.println("---------------------");
			System.out.format(FORMAT, "Cartridge", cartridge.getCartridgeType());
			System.out.format(FORMAT, "Name", cartridge.getDisplayName());
			System.out.format(FORMAT, "Description", cartridge.getDescription());
			System.out.format(FORMAT, "Version", cartridge.getVersion());
			System.out.format(FORMAT, "Tenancy Model", cartridge.getMultiTenant() ? "Multi-Tenant" : "Single-Tenant");
			System.out.format(FORMAT, "Alias", cartridge.getCartridgeAlias());
			if (StringUtils.isNotBlank(cartridge.getPolicyDescription())) {
				System.out.format(FORMAT, "Policy", cartridge.getPolicyDescription());
			}
			System.out.format(FORMAT, "Access URL(s)", getAccessURLs(cartridge));
			if (StringUtils.isNotBlank(cartridge.getIp())) {
				System.out.format(FORMAT, "Host", cartridge.getIp());
			}
			if (StringUtils.isNotBlank(cartridge.getDbUserName())) {
				System.out.format(FORMAT, "Database Username", cartridge.getDbUserName());
			}
			if (StringUtils.isNotBlank(cartridge.getPassword())) {
				System.out.format(FORMAT, "Password", cartridge.getPassword());
			}
			if (StringUtils.isNotBlank(cartridge.getRepoURL())) {
				System.out.format(FORMAT, "Repository URL", cartridge.getRepoURL());
			}
			System.out.format(FORMAT, "Status", cartridge.getStatus());
			System.out.format(FORMAT, "Running Instances",
					cartridge.getMultiTenant() ? "N/A" : String.valueOf(cartridge.getActiveInstances()));
			System.out.println();

        } catch (RemoteException e) {
        	handleException(e);
        }
	}

	public void unsubscribe(String alias) throws CommandException {
		try {
			stub.unsubscribe(alias);
			System.out.println("You have successfully unsubscribed " + alias);
        } catch (ApplicationManagementServiceADCExceptionException e) {
        	handleException("cannot.unsubscribe", e);
        } catch (ApplicationManagementServiceNotSubscribedExceptionException e) {
			handleException("notsubscribed.error", e, alias);
        } catch (RemoteException e) {
        	handleException(e);
        }
	}

	public void sync(String alias) throws CommandException {
		try {
			System.out.format("Synchronizing repository for alias: %s%n", alias);
			stub.synchronizeRepository(alias);
		} catch (ApplicationManagementServiceADCExceptionException e) {
			handleException("cannot.syncrepo", e);
		} catch (RemoteException e) {
			handleException(e);
		} catch (ApplicationManagementServiceNotSubscribedExceptionException e) {
			handleException("notsubscribed.error", e, alias);
		}
	}

	public String addDomainMapping(String domain, String alias) throws CommandException {
		try {
			return stub.addDomainMapping(domain, alias);
		} catch (ApplicationManagementServiceADCExceptionException e) {
			handleException("cannot.mapdomain", e);
		} catch (RemoteException e) {
			handleException(e);
		} catch (ApplicationManagementServiceNotSubscribedExceptionException e) {
			handleException("notsubscribed.error", e, alias);
		} catch (ApplicationManagementServiceDomainMappingExistsExceptionException e) {
			handleException("domainmapping.exists.error", e, domain, alias);
		}
		return null;
	}

	public void removeDomainMapping(String alias) throws CommandException {
		try {
			stub.removeDomainMapping(alias);
			System.out.format("Domain mapping removed for alias: %s.%n", alias);
		} catch (ApplicationManagementServiceADCExceptionException e) {
			handleException("cannot.removedomain", e);
		} catch (RemoteException e) {
			handleException(e);
		} catch (ApplicationManagementServiceNotSubscribedExceptionException e) {
			handleException("notsubscribed.error", e, alias);
		}
	}

	public void subscribe(String cartridgeType, String alias, String policy, String externalRepoURL,
			boolean privateRepo, String username, String password, String dataCartridgeType, String dataCartridgeAlias)
			throws CommandException {
		
		SubscriptionInfo subcriptionConnectInfo = null;
		if (StringUtils.isNotBlank(dataCartridgeType) && StringUtils.isNotBlank(dataCartridgeAlias)) {
			System.out.format("Subscribing to data cartridge %s with alias %s.%n", dataCartridgeType,
					dataCartridgeAlias);
			try {
				subcriptionConnectInfo = stub.subscribe(dataCartridgeType, dataCartridgeAlias, null, null, false, null,
						null, null, null);
				System.out.format("You have successfully subscribed to %s cartridge with alias %s.%n",
						dataCartridgeType, dataCartridgeAlias);
				System.out.format("%nSubscribing to %s cartridge and connecting with %s data cartridge.%n", alias,
						dataCartridgeAlias);
			} catch (RemoteException e) {
				handleException(e);
			} catch (ApplicationManagementServiceADCExceptionException e) {
				handleException("cannot.subscribe", e);
			} catch (ApplicationManagementServiceRepositoryRequiredExceptionException e) {
				handleException("repository.required", e);
			} catch (ApplicationManagementServiceUnregisteredCartridgeExceptionException e) {
				handleException("cartridge.notregistered", e, dataCartridgeType);
			} catch (ApplicationManagementServiceInvalidCartridgeAliasExceptionException e) {
				handleException("cartridge.invalid.alias", e);
			} catch (ApplicationManagementServiceAlreadySubscribedExceptionException e) {
				handleException("cartridge.already.subscribed", e, e.getFaultMessage().getAlreadySubscribedException()
						.getCartridgeType());
			} catch (ApplicationManagementServiceDuplicateCartridgeAliasExceptionException e) {
				handleException("cartridge.alias.duplicate", e, dataCartridgeAlias);
			} catch (ApplicationManagementServicePolicyExceptionException e) {
				handleException("policy.error", e);
			} catch (ApplicationManagementServiceRepositoryTransportExceptionException e) {
				handleException("repository.transport.error", e, externalRepoURL);
			} catch (ApplicationManagementServiceRepositoryCredentialsRequiredExceptionException e) {
				handleException("repository.credentials.required", e, externalRepoURL);
			} catch (ApplicationManagementServiceInvalidRepositoryExceptionException e) {
				handleException("repository.invalid.error", e, externalRepoURL);
			}
		}
		
		
		try {
			SubscriptionInfo subcriptionInfo = stub.subscribe(cartridgeType, alias, policy, externalRepoURL,
					privateRepo, username, password, dataCartridgeType, dataCartridgeAlias);

			System.out.format("You have successfully subscribed to %s cartridge with alias %s.%n", cartridgeType, alias);

			String repoURL = null;
			String hostnames = null;
			String hostnamesLabel = null;
			if (subcriptionInfo != null) {
				repoURL = subcriptionInfo.getRepositoryURL();
				hostnames = subcriptionInfo.getHostname();
				hostnamesLabel = "host name";

				if (repoURL != null) {
					System.out.println("GIT Repository URL: " + repoURL);
				}

				Cartridge cart = stub.getCartridgeInfo(alias);
				System.out.format("Your application is being published here. %s%n", getAccessURLs(cart));
			}
			if (subcriptionConnectInfo != null) {
				hostnames += ", " + subcriptionConnectInfo.getHostname();
				hostnamesLabel = "host names";

				Cartridge cart = stub.getCartridgeInfo(alias);
				System.out.format("Your data application is being published here. %s%n", getAccessURLs(cart));
			}
			if (externalRepoURL != null) {
				String takeTimeMsg = "(this might take few minutes... depending on repo size)\n";
				System.out.println(takeTimeMsg);
			}

			System.out.format("Please map the %s \"%s\" to ELB IP%n", hostnamesLabel, hostnames);
		} catch (RemoteException e) {
			handleException(e);
		} catch (ApplicationManagementServiceADCExceptionException e) {
			handleException("cannot.subscribe", e);
		} catch (ApplicationManagementServiceRepositoryRequiredExceptionException e) {
			handleException("repository.required", e);
		} catch (ApplicationManagementServiceUnregisteredCartridgeExceptionException e) {
			handleException("cartridge.notregistered", e, cartridgeType);
		} catch (ApplicationManagementServiceInvalidCartridgeAliasExceptionException e) {
			handleException("cartridge.invalid.alias", e);
		} catch (ApplicationManagementServiceAlreadySubscribedExceptionException e) {
			handleException("cartridge.already.subscribed", e, e.getFaultMessage().getAlreadySubscribedException()
					.getCartridgeType());
		} catch (ApplicationManagementServiceDuplicateCartridgeAliasExceptionException e) {
			handleException("cartridge.alias.duplicate", e, alias);
		} catch (ApplicationManagementServicePolicyExceptionException e) {
			handleException("policy.error", e);
		} catch (ApplicationManagementServiceRepositoryTransportExceptionException e) {
			handleException("repository.transport.error", e, externalRepoURL);
		} catch (ApplicationManagementServiceRepositoryCredentialsRequiredExceptionException e) {
			handleException("repository.credentials.required", e, externalRepoURL);
		} catch (ApplicationManagementServiceInvalidRepositoryExceptionException e) {
			handleException("repository.invalid.error", e, externalRepoURL);
		} catch (ApplicationManagementServiceNotSubscribedExceptionException e) {
			handleException("notsubscribed.error", e, alias);
		}
	}

	private String getAccessURLs(Cartridge cartridge) {
		String[] accessURLs = cartridge.getAccessURLs();
		StringBuilder urlBuilder = new StringBuilder();
		if (accessURLs != null) {
			for (int i = 0; i < accessURLs.length; i++) {
				String url = accessURLs[i];
				if (url != null) {
					if (i > 0) {
						urlBuilder.append(", ");
					}
					urlBuilder.append(url);
				}
			}
		}
		return urlBuilder.toString();
	}

	private void handleException(Exception e) throws CommandException {
		if (logger.isDebugEnabled()) {
			logger.debug("Displaying message from Exception {}\n{}", e.getClass(), e.getMessage());
		}
		// TODO: Fix handling error message.
		// Sometimes the Axis2 stub throws only the RemoteException (an
		// AxisFault)
		// So, other exceptions won't come here.
		String message = e.getMessage();
		if (message == null || (message != null && message.contains("Exception"))) {
			message = "Error executing command!";
		}
		if (logger.isErrorEnabled()) {
			logger.error(message);
		}
		System.out.println(message);
		throw new CommandException(message, e);
	}
    
    private void handleException(String key, Exception e, Object... args) throws CommandException {
    	if (logger.isDebugEnabled()) {
    		logger.debug("Displaying message for {}. Exception thrown is {}", key, e.getClass());
    	}
    	String message = CliUtils.getMessage(key, args);
        if (logger.isErrorEnabled()) {
        	logger.error(message);
        }
        System.out.println(message);
        throw new CommandException(message, e);
    }
}

package org.apache.stratos.cartridge.agent.config;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.exception.ParameterNotFoundException;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentConstants;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentUtils;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Cartridge agent configuration.
 */
public class CartridgeAgentConfiguration {

    private static final Log log = LogFactory.getLog(CartridgeAgentConfiguration.class);
    private static volatile CartridgeAgentConfiguration instance;
    private boolean initialized;
    private final String serviceGroup;
    private final String serviceName;
    private final String clusterId;
    private final String networkPartitionId;
    private final String partitionId;
    private final String memberId;
    private final String cartridgeKey;
    private final String appPath;
    private final String repoUrl;
    private final List<Integer> ports;
    private final List<String> logFilePaths;
    private final boolean isCommitsEnabled;
    private final boolean isCheckoutEnabled;
    private final String listenAddress;
    private final String lbClusterId;
    private final String tenantId;
    private final String isClustered;
    private final String minCount;
    private Map<String, String> parameters;
    private boolean isMultitenant;
    private String persistenceMappings;
    private boolean isInternalRepo;
    private String isPrimary;
    private String lbPrivateIp;
    private String lbPublicIp;
    private String deployment;
    private String managerServiceName;
    private String workerServiceName;
    private String superTenantRepositoryPath;
    private String tenantRepositoryPath;
    private String kubernetesClusterId;
        

    private CartridgeAgentConfiguration() {
        parameters = loadParametersFile();

        try {
            serviceGroup = readServiceGroup();
            isClustered = readClustering();
            serviceName = readParameterValue(CartridgeAgentConstants.SERVICE_NAME);
            clusterId = readParameterValue(CartridgeAgentConstants.CLUSTER_ID);
            networkPartitionId = readParameterValue(CartridgeAgentConstants.NETWORK_PARTITION_ID);
            partitionId = readParameterValue(CartridgeAgentConstants.PARTITION_ID);
            memberId = readMemberIdValue(CartridgeAgentConstants.MEMBER_ID);
            cartridgeKey = readParameterValue(CartridgeAgentConstants.CARTRIDGE_KEY);
            appPath = readParameterValue(CartridgeAgentConstants.APP_PATH);
            repoUrl = readParameterValue(CartridgeAgentConstants.REPO_URL);
            ports = readPorts();
            logFilePaths = readLogFilePaths();
            isMultitenant = readMultitenant(CartridgeAgentConstants.MULTITENANT);
            persistenceMappings = readPersistenceMapping();
            isCommitsEnabled = readCommitParameterValue();
            isCheckoutEnabled = Boolean.parseBoolean(System.getProperty(CartridgeAgentConstants.AUTO_CHECKOUT));

            listenAddress = System.getProperty(CartridgeAgentConstants.LISTEN_ADDRESS);
            isInternalRepo = readInternalRepo(CartridgeAgentConstants.PROVIDER);
            tenantId = readParameterValue(CartridgeAgentConstants.TENANT_ID);
            lbClusterId = readLBClusterIdValue(CartridgeAgentConstants.LB_CLUSTER_ID);
            minCount = readMinCountValue(CartridgeAgentConstants.MIN_INSTANCE_COUNT);
            // not mandatory
            lbPrivateIp = System.getProperty(CartridgeAgentConstants.LB_PRIVATE_IP);
            lbPublicIp = System.getProperty(CartridgeAgentConstants.LB_PUBLIC_IP);
            tenantRepositoryPath =  System.getProperty(CartridgeAgentConstants.TENANT_REPO_PATH);
            superTenantRepositoryPath = System.getProperty(CartridgeAgentConstants.SUPER_TENANT_REPO_PATH);

            deployment = readDeployment();
            managerServiceName = readManagerServiceType();
            workerServiceName = readWorkerServiceType();
            isPrimary = readIsPrimary();
            kubernetesClusterId = readKubernetesClusterIdValue(CartridgeAgentConstants.KUBERNETES_CLUSTER_ID);
            
        } catch (ParameterNotFoundException e) {
            throw new RuntimeException(e);
        }

        if (log.isInfoEnabled()) {
            log.info("Cartridge agent configuration initialized");
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("service-name: %s", serviceName));
            log.debug(String.format("cluster-id: %s", clusterId));
            log.debug(String.format("network-partition-id: %s", networkPartitionId));
            log.debug(String.format("partition-id: %s", partitionId));
            log.debug(String.format("member-id: %s", memberId));
            log.debug(String.format("cartridge-key: %s", cartridgeKey));
            log.debug(String.format("app-path: %s", appPath));
            log.debug(String.format("repo-url: %s", repoUrl));
            log.debug(String.format("ports: %s", ports.toString()));
            log.debug(String.format("lb-private-ip: %s", lbPrivateIp));
            log.debug(String.format("lb-public-ip: %s", lbPublicIp));
        }
    }

    private String readKubernetesClusterIdValue(String kubernetesClusterId) {
		String kubernetesClusterIdValue = null;
		if (parameters.containsKey(kubernetesClusterId)) {
			kubernetesClusterIdValue = parameters.get(kubernetesClusterId);
		}

		if (System.getProperty(kubernetesClusterId) != null) {
			kubernetesClusterIdValue = System.getProperty(kubernetesClusterId);
		}
		return kubernetesClusterIdValue;
	}

	private String readMemberIdValue(String memberId) {
		String memberIdValue = null;
		if (parameters.containsKey(memberId) && parameters.get(memberId) != null) {
			memberIdValue = parameters.get(memberId);
		} else if (System.getProperty(memberId) != null) {
			memberIdValue = System.getProperty(memberId);
		} else {	
			String hostname = "unknown";
			try {
				log.info("Reading hostname from container");
				InetAddress addr;
				addr = InetAddress.getLocalHost();
				hostname = addr.getHostName();
			} catch (UnknownHostException e) {
				String msg = "Hostname can not be resolved";
				log.error(msg, e);
			}
			memberIdValue = hostname;
			if (log.isDebugEnabled()) {
				log.debug("MemberId  is taking the value of hostname : [" + memberIdValue + "] ");
			}
		}
		return memberIdValue;
	}

	private String readDeployment(){
        if (parameters.containsKey(CartridgeAgentConstants.DEPLOYMENT)) {
            return parameters.get(CartridgeAgentConstants.DEPLOYMENT);
        }
        return null;
    }

	private String readLBClusterIdValue(String lbClusterId) {
		String lbClusterIdValue = null;
		if (parameters.containsKey(lbClusterId)) {
			lbClusterIdValue = parameters.get(lbClusterId);
		}

		if (System.getProperty(lbClusterId) != null) {
			lbClusterIdValue = System.getProperty(lbClusterId);
		}
		return lbClusterIdValue;
	}
    
	private String readMinCountValue(String minCountParam) throws ParameterNotFoundException {

		String minCountValue = null;
		if (parameters.containsKey(minCountParam)) {
			minCountValue = parameters.get(minCountParam);
		}
		if (System.getProperty(minCountParam) != null) {
			minCountValue = System.getProperty(minCountParam);
		}

		if (Boolean.parseBoolean(isClustered)) {
			 String message = "Cannot find the value of required parameter: " + minCountParam;
			throw new ParameterNotFoundException(message);
		}
		return minCountValue;
	}

		
    private String readManagerServiceType(){

        if (deployment == null) {
            return null;
        }

        if (getDeployment().equalsIgnoreCase(CartridgeAgentConstants.DEPLOYMENT_MANAGER)) {
            // if this is a manager, manager service type = service name
            return serviceName;

        } else if (getDeployment().equalsIgnoreCase(CartridgeAgentConstants.DEPLOYMENT_WORKER)) {
            // if a worker, need to read the manager service type sent by payload
            if (parameters.containsKey(CartridgeAgentConstants.MANAGER_SERVICE_TYPE)) {
                return parameters.get(CartridgeAgentConstants.MANAGER_SERVICE_TYPE);
            }

        } else if (getDeployment().equalsIgnoreCase(CartridgeAgentConstants.DEPLOYMENT_DEFAULT)) {
            // for default deployment, no manager service type
            return null;

        }

        return null;
    }

    private String readWorkerServiceType(){

        if (deployment == null) {
            return null;
        }

        if (getDeployment().equalsIgnoreCase(CartridgeAgentConstants.DEPLOYMENT_WORKER)) {
            // if this is a worker, worker service type = service name
            return serviceName;

        } else if (getDeployment().equalsIgnoreCase(CartridgeAgentConstants.DEPLOYMENT_MANAGER)) {
            // if a manager, need to read the worker service type sent by payload
            if (parameters.containsKey(CartridgeAgentConstants.WORKER_SERVICE_TYPE)) {
                return parameters.get(CartridgeAgentConstants.WORKER_SERVICE_TYPE);
            }

        } else if (getDeployment().equalsIgnoreCase(CartridgeAgentConstants.DEPLOYMENT_DEFAULT)) {
            // for default deployment, no worker service type
            return null;

        }

        return null;
    }

    private String readIsPrimary(){
        if (parameters.containsKey(CartridgeAgentConstants.CLUSTERING_PRIMARY_KEY)) {
            return parameters.get(CartridgeAgentConstants.CLUSTERING_PRIMARY_KEY);
        }
        return null;
    }

    /**
     * Get cartridge agent configuration singleton instance.
     *
     * @return
     */
    public static CartridgeAgentConfiguration getInstance() {
        if (instance == null) {
            synchronized (CartridgeAgentConfiguration.class) {
                if (instance == null) {
                    instance = new CartridgeAgentConfiguration();
                }
            }
        }
        return instance;
    }

    private boolean readCommitsEnabled(String commitEnabled) {
        boolean isCommitEnabled = false;
        try {
            isCommitEnabled = Boolean.parseBoolean(readParameterValue(commitEnabled));

        } catch (ParameterNotFoundException e) {
            // Missing commits enabled flag is not an exception
            log.error(" Commits enabled payload parameter is not found");
        }
        return isCommitEnabled;
    }

    private boolean readMultitenant(String multitenant) throws ParameterNotFoundException {
        String multitenantStringValue = readParameterValue(multitenant);
        return Boolean.parseBoolean(multitenantStringValue);
    }

    private boolean readInternalRepo(String internalRepo) {
        String internalRepoStringValue = null;
        try {
            internalRepoStringValue = readParameterValue(internalRepo);
        } catch (ParameterNotFoundException e) {
            // Missing INTERNAL parameter is not an exception
            log.info(" INTERNAL payload parameter is not found");
        }

        if(internalRepoStringValue != null && internalRepoStringValue.equals(CartridgeAgentConstants.INTERNAL)) {
            return true;
        } else{
            return false;
        }
    }

    private String readPersistenceMapping() {
        String persistenceMapping = null;
        try {
            persistenceMapping = readParameterValue("PERSISTENCE_MAPPING");
        } catch (ParameterNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot read persistence mapping : " + e.getMessage());
            }
        }
        if (persistenceMapping == null || persistenceMapping.isEmpty()) {
            return null;
        }
        return persistenceMapping;
    }


    private Map<String, String> loadParametersFile() {
        Map<String, String> parameters = new HashMap<String, String>();
        try {

            // read launch params
            File file = new File(System.getProperty(CartridgeAgentConstants.PARAM_FILE_PATH));
            if (!file.exists()) {
                log.warn(String.format("File not found: %s", CartridgeAgentConstants.PARAM_FILE_PATH));
                return parameters;
            }
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] params = line.split(",");
                for (String string : params) {
                    if (string != null) {
                        String[] var = string.split("=");
                        if (var.length >= 2) {
                            parameters.put(var[0], var[1]);
                        }
                    }
                }
            }
            scanner.close();
        } catch (Exception e) {
            String message = "Could not read launch parameter file, hence trying to read from System properties.";
            log.warn(message, e);
        }

        return parameters;
    }

    private String readServiceGroup() {
        if (parameters.containsKey(CartridgeAgentConstants.SERVICE_GROUP)) {
            return parameters.get(CartridgeAgentConstants.SERVICE_GROUP);
        } else {
            return null;
        }
    }

    private String readClustering() {
        if (parameters.containsKey(CartridgeAgentConstants.CLUSTERING)) {
            return parameters.get(CartridgeAgentConstants.CLUSTERING);
        } else {
            return null;
        }
    }

    private String readParameterValue(String parameterName) throws ParameterNotFoundException {

        if (parameters.containsKey(parameterName)) {
            return parameters.get(parameterName);
        }

        if (System.getProperty(parameterName) != null) {
            return System.getProperty(parameterName);
        }

        String message = "Cannot find the value of required parameter: " + parameterName;
        throw new ParameterNotFoundException(message);
    }

    private boolean readCommitParameterValue() throws ParameterNotFoundException {

        if (parameters.containsKey(CartridgeAgentConstants.COMMIT_ENABLED)) {
            return Boolean.parseBoolean(parameters.get(CartridgeAgentConstants.COMMIT_ENABLED));
        }

        if (System.getProperty(CartridgeAgentConstants.COMMIT_ENABLED) != null) {
            return Boolean.parseBoolean(System.getProperty(CartridgeAgentConstants.COMMIT_ENABLED));
        }

        if (System.getProperty(CartridgeAgentConstants.AUTO_COMMIT) != null) {
            return Boolean.parseBoolean(System.getProperty(CartridgeAgentConstants.AUTO_COMMIT));
        }
        log.info(CartridgeAgentConstants.COMMIT_ENABLED + " is not found and setting it to false");
        return false;
    }

    private List<Integer> readPorts() throws ParameterNotFoundException {
        List<Integer> ports = new ArrayList<Integer>();
        String portsStr = readParameterValue(CartridgeAgentConstants.PORTS);
        List<String> portsStrList = CartridgeAgentUtils.splitUsingTokenizer(portsStr, "|");
        for (String port : portsStrList) {
            ports.add(Integer.parseInt(port));
        }
        return ports;
    }

    private List<String> readLogFilePaths() {

        String logFileStr = null;
        try {
            logFileStr = readParameterValue(CartridgeAgentConstants.LOG_FILE_PATHS);
        } catch (ParameterNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot read log file path : " + e.getMessage());
            }
        }
        if (logFileStr == null || logFileStr.isEmpty()) {
            return null;
        }
        return CartridgeAgentUtils.splitUsingTokenizer(logFileStr.trim(), "|");
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getCartridgeKey() {
        return cartridgeKey;
    }

    public String getAppPath() {
        return appPath;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public List<String> getLogFilePaths() {
        return logFilePaths;
    }

    public boolean isMultitenant() {
        return isMultitenant;
    }

    public String getPersistenceMappings() {
        return persistenceMappings;
    }

    public boolean isCommitsEnabled() {
        return isCommitsEnabled;
    }

    public String getListenAddress() {
        return listenAddress;
    }

    public boolean isInternalRepo() {
        return isInternalRepo;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getLbClusterId() {
        return lbClusterId;
    }

    public String getServiceGroup() {
        return serviceGroup;
    }

    public String getIsClustered() {
        return isClustered;
    }

    public String getMinCount() {
        return minCount;
    }

    public String getIsPrimary() {
        return isPrimary;
    }

	public String getLbPublicIp() {
		return lbPublicIp;
	}

	public void setLbPublicIp(String lbPublicIp) {
		this.lbPublicIp = lbPublicIp;
	}

	public String getLbPrivateIp() {
		return lbPrivateIp;
	}

	public void setLbPrivateIp(String lbPrivateIp) {
		this.lbPrivateIp = lbPrivateIp;
	}

    public String getDeployment() {
        return deployment;
    }

    public void setDeployment(String deployment) {
        this.deployment = deployment;
    }

    public String getManagerServiceName() {
        return managerServiceName;
    }

    public void setManagerServiceName(String managerServiceName) {
        this.managerServiceName = managerServiceName;
    }

    public String getWorkerServiceName() {
        return workerServiceName;
    }

    public void setWorkerServiceName(String workerServiceName) {
        this.workerServiceName = workerServiceName;
    }

    public String getSuperTenantRepositoryPath() {
        return superTenantRepositoryPath;
    }

    public String getTenantRepositoryPath() {
        return tenantRepositoryPath;
    }

    public boolean isCheckoutEnabled() {
        return isCheckoutEnabled;
    }
    
    public boolean isInitialized() {
    	return initialized;
    }
    
    public void setInitialized(boolean initialized) {
    	this.initialized = initialized;
    }

	public String getKubernetesClusterId() {
		return kubernetesClusterId;
	}
    
    
}

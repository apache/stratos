package org.apache.stratos.cartridge.agent.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.exception.ParameterNotFoundException;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentConstants;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Cartridge agent configuration.
 */
public class CartridgeAgentConfiguration {

    private static final Log log = LogFactory.getLog(CartridgeAgentConfiguration.class);
    private static volatile CartridgeAgentConfiguration instance;

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
    private Map<String, String> parameters;
    private boolean isMultitenant;
    private String persistenceMappings;

    private CartridgeAgentConfiguration() {
    	parameters = loadParametersFile();

        try {
            serviceName = readParameterValue(CartridgeAgentConstants.SERVICE_NAME);
            clusterId = readParameterValue(CartridgeAgentConstants.CLUSTER_ID);
            networkPartitionId = readParameterValue(CartridgeAgentConstants.NETWORK_PARTITION_ID);
            partitionId = readParameterValue(CartridgeAgentConstants.PARTITION_ID);
            memberId = readParameterValue(CartridgeAgentConstants.MEMBER_ID);
            cartridgeKey = readParameterValue(CartridgeAgentConstants.CARTRIDGE_KEY);
            appPath = readParameterValue(CartridgeAgentConstants.APP_PATH);
            repoUrl = readParameterValue(CartridgeAgentConstants.REPO_URL);
            ports = readPorts();
            logFilePaths = readLogFilePaths();
            isMultitenant = readMultitenant(CartridgeAgentConstants.MULTITENANT);
            persistenceMappings = readPersisenceMapping();

        } catch (ParameterNotFoundException e) {
            throw new RuntimeException(e);
        }

        if(log.isInfoEnabled()) {
            log.info("Cartridge agent configuration initialized");
        }

        if(log.isDebugEnabled()) {
            log.debug(String.format("service-name: %s", serviceName));
            log.debug(String.format("cluster-id: %s", clusterId));
            log.debug(String.format("network-partition-id: %s", networkPartitionId));
            log.debug(String.format("partition-id: %s", partitionId));
            log.debug(String.format("member-id: %s", memberId));
            log.debug(String.format("cartridge-key: %s", cartridgeKey));
            log.debug(String.format("app-path: %s", appPath));
            log.debug(String.format("repo-url: %s", repoUrl));
            log.debug(String.format("ports: %s", ports.toString()));
        }
    }

    private boolean readMultitenant(String multitenant) throws ParameterNotFoundException {
    	String multitenantStringValue = readParameterValue(multitenant);
    	return Boolean.parseBoolean(multitenantStringValue);
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

    private String readPersisenceMapping() {
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
            if(!file.exists()) {
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

	private String readParameterValue(String parameterName) throws ParameterNotFoundException{

		if (parameters.containsKey(parameterName)) {
			return parameters.get(parameterName);
		}

		if (System.getProperty(parameterName) != null) {
			return System.getProperty(parameterName);
		}

		String message = "Cannot find the value of required parameter: "+parameterName;
		throw new ParameterNotFoundException(message);
	}

    private List<Integer> readPorts() throws ParameterNotFoundException {
        List<Integer> ports = new ArrayList<Integer>();
        String portsStr = readParameterValue(CartridgeAgentConstants.PORTS);
        List<String> portsStrList = CartridgeAgentUtils.splitUsingTokenizer(portsStr, "|");
        for(String port : portsStrList) {
            ports.add(Integer.parseInt(port));
        }
        return ports;
    }

    private List<String> readLogFilePaths () {

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

}

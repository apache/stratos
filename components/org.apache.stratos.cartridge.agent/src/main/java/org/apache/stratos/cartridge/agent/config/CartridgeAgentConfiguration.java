package org.apache.stratos.cartridge.agent.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentConstants;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
    private CharSequence mbIp;
    private Object mbPort;

    private CartridgeAgentConfiguration() {
        serviceName = readParameterValue(CartridgeAgentConstants.SERVICE_NAME);
        clusterId = readParameterValue(CartridgeAgentConstants.CLUSTER_ID);
        networkPartitionId = readParameterValue(CartridgeAgentConstants.NETWORK_PARTITION_ID);
        partitionId = readParameterValue(CartridgeAgentConstants.PARTITION_ID);
        memberId = readParameterValue(CartridgeAgentConstants.MEMBER_ID);
        cartridgeKey = readParameterValue(CartridgeAgentConstants.CARTRIDGE_KEY);
        appPath = readParameterValue(CartridgeAgentConstants.APP_PATH);
        repoUrl = readParameterValue(CartridgeAgentConstants.REPO_URL);
        ports = readPorts();

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

    /**
     * Get cartridge agent configuration singleton instance.
     *
     * @return
     */
    public static synchronized CartridgeAgentConfiguration getInstance() {
        if (instance == null) {
            synchronized (CartridgeAgentConfiguration.class) {
                if (instance == null) {
                    instance = new CartridgeAgentConfiguration();
                }
            }
        }
        return instance;
    }

    private String readParameterValue(String parameterName) {
        try {
            // read launch params
            File file = new File(System.getProperty(CartridgeAgentConstants.PARAM_FILE_PATH));
            if(!file.exists()) {
                throw new RuntimeException(String.format("File not found: %s", CartridgeAgentConstants.PARAM_FILE_PATH));
            }
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] params = line.split(",");
                for (String string : params) {
                    String[] var = string.split("=");
                    if(parameterName.equals(var[0])){
                        return var[1];
                    }
                }
            }
            scanner.close();
        } catch (Exception e) {
            throw new RuntimeException("Could not read launch parameter file", e);
        }
        return null;
    }

    private List<Integer> readPorts() {
        List<Integer> ports = new ArrayList<Integer>();
        String portsStr = readParameterValue(CartridgeAgentConstants.PORTS);
        List<String> portsStrList = CartridgeAgentUtils.splitUsingTokenizer(portsStr, "|");
        for(String port : portsStrList) {
            ports.add(Integer.parseInt(port));
        }
        return ports;
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

    public CharSequence getMbIp() {
        return mbIp;
    }

    public void setMbIp(CharSequence mbIp) {
        this.mbIp = mbIp;
    }

    public Object getMbPort() {
        return mbPort;
    }

    public void setMbPort(Object mbPort) {
        this.mbPort = mbPort;
    }
}

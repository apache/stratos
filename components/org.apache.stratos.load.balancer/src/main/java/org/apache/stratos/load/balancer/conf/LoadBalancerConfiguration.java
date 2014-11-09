/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.load.balancer.conf;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.conf.domain.Algorithm;
import org.apache.stratos.load.balancer.conf.domain.MemberIpType;
import org.apache.stratos.load.balancer.conf.domain.TenantIdentifier;
import org.apache.stratos.load.balancer.conf.structure.Node;
import org.apache.stratos.load.balancer.conf.structure.NodeBuilder;
import org.apache.stratos.load.balancer.conf.util.Constants;
import org.apache.stratos.load.balancer.context.LoadBalancerContext;
import org.apache.stratos.load.balancer.context.LoadBalancerContextUtil;
import org.apache.stratos.load.balancer.exception.InvalidConfigurationException;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Load balancer configuration definition.
 */
public class LoadBalancerConfiguration {
    private static final Log log = LogFactory.getLog(LoadBalancerConfiguration.class);
    private static volatile LoadBalancerConfiguration instance;

    private String defaultAlgorithmName;
    private boolean failOverEnabled;
    private boolean sessionAffinityEnabled;
    private long endpointTimeout;
    private long sessionTimeout;
    private boolean cepStatsPublisherEnabled;
    private String cepIp;
    private int cepPort;
    private boolean topologyEventListenerEnabled;
    private MemberIpType topologyMemberIpType = MemberIpType.Private;
    private Map<String, Algorithm> algorithmMap;
    private String topologyServiceFilter;
    private String topologyClusterFilter;
    private boolean multiTenancyEnabled;
    private TenantIdentifier tenantIdentifier;
    private List<String> tenantIdentifierRegexList;
    private String topologyMemberFilter;
    private String networkPartitionId;
    private boolean reWriteLocationHeader;

    /**
     * Load balancer configuration is singleton.
     */
    private LoadBalancerConfiguration() {
        this.algorithmMap = new HashMap<String, Algorithm>();
    }

    /**
     * Get load balancer configuration singleton instance.
     *
     * @return Load balancer configuration
     */
    public static LoadBalancerConfiguration getInstance() {
        if (instance == null) {
            synchronized (LoadBalancerConfiguration.class) {
                if (instance == null) {
                    // Read load balancer configuration from file
                    LoadBalancerConfigurationReader reader = new LoadBalancerConfigurationReader();
                    instance = reader.readFromFile();
                }
            }
        }
        return instance;
    }

    /**
     * Clear load balancer configuration singleton instance and referencing contexts.
     */
    public static void clear() {
        synchronized (LoadBalancerConfiguration.class) {
            instance = null;
            // Clear load balancer context
            LoadBalancerContext.getInstance().clear();
            // Clear topology
            TopologyManager.getTopology().clear();
        }
    }

    public String getDefaultAlgorithmName() {
        return defaultAlgorithmName;
    }

    public void setDefaultAlgorithmName(String defaultAlgorithmName) {
        this.defaultAlgorithmName = defaultAlgorithmName;
    }

    public boolean isFailOverEnabled() {
        return failOverEnabled;
    }

    public void setFailOverEnabled(boolean failOverEnabled) {
        this.failOverEnabled = failOverEnabled;
    }

    public boolean isSessionAffinityEnabled() {
        return sessionAffinityEnabled;
    }

    public void setSessionAffinityEnabled(boolean sessionAffinityEnabled) {
        this.sessionAffinityEnabled = sessionAffinityEnabled;
    }

    public long getEndpointTimeout() {
        return endpointTimeout;
    }

    public void setEndpointTimeout(long endpointTimeout) {
        this.endpointTimeout = endpointTimeout;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public boolean isCepStatsPublisherEnabled() {
        return cepStatsPublisherEnabled;
    }

    public void setCepStatsPublisherEnabled(boolean cepStatsPublisherEnabled) {
        this.cepStatsPublisherEnabled = cepStatsPublisherEnabled;
    }

    public String getCepIp() {
        return cepIp;
    }

    public void setCepIp(String cepIp) {
        this.cepIp = cepIp;
    }

    public int getCepPort() {
        return cepPort;
    }

    public void setCepPort(int cepPort) {
        this.cepPort = cepPort;
    }

    public boolean isTopologyEventListenerEnabled() {
        return topologyEventListenerEnabled;
    }

    public void setTopologyEventListenerEnabled(boolean topologyEventListenerEnabled) {
        this.topologyEventListenerEnabled = topologyEventListenerEnabled;
    }

    public MemberIpType getTopologyMemberIpType() {
        return topologyMemberIpType;
    }

    public void setTopologyMemberIpType(MemberIpType type) {
        topologyMemberIpType = type;
    }

    public Collection<Algorithm> getAlgorithms() {
        return algorithmMap.values();
    }

    public Algorithm getAlgorithm(String algorithmName) {
        return algorithmMap.get(algorithmName);
    }

    void addAlgorithm(Algorithm algorithm) {
        algorithmMap.put(algorithm.getName(), algorithm);
    }

    public void setTopologyServiceFilter(String topologyServiceFilter) {
        this.topologyServiceFilter = topologyServiceFilter;
    }

    public String getTopologyServiceFilter() {
        return topologyServiceFilter;
    }

    public void setTopologyClusterFilter(String topologyClusterFilter) {
        this.topologyClusterFilter = topologyClusterFilter;
    }

    public String getTopologyClusterFilter() {
        return topologyClusterFilter;
    }

    public void setTopologyMemberFilter(String topologyMemberFilter) {
        this.topologyMemberFilter = topologyMemberFilter;
    }

    public String getTopologyMemberFilter() {
        return topologyMemberFilter;
    }

    public boolean isMultiTenancyEnabled() {
        return multiTenancyEnabled;
    }

    public void setMultiTenancyEnabled(boolean multiTenancyEnabled) {
        this.multiTenancyEnabled = multiTenancyEnabled;
    }

    public void setTenantIdentifier(TenantIdentifier tenantIdentifier) {
        this.tenantIdentifier = tenantIdentifier;
    }

    public TenantIdentifier getTenantIdentifier() {
        return tenantIdentifier;
    }

    public void setTenantIdentifierRegexList(List<String> tenantIdentifierRegexList) {
        this.tenantIdentifierRegexList = tenantIdentifierRegexList;
    }

    public List<String> getTenantIdentifierRegexList() {
        return tenantIdentifierRegexList;
    }

    public void setNetworkPartitionId(String networkPartitionId) {
        this.networkPartitionId = networkPartitionId;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public void setRewriteLocationHeader(boolean reWriteLocationHeader) {
        this.reWriteLocationHeader = reWriteLocationHeader;
    }

    public boolean isReWriteLocationHeader() {
        return reWriteLocationHeader;
    }

    private static class LoadBalancerConfigurationReader {

        public LoadBalancerConfiguration readFromFile() {
            String configFilePath = System.getProperty("loadbalancer.conf.file");
            if (configFilePath == null) {
                throw new RuntimeException("loadbalancer.conf.file' system property is not set");
            }

            try {
                // Read load balancer configuraiton file
                StringBuilder configFileContent = new StringBuilder();
                File configFile = new File(configFilePath);
                Scanner scanner = new Scanner(configFile);
                while (scanner.hasNextLine()) {
                    configFileContent.append(scanner.nextLine().trim() + "\n");
                }

                // Build node structure
                Node loadBalancerNode = NodeBuilder.buildNode(configFileContent.toString());
                // Transform node structure to configuration
                LoadBalancerConfiguration configuration = transform(loadBalancerNode);
                return configuration;
            } catch (Exception e) {
                throw new InvalidConfigurationException(String.format("Could not read load balancer configuration: %s", configFilePath), e);
            }
        }

        private LoadBalancerConfiguration transform(Node loadBalancerNode) {
            LoadBalancerConfiguration configuration = new LoadBalancerConfiguration();
            if (loadBalancerNode == null || (!loadBalancerNode.getName().equals(Constants.CONF_ELEMENT_LOADBALANCER))) {
                throw new InvalidConfigurationException("loadbalancer node was not found");
            }

            // Set load balancer properties
            String defaultAlgorithm = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_ALGORITHM);
            validateRequiredPropertyInNode(Constants.CONF_PROPERTY_ALGORITHM, defaultAlgorithm, "loadbalancer");
            configuration.setDefaultAlgorithmName(defaultAlgorithm);

            String failOver = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_FAILOVER);
            if (StringUtils.isNotBlank(failOver)) {
                configuration.setFailOverEnabled(Boolean.parseBoolean(failOver));
            }

            String sessionAffinity = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_SESSION_AFFINITY);
            if (StringUtils.isNotBlank(sessionAffinity)) {
                configuration.setSessionAffinityEnabled(Boolean.parseBoolean(sessionAffinity));
            }

            String endpointTimeout = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_ENDPOINT_TIMEOUT);
            if (StringUtils.isNotBlank(endpointTimeout)) {
                configuration.setEndpointTimeout(Long.parseLong(endpointTimeout));
            } else {
                // Endpoint timeout is not found, set default value
                configuration.setEndpointTimeout(Constants.DEFAULT_ENDPOINT_TIMEOUT);
                if(log.isWarnEnabled()) {
                    log.warn(String.format("Endpoint timeout not found, using default: %d", configuration.getEndpointTimeout()));
                }
            }

            String sessionTimeout = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_SESSION_TIMEOUT);
            if (StringUtils.isNotBlank(sessionTimeout)) {
                configuration.setSessionTimeout(Long.parseLong(sessionTimeout));
            } else {
                // Session timeout is not found, set default value
                configuration.setSessionTimeout(Constants.DEFAULT_SESSION_TIMEOUT);
                if(log.isWarnEnabled()) {
                    log.warn(String.format("Session timeout not found, using default: %d", configuration.getSessionTimeout()));
                }
            }

            String topologyEventListenerEnabled = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_TOPOLOGY_EVENT_LISTENER);
            validateRequiredPropertyInNode(Constants.CONF_PROPERTY_TOPOLOGY_EVENT_LISTENER, topologyEventListenerEnabled, Constants.CONF_ELEMENT_LOADBALANCER);
            configuration.setTopologyEventListenerEnabled(Boolean.parseBoolean(topologyEventListenerEnabled));

            if(configuration.isTopologyEventListenerEnabled()) {
                String topologyMemberIpType =  loadBalancerNode.getProperty(Constants.CONF_PROPERTY_TOPOLOGY_MEMBER_IP_TYPE);
                validateRequiredPropertyInNode(Constants.CONF_PROPERTY_TOPOLOGY_MEMBER_IP_TYPE, topologyMemberIpType, Constants.CONF_ELEMENT_LOADBALANCER);
                configuration.setTopologyMemberIpType(transformMemberIpType(topologyMemberIpType));
            }

            String statsPublisherEnabled = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_CEP_STATS_PUBLISHER);
            if (StringUtils.isNotBlank(statsPublisherEnabled)) {
                configuration.setCepStatsPublisherEnabled(Boolean.parseBoolean(statsPublisherEnabled));
            }
            String multiTenancyEnabled = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_MULTI_TENANCY);
            if (StringUtils.isNotBlank(multiTenancyEnabled)) {
                configuration.setMultiTenancyEnabled(Boolean.parseBoolean(multiTenancyEnabled));
            }

            // Read topology service filter and topology cluster filter
            if (configuration.isTopologyEventListenerEnabled()) {
                String serviceFilter = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_TOPOLOGY_SERVICE_FILTER);
                if (StringUtils.isNotBlank(serviceFilter)) {
                    configuration.setTopologyServiceFilter(serviceFilter);
                }
                String clusterFilter = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_TOPOLOGY_CLUSTER_FILTER);
                if (StringUtils.isNotBlank(clusterFilter)) {
                    configuration.setTopologyClusterFilter(clusterFilter);
                }
                String memberFilter = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_TOPOLOGY_MEMBER_FILTER);
                if (StringUtils.isNotBlank(memberFilter)) {
                    configuration.setTopologyMemberFilter(memberFilter);
                }
            }

            // Read cep ip and port if cep stats publisher is enabled
            if (configuration.isCepStatsPublisherEnabled()) {
                String cepIp = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_CEP_IP);
                validateRequiredPropertyInNode(Constants.CONF_PROPERTY_CEP_IP, cepIp, "loadbalancer");
                configuration.setCepIp(cepIp);

                String cepPort = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_CEP_PORT);
                validateRequiredPropertyInNode(Constants.CONF_PROPERTY_CEP_PORT, cepPort, "loadbalancer");
                configuration.setCepPort(Integer.parseInt(cepPort));

                String networkPartitionId = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_NETWORK_PARTITION_ID);
                validateRequiredPropertyInNode(Constants.CONF_PROPERTY_NETWORK_PARTITION_ID, networkPartitionId, "loadbalancer");
                configuration.setNetworkPartitionId(networkPartitionId);
            }

            if (configuration.isMultiTenancyEnabled()) {
                String tenantIdentifierStr = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_TENANT_IDENTIFIER);
                validateRequiredPropertyInNode(Constants.CONF_PROPERTY_TENANT_IDENTIFIER, tenantIdentifierStr, "loadbalancer");

                if (tenantIdentifierStr.equals(Constants.CONF_PROPERTY_VALUE_TENANT_ID)) {
                    configuration.setTenantIdentifier(TenantIdentifier.TenantId);
                } else if (tenantIdentifierStr.equals(Constants.CONF_PROPERTY_VALUE_TENANT_DOMAIN)) {
                    configuration.setTenantIdentifier(TenantIdentifier.TenantDomain);
                } else {
                    throw new InvalidConfigurationException(String.format("Tenant identifier %s is not valid", tenantIdentifierStr));
                }

                String tenantIdentifierRegex = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_TENANT_IDENTIFIER_REGEX);
                validateRequiredPropertyInNode(Constants.CONF_PROPERTY_TENANT_IDENTIFIER_REGEX, tenantIdentifierRegex, "loadbalancer");
                try {
                    Pattern.compile(tenantIdentifierRegex);
                } catch (Exception e) {
                    throw new InvalidConfigurationException(String.format("Invalid tenant identifier regular expression: %s", tenantIdentifierRegex), e);
                }
                List<String> regexList = new ArrayList<String>();
                if(tenantIdentifierRegex.contains(org.apache.stratos.messaging.util.Constants.FILTER_VALUE_SEPARATOR)) {
                    String[] regexArray;
                    regexArray = tenantIdentifierRegex.split(org.apache.stratos.messaging.util.Constants.FILTER_VALUE_SEPARATOR);
                    for(String regex: regexArray) {
                       regexList.add(regex);
                    }
                } else {
                    regexList.add(tenantIdentifierRegex);
                }
                configuration.setTenantIdentifierRegexList(regexList);
            }

            Node algorithmsNode = loadBalancerNode.findChildNodeByName(Constants.CONF_ELEMENT_ALGORITHMS);
            validateRequiredNode(loadBalancerNode, Constants.CONF_ELEMENT_ALGORITHMS);

            for (Node algorithmNode : algorithmsNode.getChildNodes()) {
                String className = algorithmNode.getProperty(Constants.CONF_PROPERTY_CLASS_NAME);
                validateRequiredPropertyInNode(Constants.CONF_PROPERTY_CLASS_NAME, className, "algorithm", algorithmNode.getName());
                Algorithm algorithm = new Algorithm(algorithmNode.getName(), className);
                configuration.addAlgorithm(algorithm);
            }

            String rewriteLocationHeader = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_REWRITE_LOCATION_HEADER);
            if(StringUtils.isNotEmpty(rewriteLocationHeader)) {
                configuration.setRewriteLocationHeader(Boolean.parseBoolean(topologyEventListenerEnabled));
            }

            if (!configuration.isTopologyEventListenerEnabled()) {
                Node servicesNode = loadBalancerNode.findChildNodeByName(Constants.CONF_ELEMENT_SERVICES);
                validateRequiredNode(servicesNode, Constants.CONF_ELEMENT_SERVICES);

                for (Node serviceNode : servicesNode.getChildNodes()) {
                    ServiceType serviceType = ServiceType.SingleTenant;
                    String multiTenant = serviceNode.getProperty(Constants.CONF_PROPERTY_MULTI_TENANT);
                    if (StringUtils.isNotBlank(multiTenant) && (Boolean.parseBoolean(multiTenant))) {
                        serviceType = ServiceType.MultiTenant;
                    }
                    Service service = new Service(serviceNode.getName(), serviceType);
                    Node clustersNode = serviceNode.findChildNodeByName(Constants.CONF_ELEMENT_CLUSTERS);

                    for (Node clusterNode : clustersNode.getChildNodes()) {
                        String clusterId = clusterNode.getName();
                        Cluster cluster = new Cluster(service.getServiceName(), clusterId, null, null, null);

                        String tenantRange = clusterNode.getProperty(Constants.CONF_PROPERTY_TENANT_RANGE);
                        if (StringUtils.isNotBlank(tenantRange)) {
                            if (service.getServiceType() != ServiceType.MultiTenant) {
                                throw new InvalidConfigurationException(String.format("%s property is not valid for non multi-tenant service cluster: [service] %s [cluster] %s",
                                        Constants.CONF_PROPERTY_TENANT_RANGE, service.getServiceName(), cluster.getClusterId()));
                            }
                            cluster.setTenantRange(tenantRange);
                        }

                        String algorithm = clusterNode.getProperty(Constants.CONF_PROPERTY_ALGORITHM);
                        if (StringUtils.isNotBlank(algorithm)) {
                            cluster.setLoadBalanceAlgorithmName(algorithm);
                        }

                        String hosts = clusterNode.getProperty(Constants.CONF_ELEMENT_HOSTS);
                        validateRequiredPropertyInNode(Constants.CONF_ELEMENT_HOSTS, hosts, "cluster", clusterNode.getName());

                        String[] hostsArray = hosts.split(",");
                        for (String hostsName : hostsArray) {
                            cluster.addHostName(hostsName.trim());
                        }

                        Node membersNode = clusterNode.findChildNodeByName(Constants.CONF_ELEMENT_MEMBERS);
                        validateRequiredNode(membersNode, Constants.CONF_ELEMENT_MEMBERS, String.format("cluster %s", clusterId));

                        for (Node memberNode : membersNode.getChildNodes()) {
                            String memberId = memberNode.getName();
                            // we are making it as 1 because we are not using this for static loadbalancer configuration
                            long initTime = -1;
                            Member member = new Member(cluster.getServiceName(), cluster.getClusterId(), Constants.STATIC_NETWORK_PARTITION, Constants.STATIC_PARTITION, memberId, initTime);
                            String ip = memberNode.getProperty(Constants.CONF_PROPERTY_IP);
                            validateRequiredPropertyInNode(Constants.CONF_PROPERTY_IP, ip, String.format("member %s", memberId));

                            member.setMemberIp(ip);
                            Node portsNode = memberNode.findChildNodeByName(Constants.CONF_ELEMENT_PORTS);
                            validateRequiredNode(portsNode, Constants.CONF_ELEMENT_PORTS, String.format("member %s", memberId));

                            for (Node portNode : portsNode.getChildNodes()) {
                                String value = portNode.getProperty(Constants.CONF_PROPERTY_VALUE);
                                validateRequiredPropertyInNode(Constants.CONF_PROPERTY_VALUE, value, "port", String.format("member %s", memberId));

                                String proxy = portNode.getProperty(Constants.CONF_PROPERTY_PROXY);
                                validateRequiredPropertyInNode(Constants.CONF_PROPERTY_PROXY, proxy, "port", String.format("member %s", memberId));

                                Port port = new Port(portNode.getName(), Integer.valueOf(value), Integer.valueOf(proxy));
                                member.addPort(port);
                            }
                            member.setStatus(MemberStatus.Activated);
                            cluster.addMember(member);
                        }
                        // Add cluster to service
                        service.addCluster(cluster);

                        // Add service to topology manager if not exists
                        try {
                            // TODO - fix properly!
                            // this lock is not needed since, this Topology is not shared. This is
                            // used by LB only
                            //TopologyManager.acquireWriteLock();
                            if (!TopologyManager.getTopology().serviceExists(service.getServiceName())) {
                                TopologyManager.getTopology().addService(service);
                            }
                        } finally {
                            //TopologyManager.releaseWriteLock();
                        }

                        // Add cluster to load balancer context
                        LoadBalancerContextUtil.addClusterAgainstHostNames(cluster);
                    }
                }
            }
            return configuration;
        }

        private MemberIpType transformMemberIpType(String topologyMemberIpType) {
            if("private".equals(topologyMemberIpType)) {
                return MemberIpType.Private;
            }
            else if("public".equals(topologyMemberIpType)) {
                return MemberIpType.Public;
            }
            else {
                throw new InvalidConfigurationException(String.format("Topology member ip address type is not valid: %s", topologyMemberIpType));
            }
        }

        private void validateRequiredNode(Node node, String nodeName) {
            if (node == null) {
                throw new RuntimeException(String.format("%s node was not found", nodeName));
            }
        }

        private void validateRequiredNode(Node node, String nodeName, String parentNodeName) {
            if (node == null) {
                throw new RuntimeException(String.format("%s node was not found in %s", nodeName, parentNodeName));
            }
        }

        private void validateRequiredPropertyInNode(String propertyName, String value, String nodeName) {
            validateRequiredPropertyInNode(propertyName, value, nodeName, "");
        }

        private void validateRequiredPropertyInNode(String propertyName, String value, String nodeName, String nodeItem) {
            if (StringUtils.isBlank(value)) {
                throw new InvalidConfigurationException(String.format("%s property was not found in %s node %s", propertyName, nodeName, nodeItem));
            }
        }
    }
}

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
import org.apache.stratos.load.balancer.LoadBalancerContext;
import org.apache.stratos.load.balancer.conf.domain.Algorithm;
import org.apache.stratos.load.balancer.conf.structure.Node;
import org.apache.stratos.load.balancer.conf.structure.NodeBuilder;
import org.apache.stratos.load.balancer.conf.util.Constants;
import org.apache.stratos.load.balancer.exception.InvalidConfigurationException;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Port;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.io.File;
import java.util.*;

/**
 * Load balancer configuration definition.
 */
public class LoadBalancerConfiguration {
    private static final Log log = LogFactory.getLog(LoadBalancerConfiguration.class);
    private static volatile LoadBalancerConfiguration instance;

    private String defaultAlgorithmName;
    private boolean failOver;
    private boolean sessionAffinity;
    private long sessionTimeout;
    private boolean cepStatsPublisherEnabled;
    private String mbIp;
    private int mbPort;
    private String cepIp;
    private int cepPort;
    private boolean topologyEventListenerEnabled;
    private boolean usePublicIpAddresses;
    private Map<String, Algorithm> algorithmMap;
    private String topologyServiceFilter;
    private String topologyClusterFilter;

    /**
     * Load balancer configuration is singleton.
     */
    private LoadBalancerConfiguration() {
        this.algorithmMap = new HashMap<String, Algorithm>();
    }

    public static synchronized LoadBalancerConfiguration getInstance() {
        if (instance == null) {
            synchronized (LoadBalancerConfiguration.class) {
                if (instance == null) {
                    // Clear load balancer context
                    LoadBalancerContext.getInstance().clear();
                    // Read load balancer configuration from file
                    LoadBalancerConfigurationReader reader = new LoadBalancerConfigurationReader();
                    instance = reader.readFromFile();
                }
            }
        }
        return instance;
    }

    public String getDefaultAlgorithmName() {
        return defaultAlgorithmName;
    }

    public void setDefaultAlgorithmName(String defaultAlgorithmName) {
        this.defaultAlgorithmName = defaultAlgorithmName;
    }

    public boolean isFailOver() {
        return failOver;
    }

    public void setFailOver(boolean failOver) {
        this.failOver = failOver;
    }

    public boolean isSessionAffinity() {
        return sessionAffinity;
    }

    public void setSessionAffinity(boolean sessionAffinity) {
        this.sessionAffinity = sessionAffinity;
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

    public String getMbIp() {
        return mbIp;
    }

    public void setMbIp(String mbIp) {
        this.mbIp = mbIp;
    }

    public int getMbPort() {
        return mbPort;
    }

    public void setMbPort(int mbPort) {
        this.mbPort = mbPort;
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

    public boolean isUsePublicIpAddresses() {
        return usePublicIpAddresses;
    }

    public void setUsePublicIpAddresses(boolean usePublicIpAddresses) {
        this.usePublicIpAddresses = usePublicIpAddresses;
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

    private static class LoadBalancerConfigurationReader {

        private String property;

        public LoadBalancerConfiguration readFromFile() {
            String configFilePath = System.getProperty("loadbalancer.conf.file");
            if(configFilePath == null){
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
            if (StringUtils.isBlank(defaultAlgorithm)) {
                throw new InvalidConfigurationException("algorithm property was not found in loadbalancer node");
            }
            configuration.setDefaultAlgorithmName(defaultAlgorithm);

            String failOver = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_FAILOVER);
            if (StringUtils.isNotBlank(failOver)) {
                configuration.setFailOver(Boolean.parseBoolean(failOver));
            }

            String sessionAffinity = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_SESSION_AFFINITY);
            if (StringUtils.isNotBlank(sessionAffinity)) {
                configuration.setSessionAffinity(Boolean.parseBoolean(sessionAffinity));
            }
            String sessionTimeout = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_SESSION_TIMEOUT);
            if (StringUtils.isNotBlank(sessionTimeout)) {
                configuration.setSessionTimeout(Long.parseLong(sessionTimeout));
            } else {
                // Session timeout is not found, set default value
                configuration.setSessionTimeout(Constants.DEFAULT_SESSION_TIMEOUT);
            }
            String topologyEventListenerEnabled = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_TOPOLOGY_EVENT_LISTENER_ENABLED);
            if (StringUtils.isNotBlank(topologyEventListenerEnabled)) {
                configuration.setTopologyEventListenerEnabled(Boolean.parseBoolean(topologyEventListenerEnabled));
            }
            String statsPublisherEnabled = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_CEP_STATS_PUBLISHER_ENABLED);
            if (StringUtils.isNotBlank(statsPublisherEnabled)) {
                configuration.setCepStatsPublisherEnabled(Boolean.parseBoolean(statsPublisherEnabled));
            }

            // Read mb ip, port, topology service filter and topology cluster filter if topology event listener is enabled
            if (configuration.isTopologyEventListenerEnabled()) {
                String mbIp = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_MB_IP);
                String mbPort = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_MB_PORT);
                if (StringUtils.isBlank(mbIp)) {
                    throw new InvalidConfigurationException(String.format("%s property was not found in loadbalancer node", Constants.CONF_PROPERTY_MB_IP));
                }
                if (StringUtils.isBlank(mbPort)) {
                    throw new InvalidConfigurationException(String.format("%s property was not found in loadbalancer node", Constants.CONF_PROPERTY_MB_PORT));
                }

                configuration.setMbIp(mbIp);
                configuration.setMbPort(Integer.parseInt(mbPort));

                String serviceFilter = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_TOPOLOGY_SERVICE_FILTER);
                if(StringUtils.isNotBlank(serviceFilter)) {
                    configuration.setTopologyServiceFilter(serviceFilter);
                }
                String clusterFilter = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_TOPOLOGY_CLUSTER_FILTER);
                if(StringUtils.isNotBlank(clusterFilter)) {
                    configuration.setTopologyClusterFilter(clusterFilter);
                }
            }

            // Read cep ip and port if cep stats publisher is enabled
            if (configuration.isCepStatsPublisherEnabled()) {
                String cepIp = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_CEP_IP);
                String cepPort = loadBalancerNode.getProperty(Constants.CONF_PROPERTY_CEP_PORT);
                if (StringUtils.isBlank(cepIp)) {
                    throw new InvalidConfigurationException(String.format("%s property was not found in loadbalancer node", Constants.CONF_PROPERTY_CEP_IP));
                }
                if (StringUtils.isBlank(cepPort)) {
                    throw new InvalidConfigurationException(String.format("%s property was not found in loadbalancer node", Constants.CONF_PROPERTY_CEP_PORT));
                }

                configuration.setCepIp(cepIp);
                configuration.setCepPort(Integer.parseInt(cepPort));
            }

            Node algorithmsNode = loadBalancerNode.findChildNodeByName(Constants.CONF_ELEMENT_ALGORITHMS);
            if (loadBalancerNode == null) {
                throw new RuntimeException(String.format("%s node was node found", Constants.CONF_ELEMENT_ALGORITHMS));
            }
            for (Node algorithmNode : algorithmsNode.getChildNodes()) {
                String className = algorithmNode.getProperty(Constants.CONF_PROPERTY_CLASS_NAME);
                if (StringUtils.isBlank(className)) {
                    throw new InvalidConfigurationException(String.format("%s property was not found in algorithm %s", Constants.CONF_PROPERTY_CLASS_NAME, algorithmNode.getName()));
                }
                Algorithm algorithm = new Algorithm(algorithmNode.getName(), className);
                configuration.addAlgorithm(algorithm);
            }

            if (!configuration.isTopologyEventListenerEnabled()) {
                Node servicesNode = loadBalancerNode.findChildNodeByName(Constants.CONF_ELEMENT_SERVICES);
                if (loadBalancerNode == null) {
                    throw new RuntimeException(String.format("%s node was not found", Constants.CONF_ELEMENT_SERVICES));
                }

                for (Node serviceNode : servicesNode.getChildNodes()) {
                    Service service = new Service(serviceNode.getName());
                    Node clustersNode = serviceNode.findChildNodeByName(Constants.CONF_ELEMENT_CLUSTERS);

                    for (Node clusterNode : clustersNode.getChildNodes()) {
                        String clusterId = clusterNode.getName();
                        Cluster cluster = new Cluster(service.getServiceName(), clusterId, null);
                        String hosts = clusterNode.getProperty(Constants.CONF_ELEMENT_HOSTS);
                        if (StringUtils.isBlank(hosts)) {
                            throw new InvalidConfigurationException(String.format("%s node was not found in cluster %s", Constants.CONF_ELEMENT_HOSTS, clusterNode.getName()));
                        }
                        String[] hostsArray = hosts.split(",");
                        // TODO: Add multiple host-names to cluster
                        cluster.setHostName(hostsArray[0]);

                        Node membersNode = clusterNode.findChildNodeByName(Constants.CONF_ELEMENT_MEMBERS);
                        if (membersNode == null) {
                            throw new InvalidConfigurationException(String.format("%s node was not found in cluster %s", Constants.CONF_ELEMENT_MEMBERS, clusterId));
                        }

                        for (Node memberNode : membersNode.getChildNodes()) {
                            String memberId = memberNode.getName();
                            Member member = new Member(cluster.getServiceName(), cluster.getClusterId(), memberId);
                            String ip = memberNode.getProperty(Constants.CONF_PROPERTY_IP);
                            if (StringUtils.isBlank(ip)) {
                                throw new InvalidConfigurationException(String.format("%s property was not found in member %s", Constants.CONF_PROPERTY_IP, memberId));
                            }
                            member.setMemberIp(ip);
                            Node portsNode = memberNode.findChildNodeByName(Constants.CONF_ELEMENT_PORTS);
                            if (portsNode == null) {
                                throw new InvalidConfigurationException(String.format("%s node was not found in member %s", Constants.CONF_ELEMENT_PORTS, memberId));
                            }
                            for (Node portNode : portsNode.getChildNodes()) {
                                String value = portNode.getProperty(Constants.CONF_PROPERTY_VALUE);
                                if (StringUtils.isBlank(value)) {
                                    throw new InvalidConfigurationException(String.format("%s property was not found in port %s in member %s", Constants.CONF_PROPERTY_VALUE, portNode.getName(), memberId));
                                }
                                String proxy = portNode.getProperty(Constants.CONF_PROPERTY_PROXY);
                                if (StringUtils.isBlank(proxy)) {
                                    throw new InvalidConfigurationException(String.format("%s property was not found in port %s in member %s", Constants.CONF_PROPERTY_PROXY, portNode.getName(), memberId));
                                }
                                Port port = new Port(portNode.getName(), Integer.valueOf(value), Integer.valueOf(proxy));
                                member.addPort(port);
                            }
                            cluster.addMember(member);
                        }
                        service.addCluster(cluster);

                        // Add cluster to load balancer context Map<Hostname,Cluster>
                        LoadBalancerContext.getInstance().addCluster(cluster);
                    }

                    // Add service to topology manager
                    try {
                        TopologyManager.acquireWriteLock();
                        TopologyManager.getTopology().addService(service);
                    } finally {
                        TopologyManager.releaseWriteLock();
                    }
                }
            }
            return configuration;
        }
    }
}

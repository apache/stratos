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
                    instance = reader.readConfigurationFromFile();
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

    private static class LoadBalancerConfigurationReader {

        private String property;

        public LoadBalancerConfiguration readConfigurationFromFile() {
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
            if(loadBalancerNode == null) {
                throw new InvalidConfigurationException("loadbalancer node was not found");
            }

            // Set load balancer properties
            String defaultAlgorithm = loadBalancerNode.getProperty("algorithm");
            if(StringUtils.isBlank(defaultAlgorithm)) {
                throw new InvalidConfigurationException("algorithm property was not found in loadbalancer node");
            }
            configuration.setDefaultAlgorithmName(defaultAlgorithm);

            String failOver = loadBalancerNode.getProperty("failover");
            if(StringUtils.isNotBlank(failOver)) {
                configuration.setFailOver(Boolean.parseBoolean(failOver));
            }

            String sessionAffinity = loadBalancerNode.getProperty("session-affinity");
            if(StringUtils.isNotBlank(sessionAffinity)) {
                configuration.setSessionAffinity(Boolean.parseBoolean(sessionAffinity));
            }
            String sessionTimeout =  loadBalancerNode.getProperty("session-timeout");
            if(StringUtils.isNotBlank(sessionTimeout)) {
                configuration.setSessionTimeout(Long.parseLong(sessionTimeout));
            }
            else {
                // Session timeout is not found, set default value
                configuration.setSessionTimeout(90000);
            }
            String topologyEventListenerEnabled = loadBalancerNode.getProperty("topology-event-listener-enabled");
            if(StringUtils.isNotBlank(topologyEventListenerEnabled)) {
                configuration.setTopologyEventListenerEnabled(Boolean.parseBoolean(topologyEventListenerEnabled));
            }
            String statsPublisherEnabled = loadBalancerNode.getProperty("cep-stats-publisher-enabled");
            if(StringUtils.isNotBlank(statsPublisherEnabled)) {
                configuration.setCepStatsPublisherEnabled(Boolean.parseBoolean(statsPublisherEnabled));
            }

            // Read mb ip and port if topology event listener is enabled
            if(configuration.isTopologyEventListenerEnabled()) {
                String mbIp = loadBalancerNode.getProperty("mb-ip");
                String mbPort = loadBalancerNode.getProperty("mb-port");
                if(StringUtils.isBlank(mbIp)) {
                    throw new InvalidConfigurationException("mb-ip property was not found in loadbalancer node");
                }
                if(StringUtils.isBlank(mbPort)) {
                    throw new InvalidConfigurationException("mb-port property was not found in loadbalancer node");
                }

                configuration.setMbIp(mbIp);
                configuration.setMbPort(Integer.parseInt(mbPort));
            }

            // Read cep ip and port if cep stats publisher is enabled
            if(configuration.isCepStatsPublisherEnabled()) {
                String cepIp = loadBalancerNode.getProperty("cep-ip");
                String cepPort = loadBalancerNode.getProperty("cep-port");
                if(StringUtils.isBlank(cepIp)) {
                    throw new InvalidConfigurationException("cep-ip property was not found in loadbalancer node");
                }
                if(StringUtils.isBlank(cepPort)) {
                    throw new InvalidConfigurationException("cep-port property was not found in loadbalancer node");
                }

                configuration.setCepIp(cepIp);
                configuration.setCepPort(Integer.parseInt(cepPort));
            }

            Node algorithmsNode = loadBalancerNode.findChildNodeByName("algorithms");
            if(loadBalancerNode == null) {
                throw new RuntimeException("algorithms node was node found");
            }
            for(Node algorithmNode : algorithmsNode.getChildNodes()) {
                String className = algorithmNode.getProperty("class-name");
                if(StringUtils.isBlank(className)) {
                    throw new InvalidConfigurationException(String.format("class-name property was not found in algorithm %s", algorithmNode.getName()));
                }
                Algorithm algorithm = new Algorithm(algorithmNode.getName(), className);
                configuration.addAlgorithm(algorithm);
            }

            if(!configuration.isTopologyEventListenerEnabled()) {
                Node servicesNode = loadBalancerNode.findChildNodeByName("services");
                if(loadBalancerNode == null) {
                    throw new RuntimeException("services node was not found");
                }

                for(Node serviceNode : servicesNode.getChildNodes()) {
                    Service service = new Service(serviceNode.getName());
                    Node clustersNode = serviceNode.findChildNodeByName("clusters");

                    for(Node clusterNode : clustersNode.getChildNodes()) {
                        String clusterId = clusterNode.getName();
                        Cluster cluster = new Cluster(service.getServiceName(), clusterId, null);
                        String hosts = clusterNode.getProperty("hosts");
                        if(StringUtils.isBlank(hosts)) {
                            throw new InvalidConfigurationException(String.format("hosts node was not found in cluster %s", clusterNode.getName()));
                        }
                        String[] hostsArray = hosts.split(",");
                        // TODO: Add multiple host-names to cluster
                        cluster.setHostName(hostsArray[0]);

                        Node membersNode = clusterNode.findChildNodeByName("members");
                        if(membersNode == null) {
                            throw new InvalidConfigurationException(String.format("members node was not found in cluster %s", clusterId));
                        }

                        for(Node memberNode : membersNode.getChildNodes()) {
                            String memberId = memberNode.getName();
                            Member member = new Member(cluster.getServiceName(), cluster.getClusterId(), memberId);
                            String ip = memberNode.getProperty("ip");
                            if(StringUtils.isBlank(ip)) {
                                throw new InvalidConfigurationException(String.format("ip property was not found in member %s", memberId));
                            }
                            member.setMemberIp(ip);
                            Node portsNode = memberNode.findChildNodeByName("ports");
                            if(portsNode == null) {
                                throw new InvalidConfigurationException(String.format("ports node was not found in member %s", memberId));
                            }
                            for(Node portNode : portsNode.getChildNodes()) {
                                String value = portNode.getProperty("value");
                                if(StringUtils.isBlank(value)) {
                                    throw new InvalidConfigurationException(String.format("value property was not found in port %s in member %s", portNode.getName(), memberId));
                                }
                                String proxy = portNode.getProperty("proxy");
                                if(StringUtils.isBlank(proxy)) {
                                    throw new InvalidConfigurationException(String.format("proxy property was not found in port %s in member %s", portNode.getName(), memberId));
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
                    }
                    finally {
                        TopologyManager.releaseWriteLock();
                    }
                }
            }
            return configuration;
        }
    }
}

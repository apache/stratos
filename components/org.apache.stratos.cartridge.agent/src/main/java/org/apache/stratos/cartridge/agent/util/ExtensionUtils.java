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

package org.apache.stratos.cartridge.agent.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.common.util.CommandUtils;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Cartridge agent extension utility methods.
 */
public class ExtensionUtils {
    private static final Log log = LogFactory.getLog(ExtensionUtils.class);

    private static String getExtensionsDir() {
        String extensionsDir = System.getProperty(CartridgeAgentConstants.EXTENSIONS_DIR);
        if (StringUtils.isBlank(extensionsDir)) {
            throw new RuntimeException(String.format("System property not found: %s", CartridgeAgentConstants.EXTENSIONS_DIR));
        }
        return extensionsDir;
    }

    private static String prepareCommand(String scriptFile) throws FileNotFoundException {
        String extensionsDir = getExtensionsDir();
        String filePath = (extensionsDir.endsWith(File.separator)) ?
                extensionsDir + scriptFile :
                extensionsDir + File.separator + scriptFile;

        File file = new File(filePath);
        if (file.exists() && !file.isDirectory()) {
            return filePath;
        }

        throw new FileNotFoundException("Script file not found:" + filePath);
    }

    public static void addPayloadParameters(Map<String, String> envParameters){
        envParameters.put("STRATOS_APP_PATH", CartridgeAgentConfiguration.getInstance().getAppPath());
        envParameters.put("STRATOS_PARAM_FILE_PATH", System.getProperty(CartridgeAgentConstants.PARAM_FILE_PATH));
        envParameters.put("STRATOS_SERVICE_NAME", CartridgeAgentConfiguration.getInstance().getServiceName());
        envParameters.put("STRATOS_TENANT_ID", CartridgeAgentConfiguration.getInstance().getTenantId());
        envParameters.put("STRATOS_CARTRIDGE_KEY", CartridgeAgentConfiguration.getInstance().getCartridgeKey());
        envParameters.put("STRATOS_LB_CLUSTER_ID", CartridgeAgentConfiguration.getInstance().getLbClusterId());
        envParameters.put("STRATOS_CLUSTER_ID", CartridgeAgentConfiguration.getInstance().getClusterId());
        envParameters.put("STRATOS_NETWORK_PARTITION_ID", CartridgeAgentConfiguration.getInstance().getNetworkPartitionId());
        envParameters.put("STRATOS_PARTITION_ID", CartridgeAgentConfiguration.getInstance().getPartitionId());
        envParameters.put("STRATOS_PERSISTENCE_MAPPINGS", CartridgeAgentConfiguration.getInstance().getPersistenceMappings());
        envParameters.put("STRATOS_REPO_URL", CartridgeAgentConfiguration.getInstance().getRepoUrl());

        // Add LB instance public/private IPs to environment parameters
        String lbClusterIdInPayload = CartridgeAgentConfiguration.getInstance().getLbClusterId();
        String[] memberIps = getLbMemberIp(lbClusterIdInPayload);
        String lbIp, lbPublicIp;
        if (memberIps != null && memberIps.length > 1) {
        	lbIp = memberIps[0];
        	lbPublicIp = memberIps[1];
        } else {
        	lbIp = CartridgeAgentConfiguration.getInstance().getLbPrivateIp();
        	lbPublicIp = CartridgeAgentConfiguration.getInstance().getLbPublicIp();
        }
        
        envParameters.put("STRATOS_LB_IP", lbIp);
        envParameters.put("STRATOS_LB_PUBLIC_IP", lbPublicIp);

        Topology topology = TopologyManager.getTopology();
        if (topology.isInitialized()){
            Service service = topology.getService(CartridgeAgentConfiguration.getInstance().getServiceName());
            Cluster cluster = service.getCluster(CartridgeAgentConfiguration.getInstance().getClusterId());
            String memberIdInPayload = CartridgeAgentConfiguration.getInstance().getMemberId();
            addProperties(service.getProperties(), envParameters, "SERVICE_PROPERTY");
            addProperties(cluster.getProperties(), envParameters, "CLUSTER_PROPERTY");
            addProperties(cluster.getMember(memberIdInPayload).getProperties(), envParameters, "MEMBER_PROPERTY");
        }
    }

    public static void addProperties(Properties properties, Map<String, String> envParameters, String prefix){
        if (properties == null || properties.entrySet() == null){
            return;
        }
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            envParameters.put("STRATOS_ " + prefix + "_" + entry.getKey().toString(), entry.getValue().toString());
            if (log.isDebugEnabled()) {
                log.debug(String.format("Property added: [key] %s [value] %s",
                        "STRATOS_ " + prefix + "_" + entry.getKey().toString(), entry.getValue().toString()));
            }
        }
    }

    public static String[] getLbMemberIp(String lbClusterId) {
        Topology topology = TopologyManager.getTopology();
        Collection<Service> serviceCollection = topology.getServices();

        for (Service service : serviceCollection) {
            Collection<Cluster> clusterCollection = service.getClusters();
            for (Cluster cluster : clusterCollection) {
                Collection<Member> memberCollection = cluster.getMembers();
                for (Member member : memberCollection) {
                    if (member.getClusterId().equals(lbClusterId)) {
                        return new String[]{member.getDefaultPrivateIP(), member.getDefaultPublicIP()};
                    }
                }
            }
        }
        return null;
    }

    private static Map<String, String> cleanProcessParameters(Map<String, String> envParameters) {
        Iterator<Map.Entry<String, String>> iter = envParameters.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            if (entry.getValue() == null) {
                iter.remove();
            }
        }
        return envParameters;
    }

    public static void executeStartServersExtension(Map<String, String> envParameters) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing start servers extension");
            }
            String script = System.getProperty(CartridgeAgentConstants.START_SERVERS_SCRIPT);
            String command = prepareCommand(script);
            addPayloadParameters(envParameters);
            cleanProcessParameters(envParameters);
            String output = CommandUtils.executeCommand(command, envParameters);
            if (log.isDebugEnabled()) {
                log.debug("Start server script returned:" + output);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not execute start servers extension", e);
            }
        }
    }

    public static void executeCleanupExtension() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing cleanup extension");
            }
            String script = System.getProperty(CartridgeAgentConstants.CLEAN_UP_SCRIPT);
            String command = prepareCommand(script);
            String output = CommandUtils.executeCommand(command);
            if (log.isDebugEnabled()) {
                log.debug("Cleanup script returned:" + output);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not execute cleanup extension", e);
            }
        }
    }

    public static void executeInstanceStartedExtension(Map<String, String> envParameters) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing instance started extension");
            }
            String script = System.getProperty(CartridgeAgentConstants.INSTANCE_STARTED_SCRIPT);
            String command = prepareCommand(script);
            addPayloadParameters(envParameters);
            cleanProcessParameters(envParameters);
            String output = CommandUtils.executeCommand(command, envParameters);
            if (log.isDebugEnabled()) {
                log.debug("Instance started script returned:" + output);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not execute instance started extension", e);
            }
        }
    }

    public static void executeInstanceActivatedExtension() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing instance activated extension");
            }
            String script = System.getProperty(CartridgeAgentConstants.INSTANCE_ACTIVATED_SCRIPT);
            String command = prepareCommand(script);
            String output = CommandUtils.executeCommand(command);
            if (log.isDebugEnabled()) {
                log.debug("Instance activated script returned:" + output);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not execute instance activated extension", e);
            }
        }
    }

    public static void executeArtifactsUpdatedExtension(Map<String, String> envParameters) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing artifacts updated extension");
            }
            String script = System.getProperty(CartridgeAgentConstants.ARTIFACTS_UPDATED_SCRIPT);
            String command = prepareCommand(script);
            addPayloadParameters(envParameters);
            cleanProcessParameters(envParameters);
            String output = CommandUtils.executeCommand(command, envParameters);
            if (log.isDebugEnabled()) {
                log.debug("Artifacts updated script returned:" + output);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not execute artifacts updated extension", e);
            }
        }
    }

    public static void executeCopyArtifactsExtension(String source, String destination) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing artifacts copy extension");
            }
            String command = prepareCommand(System.getProperty(CartridgeAgentConstants.ARTIFACTS_COPY_SCRIPT));
            CommandUtils.executeCommand(command + " " + source + " " + destination);
        } catch (Exception e) {
            log.error("Could not execute artifacts copy extension", e);
        }
    }

    /*
    This will execute the volume mounting script which format and mount the
    persistance volumes.
     */
    public static void executeVolumeMountExtension(String persistenceMappingsPayload) {
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Executing volume mounting extension: [payload] %s", persistenceMappingsPayload));
            }
            String script = System.getProperty(CartridgeAgentConstants.MOUNT_VOLUMES_SCRIPT);
            String command = prepareCommand(script);
            //String payloadPath = System.getProperty(CartridgeAgentConstants.PARAM_FILE_PATH);
            // add payload file path as argument so inside the script we can source
            // it  to get the env variables set by the startup script

            String output = CommandUtils.executeCommand(command + " " + persistenceMappingsPayload);
            if (log.isDebugEnabled()) {
                log.debug("Volume mount script returned:" + output);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not execute volume mounting extension", e);
            }
        }
    }

    public static void executeMemberActivatedExtension(Map<String, String> envParameters) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing member activated extension");
            }
            String script = System.getProperty(CartridgeAgentConstants.MEMBER_ACTIVATED_SCRIPT);
            String command = prepareCommand(script);
            addPayloadParameters(envParameters);
            cleanProcessParameters(envParameters);
            String output = CommandUtils.executeCommand(command, envParameters);
            if (log.isDebugEnabled()) {
                log.debug("Member activated script returned:" + output);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not execute member activated extension", e);
            }
        }
    }

    public static void executeMemberTerminatedExtension(Map<String, String> envParameters) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing member terminated extension");
            }
            String script = System.getProperty(CartridgeAgentConstants.MEMBER_TERMINATED_SCRIPT);
            String command = prepareCommand(script);
            addPayloadParameters(envParameters);
            cleanProcessParameters(envParameters);
            String output = CommandUtils.executeCommand(command, envParameters);
            if (log.isDebugEnabled()) {
                log.debug("Member terminated script returned:" + output);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not execute member terminated extension", e);
            }
        }
    }

    public static void executeMemberStartedExtension(Map<String, String> envParameters) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing member started extension");
            }
            String script = System.getProperty(CartridgeAgentConstants.MEMBER_STARTED_SCRIPT);
            String command = prepareCommand(script);
            addPayloadParameters(envParameters);
            cleanProcessParameters(envParameters);
            String output = CommandUtils.executeCommand(command, envParameters);
            if (log.isDebugEnabled()) {
                log.debug("Member started script returned:" + output);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not execute member started extension", e);
            }
        }
    }

    public static void executeMemberSuspendedExtension(Map<String, String> envParameters) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing member suspended extension");
            }
            String script = System.getProperty(CartridgeAgentConstants.MEMBER_SUSPENDED_SCRIPT);
            String command = prepareCommand(script);
            addPayloadParameters(envParameters);
            cleanProcessParameters(envParameters);
            String output = CommandUtils.executeCommand(command, envParameters);
            if (log.isDebugEnabled()) {
                log.debug("Member suspended script returned:" + output);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not execute member suspended extension", e);
            }
        }
    }

    public static void executeCompleteTopologyExtension(Map<String, String> envParameters) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing complete topology extension");
            }
            String script = System.getProperty(CartridgeAgentConstants.COMPLETE_TOPOLOGY_SCRIPT);
            String command = prepareCommand(script);
            addPayloadParameters(envParameters);
            cleanProcessParameters(envParameters);
            String output = CommandUtils.executeCommand(command, envParameters);
            if (log.isDebugEnabled()) {
                log.debug("Complete topology script returned:" + output);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not execute complete topology extension", e);
            }
        }
    }

    public static void executeCompleteTenantExtension(Map<String, String> envParameters) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing complete tenant extension");
            }
            String script = System.getProperty(CartridgeAgentConstants.COMPLETE_TENANT_SCRIPT);
            String command = prepareCommand(script);
            addPayloadParameters(envParameters);
            cleanProcessParameters(envParameters);
            String output = CommandUtils.executeCommand(command, envParameters);
            if (log.isDebugEnabled()) {
                log.debug("Complete tenant script returned:" + output);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not execute complete tenant extension", e);
            }
        }
    }

    public static void executeDomainMappingAddedExtension(Map<String, String> envParameters) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing domain mapping added extension");
            }
            String script = System.getProperty(CartridgeAgentConstants.DOMAIN_MAPPING_ADDED_SCRIPT);
            String command = prepareCommand(script);
            addPayloadParameters(envParameters);
            cleanProcessParameters(envParameters);
            String output = CommandUtils.executeCommand(command, envParameters);
            if (log.isDebugEnabled()) {
                log.debug("Domain mapping added script returned:" + output);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not execute domain mapping added extension", e);
            }
        }
    }

    public static void executeDomainMappingRemovedExtension(Map<String, String> envParameters) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing domain mapping removed extension");
            }
            String script = System.getProperty(CartridgeAgentConstants.DOMAIN_MAPPING_REMOVED_SCRIPT);
            String command = prepareCommand(script);
            addPayloadParameters(envParameters);
            cleanProcessParameters(envParameters);
            String output = CommandUtils.executeCommand(command, envParameters);
            if (log.isDebugEnabled()) {
                log.debug("Domain mapping removed script returned:" + output);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not execute domain mapping removed extension", e);
            }

        }
    }

    public static void executeTenantSubscribedExtension(Map<String, String> envParameters) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing tenant subscribed extension");
            }
            String script = System.getProperty(CartridgeAgentConstants.TENANT_SUBSCRIBED_SCRIPT);
            String command = prepareCommand(script);
            addPayloadParameters(envParameters);
            cleanProcessParameters(envParameters);
            CommandUtils.executeCommand(command, envParameters);
        } catch (Exception e) {
            log.error("Could not execute tenant subscribed extension", e);
        }
    }

    public static void executeTenantUnSubscribedExtension(Map<String, String> envParameters) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Executing tenant un-subscribed extension");
            }
            String script = System.getProperty(CartridgeAgentConstants.TENANT_UNSUBSCRIBED_SCRIPT);
            String command = prepareCommand(script);
            addPayloadParameters(envParameters);
            cleanProcessParameters(envParameters);
            CommandUtils.executeCommand(command, envParameters);
        } catch (Exception e) {
            log.error("Could not execute tenant un-subscribed extension", e);
        }
    }

    public static boolean isTopologyInitialized() {
        TopologyManager.acquireReadLock();
        boolean active = TopologyManager.getTopology().isInitialized();
        TopologyManager.releaseReadLock();
        return active;
    }

    public static void waitForCompleteTopology() {
        while (!isTopologyInitialized()) {
            if (log.isInfoEnabled()) {
                log.info("Waiting for complete topology event...");
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Check if the specified member is in the topology and is in MemberStatus.Initialized state
     * @param serviceName
     * @param clusterId
     * @param memberId
     * @return true if member is present in the topology and in initialized state, false otherwise
     */
    public static boolean checkTopologyConsistency(String serviceName, String clusterId, String memberId) {

        Member activatedMember = getMemberFromTopology(serviceName, clusterId, memberId);
        // this doesn't work for all events: if (activatedMember.getStatus() != MemberStatus.Initialized) {
        if (activatedMember == null) {
            if (log.isErrorEnabled()) {
                //log.error(String.format("Member found in topology, but not in initialized state [member] %s", memberId));
            	log.error(String.format("Member not found in topology [member] %s %s %s ", memberId, clusterId, serviceName));
            }
            return false;
        }

        return true;
    }

    /**
     * Gets the specified member from the topology
     * @param serviceName
     * @param clusterId
     * @param memberId
     * @return {@link org.apache.stratos.messaging.domain.topology.Member} if member is in the topology, null otherwise
     */
    public static Member getMemberFromTopology(String serviceName, String clusterId, String memberId) {
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(serviceName);
        if (service == null) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Service not found in topology [service] %s", serviceName));
            }
            return null;
        }

        Cluster cluster = service.getCluster(clusterId);
        if (cluster == null) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Cluster id not found in topology [cluster] %s", clusterId));
            }
            return null;
        }

        Member activatedMember = cluster.getMember(memberId);
        if (activatedMember == null) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Member id not found in topology [member] %s", memberId));
            }
            return null;
        }

        return activatedMember;
    }
}

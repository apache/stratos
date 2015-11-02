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
package org.apache.stratos.autoscaler.algorithms.networkpartition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.algorithms.NetworkPartitionAlgorithm;
import org.apache.stratos.autoscaler.pojo.policy.PolicyManager;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ApplicationPolicy;
import org.apache.stratos.common.constants.StratosConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One after algorithm for the network-partition or groups
 */
public class OneAfterAnotherAlgorithm implements NetworkPartitionAlgorithm {

    private static final Log log = LogFactory.getLog(NetworkPartitionAlgorithm.class);

    @Override
    public List<String> getNextNetworkPartitions(NetworkPartitionAlgorithmContext
                                                         networkPartitionAlgorithmContext) {

        if (networkPartitionAlgorithmContext == null) {
            if (log.isWarnEnabled()) {
                String msg = "Network partition algorithm context is null";
                log.warn(msg);
            }
            return null;
        }

        String applicationId = networkPartitionAlgorithmContext.getApplicationId();
        if (applicationId == null) {
            if (log.isWarnEnabled()) {
                String msg = "Application id is null in network partition algorithm context";
                log.warn(msg);
            }
            return null;
        }

        ApplicationPolicy applicationPolicy = PolicyManager.getInstance().getApplicationPolicy(
                networkPartitionAlgorithmContext.getApplicationPolicyId());
        if (applicationPolicy == null) {
            if (log.isWarnEnabled()) {
                String msg = String.format("No application policy found in network partition " +
                        "algorithm context [application-id] %s", applicationId);
                log.warn(msg);
            }
            return null;
        }

        String applicationPolicyId = applicationPolicy.getId();
        String[] networkPartitionGroups = applicationPolicy.getNetworkPartitionGroups();
        if (networkPartitionGroups != null && networkPartitionGroups.length != 0) {
            if (log.isDebugEnabled()) {
                String msg = String.format("Network partition groups property found in " +
                                "application policy [application-id] %s [application-policy-id] %s. "
                                + "Hence using network partition groups for app bursting",
                        applicationId, applicationPolicyId);
                log.debug(msg);
            }
            int totalNetworkPartitionGroups = networkPartitionGroups.length;
            if (log.isDebugEnabled()) {
                String msg = String.format("%s network partition groups found in application " +
                                "policy [application-id] %s [application-policy-id] %s",
                        totalNetworkPartitionGroups, applicationId, applicationPolicyId);
                log.debug(msg);
            }

            int currentPartitionIndex = networkPartitionAlgorithmContext.
                    getCurrentNetworkPartitionIndex().intValue();
            if (log.isDebugEnabled()) {
                String msg = String.format("Current network partition group index is %s " +
                                "[application-id] %s [application-policy-d] %s",
                        currentPartitionIndex, applicationId, applicationPolicyId);
                log.debug(msg);
            }

            if (currentPartitionIndex >= totalNetworkPartitionGroups) {
                if (log.isDebugEnabled()) {
                    String msg = String.format("currentPartitionIndex %s >= " +
                                    "totalNetworkPartitionGroups %s, hence no more network partition" +
                                    " groups are available [application-id] %s " +
                                    "[application-policy-id] %s", currentPartitionIndex,
                            totalNetworkPartitionGroups, applicationId, applicationPolicyId);
                    log.debug(msg);
                }
                return null;
            }

            int selectedIndex = networkPartitionAlgorithmContext.
                    getCurrentNetworkPartitionIndex().incrementAndGet();
            if (log.isDebugEnabled()) {
                String msg = String.format("Selected network partition group index is %s " +
                                "(starting from 1,2,3...) [application-id] %s " +
                                "[application-policy-d] %s", selectedIndex,
                        applicationId, applicationPolicyId);
                log.debug(msg);
            }

            if (log.isDebugEnabled()) {
                String msg = String.format("Selected network partition group is %s " +
                                "[application-id] %s [application-policy-id] %s",
                        networkPartitionGroups[selectedIndex - 1], applicationId, applicationPolicyId);
                log.debug(msg);
            }

            List<String> selectedNetworkPartitions = new ArrayList<String>();
            Collections.addAll(selectedNetworkPartitions, networkPartitionGroups[selectedIndex - 1].
                    split(StratosConstants.APPLICATION_POLICY_NETWORK_PARTITIONS_SPLITTER));

            return selectedNetworkPartitions;
        }

        String[] networkPartitions = applicationPolicy.getNetworkPartitions();
        if (networkPartitions == null || networkPartitions.length == 0) {
            if (log.isWarnEnabled()) {
                String msg = String.format("Network partitions found in application policy " +
                                "[application-id] %s [application-policy-id] %s",
                        applicationId, applicationPolicyId);
                log.warn(msg);
            }
            return null;
        }

        int totalNetworkPartitions = networkPartitions.length;
        if (log.isDebugEnabled()) {
            String msg = String.format("%s network partitions found in application policy " +
                            "[application-id] %s [application-policy-id] %s",
                    totalNetworkPartitions, applicationId, applicationPolicyId);
            log.debug(msg);
        }

        int currentPartitionIndex = networkPartitionAlgorithmContext.
                getCurrentNetworkPartitionIndex().intValue();
        if (log.isDebugEnabled()) {
            String msg = String.format("Current network partition index is %s " +
                            "[application-id] %s [application-policy-id] %s",
                    currentPartitionIndex, applicationId, applicationPolicyId);
            log.debug(msg);
        }

        if (currentPartitionIndex >= totalNetworkPartitions) {
            if (log.isDebugEnabled()) {
                String msg = String.format("currentPartitionIndex %s >= totalNetworkPartitions %s , " +
                                "hence no more network partitions are available "
                                + "[application-id] %s [application-policy-id] %s", currentPartitionIndex,
                        totalNetworkPartitions, applicationId, applicationPolicyId);
                log.debug(msg);
            }
            return null;
        }

        int selectedIndex = networkPartitionAlgorithmContext.getCurrentNetworkPartitionIndex().
                incrementAndGet();
        if (log.isDebugEnabled()) {
            String msg = String.format("Selected network partition index is %s " +
                            "(starting from 1,2,3...) [application-id] %s [application-policy-id] %s",
                    selectedIndex, applicationId, applicationPolicyId);
            log.debug(msg);
        }

        if (log.isDebugEnabled()) {
            String msg = String.format("Selected network partition is %s [application-id] %s " +
                            "[application-policy-id] %s",
                    networkPartitions[selectedIndex - 1], applicationId, applicationPolicyId);
            log.debug(msg);
        }

        List<String> nextNetworkPartitions = new ArrayList<String>();
        nextNetworkPartitions.add(networkPartitions[selectedIndex - 1]);

        return nextNetworkPartitions;
    }

    @Override
    public List<String> getDefaultNetworkPartitions(NetworkPartitionAlgorithmContext
                                                            networkPartitionAlgorithmContext) {
        if (networkPartitionAlgorithmContext == null) {
            if (log.isWarnEnabled()) {
                String msg = "Network partition algorithm context is null";
                log.warn(msg);
            }
            return null;
        }

        String applicationId = networkPartitionAlgorithmContext.getApplicationId();
        if (applicationId == null) {
            if (log.isWarnEnabled()) {
                String msg = "Application id is null in network partition algorithm context";
                log.warn(msg);
            }
            return null;
        }

        ApplicationPolicy applicationPolicy = PolicyManager.getInstance().getApplicationPolicy(
                networkPartitionAlgorithmContext.getApplicationPolicyId());
        if (applicationPolicy == null) {
            if (log.isWarnEnabled()) {
                String msg = String.format("No application policy found in network partition " +
                        "algorithm context [application-id] %s", applicationId);
                log.warn(msg);
            }
            return null;
        }

        String applicationPolicyId = applicationPolicy.getId();
        String[] networkPartitionGroups = applicationPolicy.getNetworkPartitionGroups();
        List<String> selectedNetworkPartitions = new ArrayList<String>();

        if (networkPartitionGroups != null && networkPartitionGroups.length != 0) {
            if (log.isDebugEnabled()) {
                String msg = String.format("Network partition groups property found in application " +
                                "policy [application-id] %s [application-policy-id] %s. "
                                + "Hence using network partition groups for app bursting",
                        applicationId, applicationPolicyId);
                log.debug(msg);
            }
            int totalNetworkPartitionGroups = networkPartitionGroups.length;
            if (log.isDebugEnabled()) {
                String msg = String.format("%s network partition groups found in application policy" +
                                " [application-id] %s [application-policy-id] %s",
                        totalNetworkPartitionGroups, applicationId, applicationPolicyId);
                log.debug(msg);
            }
            //Returning the very first network-partition group as the default one
            Collections.addAll(selectedNetworkPartitions, networkPartitionGroups[0].
                    split(StratosConstants.APPLICATION_POLICY_NETWORK_PARTITIONS_SPLITTER));

        } else {
            //Returning the very first network-partition as the default one
            String[] networkPartitions = applicationPolicy.getNetworkPartitions();
            selectedNetworkPartitions.add(networkPartitions[0]);
        }

        return selectedNetworkPartitions;

    }
}

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

package org.apache.stratos.autoscaler.monitor.cluster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.exception.partition.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.policy.PolicyValidationException;
import org.apache.stratos.cloud.controller.stub.pojo.MemberContext;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;


/*
 * Factory class for creating cluster monitors.
 */
public class ClusterMonitorFactory {

	private static final Log log = LogFactory.getLog(ClusterMonitorFactory.class);
	public static final String IS_PRIMARY = "PRIMARY";

	/**
     * @param cluster the cluster to be monitored
     * @return the created cluster monitor
     * @throws PolicyValidationException    when deployment policy is not valid
     * @throws PartitionValidationException when partition is not valid
     */
    public static AbstractClusterMonitor getMonitor(Cluster cluster)
            throws PolicyValidationException, PartitionValidationException {

        AbstractClusterMonitor clusterMonitor;
        if (cluster.isKubernetesCluster()) {
            clusterMonitor = getDockerServiceClusterMonitor(cluster);
        } else if (cluster.isLbCluster()) {
            clusterMonitor = getVMLbClusterMonitor(cluster);
        } else {
            clusterMonitor = getVMServiceClusterMonitor(cluster);
        }

        return clusterMonitor;
    }

    private static VMServiceClusterMonitor getVMServiceClusterMonitor(Cluster cluster)
            throws PolicyValidationException, PartitionValidationException {

        if (null == cluster) {
            return null;
        }

        VMServiceClusterMonitor clusterMonitor = new VMServiceClusterMonitor(cluster.getServiceName(), cluster.getClusterId());

        // find lb reference type
        java.util.Properties props = cluster.getProperties();

        if (props != null) {
            if (props.containsKey(StratosConstants.LOAD_BALANCER_REF)) {
                String value = props.getProperty(StratosConstants.LOAD_BALANCER_REF);
                clusterMonitor.setLbReferenceType(value);
                if (log.isDebugEnabled()) {
                    log.debug("Set the lb reference type: " + value);
                }
            }

            // set hasPrimary property
            // hasPrimary is true if there are primary members available in that cluster
            clusterMonitor.setHasPrimary(Boolean.parseBoolean(cluster.getProperties().getProperty(IS_PRIMARY)));
        }


        log.info("VMServiceClusterMonitor created: " + clusterMonitor.toString());
        return clusterMonitor;
    }

    private static VMLbClusterMonitor getVMLbClusterMonitor(Cluster cluster)
            throws PolicyValidationException, PartitionValidationException {

        if (null == cluster) {
            return null;
        }

        VMLbClusterMonitor clusterMonitor =
                new VMLbClusterMonitor(cluster.getServiceName(), cluster.getClusterId());
        clusterMonitor.setStatus(ClusterStatus.Created);

        log.info("VMLbClusterMonitor created: " + clusterMonitor.toString());
        return clusterMonitor;
    }

    /**
     * @param cluster - the cluster which needs to be monitored
     * @return - the cluster monitor
     */
    private static KubernetesServiceClusterMonitor getDockerServiceClusterMonitor(Cluster cluster)
            throws PolicyValidationException {

        if (null == cluster) {
            return null;
        }

//        String autoscalePolicyName = cluster.getAutoscalePolicyName();
//
//        AutoscalePolicy autoscalePolicy =
//                PolicyManager.getInstance()
//                        .getAutoscalePolicy(autoscalePolicyName);
//        if (log.isDebugEnabled()) {
//            log.debug("Autoscaling policy name: " + autoscalePolicyName);
//        }
//
//        AutoscalePolicy policy = PolicyManager.getInstance().getAutoscalePolicy(autoscalePolicyName);
//
//        if (policy == null) {
//            String msg = String.format("Autoscaling policy is null: [policy-name] %s", autoscalePolicyName);
//            log.error(msg);
//            throw new PolicyValidationException(msg);
//        }
//
        java.util.Properties properties = cluster.getProperties();
        if (properties == null) {
            String message = String.format("Properties not found in kubernetes cluster: [cluster-id] %s",
                    cluster.getClusterId());
            log.error(message);
            throw new RuntimeException(message);
        }
//        String minReplicasProperty = properties.getProperty(StratosConstants.KUBERNETES_MIN_REPLICAS);
//        int minReplicas = 0;
//        if (minReplicasProperty != null && !minReplicasProperty.isEmpty()) {
//            minReplicas = Integer.parseInt(minReplicasProperty);
//        }
//
//        int maxReplicas = 0;
//        String maxReplicasProperty = properties.getProperty(StratosConstants.KUBERNETES_MAX_REPLICAS);
//        if (maxReplicasProperty != null && !maxReplicasProperty.isEmpty()) {
//            maxReplicas = Integer.parseInt(maxReplicasProperty);
//        }
//
//        String kubernetesHostClusterID = properties.getProperty(StratosConstants.KUBERNETES_CLUSTER_ID);
//        KubernetesClusterContext kubernetesClusterCtxt = new KubernetesClusterContext(kubernetesHostClusterID,
//                cluster.getClusterId(), cluster.getServiceName(),  autoscalePolicy, minReplicas, maxReplicas);


        KubernetesServiceClusterMonitor dockerClusterMonitor = new KubernetesServiceClusterMonitor(cluster.getServiceName(), cluster.getClusterId());

        //populate the members after restarting
        for (Member member : cluster.getMembers()) {
            String memberId = member.getMemberId();
            String clusterId = member.getClusterId();
            MemberContext memberContext = new MemberContext();
            memberContext.setMemberId(memberId);
            memberContext.setClusterId(clusterId);
            memberContext.setInitTime(member.getInitTime());

            // if there is at least one member in the topology, that means service has been created already
            // this is to avoid calling startContainer() method again
            //kubernetesClusterCtxt.setServiceClusterCreated(true);

            if (MemberStatus.Activated.equals(member.getStatus())) {
                if (log.isDebugEnabled()) {
                    String msg = String.format("Active member loaded from topology and added to active member list, %s", member.toString());
                    log.debug(msg);
                }
                dockerClusterMonitor.getKubernetesClusterCtxt().addActiveMember(memberContext);
            } else if (MemberStatus.Created.equals(member.getStatus())
                    || MemberStatus.Starting.equals(member.getStatus())) {
                if (log.isDebugEnabled()) {
                    String msg = String.format("Pending member loaded from topology and added to pending member list, %s", member.toString());
                    log.debug(msg);
                }
                dockerClusterMonitor.getKubernetesClusterCtxt().addPendingMember(memberContext);
            }

            //kubernetesClusterCtxt.addMemberStatsContext(new MemberStatsContext(memberId));
            if (log.isInfoEnabled()) {
                log.info(String.format("Member stat context has been added: [member] %s", memberId));
            }
        }

        // find lb reference type
        if (properties.containsKey(StratosConstants.LOAD_BALANCER_REF)) {
            String value = properties.getProperty(StratosConstants.LOAD_BALANCER_REF);
            dockerClusterMonitor.setLbReferenceType(value);
            if (log.isDebugEnabled()) {
                log.debug("Set the lb reference type: " + value);
            }
        }

        log.info("KubernetesServiceClusterMonitor created: " + dockerClusterMonitor.toString());
        return dockerClusterMonitor;
    }
}

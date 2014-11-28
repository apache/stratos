package org.apache.stratos.autoscaler.rule;
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
import org.apache.stratos.autoscaler.*;
import org.apache.stratos.autoscaler.algorithm.AutoscaleAlgorithm;
import org.apache.stratos.autoscaler.algorithm.OneAfterAnother;
import org.apache.stratos.autoscaler.algorithm.RoundRobin;
import org.apache.stratos.autoscaler.client.CloudControllerClient;
import org.apache.stratos.autoscaler.client.InstanceNotificationClient;
import org.apache.stratos.autoscaler.context.cluster.KubernetesClusterContext;
import org.apache.stratos.autoscaler.context.member.MemberStatsContext;
import org.apache.stratos.autoscaler.context.partition.network.ClusterLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.context.partition.ClusterLevelPartitionContext;
import org.apache.stratos.autoscaler.exception.cartridge.TerminationException;
import org.apache.stratos.autoscaler.monitor.cluster.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.VMClusterMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.VMServiceClusterMonitor;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.cloud.controller.stub.pojo.MemberContext;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.autoscaler.context.AutoscalerContext;

/**
 * This will have utility methods that need to be executed from rule file...
 */
public class RuleTasksDelegator {

    public static final double SCALE_UP_FACTOR = 0.8;   //get from config
    public static final double SCALE_DOWN_FACTOR = 0.2;
    private static boolean arspiIsSet = false;

    private static final Log log = LogFactory.getLog(RuleTasksDelegator.class);

    public double getPredictedValueForNextMinute(float average, float gradient, float secondDerivative, int timeInterval) {
        double predictedValue;
//        s = u * t + 0.5 * a * t * t
        if (log.isDebugEnabled()) {
            log.debug(String.format("Predicting the value, [average]: %s , [gradient]: %s , [second derivative]" +
                    ": %s , [time intervals]: %s ", average, gradient, secondDerivative, timeInterval));
        }
        predictedValue = average + gradient * timeInterval + 0.5 * secondDerivative * timeInterval * timeInterval;

        return predictedValue;
    }


    public int getNumberOfInstancesRequiredBasedOnRif(float rifPredictedValue, float requestsServedPerInstance, float averageRequestsServedPerInstance, boolean arspiReset) {

        float requestsInstanceCanHandle = requestsServedPerInstance;

        if (arspiReset && averageRequestsServedPerInstance != 0) {
            requestsInstanceCanHandle = averageRequestsServedPerInstance;

        }
        float numberOfInstances = 0;
        if (requestsInstanceCanHandle != 0) {
            numberOfInstances = rifPredictedValue / requestsInstanceCanHandle;
            arspiReset = true;

        } else {
            arspiReset = false;
        }
        return (int) Math.ceil(numberOfInstances);
    }

    public int getNumberOfInstancesRequiredBasedOnLoadAndMemoryConsumption(float upperLimit, float lowerLimit, double predictedValue, int activeMemberCount) {

        double numberOfInstances = (activeMemberCount * predictedValue) / upperLimit;

        return (int) Math.ceil(numberOfInstances);
    }

    public int getMaxNumberOfInstancesRequired(int numberOfInstancesReuquiredBasedOnRif, int numberOfInstancesReuquiredBasedOnMemoryConsumption, boolean mcReset, int numberOfInstancesReuquiredBasedOnLoadAverage, boolean laReset) {
        int numberOfInstances = 0;

        int rifBasedRequiredInstances = 0;
        int mcBasedRequiredInstances = 0;
        int laBasedRequiredInstances = 0;
        if (arspiIsSet) {
            rifBasedRequiredInstances = numberOfInstancesReuquiredBasedOnRif;
        }
        if (mcReset) {
            rifBasedRequiredInstances = numberOfInstancesReuquiredBasedOnMemoryConsumption;
        }
        if (laReset) {
            rifBasedRequiredInstances = numberOfInstancesReuquiredBasedOnLoadAverage;
        }
        numberOfInstances = Math.max(Math.max(numberOfInstancesReuquiredBasedOnMemoryConsumption, numberOfInstancesReuquiredBasedOnLoadAverage), numberOfInstancesReuquiredBasedOnRif);
        return numberOfInstances;
    }

    public int getMemberCount(String clusterId, int scalingPara) {

        int activeMemberCount = 0;
        int memberCount = 0;
        for (Service service : TopologyManager.getTopology().getServices()) {
            if (service.clusterExists(clusterId)) {
                Cluster cluster = service.getCluster(clusterId);

                for (Member member : cluster.getMembers()) {
                    if (member.isActive() || member.getStatus() == MemberStatus.Created || member.getStatus() == MemberStatus.Starting) {
                        memberCount++;
                        if (member.isActive()) {
                            activeMemberCount++;
                        }
                    }
                }
            }
        }
        if (scalingPara == 1) {
            return memberCount;
        } else {
            return activeMemberCount;
        }


    }

    public AutoscaleAlgorithm getAutoscaleAlgorithm(String partitionAlgorithm) {
        AutoscaleAlgorithm autoscaleAlgorithm = null;
        if (log.isDebugEnabled()) {
            log.debug(String.format("Partition algorithm is ", partitionAlgorithm));
        }
        if (Constants.ROUND_ROBIN_ALGORITHM_ID.equals(partitionAlgorithm)) {

            autoscaleAlgorithm = new RoundRobin();
        } else if (Constants.ONE_AFTER_ANOTHER_ALGORITHM_ID.equals(partitionAlgorithm)) {

            autoscaleAlgorithm = new OneAfterAnother();
        } else {
            if (log.isErrorEnabled()) {
                log.error(String.format("Partition algorithm %s could not be identified !", partitionAlgorithm));
            }
        }
        return autoscaleAlgorithm;
    }

    public void delegateInstanceCleanup(String memberId) {

        try {

            //calling SM to send the instance notification event.
            InstanceNotificationClient.getInstance().sendMemberCleanupEvent(memberId);
            log.info("Instance clean up event sent for [member] " + memberId);
        } catch (Throwable e) {
            log.error("Cannot terminate instance", e);
        }
    }

    public void delegateSpawn(ClusterLevelPartitionContext clusterMonitorPartitionContext, String clusterId, String instanceId, String lbRefType, boolean isPrimary) {

        try {

            String nwPartitionId = clusterMonitorPartitionContext.getNetworkPartitionId();
            NetworkPartitionLbHolder lbHolder =
                    PartitionManager.getInstance()
                            .getNetworkPartitionLbHolder(nwPartitionId);
            String lbClusterId = getLbClusterId(lbRefType, clusterMonitorPartitionContext, lbHolder);
            //Calculate accumulation of minimum counts of all the partition of current network partition
            int minimumCountOfNetworkPartition = 0;
            VMClusterMonitor vmClusterMonitor = (VMClusterMonitor) AutoscalerContext.getInstance().getClusterMonitor(clusterId);
            for (ClusterLevelPartitionContext partitionContextOfCurrentNetworkClusterMonitorPartition : vmClusterMonitor.getNetworkPartitionCtxt(instanceId, nwPartitionId).
                    getPartitionCtxts().values()) {

                minimumCountOfNetworkPartition += partitionContextOfCurrentNetworkClusterMonitorPartition.getMinimumMemberCount();
            }
            MemberContext memberContext =
                    CloudControllerClient.getInstance()
                            .spawnAnInstance(clusterMonitorPartitionContext.getPartition(),
                                    clusterId,
                                    lbClusterId, clusterMonitorPartitionContext.getNetworkPartitionId(),
                                    isPrimary,
                                    minimumCountOfNetworkPartition);
            if (memberContext != null) {
                clusterMonitorPartitionContext.addPendingMember(memberContext);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Pending member added, [member] %s [partition] %s", memberContext.getMemberId(),
                            memberContext.getPartition().getId()));
                }

            } else if (log.isDebugEnabled()) {
                log.debug("Returned member context is null, did not add to pending members");
            }

        } catch (Throwable e) {
            String message = "Cannot spawn an instance";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }


    public void delegateScalingDependencyNotification(String clusterId, String networkPartitionId, float factor) {

        //Notify parent for checking scaling dependencies
        AbstractClusterMonitor clusterMonitor = AutoscalerContext.getInstance().getClusterMonitor(clusterId);
        if (clusterMonitor instanceof VMServiceClusterMonitor) {

            VMServiceClusterMonitor vmServiceClusterMonitor = (VMServiceClusterMonitor) clusterMonitor;
            vmServiceClusterMonitor.sendClusterScalingEvent(networkPartitionId, factor);
        }

    }

    // Original method. Assume this is invoked from mincheck.drl
    
   /* public void delegateSpawn(PartitionContext partitionContext, String clusterId, String lbRefType) {
        try {

            String nwPartitionId = partitionContext.getNetworkPartitionId();
                                                         .getNetworkPartitionLbHolder(nwPartitionId);
            NetworkPartitionLbHolder lbHolder =
                                          PartitionManager.getInstance()
                                                          .getNetworkPartitionLbHolder(nwPartitionId);

            
            String lbClusterId = getLbClusterId(lbRefType, partitionContext, lbHolder);

            MemberContext memberContext =
                                         CloudControllerClient.getInstance()
                                                              .spawnAnInstance(partitionContext.getPartition(),
                                                                      clusterId,
                                                                      lbClusterId, partitionContext.getNetworkPartitionId());
            if (memberContext != null) {
                partitionContext.addPendingMember(memberContext);
                if(log.isDebugEnabled()){
                    log.debug(String.format("Pending member added, [member] %s [partition] %s", memberContext.getMemberId(),
                            memberContext.getPartition().getPartitionId()));
                }
            } else if(log.isDebugEnabled()){
                log.debug("Returned member context is null, did not add to pending members");
            }

        } catch (Throwable e) {
            String message = "Cannot spawn an instance";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
   	}*/


    public static String getLbClusterId(String lbRefType, ClusterLevelPartitionContext partitionCtxt,
                                        NetworkPartitionLbHolder networkPartitionLbHolder) {

        String lbClusterId = null;

        if (lbRefType != null) {
            if (lbRefType.equals(StratosConstants.DEFAULT_LOAD_BALANCER)) {
                lbClusterId = networkPartitionLbHolder.getDefaultLbClusterId();
//                lbClusterId = nwPartitionCtxt.getDefaultLbClusterId();
            } else if (lbRefType.equals(StratosConstants.SERVICE_AWARE_LOAD_BALANCER)) {
                String serviceName = partitionCtxt.getServiceName();
                lbClusterId = networkPartitionLbHolder.getLBClusterIdOfService(serviceName);
//                lbClusterId = nwPartitionCtxt.getLBClusterIdOfService(serviceName);
            } else {
                log.warn("Invalid LB reference type defined: [value] " + lbRefType);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Getting LB id for spawning instance [lb reference] %s ," +
                            " [partition] %s [network partition] %s [Lb id] %s ", lbRefType, partitionCtxt.getPartitionId(),
                    networkPartitionLbHolder.getNetworkPartitionId(), lbClusterId));
        }
        return lbClusterId;
    }

    public void delegateTerminate(ClusterLevelPartitionContext clusterMonitorPartitionContext, String memberId) {

        log.info("Starting to terminate Member [ " + memberId + " ], in Partition [ " +
                clusterMonitorPartitionContext.getPartitionId() + " ], NW Partition [ " +
                clusterMonitorPartitionContext.getNetworkPartitionId() + " ]");

        try {
            //Moving member to pending termination list
            if (clusterMonitorPartitionContext.activeMemberAvailable(memberId)) {
                clusterMonitorPartitionContext.moveActiveMemberToTerminationPendingMembers(memberId);
            } else if (clusterMonitorPartitionContext.pendingMemberAvailable(memberId)) {
                clusterMonitorPartitionContext.movePendingMemberToObsoleteMembers(memberId);
            }
        } catch (Throwable e) {
            log.error("Cannot terminate instance", e);
        }
    }

    public void delegateTerminateDependency(ClusterLevelPartitionContext clusterMonitorPartitionContext, String memberId) {
        try {
            //calling SM to send the instance notification event.
            if (log.isDebugEnabled()) {
                log.debug("delegateTerminateDependency:memberId:" + memberId);
            }
            //InstanceNotificationClient.getInstance().sendMemberCleanupEvent(memberId);
            //partitionContext.moveActiveMemberToTerminationPendingMembers(memberId);
            //CloudControllerClient.getInstance().terminate(memberId);
        } catch (Throwable e) {
            log.error("Cannot terminate instance", e);
        }
    }

    public void terminateObsoleteInstance(String memberId) {
        try {
            CloudControllerClient.getInstance().terminate(memberId);
        } catch (Throwable e) {
            log.error("Cannot terminate instance", e);
        }
    }

    //Grouping
    public void delegateTerminateAll(String clusterId) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("delegateTerminateAll - begin");
            }
            CloudControllerClient.getInstance().terminateAllInstances(clusterId);
            if (log.isDebugEnabled()) {
                log.debug("delegateTerminateAll - done");
            }
        } catch (Throwable e) {
            log.error("Cannot terminate instance", e);
        }
    }

    public void delegateStartContainers(KubernetesClusterContext kubernetesClusterContext) {
        try {
            String kubernetesClusterId = kubernetesClusterContext.getKubernetesClusterID();
            String clusterId = kubernetesClusterContext.getClusterId();
            CloudControllerClient ccClient = CloudControllerClient.getInstance();
            MemberContext[] memberContexts = ccClient.startContainers(kubernetesClusterId, clusterId);
            if (null != memberContexts) {
                for (MemberContext memberContext : memberContexts) {
                    if (null != memberContext) {
                        kubernetesClusterContext.addPendingMember(memberContext);
                        kubernetesClusterContext.setServiceClusterCreated(true);
                        if (log.isDebugEnabled()) {
                            log.debug(String.format(
                                    "Pending member added, [member] %s [kub cluster] %s",
                                    memberContext.getMemberId(), kubernetesClusterId));
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Returned member context is null, did not add any pending members");
                        }
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Returned member context is null, did not add to pending members");
                }
            }
        } catch (Exception e) {
            log.error("Cannot create containers ", e);
        }
    }

    public void delegateScaleUpContainers(KubernetesClusterContext kubernetesClusterContext,
                                          int newReplicasCount) {
        String clusterId = kubernetesClusterContext.getClusterId();
        try {
            CloudControllerClient ccClient = CloudControllerClient.getInstance();
            // getting newly created pods' member contexts
            MemberContext[] memberContexts = ccClient.updateContainers(clusterId, newReplicasCount);
            if (null != memberContexts) {
                for (MemberContext memberContext : memberContexts) {
                    if (null != memberContext) {
                        kubernetesClusterContext.addPendingMember(memberContext);
                        if (log.isDebugEnabled()) {
                            String kubernetesClusterID = kubernetesClusterContext.getKubernetesClusterID();
                            log.debug(String.format(
                                    "Pending member added, [member] %s [kub cluster] %s",
                                    memberContext.getMemberId(), kubernetesClusterID));
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Returned member context is null, did not add any pending members");
                        }
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Returned array of member context is null, did not add to pending members");
                }
            }
        } catch (Exception e) {
            log.error("Scaling up failed, couldn't update kubernetes controller ", e);
        }
    }

    public void delegateScaleDownContainers(KubernetesClusterContext kubernetesClusterContext,
                                            int newReplicasCount) {
        String clusterId = kubernetesClusterContext.getClusterId();
        try {
            CloudControllerClient ccClient = CloudControllerClient.getInstance();
            // getting terminated pods's member contexts
            MemberContext[] memberContexts = ccClient.updateContainers(clusterId, newReplicasCount);
            if (null != memberContexts) {
                for (MemberContext memberContext : memberContexts) {
                    if (null != memberContext) {
                        // we are not removing from active/pending list, it will be handled in AS event receiver
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Scaling down, terminated the member with id %s in cluster %s",
                                    memberContext.getMemberId(), memberContext.getClusterId()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Scaling down failed, couldn't update kubernetes controller ", e);
        }
    }

    public void delegateTerminateContainer(KubernetesClusterContext kubernetesClusterContext, String memberId) {
        try {
            CloudControllerClient ccClient = CloudControllerClient.getInstance();
            ccClient.terminateContainer(memberId);
        } catch (TerminationException e) {
            log.error("Cannot delete container ", e);
        }
    }

    public int getPredictedReplicasForStat(int minReplicas, float statUpperLimit, float statPredictedValue) {
        if (statUpperLimit == 0) {
            return 0;
        }
        float predictedValue = ((minReplicas / statUpperLimit) * statPredictedValue);
        return (int) Math.ceil(predictedValue);
    }

    public double getLoadAveragePredictedValue(ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext) {
        double loadAveragePredicted = 0.0d;
        int totalMemberCount = 0;

        for (ClusterLevelPartitionContext partitionContext : clusterLevelNetworkPartitionContext.getPartitionCtxts().values()) {
            for (MemberStatsContext memberStatsContext : partitionContext.getMemberStatsContexts().values()) {

                float memberAverageLoadAverage = memberStatsContext.getLoadAverage().getAverage();
                float memberGredientLoadAverage = memberStatsContext.getLoadAverage().getGradient();
                float memberSecondDerivativeLoadAverage = memberStatsContext.getLoadAverage().getSecondDerivative();

                double memberPredictedLoadAverage = getPredictedValueForNextMinute(memberAverageLoadAverage, memberGredientLoadAverage, memberSecondDerivativeLoadAverage, 1);

                log.debug("Member ID : " + memberStatsContext.getMemberId() + " : Predicted Load Average : " + memberPredictedLoadAverage);

                loadAveragePredicted += memberPredictedLoadAverage;
                ++totalMemberCount;
            }
        }

        if (totalMemberCount > 0) {
            log.debug("Predicted load average : " + loadAveragePredicted / totalMemberCount);
            return loadAveragePredicted / totalMemberCount;
        } else {
            return 0;
        }
    }

    public double getMemoryConsumptionPredictedValue(ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext) {
        double memoryConsumptionPredicted = 0.0d;
        int totalMemberCount = 0;

        for (ClusterLevelPartitionContext partitionContext : clusterLevelNetworkPartitionContext.getPartitionCtxts().values()) {
            for (MemberStatsContext memberStatsContext : partitionContext.getMemberStatsContexts().values()) {

                float memberMemoryConsumptionAverage = memberStatsContext.getMemoryConsumption().getAverage();
                float memberMemoryConsumptionGredient = memberStatsContext.getMemoryConsumption().getGradient();
                float memberMemoryConsumptionSecondDerivative = memberStatsContext.getMemoryConsumption().getSecondDerivative();

                double memberPredictedMemoryConsumption = getPredictedValueForNextMinute(memberMemoryConsumptionAverage, memberMemoryConsumptionGredient, memberMemoryConsumptionSecondDerivative, 1);

                log.debug("Member ID : " + memberStatsContext.getMemberId() + " : Predicted Memory Consumption : " + memberPredictedMemoryConsumption);

                memoryConsumptionPredicted += memberPredictedMemoryConsumption;
                ++totalMemberCount;
            }
        }

        if (totalMemberCount > 0) {
            log.debug("Predicted memory consumption : " + memoryConsumptionPredicted / totalMemberCount);
            return memoryConsumptionPredicted / totalMemberCount;
        } else {
            return 0;
        }
    }
}

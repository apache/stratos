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

package org.apache.stratos.autoscaler.rule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.algorithms.PartitionAlgorithm;
import org.apache.stratos.autoscaler.algorithms.partition.OneAfterAnother;
import org.apache.stratos.autoscaler.algorithms.partition.RoundRobin;
import org.apache.stratos.autoscaler.client.AutoscalerCloudControllerClient;
import org.apache.stratos.autoscaler.context.AutoscalerContext;
import org.apache.stratos.autoscaler.context.cluster.ClusterContext;
import org.apache.stratos.autoscaler.context.cluster.ClusterInstanceContext;
import org.apache.stratos.autoscaler.context.member.MemberStatsContext;
import org.apache.stratos.autoscaler.context.partition.ClusterLevelPartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.ClusterLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.event.publisher.InstanceNotificationPublisher;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.cloud.controller.stub.domain.MemberContext;
import org.apache.stratos.common.constants.StratosConstants;

/**
 * This will have utility methods that need to be executed from rule file...
 */
public class RuleTasksDelegator {

    private static boolean arspiIsSet = false;

    private static final Log log = LogFactory.getLog(RuleTasksDelegator.class);

    public double getPredictedValueForNextMinute(float average, float gradient, float secondDerivative,
                                                 int timeInterval) {
        double predictedValue;
        //        s = u * t + 0.5 * a * t * t
        if (log.isDebugEnabled()) {
            log.debug(String.format("Predicting the value, [average]: %s , [gradient]: %s , [second derivative] " +
                    ": %s , [time intervals]: %s ", average, gradient, secondDerivative, timeInterval));
        }
        predictedValue = average + gradient * timeInterval + 0.5 * secondDerivative * timeInterval * timeInterval;

        return predictedValue;
    }

    public int getNumberOfInstancesRequiredBasedOnRif(float rifPredictedValue, float rifThreshold) {

        if (rifThreshold != 0) {

            float requiredNumberOfInstances = rifPredictedValue / rifThreshold;
            return (int) Math.ceil(requiredNumberOfInstances);
        } else {
            log.error("Request in flight threshold is Zero");
            return 0;
        }

    }

    public int getNumberOfInstancesRequiredBasedOnMemoryConsumption(float threshold, double predictedValue, int min,
                                                                    int max) {
        double numberOfAdditionalInstancesRequired = 0;
        if (predictedValue != threshold) {

            float scalingRange = 100 - threshold;
            int instanceRange = max - min;

            if (instanceRange != 0) {

                float gradient = scalingRange / instanceRange;
                numberOfAdditionalInstancesRequired = (predictedValue - threshold) / gradient;
            }

            if (predictedValue < threshold) {
                //Since predicted-value is less, it can be scale-down
                return min - 1;
            }
        }

        return (int) Math.ceil(min + numberOfAdditionalInstancesRequired);
    }

    public int getNumberOfInstancesRequiredBasedOnLoadAverage(float threshold, double predictedValue, int min) {

        double numberOfInstances;
        if (threshold != 0) {

            numberOfInstances = (min * predictedValue) / threshold;
            return (int) Math.ceil(numberOfInstances);
        }

        return min;
    }

    public int getMaxNumberOfInstancesRequired(int numberOfInstancesRequiredBasedOnRif,
                                               int numberOfInstancesRequiredBasedOnMemoryConsumption, boolean mcReset,
                                               int numberOfInstancesReuquiredBasedOnLoadAverage, boolean laReset) {
        int numberOfInstances = 0;

        int rifBasedRequiredInstances = 0;
        int mcBasedRequiredInstances = 0;
        int laBasedRequiredInstances = 0;
        if (arspiIsSet) {
            rifBasedRequiredInstances = numberOfInstancesRequiredBasedOnRif;
        }
        if (mcReset) {
            mcBasedRequiredInstances = numberOfInstancesRequiredBasedOnMemoryConsumption;
        }
        if (laReset) {
            laBasedRequiredInstances = numberOfInstancesReuquiredBasedOnLoadAverage;
        }
        numberOfInstances = Math.max(Math.max(numberOfInstancesRequiredBasedOnMemoryConsumption,
                        numberOfInstancesReuquiredBasedOnLoadAverage),
                numberOfInstancesRequiredBasedOnRif);
        return numberOfInstances;
    }

    public PartitionAlgorithm getPartitionAlgorithm(String partitionAlgorithm) {

        PartitionAlgorithm autoscaleAlgorithm = null;
        //FIXME to not parse for algo when partition is chosen by the parent

        if (partitionAlgorithm == null) {
            //Send one after another as default
            partitionAlgorithm = StratosConstants.PARTITION_ONE_AFTER_ANOTHER_ALGORITHM_ID;
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Retrieving partition algorithm [Partition algorithm]: %s", partitionAlgorithm));
        }
        if (StratosConstants.PARTITION_ROUND_ROBIN_ALGORITHM_ID.equals(partitionAlgorithm)) {
            autoscaleAlgorithm = new RoundRobin();
        } else if (StratosConstants.PARTITION_ONE_AFTER_ANOTHER_ALGORITHM_ID.equals(partitionAlgorithm)) {
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
            // send the instance notification event.
            InstanceNotificationPublisher.getInstance().sendInstanceCleanupEventForMember(memberId);
            log.info("Instance clean up event sent for [member] " + memberId);

        } catch (Exception e) {
            log.error("Cannot terminate instance", e);
        }
    }

    /**
     * Invoked from drools to start an instance.
     *
     * @param clusterMonitorPartitionContext Cluster monitor partition context
     * @param clusterId                      Cluster id
     * @param clusterInstanceId              Instance id
     * @param isPrimary                      Is a primary member
     */
    public void delegateSpawn(ClusterLevelPartitionContext clusterMonitorPartitionContext, String clusterId,
                              String clusterInstanceId, boolean isPrimary, String autoscalingReason, long scalingTime) {

        try {
            String nwPartitionId = clusterMonitorPartitionContext.getNetworkPartitionId();

            // Calculate accumulation of minimum counts of all the partition of current network partition
            int minimumCountOfNetworkPartition;
            ClusterMonitor clusterMonitor = AutoscalerContext.getInstance().getClusterMonitor(clusterId);
            ClusterContext clusterContext = clusterMonitor.getClusterContext();
            ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext =
                    clusterContext.getNetworkPartitionCtxt(nwPartitionId);
            ClusterInstanceContext clusterInstanceContext =
                    (ClusterInstanceContext) clusterLevelNetworkPartitionContext.
                            getInstanceContext(
                                    clusterInstanceId);
            minimumCountOfNetworkPartition = clusterInstanceContext.getMinInstanceCount();

            MemberContext memberContext = AutoscalerCloudControllerClient.getInstance().startInstance(
                    clusterMonitorPartitionContext.getPartition(), clusterId, clusterInstanceId,
                    clusterMonitorPartitionContext.getNetworkPartitionId(), isPrimary, minimumCountOfNetworkPartition,
                    autoscalingReason, scalingTime);
            if (memberContext != null) {
                ClusterLevelPartitionContext partitionContext = clusterInstanceContext.
                        getPartitionCtxt(
                                clusterMonitorPartitionContext
                                        .getPartitionId());
                partitionContext.addPendingMember(memberContext);
                partitionContext.addMemberStatsContext(new MemberStatsContext(memberContext.getMemberId()));
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Pending member added, [member] %s [partition] %s",
                            memberContext.getMemberId(), memberContext.getPartition().getId()));
                }

            } else {
                if (log.isErrorEnabled()) {
                    log.error("Member context returned from cloud controller is null");
                }
            }
        } catch (Exception e) {
            String message = String.format("Could not start instance: [cluster-id] %s [instance-id] %s", clusterId,
                    clusterInstanceId);
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    public void delegateScalingDependencyNotification(String clusterId, String networkPartitionId, String instanceId,
                                                      int requiredInstanceCount, int minimumInstanceCount) {

        if (log.isDebugEnabled()) {
            log.debug("Scaling dependent notification is going to the [parentInstance] " + instanceId);
        }
        //Notify parent for checking scaling dependencies
        ClusterMonitor clusterMonitor = AutoscalerContext.getInstance().getClusterMonitor(clusterId);
        float fMinimumInstanceCount = minimumInstanceCount;
        float factor = requiredInstanceCount / fMinimumInstanceCount;
        clusterMonitor.sendClusterScalingEvent(networkPartitionId, instanceId, factor);
    }

    public void delegateScalingOverMaxNotification(String clusterId, String networkPartitionId, String instanceId) {
        if (log.isDebugEnabled()) {
            log.debug("Scaling max out notification is going to the [parentInstance] " + instanceId);
        }
        //Notify parent for checking scaling dependencies
        ClusterMonitor clusterMonitor = AutoscalerContext.getInstance().getClusterMonitor(clusterId);
        clusterMonitor.sendScalingOverMaxEvent(networkPartitionId, instanceId);
    }

    public void delegateScalingDownBeyondMinNotification(String clusterId, String networkPartitionId,
                                                         String instanceId) {
        if (log.isDebugEnabled()) {
            log.debug("Scaling down lower min notification is going to the [parentInstance] " + instanceId);
        }
        //Notify parent for checking scaling dependencies
        ClusterMonitor clusterMonitor = AutoscalerContext.getInstance().getClusterMonitor(clusterId);
        clusterMonitor.sendScalingDownBeyondMinEvent(networkPartitionId, instanceId);
    }

    public void delegateTerminate(ClusterLevelPartitionContext clusterMonitorPartitionContext, String memberId) {

        try {
            //Moving member to pending termination list
            if (clusterMonitorPartitionContext.activeMemberAvailable(memberId)) {

                log.info(String.format("[scale-down] Moving active member to termination pending list [member id] %s " +
                                "[partition] %s [network partition] %s", memberId,
                        clusterMonitorPartitionContext.getPartitionId(),
                        clusterMonitorPartitionContext.getNetworkPartitionId()));
                clusterMonitorPartitionContext.moveActiveMemberToTerminationPendingMembers(memberId);
                clusterMonitorPartitionContext.removeMemberStatsContext(memberId);
            } else if (clusterMonitorPartitionContext.pendingMemberAvailable(memberId)) {

                log.info(String.format(
                        "[scale-down] Moving pending member to termination pending list [member id] %s " +
                                "[partition] %s [network partition] %s", memberId,
                        clusterMonitorPartitionContext.getPartitionId(),
                        clusterMonitorPartitionContext.getNetworkPartitionId()));
                clusterMonitorPartitionContext.movePendingMemberToObsoleteMembers(memberId);
                clusterMonitorPartitionContext.removeMemberStatsContext(memberId);
            }
        } catch (Exception e) {
            log.error("[scale-down] Cannot move member to termination pending list ", e);
        }
    }

    public void delegateTerminateDependency(ClusterLevelPartitionContext clusterMonitorPartitionContext,
                                            String memberId) {
        try {
            //calling SM to send the instance notification event.
            if (log.isDebugEnabled()) {
                log.debug("delegateTerminateDependency:memberId:" + memberId);
            }

        } catch (Exception e) {
            log.error("Cannot terminate instance", e);
        }
    }

    public void terminateObsoleteInstance(String memberId) {
        try {
            AutoscalerCloudControllerClient.getInstance().terminateInstance(memberId);
        } catch (Exception e) {
            log.error("Cannot terminate instance", e);
        }
    }

    //Grouping
    public void delegateTerminateAll(String clusterId) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("delegateTerminateAll - begin");
            }
            AutoscalerCloudControllerClient.getInstance().terminateAllInstances(clusterId);
            if (log.isDebugEnabled()) {
                log.debug("delegateTerminateAll - done");
            }
        } catch (Exception e) {
            log.error("Cannot terminate instance", e);
        }
    }

    public int getPredictedReplicasForStat(int minReplicas, float statUpperLimit, float statPredictedValue) {
        if (statUpperLimit == 0) {
            return 0;
        }
        float predictedValue = ((minReplicas / statUpperLimit) * statPredictedValue);
        return (int) Math.ceil(predictedValue);
    }

    public double getLoadAveragePredictedValue(ClusterInstanceContext clusterInstanceContext) {
        double loadAveragePredicted = 0.0d;
        int totalMemberCount = 0;
        for (ClusterLevelPartitionContext partitionContext : clusterInstanceContext.getPartitionCtxts()) {
            for (MemberStatsContext memberStatsContext : partitionContext.getMemberStatsContexts().values()) {

                float memberAverageLoadAverage = memberStatsContext.getLoadAverage().getAverage();
                float memberGredientLoadAverage = memberStatsContext.getLoadAverage().getGradient();
                float memberSecondDerivativeLoadAverage = memberStatsContext.getLoadAverage().getSecondDerivative();

                double memberPredictedLoadAverage =
                        getPredictedValueForNextMinute(memberAverageLoadAverage, memberGredientLoadAverage,
                                memberSecondDerivativeLoadAverage, 1);

                if (log.isDebugEnabled()) {
                    log.debug(String.format("[cluster-instance-id] %s [member-id] %s " + "[predicted load average] %s ",
                            clusterInstanceContext.getId(), memberStatsContext.getMemberId(),
                            memberPredictedLoadAverage));
                }
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

    public double getMemoryConsumptionPredictedValue(ClusterInstanceContext clusterInstanceContext) {
        double memoryConsumptionPredicted = 0.0d;
        int totalMemberCount = 0;
        for (ClusterLevelPartitionContext partitionContext : clusterInstanceContext.getPartitionCtxts()) {
            for (MemberStatsContext memberStatsContext : partitionContext.getMemberStatsContexts().values()) {

                float memberMemoryConsumptionAverage = memberStatsContext.getMemoryConsumption().getAverage();
                float memberMemoryConsumptionGredient = memberStatsContext.getMemoryConsumption().getGradient();
                float memberMemoryConsumptionSecondDerivative =
                        memberStatsContext.getMemoryConsumption().getSecondDerivative();

                double memberPredictedMemoryConsumption =
                        getPredictedValueForNextMinute(memberMemoryConsumptionAverage, memberMemoryConsumptionGredient,
                                memberMemoryConsumptionSecondDerivative, 1);

                if (log.isDebugEnabled()) {
                    log.debug(String.format("[member-id] %s [predicted memory consumption] %s ",
                            memberStatsContext.getMemberId(), memberPredictedMemoryConsumption));
                }
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

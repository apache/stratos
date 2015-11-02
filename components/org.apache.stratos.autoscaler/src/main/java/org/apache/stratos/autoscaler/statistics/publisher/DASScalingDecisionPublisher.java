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

package org.apache.stratos.autoscaler.statistics.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.util.AutoscalerConstants;
import org.apache.stratos.common.statistics.publisher.ThriftStatisticsPublisher;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.StreamDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * MemberInfoPublisher to publish member information/metadata to DAS.
 */
public class DASScalingDecisionPublisher extends ThriftStatisticsPublisher implements ScalingDecisionPublisher {

    private static final Log log = LogFactory.getLog(DASScalingDecisionPublisher.class);
    private static final String DATA_STREAM_NAME = "scaling_decision";
    private static final String VERSION = "1.0.0";
    private static final String DAS_THRIFT_CLIENT_NAME = "das";
    private ExecutorService executorService;

    public DASScalingDecisionPublisher() {
        super(createStreamDefinition(), DAS_THRIFT_CLIENT_NAME);
        executorService = StratosThreadPool.getExecutorService("autoscaler.stats.publisher.thread.pool", 10);
    }

    private static StreamDefinition createStreamDefinition() {
        try {
            // Create stream definition
            StreamDefinition streamDefinition = new StreamDefinition(DATA_STREAM_NAME, VERSION);
            streamDefinition.setNickName("Member Information");
            streamDefinition.setDescription("Member Information");
            List<Attribute> payloadData = new ArrayList<Attribute>();

            // Set payload definition
            payloadData.add(new Attribute(AutoscalerConstants.TIMESTAMP, AttributeType.LONG));
            payloadData.add(new Attribute(AutoscalerConstants.SCALING_DECISION_ID, AttributeType.STRING));
            payloadData.add(new Attribute(AutoscalerConstants.CLUSTER_ID, AttributeType.STRING));
            payloadData.add(new Attribute(AutoscalerConstants.MIN_INSTANCE_COUNT, AttributeType.INT));
            payloadData.add(new Attribute(AutoscalerConstants.MAX_INSTANCE_COUNT, AttributeType.INT));
            payloadData.add(new Attribute(AutoscalerConstants.RIF_PREDICTED, AttributeType.INT));
            payloadData.add(new Attribute(AutoscalerConstants.RIF_THRESHOLD, AttributeType.INT));
            payloadData.add(new Attribute(AutoscalerConstants.RIF_REQUIRED_INSTANCES, AttributeType.INT));
            payloadData.add(new Attribute(AutoscalerConstants.MC_PREDICTED, AttributeType.INT));
            payloadData.add(new Attribute(AutoscalerConstants.MC_THRESHOLD, AttributeType.INT));
            payloadData.add(new Attribute(AutoscalerConstants.MC_REQUIRED_INSTANCES, AttributeType.INT));
            payloadData.add(new Attribute(AutoscalerConstants.LA_PREDICTED, AttributeType.INT));
            payloadData.add(new Attribute(AutoscalerConstants.LA_THRESHOLD, AttributeType.INT));
            payloadData.add(new Attribute(AutoscalerConstants.LA_REQUIRED_INSTANCES, AttributeType.INT));
            payloadData.add(new Attribute(AutoscalerConstants.REQUIRED_INSTANCE_COUNT, AttributeType.INT));
            payloadData.add(new Attribute(AutoscalerConstants.ACTIVE_INSTANCE_COUNT, AttributeType.INT));
            payloadData.add(new Attribute(AutoscalerConstants.ADDITIONAL_INSTANCE_COUNT, AttributeType.INT));
            payloadData.add(new Attribute(AutoscalerConstants.SCALING_REASON, AttributeType.STRING));
            streamDefinition.setPayloadData(payloadData);
            return streamDefinition;
        } catch (Exception e) {
            throw new RuntimeException("Could not create stream definition", e);
        }
    }

    /**
     * Publishing scaling decision to DAS.
     *
     * @param timestamp               Scaling Time
     * @param scalingDecisionId       Scaling Decision Id
     * @param clusterId               Cluster Id
     * @param minInstanceCount        Minimum Instance Count
     * @param maxInstanceCount        Maximum Instance Count
     * @param rifPredicted            RIF Predicted
     * @param rifThreshold            RIF Threshold
     * @param rifRequiredInstances    RIF Required Instances
     * @param mcPredicted             MC Predicted
     * @param mcThreshold             MC Threshold
     * @param mcRequiredInstances     MC Required Instances
     * @param laPredicted             LA Predicted
     * @param laThreshold             LA Threshold
     * @param laRequiredInstance      LA Required Instance
     * @param requiredInstanceCount   Required Instance Count
     * @param activeInstanceCount     Active Instance Count
     * @param additionalInstanceCount Additional Instance Needed
     * @param scalingReason           Scaling Reason
     */
    @Override
    public void publish(final Long timestamp, final String scalingDecisionId, final String clusterId,
                        final int minInstanceCount, final int maxInstanceCount,
                        final int rifPredicted, final int rifThreshold, final int rifRequiredInstances,
                        final int mcPredicted, final int mcThreshold, final int mcRequiredInstances,
                        final int laPredicted, final int laThreshold, final int laRequiredInstance,
                        final int requiredInstanceCount, final int activeInstanceCount,
                        final int additionalInstanceCount, final String scalingReason) {
        Runnable publisher = new Runnable() {
            @Override
            public void run() {
                if (log.isDebugEnabled())

                {
                    log.debug(String.format("Publishing scaling decision: [timestamp] %d [scaling_decision_id] %s " +
                                    "[cluster_id] %s [min_instance_count] %d [max_instance_count] %d " +
                                    "[rif_predicted] %d [rif_threshold] %d [rif_required_instances] %d " +
                                    "[mc_predicted] %d [mc_threshold] %d [mc_required_instances] %d " +
                                    "[la_predicted] %d [la_threshold] %d [la_required_instances] %d " +
                                    "[required_instance_count] %d [active_instance_count] %d " +
                                    "[addtitional_instance_count] %d [scaling_reason] %s",
                            timestamp, scalingDecisionId, clusterId, minInstanceCount, maxInstanceCount, rifPredicted,
                            rifThreshold, rifRequiredInstances, mcPredicted, mcThreshold, mcRequiredInstances,
                            laPredicted, laThreshold, laRequiredInstance, requiredInstanceCount, activeInstanceCount,
                            additionalInstanceCount, scalingReason));
                }

                //adding payload data
                List<Object> payload = new ArrayList<Object>();
                payload.add(timestamp);
                payload.add(scalingDecisionId);
                payload.add(clusterId);
                payload.add(minInstanceCount);
                payload.add(maxInstanceCount);
                payload.add(rifPredicted);
                payload.add(rifThreshold);
                payload.add(rifRequiredInstances);
                payload.add(mcPredicted);
                payload.add(mcThreshold);
                payload.add(mcRequiredInstances);
                payload.add(laPredicted);
                payload.add(laThreshold);
                payload.add(laRequiredInstance);
                payload.add(requiredInstanceCount);
                payload.add(activeInstanceCount);
                payload.add(additionalInstanceCount);
                payload.add(scalingReason);
                DASScalingDecisionPublisher.super.publish(payload.toArray());
            }

        };
        executorService.execute(publisher);
    }
}


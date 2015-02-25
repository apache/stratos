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

package org.apache.stratos.autoscaler.context.cluster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.exception.partition.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.policy.PolicyValidationException;
import org.apache.stratos.autoscaler.pojo.policy.PolicyManager;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.messaging.domain.topology.Cluster;

public class ClusterContextFactory {

    private static final Log log = LogFactory.getLog(ClusterContextFactory.class);

    public static ClusterContext getVMClusterContext(String instanceId, Cluster cluster, boolean hasScalingDependents)
            throws PolicyValidationException, PartitionValidationException {

        if (null == cluster) {
            return null;
        }
        
        String autoscalePolicyName = cluster.getAutoscalePolicyName();
        AutoscalePolicy autoscalePolicy = PolicyManager.getInstance().getAutoscalePolicy(autoscalePolicyName);

        if (log.isDebugEnabled()) {
            log.debug("Autoscaler policy name: " + autoscalePolicyName);
        }

        return new ClusterContext(cluster.getClusterId(), cluster.getServiceName(), autoscalePolicy, hasScalingDependents);
    }
}

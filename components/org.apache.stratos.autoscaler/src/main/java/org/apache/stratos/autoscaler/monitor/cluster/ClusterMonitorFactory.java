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
import org.apache.stratos.messaging.domain.topology.Cluster;

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
    public static ClusterMonitor getMonitor(Cluster cluster, boolean hasScalingDependents,
                                                    boolean groupScalingEnabledSubtree)
            throws PolicyValidationException, PartitionValidationException {

        ClusterMonitor clusterMonitor =
                getClusterMonitor(cluster, hasScalingDependents, groupScalingEnabledSubtree);
        return clusterMonitor;
    }

    private static ClusterMonitor getClusterMonitor(Cluster cluster, boolean hasScalingDependents,
                                                    boolean groupScalingEnabledSubtree)
            throws PolicyValidationException, PartitionValidationException {

        if (null == cluster) {
            return null;
        }

        ClusterMonitor clusterMonitor = new ClusterMonitor(cluster, hasScalingDependents, groupScalingEnabledSubtree);

        // find lb reference type
        java.util.Properties props = cluster.getProperties();

        if (props != null) {
//            if (props.containsKey(StratosConstants.LOAD_BALANCER_REF)) {
//                String value = props.getProperty(StratosConstants.LOAD_BALANCER_REF);
//                clusterMonitor.setLbReferenceType(value);
//                if (log.isDebugEnabled()) {
//                    log.debug("Set the lb reference type: " + value);
//                }
//            }

            // set hasPrimary property
            // hasPrimary is true if there are primary members available in that cluster
            clusterMonitor.setHasPrimary(Boolean.parseBoolean(cluster.getProperties().getProperty(IS_PRIMARY)));
        }


        log.info("ClusterMonitor created: " + clusterMonitor.toString());
        return clusterMonitor;
    }
}

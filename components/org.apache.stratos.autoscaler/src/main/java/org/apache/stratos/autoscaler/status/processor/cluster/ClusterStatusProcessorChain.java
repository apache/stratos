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
package org.apache.stratos.autoscaler.status.processor.cluster;

import org.apache.stratos.autoscaler.status.processor.StatusProcessorChain;

/**
 * Cluster status tracking processor chain
 */
public class ClusterStatusProcessorChain extends StatusProcessorChain {
    private ClusterStatusTerminatedProcessor clusterStatusTerminatedProcessor;
    private ClusterStatusInactiveProcessor clusterStatusInactiveProcessor;
    private ClusterStatusActiveProcessor clusterStatusActiveProcessor;

    @Override
    public void initialize() {
        clusterStatusTerminatedProcessor = new ClusterStatusTerminatedProcessor();
        add(clusterStatusTerminatedProcessor);

        clusterStatusInactiveProcessor = new ClusterStatusInactiveProcessor();
        add(clusterStatusInactiveProcessor);

        clusterStatusActiveProcessor = new ClusterStatusActiveProcessor();
        add(clusterStatusActiveProcessor);

    }

    public boolean process(final String type, final String clusterId, final String instanceId) {
        ClusterStatusProcessor root = (ClusterStatusProcessor) list.getFirst();
        if (root == null) {
            throw new RuntimeException("Message processor chain is not initialized");
        }
        return root.process(type, clusterId, instanceId);
    }
}

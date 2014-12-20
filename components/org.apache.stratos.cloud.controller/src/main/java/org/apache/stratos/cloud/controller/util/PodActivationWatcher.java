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
package org.apache.stratos.cloud.controller.util;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.MemberContext;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.kubernetes.client.KubernetesApiClient;
import org.apache.stratos.kubernetes.client.model.Pod;

/**
 * Checks whether a container is active and update the
 * {@link org.apache.stratos.cloud.controller.context.CloudControllerContext}.
 */
public class PodActivationWatcher implements Runnable {

    private static final Log log = LogFactory.getLog(PodActivationWatcher.class);

    private static final String POD_STATE_RUNNING = "Running";

    private String podId;
    private MemberContext memberContext;
    private KubernetesApiClient kubApi;
    
    public PodActivationWatcher(String podId, MemberContext memberContext, KubernetesApiClient kubApi) {
        this.podId = podId;
        this.memberContext = memberContext;
        this.kubApi = kubApi;
    }

    @Override
    public void run() {
        try {
            CloudControllerContext cloudControllerContext = CloudControllerContext.getInstance();
            Pod pod = kubApi.getPod(podId);
            if (log.isDebugEnabled()) {
                log.debug("Pod activation watcher running: [status] " + pod.getCurrentState().getStatus());
            }
            if (POD_STATE_RUNNING.equals(pod.getCurrentState().getStatus()) && memberContext.getPublicIpAddress() == null) {
                String hostIP = pod.getCurrentState().getHost();
                memberContext.setPublicIpAddress(hostIP);
                memberContext.setPrivateIpAddress(hostIP);
                cloudControllerContext.addMemberContext(memberContext);

                // trigger topology
                TopologyBuilder.handleMemberSpawned(memberContext);
                cloudControllerContext.persist();
            }
            
        } catch (Exception e) {
            // not logging exception intentionally
            log.error("Container activation watcher failed: " + e.getMessage());
            
            if (log.isDebugEnabled()) {
                log.debug(e);
            }
        }
    }
}

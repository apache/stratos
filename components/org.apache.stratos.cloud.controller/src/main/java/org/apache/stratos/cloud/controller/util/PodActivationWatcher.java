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
import org.apache.stratos.cloud.controller.pojo.MemberContext;
import org.apache.stratos.cloud.controller.registry.RegistryManager;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.topology.TopologyBuilder;
import org.apache.stratos.kubernetes.client.KubernetesApiClient;
import org.apache.stratos.kubernetes.client.model.Pod;

/**
 * Checks whether a container is active and update the {@link FasterLookUpDataHolder}.
 */
public class PodActivationWatcher implements Runnable {

    private static final Log LOG = LogFactory
            .getLog(PodActivationWatcher.class);
    private String podId;
    private MemberContext ctxt;
    private KubernetesApiClient kubApi;
    
    public PodActivationWatcher(String podId, MemberContext ctxt, KubernetesApiClient kubApi) {
        this.podId = podId;
        this.ctxt = ctxt;
        this.kubApi = kubApi;
    }

    @Override
    public void run() {
        try {
            FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();
            Pod pod = kubApi.getPod(podId);
            if (LOG.isDebugEnabled()) {
                LOG.debug("PodActivationWatcher running : "+pod.getCurrentState().getStatus());
            }
            if ("Running".equals(pod.getCurrentState().getStatus()) && ctxt.getPublicIpAddress() == null) {
                String hostIP = pod.getCurrentState().getHost();
                ctxt.setPublicIpAddress(hostIP);
                ctxt.setPrivateIpAddress(hostIP);
                dataHolder.addMemberContext(ctxt);
                // trigger topology
                TopologyBuilder.handleMemberSpawned(ctxt.getCartridgeType(), ctxt.getClusterId(), 
                        null, hostIP, hostIP, ctxt);
                
                RegistryManager.getInstance().persist(dataHolder);
                
            }
            
        } catch (Exception e) {
            // not logging exception intentionally
            LOG.error("Container Activation Watcher Failed.. Cause: "+e.getMessage());
            
            if (LOG.isDebugEnabled()) {
                LOG.debug(e);
            }
        }
        
    }
    
}

/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.load.balancer.mediators;

import org.apache.stratos.load.balancer.statistics.LoadBalancerStatsCollector;
import org.apache.stratos.load.balancer.util.Constants;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * This Synapse mediator counts the responses that are going across LB.
 * 
 */
public class ResponseInterceptor extends AbstractMediator implements ManagedLifecycle {

    public boolean mediate(MessageContext synCtx) {
        if(log.isDebugEnabled()) {
            log.debug("Mediation started " + ResponseInterceptor.class.getName());
        }
        String clusterId = (String) synCtx.getProperty(Constants.CLUSTER_ID);
        LoadBalancerStatsCollector.getInstance().decrementRequestInflightCount(clusterId);
        return true;
    }

    @Override
    public void destroy() {
        if(log.isDebugEnabled()) {
            log.debug("ResponseInterceptor mediator destroyed");
        }
    }

    @Override
    public void init(SynapseEnvironment arg0) {
        if(log.isDebugEnabled()) {
            log.debug("ResponseInterceptor mediator initiated");
        }
    }
}

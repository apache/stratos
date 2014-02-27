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

import org.apache.commons.lang3.StringUtils;
import org.apache.stratos.load.balancer.statistics.InFlightRequestDecrementCallable;
import org.apache.stratos.load.balancer.statistics.LoadBalancerStatisticsExecutor;
import org.apache.stratos.load.balancer.util.Constants;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediator;

import java.util.concurrent.FutureTask;

/**
 * This Synapse mediator counts the responses that are going across LB.
 */
public class ResponseInterceptor extends AbstractMediator implements ManagedLifecycle {

    @Override
    public void init(SynapseEnvironment arg) {
        if (log.isDebugEnabled()) {
            log.debug("ResponseInterceptor mediator initiated");
        }
    }

    public boolean mediate(MessageContext messageContext) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Response interceptor mediation started");
            }
            String clusterId = (String) messageContext.getProperty(Constants.CLUSTER_ID);
            if (StringUtils.isNotBlank(clusterId)) {
                FutureTask<Object> task = new FutureTask<Object>(new InFlightRequestDecrementCallable(clusterId));
                LoadBalancerStatisticsExecutor.getInstance().getService().submit(task);
            } else{
            	if (log.isDebugEnabled()) {
                    log.debug("Could not decrement in-flight request count : cluster id not found in message context");
                }
            }
            
        } catch (Exception e) {
            if(log.isErrorEnabled()) {
                log.error("Could not decrement in-flight request count", e);
            }
        }
        return true;
    }

    @Override
    public void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("Response interceptor mediator destroyed");
        }
    }
}

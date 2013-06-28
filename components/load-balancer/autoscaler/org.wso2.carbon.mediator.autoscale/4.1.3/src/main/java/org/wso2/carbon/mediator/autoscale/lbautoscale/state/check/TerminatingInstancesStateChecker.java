/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.mediator.autoscale.lbautoscale.state.check;

import org.apache.axis2.clustering.management.GroupManagementAgent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.mediator.autoscale.lbautoscale.context.LoadBalancerContext;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscaleConstants;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscaleUtil;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscalerTaskDSHolder;

public class TerminatingInstancesStateChecker implements Runnable {
    
    private static final Log log = LogFactory.getLog(TerminatingInstancesStateChecker.class);
    private LoadBalancerContext groupCtxt = null;
    private String domain;
    private String subDomain;
    
    public TerminatingInstancesStateChecker(LoadBalancerContext ctxt, String aDomain, String aSubDomain) {
        groupCtxt = ctxt;
        domain = aDomain;
        subDomain = aSubDomain;
    }

    @Override
    public void run() {

        if (groupCtxt != null) {

            groupCtxt.incrementTerminatingInstances(1);
            
            int totalWaitedTime = 0;
            int serverStartupDelay = AutoscalerTaskDSHolder
                .getInstance()
                .getWholeLoadBalancerConfig()
                .getLoadBalancerConfig()
                .getServerStartupDelay();
            log.info("Terminating Instances State Checker has started for: " +
                AutoscaleUtil.domainSubDomainString(domain, subDomain) + ". Check expiry time : " + serverStartupDelay);

            // for each sub domain, get the clustering group management agent
            GroupManagementAgent agent =
                AutoscalerTaskDSHolder.getInstance().getAgent()
                    .getGroupManagementAgent(domain,
                        subDomain);

            int startingRunningInstanceCount = agent.getMembers().size();

            // we give some time for the server to be terminated, we'll check time to time
            // whether the instance has actually left the cluster.
            while (agent.getMembers().size() == startingRunningInstanceCount &&
                totalWaitedTime < serverStartupDelay) {

                try {
                    Thread.sleep(AutoscaleConstants.INSTANCE_REMOVAL_CHECK_TIME);
                } catch (InterruptedException ignore) {
                }

                totalWaitedTime += AutoscaleConstants.INSTANCE_REMOVAL_CHECK_TIME;
            }

            log.debug(AutoscaleUtil.domainSubDomainString(domain, subDomain) + "- Waited for : " +
                totalWaitedTime +
                " (milliseconds) till terminating member left the cluster.");

            // we recalculate number of alive instances
            groupCtxt.decrementTerminatingInstancesIfNotZero(1);
        }

    }

}

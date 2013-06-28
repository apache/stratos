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
import org.wso2.carbon.mediator.autoscale.lbautoscale.clients.CloudControllerClient;
import org.wso2.carbon.mediator.autoscale.lbautoscale.context.LoadBalancerContext;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscaleConstants;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscaleUtil;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscalerTaskDSHolder;

public class PendingInstancesStateChecker implements Runnable {
    
    private static final Log log = LogFactory.getLog(PendingInstancesStateChecker.class);
    private LoadBalancerContext groupCtxt = null;
    private String domain;
    private String subDomain;
    private int expectedIncrementOfinstanceCount, originalRunningInstanceCount;
    private CloudControllerClient ccClient;
    
    public PendingInstancesStateChecker(LoadBalancerContext ctxt, String aDomain, String aSubDomain,
            int anexpectedInstanceCount, int currentCount, CloudControllerClient client) {
        groupCtxt = ctxt;
        domain = aDomain;
        subDomain = aSubDomain;
        expectedIncrementOfinstanceCount = anexpectedInstanceCount;
        originalRunningInstanceCount = currentCount;
        ccClient = client;
    }

    @Override
    public void run() {

        if (groupCtxt != null) {

            int totalWaitedTime = 0;
            int serverStartupDelay = AutoscalerTaskDSHolder
                .getInstance()
                .getWholeLoadBalancerConfig()
                .getLoadBalancerConfig()
                .getServerStartupDelay();

            log.debug("Pending Instances State Checker has started for: " +
                AutoscaleUtil.domainSubDomainString(domain, subDomain) + ". Check expiry time : " + serverStartupDelay);

            // for each sub domain, get the clustering group management agent
            GroupManagementAgent agent =
                AutoscalerTaskDSHolder.getInstance().getAgent()
                    .getGroupManagementAgent(domain,
                        subDomain);
            int startingRunningInstanceCount = agent.getMembers().size();

            // we give some time for the server to get joined, we'll check time to time
            // whether the instance has actually joined the ELB.
            while ((agent.getMembers().size() < (originalRunningInstanceCount + expectedIncrementOfinstanceCount)) &&
                totalWaitedTime < serverStartupDelay) {
                int upToDateRunningInstanceCount = agent.getMembers().size();

                log.debug(AutoscaleUtil.domainSubDomainString(domain, subDomain) +
                    " - Number of current running instances " +
                        upToDateRunningInstanceCount);

                if (upToDateRunningInstanceCount > startingRunningInstanceCount) {
                    int newlyJoinedInstanceCount = upToDateRunningInstanceCount - startingRunningInstanceCount;
                    // set new running instance count
                    groupCtxt.setRunningInstanceCount(upToDateRunningInstanceCount);
                    // decrement the pending instance count
                    groupCtxt.decrementPendingInstancesIfNotZero(newlyJoinedInstanceCount);
                    // update the starting running instance count
                    startingRunningInstanceCount = upToDateRunningInstanceCount;

                    log.debug(AutoscaleUtil.domainSubDomainString(domain, subDomain) +
                        " - Instances newly joined: " +
                            newlyJoinedInstanceCount);
                }

                try {
                    Thread.sleep(AutoscaleConstants.INSTANCE_REMOVAL_CHECK_TIME);
                } catch (InterruptedException ignore) {
                }

                totalWaitedTime += AutoscaleConstants.INSTANCE_REMOVAL_CHECK_TIME;
            }

            log.debug(AutoscaleUtil.domainSubDomainString(domain, subDomain) + "- Waited for : " +
                totalWaitedTime +
                " (milliseconds) till pending members get joined.");

            if (agent.getMembers().size() < (originalRunningInstanceCount + expectedIncrementOfinstanceCount)) {

                int instanceCountFailedToJoin =
                    originalRunningInstanceCount + expectedIncrementOfinstanceCount - agent.getMembers().size();
                log.debug(AutoscaleUtil.domainSubDomainString(domain, subDomain) +
                    "Instances that are failed to join: " +
                        instanceCountFailedToJoin);

                // to avoid an infinite loop
                int retries = instanceCountFailedToJoin + 2;

                while (instanceCountFailedToJoin > 0 && retries > 0) {
                    // instances spawned haven't joined ELB, so we assume that instance is
                    // corrupted.
                    // hence, we ask CC to terminate it.
                    try {
                        log.debug("Terminating lastly spwaned instance of " +
                            AutoscaleUtil.domainSubDomainString(domain, subDomain));
                        ccClient.terminateLastlySpawnedInstance(domain, subDomain);
                        instanceCountFailedToJoin--;
                        // decrement pending count
                        groupCtxt.decrementPendingInstancesIfNotZero(1);
                    } catch (Exception e) {
                        log
                            .error(
                                "Instance termination failed for " +
                                    AutoscaleUtil.domainSubDomainString(domain, subDomain),
                                e);
                    } finally {
                        retries--;
                    }
                }

                // decrement pending count
                groupCtxt.decrementPendingInstancesIfNotZero(instanceCountFailedToJoin);
            }

        }

    }

}

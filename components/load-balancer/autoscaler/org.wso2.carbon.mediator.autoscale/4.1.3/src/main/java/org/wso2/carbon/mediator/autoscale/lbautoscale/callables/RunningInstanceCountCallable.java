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
package org.wso2.carbon.mediator.autoscale.lbautoscale.callables;

import java.util.concurrent.Callable;

import org.apache.axis2.clustering.management.GroupManagementAgent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscalerTaskDSHolder;

/** Calculate running instances of each service domain, sub domain combination **/
public class RunningInstanceCountCallable implements Callable<Integer> {

    private static final Log log = LogFactory.getLog(RunningInstanceCountCallable.class);
    private String domain;
    private String subDomain;
    
    public RunningInstanceCountCallable(String domain, String subDomain){
        this.domain = domain;
        this.subDomain = subDomain;
    }
    
    @Override
    public Integer call() throws Exception {
        int runningInstances;
        // for each sub domain, get the clustering group management agent
        GroupManagementAgent agent =
            AutoscalerTaskDSHolder.getInstance().getAgent()
                .getGroupManagementAgent(this.domain,
                    this.subDomain);

        // if it isn't null
        if (agent != null) {
            // we calculate running instance count for this service domain
            runningInstances = agent.getMembers().size();
        } else {
            // if agent is null, we assume no service instances are running
            runningInstances = 0;
        }

        log.debug("Running instance count for domain: " +
            this.domain +
                ", sub domain: " +
                this.subDomain +
                " is " +
                runningInstances);
        
        return runningInstances;
    }

}

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.mediator.autoscale.lbautoscale.clients.CloudControllerClient;
import org.wso2.carbon.mediator.autoscale.lbautoscale.context.AppDomainContext;

/** Calculate instances of each service domain, sub domain combination **/
public class InstanceCountCallable implements Callable<Boolean> {

    private static final Log log = LogFactory.getLog(InstanceCountCallable.class);
    private String domain;
    private String subDomain;
    private CloudControllerClient client;
    private AppDomainContext appCtxt;
    private ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public InstanceCountCallable(String domain, String subDomain, CloudControllerClient client, AppDomainContext appCtxt){
        this.domain = domain;
        this.subDomain = subDomain;
        this.client = client;
        this.appCtxt = appCtxt;
    }
    
    @Override
    public Boolean call() throws Exception {
        log.debug("Computation of instance counts started for domain: " + this.domain +
            " and sub domain: " + this.subDomain);

        Callable<Integer> worker = new RunningInstanceCountCallable(this.domain, this.subDomain);
        Future<Integer> runningInstanceCount = executor.submit(worker);

//        worker = new PendingInstanceCountCallable(this.domain, this.subDomain, client);
//        Future<Integer> pendingInstanceCount = executor.submit(worker);

        int runningInstances = 0, pendingInstances = 0;
        if (appCtxt != null) {

            try {
                // get the values of Callables
                runningInstances = runningInstanceCount.get();
                pendingInstances = appCtxt.getPendingInstanceCount();
            } catch (Exception e) {
                // no need to throw
                log.error(e.getMessage(), e);
            }

            appCtxt.setRunningInstanceCount(runningInstances);
            appCtxt.setPendingInstanceCount(pendingInstances);
            
        }
        return true;
    }

}

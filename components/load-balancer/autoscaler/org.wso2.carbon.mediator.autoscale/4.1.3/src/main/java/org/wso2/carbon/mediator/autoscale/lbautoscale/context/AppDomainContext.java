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
package org.wso2.carbon.mediator.autoscale.lbautoscale.context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration;


/**
 * Contextual information related to autoscaling for a particular clustering domain
 */
public class AppDomainContext extends LoadBalancerContext{

    private static final long serialVersionUID = 6582721801663800609L;

    private static final Log log = LogFactory.getLog(AppDomainContext.class);

    /**
     * Request tokens of requests in flight
     * <p/>
     * Key - request token ID, Value - message received time
     */
    private Map<String, Long> requestTokens = new ConcurrentHashMap<String, Long>();
    private List<Integer> requestTokenListLengths;
    private LoadBalancerConfiguration.ServiceConfiguration serviceConfig;

    public AppDomainContext(LoadBalancerConfiguration.ServiceConfiguration serviceConfig) {
        this.serviceConfig = serviceConfig;
        requestTokenListLengths = new Vector<Integer>(serviceConfig.getRoundsToAverage());
    }

    public LoadBalancerConfiguration.ServiceConfiguration getServiceConfig() {
        return serviceConfig;
    }

    /**
     * If there is insufficient number of messages we cannot make a scaling decision.
     *
     * @return true - if a scaling decision can be made
     */
    public boolean canMakeScalingDecision() {
        return requestTokenListLengths.size() >= serviceConfig.getRoundsToAverage();
    }

    public void addRequestToken(String tokenId) {
        requestTokens.put(tokenId, System.currentTimeMillis());
        if (log.isDebugEnabled()) {
            log.debug("Request Tokens Added : "+requestTokens.size());
        }
    }

    public void removeRequestToken(String tokenId) {
        requestTokens.remove(tokenId);
    }

//    public int getRunningInstanceCount() {
//        return super.getRunningInstanceCount();
//    }

    /**
     * This will set the running instance count for this app domain
     * and also will return the difference of current running instance count and previous count.
     * @param runningInstanceCount current running instance count
     * @return difference of current running instance count and previous count.
     */
//    public int setRunningInstanceCount(int runningInstanceCount) {
//        int diff = 0;
//        
//        if(this.runningInstanceCount < runningInstanceCount){
//            diff = runningInstanceCount - this.runningInstanceCount;
//        }
//        
//        this.runningInstanceCount = runningInstanceCount;
//        
//        return diff;
//    }

    public void expireRequestTokens() {
        for (Map.Entry<String, Long> entry : requestTokens.entrySet()) {
            if (System.currentTimeMillis() - entry.getValue() >= serviceConfig.getMessageExpiryTime()) {
                requestTokens.remove(entry.getKey());
                if (log.isDebugEnabled()) {
                    log.debug("Request Tokens Expired : " + requestTokens.get(entry.getKey()));
                }
            }
        }
    }

    public void recordRequestTokenListLength() {
        if (requestTokenListLengths.size() >= serviceConfig.getRoundsToAverage()) {
            int count = requestTokenListLengths.remove(0);
            if (log.isDebugEnabled()) {
                log.debug("Request Tokens Removed : " + count);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Request Tokens Added : " + requestTokens.size());
        }
        requestTokenListLengths.add(requestTokens.size());
    }


//    public synchronized int getPendingInstances() {
//        return pendingInstances;
//    }

//    public synchronized void incrementPendingInstances() {
//        this.pendingInstances++;
//    }

//    public synchronized void decrementPendingInstancesIfNotZero(int diff) {
//        
//        while (diff > 0 && this.pendingInstances > 0 ){
//            this.pendingInstances--;
//            diff--;
//        }
//        
//    }
    
//    public synchronized int getInstances() {
//        return runningInstanceCount + pendingInstances;
//    }

    /**
     * Get the average requests in flight, averaged over the number of  of observations rounds
     *
     * @return number of average requests in flight. -1 if there no requests were received
     */
    public long getAverageRequestsInFlight() {
        long total = 0;
        for (Integer messageQueueLength : requestTokenListLengths) {
            total += messageQueueLength;
        }
        int size = requestTokenListLengths.size();
        if (size == 0) {
            return -1; // -1 means that no requests have been received
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Total Tokens : "+total+ " : Size: "+size);
        }
        return (long) total / size;
    }


//    public synchronized void resetRunningPendingInstances() {
//        pendingInstances = 0;
//        runningInstanceCount = 0;
//    }
}

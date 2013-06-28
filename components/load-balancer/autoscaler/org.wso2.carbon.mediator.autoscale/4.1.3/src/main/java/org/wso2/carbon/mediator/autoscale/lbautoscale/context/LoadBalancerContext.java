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

import java.io.Serializable;

/**
 * Contextual information related to autoscaling for a particular domain
 */
public class LoadBalancerContext implements Serializable{

    private static final long serialVersionUID = -2022110665957598060L;
    private int runningInstances;
    private int pendingInstances;
    private int terminatingInstances;
    
    public synchronized int getTerminatingInstanceCount() {
        return terminatingInstances;
    }
    
    public synchronized int getRunningInstanceCount() {
        return runningInstances;
    }
    
    public synchronized int getPendingInstanceCount() {
        return pendingInstances;
    }

    /**
     * This will set the running instance count for a domain
     * and also will return the difference of current running instance count and previous count.
     * @param runningInstanceCount current running instance count
     * @return difference of current running instance count and previous count.
     */
    public synchronized int setRunningInstanceCount(int count) {
        int diff = 0;

        if (this.runningInstances < count) {
            diff = count - this.runningInstances;
        }

        this.runningInstances = count;

        return diff;
    }

    public synchronized int getInstances() {
        return runningInstances + pendingInstances;
    }
    
    public synchronized void setPendingInstanceCount(int count) {
        
        this.pendingInstances = count;
    }
    
    public synchronized void setTerminatingInstanceCount(int count) {
        
        this.terminatingInstances = count;
    }

    public synchronized void incrementPendingInstances(int diff) {

        this.pendingInstances += diff;
    }
    
    public synchronized void incrementTerminatingInstances(int diff) {

        this.terminatingInstances += diff;
    }
    
    public synchronized void decrementPendingInstancesIfNotZero(int diff) {

        while (diff > 0 && this.pendingInstances > 0) {
            this.pendingInstances--;
            diff--;
        }

    }
    
    public synchronized void decrementTerminatingInstancesIfNotZero(int diff) {

        while (diff > 0 && this.terminatingInstances > 0) {
            this.terminatingInstances--;
            diff--;
        }

    }
}

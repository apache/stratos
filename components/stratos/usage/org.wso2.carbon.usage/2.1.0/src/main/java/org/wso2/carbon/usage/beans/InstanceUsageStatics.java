/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.usage.beans;

import java.util.Calendar;

public class InstanceUsageStatics {

    private Calendar startTime;
    private Calendar stopTime;
    private String instanceURL;
    private Integer instanceID;
    private long usedTimeInSeconds;
    private boolean running;

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public Calendar getStartTime() {
        return startTime;
    }

    public void setStartTime(Calendar startTime) {
        this.startTime = startTime;
    }

    public Calendar getStopTime() {
        return stopTime;
    }

    public void setStopTime(Calendar stopTime) {
        //Check weather stop time is Default value in database in that case
        //server should still running so set isRunning as true
        Calendar fixedDate = Calendar.getInstance();
        fixedDate.set(2001, 1, 1, 00, 00, 00);
        if (stopTime.compareTo(fixedDate) == 0) {
            this.running = true;
        }
        this.stopTime = stopTime;
    }

    public String getInstanceURL() {
        return instanceURL;
    }

    public void setInstanceURL(String instanceURL) {
        this.instanceURL = instanceURL;
    }

    public Integer getInstanceID() {
        return instanceID;
    }

    public void setInstanceID(Integer instanceID) {
        this.instanceID = instanceID;
    }

    public long getUsedTimeInSeconds() {
        long returnValue = (this.stopTime.getTimeInMillis() -
                this.startTime.getTimeInMillis()) / 1000;
        if (returnValue < 0) {
            running = true;
        }
        return usedTimeInSeconds;
    }
    public void setUsedTimeInSeconds(long value){
        this.usedTimeInSeconds=value;
    }

}
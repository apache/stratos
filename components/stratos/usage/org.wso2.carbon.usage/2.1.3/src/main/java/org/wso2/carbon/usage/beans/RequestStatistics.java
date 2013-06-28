/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.usage.beans;
public class RequestStatistics {
    private String key;
    private long requestCount;
    private long responseCount;
    private long faultCount;

    public RequestStatistics(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(long requestCount) {
        this.requestCount = requestCount;
    }

    public long getResponseCount() {
        return responseCount;
    }

    public void setResponseCount(long responseCount) {
        this.responseCount = responseCount;
    }

    public long getFaultCount() {
        return faultCount;
    }

    public void setFaultCount(long faultCount) {
        this.faultCount = faultCount;
    }

}

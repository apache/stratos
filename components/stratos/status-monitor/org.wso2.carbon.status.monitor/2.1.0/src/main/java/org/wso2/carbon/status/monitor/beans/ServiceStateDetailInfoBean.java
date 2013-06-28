/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.status.monitor.beans;

/**
 * Service State Details, includes the log information
 */
public class ServiceStateDetailInfoBean {
    private String service;
    private String serviceStateDetail;
    private long stateLoggedTime;
    private long detailLoggedTime;

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getServiceStateDetail() {
        return serviceStateDetail;
    }

    public void setServiceStateDetail(String serviceStateDetail) {
        this.serviceStateDetail = serviceStateDetail;
    }

    public long getStateLoggedTime() {
        return stateLoggedTime;
    }

    public void setStateLoggedTime(long stateLoggedTime) {
        this.stateLoggedTime = stateLoggedTime;
    }

    public long getDetailLoggedTime() {
        return detailLoggedTime;
    }

    public void setDetailLoggedTime(long detailLoggedTime) {
        this.detailLoggedTime = detailLoggedTime;
    }

}

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
package org.wso2.carbon.stratos.common.constants;

public class UsageConstants {
    final public static String ANY_DURATION = "anyDuration";

    public static final String SYSTEM_METERING_PATH =
            "/repository/components/org.wso2.carbon.system-metering";

    public static final String CUSTOM_METERING_PATH =
        "/repository/components/org.wso2.carbon.system-metering";

    // the measurements
    final public static String CAPACITY_USAGE = "capacity-usage";
    
    final public static String SERVICE_REQUEST_COUNT = "serviceRequestCount";
    final public static String SERVICE_RESPONSE_COUNT = "serviceResponseCount";
    final public static String SERVICE_FAULT_COUNT = "serviceFaultCount";
    
    public static final String BANDWIDTH_KEY_PATTERN = "%Bandwidth%";
    public static final String REGISTRY_BANDWIDTH = "RegistryBandwidth";
    public static final String SERVICE_BANDWIDTH = "ServiceBandwidth";
    public static final String WEBAPP_BANDWIDTH = "WebappBandwidth";
    public static final String IN_LABLE = "-In";
    public static final String OUT_LABLE = "-Out";
    public static final String REGISTRY_CONTENT_BANDWIDTH = "ContentBandwidth";
    
    final public static String REGISTRY_INCOMING_BW = REGISTRY_BANDWIDTH + IN_LABLE;
    final public static String REGISTRY_OUTGOING_BW = REGISTRY_BANDWIDTH + OUT_LABLE;
    final public static String REGISTRY_TOTAL_BW = "registry-total-bw-usage";
    final public static String NUMBER_OF_USERS = "number-of-users";

    final public static String SERVICE_INCOMING_BW = SERVICE_BANDWIDTH + IN_LABLE;
    final public static String SERVICE_OUTGOING_BW = SERVICE_BANDWIDTH + OUT_LABLE;
    final public static String SERVICE_TOTAL_BW = "serviceRequestTotalBw";
    
    final public static String WEBAPP_INCOMING_BW = WEBAPP_BANDWIDTH + IN_LABLE;
    final public static String WEBAPP_OUTGOING_BW = WEBAPP_BANDWIDTH + OUT_LABLE;

    final public static String API_CALL_COUNT = "apiCallCount";
}

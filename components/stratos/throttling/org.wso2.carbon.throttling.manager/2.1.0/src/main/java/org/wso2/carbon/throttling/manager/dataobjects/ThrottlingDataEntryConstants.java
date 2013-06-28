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
package org.wso2.carbon.throttling.manager.dataobjects;

public class ThrottlingDataEntryConstants {

    final public static String TENANT_INCOMING_BANDWIDTH = "tenantIncomingBandwidth";
    final public static String TENANT_OUTGOING_BANDWIDTH = "tenantOutgoingBandwidth";

    final public static String TENANT_CAPACITY = "tenantCapacity";
    final public static String TENANT_HISTORY_CAPACITY = "tenantHistoryCapacity";

    final public static String USERS_COUNT = "usersCount";

    // some custom objects
    final public static String CUSTOMER = "customer";
    final public static String PACKAGE = "package";
    final public static String USER_MANAGER = "userManager";
    final public static String REGISTRY_INCOMING_BANDWIDTH = "registryIncomingBandwidth";
    final public static String REGISTRY_OUTGOING_BANDWIDTH = "registryOutgoingBandwidth";
    final public static String SERVICE_INCOMING_BANDWIDTH = "serviceIncomingBandwidth";
    final public static String SERVICE_OUTGOING_BANDWIDTH = "serviceOutgoingBandwidth";
    final public static String WEBAPP_INCOMING_BANDWIDTH = "webappIncomingBandwidth";
    final public static String WEBAPP_OUTGOING_BANDWIDTH = "webappOutgoingBandwidth";
    final public static String SERVICE_REQUEST_COUNT = "serviceRequestCount";
    final public static String SERVICE_RESPONSE_COUNT = "serviceResponseCount";
    
}

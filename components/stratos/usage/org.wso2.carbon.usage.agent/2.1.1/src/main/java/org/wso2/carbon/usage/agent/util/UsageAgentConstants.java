/**
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.usage.agent.util;

/**
 * this class define constants for UsageAgent component
 */
public class UsageAgentConstants {
    public static final String ELEMENT_NAME_DATA = "Data";
    public static final String ELEMENT_NAME_KEY = "Key";
    public static final String ELEMENT_NAME_VALUE = "Value";

    public static final String STATISTICS_DATA_NS_URI = "http://wso2.org/ns/2009/09/bam/server/user-defined/data";
    public static final String STATISTICS_DATA_NS_PREFIX = "svrusrdata";

    // OM element names
    public static final String STATISTICS_DATA_ELEMENT_NAME_EVENT = "Event";
    public static final String STATISTICS_DATA_ELEMENT_NAME_SERVICE_STATISTICS_DATA = "ServerUserDefinedData";
    public static final String STATISTICS_DATA_ELEMENT_NAME_TENANT_ID = "TenantID";
    public static final String STATISTICS_DATA_ELEMENT_NAME_SERVER_NAME = "ServerName";

    public static final String BAM_SERVER_URL = "BamServerURL";
    public static final String BAM_SERVER_STAT_SERVICE = "BAMServerUserDefinedDataSubscriberService";
    public static final String BAM_SERVER_STAT_FILTER = "carbon/bam/data/publishers/bandwidth-stat";

    public static final String TOPIC_SEPARATOR = "/";
    public static final String EVENT_NAME = "UsageMeteringEvent";

    public static final String BANDWIDTH_USAGE_TOPIC = BAM_SERVER_STAT_FILTER;
//    public static final String BANDWIDTH_USAGE_TOPIC = BAM_SERVER_STAT_FILTER + TOPIC_SEPARATOR + EVENT_NAME;


    public static final String BANDWIDTH_USAGE_SERVICES_CONTEXT = "services";
    public static final String BANDWIDTH_USAGE_WEBAPPS_CONTEXT = "webapps";
    public static final String BANDWIDTH_CARBON = "carbon";
}

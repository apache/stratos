/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.status.monitor.agent.constants;

/**
 * Constants specific to the Status Monitor Agents
 */
public class StatusMonitorAgentConstants {

    /*CEP and MB Specific constants*/
    public static final String QPID_ICF = "org.apache.qpid.jndi.PropertiesFileInitialContextFactory";
    public static final String CF_NAME_PREFIX = "connectionfactory.";
    public static final String CF_NAME = "qpidConnectionfactory";

    public static final String CARBON_CLIENT_ID = "carbon";
    public static final String CARBON_VIRTUAL_HOST_NAME = "carbon";

    /*CEP Server client specific constants*/
    public static final String CEP_DEFAULT_PORT = "5674";
    public static final String QUEUE_NAME_CEP = "testQueueQACEP1";

    /*MB Server client specific constants*/
    public static final String QUEUE_NAME_MB = "testQueueQA6";

    /*Gadget Server specific constants*/
    public static final String GS_SAMPLE_TEST_RESOURCE_PATH =
            "/_system/config/repository/gadget-server/gadgets/AmazonSearchGadget/amazon-search.xml";
    public static final String GREG_SAMPLE_TEST_RESOURCE_PATH =
            "/_system/local/registry.txt";

    /*TrustStore and Identity constants*/
    public static final String TRUST_STORE = "javax.net.ssl.trustStore";
    public static final String TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword";
    public static final String TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";
    public static final String AUTHENTICATION_ADMIN_PATH = "/services/AuthenticationAdmin";

    /*Common constants*/
    public static final String TENANT_SERVICES = "/services/t/";
}

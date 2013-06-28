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
package org.wso2.carbon.status.monitor.core.constants;

/**
 * Constants common to the stratos status monitor
 */
public class StatusMonitorConstants {

    /*Status Monitor Configuration File*/
    public static final String STATUS_MONITOR_CONFIG = "status-monitor-config.xml";
    public static final String CONFIG_NS = "http://wso2.com/carbon/status/monitor/config";

    /*Authentication Configurations*/
    public static final String AUTH_CONFIG = "authConfig";
    public static final String JKS_LOCATION = "jksLocation";
    public static final String AUTHCONFIG_USER_NAME = "userName";
    public static final String AUTHCONFIG_PASSWORD = "password";
    public static final String AUTHCONFIG_TENANT = "tenantDomain";

    /*The tenant with the service samples*/
    public static final String PS_CONFIG = "platformSample";
    public static final String PSCONFIG_TENANT = "tenantDomain";

    /*Database configurations*/
    public static final String DB_CONFIG = "dbConfig";
    public static final String NS_PREFIX = "";
    public static final String DBCONFIG_VALIDATION_QUERY = "validationQuery";
    public static final String DBCONFIG_MAX_WAIT = "maxWait";
    public static final String DBCONFIG_MIN_IDLE = "minIdle";
    public static final String DBCONFIG_MAX_ACTIVE = "maxActive";
    public static final String DBCONFIG_DRIVER_NAME = "driverName";
    public static final String DBCONFIG_PASSWORD = "password";
    public static final String DBCONFIG_USER_NAME = "userName";
    public static final String DBCONFIG_URL = "url";

    public static final long HOUR_IN_MILLIS = 3600000;

    /*Services List*/
    public static final String MANAGER = "StratosLive Manager";
    public static final String ESB = "StratosLive Enterprise Service Bus";
    public static final String APPSERVER = "StratosLive Application Server";
    public static final String DATA = "StratosLive Data Services Server";
    public static final String GOVERNANCE = "StratosLive Governance Registry";
    public static final String IDENTITY = "StratosLive Identity Server";
    public static final String MONITOR = "StratosLive Business Activity Monitor";
    public static final String PROCESS = "StratosLive Business Process Server";
    public static final String RULE = "StratosLive Business Rules Server";
    public static final String MASHUP = "StratosLive Mashup Server";
    public static final String GADGET = "StratosLive Gadget Server";
    public static final String CEP = "StratosLive Complex Event Processing Server";
    public static final String MESSAGING = "StratosLive Message Broker";

    public static final String CARBON_OM_NAMESPACE = "http://service.carbon.wso2.org";

    /*Hosts list*/
    public static final String APPSERVER_HOST = "appserver.stratoslive.wso2.com";
    public static final String APPSERVER_HTTP = "http://appserver.stratoslive.wso2.com";
    public static final String MONITOR_HOST = "monitor.stratoslive.wso2.com";

    public static final String MESSAGING_HOST = "messaging.stratoslive.wso2.com";
    public static final String MESSAGING_DEFAULT_PORT = "5675";

    public static final String MASHUP_HOST = "mashup.stratoslive.wso2.com";
    public static final String MASHUP_HTTP = "http://mashup.stratoslive.wso2.com";

    public static final String PROCESS_HOST = "process.stratoslive.wso2.com";
    public static final String PROCESS_HTTP = "http://process.stratoslive.wso2.com";

    public static final String RULE_HOST = "rule.stratoslive.wso2.com";
    public static final String RULE_HTTP = "http://rule.stratoslive.wso2.com";

    public static final String CEP_HOST = "cep.stratoslive.wso2.com";

    public static final String ESB_HOST = "esb.stratoslive.wso2.com";
    public static final String ESB_HTTP = "http://esb.stratoslive.wso2.com";
    public static final String ESB_NHTTP_PORT = "8280";

    public static final String GOVERNANCE_HOST = "governance.stratoslive.wso2.com";
    public static final String GOVERNANCE_HTTP = "http://governance.stratoslive.wso2.com";

    public static final String GADGETS_HOST = "gadget.stratoslive.wso2.com";
    public static final String GADGETS_HTTP = "http://gadget.stratoslive.wso2.com";

    public static final String DATA_HOST = "data.stratoslive.wso2.com";
    public static final String DATA_HTTP = "http://data.stratoslive.wso2.com";

    public static final String IDENTITY_HOST = "identity.stratoslive.wso2.com";
    public static final String IDENTITY_HTTPS = "https://identity.stratoslive.wso2.com";

    public static final String MANAGER_HOST = "stratoslive.wso2.com";
    public static final String MANAGER_HTTPS = "https://stratoslive.wso2.com";

    public static final long SLEEP_TIME = 15 * 60 * 1000;


    /*SQL Statements - Status Monitor Core and Common*/
    public static final String GET_SERVICE_ID_SQL_WSL_NAME_LIKE_SQL =
            "SELECT WSL_ID FROM WSL_SERVICE WHERE WSL_NAME LIKE ";
    public static final String GET_SERVICE_STATE_ID_SQL =
            "SELECT WSL_ID FROM WSL_SERVICE_STATE WHERE WSL_SERVICE_ID = ";
    public static final String ID = "WSL_ID";
    public static final String STATE_ID = "WSL_STATE_ID";
    public static final String TIMESTAMP = "WSL_TIMESTAMP";
    public static final String NAME = "WSL_NAME";
    public static final String ORDER_BY_TIMESTAMP_SQL = " ORDER BY WSL_TIMESTAMP DESC LIMIT 1";
    public static final String ORDER_BY_TIMESTAMP_SQL_DESC_LIMIT_01_SQL =
            " ORDER BY WSL_TIMESTAMP DESC LIMIT 0,1";
    public static final String SELECT_ALL_FROM_WSL_SERVICE_STATE_SQL =
            "SELECT * FROM  WSL_SERVICE_STATE WHERE WSL_SERVICE_ID =";

    /*SQL Statements - Status Monitor Back End*/
    public static final String GET_STATE_NAME_SQL = "SELECT WSL_NAME FROM WSL_STATE";
    public static final String GET_SERVICE_NAME_SQL = "SELECT WSL_NAME FROM WSL_SERVICE";
    public static final String GET_ALL_STATE_DETAIL_SQL = "select s.WSL_NAME, ss.WSL_TIMESTAMP, " +
            "ssd.WSL_DETAIL, ssd.WSL_TIMESTAMP " +
            "from WSL_SERVICE as s, WSL_SERVICE_STATE as ss, WSL_SERVICE_STATE_DETAIL as " +
            "ssd where s.WSL_ID = ss.WSL_SERVICE_ID AND ss.WSL_ID = ssd.WSL_SERVICE_STATE_ID " +
            "AND (ss.WSL_STATE_ID=2 OR ss.WSL_STATE_ID=3) order by ss.WSL_TIMESTAMP DESC";
    public static final String SERVICE_WSL_NAME = "s.WSL_NAME";
    public static final String SERVICE_STATE_WSL_TIMESTAMP = "ss.WSL_TIMESTAMP";
    public static final String SERVICE_STATE_DETAIL_WSL_TIMESTAMP = "ssd.WSL_TIMESTAMP";
    public static final String SERVICE_STATE_DETAIL = "ssd.WSL_DETAIL";
    public static final String GET_SERVICE_STATE_SQL =
            "SELECT * FROM WSL_SERVICE_STATE WHERE WSL_SERVICE_ID=";

    /*SQL statements - Status Monitor Agent*/
    public static final String INSERT_STAT_SQL = "INSERT INTO WSL_SERVICE_HEARTBEAT VALUES (?,?,?,?)";
    public static final String INSERT_STATE_SQL = "INSERT INTO WSL_SERVICE_STATE VALUES (?,?,?,?)";
    public static final String UPDATE_STATE_SQL =
            "UPDATE WSL_SERVICE_STATE SET WSL_STATE_ID=? WHERE WSL_ID= ?";
    public static final String INSERT_STATE_DETAIL_SQL =
            "INSERT INTO WSL_SERVICE_STATE_DETAIL VALUES (?,?,?,?)";

}


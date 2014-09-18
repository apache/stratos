/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.haproxy.extension;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * HAProxy context to read and store system properties.
 */
public class HAProxyContext {
    private static final Log log = LogFactory.getLog(HAProxyContext.class);
    private static volatile HAProxyContext context;

    private String haProxyPrivateIp;
    private String executableFilePath;
    private String templatePath;
    private String templateName;
    private String scriptsPath;
    private String confFilePath;
    private String statsSocketFilePath;
    private boolean cepStatsPublisherEnabled;
    private String thriftReceiverIp;
    private String thriftReceiverPort;
    private String networkPartitionId;
    private String clusterId;

    private HAProxyContext() {
        this.haProxyPrivateIp = System.getProperty(Constants.HAPROXY_PRIVATE_IP);
        this.executableFilePath = System.getProperty(Constants.EXECUTABLE_FILE_PATH);
        this.templatePath = System.getProperty(Constants.TEMPLATES_PATH);
        this.templateName = System.getProperty(Constants.TEMPLATES_NAME);
        this.scriptsPath = System.getProperty(Constants.SCRIPTS_PATH);
        this.confFilePath = System.getProperty(Constants.CONF_FILE_PATH);
        this.statsSocketFilePath = System.getProperty(Constants.STATS_SOCKET_FILE_PATH);
        this.cepStatsPublisherEnabled = Boolean.getBoolean(Constants.CEP_STATS_PUBLISHER_ENABLED);
        this.thriftReceiverIp = System.getProperty(Constants.THRIFT_RECEIVER_IP);
        this.thriftReceiverPort = System.getProperty(Constants.THRIFT_RECEIVER_PORT);
        this.networkPartitionId = System.getProperty(Constants.NETWORK_PARTITION_ID);
        this.clusterId = System.getProperty(Constants.CLUSTER_ID);

        if (log.isDebugEnabled()) {
            log.debug(Constants.HAPROXY_PRIVATE_IP + " = " + haProxyPrivateIp);
            log.debug(Constants.EXECUTABLE_FILE_PATH + " = " + executableFilePath);
            log.debug(Constants.TEMPLATES_PATH + " = " + templatePath);
            log.debug(Constants.TEMPLATES_NAME + " = " + templateName);
            log.debug(Constants.SCRIPTS_PATH + " = " + scriptsPath);
            log.debug(Constants.CONF_FILE_PATH + " = " + confFilePath);
            log.debug(Constants.STATS_SOCKET_FILE_PATH + " = " + statsSocketFilePath);
            log.debug(Constants.CEP_STATS_PUBLISHER_ENABLED + " = " + cepStatsPublisherEnabled);
            log.debug(Constants.THRIFT_RECEIVER_IP + " = " + thriftReceiverIp);
            log.debug(Constants.THRIFT_RECEIVER_PORT + " = " + thriftReceiverPort);
            log.debug(Constants.NETWORK_PARTITION_ID + " = " + networkPartitionId);
            log.debug(Constants.CLUSTER_ID + " = " + clusterId);
        }
    }

    public static HAProxyContext getInstance() {
        if (context == null) {
            synchronized (HAProxyContext.class) {
                if (context == null) {
                    context = new HAProxyContext();
                }
            }
        }
        return context;
    }

    public void validate() {
        validateSystemProperty(Constants.HAPROXY_PRIVATE_IP);
        validateSystemProperty(Constants.EXECUTABLE_FILE_PATH);
        validateSystemProperty(Constants.TEMPLATES_PATH);
        validateSystemProperty(Constants.TEMPLATES_NAME);
        validateSystemProperty(Constants.SCRIPTS_PATH);
        validateSystemProperty(Constants.CONF_FILE_PATH);
        validateSystemProperty(Constants.STATS_SOCKET_FILE_PATH);
        validateSystemProperty(Constants.CEP_STATS_PUBLISHER_ENABLED);
        validateSystemProperty(Constants.CLUSTER_ID);

        if(cepStatsPublisherEnabled) {
            validateSystemProperty(Constants.THRIFT_RECEIVER_IP);
            validateSystemProperty(Constants.THRIFT_RECEIVER_PORT);
            validateSystemProperty(Constants.NETWORK_PARTITION_ID);
        }
    }

    private void validateSystemProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        if(StringUtils.isEmpty(value)) {
            throw new RuntimeException("System property was not found: " + propertyName);
        }
    }

    public String getHAProxyPrivateIp() {
        return haProxyPrivateIp;
    }

    public String getExecutableFilePath() {
        return executableFilePath;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getScriptsPath() {
        return scriptsPath;
    }

    public String getConfFilePath() {
        return confFilePath;
    }

    public String getStatsSocketFilePath() {
        return statsSocketFilePath;
    }

    public boolean isCEPStatsPublisherEnabled() {
        return cepStatsPublisherEnabled;
    }

    public String getNetworkPartitionId() { return networkPartitionId; };

    public String getClusterId() { return clusterId; };
}

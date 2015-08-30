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

package org.apache.stratos.lvs.extension;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * LVS context to read and store system properties.
 */
public class LVSContext {
    private static final Log log = LogFactory.getLog(LVSContext.class);
    private static volatile LVSContext context;

    private String lvsPrivateIp;
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
    private String serviceName;
	private String virtualIPsForServices;
	private String keepAlivedStartCommand;
	private String serverState;
	private String lvsScheduleAlgo;

    private LVSContext() {
        this.lvsPrivateIp = System.getProperty(Constants.LVS_PRIVATE_IP);
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
        this.serviceName = System.getProperty(Constants.SERVICE_NAME);
	    this.virtualIPsForServices=System.getProperty(Constants.VIRTUALIPS_FOR_SERVICES);
	    this.keepAlivedStartCommand=Constants.KEEPALIVED_START_COMMAND;
	    this.serverState=System.getProperty(Constants.SERVER_STATE);
	    this.lvsScheduleAlgo = System.getProperty(Constants.LVS_SCHEDULE_ALGO);

        if (log.isDebugEnabled()) {
            log.debug(Constants.LVS_PRIVATE_IP + " = " + lvsPrivateIp);
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
	        log.debug(Constants.VIRTUALIPS_FOR_SERVICES + " = " + virtualIPsForServices);
	        log.debug(Constants.LVS_SCHEDULE_ALGO + " = " + lvsScheduleAlgo);
        }
    }

    public static LVSContext getInstance() {
        if (context == null) {
            synchronized (LVSContext.class) {
                if (context == null) {
                    context = new LVSContext();
                }
            }
        }
        return context;
    }

    public void validate() {
        validateSystemProperty(Constants.LVS_PRIVATE_IP);
        validateSystemProperty(Constants.EXECUTABLE_FILE_PATH);
        validateSystemProperty(Constants.TEMPLATES_PATH);
        validateSystemProperty(Constants.TEMPLATES_NAME);
        validateSystemProperty(Constants.SCRIPTS_PATH);
        validateSystemProperty(Constants.CONF_FILE_PATH);
        validateSystemProperty(Constants.STATS_SOCKET_FILE_PATH);
        validateSystemProperty(Constants.CEP_STATS_PUBLISHER_ENABLED);
        validateSystemProperty(Constants.CLUSTER_ID);

        if (cepStatsPublisherEnabled) {
            validateSystemProperty(Constants.THRIFT_RECEIVER_IP);
            validateSystemProperty(Constants.THRIFT_RECEIVER_PORT);
            validateSystemProperty(Constants.NETWORK_PARTITION_ID);
        }
    }

    private void validateSystemProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        if (StringUtils.isEmpty(value)) {
            throw new RuntimeException("System property was not found: " + propertyName);
        }
    }

    public String getLvsPrivateIp() {
        return lvsPrivateIp;
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

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getServiceName() {
        return serviceName;
    }

	public String getVirtualIPsForServices() {
		return virtualIPsForServices;
	}

	public void setVirtualIPsForServices(String virtualIPsForServices) {
		this.virtualIPsForServices = virtualIPsForServices;
	}

	public String getKeepAlivedStartCommand() {
		return keepAlivedStartCommand;
	}

	public void setKeepAlivedStartCommand(String keepAlivedStartCommand) {
		this.keepAlivedStartCommand = keepAlivedStartCommand;
	}

	public String getServerState() {
		return serverState;
	}

	public void setServerState(String serverState) {
		this.serverState = serverState;
	}

	public String getLvsScheduleAlgo() {
		return lvsScheduleAlgo;
	}

	public void setLvsScheduleAlgo(String lvsScheduleAlgo) {
		this.lvsScheduleAlgo = lvsScheduleAlgo;
	}

	public boolean getIsKeepAlivedUsed() {
		return false;
	}
}

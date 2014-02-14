/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.cartridge.agent.data.publisher;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DataPublisherConfiguration {

    private static final Log log = LogFactory.getLog(DataPublisherConfiguration.class);

    private static final String ENABLE_DATA_PUBLISHER = "enable.data.publisher";
    private static final String MONITORING_SERVER_IP = "monitoring.server.ip";
    private static final String MONITORING_SERVER_PORT = "monitoring.server.port";
    private static final String MONITORING_SERVER_SECURE_PORT = "monitoring.server.secure.port";
    private static final String MONITORING_SERVER_ADMIN_USERNAME = "monitoring.server.admin.username";
    private static final String MONITORING_SERVER_ADMIN_PASSWORD = "monitoring.server.admin.password";

    private boolean enable;
    private String monitoringServerUrl;
    private String monitoringServerIp;
    private String monitoringServerPort;
    private String monitoringServerSecurePort;
    private String adminUsername;
    private String adminPassword;
    private static volatile DataPublisherConfiguration dataPublisherConfiguration;

    private DataPublisherConfiguration () {
        readConfig();
    }

    private void readConfig () {

        String enabled = System.getProperty(ENABLE_DATA_PUBLISHER);

        setEnable(Boolean.parseBoolean(enabled));
        if (!isEnabled()) {
            log.info("Data Publisher disabled");
            // disabled; no need to read other parameters
            return;
        }

        log.info("Data publishing is enabled");

        monitoringServerIp = System.getProperty(MONITORING_SERVER_IP);
        if(StringUtils.isBlank(monitoringServerIp)) {
            throw new RuntimeException("System property not found: " + MONITORING_SERVER_IP);
        }

        monitoringServerPort = System.getProperty(MONITORING_SERVER_PORT);
        if(StringUtils.isBlank(monitoringServerPort)) {
            throw new RuntimeException("System property not found: " + MONITORING_SERVER_PORT);
        }

        monitoringServerUrl = "tcp://" + monitoringServerIp + ":" + monitoringServerPort;

        monitoringServerSecurePort = System.getProperty(MONITORING_SERVER_SECURE_PORT);
        if(StringUtils.isBlank(monitoringServerSecurePort)) {
            throw new RuntimeException("System property not found: " + MONITORING_SERVER_SECURE_PORT);
        }

        adminUsername = System.getProperty(MONITORING_SERVER_ADMIN_USERNAME);
        if(StringUtils.isBlank(adminUsername)) {
            throw new RuntimeException("System property not found: " + MONITORING_SERVER_ADMIN_USERNAME);
        }

        adminPassword = System.getProperty(MONITORING_SERVER_ADMIN_PASSWORD);
        if(StringUtils.isBlank(adminPassword)) {
            throw new RuntimeException("System property not found: " + MONITORING_SERVER_ADMIN_PASSWORD);
        }

        log.info("Data Publisher configuration initialized");
    }

    public static DataPublisherConfiguration getInstance () {

        if (dataPublisherConfiguration == null) {
            synchronized (DataPublisherConfiguration.class) {
                if (dataPublisherConfiguration == null) {
                    dataPublisherConfiguration = new DataPublisherConfiguration();
                }
            }
        }

        return dataPublisherConfiguration;
    }

    public String getMonitoringServerUrl() {
        return monitoringServerUrl;
    }

    public void setMonitoringServerUrl(String monitoringServerUrl) {
        this.monitoringServerUrl = monitoringServerUrl;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public boolean isEnabled() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getMonitoringServerPort() {
        return monitoringServerPort;
    }

    public void setMonitoringServerPort(String monitoringServerPort) {
        this.monitoringServerPort = monitoringServerPort;
    }

    public String getMonitoringServerSecurePort() {
        return monitoringServerSecurePort;
    }

    public void setMonitoringServerSecurePort(String monitoringServerSecurePort) {
        this.monitoringServerSecurePort = monitoringServerSecurePort;
    }

    public String getMonitoringServerIp() {
        return monitoringServerIp;
    }

    public void setMonitoringServerIp(String monitoringServerIp) {
        this.monitoringServerIp = monitoringServerIp;
    }
}

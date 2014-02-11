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

public class DataPublisherConfiguration {

    private String monitoringServerUrl;
    private String adminUsername;
    private String adminPassword;
    private static volatile DataPublisherConfiguration dataPublisherConfiguration;

    private DataPublisherConfiguration () {
        readConfig();
    }

    private void readConfig () {
        //TODO: read and store
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
}

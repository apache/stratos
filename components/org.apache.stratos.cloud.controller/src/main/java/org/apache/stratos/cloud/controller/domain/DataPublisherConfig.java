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
package org.apache.stratos.cloud.controller.domain;

import java.io.Serializable;

import org.apache.stratos.cloud.controller.util.CloudControllerConstants;

/**
 *
 *
 */
public class DataPublisherConfig implements Serializable{

    private static final long serialVersionUID = -2097472019584151205L;
    private String bamUsername = CloudControllerConstants.DEFAULT_BAM_SERVER_USER_NAME;
    private String bamPassword = CloudControllerConstants.DEFAULT_BAM_SERVER_PASSWORD;
    private String dataPublisherCron = CloudControllerConstants.PUB_CRON_EXPRESSION;
    private String cassandraConnUrl = CloudControllerConstants.DEFAULT_CASSANDRA_URL;
    private String cassandraUser = CloudControllerConstants.DEFAULT_CASSANDRA_USER;
    private String cassandraPassword = CloudControllerConstants.DEFAULT_CASSANDRA_PASSWORD;
    
    public String getBamUsername() {
        return bamUsername;
    }

    public void setBamUsername(String bamUsername) {
        this.bamUsername = bamUsername;
    }

    public String getBamPassword() {
        return bamPassword;
    }

    public void setBamPassword(String bamPassword) {
        this.bamPassword = bamPassword;
    }

    public String getDataPublisherCron() {
        return dataPublisherCron;
    }

    public void setDataPublisherCron(String dataPublisherCron) {
        this.dataPublisherCron = dataPublisherCron;
    }
    public String getCassandraConnUrl() {
        return cassandraConnUrl;
    }

    public void setCassandraConnUrl(String cassandraHostAddr) {
        this.cassandraConnUrl = cassandraHostAddr;
    }

    public String getCassandraUser() {
        return cassandraUser;
    }

    public void setCassandraUser(String cassandraUser) {
        this.cassandraUser = cassandraUser;
    }

    public String getCassandraPassword() {
        return cassandraPassword;
    }

    public void setCassandraPassword(String cassandraPassword) {
        this.cassandraPassword = cassandraPassword;
    }
}

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

package org.apache.stratos.manager.payload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.utils.CartridgeConstants;

import java.io.Serializable;

/**
 * Contains basic payload data fields
 */
public class BasicPayloadData implements Serializable {

    private static Log log = LogFactory.getLog(BasicPayloadData.class);

    private String serviceName;
    private String clusterId;
    private String hostName;
    private int tenantId;
    private String tenantRange;
    private String subscriptionAlias;
    private String deployment;
    private String puppetIp;
    private String subscriptionKey;
    private String applicationPath;
    private String gitRepositoryUrl;
    private String portMappings;
    private String multitenant;

    protected StringBuilder payloadBuilder;

    public BasicPayloadData() {

    }

    public void populatePayload () {

        payloadBuilder = new StringBuilder();

        payloadBuilder.append("SERVICE_NAME=" + getServiceName());
        payloadBuilder.append(",");
        payloadBuilder.append("HOST_NAME=" + getHostName());
        payloadBuilder.append(",");
        payloadBuilder.append("MULTITENANT=" + getMultitenant());
        payloadBuilder.append(",");
        payloadBuilder.append("TENANT_ID=" + getTenantId());
        payloadBuilder.append(",");
        payloadBuilder.append("TENANT_RANGE=" + getTenantRange());
        payloadBuilder.append(",");
        payloadBuilder.append("CARTRIDGE_ALIAS=" + getSubscriptionAlias());
        payloadBuilder.append(",");
        payloadBuilder.append("CLUSTER_ID=" + getClusterId());
        payloadBuilder.append(",");
        payloadBuilder.append("CARTRIDGE_KEY=" + getSubscriptionKey());
        payloadBuilder.append(",");
        payloadBuilder.append("DEPLOYMENT=" + getDeployment());
        payloadBuilder.append(",");
        //payloadBuilder.append("APP_PATH=" + getApplicationPath());
        //payloadBuilder.append(",");
        payloadBuilder.append("GIT_REPO=" + getGitRepositoryUrl());
        payloadBuilder.append(",");
        payloadBuilder.append("PORTS=" + getPortMappings());

        //Payload Data exposed as system variables
        payloadBuilder.append(",");
        payloadBuilder.append("PUPPET_IP=" + System.getProperty(CartridgeConstants.PUPPET_IP));
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantRange() {
        return tenantRange;
    }

    public void setTenantRange(String tenantRange) {
        this.tenantRange = tenantRange;
    }

    public String getSubscriptionAlias() {
        return subscriptionAlias;
    }

    public void setSubscriptionAlias(String subscriptionAlias) {
        this.subscriptionAlias = subscriptionAlias;
    }

    public String getDeployment() {
        return deployment;
    }

    public void setDeployment(String deployment) {
        this.deployment = deployment;
    }

    public String getPuppetIp() {
        return puppetIp;
    }

    public void setPuppetIp(String puppetIp) {
        this.puppetIp = puppetIp;
    }

    public String getSubscriptionKey() {
        return subscriptionKey;
    }

    public void setSubscriptionKey(String subscriptionKey) {
        this.subscriptionKey = subscriptionKey;
    }

    public StringBuilder getPayloadData () {

        return payloadBuilder;
    }

    public String getApplicationPath() {
        return applicationPath;
    }

    public void setApplicationPath(String applicationPath) {
        this.applicationPath = applicationPath;
    }

    public String getGitRepositoryUrl() {
        return gitRepositoryUrl;
    }

    public void setGitRepositoryUrl(String gitRepositoryUrl) {
        this.gitRepositoryUrl = gitRepositoryUrl;
    }

    public String getPortMappings() {
        return portMappings;
    }

    public void setPortMappings(String portMappings) {
        this.portMappings = portMappings;
    }

    public String getMultitenant() {
        return multitenant;
    }

    public void setMultitenant(String multitenant) {
        this.multitenant = multitenant;
    }
}

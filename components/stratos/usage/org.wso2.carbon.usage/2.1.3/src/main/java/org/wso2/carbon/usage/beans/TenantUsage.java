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
package org.wso2.carbon.usage.beans;

public class TenantUsage {
    private int tenantId;
    private String domain;
    private int numberOfUsers;
    private UsageEntry[] usageEntries;
    private BandwidthStatistics[] registryBandwidthStatistics;
    private BandwidthStatistics[] serviceBandwidthStatistics;
    private BandwidthStatistics[] webappBandwidthStatistics;
    private CartridgeStatistics[] cartridgeStatistics;
    private RequestStatistics[] requestStatistics;
    private TenantDataCapacity registryCapacity;
    private RequestStatistics totalRequestStatistics;
    private BandwidthStatistics totalRegistryBandwidth;
    private BandwidthStatistics totalServiceBandwidth;
    private BandwidthStatistics totalWebappBandwidth;
    private CartridgeStatistics totalCartridgeHours;
    private APIManagerUsageStats[] apiManagerUsageStats;

    public APIManagerUsageStats[] getApiManagerUsageStats() {
        return apiManagerUsageStats;
    }

    public void setApiManagerUsageStats(APIManagerUsageStats[] apiManagerUsageStats) {
        this.apiManagerUsageStats = apiManagerUsageStats;
    }

    public BandwidthStatistics[] getRegistryBandwidthStatistics() {
        return registryBandwidthStatistics;
    }

    public void setRegistryBandwidthStatistics(BandwidthStatistics[] registryBandwidthStatistics) {
        this.registryBandwidthStatistics = registryBandwidthStatistics;
    }

    public BandwidthStatistics[] getServiceBandwidthStatistics() {
        return serviceBandwidthStatistics;
    }

    public void setServiceBandwidthStatistics(BandwidthStatistics[] serviceBandwidthStatistics) {
        this.serviceBandwidthStatistics = serviceBandwidthStatistics;
    }

    public BandwidthStatistics[] getWebappBandwidthStatistics() {
        return webappBandwidthStatistics;
    }

    public void setWebappBandwidthStatistics(BandwidthStatistics[] webappBandwidthStatistics) {
        this.webappBandwidthStatistics = webappBandwidthStatistics;
    }

    public RequestStatistics getTotalRequestStatistics() {
        return totalRequestStatistics;
    }

    public void setTotalRequestStatistics(RequestStatistics totalRequestStatistics) {
        this.totalRequestStatistics = totalRequestStatistics;
    }

    public BandwidthStatistics getTotalRegistryBandwidth() {
        return totalRegistryBandwidth;
    }

    public void setTotalRegistryBandwidth(BandwidthStatistics totalRegistryBandwidth) {
        this.totalRegistryBandwidth = totalRegistryBandwidth;
    }

    public BandwidthStatistics getTotalServiceBandwidth() {
        return totalServiceBandwidth;
    }

    public void setTotalServiceBandwidth(BandwidthStatistics totalServiceBandwidth) {
        this.totalServiceBandwidth = totalServiceBandwidth;
    }

    public BandwidthStatistics getTotalWebappBandwidth() {
        return totalWebappBandwidth;
    }

    public void setTotalWebappBandwidth(BandwidthStatistics totalWebappBandwidth) {
        this.totalWebappBandwidth = totalWebappBandwidth;
    }

    public CartridgeStatistics[] getCartridgeStatistics() {
        return cartridgeStatistics;
    }

    public void setCartridgeStatistics(CartridgeStatistics[] cartridgeStatistics) {
        this.cartridgeStatistics = cartridgeStatistics;
    }

    public CartridgeStatistics getTotalCartridgeHours() {
        return totalCartridgeHours;
    }

    public void setTotalCartridgeHours(CartridgeStatistics totalCartridgeHours) {
        this.totalCartridgeHours = totalCartridgeHours;
    }

    public TenantUsage() {
        // empty method required for used in web services
    }

    public TenantUsage(int tenantId, String domain) {
        this.tenantId = tenantId;
        this.domain = domain;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public UsageEntry[] getUsageEntries() {
        return usageEntries;
    }

    public void setUsageEntries(UsageEntry[] usageEntries) {
        this.usageEntries = usageEntries;
    }

    public RequestStatistics[] getRequestStatistics() {
        return requestStatistics;
    }

    public void setRequestStatistics(RequestStatistics[] requestStatistics) {
        this.requestStatistics = requestStatistics;
    }

    public TenantDataCapacity getRegistryCapacity() {
        return registryCapacity;
    }

    public void setRegistryCapacity(TenantDataCapacity registryCapacity) {
        this.registryCapacity = registryCapacity;
    }

    public int getNumberOfUsers() {
        return numberOfUsers;
    }

    public void setNumberOfUsers(int numberOfUsers) {
        this.numberOfUsers = numberOfUsers;
    }

    public long getRegistryContentCapacity(){
        long retValue = 0;
        if (registryCapacity != null) {
            retValue = registryCapacity.getRegistryContentCapacity();
        }
        return retValue;
    }

    public long getRegistryContentHistoryCapacity(){
        long retValue = 0;
        if (registryCapacity != null) {
            retValue = registryCapacity.getRegistryContentHistoryCapacity();
        }
        return retValue;
    }

    public long getTotalIncomingBandwidth(){
        long incomingBW =  0;
        if(totalRegistryBandwidth != null){
            incomingBW += totalRegistryBandwidth.getIncomingBandwidth();
        }
        if(totalServiceBandwidth != null){
            incomingBW += totalServiceBandwidth.getIncomingBandwidth();
        }
        if(totalWebappBandwidth != null){
            incomingBW += totalWebappBandwidth.getIncomingBandwidth();
        }
        return incomingBW;
    }

    public long getTotalOutgoingBandwidth(){
        long outgoingBW =  0;
        if(totalRegistryBandwidth != null){
            outgoingBW += totalRegistryBandwidth.getOutgoingBandwidth();
        }
        if(totalServiceBandwidth != null){
            outgoingBW += totalServiceBandwidth.getOutgoingBandwidth();
        }
        if(totalWebappBandwidth != null){
            outgoingBW += totalWebappBandwidth.getOutgoingBandwidth();
        }
        return outgoingBW;
    }
}

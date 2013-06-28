/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.stratos.cloud.controller.util;

import org.jclouds.compute.domain.NodeMetadata;

/**
 * This class holds the data to be published to BAM.
 */
public class CartridgeInstanceData {

    // Cartridge type
    private String type;
    
    private String nodeId;
    
    private String domain;
    
    private String subDomain;
    
    private String iaas;
    
    private String status;
    
    private String tenantRange;
    
    private String alias;
    
    private boolean isMultiTenant;
    
    private NodeMetadata metaData;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getSubDomain() {
        return subDomain;
    }

    public void setSubDomain(String subDomain) {
        this.subDomain = subDomain;
    }

    public String getIaas() {
        return iaas;
    }

    public void setIaas(String iaas) {
        this.iaas = iaas;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public NodeMetadata getMetaData() {
        return metaData;
    }

    public void setMetaData(NodeMetadata metaData) {
        this.metaData = metaData;
    }

    public String getTenantRange() {
        return tenantRange;
    }

    public void setTenantRange(String tenantRange) {
        this.tenantRange = tenantRange;
    }

    public boolean isMultiTenant() {
        return isMultiTenant;
    }

    public void setMultiTenant(boolean isMultiTenant) {
        this.isMultiTenant = isMultiTenant;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }
    
    
}

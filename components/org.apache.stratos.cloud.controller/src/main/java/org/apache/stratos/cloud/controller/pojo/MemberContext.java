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
package org.apache.stratos.cloud.controller.pojo;

/**
 * Holds runtime data of a Member.
 * @author nirmal
 *
 */
public class MemberContext {

    // id of the member
    private String memberId;
    // corresponding jclouds node id
    private String nodeId;
    // cluster id of this member
    private String clusterId;
    // partition id this member is in.
    private String partitionId;
    // cartridge type this member belongs to.
    private String cartridgeType;
    // allocated ip
    private String allocatedIpAddress;
    
    public MemberContext(String id, String nodeId, String clusterId, String partitionId, String cartridgeType, String ip) {
        this.memberId = id;
        this.nodeId = nodeId;
        this.clusterId = clusterId;
        this.partitionId = partitionId;
        this.cartridgeType = cartridgeType;
        this.allocatedIpAddress = ip;
        
    }
    
    public String getMemberId() {
        return memberId;
    }
    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
    public String getNodeId() {
        return nodeId;
    }
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    public String getClusterId() {
        return clusterId;
    }
    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }
    public String getPartitionId() {
        return partitionId;
    }
    public void setPartitionId(String partitionId) {
        this.partitionId = partitionId;
    }
    public String getCartridgeType() {
        return cartridgeType;
    }
    public void setCartridgeType(String cartridgeType) {
        this.cartridgeType = cartridgeType;
    }
    public String getAllocatedIpAddress() {
        return allocatedIpAddress;
    }
    public void setAllocatedIpAddress(String allocatedIpAddress) {
        this.allocatedIpAddress = allocatedIpAddress;
    }
    
    
}

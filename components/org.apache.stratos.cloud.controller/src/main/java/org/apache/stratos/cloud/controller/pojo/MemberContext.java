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

import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;

import java.io.Serializable;

/**
 * Holds information about a Member.
 *
 */
public class MemberContext implements Serializable {

    private static final long serialVersionUID = -388327475844701869L;
    // id of the member
    private String memberId;
    // corresponding jclouds node id
    private String nodeId;
    // instance id - derived from nodeId
    private String instanceId;
    // cluster id of this member
    private String clusterId;
    // partition this member is in.
    private Partition partition;
    // cartridge type this member belongs to.
    private String cartridgeType;
    // private ip
    private String privateIpAddress;
    // public ip
    private String publicIpAddress;
    // manually allocated ip
    private String allocatedIpAddress;
    // member initiated time
    private long initTime;
    // lb cluster id of this member
    private String lbClusterId;
    //network partition id
    private String networkPartitionId;

    private Properties properties;
    
    public MemberContext(String id, String clusterId, Partition partition) {
        this.memberId = id;
        this.clusterId = clusterId;
        this.setPartition(partition);
        init();
    }
    
    public MemberContext() {
        init();
    }
    
    private void init() {
        this.properties = new Properties();
        this.properties.setProperties(new Property[0]);
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
    public String getCartridgeType() {
        return cartridgeType;
    }
    public void setCartridgeType(String cartridgeType) {
        this.cartridgeType = cartridgeType;
    }
    public Partition getPartition() {
        return partition;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(String privateIpAddress) {
        this.privateIpAddress = privateIpAddress;
    }

    public String getAllocatedIpAddress() {
        return allocatedIpAddress;
    }

    public void setAllocatedIpAddress(String allocatedIpAddress) {
        this.allocatedIpAddress = allocatedIpAddress;
    }

    public long getInitTime() {
        return initTime;
    }

    public void setInitTime(long initTime) {
        this.initTime = initTime;
    }

    public String getLbClusterId() {
        return lbClusterId;
    }

    public void setLbClusterId(String lbClusterId) {
        this.lbClusterId = lbClusterId;
    }


    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public void setNetworkPartitionId(String networkPartitionId) {
        this.networkPartitionId = networkPartitionId;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((clusterId == null) ? 0 : clusterId.hashCode());
		result = prime * result
				+ ((memberId == null) ? 0 : memberId.hashCode());
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MemberContext other = (MemberContext) obj;
		if (clusterId == null) {
			if (other.clusterId != null)
				return false;
		} else if (!clusterId.equals(other.clusterId))
			return false;
		if (memberId == null) {
			if (other.memberId != null)
				return false;
		} else if (!memberId.equals(other.memberId))
			return false;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		return true;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "MemberContext [memberId=" + memberId + ", nodeId=" + nodeId + ", instanceId="
                + instanceId + ", clusterId=" + clusterId + ", partition=" + partition
                + ", cartridgeType=" + cartridgeType + ", privateIpAddress=" + privateIpAddress
                + ", publicIpAddress=" + publicIpAddress + ", allocatedIpAddress="
                + allocatedIpAddress + ", initTime=" + initTime + ", lbClusterId=" + lbClusterId
                + ", networkPartitionId=" + networkPartitionId + ", properties=" + properties + "]";
    }
    
}

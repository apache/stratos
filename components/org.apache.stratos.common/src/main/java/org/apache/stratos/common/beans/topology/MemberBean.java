/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.common.beans.topology;

import org.apache.stratos.common.beans.PropertyBean;
import org.apache.stratos.common.beans.cartridge.PortMappingBean;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class MemberBean {

    private String serviceName;
    private String clusterId;
    private String clusterInstanceId;
    private String networkPartitionId;
    private String partitionId;
    private String memberId;
    private String status;
    private String defaultPrivateIP;
    private List<String> memberPrivateIPs;
    private String lbClusterId;
    private String defaultPublicIP;
    private List<String> memberPublicIPs;
    private List<PortMappingBean> ports;
    private List<PropertyBean> property;

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

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public void setNetworkPartitionId(String networkPartitionId) {
        this.networkPartitionId = networkPartitionId;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(String partitionId) {
        this.partitionId = partitionId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public List<String> getMemberPrivateIPs() {
        return memberPrivateIPs;
    }

    public void setMemberPrivateIPs(List<String> memberPrivateIPs) {
        this.memberPrivateIPs = memberPrivateIPs;
    }

    public List<String> getMemberPublicIPs() {
        return memberPublicIPs;
    }

    public void setMemberPublicIPs(List<String> memberPublicIPs) {
        this.memberPublicIPs = memberPublicIPs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDefaultPrivateIP() {
        return defaultPrivateIP;
    }

    public void setDefaultPrivateIP(String defaultPrivateIP) {
        this.defaultPrivateIP = defaultPrivateIP;
    }

    public String getLbClusterId() {
        return lbClusterId;
    }

    public void setLbClusterId(String lbClusterId) {
        this.lbClusterId = lbClusterId;
    }

    public String getDefaultPublicIP() {
        return defaultPublicIP;
    }

    public void setDefaultPublicIP(String defaultPublicIP) {
        this.defaultPublicIP = defaultPublicIP;
    }

    public List<PropertyBean> getProperty() {
        return property;
    }

    public void setProperty(List<PropertyBean> property) {
        this.property = property;
    }

    public String getClusterInstanceId() {
        return clusterInstanceId;
    }

    public void setClusterInstanceId(String clusterInstanceId) {
        this.clusterInstanceId = clusterInstanceId;
    }

    public List<PortMappingBean> getPorts() {
        return ports;
    }

    public void setPorts(List<PortMappingBean> ports) {
        this.ports = ports;
    }

    @Override
    public String toString() {
        return "Member [serviceName=" + getServiceName()
                + ", clusterId=" + getClusterId()
                + ", memberId=" + getMemberId()
                + ", clusterInstanceId=" + getClusterInstanceId()
                + ", networkPartitionId=" + getNetworkPartitionId()
                + ", partitionId=" + getPartitionId()
                + ", status=" + getStatus()
                + ", defaultPrivateIP=" + getDefaultPrivateIP()
                + ", memberPrivateIPs=" + memberPrivateIPs.toString()
                + ", defaultPublicIP=" + getDefaultPublicIP()
                + ", memberPublicIPs=" + memberPublicIPs.toString()
                + ", lbClusterId=" + getLbClusterId()
                + ", ports=" + getPorts()
                + ", property=" + getProperty() + "]";
    }
}

/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.cli.beans.grouping.applications;

public class Member
{
    private String networkPartitionId;

    private String memberPublicIp;

    private String status;

    private Property[] property;

    private String memberId;

    private String partitionId;

    private String clusterId;

    private String memberIp;

    private String serviceName;

    public String getNetworkPartitionId ()
    {
        return networkPartitionId;
    }

    public void setNetworkPartitionId (String networkPartitionId)
    {
        this.networkPartitionId = networkPartitionId;
    }

    public String getMemberPublicIp ()
    {
        return memberPublicIp;
    }

    public void setMemberPublicIp (String memberPublicIp)
    {
        this.memberPublicIp = memberPublicIp;
    }

    public String getStatus ()
    {
        return status;
    }

    public void setStatus (String status)
    {
        this.status = status;
    }

    public Property[] getProperty ()
    {
        return property;
    }

    public void setProperty (Property[] property)
    {
        this.property = property;
    }

    public String getMemberId ()
    {
        return memberId;
    }

    public void setMemberId (String memberId)
    {
        this.memberId = memberId;
    }

    public String getPartitionId ()
    {
        return partitionId;
    }

    public void setPartitionId (String partitionId)
    {
        this.partitionId = partitionId;
    }

    public String getClusterId ()
    {
        return clusterId;
    }

    public void setClusterId (String clusterId)
    {
        this.clusterId = clusterId;
    }

    public String getMemberIp ()
    {
        return memberIp;
    }

    public void setMemberIp (String memberIp)
    {
        this.memberIp = memberIp;
    }

    public String getServiceName ()
    {
        return serviceName;
    }

    public void setServiceName (String serviceName)
    {
        this.serviceName = serviceName;
    }
}

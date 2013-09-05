package org.apache.stratos.adc.topology.mgt.group.mgt;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


import org.apache.axis2.clustering.Member;
import org.apache.axis2.clustering.management.DefaultGroupManagementAgent;

/**
 * This GroupManagementAgent can handle group membership based on cluster sub-domains
 */
public class SubDomainAwareGroupManagementAgent extends DefaultGroupManagementAgent {

    private String subDomain;

    public SubDomainAwareGroupManagementAgent(String subDomain) {
        this.subDomain = subDomain;
    }

    @Override
    public void applicationMemberAdded(Member member) {
        String subDomain = member.getProperties().getProperty("subDomain");
        if (subDomain == null || subDomain.equals(this.subDomain)) {
            super.applicationMemberAdded(member);
        }
    }

    @Override
    public void applicationMemberRemoved(Member member) {
        String subDomain = member.getProperties().getProperty("subDomain");
        if (subDomain == null || subDomain.equals(this.subDomain)) {
            super.applicationMemberRemoved(member);
        }
    }
}

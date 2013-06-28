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
package org.wso2.carbon.lb.common.group.mgt;

import org.apache.axis2.clustering.Member;
import org.apache.axis2.clustering.management.DefaultGroupManagementAgent;
import org.apache.synapse.endpoints.dispatch.SALSessions;
import org.wso2.carbon.lb.common.conf.util.Constants;

/**
 * This GroupManagementAgent can handle group membership based on cluster sub-domains.
 */
public class SubDomainAwareGroupManagementAgent extends DefaultGroupManagementAgent {

    private String subDomain;

    public SubDomainAwareGroupManagementAgent(String subDomain) {
        this.subDomain = subDomain;
    }

    @Override
    public void applicationMemberAdded(Member member) {
        String subDomain = member.getProperties().getProperty("subDomain");
        if ((subDomain == null && Constants.DEFAULT_SUB_DOMAIN.equals(this.subDomain)) ||
            subDomain.equals(this.subDomain)) {
            super.applicationMemberAdded(member);
        }
    }

    @Override
    public void applicationMemberRemoved(Member member) {
        
        // remove the sessions bound with this member
        SALSessions.getInstance().removeSessionsOfMember(member);
        
        String subDomain = member.getProperties().getProperty("subDomain");
        if ((subDomain == null && Constants.DEFAULT_SUB_DOMAIN.equals(this.subDomain)) ||
            subDomain.equals(this.subDomain)) {
            super.applicationMemberRemoved(member);
        }
    }
}

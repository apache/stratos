package org.wso2.carbon.adc.topology.mgt.group.mgt;

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
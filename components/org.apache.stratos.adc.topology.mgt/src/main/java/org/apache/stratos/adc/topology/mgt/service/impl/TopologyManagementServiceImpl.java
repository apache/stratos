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

package org.apache.stratos.adc.topology.mgt.service.impl;

import org.apache.stratos.adc.topology.mgt.service.TopologyManagementService;
import org.apache.stratos.adc.topology.mgt.serviceobjects.DomainContext;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.ArrayList;
import java.util.List;

public class TopologyManagementServiceImpl implements TopologyManagementService {

    public String[] getDomains(String cartridgeType, int tenantId) {
        List<String> domains = new ArrayList<String>();
        try {
            TopologyManager.acquireReadLock();
            for (Service service : TopologyManager.getTopology().getServices()) {
                for (Cluster cluster : service.getClusters()) {
                    domains.add(cluster.getClusterId());
                }
            }
        } finally {
            TopologyManager.releaseReadLock();
        }
        return domains.toArray(new String[domains.size()]);
    }

    public String[] getSubDomains(String cartridgeType, int tenantId) {
        List<String> subDomains = new ArrayList<String>();
        // TODO Should be able to remove
        return subDomains.toArray(new String[subDomains.size()]);
    }

    public String[] getActiveIPs(String cartridgeType, String domain, String subDomain) {
        List<String> publicIps = new ArrayList<String>();

        if (domain == null || subDomain == null) {
            return new String[0];
        }

        try {
            TopologyManager.acquireReadLock();
            for (Service service : TopologyManager.getTopology().getServices()) {
                for (Cluster cluster : service.getClusters()) {
                    if (domain.equals(cluster.getClusterId())) {
                        for (Member member : cluster.getMembers()) {
                            if (member.isActive()) {
                                publicIps.add(member.getMemberIp());
                            }
                        }
                    }
                }
            }
        } finally {
            TopologyManager.releaseReadLock();
        }
        return publicIps.toArray(new String[publicIps.size()]);
    }

    /* (non-Javadoc)
     * @see org.wso2.carbon.stratos.topology.mgt.service.TopologyManagementService#getDomainsAndSubdomains(java.lang.String, int)
     */
    public DomainContext[] getDomainsAndSubdomains(String cartridgeType, int tenantId) {
        List<DomainContext> domainContexts = new ArrayList<DomainContext>();
        // TODO Should be able to remove
        return domainContexts.toArray(new DomainContext[domainContexts.size()]);
    }
}

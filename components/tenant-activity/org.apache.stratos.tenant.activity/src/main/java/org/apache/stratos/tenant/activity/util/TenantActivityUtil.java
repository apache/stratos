/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.stratos.tenant.activity.util;

import org.apache.axis2.AxisFault;
import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.stratos.tenant.activity.commands.GetActiveTenantsInMemberRequest;
import org.apache.stratos.tenant.activity.commands.GetActiveTenantsInMemberResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.tenant.activity.beans.TenantDataBean;

import java.util.ArrayList;
import java.util.List;


public class TenantActivityUtil {
    private static final Log log = LogFactory.getLog(TenantActivityUtil.class);

    public static List<TenantDataBean> getActiveTenantsInCluster() throws AxisFault {
        List<TenantDataBean> tenants = new ArrayList<TenantDataBean>();
        try {
            ClusteringAgent agent = getClusteringAgent();
            List<ClusteringCommand> list = agent.sendMessage(new GetActiveTenantsInMemberRequest(), true);
            if (log.isDebugEnabled()) {
                log.debug("sent cluster command to to get Active tenants on cluster");
            }
            for (ClusteringCommand command : list) {
                if (command instanceof GetActiveTenantsInMemberResponse) {
                    GetActiveTenantsInMemberResponse response = (GetActiveTenantsInMemberResponse) command;
                    for (TenantDataBean tenant : response.getTenants()) {
                        tenants.add(tenant);
                    }
                }
            }

        } catch (AxisFault f) {
            String msg = "Error in getting active tenant by cluster commands";
            log.error(msg, f);
            throw new AxisFault(msg);
        }
        return tenants;
    }


    private static ClusteringAgent getClusteringAgent() throws AxisFault {

        AxisConfiguration axisConfig =
                Util.getConfigurationContextService().getServerConfigContext().getAxisConfiguration();
        return axisConfig.getClusteringAgent();
    }

    public static int indexOfTenantInList(List<TenantDataBean> list, TenantDataBean tenant) {
        for (int i = 0; i < list.size(); i++) {
            if (tenant.getDomain().equalsIgnoreCase(list.get(i).getDomain())) {
                return i;
            }
        }
        return -1;
    }
}
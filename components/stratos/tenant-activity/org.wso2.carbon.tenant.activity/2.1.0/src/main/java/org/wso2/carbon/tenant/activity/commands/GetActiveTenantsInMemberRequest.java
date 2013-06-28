/*
 *  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.tenant.activity.commands;

import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.tenant.activity.beans.TenantDataBean;
import org.wso2.carbon.user.api.Tenant;

import java.util.ArrayList;
import java.util.List;

/**
 * This Cluster message is used to get active tenants in each node then
 * send response as other cluster command which holds active tenants
 */
public class GetActiveTenantsInMemberRequest extends ClusteringMessage {
    private static final Log log = LogFactory.getLog(GetActiveTenantsInMemberRequest.class);
    private List<TenantDataBean> tenants = new ArrayList<TenantDataBean>();

    public void execute(ConfigurationContext configurationContext) throws ClusteringFault {
        try {
            for (Tenant tenant : TenantAxisUtils.getActiveTenants(configurationContext)) {
                TenantDataBean tb = new TenantDataBean();
                tb.setDomain(tenant.getDomain());
                tenants.add(tb);
            }
        } catch (Exception e) {
            String msg = "Cannot get Active tenants";
            log.error(msg, e);
            throw new ClusteringFault(msg, e);
        }
    }

    public ClusteringCommand getResponse() {
        return new GetActiveTenantsInMemberResponse(tenants);
    }
}

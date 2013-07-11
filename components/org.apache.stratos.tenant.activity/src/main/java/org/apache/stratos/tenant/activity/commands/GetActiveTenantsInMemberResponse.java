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
package org.apache.stratos.tenant.activity.commands;

import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.stratos.tenant.activity.beans.TenantDataBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This clustering command is used send active tenants list if we send clustering message
 * GetActiveTenantsInMemberRequest then as a response we can get GetActiveTenantsInMemberResponse
 * which holds active tenants list
 */
public class GetActiveTenantsInMemberResponse extends ClusteringCommand {

    private List<TenantDataBean> tenants = new ArrayList<TenantDataBean>();

    public GetActiveTenantsInMemberResponse(List<TenantDataBean> tenantList) {
        tenants = tenantList;
    }

    public void addTenant(TenantDataBean tenant) {
        tenants.add(tenant);
    }

    public List<TenantDataBean> getTenants() {
        return Collections.unmodifiableList(tenants);
    }

    @Override
    public void execute(ConfigurationContext configurationContext) throws ClusteringFault {
    }
}

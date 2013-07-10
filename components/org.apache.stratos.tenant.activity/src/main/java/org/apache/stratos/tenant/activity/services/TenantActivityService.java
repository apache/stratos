/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.stratos.tenant.activity.services;

import org.apache.stratos.tenant.activity.util.Util;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.core.AbstractAdmin;
import org.apache.stratos.tenant.activity.beans.PaginatedTenantDataBean;
import org.apache.stratos.tenant.activity.beans.TenantDataBean;
import org.apache.stratos.tenant.activity.util.TenantActivityUtil;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.utils.DataPaginator;

import java.util.*;

/**
 * This service use to get active tenants related data.If its single node
 */
public class TenantActivityService extends AbstractAdmin {
    List<String> list = new ArrayList<String>();

    public int getActiveTenantCount() throws Exception {
        return getAllActiveTenantList().size();
    }

    /**
     *
     * @return Active tenants list on service cluster
     * @throws Exception  when error in retrieving active tenants list
     */
    private List<TenantDataBean> getAllActiveTenantList() throws Exception {
       // ClusterMgtUtil cm = new ClusterMgtUtil();
        List<TenantDataBean> list = new ArrayList<TenantDataBean>();
        //This will add current node active tenants list to tenant list
        for (Tenant tenant : TenantAxisUtils.getActiveTenants(Util.getConfigurationContextService().getServerConfigContext())) {
            TenantDataBean tb = new TenantDataBean();
            tb.setDomain(tenant.getDomain());
            list.add(tb);
        }
        //if there are multiple nodes in clusters get active tenants from there as well
        for (TenantDataBean tenantDataBean : TenantActivityUtil.getActiveTenantsInCluster()) {
            if (TenantActivityUtil.indexOfTenantInList(list, tenantDataBean) < 0) {
                list.add(tenantDataBean);
            }

        }
        return list;
    }

    public PaginatedTenantDataBean retrievePaginatedActiveTenants(int pageNumber) throws Exception {
        List<TenantDataBean> tenantList = getAllActiveTenantList();
        // Pagination
        PaginatedTenantDataBean paginatedTenantInfoBean = new PaginatedTenantDataBean();
        DataPaginator.doPaging(pageNumber, tenantList, paginatedTenantInfoBean);
        return paginatedTenantInfoBean;
    }

    public boolean isActiveTenantOnService(String domainName) throws Exception {
        boolean state = false;
        for (TenantDataBean tenant : getAllActiveTenantList()) {
            if (tenant.getDomain().equalsIgnoreCase(domainName)) {
                return true;
            }
        }
        return state;
    }
}
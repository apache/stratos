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
package org.wso2.carbon.usage.services;

import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.usage.beans.InstanceUsageStatics;
import org.wso2.carbon.usage.beans.PaginatedInstanceUsage;
import org.wso2.carbon.usage.beans.PaginatedTenantUsageInfo;
import org.wso2.carbon.usage.beans.TenantUsage;
import org.wso2.carbon.usage.util.Util;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;

import java.text.ParseException;
import java.util.*;

public class UsageService extends AbstractAdmin {
    /**
     * Return the usage of the current logged in tenant
     *
     * @param yearMonth year month
     * @return the current usage of the tenant
     * @throws Exception
     */
    public TenantUsage retrieveCurrentTenantUsage(String yearMonth) throws Exception {
        UserRegistry registry = (UserRegistry) getConfigUserRegistry();
        int tenantId = registry.getTenantId();
        return Util.getTenantUsageRetriever().getTenantUsage(tenantId, yearMonth);
    }

    /**
     * Return the all the tenant usages, requires super admin permissions
     *
     * @param yearMonth
     * @return
     * @throws Exception
     */
    public TenantUsage[] retrieveTenantUsages(String yearMonth) throws Exception {
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        Tenant[] tenants = (Tenant[]) tenantManager.getAllTenants();
        List<TenantUsage> tenantUsages = new ArrayList<TenantUsage>();
        for (Tenant tenant : tenants) {
            if (tenant.isActive()) {
                TenantUsage tenantUsage = Util.getTenantUsageRetriever().getTenantUsage(
                        tenant.getId(), yearMonth);
                tenantUsages.add(tenantUsage);
            }
        }
        return tenantUsages.toArray(new TenantUsage[tenantUsages.size()]);
    }

    /**
     * Return the all the tenant usages paginated, requires super admin permissions
     *
     * @param yearMonth
     * @param pageNumber
     * @param entriesPerPage
     * @return PaginatedTenantUsageInfo
     * @throws Exception
     */
    public PaginatedTenantUsageInfo retrievePaginatedTenantUsages(String yearMonth, int pageNumber,
                                                                  int entriesPerPage) throws Exception {
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        Tenant[] tenants = (Tenant[]) tenantManager.getAllTenants();
        List<TenantUsage> tenantUsages = new ArrayList<TenantUsage>();

        int i = 0;
        int numberOfPages = 0;
        for (Tenant tenant : tenants) {
            if (tenant.isActive()) {
                if (i % entriesPerPage == 0) {
                    numberOfPages++;
                }
                if (numberOfPages == pageNumber) {
                    TenantUsage tenantUsage = Util.getTenantUsageRetriever().getTenantUsage(
                            tenant.getId(), yearMonth);
                    tenantUsages.add(tenantUsage);
                }
                i++;
            }
        }
        PaginatedTenantUsageInfo paginatedTenantInfo = new PaginatedTenantUsageInfo();
        paginatedTenantInfo.setTenantUsages(
                tenantUsages.toArray(new TenantUsage[tenantUsages.size()]));
        paginatedTenantInfo.setNumberOfPages(numberOfPages);
        paginatedTenantInfo.setPageNumber(pageNumber);
        return paginatedTenantInfo;
    }

    /**
     * Returns usage of a particular tenant, requires super admin permissions
     *
     * @param yearMonth
     * @param tenantId
     * @return
     * @throws Exception
     */
    public TenantUsage retrieveTenantUsage(String yearMonth, int tenantId) throws Exception {
        return Util.getTenantUsageRetriever().getTenantUsage(tenantId, yearMonth);
    }

    public InstanceUsageStatics[] retrieveInstanceUsage() throws Exception {
        return Util.getTenantUsageRetriever().getInstanceUsages();
    }

    /**
     * @param yearMonth      year and month that used to retrieve data
     * @param pageNumber
     * @param entriesPerPage number of entries per page
     * @return PaginatedInstanceUsage object that hold instance data and other parameters
     * @throws Exception when retrieving Paginated Instance Usage error occurs
     */
    public PaginatedInstanceUsage retrievePaginatedInstanceUsage(String yearMonth, int pageNumber,
                                                                 int entriesPerPage) throws Exception {
        InstanceUsageStatics[] instanceUsages = retrieveInstanceUsage();
        List<InstanceUsageStatics> instanceUsagesList = new ArrayList<InstanceUsageStatics>();
        PaginatedInstanceUsage paginatedInstanceUsages = new PaginatedInstanceUsage();
        int i = 0;
        int numberOfPages = 0;
        if (instanceUsages != null && instanceUsages.length > 0) {
            for (InstanceUsageStatics usage : instanceUsages) {
                InstanceUsageStatics instance = getValidUsageEntry(usage, yearMonth);
                if (instance != null) {
                    if (i % entriesPerPage == 0) {
                        numberOfPages++;
                    }
                }
            }

            paginatedInstanceUsages.setInstanceUsages(
                instanceUsagesList.toArray(new InstanceUsageStatics[instanceUsagesList.size()]));
        } else {
            paginatedInstanceUsages.setInstanceUsages(null);
        }

        paginatedInstanceUsages.setNumberOfPages(numberOfPages);
        paginatedInstanceUsages.setPageNumber(pageNumber);
        return paginatedInstanceUsages;
    }

    /**
     * @param usage     is Instance usage Statics object that holds data
     * @param yearMonth year and month that need to check with instance usage data
     * @return instance static if instance usage data match with given year and month, else null
     */
    public InstanceUsageStatics getValidUsageEntry(InstanceUsageStatics usage, String yearMonth) {
        Date date = Calendar.getInstance().getTime();
        if (yearMonth != null) {
            try {
                date = CommonUtil.getDateFromMonthString(yearMonth);
            } catch (ParseException e) {

            }
        }
        Calendar startDate = Calendar.getInstance();
        startDate.setTime(date);
        Calendar endDate = (Calendar) startDate.clone();
        endDate.add(Calendar.MONTH, 1);
        if (usage.getStartTime().compareTo(startDate) <= 0 && usage.getStopTime().compareTo(endDate) >= 0) {
            usage.setUsedTimeInSeconds((endDate.getTimeInMillis() -
                    startDate.getTimeInMillis()) / 1000);
            return usage;
        }
        if (usage.getStartTime().compareTo(startDate) > 0 && usage.getStartTime().compareTo(endDate) < 0) {
            if (usage.getStopTime().compareTo(endDate) < 0) {
                usage.setUsedTimeInSeconds((usage.getStopTime().getTimeInMillis() -
                        usage.getStartTime().getTimeInMillis()) / 1000);
                return usage;

            } else if (usage.getStopTime().compareTo(endDate) > 0) {
                usage.setUsedTimeInSeconds((endDate.getTimeInMillis() -
                        usage.getStartTime().getTimeInMillis()) / 1000);
                return usage;

            }
        }
        if (usage.getStartTime().compareTo(startDate) < 0 && usage.getStopTime().compareTo(endDate) < 0) {
            if (usage.getStopTime().compareTo(startDate) > 0) {
                usage.setUsedTimeInSeconds((usage.getStopTime().getTimeInMillis() -
                        startDate.getTimeInMillis()) / 1000);
                return usage;
            }
        }
        return null;
    }
}

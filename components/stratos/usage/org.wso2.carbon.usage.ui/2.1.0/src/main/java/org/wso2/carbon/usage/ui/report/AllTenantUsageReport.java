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

package org.wso2.carbon.usage.ui.report;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.usage.stub.beans.xsd.PaginatedTenantUsageInfo;
import org.wso2.carbon.usage.stub.beans.xsd.TenantUsage;
import org.wso2.carbon.usage.ui.utils.UsageUtil;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;


/**
 * this class is used to generate reports of all tenant usages
 */
public class AllTenantUsageReport {

    private TenantUsage[] tenantUsages;
    private String yearMonth;

    /**
     * @param config
     * @param session
     * @param request
     * @throws Exception
     */
    private static final Log log = LogFactory.getLog(AllTenantUsageReport.class);

    public AllTenantUsageReport(ServletConfig config, HttpSession session, HttpServletRequest request)
            throws Exception {
    	tenantUsages = UsageUtil.retrieveTenantUsages(request, config, session);
     
 
        yearMonth = (String) request.getSession().getAttribute("year-month");
    }

    public List<AllTenantUsageData> getUsageReportData() {

        List<AllTenantUsageData> reportData = new ArrayList<AllTenantUsageData>();   // all the strings need to be passed to
        //  generate the report are added to this list

        if (yearMonth == null) {
            //  get the current year month
            yearMonth = UsageUtil.getCurrentYearMonth();
        }
        String currentYearMonth = UsageUtil.getCurrentYearMonth();

        //  add all the usage data to the list
        try {
            for (TenantUsage usage : tenantUsages) {
                AllTenantUsageData usageData = new AllTenantUsageData();
                usageData.setYearMonth(yearMonth);
                String currentDataStorage = UsageUtil.getTotalDataStorage(usage);
                String regBandwidth = UsageUtil.getTotalBandwidth(usage.getTotalRegistryBandwidth());
                String svcBandwidth = UsageUtil.getTotalBandwidth(usage.getTotalServiceBandwidth());
                long svcTotalRequest = usage.getTotalRequestStatistics().getRequestCount();
                int numberOfUsers = usage.getNumberOfUsers();

                //  String username = (String) request.getSession().getAttribute("logged-user");
                String tenantName = usage.getDomain();
                int tenantId = usage.getTenantId();
                String fullReportLink = "any_tenant_usage.jsp?tenant-id=" + tenantId + "&year-month=" + yearMonth;

                usageData.setTenantName(tenantName);
                if (yearMonth.equals(currentYearMonth)) {
                    usageData.setNumberOfUsers(Integer.toString(numberOfUsers));
                    usageData.setCurrentDataStorage(currentDataStorage);
                }
                // if the yearMonth is not current, number of users coloumn and storage usage coloumn are empty
                else {
                    usageData.setNumberOfUsers("-");
                    usageData.setCurrentDataStorage("-");
                }
                usageData.setRegBandwidth(regBandwidth);
                usageData.setSvcBandwidth(svcBandwidth);
                usageData.setSvcTotalRequest(Long.toString(svcTotalRequest));
                reportData.add(usageData);
            }
        }
        catch (Exception e) {
            String msg = "Error while retrieving tenant usages for month : " + yearMonth;
            log.error(msg, e);
        }
        return reportData;         // return as an array
    }


}

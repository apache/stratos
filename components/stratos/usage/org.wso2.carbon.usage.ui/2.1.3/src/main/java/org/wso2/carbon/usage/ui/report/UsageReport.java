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

import org.wso2.carbon.usage.stub.beans.xsd.BandwidthStatistics;
import org.wso2.carbon.usage.stub.beans.xsd.RequestStatistics;
import org.wso2.carbon.usage.stub.beans.xsd.TenantUsage;
import org.wso2.carbon.usage.ui.utils.UsageUtil;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to generate tenant usage report
 */
public class UsageReport {
    private TenantUsage usage;
    private String yearMonth;

    public UsageReport(ServletConfig config, HttpSession session, HttpServletRequest request)
            throws Exception {
        usage = UsageUtil.retrieveCurrentTenantUsage(request, config, session);
        yearMonth = (String) request.getSession().getAttribute("year-month");

    }


    public List<String> getUsageReportData() {

        int numberOfUsers = usage.getNumberOfUsers();
        if (yearMonth == null) {

            yearMonth = UsageUtil.getCurrentYearMonth();
        }

        String tenantName = usage.getDomain();
        String currentYearMonth = UsageUtil.getCurrentYearMonth();
        List<String> reportData = new ArrayList<String>();
        reportData.add("Basic Tenant Details");
        reportData.add("");
        reportData.add("Duration");
        reportData.add("Tenant Name");
        reportData.add("Number of users");
        reportData.add("Basic Tenant Details");
        reportData.add("");
        reportData.add(yearMonth);
        reportData.add(tenantName);
        reportData.add(String.valueOf(numberOfUsers));


        if (currentYearMonth.equals(yearMonth)) {
            reportData.add("Storage Usage");
            reportData.add("Data Storage");
            reportData.add("Current Data Storage");
            reportData.add("Historical Data Storage");
            reportData.add("Total Data Storage");
            String totalDataStorage = UsageUtil.getTotalDataStorage(usage);
            String currentDataStorage = UsageUtil.getCurrentDataStorage(usage);
            String historyDataStorage = UsageUtil.getHistoryDataStorage(usage);
            reportData.add("Storage Usage");
            reportData.add("Registry Content");
            reportData.add(totalDataStorage);
            reportData.add(currentDataStorage);
            reportData.add(historyDataStorage);
        }

        String totRegInBandwidth = UsageUtil.getIncomingBandwidth(usage.getTotalRegistryBandwidth());
        String totRegOutBandwidth = UsageUtil.getOutgoingBandwidth(usage.getTotalRegistryBandwidth());
        String totRegBandwidth = UsageUtil.getTotalBandwidth(usage.getTotalRegistryBandwidth());
        reportData.add("Registry Bandwidth Usage");
        reportData.add("Server Name");
        reportData.add("Incoming Bandwidth");
        reportData.add("Outgoing Bandwidth");
        reportData.add("Total Bandwidth");
        BandwidthStatistics[] regBWStats = usage.getRegistryBandwidthStatistics();
        if (regBWStats != null) {
            for (BandwidthStatistics stat : regBWStats) {
                String regInBandwidth = UsageUtil.getIncomingBandwidth(stat);
                String regOutBandwidth = UsageUtil.getOutgoingBandwidth(stat);
                String regBandwidth = UsageUtil.getTotalBandwidth(stat);
                reportData.add("Server Name****");
                reportData.add(regInBandwidth);
                reportData.add(regOutBandwidth);
                reportData.add(regBandwidth);

            }
        }
        reportData.add("Registry Bandwidth Usage");
        reportData.add("All Server Total");
        reportData.add(totRegInBandwidth);
        reportData.add(totRegOutBandwidth);
        reportData.add(totRegBandwidth);

        String totSvcInBandwidth = UsageUtil.getIncomingBandwidth(usage.getTotalServiceBandwidth());
        String totSvcOutBandwidth = UsageUtil.getOutgoingBandwidth(usage.getTotalServiceBandwidth());
        String totSvcBandwidth = UsageUtil.getTotalBandwidth(usage.getTotalServiceBandwidth());
        reportData.add("Service Bandwidth Usage");
        reportData.add("Server Name");
        reportData.add("Incoming Bandwidth");
        reportData.add("Outgoing Bandwidth");
        reportData.add("Total Bandwidth");
        BandwidthStatistics[] svcBWStats = usage.getServiceBandwidthStatistics();
        if (svcBWStats != null) {
            for (BandwidthStatistics stat : svcBWStats) {
                String svcInBandwidth = UsageUtil.getIncomingBandwidth(stat);
                String svcOutBandwidth = UsageUtil.getOutgoingBandwidth(stat);
                String svcBandwidth = UsageUtil.getTotalBandwidth(stat);
                reportData.add("Server Name****");
                reportData.add(svcInBandwidth);
                reportData.add(svcOutBandwidth);
                reportData.add(svcBandwidth);

            }
        }
        reportData.add("Service Bandwidth Usage");
        reportData.add("All Server Total");
        reportData.add(totSvcInBandwidth);
        reportData.add(totSvcOutBandwidth);
        reportData.add(totSvcBandwidth);

        String totWebappInBandwidth = UsageUtil.getIncomingBandwidth(usage.getTotalWebappBandwidth());
        String totWebappOutBandwidth = UsageUtil.getOutgoingBandwidth(usage.getTotalWebappBandwidth());
        String totWebappBandwidth = UsageUtil.getTotalBandwidth(usage.getTotalWebappBandwidth());
        BandwidthStatistics[] webappBWStats = usage.getWebappBandwidthStatistics();
        reportData.add("Webapp Bandwidth Usage");
        reportData.add("Server Name");
        reportData.add("Incoming Bandwidth");
        reportData.add("Outgoing Bandwidth");
        reportData.add("Total Bandwidth");
        if (webappBWStats != null) {
            for (BandwidthStatistics stat : webappBWStats) {
                String webappInBandwidth = UsageUtil.getIncomingBandwidth(stat);
                String webappOutBandwidth = UsageUtil.getOutgoingBandwidth(stat);
                String webappBandwidth = UsageUtil.getTotalBandwidth(stat);
                reportData.add("Server Name****");
                reportData.add(webappInBandwidth);
                reportData.add(webappOutBandwidth);
                reportData.add(webappBandwidth);
            }
        }
        reportData.add("Webapp Bandwidth Usage");
        reportData.add("All Server Total");
        reportData.add(totWebappInBandwidth);
        reportData.add(totWebappOutBandwidth);
        reportData.add(totWebappBandwidth);


        long totSvcReqCount = usage.getTotalRequestStatistics().getRequestCount();
        long totSvcRespCount = usage.getTotalRequestStatistics().getResponseCount();
        long totSvcFaultCount = usage.getTotalRequestStatistics().getFaultCount();
        RequestStatistics[] svcStats = usage.getRequestStatistics();
        reportData.add("Service Usage Statistic");
        reportData.add("Server Name");
        reportData.add("Request Count");
        reportData.add("Response Count");
        reportData.add("Fault Count");
        if (svcStats != null && svcStats.length>0 && svcStats[0]!=null) {
            for (RequestStatistics stat : svcStats) {
                long svcReqCount = stat.getRequestCount();
                long svcResCount = stat.getResponseCount();
                long svcFaultCount = stat.getFaultCount();
                reportData.add("Server Name****");
                reportData.add(String.valueOf(svcReqCount));
                reportData.add(String.valueOf(svcResCount));
                reportData.add(String.valueOf(svcFaultCount));
            }
        }
        reportData.add("Service Usage Statistic");
        reportData.add("All Server Total");
        reportData.add(String.valueOf(totSvcReqCount));
        reportData.add(String.valueOf(totSvcRespCount));
        reportData.add(String.valueOf(totSvcFaultCount));

        return reportData;
    }
}

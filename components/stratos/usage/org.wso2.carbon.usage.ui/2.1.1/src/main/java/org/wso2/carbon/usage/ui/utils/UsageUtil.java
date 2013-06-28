/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.usage.ui.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.registry.common.ui.UIException;
import org.wso2.carbon.usage.stub.beans.xsd.*;
import org.wso2.carbon.usage.ui.clients.UsageServiceClient;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class UsageUtil {
    private static final Log log = LogFactory.getLog(UsageUtil.class);

    private static long KB_LIMIT = 1024;
    private static long MB_LIMIT = 1024 * 1024;
    private static long GB_LIMIT = 1024 * 1024 * 1024;
    private static long TB_LIMIT = (long) 1024 * 1024 * 1024 * 1024;

    public static TenantUsage retrieveCurrentTenantUsage(ServletRequest request,
                                                         ServletConfig config, HttpSession session) throws Exception {
        try {
            UsageServiceClient serviceClient = new UsageServiceClient(config, session);
            String yearMonth = request.getParameter("year-month");
            if (yearMonth == null) {
                // get the current year month
                yearMonth = getCurrentYearMonth();
            }
            return serviceClient.retrieveCurrentTenantUsage(yearMonth);
        } catch (Exception e) {
            String msg = "Failed to get current tenant usage.";
            log.error(msg, e);
            throw new UIException(msg, e);
        }
    }

    public static TenantUsage[] retrieveTenantUsages(ServletRequest request,
                                                     ServletConfig config, HttpSession session) throws Exception {
        try {
            UsageServiceClient serviceClient = new UsageServiceClient(config, session);
            String yearMonth = request.getParameter("year-month");
            if (yearMonth == null) {
                // get the current year month
                yearMonth = getCurrentYearMonth();
            }
            return serviceClient.retrieveTenantUsages(yearMonth);
        } catch (Exception e) {
            String msg = "Failed to get all tenants usages.";
            log.error(msg, e);
            throw new UIException(msg, e);
        }
    }

    public static PaginatedTenantUsageInfo retrievePaginatedTenantUsages(ServletRequest request,
                                                                         ServletConfig config, HttpSession session) throws Exception {
        String requestedPage = request.getParameter("requestedPage");
        int pageNumber = 1;
        int numberOfPages = 1;
        int entriesPerPage = 15;
        if (requestedPage != null && requestedPage.length() > 0) {
            pageNumber = new Integer(requestedPage);
        }

        try {
            UsageServiceClient serviceClient = new UsageServiceClient(config, session);
            String yearMonth = request.getParameter("year-month");
            if (yearMonth == null) {
                // get the current year month
                yearMonth = getCurrentYearMonth();
            }
            return serviceClient.retrievePaginatedTenantUsages(yearMonth, pageNumber, entriesPerPage);
        } catch (Exception e) {
            String msg = "Failed to get all tenants usages.";
            log.error(msg, e);
            throw new UIException(msg, e);
        }
    }

    public static TenantUsage retrieveTenantUsage(ServletRequest request,
                                                  ServletConfig config, HttpSession session) throws Exception {
        try {
            UsageServiceClient serviceClient = new UsageServiceClient(config, session);
            String yearMonth = request.getParameter("year-month");
            if (yearMonth == null) {
                // get the current year month
                yearMonth = getCurrentYearMonth();
            }
            String tenantIdStr = request.getParameter("tenant-id");
            if (tenantIdStr == null) {
                tenantIdStr = "0";
            }
            return serviceClient.retrieveTenantUsage(yearMonth, Integer.parseInt(tenantIdStr));
        } catch (Exception e) {
            String msg = "Failed to get tenant usages.";
            log.error(msg, e);
            throw new UIException(msg, e);
        }
    }


    public static String convertBytesToString(long storage) {
        if (storage < KB_LIMIT) {
            return storage + " Byte(s)";
        } else if (storage < MB_LIMIT) {
            return storage / KB_LIMIT + " KByte(s)";
        } else if (storage < GB_LIMIT) {
            return storage / MB_LIMIT + " MByte(s)";
        } else if (storage < TB_LIMIT) {
            return storage / GB_LIMIT + " GByte(s)";
        } else {
            return storage / TB_LIMIT + " TByte(s)";
        }
    }

    public static String getCurrentYearMonth() {
        Calendar calendar = Calendar.getInstance();
        return CommonUtil.getMonthString(calendar);
    }

    public static String[] getYearMonths() {
        // we will list 100 months for now
        List<String> yearMonths = new ArrayList<String>();
        for (int i = 0; i > -100; i--) {
            String yearMonth = CommonUtil.getMonthString(i);
            yearMonths.add(yearMonth);
        }
        return yearMonths.toArray(new String[yearMonths.size()]);
    }

    public static String getCurrentDataStorage(TenantUsage usage) {
        TenantDataCapacity regData = usage.getRegistryCapacity();
        long currentData = 0;
        if (regData != null) {
            currentData = regData.getRegistryContentCapacity();
        }
        return convertBytesToString(currentData);
    }

    public static String getHistoryDataStorage(TenantUsage usage) {
        TenantDataCapacity historyData = usage.getRegistryCapacity();
        long currentData = 0;
        if (historyData != null) {
            currentData = historyData.getRegistryContentHistoryCapacity();
        }
        return convertBytesToString(currentData);
    }

    public static String getTotalDataStorage(TenantUsage usage) {
        TenantDataCapacity regData = usage.getRegistryCapacity();
        long totalDataStorage = 0;
        if (regData != null) {
            totalDataStorage =
                    regData.getRegistryContentCapacity() + regData.getRegistryContentHistoryCapacity();
        }
        return convertBytesToString(totalDataStorage);
    }

    public static String getIncomingBandwidth(BandwidthStatistics bandwidth) {
        long totalBW = 0;
        if (bandwidth != null) {
            totalBW = bandwidth.getIncomingBandwidth();
        }
        return convertBytesToString(totalBW);
    }

    public static String getOutgoingBandwidth(BandwidthStatistics bandwidth) {
        long totalBW = 0;
        if (bandwidth != null) {
            totalBW = bandwidth.getOutgoingBandwidth();
        }
        return convertBytesToString(totalBW);
    }

    public static String getTotalBandwidth(BandwidthStatistics bandwidth) {
        long totalBW = 0;
        if (bandwidth != null) {
            totalBW = bandwidth.getIncomingBandwidth() + bandwidth.getOutgoingBandwidth();
        }
        return convertBytesToString(totalBW);
    }

    public static InstanceUsageStatics[] retrieveInstanceUsage(ServletRequest request,
                                                               ServletConfig config, HttpSession session)
            throws Exception {

        try {
            UsageServiceClient serviceClient = new UsageServiceClient(config, session);
            InstanceUsageStatics[] returnInstanceUsage = serviceClient.retrieveInstanceUsage();
            return returnInstanceUsage;
        } catch (Exception e) {
            String msg = "Failed to get current instance usage.";
            log.error(msg, e);
            throw new UIException(msg, e);
        }
    }

    public static PaginatedInstanceUsage retrievePaginatedInstanceUsages(ServletRequest request,
                                                                         ServletConfig config, HttpSession session) throws Exception {
        String requestedPage = request.getParameter("requestedPage");
        int pageNumber = 1;
        int numberOfPages = 1;
        int entriesPerPage = 15;
        if (requestedPage != null && requestedPage.length() > 0) {
            pageNumber = new Integer(requestedPage);
        }
        try {
            UsageServiceClient serviceClient = new UsageServiceClient(config, session);
            String yearMonth = request.getParameter("year-month");
            if (yearMonth == null) {
                // get the current year month
                yearMonth = getCurrentYearMonth();
            }
            return serviceClient.retrievePaginatedInstanceUsage(yearMonth, pageNumber, entriesPerPage);
        } catch (Exception e) {
            String msg = "Failed to get paginated instance usages.";
            log.error(msg, e);
            throw new UIException(msg, e);
        }
    }
    public static String getAPIUsage(TenantUsage usage) {
        long count = 0;

        if (usage.getApiManagerUsageStats() != null) {
            count =usage.getApiManagerUsageStats()[0].getRequestCount();
        }
        return Long.toString(count);
    }
}

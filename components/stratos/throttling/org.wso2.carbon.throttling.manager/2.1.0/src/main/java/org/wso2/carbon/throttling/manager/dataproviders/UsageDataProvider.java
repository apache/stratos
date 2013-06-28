/**
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.throttling.manager.dataproviders;

import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.throttling.manager.dataobjects.ThrottlingDataContext;
import org.wso2.carbon.throttling.manager.dataobjects.ThrottlingDataEntryConstants;
import org.wso2.carbon.throttling.manager.exception.ThrottlingException;
import org.wso2.carbon.throttling.manager.utils.Util;
import org.wso2.carbon.usage.beans.BandwidthStatistics;
import org.wso2.carbon.usage.beans.RequestStatistics;
import org.wso2.carbon.usage.api.TenantUsageRetriever;
import org.wso2.carbon.usage.beans.TenantUsage;

/**
 *
 */
public class UsageDataProvider extends DataProvider {
    private static final Log log = LogFactory.getLog(UsageDataProvider.class);
    
    @Override
    public void invoke(ThrottlingDataContext dataContext) throws ThrottlingException {
        int tenantId = dataContext.getTenantId();
        String userName = dataContext.getUserName();
        String yearMonth = Util.getCurrentMonthString(Calendar.getInstance());
        TenantUsageRetriever tenantUsageRetriever = Util.getTenantUsageRetriever();
        
        try {
            TenantUsage usage = tenantUsageRetriever.getTenantUsage(tenantId, yearMonth);
            
            //Bandwidth usages
            long tenantIncomingBandwidth = usage.getTotalIncomingBandwidth();
            long tenantOutgoingBandwidth = usage.getTotalOutgoingBandwidth();
            dataContext.addDataLong(ThrottlingDataEntryConstants.TENANT_INCOMING_BANDWIDTH,
                    tenantIncomingBandwidth);
            dataContext.addDataLong(ThrottlingDataEntryConstants.TENANT_OUTGOING_BANDWIDTH,
                    tenantOutgoingBandwidth);
            
            //Registry space capacity
            long currentTenantCapacity = usage.getRegistryContentCapacity();
            long historyTenantCapacity = usage.getRegistryContentHistoryCapacity();
            dataContext.addDataLong(ThrottlingDataEntryConstants.TENANT_CAPACITY,
                    currentTenantCapacity);
            dataContext.addDataLong(ThrottlingDataEntryConstants.TENANT_HISTORY_CAPACITY,
                    historyTenantCapacity);
            //Assigning registry bandwidths
            BandwidthStatistics totalRgistryBW=usage.getTotalRegistryBandwidth();
            dataContext.addDataLong(ThrottlingDataEntryConstants.REGISTRY_INCOMING_BANDWIDTH,
                    totalRgistryBW.getIncomingBandwidth());
            dataContext.addDataLong(ThrottlingDataEntryConstants.REGISTRY_OUTGOING_BANDWIDTH,
                    totalRgistryBW.getOutgoingBandwidth());

            //Assigning service bandwidths
            BandwidthStatistics serviceBWStatistic=usage.getTotalServiceBandwidth();
            dataContext.addDataLong(ThrottlingDataEntryConstants.SERVICE_INCOMING_BANDWIDTH,
                    serviceBWStatistic.getIncomingBandwidth());
            dataContext.addDataLong(ThrottlingDataEntryConstants.SERVICE_OUTGOING_BANDWIDTH,
                    serviceBWStatistic.getOutgoingBandwidth());
            
            //Assigning webapp bandwidths
            BandwidthStatistics webappBWStatistic = usage.getTotalWebappBandwidth();
            dataContext.addDataLong(ThrottlingDataEntryConstants.WEBAPP_INCOMING_BANDWIDTH, 
                    webappBWStatistic.getIncomingBandwidth());
            dataContext.addDataLong(ThrottlingDataEntryConstants.WEBAPP_OUTGOING_BANDWIDTH, 
                    webappBWStatistic.getOutgoingBandwidth());
            
            //Assigning service requests and response
            RequestStatistics requestStat = usage.getTotalRequestStatistics();
            dataContext.addDataLong(ThrottlingDataEntryConstants.SERVICE_REQUEST_COUNT, 
                    requestStat.getRequestCount());
            dataContext.addDataLong(ThrottlingDataEntryConstants.SERVICE_RESPONSE_COUNT, 
                    requestStat.getResponseCount());
            
            //Get number of users
            int usersCount = usage.getNumberOfUsers();
            dataContext.addDataInt(ThrottlingDataEntryConstants.USERS_COUNT, usersCount);

        } catch (Exception e) {
            String msg = "Error in retrieving Usage information. " + "tenant id: " + tenantId
                    + ", user name: " + userName + ".";
            log.error(msg, e);
            throw new ThrottlingException(msg, e);
        }

    }

}

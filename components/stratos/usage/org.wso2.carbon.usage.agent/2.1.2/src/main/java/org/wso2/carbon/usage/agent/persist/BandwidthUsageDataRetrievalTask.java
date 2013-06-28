/*
 * Copyright (c) 2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.usage.agent.persist;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.core.init.CarbonServerManager;
import org.wso2.carbon.stratos.common.constants.UsageConstants;
import org.wso2.carbon.stratos.common.util.StratosConfiguration;
import org.wso2.carbon.usage.agent.beans.BandwidthUsage;
import org.wso2.carbon.usage.agent.config.UsageAgentConfiguration;
import org.wso2.carbon.tomcat.ext.transport.statistics.TransportStatisticsContainer;
import org.wso2.carbon.tomcat.ext.transport.statistics.TransportStatisticsEntry;
import org.wso2.carbon.usage.agent.util.UsageAgentConstants;
import org.wso2.carbon.usage.agent.util.Util;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.CarbonUtils;

import java.util.Queue;

public class BandwidthUsageDataRetrievalTask implements Runnable {
    private static final Log log = LogFactory.getLog(BandwidthUsageDataRetrievalTask.class);

    private Queue<TransportStatisticsEntry> transportStats;
    private UsageAgentConfiguration configuration;

    //This will be decided based on whether a BAM Server URL is provided or not
    private boolean isBamAvailable=false;

    public BandwidthUsageDataRetrievalTask(UsageAgentConfiguration configuration) {
        transportStats = TransportStatisticsContainer.getTransportStatistics();
        this.configuration = configuration;

        //Checking for the BAM Server URL
        String bamServerUrl = ServerConfiguration.getInstance().getFirstProperty("BamServerURL");
        if(bamServerUrl != null){
            this.isBamAvailable = true;
        }
    }

    public void run() {
        /*if (log.isDebugEnabled()) {
            log.debug("Retrieving Service and Web App bandwidth usage statistics.");
        }*/

        if (!transportStats.isEmpty()) {
            for (int i = 0; i < configuration.getUsageTasksNumberOfRecordsPerExecution() && !transportStats.isEmpty(); i++) {
                TransportStatisticsEntry entry = transportStats.remove();
                try {
                    if(!isBamAvailable){
                        return;
                    }

                    int tenantId = getTenantID(entry.getTenantName());
                    //if the tenant does not exist, no need and no way of updating the usage data
                    //therefore ignore it
                    if(tenantId<0){
                        return;
                    }
                    if (inferMeasurement(entry).equals(UsageConstants.SERVICE_BANDWIDTH)) {
                        if (entry.getRequestSize() > 0) {
                            Util.addToPersistingControllerQueue(new BandwidthUsage(getTenantID(entry.getTenantName()), UsageConstants.SERVICE_INCOMING_BW, entry.getRequestSize()));
                        }
                        if (entry.getResponseSize() > 0) {
                            Util.addToPersistingControllerQueue(new BandwidthUsage(getTenantID(entry.getTenantName()), UsageConstants.SERVICE_OUTGOING_BW, entry.getResponseSize()));
                        }
                    } else if (inferMeasurement(entry).equals(UsageConstants.WEBAPP_BANDWIDTH)) {
                        if (entry.getRequestSize() > 0) {
                            Util.addToPersistingControllerQueue(new BandwidthUsage(getTenantID(entry.getTenantName()), UsageConstants.WEBAPP_INCOMING_BW, entry.getRequestSize()));
                        }
                        if (entry.getResponseSize() > 0) {
                            Util.addToPersistingControllerQueue(new BandwidthUsage(getTenantID(entry.getTenantName()), UsageConstants.WEBAPP_OUTGOING_BW, entry.getResponseSize()));
                        }
                    }
                } catch (UserStoreException e) {
                    log.error("Error persisting bandwidth usage statistics.", e);
                }

            }
        }
    }


    private String inferMeasurement(TransportStatisticsEntry entry) {
        if (entry.getContext() != null) {
            if (entry.getContext().equals(UsageAgentConstants.BANDWIDTH_USAGE_SERVICES_CONTEXT)) {
                return UsageConstants.SERVICE_BANDWIDTH;
            } else if (entry.getContext().equals(UsageAgentConstants.BANDWIDTH_USAGE_WEBAPPS_CONTEXT)) {
                return UsageConstants.WEBAPP_BANDWIDTH;
            }
        }

        return UsageAgentConstants.BANDWIDTH_CARBON;
    }

    private int getTenantID(String tenantDomain) throws UserStoreException {
        return Util.getRealmService().getTenantManager().getTenantId(tenantDomain);
    }
}

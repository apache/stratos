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

package org.wso2.carbon.usage.summary.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.analytics.hive.extension.AbstractHiveAnalyzer;
import org.wso2.carbon.usage.summary.helper.util.DataAccessObject;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper class to set the date range for the next monthly summarization cycle in the hive config.
 * This is important to select the data slice corresponding the current monthly summarization cycle
 * from the usage hourly tables.
 */
public class MonthlyServiceStatsSummarizerHelper extends AbstractHiveAnalyzer {

    private static Log log = LogFactory.getLog(HourlySummarizerHelper.class);

    public void execute() {
        log.info("Running custom analyzer for Stratos service stats monthly summarization.");
        try {
            String lastMonthlyTimestampStr = DataAccessObject.getInstance().getAndUpdateLastUsageMonthlyTimestamp();
            Long lastMonthlyTimestampSecs = Timestamp.valueOf(lastMonthlyTimestampStr).getTime() / 1000;

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
            String currentTsStr = formatter.format(new Date().getTime());
            Long currentTsSecs = Timestamp.valueOf(currentTsStr).getTime() / 1000;

            log.info("Running monthly service stats analytics from " + lastMonthlyTimestampStr + " to " + currentTsStr);
            setProperty("last_monthly_ts", lastMonthlyTimestampSecs.toString());
            setProperty("current_monthly_ts", currentTsSecs.toString());
        } catch (Exception e) {
            log.error("An error occurred while setting month range for monthly service stats analytics. ", e);
        }


    }
}

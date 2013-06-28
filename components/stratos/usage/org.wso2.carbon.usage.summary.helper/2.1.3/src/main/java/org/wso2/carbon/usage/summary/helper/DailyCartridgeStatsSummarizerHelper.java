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
 * Helper class to set the hour range for the next daily summarization cycle in the hive config.
 * This is important to select the data slice corresponding the current daily summarization cycle
 * from the usage hourly tables.
 */
public class DailyCartridgeStatsSummarizerHelper extends AbstractHiveAnalyzer {

    private static Log log = LogFactory.getLog(HourlySummarizerHelper.class);

    public void execute() {
        log.info("Running custom analyzer for Stratos cartridge stats daily summarization.");
        try {
            String lastDailyTimestampStr = DataAccessObject.getInstance().getAndUpdateLastCartridgeStatsDailyTimestamp();
            Long lastDailyTimestampSecs = Timestamp.valueOf(lastDailyTimestampStr).getTime() / 1000;

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:00:00");
            String currentTsStr = formatter.format(new Date().getTime());
            Long currentTsSecs = Timestamp.valueOf(currentTsStr).getTime() / 1000;

            log.info("Running daily cartridge stats analytics from " + lastDailyTimestampStr + " to " + currentTsStr);
            setProperty("last_daily_ts", lastDailyTimestampSecs.toString());
            setProperty("current_daily_ts", currentTsSecs.toString());
        } catch (Exception e) {
            log.error("An error occurred while setting date range for daily cartridge stats analysis. ", e);
        }
    }
}

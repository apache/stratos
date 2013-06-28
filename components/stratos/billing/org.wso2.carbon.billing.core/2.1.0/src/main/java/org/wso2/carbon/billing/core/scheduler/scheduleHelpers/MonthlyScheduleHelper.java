/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.billing.core.scheduler.scheduleHelpers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.jdbc.DataAccessObject;
import org.wso2.carbon.billing.core.scheduler.ScheduleHelper;
import org.wso2.carbon.billing.core.scheduler.SchedulerContext;

import java.util.Map;
import java.util.TimeZone;

public class MonthlyScheduleHelper implements ScheduleHelper {
    private static final Log log = LogFactory.getLog(MonthlyScheduleHelper.class);

    private static final String CRON_KEY = "cron";
    private static final String TIME_ZONE_KEY = "timeZone";
    private static final String DEFAULT_TIMEZONE = "GMT-8:00";

    private String cron;
    private TimeZone timeZone;
    

    public void init(Map<String, String> triggerCalculatorConfig) throws BillingException {
        String timeZoneStr = triggerCalculatorConfig.get(TIME_ZONE_KEY);
        if (timeZoneStr == null) {
            timeZoneStr = DEFAULT_TIMEZONE;
        }
        timeZone = TimeZone.getTimeZone(timeZoneStr);
        //Timezone is important when comparing dates in sql queries
        DataAccessObject.TIMEZONE = timeZoneStr;
        
        cron = triggerCalculatorConfig.get(CRON_KEY);
        log.debug("Cron string: " + cron);
    }

    public void invoke(SchedulerContext schedulerContext) throws BillingException {
        schedulerContext.setCronString(cron);
    }
}

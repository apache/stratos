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
package org.wso2.carbon.billing.test;

import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.scheduler.ScheduleHelper;
import org.wso2.carbon.billing.core.scheduler.SchedulerContext;

import java.util.Map;

public class FiveSecondTriggerCalculator implements ScheduleHelper {
    private int countUpToLimit;
    private static int count;

    public void init(Map<String, String> triggerCalculatorConfig) {
        String countUpToStr = triggerCalculatorConfig.get("countUpToLimit");
        countUpToLimit = Integer.parseInt(countUpToStr);
        count = 0; // obviously this is not thread-safe.
        // nothing to init
    }

    public void invoke(SchedulerContext schedulerContext) throws BillingException {
        if (count >= countUpToLimit) {
            //schedulerContext.setNextTriggerInterval(-1);
            return;
        }
        //long currentTime = System.currentTimeMillis();
        //schedulerContext.setNextTriggerInterval(5000);
        count++;
    }
}

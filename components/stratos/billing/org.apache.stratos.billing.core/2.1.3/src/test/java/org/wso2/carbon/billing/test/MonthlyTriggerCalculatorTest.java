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

import junit.framework.TestCase;
import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.scheduler.SchedulerContext;
import org.wso2.carbon.billing.core.scheduler.scheduleHelpers.MonthlyScheduleHelper;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class MonthlyTriggerCalculatorTest extends TestCase {
    @Override
    public void setUp() {
        // nothing to setup here
    }

    public void testNextTriggerInterval() throws BillingException {
        nextMonthFirstDate("1", 1999, Calendar.DECEMBER, 2, 2000, Calendar.JANUARY, 1, 1999,
                Calendar.NOVEMBER, 1, 1999, Calendar.NOVEMBER, 30, "1999-November");
        nextMonthFirstDate("1", 2009, Calendar.DECEMBER, 2, 2010, Calendar.JANUARY, 1, 2009,
                Calendar.NOVEMBER, 1, 2009, Calendar.NOVEMBER, 30, "2009-November");
        nextMonthFirstDate("1", 2009, Calendar.DECEMBER, 31, 2010, Calendar.JANUARY, 1, 2009,
                Calendar.NOVEMBER, 1, 2009, Calendar.NOVEMBER, 30, "2009-November");
        nextMonthFirstDate("15", 2010, Calendar.FEBRUARY, 8, 2010, Calendar.MARCH, 15, 2010,
                Calendar.JANUARY, 15, 2010, Calendar.FEBRUARY, 14, "2010-January");
    }

    private void nextMonthFirstDate(String triggerOn, int someYear, int someMonth, int someDay,
                                    int nextMonthFirstDateYear, int nextMonthFirstDateMonth,
                                    int nextMonthFirstDateDay, int durationStartYear,
                                    int durationStartMonth, int durationStartDay,
                                    int durationEndYear, int durationEndMonth, int durationEndDay,
                                    String yearMonthString) throws BillingException {
        Calendar someCalender = Calendar.getInstance();
        someCalender.set(someYear, someMonth, someDay);
        long someDateTimeStamp = someCalender.getTimeInMillis();

        Map<String, String> args = new HashMap<String, String>();
        String timeZoneStr = "GMT-8:00";
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneStr);
        args.put("timeZone", timeZoneStr);
        args.put("dayToTriggerOn", triggerOn);
        args.put("hourToTriggerOn", "0");
        MonthlyScheduleHelper monthlyTriggerCalculator = new MonthlyScheduleHelper();
        monthlyTriggerCalculator.init(args);

        SchedulerContext schedulerContext = new SchedulerContext();
        //monthlyTriggerCalculator.invoke(schedulerContext, someDateTimeStamp);

        //long nextMonthFirstTimeStampDuration = schedulerContext.getNextTriggerInterval();

        //long durationStart = schedulerContext.getCurrentDurationStart();
        //long durationEnd = schedulerContext.getCurrentDurationEnd();
        //String realMonthStr = schedulerContext.getDurationString();

        Calendar nextMonthFirstCalendar = Calendar.getInstance(timeZone);
        //nextMonthFirstCalendar.setTimeInMillis(someDateTimeStamp + nextMonthFirstTimeStampDuration);

        assertEquals("Year should be " + nextMonthFirstDateYear,
                nextMonthFirstCalendar.get(Calendar.YEAR), nextMonthFirstDateYear);
        assertEquals("Month should be " + nextMonthFirstDateMonth,
                nextMonthFirstCalendar.get(Calendar.MONTH), nextMonthFirstDateMonth);
        assertEquals("Date should be " + nextMonthFirstDateDay,
                nextMonthFirstCalendar.get(Calendar.DAY_OF_MONTH), nextMonthFirstDateDay);

        Calendar durationStartCalendar = Calendar.getInstance(timeZone);
        //durationStartCalendar.setTimeInMillis(durationStart);

        assertEquals("Year should be " + durationStartYear,
                durationStartCalendar.get(Calendar.YEAR), durationStartYear);
        assertEquals("Month should be " + durationStartMonth,
                durationStartCalendar.get(Calendar.MONTH), durationStartMonth);
        assertEquals("Date should be " + durationStartDay,
                durationStartCalendar.get(Calendar.DAY_OF_MONTH), durationStartDay);

        Calendar durationEndCalendar = Calendar.getInstance(timeZone);
        //durationEndCalendar.setTimeInMillis(durationEnd);

        assertEquals("Year should be " + durationEndYear, durationEndCalendar.get(Calendar.YEAR),
                durationEndYear);
        assertEquals("Month should be " + durationEndMonth,
                durationEndCalendar.get(Calendar.MONTH), durationEndMonth);
        assertEquals("Date should be " + durationEndDay,
                durationEndCalendar.get(Calendar.DAY_OF_MONTH), durationEndDay);

        //assertEquals("YearMonth String should be equal", realMonthStr, yearMonthString);
    }

}

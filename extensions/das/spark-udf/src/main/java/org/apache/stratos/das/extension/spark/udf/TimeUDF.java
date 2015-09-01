/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.das.extension.spark.udf;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Implementing UDF for implementing spark sql query related to time.
 */
public class TimeUDF {
    /**
     * Convert time(ms) to DateFormat
     *
     * @param timeStamp time in ms
     * @return date as String
     */
    public String time(Long timeStamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date = new Date(timeStamp.longValue());
        return sdf.format(date);
    }

    /**
     * Get the current time in ms
     *
     * @param param
     * @return
     */
    public long current_time(Integer param) {
        return System.currentTimeMillis();
    }
}
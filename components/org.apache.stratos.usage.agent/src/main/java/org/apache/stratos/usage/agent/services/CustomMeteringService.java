/*
 *Licensed to the Apache Software Foundation (ASF) under one
 *or more contributor license agreements.  See the NOTICE file
 *distributed with this work for additional information
 *regarding copyright ownership.  The ASF licenses this file
 *to you under the Apache License, Version 2.0 (the
 *"License"); you may not use this file except in compliance
 *with the License.  You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.apache.stratos.usage.agent.services;

import org.wso2.carbon.core.AbstractAdmin;
import org.apache.stratos.usage.agent.api.CustomMeteringAgent;
import org.apache.stratos.usage.agent.exception.UsageException;

/**
 *      CustomMeteringService class defines methods to get recorded duration, to check whether
 *      usage entries exists, persist usage, retrieve usage and add usage.
 */
public class CustomMeteringService extends AbstractAdmin {

    /**
     * method to get recorded durations
     * @param measurement  the measurement name
     * @return  duration array
     * @throws Exception
     */

    public String[] getRecordedDurations(String measurement) throws Exception {
        return new CustomMeteringAgent(getGovernanceRegistry()).getRecordedDurations(measurement);
    }

    /**
     * method to check whether usage entry exists or not
     * @param duration  duration
     * @param measurement measurement name
     * @return true if usage entry exist
     * @throws Exception
     */
    public boolean isUsageEntryExists( String duration, String measurement)
            throws Exception {
        return new CustomMeteringAgent(getGovernanceRegistry()).isUsageEntryExists(duration,
                measurement);
    }

    /**
     * method to persist usage
     * @param duration
     * @param measurement measurement name
     * @param value   value of measurement
     * @throws Exception
     */
    public void persistUsage( String duration, String measurement, String value)
            throws Exception {
        new CustomMeteringAgent(getGovernanceRegistry()).persistUsage(duration, measurement, value);
    }

    /**
     * method to retrieve usage
     * @param duration
     * @param measurement measurement name
     * @return usage value
     * @throws UsageException
     */

    public String retrieveUsage( String duration, String measurement)
            throws UsageException {
        return new CustomMeteringAgent(getGovernanceRegistry())
                .retrieveUsage(duration, measurement);
    }

    /**
     * method to add usage entries
     * @param userName user name
     * @param duration duration of the measurement
     * @param measurement measurement name
     * @param value usage value
     * @return usage value
     * @throws Exception
     */
    public long addUsage(String userName, String duration, String measurement, long value)
            throws Exception {
        return new CustomMeteringAgent(getGovernanceRegistry()).addUsage(duration, measurement,
                value);
    }
}
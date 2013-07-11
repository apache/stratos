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
package org.apache.stratos.usage.agent.api;

import org.apache.stratos.usage.agent.exception.UsageException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.*;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.apache.stratos.common.constants.UsageConstants;

/**
 *   CustomMeteringAgent is used to get recorded duration, to check whether usage entry exists,
 *   to persist and retrieve usage.
 */

public class CustomMeteringAgent {
    private static final Log log = LogFactory.getLog(CustomMeteringAgent.class);
    private Registry registry;

    /**
     * Constructor for the custom metering agent
     *
     * @param registry governance registry of super tenant
     */
    public CustomMeteringAgent(Registry registry) {
        this.registry = registry;
    }

    /**
     * Get recorded durations
     * @param measurement  the measurement to get the duration
     * @return the durations array
     * @throws org.apache.stratos.usage.agent.exception.UsageException
     */
    public String[] getRecordedDurations(String measurement) throws UsageException {
        String[] durations;
        String measurementCollectionPath =
                UsageConstants.CUSTOM_METERING_PATH + RegistryConstants.PATH_SEPARATOR +
                        measurement;
        try {
            Resource resource = registry.get(measurementCollectionPath);
            if (!(resource instanceof Collection)) {
                String msg =
                        "The records collection is not a registry collection. path: " +
                                measurementCollectionPath + ".";
                log.error(msg);
                throw new UsageException(msg);
            }
            durations = ((Collection) resource).getChildren();
        } catch (RegistryException e) {
            String msg =
                    "Error in checking the usage entry exists. measurement: " + measurement + ".";
            log.error(msg, e);
            throw new UsageException(msg, e);
        }
        return durations;
    }

    /**
     * Check whether the usage entry exist or not
     *
     * @param duration    the duration (e.g. year month), null for any duration
     * @param measurement measurement key
     * @return true if usage entry exists
     * @throws org.apache.stratos.usage.agent.exception.UsageException
     */
    public boolean isUsageEntryExists(String duration, String measurement)
            throws UsageException {
        if (duration == null) {
            duration = UsageConstants.ANY_DURATION;
        }
        String measurementPath =
                UsageConstants.CUSTOM_METERING_PATH + RegistryConstants.PATH_SEPARATOR +
                        measurement +
                        RegistryConstants.PATH_SEPARATOR + duration;
        try {
            if (registry.resourceExists(measurementPath)) {
                return true;
            }
        } catch (RegistryException e) {
            String msg =
                    "Error in checking the usage entry exists. measurement: " + measurement + ".";
            log.error(msg, e);
            throw new UsageException(msg, e);
        }
        return false;
    }

    /**
     * Persist usage of a user
     *
     * @param duration    the duration (e.g. year month), null for any duration
     * @param measurement measurement key
     * @param value       measurement value
     * @throws org.apache.stratos.usage.agent.exception.UsageException
     */
    public void persistUsage(String duration, String measurement, String value)
            throws UsageException {
        if (duration == null) {
            duration = UsageConstants.ANY_DURATION;
        }
        Resource measurementResource;
        String measurementPath =
                UsageConstants.CUSTOM_METERING_PATH + RegistryConstants.PATH_SEPARATOR +
                        measurement +
                        RegistryConstants.PATH_SEPARATOR + duration;
        try {
            measurementResource = registry.newResource();
            ((ResourceImpl) measurementResource).setVersionableChange(false);
            // save the measurement value in resource
            measurementResource.setContent(value);
            registry.put(measurementPath, measurementResource);
        } catch (RegistryException e) {
            String msg =
                    "Error in persisting the usage. measurement: " +
                            measurement + ".";
            log.error(msg, e);
            throw new UsageException(msg, e);
        }
    }

    /**
     * Retrieve usage of a user
     *
     * @param duration    the duration (e.g. year month), null for any duration
     * @param measurement measurement key
     * @return measurement value
     * @throws org.apache.stratos.usage.agent.exception.UsageException
     */
    public String retrieveUsage(String duration, String measurement)
            throws UsageException {
        String usageValue;
        Resource measurementResource;
        String measurementPath =
                UsageConstants.CUSTOM_METERING_PATH + RegistryConstants.PATH_SEPARATOR +
                        measurement +
                        RegistryConstants.PATH_SEPARATOR + duration;
        try {
            measurementResource = registry.get(measurementPath);
            // save the measurement value in resource
            byte[] contentBytes = (byte[]) measurementResource.getContent();
            usageValue = new String(contentBytes);
        } catch (RegistryException e) {
            String msg =
                    "Error in retrieving the usage. measurement: " +
                            measurement + ".";
            log.error(msg, e);
            throw new UsageException(msg, e);
        }
        return usageValue;
    }

    /**
     * Add a long value to the usage, if there were no previous entry, this will
     * start with value 0
     *
     * @param duration    the duration (e.g. year month), null for any duration
     * @param measurement measurement key
     * @param value       measurement value
     * @return the added measurement value
     * @throws org.apache.stratos.usage.agent.exception.UsageException
     */
    public long addUsage(String duration, String measurement, long value)
            throws UsageException {
        if (duration == null) {
            duration = UsageConstants.ANY_DURATION;
        }
        // adding the bandwidth have to be in a transaction
        boolean transactionSuccess = false;
        try {
            registry.beginTransaction();
            if (isUsageEntryExists(duration, measurement)) {
                String usageStr = retrieveUsage(duration, measurement);
                try {
                    long storedValue = Long.parseLong(usageStr);
                    value += storedValue;
                } catch (NumberFormatException e) {
                    String msg = "Error in parsing the integer string: " + usageStr;
                    log.error(msg, e);
                    throw new RegistryException(msg, e);
                }
            }
            String valueStr = Long.toString(value);
            persistUsage(duration, measurement, valueStr);
            transactionSuccess = true;
        } catch (RegistryException e) {
            String msg =
                    "Error in invoking the add usage. measurement: " +
                            measurement + ".";
            log.error(msg, e);
            throw new UsageException(msg, e);
        } finally {
            try {
                if (transactionSuccess) {
                    registry.commitTransaction();
                } else {
                    registry.rollbackTransaction();
                }
            } catch (RegistryException e) {
                String msg = "Error in commiting/rollbacking the transaction";
                log.error(msg, e);
            }
        }
        return value;
    }
}

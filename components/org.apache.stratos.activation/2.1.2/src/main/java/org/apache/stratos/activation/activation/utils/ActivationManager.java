/*
 *  Copyright (c) 2005-2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.stratos.activation.activation.utils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Activations of Services for Tenants.
 */
public class ActivationManager {

    private static final ConcurrentHashMap<Integer, Boolean> activations =
            new ConcurrentHashMap<Integer, Boolean>();
    private static Timer timer = null;

    /**
     * Starts cleaning up cached activation records at periodic intervals.
     */
    public static void startCacheCleaner() {
        TimerTask faultyServiceRectifier = new CacheCleaner();
        timer = new Timer();
        // Retry in 1 minute
        long retryIn = 1000 * 60;
        timer.schedule(faultyServiceRectifier, 0, retryIn);
    }

    /**
     * Stops cleaning up cached activation records.
     */
    public static void stopCacheCleaner() {
        timer.cancel();
        timer = null;
    }

    /**
     * Method to set an activation record.
     *
     * @param tenantId the tenant identifier.
     * @param status   true if the service is active or false if not.
     */
    public static void setActivation(int tenantId, boolean status) {
        ActivationManager.activations.put(tenantId, status);
    }

    /**
     * Method to check whether an activation record exists for the given tenant.
     *
     * @param tenantId the tenant identifier.
     *
     * @return true if a record exists.
     */
    public static boolean activationRecorded(int tenantId) {
        return ActivationManager.activations.get(tenantId) != null;
    }

    /**
     * Method to retrieve an activation record.
     *
     * @param tenantId the tenant identifier.
     *
     * @return true if the service is active or false if not.
     */
    public static boolean getActivation(int tenantId) {
        return ActivationManager.activations.get(tenantId) != null &&
                ActivationManager.activations.get(tenantId);
    }

    private static class CacheCleaner extends TimerTask {

        /**
         * {@inheritDoc}
         */
        public void run() {
            ActivationManager.activations.clear();
        }
    }

}

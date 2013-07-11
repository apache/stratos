/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.status.monitor.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.apache.stratos.status.monitor.core.StatusMonitorConfigurationBuilder;
import org.apache.stratos.status.monitor.core.constants.StatusMonitorConstants;
import org.apache.stratos.status.monitor.core.exception.StatusMonitorException;
import org.apache.stratos.status.monitor.core.jdbc.MySQLConnectionInitializer;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;

/**
 * Utility methods for Status monitor core.
 */
public class StatusMonitorUtil {

    private static final Log log =
            LogFactory.getLog(StatusMonitorUtil.class);
    private static StatusMonitorConfigurationBuilder statusMonitorConfiguration;

    public static StatusMonitorConfigurationBuilder getStatusMonitorConfiguration() {
        return statusMonitorConfiguration;
    }

    /**
     * load the configuration for the status monitoring
     * @param bundleContext, BundleContext
     * @throws org.apache.stratos.status.monitor.core.exception.StatusMonitorException,
     * if the status monitoring failed.
     */
    public static void initStatusMonitor(
            BundleContext bundleContext) throws StatusMonitorException {
        String configFile =
                CarbonUtils.getCarbonConfigDirPath() + File.separator +
                        StatusMonitorConstants.STATUS_MONITOR_CONFIG;
        try {
            statusMonitorConfiguration = new StatusMonitorConfigurationBuilder(configFile);
        } catch (Exception e) {
            String msg = "The Status Monitor Configuration file not found";
            log.error(msg, e);
            throw new StatusMonitorException (msg, e);
        }
        try {
            MySQLConnectionInitializer.initialize();
        } catch (Exception e) {
            String msg = "Error in initializing the mysql connection for the health monitoring";
            log.error(msg, e);
            throw new StatusMonitorException (msg, e);
        }
    }

}

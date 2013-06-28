/*
 *  Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.status.monitor.ui.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.status.monitor.stub.beans.xsd.ServiceStateDetailInfoBean;
import org.wso2.carbon.status.monitor.stub.beans.xsd.ServiceStateInfoBean;
import org.wso2.carbon.status.monitor.ui.clients.HealthMonitorServiceClient;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Utility methods for status.monitor.ui
 */
public class StatusMonitorUtil {
    private static final Log log = LogFactory.getLog(
            org.wso2.carbon.status.monitor.ui.utils.StatusMonitorUtil.class);

    /**
     * Get all the service statuses
     *
     * @param request HttpServletRequest
     * @param config  ServletConfig
     * @param session HttpSession
     * @return ServiceStateInfoBean[] - Array of service states
     * @throws Exception , if getting the service status information failed.
     */
    public static ServiceStateInfoBean[] retrieveStatuses(
            HttpServletRequest request, ServletConfig config,
                                              HttpSession session) throws Exception {

        try {
            HealthMonitorServiceClient serviceClient =
                    new HealthMonitorServiceClient(config, session);
            return serviceClient.retrieveStatuses();
        } catch (Exception e) {
            String msg = "Failed to get the service status info beans";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    /**
     * Get all the service statuses
     * @param request HttpServletRequest
     * @param config  ServletConfig
     * @param session HttpSession
     * @return ServiceStateDetailInfoBean[] - Array of service state details
     * @throws Exception , if getting the service state detail information failed.
     */
    public static ServiceStateDetailInfoBean[] retrieveStateDetails(
            HttpServletRequest request, ServletConfig config,
            HttpSession session) throws Exception {
        try {
            HealthMonitorServiceClient serviceClient =
                    new HealthMonitorServiceClient(config, session);
            return serviceClient.retrieveStateDetails();
        } catch (Exception e) {
            String msg = "Failed to get the service state details info beans";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    /**
     * Get the status of a particular service
     *
     * @param request HttpServletRequest
     * @param config  ServletConfig
     * @param session HttpSession
     * @return ServiceStateInfoBean - State details of the service
     * @throws Exception, if error in getting the service state information
     */
    public static ServiceStateInfoBean getStatus(HttpServletRequest request, ServletConfig config,
                                           HttpSession session) throws Exception {
        String serviceName = "";
        try {
            serviceName = request.getParameter("service");
            HealthMonitorServiceClient serviceClient =
                    new HealthMonitorServiceClient(config, session);
            return serviceClient.getServiceStatus(serviceName);
        } catch (Exception e) {
            String msg = "Failed to get the status details of the service:" + serviceName;
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }
}

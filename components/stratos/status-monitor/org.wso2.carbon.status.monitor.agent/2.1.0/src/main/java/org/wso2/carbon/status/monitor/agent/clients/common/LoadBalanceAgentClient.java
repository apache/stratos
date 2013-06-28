/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.status.monitor.agent.clients.common;

import org.apache.axis2.AxisFault;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.load.balance.agent.stub.LoadBalanceAgentServiceStub;
import org.wso2.carbon.status.monitor.agent.internal.core.MySQLConnector;

import java.rmi.RemoteException;
import java.sql.SQLException;

/**
 * The agent class that gets the load average of the service instances
 */
public class LoadBalanceAgentClient {
    private static final Log log = LogFactory.getLog(LoadBalanceAgentClient.class);
    /**
     * Load Balance Agent - This gets the load information from the service instances of each of the
     * service cluster.
     */
    private static final String serviceName = "LoadBalanceAgentService";
    private static final String services = "/services/";


    /**
     * gets the load average from the remote instances given in the instances map.
     *
     * @param hostName, host name of the service
     * @param serviceID, int
     * @return the load average of the given instance.
     * @throws java.sql.SQLException, if exception in writing to the database.
     */
    public static double getLoadAverageFromInstances(String hostName, int serviceID) throws SQLException {
        double loadAverage = -1;
        LoadBalanceAgentServiceStub stub;
        String serviceUrl;

        // gets the instance and the service
        try {
            serviceUrl = "https://" + hostName + services + serviceName;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to get the running application instances to get the " +
                        "load average", e);
            }
            return loadAverage;
        }

        // gets the load average of the service instance
        try {
            stub = new LoadBalanceAgentServiceStub(serviceUrl);
            loadAverage = stub.getLoadAverage();
        } catch (AxisFault e) {
            MySQLConnector.insertStats(serviceID, false);
            MySQLConnector.insertState(serviceID, false, e.getMessage());
            String msg = "Invoking the Load Balance Agent Client Failed";
            log.warn(msg, e);
        } catch (RemoteException e) {
            MySQLConnector.insertStats(serviceID, false);
            MySQLConnector.insertState(serviceID, false, e.getMessage());
            String msg = "Remote Exception in invoking the Load Balance Agent Client";
            log.warn(msg, e);
        } catch (Exception e) {
            MySQLConnector.insertStats(serviceID, false);
            MySQLConnector.insertState(serviceID, false, e.getMessage());
            String msg = "Exception in invoking the Load Balance Agent Client";
            log.warn(msg, e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Load Average from the service: " + serviceUrl + ": " + loadAverage);
        }
        return loadAverage;
    }

}

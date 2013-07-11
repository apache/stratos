/*
 *Licensed to the Apache Software Foundation (ASF) under one
 *or more contributor license agreements.  See the NOTICE file
 *distributed with this work for additional information
 *regarding copyright ownership.  The ASF licenses this file
 *to you under the Apache License, Version 2.0 (the
 *"License"); you may not use this file except in compliance
 *with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.apache.stratos.status.monitor.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.AbstractAdmin;
import org.apache.stratos.status.monitor.beans.ServiceStateDetailInfoBean;
import org.apache.stratos.status.monitor.beans.ServiceStateInfoBean;
import org.apache.stratos.status.monitor.core.jdbc.MySQLConnectionInitializer;
import org.apache.stratos.status.monitor.internal.core.MySQLConnector;

import java.util.List;

/**
 * This is the admin Web service which is used for health monitoring of the services
 */
public class HealthMonitorService extends AbstractAdmin {
    private static final Log log = LogFactory.getLog(
            HealthMonitorService.class);

    /**
     * Get the status of all the services
     *
     * @return serviceStateInfoBean[]
     * @throws Exception if failed to get Tenant Manager
     */
    public ServiceStateInfoBean[] getAllServiceStatus() throws Exception {
        List<ServiceStateInfoBean> serviceStatusList = MySQLConnector.getAllServiceState();
        return serviceStatusList.toArray(new ServiceStateInfoBean[serviceStatusList.size()]);
    }

    /**
     * Get the status of a specific service
     *
     * @param ServiceName: String
     * @return ServiceInfoBean: Service Information
     * @throws Exception, if the retrieval of the service status failed.
     */
    public ServiceStateInfoBean getServiceStatus(String ServiceName) throws Exception {
        int serviceID = MySQLConnectionInitializer.getServiceID(ServiceName);
        return MySQLConnector.getServiceState(serviceID);
    }

    public ServiceStateDetailInfoBean[] getAllServiceStateDetail() throws Exception {
        List<ServiceStateDetailInfoBean> serviceStatusDetailList =
                MySQLConnector.getAllServiceStateDetail();
        return serviceStatusDetailList.toArray(
                new ServiceStateDetailInfoBean[serviceStatusDetailList.size()]);
    }
}


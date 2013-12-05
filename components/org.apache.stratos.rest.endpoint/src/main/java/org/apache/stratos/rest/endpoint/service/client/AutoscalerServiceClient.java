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

package org.apache.stratos.rest.endpoint.service.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.stub.AutoScalerServiceStub;
import org.apache.stratos.rest.endpoint.ServiceHolder;

import java.rmi.RemoteException;

public class AutoscalerServiceClient {

    private AutoScalerServiceStub stub;

    private static final Log log = LogFactory.getLog(AutoscalerServiceClient.class);
    private static volatile AutoscalerServiceClient serviceClient;
    private static final String AUTOSCALER_SERVICE_URL = "autoscaler.service.url";

    public AutoscalerServiceClient(String epr) throws AxisFault {

        ConfigurationContext clientConfigContext = ServiceHolder.getConfigurationContext().getClientConfigContext();
        try {
            stub = new AutoScalerServiceStub(clientConfigContext, epr);
            stub._getServiceClient().getOptions().setTimeOutInMilliSeconds(300000);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate AutoscalerService client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new AxisFault(msg, axisFault);
        }
    }

    public static AutoscalerServiceClient getServiceClient() throws AxisFault {
        if (serviceClient == null) {
            synchronized (AutoscalerServiceClient.class) {
                if (serviceClient == null) {
                    serviceClient = new AutoscalerServiceClient(System.getProperty(AUTOSCALER_SERVICE_URL));
                }
            }
        }
        return serviceClient;
    }

    public org.apache.stratos.cloud.controller.deployment.partition.Partition[] getAvailablePartitions ()
            throws Exception {

        org.apache.stratos.cloud.controller.deployment.partition.Partition[] partitions;
        try {
             partitions = stub.getAllAvailablePartitions();

        } catch (RemoteException e) {
            String errorMsg = "Error in getting available partitions";
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        }

        return partitions;
    }
}

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

package org.apache.stratos.manager.grouping.manager;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceUnregisteredCartridgeExceptionException;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.InvalidServiceGroupException;
import org.apache.stratos.manager.exception.PersistenceManagerException;
import org.apache.stratos.manager.exception.ServiceGroupDefinitioException;
import org.apache.stratos.manager.grouping.definitions.ServiceGroupDefinition;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;

import java.rmi.RemoteException;
import java.util.List;

public class ServiceGroupingManager {

    private static Log log = LogFactory.getLog(ServiceGroupingManager.class);

    private DataInsertionAndRetrievalManager dataInsertionAndRetrievalMgr;

    public ServiceGroupingManager () {
        dataInsertionAndRetrievalMgr = new DataInsertionAndRetrievalManager();
    }

    public void deployServiceGroupDefinition (ServiceGroupDefinition serviceGroupDefinition) throws InvalidServiceGroupException,
            CloudControllerServiceUnregisteredCartridgeExceptionException, ServiceGroupDefinitioException, ADCException {


        // if any cartridges are specified in the group, they should be already deployed
        if (serviceGroupDefinition.getCartridges() != null) {
            List<String> cartridgeTypes = serviceGroupDefinition.getCartridges();

            CloudControllerServiceClient ccServiceClient = null;

            try {
                ccServiceClient = CloudControllerServiceClient.getServiceClient();

            } catch (AxisFault axisFault) {
                throw new ADCException(axisFault);
            }

            for (String cartridgeType : cartridgeTypes) {
                try {
                    if(ccServiceClient.getCartridgeInfo(cartridgeType) == null) {
                        // cartridge is not deployed, can't continue
                        throw new InvalidServiceGroupException("No Cartridge Definition found with type " + cartridgeType);
                    }
                } catch (RemoteException e) {
                    throw new ADCException(e);
                }
            }
        }

        // if any sub groups are specified in the group, they should be already deployed
        if (serviceGroupDefinition.getSubGroups() != null) {
            List<String> subGroupNames = serviceGroupDefinition.getSubGroups();
            for (String subGroupName : subGroupNames) {
                if (getServiceGroupDefinition(subGroupName) == null) {
                    // sub group not deployed, can't continue
                    throw new InvalidServiceGroupException("No Service Group Definition found with name " + subGroupName);
                }
            }
        }

        try {
            dataInsertionAndRetrievalMgr.peristServiceGroupDefinition(serviceGroupDefinition);

        } catch (PersistenceManagerException e) {
            throw new InvalidServiceGroupException(e);
        }
    }

    public ServiceGroupDefinition getServiceGroupDefinition (String serviceGroupDefinitionName) throws ServiceGroupDefinitioException {

        try {
            return dataInsertionAndRetrievalMgr.getServiceGroupDefinition(serviceGroupDefinitionName);

        } catch (PersistenceManagerException e) {
            throw new ServiceGroupDefinitioException(e);
        }
    }

    public void undeployServiceGroupDefinition (String serviceGroupDefinitionName) throws ServiceGroupDefinitioException {

        try {
            dataInsertionAndRetrievalMgr.removeServiceGroupDefinition(serviceGroupDefinitionName);

        } catch (PersistenceManagerException e) {
            throw new ServiceGroupDefinitioException(e);
        }
    }
}

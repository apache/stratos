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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceUnregisteredCartridgeExceptionException;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.InvalidServiceGroupException;
import org.apache.stratos.manager.exception.ServiceGroupDefinitioException;
import org.apache.stratos.manager.grouping.definitions.ServiceGroupDefinition;
import org.apache.stratos.manager.grouping.deployer.DefaultServiceGroupDeployer;
import org.apache.stratos.manager.grouping.deployer.ServiceGroupDeployer;


public class ServiceGroupingManager {

    //private static Log log = LogFactory.getLog(ServiceGroupingManager.class);
    private ServiceGroupDeployer serviceGroupDeployer;

    public ServiceGroupingManager () {
        serviceGroupDeployer = new DefaultServiceGroupDeployer();
    }

    public void deployServiceGroupDefinition (ServiceGroupDefinition serviceGroupDefinition) throws InvalidServiceGroupException,
            CloudControllerServiceUnregisteredCartridgeExceptionException, ServiceGroupDefinitioException, ADCException {

        serviceGroupDeployer.deployServiceGroupDefinition(serviceGroupDefinition);
    }

    public ServiceGroupDefinition getServiceGroupDefinition (String serviceGroupDefinitionName) throws ADCException, ServiceGroupDefinitioException {

        return serviceGroupDeployer.getServiceGroupDefinition(serviceGroupDefinitionName);
    }

    public void undeployServiceGroupDefinition (String serviceGroupDefinitionName) throws ADCException, ServiceGroupDefinitioException {

        serviceGroupDeployer.undeployServiceGroupDefinition(serviceGroupDefinitionName);
    }
}

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
package org.apache.stratos.cloud.controller.validate;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.InvalidPartitionException;
import org.apache.stratos.cloud.controller.interfaces.Iaas;
import org.apache.stratos.cloud.controller.pojo.IaasProvider;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;
import org.apache.stratos.messaging.domain.topology.Scope;


/**
 * The Openstack Nova {@link PartitionValidator} implementation.
 * @author nirmal
 *
 */
public class OpenstackNovaPartitionValidator implements PartitionValidator {
    
    private static final Log log = LogFactory.getLog(OpenstackNovaPartitionValidator.class);
    private IaasProvider iaasProvider;
    private Iaas iaas;

    @Override
    public IaasProvider validate(String partitionId, Properties properties) throws InvalidPartitionException {
        try {
            // validate the existence of the zone and hosts properties.
            if (properties.containsKey(Scope.region.toString())) {
                String region = properties.getProperty(Scope.region.toString());
                
                if (iaasProvider.getImage() != null && !iaasProvider.getImage().contains(region)) {

                    String msg =
                                 "Invalid Partition Detected : " + partitionId +
                                         " - Cause: Invalid Region: " + region;
                    log.error(msg);
                    throw new InvalidPartitionException(msg);
                } 
                
                iaas.isValidRegion(region);
                
                IaasProvider updatedIaasProvider = new IaasProvider(iaasProvider);
                Iaas updatedIaas = CloudControllerUtil.getIaas(updatedIaasProvider);
                updatedIaas.setIaasProvider(updatedIaasProvider);
                
                if (properties.containsKey(Scope.host.toString())) {
                    String host = properties.getProperty(Scope.host.toString());
                    iaas.isValidHost(region, host);
                    
                    updatedIaasProvider.setProperty(CloudControllerConstants.HOST, host);
                    updatedIaas.buildTemplate();
                } 
                
                return updatedIaasProvider;
                
                
            } else {

                return iaasProvider;
            }
        } catch (Exception ex) {
            String msg = "Invalid Partition Detected : "+partitionId;
            log.error(msg, ex);
            throw new InvalidPartitionException(msg, ex);
        }
    }

    @Override
    public void setIaasProvider(IaasProvider iaas) {
        this.iaasProvider = iaas;
        this.iaas = iaas.getIaas();
    }

}

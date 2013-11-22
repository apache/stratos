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
import org.apache.stratos.cloud.controller.interfaces.Iaas;
import org.apache.stratos.cloud.controller.util.IaasProvider;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;
import org.apache.stratos.messaging.domain.topology.Scope;


/**
 * The Openstack Nova {@link PartitionValidator} implementation.
 * @author nirmal
 *
 */
public class OpenstackNovaPartitionValidator implements PartitionValidator {
    
    private static final String NAME = "openstack";
    private IaasProvider iaasProvider;
    private Iaas iaas;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean validate(Properties properties) {
        // validate the existence of the zone and hosts properties.
        if (properties.containsKey(Scope.ZONE.toString())) {
            String zone = properties.getProperty(Scope.ZONE.toString());
            if (iaas.isValidZone(iaasProvider, null, zone)) {
                if (properties.containsKey(Scope.HOST.toString())) {
                    String host = properties.getProperty(Scope.HOST.toString());
                    return iaas.isValidHost(iaasProvider, zone, host);
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void setIaasProvider(IaasProvider iaas) {
        this.iaasProvider = iaas;
        this.iaas = iaas.getIaas();
    }

}

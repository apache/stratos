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

package org.apache.stratos.manager.deploy.service.multitenant.lb;

import org.apache.stratos.cloud.controller.domain.xsd.CartridgeInfo;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.AlreadySubscribedException;
import org.apache.stratos.manager.exception.UnregisteredCartridgeException;

public class LBService extends Service {

	private static final long serialVersionUID = 7202575406524382777L;

    private String loadBalancedServiceType;

    public LBService(String type, String autoscalingPolicyName, String deploymentPolicyName, int tenantId,
                     CartridgeInfo cartridgeInfo, String tenantRange, boolean isPublic) {

        super(type, autoscalingPolicyName, deploymentPolicyName, tenantId, cartridgeInfo, tenantRange, isPublic);
    }

    public void create () throws ADCException {

        try {
            setPayloadData(create(null, getCluster(), null, null, getCartridgeInfo(), getSubscriptionKey(), null));

        } catch (AlreadySubscribedException e) {
            throw new ADCException(e);
        }
    }

    @Override
    public void deploy(Properties properties) throws ADCException, UnregisteredCartridgeException {

        //register the service
        register(getCartridgeInfo(), getCluster(), getPayloadData(), getAutoscalingPolicyName(), getDeploymentPolicyName(),
                properties, null);
    }

    public String getLoadBalancedServiceType() {
        return loadBalancedServiceType;
    }

    public void setLoadBalancedServiceType(String loadBalancedServiceType) {
        this.loadBalancedServiceType = loadBalancedServiceType;
    }
}

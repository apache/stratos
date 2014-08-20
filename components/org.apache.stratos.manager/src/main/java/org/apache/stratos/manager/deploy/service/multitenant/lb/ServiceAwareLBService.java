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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.stub.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.stub.pojo.ClusterContext;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.manager.client.AutoscalerServiceClient;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.dao.Cluster;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.AlreadySubscribedException;
import org.apache.stratos.manager.exception.UnregisteredCartridgeException;
import org.apache.stratos.manager.payload.PayloadData;
import org.apache.stratos.manager.repository.Repository;
import org.apache.stratos.manager.subscriber.Subscriber;
import org.apache.stratos.manager.subscription.utils.CartridgeSubscriptionUtils;
import org.apache.stratos.manager.utils.CartridgeConstants;

import java.rmi.RemoteException;
import java.util.Map;

public class ServiceAwareLBService extends LBService {

	private static final long serialVersionUID = -4107281204555031986L;

	public ServiceAwareLBService(String type, String autoscalingPolicyName, String deploymentPolicyName, int tenantId,
                                 CartridgeInfo cartridgeInfo, String tenantRange) {

        super(type, autoscalingPolicyName, deploymentPolicyName, tenantId, cartridgeInfo, tenantRange);
    }

    private static Log log = LogFactory.getLog(ServiceAwareLBService.class);

    private boolean serviceAwareLBExists;

    public PayloadData create (String alias, Cluster cluster, Subscriber subscriber, Repository repository, CartridgeInfo cartridgeInfo,
                               String subscriptionKey, Map<String, String> customPayloadEntries) throws ADCException, AlreadySubscribedException {

        // call the relevant method to get the cluster id, using deployment policy and type
        String clusterId = null;

        try {
            clusterId = AutoscalerServiceClient.getServiceClient().getServiceLBClusterId(getLoadBalancedServiceType(), getDeploymentPolicyName());

        } catch (Exception e) {
            log.error("Error occurred in retrieving Service LB cluster id" + e.getMessage());
            throw new ADCException(e);
        }

        if (clusterId != null) {

            //set the cluster id to Cluster object
            cluster.setClusterDomain(clusterId);
            if (log.isDebugEnabled()) {
                log.debug("Set existing Service LB cluster id " + clusterId);
            }
            serviceAwareLBExists = true;

            //get the hostname for this cluster and set it
            ClusterContext clusterContext;
            try {
                clusterContext = CloudControllerServiceClient.getServiceClient().getClusterContext(clusterId);

            } catch (RemoteException e) {
                log.error("Error occurred in retrieving Cluster Context for Service LB ", e);
                throw new ADCException(e);
            }

            if (clusterContext != null) {
                cluster.setHostName(clusterContext.getHostName());
                if (log.isDebugEnabled()) {
                    log.debug("Set existing Service LB hostname " + clusterContext.getHostName());
                }
            }

            return null;

        } else {

            // set cluster domain
            cluster.setClusterDomain(generateClusterId(getLoadBalancedServiceType(), cartridgeInfo.getType()));
            // set hostname
            cluster.setHostName(generateHostName(getLoadBalancedServiceType(), cartridgeInfo.getHostName()));

            PayloadData serviceLevelLbPayloadData = createPayload(cartridgeInfo, subscriptionKey, subscriber, cluster,
                    repository, alias, customPayloadEntries);

            // add payload entry for load balanced service type
            serviceLevelLbPayloadData.add(CartridgeConstants.LOAD_BALANCED_SERVICE_TYPE, getLoadBalancedServiceType());
            return serviceLevelLbPayloadData;
        }
    }

    protected String generateClusterId (String loadBalancedServiceType, String cartridgeType) {

        String clusterId = cartridgeType + "." + loadBalancedServiceType + "." + getCartridgeInfo().getHostName() + ".domain";
        // limit the cartridge alias to 30 characters in length
        if (clusterId.length() > 30) {
            clusterId = CartridgeSubscriptionUtils.limitLengthOfString(clusterId, 30);
        }

        return clusterId;
    }

    protected String generateHostName (String loadBalancedServiceType, String cartridgeDefinitionHostName) {

        return getCartridgeInfo().getType() + "." + loadBalancedServiceType + "." + cartridgeDefinitionHostName;
    }

    public void register(CartridgeInfo cartridgeInfo, Cluster cluster, PayloadData payloadData, String autoscalePolicyName, String deploymentPolicyName, Properties properties) throws ADCException, UnregisteredCartridgeException {

        if (!serviceAwareLBExists) {
            super.register(cartridgeInfo, cluster, payloadData, autoscalePolicyName, deploymentPolicyName, properties, null);

        }else {
            log.info("Service Aware LB already exists for cartridge type: " + getLoadBalancedServiceType() + ", deployment policy: " + getDeploymentPolicyName());
        }
    }
}

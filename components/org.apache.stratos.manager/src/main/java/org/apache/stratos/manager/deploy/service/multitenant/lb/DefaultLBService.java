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
import org.apache.stratos.cloud.controller.domain.xsd.CartridgeInfo;
import org.apache.stratos.cloud.controller.domain.xsd.ClusterContext;
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

import java.rmi.RemoteException;
import java.util.Map;

public class DefaultLBService extends LBService {

	private static final long serialVersionUID = 8650714821857129052L;
	private static Log log = LogFactory.getLog(DefaultLBService.class);
    private boolean defaultLBServiceExists = false;

    public DefaultLBService (String type, String autoscalingPolicyName, String deploymentPolicyName, int tenantId,
                            CartridgeInfo cartridgeInfo, String tenantRange, boolean isPublic) {

        super(type, autoscalingPolicyName, deploymentPolicyName, tenantId, cartridgeInfo, tenantRange, isPublic);
    }

    public PayloadData create(String alias, Cluster cluster, Subscriber subscriber, Repository repository, CartridgeInfo cartridgeInfo,
                              String subscriptionKey, Map<String, String> customPayloadEntries)
            throws ADCException, AlreadySubscribedException {

        // call the relevant method to get the cluster id, using deployment policy
        String clusterId = null;
        try {
            clusterId = AutoscalerServiceClient.getServiceClient().getDefaultLBClusterId(getDeploymentPolicyName());
        } catch (Exception e) {
            log.error("Error occurred in retrieving default LB cluster id" + e.getMessage());
            throw new ADCException(e);
        }

        if (clusterId != null) {
            //set the cluster id to Cluster object
            cluster.setClusterDomain(clusterId);
            if (log.isDebugEnabled()) {
                log.debug("Set existing default LB cluster id " + clusterId);
            }
            defaultLBServiceExists = true;

            //get the hostname for this cluster and set it
            ClusterContext clusterContext;
            try {
                clusterContext = CloudControllerServiceClient.getServiceClient().getClusterContext(clusterId);

            } catch (RemoteException e) {
                log.error("Error occurred in retrieving Cluster Context for default LB ", e);
                throw new ADCException(e);
            }

            if (clusterContext != null) {
                cluster.setHostName(clusterContext.getHostName());
                if (log.isDebugEnabled()) {
                    log.debug("Set existing default LB hostname " + clusterContext.getHostName());
                }
            }

            return null;

        } else {
            // set cluster domain
            cluster.setClusterDomain(generateClusterId(null, cartridgeInfo.getType()));
            // set hostname
            cluster.setHostName(generateHostName(null, cartridgeInfo.getHostName()));

            return createPayload(cartridgeInfo, subscriptionKey, subscriber, cluster, repository, alias, customPayloadEntries);
        }
    }

    public void register(CartridgeInfo cartridgeInfo, Cluster cluster, PayloadData payloadData, String autoscalePolicyName, String deploymentPolicyName, Properties properties) throws ADCException, UnregisteredCartridgeException {

        //log.info("Register service with payload data ["+payloadData+"] ");
        if (!defaultLBServiceExists) {
            super.register(cartridgeInfo, cluster, payloadData, autoscalePolicyName, deploymentPolicyName, properties, null);
        }else {
            log.info("Default LB already exists for deployment policy: " + getDeploymentPolicyName());
        }
    }
}

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

package org.apache.stratos.manager.lb.category;

import java.rmi.RemoteException;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.stub.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.manager.client.AutoscalerServiceClient;
import org.apache.stratos.manager.dao.Cluster;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.AlreadySubscribedException;
import org.apache.stratos.manager.exception.UnregisteredCartridgeException;
import org.apache.stratos.manager.payload.PayloadData;
import org.apache.stratos.manager.repository.Repository;
import org.apache.stratos.manager.subscriber.Subscriber;
import org.apache.stratos.manager.subscription.utils.CartridgeSubscriptionUtils;

public class DefaultLoadBalancerCategory extends LoadBalancerCategory {

    private boolean defaultLBExists;

    private static Log log = LogFactory.getLog(DefaultLoadBalancerCategory.class);
    
    public PayloadData create(String alias, Cluster cluster, Subscriber subscriber, Repository repository, CartridgeInfo cartridgeInfo,
                              String subscriptionKey, Map<String, String> customPayloadEntries)
            throws ADCException, AlreadySubscribedException {

        // call the relevant method to get the cluster id, using deployment policy
        String clusterId = null;
		try {
			clusterId = AutoscalerServiceClient.getServiceClient().getDefaultLBClusterId(getDeploymentPolicyName());
		} catch (Exception e) {			
			log.error("Error occurred in retrieving default LB cluster id.  " + e.getMessage());
			throw new ADCException(e);
		}

        if (clusterId != null) {

            //set the cluster id to Cluster object
        	cluster.setClusterDomain(clusterId);
            defaultLBExists = true;
            //need to check if we can get the host name as well..

        } else {
            clusterId = alias + "." + cartridgeInfo.getType() + ".domain";

            // limit the cartridge alias to 30 characters in length
            if (clusterId.length() > 30) {
                clusterId = CartridgeSubscriptionUtils.limitLengthOfString(clusterId, 30);
            }
            cluster.setClusterDomain(clusterId);
            // set hostname
            cluster.setHostName(alias + "." + cluster.getHostName());
        }

        return createPayload(cartridgeInfo, subscriptionKey, subscriber,
                cluster, repository, alias, customPayloadEntries);
    }

    public void register(CartridgeInfo cartridgeInfo, Cluster cluster, PayloadData payloadData, String autoscalePolicyName, String deploymentPolicyName, Properties properties) throws ADCException, UnregisteredCartridgeException {

        if (!isDefaultLBExists()) {
            log.info("Payload: " + payloadData.getCompletePayloadData().toString());
            super.register(cartridgeInfo, cluster, payloadData, autoscalePolicyName, deploymentPolicyName, properties);
        }else {
        	log.info(" Default LB exists... Not registering...");
        }
    }


    public boolean isDefaultLBExists() {
        return defaultLBExists;
    }

    public void setDefaultLBExists(boolean defaultLBExists) {
        this.defaultLBExists = defaultLBExists;
    }
}

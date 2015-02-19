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
 * KIND, either express or implied.  TcSee the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.autoscaler.client;

import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.pojo.ApplicationClusterContext;
import org.apache.stratos.autoscaler.exception.cartridge.CartridgeInformationException;
import org.apache.stratos.autoscaler.exception.cartridge.SpawningException;
import org.apache.stratos.autoscaler.exception.cartridge.TerminationException;
import org.apache.stratos.autoscaler.exception.partition.PartitionValidationException;
import org.apache.stratos.autoscaler.util.AutoscalerConstants;
import org.apache.stratos.autoscaler.util.AutoscalerObjectConverter;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.cloud.controller.stub.*;
import org.apache.stratos.cloud.controller.stub.domain.CartridgeInfo;
import org.apache.stratos.cloud.controller.stub.domain.InstanceContext;
import org.apache.stratos.cloud.controller.stub.domain.MemberContext;
import org.apache.stratos.cloud.controller.stub.domain.Partition;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.constants.StratosConstants;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class will call cloud controller web service to take the action decided by Autoscaler
 */
public class CloudControllerClient {

    private static final Log log = LogFactory.getLog(CloudControllerClient.class);
    private static CloudControllerServiceStub stub;

    /* An instance of a CloudControllerClient is created when the class is loaded. 
     * Since the class is loaded only once, it is guaranteed that an object of 
     * CloudControllerClient is created only once. Hence it is singleton.
     */
    private static class InstanceHolder {
        private static final CloudControllerClient INSTANCE = new CloudControllerClient();
    }

    public static CloudControllerClient getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private CloudControllerClient() {
        try {
            XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
            int port = conf.getInt("autoscaler.cloudController.port", AutoscalerConstants.CLOUD_CONTROLLER_DEFAULT_PORT);
            String hostname = conf.getString("autoscaler.cloudController.hostname", "localhost");
            String epr = "https://" + hostname + ":" + port + "/" + AutoscalerConstants.CLOUD_CONTROLLER_SERVICE_SFX;
            int cloudControllerClientTimeout = conf.getInt("autoscaler.cloudController.clientTimeout", 180000);

            stub = new CloudControllerServiceStub(epr);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, cloudControllerClientTimeout);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, cloudControllerClientTimeout);
        } catch (Exception e) {
            log.error("Could not initialize cloud controller client", e);
        }
    }
    
  
    public synchronized MemberContext startInstance(Partition partition,
                                                    String clusterId, String clusterInstanceId,
                                                    String networkPartitionId, boolean isPrimary,
                                                    int minMemberCount) throws SpawningException {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Trying to spawn an instance via cloud controller: " +
                                "[cluster] %s [partition] %s [network-partition-id] %s",
                        clusterId, partition.getId(), networkPartitionId));
            }

            XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
            long expiryTime = conf.getLong(StratosConstants.OBSOLETED_MEMBER_EXPIRY_TIMEOUT, 86400000);
            if (log.isDebugEnabled()) {
                log.debug("Member obsolete expiry time is set to: " + expiryTime);
            }

            InstanceContext instanceContext = new InstanceContext();
            instanceContext.setClusterId(clusterId);
            instanceContext.setClusterInstanceId(clusterInstanceId);
            instanceContext.setPartition(partition);
            instanceContext.setInitTime(System.currentTimeMillis());
            instanceContext.setObsoleteExpiryTime(expiryTime);
            instanceContext.setNetworkPartitionId(networkPartitionId);

            Properties memberContextProps = new Properties();
            Property isPrimaryProp = new Property();
            isPrimaryProp.setName("PRIMARY");
            isPrimaryProp.setValue(String.valueOf(isPrimary));

            Property minCountProp = new Property();
            minCountProp.setName(StratosConstants.MIN_COUNT);
            minCountProp.setValue(String.valueOf(minMemberCount));

            memberContextProps.addProperty(isPrimaryProp);
            memberContextProps.addProperty(minCountProp);
            instanceContext.setProperties(AutoscalerUtil.toStubProperties(memberContextProps));

            long startTime = System.currentTimeMillis();
            MemberContext memberContext = stub.startInstance(instanceContext);
            if (log.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                log.debug(String.format("Service call startInstance() returned in %dms", (endTime - startTime)));
            }
            return memberContext;
        } catch (CloudControllerServiceCartridgeNotFoundExceptionException e) {
            String message = e.getFaultMessage().getCartridgeNotFoundException().getMessage();
            log.error(message, e);
            throw new SpawningException(message, e);
        } catch (RemoteException e) {
            log.error(e.getMessage(), e);
            throw new SpawningException(e.getMessage(), e);
        } catch (CloudControllerServiceInvalidIaasProviderExceptionException e) {
            String message = e.getFaultMessage().getInvalidIaasProviderException().getMessage();
            log.error(message, e);
            throw new SpawningException(message, e);
        } catch (CloudControllerServiceCloudControllerExceptionException e) {
            String message = e.getMessage();
            log.error(message, e);
            throw new SpawningException(message, e);
        }
    }

    
    public synchronized void createApplicationClusters(String appId,
                                                       ApplicationClusterContext[] applicationClusterContexts) {
        List<org.apache.stratos.cloud.controller.stub.domain.ApplicationClusterContext> contextDTOs =
                                        new ArrayList<org.apache.stratos.cloud.controller.stub.domain.ApplicationClusterContext>();
        if(applicationClusterContexts != null) {
            for (ApplicationClusterContext applicationClusterContext : applicationClusterContexts) {
                if(applicationClusterContext != null) {
                    org.apache.stratos.cloud.controller.stub.domain.ApplicationClusterContext dto = new org.apache.stratos.cloud.controller.stub.domain.ApplicationClusterContext();
                    dto.setClusterId(applicationClusterContext.getClusterId());
                    dto.setAutoscalePolicyName(applicationClusterContext.getAutoscalePolicyName());
                    dto.setDeploymentPolicyName(applicationClusterContext.getDeploymentPolicyName());
                    dto.setCartridgeType(applicationClusterContext.getCartridgeType());
                    dto.setHostName(applicationClusterContext.getHostName());
                    dto.setTenantRange(applicationClusterContext.getTenantRange());
                    dto.setTextPayload(applicationClusterContext.getTextPayload());
                    dto.setLbCluster(applicationClusterContext.isLbCluster());
                    dto.setProperties(AutoscalerUtil.toStubProperties(applicationClusterContext.getProperties()));
                    dto.setDependencyClusterIds(applicationClusterContext.getDependencyClusterIds());
                    contextDTOs.add(dto);
                }
            }
        }

        org.apache.stratos.cloud.controller.stub.domain.ApplicationClusterContext[] applicationClusterContextDTOs =
                new org.apache.stratos.cloud.controller.stub.domain.ApplicationClusterContext[contextDTOs.size()];
        contextDTOs.toArray(applicationClusterContextDTOs);
        try {
            stub.createApplicationClusters(appId, applicationClusterContextDTOs);
        } catch (RemoteException e) {
            String msg = e.getMessage();
            log.error(msg, e);
            //throw new TerminationException(msg, e);
        } catch (CloudControllerServiceApplicationClusterRegistrationExceptionException e) {
            String msg = e.getMessage();
            log.error(msg, e);
        }


    }
   
    
}

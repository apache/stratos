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

package org.apache.stratos.adc.mgt.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.internal.DataHolder;
import org.apache.stratos.autoscaler.stub.AutoScalerServiceStub;

import java.rmi.RemoteException;

public class AutoscalerServiceClient {

    private AutoScalerServiceStub stub;

    private static final Log log = LogFactory.getLog(AutoscalerServiceClient.class);
    private static volatile AutoscalerServiceClient serviceClient;
    private static final String AUTOSCALER_SERVICE_URL = "autoscaler.service.url";

    public AutoscalerServiceClient(String epr) throws AxisFault {

        ConfigurationContext clientConfigContext = DataHolder.getClientConfigContext();
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

    /*public org.apache.stratos.cloud.controller.deployment.partition.Partition[] getAvailablePartitions ()
            throws Exception {

        org.apache.stratos.cloud.controller.deployment.partition.Partition[] partitions;
        try {
            partitions = stub.get;

        } catch (RemoteException e) {
            String errorMsg = "Error in getting available partitions";
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        }

        return partitions;
    }*/

    /*public org.apache.stratos.cloud.controller.deployment.partition.Partition getPartition (String partitionId)
            throws Exception{

        org.apache.stratos.cloud.controller.deployment.partition.Partition partition;
        try {
            partition = stub.getPartition(partitionId);

        } catch (RemoteException e) {
            String errorMsg = "Error in getting available partitions";
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        }

        return partition;
    }*/

    /*public org.apache.stratos.cloud.controller.deployment.partition.Partition [] getPartitions (String deploymentPolicyId,
                                                                                            String partitionGroupId)
            throws Exception{

        org.apache.stratos.cloud.controller.deployment.partition.Partition[] partitions;
        try {
            partitions = stub.getPartitions(deploymentPolicyId, partitionGroupId);

        } catch (RemoteException e) {
            String errorMsg = "Error in getting available partitions";
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        }

        return partitions;
    }

    public org.apache.stratos.autoscaler.partition.xsd.PartitionGroup [] getPartitionGroups (String deploymentPolicyId)
            throws Exception{

        org.apache.stratos.autoscaler.partition.xsd.PartitionGroup [] partitionGroups;
        try {
            partitionGroups = stub.getPartitionGroups(deploymentPolicyId);

        } catch (RemoteException e) {
            String errorMsg = "Error in getting available partitions";
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        }

        return partitionGroups;
    }*/

    public org.apache.stratos.autoscaler.policy.model.AutoscalePolicy[] getAutoScalePolicies ()
            throws Exception {

        org.apache.stratos.autoscaler.policy.model.AutoscalePolicy[] autoscalePolicies;
        try {
            autoscalePolicies = stub.getAllAutoScalingPolicy();

        } catch (RemoteException e) {
            String errorMsg = "Error in getting available partitions";
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        }

        return autoscalePolicies;
    }

    /*public org.apache.stratos.autoscaler.policy.model.AutoscalePolicy getAutoScalePolicy (String autoscalingPolicyId)
            throws Exception {

        org.apache.stratos.autoscaler.policy.model.AutoscalePolicy autoscalePolicy;
        try {
            autoscalePolicy = stub.getAutoscalingPolicy(autoscalingPolicyId);

        } catch (RemoteException e) {
            String errorMsg = "Error in getting available partitions";
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        }

        return autoscalePolicy;
    }*/

    public org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy [] getDeploymentPolicies()
            throws Exception {

        org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy[] deploymentPolicies;
        try {
            deploymentPolicies = stub.getAllDeploymentPolicies();

        } catch (RemoteException e) {
            String errorMsg = "Error in getting available deployment policies";
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        }

        return deploymentPolicies;
    }

    public org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy [] getDeploymentPolicies(String cartridgeType)
            throws Exception {

        org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy[] deploymentPolicies;
        try {
            deploymentPolicies = stub.getValidDeploymentPoliciesforCartridge(cartridgeType);

        } catch (RemoteException e) {
            String errorMsg = "Error in getting available deployment policies";
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        }

        return deploymentPolicies;
    }

    /*public org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy getDeploymentPolicy (String deploymentPolicyId)
            throws Exception {

        org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy deploymentPolicy;
        try {
            deploymentPolicy = stub.getDeploymentPolicy(deploymentPolicyId);

        } catch (RemoteException e) {
            String errorMsg = "Error in getting available deployment policies";
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        }

        return deploymentPolicy;
    }*/
}

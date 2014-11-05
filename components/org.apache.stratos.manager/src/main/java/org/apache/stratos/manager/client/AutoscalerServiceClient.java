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

package org.apache.stratos.manager.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.stub.*;
import org.apache.stratos.autoscaler.stub.policy.model.DeploymentPolicy;
import org.apache.stratos.autoscaler.stub.kubernetes.KubernetesGroup;
import org.apache.stratos.autoscaler.stub.kubernetes.KubernetesHost;
import org.apache.stratos.autoscaler.stub.kubernetes.KubernetesMaster;
import org.apache.stratos.autoscaler.stub.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.applications.pojo.xsd.ApplicationContext;
import org.apache.stratos.autoscaler.pojo.xsd.ServiceGroup;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.manager.internal.DataHolder;
import org.apache.stratos.manager.utils.CartridgeConstants;

import java.rmi.RemoteException;

public class AutoscalerServiceClient {

    private AutoScalerServiceStub stub;

    private static final Log log = LogFactory.getLog(AutoscalerServiceClient.class);
    private static volatile AutoscalerServiceClient serviceClient;

    public AutoscalerServiceClient(String epr) throws AxisFault {


        String autosclaerSocketTimeout =
                System.getProperty(CartridgeConstants.AUTOSCALER_SOCKET_TIMEOUT) == null ? "300000" : System.getProperty(CartridgeConstants.AUTOSCALER_SOCKET_TIMEOUT);
        String autosclaerConnectionTimeout =
                System.getProperty(CartridgeConstants.AUTOSCALER_CONNECTION_TIMEOUT) == null ? "300000" : System.getProperty(CartridgeConstants.AUTOSCALER_CONNECTION_TIMEOUT);

        ConfigurationContext clientConfigContext = DataHolder.getClientConfigContext();
        try {
            stub = new AutoScalerServiceStub(clientConfigContext, epr);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, new Integer(autosclaerSocketTimeout));
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, new Integer(autosclaerConnectionTimeout));

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate autoscaler service client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new AxisFault(msg, axisFault);
        }
    }

    public static AutoscalerServiceClient getServiceClient() throws AxisFault {
        if (serviceClient == null) {
            synchronized (AutoscalerServiceClient.class) {
                if (serviceClient == null) {
                    serviceClient = new AutoscalerServiceClient(System.getProperty(CartridgeConstants.AUTOSCALER_SERVICE_URL));
                }
            }
        }
        return serviceClient;
    }

    public Partition[] getAvailablePartitions() throws RemoteException {

        Partition[] partitions;
        partitions = stub.getAllAvailablePartitions();

        return partitions;
    }

    public void undeployServiceGroupDefinition (String serviceGroupName)
            throws RemoteException, AutoScalerServiceAutoScalerExceptionException {
        stub.undeployServiceGroup(serviceGroupName);
    }

    public Partition getPartition(
            String partitionId) throws RemoteException {

        Partition partition;
        partition = stub.getPartition(partitionId);

        return partition;
    }

    public Partition[] getPartitionsOfGroup(
            String deploymentPolicyId, String partitionGroupId)
            throws RemoteException {

        Partition[] partitions;
        partitions = stub.getPartitionsOfGroup(deploymentPolicyId,
                partitionGroupId);

        return partitions;
    }

    public Partition[]
    getPartitionsOfDeploymentPolicy(
            String deploymentPolicyId) throws RemoteException {

        Partition[] partitions;
        partitions = stub.getPartitionsOfDeploymentPolicy(deploymentPolicyId);

        return partitions;
    }

    public org.apache.stratos.autoscaler.stub.partition.PartitionGroup[] getPartitionGroups(
            String deploymentPolicyId) throws RemoteException {

        org.apache.stratos.autoscaler.stub.partition.PartitionGroup[] partitionGroups;
        partitionGroups = stub.getPartitionGroups(deploymentPolicyId);

        return partitionGroups;
    }

    public org.apache.stratos.autoscaler.stub.policy.model.AutoscalePolicy[] getAutoScalePolicies()
            throws RemoteException {

        org.apache.stratos.autoscaler.stub.policy.model.AutoscalePolicy[] autoscalePolicies;
        autoscalePolicies = stub.getAllAutoScalingPolicy();

        return autoscalePolicies;
    }

    public org.apache.stratos.autoscaler.stub.policy.model.AutoscalePolicy getAutoScalePolicy(
            String autoscalingPolicyId) throws RemoteException {

        org.apache.stratos.autoscaler.stub.policy.model.AutoscalePolicy autoscalePolicy;
        autoscalePolicy = stub.getAutoscalingPolicy(autoscalingPolicyId);

        return autoscalePolicy;
    }

    public org.apache.stratos.autoscaler.stub.policy.model.DeploymentPolicy[] getDeploymentPolicies()
            throws RemoteException {

        org.apache.stratos.autoscaler.stub.policy.model.DeploymentPolicy[] deploymentPolicies;
        deploymentPolicies = stub.getAllDeploymentPolicies();

        return deploymentPolicies;
    }

    public org.apache.stratos.autoscaler.stub.policy.model.DeploymentPolicy[] getDeploymentPolicies(
            String cartridgeType) throws RemoteException {

        org.apache.stratos.autoscaler.stub.policy.model.DeploymentPolicy[] deploymentPolicies;
        deploymentPolicies = stub
                .getValidDeploymentPoliciesforCartridge(cartridgeType);

        return deploymentPolicies;
    }

    public void checkLBExistenceAgainstPolicy(String clusterId, String deploymentPolicyId) throws RemoteException,
            AutoScalerServiceNonExistingLBExceptionException {
        stub.checkLBExistenceAgainstPolicy(clusterId, deploymentPolicyId);
    }

    public boolean checkDefaultLBExistenceAgainstPolicy(
            String deploymentPolicyId) throws RemoteException {
        return stub.checkDefaultLBExistenceAgainstPolicy(deploymentPolicyId);
    }

    public boolean checkServiceLBExistenceAgainstPolicy(String serviceName, String deploymentPolicyId) throws RemoteException {
        return stub.checkServiceLBExistenceAgainstPolicy(serviceName, deploymentPolicyId);
    }

    public org.apache.stratos.autoscaler.stub.policy.model.DeploymentPolicy getDeploymentPolicy(String deploymentPolicyId) throws RemoteException {

        org.apache.stratos.autoscaler.stub.policy.model.DeploymentPolicy deploymentPolicy;
        deploymentPolicy = stub.getDeploymentPolicy(deploymentPolicyId);

        return deploymentPolicy;
    }

    public boolean deployDeploymentPolicy(DeploymentPolicy deploymentPolicy) throws RemoteException,
            AutoScalerServiceInvalidPolicyExceptionException {

        return stub.addDeploymentPolicy(deploymentPolicy);

    }

    public boolean updateDeploymentPolicy(DeploymentPolicy deploymentPolicy) throws RemoteException,
            AutoScalerServiceInvalidPolicyExceptionException {

        return stub.updateDeploymentPolicy(deploymentPolicy);

    }

    public boolean deployAutoscalingPolicy(AutoscalePolicy autoScalePolicy) throws RemoteException,
            AutoScalerServiceInvalidPolicyExceptionException {

        return stub.addAutoScalingPolicy(autoScalePolicy);
    }

    public boolean updateAutoscalingPolicy(AutoscalePolicy autoScalePolicy) throws RemoteException,
            AutoScalerServiceInvalidPolicyExceptionException {

        return stub.updateAutoScalingPolicy(autoScalePolicy);
    }

    public boolean deployPartition(Partition partition) throws RemoteException,
            AutoScalerServiceInvalidPartitionExceptionException {

        return stub.addPartition(partition);

    }

    public String getDefaultLBClusterId(String deploymentPolicy) throws RemoteException {
        return stub.getDefaultLBClusterId(deploymentPolicy);
    }


    public ServiceGroup getServiceGroup(String serviceGroupDefinitionName) throws RemoteException {
        return stub.getServiceGroup(serviceGroupDefinitionName);
    }

    public void deployServiceGroup(ServiceGroup serviceGroup) throws AutoScalerServiceInvalidServiceGroupExceptionException, RemoteException {
        stub.deployServiceGroup(serviceGroup);
    }

    public void deployApplication (ApplicationContext applicationContext) throws
            AutoScalerServiceApplicationDefinitionExceptionException, RemoteException {
        stub.deployApplicationDefinition(applicationContext);
    }

    public void undeployApplication (String applicationId, int tenantId, String tenantDomain) throws
            AutoScalerServiceApplicationDefinitionExceptionException, RemoteException {
        stub.unDeployApplicationDefinition(applicationId, tenantId, tenantDomain);
    }


    public String getServiceLBClusterId(String serviceType, String deploymentPolicy) throws RemoteException {
        return stub.getServiceLBClusterId(serviceType, deploymentPolicy);
    }

    public boolean deployKubernetesGroup(KubernetesGroup kubernetesGroup) throws RemoteException,
            AutoScalerServiceInvalidKubernetesGroupExceptionException {
        return stub.addKubernetesGroup(kubernetesGroup);
    }

    public boolean deployKubernetesHost(String kubernetesGroupId, KubernetesHost kubernetesHost)
            throws RemoteException, AutoScalerServiceInvalidKubernetesHostExceptionException,
            AutoScalerServiceNonExistingKubernetesGroupExceptionException {

        return stub.addKubernetesHost(kubernetesGroupId, kubernetesHost);
    }

    public boolean updateKubernetesMaster(KubernetesMaster kubernetesMaster)
            throws RemoteException, AutoScalerServiceInvalidKubernetesMasterExceptionException,
            AutoScalerServiceNonExistingKubernetesMasterExceptionException {
        return stub.updateKubernetesMaster(kubernetesMaster);
    }

    public KubernetesGroup[] getAvailableKubernetesGroups() throws RemoteException {
        return stub.getAllKubernetesGroups();
    }

    public KubernetesGroup getKubernetesGroup(String kubernetesGroupId)
            throws RemoteException, AutoScalerServiceNonExistingKubernetesGroupExceptionException {
        return stub.getKubernetesGroup(kubernetesGroupId);
    }

    public boolean undeployKubernetesGroup(String kubernetesGroupId)
            throws RemoteException, AutoScalerServiceNonExistingKubernetesGroupExceptionException {
        return stub.removeKubernetesGroup(kubernetesGroupId);
    }

    public boolean undeployKubernetesHost(String kubernetesHostId)
            throws RemoteException, AutoScalerServiceNonExistingKubernetesHostExceptionException {
        return stub.removeKubernetesHost(kubernetesHostId);
    }

    public KubernetesHost[] getKubernetesHosts(String kubernetesGroupId)
            throws RemoteException, AutoScalerServiceNonExistingKubernetesGroupExceptionException {
        return stub.getHostsForKubernetesGroup(kubernetesGroupId);
    }

    public KubernetesMaster getKubernetesMaster(String kubernetesGroupId)
            throws RemoteException, AutoScalerServiceNonExistingKubernetesGroupExceptionException {
        return stub.getMasterForKubernetesGroup(kubernetesGroupId);
    }

    public boolean updateKubernetesHost(KubernetesHost kubernetesHost)
            throws RemoteException, AutoScalerServiceInvalidKubernetesHostExceptionException,
            AutoScalerServiceNonExistingKubernetesHostExceptionException {
        return stub.updateKubernetesHost(kubernetesHost);
    }
    
    public void updateClusterMonitor(String clusterId, Properties properties) throws RemoteException, AutoScalerServiceInvalidArgumentExceptionException {
        stub.updateClusterMonitor(clusterId, properties);
    }
}

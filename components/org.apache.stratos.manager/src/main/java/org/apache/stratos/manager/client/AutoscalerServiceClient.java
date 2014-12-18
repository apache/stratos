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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition;
import org.apache.stratos.autoscaler.stub.pojo.ApplicationContext;
import org.apache.stratos.autoscaler.stub.pojo.ServiceGroup;
import org.apache.stratos.autoscaler.stub.*;
import org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy;
import org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy;
import org.apache.stratos.common.Properties;
import org.apache.stratos.manager.internal.DataHolder;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;
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

    public void undeployServiceGroupDefinition(String serviceGroupName)
            throws RemoteException, AutoScalerServiceAutoScalerExceptionException {
        stub.removeServiceGroup(serviceGroupName);
    }

    public ApplicationLevelNetworkPartition[] getApplicationLevelNetworkPartition(
            String deploymentPolicyId) throws RemoteException {

        ApplicationLevelNetworkPartition[] partitionGroups;
//        partitionGroups = stub.getPartitionGroups(deploymentPolicyId);

//        return partitionGroups;
        //FIXME add a method to autoscaler to return Application Level NetworkPartitions
        return null;
    }

    public org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy[] getAutoScalePolicies()
            throws RemoteException {

        org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy[] autoscalePolicies;
        autoscalePolicies = stub.getAutoScalingPolicies();

        return autoscalePolicies;
    }

    public org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy getAutoScalePolicy(
            String autoscalingPolicyId) throws RemoteException {

        org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy autoscalePolicy;
        autoscalePolicy = stub.getAutoscalingPolicy(autoscalingPolicyId);

        return autoscalePolicy;
    }

    public org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy getDeploymentPolicy(String deploymentPolicyId) throws RemoteException {

        org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy deploymentPolicy;
        deploymentPolicy = stub.getDeploymentPolicy(deploymentPolicyId);

        return deploymentPolicy;
    }

    public void addApplication(ApplicationContext applicationContext) throws AutoScalerServiceApplicationDefinitionExceptionException, RemoteException {
        stub.addApplication(applicationContext);
    }

    public ApplicationContext getApplication(String applicationId) throws RemoteException {
        return stub.getApplication(applicationId);
    }

    public ApplicationContext[] getApplications() throws RemoteException {
        return stub.getApplications();
    }

    public boolean deployApplication(String applicationId, DeploymentPolicy deploymentPolicy) throws RemoteException,
            AutoScalerServiceInvalidPolicyExceptionException, AutoScalerServiceApplicationDefinitionExceptionException {

        return stub.deployApplication(applicationId, deploymentPolicy);

    }

    public void undeployApplication(String applicationId) throws
            AutoScalerServiceApplicationDefinitionExceptionException, RemoteException {
        stub.undeployApplication(applicationId);
    }

    public void deleteApplication(String applicationId) throws RemoteException {
        stub.deleteApplication(applicationId);
    }

    public boolean deployAutoscalingPolicy(AutoscalePolicy autoScalePolicy) throws RemoteException,
            AutoScalerServiceInvalidPolicyExceptionException {

        return stub.addAutoScalingPolicy(autoScalePolicy);
    }

    public boolean updateAutoscalingPolicy(AutoscalePolicy autoScalePolicy) throws RemoteException,
            AutoScalerServiceInvalidPolicyExceptionException {

        return stub.updateAutoScalingPolicy(autoScalePolicy);
    }

    public ServiceGroup getServiceGroup(String serviceGroupDefinitionName) throws RemoteException {
        return stub.getServiceGroup(serviceGroupDefinitionName);
    }

    public ServiceGroup[] getServiceGroups() throws RemoteException, AutoScalerServiceAutoScalerExceptionException {
        return stub.getServiceGroups();
    }

    public void addServiceGroup(ServiceGroup serviceGroup) throws AutoScalerServiceInvalidServiceGroupExceptionException, RemoteException {
        stub.addServiceGroup(serviceGroup);
    }

    public void removeServiceGroup(String groupName) throws RemoteException {
        stub.removeServiceGroup(groupName);
    }

    public void updateClusterMonitor(String clusterId, Properties properties) throws RemoteException, AutoScalerServiceInvalidArgumentExceptionException {
        stub.updateClusterMonitor(clusterId, ApplicationManagementUtil.toAutoscalerStubProperties(properties));
    }
}

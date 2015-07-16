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

package org.apache.stratos.common.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.stub.*;
import org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy;
import org.apache.stratos.autoscaler.stub.deployment.policy.ApplicationPolicy;
import org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.stub.pojo.ApplicationContext;
import org.apache.stratos.autoscaler.stub.pojo.ServiceGroup;
import org.apache.stratos.common.constants.StratosConstants;

import java.rmi.RemoteException;

public class AutoscalerServiceClient {

    private AutoscalerServiceStub stub;

    private static final Log log = LogFactory.getLog(AutoscalerServiceClient.class);
    private static volatile AutoscalerServiceClient instance;

    private AutoscalerServiceClient(String epr) throws AxisFault {

        String autosclaerSocketTimeout = System.getProperty(StratosConstants.AUTOSCALER_CLIENT_SOCKET_TIMEOUT) == null ?
                StratosConstants.DEFAULT_CLIENT_SOCKET_TIMEOUT :
                System.getProperty(StratosConstants.AUTOSCALER_CLIENT_SOCKET_TIMEOUT);

        String autosclaerConnectionTimeout = System.getProperty(StratosConstants.AUTOSCALER_CLIENT_CONNECTION_TIMEOUT) == null ?
                StratosConstants.DEFAULT_CLIENT_CONNECTION_TIMEOUT :
                System.getProperty(StratosConstants.AUTOSCALER_CLIENT_CONNECTION_TIMEOUT);

        try {
            stub = new AutoscalerServiceStub(epr);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT,
                    Integer.valueOf(autosclaerSocketTimeout));
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT,
                    Integer.valueOf(autosclaerConnectionTimeout));

        } catch (AxisFault axisFault) {
            String msg = "Could not initialize autoscaler service client";
            log.error(msg, axisFault);
            throw new AxisFault(msg, axisFault);
        }
    }

    public static AutoscalerServiceClient getInstance() throws AxisFault {
        if (instance == null) {
            synchronized (AutoscalerServiceClient.class) {
                if (instance == null) {
                    instance = new AutoscalerServiceClient(System.getProperty(StratosConstants.AUTOSCALER_SERVICE_URL));
                }
            }
        }
        return instance;
    }

    public void undeployServiceGroupDefinition(String serviceGroupName)
            throws RemoteException, AutoscalerServiceCartridgeGroupNotFoundExceptionException {
        stub.removeServiceGroup(serviceGroupName);
    }

    public org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy[] getAutoScalePolicies()
            throws RemoteException {
        return stub.getAutoScalingPolicies();
    }

    public org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy getAutoScalePolicy(
            String autoscalingPolicyId) throws RemoteException {
        return stub.getAutoscalingPolicy(autoscalingPolicyId);
    }

	public org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy getAutoScalePolicyForTenant(
			String autoscalingPolicyId,int tenantId) throws RemoteException {
		return stub.getAutoscalingPolicyForTenant(autoscalingPolicyId,tenantId);
	}

    public void addApplication(ApplicationContext applicationContext)
            throws AutoscalerServiceApplicationDefinitionExceptionException,
            RemoteException, AutoscalerServiceCartridgeNotFoundExceptionException,
            AutoscalerServiceCartridgeGroupNotFoundExceptionException {
        stub.addApplication(applicationContext);
    }

    public void updateApplication(ApplicationContext applicationContext)
            throws AutoscalerServiceApplicationDefinitionExceptionException,
            RemoteException, AutoscalerServiceCartridgeNotFoundExceptionException,
            AutoscalerServiceCartridgeGroupNotFoundExceptionException {
        stub.updateApplication(applicationContext);
    }

    public ApplicationContext getApplication(String applicationId) throws RemoteException {
        return stub.getApplication(applicationId);
    }

    public ApplicationContext getApplicationByTenant(String applicationId, int tenantId) throws RemoteException {
        return stub.getApplicationByTenant(applicationId, tenantId);
    }

    public boolean existApplication(String applicationId,int tenantId) throws RemoteException {
        return stub.existApplication(applicationId,tenantId);
    }

    public ApplicationContext[] getApplications() throws RemoteException {
        return stub.getApplications();
    }

    public ApplicationContext[] getApplicationsByTenant(int tenantId) throws RemoteException {
        return stub.getApplicationsByTenant(tenantId);
    }

    public boolean deployApplication(String applicationId, String applicationPolicyId) throws RemoteException,
            AutoscalerServiceInvalidPolicyExceptionException, AutoscalerServiceApplicationDefinitionExceptionException {
        return stub.deployApplication(applicationId, applicationPolicyId);
    }

    public void addApplicationPolicy(ApplicationPolicy applicationPolicy)
            throws RemoteException,
            AutoscalerServiceRemoteExceptionException, AutoscalerServiceInvalidApplicationPolicyExceptionException, AutoscalerServiceApplicationPolicyAlreadyExistsExceptionException {
        stub.addApplicationPolicy(applicationPolicy);
    }

    public ApplicationPolicy getApplicationPolicy(String applicationPolicyUuid) throws RemoteException {
        return stub.getApplicationPolicy(applicationPolicyUuid);
    }

	public ApplicationPolicy getApplicationPolicyByTenant(String applicationPolicyId,int tenantId) throws RemoteException {
		return stub.getApplicationPolicyByTenant(applicationPolicyId,tenantId);
	}

    public ApplicationPolicy[] getApplicationPolicies() throws RemoteException {
        return stub.getApplicationPolicies();
    }

    public void updateApplicationPolicy(ApplicationPolicy applicationPolicy)
            throws RemoteException, AutoscalerServiceApplicatioinPolicyNotExistsExceptionException,
            AutoscalerServiceRemoteExceptionException, AutoscalerServiceInvalidApplicationPolicyExceptionException {
        stub.updateApplicationPolicy(applicationPolicy);
    }

    public void removeApplicationPolicy(String applicationPolicyId)
            throws RemoteException, AutoscalerServiceInvalidPolicyExceptionException, AutoscalerServiceUnremovablePolicyExceptionException {
        stub.removeApplicationPolicy(applicationPolicyId);
    }

    public String[] getApplicationNetworkPartitions(String applicationId) throws RemoteException,
            AutoscalerServiceAutoScalerExceptionException {
        return stub.getApplicationNetworkPartitions(applicationId);
    }

    public void undeployApplication(String applicationId, boolean force) throws
            AutoscalerServiceApplicationDefinitionExceptionException, RemoteException {
        stub.undeployApplication(applicationId, force);
    }

    public void deleteApplication(String applicationId) throws RemoteException {
        stub.deleteApplication(applicationId);
    }

    public boolean addAutoscalingPolicy(AutoscalePolicy autoScalePolicy) throws RemoteException,
            AutoscalerServiceAutoScalingPolicyAlreadyExistExceptionException {
        return stub.addAutoScalingPolicy(autoScalePolicy);
    }

    public boolean updateAutoscalingPolicy(AutoscalePolicy autoScalePolicy) throws RemoteException,
            AutoscalerServiceInvalidPolicyExceptionException {
        return stub.updateAutoScalingPolicy(autoScalePolicy);
    }

    public boolean removeAutoscalingPolicy(String autoScalePolicyId) throws RemoteException,
            AutoscalerServicePolicyDoesNotExistExceptionException, AutoscalerServiceUnremovablePolicyExceptionException {
        return stub.removeAutoScalingPolicy(autoScalePolicyId);
    }

    public ServiceGroup getServiceGroup(String serviceGroupDefinitionName) throws RemoteException {
        return stub.getServiceGroup(serviceGroupDefinitionName);
    }

	public ServiceGroup getServiceGroupByTenant(String serviceGroupDefinitionName,int tenantId) throws RemoteException {
		return stub.getServiceGroupByTenant(serviceGroupDefinitionName,tenantId);
	}

    public DeploymentPolicy getDeploymentPolicyByTenant(String deploymentPolicyId,int tenantId) throws RemoteException {
        return stub.getDeploymentPolicyByTenant(deploymentPolicyId, tenantId);
    }

    public DeploymentPolicy[] getDeploymentPoliciesByTenant(int tenantId) throws RemoteException {
        return stub.getDeploymentPoliciesByTenant(tenantId);
    }

    public ServiceGroup[] getServiceGroups() throws RemoteException, AutoscalerServiceAutoScalerExceptionException {
        return stub.getServiceGroups();
    }

    public ServiceGroup[] getServiceGroupsByTenant(int tenantId) throws RemoteException,
            AutoscalerServiceAutoScalerExceptionException {
        return stub.getServiceGroupsByTenant(tenantId);
    }

    public void addServiceGroup(ServiceGroup serviceGroup) throws AutoscalerServiceInvalidServiceGroupExceptionException,
            RemoteException {
        stub.addServiceGroup(serviceGroup);
    }

    public void updateServiceGroup(ServiceGroup serviceGroup) throws AutoscalerServiceInvalidServiceGroupExceptionException,
            RemoteException {
        stub.updateServiceGroup(serviceGroup);
    }

    public void removeServiceGroup(String groupName) throws RemoteException,
            AutoscalerServiceCartridgeGroupNotFoundExceptionException {
        stub.removeServiceGroup(groupName);
    }

    public void updateClusterMonitor(String clusterId, org.apache.stratos.autoscaler.stub.Properties properties)
            throws RemoteException, AutoscalerServiceInvalidArgumentExceptionException {
        stub.updateClusterMonitor(clusterId, properties);
    }

    public String findClusterId(String applicationId, String alias) throws RemoteException {
        return stub.findClusterId(applicationId, alias);
    }

    public void addDeploymentPolicy(DeploymentPolicy deploymentPolicy) throws RemoteException,
            AutoscalerServiceRemoteExceptionException,
            AutoscalerServiceInvalidDeploymentPolicyExceptionException, AutoscalerServiceDeploymentPolicyAlreadyExistsExceptionException {
        stub.addDeployementPolicy(deploymentPolicy);
    }

    public void updateDeploymentPolicy(DeploymentPolicy deploymentPolicy) throws RemoteException,
            AutoscalerServiceDeploymentPolicyNotExistsExceptionException,
            AutoscalerServiceInvalidDeploymentPolicyExceptionException, AutoscalerServiceRemoteExceptionException,
            AutoscalerServiceInvalidPolicyExceptionException, AutoscalerServiceCloudControllerConnectionExceptionException {
        stub.updateDeploymentPolicy(deploymentPolicy);
    }

    public void removeDeploymentPolicy(String deploymentPolicyId) throws RemoteException,
            AutoscalerServiceDeploymentPolicyNotExistsExceptionException,
            AutoscalerServiceUnremovablePolicyExceptionException {
        stub.removeDeployementPolicy(deploymentPolicyId);
    }

    public DeploymentPolicy getDeploymentPolicy(String deploymentPolicyId) throws RemoteException {
        return stub.getDeploymentPolicy(deploymentPolicyId);
    }

	public DeploymentPolicy getDeploymentPolicyForTenant(String deploymentPolicyId,int tenantId) throws RemoteException {
		return stub.getDeploymentPolicyForTenant(deploymentPolicyId,tenantId);
	}

    public DeploymentPolicy[] getDeploymentPolicies() throws RemoteException {
        return stub.getDeploymentPolicies();
    }

}

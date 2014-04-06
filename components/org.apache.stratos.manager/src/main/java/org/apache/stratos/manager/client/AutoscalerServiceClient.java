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
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.manager.internal.DataHolder;
import org.apache.stratos.manager.utils.CartridgeConstants;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.stub.AutoScalerServiceInvalidPartitionExceptionException;
import org.apache.stratos.autoscaler.stub.AutoScalerServiceInvalidPolicyExceptionException;
import org.apache.stratos.autoscaler.stub.AutoScalerServiceNonExistingLBExceptionException;
import org.apache.stratos.autoscaler.stub.AutoScalerServiceStub;

import java.rmi.RemoteException;

public class AutoscalerServiceClient {

    private AutoScalerServiceStub stub;

    private static final Log log = LogFactory.getLog(AutoscalerServiceClient.class);
    private static volatile AutoscalerServiceClient serviceClient;

    public AutoscalerServiceClient(String epr) throws AxisFault {
    	
    	
    	
    	String autosclaerSocketTimeout =
    		System.getProperty(CartridgeConstants.AUTOSCALER_SOCKET_TIMEOUT) == null ? "300000": System.getProperty(CartridgeConstants.AUTOSCALER_SOCKET_TIMEOUT);
		String autosclaerConnectionTimeout = 
			System.getProperty(CartridgeConstants.AUTOSCALER_CONNECTION_TIMEOUT) == null ? "300000" : System.getProperty(CartridgeConstants.AUTOSCALER_CONNECTION_TIMEOUT) ;
    	
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

    public Partition[] getAvailablePartitions () throws RemoteException {

		Partition[] partitions;
		partitions = stub.getAllAvailablePartitions();

		return partitions;
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

	public org.apache.stratos.autoscaler.partition.PartitionGroup[] getPartitionGroups(
			String deploymentPolicyId) throws RemoteException {

		org.apache.stratos.autoscaler.partition.PartitionGroup[] partitionGroups;
		partitionGroups = stub.getPartitionGroups(deploymentPolicyId);

		return partitionGroups;
	}

	public org.apache.stratos.autoscaler.policy.model.AutoscalePolicy[] getAutoScalePolicies()
			throws RemoteException {

		org.apache.stratos.autoscaler.policy.model.AutoscalePolicy[] autoscalePolicies;
		autoscalePolicies = stub.getAllAutoScalingPolicy();

		return autoscalePolicies;
	}

	public org.apache.stratos.autoscaler.policy.model.AutoscalePolicy getAutoScalePolicy(
			String autoscalingPolicyId) throws RemoteException {

		org.apache.stratos.autoscaler.policy.model.AutoscalePolicy autoscalePolicy;
		autoscalePolicy = stub.getAutoscalingPolicy(autoscalingPolicyId);

		return autoscalePolicy;
	}

	public org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy[] getDeploymentPolicies()
			throws RemoteException {

		org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy[] deploymentPolicies;
		deploymentPolicies = stub.getAllDeploymentPolicies();

		return deploymentPolicies;
	}

	public org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy[] getDeploymentPolicies(
			String cartridgeType) throws RemoteException {

		org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy[] deploymentPolicies;
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

    public org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy getDeploymentPolicy (String deploymentPolicyId) throws RemoteException {

		org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy deploymentPolicy;
		deploymentPolicy = stub.getDeploymentPolicy(deploymentPolicyId);

		return deploymentPolicy;
	}

    public boolean deployDeploymentPolicy (DeploymentPolicy deploymentPolicy) throws RemoteException, 
    	AutoScalerServiceInvalidPolicyExceptionException {

            return stub.addDeploymentPolicy(deploymentPolicy);

    }

    public boolean deployAutoscalingPolicy (AutoscalePolicy autoScalePolicy) throws RemoteException, 
    	AutoScalerServiceInvalidPolicyExceptionException {

            return stub.addAutoScalingPolicy(autoScalePolicy);

    }

    public boolean deployPartition (Partition partition) throws RemoteException,
    	AutoScalerServiceInvalidPartitionExceptionException {

            return stub.addPartition(partition);

    }
    
    public String getDefaultLBClusterId (String deploymentPolicy) throws RemoteException {
    	return stub.getDefaultLBClusterId(deploymentPolicy);
    }
    
    
    public String getServiceLBClusterId (String serviceType, String deploymentPolicy) throws RemoteException {
    	return stub.getServiceLBClusterId(serviceType, deploymentPolicy);
    }
    
}

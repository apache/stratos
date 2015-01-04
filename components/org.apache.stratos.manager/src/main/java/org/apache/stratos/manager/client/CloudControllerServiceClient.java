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
import org.apache.stratos.cloud.controller.stub.*;
import org.apache.stratos.cloud.controller.stub.domain.*;
import org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesCluster;
import org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost;
import org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesMaster;
import org.apache.stratos.common.Properties;
import org.apache.stratos.manager.internal.ServiceReferenceHolder;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;
import org.apache.stratos.manager.utils.CartridgeConstants;

import java.rmi.RemoteException;

public class CloudControllerServiceClient {

	private CloudControllerServiceStub stub;

	private static final Log log = LogFactory.getLog(CloudControllerServiceClient.class);
    private static volatile CloudControllerServiceClient serviceClient;

	public CloudControllerServiceClient(String epr) throws AxisFault {

		String ccSocketTimeout = 
			System.getProperty(CartridgeConstants.CC_SOCKET_TIMEOUT) == null ? "300000" : System.getProperty(CartridgeConstants.CC_SOCKET_TIMEOUT);
		String ccConnectionTimeout = 
			System.getProperty(CartridgeConstants.CC_CONNECTION_TIMEOUT) == null ? "300000" : System.getProperty(CartridgeConstants.CC_CONNECTION_TIMEOUT);
		
		ConfigurationContext clientConfigContext = ServiceReferenceHolder.getClientConfigContext();
		try {
			stub = new CloudControllerServiceStub(clientConfigContext, epr);
			stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, new Integer(ccSocketTimeout));
			stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, new Integer(ccConnectionTimeout));

		} catch (AxisFault axisFault) {
			String msg = "Failed to initiate AutoscalerService client. " + axisFault.getMessage();
			log.error(msg, axisFault);
			throw new AxisFault(msg, axisFault);
		}

	}

    public static CloudControllerServiceClient getServiceClient() throws AxisFault {
        if (serviceClient == null) {
            synchronized (CloudControllerServiceClient.class) {
                if (serviceClient == null) {
                    serviceClient = new CloudControllerServiceClient(
                            System.getProperty(CartridgeConstants.CLOUD_CONTROLLER_SERVICE_URL));
                }
            }
        }
        return serviceClient;
    }

    public void addCartridge(CartridgeConfig cartridgeConfig)
    		throws RemoteException, CloudControllerServiceInvalidCartridgeDefinitionExceptionException,
            CloudControllerServiceInvalidIaasProviderExceptionException {

		stub.addCartridge(cartridgeConfig);

	}

    public void removeCartridge(String cartridgeType) throws RemoteException, CloudControllerServiceInvalidCartridgeTypeExceptionException {
		stub.removeCartridge(cartridgeType);
	}

    public String [] getServiceGroupSubGroups(String name) throws RemoteException, CloudControllerServiceInvalidServiceGroupExceptionException {
    	return stub.getServiceGroupSubGroups(name);
    }
    
    public String [] getServiceGroupCartridges(String name) throws RemoteException, CloudControllerServiceInvalidServiceGroupExceptionException {
    	return stub.getServiceGroupCartridges(name);
    }
    
    public Dependencies getServiceGroupDependencies (String name)throws RemoteException, CloudControllerServiceInvalidServiceGroupExceptionException {
    	return stub.getServiceGroupDependencies(name);
    }
     
    public ServiceGroup getServiceGroup(String name) throws RemoteException, CloudControllerServiceInvalidServiceGroupExceptionException {
    	return stub.getServiceGroup(name);
    }

	public boolean register(String clusterId, String cartridgeType,
                            String payload, String tenantRange,
                            String hostName, Properties properties,
                            String autoscalorPolicyName, String deploymentPolicyName, Persistence persistence) throws RemoteException,
                            CloudControllerServiceCartridgeNotFoundExceptionException {
	    Registrant registrant = new Registrant();
	    registrant.setClusterId(clusterId);
	    registrant.setCartridgeType(cartridgeType);
	    registrant.setTenantRange(tenantRange);
	    registrant.setHostName(hostName);
	    registrant.setProperties(ApplicationManagementUtil.toCCStubProperties(properties));
	    registrant.setPayload(payload);
	    registrant.setAutoScalerPolicyName(autoscalorPolicyName);
        registrant.setDeploymentPolicyName(deploymentPolicyName);
        registrant.setPersistence(persistence);
		return stub.registerService(registrant);

	}

    public void terminateAllInstances(String clusterId) throws RemoteException, 
    CloudControllerServiceInvalidClusterExceptionException {
		stub.terminateInstances(clusterId);
	}

	public String[] getRegisteredCartridges() throws RemoteException {
		return stub.getCartridges();
	}

	public CartridgeInfo getCartridgeInfo(String cartridgeType) throws RemoteException, 
	CloudControllerServiceCartridgeNotFoundExceptionException {
		return stub.getCartridgeInfo(cartridgeType);
	}
	
	public void unregisterService(String clusterId) throws RemoteException, 
	CloudControllerServiceUnregisteredClusterExceptionException {
	    stub.unregisterService(clusterId);
	}

    public ClusterContext getClusterContext (String clusterId) throws RemoteException {

        return stub.getClusterContext(clusterId);
    }
    
    public boolean deployKubernetesCluster(KubernetesCluster kubernetesCluster) throws RemoteException,
            CloudControllerServiceInvalidKubernetesClusterExceptionException {
        return stub.addKubernetesCluster(kubernetesCluster);
    }

    public boolean deployKubernetesHost(String kubernetesClusterId, KubernetesHost kubernetesHost)
            throws RemoteException, CloudControllerServiceInvalidKubernetesHostExceptionException,
            CloudControllerServiceNonExistingKubernetesClusterExceptionException {

        return stub.addKubernetesHost(kubernetesClusterId, kubernetesHost);
    }

    public boolean updateKubernetesMaster(KubernetesMaster kubernetesMaster) throws RemoteException,
            CloudControllerServiceInvalidKubernetesMasterExceptionException,
            CloudControllerServiceNonExistingKubernetesMasterExceptionException {
        return stub.updateKubernetesMaster(kubernetesMaster);
    }

    public KubernetesCluster[] getAvailableKubernetesClusters() throws RemoteException {
        return stub.getKubernetesClusters();
    }

    public KubernetesCluster getKubernetesCluster(String kubernetesClusterId) throws RemoteException,
            CloudControllerServiceNonExistingKubernetesClusterExceptionException {
        return stub.getKubernetesCluster(kubernetesClusterId);
    }

    public boolean undeployKubernetesCluster(String kubernetesClusterId) throws RemoteException,
            CloudControllerServiceNonExistingKubernetesClusterExceptionException {
        return stub.removeKubernetesCluster(kubernetesClusterId);
    }

    public boolean undeployKubernetesHost(String kubernetesHostId) throws RemoteException,
            CloudControllerServiceNonExistingKubernetesHostExceptionException {
        return stub.removeKubernetesHost(kubernetesHostId);
    }

    public KubernetesHost[] getKubernetesHosts(String kubernetesClusterId) throws RemoteException,
            CloudControllerServiceNonExistingKubernetesClusterExceptionException {
        return stub.getHostsForKubernetesCluster(kubernetesClusterId);
    }

    public KubernetesMaster getKubernetesMaster(String kubernetesClusterId) throws RemoteException,
            CloudControllerServiceNonExistingKubernetesClusterExceptionException {
        return stub.getMasterForKubernetesCluster(kubernetesClusterId);
    }

    public boolean updateKubernetesHost(KubernetesHost kubernetesHost) throws RemoteException,
            CloudControllerServiceInvalidKubernetesHostExceptionException,
            CloudControllerServiceNonExistingKubernetesHostExceptionException {
        return stub.updateKubernetesHost(kubernetesHost);
    }
}

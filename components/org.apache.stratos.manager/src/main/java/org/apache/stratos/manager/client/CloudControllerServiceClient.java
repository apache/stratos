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
import org.apache.stratos.cloud.controller.stub.pojo.*;
import org.apache.stratos.manager.internal.DataHolder;
import org.apache.stratos.manager.utils.CartridgeConstants;

import java.rmi.RemoteException;
import java.util.Iterator;

public class CloudControllerServiceClient {

	private CloudControllerServiceStub stub;

	private static final Log log = LogFactory.getLog(CloudControllerServiceClient.class);
    private static volatile CloudControllerServiceClient serviceClient;

	public CloudControllerServiceClient(String epr) throws AxisFault {

		String ccSocketTimeout = 
			System.getProperty(CartridgeConstants.CC_SOCKET_TIMEOUT) == null ? "300000" : System.getProperty(CartridgeConstants.CC_SOCKET_TIMEOUT);
		String ccConnectionTimeout = 
			System.getProperty(CartridgeConstants.CC_CONNECTION_TIMEOUT) == null ? "300000" : System.getProperty(CartridgeConstants.CC_CONNECTION_TIMEOUT);
		
		ConfigurationContext clientConfigContext = DataHolder.getClientConfigContext();
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

    public void deployCartridgeDefinition (CartridgeConfig cartridgeConfig)
    		throws RemoteException, CloudControllerServiceInvalidCartridgeDefinitionExceptionException,
            CloudControllerServiceInvalidIaasProviderExceptionException {

		stub.deployCartridgeDefinition(cartridgeConfig);

	}

    public void unDeployCartridgeDefinition (String cartridgeType) throws RemoteException, CloudControllerServiceInvalidCartridgeTypeExceptionException {

		stub.undeployCartridgeDefinition(cartridgeType);

	}
    
    public void deployServiceGroup (ServiceGroup serviceGroup) throws RemoteException, CloudControllerServiceInvalidServiceGroupExceptionException {
    	stub.deployServiceGroup(serviceGroup);
    }
    
    public void undeployServiceGroup (String name)throws RemoteException, CloudControllerServiceInvalidServiceGroupExceptionException {
    	stub.undeployServiceGroup(name);
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
                            CloudControllerServiceUnregisteredCartridgeExceptionException {		
	    Registrant registrant = new Registrant();
	    registrant.setClusterId(clusterId);
	    registrant.setCartridgeType(cartridgeType);
	    registrant.setTenantRange(tenantRange);
	    registrant.setHostName(hostName);
	    registrant.setProperties(properties);
	    registrant.setPayload(payload);
	    registrant.setAutoScalerPolicyName(autoscalorPolicyName);
        registrant.setDeploymentPolicyName(deploymentPolicyName);
        registrant.setPersistence(persistence);
		return stub.registerService(registrant);

	}

    @SuppressWarnings("unused")
    private Properties
        extractProperties(java.util.Properties properties) {
        Properties props = new Properties();
        if (properties != null) {

            for (Iterator<Object> iterator = properties.keySet().iterator(); iterator.hasNext();) {
                String key = (String) iterator.next();
                String value = properties.getProperty(key);

                Property prop = new Property();
                prop.setName(key);
                prop.setValue(value);

                props.addProperties(prop);

            }
        }
        return props;
    }

    public void terminateAllInstances(String clusterId) throws RemoteException, 
    CloudControllerServiceInvalidClusterExceptionException {
		stub.terminateAllInstances(clusterId);
	}

	public String[] getRegisteredCartridges() throws RemoteException {
		return stub.getRegisteredCartridges();
	}

	public CartridgeInfo getCartridgeInfo(String cartridgeType) throws RemoteException, 
	CloudControllerServiceUnregisteredCartridgeExceptionException {
		return stub.getCartridgeInfo(cartridgeType);
	}
	
	public void unregisterService(String clusterId) throws RemoteException, 
	CloudControllerServiceUnregisteredClusterExceptionException {
	    stub.unregisterService(clusterId);
	}

    public ClusterContext getClusterContext (String clusterId) throws RemoteException {

        return stub.getClusterContext(clusterId);
    }
}

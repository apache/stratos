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
import org.apache.stratos.adc.mgt.exception.UnregisteredCartridgeException;
import org.apache.stratos.adc.mgt.internal.DataHolder;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
import org.apache.stratos.cloud.controller.pojo.CartridgeConfig;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.pojo.Properties;
import org.apache.stratos.cloud.controller.pojo.Registrant;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceIllegalArgumentExceptionException;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceStub;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceUnregisteredCartridgeExceptionException;
import org.apache.stratos.cloud.controller.pojo.Property;

import java.rmi.RemoteException;
import java.util.Iterator;

public class CloudControllerServiceClient {

	private CloudControllerServiceStub stub;

	private static final Log log = LogFactory.getLog(CloudControllerServiceClient.class);
    private static volatile CloudControllerServiceClient serviceClient;

	public CloudControllerServiceClient(String epr) throws AxisFault {

		ConfigurationContext clientConfigContext = DataHolder.getClientConfigContext();
		try {
			stub = new CloudControllerServiceStub(clientConfigContext, epr);
			stub._getServiceClient().getOptions().setTimeOutInMilliSeconds(300000);

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
            throws Exception {

        try {
            stub.deployCartridgeDefinition(cartridgeConfig);

        } catch (RemoteException e) {
            String errorMsg = "Error in deploying cartridge definition";
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        }
    }

    public void unDeployCartridgeDefinition (String cartridgeType)
            throws Exception {

        try {
            stub.undeployCartridgeDefinition(cartridgeType);

        } catch (RemoteException e) {
            String errorMsg = "Error in deploying cartridge definition";
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        }
    }

	public boolean register(String clusterId, String cartridgeType,
	                        String payload, String tenantRange,
                            String hostName, Properties properties,
                            String autoscalorPolicyName, String deploymentPolicyName) throws RemoteException,
                            CloudControllerServiceUnregisteredCartridgeExceptionException, 
                            CloudControllerServiceIllegalArgumentExceptionException {		
	    Registrant registrant = new Registrant();
	    registrant.setClusterId(clusterId);
	    registrant.setCartridgeType(cartridgeType);
	    registrant.setTenantRange(tenantRange);
	    registrant.setHostName(hostName);
	    registrant.setProperties(properties);
	    registrant.setPayload(payload);
	    registrant.setAutoScalerPolicyName(autoscalorPolicyName);
        registrant.setDeploymentPolicyName(deploymentPolicyName);
		return stub.registerService(registrant);

	}

    @SuppressWarnings("unused")
    private org.apache.stratos.cloud.controller.pojo.Properties
        extractProperties(java.util.Properties properties) {
        org.apache.stratos.cloud.controller.pojo.Properties props =
                                                                 new org.apache.stratos.cloud.controller.pojo.Properties();
        if (properties != null) {

            for (Iterator iterator = properties.keySet().iterator(); iterator.hasNext();) {
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

    public void terminateAllInstances(String clusterId) throws Exception {
		stub.terminateAllInstances(clusterId);
	}

	public String[] getRegisteredCartridges() throws Exception {
		return stub.getRegisteredCartridges();
	}

    public CartridgeInfo getCartridgeInfo(String cartridgeType) throws UnregisteredCartridgeException, Exception {
		try {
			return stub.getCartridgeInfo(cartridgeType);
		} catch (RemoteException e) {
			throw e;
		} catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
			throw new UnregisteredCartridgeException("Not a registered cartridge " + cartridgeType, cartridgeType, e);
		}
	}
	
	public void unregisterService(String clusterId) throws Exception {
	    stub.unregisterService(clusterId);
	}

}

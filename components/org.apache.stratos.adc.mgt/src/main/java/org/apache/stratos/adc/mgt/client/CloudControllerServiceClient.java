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
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceStub;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceUnregisteredCartridgeExceptionException;
import org.apache.stratos.cloud.controller.util.xsd.CartridgeInfo;
import org.apache.stratos.cloud.controller.util.xsd.Properties;

import java.rmi.RemoteException;

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
                            System.getProperty(CartridgeConstants.AUTOSCALER_SERVICE_URL));
                }
            }
        }
        return serviceClient;
    }

	public boolean register(String clusterId, String cartridgeType,
	                        String payload, String tenantRange,
                            String hostName, Properties properties,
                            String autoscalorPolicyName) throws RemoteException, CloudControllerServiceUnregisteredCartridgeExceptionException
	                                                                                  {		
		return stub.registerService(clusterId, tenantRange, cartridgeType, hostName,
		                            properties, payload, autoscalorPolicyName);

	}

    public boolean terminateAllInstances(String clusterId) throws Exception {
		return stub.terminateAllInstances(clusterId);
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
	
	public boolean unregisterService(String clusterId) throws Exception {
	    return stub.unregisterService(clusterId);
	}

}

/*
 * Copyright WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.adc.mgt.client;

import java.rmi.RemoteException;

import javax.activation.DataHandler;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.adc.mgt.exception.UnregisteredCartridgeException;
import org.wso2.carbon.adc.mgt.internal.DataHolder;
import org.wso2.carbon.stratos.cloud.controller.stub.CloudControllerServiceStub;
import org.wso2.carbon.stratos.cloud.controller.stub.CloudControllerServiceUnregisteredCartridgeExceptionException;
import org.wso2.carbon.stratos.cloud.controller.util.xsd.CartridgeInfo;
import org.wso2.carbon.stratos.cloud.controller.util.xsd.Properties;

public class CloudControllerServiceClient {

	private CloudControllerServiceStub stub;

	private static final Log log = LogFactory.getLog(CloudControllerServiceClient.class);

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

	public boolean register(String domainName, String subDomain, String cartridgeType,
	                        DataHandler payload, String tenantRange, String hostName, Properties properties) throws RemoteException, CloudControllerServiceUnregisteredCartridgeExceptionException
	                                                                                  {		
		return stub.registerService(domainName, subDomain, tenantRange, cartridgeType, hostName,
		                            properties, payload);

	}

	public String startInstance(String domain, String subDomain) throws Exception {
		return stub.startInstance(domain, subDomain);
	}

	public boolean terminateAllInstances(String domain, String subDomain) throws Exception {
		return stub.terminateAllInstances(domain, subDomain);
	}

	public String[] getRegisteredCartridges() throws Exception {
		return stub.getRegisteredCartridges();
	}

	public boolean createKeyPair(String cartridge, String keyPairName, String publicKey)
	                                                                                    throws Exception {
		return stub.createKeyPairFromPublicKey(cartridge, keyPairName, publicKey);
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
	
	public boolean unregisterService(String domain, String subDomain) throws Exception {
	    return stub.unregisterService(domain, subDomain);
	}

}

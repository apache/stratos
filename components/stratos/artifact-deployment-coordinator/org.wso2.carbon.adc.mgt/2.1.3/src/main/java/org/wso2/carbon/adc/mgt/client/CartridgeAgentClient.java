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

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cartridge.agent.stub.CartridgeAgentServiceStub;
import org.wso2.carbon.adc.mgt.internal.DataHolder;

public class CartridgeAgentClient {

	private static final Log log = LogFactory.getLog(CartridgeAgentClient.class);
	CartridgeAgentServiceStub stub = null;
	public CartridgeAgentClient(String epr) throws AxisFault {
	  ConfigurationContext clientConfigContext = DataHolder.getClientConfigContext();
	  stub = new CartridgeAgentServiceStub(clientConfigContext, epr);
    }
	
	public void unregister(String domain, String subDomain, String hostName) throws Exception {
		log.info(" ** Unregistering -- ");
		stub.unregister(domain, subDomain, hostName);
	}
}

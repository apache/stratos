/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.cartridge.agent.service;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cartridge.agent.exception.CartridgeAgentException;
import org.wso2.carbon.cartridge.agent.ClusteringClient;
import org.wso2.carbon.cartridge.agent.internal.CartridgeAgentConstants;
import org.wso2.carbon.cartridge.agent.internal.DataHolder;
import org.wso2.carbon.cartridge.agent.registrant.Registrant;

/**
 * Web service used for allowing {@link Registrant}s to register themselves with the Cartridge Agent
 *
 * @see Registrant
 */
@SuppressWarnings("unused")
public class CartridgeAgentService {
    private static final Log log = LogFactory.getLog(CartridgeAgentService.class);

    public boolean register(Registrant registrant) throws CartridgeAgentException {

        MessageContext messageContext = MessageContext.getCurrentMessageContext();
        ConfigurationContext configurationContext = messageContext.getConfigurationContext();
        ClusteringClient clusteringClient =
                (ClusteringClient) configurationContext.getProperty(CartridgeAgentConstants.CLUSTERING_CLIENT);
        if (registrant.getRemoteHost() == null || registrant.getRemoteHost().isEmpty()) {
            String remoteAddr = (String) messageContext.getProperty("REMOTE_ADDR");
            registrant.setRemoteHost(remoteAddr);
        }
        log.info("Trying to add new registrant " + registrant + "...");
        clusteringClient.joinGroup(registrant, configurationContext);
//        Main.getHealthChecker().start(registrant);
        DataHolder.getHealthChecker().start(registrant);
        return true;

    }

    public boolean unregister(String domain, String subDomain, String hostName) throws CartridgeAgentException {

        MessageContext messageContext = MessageContext.getCurrentMessageContext();
        ConfigurationContext configurationContext = messageContext.getConfigurationContext();
        ClusteringClient clusteringClient =
                (ClusteringClient) configurationContext.getProperty(CartridgeAgentConstants.CLUSTERING_CLIENT);
        clusteringClient.removeClusterDomain(domain, subDomain, hostName, configurationContext);
        return true;

    }
}

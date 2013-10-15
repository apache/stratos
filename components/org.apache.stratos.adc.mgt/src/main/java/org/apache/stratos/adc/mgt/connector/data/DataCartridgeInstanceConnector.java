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

package org.apache.stratos.adc.mgt.connector.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.connector.CartridgeInstanceConnector;
import org.apache.stratos.adc.mgt.dto.Cartridge;
import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.exception.NotSubscribedException;
import org.apache.stratos.adc.mgt.instance.CartridgeInstance;
import org.apache.stratos.adc.mgt.utils.ApplicationManagementUtil;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;

import java.util.Properties;

public class DataCartridgeInstanceConnector extends CartridgeInstanceConnector {

    private static Log log = LogFactory.getLog(DataCartridgeInstanceConnector.class);

    @Override
    public Properties createConnection(CartridgeInstance cartridgeInstance,
                                       CartridgeInstance connectingCartridgeInstance) throws ADCException {

        //TODO: change the logic to do with topology information
        log.info("Retrieving cartridge information for connecting ... alias : " +
                connectingCartridgeInstance.getAlias() + ", Type: " + connectingCartridgeInstance.getType());

        Properties connectionProperties = new Properties();

        int maxAttempts = Integer.parseInt(System.getProperty(CartridgeConstants.MAX_ATTEMPTS, "50"));
        int attempts = 0;
        while (attempts < maxAttempts) {
            attempts++;
            Cartridge cartridge = null;
            try {
                cartridge = ApplicationManagementUtil.getCartridgeInfo(
                        connectingCartridgeInstance.getAlias(),
                        connectingCartridgeInstance.getSubscriber().getTenantDomain());

            } catch (NotSubscribedException e) {
                // This cannot happen here.
            }
            if (cartridge != null) {
                if (!cartridge.getStatus().equals("ACTIVE")) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ignore) {
                    }
                } else {
                    connectionProperties.setProperty("MYSQL_HOST", cartridge.getIp());
                    connectionProperties.setProperty("MYSQL_USER", cartridge.getDbUserName());
                    connectionProperties.setProperty("MYSQL_PASSWORD", cartridge.getPassword());
                    log.info("Connection information retrieved for " + cartridgeInstance + " and " +
                            connectingCartridgeInstance);
                    break;
                }
            }

            if(attempts == maxAttempts) {
                String errorMsg = "Failed to connect " + cartridgeInstance + " and " + connectingCartridgeInstance;
                log.error(errorMsg);
                throw  new ADCException(errorMsg);
            }
        }

        return connectionProperties;
    }

    @Override
    public Properties teminateConnection(CartridgeInstance cartridgeInstance,
                                         CartridgeInstance connectedCartridgeInstance) throws ADCException {
        return null;
    }
}

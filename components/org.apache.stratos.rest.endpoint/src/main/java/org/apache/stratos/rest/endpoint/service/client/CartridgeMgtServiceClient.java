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

package org.apache.stratos.rest.endpoint.service.client;

public class CartridgeMgtServiceClient {

    /*private CartridgeMgtServiceStub cartridgeMgtServiceStub;
    private static final Log log = LogFactory.getLog(CartridgeMgtServiceClient.class);
    private static volatile CartridgeMgtServiceClient serviceClient;
    private static final String CARTRIDGE_MGT_EPR = "cartridge.mgt.epr";

    private CartridgeMgtServiceClient(String epr) throws AxisFault {

        ConfigurationContext clientConfigContext = ServiceHolder.getConfigurationContext().getClientConfigContext();
        try {
            cartridgeMgtServiceStub = new CartridgeMgtServiceStub(clientConfigContext, epr);
            cartridgeMgtServiceStub._getServiceClient().getOptions().setTimeOutInMilliSeconds(300000);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate CartridgeMgtServiceClient client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new AxisFault(msg, axisFault);
        }

    }

    public static CartridgeMgtServiceClient getServiceClient() throws AxisFault {
        if (serviceClient == null) {
            synchronized (CartridgeMgtServiceClient.class) {
                if (serviceClient == null) {
                    serviceClient = new CartridgeMgtServiceClient(System.getProperty(CARTRIDGE_MGT_EPR));
                }
            }
        }
        return serviceClient;
    }

    public void deployCartridgedefinition (CartridgeConfig cartridgeConfig)
            throws Exception {

        try {
            cartridgeMgtServiceStub.deployCartridgeDefinition(cartridgeConfig);

        } catch (RemoteException e) {
            String errorMsg = "Transport error in deploying cartridge definition";
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        }
    }

    public void undeployCartridgeDefinition (String cartridgeType) throws Exception {

        try {
            cartridgeMgtServiceStub.undeployCartridgeDefinition(cartridgeType);

        } catch (RemoteException e) {
            String errorMsg = "Transport error in undeploying cartridge definition type " + cartridgeType;
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        }
    } */
}

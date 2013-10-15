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

package org.apache.stratos.adc.mgt.connector;

import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.instance.CartridgeInstance;

import java.util.Properties;

public abstract class CartridgeInstanceConnector {

    /**
     * Connects two cartridge instances
     *
     * @param cartridgeInstance CartridgeInstance instance
     * @param connectingCartridgeInstance CartridgeInstance instance that is connecting with cartridgeInstance
     *
     * @return Custom properties for the connection as a key value set
     * @throws ADCException in case of an error
     */
    public abstract Properties createConnection (CartridgeInstance cartridgeInstance,
                                                 CartridgeInstance connectingCartridgeInstance) throws ADCException;
    public abstract Properties teminateConnection (CartridgeInstance cartridgeInstance,
                                                   CartridgeInstance connectedCartridgeInstance) throws ADCException;
}

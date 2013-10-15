/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.stratos.adc.mgt.payload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;

public class PayloadFactory {

	private static Log log = LogFactory.getLog(PayloadFactory.class);

    /**
     * Creates and returns a Payload instance
     *
     * @param cartridgeProvider Cartridge provider
     * @param cartridgeType Cartridge type
     * @param payloadFilePath Location to create the payload
     * @return Payload instance
     * @throws ADCException if no matching criteria is found to create a payload
     */
	public static Payload getPayloadInstance (String cartridgeProvider, String cartridgeType, String payloadFilePath)
            throws ADCException {

        Payload payload = null;

        if(cartridgeProvider.equals(CartridgeConstants.PROVIDER_NAME_WSO2)) {
            payload = new CarbonPayload(payloadFilePath);

        } else {
            if(cartridgeType.equals(CartridgeConstants.MYSQL_CARTRIDGE_NAME)) {
                payload = new DataPayload(payloadFilePath);
            }
            else {
                payload = new NonCarbonPayload(payloadFilePath);
            }
        }

        if(payload == null) {
            throw new ADCException("Unable to find matching payload for cartridge type " + cartridgeType +
                    ", provider " + cartridgeProvider);
        }

        return payload;
    }
}

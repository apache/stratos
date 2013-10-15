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

package org.apache.stratos.adc.mgt.instance.factory;

import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.instance.CartridgeInstance;
import org.apache.stratos.adc.mgt.instance.DataCartridgeInstance;
import org.apache.stratos.adc.mgt.instance.MultiTenantCartridgeInstance;
import org.apache.stratos.adc.mgt.instance.SingleTenantCartridgeInstance;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
import org.apache.stratos.cloud.controller.util.xsd.CartridgeInfo;

public class CartridgeInstanceFactory {

    /**
     * Returns the relevant CartridgeInstance object for the given criteria
     *
     * @param cartridgeInfo CartridgeInfo instance
     * @return CartridgeInstance instance
     * @throws ADCException if no matching criteria is there to create a CartridgeInstance object
     */
    public static CartridgeInstance getCartridgeInstance (CartridgeInfo cartridgeInfo)
            throws ADCException {

        CartridgeInstance cartridgeInstance = null;
        if(cartridgeInfo.getMultiTenant()) {
            cartridgeInstance = new MultiTenantCartridgeInstance(cartridgeInfo);

        } else {
            if(cartridgeInfo.getType().equals(CartridgeConstants.MYSQL_CARTRIDGE_NAME)) {
                cartridgeInstance = new DataCartridgeInstance(cartridgeInfo);
            }
            else if (cartridgeInfo.getType().equals(CartridgeConstants.PHP_CARTRIDGE_NAME)) {
                cartridgeInstance = new SingleTenantCartridgeInstance(cartridgeInfo);
            }
            else if (cartridgeInfo.getType().equals(CartridgeConstants.TOMCAT_CARTRIDGE_NAME)) {
                cartridgeInstance = new SingleTenantCartridgeInstance(cartridgeInfo);
            }
        }

        if(cartridgeInstance == null) {
            throw new ADCException("Unable to find matching CartridgeInstance for "
                    + cartridgeInfo);
        }

        return cartridgeInstance;
    }
}

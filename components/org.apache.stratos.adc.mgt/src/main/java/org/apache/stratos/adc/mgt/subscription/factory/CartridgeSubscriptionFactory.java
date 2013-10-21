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

package org.apache.stratos.adc.mgt.subscription.factory;

import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
import org.apache.stratos.adc.mgt.subscription.DataCartridgeSubscription;
import org.apache.stratos.adc.mgt.subscription.MultiTenantCartridgeSubscription;
import org.apache.stratos.adc.mgt.subscription.SingleTenantCartridgeSubscription;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
import org.apache.stratos.cloud.controller.util.xsd.CartridgeInfo;

public class CartridgeSubscriptionFactory {

    /**
     * Returns the relevant CartridgeSubscription object for the given criteria
     *
     * @param cartridgeInfo CartridgeInfo subscription
     * @return CartridgeSubscription subscription
     * @throws ADCException if no matching criteria is there to create a CartridgeSubscription object
     */
    public static CartridgeSubscription getCartridgeSubscriptionInstance(CartridgeInfo cartridgeInfo)
            throws ADCException {

        CartridgeSubscription cartridgeSubscription = null;
        if(cartridgeInfo.getMultiTenant()) {
            cartridgeSubscription = new MultiTenantCartridgeSubscription(cartridgeInfo);

        } else {
            if(cartridgeInfo.getType().equals(CartridgeConstants.MYSQL_CARTRIDGE_NAME)) {
                cartridgeSubscription = new DataCartridgeSubscription(cartridgeInfo);
            }
            else if (cartridgeInfo.getType().equals(CartridgeConstants.PHP_CARTRIDGE_NAME)) {
                cartridgeSubscription = new SingleTenantCartridgeSubscription(cartridgeInfo);
            }
            else if (cartridgeInfo.getType().equals(CartridgeConstants.TOMCAT_CARTRIDGE_NAME)) {
                cartridgeSubscription = new SingleTenantCartridgeSubscription(cartridgeInfo);
            }
        }

        if(cartridgeSubscription == null) {
            throw new ADCException("Unable to create a CartridgeSubscription subscription for "
                    + cartridgeInfo);
        }

        return cartridgeSubscription;
    }
}

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

package org.apache.stratos.adc.mgt.subscription.tenancy;

import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.exception.AlreadySubscribedException;
import org.apache.stratos.adc.mgt.exception.NotSubscribedException;
import org.apache.stratos.adc.mgt.exception.UnregisteredCartridgeException;
import org.apache.stratos.adc.mgt.payload.PayloadArg;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
import org.apache.stratos.cloud.controller.pojo.Properties;

import java.io.Serializable;

public abstract class SubscriptionTenancyBehaviour implements Serializable {

    private static final long serialVersionUID = 1L;
    protected CartridgeSubscription cartridgeSubscription;

    public SubscriptionTenancyBehaviour(CartridgeSubscription cartridgeSubscription) {
        this.cartridgeSubscription = cartridgeSubscription;
    }

    public void setCartridgeSubscription (CartridgeSubscription cartridgeSubscription) {
        this.cartridgeSubscription = cartridgeSubscription;
    }

    public CartridgeSubscription getCartridgeSubscription () {
        return cartridgeSubscription;
    }

    public abstract void createSubscription() throws ADCException, AlreadySubscribedException;

    public abstract void registerSubscription(Properties properties) throws ADCException, UnregisteredCartridgeException;

    public abstract void removeSubscription() throws ADCException, NotSubscribedException;

    public abstract PayloadArg createPayloadParameters(PayloadArg payloadArg) throws ADCException;
}

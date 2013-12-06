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

package org.apache.stratos.adc.mgt.subscription;

import org.apache.stratos.adc.mgt.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.exception.NotSubscribedException;
import org.apache.stratos.adc.mgt.exception.UnregisteredCartridgeException;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;

import java.util.Properties;

public class ApplicationCartridgeSubscription extends AbstractCartridgeSubscription {


    /**
     * Constructor
     *
     * @param cartridgeInfo CartridgeInfo subscription
     */
    public ApplicationCartridgeSubscription(CartridgeInfo cartridgeInfo) {
        super(cartridgeInfo);
    }

    @Override
    public CartridgeSubscriptionInfo registerSubscription(Properties properties) throws ADCException, UnregisteredCartridgeException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeSubscription() throws ADCException, NotSubscribedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

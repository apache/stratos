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

package org.apache.stratos.adc.mgt.test;

import junit.framework.TestCase;
import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
import org.apache.stratos.adc.mgt.subscription.factory.CartridgeSubscriptionFactory;
import org.apache.stratos.cloud.controller.util.xsd.CartridgeInfo;

public class CartridgeSubscriptionTest extends TestCase {

    private CartridgeSubscription getCartridgeInstance (CartridgeInfo cartridgeInfo) {

        try {
            return CartridgeSubscriptionFactory.getCartridgeSubscriptionInstance(cartridgeInfo);

        } catch (ADCException e) {
            throw new RuntimeException(e);
        }
    }

    public void testCarbonCartridge () {

        CartridgeInfo cartridgeInfo = new CartridgeInfo();
        cartridgeInfo.setMultiTenant(true);
        cartridgeInfo.setType("esb");
        assertNotNull(getCartridgeInstance(cartridgeInfo));
    }
     //TODO FIXME
    /*public void testPhpCartridgeInstance () {

        CartridgeInfo cartridgeInfo = new CartridgeInfo();
        cartridgeInfo.setMultiTenant(false);
        cartridgeInfo.setType("php");
        assertNotNull(getCartridgeInstance(cartridgeInfo));
    }

    public void testMySqlCartridgeInstance () {

        CartridgeInfo cartridgeInfo = new CartridgeInfo();
        cartridgeInfo.setMultiTenant(false);
        cartridgeInfo.setType("mysql");
        assertNotNull(getCartridgeInstance(cartridgeInfo));
    }

    public void testTomcatCartridgeInstance () {

        CartridgeInfo cartridgeInfo = new CartridgeInfo();
        cartridgeInfo.setMultiTenant(false);
        cartridgeInfo.setType("tomcat");
        assertNotNull(getCartridgeInstance(cartridgeInfo));
    }*/
}

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

package org.apache.stratos.manager.subscription.factory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.lb.category.LBDataContext;
import org.apache.stratos.manager.lb.category.LoadBalancerCategory;
import org.apache.stratos.manager.subscription.*;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionTenancyBehaviour;
import org.apache.stratos.manager.utils.CartridgeConstants;

public class CartridgeSubscriptionFactory {

    private static Log log = LogFactory.getLog(CartridgeSubscriptionFactory.class);

    /**
     * Returns the relevant CartridgeSubscription object for the given criteria
     *
     * @param cartridgeInfo CartridgeInfo instance
     * @param subscriptionTenancyBehaviour SubscriptionTenancyBehaviour instance
     * @return CartridgeSubscription subscription
     * @throws ADCException if no matching criteria is there to create a CartridgeSubscription object
     */

    public static CartridgeSubscription getCartridgeSubscriptionInstance(CartridgeInfo cartridgeInfo,
                                                                         SubscriptionTenancyBehaviour subscriptionTenancyBehaviour)
            throws ADCException {

        //TODO: fix the logic properly
        CartridgeSubscription cartridgeSubscription = null;
        if(cartridgeInfo.getMultiTenant()) {
            cartridgeSubscription = new FrameworkCartridgeSubscription(cartridgeInfo, subscriptionTenancyBehaviour);

        } else {
            // TODO: fix properly with cartridgeCategory element
            if(cartridgeInfo.getProvider().equals(CartridgeConstants.DATA_CARTRIDGE_PROVIDER)) {
                cartridgeSubscription = new DataCartridgeSubscription(cartridgeInfo, subscriptionTenancyBehaviour);
            }
            else if (cartridgeInfo.getProvider().equals("application")) {
                cartridgeSubscription = new ApplicationCartridgeSubscription(cartridgeInfo, subscriptionTenancyBehaviour);
            }
            else {
                cartridgeSubscription = new FrameworkCartridgeSubscription(cartridgeInfo, subscriptionTenancyBehaviour);
            }
        }

        if(cartridgeSubscription == null) {
            throw new ADCException("Unable to create a CartridgeSubscription subscription for " + cartridgeInfo);
        }

        return cartridgeSubscription;
    }

    public static CartridgeSubscription getLBCartridgeSubscriptionInstance (LBDataContext lbDataContext, LoadBalancerCategory loadBalancerCategory)
            throws ADCException {

        if (!lbDataContext.getLbCartridgeInfo().getProvider().equals("loadbalancer") && !lbDataContext.getLbCartridgeInfo().getProvider().equals("lb")) {
            throw new ADCException("LB cartridge provider should be either lb or loadbalancer");
        }

        return new LBCartridgeSubscription(lbDataContext.getLbCartridgeInfo(), null, loadBalancerCategory);
    }
}

/**
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.payment.paypal.internal;

import com.paypal.sdk.profiles.APIProfile;
import com.paypal.sdk.profiles.ProfileFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.business.messaging.paypal.integration.PaypalSOAPProxy;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.stratos.common.util.StratosConfiguration;
import org.wso2.carbon.payment.paypal.services.PaypalService;

/**
 * @scr.component name="org.wso2.carbon.payment.paypal" immediate="true"
 */
public class PaymentServiceComponent {

    private static Log log = LogFactory.getLog(PaymentServiceComponent.class);

    protected void activate(ComponentContext ctxt){
        log.debug("Activating PaymentService Bundle");
        try{
            if(CommonUtil.getStratosConfig()==null){
                StratosConfiguration stratosConfig = CommonUtil.loadStratosConfiguration();
                CommonUtil.setStratosConfig(stratosConfig);
            }

            //create the APIProfile
            APIProfile profile = ProfileFactory.createSignatureAPIProfile();
            profile.setAPIUsername(CommonUtil.getStratosConfig().getPaypalAPIUsername());
            profile.setAPIPassword(CommonUtil.getStratosConfig().getPaypalAPIPassword());
            profile.setSignature(CommonUtil.getStratosConfig().getPaypalAPISignature());
            profile.setEnvironment(CommonUtil.getStratosConfig().getPaypalEnvironment());

            PaypalService.proxy = PaypalSOAPProxy.createPaypalSOAPProxy(profile);
            log.info("PaymentService Bundle activated");
        }catch(Throwable e){
            log.error("Error occurred while creating PayPalProxy: " + e.getMessage(), e);
        }
    }

    protected void deactivate(ComponentContext ctxt){
        //TODO: Do I have to null the soapproxy object?
        log.debug("PayPalService Bundle deactivated");
    }

}

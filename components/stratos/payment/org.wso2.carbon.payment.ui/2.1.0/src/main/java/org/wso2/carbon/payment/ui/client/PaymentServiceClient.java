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

package org.wso2.carbon.payment.ui.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.payment.stub.StratosPaymentStub;
import org.wso2.carbon.payment.stub.dto.ECDetailResponse;
import org.wso2.carbon.payment.stub.dto.ECResponse;
import org.wso2.carbon.payment.stub.dto.TransactionResponse;


public class PaymentServiceClient {

    private StratosPaymentStub stub;

    private static final Log log = LogFactory.getLog(PaymentServiceClient.class);

    public PaymentServiceClient(ConfigurationContext configCtx, String backendServerURL,
                                String cookie) throws AxisFault{

        String serviceURL = backendServerURL + "StratosPayment";
        stub = new StratosPaymentStub(configCtx, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    public ECResponse initExpressCheckout(String amount, String successUrl, String cancelUrl){
        ECResponse response = null;
        try {
            response = stub.initExpressCheckout(amount, successUrl, cancelUrl);
        } catch (Exception e) {
            log.error("Error occurred while initiating express checkout transaction: " +
                        e.getMessage());
        }
        return response;
    }

    public ECDetailResponse getExpressCheckoutDetails(String token) throws Exception{
        ECDetailResponse detailResponse = null;

        try{
            detailResponse = stub.getExpressCheckoutDetails(token);
        }catch (Exception e){
            log.error("Error occurred while getting express checkout details " +
                        e.getMessage());
            throw e;
        }
        return detailResponse;
    }

    public TransactionResponse doExpressCheckout(String token, String payerId, String amount,
                                  String tenantDomain) throws Exception{
        TransactionResponse tr = new TransactionResponse();
        try{
            tr = stub.doExpressCheckout(token, payerId, amount, tenantDomain);
        }catch (Exception e){
            log.error("Error occurred while DoExpressCheckout operation: " + e.getMessage());
            throw e;
        }

        return tr;
    }

}

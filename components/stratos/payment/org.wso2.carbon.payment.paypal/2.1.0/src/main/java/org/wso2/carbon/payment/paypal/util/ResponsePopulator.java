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
package org.wso2.carbon.payment.paypal.util;

import com.paypal.soap.api.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.payment.paypal.dto.*;

/**
 * Populates the response received from paypal to simple objects
 */
public class ResponsePopulator {

    private static Log log = LogFactory.getLog(ResponsePopulator.class);

    //Populates the response received after initiating an EC scenario
    public ECResponse populateSetECResponse(SetExpressCheckoutResponseType response){
        ECResponse ecr = new ECResponse();
        //if the response is null, return an empty object
        if(response==null){
            return ecr;
        }else{
            ecr.setTimestamp(response.getTimestamp().toString());
            ecr.setAck(response.getAck().toString());
            //if ack is success, set the token
            if(PaymentConstants.RESPONSE_SUCCESS.equals(ecr.getAck())){
                ecr.setToken(response.getToken());
            }
            //if ack is failure, then set error
            if(PaymentConstants.RESPONSE_FAILURE.equals(ecr.getAck())){
                PaypalError error = new PaypalError();
                ErrorType et = response.getErrors(0);
                if(et!=null){
                    error.setErrorCode(et.getErrorCode().toString());
                    error.setShortMessage(et.getShortMessage());
                    error.setLongMessage(et.getLongMessage());
                }
                ecr.setError(error);
            }
        }
        return ecr;
    }

    //Populating the response received when getting EC details
    public ECDetailResponse populateECDetailResponse (GetExpressCheckoutDetailsResponseType response){
        ECDetailResponse ecdr = new ECDetailResponse();
        //if the response is null, return empty object
        if(response==null){
            return ecdr;
        }else{
            ecdr.setTimestamp(response.getTimestamp().toString());
            ecdr.setAck(response.getAck().toString());
            if(PaymentConstants.RESPONSE_SUCCESS.equals(ecdr.getAck())){
                GetExpressCheckoutDetailsResponseDetailsType responseDetails =
                        response.getGetExpressCheckoutDetailsResponseDetails();

                ecdr.setToken(responseDetails.getToken().toString());

                PayerInfoType payerInfo = responseDetails.getPayerInfo();
                Payer payer = new Payer();
                if(payerInfo!=null){
                    payer.setBusiness(payerInfo.getPayerBusiness());
                    payer.setPayerId(payerInfo.getPayerID());
                    PersonNameType personName = payerInfo.getPayerName();
                    if(personName!=null){
                        payer.setFirstName(personName.getFirstName());
                        payer.setLastName(personName.getLastName());
                    }
                    ecdr.setPayer(payer);
                }

                AddressType addressType = responseDetails.getBillingAddress();
                Address address = new Address();
                if(addressType!=null){
                    address.setName(addressType.getName());
                    address.setStreet1(addressType.getStreet1());
                    address.setStreet2(addressType.getStreet2());
                    address.setCity(addressType.getCityName());
                    address.setCountry(addressType.getCountryName());
                    address.setPostalCode(addressType.getPostalCode());
                    address.setState(addressType.getStateOrProvince());
                    address.setPhone(addressType.getPhone());
                }
                ecdr.setAddress(address);
                PaymentDetailsType [] paymentDetailsArr = responseDetails.getPaymentDetails();
                PaymentDetailsType paymentDetails = paymentDetailsArr[0];
                if(paymentDetails!=null){
                    BasicAmountType bat = paymentDetails.getOrderTotal();
                    if(bat!=null){
                        ecdr.setOrderTotal(bat.get_value());
                    }

                }

            }else{
                PaypalError error = new PaypalError();
                ErrorType et = response.getErrors(0);
                if(et!=null){
                    error.setErrorCode(et.getErrorCode().toString());
                    error.setShortMessage(et.getShortMessage());
                    error.setLongMessage(et.getLongMessage());
                }
                ecdr.setError(error);
            }
            
        }

        return ecdr;
    }

    //Populate the response received after doing the transaction
    public TransactionResponse populateDoECResponse (DoExpressCheckoutPaymentResponseType response){
        TransactionResponse tr = new TransactionResponse();

        tr.setTimestamp(response.getTimestamp().toString());
        tr.setAck(response.getAck().toString());

        if(PaymentConstants.RESPONSE_SUCCESS.equals(tr.getAck())){
            DoExpressCheckoutPaymentResponseDetailsType responseDetails =
                    response.getDoExpressCheckoutPaymentResponseDetails();
            tr.setToken(responseDetails.getToken());
            tr.setTransactionId(responseDetails.getPaymentInfo()[0].getTransactionID());
            PaymentInfoType [] paymentInfoArr = responseDetails.getPaymentInfo();
            PaymentInfoType paymentInfo = paymentInfoArr[0];
            //TODO: I am not sure whether this is the correct amount value
            tr.setAmount(paymentInfo.getGrossAmount().get_value());
            tr.setPaymentStatus(paymentInfo.getPaymentStatus().toString());
            log.debug("Payment Status: " + paymentInfo.getPaymentStatus().toString());
        }else{
            ErrorType et = response.getErrors(0);
            PaypalError error = new PaypalError();
            error.setErrorCode(et.getErrorCode().toString());
            error.setShortMessage(et.getShortMessage());
            error.setLongMessage(et.getLongMessage());

            tr.setError(error);
        }

        //Return the transaction response object
        return tr;
    }
}

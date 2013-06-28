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

package org.wso2.carbon.payment.paypal.dto;

/**
 * Class to hold the response coming after initiating an
 * Express Checkout scenario
 */

public class ECResponse {

    //Timestamp which the payment scenario initiated
    private String timestamp;

    //Ack received for the SetEC request
    private String ack;

    //Token received with the response. This is necessary in the future steps
    private String token;

    //PaypalError details if an error occurs
    private PaypalError error;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getAck() {
        return ack;
    }

    public void setAck(String ack) {
        this.ack = ack;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public PaypalError getError() {
        return error;
    }

    public void setError(PaypalError error) {
        this.error = error;
    }
}

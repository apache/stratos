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

package org.apache.stratos.adc.mgt.payload;

import java.io.Serializable;

public abstract class PayloadData implements Serializable {

    protected StringBuilder additionalPayloadDataBuilder;
    //private Map<String, String> payloadDataMap;
    private BasicPayloadData basicPayloadData;

    public PayloadData(BasicPayloadData basicPayloadData) {
        this.setBasicPayloadData(basicPayloadData);
        additionalPayloadDataBuilder = new StringBuilder();
        //payloadDataMap = new HashMap<String, String>();
    }

    public void add (String payloadDataName, String payloadDataValue) {

        if(additionalPayloadDataBuilder.length() > 0) {
            additionalPayloadDataBuilder.append(",");
        }

        //payloadDataMap.put(payloadDataName, payloadDataValue);
        additionalPayloadDataBuilder.append(payloadDataName + "=" + payloadDataValue);
    }

    /*public String getPayloadDataValue (String payloadDataName) {
        return payloadDataMap.get(payloadDataName);
    }*/

    public StringBuilder getCompletePayloadData () {
        return additionalPayloadDataBuilder.append(",").append(getBasicPayloadData().getPayloadData());
    }

    public BasicPayloadData getBasicPayloadData() {
        return basicPayloadData;
    }

    public void setBasicPayloadData(BasicPayloadData basicPayloadData) {
        this.basicPayloadData = basicPayloadData;
    }
}

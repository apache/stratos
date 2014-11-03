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

package org.apache.stratos.autoscaler.applications.payload;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class PayloadData implements Serializable {

    private BasicPayloadData basicPayloadData;
    private Map<String, String> completePayloadMap;
    public PayloadData(BasicPayloadData basicPayloadData) {

        this.setBasicPayloadData(basicPayloadData);
        completePayloadMap = new HashMap<String, String>();
    }

    public void add (String payloadDataName, String payloadDataValue) {
        completePayloadMap.put(payloadDataName, payloadDataValue);
    }

    public StringBuilder getCompletePayloadData () {

        //return completePayloadMap;
        StringBuilder completePayload = new StringBuilder();
        completePayload.append(basicPayloadData.getPayloadData());
        Iterator< String > iter = completePayloadMap.keySet().iterator();
        while(iter.hasNext()) {
            String key = iter.next();
            String val = completePayloadMap.get(key);
            if(completePayload.length() > 0){
                completePayload.append(",");
            }
            completePayload.append(key + "=" + val);
        }
        return completePayload;
    }

    public BasicPayloadData getBasicPayloadData() {
        return basicPayloadData;
    }

    public void setBasicPayloadData(BasicPayloadData basicPayloadData) {
        this.basicPayloadData = basicPayloadData;
    }

    public String toString () {
        return getCompletePayloadData().toString();
    }
}

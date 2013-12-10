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

package org.apache.stratos.adc.mgt.utils;

import org.apache.stratos.adc.mgt.lookup.ClusterIdToCartridgeSubscriptionMap;
import org.apache.stratos.adc.mgt.lookup.SubscriptionAliasToCartridgeSubscriptionMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class Serializer {

    /**
     * Serialize a SubscriptionAliasToCartridgeSubscriptionMap to a byte array.
     * @param aliasToSubscriptionMap
     * @return byte[]
     * @throws java.io.IOException
     */
    public static byte[] serializeAliasToSubscriptionMapToByteArray (SubscriptionAliasToCartridgeSubscriptionMap aliasToSubscriptionMap)
            throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(aliasToSubscriptionMap);

            return bos.toByteArray();

        } finally {
            if (out != null) {
                out.close();
            }
            bos.close();
        }

    }

    /**
     * Serialize a ClusterIdToCartridgeSubscriptionMap to a byte array.
     * @param clusterIdToSubscriptionMap
     * @return byte[]
     * @throws java.io.IOException
     */
    public static byte[] serializeClusterIdToSubscriptionMapToByteArray (ClusterIdToCartridgeSubscriptionMap clusterIdToSubscriptionMap)
            throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(clusterIdToSubscriptionMap);

            return bos.toByteArray();

        } finally {
            if (out != null) {
                out.close();
            }
            bos.close();
        }

    }
}

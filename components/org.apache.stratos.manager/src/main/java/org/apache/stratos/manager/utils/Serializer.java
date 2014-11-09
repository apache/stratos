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

package org.apache.stratos.manager.utils;

import org.apache.stratos.manager.composite.application.beans.ApplicationDefinition;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.grouping.definitions.ServiceGroupDefinition;
import org.apache.stratos.manager.lookup.ClusterIdToSubscription;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.subscription.ApplicationSubscription;
import org.apache.stratos.manager.subscription.GroupSubscription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class Serializer {

    /**
     * Serialize a SubscriptionContext instance to a byte array.
     * @param cartridgeSubscription
     * @return byte[]
     * @throws java.io.IOException
     */
    public static byte[] serializeSubscriptionSontextToByteArray(CartridgeSubscription cartridgeSubscription)
            throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(cartridgeSubscription);

            return bos.toByteArray();

        } finally {
            if (out != null) {
                out.close();
            }
            bos.close();
        }

    }

    public static byte[] serializeGroupSubscriptionToByteArray (GroupSubscription groupSubscription)
            throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(groupSubscription);

            return bos.toByteArray();

        } finally {
            if (out != null) {
                out.close();
            }
            bos.close();
        }

    }

    public static byte[] serializeCompositeAppSubscriptionToByteArray (ApplicationSubscription compositeAppSubscription)
            throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(compositeAppSubscription);

            return bos.toByteArray();

        } finally {
            if (out != null) {
                out.close();
            }
            bos.close();
        }

    }

    public static byte[] serializeServiceToByteArray(Service service)
            throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(service);

            return bos.toByteArray();

        } finally {
            if (out != null) {
                out.close();
            }
            bos.close();
        }

    }

    public static byte [] serializeServiceGroupDefinitionToByteArray (ServiceGroupDefinition serviceGroupDefinition)
            throws  IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(serviceGroupDefinition);

            return bos.toByteArray();

        } finally {
            if (out != null) {
                out.close();
            }
            bos.close();
        }
    }
}

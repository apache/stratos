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

package org.apache.stratos.messaging.broker.connect;

import org.apache.stratos.messaging.util.Util;

import java.io.File;
import java.util.Properties;

public class MqttConstants {

    public static final String MQTT_URL_DEFAULT = "defaultValue";

    /**
     * Quality of Service for message delivery:
     * Setting it to 2 to make sure that message is guaranteed to deliver once
     * using two-phase acknowledgement across the network.
     */
    public static final int QOS = 2;
    public static String CONFIG_FILE_LOCATION = System.getProperty("jndi.properties.dir");
    public static Properties MQTT_PROPERTIES = Util.getProperties(CONFIG_FILE_LOCATION
            + File.separator + "mqtttopic.properties");
}

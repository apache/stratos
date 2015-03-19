/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.messaging.util;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.message.JsonMessage;
import org.apache.commons.lang.math.NumberUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

/**
 * Messaging module utility class
 */
public class MessagingUtil {

    private static final Log log = LogFactory.getLog(MessagingUtil.class);

    private static final int START_INDEX = 0;
    private static final String SLASH = "/";
    private static final String DOT = ".";
    private static final String HASH = "#";
    private static final String GREATER_THAN = ">";
    private static final String ORG_APACHE_STRATOS_MESSAGING_EVENT_PACKAGE = "org.apache.stratos.messaging.event.";
    private static final String HYPHEN_MINUS = "-";
    private static final String EMPTY_SPACE = "";
    private static final String TENANT_RANGE_DELIMITER = "-";
    private static final String AVERAGE_PING_INTERVAL_PROPERTY = "stratos.messaging.averagePingInterval";
    private static final String FAILOVER_PING_INTERVAL_PROPERTY = "stratos.messaging.failoverPingInterval";
    private static final int DEFAULT_AVERAGE_PING_INTERVAL = 1000;
    private static final int DEFAULT_FAILOVER_PING_INTERVAL = 30000;

    // Time interval between each ping message sent to topic.
    private static int averagePingInterval;
    // Time interval between each ping message after an error had occurred.
    private static int failoverPingInterval;

    /**
     * Enum for Messaging topics
     */
    public static enum Topics {
        TOPOLOGY_TOPIC("topology/#"),
        HEALTH_STAT_TOPIC("summarized-health-stats/#"),
        INSTANCE_STATUS_TOPIC("instance/status/#"),
        INSTANCE_NOTIFIER_TOPIC("instance/notifier/#"),
        APPLICATION_TOPIC("application/#"),
        APPLICATION_SIGNUP_TOPIC("application/signup/#"),
        CLUSTER_STATUS_TOPIC("cluster/status/#"),
        TENANT_TOPIC("tenant/#"),
        DOMAIN_MAPPING_TOPIC("domain/mapping/#");

        private String topicName;

        private Topics(String topicName) {
            if (getMessagingProtocol().equals(MessagingConstants.AMQP)) {
                topicName = topicName.replace(SLASH, DOT).replace(HASH, GREATER_THAN);
            }
            this.topicName = topicName;
        }

        /**
         * Get the topic name
         *
         * @return topic name
         */
        public String getTopicName() {
            return topicName;
        }

    }

    /**
     * Properties of the given file
     *
     * @param filePath path of the property file
     * @return Properties of the given file
     */
    public static Properties getProperties(String filePath) {
        Properties props = new Properties();
        InputStream is = null;

        // load properties from a properties file
        try {
            File f = new File(filePath);
            is = new FileInputStream(f);
            props.load(is);
        } catch (Exception e) {
            log.error("Failed to load properties from file: " + filePath, e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ignore) {
            }
        }

        return props;
    }

    /**
     * Validate tenant range.
     * Valid formats: Integer-Integer, Integer-*
     * Examples: 1-100, 101-200, 201-*
     *
     * @param tenantRange Tenant range
     */
    public static void validateTenantRange(String tenantRange) {
        boolean valid = false;
        if (tenantRange != null) {
            if (tenantRange.equals("*")) {
                valid = true;
            } else {
                String[] array = tenantRange.split(TENANT_RANGE_DELIMITER);
                if (array.length == 2) {
                    // Integer-Integer
                    if (NumberUtils.isNumber(array[0]) && (NumberUtils.isNumber(array[1]))) {
                        valid = true;
                    }
                    // Integer-*
                    else if (NumberUtils.isNumber(array[0]) && "*".equals(array[1])) {
                        valid = true;
                    }
                }
            }

        }
        if (!valid)
            throw new RuntimeException(String.format("Tenant range %s is not valid", tenantRange));
    }


    /**
     * Transform json into an object of given type.
     *
     * @param json json string
     * @param type type of the class
     * @return Object of the json String
     */
    public static Object jsonToObject(String json, Class type) {
        return (new JsonMessage(json, type)).getObject();
    }

    /**
     * Create a JSON string
     *
     * @param obj object
     * @return JSON string
     */
    public static String ObjectToJson(Object obj) {
        Gson gson = new Gson();
        String result = gson.toJson(obj);
        return result;
    }

    /**
     * Fetch value from system param
     *
     * @return Fail over ping interval
     */
    public static int getSubscriberFailoverInterval() {
        if (failoverPingInterval <= 0) {
            failoverPingInterval =
                    MessagingUtil.getNumericSystemProperty(DEFAULT_FAILOVER_PING_INTERVAL,
                            FAILOVER_PING_INTERVAL_PROPERTY);
        }
        return failoverPingInterval;
    }

    /**
     * Method to safely access numeric system properties
     *
     * @param defaultValue default value of the property
     * @param propertyKey  property key
     * @return Numeric system properties
     */
    public static Integer getNumericSystemProperty(Integer defaultValue, String propertyKey) {
        try {
            return Integer.valueOf(System.getProperty(propertyKey));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public static String getMessagingProtocol() {
        return System.getProperty(MessagingConstants.MESSAGING_TRANSPORT, MessagingConstants.AMQP);
    }

    /**
     * Get the Message topic name for event
     *
     * @param event event name
     * @return String topic name of the event
     */
    public static String getMessageTopicName(Event event) {

        String topicName = event.getClass().getName().substring(ORG_APACHE_STRATOS_MESSAGING_EVENT_PACKAGE.length());
        if (getMessagingProtocol().equals(MessagingConstants.MQTT)) {
            topicName = topicName.replace(DOT, SLASH);
        }
        return topicName;
    }

    /**
     * Get the event name for topic
     *
     * @param topic topic Name
     * @return String Event name for topic
     */
    public static String getEventClassNameForTopic(String topic) {
        String eventClassName = ORG_APACHE_STRATOS_MESSAGING_EVENT_PACKAGE.concat(topic);
        if (getMessagingProtocol().equals(MessagingConstants.MQTT)) {
            eventClassName = eventClassName.replace(SLASH, DOT);
        }
        return eventClassName;
    }

    /**
     * Get the random string with UUID
     *
     * @param len length of the String
     * @return Random String
     */
    public static String getRandomString(int len) {
        return UUID.randomUUID().toString().replace(HYPHEN_MINUS, EMPTY_SPACE).substring(START_INDEX, len);
    }

}
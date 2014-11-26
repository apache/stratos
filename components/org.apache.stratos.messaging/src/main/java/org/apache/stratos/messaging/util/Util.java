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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

/**
 *  Messaging module utility class
 */
public class Util {
	private static final Log log = LogFactory.getLog(Util.class);
	public static final int BEGIN_INDEX = 35;
	// Time interval between each ping message sent to topic.
	private static int averagePingInterval;
	// Time interval between each ping message after an error had occurred.
	private static int failoverPingInterval;

	/**
	 * Properties of the given file
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
	 * @param tenantRange Tenant range
	 */
	public static void validateTenantRange(String tenantRange) {
		boolean valid = false;
		if (tenantRange != null) {
			if (tenantRange.equals("*")) {
				valid = true;
			} else {
				String[] array = tenantRange.split(Constants.TENANT_RANGE_DELIMITER);
				if (array.length == 2) {
					// Integer-Integer
					if (isNumber(array[0]) && (isNumber(array[1]))) {
						valid = true;
					}
					// Integer-*
					else if (isNumber(array[0]) && "*".equals(array[1])) {
						valid = true;
					}
				}
			}

		}
		if (!valid)
			throw new RuntimeException(String.format("Tenant range %s is not valid", tenantRange));
	}

	/**
	 * Check given string is a number
	 * @param inputStr String to be checked
	 * @return Boolean of given string is a number
	 */
    public static boolean isNumber(String inputStr) {
        try {
            Integer.parseInt(inputStr);
            return true;
        }
        catch (NumberFormatException ignore) {
            // Not a valid number
        }
        return false;
    }
    
    /**
     * Transform json into an object of given type.
     * @param json json string
     * @param type type of the class
     * @return Object of the json String
     */
    public static Object jsonToObject(String json, Class type) {
        return (new JsonMessage(json, type)).getObject();
    }

	/**
	 * Create a JSON string
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
	 * @return Average ping interval
	 */
	public static int getAveragePingInterval() {
		if (averagePingInterval <= 0) {
			averagePingInterval =
			                      Util.getNumericSystemProperty(Constants.DEFAULT_AVERAGE_PING_INTERVAL,
			                                                    Constants.AVERAGE_PING_INTERVAL_PROPERTY);
		}
		return averagePingInterval;
	}

	/**
	 * Fetch value from system param
	 * @return Fail over ping interval
	 */
	public static int getFailoverPingInterval() {
		if (failoverPingInterval <= 0) {
			failoverPingInterval =
			                       Util.getNumericSystemProperty(Constants.DEFAULT_FAILOVER_PING_INTERVAL,
			                                                     Constants.FAILOVER_PING_INTERVAL_PROPERTY);
		}
		return failoverPingInterval;
	}

	/**
	 * Method to safely access numeric system properties
	 * @param defaultValue default value of the property
	 * @param propertyKey property key
	 * @return Numeric system properties
	 */
	public static Integer getNumericSystemProperty(Integer defaultValue, String propertyKey) {
		try {
			return Integer.valueOf(System.getProperty(propertyKey));
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	/**
	 * Get the Message topic name for event
	 * @param event event name
	 * @return String topic name of the event
	 */
	public static String getMessageTopicName(Event event) {
		return event.getClass().getName().substring(BEGIN_INDEX).replace(".", "/");
	}

	/**
	 * Get the event name for topic
	 * @param topic topic Name
	 * @return String Event name for topic
	 */
	public static String getEventNameForTopic(String topic) {
		return "org.apache.stratos.messaging.event.".concat(topic.replace("/", "."));
	}

	/**
	 * Get the random string with UUID
	 * @param len length of the String
	 * @return Random String
	 */

	public static String getRandomString(int len) {
		return UUID.randomUUID().toString().replace("-","").substring(0,len);
	}
}

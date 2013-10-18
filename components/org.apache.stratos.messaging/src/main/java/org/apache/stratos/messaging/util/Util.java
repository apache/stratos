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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.message.JsonMessage;

public class Util {
	private static final Log log = LogFactory.getLog(Util.class);

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
				is.close();
			} catch (IOException ignore) {
			}
		}

		return props;
	}

    /**
     * Validate tenant range.
     * Valid formats: Integer-Integer, Integer-*
     * Examples: 1-100, 101-200, 201-*
     * @param tenantRange
     */
    public static void validateTenantRange(String tenantRange) {
        boolean valid = false;
        if(tenantRange != null) {
            String[] array = tenantRange.split(Constants.TENANT_RANGE_DELIMITER);
            if(array.length == 2) {
                // Integer-Integer
                if(isNumber(array[0]) && (isNumber(array[1]))){
                    valid = true;
                }
                // Integer-*
                else if(isNumber(array[0]) && "*".equals(array[1])) {
                    valid = true;
                }
            }
        }
        if(!valid)
            throw new RuntimeException(String.format("Tenant range %s is not valid", tenantRange));
    }

    public static boolean isNumber(String s) {
        try {
            Integer.parseInt(s);
            return true;
        }
        catch (NumberFormatException e) {
        }
        return false;
    }
    
    /**
     * Transform json into an object of given type.
     * @param json
     * @param type
     * @return
     */
    public static Object jsonToObject(String json, Class type) {
        return (new JsonMessage(json, type)).getObject();
    }

}

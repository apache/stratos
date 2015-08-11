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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class CartridgeConfigFileReader {

    private static final Log log = LogFactory.getLog(CartridgeConfigFileReader.class);
    private static String carbonHome = CarbonUtils.getCarbonHome();

    /**
     * Reads cartridge-config.properties file and assign properties to system
     * properties
     */
    public static void readProperties() {

        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(carbonHome + File.separator + "repository" +
                    File.separator + "conf" + File.separator +
                    "cartridge-config.properties"));
        } catch (Exception e) {
            log.error("Exception is occurred in reading properties file. Reason:" + e.getMessage());
        }
        if (log.isInfoEnabled()) {
            log.info("Setting config properties into System properties");
        }

        if (log.isDebugEnabled()) {
            log.debug("Start reading properties and set it as system properties");
        }
        SecretResolver secretResolver = SecretResolverFactory.create(properties);
        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            if (log.isDebugEnabled()) {
                log.debug(" >>> Property Name :" + name + " Property Value :" + value);
            }
            if (value.equalsIgnoreCase("secretAlias:" + name)) {
                if (log.isDebugEnabled()) {
                    log.debug("Secret Alias Found : " + name);
                }
                if (secretResolver != null && secretResolver.isInitialized()) {
                    if (log.isDebugEnabled()) {
                        log.debug("SecretResolver is initialized ");
                    }
                    if (secretResolver.isTokenProtected(name)) {
                        if (log.isDebugEnabled()) {
                            log.debug("SecretResolver [" + name + "] is token protected");
                        }
                        value = secretResolver.resolve(name);
                        if (log.isDebugEnabled()) {
                            log.debug("SecretResolver [" + name + "] is decrypted properly");
                        }
                    }
                }
            }

            System.setProperty(name, value);
        }
    }
}

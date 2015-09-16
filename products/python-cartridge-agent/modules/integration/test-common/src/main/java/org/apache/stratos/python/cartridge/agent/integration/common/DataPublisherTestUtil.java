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

package org.apache.stratos.python.cartridge.agent.integration.common;

import java.io.File;

public class DataPublisherTestUtil {
    public static final String LOCAL_HOST = "localhost";
    public static final String PATH_SEP = File.separator;

    public static void setTrustStoreParams() {
        String trustStore = DataPublisherTestUtil.getCommonResourcesPath();
        System.setProperty("javax.net.ssl.trustStore", trustStore + File.separator + "client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
    }

    public static void setKeyStoreParams() {
        String keyStore = DataPublisherTestUtil.getCommonResourcesPath();
        System.setProperty("Security.KeyStore.Location", keyStore + File.separator + "wso2carbon.jks");
        System.setProperty("Security.KeyStore.Password", "wso2carbon");
    }

    public static String getDataAgentConfigPath() {
        String filePath = DataPublisherTestUtil.getCommonResourcesPath();
        return filePath + File.separator + "data-agent-config.xml";
    }

    public static String getDataBridgeConfigPath() {
        String filePath = DataPublisherTestUtil.getCommonResourcesPath();
        return filePath + File.separator + "data-bridge-config.xml";
    }

    public static String getCommonResourcesPath() {
        return DataPublisherTestUtil.class.getResource(PATH_SEP).getPath() + PATH_SEP + ".." + PATH_SEP + ".." +
                PATH_SEP + "src" + PATH_SEP + "test" + PATH_SEP + "resources" + PATH_SEP + "common";
    }
}
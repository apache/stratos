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

package org.apache.stratos.metadata.client.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadata.client.sample.MetaDataServiceClientSample;
import org.apache.stratos.metadata.client.util.MetaDataClientConstants;


import java.io.File;

public class MetaDataClientConfig {

    private static final Log log = LogFactory.getLog(MetaDataServiceClientSample.class);

    private String metaDataServiceBaseUrl;

    private String dataExtractorClass;

    private String username;

    private String password;

    private XMLConfiguration config;

    private static volatile MetaDataClientConfig metaDataClientConfig;

    private MetaDataClientConfig () {
        readConfig();
    }

    private void readConfig () throws RuntimeException{

        // the config file path is found from a system property
        String configFilePath = System.getProperty(MetaDataClientConstants.METADATA_CLIENT_CONFIG_FILE);
        if (configFilePath == null) {
            throw new RuntimeException("Unable to load the configuration file; no System Property found for " + MetaDataClientConstants.METADATA_CLIENT_CONFIG_FILE);
        }
        loadConfig(configFilePath);

        // read configurations
        metaDataServiceBaseUrl = config.getString(MetaDataClientConstants.METADATA_SERVICE_BASE_URL);
        if (metaDataServiceBaseUrl == null) {
            throw new RuntimeException("Unable to find metadata service base URL [ " +
                    MetaDataClientConstants.METADATA_SERVICE_BASE_URL  + " ] in the config file");
        }

        username = config.getString(MetaDataClientConstants.METADATA_SERVICE_USERNAME);
        if (username == null) {
            throw new RuntimeException("Meta data service username not defined in the configuration");
        }

        password = config.getString(MetaDataClientConstants.METADATA_SERVICE_PASSWORD);
        if (password == null) {
            throw new RuntimeException("Meta data service password not defined in the configuration");
        }

        dataExtractorClass = config.getString(MetaDataClientConstants.METADATA_CLIENT_DATA_EXTRACTOR_CLASS);
        if (dataExtractorClass == null) {
            if(log.isDebugEnabled()) {
                log.debug("No custom Data Extractor class detected in the configuration");
            }
        }
    }

    private void  loadConfig (String configFilePath) {

        try {

            File confFile;
            if (configFilePath != null && !configFilePath.isEmpty()) {
                confFile = new File(configFilePath);

            } else {
                confFile = new File(configFilePath);
            }

            config = new XMLConfiguration(confFile);

        } catch (ConfigurationException e) {
            String errorMsg = "Unable to load configuration file at " + configFilePath;
            throw new RuntimeException(errorMsg);
        }
    }

    public static MetaDataClientConfig getInstance () {

        if (metaDataClientConfig == null) {
            synchronized (MetaDataClientConfig.class) {
                if (metaDataClientConfig == null) {
                    metaDataClientConfig = new MetaDataClientConfig();
                }
            }
        }

        return metaDataClientConfig;
    }

    public String getMetaDataServiceBaseUrl() {
        return metaDataServiceBaseUrl;
    }

    public String getDataExtractorClass() {
        return dataExtractorClass;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }


/*
Sample Configuration file:

    <configuration>
        <metadataService>
            <baseUrl>localhost</baseUrl>
            <username>admin</username>
            <password>admin</password>
        </metadataService>
        <metadataClient>
            <dataExtractorClass>org.foo.MyDataExtractor</dataExtractorClass>
        </metadataClient>
    </configuration>

*/
}

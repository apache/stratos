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

package org.apache.stratos.cartridge.agent.config.configurator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Jndi configurator to configure message broker related settings and generate jndi.properties file.
 */
public class JndiConfigurator {
    private static final Log log = LogFactory.getLog(JndiConfigurator.class);

    public static void configure() {
        if(log.isDebugEnabled()) {
            log.debug("Configuring jndi.properties file");
        }
        generateJndiPropertiesFile();
    }

    private static void generateJndiPropertiesFile() {
        String mbIp = System.getProperty("mb.ip");
        if(StringUtils.isBlank(mbIp)) {
            throw new RuntimeException("System property not found: mb.ip");
        }
        String mbPort = System.getProperty("mb.port");
        if(StringUtils.isBlank(mbPort)) {
            throw new RuntimeException("System property not found: mb.port");
        }
        String templateFilePath =  System.getProperty("jndi.properties.template.file.path");
        if(StringUtils.isBlank(templateFilePath)) {
            throw new RuntimeException("System property not found: jndi.properties.template.file.path");
        }
        String jndiFileDir = System.getProperty("jndi.properties.dir");
        if(StringUtils.isBlank(jndiFileDir)) {
            throw new RuntimeException("System property not found: jndi.properties.dir");
        }
        String jndiFilePath = null;
        if(jndiFileDir.endsWith("/")) {
            jndiFilePath = jndiFileDir + "jndi.properties";
        }
        else {
            jndiFilePath = jndiFileDir + "/" + "jndi.properties";
        }

        File templateFile = new File(templateFilePath);
        if (!templateFile.exists()) {
            throw new RuntimeException(String.format("File not found: %s", templateFilePath));
        }
        try {
            // Read template file
            String content = readFileContent(templateFilePath);

            // Update message broker configuration
            content = content.replace("$mb_ip", mbIp);
            content = content.replace("$mb_port", mbPort);

            // Write jndi.properties file
            writeFileContent(content, jndiFilePath);
            if(log.isDebugEnabled()) {
                log.debug(String.format("jndi.properties file written to: %s", jndiFilePath));
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not write jndi.properties file", e);
        }
    }

    private static String readFileContent(String templateFilePath) throws IOException {
        FileInputStream inputStream = new FileInputStream(templateFilePath);
        try {
            return IOUtils.toString(inputStream);
        } finally {
            inputStream.close();
        }
    }

    private static void writeFileContent(String content, String filePath) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(filePath);
        IOUtils.write(content, outputStream);
    }
}

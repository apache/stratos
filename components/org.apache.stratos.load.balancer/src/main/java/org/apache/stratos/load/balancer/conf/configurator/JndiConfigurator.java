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

package org.apache.stratos.load.balancer.conf.configurator;

import org.apache.commons.io.IOUtils;
import org.apache.stratos.load.balancer.conf.LoadBalancerConfiguration;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Jndi configurator to configure message broker related settings and generate jndi.properties file.
 */
public class JndiConfigurator {

    public static void configure(LoadBalancerConfiguration configuration) {
        generateJndiPropertiesFile(configuration);
    }

    private static void generateJndiPropertiesFile(LoadBalancerConfiguration configuration) {
        String templateFilePath = CarbonUtils.getCarbonHome() + File.separator + "repository" + File.separator + "conf" + File.separator + "templates" + File.separator + "jndi.properties.template";
        File templateFile = new File(templateFilePath);
        if(!templateFile.exists()) {
            throw new RuntimeException("jndi.properties.template file not found");
        }
        try {
            // Read template file
            String content = readFileContent(templateFilePath);

            // Update message broker configuration
            content = content.replace("$mb_ip", configuration.getMbIp());
            content = content.replace("$mb_port", String.valueOf(configuration.getMbPort()));

            // Write jndi.properties file
            String jndiFilePath = CarbonUtils.getCarbonHome() + File.separator + "repository" + File.separator + "conf" + File.separator + "jndi.properties";
            writeFileContent(content, jndiFilePath);
        }
        catch (Exception e) {
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

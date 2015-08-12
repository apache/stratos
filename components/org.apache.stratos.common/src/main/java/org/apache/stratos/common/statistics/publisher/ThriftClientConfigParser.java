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

package org.apache.stratos.common.statistics.publisher;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.util.AxiomXpathParserUtil;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

import java.io.File;
import java.util.Iterator;

/**
 * Thrift client config parser.
 */
public class ThriftClientConfigParser {

    private static final Log log = LogFactory.getLog(ThriftClientConfigParser.class);

    /**
     * Fields to be read from the thrift-client-config.xml file
     */
    private static final String USERNAME_ELEMENT = "username";
    private static final String PASSWORD_ELEMENT = "password";
    private static final String IP_ELEMENT = "ip";
    private static final String PORT_ELEMENT = "port";

    /**
     * This method reads thrift-client-config.xml file and assign necessary credential
     * values into thriftClientInfo object.  A singleton design has been implemented
     * with the use of thriftClientIConfig class.
     * <p/>
     * The filePath argument is the path to thrift-client-config.xml file
     *
     * @param filePath the path to thrift-client-config.xml file
     * @return ThriftClientConfig object
     */
    public static ThriftClientConfig parse(String filePath) {
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Parsing thrift client config file: %s", filePath));
            }

            ThriftClientConfig thriftClientIConfig = new ThriftClientConfig();
            ThriftClientInfo thriftClientInfo = new ThriftClientInfo();
            thriftClientIConfig.setThriftClientInfo(thriftClientInfo);

            File configFile = new File(filePath);
            if (!configFile.exists()) {
                throw new RuntimeException(String.format("Thrift client config file does not exist: %s", filePath));
            }
            OMElement document = AxiomXpathParserUtil.parse(configFile);
            Iterator thriftClientIterator = document.getChildElements();

            //Initialize the SecretResolver providing the configuration element.
            SecretResolver secretResolver = SecretResolverFactory.create(document, false);

            String userNameValuesStr = null;
            String passwordValueStr = null;
            String ipValuesStr = null;
            String portValueStr = null;

            //same entry used in cipher-text.properties and cipher-tool.properties.
            String secretAlias = "thrift.client.configuration.password";

            // Iterate the thrift-client-config.xml file and read child element
            // consists of credential information necessary for ThriftStatisticsPublisher
            while (thriftClientIterator.hasNext()) {
                OMElement thriftClientElement = (OMElement) thriftClientIterator.next();

                if (USERNAME_ELEMENT.equals(thriftClientElement.getQName().getLocalPart())) {
                    userNameValuesStr = thriftClientElement.getText();
                    thriftClientInfo.setUsername(userNameValuesStr);
                }
                //password field protected using Secure vault
                if (PASSWORD_ELEMENT.equals(thriftClientElement.getQName().getLocalPart())) {
                    if ((secretResolver != null) && (secretResolver.isInitialized())) {
                        if (secretResolver.isTokenProtected(secretAlias)) {
                            passwordValueStr = secretResolver.resolve(secretAlias);
                        } else {
                            passwordValueStr = thriftClientElement.getText();
                        }
                    } else {
                        passwordValueStr = thriftClientElement.getText();
                    }
                    thriftClientInfo.setPassword(passwordValueStr);
                }

                if (IP_ELEMENT.equals(thriftClientElement.getQName().getLocalPart())) {
                    ipValuesStr = thriftClientElement.getText();
                    thriftClientInfo.setIp(ipValuesStr);
                }

                if (PORT_ELEMENT.equals(thriftClientElement.getQName().getLocalPart())) {
                    portValueStr = thriftClientElement.getText();
                    thriftClientInfo.setPort(portValueStr);
                }
            }

            if (userNameValuesStr == null) {
                throw new RuntimeException("Username value not found in thrift client configuration");
            }
            if (passwordValueStr == null) {
                throw new RuntimeException("Password not found in thrift client configuration ");
            }

            if (ipValuesStr == null) {
                throw new RuntimeException("Ip values not found in thrift client configuration ");
            }

            if (portValueStr == null) {
                throw new RuntimeException("Port not found in thrift client configuration ");
            }

            return thriftClientIConfig;
        } catch (Exception e) {
            throw new RuntimeException("Could not parse thrift client configuration", e);
        }
    }
}

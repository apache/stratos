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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.conf.LoadBalancerConfiguration;
import org.apache.stratos.load.balancer.conf.domain.Algorithm;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.utils.CarbonUtils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Synapse configurator to configure Synapse settings required for the load balancer.
 */
public class SynapseConfigurator {
    private static final Log log = LogFactory.getLog(SynapseConfigurator.class);

    /**
     * Configure Synapse using load balancer configuration.
     * @param configuration
     */
    public static void configure(LoadBalancerConfiguration configuration) {
        configureMainSequence(configuration);
    }
    /**
     * Configure main sequence send mediator endpoint.
     * @param configuration Load balancer configuration.
     */
    public static void configureMainSequence(LoadBalancerConfiguration configuration) {
        String filePath = CarbonUtils.getCarbonHome() + File.separator + "repository" + File.separator + "deployment" + File.separator + "server" + File.separator
                          + "synapse-configs" + File.separator + "default" + File.separator + "sequences" + File.separator + "main.xml";
        configureMainSequence(configuration, filePath, filePath);
    }

    /**
     * Configure main sequence send mediator endpoint.
     * @param configuration Load balancer configuration.
     * @param inputFilePath Input file path.
     * @param outputFilePath Output file path.
     */
    public static void configureMainSequence(LoadBalancerConfiguration configuration, String inputFilePath, String outputFilePath) {
        try {
            if(log.isInfoEnabled()) {
                log.info("Configuring synapse main sequence...");
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Reading synapse main sequence: %s", inputFilePath));
            }
            File inputFile = new File(inputFilePath);
            if(!inputFile.exists()) {
                throw new RuntimeException(String.format("File not found: %s", inputFilePath));
            }
            FileInputStream file = new FileInputStream(inputFile);
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(file);
            XPath xPath = XPathFactory.newInstance().newXPath();

            String expression = "/sequence/in/send/endpoint/class/parameter";
            if (log.isDebugEnabled()) {
                log.debug(String.format("xpath expression = %s", expression));
            }
            boolean updated = false;
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                Node parameter = node.getAttributes().getNamedItem("name");
                if (parameter.getNodeValue().equals("algorithmClassName")) {
                    String defaultAlgorithmName = configuration.getDefaultAlgorithmName();
                    if (StringUtils.isBlank(defaultAlgorithmName)) {
                        throw new RuntimeException("Default algorithm name not found in load balancer configuration");
                    }
                    Algorithm defaultAlgorithm = configuration.getAlgorithm(defaultAlgorithmName);
                    if (defaultAlgorithm == null) {
                        throw new RuntimeException("Default algorithm not found in load balancer configuration");
                    }
                    String algorithmClassName = defaultAlgorithm.getClassName();
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Setting algorithmClassName = %s", algorithmClassName));
                    }
                    node.setTextContent(algorithmClassName);
                    updated = true;
                } else if (parameter.getNodeValue().equals("failover")) {
                    String value = String.valueOf(configuration.isFailOver());
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Setting failover = %s", value));
                    }
                    node.setTextContent(value);
                    updated = true;
                } else if (parameter.getNodeValue().equals("sessionAffinity")) {
                    String value = String.valueOf(configuration.isSessionAffinity());
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Setting sessionAffinity = %s", value));
                    }
                    node.setTextContent(value);
                    updated = true;
                } else if (parameter.getNodeValue().equals("sessionTimeout")) {
                    String value = String.valueOf(configuration.getSessionTimeout());
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Setting sessionTimeout = %s", value));
                    }
                    node.setTextContent(value);
                    updated = true;
                }
            }
            if (updated) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Updating synapse main sequence: %s", outputFilePath));
                }
                write(xmlDocument, outputFilePath);
                if(log.isInfoEnabled()) {
                    log.info("Synapse main sequence configured successfully");
                }
            }
            else {
                throw new RuntimeException(String.format("Send mediator endpoint configuration not found: %s", inputFilePath));
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not configure synapse settings", e);
        }
    }

    /**
     * Write xml document to file.
     * @param document
     * @param outputFilePath
     * @throws IOException
     */
    private static void write(Document document, String outputFilePath) throws IOException {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            DocumentType documentType = document.getDoctype();
            if (documentType != null) {
                String publicId = documentType.getPublicId();
                if (publicId != null) {
                    transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, publicId);
                }
                transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, documentType.getSystemId());
            }
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            Source source = new DOMSource(document);
            FileOutputStream outputStream = new FileOutputStream(outputFilePath);
            Result result = new StreamResult(outputStream);
            transformer.transform(source, result);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not write xml file: s%", outputFilePath), e);
        }
    }
}

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

package org.apache.stratos.cloud.controller.config.parser;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.config.CloudControllerConfig;
import org.apache.stratos.cloud.controller.util.AxiomXpathParserUtil;
import org.apache.stratos.cloud.controller.exception.MalformedConfigurationFileException;
import org.apache.stratos.cloud.controller.domain.DataPublisherConfig;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.TopologyConfig;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;

/**
 * Parse the cloud-controller.xml
 *
 *
 */
public class CloudControllerConfigParser {
    private static final Log log = LogFactory.getLog(CloudControllerConfigParser.class);
    private static final String FILE_NAME = "cloud-controller.xml";

    /**
     * Parse the cloud-controller.xml file.
     * @param elt document element.
     * @throws MalformedConfigurationFileException
     */
    public static void parse(OMElement elt) throws MalformedConfigurationFileException {

        extractIaasProviders(elt, AxiomXpathParserUtil.getMatchingNodes(elt, CloudControllerConstants.IAAS_PROVIDER_XPATH));
        extractDataPublisherConfig(elt, AxiomXpathParserUtil.getElement(FILE_NAME, elt, CloudControllerConstants.DATA_PUBLISHER_ELEMENT,
                                        CloudControllerConstants.DATA_PUBLISHER_XPATH));
        extractTopologySyncConfig(elt, AxiomXpathParserUtil.getElement(FILE_NAME, elt, CloudControllerConstants.TOPOLOGY_SYNC_ELEMENT,
                        CloudControllerConstants.TOPOLOGY_SYNC_XPATH));
    }




    private static void extractIaasProviders(OMElement elt, List<OMNode> nodeList) {
        List<IaasProvider> iaasProviders = CloudControllerConfig.getInstance().getIaasProviders();

        if (iaasProviders == null) {
            CloudControllerConfig.getInstance()
                    .setIaasProviders((iaasProviders = new ArrayList<IaasProvider>()));
        }

        // this is a valid scenario. User can have 0..1 iaas provider elements
        // in cloud-controller xml.
        if (nodeList == null || nodeList.isEmpty()) {
            log.debug("No IaasProvider element found in "+FILE_NAME);
            return;
        }
        
        for (OMNode node : nodeList) {
            iaasProviders.add(IaasProviderConfigParser.getIaasProvider(FILE_NAME, elt, node, null));
        }
    }
    
    private static void extractDataPublisherConfig(OMElement rootElt, OMElement element) {
        if (element == null) {
            log.debug("No data publisher config found in "+FILE_NAME);
            return;
        }

        CloudControllerConfig config = CloudControllerConfig.getInstance();
        // get enable attribute
        boolean isEnable =
                Boolean.parseBoolean(element.getAttributeValue(new QName(
                        CloudControllerConstants.ENABLE_ATTR)));
        config.setEnableBAMDataPublisher(isEnable);

        if (isEnable) {
            // get bam server info
            OMElement childElement =
                    AxiomXpathParserUtil.getFirstChildElement(element,
                            CloudControllerConstants.BAM_SERVER_ELEMENT);
            OMElement elt;

            DataPublisherConfig dataPublisherConfig = new DataPublisherConfig();
            config.setDataPubConfig(dataPublisherConfig);
            
            if (childElement != null) {
                // set bam user name
                elt =
                        AxiomXpathParserUtil.getFirstChildElement(childElement,
                                CloudControllerConstants.BAM_SERVER_ADMIN_USERNAME_ELEMENT);
                if (elt != null) {
                    dataPublisherConfig.setBamUsername(elt.getText());
                }
                // set bam password
                elt =
                        AxiomXpathParserUtil.getFirstChildElement(childElement,
                                CloudControllerConstants.BAM_SERVER_ADMIN_PASSWORD_ELEMENT);
                if (elt != null) {
                    String password = AxiomXpathParserUtil.resolveSecret(rootElt, elt);
                    if (password == null) {
                        AxiomXpathParserUtil.plainTextWarn(CloudControllerConstants.BAM_SERVER_ADMIN_PASSWORD_ELEMENT);
                        password = elt.getText();
                    }

                    if (password != null) {
                        dataPublisherConfig.setBamPassword(password);
                    }
                }
            }

            // set cron
            childElement = AxiomXpathParserUtil.getFirstChildElement(element, CloudControllerConstants.CRON_ELEMENT);
            if (childElement != null) {
                dataPublisherConfig.setDataPublisherCron(childElement.getText());
            }

            // set cassandra info
            childElement = AxiomXpathParserUtil.getFirstChildElement(element, CloudControllerConstants.CASSANDRA_INFO_ELEMENT);

            if (childElement != null) {
                // set connection url
                elt = AxiomXpathParserUtil.getFirstChildElement(childElement, CloudControllerConstants.CONNECTION_URL_ELEMENT);
                if (elt != null) {
                    dataPublisherConfig.setCassandraConnUrl(elt.getText());
                }

                // set user name
                elt = AxiomXpathParserUtil.getFirstChildElement(childElement, CloudControllerConstants.USER_NAME_ELEMENT);
                if (elt != null) {
                    dataPublisherConfig.setCassandraUser(elt.getText());
                }
                // set password
                elt = AxiomXpathParserUtil.getFirstChildElement(childElement, CloudControllerConstants.PASSWORD_ELEMENT);
                if (elt != null) {
                    String password = AxiomXpathParserUtil.resolveSecret(rootElt, elt);
                    if (password == null) {
                        AxiomXpathParserUtil.plainTextWarn(CloudControllerConstants.PASSWORD_ELEMENT);
                        password = elt.getText();
                    }

                    if (password != null) {
                        dataPublisherConfig.setCassandraPassword(password);
                    }
                }
            }

        }
    }

    private static void extractTopologySyncConfig(OMElement elt, OMElement element) {

        if (element == null) {
            log.debug("No Topology sync config is found "+FILE_NAME);
            return;
        }

        // get enable attribute
        boolean isEnable =
                Boolean.parseBoolean(element.getAttributeValue(new QName(
                        CloudControllerConstants.ENABLE_ATTR)));

        CloudControllerConfig config = CloudControllerConfig.getInstance();

        config.setEnableTopologySync(isEnable);
        if (!isEnable) {
            if (log.isWarnEnabled()) {
                log.warn("Topology synchronization is disabled!");
            }
        }

        if (isEnable) {
            TopologyConfig topologyConfig = new TopologyConfig();
            // load properties
            IaasProviderConfigParser.loadProperties(FILE_NAME, element, topologyConfig.getProperties());

            config.setTopologyConfig(topologyConfig);
        }
    }
    
}

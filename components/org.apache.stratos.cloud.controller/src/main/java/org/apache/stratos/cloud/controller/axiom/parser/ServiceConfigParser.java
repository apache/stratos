/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.cloud.controller.axiom.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.axiom.AxiomXpathParserUtil;
import org.apache.stratos.cloud.controller.exception.MalformedConfigurationFileException;
import org.apache.stratos.cloud.controller.pojo.AppType;
import org.apache.stratos.cloud.controller.pojo.Cartridge;
import org.apache.stratos.cloud.controller.pojo.IaasProvider;
import org.apache.stratos.cloud.controller.pojo.PortMapping;
import org.apache.stratos.cloud.controller.pojo.ServiceContext;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;

/**
 * Parse the cartridge definition files.
 * 
 * @author nirmal
 * 
 */
public class ServiceConfigParser {
    private static final Log log = LogFactory.getLog(ServiceConfigParser.class);

    /**
     * Parse the config file.
     * 
     * @param elt
     *            document element.
     * @throws MalformedConfigurationFileException
     */
    public static List<Cartridge>
        parse(String file, OMElement elt) throws MalformedConfigurationFileException {

        return extractCartridges(file, elt);
    }

    public static List<ServiceContext> extractServiceContexts(File file, OMElement elt) {

        String fileName = file.getAbsolutePath();
        List<ServiceContext> serviceContextList = new ArrayList<ServiceContext>();

        // services can be found from this XPATH
        String xpath = CloudControllerConstants.SERVICES_ELEMENT_XPATH;
        List<?> serviceNodes = AxiomXpathParserUtil.getMatchingNodes(xpath, elt);

        if (serviceNodes == null || serviceNodes.isEmpty()) {
            // or from this XPATH
            xpath = CloudControllerConstants.SERVICE_ELEMENT_XPATH;
            serviceNodes = AxiomXpathParserUtil.getMatchingNodes(xpath, elt);
        }

        if (serviceNodes == null || serviceNodes.isEmpty()) {
            log.warn("No service found in this configuration file : " + fileName);
            return serviceContextList;
        }

        for (Object obj : serviceNodes) {
            ServiceContext serviceCtxt = new ServiceContext();

            // set the definition file
            serviceCtxt.setFile(file);

            if (obj instanceof OMNode) {
                OMNode serviceNode = (OMNode) obj;

                if (serviceNode.getType() == OMNode.ELEMENT_NODE) {

                    OMElement node = (OMElement) serviceNode;

                    if (node.getAttribute(new QName(CloudControllerConstants.SERVICE_DOMAIN_ATTR)) == null) {
                        String msg =
                                     "Essential '" + CloudControllerConstants.SERVICE_DOMAIN_ATTR +
                                             "' " + "attribute of '" +
                                             CloudControllerConstants.SERVICE_ELEMENT +
                                             "' element cannot be found in " + fileName;

                        handleException(msg);
                    }

                    // set domain name
                    serviceCtxt.setClusterId(node.getAttribute(new QName(
                                                                         CloudControllerConstants.SERVICE_DOMAIN_ATTR))
                                                 .getAttributeValue());
                    // set tenant range
                    serviceCtxt.setTenantRange(node.getAttribute(new QName(
                                                                           CloudControllerConstants.SERVICE_TENANT_RANGE_ATTR))
                                                   .getAttributeValue());

                    serviceCtxt.setAutoScalerPolicyName(node.getAttribute(new QName(
                                                                                    CloudControllerConstants.POLICY_NAME))
                                                            .getAttributeValue());

                    OMNode cartridgeNode =
                                           AxiomXpathParserUtil.getFirstMatchingNode(xpath +
                                                                                             CloudControllerConstants.CARTRIDGE_ELEMENT_XPATH,
                                                                                     node);

                    if (cartridgeNode != null && cartridgeNode.getType() == OMNode.ELEMENT_NODE) {

                        OMElement cartridgeElt = (OMElement) cartridgeNode;

                        String type =
                                      cartridgeElt.getAttribute(new QName(
                                                                          CloudControllerConstants.TYPE_ATTR))
                                                  .getAttributeValue();

                        if ("".equals(type)) {
                            String msg =
                                         "Essential '" + CloudControllerConstants.TYPE_ATTR + "' " +
                                                 " attribute of '" +
                                                 CloudControllerConstants.CARTRIDGE_ELEMENT +
                                                 "' of '" +
                                                 CloudControllerConstants.SERVICE_ELEMENT +
                                                 "' element cannot be found in " + fileName;

                            handleException(msg);
                        }

                        // set Cartridge type
                        serviceCtxt.setCartridgeType(type);

                    }
                    if (serviceCtxt.getCartridgeType() == null) {
                        String msg =
                                     "Essential '" + CloudControllerConstants.CARTRIDGE_ELEMENT +
                                             "' element" + " has not specified in " + fileName;
                        handleException(msg);
                    }

                    // load payload
                    loadPayload(AxiomXpathParserUtil.getMatchingNodes(xpath +
                                                                              CloudControllerConstants.PAYLOAD_ELEMENT_XPATH,
                                                                      node), serviceCtxt);

                    // load host name
                    loadHostName(AxiomXpathParserUtil.getMatchingNodes(xpath +
                                                                               CloudControllerConstants.HOST_ELEMENT_XPATH,
                                                                       node), serviceCtxt);

                    // load properties
                    IaasProviderConfigParser.loadProperties(fileName, node,
                                                            serviceCtxt.getProperties());

                }
            }

            FasterLookUpDataHolder.getInstance().addServiceContext(serviceCtxt);
            // add each domain specific template to list
            serviceContextList.add(serviceCtxt);
        }

        return serviceContextList;

    }

    private static void loadHostName(final List<OMNode> nodes, final ServiceContext serviceCtxt) {

        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        // read host element
        if (nodes.get(0).getType() == OMNode.ELEMENT_NODE) {

            OMElement node = (OMElement) nodes.get(0);

            if (node.getText() != null) {
                serviceCtxt.setHostName(node.getText());
            }

        }
    }

    private static void loadPayload(final List<OMNode> nodes, final ServiceContext serviceCtxt) {

        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        // read payload element
        if (nodes.get(0).getType() == OMNode.ELEMENT_NODE) {

            OMElement node = (OMElement) nodes.get(0);

            if (node.getText() != null) {
                StringBuilder payload = new StringBuilder(node.getText());
                serviceCtxt.setPayload(payload);

            }

        }

    }

    private static List<Cartridge>
        extractCartridges(String file, OMElement elt) throws MalformedConfigurationFileException {

        FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();

        List<IaasProvider> iaasProviders = dataHolder.getIaasProviders();

        if (iaasProviders == null) {
            dataHolder.setIaasProviders((iaasProviders = new ArrayList<IaasProvider>()));
        }

        List<Cartridge> cartridges = new ArrayList<Cartridge>();

        String xpath = CloudControllerConstants.CARTRIDGES_ELEMENT_XPATH;

        // cartridges can be found from this XPATH
        List<?> cartridgeNodes = AxiomXpathParserUtil.getMatchingNodes(xpath, elt);

        if (cartridgeNodes == null || cartridgeNodes.isEmpty()) {
            // or from this XPATH
            xpath = CloudControllerConstants.CARTRIDGE_ELEMENT_XPATH;
            cartridgeNodes = AxiomXpathParserUtil.getMatchingNodes(xpath, elt);
        }

        if (cartridgeNodes == null || cartridgeNodes.isEmpty()) {
            log.warn("No cartridge found in this configuration file : " + file);
            return cartridges;
        }

        for (Object obj : cartridgeNodes) {

            if (obj instanceof OMNode) {
                OMNode cartridgeNode = (OMNode) obj;

                if (cartridgeNode.getType() == OMNode.ELEMENT_NODE) {

                    OMElement cartridgeElement = (OMElement) cartridgeNode;

                    // retrieve Attributes of a Cartridge definition
                    String type =
                                  cartridgeElement.getAttributeValue(new QName(
                                                                               CloudControllerConstants.TYPE_ATTR));
                    String host =
                                  cartridgeElement.getAttributeValue(new QName(
                                                                               CloudControllerConstants.HOST_ATTR));
                    String provider =
                                      cartridgeElement.getAttributeValue(new QName(
                                                                                   CloudControllerConstants.PROVIDER_ATTR));

                    String version =
                                     cartridgeElement.getAttributeValue(new QName(
                                                                                  CloudControllerConstants.VERSION_ATTR));

                    boolean multiTenant =
                                          Boolean.valueOf(cartridgeElement.getAttributeValue(new QName(
                                                                                                       CloudControllerConstants.MULTI_TENANT_ATTR)));

                    Cartridge aCartridge;

                    if ((aCartridge = dataHolder.getCartridge(type)) == null) {

                        aCartridge = new Cartridge(type, host, provider, version, multiTenant);
                    }

                    // read displayName
                    Iterator<?> itName =
                                         cartridgeElement.getChildrenWithName(new QName(
                                                                                        CloudControllerConstants.DISPLAY_NAME_ELEMENT));

                    if (itName.hasNext()) {
                        OMElement name = (OMElement) itName.next();

                        aCartridge.setDisplayName(name.getText());
                    }

                    // read description
                    Iterator<?> it =
                                     cartridgeElement.getChildrenWithName(new QName(
                                                                                    CloudControllerConstants.DESCRIPTION_ELEMENT));

                    if (it.hasNext()) {
                        OMElement desc = (OMElement) it.next();

                        aCartridge.setDescription(desc.getText());
                    }

                    // load properties of this cartridge
                    IaasProviderConfigParser.loadProperties(file, cartridgeElement,
                                                            aCartridge.getProperties());

                    // retrieve the list of IaaS providers
                    List<?> iaasProviderNodes =
                                                AxiomXpathParserUtil.getMatchingNodes(xpath +
                                                                                              CloudControllerConstants.IAAS_PROVIDER_ELEMENT_XPATH,
                                                                                      cartridgeElement);

                    getIaasProviders(file, elt, iaasProviders, cartridgeElement.toString(),
                                     aCartridge, iaasProviderNodes);

                    // load dirs
                    List<?> deploymentNodes =
                                              AxiomXpathParserUtil.getMatchingNodes(xpath +
                                                                                            CloudControllerConstants.DEPLOYMENT_ELEMENT_XPATH,
                                                                                    cartridgeElement);
                    setDeploymentDirs(file, cartridgeElement.toString(), aCartridge,
                                      deploymentNodes);

                    // load port mappings
                    List<?> portMappingNodes =
                                               AxiomXpathParserUtil.getMatchingNodes(xpath +
                                                                                             CloudControllerConstants.PORT_MAPPING_ELEMENT_XPATH,
                                                                                     cartridgeElement);
                    getPortMappings(file, cartridgeElement.toString(), aCartridge, portMappingNodes);

                    // load appTypes
                    List<?> appTypesNodes =
                                            AxiomXpathParserUtil.getMatchingNodes(xpath +
                                                                                          CloudControllerConstants.APP_TYPES_ELEMENT_XPATH,
                                                                                  cartridgeElement);
                    getAppTypes(file, cartridgeElement.toString(), aCartridge, appTypesNodes);

                    cartridges.add(aCartridge);

                    if (dataHolder.getCartridge(type) == null) {
                        dataHolder.addCartridge(aCartridge);
                    }
                }
            }
        }

        return cartridges;
    }

    /**
     * @param iaasProviders
     * @param cartridgeElementString
     * @param aCartridge
     * @param iaasProviderNodes
     */
    private static void getIaasProviders(final String fileName, final OMElement elt,
        List<IaasProvider> iaasProviders, String cartridgeElementString, Cartridge aCartridge,
        List<?> iaasProviderNodes) {
        for (Object nodeObj : iaasProviderNodes) {
            if (nodeObj instanceof OMNode) {
                OMNode iaasProviderNode = (OMNode) nodeObj;

                if (iaasProviderNode.getType() == OMNode.ELEMENT_NODE) {

                    OMElement iaasElt = (OMElement) iaasProviderNode;

                    // add the IaasProvider to this cartridge
                    aCartridge.addIaasProvider(IaasProviderConfigParser.getIaasProvider(fileName,
                                                                                        elt,
                                                                                        iaasElt,
                                                                                        iaasProviders));

                } else {
                    String msg =
                                 "Essential '" + CloudControllerConstants.IAAS_PROVIDER_ELEMENT +
                                         "' element cannot" + " be found in " +
                                         cartridgeElementString + " of " + fileName;
                    handleException(msg);
                }

            }
        }
    }

    /**
     * @param cartridgeElementString
     *            Cartridges section as a {@link String}
     * @param aCartridge
     *            {@link Cartridge} instance.
     * @param deploymentNodes
     *            list of deployment directory nodes
     */
    private static void setDeploymentDirs(String fileName, String cartridgeElementString,
        Cartridge aCartridge, List<?> deploymentNodes) {
        Object nodeObj;
        if ((nodeObj = deploymentNodes.get(0)) instanceof OMNode) {
            OMNode deploymentNode = (OMNode) nodeObj;

            if (deploymentNode.getType() == OMNode.ELEMENT_NODE) {

                OMElement deployElt = (OMElement) deploymentNode;

                if (deployElt.getAttributeValue(new QName(CloudControllerConstants.BASE_DIR_ATTR)) != null) {

                    aCartridge.setBaseDir(deployElt.getAttributeValue(new QName(
                                                                                CloudControllerConstants.BASE_DIR_ATTR)));
                }

                for (Iterator<?> iterator =
                                            deployElt.getChildrenWithName(new QName(
                                                                                    CloudControllerConstants.DIRECTORY_ELEMENT)); iterator.hasNext();) {
                    OMElement dir = (OMElement) iterator.next();
                    aCartridge.addDeploymentDir(dir.getText());
                }

            } else {
                String msg =
                             "Essential '" + CloudControllerConstants.DEPLOYMENT_ELEMENT +
                                     "' element cannot" + " be found in " + cartridgeElementString +
                                     " of " + fileName;
                handleException(msg);
            }

        }
    }

    /**
     * @param cartridgeElementString
     *            Cartridges section as a {@link String}
     * @param aCartridge
     *            {@link Cartridge} instance.
     * @param portMappingNodes
     *            nodes of port mapping elements
     */
    private static void getPortMappings(final String fileName, String cartridgeElementString,
        Cartridge aCartridge, List<?> portMappingNodes) {
        Object nodeObj;
        if (!portMappingNodes.isEmpty()) {
            if ((nodeObj = portMappingNodes.get(0)) instanceof OMNode) {
                OMNode portMappingNode = (OMNode) nodeObj;

                if (portMappingNode.getType() == OMNode.ELEMENT_NODE) {

                    OMElement portMappingElt = (OMElement) portMappingNode;

                    for (Iterator<?> iterator =
                                                portMappingElt.getChildrenWithName(new QName(
                                                                                             CloudControllerConstants.HTTP_ELEMENT)); iterator.hasNext();) {
                        OMElement httpElt = (OMElement) iterator.next();

                        String port =
                                      httpElt.getAttributeValue(new QName(
                                                                          CloudControllerConstants.PORT_ATTR));
                        String proxyPort =
                                           httpElt.getAttributeValue(new QName(
                                                                               CloudControllerConstants.PROXY_PORT_ATTR));

                        PortMapping mapping =
                                              new PortMapping(
                                                              CloudControllerConstants.HTTP_ELEMENT,
                                                              port, proxyPort);

                        aCartridge.addPortMapping(mapping);
                    }

                    for (Iterator<?> iterator =
                                                portMappingElt.getChildrenWithName(new QName(
                                                                                             CloudControllerConstants.HTTPS_ELEMENT)); iterator.hasNext();) {
                        OMElement httpsElt = (OMElement) iterator.next();

                        String port =
                                      httpsElt.getAttributeValue(new QName(
                                                                           CloudControllerConstants.PORT_ATTR));
                        String proxyPort =
                                           httpsElt.getAttributeValue(new QName(
                                                                                CloudControllerConstants.PROXY_PORT_ATTR));

                        PortMapping mapping =
                                              new PortMapping(
                                                              CloudControllerConstants.HTTPS_ELEMENT,
                                                              port, proxyPort);

                        aCartridge.addPortMapping(mapping);
                    }

                } else {
                    String msg =
                                 "Essential '" + CloudControllerConstants.PORT_MAPPING_ELEMENT +
                                         "' element cannot" + " be found in " +
                                         cartridgeElementString + " of " + fileName;
                    handleException(msg);
                }

            }
        }
    }

    /**
     * @param cartridgeElementString
     *            Cartridges section as a {@link String}
     * @param aCartridge
     *            {@link org.apache.stratos.cloud.controller.pojo.Cartridge} instance.
     * @param appTypesNodes
     *            nodes of App types.
     */
    private static void getAppTypes(final String fileName, String cartridgeElementString,
        Cartridge aCartridge, List<?> appTypesNodes) {
        Object nodeObj;
        if (!appTypesNodes.isEmpty()) {
            if ((nodeObj = appTypesNodes.get(0)) instanceof OMNode) {
                OMNode appTypeNode = (OMNode) nodeObj;

                if (appTypeNode.getType() == OMNode.ELEMENT_NODE) {

                    OMElement appTypesElt = (OMElement) appTypeNode;

                    for (Iterator<?> iterator =
                                                appTypesElt.getChildrenWithName(new QName(
                                                                                          CloudControllerConstants.APP_TYPE_ELEMENT)); iterator.hasNext();) {
                        OMElement appElt = (OMElement) iterator.next();

                        String name =
                                      appElt.getAttributeValue(new QName(
                                                                         CloudControllerConstants.NAME_ATTR));
                        String appSpecificMapping =
                                                    appElt.getAttributeValue(new QName(
                                                                                       CloudControllerConstants.APP_SPECIFIC_MAPPING_ATTR));

                        AppType appType;

                        if (appSpecificMapping == null) {
                            appType = new AppType(name);
                        } else {
                            appType = new AppType(name, Boolean.valueOf(appSpecificMapping));
                        }

                        aCartridge.addAppType(appType);
                    }

                } else {
                    String msg =
                                 "Essential '" + CloudControllerConstants.APP_TYPE_ELEMENT +
                                         "' element cannot" + " be found in " +
                                         cartridgeElementString + " of " + fileName;
                    handleException(msg);
                }

            }
        }
    }

    private static void
        handleException(final String msg) throws MalformedConfigurationFileException {
        log.error(msg);
        throw new MalformedConfigurationFileException(msg);
    }

}

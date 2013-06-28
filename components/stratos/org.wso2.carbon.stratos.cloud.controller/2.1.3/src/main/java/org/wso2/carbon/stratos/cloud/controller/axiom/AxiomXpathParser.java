/*
 * Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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
package org.wso2.carbon.stratos.cloud.controller.axiom;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.om.impl.dom.ElementImpl;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;
import org.w3c.dom.Element;
import org.wso2.carbon.stratos.cloud.controller.exception.CloudControllerException;
import org.wso2.carbon.stratos.cloud.controller.exception.MalformedConfigurationFileException;
import org.wso2.carbon.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.wso2.carbon.stratos.cloud.controller.util.AppType;
import org.wso2.carbon.stratos.cloud.controller.util.Cartridge;
import org.wso2.carbon.stratos.cloud.controller.util.CloudControllerConstants;
import org.wso2.carbon.stratos.cloud.controller.util.CloudControllerUtil;
import org.wso2.carbon.stratos.cloud.controller.util.IaasProvider;
import org.wso2.carbon.stratos.cloud.controller.util.PortMapping;
import org.wso2.carbon.stratos.cloud.controller.util.ServiceContext;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

/**
 * This class is parsing configuration files using Axiom Xpath.
 */
public class AxiomXpathParser {

	private static final Log log = LogFactory.getLog(AxiomXpathParser.class);
	private OMElement documentElement;
	private final File xmlSource;

	public AxiomXpathParser(final File xmlFile) {
		xmlSource = xmlFile;
	}

	/**
     * @param cartridgeElement Cartridges section as a {@link String}
     * @param aCartridge {@link Cartridge} instance.
     * @param appTypesNodes nodes of App types.
     */
    private void getAppTypes(String cartridgeElementString, Cartridge aCartridge,
                             List<?> appTypesNodes) {
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
	    					appType =
	    					          new AppType(name,
	    					                      Boolean.valueOf(appSpecificMapping));
	    				}

	    				aCartridge.addAppType(appType);
	    			}

	    		} else {
	    			String msg =
	    			             "Essential '" + CloudControllerConstants.APP_TYPE_ELEMENT +
	    			                     "' element cannot" + " be found in " +
	    			                     cartridgeElementString + " of " +
	    			                     xmlSource;
	    			handleException(msg);
	    		}

	    	}
	    }
    }

	/**
	 * @return a List of {@link Cartridge}s.
	 */
	public List<Cartridge> getCartridgesList() {

		FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();

		List<IaasProvider> iaasProviders = dataHolder.getIaasProviders();

		if (iaasProviders == null) {
			dataHolder.setIaasProviders((iaasProviders = new ArrayList<IaasProvider>()));
		}

		List<Cartridge> cartridges = new ArrayList<Cartridge>();

		String xpath = CloudControllerConstants.CARTRIDGES_ELEMENT_XPATH;
		
		// cartridges can be found from this XPATH
		List<?> cartridgeNodes = getMatchingNodes(xpath, documentElement);

		if (cartridgeNodes == null || cartridgeNodes.isEmpty()) {
			// or from this XPATH
			xpath = CloudControllerConstants.CARTRIDGE_ELEMENT_XPATH;
			cartridgeNodes = getMatchingNodes(xpath, documentElement);
		}

		if (cartridgeNodes == null || cartridgeNodes.isEmpty()) {
			log.warn("No cartridge found in this configuration file : " + xmlSource.getPath());
			return cartridges;
		}

		for (Object obj : cartridgeNodes) {

			if (obj instanceof OMNode) {
				OMNode cartridgeNode = (OMNode) obj;

				if (cartridgeNode.getType() == OMNode.ELEMENT_NODE) {

					OMElement cartridgeElement = (OMElement) cartridgeNode;

					// retrieve Attributes of a Cartridge definition
					String type = cartridgeElement.getAttributeValue(new QName(
					                                                           CloudControllerConstants.TYPE_ATTR));
					String host = cartridgeElement.getAttributeValue(new QName(
					                                                           CloudControllerConstants.HOST_ATTR));
					String provider = cartridgeElement.getAttributeValue(new QName(
					                                                               CloudControllerConstants.PROVIDER_ATTR));
					
					String version =
			                  cartridgeElement.getAttributeValue(new QName(
			                                                               CloudControllerConstants.VERSION_ATTR));

					boolean multiTenant = Boolean.valueOf(cartridgeElement.getAttributeValue(new QName(
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
					loadProperties(cartridgeElement, aCartridge.getProperties());

					// retrieve the list of IaaS providers
					List<?> iaasProviderNodes = getMatchingNodes(xpath + CloudControllerConstants.IAAS_PROVIDER_ELEMENT_XPATH,
					                                             cartridgeElement);

					getIaasProviders(iaasProviders, cartridgeElement.toString(), aCartridge, iaasProviderNodes);

					// load dirs
					List<?> deploymentNodes = getMatchingNodes(xpath + CloudControllerConstants.DEPLOYMENT_ELEMENT_XPATH,
					                                           cartridgeElement);
                    setDeploymentDirs(cartridgeElement.toString(), aCartridge, deploymentNodes);

					// load port mappings
					List<?> portMappingNodes =
					                           getMatchingNodes(xpath +
					                                                    CloudControllerConstants.PORT_MAPPING_ELEMENT_XPATH,
					                                            cartridgeElement);
					getPortMappings(cartridgeElement.toString(), aCartridge, portMappingNodes);

					// load appTypes
					List<?> appTypesNodes =
					                        getMatchingNodes(xpath +
					                                                 CloudControllerConstants.APP_TYPES_ELEMENT_XPATH,
					                                         cartridgeElement);
					getAppTypes(cartridgeElement.toString(), aCartridge, appTypesNodes);

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
    private void getIaasProviders(List<IaasProvider> iaasProviders,
                                  String cartridgeElementString, Cartridge aCartridge,
                                  List<?> iaasProviderNodes) {
	    for (Object nodeObj : iaasProviderNodes) {
	    	if (nodeObj instanceof OMNode) {
	    		OMNode iaasProviderNode = (OMNode) nodeObj;

	    		if (iaasProviderNode.getType() == OMNode.ELEMENT_NODE) {

	    			OMElement iaasElt = (OMElement) iaasProviderNode;

	    			// add the IaasProvider to this cartridge
	    			aCartridge.addIaasProvider(getIaasProvider(iaasElt, iaasProviders));

	    		} else {
	    			String msg =
	    			             "Essential '" +
	    			                     CloudControllerConstants.IAAS_PROVIDER_ELEMENT +
	    			                     "' element cannot" + " be found in " +
	    			                     cartridgeElementString + " of " +
	    			                     xmlSource;
	    			handleException(msg);
	    		}

	    	}
	    }
    }

	private Element getDOMElement(final OMElement omElement) {

		// Get the StAX reader from the created element
		XMLStreamReader llomReader = omElement.getXMLStreamReader();

		// Create the DOOM OMFactory
		OMFactory doomFactory = DOOMAbstractFactory.getOMFactory();

		// Create the new builder
		StAXOMBuilder doomBuilder = new StAXOMBuilder(doomFactory, llomReader);

		// Get the document element
		OMElement newElem = doomBuilder.getDocumentElement();

		return newElem instanceof Element ? (Element) newElem : null;
	}

	private OMElement getElement(final Object obj) {
		OMNode node;
		if ((obj instanceof OMNode) && (node = (OMNode) obj).getType() == OMNode.ELEMENT_NODE) {

			OMElement element = (OMElement) node;

			return element;

		}

		return null;
	}

	private OMElement getElement(final OMElement rootElt, final String eltStr, final String xpath) {
		List<?> nodes = getMatchingNodes(xpath, rootElt);
		neglectingWarn(eltStr, nodes.size());
		OMElement element = getElement(nodes.get(0));
		return element;
	}

	private OMElement getFirstChildElement(final OMElement root, final String childName) {
		Iterator<?> it = root.getChildrenWithName(new QName(childName));
		if (it.hasNext()) {
			return (OMElement) it.next();
		}

		return null;
	}

	/**
	 * 
	 * @param xpath
	 *            XPATH expression to be read.
	 * @param elt
	 *            OMElement to be used for the search.
	 * @return List matching OMNode list
	 */
	@SuppressWarnings("unchecked")
	public OMNode getFirstMatchingNode(final String xpath, final OMElement elt) {

		AXIOMXPath axiomXpath;
		List<OMNode> nodeList = null;
		try {
			axiomXpath = new AXIOMXPath(xpath);
			nodeList = axiomXpath.selectNodes(elt);
		} catch (JaxenException e) {
			String msg = "Error occurred while reading the Xpath (" + xpath + ")";
			log.error(msg, e);
			throw new CloudControllerException(msg, e);
		}

		return nodeList != null ? nodeList.get(0) : null;
	}

	private IaasProvider getIaasProvider(final OMNode item, List<IaasProvider> iaases) {

		IaasProvider iaas = null;

		if (item.getType() == OMNode.ELEMENT_NODE) {

			OMElement iaasElt = (OMElement) item;

			if (iaases != null) {
				// check whether this is a reference to a predefined IaaS.
				for (IaasProvider iaasProvider : iaases) {
					if (iaasProvider.getType()
					                .equals(iaasElt.getAttribute(new QName(
					                                                       CloudControllerConstants.TYPE_ATTR))
					                               .getAttributeValue())) {
						iaas = new IaasProvider(iaasProvider);
						break;
					}
				}
			}

			if (iaas == null) {
				iaas = new IaasProvider();
			}

			if (iaas.getType() == null) {
				iaas.setType(iaasElt.getAttribute(new QName(CloudControllerConstants.TYPE_ATTR))
				                    .getAttributeValue());
			}

			if ("".equals(iaas.getType())) {
				String msg =
				             "'" + CloudControllerConstants.IAAS_PROVIDER_ELEMENT + "' element's '" +
				                     CloudControllerConstants.TYPE_ATTR +
				                     "' attribute should be specified!";

				handleException(msg);

			}

			// this is not mandatory
			String name =
			              (iaasElt.getAttribute(new QName(CloudControllerConstants.NAME_ATTR)) == null)
			                                                                                     ? iaas.getName()
			                                                                                     : iaasElt.getAttributeValue(new QName(
			                                                                                                                           CloudControllerConstants.NAME_ATTR));

			iaas.setName(name);

			String xpath = CloudControllerConstants.IAAS_PROVIDER_ELEMENT_XPATH;

			// load other elements
			loadClassName(iaas, iaasElt);
			loadMaxInstanceLimit(iaas, iaasElt);
			loadProperties(iaasElt, iaas.getProperties());
			loadTemplate(iaas, iaasElt);
			loadScalingOrders(iaas, iaasElt);
			loadProvider(iaas, iaasElt);
			loadIdentity(iaas, iaasElt);
			loadCredentials(iaas, iaasElt, xpath);
		}

		return iaas;
	}

	/**
	 * 
	 * @param xpath
	 *            XPATH expression to be read.
	 * @return List matching OMNode list
	 */
	@SuppressWarnings("unchecked")
	public List<OMNode> getMatchingNodes(final String xpath) {

		AXIOMXPath axiomXpath;
		List<OMNode> nodeList = null;
		try {
			axiomXpath = new AXIOMXPath(xpath);
			nodeList = axiomXpath.selectNodes(documentElement);
		} catch (JaxenException e) {
			String msg = "Error occurred while reading the Xpath (" + xpath + ")";
			log.error(msg, e);
			throw new CloudControllerException(msg, e);
		}

		return nodeList;
	}

	/**
	 * 
	 * @param xpath
	 *            XPATH expression to be read.
	 * @param elt
	 *            OMElement to be used for the search.
	 * @return List matching OMNode list
	 */
	@SuppressWarnings("unchecked")
	public List<OMNode> getMatchingNodes(final String xpath, final OMElement elt) {

		AXIOMXPath axiomXpath;
		List<OMNode> nodeList = null;
		try {
			axiomXpath = new AXIOMXPath(xpath);
			nodeList = axiomXpath.selectNodes(elt);
		} catch (JaxenException e) {
			String msg = "Error occurred while reading the Xpath (" + xpath + ")";
			log.error(msg, e);
			throw new CloudControllerException(msg, e);
		}

		return nodeList;
	}

	/**
     * @param cartridgeElement Cartridges section as a {@link String}
     * @param aCartridge {@link Cartridge} instance.
     * @param portMappingNodes nodes of port mapping elements
     */
    private void getPortMappings(String cartridgeElementString, Cartridge aCartridge,
                                 List<?> portMappingNodes) {
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
	    			             "Essential '" +
	    			                     CloudControllerConstants.PORT_MAPPING_ELEMENT +
	    			                     "' element cannot" + " be found in " +
	    			                     cartridgeElementString + " of " +
	    			                     xmlSource;
	    			handleException(msg);
	    		}

	    	}
	    }
    }

	public List<ServiceContext> getServiceContexts() {

		List<ServiceContext> serviceContextList = new ArrayList<ServiceContext>();

		// services can be found from this XPATH
		String xpath = CloudControllerConstants.SERVICES_ELEMENT_XPATH;
		List<?> serviceNodes = getMatchingNodes(xpath, documentElement);

		if (serviceNodes == null || serviceNodes.isEmpty()) {
			// or from this XPATH
			xpath = CloudControllerConstants.SERVICE_ELEMENT_XPATH;
			serviceNodes = getMatchingNodes(xpath, documentElement);
		}

		if (serviceNodes == null || serviceNodes.isEmpty()) {
			log.warn("No service found in this configuration file : " + xmlSource.getPath());
			return serviceContextList;
		}

		for (Object obj : serviceNodes) {
			ServiceContext serviceCtxt = new ServiceContext();

			// set the definition file
			serviceCtxt.setFile(xmlSource);
			
			if (obj instanceof OMNode) {
				OMNode serviceNode = (OMNode) obj;

				if (serviceNode.getType() == OMNode.ELEMENT_NODE) {

					OMElement node = (OMElement) serviceNode;

					if (node.getAttribute(new QName(CloudControllerConstants.SERVICE_DOMAIN_ATTR)) == null) {
						String msg =
						             "Essential '" + CloudControllerConstants.SERVICE_DOMAIN_ATTR + "' " +
						                     "attribute of '" + CloudControllerConstants.SERVICE_ELEMENT +
						                     "' element cannot be found in " + xmlSource;

						handleException(msg);
					}

					// set domain name
					serviceCtxt.setDomainName(node.getAttribute(new QName(
					                                                      CloudControllerConstants.SERVICE_DOMAIN_ATTR))
					                              .getAttributeValue());

					// set sub domain
					serviceCtxt.setSubDomainName(node.getAttribute(new QName(
					                                                         CloudControllerConstants.SERVICE_SUB_DOMAIN_ATTR))
					                                 .getAttributeValue());

					// set tenant range
					serviceCtxt.setTenantRange(node.getAttribute(new QName(
					                                                       CloudControllerConstants.SERVICE_TENANT_RANGE_ATTR))
					                               .getAttributeValue());

					OMNode cartridgeNode =
					                       getFirstMatchingNode(xpath +
					                                                    CloudControllerConstants.CARTRIDGE_ELEMENT_XPATH,
					                                            node);

					if (cartridgeNode != null && cartridgeNode.getType() == OMNode.ELEMENT_NODE) {

						OMElement cartridgeElt = (OMElement) cartridgeNode;

						String type =
								cartridgeElt.getAttribute(new QName(CloudControllerConstants.TYPE_ATTR))
						                  .getAttributeValue();

						if ("".equals(type)) {
							String msg =
							             "Essential '" + CloudControllerConstants.TYPE_ATTR + "' " +
							                     " attribute of '" +
							                     CloudControllerConstants.CARTRIDGE_ELEMENT + "' of '" +
							                     CloudControllerConstants.SERVICE_ELEMENT +
							                     "' element cannot be found in " + xmlSource;

							handleException(msg);
						}
						
						// set Cartridge type
						serviceCtxt.setCartridgeType(type);

					}
					if (serviceCtxt.getCartridgeType() == null) {
						String msg =
						             "Essential '" + CloudControllerConstants.CARTRIDGE_ELEMENT +
						                     "' element" + " has not specified in " + xmlSource;
						handleException(msg);
					}

					// load payload
					loadPayload(getMatchingNodes(xpath + CloudControllerConstants.PAYLOAD_ELEMENT_XPATH,
					                             node), serviceCtxt);

					// load host name
					loadHostName(getMatchingNodes(xpath + CloudControllerConstants.HOST_ELEMENT_XPATH,
					                              node), serviceCtxt);

					// load properties
					loadProperties(node, serviceCtxt.getProperties());

				}
			}

			FasterLookUpDataHolder.getInstance().addServiceContext(serviceCtxt);
			// add each domain specific template to list
			serviceContextList.add(serviceCtxt);
		}

		return serviceContextList;

	}

	public File getXmlSource() {
		return xmlSource;
	}

	private void handleException(final String msg) {
		log.error(msg);
		throw new MalformedConfigurationFileException(msg);
	}

	private void handleException(final String msg, final Exception e) {
		log.error(msg, e);
		throw new MalformedConfigurationFileException(msg, e);
	}

	private void loadClassName(final IaasProvider iaas, final OMElement iaasElt) {

		Iterator<?> it =
		                 iaasElt.getChildrenWithName(new QName(
		                                                       CloudControllerConstants.CLASS_NAME_ELEMENT));

		if (it.hasNext()) {
			OMElement classNameElt = (OMElement) it.next();
			iaas.setClassName(classNameElt.getText());
		}

		if (it.hasNext()) {
			log.warn(xmlSource + " contains more than one " +
			         CloudControllerConstants.CLASS_NAME_ELEMENT + " elements!" +
			         " Elements other than the first will be neglected.");
		}

		if (iaas.getClassName() == null) {
			String msg =
			             "Essential '" + CloudControllerConstants.CLASS_NAME_ELEMENT + "' element " +
			                     "has not specified in " + xmlSource;
			handleException(msg);
		}

	}

	private void loadCredentials(final IaasProvider iaas, final OMElement iaasElt,
	                             final String xpath) {

		Iterator<?> it =
		                 iaasElt.getChildrenWithName(new QName(
		                                                       CloudControllerConstants.CREDENTIAL_ELEMENT));

		if (it.hasNext()) {
			OMElement credentialElt = (OMElement) it.next();

			// retrieve the value using secure vault
			SecretResolver secretResolver = SecretResolverFactory.create(documentElement, false);
			String alias =
			               credentialElt.getAttributeValue(new QName(
			                                                         CloudControllerConstants.ALIAS_ATTRIBUTE));

			// retrieve the secured password
			if (secretResolver != null && secretResolver.isInitialized() &&
			    secretResolver.isTokenProtected(alias)) {

				iaas.setCredential(secretResolver.resolve(alias));

			}

			// if we still cannot find a value, we try to assign the value which
			// is specified
			// in the element, if any
			if (iaas.getCredential() == null) {
				log.warn("Unable to find a value for " + CloudControllerConstants.CREDENTIAL_ELEMENT +
				         " element from Secure Vault." +
				         "Hence we will try to assign the plain text value (if specified).");
				iaas.setCredential(credentialElt.getText());
			}
		}

		if (it.hasNext()) {
			log.warn(xmlSource + " contains more than one " +
			         CloudControllerConstants.CREDENTIAL_ELEMENT + " elements!" +
			         " Elements other than the first will be neglected.");
		}

		if (iaas.getCredential() == null) {
			String msg =
			             "Essential '" + CloudControllerConstants.CREDENTIAL_ELEMENT + "' element" +
			                     " has not specified in " + xmlSource;
			handleException(msg);
		}

	}
	
	private void loadHostName(final List<OMNode> nodes, final ServiceContext serviceCtxt) {

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

	private void loadIdentity(final IaasProvider iaas, final OMElement iaasElt) {

		Iterator<?> it =
		                 iaasElt.getChildrenWithName(new QName(CloudControllerConstants.IDENTITY_ELEMENT));

		if (it.hasNext()) {
			OMElement identityElt = (OMElement) it.next();

			// retrieve the value using secure vault
			SecretResolver secretResolver = SecretResolverFactory.create(documentElement, false);
			String alias =
			               identityElt.getAttributeValue(new QName(
			                                                       CloudControllerConstants.ALIAS_ATTRIBUTE));

			// retrieve the secured password
			if (secretResolver != null && secretResolver.isInitialized() &&
			    secretResolver.isTokenProtected(alias)) {

				iaas.setIdentity(secretResolver.resolve(alias));

			}

			// if we still cannot find a value, we try to assign the value which
			// is specified
			// in the element, if any
			if (iaas.getIdentity() == null) {
				log.warn("Unable to find a value for " + CloudControllerConstants.IDENTITY_ELEMENT +
				         " element from Secure Vault." +
				         "Hence we will try to assign the plain text value (if specified).");
				iaas.setIdentity(identityElt.getText());
			}
		}

		if (it.hasNext()) {
			log.warn(xmlSource + " contains more than one " + CloudControllerConstants.IDENTITY_ELEMENT +
			         " elements!" + " Elements other than the first will be neglected.");
		}

		if (iaas.getIdentity() == null) {
			String msg =
			             "Essential '" + CloudControllerConstants.IDENTITY_ELEMENT + "' element" +
			                     " has not specified in " + xmlSource;
			handleException(msg);
		}

	}

	private void loadMaxInstanceLimit(IaasProvider iaas, final OMElement iaasElt) {

		Iterator<?> it =
		                 iaasElt.getChildrenWithName(new QName(
		                                                       CloudControllerConstants.MAX_INSTANCE_LIMIT_ELEMENT));

		if (it.hasNext()) {
			OMElement maxInstanceLimitElt = (OMElement) it.next();

			try {
				iaas.setMaxInstanceLimit(Integer.parseInt(maxInstanceLimitElt.getText()));
			} catch (NumberFormatException e) {
				String msg =
				             CloudControllerConstants.MAX_INSTANCE_LIMIT_ELEMENT +
				                     " element contained" + " in " + xmlSource + "" +
				                     " has a value which is not an Integer value.";
				handleException(msg, e);
			}

		}

		if (it.hasNext()) {
			log.warn(xmlSource + " contains more than one " +
			         CloudControllerConstants.MAX_INSTANCE_LIMIT_ELEMENT + " elements!" +
			         " Elements other than the first will be neglected.");
		}

	}

	private void loadPayload(final List<OMNode> nodes, final ServiceContext serviceCtxt) {

		if (nodes == null || nodes.isEmpty()) {
			return;
		}

		// read payload element
		if (nodes.get(0).getType() == OMNode.ELEMENT_NODE) {

			OMElement node = (OMElement) nodes.get(0);

			if (node.getText() != null) {
				byte[] payload = CloudControllerUtil.getBytesFromFile(node.getText());
				serviceCtxt.setPayload(payload);

			}

		}

	}

	private void loadProperties(final OMElement iaasElt, final Map<String, String> propertyMap) {

		Iterator<?> it =
		                 iaasElt.getChildrenWithName(new QName(CloudControllerConstants.PROPERTY_ELEMENT));

		while (it.hasNext()) {
			OMElement prop = (OMElement) it.next();

			if (prop.getAttribute(new QName(CloudControllerConstants.PROPERTY_NAME_ATTR)) == null ||
			    prop.getAttribute(new QName(CloudControllerConstants.PROPERTY_VALUE_ATTR)) == null) {

				String msg =
				             "Property element's, name and value attributes should be specified " +
				                     "in " + xmlSource;

				handleException(msg);
			}

			propertyMap.put(prop.getAttribute(new QName(CloudControllerConstants.PROPERTY_NAME_ATTR))
			                    .getAttributeValue(),
			                prop.getAttribute(new QName(CloudControllerConstants.PROPERTY_VALUE_ATTR))
			                    .getAttributeValue());
		}

	}

	private void loadProvider(final IaasProvider iaas, final OMElement iaasElt) {

		Iterator<?> it =
		                 iaasElt.getChildrenWithName(new QName(CloudControllerConstants.PROVIDER_ELEMENT));

		if (it.hasNext()) {
			OMElement providerElt = (OMElement) it.next();
			iaas.setProvider(providerElt.getText());
		}

		if (it.hasNext()) {
			log.warn(xmlSource + " contains more than one " + CloudControllerConstants.PROVIDER_ELEMENT +
			         " elements!" + " Elements other than the first will be neglected.");
		}

		if (iaas.getProvider() == null) {
			String msg =
			             "Essential '" + CloudControllerConstants.PROVIDER_ELEMENT + "' element " +
			                     "has not specified in " + xmlSource;
			handleException(msg);
		}

	}

	private void loadScalingOrders(final IaasProvider iaas, final OMElement iaasElt) {
		// set scale up order
		Iterator<?> it =
		                 iaasElt.getChildrenWithName(new QName(
		                                                       CloudControllerConstants.SCALE_UP_ORDER_ELEMENT));

		if (it.hasNext()) {
			OMElement scaleUpOrderElt = (OMElement) it.next();

			try {
				iaas.setScaleUpOrder(Integer.parseInt(scaleUpOrderElt.getText()));
			} catch (NumberFormatException e) {
				String msg =
				             CloudControllerConstants.SCALE_UP_ORDER_ELEMENT + " element contained" +
				                     " in " + xmlSource + "" +
				                     " has a value which is not an Integer value.";
				handleException(msg, e);
			}
		}

		if (it.hasNext()) {
			log.warn(xmlSource + " contains more than one " +
			         CloudControllerConstants.SCALE_UP_ORDER_ELEMENT + " elements!" +
			         " Elements other than the first will be neglected.");
		}

		if (iaas.getScaleUpOrder() == -1) {
			String msg =
			             "Essential '" + CloudControllerConstants.SCALE_UP_ORDER_ELEMENT + "' element" +
			                     " has not specified in " + xmlSource;
			handleException(msg);
		}

		// set scale down order
		it = iaasElt.getChildrenWithName(new QName(CloudControllerConstants.SCALE_DOWN_ORDER_ELEMENT));

		if (it.hasNext()) {
			OMElement scaleDownElt = (OMElement) it.next();

			try {
				iaas.setScaleDownOrder(Integer.parseInt(scaleDownElt.getText()));
			} catch (NumberFormatException e) {
				String msg =
				             CloudControllerConstants.SCALE_DOWN_ORDER_ELEMENT + " element contained" +
				                     " in " + xmlSource + "" +
				                     " has a value which is not an Integer value.";
				handleException(msg, e);
			}
		}

		if (it.hasNext()) {
			log.warn(xmlSource + " contains more than one " +
			         CloudControllerConstants.SCALE_DOWN_ORDER_ELEMENT + " elements!" +
			         " Elements other than the first will be neglected.");
		}

		if (iaas.getScaleDownOrder() == -1) {
			String msg =
			             "Essential '" + CloudControllerConstants.SCALE_DOWN_ORDER_ELEMENT + "' element" +
			                     " has not specified in " + xmlSource;
			handleException(msg);
		}

	}

	private void loadTemplate(final IaasProvider iaas, final OMElement iaasElt) {

		Iterator<?> it =
		                 iaasElt.getChildrenWithName(new QName(CloudControllerConstants.IMAGE_ID_ELEMENT));

		if (it.hasNext()) {
			OMElement imageElt = (OMElement) it.next();
			iaas.setImage(imageElt.getText());
		}

		if (it.hasNext()) {
			log.warn(xmlSource + " contains more than one " + CloudControllerConstants.IMAGE_ID_ELEMENT +
			         " elements!" + " Elements other than the first will be neglected.");
		}

	}

	private void neglectingWarn(final String elt, final int size) {
		if (size > 1) {
			log.warn(xmlSource + " contains more than one " + elt + " elements!" +
			         " Elements other than the first will be neglected.");
		}
	}

	public void parse() {

		if (xmlSource.exists()) {
			try {
				documentElement = new StAXOMBuilder(xmlSource.getPath()).getDocumentElement();

			} catch (Exception ex) {
				String msg = "Error occurred when parsing the " + xmlSource.getPath() + ".";
				handleException(msg, ex);
			}
		} else {
			String msg = "Configuration file cannot be found : " + xmlSource.getPath();
			handleException(msg);
		}
	}

	private void plainTextWarn(final String elt) {
		log.warn("Unable to find a value for " + elt + " element from Secure Vault." +
		         "Hence we will try to assign the plain text value (if specified).");
	}

	private String resolveSecret(final OMElement elt) {
		// retrieve the value using secure vault
		SecretResolver secretResolver = SecretResolverFactory.create(documentElement, false);

		String alias = elt.getAttributeValue(new QName(CloudControllerConstants.ALIAS_ATTRIBUTE));

		// retrieve the secured password
		if (secretResolver != null && secretResolver.isInitialized() &&
		    secretResolver.isTokenProtected(alias)) {

			return secretResolver.resolve(alias);

		}

		return null;
	}

	public void setDataPublisherRelatedData() {

		String eltStr = CloudControllerConstants.DATA_PUBLISHER_ELEMENT;
		// get dataPublisher element
		OMElement element =
		                    getElement(documentElement, eltStr,
		                               CloudControllerConstants.DATA_PUBLISHER_XPATH);

		if (element == null) {
			return;
		}

		FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();
		// get enable attribute
		boolean isEnable =
		                   Boolean.parseBoolean(element.getAttributeValue(new QName(
		                                                                            CloudControllerConstants.ENABLE_ATTR)));
		dataHolder.setEnableBAMDataPublisher(isEnable);

		if (isEnable) {
			// get bam server info
			OMElement childElement =
			                         getFirstChildElement(element,
			                                              CloudControllerConstants.BAM_SERVER_ELEMENT);
			OMElement elt;

			if (childElement != null) {
				// set bam user name
				elt =
				      getFirstChildElement(childElement,
				                           CloudControllerConstants.BAM_SERVER_ADMIN_USERNAME_ELEMENT);
				if (elt != null) {
					dataHolder.setBamUsername(elt.getText());
				}
				// set bam password
				elt =
				      getFirstChildElement(childElement,
				                           CloudControllerConstants.BAM_SERVER_ADMIN_PASSWORD_ELEMENT);
				if (elt != null) {
					String password = resolveSecret(elt);
					if (password == null) {
						plainTextWarn(CloudControllerConstants.BAM_SERVER_ADMIN_PASSWORD_ELEMENT);
						password = elt.getText();
					}

					if (password != null) {
						dataHolder.setBamPassword(password);
					}
				}
			}

			// set cron
			childElement = getFirstChildElement(element, CloudControllerConstants.CRON_ELEMENT);
			if (childElement != null) {
				dataHolder.setDataPublisherCron(childElement.getText());
			}

			// set cassandra info
			childElement = getFirstChildElement(element, CloudControllerConstants.CASSANDRA_INFO_ELEMENT);

			if (childElement != null) {
				// set connection url
				elt = getFirstChildElement(childElement, CloudControllerConstants.CONNECTION_URL_ELEMENT);
				if (elt != null) {
					dataHolder.setCassandraConnUrl(elt.getText());
				}

				// set user name
				elt = getFirstChildElement(childElement, CloudControllerConstants.USER_NAME_ELEMENT);
				if (elt != null) {
					dataHolder.setCassandraUser(elt.getText());
				}
				// set password
				elt = getFirstChildElement(childElement, CloudControllerConstants.PASSWORD_ELEMENT);
				if (elt != null) {
					String password = resolveSecret(elt);
					if (password == null) {
						plainTextWarn(CloudControllerConstants.PASSWORD_ELEMENT);
						password = elt.getText();
					}

					if (password != null) {
						dataHolder.setCassandraPassword(password);
					}
				}
			}

		}

	}

	/**
     * @param cartridgeElement Cartridges section as a {@link String}
     * @param aCartridge {@link Cartridge} instance.
     * @param deploymentNodes list of deployment directory nodes
     */
    private void setDeploymentDirs(String cartridgeElementString, Cartridge aCartridge,
                                   List<?> deploymentNodes) {
	    Object nodeObj;
	    if ((nodeObj = deploymentNodes.get(0)) instanceof OMNode) {
	    	OMNode deploymentNode = (OMNode) nodeObj;

	    	if (deploymentNode.getType() == OMNode.ELEMENT_NODE) {

	    		OMElement deployElt = (OMElement) deploymentNode;

	    		if (deployElt.getAttributeValue(new QName(
	    		                                          CloudControllerConstants.BASE_DIR_ATTR)) != null) {

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
	    		                     "' element cannot" + " be found in " +
	    		                     cartridgeElementString + " of " + xmlSource;
	    		handleException(msg);
	    	}

	    }
    }

	public void setIaasProvidersList() {

		List<IaasProvider> iaasProviders = FasterLookUpDataHolder.getInstance().getIaasProviders();

		if (iaasProviders == null) {
			FasterLookUpDataHolder.getInstance()
			                      .setIaasProviders((iaasProviders = new ArrayList<IaasProvider>()));
		}

		List<OMNode> nodeList = getMatchingNodes(CloudControllerConstants.IAAS_PROVIDER_XPATH);

		// this is a valid scenario. User can have 0..1 iaas provider elements
		// in cloud-controller xml.
		if (nodeList == null || nodeList.isEmpty()) {
			return;
		}

		for (OMNode node : nodeList) {
			iaasProviders.add(getIaasProvider(node, iaasProviders));
		}

	}


	public void setTopologySyncRelatedData() {

		String eltStr = CloudControllerConstants.TOPOLOGY_SYNC_ELEMENT;
		// get topologySync element
		OMElement element =
		                    getElement(documentElement, eltStr,
		                               CloudControllerConstants.TOPOLOGY_SYNC_XPATH);

		if (element == null) {
			return;
		}

		// get enable attribute
		boolean isEnable =
		                   Boolean.parseBoolean(element.getAttributeValue(new QName(
		                                                                            CloudControllerConstants.ENABLE_ATTR)));

		FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();
		dataHolder.setEnableTopologySync(isEnable);

		if (isEnable) {
			// get MB server info
			OMElement childElement =
			                         getFirstChildElement(element,
			                                              CloudControllerConstants.MB_SERVER_ELEMENT);

			if (childElement != null) {
				// set MB server IP
				dataHolder.setMBServerUrl(childElement.getText());
			}

			// set cron
			childElement = getFirstChildElement(element, CloudControllerConstants.CRON_ELEMENT);
			if (childElement != null) {
				dataHolder.setTopologySynchronizerCron(childElement.getText());
			}

		}

	}

	public boolean validate(final File schemaFile) throws Exception {
		validate(documentElement, schemaFile);
		return true;
	}

	public void validate(final OMElement omElement, final File schemaFile) throws Exception {

		Element sourceElement;

		// if the OMElement is created using DOM implementation use it
		if (omElement instanceof ElementImpl) {
			sourceElement = (Element) omElement;
		} else { // else convert from llom to dom
			sourceElement = getDOMElement(omElement);
		}

		// Create a SchemaFactory capable of understanding WXS schemas.

		// Load a WXS schema, represented by a Schema instance.
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Source source = new StreamSource(schemaFile);

		// Create a Validator object, which can be used to validate
		// an instance document.
		Schema schema = factory.newSchema(source);
		Validator validator = schema.newValidator();

		// Validate the DOM tree.
		validator.validate(new DOMSource(sourceElement));
	}

}

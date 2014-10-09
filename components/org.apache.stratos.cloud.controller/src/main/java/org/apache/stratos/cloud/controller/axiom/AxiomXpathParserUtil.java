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
package org.apache.stratos.cloud.controller.axiom;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.om.impl.dom.ElementImpl;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.MalformedConfigurationFileException;
import org.apache.stratos.cloud.controller.util.*;
import org.jaxen.JaxenException;
import org.w3c.dom.Element;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * This class is parsing configuration files using Axiom Xpath.
 */
public class AxiomXpathParserUtil {

    private static final Log LOG = LogFactory.getLog(AxiomXpathParserUtil.class);
    
    private AxiomXpathParserUtil(){}
    
    public static OMElement parse(File xmlSource) throws MalformedConfigurationFileException,
        IllegalArgumentException {

        OMElement documentElement;

        if (xmlSource == null) {
            String msg = "File is null.";
            LOG.error(msg);
            throw new IllegalArgumentException(msg);
        }

        try {
            documentElement = new StAXOMBuilder(xmlSource.getPath()).getDocumentElement();
            return documentElement;

        } catch (XMLStreamException e) {
            String msg = "Failed to parse the configuration file : " + xmlSource.getPath();
            LOG.error(msg, e);
            throw new MalformedConfigurationFileException(msg, e);
        } catch (FileNotFoundException e) {
            String msg = "Configuration file cannot be found : " + xmlSource.getPath();
            LOG.error(msg);
            throw new MalformedConfigurationFileException(msg);
        }

    }

    private static Element getDOMElement(final OMElement omElement) {

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

    private static OMElement getElement(final Object obj) {
        OMNode node;
        if ((obj instanceof OMNode) && (node = (OMNode) obj).getType() == OMNode.ELEMENT_NODE) {

            OMElement element = (OMElement) node;

            return element;

        }

        return null;
    }

    public static OMElement getElement(final String fileName, final OMElement rootElt,
        final String eltStr, final String xpath) {
        List<?> nodes = getMatchingNodes(xpath, rootElt);
        neglectingWarn(fileName, eltStr, nodes.size());
        OMElement element = getElement(nodes.get(0));
        return element;
    }

    public static OMElement getFirstChildElement(final OMElement root, final String childName) {
        Iterator<?> it = root.getChildrenWithName(new QName(childName));
        if (it.hasNext()) {
            return (OMElement) it.next();
        }

        return null;
    }

    private static void neglectingWarn(final String fileName, final String elt, final int size) {
        if (size > 1) {
            LOG.warn(fileName + " contains more than one " + elt + " elements!" +
                     " Elements other than the first will be neglected.");
        }
    }

    public static void plainTextWarn(final String elt) {
        LOG.warn("Unable to find a value for " + elt + " element from Secure Vault." +
                 "Hence we will try to assign the plain text value (if specified).");
    }

    /**
     * @param xpath
     *            XPATH expression to be read.
     * @param elt
     *            OMElement to be used for the search.
     * @return List matching OMNode list
     */
    @SuppressWarnings("unchecked")
    public static OMNode getFirstMatchingNode(final String xpath, final OMElement elt) throws MalformedConfigurationFileException{

        AXIOMXPath axiomXpath;
        List<OMNode> nodeList = null;
        try {
            axiomXpath = new AXIOMXPath(xpath);
            nodeList = axiomXpath.selectNodes(elt);
            return nodeList.isEmpty() ?  null : nodeList.get(0);
        } catch (JaxenException e) {
            String msg = "Error occurred while reading the Xpath (" + xpath + ")";
            LOG.error(msg, e);
            throw new MalformedConfigurationFileException(msg, e);
        }

    }

    /**
     * @param xpath
     *            XPATH expression to be read.
     * @return List matching list
     */
    @SuppressWarnings("unchecked")
    public static List<OMNode> getMatchingNodes(OMElement elt, final String xpath) throws MalformedConfigurationFileException{

        AXIOMXPath axiomXpath;
        List<OMNode> nodeList = null;
        try {
            axiomXpath = new AXIOMXPath(xpath);
            nodeList = axiomXpath.selectNodes(elt);
            return nodeList;
        } catch (JaxenException e) {
            String msg = "Error occurred while reading the Xpath (" + xpath + ")";
            LOG.error(msg, e);
            throw new MalformedConfigurationFileException(msg, e);
        }

    }

    /**
     * @param xpath
     *            XPATH expression to be read.
     * @param elt
     *            OMElement to be used for the search.
     * @return List matching OMNode list
     */
    @SuppressWarnings("unchecked")
    public static List<OMNode> getMatchingNodes(final String xpath, final OMElement elt) throws MalformedConfigurationFileException{

        AXIOMXPath axiomXpath;
        List<OMNode> nodeList = null;
        try {
            axiomXpath = new AXIOMXPath(xpath);
            nodeList = axiomXpath.selectNodes(elt);
            return nodeList;
        } catch (JaxenException e) {
            String msg = "Error occurred while reading the Xpath (" + xpath + ")";
            LOG.error(msg, e);
            throw new MalformedConfigurationFileException(msg, e);
        }

    }

    public static void validate(final OMElement omElement, final File schemaFile) throws SAXException, IOException {

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

    public static String resolveSecret(final OMElement docElt, final OMElement elt) {
        // retrieve the value using secure vault
        SecretResolver secretResolver = SecretResolverFactory.create(docElt, false);

        String alias = elt.getAttributeValue(new QName(CloudControllerConstants.ALIAS_ATTRIBUTE));

        // retrieve the secured password
        if (secretResolver != null && secretResolver.isInitialized() &&
            secretResolver.isTokenProtected(alias)) {

            return secretResolver.resolve(alias);

        }

        return null;
    }
    
    

}

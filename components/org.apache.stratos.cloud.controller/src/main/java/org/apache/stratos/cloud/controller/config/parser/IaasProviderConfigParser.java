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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.MalformedConfigurationFileException;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

/**
 * Parse the iaas providers.
 *
 *
 */
public class IaasProviderConfigParser {
    private static final Log log = LogFactory.getLog(IaasProviderConfigParser.class);

    public static IaasProvider getIaasProvider(final String fileName, final OMElement elt, final OMNode item, List<IaasProvider> iaases) {

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
            loadClassName(fileName, iaas, iaasElt);
            loadProvider(fileName, iaas, iaasElt);
            loadProperties(fileName, iaasElt, iaas.getProperties());
            loadTemplate(fileName, iaas, iaasElt);
            loadIdentity(fileName, elt, iaas, iaasElt);
            loadCredentials(fileName, elt, iaas, iaasElt, xpath);
        }

        return iaas;
    }
    
    private static void loadClassName(final String fileName, final IaasProvider iaas, final OMElement iaasElt) {

        Iterator<?> it =
                iaasElt.getChildrenWithName(new QName(
                        CloudControllerConstants.CLASS_NAME_ELEMENT));

        if (it.hasNext()) {
            OMElement classNameElt = (OMElement) it.next();
            iaas.setClassName(classNameElt.getText());
        }

        if (it.hasNext()) {
            log.warn(" file contains more than one " +
                    CloudControllerConstants.CLASS_NAME_ELEMENT + " elements!" +
                    " Elements other than the first will be neglected.");
        }

        if (iaas.getClassName() == null) {
            String msg =
                    "Essential '" + CloudControllerConstants.CLASS_NAME_ELEMENT + "' element " +
                            "has not specified in " + fileName;
            handleException(msg);
        }

    }

    private static void loadCredentials(final String fileName, final OMElement elt, final IaasProvider iaas, final OMElement iaasElt,
                                        final String xpath) {

        Iterator<?> it =
                iaasElt.getChildrenWithName(new QName(
                        CloudControllerConstants.CREDENTIAL_ELEMENT));

        if (it.hasNext()) {
            OMElement credentialElt = (OMElement) it.next();

            // retrieve the value using secure vault
            SecretResolver secretResolver = SecretResolverFactory.create(elt, false);
            String alias = credentialElt.getAttributeValue(new QName(
                    CloudControllerConstants.ALIAS_NAMESPACE,
                    CloudControllerConstants.ALIAS_ATTRIBUTE,
                    CloudControllerConstants.ALIAS_ATTRIBUTE_PREFIX));

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
            log.warn(fileName + " contains more than one " +
                    CloudControllerConstants.CREDENTIAL_ELEMENT + " elements!" +
                    " Elements other than the first will be neglected.");
        }

        if (iaas.getCredential() == null) {
            String msg =
                    "Essential '" + CloudControllerConstants.CREDENTIAL_ELEMENT + "' element" +
                            " has not specified in " + fileName;
            handleException(msg);
        }

    }


    private static void loadIdentity(final String fileName, final OMElement elt, final IaasProvider iaas, final OMElement iaasElt) {

        Iterator<?> it =
                iaasElt.getChildrenWithName(new QName(CloudControllerConstants.IDENTITY_ELEMENT));

        if (it.hasNext()) {
            OMElement identityElt = (OMElement) it.next();

            // retrieve the value using secure vault
            SecretResolver secretResolver = SecretResolverFactory.create(elt, false);
            String alias = identityElt.getAttributeValue(new QName(
                    CloudControllerConstants.ALIAS_NAMESPACE,
                    CloudControllerConstants.ALIAS_ATTRIBUTE,
                    CloudControllerConstants.ALIAS_ATTRIBUTE_PREFIX));

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
            log.warn(fileName + " contains more than one " + CloudControllerConstants.IDENTITY_ELEMENT +
                    " elements!" + " Elements other than the first will be neglected.");
        }

        if (iaas.getIdentity() == null) {
            String msg =
                    "Essential '" + CloudControllerConstants.IDENTITY_ELEMENT + "' element" +
                            " has not specified in " + fileName;
            handleException(msg);
        }

    }



    public static void loadProperties(final String fileName, final OMElement elt, final Map<String, String> propertyMap) {

        Iterator<?> it =
                elt.getChildrenWithName(new QName(CloudControllerConstants.PROPERTY_ELEMENT));

        while (it.hasNext()) {
            OMElement prop = (OMElement) it.next();

            if (prop.getAttribute(new QName(CloudControllerConstants.PROPERTY_NAME_ATTR)) == null ||
                    prop.getAttribute(new QName(CloudControllerConstants.PROPERTY_VALUE_ATTR)) == null) {

                String msg =
                        "Property element's, name and value attributes should be specified " +
                                "in " + fileName;

                handleException(msg);
            }

            propertyMap.put(prop.getAttribute(new QName(CloudControllerConstants.PROPERTY_NAME_ATTR))
                    .getAttributeValue(),
                    prop.getAttribute(new QName(CloudControllerConstants.PROPERTY_VALUE_ATTR))
                            .getAttributeValue());
        }

    }

    private static void loadProvider(final String fileName, final IaasProvider iaas, final OMElement iaasElt) {

        Iterator<?> it =
                iaasElt.getChildrenWithName(new QName(CloudControllerConstants.PROVIDER_ELEMENT));

        if (it.hasNext()) {
            OMElement providerElt = (OMElement) it.next();
            iaas.setProvider(providerElt.getText());
        }

        if (it.hasNext()) {
            log.warn(fileName + " contains more than one " + CloudControllerConstants.PROVIDER_ELEMENT +
                    " elements!" + " Elements other than the first will be neglected.");
        }

        if (iaas.getProvider() == null) {
            String msg =
                    "Essential '" + CloudControllerConstants.PROVIDER_ELEMENT + "' element " +
                            "has not specified in " + fileName;
            handleException(msg);
        }

    }


    private static void loadTemplate(final String fileName, final IaasProvider iaas, final OMElement iaasElt) {

        Iterator<?> it =
                iaasElt.getChildrenWithName(new QName(CloudControllerConstants.IMAGE_ID_ELEMENT));

        if (it.hasNext()) {
            OMElement imageElt = (OMElement) it.next();
            iaas.setImage(imageElt.getText());
        }

        if (it.hasNext()) {
            log.warn(fileName + " contains more than one " + CloudControllerConstants.IMAGE_ID_ELEMENT +
                    " elements!" + " Elements other than the first will be neglected.");
        }

    }

    
    private static void handleException(final String msg) throws MalformedConfigurationFileException{
        log.error(msg);
        throw new MalformedConfigurationFileException(msg);
    }

}

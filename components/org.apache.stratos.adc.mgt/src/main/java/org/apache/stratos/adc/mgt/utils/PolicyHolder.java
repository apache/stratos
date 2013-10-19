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
package org.apache.stratos.adc.mgt.utils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.om.impl.dom.ElementImpl;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.dto.Policy;
import org.apache.stratos.adc.mgt.dto.PolicyDefinition;
import org.jaxen.JaxenException;
import org.w3c.dom.Element;
import org.wso2.carbon.utils.CarbonUtils;
import org.xml.sax.SAXException;

import com.google.gson.Gson;

public class PolicyHolder {

	private static Log log = LogFactory.getLog(PolicyHolder.class);

	private static final String POLICIES_ELEMENT = "policies";
	private static final String POLICY_ELEMENT = "policy";

	private static final String POLICIES_ELEMENT_XPATH = "/" + POLICIES_ELEMENT + "/" + POLICY_ELEMENT;

	private static final String NAME_ATTR = "name";
	private static final String IS_DEFAULT_ATTR = "isDefault";
	private static final String DESCRIPTION_ELEMENT = "description";
	private static final String MIN_APP_INSTANCES_ELEMENT = "min_app_instances";
	private static final String MAX_APP_INSTANCES_ELEMENT = "max_app_instances";
	private static final String MAX_REQUESTS_PER_SECOND_ELEMENT = "max_requests_per_second";
	private static final String ALARMING_UPPER_RATE_ELEMENT = "alarming_upper_rate";
	private static final String ALARMING_LOWER_RATE_ELEMENT = "alarming_lower_rate";
	private static final String SCALE_DOWN_FACTOR_ELEMENT = "scale_down_factor";
	private static final String ROUNDS_TO_AVERAGE_ELEMENT = "rounds_to_average";

	private Map<String, Policy> policyMap = new HashMap<String, Policy>();

	private Policy defaultPolicy;

	private List<PolicyDefinition> policyDefinitions = new ArrayList<PolicyDefinition>();

	private PolicyHolder(File policiesSchema, File policiesXML) {
		try {
			readPolicies(policiesSchema, policiesXML);
		} catch (Exception e) {
			log.error("Error reading policies", e);
		}
	}

	private static class SingletonHolder {
		private final static PolicyHolder INSTANCE = new PolicyHolder(new File(CarbonUtils.getCarbonConfigDirPath()
				+ File.separator + "etc" + File.separator, "policies.xsd"), new File(
				CarbonUtils.getCarbonConfigDirPath(), "policies.xml"));
	}

	public static PolicyHolder getInstance() {
		return SingletonHolder.INSTANCE;
	}

	public Policy getPolicy(String policyName) {
		return policyMap.get(policyName);
	}

	public Policy getDefaultPolicy() {
		return defaultPolicy;
	}

	public List<PolicyDefinition> getPolicyDefinitions() {
		return policyDefinitions;
	}

	private void readPolicies(File policiesSchema, File policiesXML) throws XMLStreamException, JaxenException,
			SAXException, IOException {
		if (log.isDebugEnabled()) {
			log.debug("Policies schema: " + policiesSchema.getPath());
			log.debug("Loading policies from file: " + policiesXML.getPath());
		}
		OMElement documentElement;
		if (policiesXML.exists()) {
			documentElement = new StAXOMBuilder(policiesXML.getPath()).getDocumentElement();
		} else {
			throw new IllegalStateException("Policies file cannot be found : " + policiesXML.getPath());
		}

		// Validate XML
		validate(documentElement, policiesSchema);
		
		String xpath = POLICIES_ELEMENT_XPATH;

		AXIOMXPath axiomXpath;
		axiomXpath = new AXIOMXPath(xpath);
		@SuppressWarnings("unchecked")
		List<OMNode> policyNodes = axiomXpath.selectNodes(documentElement);

		if (policyNodes == null || policyNodes.isEmpty()) {
			log.warn("No policies found in the file : " + policiesXML.getPath());
			return;
		}

		for (OMNode policyNode : policyNodes) {

			if (policyNode.getType() == OMNode.ELEMENT_NODE) {

				OMElement policyElement = (OMElement) policyNode;

				try {
					readPolicy(policyElement);
				} catch (Exception e) {
					log.error("Error reading policy", e);
				}
			}
		}
	}

	private void readPolicy(OMElement policyElement) {
		// retrieve attributes
		String name = policyElement.getAttributeValue(new QName(NAME_ATTR));
		boolean isDefault = Boolean.valueOf(policyElement.getAttributeValue(new QName(IS_DEFAULT_ATTR)));

		Policy policy = new Policy();
		policy.setName(name);
		policy.setDefaultPolicy(isDefault);

		// read description
		Iterator<?> it = policyElement.getChildrenWithName(new QName(DESCRIPTION_ELEMENT));

		if (it.hasNext()) {
			OMElement element = (OMElement) it.next();
			policy.setDescription(element.getText());
		}

		// read min_app_instances
		it = policyElement.getChildrenWithName(new QName(MIN_APP_INSTANCES_ELEMENT));

		if (it.hasNext()) {
			OMElement element = (OMElement) it.next();
			policy.setMinAppInstances(Integer.parseInt(element.getText()));
		}

		// read max_app_instances
		it = policyElement.getChildrenWithName(new QName(MAX_APP_INSTANCES_ELEMENT));

		if (it.hasNext()) {
			OMElement element = (OMElement) it.next();
			policy.setMaxAppInstances(Integer.parseInt(element.getText()));
		}

		// read max_requests_per_second
		it = policyElement.getChildrenWithName(new QName(MAX_REQUESTS_PER_SECOND_ELEMENT));

		if (it.hasNext()) {
			OMElement element = (OMElement) it.next();
			policy.setMaxRequestsPerSecond(Integer.parseInt(element.getText()));
		}

		// read rounds_to_average
		it = policyElement.getChildrenWithName(new QName(ROUNDS_TO_AVERAGE_ELEMENT));

		if (it.hasNext()) {
			OMElement element = (OMElement) it.next();
			policy.setRoundsToAverage(Integer.parseInt(element.getText()));
		}

		// read alarming_upper_rate
		it = policyElement.getChildrenWithName(new QName(ALARMING_UPPER_RATE_ELEMENT));

		if (it.hasNext()) {
			OMElement element = (OMElement) it.next();
			policy.setAlarmingUpperRate(new BigDecimal(element.getText()));
		}

		// read alarming_lower_rate
		it = policyElement.getChildrenWithName(new QName(ALARMING_LOWER_RATE_ELEMENT));

		if (it.hasNext()) {
			OMElement element = (OMElement) it.next();
			policy.setAlarmingLowerRate(new BigDecimal(element.getText()));
		}

		// read scale_down_factor
		it = policyElement.getChildrenWithName(new QName(SCALE_DOWN_FACTOR_ELEMENT));

		if (it.hasNext()) {
			OMElement element = (OMElement) it.next();
			policy.setScaleDownFactor(new BigDecimal(element.getText()));
		}
		if (log.isDebugEnabled()) {
			log.debug("Policy: " + new Gson().toJson(policy));
		}

		policyMap.put(policy.getName(), policy);
		PolicyDefinition policyDefinition = new PolicyDefinition();
		policyDefinition.setName(policy.getName());
		policyDefinition.setDescription(policy.getDescription());
		policyDefinition.setDefaultPolicy(policy.isDefaultPolicy());
		policyDefinitions.add(policyDefinition);
		
		// Set first default policy
		if (defaultPolicy == null && policy.isDefaultPolicy()) {
			defaultPolicy = policy;
		}
	}
	
	// TODO Following code is copied from
	// org.wso2.carbon.stratos.cloud.controller.axiom.AxiomXpathParser
	// There should be a common util to validate XML using a schema.
	public void validate(final OMElement omElement, final File schemaFile) throws SAXException, IOException {

		Element sourceElement;

		// if the OMElement is created using DOM implementation use it
		if (omElement instanceof ElementImpl) {
			sourceElement = (Element) omElement;
		} else { // else convert from llom to dom
			sourceElement = getDOMElement(omElement);
		}

		// Create a SchemaFactory capable of understanding WXS schemas.

		// Load a WXS schema, represented by a Schema subscription.
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Source source = new StreamSource(schemaFile);

		// Create a Validator object, which can be used to validate
		// an subscription document.
		Schema schema = factory.newSchema(source);
		Validator validator = schema.newValidator();

		// Validate the DOM tree.
		validator.validate(new DOMSource(sourceElement));
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
}

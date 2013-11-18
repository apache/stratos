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

package org.apache.stratos.autoscaler.policy.deployers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.policy.InvalidPolicyException;

/**
 * Abstract super-class, with some common logic, for reading policies. The only
 * method subclasses must implement is read().
 */
public abstract class AbstractPolicyReader<T> {

	private static final Log log = LogFactory.getLog(AbstractPolicyReader.class);

	protected File file;
	private FileInputStream fStream;

	protected AbstractPolicyReader(File file) {
		this.setFile(file);
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public abstract T read() throws InvalidPolicyException;

	protected OMElement getDocument() throws Exception {
		fStream = new FileInputStream(file);
		XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(fStream);
		StAXOMBuilder builder = new StAXOMBuilder(parser);
		return builder.getDocumentElement();
	}

	protected void closeStream() {
		try {
			if (fStream != null) {
				fStream.close();
			}
		} catch (IOException e) {
			log.debug("Unable to close the input stream", e);
		}
	}

	protected String readValueAttr(OMElement ele, String qName) {
		OMElement valueContainer = ele.getFirstChildWithName(new QName(qName));
		String value = valueContainer.getAttributeValue(new QName("value"));
		return value;
	}

	protected String readValue(OMElement ele, String qName) {
		OMElement valueContainer = ele.getFirstChildWithName(new QName(qName));
		String value = valueContainer.getText();
		return value;
	}

}

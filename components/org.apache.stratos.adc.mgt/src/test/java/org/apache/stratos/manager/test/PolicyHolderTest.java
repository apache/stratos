package org.apache.stratos.manager.test;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import junit.framework.TestCase;

import org.apache.stratos.manager.utils.PolicyHolder;

public class PolicyHolderTest extends TestCase {

	private PolicyHolder getPolicyHolder(String policiesXMLFile) {
		File policiesXmlSchema = new File("src/main/resources/policies.xsd");
		String dir = "src/test/resources/";

		Class<PolicyHolder> clazz = PolicyHolder.class;

		Constructor<PolicyHolder> c;
		try {
			c = clazz.getDeclaredConstructor(File.class, File.class);
			c.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		}
		PolicyHolder policyHolder;
		try {
			policyHolder = c.newInstance(policiesXmlSchema, new File(dir, policiesXMLFile));
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		return policyHolder;
	}

	public void testDefaultPolicy() {
		PolicyHolder policyHolder = getPolicyHolder("policies-1.xml");
		assertNotNull(policyHolder);
		assertNotNull(policyHolder.getDefaultPolicy());
	}
	
	
	public void testSinglePolicy() {
		PolicyHolder policyHolder = getPolicyHolder("policies-1.xml");
		assertNotNull(policyHolder);
		assertNotNull(policyHolder.getPolicy("single"));
		assertEquals("single", policyHolder.getPolicy("single").getName());
	}
	
	public void testElasticPolicy() {
		PolicyHolder policyHolder = getPolicyHolder("policies-1.xml");
		assertNotNull(policyHolder);
		assertNotNull(policyHolder.getPolicy("elastic"));
		assertEquals("elastic", policyHolder.getPolicy("elastic").getName());
	}
}

package org.wso2.carbon.adc.mgt.test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import junit.framework.TestCase;

import org.wso2.carbon.adc.mgt.utils.PolicyHolder;

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

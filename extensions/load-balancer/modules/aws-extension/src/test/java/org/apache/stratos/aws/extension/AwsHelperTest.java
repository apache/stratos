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

package org.apache.stratos.aws.extension;

import com.amazonaws.services.ec2.model.Filter;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;

import org.junit.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;


/**
 * AwsHelper util test.
 */
public class AwsHelperTest {

    @Test
    public void testSecurityGroupFilters()
		    throws LoadBalancerExtensionException, NoSuchMethodException, InvocationTargetException,
		           IllegalAccessException {
	    Set<Filter> filters=(Set<Filter>)genericInvokMethod(new AWSHelper(null,null), "getFilters",2,"Test VPC ID","test");

	    Assert.assertNotNull(filters);
	    Assert.assertEquals(2,filters.size());

    }

	/**
	 * Method to access the private methods
	 * @param obj
	 * @param methodName
	 * @param paramCount
	 * @param params
	 * @return
	 */
	public static Object genericInvokMethod(Object obj, String methodName,
	                                        int paramCount, Object... params) {
		Method method;
		Object requiredObj = null;
		Object[] parameters = new Object[paramCount];
		Class<?>[] classArray = new Class<?>[paramCount];
		for (int i = 0; i < paramCount; i++) {
			parameters[i] = params[i];
			classArray[i] = params[i].getClass();
		}
		try {
			method = obj.getClass().getDeclaredMethod(methodName, classArray);
			method.setAccessible(true);
			requiredObj = method.invoke(obj, params);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return requiredObj;
	}
 }

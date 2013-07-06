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
package org.apache.stratos.common.util;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class MeteringAccessValidationUtils {

	public static final String ERROR_MSG_PROPERTY_KEY_POST_FIX = "error_msg";
	public static final String IS_BLOCKED_PROPERTY_KEY_POST_FIX = "is_blocked";

	public static String generateIsBlockedPropertyKey(String action) {
		return action + "_" + IS_BLOCKED_PROPERTY_KEY_POST_FIX;
	}

	public static String generateErrorMsgPropertyKey(String action) {
		return action + "_" + ERROR_MSG_PROPERTY_KEY_POST_FIX;
	}

	public static Set<String> getAvailableActions(Properties properties) {
		Set propertyKeys = properties.keySet();
		Set<String> actions = new HashSet<String>();
		for (Object propertyKeyObj : propertyKeys) {
			String propertyKey = (String) propertyKeyObj;
			if (propertyKey.endsWith(IS_BLOCKED_PROPERTY_KEY_POST_FIX)) {
			    // -1 for the length of the '_'
				String action =
				        propertyKey.substring(0, propertyKey.length() -
				                IS_BLOCKED_PROPERTY_KEY_POST_FIX.length() - 1);
				actions.add(action);
			}
		}
		return actions;
	}
}

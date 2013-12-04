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

package org.apache.stratos.cartridge.agent.event.subscriber;

import java.io.File;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LaunchParamsUtil {

	private static final Log log = LogFactory.getLog(LaunchParamsUtil.class);
	
	public static String readParamValueFromPayload(String param) {
		String paramValue = null;
		// read launch params
		File file = new File(System.getProperty(CartridgeAgentConstants.PARAM_FILE_PATH));

		try {
			Scanner scanner = new Scanner(file);

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] params = line.split(",");
				for (String string : params) {
					 String[] var = string.split("=");
					if(param.equals(var[0])){
						paramValue = var[1];
					}
				}
			}
			scanner.close();
		} catch (Exception e) {
			//e.printStackTrace();
			log.error("Exception is occurred in reading file. ", e);
		}
		
		return paramValue;
	}
}

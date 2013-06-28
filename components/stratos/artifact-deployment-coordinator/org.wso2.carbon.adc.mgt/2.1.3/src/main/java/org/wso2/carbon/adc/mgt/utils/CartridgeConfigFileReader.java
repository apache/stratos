/*
 * Copyright WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.adc.mgt.utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.utils.CarbonUtils;

public class CartridgeConfigFileReader {

	private static String carbonHome = CarbonUtils.getCarbonHome();

	private static final Log log = LogFactory.getLog(CartridgeConfigFileReader.class);

	/**
	 * 
	 * Reads cartridge-config.properties file and assign properties to system
	 * properties
	 * 
	 */
	public static void readProperties() {

		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(carbonHome + File.separator + "repository" +
			                                    File.separator + "conf" + File.separator +
			                                    "cartridge-config.properties"));
		} catch (Exception e) {
			log.error("Exception is occurred in reading properties file. Reason:" + e.getMessage());
		}
		if (log.isInfoEnabled()) {
			log.info("Setting config properties into System properties");
		}

		for (String name : properties.stringPropertyNames()) {
			String value = properties.getProperty(name);
			System.setProperty(name, value);
		}
	}

}

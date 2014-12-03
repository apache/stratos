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

package org.apache.stratos.autoscaler.util;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.Constants;

import java.io.File;
import java.util.HashMap;

/**
 * This class contains utility methods for read configuration file.
 */
public class ConfUtil {

	private static Log log = LogFactory.getLog(ConfUtil.class);

	private XMLConfiguration config;

	//To maintain the map of config files
	private static HashMap<String, ConfUtil> instanceMap = new HashMap<String, ConfUtil>();

	private ConfUtil(String configFilePath) {
		try {

			File confFile = new File(configFilePath);
			config = new XMLConfiguration(confFile);
		} catch (ConfigurationException e) {
			log.error("Unable to load configuration file", e);
			config = new XMLConfiguration();  // continue with default values
		}
	}

	/**
	 * Get the instance of the configuration file
	 *
	 * @param configFilePath configuration file name
	 * @return ConfUtil instance
	 */
	public static ConfUtil getInstance(String configFilePath) {

		if (configFilePath == null || configFilePath.isEmpty()) {
			configFilePath = Constants.AUTOSCALER_CONFIG_FILE_NAME;
		}
		ConfUtil instance = instanceMap.get(configFilePath);
		if (instance == null) {
			instance = new ConfUtil(configFilePath);
			instanceMap.put(configFilePath, instance);
		}
		return instance;
	}

	/**
	 * Get configurations
	 *
	 * @return XMLConfiguration
	 */
	public XMLConfiguration getConfiguration() {
		return config;
	}

}

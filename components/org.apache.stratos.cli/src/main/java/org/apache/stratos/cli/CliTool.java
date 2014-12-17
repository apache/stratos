/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.cli;

import static org.apache.stratos.cli.utils.CliConstants.STRATOS_DIR;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used for input the commands through CLITool, command prompt.
 */
public class CliTool {

	private static final Logger log = LoggerFactory.getLogger(CliTool.class);

	/**
	 * Here is the place all the command line inputs get processed
	 * 
	 * @param arguments
	 *            passed to CLI tool.
	 */
	void handleConsoleInputs(String[] arguments) {
		if (log.isInfoEnabled()) {
			log.info("Stratos CLI started...");
		}
		StratosApplication application = new StratosApplication(arguments);
		application.start(arguments);
	}

	void createConfigDirectory() {
		File stratosFile = new File(System.getProperty("user.home"), STRATOS_DIR);
		if (stratosFile.exists()) {
			if (log.isInfoEnabled()) {
				log.info("Using directory: {}", stratosFile.getPath());
			}
		} else {
			if (stratosFile.mkdir()) {
				if (log.isInfoEnabled()) {
					log.info("Created directory: {}", stratosFile.getPath());
				}
			} else if (log.isWarnEnabled()) {
				log.warn("Failed to created directory: {}", stratosFile.getPath());
			}
		}
	}

}

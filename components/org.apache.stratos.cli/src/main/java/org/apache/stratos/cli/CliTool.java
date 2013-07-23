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

	private static final Logger logger = LoggerFactory.getLogger(CliTool.class);

	/**
	 * Main executable method used to call from CLI.
	 * 
	 */
	public static void main(final String[] args) {
		CliTool cliTool = new CliTool();
		cliTool.createConfigDirectory();
		cliTool.handleConsoleInputs(args);
	}

	/**
	 * Here is the place all the command line inputs get processed
	 * 
	 * @param arguments
	 *            passed to CLI tool.
	 */
	private void handleConsoleInputs(String[] arguments) {
		if (logger.isInfoEnabled()) {
			logger.info("Stratos CLI Started...");
		}
		StratosApplication application = new StratosApplication();
		application.start(arguments);
	}

	private void createConfigDirectory() {
		File stratosFile = new File(System.getProperty("user.home"), STRATOS_DIR);
		if (stratosFile.exists()) {
			if (logger.isInfoEnabled()) {
				logger.info("Using directory: {}", stratosFile.getPath());
			}
		} else {
			if (stratosFile.mkdir()) {
				if (logger.isInfoEnabled()) {
					logger.info("Created directory: {}", stratosFile.getPath());
				}
			} else if (logger.isWarnEnabled()) {
				logger.warn("Failed to created directory: {}", stratosFile.getPath());
			}
		}
	}

}

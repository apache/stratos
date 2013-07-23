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

import java.io.File;
import java.io.IOException;

import jline.console.ConsoleReader;
import jline.console.history.FileHistory;

import org.apache.stratos.cli.utils.CliConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CommandLineApplication<T extends CommandContext> {

	private static final Logger logger = LoggerFactory.getLogger(CommandLineApplication.class);

	protected final ConsoleReader reader;
	protected FileHistory history;

	public CommandLineApplication() {
		reader = createConsoleReader();
	}

	/**
	 * Creates new jline ConsoleReader.
	 * 
	 * @return a jline ConsoleReader instance
	 */
	protected ConsoleReader createConsoleReader() {
		ConsoleReader consoleReader = null;
		try {
			consoleReader = new ConsoleReader();
			consoleReader.setPrompt(getPrompt());
			history = new FileHistory(getHistoryFile());
			consoleReader.setHistory(history);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot create jline console reader", e);
		}
		return consoleReader;
	}

	public ConsoleReader getConsoleReader() {
		return reader;
	}

	protected abstract String getPrompt();

	/**
	 * Get the history file for the Console Reader.
	 * 
	 * @return File for storing history
	 */
	protected abstract File getHistoryFile();

	public final void start(String[] args) {
		Thread shutdownHookThread = new Thread("CLI Shutdown Hook") {
			@Override
			public void run() {
				performDestroy();
			}
		};
		Runtime.getRuntime().addShutdownHook(shutdownHookThread);
		int returnCode = run(args);
		if (logger.isDebugEnabled()) {
			logger.debug("Exiting with error code {}", returnCode);
		}
		System.exit(returnCode);
	}

	protected abstract int run(String[] args);

	protected void promptLoop() {
		String line = null;
		boolean exit = false;

		try {
			while (!exit && (reader != null && ((line = reader.readLine()) != null))) {
				if ("".equals(line)) {
					continue;
				}
				if (StringUtils.isNotBlank(line)) {
					execute(line);
					exit = CliConstants.EXIT_ACTION.equals(line.trim());
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Error in reading line", e);
		}
	}

	private int execute(String line) {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Executing command line: \"{}\"", line);
			}
			int returnCode = executeCommand(line);
			if (logger.isDebugEnabled()) {
				logger.debug("Command line executed \"{}\". Return code: {}", line, returnCode);
			}
			return returnCode;
		} catch (RuntimeException e) {
			if (logger.isErrorEnabled()) {
				logger.error("Error executing command line: " + line, e);
			}
			return 1;
		}
	}

	protected abstract int executeCommand(String line);

	private void performDestroy() {
		if (logger.isDebugEnabled()) {
			logger.debug("Shutting down application... Invoking destroy methods");
		}
		if (history != null) {
			try {
				history.flush();
			} catch (IOException e) {
				if (logger.isErrorEnabled()) {
					logger.error("Error flushing history.", e);
				}
			}
		}
		destroy();
	}

	/**
	 * May override to perform action before destroying
	 */
	protected void destroy() {
	};

	public String getInput(String prompt) {
		return getInput(prompt, null);
	}

	public String getInput(String prompt, Character mask) {
		String line = null;
		try {
			reader.setPrompt(prompt + ": ");
			while ((line = reader.readLine(mask)) != null) {
				if ("".equals(line)) {
					continue;
				}
				return line;
			}
		} catch (IOException e) {
			throw new IllegalStateException("Error in reading line", e);
		} finally {
			reader.setPrompt(CliConstants.STRATOS_SHELL_PROMPT);
		}
		return line;
	}
	
	/**
	 * @return {@code true if user confirmed}
	 */
	public boolean getConfirmation(String prompt) {
		prompt = prompt + " [yes/no]";

		String input = "";
		int tries = 0;
		do {
			tries++;
			input = getInput(prompt);
		} while (!"y".equals(input) && !"yes".equals(input) && !"n".equals(input) && !"no".equals(input) && tries < 3);

		return "y".equals(input) || "yes".equals(input);
	}

}

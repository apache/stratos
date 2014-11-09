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
package org.apache.stratos.cli.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.stratos.cli.RestCommandLineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.stratos.cli.Command;
import org.apache.stratos.cli.StratosCommandContext;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;

@Deprecated
public class UnsubscribeCommand implements Command<StratosCommandContext> {

	private static final Logger logger = LoggerFactory.getLogger(UnsubscribeCommand.class);
	
	private final Options options;

	public UnsubscribeCommand() {
		options = constructOptions();
	}
	
	/**
	 * Construct Options.
	 * 
	 * @return Options expected from command-line.
	 */
	private Options constructOptions() {
		final Options options = new Options();
		Option forceOption = new Option(CliConstants.FORCE_OPTION, CliConstants.FORCE_LONG_OPTION, false,
				"Never prompt for confirmation");
		options.addOption(forceOption);
		return options;
	}

	@Override
	public String getName() {
		return CliConstants.UNSUBSCRIBE_ACTION;
	}

	@Override
	public String getDescription() {
		return "Unsubscribe from a subscribed cartridge";
	}

	@Override
	public String getArgumentSyntax() {
		return "[Cartridge alias]";
	}

	@Override
	public int execute(StratosCommandContext context, String[] args) throws CommandException {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing {} command...", getName());
		}
		if (args != null && args.length > 0) {
			String[] remainingArgs = null;
			String alias = null;
			boolean force = false;
			final CommandLineParser parser = new GnuParser();
			CommandLine commandLine;
			try {
				commandLine = parser.parse(options, args);
				remainingArgs = commandLine.getArgs();
				if (remainingArgs != null && remainingArgs.length == 1) {
					// Get alias
					alias = remainingArgs[0];
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Unsubscribe: not enough arguments");
					}
					context.getStratosApplication().printUsage(getName());
					return CliConstants.COMMAND_FAILED;
				}

				if (commandLine.hasOption(CliConstants.FORCE_OPTION)) {
					if (logger.isTraceEnabled()) {
						logger.trace("Force option is passed");
					}
					force = true;
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Unsubscribing {}, Force Option: {}", alias, force);
				}
				if (force || context.getApplication().getConfirmation("Are you sure you want to unsubscribe?")) {
					System.out.format("Unsubscribing the cartridge %s%n", alias);
					//CommandLineService.getInstance().unsubscribe(alias);
                    RestCommandLineService.getInstance().unsubscribe(alias);
				}
				return CliConstants.COMMAND_SUCCESSFULL;
			} catch (ParseException e) {
				if (logger.isErrorEnabled()) {
					logger.error("Error parsing arguments", e);
				}
				System.out.println(e.getMessage());
				return CliConstants.COMMAND_FAILED;
			}
		} else {
			context.getStratosApplication().printUsage(getName());
			return CliConstants.COMMAND_FAILED;
		}
	}

	@Override
	public Options getOptions() {
		return options;
	}

}

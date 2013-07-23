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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.stratos.cli.Command;
import org.apache.stratos.cli.CommandLineService;
import org.apache.stratos.cli.StratosCommandContext;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;

public class ListCommand implements Command<StratosCommandContext> {

	private static final Logger logger = LoggerFactory.getLogger(ListCommand.class);
	
	private final Options options;

	public ListCommand() {
		options = constructOptions();
	}
	
	/**
	 * Construct Options.
	 * 
	 * @return Options expected from command-line.
	 */
	private Options constructOptions() {
		final Options options = new Options();
		Option fullOption = new Option(CliConstants.FULL_OPTION, CliConstants.FULL_LONG_OPTION, false,
				"Display extra details");
		options.addOption(fullOption);
		return options;
	}

	@Override
	public String getName() {
		return CliConstants.LIST_ACTION;
	}

	@Override
	public String getDescription() {
		return "List subscribed cartridges with details";
	}

	@Override
	public String getArgumentSyntax() {
		return null;
	}

	@Override
	public int execute(StratosCommandContext context, String[] args) throws CommandException {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing {} command...", getName());
		}
		if (args == null || args.length == 0) {
			CommandLineService.getInstance().listSubscribedCartridges(false);
			return CliConstants.SUCCESSFUL_CODE;
		} else if (args != null && args.length > 0) {
			String[] remainingArgs = null;
			boolean full = false;
			final CommandLineParser parser = new GnuParser();
			CommandLine commandLine;
			try {
				commandLine = parser.parse(options, args);
				remainingArgs = commandLine.getArgs();
				if (!(remainingArgs == null || remainingArgs.length == 0)) {
					context.getStratosApplication().printUsage(getName());
					return CliConstants.BAD_ARGS_CODE;
				}

				if (commandLine.hasOption(CliConstants.FULL_OPTION)) {
					if (logger.isTraceEnabled()) {
						logger.trace("Full option is passed");
					}
					full = true;
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Listing subscribed cartridges, Full Option: {}", full);
				}
				CommandLineService.getInstance().listSubscribedCartridges(full);
				return CliConstants.SUCCESSFUL_CODE;
			} catch (ParseException e) {
				if (logger.isErrorEnabled()) {
					logger.error("Error parsing arguments", e);
				}
				System.out.println(e.getMessage());
				return CliConstants.BAD_ARGS_CODE;
			}
		} else {
			context.getStratosApplication().printUsage(getName());
			return CliConstants.BAD_ARGS_CODE;
		}
	}

	@Override
	public Options getOptions() {
		return options;
	}

}

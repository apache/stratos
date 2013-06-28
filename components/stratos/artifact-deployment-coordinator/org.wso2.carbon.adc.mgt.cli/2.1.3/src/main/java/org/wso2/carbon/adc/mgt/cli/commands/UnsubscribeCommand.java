/*
 * Copyright 2013, WSO2, Inc. http://wso2.org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package org.wso2.carbon.adc.mgt.cli.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.adc.mgt.cli.Command;
import org.wso2.carbon.adc.mgt.cli.CommandLineService;
import org.wso2.carbon.adc.mgt.cli.StratosCommandContext;
import org.wso2.carbon.adc.mgt.cli.exception.CommandException;
import org.wso2.carbon.adc.mgt.cli.utils.CliConstants;

public class UnsubscribeCommand implements Command<StratosCommandContext> {

	private static final Logger logger = LoggerFactory.getLogger(UnsubscribeCommand.class);
	
	private final Options options;;

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
					return CliConstants.BAD_ARGS_CODE;
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
					CommandLineService.getInstance().unsubscribe(alias);
				}
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

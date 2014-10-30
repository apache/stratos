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

import org.apache.commons.cli.*;
import org.apache.stratos.cli.Command;
import org.apache.stratos.cli.RestCommandLineService;
import org.apache.stratos.cli.StratosCommandContext;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListMemberCommand implements Command<StratosCommandContext> {
    private static final Logger logger = LoggerFactory.getLogger(ListMemberCommand.class);


    private final Options options;

	public ListMemberCommand() {
		options = constructOptions();
	}

	/**
	 * Construct Options.
	 *
	 * @return Options expected from command-line.
	 */
	private Options constructOptions() {
		final Options options = new Options();

        Option type = new Option(CliConstants.CARTRIDGE_TYPE_OPTION, CliConstants.CARTRIDGE_TYPE_LONG_OPTION,
                true, "Cartridge Type");
        type.setArgName("cartridge-type");
        options.addOption(type);

        Option alias = new Option(CliConstants.ALIAS_OPTION, CliConstants.ALIAS_LONG_OPTION,
                true, "subscription alias");
        alias.setArgName("alias");
        options.addOption(alias);

        return options;
	}
    @Override
    public String getName() {
        return CliConstants.LIST_MEMBERS;
    }

    @Override
    public String getDescription() {
        return "List of members in a cluster";
    }

    @Override
    public String getArgumentSyntax() {
        return null;
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public int execute(StratosCommandContext context, String[] args) throws CommandException {
        if (logger.isDebugEnabled()) {
			logger.debug("Executing {} command...", getName());
		}
		if (args != null && args.length > 0) {
			String type = null;
			String alias = null;

			final CommandLineParser parser = new GnuParser();
			CommandLine commandLine;
			try {
				commandLine = parser.parse(options, args);
				if (logger.isDebugEnabled()) {
					logger.debug("Subscribing to {} cartridge with alias {}", type, alias);
				}

                if (commandLine.hasOption(CliConstants.CARTRIDGE_TYPE_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Autoscaling policy option is passed");
                    }
                    type = commandLine.getOptionValue(CliConstants.CARTRIDGE_TYPE_OPTION);
                }
                if (commandLine.hasOption(CliConstants.ALIAS_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Deployment policy option is passed");
                    }
                    alias = commandLine.getOptionValue(CliConstants.ALIAS_OPTION);
                }

                if (type == null) {
                    System.out.println("Cartridge type is required.");
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }

                if (alias == null) {
                    System.out.println("alis is required...");
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }
                RestCommandLineService.getInstance().listMembersOfCluster(type, alias);

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
}

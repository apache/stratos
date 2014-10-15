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

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.stratos.cli.Command;
import org.apache.stratos.cli.RestCommandLineService;
import org.apache.stratos.cli.StratosCommandContext;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;
import org.apache.stratos.cli.utils.CliUtils;

public class UpdateSubscriptionPropertiesCommand implements Command<StratosCommandContext> {

	private static final Logger logger = LoggerFactory.getLogger(UpdateSubscriptionPropertiesCommand.class);
	
	private final Options options;

	public UpdateSubscriptionPropertiesCommand() {
	    options = constructOptions();
	}
	
	/**
     * Construct Options.
     *
     * @return Options expected from command-line.
     */
    private Options constructOptions() {
        final Options options = new Options();

        Option resourcePath = new Option(CliConstants.RESOURCE_PATH, CliConstants.RESOURCE_PATH_LONG_OPTION, true,
                "Update subscription properties resource path");
        resourcePath.setArgName("resource path");
        options.addOption(resourcePath);
        
        return options;
    }

	@Override
	public String getName() {
		return CliConstants.UPDATE_SUBSCRIPTION_ACTION;
	}

	@Override
	public String getDescription() {
		return "Update a previously made subscription.";
	}

	@Override
	public String getArgumentSyntax() {
		return "[cartridge-subscription-alias]";
	}

	@Override
    public int execute(StratosCommandContext context, String[] args) throws CommandException {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing {} command...", getName());
        }

        final CommandLineParser parser = new GnuParser();
        CommandLine commandLine;
        String[] remainingArgs = null;
        String resourcePath = null, subscriptionJson = null;
        try {

            commandLine = parser.parse(options, args);
            remainingArgs = commandLine.getArgs();

            if (remainingArgs != null && remainingArgs.length == 1) {

                String alias = remainingArgs[0];
                if (logger.isDebugEnabled()) {
                    logger.debug("Getting info {}", alias);
                }

                if (commandLine.hasOption(CliConstants.RESOURCE_PATH)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("{} option is passed", CliConstants.RESOURCE_PATH);
                    }
                    try {
                        resourcePath = commandLine.getOptionValue(CliConstants.RESOURCE_PATH);
                        subscriptionJson = CliUtils.readResource(resourcePath);
                    } catch (IOException e) {
                        System.out.println("Invalid resource path");
                        return CliConstants.COMMAND_FAILED;
                    }
                }

                RestCommandLineService.getInstance().updateSubscritptionProperties(alias, subscriptionJson);
                return CliConstants.COMMAND_SUCCESSFULL;
            } else {
                context.getStratosApplication().printUsage(getName());
                return CliConstants.COMMAND_FAILED;
            }
        } catch (ParseException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error parsing arguments", e);
            }
            System.out.println(e.getMessage());
            return CliConstants.COMMAND_FAILED;
        }

    }

	@Override
	public Options getOptions() {
		return options;
	}

}

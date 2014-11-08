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
import org.apache.commons.lang3.StringUtils;
import org.apache.stratos.cli.RestCommandLineService;
import org.apache.stratos.cli.utils.CliUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.stratos.cli.Command;
import org.apache.stratos.cli.StratosCommandContext;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;

import java.io.IOException;

@Deprecated
public class SubscribeCartridgeCommand implements Command<StratosCommandContext> {

    private static final Logger logger = LoggerFactory.getLogger(SubscribeCartridgeCommand.class);

    private final Options options;

    public SubscribeCartridgeCommand() {
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
                "Cartridge subscription resource path");
        resourcePath.setArgName("resource path");
        options.addOption(resourcePath);

        Option autoscaling = new Option(CliConstants.AUTOSCALING_POLICY_OPTION, CliConstants.AUTOSCALING_POLICY_LONG_OPTION,
                true, "Auto-scaling policy");
        autoscaling.setArgName("auto-scaling-policy");
        options.addOption(autoscaling);

        Option deployment = new Option(CliConstants.DEPLOYMENT_POLICY_OPTION, CliConstants.DEPLOYMENT_POLICY_LONG_OPTION,
                true, "Deployment-policy");
        deployment.setArgName("deployment-policy");
        options.addOption(deployment);

        Option removeOnTermination = new Option(CliConstants.REMOVE_ON_TERMINATION_OPTION, CliConstants.REMOVE_ON_TERMINATION_LONG_OPTION,
                true, "Remove-on-termination");
        removeOnTermination.setArgName("remove-on-termination");
        options.addOption(removeOnTermination);

        Option size = new Option(CliConstants.VOLUME_SIZE_OPTION, CliConstants.VOLUME_SIZE_LONG_OPTION, true, "Volume-size");
        size.setArgName("volume-size");
        options.addOption(size);

        Option volumeId = new Option(CliConstants.VOLUME_ID_OPTION, CliConstants.VOLUME_ID_LONG_OPTION, true, "Volume-id");
        volumeId.setArgName("volume-id");
        options.addOption(volumeId);

        Option persistance = new Option(CliConstants.PERSISTANCE_VOLUME_OPTION, CliConstants.PERSISTANCE_VOLUME_LONG_OPTION,
                true, "Persistance-volume");
        persistance.setArgName("persistance-volume");
        options.addOption(persistance);

        Option urlOption = new Option(CliConstants.REPO_URL_OPTION, CliConstants.REPO_URL_LONG_OPTION, true,
                "GIT repository URL");
        urlOption.setArgName("url");
        options.addOption(urlOption);

        Option usernameOption = new Option(CliConstants.USERNAME_OPTION, CliConstants.USERNAME_LONG_OPTION, true,
                "GIT repository username");
        usernameOption.setArgName("username");
        options.addOption(usernameOption);

        Option passwordOption = new Option(CliConstants.PASSWORD_OPTION, CliConstants.PASSWORD_LONG_OPTION, true,
                "GIT repository password");
        passwordOption.setArgName("password");
        passwordOption.setOptionalArg(true);
        options.addOption(passwordOption);

        Option upstreamCommitsEnabledOption = new Option(CliConstants.ENABLE_COMMITS_OPTION, CliConstants.ENABLE_COMMITS_LONG_OPTION, true,
                "Enable Git commit upstream");
        upstreamCommitsEnabledOption.setArgName("enable-commits");
        upstreamCommitsEnabledOption.setOptionalArg(true);
        options.addOption(upstreamCommitsEnabledOption);

        return options;
    }

    public String getName() {
        return CliConstants.SUBSCRIBE_ACTION;
    }

    public String getDescription() {
        return "Subscribe to a cartridge";
    }

    public String getArgumentSyntax() {
        return "[cartridge-type] [cartridge-subscription-alias]";
    }

    public int execute(StratosCommandContext context, String[] args) throws CommandException {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing {} command...", getName());
        }
        if (args != null && args.length > 0) {
            String[] remainingArgs = null;
            String type = null;
            String alias = null;
            String policy = null;
            String asPolicy = null;
            String depPolicy = null;
            String repoURL = null, username = "", password = "";
            String size = null;
            String volumeID = null;
            String resourcePath = null;
            String subscriptionJson = null;

            boolean removeOnTermination = false;
            boolean privateRepo = false;
            boolean persistanceMapping = false;
            boolean commitsEnabled = false;
            boolean isMultiTenant = false;

            final CommandLineParser parser = new GnuParser();
            CommandLine commandLine;
            try {
                commandLine = parser.parse(options, args);
                remainingArgs = commandLine.getArgs();
                if (remainingArgs != null && remainingArgs.length == 2) {
                    // Get type
                    type = remainingArgs[0];
                    alias = remainingArgs[1];
                } else if (commandLine.hasOption(CliConstants.RESOURCE_PATH)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Resource path option is passed");
                    }
                    try {
                        resourcePath = commandLine.getOptionValue(CliConstants.RESOURCE_PATH);
                        subscriptionJson = CliUtils.readResource(resourcePath);
                    } catch (IOException e) {
                        System.out.println("Invalid resource path");
                        return CliConstants.COMMAND_FAILED;
                    }

                    if (resourcePath == null) {
                        System.out.println("usage: " + getName() + " [-p <resource-path>]");
                        return CliConstants.COMMAND_FAILED;
                    }

                    RestCommandLineService.getInstance().subscribe(subscriptionJson);
                    return CliConstants.COMMAND_FAILED;

                } else {
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }

                // This will check the subscribe cartridge type is multi tenant or single tenant
                isMultiTenant = RestCommandLineService.getInstance().isMultiTenant(type);

                if (logger.isDebugEnabled()) {
                    logger.debug("Subscribing to {} cartridge with alias {}", type, alias);
                }
                if (commandLine.hasOption(CliConstants.AUTOSCALING_POLICY_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Autoscaling policy option is passed");
                    }
                    asPolicy = commandLine.getOptionValue(CliConstants.AUTOSCALING_POLICY_OPTION);
                }
                if (commandLine.hasOption(CliConstants.DEPLOYMENT_POLICY_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Deployment policy option is passed");
                    }
                    depPolicy = commandLine.getOptionValue(CliConstants.DEPLOYMENT_POLICY_OPTION);
                }
                if (commandLine.hasOption(CliConstants.REPO_URL_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("RepoURL option is passed");
                    }
                    repoURL = commandLine.getOptionValue(CliConstants.REPO_URL_OPTION);
                }
                if (commandLine.hasOption(CliConstants.VOLUME_SIZE_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Volume size option is passed");

                    }
                    size = commandLine.getOptionValue(CliConstants.VOLUME_SIZE_OPTION);
                }

                if (commandLine.hasOption(CliConstants.VOLUME_ID_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Volume id option is passed");

                    }
                    volumeID = commandLine.getOptionValue(CliConstants.VOLUME_ID_OPTION);
                }


                if (commandLine.hasOption(CliConstants.REMOVE_ON_TERMINATION_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Remove on termination option is passed");

                    }

                    String optionValue = commandLine.getOptionValue(CliConstants.REMOVE_ON_TERMINATION_OPTION);
                    if (optionValue.equals("true")) {
                        removeOnTermination = true;
                    } else if (optionValue.equals("false")) {
                        removeOnTermination = false;
                    } else {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Invalid remove on termination option value");
                        }
                        System.out.println("Invalid remove on termination option value.");
                        context.getStratosApplication().printUsage(getName());
                        return CliConstants.COMMAND_FAILED;
                    }
                }
                if (commandLine.hasOption(CliConstants.PERSISTANCE_VOLUME_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Persistance volume option is passed");

                    }

                    String optionValue = commandLine.getOptionValue(CliConstants.PERSISTANCE_VOLUME_OPTION);
                    if (optionValue.equals("true")) {
                        persistanceMapping = true;
                    } else if (optionValue.equals("false")) {
                        persistanceMapping = false;
                    } else {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Invalid persistance mapping option value");
                        }
                        System.out.println("Invalid persistance mapping option value.");
                        context.getStratosApplication().printUsage(getName());
                        return CliConstants.COMMAND_FAILED;
                    }

                }
                if (commandLine.hasOption(CliConstants.USERNAME_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Username option is passed");
                    }
                    username = commandLine.getOptionValue(CliConstants.USERNAME_OPTION);
                }
                if (commandLine.hasOption(CliConstants.PASSWORD_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Password option is passed");
                    }
                    password = commandLine.getOptionValue(CliConstants.PASSWORD_OPTION);
                }
                if (commandLine.hasOption(CliConstants.ENABLE_COMMITS_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Upstream git commits are enabled");
                    }
                    commitsEnabled = true;
                }

                if ( ! isMultiTenant && depPolicy == null) {
                    System.out.println("Deployment policy is required.");
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }

                if ( ! isMultiTenant && asPolicy == null) {
                    System.out.println("Autoscaling policy is required.");
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }

                if ((!persistanceMapping) && ((size != null) || removeOnTermination)) {
                    System.out.println("You have to enable persistance mapping in cartridge subscription");
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }

				if (StringUtils.isNotBlank(username) && StringUtils.isBlank(password)) {
					password = context.getApplication().getInput("GIT Repository Password", '*');
				}

                RestCommandLineService.getInstance().subscribe(type, alias, repoURL, privateRepo, username,
                		password, asPolicy, depPolicy, size, removeOnTermination,
                        persistanceMapping, commitsEnabled, volumeID);

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

    public Options getOptions() {
        return options;
    }
}

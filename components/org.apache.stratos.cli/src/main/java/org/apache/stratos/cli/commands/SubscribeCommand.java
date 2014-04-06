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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.stratos.cli.Command;
import org.apache.stratos.cli.StratosCommandContext;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;

public class SubscribeCommand implements Command<StratosCommandContext> {

	private static final Logger logger = LoggerFactory.getLogger(ListSubscribedCartridgesCommand.class);

	private final Options options;

	public SubscribeCommand() {
		options = constructOptions();
	}

	/**
	 * Construct Options.
	 * 
	 * @return Options expected from command-line.
	 */
	private Options constructOptions() {
		final Options options = new Options();
		//Option policyOption = new Option(CliConstants.POLICY_OPTION, CliConstants.POLICY_LONG_OPTION, true,
		//		"Auto-scaling policy.\nPlease use \"" + CliConstants.POLICIES_ACTION
		//				+ "\" command to view the available policies.");
		//policyOption.setArgName("policy name");
		//options.addOption(policyOption);

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

        Option persistance = new Option(CliConstants.PERSISTANCE_VOLUME_OPTION, CliConstants.PERSISTANCE_VOLUME_LONG_OPTION,
                true, "Persistance-volume");
        persistance.setArgName("persistance-volume");
        options.addOption(persistance);

		Option urlOption = new Option(CliConstants.REPO_URL_OPTION, CliConstants.REPO_URL_LONG_OPTION, true,
				"GIT repository URL");
		urlOption.setArgName("url");
		options.addOption(urlOption);

		//options.addOption(CliConstants.PRIVATE_REPO_OPTION, CliConstants.PRIVATE_REPO_LONG_OPTION, false,
		//		"Private repository");

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
		return "[Cartridge type] [Cartridge alias]";
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

            boolean removeOnTermination = false;
			boolean privateRepo = false;
            boolean persistanceMapping = false;
            boolean commitsEnabled = false;

			final CommandLineParser parser = new GnuParser();
			CommandLine commandLine;
			try {
				commandLine = parser.parse(options, args);
				remainingArgs = commandLine.getArgs();
				if (remainingArgs != null && remainingArgs.length == 2) {
					// Get type
					type = remainingArgs[0];
					alias = remainingArgs[1];
				} else {
					context.getStratosApplication().printUsage(getName());
					return CliConstants.BAD_ARGS_CODE;
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Subscribing to {} cartridge with alias {}", type, alias);
				}

				//if (commandLine.hasOption(CliConstants.POLICY_OPTION)) {
				//	if (logger.isTraceEnabled()) {
				//		logger.trace("Policy option is passed");
				//	}
				//	policy = commandLine.getOptionValue(CliConstants.POLICY_OPTION);
				//}
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
				//if (commandLine.hasOption(CliConstants.PRIVATE_REPO_OPTION)) {
				//	if (logger.isTraceEnabled()) {
				//		logger.trace("privateRepo option is passed");
				//	}
				//	privateRepo = true;
				//}
                if (commandLine.hasOption(CliConstants.VOLUME_SIZE_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Volume size option is passed");

                    }
                    size = commandLine.getOptionValue(CliConstants.VOLUME_SIZE_OPTION);
                }
                if (commandLine.hasOption(CliConstants.REMOVE_ON_TERMINATION_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Remove on termination option is passed");

                    }
                    removeOnTermination = true;
                }
                if (commandLine.hasOption(CliConstants.PERSISTANCE_VOLUME_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Persistance volume option is passed");

                    }
                    persistanceMapping = true;
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

                if (depPolicy == null) {
                    System.out.println("Deployment policy is required.");
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.BAD_ARGS_CODE;
                }

                if (asPolicy == null) {
                    System.out.println("Autoscaling policy is required.");
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.BAD_ARGS_CODE;
                }

                if ( (! persistanceMapping) && ((size != null) || removeOnTermination)) {
                    System.out.println("You have to enable persistance mapping in cartridge subscription");
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.BAD_ARGS_CODE;
                }
				
				if (StringUtils.isNotBlank(username) && StringUtils.isBlank(password)) {
					password = context.getApplication().getInput("GIT Repository Password", '*');
				}

                RestCommandLineService.getInstance().subscribe(type, alias, repoURL, privateRepo, username,
                		password, asPolicy, depPolicy, size, removeOnTermination,
                        persistanceMapping, commitsEnabled);

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

	public Options getOptions() {
		return options;
	}

}

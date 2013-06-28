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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.adc.mgt.cli.Command;
import org.wso2.carbon.adc.mgt.cli.CommandLineService;
import org.wso2.carbon.adc.mgt.cli.StratosCommandContext;
import org.wso2.carbon.adc.mgt.cli.exception.CommandException;
import org.wso2.carbon.adc.mgt.cli.utils.CliConstants;

public class SubscribeCommand implements Command<StratosCommandContext> {

	private static final Logger logger = LoggerFactory.getLogger(ListCommand.class);

	private final Options options;;

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
		Option policyOption = new Option(CliConstants.POLICY_OPTION, CliConstants.POLICY_LONG_OPTION, true,
				"Auto-scaling policy.\nPlease use \"" + CliConstants.POLICIES_ACTION
						+ "\" command to view the available policies.");
		policyOption.setArgName("policy name");
		options.addOption(policyOption);

		Option connectOption = new Option(CliConstants.CONNECT_OPTION, CliConstants.CONNECT_LONG_OPTION, true,
				"Data cartridge type");
		connectOption.setArgName("data cartridge type");
		options.addOption(connectOption);

		Option aliasOption = new Option(CliConstants.DATA_ALIAS_OPTION, CliConstants.DATA_ALIAS_LONG_OPTION, true,
				"Data cartridge alias");
		aliasOption.setArgName("alias");
		options.addOption(aliasOption);

		Option urlOption = new Option(CliConstants.REPO_URL_OPTION, CliConstants.REPO_URL_LONG_OPTION, true,
				"GIT repository URL");
		urlOption.setArgName("url");
		options.addOption(urlOption);

		options.addOption(CliConstants.PRIVATE_REPO_OPTION, CliConstants.PRIVATE_REPO_LONG_OPTION, false,
				"Private repository");

		Option usernameOption = new Option(CliConstants.USERNAME_OPTION, CliConstants.USERNAME_LONG_OPTION, true,
				"GIT repository username");
		usernameOption.setArgName("username");
		options.addOption(usernameOption);

		Option passwordOption = new Option(CliConstants.PASSWORD_OPTION, CliConstants.PASSWORD_LONG_OPTION, true,
				"GIT repository password");
		passwordOption.setArgName("password");
		passwordOption.setOptionalArg(true);
		options.addOption(passwordOption);
		return options;
	}

	@Override
	public String getName() {
		return CliConstants.SUBSCRIBE_ACTION;
	}

	@Override
	public String getDescription() {
		return "Subscribe to a cartridge";
	}

	@Override
	public String getArgumentSyntax() {
		return "[Cartridge type] [Cartridge alias]";
	}

	@Override
	public int execute(StratosCommandContext context, String[] args) throws CommandException {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing {} command...", getName());
		}
		if (args != null && args.length > 0) {
			String[] remainingArgs = null;
			String type = null;
			String alias = null;
			String policy = null;
			String repoURL = null, dataCartridgeType = null, dataCartridgeAlias = null, username = "", password = "";
			boolean privateRepo = false;
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

				if (commandLine.hasOption(CliConstants.POLICY_OPTION)) {
					if (logger.isTraceEnabled()) {
						logger.trace("Policy option is passed");
					}
					policy = commandLine.getOptionValue(CliConstants.POLICY_OPTION);
				}
				if (commandLine.hasOption(CliConstants.REPO_URL_OPTION)) {
					if (logger.isTraceEnabled()) {
						logger.trace("RepoURL option is passed");
					}
					repoURL = commandLine.getOptionValue(CliConstants.REPO_URL_OPTION);
				}
				if (commandLine.hasOption(CliConstants.PRIVATE_REPO_OPTION)) {
					if (logger.isTraceEnabled()) {
						logger.trace("privateRepo option is passed");
					}
					privateRepo = true;
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
				if (commandLine.hasOption(CliConstants.CONNECT_OPTION)) {
					if (logger.isTraceEnabled()) {
						logger.trace("Connect option is passed");
					}
					dataCartridgeType = commandLine.getOptionValue(CliConstants.CONNECT_OPTION);
				}
				if (commandLine.hasOption(CliConstants.DATA_ALIAS_OPTION)) {
					if (logger.isTraceEnabled()) {
						logger.trace("Data alias option is passed");
					}
					dataCartridgeAlias = commandLine.getOptionValue(CliConstants.DATA_ALIAS_OPTION);
				}
				
				if (StringUtils.isNotBlank(username) && StringUtils.isBlank(password)) {
					password = context.getApplication().getInput("GIT Repository Password", '*');
				}

				if (StringUtils.isNotBlank(dataCartridgeType) && !StringUtils.isNotBlank(dataCartridgeAlias)) {
					System.out.println("Data cartridge alias is required.");
					context.getStratosApplication().printUsage(getName());
					return CliConstants.BAD_ARGS_CODE;
				}
				CommandLineService.getInstance().subscribe(type, alias, policy, repoURL, privateRepo, username,
						password, dataCartridgeType, dataCartridgeAlias);
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

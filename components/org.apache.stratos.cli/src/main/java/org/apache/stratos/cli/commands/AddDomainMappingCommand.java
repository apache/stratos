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

import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.stratos.cli.Command;
import org.apache.stratos.cli.CommandLineService;
import org.apache.stratos.cli.StratosCommandContext;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;

public class AddDomainMappingCommand implements Command<StratosCommandContext> {

	private static final Logger logger = LoggerFactory.getLogger(AddDomainMappingCommand.class);

	public AddDomainMappingCommand() {
	}

	@Override
	public String getName() {
		return CliConstants.ADD_DOMAIN_MAPPING_ACTION;
	}

	@Override
	public String getDescription() {
		return "Map domain for the subscribed cartridge";
	}

	@Override
	public String getArgumentSyntax() {
		return "[Cartridge alias] [Domain]";
	}

	@Override
	public int execute(StratosCommandContext context, String[] args) throws CommandException {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing {} command...", getName());
		}
		if (args != null && args.length == 2) {
			String alias = args[0];
			String domain = args[1];
			if (logger.isDebugEnabled()) {
				logger.debug("Adding domain mapping {} for alias {}", domain, alias);
			}

			String domainToDisplay = null;

			domainToDisplay = CommandLineService.getInstance().addDomainMapping(domain, alias);

			if (StringUtils.isBlank(domainToDisplay)) {
				System.out.println("Error adding domain mapping.");
				return CliConstants.COMMAND_FAILED;
			} else {
				System.out.format("Your own domain is added. Please CNAME it to systems domain %s.%n", domainToDisplay);
				return CliConstants.COMMAND_SUCCESSFULL;
			}
		} else {
			context.getStratosApplication().printUsage(getName());
			return CliConstants.COMMAND_FAILED;
		}
	}

	@Override
	public Options getOptions() {
		return null;
	}

}

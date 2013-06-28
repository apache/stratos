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

import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.adc.mgt.cli.Command;
import org.wso2.carbon.adc.mgt.cli.CommandLineService;
import org.wso2.carbon.adc.mgt.cli.StratosCommandContext;
import org.wso2.carbon.adc.mgt.cli.exception.CommandException;
import org.wso2.carbon.adc.mgt.cli.utils.CliConstants;

public class CartridgesCommand implements Command<StratosCommandContext> {

	private static final Logger logger = LoggerFactory.getLogger(CartridgesCommand.class);

	public CartridgesCommand() {
	}

	@Override
	public String getName() {
		return CliConstants.CARTRIDGES_ACTION;
	}

	@Override
	public String getDescription() {
		return "List available cartridges";
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
			CommandLineService.getInstance().listAvailableCartridges();
			return CliConstants.SUCCESSFUL_CODE;
		} else {
			context.getStratosApplication().printUsage(getName());
			return CliConstants.BAD_ARGS_CODE;
		}
	}

	@Override
	public Options getOptions() {
		return null;
	}

}

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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.apache.stratos.cli.Command;
import org.apache.stratos.cli.RestCommandLineService;
import org.apache.stratos.cli.StratosCommandContext;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscribedCartridgeInfoCommand implements Command<StratosCommandContext>{
	
	private static final Logger logger = LoggerFactory.getLogger(SubscribedCartridgeInfoCommand.class);
	
	private final Options options;
	
	public SubscribedCartridgeInfoCommand() {
		options = constructOptions();
	}
	
	/**
	 * Construct Options.
	 * 
	 * @return Options expected from command-line.
	 */
	private Options constructOptions() {

		final Options options = new Options();
		return options;
	}
	
	@Override
	public String getName() {
		return CliConstants.LIST_INFO_ACTION;
	}

	@Override
	public String getDescription() {
		return "List subscribed cartridges with details";
	}

	@Override
	public String getArgumentSyntax() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Options getOptions() {
		return options;
	}

	@Override
	public int execute(StratosCommandContext context, String[] args, Option[] already_parsed_opts) throws CommandException {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing {} command...", getName());
		}
		
		if (args != null && args.length > 0) {
			String alias = args[0];

            if(StringUtils.isBlank(alias)){
                System.out.println("Please specify a non blank alias.");
                return CliConstants.BAD_ARGS_CODE;
            }
            else{
                RestCommandLineService.getInstance().listSubscribedCartridgeInfo(alias);
                return CliConstants.SUCCESSFUL_CODE;
            }
		}else {
			context.getStratosApplication().printUsage(getName());
			return CliConstants.BAD_ARGS_CODE;
		}
	}

}

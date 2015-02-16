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
import org.apache.stratos.cli.Command;
import org.apache.stratos.cli.RestCommandLineService;
import org.apache.stratos.cli.StratosCommandContext;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DescribeApplicationSignupCommand implements Command<StratosCommandContext> {

	private static final Logger logger = LoggerFactory.getLogger(DescribeApplicationSignupCommand.class);

	public DescribeApplicationSignupCommand() {
	}

	public String getName() {
		return "describe-application-signup";
	}

	public String getDescription() {
		return "Describe application sign up";
	}

	public String getArgumentSyntax() {
		return "[application-id]";
	}

	public int execute(StratosCommandContext context, String[] args) throws CommandException {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing command: ", getName());
		}
		if ((args == null) || (args.length == 0)) {
            context.getStratosApplication().printUsage(getName());
            return CliConstants.COMMAND_FAILED;
		} else {
            String applicationId = args[0];
            RestCommandLineService.getInstance().describeApplicationSignup(applicationId);
            return CliConstants.COMMAND_SUCCESSFULL;
		}
	}

	public Options getOptions() {
		return null;
	}
}

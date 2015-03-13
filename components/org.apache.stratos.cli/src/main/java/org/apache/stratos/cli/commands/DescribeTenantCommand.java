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
import org.apache.stratos.cli.Command;
import org.apache.stratos.cli.RestCommandLineService;
import org.apache.stratos.cli.StratosCommandContext;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;
import org.slf4j.LoggerFactory;

public class DescribeTenantCommand implements Command<StratosCommandContext> {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DescribeAutoScalingPolicyCommand.class);
    /**
     * @return The name of the command
     */
    @Override
    public String getName() {
        return CliConstants.DESCRIBE_TENANT;
    }

    /**
     * Information about the command
     *
     * @return The description of the command
     */
    @Override
    public String getDescription() {
        return "Describing the tenant";
    }

    /**
     * This should return the syntax required for the command.
     * <p/>
     * Used to display help.
     *
     * @return The syntax for this command
     */
    @Override
    public String getArgumentSyntax() {
        return "[Domain-Name]";
    }

    /**
     * The options accepted by the command
     *
     * @return The Options for the commands
     */
    @Override
    public Options getOptions() {
        return null;
    }

    /**
     * Executing the commands. Returns a code
     *
     * @param context The context assoicated with the Command Line Application
     * @param args    The arguments for the command
     * @return The status code
     * @throws org.apache.stratos.cli.exception.CommandException if any errors occur when executing the command
     */
    @Override
    public int execute(StratosCommandContext context, String[] args, Option[] alreadyParsedOpts) throws CommandException {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing {} command...", getName());
        }
        if (args != null && args.length == 1) {
            String domainName = args[0];
            if (logger.isDebugEnabled()) {
                logger.debug("Getting tenant info {}", domainName);
            }
            RestCommandLineService.getInstance().describeTenant(domainName);
            return CliConstants.COMMAND_SUCCESSFULL;
        } else {
            context.getStratosApplication().printUsage(getName());
            return CliConstants.COMMAND_FAILED;
        }
    }
}
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

import static org.apache.stratos.cli.utils.CliUtils.mergeOptionArrays;

/**
 * Deploy application command.
 */
public class DeployApplicationCommand implements Command<StratosCommandContext> {

    private static final Logger log = LoggerFactory.getLogger(DeployApplicationCommand.class);

    private Options options;

    public DeployApplicationCommand(){
        options = constructOptions();
    }

    private Options constructOptions() {
        final Options options = new Options();

        Option applicationOption = new Option(CliConstants.APPLICATION_ID_OPTION, CliConstants.APPLICATION_ID_LONG_OPTION, true,
                "Application option");
        applicationOption.setArgName("applicationId");
        options.addOption(applicationOption);

        Option applicationPolicyOption = new Option(CliConstants.APPLICATION_POLICY_ID_OPTION, CliConstants.APPLICATION_POLICY_ID_LONG_OPTION, true,
                "Application policy");
        applicationPolicyOption.setArgName("applicationPolicyId");
        options.addOption(applicationPolicyOption);

        return options;
    }

    @Override
    public String getName() {
        return "deploy-application";
    }

    @Override
    public String getDescription() {
        return "deploy application";
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
    public int execute(StratosCommandContext context, String[] args, Option[] alreadyParsedOpts) throws CommandException {
        if (log.isDebugEnabled()) {
            log.debug("Executing {} command...", getName());
        }

        if (args != null && args.length > 0) {
            String applicationId= null;
            String applicationPolicyId= null;

            final CommandLineParser parser = new GnuParser();
            CommandLine commandLine;

            try {
                commandLine = parser.parse(options, args);

                //merge newly discovered options with previously discovered ones.
                Options opts = mergeOptionArrays(alreadyParsedOpts, commandLine.getOptions());

                if (log.isDebugEnabled()) {
                    log.debug("Deploy application");
                }

                if (opts.hasOption(CliConstants.APPLICATION_ID_OPTION)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Application Id option is passed");
                    }
                    applicationId = opts.getOption(CliConstants.APPLICATION_ID_OPTION).getValue();
                }
                if (opts.hasOption(CliConstants.APPLICATION_POLICY_ID_OPTION)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Application policy Id option is passed");
                    }
                    applicationPolicyId = opts.getOption(CliConstants.APPLICATION_POLICY_ID_OPTION).getValue();
                }

                if (applicationId == null || applicationPolicyId == null) {
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }

                RestCommandLineService.getInstance().deployApplication(applicationId,applicationPolicyId);
                return CliConstants.COMMAND_SUCCESSFULL;

            } catch (ParseException e) {
                if (log.isErrorEnabled()) {
                    log.error("Error parsing arguments", e);
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

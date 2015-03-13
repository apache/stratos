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
import org.apache.stratos.cli.utils.CliUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.apache.stratos.cli.utils.CliUtils.mergeOptionArrays;

public class UpdateAutoscalingPolicyCommand implements Command<StratosCommandContext> {

    private static final Logger log = LoggerFactory.getLogger(UpdateAutoscalingPolicyCommand.class);

    private final Options options;

    public UpdateAutoscalingPolicyCommand(){
        options = constructOptions();
    }

    public Options getOptions() {
        return options;
    }

    private Options constructOptions() {
        final Options options = new Options();

        Option resourcePath = new Option(CliConstants.RESOURCE_PATH, CliConstants.RESOURCE_PATH_LONG_OPTION, true,
                "Autoscaling policy resource path");
        resourcePath.setArgName("resource path");
        options.addOption(resourcePath);

        return options;
    }

    public String getName() {
        return "update-autoscaling-policy";
    }

    public String getDescription() {
        return "Update autoscaling policy";
    }

    public String getArgumentSyntax() {
        return null;
    }

    public int execute(StratosCommandContext context, String[] args, Option[] alreadyParsedOpts) throws CommandException {
        if (log.isDebugEnabled()) {
            log.debug("Executing {} command...", getName());
        }

        if (args != null && args.length > 0) {
            String resourcePath = null;
            String resourceFileContent = null;

            final CommandLineParser parser = new GnuParser();
            CommandLine commandLine;

            try {
                commandLine = parser.parse(options, args);
                //merge newly discovered options with previously discovered ones.
                Options opts = mergeOptionArrays(alreadyParsedOpts, commandLine.getOptions());

                if (log.isDebugEnabled()) {
                    log.debug("Updating autoscaling policy");
                }

                if (opts.hasOption(CliConstants.RESOURCE_PATH)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Resource path option is passed");
                    }
                    resourcePath = opts.getOption(CliConstants.RESOURCE_PATH).getValue();
                    resourceFileContent = CliUtils.readResource(resourcePath);
                }

                if (resourcePath == null) {
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }

                RestCommandLineService.getInstance().updateAutoscalingPolicy(resourceFileContent);
                return CliConstants.COMMAND_SUCCESSFULL;

            } catch (ParseException e) {
                if (log.isErrorEnabled()) {
                    log.error("Error parsing arguments", e);
                }
                System.out.println(e.getMessage());
                return CliConstants.COMMAND_FAILED;
            } catch (IOException ignore) {
                System.out.println("Invalid resource path");
                return CliConstants.COMMAND_FAILED;
            }
        } else {
            context.getStratosApplication().printUsage(getName());
            return CliConstants.COMMAND_FAILED;
        }
    }
}

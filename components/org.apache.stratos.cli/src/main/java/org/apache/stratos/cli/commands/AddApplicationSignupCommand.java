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

/**
 * Add application signup command.
 */
public class AddApplicationSignupCommand implements Command<StratosCommandContext> {

    private static final Logger log = LoggerFactory.getLogger(AddApplicationSignupCommand.class);

    private Options options;

    public AddApplicationSignupCommand() {
        options = new Options();
        Option option = new Option(CliConstants.RESOURCE_PATH, CliConstants.RESOURCE_PATH_LONG_OPTION, true,
                "Application sign up resource path");
        option.setArgName("resource path");
        options.addOption(option);
    }

    @Override
    public String getName() {
    	return "add-application-signup";
    }

    @Override
    public String getDescription() {
        return "Add application sign up";
    }

    @Override
    public String getArgumentSyntax() {
        return "[application-id]";
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public int execute(StratosCommandContext context, String[] args, Option[] alreadyParsedOpts) throws CommandException {
        if (log.isDebugEnabled()) {
            log.debug("Executing command: ", getName());
        }

        if ((args == null) || (args.length <= 0)) {
            context.getStratosApplication().printUsage(getName());
            return CliConstants.COMMAND_FAILED;
        }
        String applicationId = args[0];

        try {
            CommandLineParser parser = new GnuParser();
            CommandLine commandLine = parser.parse(options, args);
            //merge newly discovered options with previously discovered ones.
            Options opts = mergeOptionArrays(alreadyParsedOpts, commandLine.getOptions());
            if (opts.hasOption(CliConstants.RESOURCE_PATH)) {
                String resourcePath = opts.getOption(CliConstants.RESOURCE_PATH).getValue();
                if (resourcePath == null) {
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }
                String resourceFileContent = CliUtils.readResource(resourcePath);
                RestCommandLineService.getInstance().addApplicationSignup(resourceFileContent, applicationId);
                return CliConstants.COMMAND_SUCCESSFULL;
            } else {
                context.getStratosApplication().printUsage(getName());
                return CliConstants.COMMAND_FAILED;
            }
        } catch (ParseException e) {
            log.error("Error parsing arguments", e);
            System.out.println(e.getMessage());
            return CliConstants.COMMAND_FAILED;
        } catch (IOException e) {
            System.out.println("Invalid resource path");
            return CliConstants.COMMAND_FAILED;
        } catch (Exception e) {
            String message = "Unknown error occurred: " + e.getMessage();
            System.out.println(message);
            log.error(message, e);
            return CliConstants.COMMAND_FAILED;
        }
    }
}

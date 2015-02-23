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

public class UpdateUserCommand implements Command<StratosCommandContext> {
    private static final Logger logger = LoggerFactory.getLogger(UpdateUserCommand.class);

    private final Options options;

    public UpdateUserCommand(){
        options = constructOptions();
    }

    private Options constructOptions() {
        final Options options = new Options();

        Option usernameOption = new Option(CliConstants.USERNAME_OPTION, CliConstants.USERNAME_LONG_OPTION, true,
                "User name");
        usernameOption.setArgName("userName");
        options.addOption(usernameOption);

        Option passwordOption = new Option(CliConstants.PASSWORD_OPTION, CliConstants.PASSWORD_LONG_OPTION, true,
                "User credential");
        passwordOption.setArgName("credential");
        options.addOption(passwordOption);

        Option roleOption = new Option(CliConstants.ROLE_NAME_OPTION, CliConstants.ROLE_NAME_LONG_OPTION, true,
                "User Role");
        roleOption.setArgName("role");
        options.addOption(roleOption);

        Option fistnameOption = new Option(CliConstants.FIRST_NAME_OPTION, CliConstants.FIRST_NAME_LONG_OPTION, true,
                "User first name");
        fistnameOption.setArgName("firstName");
        options.addOption(fistnameOption);

        Option lastnameOption = new Option(CliConstants.LAST_NAME_OPTION, CliConstants.LAST_NAME_LONG_OPTION, true,
                "User last name");
        lastnameOption.setArgName("lastName");
        options.addOption(lastnameOption);

        Option emailOption = new Option(CliConstants.EMAIL_OPTION, CliConstants.EMAIL_LONG_OPTION, true,
                "User email");
        emailOption.setArgName("email");
        options.addOption(emailOption);

        Option profileNameOption = new Option(CliConstants.PROFILE_NAME_OPTION, CliConstants.PROFILE_NAME_LONG_OPTION, true,
                "Profile name");
        profileNameOption.setArgName("profileName");
        options.addOption(profileNameOption);

        return options;
    }

    public String getName() {
        return "update-user";
    }

    public String getDescription() {
        return "Update an existing user";
    }

    public String getArgumentSyntax() {
        return null;
    }

    public int execute(StratosCommandContext context, String[] args) throws CommandException {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing {} command...", getName());
        }

        if (args != null && args.length > 0) {
            String userName= null;
            String credential= null;
            String role= null;
            String firstName= null;
            String lastName= null;
            String email= null;
            String profileName= null;

            final CommandLineParser parser = new GnuParser();
            CommandLine commandLine;

            try {
                commandLine = parser.parse(options, args);

                if (logger.isDebugEnabled()) {
                    logger.debug("Update user");
                }

                if (commandLine.hasOption(CliConstants.USERNAME_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Username option is passed");
                    }
                    userName = commandLine.getOptionValue(CliConstants.USERNAME_OPTION);
                }
                if (commandLine.hasOption(CliConstants.PASSWORD_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Credential option is passed");
                    }
                    credential = commandLine.getOptionValue(CliConstants.PASSWORD_OPTION);
                }
                if (commandLine.hasOption(CliConstants.ROLE_NAME_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Role option is passed");
                    }
                    role = commandLine.getOptionValue(CliConstants.ROLE_NAME_OPTION);
                }
                if (commandLine.hasOption(CliConstants.FIRST_NAME_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("First name option is passed");
                    }
                    firstName = commandLine.getOptionValue(CliConstants.FIRST_NAME_OPTION);
                }
                if (commandLine.hasOption(CliConstants.LAST_NAME_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Last name option is passed");
                    }
                    lastName = commandLine.getOptionValue(CliConstants.LAST_NAME_OPTION);
                }
                if (commandLine.hasOption(CliConstants.EMAIL_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Email option is passed");
                    }
                    email = commandLine.getOptionValue(CliConstants.EMAIL_OPTION);
                }
                if (commandLine.hasOption(CliConstants.PROFILE_NAME_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Profile name option is passed");
                    }
                    profileName = commandLine.getOptionValue(CliConstants.PROFILE_NAME_OPTION);
                }


                if (userName == null || credential == null || role == null || firstName == null || lastName == null || email == null) {
                    System.out.println("usage: " + getName() + " [-u <user name>] [-p <credential>] [-r <role>] [-f <first name>] [-l <last name>] [-e <email>] [-pr <profile name>]");
                    return CliConstants.COMMAND_FAILED;
                }

                RestCommandLineService.getInstance().updateUser(userName, credential, role, firstName, lastName, email, profileName);
                return CliConstants.COMMAND_SUCCESSFULL;

            } catch (ParseException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Error parsing arguments", e);
                }
                System.out.println(e.getMessage());
                return CliConstants.COMMAND_FAILED;
            }

        } else {
            context.getStratosApplication().printUsage(getName());
            return CliConstants.COMMAND_FAILED;
        }
    }

    public Options getOptions() {
        return options;
    }
}

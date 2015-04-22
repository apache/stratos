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
 * Update an existing user
 */
public class UpdateUserCommand implements Command<StratosCommandContext> {
    private static final Logger log = LoggerFactory.getLogger(UpdateUserCommand.class);

    private final Options options;

    public UpdateUserCommand(){
        options = constructOptions();
    }

    private Options constructOptions() {
        final Options options = new Options();

        Option userNameOption = new Option(CliConstants.USERNAME_OPTION, CliConstants.USERNAME_LONG_OPTION, true,
                "Username");
        userNameOption.setArgName("username");
        options.addOption(userNameOption);

        Option passwordOption = new Option(CliConstants.PASSWORD_OPTION, CliConstants.PASSWORD_LONG_OPTION, true,
                "User credential");
        passwordOption.setArgName("credential");
        options.addOption(passwordOption);

        Option roleOption = new Option(CliConstants.ROLE_NAME_OPTION, CliConstants.ROLE_NAME_LONG_OPTION, true,
                "User role");
        roleOption.setArgName("role");
        options.addOption(roleOption);

        Option firstNameOption = new Option(CliConstants.FIRST_NAME_OPTION, CliConstants.FIRST_NAME_LONG_OPTION, true,
                "User first name");
        firstNameOption.setArgName("firstName");
        options.addOption(firstNameOption);

        Option lastNameOption = new Option(CliConstants.LAST_NAME_OPTION, CliConstants.LAST_NAME_LONG_OPTION, true,
                "User last name");
        lastNameOption.setArgName("lastName");
        options.addOption(lastNameOption);

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

    public int execute(StratosCommandContext context, String[] args, Option[] alreadyParsedOpts) throws CommandException {
        if (log.isDebugEnabled()) {
            log.debug("Executing {} command...", getName());
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
                //merge newly discovered options with previously discovered ones.
                Options opts = mergeOptionArrays(alreadyParsedOpts, commandLine.getOptions());

                if (log.isDebugEnabled()) {
                    log.debug("Update user");
                }

                if (opts.hasOption(CliConstants.USERNAME_OPTION)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Username option is passed");
                    }
                    userName = opts.getOption(CliConstants.USERNAME_OPTION).getValue();
                }
                if (opts.hasOption(CliConstants.PASSWORD_OPTION)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Credential option is passed");
                    }
                    credential = opts.getOption(CliConstants.PASSWORD_OPTION).getValue();
                }
                if (opts.hasOption(CliConstants.ROLE_NAME_OPTION)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Role option is passed");
                    }
                    role = opts.getOption(CliConstants.ROLE_NAME_OPTION).getValue();
                }
                if (opts.hasOption(CliConstants.FIRST_NAME_OPTION)) {
                    if (log.isTraceEnabled()) {
                        log.trace("First name option is passed");
                    }
                    firstName = opts.getOption(CliConstants.FIRST_NAME_OPTION).getValue();
                }
                if (opts.hasOption(CliConstants.LAST_NAME_OPTION)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Last name option is passed");
                    }
                    lastName = opts.getOption(CliConstants.LAST_NAME_OPTION).getValue();
                }
                if (opts.hasOption(CliConstants.EMAIL_OPTION)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Email option is passed");
                    }
                    email = opts.getOption(CliConstants.EMAIL_OPTION).getValue();
                }
                if (opts.hasOption(CliConstants.PROFILE_NAME_OPTION)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Profile name option is passed");
                    }
                    profileName = opts.getOption(CliConstants.PROFILE_NAME_OPTION).getValue();
                }


                if (userName == null || credential == null || role == null || firstName == null || lastName == null || email == null) {
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }

                RestCommandLineService.getInstance().updateUser(userName, credential, role, firstName, lastName, email, profileName);
                return CliConstants.COMMAND_SUCCESSFULL;

            } catch (ParseException e) {
                log.error("Error parsing arguments", e);
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

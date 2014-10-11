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
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;

public class AddTenantCommand implements Command<StratosCommandContext> {

    private static final Logger logger = LoggerFactory.getLogger(AddTenantCommand.class);

    private final Options options;

    public AddTenantCommand(){
        options = constructOptions();
    }

    private Options constructOptions() {
        final Options options = new Options();

        Option usernameOption = new Option(CliConstants.USERNAME_OPTION, CliConstants.USERNAME_LONG_OPTION, true,
                "Tenant user name");
        usernameOption.setArgName("username");
        options.addOption(usernameOption);

        Option fistnameOption = new Option(CliConstants.FIRST_NAME_OPTION, CliConstants.FIRST_NAME_LONG_OPTION, true,
                "Tenant first name");
        fistnameOption.setArgName("firstname");
        options.addOption(fistnameOption);

        Option lastnameOption = new Option(CliConstants.LAST_NAME_OPTION, CliConstants.LAST_NAME_LONG_OPTION, true,
                "Tenant last name");
        lastnameOption.setArgName("lastname");
        options.addOption(lastnameOption);

        Option passwordOption = new Option(CliConstants.PASSWORD_OPTION, CliConstants.PASSWORD_LONG_OPTION, true,
                "Tenant password");
        passwordOption.setArgName("password");
        options.addOption(passwordOption);

        Option domainOption = new Option(CliConstants.DOMAIN_NAME_OPTION, CliConstants.DOMAIN_NAME_LONG_OPTION, true,
                "Tenant domain");
        domainOption.setArgName("domain");
        options.addOption(domainOption);

        Option emailOption = new Option(CliConstants.EMAIL_OPTION, CliConstants.EMAIL_LONG_OPTION, true,
                "Tenant email");
        emailOption.setArgName("email");
        options.addOption(emailOption);

        return options;
    }

    public String getName() {
        return CliConstants.ADD_TENANT;
    }

    public String getDescription() {
        return "Add new tenant";
    }

    public String getArgumentSyntax() {
        return null;
    }

    public int execute(StratosCommandContext context, String[] args) throws CommandException {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing {} command...", getName());
        }

        if (args != null && args.length > 0) {
            String admin = null;
            String firstName = null;
            String lastaName = null;
            String password = null;
            String domain = null;
            String email = null;

            final CommandLineParser parser = new GnuParser();
            CommandLine commandLine;

            try {
                commandLine = parser.parse(options, args);

                if (logger.isDebugEnabled()) {
                    logger.debug("Add tenant");
                }

                if (commandLine.hasOption(CliConstants.USERNAME_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Username option is passed");
                    }
                    admin = commandLine.getOptionValue(CliConstants.USERNAME_OPTION);
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
                    lastaName = commandLine.getOptionValue(CliConstants.LAST_NAME_OPTION);
                }
                if (commandLine.hasOption(CliConstants.PASSWORD_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Password option is passed");
                    }
                    password = commandLine.getOptionValue(CliConstants.PASSWORD_OPTION);
                }
                if (commandLine.hasOption(CliConstants.DOMAIN_NAME_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Domain name option is passed");
                    }
                    domain = commandLine.getOptionValue(CliConstants.DOMAIN_NAME_OPTION);
                }
                if (commandLine.hasOption(CliConstants.EMAIL_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Email option is passed");
                    }
                    email = commandLine.getOptionValue(CliConstants.EMAIL_OPTION);

                }

                if (admin == null || firstName == null || lastaName == null || password == null || domain == null || email == null) {
                    System.out.println("usage: " + getName() + " [-u <user name>] [-f <first name>] [-l <last name>] [-p <password>] [-d <domain name>] [-e <email>]");
                    return CliConstants.COMMAND_FAILED;
                }

                RestCommandLineService.getInstance().addTenant(admin, firstName, lastaName, password, domain, email);
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

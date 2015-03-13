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

public class UpdateTenantCommand implements Command<StratosCommandContext> {
    private static final Logger log = LoggerFactory.getLogger(UpdateTenantCommand.class);

    private final Options options;

    public UpdateTenantCommand(){
        options = constructOptions();
    }

    private Options constructOptions() {
        final Options options = new Options();

        Option usernameOption = new Option(CliConstants.USERNAME_OPTION, CliConstants.USERNAME_LONG_OPTION, true,
                "Tenant user name");
        usernameOption.setArgName("username");
        options.addOption(usernameOption);

        Option firstnameOption = new Option(CliConstants.FIRST_NAME_OPTION, CliConstants.FIRST_NAME_LONG_OPTION, true,
                "Tenant first name");
        firstnameOption.setArgName("firstname");
        options.addOption(firstnameOption);

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

        Option idOption = new Option(CliConstants.ID_OPTION, CliConstants.ID_LONG_OPTION, true,
                "Tenant id");
        emailOption.setArgName("id");
        options.addOption(idOption);

        return options;
    }

    public String getName() {
        return "update-tenant";
    }

    public String getDescription() {
        return "Update an existing tenant";
    }

    public String getArgumentSyntax() {
        return null;
    }

    public int execute(StratosCommandContext context, String[] args, Option[] alreadyParsedOpts) throws CommandException {
        if (log.isDebugEnabled()) {
            log.debug("Executing {} command...", getName());
        }

        if (args != null && args.length > 0) {
            String admin = null;
            String firstName = null;
            String lastName = null;
            String password = null;
            String domain = null;
            String email = null;
            int id=0;

            final CommandLineParser parser = new GnuParser();
            CommandLine commandLine;

            try {
                commandLine = parser.parse(options, args);
                //merge newly discovered options with previously discovered ones.
                Options opts = mergeOptionArrays(alreadyParsedOpts, commandLine.getOptions());

                if (log.isDebugEnabled()) {
                    log.debug("Update tenant");
                }

                if (opts.hasOption(CliConstants.USERNAME_OPTION)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Username option is passed");
                    }
                    admin = opts.getOption(CliConstants.USERNAME_OPTION).getValue();
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
                if (opts.hasOption(CliConstants.PASSWORD_OPTION)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Password option is passed");
                    }
                    password = opts.getOption(CliConstants.PASSWORD_OPTION).getValue();
                }
                if (opts.hasOption(CliConstants.DOMAIN_NAME_OPTION)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Domain name option is passed");
                    }
                    domain = opts.getOption(CliConstants.DOMAIN_NAME_OPTION).getValue();
                }
                if (opts.hasOption(CliConstants.EMAIL_OPTION)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Email option is passed");
                    }
                    email = opts.getOption(CliConstants.EMAIL_OPTION).getValue();

                }

                if (opts.hasOption(CliConstants.ID_OPTION)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Id option is passed");
                    }
                    id = Integer.parseInt(opts.getOption(CliConstants.ID_OPTION).getValue());

                }

                if (id == 0 ||admin == null || firstName == null || lastName == null || password == null || domain == null || email == null) {
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }

                RestCommandLineService.getInstance().updateTenant(id,admin, firstName, lastName, password, domain, email);
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

    public Options getOptions() {
        return options;
    }
}

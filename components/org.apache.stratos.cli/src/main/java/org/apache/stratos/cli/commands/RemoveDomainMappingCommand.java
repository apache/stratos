/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
 * Remove domain mappings command.
 */
public class RemoveDomainMappingCommand implements Command<StratosCommandContext> {

    private static final Logger log = LoggerFactory.getLogger(RemoveDomainMappingCommand.class);

    private Options options;

    public RemoveDomainMappingCommand() {
        options = new Options();
        Option applicationIdOption = new Option(CliConstants.APPLICATION_ID_OPTION,
                CliConstants.APPLICATION_ID_LONG_OPTION, true,
                "Application Id");
        applicationIdOption.setArgName("application id");
        Option domainNameOption = new Option(CliConstants.DOMAIN_NAME_OPTION, CliConstants.DOMAIN_NAME_LONG_OPTION,
                true, "Domain name");
        domainNameOption.setArgName("domain name");
        options.addOption(applicationIdOption);
        options.addOption(domainNameOption);
    }

    @Override
    public String getName() {
        return "remove-domain-mapping";
    }

    @Override
    public String getDescription() {
        return "Remove domain mapping";
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
            log.debug("Executing command: ", getName());
        }

        if ((args == null) || (args.length <= 0)) {
            context.getStratosApplication().printUsage(getName());
            return CliConstants.COMMAND_FAILED;
        }

        try {
            CommandLineParser parser = new GnuParser();
            CommandLine commandLine = parser.parse(options, args);
            //merge newly discovered options with previously discovered ones.
            Options opts = mergeOptionArrays(alreadyParsedOpts, commandLine.getOptions());

            if ((opts.hasOption(CliConstants.APPLICATION_ID_OPTION)) && (opts.hasOption(CliConstants.DOMAIN_NAME_OPTION))) {

                // get application id arg value
                String applicationId = opts.getOption(CliConstants.APPLICATION_ID_OPTION).getValue();
                if (applicationId == null) {
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }

                // get domain name arg value
                String domainName = opts.getOption(CliConstants.DOMAIN_NAME_OPTION).getValue();
                if (domainName == null) {
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }
                RestCommandLineService.getInstance().removeDomainMappings(applicationId, domainName);
                return CliConstants.COMMAND_SUCCESSFULL;
            } else {
                context.getStratosApplication().printUsage(getName());
                return CliConstants.COMMAND_FAILED;
            }
        } catch (ParseException e) {
            log.error("Error parsing arguments", e);
            System.out.println(e.getMessage());
            return CliConstants.COMMAND_FAILED;
        } catch (Exception e) {
            String message = "Unknown error occurred: " + e.getMessage();
            System.out.println(message);
            log.error(message, e);
            return CliConstants.COMMAND_FAILED;
        }

    }
}

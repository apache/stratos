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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class AddNetworkPartitionCommand implements Command<StratosCommandContext> {

    private static final Logger logger = LoggerFactory.getLogger(AddNetworkPartitionCommand.class);

    private final Options options;

    public AddNetworkPartitionCommand(){
        options = constructOptions();
    }

    private Options constructOptions() {
        final Options options = new Options();

        Option resourcePath = new Option(CliConstants.RESOURCE_PATH, CliConstants.RESOURCE_PATH_LONG_OPTION, true,
                "Network partition resource path");
        resourcePath.setArgName("resource path");
        options.addOption(resourcePath);

        return options;
    }
    /**
     * @return The name of the command
     */
    @Override
    public String getName() {
        return "add-network-partition";
    }

    /**
     * Information about the command
     *
     * @return The description of the command
     */
    @Override
    public String getDescription() {
        return "Add network partition deployment";
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
        return null;
    }

    /**
     * The options accepted by the command
     *
     * @return The Options for the commands
     */
    @Override
    public Options getOptions() {
        return options;
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
    public int execute(StratosCommandContext context, String[] args) throws CommandException {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing {} command...", getName());
            logger.debug("Get name" + getName());
        }

        if (args != null && args.length > 0) {
            String resourcePath = null;
            String partitionJson = null;

            final CommandLineParser parser = new GnuParser();
            CommandLine commandLine;

            try {
                commandLine = parser.parse(options, args);

                if (logger.isDebugEnabled()) {
                    logger.debug("Network Partition deployment");
                }

                if (commandLine.hasOption(CliConstants.RESOURCE_PATH)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Resource path option is passed");
                    }
                    resourcePath = commandLine.getOptionValue(CliConstants.RESOURCE_PATH);
                    partitionJson = readResource(resourcePath);
                }

                if (resourcePath == null) {
                    System.out.println("usage: " + getName() + " [-p <resource path>]");
                    return CliConstants.COMMAND_FAILED;
                }

                RestCommandLineService.getInstance().addNetworkPartition(partitionJson);
                return CliConstants.COMMAND_SUCCESSFULL;

            } catch (ParseException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Error parsing arguments", e);
                }
                System.out.println(e.getMessage());
                return CliConstants.COMMAND_FAILED;
            } catch (IOException e) {
                System.out.println("Invalid resource path");
                return CliConstants.COMMAND_FAILED;
            }


        } else {
            context.getStratosApplication().printUsage(getName());
            return CliConstants.COMMAND_FAILED;
        }
    }

    private String readResource(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }
}
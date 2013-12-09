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
import java.io.InputStreamReader;

public class CartridgeDeploymentCommand implements Command<StratosCommandContext> {

    private static final Logger logger = LoggerFactory.getLogger(CartridgeDeploymentCommand.class);

    private final Options options;

    public CartridgeDeploymentCommand(){
        options = constructOptions();
    }

    private Options constructOptions() {
        final Options options = new Options();

        Option resourcePath = new Option(CliConstants.RESOURCE_PATH, CliConstants.RESOURCE_PATH, true,
                "Cartridge deployment resource path");
        resourcePath.setArgName("resource path");
        options.addOption(resourcePath);

        return options;
    }

    @Override
    public String getName() {
        return CliConstants.CARTRIDGE_DEPLOYMENT;
    }

    @Override
    public String getDescription() {
        return "Add new cartridge deployment";
    }

    @Override
    public String getArgumentSyntax() {
        return null;
    }

    @Override
    public int execute(StratosCommandContext context, String[] args) throws CommandException {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing {} command...", getName());
        }

        if (args != null || args.length > 0) {
            String resourcePath = null;
            String cartridgeDeploymentJSON = null;

            final CommandLineParser parser = new GnuParser();
            CommandLine commandLine;

            try {
                commandLine = parser.parse(options, args);

                if (logger.isDebugEnabled()) {
                    logger.debug("Cartridge deployment");
                }

                if (commandLine.hasOption(CliConstants.RESOURCE_PATH)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Resource path option is passed");
                    }
                    resourcePath = commandLine.getOptionValue(CliConstants.RESOURCE_PATH);
                    cartridgeDeploymentJSON = readResource(resourcePath);
                }

                if (resourcePath == null) {
                    System.out.println("usage: " + getName() + " [-p <resource path>]");
                    return CliConstants.BAD_ARGS_CODE;
                }

                RestCommandLineService.getInstance().deployCartridgeDefinition(cartridgeDeploymentJSON);
                return CliConstants.SUCCESSFUL_CODE;

            } catch (ParseException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Error parsing arguments", e);
                }
                System.out.println(e.getMessage());
                return CliConstants.BAD_ARGS_CODE;
            } catch (IOException e) {
                e.printStackTrace();
                return CliConstants.BAD_ARGS_CODE;
            }


        } else {
            context.getStratosApplication().printUsage(getName());
            return CliConstants.BAD_ARGS_CODE;
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

    @Override
    public Options getOptions() {
        return options;
    }
}

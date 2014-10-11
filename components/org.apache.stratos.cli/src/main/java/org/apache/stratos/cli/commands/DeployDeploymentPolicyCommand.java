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

public class DeployDeploymentPolicyCommand implements Command<StratosCommandContext> {

    private static final Logger logger = LoggerFactory.getLogger(DeployDeploymentPolicyCommand.class);

    private final Options options;

    public DeployDeploymentPolicyCommand(){
        options = constructOptions();
    }

    private Options constructOptions() {
        final Options options = new Options();

        Option resourcePath = new Option(CliConstants.RESOURCE_PATH, CliConstants.RESOURCE_PATH_LONG_OPTION, true,
                "Deployment policy deployment resource path");
        resourcePath.setArgName("resource path");
        options.addOption(resourcePath);

        return options;
    }

    public String getName() {
        return CliConstants.DEPLOYMENT_POLICY_DEPLOYMENT;
    }

    public String getDescription() {
        return "Add new deployment policy";
    }

    public String getArgumentSyntax() {
        return null;
    }

    public int execute(StratosCommandContext context, String[] args) throws CommandException {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing {} command...", getName());
        }

        if (args != null && args.length > 0) {
            String resourcePath = null;
            String deploymentPolicyDeployment = null;

            final CommandLineParser parser = new GnuParser();
            CommandLine commandLine;

            try {
                commandLine = parser.parse(options, args);

                if (logger.isDebugEnabled()) {
                    logger.debug("Deployment policy deployment");
                }

                if (commandLine.hasOption(CliConstants.RESOURCE_PATH)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Resource path option is passed");
                    }
                    resourcePath = commandLine.getOptionValue(CliConstants.RESOURCE_PATH);
                    deploymentPolicyDeployment = readResource(resourcePath);
                }

                if (resourcePath == null) {
                    System.out.println("usage: " + getName() + " [-p <resource path>]");
                    return CliConstants.COMMAND_FAILED;
                }

                RestCommandLineService.getInstance().deployDeploymentPolicy(deploymentPolicyDeployment);
                return CliConstants.COMMAND_SUCCESSFULL;

            } catch (ParseException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Error parsing arguments", e);
                }
                System.out.println(e.getMessage());
                return CliConstants.COMMAND_FAILED;
            } catch (IOException e) {
                //e.printStackTrace();
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

    public Options getOptions() {
        return options;
    }
}

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
 * Update kubernetes host command.
 */
public class UpdateKubernetesHostCommand implements Command<StratosCommandContext> {

    private static final Logger log = LoggerFactory.getLogger(UpdateKubernetesHostCommand.class);

    private Options options;

    public UpdateKubernetesHostCommand() {
        options = new Options();
        Option clusterIdOption = new Option(CliConstants.CLUSTER_ID_OPTION, CliConstants.CLUSTER_ID_LONG_OPTION, true,
                "Kubernetes cluster id");
        clusterIdOption.setArgName("cluster id");
        options.addOption(clusterIdOption);

        Option hostIdOption = new Option(CliConstants.HOST_ID_OPTION, CliConstants.HOST_ID_LONG_OPTION, true,
                "Kubernetes host id");
        hostIdOption.setArgName("host id");
        options.addOption(hostIdOption);

        Option resourcePathOption = new Option(CliConstants.RESOURCE_PATH, CliConstants.RESOURCE_PATH_LONG_OPTION, true,
                "Kubernetes host resource path");
        resourcePathOption.setArgName("resource path");
        options.addOption(resourcePathOption);
    }
    @Override
    public String getName() {
        return "update-kubernetes-host";
    }

    @Override
    public String getDescription() {
        return "Update kubernetes host";
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
            
            if((opts.hasOption(CliConstants.RESOURCE_PATH)) && (opts.hasOption(CliConstants.HOST_ID_OPTION)) 
            		&& (opts.hasOption(CliConstants.CLUSTER_ID_OPTION))) {
            	               
                // get cluster id arg value
            	String clusterId = opts.getOption(CliConstants.CLUSTER_ID_OPTION).getValue();
                if (clusterId == null) {
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }
                
                // get host id arg value
            	String hostId = opts.getOption(CliConstants.HOST_ID_OPTION).getValue();
                if (hostId == null) {
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }
                
                // get resource path arg value
            	String resourcePath = opts.getOption(CliConstants.RESOURCE_PATH).getValue();
                if (resourcePath == null) {
                    context.getStratosApplication().printUsage(getName());
                    return CliConstants.COMMAND_FAILED;
                }
                String resourceFileContent = CliUtils.readResource(resourcePath);
                
                RestCommandLineService.getInstance().updateKubernetesHost(resourceFileContent, clusterId, hostId);
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

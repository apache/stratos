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
package org.apache.stratos.cli;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrTokenizer;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.stratos.cli.commands.*;
import org.apache.stratos.cli.completer.CommandCompleter;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.stratos.cli.utils.CliConstants.STRATOS_DIR;
import static org.apache.stratos.cli.utils.CliConstants.STRATOS_HISTORY_DIR;

public class StratosApplication extends CommandLineApplication<StratosCommandContext> {

    private static final Logger logger = LoggerFactory.getLogger(StratosApplication.class);

    private final Map<String, Command<StratosCommandContext>> commands;
    private final StratosCommandContext context;

    private final Options options;

    public StratosApplication(String[] args) {
        super(args);
        commands = new TreeMap<String, Command<StratosCommandContext>>();
        context = new StratosCommandContext(this);

        options = constructOptions();

        createCommands();
    }

    /**
     * Construct Options.
     *
     * @return Options expected from command-line.
     */
    private Options constructOptions() {
        final Options options = new Options();
        Option usernameOption = new Option(CliConstants.USERNAME_OPTION, CliConstants.USERNAME_LONG_OPTION, true,
                "Username");
        usernameOption.setArgName("username");
        options.addOption(usernameOption);

        Option passwordOption = new Option(CliConstants.PASSWORD_OPTION, CliConstants.PASSWORD_LONG_OPTION, true,
                "Password");
        passwordOption.setArgName("password");
        passwordOption.setOptionalArg(true);
        options.addOption(passwordOption);
        options.addOption(CliConstants.HELP_OPTION, CliConstants.HELP_LONG_OPTION, false, "Display this help");
        options.addOption(CliConstants.TRACE_OPTION, false, "Enable trace logging");
        options.addOption(CliConstants.DEBUG_OPTION, false, "Enable debug logging");
        return options;
    }

    private void createCommands() {
        Command<StratosCommandContext> command = new HelpCommand();
        commands.put(command.getName(), command);

        command = new ExitCommand();
        commands.put(command.getName(), command);

        command = new ListCartridgesCommand();
        commands.put(command.getName(), command);

        command = new AddTenantCommand();
        commands.put(command.getName(), command);

        command = new AddUserCommand();
        commands.put(command.getName(), command);

        command = new DeleteUserCommand();
        commands.put(command.getName(), command);

        command = new ListUsers();
        commands.put(command.getName(), command);

        command = new ListTenants();
        commands.put(command.getName(), command);

        command = new DeactivateTenantCommand();
        commands.put(command.getName(), command);

        command = new ActivateTenantCommand();
        commands.put(command.getName(), command);

        command = new AddCartridgeCommand();
        commands.put(command.getName(), command);

        command = new UpdateCartridgeCommand();
        commands.put(command.getName(), command);

        command = new AddAutoscalingPolicyCommand();
        commands.put(command.getName(), command);

        command = new CreateApplicationCommand();
        commands.put(command.getName(), command);

        command = new ListApplicationsCommand();
        commands.put(command.getName(), command);

        command = new RemoveCartridgeCommand();
        commands.put(command.getName(), command);

        command = new ListAutoscalePolicyCommand();
        commands.put(command.getName(), command);

        command = new DescribeCartridgeCommand();
        commands.put(command.getName(), command);

        command = new DescribeDeploymentPolicyCommand();
        commands.put(command.getName(), command);

        command = new DescribeAutoScalingPolicyCommand();
        commands.put(command.getName(), command);

        command = new SynchronizeArtifactsCommand();
        commands.put(command.getName(), command);

        command = new AddKubernetesClusterCommand();
        commands.put(command.getName(), command);

        command = new ListKubernetesClustersCommand();
        commands.put(command.getName(), command);

        command = new ListKubernetesHostsCommand();
        commands.put(command.getName(), command);

        command = new AddKubernetesHostCommand();
        commands.put(command.getName(), command);

        command = new RemoveKubernetesClusterCommand();
        commands.put(command.getName(), command);

        command = new RemoveKubernetesHostCommand();
        commands.put(command.getName(), command);

        command = new UpdateKubernetesMasterCommand();
        commands.put(command.getName(), command);

        command = new UpdateKubernetesHostCommand();
        commands.put(command.getName(), command);

        command = new AddCartridgeGroupCommand();
        commands.put(command.getName(), command);

        command = new DescribeCartridgeGroupCommand();
        commands.put(command.getName(), command);

        command = new ListCartridgeGroupsCommand();
        commands.put(command.getName(), command);

        command = new RemoveCartridgeGroupCommand();
        commands.put(command.getName(), command);

        command = new DeployApplicationCommand();
        commands.put(command.getName(), command);

        command = new UndeployApplicationCommand();
        commands.put(command.getName(), command);

        command = new DescribeApplicationCommand();
        commands.put(command.getName(), command);

        command = new AddDomainMappingsCommand();
        commands.put(command.getName(), command);

        command = new ListDomainMappingsCommand();
        commands.put(command.getName(), command);

        command = new RemoveDomainMappingsCommand();
        commands.put(command.getName(), command);

        command=new DeleteAutoScalingPolicyCommand();
        commands.put(command.getName(), command);

        command=new AddNetworkPartitionCommand();
        commands.put(command.getName(), command);

        command=new RemoveNetworkPartitionCommand();
        commands.put(command.getName(), command);

        command = new ListNetworkPartitionCommand();
        commands.put(command.getName(), command);

        command = new UpdateNetworkPartitionCommand();
        commands.put(command.getName(), command);

        if (logger.isDebugEnabled()) {
            logger.debug("Created {} commands for the application. {}", commands.size(), commands.keySet());
        }
    }

    private void createAutocomplete() {
        reader.addCompleter(new CommandCompleter(commands));
    }

    @Override
    protected String getPrompt() {
        return CliConstants.STRATOS_SHELL_PROMPT;
    }

    @Override
    protected File getHistoryFile(String username) {
        File stratosFile = new File(System.getProperty("user.home"), STRATOS_DIR);
        File historyFile = new File(stratosFile, STRATOS_HISTORY_DIR + "_" + username);
        return historyFile;
    }

    @Override
    public int run(String[] args) {
        boolean loaded = loadRequiredProperties();
        if (!loaded) {
            return CliConstants.ERROR_CODE;
        }

        // To get the command action from arguments
        String[] remainingArgs = null;

        // Command action
        String action = null;

        String usernameInput = null;
        String passwordInput = null;

        if (args != null && args.length > 0) {
            // Arguments are passed.
            if (logger.isDebugEnabled()) {
                logger.debug("Arguments:");
                for (String arg : args) {
                    logger.debug(arg);
                }
            }

            final CommandLineParser parser = new GnuParser();
            CommandLine commandLine;
            try {
                // Must add all options. Otherwise actions cannot be performed directly by command line.
                Options allCommandOptions = new Options();
                for (Command<StratosCommandContext> command : commands.values()) {
                    Options commandOptions = command.getOptions();
                    if (commandOptions != null) {
                        Collection<?> allOptions = commandOptions.getOptions();
                        for (Object o : allOptions) {
                            allCommandOptions.addOption((Option) o);
                        }
                    }
                }
                // Add options in this application
                Collection<?> allOptions = options.getOptions();
                for (Object o : allOptions) {
                    allCommandOptions.addOption((Option) o);
                }

                commandLine = parser.parse(options, args, true);
                remainingArgs = commandLine.getArgs();
                if (remainingArgs != null && remainingArgs.length > 0) {
                    // Get command action
                    action = remainingArgs[0];
                }

                // Set logger levels from this point onwards
                setLoggerLevel(commandLine.hasOption(CliConstants.TRACE_OPTION),
                        commandLine.hasOption(CliConstants.DEBUG_OPTION));

                if (commandLine.hasOption(CliConstants.USERNAME_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Username option is passed");
                    }
                    usernameInput = commandLine.getOptionValue(CliConstants.USERNAME_OPTION);
                }
                if (commandLine.hasOption(CliConstants.PASSWORD_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Password option is passed");
                    }
                    passwordInput = commandLine.getOptionValue(CliConstants.PASSWORD_OPTION);
                }
                if (commandLine.hasOption(CliConstants.HELP_ACTION)) {
                    printHelp();
                    return CliConstants.COMMAND_SUCCESSFULL;
                }
            } catch (ParseException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Error parsing arguments when trying to login", e);
                }
                System.out.println(e.getMessage());
                return CliConstants.COMMAND_FAILED;
            }

        }

        if (StringUtils.isNotBlank(action)) {
            // User is executing an action
            if (logger.isDebugEnabled()) {
                logger.debug("Action: {}", action);
            }
            Command<StratosCommandContext> command = commands.get(action);
            if (command == null) {
                printHelp();
                return CliConstants.COMMAND_FAILED;
            }

            boolean loginRequired = !CliConstants.HELP_ACTION.equals(action);

            if (loginRequired && logger.isDebugEnabled()) {
                logger.debug("Trying to login...");
            }

            if (loginRequired && !login(usernameInput, passwordInput, false)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Exiting from CLI. Login required but login might have failed: {}", action);
                }
                // Exit
                return CliConstants.ERROR_CODE;
            }

            try {
                String[] actionArgs = Arrays.copyOfRange(remainingArgs, 1, remainingArgs.length);
                if (logger.isDebugEnabled()) {
                    logger.debug("Executing Action: {} {}", action, Arrays.asList(actionArgs));
                }
                int returnCode = command.execute(context, actionArgs);
                if (logger.isDebugEnabled()) {
                    logger.debug("Exiting with error code {} after executing action {}", returnCode, action);
                }
                System.exit(returnCode);
            } catch (CommandException e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Error executing command: " + action, e);
                }
                return CliConstants.ERROR_CODE;
            }
        } else {
            if (login(usernameInput, passwordInput, true)) {
                System.out.println("Successfully authenticated");
            } else {
                // Exit
                return CliConstants.ERROR_CODE;
            }

            promptLoop();
        }
        return CliConstants.COMMAND_SUCCESSFULL;
    }

    private boolean login(String usernameInput, String passwordInput, boolean validateLogin) {
        // TODO Previous CLI version uses a keystore. Here we are not using it.
        // Check whether user has passed username and password
        if (StringUtils.isBlank(usernameInput) && StringUtils.isBlank(passwordInput)) {
            // User has not passed any arguments.
            // Try authenticating from the values found
            usernameInput = context.getString(CliConstants.STRATOS_USERNAME_ENV_PROPERTY);
            passwordInput = context.getString(CliConstants.STRATOS_PASSWORD_ENV_PROPERTY);

            if (logger.isDebugEnabled()) {
                if (StringUtils.isNotBlank(usernameInput) && StringUtils.isNotBlank(passwordInput)) {
                    logger.debug("Found authentication details for {} from context", usernameInput);
                }
            }

        }
        // Get user input if not passed as args
        if (StringUtils.isBlank(usernameInput)) {
            usernameInput = getInput("Username");
        }
        if (StringUtils.isBlank(passwordInput)) {
            passwordInput = getInput("Password", '*');
        }

        boolean success = false;
        String stratosURL = null;
        stratosURL = context.getString(CliConstants.STRATOS_URL_ENV_PROPERTY);

        // This is to create the history file.
        // This section execute only when user didn't enter the username as command line arguments
        if (username == null) {
            reader = null;
            reader = createConsoleReaderWhithoutArgs(usernameInput);
        }

        createAutocomplete();

        try {
            success = RestCommandLineService.getInstance().login(stratosURL, usernameInput, passwordInput, validateLogin);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error when trying to login", e);
            }
        }
        if (success) {
            if (logger.isDebugEnabled()) {
                logger.debug("Successfully authenticated");
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Authentication failed.");
            }
        }
        return success;
    }

    @Override
    protected int executeCommand(String line) {
        String[] tokens = new StrTokenizer(line).getTokenArray();
        String action = tokens[0];
        String[] actionArgs = Arrays.copyOfRange(tokens, 1, tokens.length);
        if (logger.isDebugEnabled()) {
            logger.debug("Executing command action: {}, Tokens: {}", action, tokens.length);
        }
        Command<StratosCommandContext> command = commands.get(action);
        if (command == null) {
            System.out.println(action + ": command not found.");
            return CliConstants.COMMAND_FAILED;
        }
        try {
            return command.execute(context, actionArgs);
        } catch (CommandException e) {
            if (logger.isErrorEnabled()) {
                logger.error("Error executing command: " + action, e);
            }
            return CliConstants.ERROR_CODE;
        }
    }

    /**
     * @return {@code true} if required properties are loaded
     */
    private boolean loadRequiredProperties() {
        if (logger.isDebugEnabled()) {
            logger.debug("Loading properties...");
        }
        // Load properties
        String stratosURL = null;
        String username = null;
        String password = null;

        stratosURL = System.getenv(CliConstants.STRATOS_URL_ENV_PROPERTY);
        username = System.getenv(CliConstants.STRATOS_USERNAME_ENV_PROPERTY);
        password = System.getenv(CliConstants.STRATOS_PASSWORD_ENV_PROPERTY);

        if (StringUtils.isBlank(stratosURL)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Required configuration not found.");
            }
            // Stratos Controller details are not set.
            System.out.format("Could not find required \"%s\" variable in your environment.%n",
                    CliConstants.STRATOS_URL_ENV_PROPERTY);
            return false;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Required configuration found. Validating {}", stratosURL);
        }

        int slashCount = StringUtils.countMatches(stratosURL, "/");
        int colonCount = StringUtils.countMatches(stratosURL, ":");

        UrlValidator urlValidator = new UrlValidator(new String[] { "https" },UrlValidator.ALLOW_LOCAL_URLS);

        // port must be provided, so colonCount must be 2
        // context path must not be provided, so slashCount must not be >3

        if (!urlValidator.isValid(stratosURL) || colonCount != 2 || slashCount >3) {
            if (logger.isDebugEnabled()) {
                logger.debug("Stratos Controller URL {} is not valid", stratosURL);
            }
            System.out.format(
                    "The \"%s\" variable in your environment is not a valid URL. You have provided \"%s\".%n"
                            + "Please provide the Stratos Controller URL as follows%nhttps://<host>:<port>%n",
                    CliConstants.STRATOS_URL_ENV_PROPERTY, stratosURL);
            return false;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Stratos Controller URL {} is valid.", stratosURL);
            logger.debug("Adding the values to context.");
        }
        context.put(CliConstants.STRATOS_URL_ENV_PROPERTY, stratosURL);
        context.put(CliConstants.STRATOS_USERNAME_ENV_PROPERTY, username);
        context.put(CliConstants.STRATOS_PASSWORD_ENV_PROPERTY, password);
        return true;
    }

    private void setLoggerLevel(boolean trace, boolean debug) {
        // We are using Log4j. So, get the logger and set log levels.
        org.apache.log4j.Logger logger = LogManager.getLogger(StratosApplication.class.getPackage().getName());
        if (logger != null && trace) {
            logger.setLevel(Level.TRACE);
            LogManager.getRootLogger().setLevel(Level.TRACE);
        } else if (logger != null && debug) {
            logger.setLevel(Level.DEBUG);
            LogManager.getRootLogger().setLevel(Level.DEBUG);
        }
    }

    public void printHelp(final String action) {
        Command<StratosCommandContext> command = commands.get(action);
        if (command == null) {
            System.out.println(action + ": command not found. Help not available.");
            return;
        }
        System.out.println(command.getDescription());
        Options options = command.getOptions();
        if (options != null) {
            if (StringUtils.isNotBlank(command.getArgumentSyntax())) {
                printHelp(command.getName() + " " + command.getArgumentSyntax(), options);
            } else {
                printHelp(command.getName(), options);
            }
        } else {
            // No options. Just print the usage.
            printUsage(command);
        }
    }

    public void printHelp() {
        printHelp(CliConstants.STRATOS_APPLICATION_NAME, options);
        System.out.println("\n\nAvailable Commands: ");
        for (String action : commands.keySet()) {
            Command<StratosCommandContext> command = commands.get(action);
            if (command != null) {
                System.out.format("%-35s %s%n", command.getName(), command.getDescription());
            }
        }

        System.out.println("\nFor help on a specific command type:\nhelp [command]");
    }

    /**
     * Print "help" with usage
     */
    private void printHelp(final String commandLineSyntax, final Options options) {
        final HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(commandLineSyntax, options, true);
    }

    public void printUsage(final String action) {
        Command<StratosCommandContext> command = commands.get(action);
        if (command == null) {
            return;
        }
        printUsage(command);
    }

    private void printUsage(Command<StratosCommandContext> command) {
        Options options = command.getOptions();
        if (options != null) {
            if (StringUtils.isNotBlank(command.getArgumentSyntax())) {
                printUsage(command.getName() + " " + command.getArgumentSyntax(), options);
            } else {
                printUsage(command.getName(), options);
            }
        } else {
            System.out.print("usage: ");
            if (StringUtils.isNotBlank(command.getArgumentSyntax())) {
                System.out.println(command.getName() + " " + command.getArgumentSyntax());
            } else {
                System.out.println(command.getName());
            }
        }
    }

    /**
     * Print "usage"
     */
    private void printUsage(final String commandLineSyntax, final Options options) {
        final PrintWriter writer = new PrintWriter(System.out);
        final HelpFormatter usageFormatter = new HelpFormatter();
        usageFormatter.printUsage(writer, 80, commandLineSyntax, options);
        writer.flush();
    }

}

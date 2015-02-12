package org.apache.stratos.cli.commands;

import org.apache.commons.cli.*;
import org.apache.stratos.cli.Command;
import org.apache.stratos.cli.RestCommandLineService;
import org.apache.stratos.cli.StratosCommandContext;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by dinithi on 2/11/15.
 */
public class ListCartridgesByFilterCommand implements Command<StratosCommandContext>{

    private static final Logger logger = LoggerFactory.getLogger(ListCartridgesByFilterCommand.class);

    private final Options options;

    public ListCartridgesByFilterCommand(){
        options = constructOptions();
    }

    private Options constructOptions() {
        final Options options = new Options();

        Option filterOption = new Option(CliConstants.CARTRIDGE_FILTER_OPTION, CliConstants.CARTRIDGE_FILTER_LONG_OPTION, true,
                "Filter");
        filterOption.setArgName("filter");
        options.addOption(filterOption);

        return options;
    }

    public String getName() {
        return "list-cartridges-by-filter";
    }

    public String getDescription() {
        return "List cartridges by a filter";
    }

    public String getArgumentSyntax() {
        return null;
    }

    public int execute(StratosCommandContext context, String[] args) throws CommandException {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing {} command...", getName());
        }

        if (args != null && args.length > 0) {
            String filter= null;

            final CommandLineParser parser = new GnuParser();
            CommandLine commandLine;

            try {
                commandLine = parser.parse(options, args);

                if (logger.isDebugEnabled()) {
                    logger.debug("List cartridges by a filter");
                }

                if (commandLine.hasOption(CliConstants.CARTRIDGE_FILTER_OPTION)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Filter option is passed");
                    }
                    filter = commandLine.getOptionValue(CliConstants.CARTRIDGE_FILTER_OPTION);
                }

                if (filter == null) {
                    System.out.println("usage: " + getName() + "usage: " + getName() + " [-f <filter>]");
                    return CliConstants.COMMAND_FAILED;
                }

                RestCommandLineService.getInstance().listCartridgesByFilter(filter);
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

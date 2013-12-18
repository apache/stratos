package org.apache.stratos.cli.commands;

import org.apache.commons.cli.Options;
import org.apache.stratos.cli.Command;
import org.apache.stratos.cli.RestCommandLineService;
import org.apache.stratos.cli.StratosCommandContext;
import org.apache.stratos.cli.exception.CommandException;
import org.apache.stratos.cli.utils.CliConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoscalePolicyCommand implements Command<StratosCommandContext> {

    private static final Logger logger = LoggerFactory.getLogger(AutoscalePolicyCommand.class);

    public AutoscalePolicyCommand() {
    }

    public String getName() {
        return CliConstants.LIST_AUTOSCALE_POLICY;
    }

    public String getDescription() {
        return "List available autoscale policies";
    }

    public String getArgumentSyntax() {
        return null;
    }

    public int execute(StratosCommandContext context, String[] args) throws CommandException {
        if (logger.isDebugEnabled()) {
            logger.debug("Executing {} command...", getName());
        }
        if (args == null || args.length == 0) {
            //CommandLineService.getInstance().listAvailableCartridges();
            RestCommandLineService.getInstance().listAutoscalePolicies();
            return CliConstants.SUCCESSFUL_CODE;
        } else {
            context.getStratosApplication().printUsage(getName());
            return CliConstants.BAD_ARGS_CODE;
        }
    }

    public Options getOptions() {
        return null;
    }
}

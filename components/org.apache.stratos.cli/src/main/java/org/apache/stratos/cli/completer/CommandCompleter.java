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
package org.apache.stratos.cli.completer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.stratos.cli.Command;
import org.apache.stratos.cli.StratosApplication;
import org.apache.stratos.cli.StratosCommandContext;
import org.apache.stratos.cli.utils.CliConstants;

public class CommandCompleter implements Completer {

	private static final Logger logger = LoggerFactory.getLogger(StratosApplication.class);

	/**
	 * Keep arguments for each command
	 */
	private final Map<String, Collection<String>> argumentMap;
	
	private final Completer helpCommandCompleter;

	private final Completer defaultCommandCompleter;
	
	private final Completer fileNameCompleter;

	public CommandCompleter(Map<String, Command<StratosCommandContext>> commands) {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating auto complete for {} commands", commands.size());
		}
		fileNameCompleter = new StratosFileNameCompleter();
		argumentMap = new HashMap<String, Collection<String>>();
		defaultCommandCompleter = new StringsCompleter(commands.keySet());
		helpCommandCompleter = new ArgumentCompleter(new StringsCompleter(CliConstants.HELP_ACTION),
				defaultCommandCompleter);
		for (String action : commands.keySet()) {
			
			Command<StratosCommandContext> command = commands.get(action);
			Options commandOptions = command.getOptions();
			if (commandOptions != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Creating argument completer for command: {}", action);
				}
				List<String> arguments = new ArrayList<String>();
				Collection<?> allOptions = commandOptions.getOptions();
				for (Object o : allOptions) {
					Option option = (Option) o;
					String longOpt = option.getLongOpt();
					String opt = option.getOpt();
					if (StringUtils.isNotBlank(longOpt)) {
						arguments.add("--" + longOpt);
					} else if (StringUtils.isNotBlank(opt)) {
						arguments.add("-" + opt);
					}
				}

				argumentMap.put(action, arguments);
			}
		}
	}

	@Override
	public int complete(String buffer, int cursor, List<CharSequence> candidates) {
			
		if(buffer.contains(CliConstants.RESOURCE_PATH_LONG_OPTION)) {
			return fileNameCompleter.complete(buffer, cursor, candidates);
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Buffer: {}, cursor: {}", buffer, cursor);
			logger.trace("Candidates {}", candidates);
		}
		if (StringUtils.isNotBlank(buffer)) {
			// User is typing a command
			StrTokenizer strTokenizer = new StrTokenizer(buffer);
			String action = strTokenizer.next();
			Collection<String> arguments = argumentMap.get(action);
			if (arguments != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Arguments found for {}, Tokens: {}", action, strTokenizer.getTokenList());
					logger.trace("Arguments for {}: {}", action, arguments);
				}
				List<String> args = new ArrayList<String>(arguments);
				List<Completer> completers = new ArrayList<Completer>();
				for (String token : strTokenizer.getTokenList()) {
					boolean argContains = arguments.contains(token);
					if (token.startsWith("-") && !argContains) {
						continue;
					}
					if (argContains) {
						if (logger.isTraceEnabled()) {
							logger.trace("Removing argument {}", token);
						}
						args.remove(token);
					}
					completers.add(new StringsCompleter(token));
				}
				completers.add(new StringsCompleter(args));
				Completer completer = new ArgumentCompleter(completers);
				return completer.complete(buffer, cursor, candidates);
			} else if (CliConstants.HELP_ACTION.equals(action)) {
				// For help action, we need to display available commands as arguments
				return helpCommandCompleter.complete(buffer, cursor, candidates);
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Using Default Completer...");
		}
		return defaultCommandCompleter.complete(buffer, cursor, candidates);
	}

}

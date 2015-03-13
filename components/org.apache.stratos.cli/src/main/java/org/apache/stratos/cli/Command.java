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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.stratos.cli.exception.CommandException;

public interface Command<T extends CommandContext> {

	/**
	 * @return The name of the command
	 */
	String getName();

	/**
	 * Information about the command
	 * 
	 * @return The description of the command
	 */
	String getDescription();

	/**
	 * This should return the syntax required for the command.
	 * 
	 * Used to display help.
	 * 
	 * @return The syntax for this command
	 */
	String getArgumentSyntax();

	/**
	 * The options accepted by the command
	 * 
	 * @return The Options for the commands
	 */
	Options getOptions();

	/**
	 * Executing the commands. Returns a code
	 * 
	 * @param context
	 *            The context assoicated with the Command Line Application
	 * @param args
	 *            The arguments for the command
	 * @param alreadyParsedOpts
	 *            Options parsed by any parent parsers.
	 * @return The status code
	 * @throws org.apache.stratos.cli.exception.CommandException
	 *             if any errors occur when executing the command
	 */
	int execute(T context, String[] args, Option[] alreadyParsedOpts) throws CommandException;

}

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

package org.apache.stratos.cli.utils;

/**
 * Constants for CLI Tool
 */
public class CliConstants {

	public static final String STRATOS_APPLICATION_NAME = "stratos";

	public static final String STRATOS_URL_ENV_PROPERTY = "STRATOS_URL";

	public static final String STRATOS_USERNAME_ENV_PROPERTY = "STRATOS_USERNAME";

	public static final String STRATOS_PASSWORD_ENV_PROPERTY = "STRATOS_PASSWORD";

	public static final String STRATOS_SHELL_PROMPT = "stratos> ";
	
	public static final int SUCCESSFUL_CODE = 0;
	public static final int BAD_ARGS_CODE = 1;
	public static final int ERROR_CODE = 2;


	/**
	 * The Directory for storing configuration
	 */
	public static final String STRATOS_DIR = ".stratos";
	public static final String STRATOS_HISTORY_DIR = ".history";

	public static final String HELP_ACTION = "help";

	/**
	 * Subscribe to a cartridge.
	 */
	public static final String SUBSCRIBE_ACTION = "subscribe";

	public static final String UNSUBSCRIBE_ACTION = "unsubscribe";

	/**
	 * List the subscribed cartridges
	 */
	public static final String LIST_ACTION = "list";

	/**
	 * List the available cartridges
	 */
	public static final String CARTRIDGES_ACTION = "cartridges";

    /**
     * Add tenant
     */
    public static final String ADD_TENANT = "add-tenant";

	/**
	 * Give information of a cartridge.
	 */
	public static final String INFO_ACTION = "info";

	/**
	 * Synchronize repository
	 */
	public static final String SYNC_ACTION = "sync";

	/**
	 * Domain mapping
	 */
	public static final String ADD_DOMAIN_MAPPING_ACTION = "add-domain-mapping";
	/**
	 * Remove Domain mapping
	 */
	public static final String REMOVE_DOMAIN_MAPPING_ACTION = "remove-domain-mapping";
	
	/**
	 * List the available policies
	 */
	public static final String POLICIES_ACTION = "policies";

	/**
	 * Exit action
	 */
	public static final String EXIT_ACTION = "exit";

	public static final String REPO_URL_OPTION = "r";
	public static final String REPO_URL_LONG_OPTION = "repo-url";
	
	public static final String PRIVATE_REPO_OPTION = "i";
	public static final String PRIVATE_REPO_LONG_OPTION = "private-repo";

	public static final String USERNAME_OPTION = "u";
	public static final String USERNAME_LONG_OPTION = "username";

	public static final String PASSWORD_OPTION = "p";
	public static final String PASSWORD_LONG_OPTION = "password";

	public static final String HELP_OPTION = "h";
	public static final String HELP_LONG_OPTION = "help";
	
	public static final String POLICY_OPTION = "o";
	public static final String POLICY_LONG_OPTION = "policy";
	
	public static final String CONNECT_OPTION = "c";
	public static final String CONNECT_LONG_OPTION = "connect";
	
	public static final String DATA_ALIAS_OPTION = "d";
	public static final String DATA_ALIAS_LONG_OPTION = "data-alias";
	
	public static final String FULL_OPTION = "f";
	public static final String FULL_LONG_OPTION = "full";
	
	public static final String FORCE_OPTION = "f";
	public static final String FORCE_LONG_OPTION = "force";
	
	public static final String TRACE_OPTION = "trace";
	
	public static final String DEBUG_OPTION = "debug";

    // Add tenant options
    public static final String FIRST_NAME_OPTION = "f";
    public static final String FIRST_NAME_LONG_OPTION = "first-name";

    public static final String LAST_NAME_OPTION = "l";
    public static final String LAST_NAME_LONG_OPTION = "last-name";

    public static final String DOMAIN_NAME_OPTION = "d";
    public static final String DOMAIN_NAME_LONG_OPTION = "domain-name";

    public static final String EMAIL_OPTION = "e";
    public static final String EMAIL_LONG_OPTION = "email";

    public static final String ACTIVE_OPTION = "a";
    public static final String ACTIVE_LONG_OPTION = "active";
}

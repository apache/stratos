/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer.git.internal;


/**
 * Git based DeploymentSynchronizer constansts
 */
public class GitDeploymentSynchronizerConstants {

    //Git repo url related constansts
    //public static final String GITHUB_HTTP_REPO_URL_PREFIX = "http://github.com";
    public static final String GIT_HTTP_REPO_URL_PREFIX = "http://";
    //public static final String GITHUB_HTTPS_REPO_URL_PREFIX = "https://github.com";
    public static final String GIT_HTTPS_REPO_URL_PREFIX = "https://";
    public static final String GITHUB_READ_ONLY_REPO_URL_PREFIX = "git://github.com";
    public static final String GIT_REPO_SSH_URL_PREFIX = "ssh://";
    public static final String GIT_REPO_SSH_URL_SUBSTRING = "@";

    //SSH related constants
    public static final String SSH_KEY_DIRECTORY = ".ssh";
    public static final String SSH_KEY = "wso2";

    //super tenant Id
    public static final int SUPER_TENANT_ID = -1234;

    //ServerKey property name from carbon.xml, for the cartridge short name --> not used. CARTRIDGE_ALIAS is used instead.
    //public static final String SERVER_KEY = "ServerKey";

    //EPR for the repository Information Service
    public static final String REPO_INFO_SERVICE_EPR = "RepoInfoServiceEpr";

    //CartridgeAlias property name from carbon.xml
    public static final String CARTRIDGE_ALIAS = "CartridgeAlias";

    //key name and path for ssh based authentication
    public static final String SSH_PRIVATE_KEY_NAME = /*DEPLOYMENT_SYNCHRONIZER + */".SshPrivateKeyName";
    public static final String SSH_PRIVATE_KEY_PATH = /*DEPLOYMENT_SYNCHRONIZER +*/ ".SshPrivateKeyPath";

    //regular expressions for extracting username and password form json string
    public static final String USERNAME_REGEX = "username:(.*?),";
    public static final String PASSWORD_REGEX = "password:(.*?)}";

    //Git based constants
    public static final String GIT_REFS_HEADS_MASTER = "refs/heads/master";
    public static final String REMOTES_ORIGIN_MASTER = "remotes/origin/master";
    public static final String REMOTE = "remote";
    public static final String ORIGIN = "origin";
    public static final String URL = "url";
    public static final String FETCH = "fetch";
    public static final String BRANCH = "branch";
    public static final String MASTER = "master";
    public static final String MERGE = "merge";
    public static final String FETCH_LOCATION = "+refs/heads/*:refs/remotes/origin/*";

}

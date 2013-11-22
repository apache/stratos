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

package org.apache.stratos.deployment.synchronizer.git.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;

import java.io.File;

/**
 * Git Repository Context class. Keeps track of git configurations per tenant.
 */
public class RepositoryContext {

    private static final Log log = LogFactory.getLog(RepositoryContext.class);

    private String gitRemoteRepoUrl;
    private String gitLocalRepoPath;
    private Repository localRepo;
    private Git git;
    private boolean cloneExists;
    private int tenantId;
    private File gitRepoDir;
    private boolean keyBasedAuthentication;
    private String repoUsername;
    private String repoPassword;

    public RepositoryContext () {

    }

    public String getGitRemoteRepoUrl() {
        return gitRemoteRepoUrl;
    }

    public void setGitRemoteRepoUrl(String gitRemoteRepoUrl) {
        this.gitRemoteRepoUrl = gitRemoteRepoUrl;
    }

    public String getGitLocalRepoPath() {
        return gitLocalRepoPath;
    }

    public void setGitLocalRepoPath(String gitLocalRepoPath) {
        this.gitLocalRepoPath = gitLocalRepoPath;
    }

    public Repository getLocalRepo() {
        return localRepo;
    }

    public void setLocalRepo(Repository localRepo) {
        this.localRepo = localRepo;
    }

    public Git getGit() {
        return git;
    }

    public void setGit(Git git) {
        this.git = git;
    }

    public boolean cloneExists() {
        return cloneExists;
    }

    public void setCloneExists(boolean cloneExists) {
        this.cloneExists = cloneExists;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public File getGitRepoDir() {
        return gitRepoDir;
    }

    public void setGitRepoDir(File gitRepoDir) {
        this.gitRepoDir = gitRepoDir;
    }

    public boolean getKeyBasedAuthentication() {
        return keyBasedAuthentication;
    }

    public void setKeyBasedAuthentication(boolean keyBasedAuthentication) {
        this.keyBasedAuthentication = keyBasedAuthentication;
    }

	public String getRepoUsername() {
		return repoUsername;
	}

	public void setRepoUsername(String repoUsername) {
		this.repoUsername = repoUsername;
	}

	public String getRepoPassword() {
		return repoPassword;
	}

	public void setRepoPassword(String repoPassword) {
		this.repoPassword = repoPassword;
	}
    
}

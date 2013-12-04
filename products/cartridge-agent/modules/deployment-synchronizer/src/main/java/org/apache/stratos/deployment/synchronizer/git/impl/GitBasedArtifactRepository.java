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

package org.apache.stratos.deployment.synchronizer.git.impl;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.deployment.synchronizer.ArtifactRepository;
import org.apache.stratos.deployment.synchronizer.RepositoryInformation;
import org.apache.stratos.deployment.synchronizer.git.internal.CustomJschConfigSessionFactory;
import org.apache.stratos.deployment.synchronizer.git.internal.GitDeploymentSynchronizerConstants;
import org.apache.stratos.deployment.synchronizer.git.internal.RepositoryContext;
import org.apache.stratos.deployment.synchronizer.git.util.Utilities;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 * Git based artifact repository.
 * 
 * 
 */
public class GitBasedArtifactRepository /*implements ArtifactRepository*/ {

    private static final Log log = LogFactory.getLog(GitBasedArtifactRepository.class);

    //Map to keep track of git context per tenant (remote urls, jgit git objects, etc.)
    private static ConcurrentHashMap<Integer, RepositoryContext> 
    					tenantToRepoContextMap = new ConcurrentHashMap<Integer, RepositoryContext>();

    private GitBasedArtifactRepository () {

    }

    /**
     * initializes and populates the git context with relevant data
     *
     * @param tenantId id of the tenant
     *
     * @throws DeploymentSynchronizerException
     */
    private static void initGitContext (RepositoryInformation repositoryInformation)  {

     /*   if (tenantId == GitDeploymentSynchronizerConstants.SUPER_TENANT_ID)
            return;*/
    	
    	log.info("In init git context............");
    	
    	int tenantId = Integer.parseInt(repositoryInformation.getTenantId());
    	String gitLocalRepoPath = repositoryInformation.getRepoPath();
        RepositoryContext gitRepoCtx = new RepositoryContext();
        String gitRemoteRepoUrl = repositoryInformation.getRepoUrl();
        
        log.info("local path " + gitLocalRepoPath);
        log.info("remote url " + gitRemoteRepoUrl);
        log.info("tenant " + tenantId);
        
        gitRepoCtx.setTenantId(tenantId);
        gitRepoCtx.setGitLocalRepoPath(gitLocalRepoPath);        
		gitRepoCtx.setGitRemoteRepoUrl(gitRemoteRepoUrl);
		
		gitRepoCtx.setRepoUsername(repositoryInformation.getRepoUsername());
		gitRepoCtx.setRepoPassword(repositoryInformation.getRepoPassword());

        try {
			if(isKeyBasedAuthentication(gitRemoteRepoUrl, tenantId)) {
			    gitRepoCtx.setKeyBasedAuthentication(true);
			    initSSHAuthentication();
			}
			else
			    gitRepoCtx.setKeyBasedAuthentication(false);
		} catch (Exception e1) {
			log.error("Exception occurred.. " + e1.getMessage(), e1);
		}

        FileRepository localRepo = null;
        try {
            localRepo = new FileRepository(new File(gitLocalRepoPath + "/.git"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        gitRepoCtx.setLocalRepo(localRepo);
        gitRepoCtx.setGit(new Git(localRepo));
        gitRepoCtx.setCloneExists(false);

        cacheGitRepoContext(tenantId, gitRepoCtx);
    }

    /**
     * Checks if key based authentication (SSH) is required
     *
     * @param url git repository url for the tenant
     * @param tenantId id of the tenant
     *
     * @return true if SSH authentication is required, else false
     *
     * @throws DeploymentSynchronizerException
     */
    private static boolean isKeyBasedAuthentication(String url, int tenantId) throws Exception {

        if (url.startsWith(GitDeploymentSynchronizerConstants.GIT_HTTP_REPO_URL_PREFIX) ||
                url.startsWith(GitDeploymentSynchronizerConstants.GIT_HTTPS_REPO_URL_PREFIX)) {//http or https url
            // authentication with username and password, not key based
            return false;
        }

        else if (url.startsWith(GitDeploymentSynchronizerConstants.GITHUB_READ_ONLY_REPO_URL_PREFIX)) { //github read-only repo url
            // no authentication required
            return false;
        }

        else if (url.startsWith(GitDeploymentSynchronizerConstants.GIT_REPO_SSH_URL_PREFIX) ||
                url.contains(GitDeploymentSynchronizerConstants.GIT_REPO_SSH_URL_SUBSTRING)) { //other repo, needs ssh authentication
            // key based authentication
            return true;
        }

        else {
            log.error("Invalid git URL provided for tenant " + tenantId);
            throw new Exception("Invalid git URL provided for tenant " + tenantId);
        }
    }

    /**
     * Initializes SSH authentication
     */
    private static void initSSHAuthentication () {

        SshSessionFactory.setInstance(new CustomJschConfigSessionFactory());
    }

    /**
     * Caches RepositoryContext against tenant repository path
     *
     * @param tenantId tenant repository path
     * @param gitRepoCtx RepositoryContext instance for tenant
     */
    private static void cacheGitRepoContext(int tenantId, RepositoryContext gitRepoCtx) {

    	log.info("caching repo context....");
        tenantToRepoContextMap.put(tenantId, gitRepoCtx);
    }

    /**
     * Retrieve cached RepositoryContext relevant to the tenant's local repo path
     *
     * @param tenantLocalRepoPath tenant's local repository path
     *
     * @return corresponding RepositoryContext instance for the
     * tenant's local repo if available, else null
     */
    private static RepositoryContext retrieveCachedGitContext (int tenantId) {

        return tenantToRepoContextMap.get(tenantId);
    }

    /**
     * Commits any changes in the local repository to the relevant remote repository
     *
     * @return
     *      
     */
    public static boolean commit() {

    	// foreach tenant's local path check for commit
    	// TODO implement later, this is applicable for management node.
    	//log.info(" In commit.... map count : " + tenantToRepoContextMap.size());
    	
		for (Entry<Integer, RepositoryContext> tenantMap : tenantToRepoContextMap
				.entrySet()) {

			int tenantId = tenantMap.getKey();
			//log.info("map count has values..tenant Id : " + tenantId);
			
			RepositoryContext gitRepoCtx = retrieveCachedGitContext(tenantId);
			if (gitRepoCtx == null) {
				
					log.info("No git repository context information found for tenant "
							+ tenantId);

				return false;
			}

			Git git = gitRepoCtx.getGit();
			StatusCommand statusCmd = git.status();
			Status status = null;
			try {
				status = statusCmd.call();

			} catch (GitAPIException e) {
				log.error(
						"Git status operation for tenant "
								+ gitRepoCtx.getTenantId() + " failed, ", e);
				return false;
			}
			//log.info("status : " + status.toString());
			if (status.isClean()) {// no changes, nothing to commit
				
					log.info("No changes detected in the local repository for tenant "
							+ tenantId);
				return false;
			}
			
			addArtifacts(gitRepoCtx, getNewArtifacts(status));
			addArtifacts(gitRepoCtx, getModifiedArtifacts(status));
			removeArtifacts(gitRepoCtx, getRemovedArtifacts(status));
			commitToLocalRepo(gitRepoCtx);
			pushToRemoteRepo(gitRepoCtx);

			return false;

		}
		return false;
    }

    /**
     * Returns the newly added artifact set relevant to the current status of the repository
     *
     * @param status git status
     *
     * @return artifact names set
     */
    private static Set<String> getNewArtifacts (Status status) {

        return status.getUntracked();
    }

    /**
     * Returns the removed (undeployed) artifact set relevant to the current status of the repository
     *
     * @param status git status
     *
     * @return artifact names set
     */
    private static Set<String> getRemovedArtifacts (Status status) {

        return status.getMissing();
    }

    /**
     * Return the modified artifacts set relevant to the current status of the repository
     *
     * @param status git status
     *
     * @return artifact names set
     */
    private static Set<String> getModifiedArtifacts (Status status) {

        return status.getModified();
    }

    /**
     * Adds the artifacts to the local staging area
     *
     * @param gitRepoCtx RepositoryContext instance
     * @param artifacts set of artifacts
     */
    private static void addArtifacts (RepositoryContext gitRepoCtx, Set<String> artifacts) {

        if(artifacts.isEmpty())
            return;

        AddCommand addCmd = gitRepoCtx.getGit().add();
        Iterator<String> it = artifacts.iterator();
        while(it.hasNext())
            addCmd.addFilepattern(it.next());

        try {
            addCmd.call();

        } catch (GitAPIException e) {
            log.error("Adding artifact to the local repository at " + gitRepoCtx.getGitLocalRepoPath() + "failed", e);
            e.printStackTrace();
        }
    }

    /**
     * Removes the set of artifacts from local repo
     *
     * @param gitRepoCtx RepositoryContext instance
     * @param artifacts Set of artifact names to remove
     */
    private static void removeArtifacts (RepositoryContext gitRepoCtx, Set<String> artifacts) {

        if(artifacts.isEmpty())
            return;

        RmCommand rmCmd = gitRepoCtx.getGit().rm();
        Iterator<String> it = artifacts.iterator();
        while (it.hasNext()) {
            rmCmd.addFilepattern(it.next());
        }

        try {
            rmCmd.call();

        } catch (GitAPIException e) {
            log.error("Removing artifact from the local repository at " + gitRepoCtx.getGitLocalRepoPath() + "failed", e);
            e.printStackTrace();
        }
    }

    /**
     * Commits changes for a tenant to relevant the local repository
     *
     * @param gitRepoCtx RepositoryContext instance for the tenant
     */
    private static void commitToLocalRepo (RepositoryContext gitRepoCtx) {

        CommitCommand commitCmd = gitRepoCtx.getGit().commit();
        commitCmd.setMessage("tenant " + gitRepoCtx.getTenantId() + "'s artifacts committed to local repo at " +
                gitRepoCtx.getGitLocalRepoPath());

        try {
            commitCmd.call();

        } catch (GitAPIException e) {
            log.error("Committing artifacts to local repository failed for tenant " + gitRepoCtx.getTenantId(), e);
            e.printStackTrace();
        }
    }

    /**
     * Pushes the artifacts of the tenant to relevant remote repository
     *
     * @param gitRepoCtx RepositoryContext instance for the tenant
     */
    private static void pushToRemoteRepo(RepositoryContext gitRepoCtx) {

        PushCommand pushCmd = gitRepoCtx.getGit().push();
        if(!gitRepoCtx.getKeyBasedAuthentication()) {
            UsernamePasswordCredentialsProvider credentialsProvider = createCredentialsProvider(gitRepoCtx);
            if (credentialsProvider != null)
                pushCmd.setCredentialsProvider(credentialsProvider);
        }

        try {
            pushCmd.call();

        } catch (GitAPIException e) {
            log.error("Pushing artifacts to remote repository failed for tenant " + gitRepoCtx.getTenantId(), e);
            e.printStackTrace();
        }
    }

    public static boolean checkout(RepositoryInformation repositoryInformation) {

    	log.info("In checkout............");
    	
    	int tenantId = Integer.parseInt(repositoryInformation.getTenantId());
    	
    	// if context for tenant is not initialized
    	if(tenantToRepoContextMap.get(tenantId) == null)
	    	initGitContext(repositoryInformation);
    	
        
		RepositoryContext gitRepoCtx = retrieveCachedGitContext(tenantId);
        if(gitRepoCtx == null) { //to handle super tenant scenario
           // if(log.isDebugEnabled())
                log.info("No git repository context information found for deployment synchronizer ");

            return true;
        }

        /*if(gitRepoCtx.getTenantId() == GitDeploymentSynchronizerConstants.SUPER_TENANT_ID)
            return true;  */
        if(!gitRepoCtx.cloneExists())
            cloneRepository(gitRepoCtx);

        return pullArtifacts(gitRepoCtx);
    }
    
    
    
    public static boolean cloneExists(RepositoryInformation repositoryInformation) {
    	
    	int tenantId = Integer.parseInt(repositoryInformation.getTenantId());
    	
    	// if context for tenant is not initialized
    	if(tenantToRepoContextMap.get(tenantId) == null)
	    	initGitContext(repositoryInformation);
    	
        
		RepositoryContext gitRepoCtx = retrieveCachedGitContext(tenantId);
        if(gitRepoCtx == null) { 
            return false;
        }

        /*if(gitRepoCtx.getTenantId() == GitDeploymentSynchronizerConstants.SUPER_TENANT_ID)
            return true;  */
        return gitRepoCtx.cloneExists();
    }

    /**
     * Pulling if any updates are available in the remote git repository. If basic authentication is required,
     * will call 'RepositoryInformationService' for credentials.
     *
     * @param gitRepoCtx RepositoryContext instance for tenant
     *
     * @return true if success, else false
     */
    private static boolean pullArtifacts (RepositoryContext gitRepoCtx) {

    	log.info("Pullingg.....");
        PullCommand pullCmd = gitRepoCtx.getGit().pull();

        if(!gitRepoCtx.getKeyBasedAuthentication()) {
            UsernamePasswordCredentialsProvider credentialsProvider = createCredentialsProvider(gitRepoCtx);
            if (credentialsProvider != null)
                pullCmd.setCredentialsProvider(credentialsProvider);
        }

        try {
            pullCmd.call();

        } catch (InvalidConfigurationException e) {
            log.warn("Git pull unsuccessful for tenant " + gitRepoCtx.getTenantId() + ", " + e.getMessage());
            //handleInvalidConfigurationError(gitRepoCtx);
            //return false;
            Utilities.deleteFolderStructure(new File(gitRepoCtx.getGitLocalRepoPath()));
            cloneRepository(gitRepoCtx);
            return true;

        } catch (JGitInternalException e) {
            log.warn("Git pull unsuccessful for tenant " + gitRepoCtx.getTenantId() + ", " + e.getMessage());
            return false;

        } catch (TransportException e) {
            log.error("Accessing remote git repository " + gitRepoCtx.getGitRemoteRepoUrl() + " failed for tenant " + gitRepoCtx.getTenantId(), e);
            e.printStackTrace();
            return false;

        } catch (CheckoutConflictException e) { //TODO: handle conflict efficiently. Currently the whole directory is deleted and re-cloned
            log.warn("Git pull for the path " + e.getConflictingPaths().toString() + " failed due to conflicts");
            Utilities.deleteFolderStructure(new File(gitRepoCtx.getGitLocalRepoPath()));
            cloneRepository(gitRepoCtx);
            return true;

        } catch (GitAPIException e) {
            log.error("Git pull operation for tenant " + gitRepoCtx.getTenantId() + " failed", e);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Handles the Invalid configuration issues
     *
     * @param gitRepoCtx RepositoryContext instance of the tenant
     */
    private void handleInvalidConfigurationError (RepositoryContext gitRepoCtx) {

        StoredConfig storedConfig = gitRepoCtx.getLocalRepo().getConfig();
        boolean modifiedConfig = false;
        if(storedConfig != null) {

            if(storedConfig.getString("branch", "master", "remote") == null ||
                    storedConfig.getString("branch", "master", "remote").isEmpty()) {

                storedConfig.setString("branch", "master", "remote", "origin");
                modifiedConfig = true;
            }

            if(storedConfig.getString("branch", "master", "merge") == null ||
                    storedConfig.getString("branch", "master", "merge").isEmpty()) {

                storedConfig.setString("branch", "master", "merge", "refs/heads/master");
                modifiedConfig = true;
            }

            if(modifiedConfig) {
                try {
                    storedConfig.save();
                   // storedConfig.load();

                } catch (IOException e) {
                    log.error("Error saving git configuration file in local repo at " + gitRepoCtx.getGitLocalRepoPath(), e);
                    e.printStackTrace();

                } /*catch (ConfigInvalidException e) {
                    log.error("Invalid configurations in local repo at " + gitRepoCtx.getGitLocalRepoPath(), e);
                    e.printStackTrace();
                }   */
            }
        }
    }

    /**
     * Clones the remote repository to the local one. If basic authentication is required,
     * will call 'RepositoryInformationService' for credentials.
     *
     * @param gitRepoCtx RepositoryContext for the tenant
     */
    private static void cloneRepository (RepositoryContext gitRepoCtx) { //should happen only at the beginning

        File gitRepoDir = new File(gitRepoCtx.getGitLocalRepoPath());
        if (gitRepoDir.exists()) {
            if(isValidGitRepo(gitRepoCtx)) { //check if a this is a valid git repo
                log.info("Existing git repository detected for tenant " + gitRepoCtx.getTenantId() + ", no clone required");
                gitRepoCtx.setCloneExists(true);
                return;
            }
            else {
                if(log.isDebugEnabled())
                    log.debug("Repository for tenant " + gitRepoCtx.getTenantId() + " is not a valid git repo");
                Utilities.deleteFolderStructure(gitRepoDir); //if not a valid git repo but non-empty, delete it (else the clone will not work)
            }
        }

        CloneCommand cloneCmd =  gitRepoCtx.getGit().cloneRepository().
                        setURI(gitRepoCtx.getGitRemoteRepoUrl()).
                        setDirectory(gitRepoDir);

        if(!gitRepoCtx.getKeyBasedAuthentication()) {
            UsernamePasswordCredentialsProvider credentialsProvider = createCredentialsProvider(gitRepoCtx);
            if (credentialsProvider != null)
                cloneCmd.setCredentialsProvider(credentialsProvider);
        }

        try {
            cloneCmd.call();
            log.info("Git clone operation for tenant " + gitRepoCtx.getTenantId() + " successful");
            gitRepoCtx.setCloneExists(true);

        } catch (TransportException e) {
            log.error("Accessing remote git repository failed for tenant " + gitRepoCtx.getTenantId(), e);
            e.printStackTrace();

        } catch (GitAPIException e) {
            log.error("Git clone operation for tenant " + gitRepoCtx.getTenantId() + " failed", e);
            e.printStackTrace();
        }
    }

    /**
     * Queries the RepositoryInformationService to obtain credentials for the tenant id + cartridge type
     * and creates a UsernamePasswordCredentialsProvider from a valid username and a password
     *
     * @param gitRepoCtx RepositoryContext instance
     *
     * @return UsernamePasswordCredentialsProvider instance or null if service invocation failed or
     * username/password is not valid
     */
    private static UsernamePasswordCredentialsProvider createCredentialsProvider (RepositoryContext gitRepoCtx) {

        //RepositoryCredentials repoCredentials = null;
        // TODO - set repo creds using the received message
        //repoCredentials = new RepositoryCredentials();
        
        /*try {
            repoCredentials = gitRepoCtx.getRepoInfoServiceClient().
                    getJsonRepositoryInformation(gitRepoCtx.getTenantId(), cartridgeShortName);

        } catch (Exception e) {
            log.error("Git json repository information query failed", e);
            return null;
        }*/

		/*if (repoCredentials != null) {
			String userName = repoCredentials.getUserName();
			String password = repoCredentials.getPassword();

            log.info("Recieved repo url [" + repoCredentials.getUrl() + "] for tenant " + gitRepoCtx.getTenantId() +
                    ", username " + userName);

			if (userName!= null && password != null) {
				return new UsernamePasswordCredentialsProvider(userName, password);
			}
		}*/

        return new UsernamePasswordCredentialsProvider(gitRepoCtx.getRepoUsername(), gitRepoCtx.getRepoPassword());
    }

    /**
     * Checks if an existing local repository is a valid git repository
     *
     * @param gitRepoCtx RepositoryContext instance
     *
     * @return true if a valid git repo, else false
     */
    private static boolean isValidGitRepo (RepositoryContext gitRepoCtx) {

        for (Ref ref : gitRepoCtx.getLocalRepo().getAllRefs().values()) { //check if has been previously cloned successfully, not empty
            if (ref.getObjectId() == null)
                continue;
            return true;
        }

        return false;
    }

    /**
     * Calls a utility method to extract the username from a json string
     *
     * @param repoInfoJsonString json format string
     *
     * @return username if exists, else an empty String
     */
    private String getUserName (String repoInfoJsonString) {
        return Utilities.getMatch(repoInfoJsonString,
                GitDeploymentSynchronizerConstants.USERNAME_REGEX, 1);
    }

    /**
     * Calls a utility method to extract the password from a json string
     *
     * @param repoInfoJsonString json format string
     *
     * @return password if exists, else an empty String
     */
    private String getPassword (String repoInfoJsonString) {
         return Utilities.getMatch(repoInfoJsonString,
                 GitDeploymentSynchronizerConstants.PASSWORD_REGEX, 1);
    }

  /*  public void initAutoCheckout(boolean b) throws Exception {

    }*/

    public void cleanupAutoCheckout() {

    }

    public String getRepositoryType() {

        return /*DeploymentSynchronizerConstants.REPOSITORY_TYPE_GIT;*/null;
    }

   /* public List<RepositoryConfigParameter> getParameters() {

        return null;
    }*/

    //public boolean update(String rootPath, String filePath, int depth) throws DeploymentSynchronizerException {

    	// TODO - implemetn later
    	
        /*RepositoryContext gitRepoCtx = retrieveCachedGitContext(filePath);
        if(gitRepoCtx == null) {
            if(log.isDebugEnabled())
                log.debug("No git repository context information found for deployment synchonizer at " + filePath);

            return false;
        }
        if(gitRepoCtx.getTenantId() == GitDeploymentSynchronizerConstants.SUPER_TENANT_ID)
            return true; //Super Tenant is inactive
        if(gitRepoCtx.cloneExists())
            return pullArtifacts(gitRepoCtx);*/

     /*   return false;
    }*/

	

}

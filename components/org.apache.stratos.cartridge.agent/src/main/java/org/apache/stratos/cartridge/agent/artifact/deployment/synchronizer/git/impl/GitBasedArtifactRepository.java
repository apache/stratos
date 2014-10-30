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

package org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer.git.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.CartridgeAgent;
import org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer.RepositoryInformation;
import org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer.git.internal.CustomJschConfigSessionFactory;
import org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer.git.internal.GitDeploymentSynchronizerConstants;
import org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer.git.internal.RepositoryContext;
import org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer.git.util.Utilities;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.extensions.ExtensionHandler;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentConstants;
import org.apache.stratos.cartridge.agent.util.ExtensionUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.apache.commons.io.*;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Git based artifact repository.
 */
public class GitBasedArtifactRepository {

    private static final int SUPER_TENANT_ID = -1234;

    private static final Log log = LogFactory.getLog(GitBasedArtifactRepository.class);
    private final ExtensionHandler extensionHandler;

    //Map to keep track of git context per tenant (remote urls, jgit git objects, etc.)
    private static ConcurrentHashMap<Integer, RepositoryContext>
            tenantToRepoContextMap = new ConcurrentHashMap<Integer, RepositoryContext>();
    private static volatile GitBasedArtifactRepository gitBasedArtifactRepository;
    private static String SUPER_TENANT_REPO_PATH = "/repository/deployment/server/";
    private static String TENANT_REPO_PATH = "/repository/tenants/";

    private GitBasedArtifactRepository() {
        extensionHandler = CartridgeAgent.getExtensionHandler();
    }

    public static GitBasedArtifactRepository getInstance() {

        if (gitBasedArtifactRepository == null) {
            synchronized (GitBasedArtifactRepository.class) {
                if (gitBasedArtifactRepository == null) {
                    gitBasedArtifactRepository = new GitBasedArtifactRepository();
                }
            }
        }
        return gitBasedArtifactRepository;
    }

    /**
     * initializes and populates the git context with relevant data
     *
     * @param repositoryInformation id of the tenant
     */
    private void initGitContext(RepositoryInformation repositoryInformation) {


        log.info("Initializing git context.");

        int tenantId = Integer.parseInt(repositoryInformation.getTenantId());
        String gitLocalRepoPath = repositoryInformation.getRepoPath();
        RepositoryContext gitRepoCtx = new RepositoryContext();
        String gitRemoteRepoUrl = repositoryInformation.getRepoUrl();
        boolean isMultitenant = repositoryInformation.isMultitenant();

        log.info("local path " + gitLocalRepoPath);
        log.info("remote url " + gitRemoteRepoUrl);
        log.info("tenant " + tenantId);

        gitRepoCtx.setTenantId(tenantId);
        gitRepoCtx.setGitLocalRepoPath(getRepoPathForTenantId(tenantId, gitLocalRepoPath, isMultitenant));
        gitRepoCtx.setGitRemoteRepoUrl(gitRemoteRepoUrl);

        gitRepoCtx.setRepoUsername(repositoryInformation.getRepoUsername());
        gitRepoCtx.setRepoPassword(repositoryInformation.getRepoPassword());

        try {
            if (isKeyBasedAuthentication(gitRemoteRepoUrl, tenantId)) {
                gitRepoCtx.setKeyBasedAuthentication(true);
                initSSHAuthentication();
            } else
                gitRepoCtx.setKeyBasedAuthentication(false);
        } catch (Exception e1) {
            log.error("Exception occurred.. " + e1.getMessage(), e1);
        }

        FileRepository localRepo = null;
        try {
            // localRepo = new FileRepository(new File(gitLocalRepoPath + "/.git"));
            // Fixing STRATOS-380
            localRepo = new FileRepository(new File(gitRepoCtx.getGitLocalRepoPath() + "/.git"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        gitRepoCtx.setLocalRepo(localRepo);
        gitRepoCtx.setGit(new Git(localRepo));
        gitRepoCtx.setCloneExists(false);

        cacheGitRepoContext(tenantId, gitRepoCtx);
    }


    // If tenant id is "-1234", then its super tenant, else tenant
    private static String getRepoPathForTenantId(int tenantId,
                                                 String gitLocalRepoPath, boolean isMultitenant) {


        StringBuilder repoPathBuilder = new StringBuilder();
        String repoPath = null;

        if (isMultitenant) {
            if (tenantId == SUPER_TENANT_ID) {
                //check if the relevant path is set as a startup param
                String superTenantRepoPath = CartridgeAgentConfiguration.getInstance().getSuperTenantRepositoryPath();

                if (superTenantRepoPath != null && !superTenantRepoPath.isEmpty()) {
                    superTenantRepoPath = superTenantRepoPath.startsWith("/") ? superTenantRepoPath : "/".concat(superTenantRepoPath);
                    repoPathBuilder.append(gitLocalRepoPath).append(superTenantRepoPath);

                } else {
                    repoPathBuilder.append(gitLocalRepoPath).append(SUPER_TENANT_REPO_PATH);
                }
            } else {
                // create folder with tenant id
                createTenantDir(tenantId, gitLocalRepoPath);
                //check if the relevant path is set as a startup param
                String tenantRepoPath = CartridgeAgentConfiguration.getInstance().getTenantRepositoryPath();

                if (tenantRepoPath != null && !tenantRepoPath.isEmpty()) {
                    tenantRepoPath = tenantRepoPath.startsWith("/") ? tenantRepoPath : "/".concat(tenantRepoPath);
                    tenantRepoPath = tenantRepoPath.endsWith("/") ? tenantRepoPath : tenantRepoPath.concat("/");

                    repoPathBuilder.append(gitLocalRepoPath).append(tenantRepoPath).append(tenantId);
                } else {
                    repoPathBuilder.append(gitLocalRepoPath).append(TENANT_REPO_PATH).append(tenantId);
                }
            }

            repoPath = repoPathBuilder.toString();
        } else {
            repoPath = gitLocalRepoPath;
        }
        log.info("Repo path returned : " + repoPath);
        return repoPath;
    }

    private static void createTenantDir(int tenantId, String path) {
        String dirPathName = path + TENANT_REPO_PATH + tenantId;
        boolean dirStatus = new File(dirPathName).mkdir();
        if (dirStatus) {
            log.info("Successfully created directory [" + dirPathName + "] ");
        } else {
            log.error("Directory creating failed in [" + dirPathName + "] ");
        }
    }


    /**
     * Checks if key based authentication (SSH) is required
     *
     * @param url      git repository url for the tenant
     * @param tenantId id of the tenant
     * @return true if SSH authentication is required, else false
     */
    private boolean isKeyBasedAuthentication(String url, int tenantId) {

        if (url.startsWith(GitDeploymentSynchronizerConstants.GIT_HTTP_REPO_URL_PREFIX) ||
                url.startsWith(GitDeploymentSynchronizerConstants.GIT_HTTPS_REPO_URL_PREFIX)) {//http or https url
            // authentication with username and password, not key based
            return false;
        } else if (url.startsWith(GitDeploymentSynchronizerConstants.GITHUB_READ_ONLY_REPO_URL_PREFIX)) { //github read-only repo url
            // no authentication required
            return false;
        } else if (url.startsWith(GitDeploymentSynchronizerConstants.GIT_REPO_SSH_URL_PREFIX) ||
                url.contains(GitDeploymentSynchronizerConstants.GIT_REPO_SSH_URL_SUBSTRING)) { //other repo, needs ssh authentication
            // key based authentication
            return true;
        } else {
            log.error("Invalid git URL provided for tenant " + tenantId);
            throw new RuntimeException("Invalid git URL provided for tenant " + tenantId);
        }
    }

    /**
     * Initializes SSH authentication
     */
    private void initSSHAuthentication() {

        SshSessionFactory.setInstance(new CustomJschConfigSessionFactory());
    }

    /**
     * Caches RepositoryContext against tenant repository path
     *
     * @param tenantId   tenant repository path
     * @param gitRepoCtx RepositoryContext instance for tenant
     */
    private void cacheGitRepoContext(int tenantId, RepositoryContext gitRepoCtx) {

        log.info("caching repo context");

    tenantToRepoContextMap.put(tenantId, gitRepoCtx);
}
    /**
     * Retrieve cached RepositoryContext relevant to the tenant's local repo path
     *
     * @param tenantId
     * @return corresponding RepositoryContext instance for the
     * tenant's local repo if available, else null
     */
    private RepositoryContext retrieveCachedGitContext(int tenantId) {

        return tenantToRepoContextMap.get(tenantId);
    }

    private void removeGitRepoContext(int tenantId) {
        tenantToRepoContextMap.remove(tenantId);
    }

    /**
     * Commits any changes in the local repository to the relevant remote repository
     *
     * @return
     */
    public void commit(RepositoryInformation repoInfo) {
        // TODO implement later, this is applicable for management node.

       // for (Entry<Integer, RepositoryContext> tenantMap : tenantToRepoContextMap
       //         .entrySet()) {

            int tenantId = Integer.parseInt(repoInfo.getTenantId());
            //log.info("map count has values..tenant Id : " + tenantId);

            RepositoryContext gitRepoCtx = retrieveCachedGitContext(tenantId);
            Git git = gitRepoCtx.getGit();
            StatusCommand statusCmd = git.status();
            Status status = null;
            try {
                status = statusCmd.call();

            } catch (GitAPIException e) {
                log.error(
                        "Git status operation for tenant "
                                + gitRepoCtx.getTenantId() + " failed, ", e);

            }
            //log.info("status : " + status.toString());
            if (status.isClean()) {// no changes, nothing to commit
                if (log.isDebugEnabled()) {
                    log.debug("No changes detected in the local repository for tenant " + tenantId);
                }

                return;
            }

            addArtifacts(gitRepoCtx, getNewArtifacts(status));
            addArtifacts(gitRepoCtx, getModifiedArtifacts(status));
            removeArtifacts(gitRepoCtx, getRemovedArtifacts(status));
            commitToLocalRepo(gitRepoCtx);
            pushToRemoteRepo(gitRepoCtx);

        //}
        //return false;
    }

    /**
     * Returns the newly added artifact set relevant to the current status of the repository
     *
     * @param status git status
     * @return artifact names set
     */
    private Set<String> getNewArtifacts(Status status) {

        return status.getUntracked();
    }

    /**
     * Returns the removed (undeployed) artifact set relevant to the current status of the repository
     *
     * @param status git status
     * @return artifact names set
     */
    private Set<String> getRemovedArtifacts(Status status) {

        return status.getMissing();
    }

    /**
     * Return the modified artifacts set relevant to the current status of the repository
     *
     * @param status git status
     * @return artifact names set
     */
    private Set<String> getModifiedArtifacts(Status status) {

        return status.getModified();
    }

    /**
     * Adds the artifacts to the local staging area
     *
     * @param gitRepoCtx RepositoryContext instance
     * @param artifacts  set of artifacts
     */
    private void addArtifacts(RepositoryContext gitRepoCtx, Set<String> artifacts) {

        if (artifacts.isEmpty())
            return;

        AddCommand addCmd = gitRepoCtx.getGit().add();
        Iterator<String> it = artifacts.iterator();
        while (it.hasNext())
            addCmd.addFilepattern(it.next());

        try {
            addCmd.call();
            if (log.isDebugEnabled()) {
                log.debug("Added artifacts for tenant : " + gitRepoCtx.getTenantId());
            }

        } catch (GitAPIException e) {
            log.error("Adding artifact to the local repository at " + gitRepoCtx.getGitLocalRepoPath() + "failed", e);
            log.error(e);
        }

    }

    /**
     * Removes the set of artifacts from local repo
     *
     * @param gitRepoCtx RepositoryContext instance
     * @param artifacts  Set of artifact names to remove
     */
    private void removeArtifacts(RepositoryContext gitRepoCtx, Set<String> artifacts) {

        if (artifacts.isEmpty())
            return;

        RmCommand rmCmd = gitRepoCtx.getGit().rm();
        Iterator<String> it = artifacts.iterator();
        while (it.hasNext()) {
            rmCmd.addFilepattern(it.next());
        }

        try {
            rmCmd.call();
            if (log.isDebugEnabled()) {
                log.debug("Removed artifacts for tenant : " + gitRepoCtx.getTenantId());
            }

        } catch (GitAPIException e) {
            log.error("Removing artifact from the local repository at " + gitRepoCtx.getGitLocalRepoPath() + "failed", e);
            log.error(e);
        }
    }

    /**
     * Commits changes for a tenant to relevant the local repository
     *
     * @param gitRepoCtx RepositoryContext instance for the tenant
     */
    private void commitToLocalRepo(RepositoryContext gitRepoCtx) {

        CommitCommand commitCmd = gitRepoCtx.getGit().commit();
        commitCmd.setMessage("tenant " + gitRepoCtx.getTenantId() + "'s artifacts committed to local repo at " +
                gitRepoCtx.getGitLocalRepoPath());

        try {
            commitCmd.call();
            if (log.isDebugEnabled()) {
                log.debug("Committed artifacts for tenant : " + gitRepoCtx.getTenantId());
            }

        } catch (GitAPIException e) {
            log.error("Committing artifacts to local repository failed for tenant " + gitRepoCtx.getTenantId(), e);
        }
    }

    /**
     * Pushes the artifacts of the tenant to relevant remote repository
     *
     * @param gitRepoCtx RepositoryContext instance for the tenant
     */
    private void pushToRemoteRepo(RepositoryContext gitRepoCtx) {

        PushCommand pushCmd = gitRepoCtx.getGit().push();
        if (!gitRepoCtx.getKeyBasedAuthentication()) {
            UsernamePasswordCredentialsProvider credentialsProvider = createCredentialsProvider(gitRepoCtx);
            if (credentialsProvider != null)
                pushCmd.setCredentialsProvider(credentialsProvider);
        }

        try {
            pushCmd.call();
            if (log.isDebugEnabled()) {
                log.debug("Pushed artifacts for tenant : " + gitRepoCtx.getTenantId());
            }

        } catch (GitAPIException e) {
            log.error("Pushing artifacts to remote repository failed for tenant " + gitRepoCtx.getTenantId(), e);

        }
    }


   public boolean checkout (RepositoryInformation repositoryInformation) throws Exception {

       int tenantId = Integer.parseInt(repositoryInformation.getTenantId());

       // if context for tenant is not initialized
       if (tenantToRepoContextMap.get(tenantId) == null) {
           initGitContext(repositoryInformation);
       }

       RepositoryContext gitRepoCtx = retrieveCachedGitContext(tenantId);

       File gitRepoDir = new File(gitRepoCtx.getGitLocalRepoPath());
       if (!gitRepoDir.exists()) {
           return cloneRepository(gitRepoCtx);
       }
       else {
           if (isValidGitRepo(gitRepoCtx)) {
               if (log.isDebugEnabled()) {
                   log.debug("Existing git repository detected for tenant " + gitRepoCtx.getTenantId() + ", no clone required");
               }

               return pullAndHandleErrors(gitRepoCtx);

           } else {
               // not a valid git repo, check if the directory is non-empty
               if (gitRepoDir.list().length > 0) {
                   // directory is non empty. sync existing artifacts with the remote repository
                   if (syncInitialLocalArtifacts(gitRepoCtx)) {
                       log.info("Existing local artifacts for tenant [" + gitRepoCtx.getTenantId() + "] synchronized with remote repository successfully");
                       // pull any changes from the remote repo
                       return pullAndHandleErrors(gitRepoCtx);
                   }
                   return false;

               } else {
                   // directory is empty, clone
                   return cloneRepository(gitRepoCtx);
               }
           }
       }
   }

    public boolean removeRepo(int tenantId) throws IOException {
        RepositoryContext gitRepoCtx = retrieveCachedGitContext(tenantId);

        log.info("git repository deleted for tenant " + gitRepoCtx.getTenantId());

        // Stop the artifact update task
        gitRepoCtx.getArtifactSyncSchedular().shutdown();
        // Remove git repo for the tenant
        FileUtils.deleteDirectory(gitRepoCtx.getLocalRepo().getDirectory());
        FileUtils.deleteDirectory(new File(gitRepoCtx.getGitLocalRepoPath()));

        removeGitRepoContext(tenantId);

        if (tenantId == -1234) {
            if (CartridgeAgentConfiguration.getInstance().isMultitenant()) {
                ExtensionUtils.executeCopyArtifactsExtension(
                        CartridgeAgentConstants.SUPERTENANT_TEMP_PATH,
                        CartridgeAgentConfiguration.getInstance().getAppPath() + "/repository/deployment/server/"
                        );
            }
        }


        return true;
    }

    private boolean pullAndHandleErrors (RepositoryContext gitRepoCtx) {

        try {
            return pullArtifacts(gitRepoCtx);

        } catch (CheckoutConflictException e) {
            // checkout from remote HEAD
            return checkoutFromRemoteHead(gitRepoCtx, e.getConflictingPaths());
            // pull again
            /*try {
                return pullArtifacts(gitRepoCtx);

            } catch (GitAPIException e1) {
                //cannot happen here
                log.error("Git pull failed for tenant " + gitRepoCtx.getTenantId(), e1);
                return false;
            }*/
        }
    }

    private boolean checkoutFromRemoteHead(RepositoryContext gitRepoCtx, List<String> paths) {

        boolean checkoutSuccess = false;

        CheckoutCommand checkoutCmd = gitRepoCtx.getGit().checkout();
        for(String path : paths) {
            checkoutCmd.addPath(path);
            if(log.isDebugEnabled()) {
                log.debug("Added the file path " + path + " to checkout from the remote repository");
            }
        }
        // specify the start point as the HEAD of remote repository
        checkoutCmd.setStartPoint(GitDeploymentSynchronizerConstants.REMOTES_ORIGIN_MASTER);

        try {
            checkoutCmd.call();
            checkoutSuccess = true;
            log.info("Checked out the conflicting files from the remote repository successfully");

        } catch (GitAPIException e) {
            log.error("Checking out artifacts from index failed", e);
        }

        return checkoutSuccess;
    }

    private void resetToRemoteHead (RepositoryContext gitRepoCtx, List<String> paths) {

        ResetCommand resetCmd = gitRepoCtx.getGit().reset();

        // reset type is HARD, to remote master branch
        resetCmd.setMode(ResetCommand.ResetType.HARD).
                setRef(GitDeploymentSynchronizerConstants.ORIGIN + "/" + GitDeploymentSynchronizerConstants.MASTER);

        // add paths
        for(String path : paths) {
            resetCmd.addPath(path);
            if(log.isDebugEnabled()) {
                log.debug("Added the file path " + path + " to reset");
            }
        }

        try {
            resetCmd.call();
            log.info("Reset the local branch to origin master successfully");

        } catch (GitAPIException e) {
            log.error("Reset to origin master failed", e);
        }

    }

    private boolean syncInitialLocalArtifacts(RepositoryContext gitRepoCtx) throws Exception {

        boolean syncedLocalArtifacts;

        //initialize repository
        InitGitRepository(new File(gitRepoCtx.getGitLocalRepoPath()));
        //add the remote repository (origin)
        syncedLocalArtifacts = addRemote(gitRepoCtx.getLocalRepo(), gitRepoCtx.getGitRemoteRepoUrl());

        return syncedLocalArtifacts;
    }

    public void scheduleSyncTask(RepositoryInformation repoInformation, boolean autoCheckout, boolean autoCommit, long delay) {

        int tenantId = Integer.parseInt(repoInformation.getTenantId());

        RepositoryContext repoCtxt = tenantToRepoContextMap.get(tenantId);
        if (repoCtxt == null) {
            log.error("Unable to schedule artifact sync task, repositoryContext null for tenant " + tenantId);
            return;
        }

        if (repoCtxt.getArtifactSyncSchedular() == null) {
            synchronized (repoCtxt) {
                if (repoCtxt.getArtifactSyncSchedular() == null) {
                    // create a new ScheduledExecutorService instance
                    final ScheduledExecutorService artifactSyncScheduler = Executors.newScheduledThreadPool(1,
                            new ArtifactSyncTaskThreadFactory(repoCtxt.getGitLocalRepoPath()));

                    // schedule at the given interval
                    artifactSyncScheduler.scheduleAtFixedRate(new ArtifactSyncTask(repoInformation, autoCheckout, autoCommit), delay, delay, TimeUnit.SECONDS);
                    // cache
                    repoCtxt.setArtifactSyncSchedular(artifactSyncScheduler);

                    log.info("Scheduled Artifact Synchronization Task for path " + repoCtxt.getGitLocalRepoPath());

                } else {
                    log.info("Artifact Synchronization Task for path " + repoCtxt.getGitLocalRepoPath() + " already scheduled");
                }
            }
        }
    }

    public boolean cloneExists(RepositoryInformation repositoryInformation) {

        int tenantId = Integer.parseInt(repositoryInformation.getTenantId());

        // if context for tenant is not initialized
        if (tenantToRepoContextMap.get(tenantId) == null) {
            initGitContext(repositoryInformation);
        }


        RepositoryContext gitRepoCtx = retrieveCachedGitContext(tenantId);
        if (gitRepoCtx == null) {
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
     * @return true if success, else false
     */
    /*private boolean pullArtifacts(RepositoryContext gitRepoCtx) {
        if (log.isDebugEnabled()) {
            log.debug("Pulling artifacts");
        }
        PullCommand pullCmd = gitRepoCtx.getGit().pull();

        if (!gitRepoCtx.getKeyBasedAuthentication()) {
            UsernamePasswordCredentialsProvider credentialsProvider = createCredentialsProvider(gitRepoCtx);
            if (credentialsProvider != null)
                pullCmd.setCredentialsProvider(credentialsProvider);
        }

        try {
            PullResult pullResult = pullCmd.call();
            // check if we have received any updates
            if (!pullResult.getFetchResult().getTrackingRefUpdates().isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Artifacts were updated as a result of the pull operation, thread: " + Thread.currentThread().getName() + " - " +
                            Thread.currentThread().getId());
                }

                // execute artifact update extension
                extensionHandler.onArtifactUpdateSchedulerEvent(String.valueOf(gitRepoCtx.getTenantId()));
            }

        } catch (InvalidConfigurationException e) {
            log.warn("Git pull unsuccessful for tenant " + gitRepoCtx.getTenantId() + ", " + e.getMessage());
            //handleInvalidConfigurationError(gitRepoCtx);
            //return false;
            Utilities.deleteFolderStructure(new File(gitRepoCtx.getGitLocalRepoPath()));
            cloneRepository(gitRepoCtx);
            // execute artifact update extension
            extensionHandler.onArtifactUpdateSchedulerEvent(String.valueOf(gitRepoCtx.getTenantId()));
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
            // execute artifact update extension
            extensionHandler.onArtifactUpdateSchedulerEvent(String.valueOf(gitRepoCtx.getTenantId()));
            return true;

        } catch (GitAPIException e) {
            log.error("Git pull operation for tenant " + gitRepoCtx.getTenantId() + " failed", e);
            e.printStackTrace();
            return false;
        }
        return true;
    }*/

    private boolean pullArtifacts (RepositoryContext gitRepoCtx) throws CheckoutConflictException {

        PullCommand pullCmd = gitRepoCtx.getGit().pull();

        UsernamePasswordCredentialsProvider credentialsProvider = createCredentialsProvider(gitRepoCtx);

        if (credentialsProvider == null) {
            log.warn ("Remote repository credentials not available for tenant " + gitRepoCtx.getTenantId() +
                    ", aborting pull");
            return false;
        }
        pullCmd.setCredentialsProvider(credentialsProvider);

        try {
            PullResult pullResult = pullCmd.call();
            // check if we have received any updates
            if (!pullResult.getFetchResult().getTrackingRefUpdates().isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Artifacts were updated as a result of the pull operation, thread: " + Thread.currentThread().getName() + " - " +
                            Thread.currentThread().getId());
                }

                // execute artifact update extension
                extensionHandler.onArtifactUpdateSchedulerEvent(String.valueOf(gitRepoCtx.getTenantId()));
            }

        } catch (InvalidConfigurationException e) {
            log.warn("Git pull unsuccessful for tenant " + gitRepoCtx.getTenantId() + ", invalid configuration. " + e.getMessage());
            // FileUtilities.deleteFolderStructure(new File(gitRepoCtx.getLocalRepoPath()));
            //cloneRepository(gitRepoCtx);
            Utilities.deleteFolderStructure(new File(gitRepoCtx.getGitLocalRepoPath()));
            cloneRepository(gitRepoCtx);
            // execute artifact update extension
            extensionHandler.onArtifactUpdateSchedulerEvent(String.valueOf(gitRepoCtx.getTenantId()));
            return true;

        } catch (JGitInternalException e) {
            log.warn("Git pull unsuccessful for tenant " + gitRepoCtx.getTenantId() + ", " + e.getMessage());
            return false;

        } catch (TransportException e) {
            log.error("Accessing remote git repository " + gitRepoCtx.getGitRemoteRepoUrl() + " failed for tenant " + gitRepoCtx.getTenantId(), e);
            return false;

        } catch (CheckoutConflictException e) {
            log.warn("Git pull unsuccessful for tenant " + gitRepoCtx.getTenantId() + ", conflicts detected");
            throw e;

        } catch (GitAPIException e) {
            log.error("Git pull operation for tenant " + gitRepoCtx.getTenantId() + " failed", e);
            return false;
        }

        return true;
    }

    /**
     * Handles the Invalid configuration issues
     *
     * @param gitRepoCtx RepositoryContext instance of the tenant
     */
    private void handleInvalidConfigurationError(RepositoryContext gitRepoCtx) {

        StoredConfig storedConfig = gitRepoCtx.getLocalRepo().getConfig();
        boolean modifiedConfig = false;
        if (storedConfig != null) {

            if (storedConfig.getString("branch", "master", "remote") == null ||
                    storedConfig.getString("branch", "master", "remote").isEmpty()) {

                storedConfig.setString("branch", "master", "remote", "origin");
                modifiedConfig = true;
            }

            if (storedConfig.getString("branch", "master", "merge") == null ||
                    storedConfig.getString("branch", "master", "merge").isEmpty()) {

                storedConfig.setString("branch", "master", "merge", "refs/heads/master");
                modifiedConfig = true;
            }

            if (modifiedConfig) {
                try {
                    storedConfig.save();
                    // storedConfig.load();

                } catch (IOException e) {
                    log.error("Error saving git configuration file in local repo at " + gitRepoCtx.getGitLocalRepoPath(), e);
                    e.printStackTrace();

                }
            }
        }
    }

    /**
     * Clones the remote repository to the local one. If basic authentication is required,
     * will call 'RepositoryInformationService' for credentials.
     *
     * @param gitRepoCtx RepositoryContext for the tenant
     */
    /*private static void cloneRepository(RepositoryContext gitRepoCtx) { //should happen only at the beginning

        File gitRepoDir = new File(gitRepoCtx.getGitLocalRepoPath());
        if (gitRepoDir.exists()) {
            if (isValidGitRepo(gitRepoCtx)) { //check if a this is a valid git repo
                log.info("Existing git repository detected for tenant " + gitRepoCtx.getTenantId() + ", no clone required");
                gitRepoCtx.setCloneExists(true);
                return;
            } else {
                if (log.isDebugEnabled())
                    log.debug("Repository for tenant " + gitRepoCtx.getTenantId() + " is not a valid git repo");
                Utilities.deleteFolderStructure(gitRepoDir); //if not a valid git repo but non-empty, delete it (else the clone will not work)
            }
        }

        CloneCommand cloneCmd = gitRepoCtx.getGit().cloneRepository().
                setURI(gitRepoCtx.getGitRemoteRepoUrl()).
                setDirectory(gitRepoDir);

        if (!gitRepoCtx.getKeyBasedAuthentication()) {
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
    }*/

    private boolean cloneRepository (RepositoryContext gitRepoCtx) { //should happen only at the beginning

        boolean cloneSuccess = false;

        File gitRepoDir = new File(gitRepoCtx.getGitLocalRepoPath());

        CloneCommand cloneCmd =  Git.cloneRepository().
                setURI(gitRepoCtx.getGitRemoteRepoUrl()).
                setDirectory(gitRepoDir).
                setBranch(GitDeploymentSynchronizerConstants.GIT_REFS_HEADS_MASTER);

        UsernamePasswordCredentialsProvider credentialsProvider = createCredentialsProvider(gitRepoCtx);

        if (credentialsProvider == null) {
            log.warn ("Remote repository credentials not available for tenant " + gitRepoCtx.getTenantId() +
                    ", aborting clone");
            return false;
        }
        cloneCmd.setCredentialsProvider(credentialsProvider);

        try {
            cloneCmd.call();
            log.info("Git clone operation for tenant " + gitRepoCtx.getTenantId() + " successful");
            gitRepoCtx.setCloneExists(true);
            cloneSuccess = true;

        } catch (TransportException e) {
            log.error("Accessing remote git repository failed for tenant " + gitRepoCtx.getTenantId(), e);

        } catch (GitAPIException e) {
            log.error("Git clone operation for tenant " + gitRepoCtx.getTenantId() + " failed", e);
        }

        return cloneSuccess;
    }

    /**
     * Queries the RepositoryInformationService to obtain credentials for the tenant id + cartridge type
     * and creates a UsernamePasswordCredentialsProvider from a valid username and a password
     *
     * @param gitRepoCtx RepositoryContext instance
     * @return UsernamePasswordCredentialsProvider instance or null if service invocation failed or
     * username/password is not valid
     */
    private static UsernamePasswordCredentialsProvider createCredentialsProvider(RepositoryContext gitRepoCtx) {
        return new UsernamePasswordCredentialsProvider(gitRepoCtx.getRepoUsername(), gitRepoCtx.getRepoPassword());
    }

    /**
     * Checks if an existing local repository is a valid git repository
     *
     * @param gitRepoCtx RepositoryContext instance
     * @return true if a valid git repo, else false
     */
    private static boolean isValidGitRepo(RepositoryContext gitRepoCtx) {

        // check if has been marked as cloned before
        if(gitRepoCtx.cloneExists()) {
            // repo is valid
            return true;
        }

        for (Ref ref : gitRepoCtx.getLocalRepo().getAllRefs().values()) { //check if has been previously cloned successfully, not empty
            if (ref.getObjectId() == null)
                continue;
            return true;
        }

        return false;
    }

    public static void InitGitRepository (File gitRepoDir) throws Exception {

        try {
            Git.init().setDirectory(gitRepoDir).setBare(false).call();

        } catch (GitAPIException e) {
            String errorMsg = "Initializing local repo at " + gitRepoDir.getPath() + " failed";
            log.error(errorMsg, e);
            throw new Exception(errorMsg, e);
        }
    }

    public static boolean addRemote (Repository repository, String remoteUrl) {

        boolean remoteAdded = false;

        StoredConfig config = repository.getConfig();
        config.setString(GitDeploymentSynchronizerConstants.REMOTE,
                GitDeploymentSynchronizerConstants.ORIGIN,
                GitDeploymentSynchronizerConstants.URL,
                remoteUrl);

        config.setString(GitDeploymentSynchronizerConstants.REMOTE,
                GitDeploymentSynchronizerConstants.ORIGIN,
                GitDeploymentSynchronizerConstants.FETCH,
                GitDeploymentSynchronizerConstants.FETCH_LOCATION);

        config.setString(GitDeploymentSynchronizerConstants.BRANCH,
                GitDeploymentSynchronizerConstants.MASTER,
                GitDeploymentSynchronizerConstants.REMOTE,
                GitDeploymentSynchronizerConstants.ORIGIN);

        config.setString(GitDeploymentSynchronizerConstants.BRANCH,
                GitDeploymentSynchronizerConstants.MASTER,
                GitDeploymentSynchronizerConstants.MERGE,
                GitDeploymentSynchronizerConstants.GIT_REFS_HEADS_MASTER);

        try {
            config.save();
            remoteAdded = true;

        } catch (IOException e) {
            log.error("Error in adding remote origin " + remoteUrl + " for local repository " +
                    repository.toString(), e);
        }

        return remoteAdded;
    }


    public void cleanupAutoCheckout() {

    }

    public String getRepositoryType() {

        return null;
    }

    private class ArtifactSyncTask implements Runnable {

        private RepositoryInformation repositoryInformation;
        private boolean autoCheckout;
        private boolean autoCommit;

        public ArtifactSyncTask(RepositoryInformation repositoryInformation, boolean autoCheckout, boolean autoCommit) {
            this.repositoryInformation = repositoryInformation;
            this.autoCheckout = autoCheckout;
            this.autoCommit = autoCommit;
        }

        @Override
        public void run() {
            try {
                if (autoCheckout) {
                    checkout(repositoryInformation);
                }
            } catch (Exception e) {
                log.error(e);
            }
            if (autoCommit) {
                commit(repositoryInformation);
            }
        }
    }

    class ArtifactSyncTaskThreadFactory implements ThreadFactory {

        private String localRepoPath;

        public ArtifactSyncTaskThreadFactory(String localRepoPath) {
            this.localRepoPath = localRepoPath;
        }

        public Thread newThread(Runnable r) {
            return new Thread(r, "Artifact Update Thread - " + localRepoPath);
        }
    }

}

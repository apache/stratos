# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

from plugins.contracts import IArtifactCheckoutPlugin
from modules.util.log import LogFactory
from modules.util.cartridgeagentutils import Utils
from modules.artifactmgt.git.agentgithandler import AgentGitHandler
from config import Config
import constants
from exception import *
import shutil
import os


class DefaultArtifactCheckout(IArtifactCheckoutPlugin):
    """
    Default implementation for the artifact checkout handling
    """

    def __init__(self):
        super(DefaultArtifactCheckout, self).__init__()
        self.log = LogFactory().get_log(__name__)

    def checkout(self, repo_info):
        """
        Checks out the code from the remote repository.
        If local repository path is empty, a clone operation is done.
        If there is a cloned repository already on the local repository path, a pull operation
        will be performed.
        If there are artifacts not in the repository already on the local repository path,
        they will be added to a git repository, the remote url added as origin, and then
        a pull operation will be performed.

        :param Repository repo_info: The repository information object
        :return: A tuple containing whether it was an initial clone or not, and if the repo was updated on
        subsequent calls or not
        :rtype: tuple(bool, bool)
        """
        new_git_repo = AgentGitHandler.create_git_repo(repo_info)

        # check whether this is the first artifact updated event for this tenant
        existing_git_repo = AgentGitHandler.get_repo(repo_info.tenant_id)
        if existing_git_repo is not None:
            # check whether this event has updated credentials for git repo
            if AgentGitHandler.is_valid_git_repository(
                    new_git_repo) and new_git_repo.repo_url != existing_git_repo.repo_url:
                # add the new git_repo object with updated credentials to repo list
                AgentGitHandler.add_repo(new_git_repo)

                # update the origin remote URL with new credentials
                self.log.info("Changes detected in git credentials for tenant: %s" % new_git_repo.tenant_id)
                (output, errors) = AgentGitHandler.execute_git_command(
                    ["remote", "set-url", "origin", new_git_repo.repo_url], new_git_repo.local_repo_path)
                if errors.strip() != "":
                    self.log.error("Failed to update git repo remote URL for tenant: %s" % new_git_repo.tenant_id)

        git_repo = AgentGitHandler.create_git_repo(repo_info)
        if AgentGitHandler.get_repo(repo_info.tenant_id) is not None:
            # has been previously cloned, this is not the subscription run
            if AgentGitHandler.is_valid_git_repository(git_repo):
                self.log.debug("Executing git pull: [tenant-id] %s [repo-url] %s",
                               git_repo.tenant_id, git_repo.repo_url)
                updated = AgentGitHandler.pull(git_repo)
                self.log.debug("Git pull executed: [tenant-id] %s [repo-url] %s [SUCCESS] %s",
                               git_repo.tenant_id, git_repo.repo_url, updated)
            else:
                # not a valid repository, might've been corrupted. do a re-clone
                self.log.debug("Local repository is not valid. Doing a re-clone to purify.")
                git_repo.cloned = False
                self.log.debug("Executing git clone: [tenant-id] %s [repo-url] %s",
                               git_repo.tenant_id, git_repo.repo_url)

                git_repo = AgentGitHandler.clone(git_repo)
                AgentGitHandler.add_repo(git_repo)
                self.log.debug("Git clone executed: [tenant-id] %s [repo-url] %s",
                               git_repo.tenant_id, git_repo.repo_url)
        else:
            # subscribing run.. need to clone
            self.log.info("Cloning artifacts from %s for the first time to %s",
                          git_repo.repo_url, git_repo.local_repo_path)
            self.log.info("Executing git clone: [tenant-id] %s [repo-url] %s, [repo path] %s",
                          git_repo.tenant_id, git_repo.repo_url, git_repo.local_repo_path)

            if not Config.backup_initial_artifacts is None and Config.backup_initial_artifacts:
                self.check_and_backup_initial_artifacts(git_repo.local_repo_path)

            try:
                git_repo = AgentGitHandler.clone(git_repo)
                AgentGitHandler.add_repo(git_repo)
                self.log.debug("Git clone executed: [tenant-id] %s [repo-url] %s",
                               git_repo.tenant_id, git_repo.repo_url)
            except Exception as e:
                self.log.exception("Git clone operation failed: %s" % e)
                # If first git clone is failed, execute retry_clone operation
                self.log.info("Retrying git clone operation...")
                AgentGitHandler.retry_clone(git_repo)
                AgentGitHandler.add_repo(git_repo)

    def check_and_backup_initial_artifacts(self, initial_artifact_dir):
        """
        verifies if there are any default artifacts by checking the 'initial_artifact_dir' and
        whether its empty, and takes a backup to a directory  initial_artifact_dir_backup in the
        same location

        :param initial_artifact_dir: path to local artifact directory
        """
        # copy default artifacts (if any) to a a temp location
        # if directory name is dir, the backup directory name would be dir_backup
        if self.initial_artifacts_exists(initial_artifact_dir):
            self.log.info("Default artifacts exist at " + initial_artifact_dir)
            self.backup_initial_artifacts(initial_artifact_dir)
        else:
            self.log.info("No default artifacts exist at " + initial_artifact_dir)

    def initial_artifacts_exists(self, dir):
        try:
            return os.path.exists(dir) and os.listdir(dir)
        except OSError as e:
            self.log.error('Unable to check if directory exists | non-empty, error: %s' % e)
            return False

    def backup_initial_artifacts(self, src):
        self.log.info('Initial artifacts exists, taking backup to ' + Utils.strip_trailing_slash(src)
                      + constants.BACKUP_DIR_SUFFIX +
                      ' directory')
        try:
            shutil.copytree(src, Utils.strip_trailing_slash(src) + constants.BACKUP_DIR_SUFFIX)
        except OSError as e:
            self.log.error('Directory not copied. Error: %s' % e)

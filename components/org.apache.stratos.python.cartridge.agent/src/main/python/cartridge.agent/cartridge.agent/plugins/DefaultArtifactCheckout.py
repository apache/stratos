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
from modules.artifactmgt.git.agentgithandler import AgentGitHandler
from config import Config
import constants
from exception import *


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
        git_repo = AgentGitHandler.create_git_repo(repo_info)
        if AgentGitHandler.get_repo(repo_info.tenant_id) is not None:
            # has been previously cloned, this is not the subscription run
            if AgentGitHandler.is_valid_git_repository(git_repo):
                AgentGitHandler.log.debug("Executing git pull: [tenant-id] %s [repo-url] %s",
                                          git_repo.tenant_id, git_repo.repo_url)
                updated = AgentGitHandler.pull(git_repo)
                AgentGitHandler.log.debug("Git pull executed: [tenant-id] %s [repo-url] %s",
                                          git_repo.tenant_id, git_repo.repo_url)
            else:
                # not a valid repository, might've been corrupted. do a re-clone
                AgentGitHandler.log.debug("Local repository is not valid. Doing a re-clone to purify.")
                git_repo.cloned = False
                AgentGitHandler.log.debug("Executing git clone: [tenant-id] %s [repo-url] %s",
                                          git_repo.tenant_id, git_repo.repo_url)
                git_repo = AgentGitHandler.clone(git_repo)
                AgentGitHandler.add_repo(git_repo)
                AgentGitHandler.log.debug("Git clone executed: [tenant-id] %s [repo-url] %s",
                                          git_repo.tenant_id, git_repo.repo_url)
        else:
            # subscribing run.. need to clone
            AgentGitHandler.log.info("Cloning artifacts from %s for the first time to %s",
                                     git_repo.repo_url, git_repo.local_repo_path)
            AgentGitHandler.log.info("Executing git clone: [tenant-id] %s [repo-url] %s, [repo path] %s",
                                     git_repo.tenant_id, git_repo.repo_url, git_repo.local_repo_path)
            try:
                git_repo = AgentGitHandler.clone(git_repo)
                AgentGitHandler.add_repo(git_repo)
                AgentGitHandler.log.debug("Git clone executed: [tenant-id] %s [repo-url] %s",
                                          git_repo.tenant_id, git_repo.repo_url)
            except GitRepositorySynchronizationException as e:
                AgentGitHandler.log.exception("Git clone operation failed: %s" % e)
                # If first git clone is failed, execute retry_clone operation
                AgentGitHandler.log.info("Retrying git clone operation...")
                AgentGitHandler.retry_clone(git_repo)
                AgentGitHandler.add_repo(git_repo)

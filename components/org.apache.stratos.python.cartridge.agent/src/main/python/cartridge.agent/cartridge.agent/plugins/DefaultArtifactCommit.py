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

from exception import *
from git import *
from modules.artifactmgt.git.agentgithandler import AgentGitHandler
from plugins.contracts import IArtifactCommitPlugin

from modules.util.log import LogFactory


class DefaultArtifactCommit(IArtifactCommitPlugin):
    """
    Default implementation for the artifact checkout handling
    """

    def __init__(self):
        super(DefaultArtifactCommit, self).__init__()
        self.log = LogFactory().get_log(__name__)

    def commit(self, repo_info):
        """
        Commits and pushes new artifacts to the remote repository
        :param repo_info:
        :return:
        """
        git_repo = AgentGitHandler.get_repo(repo_info.tenant_id)
        if git_repo is None:
            # not cloned yet
            AgentGitHandler.log.error("Not a valid repository to push from. Aborting Git push...")
            return

        repo = Repo(git_repo.local_repo_path)
        # Get initial HEAD so in case if push fails it can be reverted to this hash
        # This way, commit and push becomes an single operation. No intermediate state will be left behind.
        (init_head, init_errors) = AgentGitHandler.execute_git_command(["rev-parse", "HEAD"], git_repo.local_repo_path)

        # remove trailing new line character, if any
        init_head = init_head.rstrip()

        # stage all untracked files
        if AgentGitHandler.stage_all(git_repo.local_repo_path):
            AgentGitHandler.log.debug("Git staged untracked artifacts successfully")
        else:
            AgentGitHandler.log.error("Git could not stage untracked artifacts")

        # check for changes in working directory
        if not repo.is_dirty():
            AgentGitHandler.log.debug("No changes detected in the local repository for tenant %s" % git_repo.tenant_id)
            return

        # commit to local repository
        commit_message = "tenant [%s]'s artifacts committed to local repo at %s" \
                         % (git_repo.tenant_id, git_repo.local_repo_path)
        # TODO: set configuratble names, check if already configured
        commit_name = git_repo.tenant_id
        commit_email = "author@example.org"
        # git config
        AgentGitHandler.execute_git_command(["config", "user.email", commit_email], git_repo.local_repo_path)
        AgentGitHandler.execute_git_command(["config", "user.name", commit_name], git_repo.local_repo_path)

        # commit
        (output, errors) = AgentGitHandler.execute_git_command(["commit", "-m", commit_message],
                                                               git_repo.local_repo_path)
        if errors.strip() == "":
            commit_hash = AgentGitHandler.find_between(output, "[master", "]").strip()
            AgentGitHandler.log.debug("Committed artifacts for tenant: %s : %s " % (git_repo.tenant_id, commit_hash))
        else:
            AgentGitHandler.log.error("Committing artifacts to local repository failed for tenant: %s, Cause: %s"
                                      % (git_repo.tenant_id, errors))
            # revert to initial commit hash
            AgentGitHandler.execute_git_command(["reset", "--hard", init_head], git_repo.local_repo_path)
            return

        # pull and rebase before pushing to remote repo
        AgentGitHandler.execute_git_command(["pull", "--rebase", "origin", "master"], git_repo.local_repo_path)
        if repo.is_dirty():
            AgentGitHandler.log.error("Git pull operation in commit job left the repository in dirty state")
            AgentGitHandler.log.error(
                "Git pull rebase operation on remote %s for tenant %s failed" % (git_repo.repo_url, git_repo.tenant_id))

            AgentGitHandler.log.warn("The working directory will be reset to the last known good commit")
            # revert to the initial commit
            AgentGitHandler.execute_git_command(["reset", "--hard", init_head], git_repo.local_repo_path)
            return
        else:
            # push to remote
            try:
                push_info_list = repo.remotes.origin.push()
                if (len(push_info_list)) == 0:
                    AgentGitHandler.log.error("Failed to push artifacts to remote repo for tenant: %s remote: %s" %
                                              (git_repo.tenant_id, git_repo.repo_url))
                    # revert to the initial commit
                    AgentGitHandler.execute_git_command(["reset", "--hard", init_head], git_repo.local_repo_path)
                    return

                for push_info in push_info_list:
                    AgentGitHandler.log.debug("Push info summary: %s" % push_info.summary)
                    if push_info.flags & PushInfo.ERROR == PushInfo.ERROR:
                        AgentGitHandler.log.error("Failed to push artifacts to remote repo for tenant: %s remote: %s" %
                                                  (git_repo.tenant_id, git_repo.repo_url))
                        # revert to the initial commit
                        AgentGitHandler.execute_git_command(["reset", "--hard", init_head], git_repo.local_repo_path)
                        return
                AgentGitHandler.log.debug(
                    "Successfully pushed artifacts for tenant: %s remote: %s" % (git_repo.tenant_id, git_repo.repo_url))
            except Exception as e:
                AgentGitHandler.log.error(
                    "Failed to push artifacts to remote repo for tenant: %s remote: %s exception: %s" %
                    (git_repo.tenant_id, git_repo.repo_url, e))
                # revert to the initial commit
                AgentGitHandler.execute_git_command(["reset", "--hard", init_head], git_repo.local_repo_path)

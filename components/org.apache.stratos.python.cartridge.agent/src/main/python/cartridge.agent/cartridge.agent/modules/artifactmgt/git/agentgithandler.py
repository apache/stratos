# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

from threading import current_thread
import os
import subprocess
import shutil
from git import *

from ... util.log import LogFactory
from ... util.asyncscheduledtask import AbstractAsyncScheduledTask, ScheduledExecutor
from ... artifactmgt.repository import Repository
from exception import GitRepositorySynchronizationException


class AgentGitHandler:
    """
    Handles all the git artifact management tasks related to a cartridge
    """

    log = LogFactory().get_log(__name__)

    __git_repositories = {}
    # (tenant_id => GitRepository)

    @staticmethod
    def checkout(repo_info):
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
        git_repo = AgentGitHandler.get_repo(repo_info.tenant_id)
        updated = False
        if git_repo is not None:
            # has been previously cloned, this is not the subscription run
            subscribe_run = False
            if AgentGitHandler.is_valid_git_repository(git_repo):
                AgentGitHandler.log.debug("Executing git pull: [tenant-id] %s [repo-url] %s",
                                          git_repo.tenant_id, git_repo.repo_url)
                try:
                    updated = AgentGitHandler.pull(git_repo)
                    AgentGitHandler.log.debug("Git pull executed: [tenant-id] %s [repo-url] %s",
                                              git_repo.tenant_id, git_repo.repo_url)
                except GitRepositorySynchronizationException as e:
                    AgentGitHandler.log.debug("Warning: Git Pull operation failed: %s" % e.get_message())

            else:
                # not a valid repository, might've been corrupted. do a re-clone
                AgentGitHandler.log.debug("Local repository is not valid. Doing a re-clone to purify.")
                git_repo.cloned = False
                AgentGitHandler.log.debug("Executing git clone: [tenant-id] %s [repo-url] %s",
                                          git_repo.tenant_id, git_repo.repo_url)
                git_repo = AgentGitHandler.clone(git_repo)
                updated = True
                AgentGitHandler.log.debug("Git clone executed: [tenant-id] %s [repo-url] %s",
                                          git_repo.tenant_id, git_repo.repo_url)
        else:
            # subscribing run.. need to clone
            git_repo = AgentGitHandler.create_git_repo(repo_info)
            AgentGitHandler.log.debug("Cloning artifacts from %s for the first time to %s",
                                      git_repo.repo_url, git_repo.local_repo_path)
            subscribe_run = True
            AgentGitHandler.log.debug("Executing git clone: [tenant-id] %s [repo-url] %s, [repo path] %s",
                                      git_repo.tenant_id, git_repo.repo_url, git_repo.local_repo_path)
            git_repo = AgentGitHandler.clone(git_repo)
            AgentGitHandler.log.debug("Git clone executed: [tenant-id] %s [repo-url] %s",
                                      git_repo.tenant_id, git_repo.repo_url)

        return subscribe_run, updated

    @staticmethod
    def sync_initial_local_artifacts(git_repo):
        # init git repo
        AgentGitHandler.init(git_repo.local_repo_path)

        # add remote repos
        return AgentGitHandler.add_remote(git_repo)

    @staticmethod
    def add_remote(git_repo):
        # add origin remote
        output, errors = AgentGitHandler.execute_git_command(["remote", "add", "origin", git_repo.repo_url],
                                                             git_repo.local_repo_path)
        if len(output) > 0:
            raise GitRepositorySynchronizationException("Error in adding remote origin %s for local repository %s"
                                                        % (git_repo.repo_url, git_repo.local_repo_path))

        # fetch
        output, errors = AgentGitHandler.execute_git_command(["fetch"], git_repo.local_repo_path)
        if "Resolving deltas: 100%" not in output:
            raise GitRepositorySynchronizationException(
                "Error in fetching from remote origin %s for local repository %s"
                % (git_repo.repo_url, git_repo.local_repo_path))

        # checkout master
        output, errors = AgentGitHandler.execute_git_command(["checkout", "master"], git_repo.local_repo_path)
        if "Branch master set up to track remote branch master from origin." not in output:
            raise GitRepositorySynchronizationException("Error in checking out master branch %s for local repository %s"
                                                        % (git_repo.repo_url, git_repo.local_repo_path))

        return True

    @staticmethod
    def init(path):
        output, errors = AgentGitHandler.execute_git_command(["init"], path)
        if "Initialized empty Git repository in" not in output:
            AgentGitHandler.log.exception("Initializing local repo at %s failed: %s" % (path, output))
            raise Exception("Initializing local repo at %s failed" % path)

    @staticmethod
    def is_valid_git_repository(git_repo):
        if git_repo.cloned:
            return True

        output, errors = AgentGitHandler.execute_git_command(["show-ref"], git_repo.local_repo_path)
        if len(output) > 0:
            refs = output.split("\n")
            for ref in refs:
                ref = ref.strip()
                if len(ref) > 0:
                    ref = ref.split(" ")
                    try:
                        AgentGitHandler.execute_git_command(["show", ref[0].strip()], git_repo.local_repo_path)
                    except RuntimeError:
                        return False
            return True
        else:
            return False

    @staticmethod
    def pull(git_repo):
        # git reset to make sure no uncommitted changes are present before the pull, no conflicts will occur
        AgentGitHandler.execute_git_command(["reset", "--hard"], git_repo.local_repo_path)

        # HEAD before pull
        (init_head, init_errors) = AgentGitHandler.execute_git_command(["rev-parse", "HEAD"], git_repo.local_repo_path)

        try:
            repo = Repo(git_repo.local_repo_path)
            repo.remotes.origin.pull()
            if repo.is_dirty():
                raise GitRepositorySynchronizationException("Git pull operation left the repository in dirty state")
        except (GitCommandError, GitRepositorySynchronizationException) as e:
            raise GitRepositorySynchronizationException("Git pull operation on %s for tenant %s failed: %s" %
                                                        (git_repo.repo_url, git_repo.tenant_id, e))

        # HEAD after pull
        (end_head, end_errors) = AgentGitHandler.execute_git_command(["rev-parse", "HEAD"], git_repo.local_repo_path)

        # check if HEAD was updated
        if init_head != end_head:
            AgentGitHandler.log.debug("Artifacts were updated as a result of the pull operation, thread: %s - %s" %
                                      (current_thread().getName(), current_thread().ident))

            return True
        else:
            return False

    @staticmethod
    def clone(git_repo):
        if os.path.isdir(git_repo.local_repo_path) and os.listdir(git_repo.local_repo_path) != []:
            # delete and recreate local repo path if not empty dir
            AgentGitHandler.log.debug("Local repository path not empty. Cleaning.")
            GitUtils.delete_folder_tree(git_repo.local_repo_path)
            GitUtils.create_dir(git_repo.local_repo_path)

        try:
            Repo.clone_from(git_repo.repo_url, git_repo.local_repo_path)
            AgentGitHandler.add_repo(git_repo)
            AgentGitHandler.log.info("Git clone operation for tenant %s successful" % git_repo.tenant_id)
            return git_repo
        except GitCommandError as e:
            raise GitRepositorySynchronizationException("Error while cloning repository: %s" % e)

    @staticmethod
    def add_repo(git_repo):
        AgentGitHandler.__git_repositories[git_repo.tenant_id] = git_repo

    @staticmethod
    def get_repo(tenant_id):
        """

        :param int tenant_id:
        :return: GitRepository object
        :rtype: GitRepository
        """
        tenant_id = str(tenant_id)
        if tenant_id in AgentGitHandler.__git_repositories:
            return AgentGitHandler.__git_repositories[tenant_id]

        return None

    @staticmethod
    def clear_repo(tenant_id):
        if tenant_id in AgentGitHandler.__git_repositories:
            del AgentGitHandler.__git_repositories[tenant_id]

    @staticmethod
    def create_git_repo(repo_info):
        git_repo = GitRepository()
        git_repo.tenant_id = repo_info.tenant_id
        git_repo.local_repo_path = repo_info.repo_path
        git_repo.repo_url = AgentGitHandler.create_auth_url(repo_info)
        git_repo.repo_username = repo_info.repo_username
        git_repo.repo_password = repo_info.repo_password
        git_repo.commit_enabled = repo_info.commit_enabled

        git_repo.cloned = False

        return git_repo

    @staticmethod
    def create_auth_url(repo_info):
        # Accepted repo url formats
        # "https://host.com/path/to/repo.git"
        # "https://username@host.org/path/to/repo.git"
        # "https://username:password@host.org/path/to/repo.git" NOT RECOMMENDED
        if repo_info.repo_username is not None and repo_info.repo_username != "":
            # credentials provided, have to modify url
            repo_url = repo_info.repo_url
            url_split = repo_url.split("//")
            if "@" in url_split[1]:
                # credentials seem to be in the url, check
                at_split = url_split[1].split("@")
                if ":" in url_split[1] and url_split[1].index(":") < url_split[1].index("@"):
                    # both username and password are in the url, check and return as is
                    credential_split = at_split[0].split(":")
                    if credential_split[0] is repo_info.repo_username and \
                            credential_split[1] is repo_info.repo_password:
                        # credentialed url with provided credentials, return as is
                        return repo_info.repo_url
                    else:
                        # credentials wrong, need to replace
                        return str(url_split[0] + "//" + repo_info.repo_username + ":" + repo_info.repo_password.strip() + "@" +
                                   at_split[1])
                else:
                    # only username is provided, need to include password
                    return str(url_split[0] + "//" + repo_info.repo_username + ":" + repo_info.repo_password.strip() + "@" +
                               at_split[1])
            else:
                # no credentials in the url, need to include username and password
                return str(url_split[0] + "//" + repo_info.repo_username + ":" + repo_info.repo_password.strip() + "@" +
                           url_split[1])
        # no credentials specified, return as is
        return repo_info.repo_url

    @staticmethod
    def push(repo_info):
        """
        Commits and pushes new artifacts to the remote repository
        :param repo_info:
        :return:
        """

        git_repo = AgentGitHandler.get_repo(repo_info.tenant_id)
        if git_repo is None:
            # not cloned yet
            raise GitRepositorySynchronizationException("Not a valid repository to push from. Aborting")

        # Get initial HEAD so in case if push fails it can be reverted to this hash
        # This way, commit and push becomes an single operation. No intermediate state will be left behind.
        (init_head, init_errors) = AgentGitHandler.execute_git_command(["rev-parse", "HEAD"], git_repo.local_repo_path)

        # check if modified
        modified, unstaged_files = AgentGitHandler.get_unstaged_files(git_repo.local_repo_path)

        AgentGitHandler.log.debug("[Git] Modified: %s" % str(modified))

        if not modified:
            AgentGitHandler.log.debug("No changes detected in the local repository for tenant %s" % git_repo.tenant_id)
            return

        AgentGitHandler.stage_all(git_repo.local_repo_path)

        # commit to local repositpory
        commit_message = "tenant [%s]'s artifacts committed to local repo at %s" \
                         % (git_repo.tenant_id, git_repo.local_repo_path)
        # TODO: set configuratble names, check if already configured
        commit_name = git_repo.tenant_id
        commit_email = "author@example.org"
        # git config
        AgentGitHandler.execute_git_command(["config", "user.email", commit_email], git_repo.local_repo_path)
        AgentGitHandler.execute_git_command(["config", "user.name", commit_name], git_repo.local_repo_path)

        # commit
        (output, errors) = AgentGitHandler.execute_git_command(
            ["commit", "-m", commit_message], git_repo.local_repo_path)
        if errors.strip() == "":
            commit_hash = AgentGitHandler.find_between(output, "[master", "]").strip()
            AgentGitHandler.log.debug("Committed artifacts for tenant : %s : %s " % (git_repo.tenant_id, commit_hash))
        else:
            AgentGitHandler.log.exception("Committing artifacts to local repository failed for tenant: %s, Cause: %s"
                                          % (git_repo.tenant_id, errors))
            # revert to initial commit hash
            AgentGitHandler.execute_git_command(["reset", "--hard", init_head], git_repo.local_repo_path)
            return

        # push to remote
        try:
            repo = Repo(git_repo.local_repo_path)
            push_info = repo.remotes.origin.push()
            if str(push_info[0].summary) is "[rejected] (fetch first)":
                # need to pull
                repo.remotes.origin.pull()
                if repo.is_dirty():
                    # auto merge failed, need to reset
                    # TODO: what to do here?
                    raise GitRepositorySynchronizationException(
                        "Git pull before push operation left repository in dirty state.")

                # pull successful, now push
                repo.remotes.origin.push()
            AgentGitHandler.log.debug("Pushed artifacts for tenant : %s" % git_repo.tenant_id)
        except (GitCommandError, GitRepositorySynchronizationException) as e:
            # revert to initial commit hash
            AgentGitHandler.execute_git_command(["reset", "--hard", init_head], git_repo.local_repo_path)

            raise GitRepositorySynchronizationException(
                "Pushing artifacts to remote repository failed for tenant %s: %s" % (git_repo.tenant_id, e))

    @staticmethod
    def get_unstaged_files(repo_path):

        (output, errors) = AgentGitHandler.execute_git_command(["status"], repo_path=repo_path)
        unstaged_files = {"modified": [], "untracked": []}

        if "nothing to commit" in output:
            return False, unstaged_files

        if "Changes not staged for commit" in output:
            # there are modified files
            modified_lines = output.split("\n\n")[2].split("\n")
            for mod_line in modified_lines:
                file_name = mod_line.split(":")[1].strip()
                unstaged_files["modified"].append(file_name)

        if "Untracked files" in output:
            # there are untracked files
            untracked_files = output.split("Untracked files:")[1].split("\n\n")[1].split("\n")
            for unt_line in untracked_files:
                unstaged_files["untracked"].append(unt_line.strip())

        return True, unstaged_files

    @staticmethod
    def stage_all(repo_path):
        (output, errors) = AgentGitHandler.execute_git_command(["add", "--all"], repo_path=repo_path)
        return True if errors.strip() == "" else False

    @staticmethod
    def find_between(s, first, last):
        try:
            start = s.index(first) + len(first)
            end = s.index(last, start)
            return s[start:end]
        except ValueError:
            return ""

    @staticmethod
    def schedule_artifact_update_task(repo_info, auto_checkout, auto_commit, update_interval):
        git_repo = AgentGitHandler.get_repo(repo_info.tenant_id)

        if git_repo is None:
            AgentGitHandler.log.error("Unable to schedule artifact sync task, repositoryContext null for tenant %s"
                                      % repo_info.tenant_id)
            return

        if git_repo.scheduled_update_task is None:
            artifact_update_task = ArtifactUpdateTask(repo_info, auto_checkout, auto_commit)
            async_task = ScheduledExecutor(update_interval, artifact_update_task)

            git_repo.scheduled_update_task = async_task
            async_task.start()
            AgentGitHandler.log.info("Scheduled Artifact Synchronization Task for path %s" % git_repo.local_repo_path)
        else:
            AgentGitHandler.log.info("Artifact Synchronization Task for path %s already scheduled"
                                     % git_repo.local_repo_path)

    @staticmethod
    def remove_repo(tenant_id):
        git_repo = AgentGitHandler.get_repo(tenant_id)

        # stop artifact update task
        git_repo.scheduled_update_task.terminate()

        # remove git contents
        try:
            GitUtils.delete_folder_tree(git_repo.local_repo_path)
        except GitRepositorySynchronizationException as e:
            AgentGitHandler.log.exception("Repository folder not deleted: %s" % e.get_message())

        AgentGitHandler.clear_repo(tenant_id)
        AgentGitHandler.log.info("git repository deleted for tenant %s" % git_repo.tenant_id)

        return True

    @staticmethod
    def execute_git_command(command, repo_path):
        """
        Executes the given command string with given environment parameters
        :param list command: Command with arguments to be executed
        :param str repo_path: Repository path to run the command on
        :return: output and error string tuple, RuntimeError if errors occur
        :rtype: tuple(str, str)
        :exception: RuntimeError
        """
        os_env = os.environ.copy()

        command.insert(0, "/usr/bin/git")
        p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=os_env, cwd=repo_path)
        output, errors = p.communicate()
        if len(errors) > 0:
            raise RuntimeError("Git Command execution failed: %s" % errors)

        return output, errors


class ArtifactUpdateTask(AbstractAsyncScheduledTask):
    """
    Checks if the auto-checkout and autocommit are enabled and executes respective tasks
    """

    def __init__(self, repo_info, auto_checkout, auto_commit):
        self.log = LogFactory().get_log(__name__)
        self.repo_info = repo_info
        self.auto_checkout = auto_checkout
        self.auto_commit = auto_commit

    def execute_task(self):
        if self.auto_checkout:
            try:
                self.log.debug("Running checkout job")
                AgentGitHandler.checkout(self.repo_info)
                # TODO: run updated scheduler extension
            except GitRepositorySynchronizationException as e:
                self.log.exception("Auto checkout task failed: %s" % e.get_message())

        if self.auto_commit:
            try:
                self.log.debug("Running commit job")
                AgentGitHandler.push(self.repo_info)
            except GitRepositorySynchronizationException as e:
                self.log.exception("Auto commit failed: %s" % e.get_message())


class GitRepository:
    """
    Represents a git repository inside a particular instance
    """

    def __init__(self):
        self.repo_url = None
        """ :type : str  """
        self.local_repo_path = None
        """ :type : str  """
        self.cloned = False
        """ :type : bool  """
        self.tenant_id = None
        """ :type : int  """
        self.repo_username = None
        """ :type : str  """
        self.repo_password = None
        """ :type : str  """
        """ :type : bool  """
        self.commit_enabled = False
        """ :type : bool  """
        self.scheduled_update_task = None
        """:type : ScheduledExecutor """


class GitUtils:
    """
    Util methods required by the AgentGitHandler
    """
    log = LogFactory().get_log(__name__)

    @staticmethod
    def create_dir(path):
        """
        mkdir the provided path
        :param path: The path to the directory to be made
        :return: True if mkdir was successful, False if dir already exists
        :rtype: bool
        """
        try:
            os.mkdir(path)
            GitUtils.log.info("Successfully created directory [%s]" % path)
            # return True
        except OSError as e:
            raise GitRepositorySynchronizationException("Directory creating failed in [%s]. " % e)

        # return False

    @staticmethod
    def delete_folder_tree(path):
        """
        Completely deletes the provided folder
        :param str path: Full path of the folder
        :return: void
        """
        try:
            shutil.rmtree(path)
            GitUtils.log.debug("Directory [%s] deleted." % path)
        except OSError as e:
            raise GitRepositorySynchronizationException("Deletion of folder path %s failed: %s" % (path, e))

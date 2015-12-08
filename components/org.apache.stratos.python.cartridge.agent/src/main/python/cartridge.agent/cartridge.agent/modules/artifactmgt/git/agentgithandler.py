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

import shutil
import subprocess
import tempfile
import urllib
import os
from distutils.dir_util import copy_tree
from threading import current_thread

import constants
import time
from config import Config
from exception import GitRepositorySynchronizationException
from git import *

from ...util.asyncscheduledtask import AbstractAsyncScheduledTask, ScheduledExecutor
from ...util.log import LogFactory


class AgentGitHandler:
    """
    Handles all the git artifact management tasks related to a cartridge
    """

    def __init__(self):
        pass

    log = LogFactory().get_log(__name__)

    __git_repositories = {}

    # (tenant_id => GitRepository)

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
        # check if modified files are present
        modified = AgentGitHandler.has_modified_files(git_repo.local_repo_path)
        if modified:
            if Config.is_commits_enabled:
                AgentGitHandler.log.debug(
                    "Un-staged files exist in working directory. Aborting git pull for this iteration...")
                return
            else:
                AgentGitHandler.log.warn("Changes detected in working directory but COMMIT_ENABLED is set to false!")
                AgentGitHandler.log.warn("Attempting to reset the working directory")
                AgentGitHandler.execute_git_command(["reset"], repo_path=git_repo.local_repo_path)

        # HEAD before pull
        (init_head, init_errors) = AgentGitHandler.execute_git_command(["rev-parse", "HEAD"], git_repo.local_repo_path)

        repo = Repo(git_repo.local_repo_path)
        AgentGitHandler.execute_git_command(["pull", "--rebase", "origin", "master"], git_repo.local_repo_path)
        AgentGitHandler.log.debug("Git pull rebase executed in checkout job")
        if repo.is_dirty():
            AgentGitHandler.log.error("Git pull operation in checkout job left the repository in dirty state")
            AgentGitHandler.log.error(
                "Git pull operation on remote %s for tenant %s failed" % (git_repo.repo_url, git_repo.tenant_id))

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
        try:
            # create a temporary location to clone
            temp_repo_path = os.path.join(tempfile.gettempdir(), "pca_temp_" + git_repo.tenant_id)
            if os.path.isdir(temp_repo_path) and os.listdir(temp_repo_path) != []:
                GitUtils.delete_folder_tree(temp_repo_path)
                GitUtils.create_dir(temp_repo_path)

            # clone the repo to a temporary location first to avoid conflicts
            AgentGitHandler.log.debug(
                "Cloning artifacts from URL: %s to temp location: %s" % (git_repo.repo_url, temp_repo_path))
            Repo.clone_from(git_repo.auth_url, temp_repo_path)

            # move the cloned dir to application path
            copy_tree(temp_repo_path, git_repo.local_repo_path)
            AgentGitHandler.log.info("Git clone operation for tenant %s successful" % git_repo.tenant_id)
            return git_repo
        except GitCommandError as e:
            raise GitRepositorySynchronizationException("Error while cloning repository for tenant %s: %s" % (
                git_repo.tenant_id, e))

    @staticmethod
    def retry_clone(git_repo):
        """Retry 'git clone' operation for defined number of attempts with defined intervals
        """
        git_clone_successful = False
        # Read properties from agent.conf
        max_retry_attempts = int(Config.artifact_clone_retry_count)
        retry_interval = int(Config.artifact_clone_retry_interval)
        retry_attempts = 0

        # Iterate until git clone is successful or reaches max retry attempts
        while git_clone_successful is False and retry_attempts < max_retry_attempts:
            try:
                retry_attempts += 1
                AgentGitHandler.clone(git_repo)
                AgentGitHandler.log.info(
                    "Retrying attempt to git clone operation for tenant %s successful" % git_repo.tenant_id)
                git_clone_successful = True
            except GitRepositorySynchronizationException as e:
                AgentGitHandler.log.exception("Retrying git clone attempt %s failed: %s" % (retry_attempts, e))
                if retry_attempts < max_retry_attempts:
                    time.sleep(retry_interval)
                else:
                    raise GitRepositorySynchronizationException("All attempts failed while retrying git clone: %s"
                                                                % e)

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
        git_repo.repo_url = repo_info.repo_url
        git_repo.auth_url = AgentGitHandler.create_auth_url(repo_info)
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
        # IMPORTANT: if the credentials are provided in the repo url, they must be url encoded
        if repo_info.repo_username is not None or repo_info.repo_password is not None:
            # credentials provided, have to modify url
            repo_url = repo_info.repo_url
            url_split = repo_url.split("://", 1)

            # urlencode repo username and password
            urlencoded_username = urllib.quote(repo_info.repo_username.strip(), safe='')
            urlencoded_password = urllib.quote(repo_info.repo_password.strip(), safe='')
            if "@" in url_split[1]:
                # credentials seem to be in the url, check
                at_split = url_split[1].split("@", 1)
                if ":" in at_split[0]:
                    # both username and password are in the url, return as is
                    return repo_info.repo_url
                else:
                    # only username is provided, need to include password
                    username_in_url = at_split[0].split(":", 1)[0]
                    return str(url_split[0] + "://" + username_in_url + ":" + urlencoded_password
                               + "@" + at_split[1])
            else:
                # no credentials in the url, need to include username and password
                return str(url_split[0] + "://" + urlencoded_username + ":" + urlencoded_password + "@" + url_split[1])
        # no credentials specified, return as is
        return repo_info.repo_url

    @staticmethod
    def has_modified_files(repo_path):
        (output, errors) = AgentGitHandler.execute_git_command(["status"], repo_path=repo_path)
        if "nothing to commit" in output:
            return False
        else:
            return True

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
            AgentGitHandler.log.info("Scheduled artifact synchronization task for path %s" % git_repo.local_repo_path)
        else:
            AgentGitHandler.log.debug("Artifact synchronization task for path %s already scheduled"
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
            AgentGitHandler.log.exception(
                "Could not remove repository folder for tenant:%s  %s" % (git_repo.tenant_id, e))

        AgentGitHandler.clear_repo(tenant_id)
        AgentGitHandler.log.info("Git repository deleted for tenant %s" % git_repo.tenant_id)

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
        AgentGitHandler.log.debug("Executing Git command: %s" % command)
        p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=os_env, cwd=repo_path)
        (output, errors) = p.communicate()
        AgentGitHandler.log.debug("Git command [output] %s" % str(output))
        AgentGitHandler.log.debug("Git command [errors] %s" % str(errors))
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
        # DO NOT change this order. The commit job should run first here.
        # This is because if the cloned location contain any un-tracked files then
        # those files should be committed and pushed first
        if self.auto_commit:
            try:
                self.log.debug("Running commit job...")
                Config.artifact_commit_plugin.plugin_object.commit(self.repo_info)
            except GitRepositorySynchronizationException as e:
                self.log.exception("Auto commit failed: %s" % e)

        if self.auto_checkout:
            try:
                self.log.debug("Running checkout job...")
                Config.artifact_checkout_plugin.plugin_object.checkout(self.repo_info)
            except GitRepositorySynchronizationException as e:
                self.log.exception("Auto checkout task failed: %s" % e)

        self.log.debug("ArtifactUpdateTask end of iteration.")


class GitRepository:
    """
    Represents a git repository inside a particular instance
    """

    def __init__(self):
        self.repo_url = None
        """ :type : str  """
        self.auth_url = None
        """ :type : str """
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

    def __init__(self):
        pass

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
            GitUtils.log.debug("Successfully created directory [%s]" % path)
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

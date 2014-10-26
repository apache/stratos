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

from threading import current_thread, Thread
from git import *
from gittle import Gittle, GittleAuth  # GitPython and Gittle are both used at the time being for pros and cons of both
import urllib2
import os
import pexpect
import subprocess

from ... util.log import LogFactory
from ... util import cartridgeagentutils, extensionutils, cartridgeagentconstants
from gitrepository import GitRepository
from ... config import cartridgeagentconfiguration
from ... util.asyncscheduledtask import AbstractAsyncScheduledTask, ScheduledExecutor
from ... artifactmgt.repositoryinformation import RepositoryInformation


class AgentGitHandler:
    """
    Handles all the git artifact management tasks related to a cartridge
    """

    log = LogFactory().get_log(__name__)

    SUPER_TENANT_ID = -1234
    SUPER_TENANT_REPO_PATH = "/repository/deployment/server/"
    TENANT_REPO_PATH = "/repository/tenants/"

    extension_handler = None

    __git_repositories = {}
    # (tenant_id => gitrepository.GitRepository)

    cartridge_agent_config = cartridgeagentconfiguration.CartridgeAgentConfiguration()

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

        :param RepositoryInformation repo_info: The repository information object
        :return: A tuple containing whether it was an initial clone or not, and the repository
        context object
        :rtype: tuple(bool, GitRepository)
        """
        repo_context = AgentGitHandler.get_repo_context(repo_info.tenant_id)
        if repo_context is not None:
            #has been previously cloned, this is not the subscription run
            subscribe_run = False
            if AgentGitHandler.is_valid_git_repository(repo_context):
                AgentGitHandler.log.debug("Existing git repository detected for tenant %r, no clone required" % repo_info.tenant_id)
                AgentGitHandler.pull(repo_context)
            else:
                if not os.listdir(repo_context.local_repo_path):
                    #empty dir, clone
                    repo_context.repo = AgentGitHandler.clone(repo_info)
                else:
                    #not empty
                    if AgentGitHandler.sync_initial_local_artifacts(repo_context):
                        AgentGitHandler.pull(repo_context)
                    else:
                        repo_context = None
        else:
            #subscribing run.. need to clone
            subscribe_run = True
            repo_context = AgentGitHandler.clone(repo_info)

        return subscribe_run, repo_context

    @staticmethod
    def sync_initial_local_artifacts(repo_context):
        #init git repo
        AgentGitHandler.init(repo_context.local_repo_path)

        # add remote repos
        return AgentGitHandler.add_remote(repo_context)

    @staticmethod
    def add_remote(repo_context):
        try:
            #add origin remote
            repo_context.repo.create_remote("origin", repo_context.repo_url)
            #fetch branch details from origin
            repo_context.repo.git.fetch()
            #checkout master branch from origin/master as tracking
            repo_context.repo.git.branch("-f", "--track", "master", "origin/master")
            return True
        except:
            AgentGitHandler.log.exception("Error in adding remote origin %r for local repository %r"
                                          % (repo_context.repo_url, repo_context.local_repo_path))
            return False

    @staticmethod
    def init(path):
        try:
            repo = Gittle.init(path)
            return repo
        except:
            AgentGitHandler.log.exception("Initializing local repo at %r failed" % path)
            raise Exception("Initializing local repo at %r failed" % path)

    @staticmethod
    def is_valid_git_repository(repo_context):
        if repo_context.cloned:
            return True

        for ref in repo_context.repo.refs:
            try:
                ref._get_object()
            except ValueError:
                return False

        return True

    @staticmethod
    def pull(repo_context):
        repo = Repo(repo_context.local_repo_path)
        import agent
        AgentGitHandler.extension_handler = agent.CartridgeAgent.extension_handler
        try:
            repo.git.checkout("master")
            pull_output = repo.git.pull()
            if "Already up-to-date." not in pull_output:
                AgentGitHandler.log.debug("Artifacts were updated as a result of the pull operation, thread: %r - %r" % (current_thread().getName(), current_thread().ident))
            else:
                AgentGitHandler.log.debug("Pull operation: Already up-to-date, thread: %r - %r" % (current_thread().getName(), current_thread().ident))

            AgentGitHandler.extension_handler.on_artifact_update_scheduler_event(repo_context.tenant_id)
        except GitCommandError as ex:
            if "fatal: Could not read from remote repository." in ex:
                #invalid configuration, need to delete and reclone
                AgentGitHandler.log.warn("Git pull unsuccessful for tenant %r, invalid configuration. %r" % (repo_context.tenant_id, ex))
                cartridgeagentutils.delete_folder_tree(repo_context.local_repo_path)
                AgentGitHandler.clone(RepositoryInformation(
                    repo_context.repo_url,
                    repo_context.repo_username,
                    repo_context.repo_password,
                    repo_context.local_repo_path,
                    repo_context.tenant_id,
                    repo_context.is_multitenant,
                    repo_context.commit_enabled
                ))
                AgentGitHandler.extension_handler.on_artifact_update_scheduler_event(repo_context.tenant_id)
            elif "error: Your local changes to the following files would be overwritten by merge:" in ex:
                #conflict error
                AgentGitHandler.log.warn("Git pull unsuccessful for tenant %r, conflicts detected." % repo_context.tenant_id)
                #raise ex

                """
                0:'git pull' returned exit status 1: error: Your local changes to the following files would be overwritten by merge:
                1:    README.md
                2:    index.php
                3:Please, commit your changes or stash them before you can merge.
                4:Aborting
                """
                conflict_list = []
                files_arr = str(ex).split("\n")
                for file_index in range(1, len(files_arr) - 2):
                    file_name = files_arr[file_index].strip()
                    conflict_list.append(file_name)
                    AgentGitHandler.log.debug("Added the file path %r to checkout from the remote repository" % file_name)

                AgentGitHandler.checkout_individually(conflict_list, repo)
            elif "fatal: unable to access " in ex:
                #transport error
                AgentGitHandler.log.exception("Accessing remote git repository %r failed for tenant %r" % (repo_context.repo_url, repo_context.tenant_id))
            else:
                AgentGitHandler.log.exception("Git pull operation for tenant %r failed" % repo_context.tenant_id)

    @staticmethod
    def checkout_individually(conflict_list, repo):
        try:
            for conflicted_file in conflict_list:
                repo.git.checkout(conflicted_file)
                AgentGitHandler.log.info("Checked out the conflicting files from the remote repository successfully")
        except:
            AgentGitHandler.log.exception("Checking out artifacts from index failed")

    @staticmethod
    def clone(repo_info):
        repo_context = None
        try:
            repo_context = AgentGitHandler.create_git_repo_context(repo_info)
            #create the directory if it doesn't exist
            if not os.path.isdir(repo_context.local_repo_path):
                cartridgeagentutils.create_dir(repo_context.local_repo_path)

            #TODO: remove gittle stuff
            #auth = AgentGitHandler.create_auth_configuration(repo_context)
            auth = None

            if auth is not None:
                # authentication is required, use Gittle
                gittle_repo = Gittle.clone(repo_context.repo_url, repo_context.local_repo_path, auth=auth)
                repo = Repo(repo_context.local_repo_path)
            else:
                # authentication is not required, use GitPython
                repo = Repo.clone_from(repo_context.repo_url, repo_context.local_repo_path)
                gittle_repo = Gittle(repo_context.local_repo_path)

            repo_context.cloned = True
            repo_context.gittle_repo = gittle_repo
            repo_context.repo  = repo
            AgentGitHandler.add_repo_context(repo_context)
            AgentGitHandler.log.info("Git clone operation for tenant %r successful" % repo_context.tenant_id)
        except urllib2.URLError:
            AgentGitHandler.log.exception("Accessing remote git repository failed for tenant %r" % repo_context.tenant_id)
        except OSError:
            AgentGitHandler.log.exception("Permission denied for repository path for tenant %r" % repo_context.tenant_id)
        except:
            AgentGitHandler.log.exception("Git clone operation for tenant %r failed" % repo_context.tenant_id)
        finally:
            return repo_context

    @staticmethod
    def create_auth_configuration(repo_context):
        """
        Creates a GittleAuth object based on the type of authorization
        :param GitRepository repo_context: The repository context object
        :return: GittleAuth object or None if no authorization needed
        :rtype: GittleAuth
        """
        if repo_context.key_based_auth:
            private_key = AgentGitHandler.get_private_key()
            auth = GittleAuth(pkey=private_key)
        elif repo_context.repo_username is not None and repo_context.repo_username.strip() != "" and \
                        repo_context.repo_password is not None and repo_context.repo_password.strip() != "":
            auth = GittleAuth(username=repo_context.repo_username, password=repo_context.repo_password)
        else:
            auth = None

        return auth

    @staticmethod
    def get_private_key():
        """
        Returns a file handler to the private key path specified by Carbon or default if not specified
        by Carbon
        :return: The file object of the private key file
        :rtype: file
        """
        pkey_name = cartridgeagentutils.get_carbon_server_property("SshPrivateKeyName")
        if pkey_name is  None:
            pkey_name = "wso2"

        pkey_path = cartridgeagentutils.get_carbon_server_property("SshPrivateKeyPath")
        if pkey_path is None:
            pkey_path = os.environ["HOME"] + "/.ssh"

        if pkey_path.endswith("/"):
            pkey_ptr = pkey_path + pkey_name
        else:
            pkey_ptr = pkey_path + "/" + pkey_name

        pkey_file = open(pkey_ptr)

        return pkey_file


    @staticmethod
    def add_repo_context(repo_context):
        AgentGitHandler.__git_repositories[repo_context.tenant_id] = repo_context

    @staticmethod
    def get_repo_context(tenant_id):
        """

        :param int tenant_id:
        :return: GitRepository object
        :rtype: GitRepository
        """
        if tenant_id in AgentGitHandler.__git_repositories:
            return AgentGitHandler.__git_repositories[tenant_id]

        return None

    @staticmethod
    def remove_repo_context(tenant_id):
        if tenant_id in AgentGitHandler.__git_repositories:
            del AgentGitHandler.__git_repositories[tenant_id]

    @staticmethod
    def create_git_repo_context(repo_info):
        repo_context = GitRepository()
        repo_context.tenant_id = repo_info.tenant_id
        repo_context.local_repo_path = AgentGitHandler.get_repo_path_for_tenant(
            repo_info.tenant_id, repo_info.repo_path, repo_info.is_multitenant)
        repo_context.repo_url = repo_info.repo_url
        repo_context.repo_username = repo_info.repo_username
        repo_context.repo_password = repo_info.repo_password
        repo_context.is_multitenant = repo_info.is_multitenant
        repo_context.commit_enabled = repo_info.commit_enabled

        if AgentGitHandler.is_key_based_auth(repo_info.repo_url, repo_info.tenant_id):
            repo_context.key_based_auth = True
        else:
            repo_context.key_based_auth = False

        repo_context.cloned = False

        repo_context.repo = None
        repo_context.gittle_repo = None

        return repo_context

    @staticmethod
    def is_key_based_auth(repo_url, tenant_id):
        """
        Checks if the given git repo has key based authentication
        :param str repo_url: Git repository remote url
        :param str tenant_id: Tenant ID
        :return: True if key based, False otherwise
        :rtype: bool
        """
        if repo_url.startswith("http://") or repo_url.startswith("https://"):
            # username and password, not key based
            return False
        elif repo_url.startswith("git://github.com"):
            # no auth required
            return False
        elif repo_url.startswith("ssh://") or "@" in repo_url:
            # key based
            return True
        else:
            AgentGitHandler.log.error("Invalid git URL provided for tenant " + tenant_id)
            raise RuntimeError("Invalid git URL provided for tenant " + tenant_id)

    @staticmethod
    def get_repo_path_for_tenant(tenant_id, git_local_repo_path, is_multitenant):
        repo_path = ""

        if is_multitenant:
            if tenant_id == AgentGitHandler.SUPER_TENANT_ID:
                #super tenant, /repository/deploy/server/
                super_tenant_repo_path = AgentGitHandler.cartridge_agent_config.super_tenant_repository_path
                #"app_path"
                repo_path += git_local_repo_path

                if super_tenant_repo_path is not None and super_tenant_repo_path != "":
                    super_tenant_repo_path = super_tenant_repo_path if super_tenant_repo_path.startswith("/") else "/" + super_tenant_repo_path
                    super_tenant_repo_path = super_tenant_repo_path if super_tenant_repo_path.endswith("/") else  super_tenant_repo_path + "/"
                    #"app_path/repository/deploy/server/"
                    repo_path += super_tenant_repo_path
                else:
                    #"app_path/repository/deploy/server/"
                    repo_path += AgentGitHandler.SUPER_TENANT_REPO_PATH

            else:
                #normal tenant, /repository/tenants/tenant_id
                tenant_repo_path = AgentGitHandler.cartridge_agent_config.tenant_repository_path
                #"app_path"
                repo_path += git_local_repo_path

                if tenant_repo_path is not None and tenant_repo_path != "":
                    tenant_repo_path = tenant_repo_path if tenant_repo_path.startswith("/") else "/" + tenant_repo_path
                    tenant_repo_path = tenant_repo_path if tenant_repo_path.endswith("/") else tenant_repo_path + "/"
                    #"app_path/repository/tenants/244653444"
                    repo_path += tenant_repo_path + tenant_id
                else:
                    #"app_path/repository/tenants/244653444"
                    repo_path += AgentGitHandler.TENANT_REPO_PATH + tenant_id

                #tenant_dir_path = git_local_repo_path + AgentGitHandler.TENANT_REPO_PATH + tenant_id
                cartridgeagentutils.create_dir(repo_path)
        else:
            #not multi tenant, app_path
            repo_path = git_local_repo_path

        AgentGitHandler.log.debug("Repo path returned : %r" % repo_path)
        return repo_path

    @staticmethod
    def commit(repo_info):
        """
        Commits and pushes new artifacts to the remote repository
        :param repo_info:
        :return:
        """
        tenant_id = repo_info.tenant_id
        repo_context = AgentGitHandler.get_repo_context(tenant_id)
        #check if modified
        modified, unstaged_files = AgentGitHandler.get_unstaged_files(repo_context.local_repo_path)

        AgentGitHandler.log.debug("Modified: %r" % str(modified))

        # TODO: check for unpushed commits and push them too
        if not modified:
            AgentGitHandler.log.debug("No changes detected in the local repository for tenant " + tenant_id)
            return

        AgentGitHandler.stage_all(repo_context.local_repo_path)

        #commit to local repositpory
        commit_message = "tenant " + tenant_id + "'s artifacts committed to local repo at " + repo_context.local_repo_path
        commit_name="First Author"
        commit_email="author@example.org"
        #git config
        (output, errors) = AgentGitHandler.execute_git_command(["config", "user.email", commit_email], repo_context.local_repo_path)
        (output, errors) = AgentGitHandler.execute_git_command(["config", "user.name", commit_name], repo_context.local_repo_path)

        #commit
        (output, errors) = AgentGitHandler.execute_git_command(["commit", "-m", commit_message], repo_context.local_repo_path)
        if errors.strip() == "":
            commit_hash = AgentGitHandler.find_between(output, "[master", "]").strip()
            AgentGitHandler.log.debug("Committed artifacts for tenant : " + tenant_id + " : " + commit_hash)
        else:
            AgentGitHandler.log.exception("Committing artifacts to local repository failed for tenant " + tenant_id)

        #push to remote
        try:
            #TODO: check key based authentication

            push_op = pexpect.spawn('git push origin master', cwd=repo_context.local_repo_path)
            #push_op.logfile = sys.stdout
            push_op.expect("Username for .*")
            push_op.sendline(repo_context.repo_username)
            push_op.expect("Password for .*")
            push_op.sendline(repo_context.repo_password)
            # result = push_op.expect([commit_hash + "  master -> master", "Authentication failed for"])
            # if result != 0:
            #     raise Exception
            #TODO: handle push failure scenarios
            #push_op.interact()
            push_op.expect(pexpect.EOF)

            AgentGitHandler.log.debug("Pushed artifacts for tenant : " + tenant_id)
        except:
            AgentGitHandler.log.exception("Pushing artifacts to remote repository failed for tenant " + tenant_id)

    @staticmethod
    def get_unstaged_files(repo_path):

        (output, errors) = AgentGitHandler.execute_git_command(["status"], repo_path=repo_path)
        unstaged_files = {"modified":[], "untracked":[]}

        if "nothing to commit" in output:
            return False, unstaged_files

        if "Changes not staged for commit" in output:
            #there are modified files
            modified_lines = output.split("\n\n")[2].split("\n")
            for mod_line in modified_lines:
                file_name = mod_line.split(":")[1].strip()
                unstaged_files["modified"].append(file_name)

        if "Untracked files" in output:
            #there are untracked files
            untracked_files = output.split("Untracked files:")[1].split("\n\n")[1].split("\n")
            for unt_line in untracked_files:
                unstaged_files["untracked"].append(unt_line.strip())

        return True, unstaged_files

    @staticmethod
    def stage_all(repo_path):
        (output, errors) = AgentGitHandler.execute_git_command(["add", "--all"], repo_path=repo_path)
        return True if errors.strip() == "" else False

    @staticmethod
    def find_between( s, first, last ):
        try:
            start = s.index( first ) + len( first )
            end = s.index( last, start )
            return s[start:end]
        except ValueError:
            return ""

    @staticmethod
    def schedule_artifact_update_scheduled_task(repo_info, auto_checkout, auto_commit, update_interval):
        repo_context = AgentGitHandler.get_repo_context(repo_info.tenant_id)

        if repo_context is None:
            AgentGitHandler.log.error("Unable to schedule artifact sync task, repositoryContext null for tenant %r" % repo_info.tenant_id)
            return

        if repo_context.scheduled_update_task is None:
            artifact_update_task = ArtifactUpdateTask(repo_info, auto_checkout, auto_commit)
            async_task = ScheduledExecutor(update_interval, artifact_update_task)

            repo_context.scheduled_update_task = async_task
            async_task.start()
            AgentGitHandler.log.info("Scheduled Artifact Synchronization Task for path %r" % repo_context.local_repo_path)
        else:
            AgentGitHandler.log.info("Artifact Synchronization Task for path %r already scheduled" % repo_context.local_repo_path)

    @staticmethod
    def remove_repo(tenant_id):
        repo_context = AgentGitHandler.get_repo_context(tenant_id)

        #stop artifact update task
        repo_context.scheduled_update_task.terminate()

        #remove git contents
        cartridgeagentutils.delete_folder_tree(repo_context.local_repo_path)

        AgentGitHandler.remove_repo_context(tenant_id)

        if tenant_id == -1234:
            if AgentGitHandler.cartridge_agent_config.is_multitenant:
                extensionutils.execute_copy_artifact_extension(
                    cartridgeagentconstants.SUPERTENANT_TEMP_PATH,
                    AgentGitHandler.cartridge_agent_config.app_path + "/repository/deployment/server/"
                )

        AgentGitHandler.log.info("git repository deleted for tenant %r" % repo_context.tenant_id)

        return True

    @staticmethod
    def execute_git_command(command, repo_path):
        """
        Executes the given command string with given environment parameters
        :param list command: Command with arguments to be executed
        :param dict[str, str] env_params: Environment variables to be used
        :return: output and error string tuple, RuntimeError if errors occur
        :rtype: tuple(str, str)
        :exception: RuntimeError
        """
        os_env = os.environ.copy()

        command.insert(0, "/usr/bin/git")
        p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=os_env, cwd=repo_path)
        output, errors = p.communicate()
        if len(errors) > 0:
            raise RuntimeError("Git Command execution failed: \n %r" % errors)

        return output, errors



class ArtifactUpdateTask(AbstractAsyncScheduledTask):
    """
    Checks if the autocheckout and autocommit are enabled and executes respective tasks
    """

    def __init__(self, repo_info, auto_checkout, auto_commit):
        self.log = LogFactory().get_log(__name__)
        self.repo_info = repo_info
        self.auto_checkout = auto_checkout
        self.auto_commit = auto_commit

    def execute_task(self):
        try:
            if self.auto_checkout:
                self.log.debug("Running checkout job")
                AgentGitHandler.checkout(self.repo_info)
        except:
            self.log.exception("Auto checkout task failed")

        if self.auto_commit:
            self.log.debug("Running commit job")
            AgentGitHandler.commit(self.repo_info)
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
import pexpect
import subprocess
import shutil

from ... util.log import LogFactory
from ... util.asyncscheduledtask import AbstractAsyncScheduledTask, ScheduledExecutor
from ... artifactmgt.repository import Repository
from ... exception.gitrepositorysynchronizationexception import GitRepositorySynchronizationException


class AgentGitHandler:
    """
    Handles all the git artifact management tasks related to a cartridge
    """

    log = LogFactory().get_log(__name__)

    # SUPER_TENANT_ID = -1234
    # SUPER_TENANT_REPO_PATH = "/repository/deployment/server/"
    # TENANT_REPO_PATH = "/repository/tenants/"

    extension_handler = None

    __git_repositories = {}
    # (tenant_id => gitrepository.GitRepository)

    # cartridge_agent_config = cartridgeagentconfiguration.CartridgeAgentConfiguration()

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
        :return: A tuple containing whether it was an initial clone or not, and the repository
        context object
        :rtype: tuple(bool, GitRepository)
        """
        repo_context = AgentGitHandler.get_repo_context(repo_info.tenant_id)
        if repo_context is not None:
            # has been previously cloned, this is not the subscription run
            subscribe_run = False
            if AgentGitHandler.is_valid_git_repository(repo_context):
                AgentGitHandler.log.debug("Executing git pull: [tenant-id] %s [repo-url] %s", repo_context.tenant_id, repo_context.repo_url)
                AgentGitHandler.pull(repo_context)
                AgentGitHandler.log.debug("Git pull executed: [tenant-id] %s [repo-url] %s", repo_context.tenant_id, repo_context.repo_url)
            else:
                # not a valid repository, might've been corrupted. do a re-clone
                AgentGitHandler.log.debug("Local repository is not valid. Doing a re-clone to purify.")
                repo_context.cloned = False
                AgentGitHandler.log.debug("Executing git clone: [tenant-id] %s [repo-url] %s", repo_context.tenant_id, repo_context.repo_url)
                repo_context = AgentGitHandler.clone(repo_context)
                AgentGitHandler.log.debug("Git clone executed: [tenant-id] %s [repo-url] %s", repo_context.tenant_id, repo_context.repo_url)
        else:
            # subscribing run.. need to clone
            repo_context = AgentGitHandler.create_git_repo_context(repo_info)
            AgentGitHandler.log.debug("Cloning artifacts from %s for the first time", repo_context.repo_url)
            subscribe_run = True
            AgentGitHandler.log.debug("Executing git clone: [tenant-id] %s [repo-url] %s", repo_context.tenant_id, repo_context.repo_url)
            repo_context = AgentGitHandler.clone(repo_context)
            AgentGitHandler.log.debug("Git clone executed: [tenant-id] %s [repo-url] %s", repo_context.tenant_id, repo_context.repo_url)

        return subscribe_run, repo_context

    @staticmethod
    def sync_initial_local_artifacts(repo_context):
        # init git repo
        AgentGitHandler.init(repo_context.local_repo_path)

        # add remote repos
        return AgentGitHandler.add_remote(repo_context)

    @staticmethod
    def add_remote(repo_context):
        # TODO: replace unnecessary pexpects
        # add origin remote
        add_remote_op = pexpect.spawn("git remote add origin " + repo_context.repo_url, cwd=repo_context.local_repo_path)
        add_remote_op_output = add_remote_op.readline()
        if len(add_remote_op_output) > 0:
            raise GitRepositorySynchronizationException("Error in adding remote origin %r for local repository %r"
                                                        % (repo_context.repo_url, repo_context.local_repo_path))

        # fetch
        fetch_op = pexpect.spawn("git fetch", cwd=repo_context.local_repo_path)
        fetch_success = False
        for p in fetch_op.readlines():
            if "Resolving deltas: 100%" in p:
                fetch_success = True

        if not fetch_success:
            raise GitRepositorySynchronizationException("Error in fetching from remote origin %r for local repository %r"
                                                        % (repo_context.repo_url, repo_context.local_repo_path))

        # checkout master
        checkout_op = pexpect.spawn("git checkout master", cwd=repo_context.local_repo_path)
        checkout_success = False
        for p in checkout_op.readlines():
            if "Branch master set up to track remote branch master from origin." in p:
                checkout_success = True

        if not checkout_success:
            raise GitRepositorySynchronizationException("Error in checking out master branch %r for local repository %r"
                                                        % (repo_context.repo_url, repo_context.local_repo_path))

        return True
        # repo_context.repo.create_remote("origin", repo_context.repo_url)
        # fetch branch details from origin
        # repo_context.repo.git.fetch()
        # checkout master branch from origin/master as tracking
        # repo_context.repo.git.branch("-f", "--track", "master", "origin/master")
        # return True

    @staticmethod
    def init(path):
        init_op = pexpect.spawn("git init", cwd=path)
        init_op_output = init_op.readline()
        if "Initialized empty Git repository in" not in init_op_output:
            # repo = Gittle.init(path)
            # return repo
            AgentGitHandler.log.exception("Initializing local repo at %r failed: %s" % (path, init_op_output))
            raise Exception("Initializing local repo at %r failed" % path)

    @staticmethod
    def is_valid_git_repository(repo_context):
        if repo_context.cloned:
            return True

        # TODO: fix this
        for ref in repo_context.repo.refs:
            try:
                ref._get_object()
            except ValueError:
                return False

        return True

    @staticmethod
    def pull(repo_context):
        # TODO: extension handler is an ugly dependancy, raise exceptions
        # repo = Repo(repo_context.local_repo_path)
        from .... agent import CartridgeAgent
        AgentGitHandler.extension_handler = CartridgeAgent.extension_handler
        # repo.git.checkout("master")
        # pull_output = repo.git.pull()

        # git reset to make sure no uncommitted changes are present before the pull, no conflicts will occur
        AgentGitHandler.execute_git_command(["reset", "--hard"], repo_context.local_repo_path)

        # HEAD before pull
        (init_head, init_errors) = AgentGitHandler.execute_git_command(["rev-parse", "HEAD"], repo_context.local_repo_path)

        pull_op = pexpect.spawn("git pull", cwd=repo_context.local_repo_path)
        pull_output = pull_op.readlines()

        # HEAD after pull
        (end_head, end_errors) = AgentGitHandler.execute_git_command(["rev-parse", "HEAD"], repo_context.local_repo_path)

        # check if HEAD was updated
        if init_head != end_head:
            AgentGitHandler.log.debug("Artifacts were updated as a result of the pull operation, thread: %r - %r" % (current_thread().getName(), current_thread().ident))
            AgentGitHandler.extension_handler.on_artifact_update_scheduler_event(repo_context.tenant_id)
        else:
            # HEAD not updated, check reasons
            for p in pull_output:
                if "Already up-to-date." in p:
                    # nothing to update
                    AgentGitHandler.log.debug("Pull operation: Already up-to-date, thread: %r - %r" % (current_thread().getName(), current_thread().ident))
                    break

                if "fatal: unable to access " in p:
                    # transport error
                    AgentGitHandler.log.exception("Accessing remote git repository %r failed for tenant %r" % (repo_context.repo_url, repo_context.tenant_id))
                    break

                if "fatal: Could not read from remote repository." in p:
                    # invalid configuration, need to delete and reclone
                    AgentGitHandler.log.warn("Git pull unsuccessful for tenant %r, invalid configuration. %r" % (repo_context.tenant_id, p))
                    GitUtils.delete_folder_tree(repo_context.local_repo_path)
                    AgentGitHandler.clone(Repository(
                        repo_context.repo_url,
                        repo_context.repo_username,
                        repo_context.repo_password,
                        repo_context.local_repo_path,
                        repo_context.tenant_id,
                        repo_context.is_multitenant,
                        repo_context.commit_enabled
                    ))

                    AgentGitHandler.extension_handler.on_artifact_update_scheduler_event(repo_context.tenant_id)
                    break

            AgentGitHandler.log.exception("Git pull operation for tenant %r failed" % repo_context.tenant_id)

    @staticmethod
    def clone(repo_context):
        # repo_context = AgentGitHandler.create_git_repo_context(repo_info)

        if os.path.isdir(repo_context.local_repo_path):
            # delete and recreate local repo path if exists
            GitUtils.delete_folder_tree(repo_context.local_repo_path)

        GitUtils.create_dir(repo_context.local_repo_path)

        clone_op = pexpect.spawn("git clone " + repo_context.repo_url + " " + repo_context.local_repo_path)
        # TODO: if pexpect username here, wrong url
        clone_output = clone_op.readlines()
        for p in clone_output:
            if "Checking connectivity... done." in p:
                repo_context.cloned = True
                AgentGitHandler.add_repo_context(repo_context)
                AgentGitHandler.log.info("Git clone operation for tenant %r successful" % repo_context.tenant_id)
                return repo_context

            if "remote: Repository not found." in p or "fatal: unable to access " in p:
                AgentGitHandler.log.exception("Accessing remote git repository failed for tenant %r" % repo_context.tenant_id)
                AgentGitHandler.log.exception("Error: %s" % p)
                return None

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
        repo_context.local_repo_path = repo_info.repo_path
        repo_context.repo_url = AgentGitHandler.create_auth_url(repo_info)
        repo_context.repo_username = repo_info.repo_username
        repo_context.repo_password = repo_info.repo_password
        repo_context.is_multitenant = repo_info.is_multitenant
        repo_context.commit_enabled = repo_info.commit_enabled

        repo_context.cloned = False

        return repo_context
    #
    # @staticmethod
    # def is_key_based_auth(repo_url, tenant_id):
    #     """
    #     Checks if the given git repo has key based authentication
    #     :param str repo_url: Git repository remote url
    #     :param str tenant_id: Tenant ID
    #     :return: True if key based, False otherwise
    #     :rtype: bool
    #     """
    #     if repo_url.startswith("http://") or repo_url.startswith("https://"):
    #         # username and password, not key based
    #         return False
    #     elif repo_url.startswith("git://github.com"):
    #         # no auth required
    #         return False
    #     elif repo_url.startswith("ssh://") or "@" in repo_url:
    #         # key based
    #         return True
    #     else:
    #         AgentGitHandler.log.error("Invalid git URL provided for tenant " + tenant_id)
    #         raise RuntimeError("Invalid git URL provided for tenant " + tenant_id)

    @staticmethod
    def create_auth_url(repo_info):
        """
        :param repo_info: Include the credentials in the repository url, if provided
        :return: Repository url with credentials included, if provided, or unaltered repository URL
        :exception: GitRepositorySynchronizationException if the repository URL is invalid
        """
        if repo_info.repo_username is None or repo_info.repo_password is None:
            # unauthenticated repo
            return repo_info.repo_url

        # git://host.xz[:port]/path/to/repo.git/
        # http[s]://host.xz[:port]/path/to/repo.git/
        # ftp[s]://host.xz[:port]/path/to/repo.git/
        # rsync://host.xz/path/to/repo.git/
        # An alternative scp-like syntax may also be used with the ssh protocol:
        # [user@]host.xz:path/to/repo.git/
        # The ssh and git protocols additionally support ~username expansion:
        # ssh://[user@]host.xz[:port]/~[user]/path/to/repo.git/
        # git://host.xz[:port]/~[user]/path/to/repo.git/
        # [user@]host.xz:/~[user]/path/to/repo.git/

        repo_url = repo_info.repo_url
        if repo_url.startswith("https://") or repo_url.startswith("http://"):
            split_url = repo_url.split("://")
            auth_str = repo_info.repo_username + ":" + repo_info.repo_password + "@"
            auth_url = split_url[0] + "://" + auth_str + split_url[1]

            return auth_url

        # TODO: verify ftp URLs
        # if repo_url.startswith("ftp://") or repo_url.startswith("ftps://"):
        #     split_url = repo_url.split("://")
        #     auth_str = repo_info.repo_username + ":" + repo_info.repo_password + "@"
        #     auth_url = split_url[0] + "://" + auth_str + split_url[1]
        #
        #     return auth_url

        raise GitRepositorySynchronizationException("Invalid repository URL: %s" % repo_url)

    # @staticmethod
    # def get_repo_path_for_tenant(tenant_id, git_local_repo_path, is_multitenant):
    #     repo_path = ""
    #
    #     if is_multitenant:
    #         if tenant_id == AgentGitHandler.SUPER_TENANT_ID:
    #             # super tenant, /repository/deploy/server/
    #             super_tenant_repo_path = AgentGitHandler.cartridge_agent_config.super_tenant_repository_path
    #             # "app_path"
    #             repo_path += git_local_repo_path
    #
    #             if super_tenant_repo_path is not None and super_tenant_repo_path != "":
    #                 super_tenant_repo_path = super_tenant_repo_path if super_tenant_repo_path.startswith("/") else "/" + super_tenant_repo_path
    #                 super_tenant_repo_path = super_tenant_repo_path if super_tenant_repo_path.endswith("/") else  super_tenant_repo_path + "/"
    #                 # "app_path/repository/deploy/server/"
    #                 repo_path += super_tenant_repo_path
    #             else:
    #                 # "app_path/repository/deploy/server/"
    #                 repo_path += AgentGitHandler.SUPER_TENANT_REPO_PATH
    #
    #         else:
    #             # normal tenant, /repository/tenants/tenant_id
    #             tenant_repo_path = AgentGitHandler.cartridge_agent_config.tenant_repository_path
    #             # "app_path"
    #             repo_path += git_local_repo_path
    #
    #             if tenant_repo_path is not None and tenant_repo_path != "":
    #                 tenant_repo_path = tenant_repo_path if tenant_repo_path.startswith("/") else "/" + tenant_repo_path
    #                 tenant_repo_path = tenant_repo_path if tenant_repo_path.endswith("/") else tenant_repo_path + "/"
    #                 # "app_path/repository/tenants/244653444"
    #                 repo_path += tenant_repo_path + tenant_id
    #             else:
    #                 # "app_path/repository/tenants/244653444"
    #                 repo_path += AgentGitHandler.TENANT_REPO_PATH + tenant_id
    #
    #             # tenant_dir_path = git_local_repo_path + AgentGitHandler.TENANT_REPO_PATH + tenant_id
    #             GitUtils.create_dir(repo_path)
    #     else:
    #         # not multi tenant, app_path
    #         repo_path = git_local_repo_path
    #
    #     AgentGitHandler.log.debug("Repo path returned : %r" % repo_path)
    #     return repo_path

    @staticmethod
    def push(repo_info):
        """
        Commits and pushes new artifacts to the remote repository
        :param repo_info:
        :return:
        """

        repo_context = AgentGitHandler.get_repo_context(repo_info.tenant_id)
        if repo_context is None:
            # not cloned yet
            raise GitRepositorySynchronizationException("Not a valid repository to push from. Aborting")

        # Get initial HEAD so in case if push fails it can be reverted to this hash
        # This way, commit and push becomes an single operation. No intermediate state will be left behind.
        (init_head, init_errors) = AgentGitHandler.execute_git_command(["rev-parse", "HEAD"], repo_context.local_repo_path)

        # check if modified
        modified, unstaged_files = AgentGitHandler.get_unstaged_files(repo_context.local_repo_path)

        AgentGitHandler.log.debug("[Git] Modified: %r" % str(modified))

        if not modified:
            AgentGitHandler.log.debug("No changes detected in the local repository for tenant " + repo_context.tenant_id)
            return

        AgentGitHandler.stage_all(repo_context.local_repo_path)

        # commit to local repositpory
        commit_message = "tenant " + repo_context.tenant_id + "'s artifacts committed to local repo at " + repo_context.local_repo_path
        # TODO: set configuratble names
        commit_name = repo_context.tenant_id
        commit_email = "author@example.org"
        # git config
        AgentGitHandler.execute_git_command(["config", "user.email", commit_email], repo_context.local_repo_path)
        AgentGitHandler.execute_git_command(["config", "user.name", commit_name], repo_context.local_repo_path)

        # commit
        (output, errors) = AgentGitHandler.execute_git_command(["commit", "-m", commit_message], repo_context.local_repo_path)
        if errors.strip() == "":
            commit_hash = AgentGitHandler.find_between(output, "[master", "]").strip()
            AgentGitHandler.log.debug("Committed artifacts for tenant : " + repo_context.tenant_id + " : " + commit_hash)
        else:
            AgentGitHandler.log.exception("Committing artifacts to local repository failed for tenant: %s, Cause: %s" % (repo_context.tenant_id, errors))
            # revert to initial commit hash
            AgentGitHandler.execute_git_command(["reset", "--hard", init_head], repo_context.local_repo_path)
            return

        # push to remote
        try:
            push_op = pexpect.spawn('git push origin master', cwd=repo_context.local_repo_path)
            # push_op.logfile = sys.stdout
            push_op.expect("Username for .*")
            push_op.sendline(repo_context.repo_username)
            push_op.expect("Password for .*")
            push_op.sendline(repo_context.repo_password)
            # result = push_op.expect([commit_hash + "  master -> master", "Authentication failed for"])
            # if result != 0:
            #     raise Exception
            # TODO: handle push failure scenarios
            # push_op.interact()
            push_op.expect(pexpect.EOF)

            AgentGitHandler.log.debug("Pushed artifacts for tenant : " + repo_context.tenant_id)
        except:
            AgentGitHandler.log.exception("Pushing artifacts to remote repository failed for tenant " + repo_context.tenant_id)
            # revert to initial commit hash
            AgentGitHandler.execute_git_command(["reset", "--hard", init_head], repo_context.local_repo_path)

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

        # stop artifact update task
        repo_context.scheduled_update_task.terminate()

        # remove git contents
        GitUtils.delete_folder_tree(repo_context.local_repo_path)
        AgentGitHandler.remove_repo_context(tenant_id)
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
            except:
                self.log.exception("Auto checkout task failed")

        if self.auto_commit:
            try:
                self.log.debug("Running commit job")
                AgentGitHandler.push(self.repo_info)
            except:
                self.log.exception("Auto commit failed")


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
        self.is_multitenant = False
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
            GitUtils.log.info("Successfully created directory [%r]" % path)
            return True
        except OSError:
            pass
            GitUtils.log.exception("Directory creating failed in [%r]. Directory already exists. " % path)

        return False

    @staticmethod
    def delete_folder_tree(path):
        """
        Completely deletes the provided folder
        :param str path: Full path of the folder
        :return: void
        """
        try:
            shutil.rmtree(path)
            GitUtils.log.debug("Directory [%r] deleted." % path)
        except OSError:
            pass
            GitUtils.log.exception("Deletion of folder path %r failed." % path)

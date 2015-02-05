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
import pdb

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
        git_repo = AgentGitHandler.get_repo(repo_info.tenant_id)
        if git_repo is not None:
            # has been previously cloned, this is not the subscription run
            subscribe_run = False
            if AgentGitHandler.is_valid_git_repository(git_repo):
                AgentGitHandler.log.debug("Executing git pull: [tenant-id] %s [repo-url] %s",
                                          git_repo.tenant_id, git_repo.repo_url)
                AgentGitHandler.pull(git_repo)
                AgentGitHandler.log.debug("Git pull executed: [tenant-id] %s [repo-url] %s",
                                          git_repo.tenant_id, git_repo.repo_url)
            else:
                # not a valid repository, might've been corrupted. do a re-clone
                AgentGitHandler.log.debug("Local repository is not valid. Doing a re-clone to purify.")
                git_repo.cloned = False
                AgentGitHandler.log.debug("Executing git clone: [tenant-id] %s [repo-url] %s",
                                          git_repo.tenant_id, git_repo.repo_url)
                git_repo = AgentGitHandler.clone(git_repo)
                AgentGitHandler.log.debug("Git clone executed: [tenant-id] %s [repo-url] %s",
                                          git_repo.tenant_id, git_repo.repo_url)
        else:
            # subscribing run.. need to clone
            git_repo = AgentGitHandler.create_git_repo(repo_info)
            AgentGitHandler.log.debug("Cloning artifacts from %s for the first time to %r",
                                      git_repo.repo_url, git_repo.local_repo_path)
            subscribe_run = True
            AgentGitHandler.log.debug("Executing git clone: [tenant-id] %s [repo-url] %s",
                                      git_repo.tenant_id, git_repo.repo_url)
            git_repo = AgentGitHandler.clone(git_repo)
            AgentGitHandler.log.debug("Git clone executed: [tenant-id] %s [repo-url] %s",
                                      git_repo.tenant_id, git_repo.repo_url)

        return subscribe_run, git_repo

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
            raise GitRepositorySynchronizationException("Error in adding remote origin %r for local repository %r"
                                                        % (git_repo.repo_url, git_repo.local_repo_path))

        # fetch
        output, errors = AgentGitHandler.execute_git_command(["fetch"], git_repo.local_repo_path)
        if "Resolving deltas: 100%" not in output:
            raise GitRepositorySynchronizationException(
                "Error in fetching from remote origin %r for local repository %r"
                % (git_repo.repo_url, git_repo.local_repo_path))

        # checkout master
        output, errors = AgentGitHandler.execute_git_command(["checkout", "master"], git_repo.local_repo_path)
        if "Branch master set up to track remote branch master from origin." not in output:
            raise GitRepositorySynchronizationException("Error in checking out master branch %r for local repository %r"
                                                        % (git_repo.repo_url, git_repo.local_repo_path))

        return True

    @staticmethod
    def init(path):
        output, errors = AgentGitHandler.execute_git_command(["init"], path)
        if "Initialized empty Git repository in" not in output:
            AgentGitHandler.log.exception("Initializing local repo at %r failed: %s" % (path, output))
            raise Exception("Initializing local repo at %r failed" % path)

    @staticmethod
    def is_valid_git_repository(git_repo):
        if git_repo.cloned:
            return True

        # TODO: fix this
        for ref in git_repo.repo.refs:
            try:
                ref._get_object()
            except ValueError:
                return False

        return True

    @staticmethod
    def pull(git_repo):
        # TODO: extension handler is an ugly dependancy, raise exceptions
        from .... agent import CartridgeAgent
        AgentGitHandler.extension_handler = CartridgeAgent.extension_handler

        # git reset to make sure no uncommitted changes are present before the pull, no conflicts will occur
        AgentGitHandler.execute_git_command(["reset", "--hard"], git_repo.local_repo_path)

        # HEAD before pull
        (init_head, init_errors) = AgentGitHandler.execute_git_command(["rev-parse", "HEAD"], git_repo.local_repo_path)

        pull_op = pexpect.spawn("git pull", cwd=git_repo.local_repo_path)
        pull_output = pull_op.readlines()

        # HEAD after pull
        (end_head, end_errors) = AgentGitHandler.execute_git_command(["rev-parse", "HEAD"], git_repo.local_repo_path)

        # check if HEAD was updated
        if init_head != end_head:
            AgentGitHandler.log.debug("Artifacts were updated as a result of the pull operation, thread: %r - %r" %
                                      (current_thread().getName(), current_thread().ident))
            AgentGitHandler.extension_handler.on_artifact_update_scheduler_event(git_repo.tenant_id)
        else:
            # HEAD not updated, check reasons
            for p in pull_output:
                if "Already up-to-date." in p:
                    # nothing to update
                    AgentGitHandler.log.debug("Pull operation: Already up-to-date, thread: %r - %r" %
                                              (current_thread().getName(), current_thread().ident))
                    break

                if "fatal: unable to access " in p:
                    # transport error
                    AgentGitHandler.log.exception("Accessing remote git repository %r failed for tenant %r" %
                                                  (git_repo.repo_url, git_repo.tenant_id))
                    break

                if "fatal: Could not read from remote repository." in p:
                    # invalid configuration, need to delete and reclone
                    AgentGitHandler.log.warn("Git pull unsuccessful for tenant %r, invalid configuration. %r" %
                                             (git_repo.tenant_id, p))
                    GitUtils.delete_folder_tree(git_repo.local_repo_path)
                    AgentGitHandler.clone(Repository(
                        git_repo.repo_url,
                        git_repo.repo_username,
                        git_repo.repo_password,
                        git_repo.local_repo_path,
                        git_repo.tenant_id,
                        git_repo.is_multitenant,
                        git_repo.commit_enabled
                    ))

                    AgentGitHandler.extension_handler.on_artifact_update_scheduler_event(git_repo.tenant_id)
                    break

            AgentGitHandler.log.exception("Git pull operation for tenant %r failed" % git_repo.tenant_id)

    @staticmethod
    def clone(git_repo):
        # repo_context = AgentGitHandler.create_git_repo_context(repo_info)

        if os.path.isdir(git_repo.local_repo_path):
            # delete and recreate local repo path if exists
            GitUtils.delete_folder_tree(git_repo.local_repo_path)

        clone_op = pexpect.spawn("git clone %r %r" % (git_repo.repo_url, git_repo.local_repo_path))
        # Accepted repo url formats
        # "https://host.com/path/to/repo.git"
        # "https://username@host.org/path/to/repo.git"
        # "https://username:password@host.org/path/to/repo.git" NOT RECOMMENDED
        result = clone_op.expect(["Username for .*", "Password for .*", "Checking connectivity... done."])
        if result == 0:
            clone_op.sendline(git_repo.repo_username)
            clone_op.expect("Password for .*")
            clone_op.sendline(git_repo.repo_password)
            clone_output = clone_op.readlines()
            for p in clone_output:
                if "Checking connectivity... done." in p:
                    git_repo.cloned = True
                    AgentGitHandler.add_repo(git_repo)
                    AgentGitHandler.log.info("Git clone operation for tenant %r successful" % git_repo.tenant_id)
                    clone_op.expect(pexpect.EOF)
                    return git_repo

                if "remote: Repository not found." in p \
                        or "fatal: unable to access " in p \
                        or "Authentication failed for" in p:
                    raise GitRepositorySynchronizationException("Git clone operation failed for tenant %r: %r"
                                                                % (git_repo.tenant_id, p))

        elif result == 1:
            clone_op.sendline(git_repo.repo_password)
            clone_output = clone_op.readlines()
            for p in clone_output:
                if "Checking connectivity... done." in p:
                    git_repo.cloned = True
                    AgentGitHandler.add_repo(git_repo)
                    AgentGitHandler.log.info("Git clone operation for tenant %r successful" % git_repo.tenant_id)
                    clone_op.expect(pexpect.EOF)
                    return git_repo

                if "remote: Repository not found." in p \
                        or "fatal: unable to access " in p \
                        or "Authentication failed for" in p:
                    raise GitRepositorySynchronizationException("Git clone operation failed for tenant %r: %r"
                                                                % (git_repo.tenant_id, p))

        elif result == 2:
            git_repo.cloned = True
            AgentGitHandler.add_repo(git_repo)
            AgentGitHandler.log.info("Git clone operation for tenant %r successful" % git_repo.tenant_id)
            clone_op.expect(pexpect.EOF)
            return git_repo

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
        # repo_context.repo_url = AgentGitHandler.create_auth_url(repo_info)
        git_repo.repo_url = repo_info.repo_url
        git_repo.repo_username = repo_info.repo_username
        git_repo.repo_password = repo_info.repo_password
        git_repo.is_multitenant = repo_info.is_multitenant
        git_repo.commit_enabled = repo_info.commit_enabled

        git_repo.cloned = False

        return git_repo

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

        AgentGitHandler.log.debug("[Git] Modified: %r" % str(modified))

        if not modified:
            AgentGitHandler.log.debug("No changes detected in the local repository for tenant %r" % git_repo.tenant_id)
            return

        AgentGitHandler.stage_all(git_repo.local_repo_path)

        # commit to local repositpory
        commit_message = "tenant [%r]'s artifacts committed to local repo at %r" \
                         % (git_repo.tenant_id, git_repo.local_repo_path)
        # TODO: set configuratble names
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
            AgentGitHandler.log.debug("Committed artifacts for tenant : %r : %r " % (git_repo.tenant_id, commit_hash))
        else:
            AgentGitHandler.log.exception("Committing artifacts to local repository failed for tenant: %s, Cause: %s"
                                          % (git_repo.tenant_id, errors))
            # revert to initial commit hash
            AgentGitHandler.execute_git_command(["reset", "--hard", init_head], git_repo.local_repo_path)
            return

        # push to remote
        try:
            push_op = pexpect.spawn('git push origin master', cwd=git_repo.local_repo_path)
            # push_op.logfile = sys.stdout
            push_op.expect("Username for .*")
            push_op.sendline(git_repo.repo_username)
            push_op.expect("Password for .*")
            push_op.sendline(git_repo.repo_password)
            # result = push_op.expect([commit_hash + "  master -> master", "Authentication failed for"])
            # if result != 0:
            #     raise Exception
            # TODO: handle push failure scenarios
            # push_op.interact()
            push_op.expect(pexpect.EOF)

            AgentGitHandler.log.debug("Pushed artifacts for tenant : %r" % git_repo.tenant_id)
        except:
            AgentGitHandler.log.exception("Pushing artifacts to remote repository failed for tenant %r"
                                          % git_repo.tenant_id)
            # revert to initial commit hash
            AgentGitHandler.execute_git_command(["reset", "--hard", init_head], git_repo.local_repo_path)

    @staticmethod
    def get_unstaged_files(repo_path):

        (output, errors) = AgentGitHandler.execute_git_command(["status"], repo_path=repo_path)
        unstaged_files = {"modified":[], "untracked":[]}

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
            AgentGitHandler.log.error("Unable to schedule artifact sync task, repositoryContext null for tenant %r"
                                      % repo_info.tenant_id)
            return

        if git_repo.scheduled_update_task is None:
            artifact_update_task = ArtifactUpdateTask(repo_info, auto_checkout, auto_commit)
            async_task = ScheduledExecutor(update_interval, artifact_update_task)

            git_repo.scheduled_update_task = async_task
            async_task.start()
            AgentGitHandler.log.info("Scheduled Artifact Synchronization Task for path %r" % git_repo.local_repo_path)
        else:
            AgentGitHandler.log.info("Artifact Synchronization Task for path %r already scheduled"
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
            AgentGitHandler.log.exception("Repository folder not deleted: %r" % e.get_message())

        AgentGitHandler.clear_repo(tenant_id)
        AgentGitHandler.log.info("git repository deleted for tenant %r" % git_repo.tenant_id)

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
            except GitRepositorySynchronizationException as e:
                self.log.exception("Auto checkout task failed: %r" % e.get_message())

        if self.auto_commit:
            try:
                self.log.debug("Running commit job")
                AgentGitHandler.push(self.repo_info)
            except GitRepositorySynchronizationException as e:
                self.log.exception("Auto commit failed: %r" % e.get_message())


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
            # return True
        except OSError as e:
            raise GitRepositorySynchronizationException("Directory creating failed in [%r]. " % e)

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
            GitUtils.log.debug("Directory [%r] deleted." % path)
        except OSError:
            raise GitRepositorySynchronizationException("Deletion of folder path %r failed." % path)

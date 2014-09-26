import logging
from threading import current_thread, Thread

from git import *

from gitrepository import GitRepository
from ... config.cartridgeagentconfiguration import CartridgeAgentConfiguration
from ... util import cartridgeagentutils
from ... util.asyncscheduledtask import AsyncScheduledTask
from ... artifactmgt.repositoryinformation import RepositoryInformation
from ... extensions.defaultextensionhandler import DefaultExtensionHandler


class AgentGitHandler:
    logging.basicConfig(level=logging.DEBUG)
    log = logging.getLogger(__name__)

    SUPER_TENANT_ID = -1234
    SUPER_TENANT_REPO_PATH = "/repository/deployment/server/"
    TENANT_REPO_PATH = "/repository/tenants/"

    extension_handler = DefaultExtensionHandler()

    __git_repositories = {}
    # (tenant_id => gitrepository.GitRepository)

    @staticmethod
    def checkout(repo_info):
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
            repo_context = AgentGitHandler.clone(repo_context)

        return {"subscribe_run": subscribe_run, "repo_context": repo_context}

    @staticmethod
    def sync_initial_local_artifacts(repo_context):
        #init git repo
        AgentGitHandler.init(repo_context.local_repo_path)

        # add remote repos
        return AgentGitHandler.add_remote(repo_context)

    @staticmethod
    def add_remote(repo_context):
        try:
            repo_context.repo.create_remote("origin", repo_context.repo_url)
            repo_context.repo.git.fetch()
            repo_context.repo.git.branch("-f", "--track", "master", "origin/master")
            return True
        except:
            AgentGitHandler.log.exception("Error in adding remote origin %r for local repository %r" % (repo_context.repo_url, repo_context.local_repo_path))
            return False

    @staticmethod
    def init(path):
        try:
            repo = Repo.init(path, mkdir=True)
            repo.git.init()
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
                #corrupt sha in the reference
                return False

        return True

    @staticmethod
    def pull(repo_context):
        repo = Repo(repo_context.local_repo_path)
        try:
            repo.git.checkout("master")
            pull_output = repo.git.pull()
            if "Already up-to-date." not in pull_output:
                AgentGitHandler.log.debug("Artifacts were updated as a result of the pull operation, thread: %r - %r" % (current_thread().getName(), current_thread().ident))

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
        #TODO: credential management
        repo_context = None
        try:
            repo_context = AgentGitHandler.create_git_repo_context(repo_info)
            #create the directory if it doesn't exist
            if not os.path.isdir(repo_context.local_repo_path):
                cartridgeagentutils.create_dir(repo_context.local_repo_path)

            repo = Repo.clone_from(repo_context.repo_url, repo_context.local_repo_path)

            repo_context.cloned = True
            repo_context.repo = repo
            AgentGitHandler.add_repo_context(repo_context)
            AgentGitHandler.log.info("Git clone operation for tenant %r successful" % repo_context.tenant_id)
        except GitCommandError as ex:
            if "remote: Repository not found." in ex:
                AgentGitHandler.log.exception("Accessing remote git repository failed for tenant %r" % repo_context.tenant_id)
                #GitPython deletes the target folder if remote not found
                cartridgeagentutils.create_dir(repo_context.local_repo_path)
            else:
                AgentGitHandler.log.exception("Git clone operation for tenant %r failed" % repo_context.tenant_id)
        finally:
            return repo_context

    @staticmethod
    def add_repo_context(repo_context):
        AgentGitHandler.__git_repositories[repo_context.tenant_id] = repo_context

    @staticmethod
    def get_repo_context(tenant_id):
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

        # TODO: push
        # push not implemented
        # if is_key_based_auth(repo_info.repo_url, tenant_id):
        #     repo.key_based_auth = True
        #     init_ssh_auth()
        # else:
        #     repo.key_based_auth = False

        repo_context.cloned = False

        repo_context.repo = None

        return repo_context

    # @staticmethod
    # def is_key_based_auth(repo_url, tenant_id):

    @staticmethod
    def get_repo_path_for_tenant(tenant_id, git_local_repo_path, is_multitenant):
        repo_path = ""

        if is_multitenant:
            if tenant_id == AgentGitHandler.SUPER_TENANT_ID:
                #super tenant, /repository/deploy/server/
                super_tenant_repo_path = CartridgeAgentConfiguration.super_tenant_repository_path
                #"app_path"
                repo_path += git_local_repo_path

                if super_tenant_repo_path is not None  and super_tenant_repo_path != "":
                    super_tenant_repo_path = super_tenant_repo_path if super_tenant_repo_path.startswith("/") else "/" + super_tenant_repo_path
                    super_tenant_repo_path = super_tenant_repo_path if super_tenant_repo_path.endswith("/") else  super_tenant_repo_path + "/"
                    #"app_path/repository/deploy/server/"
                    repo_path += super_tenant_repo_path
                else:
                    #"app_path/repository/deploy/server/"
                    repo_path += AgentGitHandler.SUPER_TENANT_REPO_PATH

            else:
                #normal tenant, /repository/tenants/tenant_id
                tenant_repo_path = CartridgeAgentConfiguration.tenant_repository_path
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
        raise NotImplementedError

    @staticmethod
    def schedule_artifact_update_scheduled_task(repo_info, auto_checkout, auto_commit, update_interval):
        repo_context = AgentGitHandler.get_repo_context(repo_info.tenant_id)

        if repo_context is None:
            AgentGitHandler.log.error("Unable to schedule artifact sync task, repositoryContext null for tenant %r" % repo_info.tenant_id)
            return

        if repo_context.scheduled_update_task is None:
            #TODO: make thread safe
            artifact_update_task = ArtifactUpdateTask(repo_info, auto_checkout, auto_commit)
            async_task = AsyncScheduledTask(update_interval, artifact_update_task)

            repo_context.scheduled_update_task = async_task
            async_task.start()
            AgentGitHandler.log.info("Scheduled Artifact Synchronization Task for path %r" % repo_context.local_repo_path)
        else:
            AgentGitHandler.log.info("Artifact Synchronization Task for path %r already scheduled" % repo_context.local_repo_path)


class ArtifactUpdateTask(Thread):

    def __init__(self, repo_info, auto_checkout, auto_commit):
        logging.basicConfig(level=logging.DEBUG)
        self.log = logging.getLogger(__name__)
        Thread.__init__(self)
        self.repo_info = repo_info
        self.auto_checkout = auto_checkout
        self.auto_commit = auto_commit

    def run(self):
        try:
            if self.auto_checkout:
                AgentGitHandler.checkout(self.repo_info)
        except:
            self.log.exception()

        if self.auto_commit:
            AgentGitHandler.commit(self.repo_info)

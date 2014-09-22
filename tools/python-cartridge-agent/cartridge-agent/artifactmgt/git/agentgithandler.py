from git import *
import gitrepository
import logging

from ...config.cartridgeagentconfiguration import CartridgeAgentConfiguration
from ...util import cartridgeagentutils


class AgentGitHandler:
    logging.basicConfig(level=logging.DEBUG)
    log = logging.getLogger(__name__)

    SUPER_TENANT_ID = -1234
    SUPER_TENANT_REPO_PATH = "/repository/deployment/server/"
    TENANT_REPO_PATH = "/repository/tenants/"

    cartridge_agent_config = CartridgeAgentConfiguration()

    __git_repositories = {}
    # (tenant_id => gitrepository.GitRepository)

    @staticmethod
    def checkout(repo_info):
        repo_context = AgentGitHandler.get_repo_context(repo_info.tenant_id)
        if repo_context is not None:
            #has been previously cloned, this is not the subscription run
            cloned = True
            #TODO: handle conflicts and errors using repo.git.pull(), status(), checkout() outputs
            AgentGitHandler.log.debug("Existing git repository detected for tenant %r, no clone required" % repo_info.tenant_id)
            AgentGitHandler.pull(repo_context)
        else:
            #subscribing run.. need to clone
            cloned = False
            repo_context = AgentGitHandler.create_git_repo_context(repo_info)
            AgentGitHandler.clone(repo_context)

        return {"cloned": cloned, "repo_context": repo_context}

    @staticmethod
    def pull(repo_context):
        #create repo object
        repo = Repo(repo_context.local_repo_path)
        repo.remotes.origin.pull()

        # TODO: handle conflict errors


    @staticmethod
    def clone(repo_context):
        try:
            repo = Repo.clone_from(repo_context.repo_url, repo_context.local_repo_path)
            repo_context.cloned = True
            AgentGitHandler.add_repo_context(repo_context)
            AgentGitHandler.log.info("Git clone operation for tenant %r successful" % repo_context.tenant_id)
        except GitCommandError as ex:
            if "remote: Repository not found." in ex.message:
                AgentGitHandler.log.exception("Accessing remote git repository failed for tenant %r" % repo_context.tenant_id)
                #GitPython deletes the target folder if remote not found
                cartridgeagentutils.create_dir(repo_context.local_repo_path)
            else:
                AgentGitHandler.log.exception("Git clone operation for tenant %r failed" % repo_context.tenant_id)

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
        repo_context = gitrepository.GitRepositoryContext()
        repo_context.tenant_id = repo_info.tenant_id
        repo_context.local_repo_path = AgentGitHandler.get_repo_path_for_tenant(
            repo_info.tenant_id, repo_info.repo_path, repo_info.is_multitenant)
        repo_context.repo_url = repo_info.repo_url
        repo_context.repo_username = repo_info.repo_username
        repo_context.repo_password = repo_info.repo_password

        # TODO: push
        # push not implemented
        # if is_key_based_auth(repo_info.repo_url, tenant_id):
        #     repo.key_based_auth = True
        #     init_ssh_auth()
        # else:
        #     repo.key_based_auth = False

        repo_context.cloned = False

        return repo_context

    # @staticmethod
    # def is_key_based_auth(repo_url, tenant_id):


    @staticmethod
    def get_repo_path_for_tenant(tenant_id, git_local_repo_path, is_multitenant):
        repo_path = ""

        if is_multitenant:
            if tenant_id == AgentGitHandler.SUPER_TENANT_ID:
                #super tenant, /repository/deploy/server/
                super_tenant_repo_path = AgentGitHandler.cartridge_agent_config.get_super_tenant_repo_path()
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
                tenant_repo_path = AgentGitHandler.cartridge_agent_config.get_tenant_repo_path()
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
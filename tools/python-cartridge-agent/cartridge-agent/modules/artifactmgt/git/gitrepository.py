from ...util.asyncscheduledtask import AsyncScheduledTask
from gittle import Gittle
from git import *

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
        self.repo = None
        """ :type : git.repo.base.Repo  """
        self.gittle_repo = None
        """ :type : gittle.gittle.Gittle  """
        self.tenant_id = None
        """ :type : int  """
        self.key_based_auth = False
        """ :type : bool  """
        self.repo_username = None
        """ :type : str  """
        self.repo_password = None
        """ :type : str  """
        self.is_multitenant = False
        """ :type : bool  """
        self.commit_enabled = False
        """ :type : bool  """
        self.scheduled_update_task = None
        """:type : AsyncScheduledTask """
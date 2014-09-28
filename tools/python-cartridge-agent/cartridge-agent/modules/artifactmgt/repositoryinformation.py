class RepositoryInformation:
    """
    Holds repository information to be used in artifact management
    """

    def __init__(self, repo_url, repo_username, repo_password, repo_path, tenant_id, is_multitenant, commit_enabled):
        self.repo_url = repo_url
        """ :type : str  """
        self.repo_username = repo_username
        """ :type : str  """
        self.repo_password = repo_password
        """ :type : str  """
        self.repo_path = repo_path
        """ :type : str  """
        self.tenant_id = tenant_id
        """ :type : int  """
        self.is_multitenant = is_multitenant
        """ :type : bool  """
        self.commit_enabled = commit_enabled
        """ :type : bool  """
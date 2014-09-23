class RepositoryInformation:
    """
    Holds repository information to be used in artifact management
    """

    def __init__(self, repo_url, repo_username, repo_password, repo_path, tenant_id, is_multitenant, commit_enabled):
        self.repo_url = repo_url
        self.repo_username = repo_username
        self.repo_password = repo_password
        self.repo_path = repo_path
        self.tenant_id = tenant_id
        self.is_multitenant = is_multitenant
        self.commit_enabled = commit_enabled
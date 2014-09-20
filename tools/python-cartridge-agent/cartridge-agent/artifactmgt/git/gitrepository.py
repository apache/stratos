class GitRepositoryContext:

    def __init__(self):
        self.repo_url = None
        self.local_repo_path = None
        self.cloned = False
        self.repo = None
        self.tenant_id = None
        self.key_based_auth = False
        self.repo_username = None
        self.repo_password = None
        #scheduled update service
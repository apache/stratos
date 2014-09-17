import json


class ArtifactUpdatedEvent:
    def __init__(self):
        self.cluster_id = None
        self.status = None
        self.repo_username = None
        self.repo_password = None
        self.repo_url = None
        self.tenant_id = None
        self.commit_enabled = None

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = ArtifactUpdatedEvent()

        instance.cluster_id = json_obj["clusterId"] if "clusterId" in json_obj else None
        instance.status = json_obj["status"] if "status" in json_obj else None
        instance.repo_username = json_obj["repoUserName"] if "repoUserName" in json_obj else None
        instance.repo_password = json_obj["repoPassword"] if "repoPassword" in json_obj else None
        instance.tenant_id = json_obj["tenantId"] if "tenantId" in json_obj else None
        instance.commit_enabled = json_obj["commitEnabled"] if "commitEnabled" in json_obj else None

        return instance